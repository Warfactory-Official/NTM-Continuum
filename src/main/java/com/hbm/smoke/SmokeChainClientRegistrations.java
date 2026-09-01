package com.hbm.smoke;

import com.hbm.client.HbmClientRegistrations;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.MachineObjRenderer;
import com.hbm.client.render.smoke.AshpitRenderer;
import com.hbm.inventory.gui.GUIAshpit;
import com.hbm.tileentity.machine.TileEntityChimneyBrick;
import com.hbm.tileentity.machine.TileEntityChimneyIndustrial;

/** Client-only registration handoff for this feature. */
public final class SmokeChainClientRegistrations {

    private SmokeChainClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(SmokeChainRegistration.ASHPIT_MENU.get(), GUIAshpit::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        sink.register(SmokeChainRegistration.CHIMNEY_BRICK.get(), ctx ->
                new MachineObjRenderer<TileEntityChimneyBrick>(0F, 1F,
                        new MachineObjRenderer.ModelDef(
                                dev.golem.core.Ids.of("hbm", "models/machines/chimney_brick.obj"),
                                dev.golem.core.Ids.of("hbm", "textures/models/machines/chimney_brick.png")))
                        .ignoreFacing().doubleSided());
        sink.register(SmokeChainRegistration.CHIMNEY_INDUSTRIAL.get(), ctx ->
                new MachineObjRenderer<TileEntityChimneyIndustrial>(180F, 1F,
                        new MachineObjRenderer.ModelDef(
                                dev.golem.core.Ids.of("hbm", "models/machines/chimney_industrial.obj"),
                                dev.golem.core.Ids.of("hbm", "textures/models/machines/chimney_industrial.png")))
                        .ignoreFacing().doubleSided());
        sink.register(SmokeChainRegistration.ASHPIT.get(), ctx -> new AshpitRenderer());
    }
}
