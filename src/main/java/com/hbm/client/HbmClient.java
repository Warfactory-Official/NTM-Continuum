package com.hbm.client;

import com.hbm.hazard.HazardSystem;
import com.hbm.packet.HbmPackets;
import dev.golem.core.client.ClientRegistry;
import dev.golem.core.client.Hud;
import dev.golem.core.client.Tooltips;

/** Client setup shared by every loader. */
public final class HbmClient {

    private HbmClient() {}

    public static void init() {
        HbmPackets.registerClientHandlers();
        com.hbm.client.render.flywheel.FlywheelCompat.startEngine();
        com.hbm.client.particle.HbmParticleHandlers.register();
        com.hbm.client.particle.SingularityParticleHandlers.register();
        ClientGameTime.install();
        AutogenSprites.install();
        FluidSprites.install();
        HbmClientRegistrations.installTints();
        HbmClientRegistrations.installItemModelProperties();
        HbmClientRegistrations.installKeybinds();
        HbmClientRegistrations.installGunAnimations();
        HbmClientRegistrations.installEntityEffects();
        HbmClientRegistrations.installArmorLayers();
        ClientRegistry.screens(HbmClientRegistrations::registerScreens);
        ClientRegistry.renderers(sink -> HbmClientRegistrations.registerRenderers(sink::blockEntity, sink::entity));
        HbmClientRegistrations.registerBlockColors();
        Hud.layers(hud -> HbmClientRegistrations.registerHud((id, renderer) -> hud.addLast(com.hbm.Constants.MOD_ID, id, renderer)));
        Tooltips.onItemTooltip((stack, lines) ->
                HazardSystem.addHazardInfo(stack, net.minecraft.client.Minecraft.getInstance().player, lines));
    }
}
