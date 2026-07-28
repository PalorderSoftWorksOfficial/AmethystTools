package com.rtc.amethystTools;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UpdateChecker {
    public final String currentVersion;
    public volatile String latestVersion;

    public UpdateChecker() {
        this.currentVersion = FabricLoader.getInstance()
                .getModContainer("amethysttools")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("1.4-fabric-1.21.11");
    }

    public void checkAsync(MinecraftServer server) {
        Thread thread = new Thread(() -> runCheck(server), "AmethystTools-UpdateChecker");
        thread.setDaemon(true);
        thread.start();
    }

    public void runCheck(MinecraftServer server) {
        try {
            URL url = new URL("https://api.modrinth.com/v2/project/YTkZHbOm/version");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String body = response.toString();
            int marker = body.indexOf("\"version_number\":\"");
            if (marker == -1) {
                return;
            }

            int start = marker + "\"version_number\":\"".length();
            int end = body.indexOf('"', start);
            if (end == -1) {
                return;
            }

            latestVersion = body.substring(start, end);

            if (isUpdateAvailable(currentVersion, latestVersion)) {
                server.getLogger().warning("There is a newer plugin version available: " + latestVersion + ", you're on: " + currentVersion);
                server.getLogger().warning("Go to its page to download: https://modrinth.com/plugin/amethystools/versions");
            }
        } catch (Exception exception) {
            server.getLogger().warning("An error occurred during the UpdateChecker check!");
        }
    }

    public boolean isUpdateAvailable(String current, String latest) {
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int cur = i < currentParts.length ? parsePart(currentParts[i]) : 0;
            int lat = i < latestParts.length ? parsePart(latestParts[i]) : 0;
            if (lat > cur) {
                return true;
            }
            if (lat < cur) {
                return false;
            }
        }
        return false;
    }

    private int parsePart(String part) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }
        return digits.length() == 0 ? 0 : Integer.parseInt(digits.toString());
    }
}
