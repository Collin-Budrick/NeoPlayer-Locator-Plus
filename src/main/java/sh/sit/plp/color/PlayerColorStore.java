package sh.sit.plp.color;

import net.neoforged.fml.loading.FMLPaths;
import sh.sit.plp.PlayerLocatorPlus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public final class PlayerColorStore {
    private static final Path COLORS_PATH = FMLPaths.CONFIGDIR.get().resolve("player-locator-plus-player-colors.properties");
    private static final Map<UUID, Integer> COLORS = new HashMap<>();
    private static boolean loaded;

    private PlayerColorStore() {
    }

    public static synchronized int get(UUID uuid) {
        ensureLoaded();
        return COLORS.getOrDefault(uuid, 0xFFFFFF);
    }

    public static synchronized void set(UUID uuid, int color) {
        ensureLoaded();
        COLORS.put(uuid, color & 0xFFFFFF);
        save();
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;

        if (!Files.exists(COLORS_PATH)) return;

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(COLORS_PATH)) {
            properties.load(input);
        } catch (IOException e) {
            PlayerLocatorPlus.LOGGER.warn("Failed to load {}", COLORS_PATH, e);
            return;
        }

        for (String key : properties.stringPropertyNames()) {
            try {
                UUID uuid = UUID.fromString(key);
                String value = properties.getProperty(key).trim();
                if (value.startsWith("#")) value = value.substring(1);
                COLORS.put(uuid, Integer.parseInt(value, 16) & 0xFFFFFF);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void save() {
        Properties properties = new Properties();
        for (Map.Entry<UUID, Integer> entry : COLORS.entrySet()) {
            properties.setProperty(entry.getKey().toString(), String.format("#%06X", entry.getValue() & 0xFFFFFF));
        }

        try {
            Files.createDirectories(COLORS_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(COLORS_PATH)) {
                properties.store(output, "NeoPlayer Locator Plus custom player colors");
            }
        } catch (IOException e) {
            PlayerLocatorPlus.LOGGER.warn("Failed to save {}", COLORS_PATH, e);
        }
    }
}
