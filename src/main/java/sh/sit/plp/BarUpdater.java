package sh.sit.plp;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import sh.sit.plp.color.ColorUtils;
import sh.sit.plp.color.PlayerColorStore;
import sh.sit.plp.config.ConfigManager;
import sh.sit.plp.config.ModConfig;
import sh.sit.plp.network.PlayerLocationsPayload;
import sh.sit.plp.network.RelativePlayerLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class BarUpdater {
    private static Map<UUID, StoredPlayerPosition> previousPositions = Map.of();

    private BarUpdater() {
    }

    public static void reset() {
        previousPositions = Map.of();
    }

    public static void fullResend(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            fullResend(player);
        }
    }

    public static void fullResend(ServerPlayer player) {
        ModConfig config = ConfigManager.getConfig();
        List<RelativePlayerLocation> relativePositions = new ArrayList<>();
        StoredPlayerPosition self = new StoredPlayerPosition(player);

        for (ServerPlayer other : player.serverLevel().players()) {
            if (other == player) continue;

            float distance = (float) player.position().distanceTo(other.position());
            if (config.maxDistance != 0 && distance > config.maxDistance) continue;

            relativePositions.add(calculateRelativeLocation(other.getUUID(), self, new StoredPlayerPosition(other)));
        }

        PacketDistributor.sendToPlayer(player, new PlayerLocationsPayload(
                config.enabled ? relativePositions : Collections.emptyList(),
                Collections.emptyList(),
                true
        ));
    }

    public static void update(MinecraftServer server) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.enabled) return;

        Map<UUID, StoredPlayerPosition> currentPositions = getPositions(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            StoredPlayerPosition previousPlayer = previousPositions.get(player.getUUID());
            int maxDistance = config.maxDistance;

            Set<UUID> removeUuids = new HashSet<>();
            for (Map.Entry<UUID, StoredPlayerPosition> entry : previousPositions.entrySet()) {
                UUID uuid = entry.getKey();
                StoredPlayerPosition prevPos = entry.getValue();
                if (uuid.equals(player.getUUID())) continue;

                StoredPlayerPosition curPos = currentPositions.get(uuid);

                if (prevPos.level != (curPos == null ? null : curPos.level) && player.level() == prevPos.level) {
                    removeUuids.add(uuid);
                }

                if (previousPlayer != null && previousPlayer.level != player.level()
                        && curPos != null && previousPlayer.level == curPos.level) {
                    removeUuids.add(uuid);
                }

                if (curPos != null && previousPlayer != null && curPos.level == player.level()
                        && curPos.level == prevPos.level && maxDistance != 0) {
                    double previousDistance = previousPlayer.pos.distanceTo(prevPos.pos);
                    double currentDistance = player.position().distanceTo(curPos.pos);
                    if (currentDistance > maxDistance && previousDistance <= maxDistance) {
                        removeUuids.add(uuid);
                    }
                }
            }

            List<RelativePlayerLocation> updatedPositions = new ArrayList<>();
            StoredPlayerPosition currentPlayer = new StoredPlayerPosition(player);

            for (Map.Entry<UUID, StoredPlayerPosition> entry : currentPositions.entrySet()) {
                UUID uuid = entry.getKey();
                StoredPlayerPosition curPos = entry.getValue();
                if (uuid.equals(player.getUUID())) continue;
                if (curPos.level != player.level()) continue;

                RelativePlayerLocation previousRelativeLocation = null;
                StoredPlayerPosition prevPos = previousPositions.get(uuid);
                if (prevPos != null && previousPlayer != null) {
                    previousRelativeLocation = calculateRelativeLocation(uuid, previousPlayer, prevPos);
                }

                RelativePlayerLocation currentRelativeLocation = calculateRelativeLocation(uuid, currentPlayer, curPos);
                if (currentRelativeLocation.equals(previousRelativeLocation)) continue;

                double currentDistance = player.position().distanceTo(curPos.pos);
                if (maxDistance != 0 && currentDistance > maxDistance) continue;

                updatedPositions.add(currentRelativeLocation);
            }

            boolean fullReset = previousPlayer != null && previousPlayer.level != player.level();
            if (updatedPositions.isEmpty() && removeUuids.isEmpty()) continue;

            PacketDistributor.sendToPlayer(player, new PlayerLocationsPayload(
                    updatedPositions,
                    new ArrayList<>(removeUuids),
                    fullReset
            ));
        }

        previousPositions = currentPositions;
    }

    public static void sendFakePlayers(ServerPlayer player) {
        ModConfig config = ConfigManager.getConfig();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<RelativePlayerLocation> positions = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Vector3f direction = new Vector3f(random.nextFloat(), random.nextFloat() * 0.75F, random.nextFloat()).normalize();
            positions.add(new RelativePlayerLocation(
                    UUID.randomUUID(),
                    direction,
                    config.sendDistance ? random.nextFloat() * 750.0F : 0.0F,
                    ColorUtils.uuidToColor(UUID.randomUUID())
            ));
        }

        PacketDistributor.sendToPlayer(player, new PlayerLocationsPayload(positions, Collections.emptyList(), false));
    }

    private static Map<UUID, StoredPlayerPosition> getPositions(MinecraftServer server) {
        Map<UUID, StoredPlayerPosition> positions = new HashMap<>();
        ModConfig config = ConfigManager.getConfig();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if ((config.sneakingHides && player.isCrouching())
                    || (config.pumpkinHides && player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN))
                    || (config.mobHeadsHide && player.getItemBySlot(EquipmentSlot.HEAD).is(PlayerLocatorPlus.HIDING_EQUIPMENT_TAG))
                    || (config.invisibilityHides && player.hasEffect(MobEffects.INVISIBILITY))
                    || player.isSpectator()) {
                continue;
            }

            positions.put(player.getUUID(), new StoredPlayerPosition(player));
        }

        return positions;
    }

    private static RelativePlayerLocation calculateRelativeLocation(UUID uuid, StoredPlayerPosition selfPos, StoredPlayerPosition otherPos) {
        ModConfig config = ConfigManager.getConfig();
        Vec3 delta = otherPos.pos.subtract(selfPos.pos);
        Vector3f direction;
        if (delta.lengthSqr() < 1.0E-8) {
            direction = new Vector3f();
        } else {
            direction = new Vector3f((float) delta.x, (float) delta.y, (float) delta.z).normalize();
        }

        direction.x = Math.round(direction.x * config.directionPrecision) / config.directionPrecision;
        direction.y = Math.round(direction.y * config.directionPrecision) / config.directionPrecision;
        direction.z = Math.round(direction.z * config.directionPrecision) / config.directionPrecision;

        float distance = 0.0F;
        if (config.sendDistance) {
            distance = (float) selfPos.pos.distanceTo(otherPos.pos);
            if (distance >= 200.0F) {
                distance = Math.round(distance / 50.0F) * 50.0F;
            }
        }

        return new RelativePlayerLocation(uuid, direction, distance, otherPos.color);
    }

    private record StoredPlayerPosition(Vec3 pos, Level level, int color) {
        private StoredPlayerPosition(ServerPlayer player) {
            this(player.position(), player.level(), calculateColor(player));
        }

        private static int calculateColor(ServerPlayer player) {
            ModConfig config = ConfigManager.getConfig();
            return switch (config.colorMode) {
                case UUID -> ColorUtils.uuidToColor(player.getUUID());
                case TEAM_COLOR -> player.getTeamColor() & 0xFFFFFF;
                case CONSTANT -> config.constantColor & 0xFFFFFF;
                case CUSTOM -> PlayerColorStore.get(player.getUUID());
            };
        }
    }
}
