package sh.sit.plp.network;

import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.UUID;

public record RelativePlayerLocation(UUID playerUuid, Vector3f direction, float distance, int color) {
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(playerUuid);
        buf.writeFloat(direction.x());
        buf.writeFloat(direction.y());
        buf.writeFloat(direction.z());
        buf.writeFloat(distance);
        buf.writeInt(color);
    }

    public static RelativePlayerLocation read(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        Vector3f direction = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        float distance = buf.readFloat();
        int color = buf.readInt();
        return new RelativePlayerLocation(uuid, direction, distance, color);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RelativePlayerLocation other)) return false;
        return Objects.equals(playerUuid, other.playerUuid)
                && Float.compare(direction.x(), other.direction.x()) == 0
                && Float.compare(direction.y(), other.direction.y()) == 0
                && Float.compare(direction.z(), other.direction.z()) == 0
                && Float.compare(distance, other.distance) == 0
                && color == other.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerUuid, direction.x(), direction.y(), direction.z(), distance, color);
    }
}
