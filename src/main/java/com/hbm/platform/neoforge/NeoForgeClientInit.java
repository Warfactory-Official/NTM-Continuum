package com.hbm.platform.neoforge;

//? if neoforge {

import com.hbm.client.HbmClientRegistrations;
//? if >=26.1 {
import com.hbm.client.render.MachineItemModelRenderer;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
//?}
import net.neoforged.bus.api.IEventBus;

/** NeoForge-only client hooks; shared client setup is {@code HbmClient}. Never loaded on a dedicated server. */
public final class NeoForgeClientInit {

    private NeoForgeClientInit() {}

    public static void init(IEventBus modBus) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        com.hbm.handler.JetpackHandler.clearClientState());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        com.hbm.client.gun.GunSmokeClient.onDisconnect());
        HbmClientRegistrations.installDebugLines();

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
