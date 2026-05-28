package com.rtc.amethystTools.utils;

import org.bukkit.Bukkit;

public class VersionUtil {

    public static String getVersion() {
        return Bukkit.getMinecraftVersion();
    }

    public static boolean isAtLeast(String version) {
        return compare(getVersion(), version) >= 0;
    }

    private static int compare(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");

        int length = Math.max(a.length, b.length);

        for (int i = 0; i < length; i++) {

            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;

            if (n1 != n2) return n1 - n2;
        }

        return 0;
    }
}