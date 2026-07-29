package com.palordersoftworks.amethysttools;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

public final class AmethystMenuScreenHandler extends GenericContainerScreenHandler {
    public enum MenuKind {
        ROOT,
        WOODEN,
        STONE,
        IRON,
        COPPER,
        GOLDEN,
        DIAMOND,
        NETHERITE
    }

    private final SimpleInventory inventory;
    private final MenuKind kind;

    public AmethystMenuScreenHandler(int syncId, PlayerInventory playerInventory, MenuKind kind) {
        this(syncId, playerInventory, new SimpleInventory(27), kind);
    }

    private AmethystMenuScreenHandler(int syncId, PlayerInventory playerInventory, SimpleInventory inventory, MenuKind kind) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, 3);
        this.inventory = inventory;
        this.kind = kind;
        populate();
    }

    private void populate() {
        inventory.clear();

        if (kind == MenuKind.ROOT) {
            inventory.setStack(10, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.WOODEN));
            inventory.setStack(11, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.STONE));
            inventory.setStack(12, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.IRON));
            inventory.setStack(13, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.COPPER));
            inventory.setStack(14, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.GOLDEN));
            inventory.setStack(15, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.DIAMOND));
            inventory.setStack(16, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.NETHERITE));
            return;
        }

        inventory.setStack(12, ToolDefinitions.createTypeIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Type.PICKAXE));
        inventory.setStack(13, ToolDefinitions.createTypeIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Type.SHOVEL));
        inventory.setStack(14, ToolDefinitions.createTypeIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Type.AXE));
        inventory.setStack(18, ToolDefinitions.createBackIcon(AmethystToolsFabric.CONFIG));
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < inventory.size()) {
            handleClick(player, slotIndex);
            return;
        }

        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    private void handleClick(PlayerEntity player, int slotIndex) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        serverPlayer.closeHandledScreen();
        serverPlayer.getServer().execute(() -> {
            if (kind == MenuKind.ROOT) {
                switch (slotIndex) {
                    case 10 -> AmethystToolsFabric.openMenu(serverPlayer, MenuKind.WOODEN);
                    case 11 -> AmethystToolsFabric.openMenu(serverPlayer, MenuKind.STONE);
                    case 12 -> AmethystToolsFabric.openMenu(serverPlayer, MenuKind.IRON);
                    case 13 -> AmethystToolsFabric.openMenu(serverPlayer, MenuKind.COPPER);
                    case 14 -> AmethystToolsFabric.openMenu(serverPlayer, MenuKind.GOLDEN);
                    case 15 -> AmethystToolsFabric.openMenu(serverPlayer, MenuKind.DIAMOND);
                    case 16 -> AmethystToolsFabric.openMenu(serverPlayer, MenuKind.NETHERITE);
                    default -> {
                    }
                }
                return;
            }

            handleToolMenu(serverPlayer, slotIndex);
        });
    }

    private void handleToolMenu(ServerPlayerEntity player, int slotIndex) {
        switch (kind) {
            case WOODEN -> handleTierMenu(player, ToolDefinitions.Tier.WOODEN, slotIndex);
            case STONE -> handleTierMenu(player, ToolDefinitions.Tier.STONE, slotIndex);
            case IRON -> handleTierMenu(player, ToolDefinitions.Tier.IRON, slotIndex);
            case COPPER -> handleTierMenu(player, ToolDefinitions.Tier.COPPER, slotIndex);
            case GOLDEN -> handleTierMenu(player, ToolDefinitions.Tier.GOLDEN, slotIndex);
            case DIAMOND -> handleTierMenu(player, ToolDefinitions.Tier.DIAMOND, slotIndex);
            case NETHERITE -> handleTierMenu(player, ToolDefinitions.Tier.NETHERITE, slotIndex);
            default -> {
            }
        }
    }

    private void handleTierMenu(ServerPlayerEntity player, ToolDefinitions.Tier tier, int slotIndex) {
        switch (slotIndex) {
            case 12 -> AmethystToolsFabric.giveTool(player, tier, ToolDefinitions.Type.PICKAXE);
            case 13 -> AmethystToolsFabric.giveTool(player, tier, ToolDefinitions.Type.SHOVEL);
            case 14 -> AmethystToolsFabric.giveTool(player, tier, ToolDefinitions.Type.AXE);
            case 18 -> AmethystToolsFabric.openMenu(player, MenuKind.ROOT);
            default -> {
            }
        }
    }
}
