package com.hbm.platform.forge;

//? if forge {

/*import com.hbm.client.HbmClientRegistrations;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.EntityPropsSyncPayload;
import com.hbm.packet.HbmPackets;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/^* Client bootstrap, isolated from {@link ForgeInit} so dedicated servers never load client classes. ^/
final class ForgeClientInit {

    private ForgeClientInit() {}

    static void init(IEventBus modBus) {
        HbmPackets.registerClientHandlers();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        com.hbm.handler.JetpackHandler.clearClientState());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        com.hbm.client.gun.GunSmokeClient.onDisconnect());
        com.hbm.client.particle.HbmParticleHandlers.register();
        com.hbm.client.particle.SingularityParticleHandlers.register();
        GolemNetwork.setClientContextSupplier(() -> new GolemNetwork.ClientContext(
                Minecraft.getInstance().level, Minecraft.getInstance().player));
        GolemNetwork.flushClientRegistrations();

        com.hbm.client.ClientGameTime.install();
        HbmClientRegistrations.installDebugLines();
        HbmClientRegistrations.installTints();
        HbmClientRegistrations.installItemModelProperties();
        HbmClientRegistrations.installKeybinds();
        HbmClientRegistrations.installGunAnimations();
        HbmClientRegistrations.installArmorLayers();
        dev.golem.core.render.ForgeItemRenderers.install(com.hbm.client.render.MachineItemRenderers::render);
        modBus.addListener((net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener(
                        (net.minecraft.server.packs.resources.ResourceManagerReloadListener) resources -> {
                            com.hbm.client.render.MachineItemModels.reload(resources);
                        }));
        HbmClientRegistrations.registerBlockColors();
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            HbmClientRegistrations.registerScreens(MenuScreens::register);
        }));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            HbmClientRegistrations.registerRenderers(new com.hbm.client.render.HbmBlockEntityRenderers.Sink() {
                @Override
                public <T extends BlockEntity, S extends dev.golem.core.render.CompatRenderState> void register(
                        BlockEntityType<? extends T> type, dev.golem.core.render.CompatBERProvider<T, S> provider) {
                    event.registerBlockEntityRenderer(type, dev.golem.core.render.Renderers.toVanilla(provider));
                }
            }, new com.hbm.client.render.HbmEntityRenderers.Sink() {
                @Override
                public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                    event.registerEntityRenderer(type, provider);
                }
            });
        });
        modBus.addListener((RegisterGuiOverlaysEvent event) -> {
            HbmClientRegistrations.registerHud((id, renderer) ->
                    event.registerAboveAll(id, (gui, graphics, partialTick, width, height) -> renderer.accept(graphics)));
        });
    }
}
*///?}
