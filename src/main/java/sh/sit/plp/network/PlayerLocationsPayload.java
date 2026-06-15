package sh.sit.plp.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import sh.sit.plp.PlayerLocatorPlus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PlayerLocationsPayload(
        List<RelativePlayerLocation> locationUpdates,
        List<UUID> removeUuids,
        boolean fullReset
) implements CustomPacketPayload {
    public static final Type<PlayerLocationsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "player_locations_v2")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLocationsPayload> STREAM_CODEC = StreamCodec.ofMember(
            PlayerLocationsPayload::write,
            PlayerLocationsPayload::read
    );

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(locationUpdates.size());
        for (RelativePlayerLocation location : locationUpdates) {
            location.write(buf);
        }

        buf.writeVarInt(removeUuids.size());
        for (UUID uuid : removeUuids) {
            buf.writeUUID(uuid);
        }

        buf.writeBoolean(fullReset);
    }

    private static PlayerLocationsPayload read(RegistryFriendlyByteBuf buf) {
        int updateCount = buf.readVarInt();
        List<RelativePlayerLocation> updates = new ArrayList<>(updateCount);
        for (int i = 0; i < updateCount; i++) {
            updates.add(RelativePlayerLocation.read(buf));
        }

        int removeCount = buf.readVarInt();
        List<UUID> removes = new ArrayList<>(removeCount);
        for (int i = 0; i < removeCount; i++) {
            removes.add(buf.readUUID());
        }

        return new PlayerLocationsPayload(updates, removes, buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
