package com.rtc.amethystTools;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        WOODEN("wooden", "Wooden", Items.OAK_PLANKS, Formatting.GOLD),
        STONE("stone", "Stone", Items.STONE, Formatting.GRAY),
        IRON("iron", "Iron", Items.IRON_INGOT, Formatting.WHITE),
        COPPER("copper", "Copper", Items.COPPER_INGOT, Formatting.GOLD),
        GOLDEN("golden", "Golden", Items.GOLD_INGOT, Formatting.YELLOW),
        DIAMOND("diamond", "Diamond", Items.DIAMOND, Formatting.AQUA),
        NETHERITE("netherite", "Netherite", Items.NETHERITE_INGOT, Formatting.DARK_GRAY);

        private final String id;
        private final String displayName;
        private final Item iconItem;
        private final Formatting formatting;

        Tier(String id, String displayName, Item iconItem, Formatting formatting) {
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

        public Formatting formatting() {
            return formatting;
        }

        public String menuKey() {
            return "menus." + id;
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
        PICKAXE("pickaxe", "Pickaxe", Items.WOODEN_PICKAXE, Formatting.AQUA),
        SHOVEL("shovel", "Shovel", Items.WOODEN_SHOVEL, Formatting.GREEN),
        AXE("axe", "Axe", Items.WOODEN_AXE, Formatting.RED);

        private final String id;
        private final String displayName;
        private final Item iconItem;
        private final Formatting formatting;

        Type(String id, String displayName, Item iconItem, Formatting formatting) {
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

        public Formatting formatting() {
            return formatting;
        }

        public String menuKey() {
            return "item." + id;
        }

        public static Type fromId(String raw) {
            String value = raw.toLowerCase(Locale.ROOT);
            return switch (value) {
                case "pickaxe" -> PICKAXE;
                case "shovel" -> SHOVEL;
                case "axe" -> AXE;
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
        return namedStack(Items.ARROW, config.get("menus.back", "Previous Page"), Formatting.YELLOW);
    }

    public static ItemStack createToolStack(AmethystToolConfig config, Tier tier, Type type) {
        ItemStack stack = new ItemStack(toolItem(tier, type));
        String prefix = config.get("item.name", "Amethyst");
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(prefix + " " + tier.displayName() + " " + type.displayName()).formatted(Formatting.DARK_PURPLE));

        NbtCompound data = new NbtCompound();
        data.putBoolean(TOOL_FLAG, true);
        data.putString(TIER_KEY, tier.id());
        data.putString(TYPE_KEY, type.id());
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
        return stack;
    }

    public static boolean isAmethystTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return false;
        }

        NbtCompound data = customData.copyNbt();
        return data.getBoolean(TOOL_FLAG);
    }

    public static String signature(ItemStack stack) {
        if (!isAmethystTool(stack)) {
            return "";
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return Registries.ITEM.getId(stack.getItem()).toString();
        }

        NbtCompound data = customData.copyNbt();
        return Registries.ITEM.getId(stack.getItem()) + ":" + data.getString(TIER_KEY) + ":" + data.getString(TYPE_KEY);
    }

    private static ItemStack namedStack(Item item, String name, Formatting formatting) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(formatting));
        return stack;
    }

    private static Item toolItem(Tier tier, Type type) {
        return switch (tier) {
            case WOODEN -> switch (type) {
                case PICKAXE -> Items.WOODEN_PICKAXE;
                case SHOVEL -> Items.WOODEN_SHOVEL;
                case AXE -> Items.WOODEN_AXE;
            };
            case STONE -> switch (type) {
                case PICKAXE -> Items.STONE_PICKAXE;
                case SHOVEL -> Items.STONE_SHOVEL;
                case AXE -> Items.STONE_AXE;
            };
            case IRON -> switch (type) {
                case PICKAXE -> Items.IRON_PICKAXE;
                case SHOVEL -> Items.IRON_SHOVEL;
                case AXE -> Items.IRON_AXE;
            };
            case COPPER -> switch (type) {
                case PICKAXE -> Items.COPPER_PICKAXE;
                case SHOVEL -> Items.COPPER_SHOVEL;
                case AXE -> Items.COPPER_AXE;
            };
            case GOLDEN -> switch (type) {
                case PICKAXE -> Items.GOLDEN_PICKAXE;
                case SHOVEL -> Items.GOLDEN_SHOVEL;
                case AXE -> Items.GOLDEN_AXE;
            };
            case DIAMOND -> switch (type) {
                case PICKAXE -> Items.DIAMOND_PICKAXE;
                case SHOVEL -> Items.DIAMOND_SHOVEL;
                case AXE -> Items.DIAMOND_AXE;
            };
            case NETHERITE -> switch (type) {
                case PICKAXE -> Items.NETHERITE_PICKAXE;
                case SHOVEL -> Items.NETHERITE_SHOVEL;
                case AXE -> Items.NETHERITE_AXE;
            };
        };
    }
}
