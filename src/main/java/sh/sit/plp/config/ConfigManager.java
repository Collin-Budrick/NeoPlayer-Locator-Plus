package sh.sit.plp.config;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import sh.sit.plp.PlayerLocatorPlus;
import sh.sit.plp.network.ModConfigPayload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigManager {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("player-locator-plus.properties");
    private static ModConfig config = new ModConfig();
    private static ModConfig configOverride;

    private ConfigManager() {
    }

    public static void init() {
        reload(true);
    }

    public static synchronized void reload(boolean fromDisk) {
        if (fromDisk) {
            ModConfig loaded = new ModConfig();
            if (Files.exists(CONFIG_PATH)) {
                Properties properties = new Properties();
                try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
                    properties.load(input);
                    loaded.load(properties);
                } catch (IOException e) {
                    PlayerLocatorPlus.LOGGER.warn("Failed to load {}", CONFIG_PATH, e);
                }
            }
            config = loaded;
        }

        save();
        PlayerLocatorPlus.LOGGER.info("Player Locator Plus config loaded from {}", CONFIG_PATH);
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                config.save().store(output, "NeoPlayer Locator Plus configuration");
            }
        } catch (IOException e) {
            PlayerLocatorPlus.LOGGER.warn("Failed to save {}", CONFIG_PATH, e);
        }
    }

    public static synchronized ModConfig getConfig() {
        if (configOverride != null && config.acceptServerConfig) {
            return configOverride;
        }
        return config;
    }

    public static synchronized ModConfig getLocalConfig() {
        return config;
    }

    public static synchronized void setConfigOverride(ModConfig override) {
        configOverride = override;
    }

    public static void sendConfig(ServerPlayer player) {
        ModConfig current = getLocalConfig();
        if (current.sendServerConfig) {
            PacketDistributor.sendToPlayer(player, new ModConfigPayload(current.copy()));
        }
    }
}
