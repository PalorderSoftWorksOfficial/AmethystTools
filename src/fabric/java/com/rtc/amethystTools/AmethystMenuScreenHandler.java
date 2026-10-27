package com.palordersoftworks.amethysttools;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

public final class AmethystMenuScreenHandler extends ChestMenu {
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

    private final SimpleContainer inventory;
    private final MenuKind kind;

    public AmethystMenuScreenHandler(int syncId, Inventory playerInventory, MenuKind kind) {
        this(syncId, playerInventory, new SimpleContainer(27), kind);
    }

    private AmethystMenuScreenHandler(int syncId, Inventory playerInventory, SimpleContainer inventory, MenuKind kind) {
        super(MenuType.GENERIC_9x3, syncId, playerInventory, inventory, 3);
        this.inventory = inventory;
        this.kind = kind;
        populate();
    }

    private void populate() {
        inventory.clearContent();

        if (kind == MenuKind.ROOT) {
            inventory.setItem(10, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.WOODEN));
            inventory.setItem(11, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.STONE));
            inventory.setItem(12, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.IRON));
            inventory.setItem(13, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.COPPER));
            inventory.setItem(14, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.GOLDEN));
            inventory.setItem(15, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.DIAMOND));
            inventory.setItem(16, ToolDefinitions.createTierIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Tier.NETHERITE));
            return;
        }

        inventory.setItem(11, ToolDefinitions.createTypeIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Type.PICKAXE));
        inventory.setItem(12, ToolDefinitions.createTypeIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Type.SHOVEL));
        inventory.setItem(13, ToolDefinitions.createTypeIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Type.AXE));
        inventory.setItem(14, ToolDefinitions.createTypeIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Type.HOE));
        inventory.setItem(15, ToolDefinitions.createTypeIcon(AmethystToolsFabric.CONFIG, ToolDefinitions.Type.SWORD));
        inventory.setItem(18, ToolDefinitions.createBackIcon(AmethystToolsFabric.CONFIG));
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        if (slotIndex >= 0 && slotIndex < inventory.getContainerSize()) {
            handleClick(player, slotIndex);
            return;
        }

        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private void handleClick(Player player, int slotIndex) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.closeContainer();
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
    }

    private void handleToolMenu(ServerPlayer player, int slotIndex) {
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

    private void handleTierMenu(ServerPlayer player, ToolDefinitions.Tier tier, int slotIndex) {
        switch (slotIndex) {
            case 11 -> AmethystToolsFabric.giveTool(player, tier, ToolDefinitions.Type.PICKAXE);
            case 12 -> AmethystToolsFabric.giveTool(player, tier, ToolDefinitions.Type.SHOVEL);
            case 13 -> AmethystToolsFabric.giveTool(player, tier, ToolDefinitions.Type.AXE);
            case 14 -> AmethystToolsFabric.giveTool(player, tier, ToolDefinitions.Type.HOE);
            case 15 -> AmethystToolsFabric.giveTool(player, tier, ToolDefinitions.Type.SWORD);
            case 18 -> AmethystToolsFabric.openMenu(player, MenuKind.ROOT);
            default -> {
            }
        }
    }
}
