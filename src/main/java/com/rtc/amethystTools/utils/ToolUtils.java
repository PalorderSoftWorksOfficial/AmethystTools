package com.rtc.amethystTools.utils;

import org.bukkit.Material;

import java.util.Locale;

@SuppressWarnings("ALL")
public class ToolUtils {

    private ToolUtils() {
    }

    public static String getTierName(String tier) {
        return switch (tier) {
            case "wooden" -> "Wooden";
            case "stone" -> "Stone";
            case "iron" -> "Iron";
            case "copper" -> "Copper";
            case "diamond" -> "Diamond";
            case "netherite" -> "Netherite";
            case "golden" -> "Gold";
            default -> "(Error Material)";
        };
    }

    public static String getTypeName(String type) {
        return switch (type) {
            case "pickaxe" -> "Pickaxe";
            case "axe" -> "Axe";
            case "shovel" -> "Shovel";
            default -> "(Error Tool)";
        };
    }

    public static Material getMaterial(String tier, String type) {

        String materialName = tier.toUpperCase(Locale.ENGLISH) + "_" + type.toUpperCase(Locale.ENGLISH);

        return Material.matchMaterial(materialName);
    }

    public static boolean hasCopperTools() {
        return Material.matchMaterial("COPPER_PICKAXE") != null;
    }
}