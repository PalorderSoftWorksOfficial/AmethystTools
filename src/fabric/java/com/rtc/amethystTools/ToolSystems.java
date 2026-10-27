package com.palordersoftworks.amethysttools;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolSystems {
    /**
     * Mining area before any Blocks enchantment, as width and height of the mined plane.
     */
    private static final int BASE_AREA = 3;
    private static final int BASE_DEPTH = 2;

    private static final ResourceKey<Enchantment> AREA_ENCHANTMENT =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(AmethystToolsFabric.MOD_ID, "blocks"));
    private static final ResourceKey<Enchantment> VEIN_ENCHANTMENT =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(AmethystToolsFabric.MOD_ID, "veinminer"));
    private static final ResourceKey<Enchantment> DEPTH_ENCHANTMENT =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(AmethystToolsFabric.MOD_ID, "depth"));
    private static final ResourceKey<Enchantment> RESONANCE_ENCHANTMENT =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(AmethystToolsFabric.MOD_ID, "resonance"));
    private static final TagKey<Block> VEIN_ORE_TAG =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(AmethystToolsFabric.MOD_ID, "veinminer_ores"));

    private static final Set<Block> BLACKLIST = createBlacklist();
    private final Set<UUID> miningLocks = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> lastHeldSignature = new HashMap<>();

    public void register() {
        PlayerBlockBreakEvents.AFTER.register(this::onAfterBlockBreak);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onAfterBlockBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack tool = serverPlayer.getMainHandItem();
        if (!ToolDefinitions.isAmethystTool(tool)) {
            return;
        }

        if (BLACKLIST.contains(state.getBlock())) {
            return;
        }

        if (!tool.isCorrectToolForDrops(state)) {
            return;
        }

        if (!miningLocks.add(serverPlayer.getUUID())) {
            return;
        }

        try {
            // Vein miner runs first so it can still walk the vein that the area break is about to open up.
            veinMine(world, serverPlayer, pos, state, tool);

            int size = areaSize(world, tool);
            int depthExtra = depthExtra(world, tool);
            float pitch = serverPlayer.getXRot();
            if (pitch > 45.0F || pitch < -45.0F) {
                breakVertical(world, serverPlayer, pos, tool, size, depthExtra);
            } else {
                breakHorizontal(world, serverPlayer, pos, tool, size, depthExtra);
            }
        } finally {
            miningLocks.remove(serverPlayer.getUUID());
        }
    }

    private void breakHorizontal(Level world, ServerPlayer player, BlockPos center, ItemStack tool, int size, int depthExtra) {
        Direction facing = player.getDirection();
        int min = -(size / 2);
        int max = min + size - 1;

        for (int y = min; y <= max; y++) {
            for (int side = min; side <= max; side++) {
                BlockPos target = (facing == Direction.NORTH || facing == Direction.SOUTH)
                        ? center.offset(side, y, 0)
                        : center.offset(0, y, side);
                breakTarget(world, player, center, target, tool);
            }
        }

        // Depth pushes that same cut further into the blocks the player is facing.
        for (int depth = 1; depth <= depthExtra; depth++) {
            BlockPos layer = center.relative(facing, depth);
            for (int y = min; y <= max; y++) {
                for (int side = min; side <= max; side++) {
                    BlockPos target = (facing == Direction.NORTH || facing == Direction.SOUTH)
                            ? layer.offset(side, y, 0)
                            : layer.offset(0, y, side);
                    breakTarget(world, player, center, target, tool);
                }
            }
        }
    }

    private void breakVertical(Level world, ServerPlayer player, BlockPos center, ItemStack tool, int size, int depthExtra) {
        Direction facing = player.getDirection();
        float pitch = player.getXRot();
        int min = -(size / 2);
        int max = min + size - 1;
        int reach = BASE_DEPTH + depthExtra;

        if (Math.abs(pitch) > 45.0F) {
            int depth = pitch > 0.0F ? -1 : 1;
            for (int y = 0; y < reach; y++) {
                for (int dx = min; dx <= max; dx++) {
                    for (int dz = min; dz <= max; dz++) {
                        BlockPos target = center.offset(dx, depth * y, dz);
                        breakTarget(world, player, center, target, tool);
                    }
                }
            }
            return;
        }

        for (int side = min; side <= max; side++) {
            for (int depth = 1; depth <= reach; depth++) {
                BlockPos target = switch (facing) {
                    case NORTH -> center.offset(side, 0, -depth);
                    case SOUTH -> center.offset(side, 0, depth);
                    case EAST -> center.offset(depth, 0, side);
                    case WEST -> center.offset(-depth, 0, side);
                    default -> center;
                };
                breakTarget(world, player, center, target, tool);
            }
        }
    }

    /**
     * Breaks an extra block the same way a vanilla block break would, but with the mining tool
     * so Fortune and Silk Touch affect the drops and experience.
     *
     * @return true when the block was actually broken
     */
    private boolean breakTarget(Level world, ServerPlayer player, BlockPos center, BlockPos target, ItemStack tool) {
        if (target.equals(center)) {
            return false;
        }

        BlockState targetState = world.getBlockState(target);
        if (targetState.isAir()) {
            return false;
        }

        if (BLACKLIST.contains(targetState.getBlock())) {
            return false;
        }

        if (!tool.isCorrectToolForDrops(targetState)) {
            return false;
        }

        BlockEntity blockEntity = targetState.hasBlockEntity() ? world.getBlockEntity(target) : null;
        if (!world.destroyBlock(target, false, player, Block.UPDATE_ALL)) {
            return false;
        }

        Block.dropResources(targetState, world, target, blockEntity, player, tool);
        maybeResonate(world, player, target, tool);
        return true;
    }

    /**
     * Breaks every block connected to the broken one when the tool carries Vein Miner,
     * up to the level based block limit.
     */
    private void veinMine(Level world, ServerPlayer player, BlockPos origin, BlockState originState, ItemStack tool) {
        AmethystToolConfig config = AmethystToolsFabric.CONFIG;
        if (!config.getBoolean("enchant.veinminer-enabled", true)) {
            return;
        }

        int level = enchantmentLevel(world, tool, VEIN_ENCHANTMENT);
        if (level <= 0) {
            return;
        }

        Block block = originState.getBlock();
        if (config.getBoolean("enchant.veinminer-ores-only", true) && !isVeinOre(originState)) {
            return;
        }

        int perLevel = Math.max(1, config.getInt("enchant.veinminer-blocks-per-level", 16));
        int limit = level * perLevel;

        Set<BlockPos> visited = new HashSet<>();
        visited.add(origin);

        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);

        int broken = 0;
        while (!queue.isEmpty() && broken < limit) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.values()) {
                if (broken >= limit) {
                    break;
                }

                BlockPos next = current.relative(direction);
                if (!visited.add(next)) {
                    continue;
                }

                BlockState nextState = world.getBlockState(next);
                if (nextState.isAir() || nextState.getBlock() != block) {
                    continue;
                }

                if (!breakTarget(world, player, origin, next, tool)) {
                    continue;
                }

                broken++;
                queue.add(next);
            }
        }
    }

    /**
     * Blocks the Depth enchantment adds beyond the base reach of the vertical cut.
     */
    private static int depthExtra(Level world, ItemStack stack) {
        AmethystToolConfig config = AmethystToolsFabric.CONFIG;
        if (!config.getBoolean("enchant.depth-enabled", true)) {
            return 0;
        }

        return Math.max(0, enchantmentLevel(world, stack, DEPTH_ENCHANTMENT));
    }

    /**
     * Resonance makes the extra blocks broken by the tool chime back with amethyst shards.
     */
    private void maybeResonate(Level world, ServerPlayer player, BlockPos target, ItemStack tool) {
        AmethystToolConfig config = AmethystToolsFabric.CONFIG;
        if (!config.getBoolean("enchant.resonance-enabled", true)) {
            return;
        }

        int level = enchantmentLevel(world, tool, RESONANCE_ENCHANTMENT);
        if (level <= 0) {
            return;
        }

        int chancePerLevel = Math.max(0, config.getInt("enchant.resonance-chance-per-level", 10));
        if (chancePerLevel == 0) {
            return;
        }

        int chance = Math.min(100, level * chancePerLevel);
        if (world.getRandom().nextInt(100) >= chance) {
            return;
        }

        Block.popResource(world, target, new ItemStack(Items.AMETHYST_SHARD));
        player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.7F);
    }

    private boolean isVeinOre(BlockState state) {
        if (state.is(VEIN_ORE_TAG, ignored -> true)) {
            return true;
        }

        return state.getBlock() instanceof DropExperienceBlock;
    }

    /**
     * Width of the mined plane. Every Blocks level grows it by the configured amount,
     * so level 1 mines 4x4, level 2 mines 5x5 and level 5 mines 8x8.
     */
    private static int areaSize(Level world, ItemStack stack) {
        AmethystToolConfig config = AmethystToolsFabric.CONFIG;
        if (!config.getBoolean("enchant.blocks-enabled", true)) {
            return BASE_AREA;
        }

        int level = enchantmentLevel(world, stack, AREA_ENCHANTMENT);
        if (level <= 0) {
            return BASE_AREA;
        }

        int extraPerLevel = Math.max(1, config.getInt("enchant.blocks-size-per-level", 1));
        return BASE_AREA + level * extraPerLevel;
    }

    private static int enchantmentLevel(Level world, ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) {
            return 0;
        }

        Registry<Enchantment> registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> holder = registry.get(key.identifier());
        return holder.map(reference -> stack.getEnchantments().getLevel(reference)).orElse(0);
    }

    private void onServerTick(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            onlinePlayers.add(player.getUUID());

            String signature = ToolDefinitions.signature(player.getMainHandItem());
            String previous = lastHeldSignature.put(player.getUUID(), signature);

            if (previous != null && !previous.equals(signature) && !signature.isEmpty()) {
                player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 2.0F);
            }
        }

        lastHeldSignature.keySet().retainAll(onlinePlayers);
    }

    private static Set<Block> createBlacklist() {
        Set<Block> blacklist = new HashSet<>();

        add(blacklist, Blocks.CHEST);
        add(blacklist, Blocks.TRAPPED_CHEST);
        add(blacklist, Blocks.SHULKER_BOX);
        Blocks.DYED_SHULKER_BOX.forEach(blacklist::add);
        add(blacklist, Blocks.BEDROCK);
        add(blacklist, Blocks.OBSIDIAN);
        add(blacklist, Blocks.CRYING_OBSIDIAN);
        add(blacklist, Blocks.COMMAND_BLOCK);
        add(blacklist, Blocks.CHAIN_COMMAND_BLOCK);
        add(blacklist, Blocks.REPEATING_COMMAND_BLOCK);
        addCopperChests(blacklist, Blocks.COPPER_CHEST);

        return blacklist;
    }

    private static void add(Set<Block> blacklist, Block block) {
        blacklist.add(block);
    }

    private static void addCopperChests(Set<Block> blacklist, net.minecraft.world.level.block.WeatheringCopperCollection<Block> collection) {
        collection.weathering().forEach(blacklist::add);
        collection.waxed().forEach(blacklist::add);
    }
}
