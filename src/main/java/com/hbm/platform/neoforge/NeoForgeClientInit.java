package com.hbm.platform.neoforge;

//? if neoforge {

import com.hbm.client.HbmClientRegistrations;
import com.hbm.packet.HbmPackets;
//? if >=26.1 {
import com.hbm.client.render.MachineItemModelRenderer;
//?}
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
//? if >=26.1 {
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
//?}
import net.minecraft.resources.Identifier;

/**
 * Client-only NeoForge setup. Isolated in its own class so it is never loaded on a dedicated server
 * (it references client-only screen/renderer classes). Wired from the mod constructor behind a
 * Dist.CLIENT guard.
 */
public final class NeoForgeClientInit {

    private NeoForgeClientInit() {}

    public static void init(IEventBus modBus) {
        HbmPackets.registerClientHandlers();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        com.hbm.handler.JetpackHandler.clearClientState());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        com.hbm.client.gun.GunSmokeClient.onDisconnect());
        // The vendored 26.1 engine is a library and must be subscribed from the constructor: two of
        // its listeners are mod-bus registry events. No-op where the engine is a mod, or absent.
        com.hbm.client.render.flywheel.FlywheelCompat.startEngine();
        com.hbm.client.particle.HbmParticleHandlers.register();
        com.hbm.client.particle.SingularityParticleHandlers.register();
        // Item tint colours (fluid icons, scraps, ...). 26.1 declares these in the item definitions and
        // the engine applies them; below it Golem Core wires the loader's colour registry to the same
        // stored colour. One call, every version, both loaders.
        com.hbm.client.ClientGameTime.install();
        HbmClientRegistrations.installDebugLines();
        HbmClientRegistrations.installTints();
        HbmClientRegistrations.installItemModelProperties();
        HbmClientRegistrations.installKeybinds();
        HbmClientRegistrations.installGunAnimations();
        HbmClientRegistrations.installEntityEffects();
        HbmClientRegistrations.installArmorLayers();

        // Look-overlay HUD (battery charge / duct type / ...) — a GUI layer drawn above the vanilla HUD,
        // so its text is extracted/submitted in the same pass (mirrors Fabric's HudElementRegistry).
        modBus.addListener((RegisterGuiLayersEvent event) -> {
            HbmClientRegistrations.registerHud((id, renderer) -> event.registerAboveAll(
                    dev.golem.core.Ids.of("hbm", id),
                    (graphics, deltaTracker) -> renderer.accept(graphics)));
        });

        modBus.addListener((RegisterMenuScreensEvent event) -> {
            HbmClientRegistrations.registerScreens(event::register);
        });

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
        // Block tint sources (sellafite shade tint) — Golem picks the era's colour API and the moment
        // this loader allows registering one.
        HbmClientRegistrations.registerBlockColors();

        // OBJ machine item models. 26.1 has a special-model system and registers a codec for them; below
        // it the equivalent hook is a BEWLR hung off the item's client extensions, fed by the same item
        // definitions parsed on resource reload.
        //? if >=26.1 {
        modBus.addListener((RegisterSpecialModelRendererEvent event) ->
                event.register(MachineItemModelRenderer.ID, MachineItemModelRenderer.Unbaked.MAP_CODEC));
        modBus.addListener((RegisterSpecialModelRendererEvent event) ->
                event.register(com.hbm.client.jetpack.JetpackGliderItemModelRenderer.ID,
                        com.hbm.client.jetpack.JetpackGliderItemModelRenderer.Unbaked.MAP_CODEC));
        modBus.addListener((RegisterSpecialModelRendererEvent event) ->
                event.register(com.hbm.client.render.RBMKColumnItemModelRenderer.ID,
                        com.hbm.client.render.RBMKColumnItemModelRenderer.Unbaked.MAP_CODEC));
        modBus.addListener((RegisterSpecialModelRendererEvent event) ->
                event.register(com.hbm.client.render.RBMKBlockColumnItemModelRenderer.ID,
                        com.hbm.client.render.RBMKBlockColumnItemModelRenderer.Unbaked.MAP_CODEC));
        // Guns are a whole item model rather than a special model: only ItemModel.update is handed the
        // display context, and a first-person weapon body is drawn in camera space, not in an item slot.
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) ->
                event.register(dev.golem.core.Ids.of("hbm", "gun"),
                        com.hbm.client.render.weapon.GunItemModel.Unbaked.MAP_CODEC));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) ->
                event.register(MachineItemModelRenderer.ID, MachineItemModelRenderer.Unbaked.MAP_CODEC));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) ->
                event.register(com.hbm.client.jetpack.JetpackGliderItemModelRenderer.ID,
                        com.hbm.client.jetpack.JetpackGliderItemModelRenderer.Unbaked.MAP_CODEC));
        //?} else {
        /*modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener(
                        (net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                                com.hbm.client.render.MachineItemModels::reload));

        modBus.addListener((net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) -> {
            var extension = new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
                private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;

                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        renderer = new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(
                                mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                            @Override
                            public void renderByItem(net.minecraft.world.item.ItemStack stack,
                                                     net.minecraft.world.item.ItemDisplayContext context,
                                                     com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                     net.minecraft.client.renderer.MultiBufferSource buffers,
                                                     int light, int overlay) {
                                com.hbm.client.render.MachineItemRenderers.render(stack, context, poseStack, buffers, light, overlay);
                            }
                        };
                    }
                    return renderer;
                }
            };
            // Registering for every HBM item is harmless: vanilla only consults the custom renderer for
            // items whose baked model reports isCustomRenderer(), i.e. those the build gave a
            // builtin/entity parent, which is exactly the machine_obj set.
            net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet().stream()
                    .filter(e -> e.getKey().location().getNamespace().equals(com.hbm.Constants.MOD_ID))
                    .forEach(e -> event.registerItem(extension, e.getValue()));
        });
        *///?}
    }
}
//?}
