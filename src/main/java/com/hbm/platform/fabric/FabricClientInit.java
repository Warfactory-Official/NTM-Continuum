package com.hbm.platform.fabric;

//? if fabric {

/*import com.hbm.client.HbmClientRegistrations;
import net.fabricmc.api.ClientModInitializer;

/^* Fabric-only client hooks; shared client setup is {@code HbmClient}. ^/
public class FabricClientInit implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                com.hbm.handler.JetpackHandler.clearClientState());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                com.hbm.client.gun.GunSmokeClient.onDisconnect());
        HbmClientRegistrations.installDebugLines();

        // Below 26.1: OBJ machine item models. 26.1 drives those through its special-model system
        // (registered by MixinSpecialModelRenderers); here the item definitions are parsed on reload and
        // every HBM item gets a dynamic renderer. Only items whose generated model parents onto
        // builtin/entity actually reach it, so registering broadly is free.
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

        net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet().stream()
                .filter(e -> e.getKey().location().getNamespace().equals(com.hbm.Constants.MOD_ID))
                .forEach(e -> net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE
                        .register(e.getValue(), (stack, mode, poseStack, buffers, light, overlay) ->
                                com.hbm.client.render.MachineItemRenderers.render(stack, mode, poseStack, buffers, light, overlay)));
        ^///?}
    }
}
*///?}
