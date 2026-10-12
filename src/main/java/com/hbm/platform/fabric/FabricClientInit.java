package com.hbm.platform.fabric;

//? if fabric {

/*import com.hbm.client.HbmClientRegistrations;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.EntityPropsSyncPayload;
import com.hbm.packet.HbmPackets;
import dev.golem.core.net.GolemNetwork;
import com.hbm.hazard.HazardSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/^* Fabric client entrypoint: registers HBM menu screens, BlockEntity renderers and packet receivers. ^/
public class FabricClientInit implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HbmPackets.registerClientHandlers();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                com.hbm.handler.JetpackHandler.clearClientState());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                com.hbm.client.gun.GunSmokeClient.onDisconnect());
        // The vendored 26.1 engine is a library and has to be handed its listeners; nowhere later
        // will do, since some of them have to be in place before the first level arrives. No-op
        // where the engine is a mod that subscribes itself, or absent.
        com.hbm.client.render.flywheel.FlywheelCompat.startEngine();
        com.hbm.client.particle.HbmParticleHandlers.register();
        com.hbm.client.particle.SingularityParticleHandlers.register();
        GolemNetwork.flushClientRegistrations();

        // Item tint colours (fluid icons, scraps, ...). 26.1 declares these in the item definitions and
        // the engine applies them; below it Golem Core wires the loader's colour registry to the same
        // stored colour. One call, every version, both loaders.
        com.hbm.client.ClientGameTime.install();
        // The material-shape sprites are computed onto the atlas, not shipped; this has to be declared
        // before the first resource load. The fluid droplets are the same, one per registered fluid.
        com.hbm.client.AutogenSprites.install();
        com.hbm.client.FluidSprites.install();
        HbmClientRegistrations.installDebugLines();
        HbmClientRegistrations.installTints();
        HbmClientRegistrations.installItemModelProperties();
        HbmClientRegistrations.installKeybinds();
        HbmClientRegistrations.installGunAnimations();
        HbmClientRegistrations.installEntityEffects();
        HbmClientRegistrations.installArmorLayers();

        // Look-overlay HUD (battery charge / duct type / ...) — drawn after the vanilla HUD each frame.
        HbmClientRegistrations.registerHud((id, renderer) ->
                dev.golem.core.client.Hud.addLast("hbm", id, renderer::accept));

        HbmClientRegistrations.installScreensAndRenderers();
        // 26.1: block tint sources (sellafite shade tint) — fabric-api has no colour registry there, so
        // register directly on the vanilla BlockColors once the client instance is fully constructed.
        // Below 26.1: OBJ machine item models. 26.1 drives those through its special-model system
        // (registered by MixinSpecialModelRenderers); here the item definitions are parsed on reload and
        // every item of a MachineItemNamespaces namespace gets a dynamic renderer. Only items whose generated model parents onto
        // builtin/entity actually reach it, so registering broadly is free.
        HbmClientRegistrations.registerBlockColors();
//? if <26.1 {

        /^net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                    @Override
                    public net.minecraft.resources.Identifier getFabricId() {
                        return dev.golem.core.Ids.of(
                                com.hbm.Constants.MOD_ID, "machine_item_models");
                    }

                    @Override
                    public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager resources) {
                        com.hbm.client.render.MachineItemModels.reload(resources);
                    }
                });

        // HBM's namespace and every addon namespace listed in MachineItemNamespaces (bound as it is added)
        com.hbm.client.render.MachineItemNamespaces.bind(namespace ->
                net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet().stream()
                        .filter(e -> e.getKey().location().getNamespace().equals(namespace))
                        .forEach(e -> net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE
                                .register(e.getValue(), (stack, mode, poseStack, buffers, light, overlay) ->
                                        com.hbm.client.render.MachineItemRenderers.render(stack, mode, poseStack, buffers, light, overlay))));
        ^///?}

        // Hazard tooltips (radioactive/hot/etc. traits on items).
        dev.golem.core.client.Tooltips.onItemTooltip((stack, lines) ->
                HazardSystem.addHazardInfo(stack, net.minecraft.client.Minecraft.getInstance().player, lines));
    }
}
*///?}
