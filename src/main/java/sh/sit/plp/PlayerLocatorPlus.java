package sh.sit.plp;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.sit.plp.client.ClientEvents;
import sh.sit.plp.client.ClientState;
import sh.sit.plp.color.PLPCommand;
import sh.sit.plp.config.ConfigManager;
import sh.sit.plp.network.ModConfigPayload;
import sh.sit.plp.network.PlayerLocationsPayload;

@Mod(PlayerLocatorPlus.MOD_ID)
public class PlayerLocatorPlus {
    public static final String MOD_ID = "player_locator_plus";
    public static final String RESOURCE_NAMESPACE = "player-locator-plus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final TagKey<Item> HIDING_EQUIPMENT_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(RESOURCE_NAMESPACE, "hiding_equipment")
    );

    private int tickCounter;

    public PlayerLocatorPlus(IEventBus modEventBus) {
        ConfigManager.init();

        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEvents.init();
        }

        LOGGER.info("NeoPlayer Locator Plus initialized for NeoForge");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(PlayerLocationsPayload.TYPE, PlayerLocationsPayload.STREAM_CODEC, this::handlePlayerLocations);
        registrar.playToClient(ModConfigPayload.TYPE, ModConfigPayload.STREAM_CODEC, this::handleModConfig);
    }

    private void handlePlayerLocations(PlayerLocationsPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND && FMLEnvironment.dist == Dist.CLIENT) {
            ClientState.handlePlayerLocations(payload);
        }
    }

    private void handleModConfig(ModConfigPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND && FMLEnvironment.dist == Dist.CLIENT) {
            ClientState.handleServerConfig(payload.config());
        }
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            BarUpdater.fullResend(player);
            ConfigManager.sendConfig(player);
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        if (tickCounter < ConfigManager.getConfig().ticksBetweenUpdates) {
            tickCounter++;
            return;
        }

        tickCounter = 0;
        BarUpdater.update(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        BarUpdater.reset();
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PLPCommand.register(event.getDispatcher());
    }
}
