package sh.sit.plp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import sh.sit.plp.PlayerLocatorPlus;
import sh.sit.plp.config.ConfigManager;
import sh.sit.plp.config.ModConfig;
import sh.sit.plp.network.ModConfigPayload;
import sh.sit.plp.network.PlayerLocationsPayload;
import sh.sit.plp.network.RelativePlayerLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public final class ClientState {
    private static final ResourceLocation EXPERIENCE_BAR_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/empty_bar");
    private static final ResourceLocation PLAYER_MARK_TEXTURE = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark");
    private static final ResourceLocation PLAYER_MARK_UP_TEXTURE = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_up");
    private static final ResourceLocation PLAYER_MARK_DOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "hud/player_mark_down");

    private static final int NAME_PLAQUE_PADDING_X = 4;
    private static final int NAME_PLAQUE_PADDING_Y = 2;
    private static final int NAME_PLAQUE_MARGIN = 2;
    private static final int NAME_PLAQUE_OVERLAP_THRESHOLD = 2;

    private static final ReentrantLock RELATIVE_POSITIONS_LOCK = new ReentrantLock();
    private static final Map<UUID, RelativePlayerLocation> RELATIVE_POSITIONS = new HashMap<>();
    private static Vec3 lastUpdatePosition = Vec3.ZERO;
    private static boolean initialized;

    private ClientState() {
    }

    public static void handlePlayerLocations(PlayerLocationsPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        RELATIVE_POSITIONS_LOCK.lock();
        try {
            if (payload.fullReset()) {
                RELATIVE_POSITIONS.clear();
            } else {
                for (UUID uuid : payload.removeUuids()) {
                    RELATIVE_POSITIONS.remove(uuid);
                }
            }

            for (RelativePlayerLocation update : payload.locationUpdates()) {
                RELATIVE_POSITIONS.put(update.playerUuid(), update);
            }

            LocalPlayer player = minecraft.player;
            lastUpdatePosition = player == null ? Vec3.ZERO : player.position();
            initialized = true;
        } finally {
            RELATIVE_POSITIONS_LOCK.unlock();
        }
    }

    public static void handleServerConfig(ModConfig config) {
        ConfigManager.setConfigOverride(config);
    }

    public static void clear() {
        RELATIVE_POSITIONS_LOCK.lock();
        try {
            RELATIVE_POSITIONS.clear();
            ConfigManager.setConfigOverride(null);
            initialized = false;
        } finally {
            RELATIVE_POSITIONS_LOCK.unlock();
        }
    }

    public static boolean isBarVisible() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null) return false;

        ModConfig config = ConfigManager.getConfig();
        if (!config.visible) return false;
        if (minecraft.options.hideGui) return false;

        if (!config.alwaysVisibleInSpectator && minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return false;
        }

        if (!config.visibleEmpty && RELATIVE_POSITIONS.isEmpty()) {
            return minecraft.getConnection() != null
                    && minecraft.getConnection().getOnlinePlayers().stream().anyMatch(info -> !info.getProfile().getId().equals(player.getUUID()));
        }

        return true;
    }

    public static void render(GuiGraphics graphics) {
        if (!initialized && RELATIVE_POSITIONS.isEmpty()) return;
        if (!isBarVisible()) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) return;

        ModConfig config = ConfigManager.getConfig();
        int barWidth = 182;
        int x = graphics.guiWidth() / 2 - 91;
        int y = graphics.guiHeight() - 32 + 3;

        graphics.blitSprite(EXPERIENCE_BAR_BACKGROUND_TEXTURE, x, y, barWidth, 5);

        boolean tabPressed = minecraft.options.keyPlayerList.isDown();
        List<NamePlaque> namePlaques = new ArrayList<>();

        RELATIVE_POSITIONS_LOCK.lock();
        try {
            for (RelativePlayerLocation position : RELATIVE_POSITIONS.values()) {
                Vec3 direction = resolveDirection(minecraft, player, position);
                double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
                if (!Double.isFinite(horizontalLength) || horizontalLength < 1.0E-8) continue;

                Vec3 view = player.getViewVector(1.0F);
                double viewLength = Math.sqrt(view.x * view.x + view.z * view.z);
                if (!Double.isFinite(viewLength) || viewLength < 1.0E-8) continue;

                double dirX = direction.x / horizontalLength;
                double dirZ = direction.z / horizontalLength;
                double viewX = view.x / viewLength;
                double viewZ = view.z / viewLength;
                double cross = viewX * dirZ - viewZ * dirX;
                double dot = viewX * dirX + viewZ * dirZ;
                double relativeAngle = -Math.atan2(cross, dot) * 180.0D / Math.PI;

                double horizontalFov = calculateHorizontalFov(minecraft.options.fov().get(), graphics.guiWidth(), graphics.guiHeight());
                double progress = (relativeAngle + horizontalFov / 2.0D) / horizontalFov;
                if (progress < 0.0D || progress > 1.0D) continue;

                int markX = x + Math.round((float) (progress * barWidth)) - 4;
                int opacity = markerOpacity(config, position.distance());
                int color = (opacity << 24) | (position.color() & 0xFFFFFF);

                String name = playerName(minecraft, position.playerUuid());
                if (name != null && config.showNamesOnTab && tabPressed) {
                    namePlaques.add(new NamePlaque(markX, name, progress));
                }

                drawColoredSprite(graphics, PLAYER_MARK_TEXTURE, markX, y - 1, 7, 7, color);

                if (config.showHeight) {
                    Vec3 normalized = direction.normalize();
                    if (normalized.y > 0.5D) {
                        graphics.blitSprite(PLAYER_MARK_UP_TEXTURE, markX + 1, y - 5, 5, 4);
                    } else if (normalized.y < -0.5D) {
                        graphics.blitSprite(PLAYER_MARK_DOWN_TEXTURE, markX + 1, y + 7, 5, 4);
                    }
                }
            }
        } finally {
            RELATIVE_POSITIONS_LOCK.unlock();
        }

        if (tabPressed && config.showNamesOnTab && !namePlaques.isEmpty()) {
            renderPlayerNamePlaques(graphics, namePlaques, y);
        }
    }

    private static Vec3 resolveDirection(Minecraft minecraft, LocalPlayer player, RelativePlayerLocation position) {
        Entity entity = minecraft.level == null ? null : minecraft.level.getPlayerByUUID(position.playerUuid());
        if (entity != null) {
            return entity.position().subtract(player.position());
        }

        Vector3f payloadDirection = position.direction();
        if (position.distance() == 0.0F) {
            return new Vec3(payloadDirection.x(), payloadDirection.y(), payloadDirection.z());
        }

        Vec3 projectedPosition = lastUpdatePosition.add(
                payloadDirection.x() * position.distance(),
                payloadDirection.y() * position.distance(),
                payloadDirection.z() * position.distance()
        );
        return projectedPosition.subtract(player.position());
    }

    private static int markerOpacity(ModConfig config, float distance) {
        if (!config.fadeMarkers) return 255;

        float clamped = Math.max(config.fadeStart, Math.min(config.fadeEnd, distance));
        float fadeProgress = 1.0F - (clamped - config.fadeStart) / (float) (config.fadeEnd - config.fadeStart);
        return Math.round(((1.0F - config.fadeEndOpacity) * fadeProgress + config.fadeEndOpacity) * 255.0F);
    }

    private static void drawColoredSprite(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height, int argb) {
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        graphics.setColor(red, green, blue, alpha);
        graphics.blitSprite(sprite, x, y, width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderPlayerNamePlaques(GuiGraphics graphics, List<NamePlaque> markers, int barY) {
        Font font = Minecraft.getInstance().font;
        markers.sort(Comparator.comparingDouble(marker -> Math.abs(marker.progress - 0.5D)));

        List<NamePlaqueRange> visibleMarkers = new ArrayList<>();
        for (NamePlaque marker : markers) {
            int textWidth = font.width(marker.playerName);
            int plaqueWidth = textWidth + NAME_PLAQUE_PADDING_X * 2;
            int plaqueX = marker.x - plaqueWidth / 2 + 4;
            int start = plaqueX;
            int end = plaqueX + plaqueWidth;

            boolean overlap = false;
            for (NamePlaqueRange visible : visibleMarkers) {
                if (visible.start - NAME_PLAQUE_OVERLAP_THRESHOLD <= end
                        && visible.end + NAME_PLAQUE_OVERLAP_THRESHOLD >= start) {
                    overlap = true;
                    break;
                }
            }

            if (!overlap) {
                visibleMarkers.add(new NamePlaqueRange(marker, start, end));
            }
        }

        for (NamePlaqueRange visible : visibleMarkers) {
            NamePlaque marker = visible.marker;
            int textWidth = font.width(marker.playerName);
            int plaqueWidth = textWidth + NAME_PLAQUE_PADDING_X * 2;
            int plaqueHeight = font.lineHeight + NAME_PLAQUE_PADDING_Y * 2;
            int plaqueX = marker.x - plaqueWidth / 2 + 4;
            int plaqueY = barY - plaqueHeight - NAME_PLAQUE_MARGIN;

            graphics.fill(plaqueX, plaqueY, plaqueX + plaqueWidth, plaqueY + plaqueHeight, 0xC0000000);
            graphics.drawString(font, marker.playerName, plaqueX + NAME_PLAQUE_PADDING_X, plaqueY + NAME_PLAQUE_PADDING_Y, 0xFFFFFFFF, false);
        }
    }

    private static String playerName(Minecraft minecraft, UUID uuid) {
        if (minecraft.getConnection() == null) return null;
        PlayerInfo info = minecraft.getConnection().getPlayerInfo(uuid);
        return info == null ? null : info.getProfile().getName();
    }

    private static double calculateHorizontalFov(int verticalFov, int width, int height) {
        double fovRad = verticalFov / 2.0D * Math.PI / 180.0D;
        double distance = height / 2.0D / Math.tan(fovRad);
        double horizontal = Math.atan(width / 2.0D / distance) * 2.0D;
        return horizontal / Math.PI * 180.0D;
    }

    private record NamePlaque(int x, String playerName, double progress) {
    }

    private record NamePlaqueRange(NamePlaque marker, int start, int end) {
    }
}
