package sh.sit.plp.client;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class ClientEvents {
    private static boolean initialized;

    private ClientEvents() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        NeoForge.EVENT_BUS.addListener(ClientEvents::onRenderGui);
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        ClientState.render(event.getGuiGraphics());
    }
}
