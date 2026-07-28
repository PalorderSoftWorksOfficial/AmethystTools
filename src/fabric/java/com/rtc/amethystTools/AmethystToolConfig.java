package com.rtc.amethystTools;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class AmethystToolConfig {
    private final Path path;
    private final Properties properties = new Properties();

    public AmethystToolConfig(String fileName) {
        this.path = FabricLoader.getInstance().getConfigDir().resolve(fileName);
    }

    public synchronized void load() {
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                copyDefaultFile();
            }
            properties.clear();
            try (InputStream inputStream = Files.newInputStream(path)) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public synchronized String get(String key, String fallback) {
        return properties.getProperty(key, fallback);
    }

    public synchronized void save() {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            properties.store(outputStream, "AmethystTools");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void copyDefaultFile() throws IOException {
        try (InputStream inputStream = AmethystToolConfig.class.getClassLoader().getResourceAsStream("amethysttools.properties")) {
            if (inputStream == null) {
                return;
            }
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
