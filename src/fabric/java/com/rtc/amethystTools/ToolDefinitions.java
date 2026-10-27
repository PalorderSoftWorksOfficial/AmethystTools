package com.palordersoftworks.amethysttools;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ToolDefinitions {
    public static final String TOOL_FLAG = "amethyst_tool";
    public static final String TIER_KEY = "tier";
    public static final String TYPE_KEY = "type";

    private ToolDefinitions() {
    }

    public enum Tier {
        WOODEN("wooden", "Wooden", Items.OAK_PLANKS, ChatFormatting.GOLD),
        STONE("stone", "Stone", Items.STONE, ChatFormatting.GRAY),
        IRON("iron", "Iron", Items.IRON_INGOT, ChatFormatting.WHITE),
        COPPER("copper", "Copper", Items.COPPER_INGOT, ChatFormatting.GOLD),
        GOLDEN("golden", "Golden", Items.GOLD_INGOT, ChatFormatting.YELLOW),
        DIAMOND("diamond", "Diamond", Items.DIAMOND, ChatFormatting.AQUA),
        NETHERITE("netherite", "Netherite", Items.NETHERITE_INGOT, ChatFormatting.DARK_GRAY);

        private final String id;
        private final String displayName;
        private final Item iconItem;
        private final ChatFormatting formatting;

        Tier(String id, String displayName, Item iconItem, ChatFormatting formatting) {
            this.id = id;
            this.displayName = displayName;
            this.iconItem = iconItem;
            this.formatting = formatting;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public Item iconItem() {
            return iconItem;
        }

        public ChatFormatting formatting() {
            return formatting;
        }

        public String menuKey() {
            return "menus.menu-" + id;
        }

        public static Tier fromId(String raw) {
            String value = raw.toLowerCase(Locale.ROOT);
            return switch (value) {
                case "wooden" -> WOODEN;
                case "stone" -> STONE;
                case "iron" -> IRON;
                case "copper" -> COPPER;
                case "golden" -> GOLDEN;
                case "diamond" -> DIAMOND;
                case "netherite" -> NETHERITE;
                default -> null;
            };
        }
    }

    public enum Type {
        PICKAXE("pickaxe", "Pickaxe", Items.WOODEN_PICKAXE, ChatFormatting.AQUA),
        SHOVEL("shovel", "Shovel", Items.WOODEN_SHOVEL, ChatFormatting.GREEN),
        AXE("axe", "Axe", Items.WOODEN_AXE, ChatFormatting.RED),
        HOE("hoe", "Hoe", Items.WOODEN_HOE, ChatFormatting.YELLOW),
        SWORD("sword", "Sword", Items.WOODEN_SWORD, ChatFormatting.LIGHT_PURPLE);

        private final String id;
        private final String displayName;
        private final Item iconItem;
        private final ChatFormatting formatting;

        Type(String id, String displayName, Item iconItem, ChatFormatting formatting) {
            this.id = id;
            this.displayName = displayName;
            this.iconItem = iconItem;
            this.formatting = formatting;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public Item iconItem() {
            return iconItem;
        }

        public ChatFormatting formatting() {
            return formatting;
        }

        public String menuKey() {
            return "item.item-" + id;
        }

        public static Type fromId(String raw) {
            String value = raw.toLowerCase(Locale.ROOT);
            return switch (value) {
                case "pickaxe" -> PICKAXE;
                case "shovel" -> SHOVEL;
                case "axe" -> AXE;
                case "hoe" -> HOE;
                case "sword" -> SWORD;
                default -> null;
            };
        }
    }

    public static List<String> tierIds() {
        return Arrays.stream(Tier.values()).map(Tier::id).toList();
    }

    public static List<String> typeIds() {
        return Arrays.stream(Type.values()).map(Type::id).toList();
    }

    public static ItemStack createTierIcon(AmethystToolConfig config, Tier tier) {
        String label = config.get(tier.menuKey(), tier.displayName() + " Tools");
        return namedStack(tier.iconItem(), label, tier.formatting());
    }

    public static ItemStack createTypeIcon(AmethystToolConfig config, Type type) {
        String label = config.get(type.menuKey(), type.displayName());
        return namedStack(type.iconItem(), label, type.formatting());
    }

    public static ItemStack createBackIcon(AmethystToolConfig config) {
        return namedStack(Items.ARROW, config.get("menus.item-previous", "Previous Page"), ChatFormatting.YELLOW);
    }

    public static ItemStack createToolStack(AmethystToolConfig config, Tier tier, Type type) {
        ItemStack stack = new ItemStack(toolItem(tier, type));
        String prefix = config.get("item.item-name", "Amethyst");
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(prefix + " " + tier.displayName() + " " + type.displayName()).withStyle(ChatFormatting.DARK_PURPLE));

        CompoundTag data = new CompoundTag();
        data.putBoolean(TOOL_FLAG, true);
        data.putString(TIER_KEY, tier.id());
        data.putString(TYPE_KEY, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    public static boolean isAmethystTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return false;
        }

        CompoundTag data = customData.copyTag();
        return data.getBooleanOr(TOOL_FLAG, false);
    }

    public static String signature(ItemStack stack) {
        if (!isAmethystTool(stack)) {
            return "";
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }

        CompoundTag data = customData.copyTag();
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + ":" + data.getStringOr(TIER_KEY, "") + ":" + data.getStringOr(TYPE_KEY, "");
    }

    private static ItemStack namedStack(Item item, String name, ChatFormatting formatting) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(formatting));
        return stack;
    }

    private static Item toolItem(Tier tier, Type type) {
        return switch (tier) {
            case WOODEN -> switch (type) {
                case PICKAXE -> Items.WOODEN_PICKAXE;
                case SHOVEL -> Items.WOODEN_SHOVEL;
                case AXE -> Items.WOODEN_AXE;
                case HOE -> Items.WOODEN_HOE;
                case SWORD -> Items.WOODEN_SWORD;
            };
            case STONE -> switch (type) {
                case PICKAXE -> Items.STONE_PICKAXE;
                case SHOVEL -> Items.STONE_SHOVEL;
                case AXE -> Items.STONE_AXE;
                case HOE -> Items.STONE_HOE;
                case SWORD -> Items.STONE_SWORD;
            };
            case IRON -> switch (type) {
                case PICKAXE -> Items.IRON_PICKAXE;
                case SHOVEL -> Items.IRON_SHOVEL;
                case AXE -> Items.IRON_AXE;
                case HOE -> Items.IRON_HOE;
                case SWORD -> Items.IRON_SWORD;
            };
            case COPPER -> switch (type) {
                case PICKAXE -> Items.COPPER_PICKAXE;
                case SHOVEL -> Items.COPPER_SHOVEL;
                case AXE -> Items.COPPER_AXE;
                case HOE -> Items.COPPER_HOE;
                case SWORD -> Items.COPPER_SWORD;
            };
            case GOLDEN -> switch (type) {
                case PICKAXE -> Items.GOLDEN_PICKAXE;
                case SHOVEL -> Items.GOLDEN_SHOVEL;
                case AXE -> Items.GOLDEN_AXE;
                case HOE -> Items.GOLDEN_HOE;
                case SWORD -> Items.GOLDEN_SWORD;
            };
            case DIAMOND -> switch (type) {
                case PICKAXE -> Items.DIAMOND_PICKAXE;
                case SHOVEL -> Items.DIAMOND_SHOVEL;
                case AXE -> Items.DIAMOND_AXE;
                case HOE -> Items.DIAMOND_HOE;
                case SWORD -> Items.DIAMOND_SWORD;
            };
            case NETHERITE -> switch (type) {
                case PICKAXE -> Items.NETHERITE_PICKAXE;
                case SHOVEL -> Items.NETHERITE_SHOVEL;
                case AXE -> Items.NETHERITE_AXE;
                case HOE -> Items.NETHERITE_HOE;
                case SWORD -> Items.NETHERITE_SWORD;
            };
        };
    }
}
