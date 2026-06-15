package sh.sit.plp.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import sh.sit.plp.PlayerLocatorPlus;
import sh.sit.plp.config.ModConfig;

public record ModConfigPayload(ModConfig config) implements CustomPacketPayload {
    public static final Type<ModConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlayerLocatorPlus.RESOURCE_NAMESPACE, "mod_config")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ModConfigPayload> STREAM_CODEC = StreamCodec.ofMember(
            ModConfigPayload::write,
            ModConfigPayload::read
    );

    private void write(RegistryFriendlyByteBuf buf) {
        config.write(buf);
    }

    private static ModConfigPayload read(RegistryFriendlyByteBuf buf) {
        return new ModConfigPayload(ModConfig.read(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
