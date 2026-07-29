package com.palordersoftworks.amethysttools;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolSystems {
    private static final Set<Block> BLACKLIST = createBlacklist();
    private final Set<UUID> miningLocks = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> lastHeldSignature = new HashMap<>();

    public void register() {
        PlayerBlockBreakEvents.AFTER.register(this::onAfterBlockBreak);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onAfterBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        ItemStack tool = serverPlayer.getMainHandStack();
        if (!ToolDefinitions.isAmethystTool(tool)) {
            return;
        }

        if (BLACKLIST.contains(state.getBlock())) {
            return;
        }

        if (!tool.isSuitableFor(state)) {
            return;
        }

        if (!miningLocks.add(serverPlayer.getUuid())) {
            return;
        }

        try {
            float pitch = serverPlayer.getPitch();
            if (pitch > 45.0F || pitch < -45.0F) {
                breakVertical(world, serverPlayer, pos, tool);
            } else {
                breakHorizontal(world, serverPlayer, pos, tool);
            }
        } finally {
            miningLocks.remove(serverPlayer.getUuid());
        }
    }

    private void breakHorizontal(World world, ServerPlayerEntity player, BlockPos center, ItemStack tool) {
        Direction facing = player.getHorizontalFacing();
        for (int y = -1; y <= 1; y++) {
            for (int side = -1; side <= 1; side++) {
                BlockPos target = (facing == Direction.NORTH || facing == Direction.SOUTH)
                        ? center.add(side, y, 0)
                        : center.add(0, y, side);
                breakTarget(world, player, center, target, tool);
            }
        }
    }

    private void breakVertical(World world, ServerPlayerEntity player, BlockPos center, ItemStack tool) {
        Direction facing = player.getHorizontalFacing();
        float pitch = player.getPitch();

        if (Math.abs(pitch) > 45.0F) {
            int depth = pitch > 0.0F ? -1 : 1;
            for (int y = 0; y < 2; y++) {
                for (int side = -1; side <= 1; side++) {
                    BlockPos target = (facing == Direction.NORTH || facing == Direction.SOUTH)
                            ? center.add(side, depth * y, 0)
                            : center.add(0, depth * y, side);
                    breakTarget(world, player, center, target, tool);
                }
            }
            return;
        }

        for (int side = -1; side <= 1; side++) {
            for (int depth = 1; depth <= 2; depth++) {
                BlockPos target = switch (facing) {
                    case NORTH -> center.add(side, 0, -depth);
                    case SOUTH -> center.add(side, 0, depth);
                    case EAST -> center.add(depth, 0, side);
                    case WEST -> center.add(-depth, 0, side);
                    default -> center;
                };
                breakTarget(world, player, center, target, tool);
            }
        }
    }

    private void breakTarget(World world, ServerPlayerEntity player, BlockPos center, BlockPos target, ItemStack tool) {
        if (target.equals(center)) {
            return;
        }

        BlockState targetState = world.getBlockState(target);
        if (targetState.isAir()) {
            return;
        }

        if (BLACKLIST.contains(targetState.getBlock())) {
            return;
        }

        if (!tool.isSuitableFor(targetState)) {
            return;
        }

        world.breakBlock(target, true, player);
    }

    private void onServerTick(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            onlinePlayers.add(player.getUuid());

            String signature = ToolDefinitions.signature(player.getMainHandStack());
            String previous = lastHeldSignature.put(player.getUuid(), signature);

            if (previous != null && !previous.equals(signature) && !signature.isEmpty()) {
                player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 2.0F);
            }
        }

        lastHeldSignature.keySet().retainAll(onlinePlayers);
    }

    private static Set<Block> createBlacklist() {
        Set<Block> blacklist = new HashSet<>();

        add(blacklist, Blocks.CHEST);
        add(blacklist, Blocks.TRAPPED_CHEST);
        add(blacklist, Blocks.SHULKER_BOX);
        add(blacklist, Blocks.BLACK_SHULKER_BOX);
        add(blacklist, Blocks.BLUE_SHULKER_BOX);
        add(blacklist, Blocks.BROWN_SHULKER_BOX);
        add(blacklist, Blocks.CYAN_SHULKER_BOX);
        add(blacklist, Blocks.GRAY_SHULKER_BOX);
        add(blacklist, Blocks.GREEN_SHULKER_BOX);
        add(blacklist, Blocks.LIGHT_BLUE_SHULKER_BOX);
        add(blacklist, Blocks.LIGHT_GRAY_SHULKER_BOX);
        add(blacklist, Blocks.LIME_SHULKER_BOX);
        add(blacklist, Blocks.MAGENTA_SHULKER_BOX);
        add(blacklist, Blocks.ORANGE_SHULKER_BOX);
        add(blacklist, Blocks.PINK_SHULKER_BOX);
        add(blacklist, Blocks.PURPLE_SHULKER_BOX);
        add(blacklist, Blocks.RED_SHULKER_BOX);
        add(blacklist, Blocks.WHITE_SHULKER_BOX);
        add(blacklist, Blocks.YELLOW_SHULKER_BOX);
        add(blacklist, Blocks.BEDROCK);
        add(blacklist, Blocks.OBSIDIAN);
        add(blacklist, Blocks.CRYING_OBSIDIAN);
        add(blacklist, Blocks.COMMAND_BLOCK);
        add(blacklist, Blocks.CHAIN_COMMAND_BLOCK);
        add(blacklist, Blocks.REPEATING_COMMAND_BLOCK);
        add(blacklist, Blocks.COPPER_CHEST);
        add(blacklist, Blocks.EXPOSED_COPPER_CHEST);
        add(blacklist, Blocks.WEATHERED_COPPER_CHEST);
        add(blacklist, Blocks.OXIDIZED_COPPER_CHEST);
        add(blacklist, Blocks.WAXED_COPPER_CHEST);
        add(blacklist, Blocks.WAXED_EXPOSED_COPPER_CHEST);
        add(blacklist, Blocks.WAXED_WEATHERED_COPPER_CHEST);
        add(blacklist, Blocks.WAXED_OXIDIZED_COPPER_CHEST);

        return blacklist;
    }

    private static void add(Set<Block> blacklist, Block block) {
        blacklist.add(block);
    }
}
