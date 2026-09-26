package com.hbm.platform.forge;

//? if forge {

/*import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.IEventBus;

/^* Forge-only client hooks; shared client setup is {@code HbmClient}. Never loaded on a dedicated server. ^/
final class ForgeClientInit {

    private ForgeClientInit() {}

    static void init(IEventBus modBus) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        com.hbm.handler.JetpackHandler.clearClientState());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        com.hbm.client.gun.GunSmokeClient.onDisconnect());
        GolemNetwork.setClientContextSupplier(() -> new GolemNetwork.ClientContext(
                Minecraft.getInstance().level, Minecraft.getInstance().player));
        dev.golem.core.render.ForgeItemRenderers.install(com.hbm.client.render.MachineItemRenderers::render);
        modBus.addListener((net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener(
                        (net.minecraft.server.packs.resources.ResourceManagerReloadListener) resources -> {
                            com.hbm.client.render.MachineItemModels.reload(resources);
                        }));
    }
}
*///?}
