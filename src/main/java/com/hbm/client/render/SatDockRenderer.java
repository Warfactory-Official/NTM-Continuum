package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineSatDock;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderDecoBlock} for {@code sat_dock}: {@code T(0.5, 1.5, 0.5) Rz(180)}, then the pad's own
 * {@code Rz(180) T(0, -1.5, 0)}, which cancel to {@code T(0.5, 0, 0.5)}; {@code satDock.renderAll()} lit, culled.
 */
public final class SatDockRenderer extends GemBER<TileEntityMachineSatDock, CompatRenderState> {

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/sat_dock.obj"));
    private final Surface skin = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/sat_dock.png");

    @Override
    protected void setup(Gem.Model<CompatRenderState> declaration) {
        declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .child(model.all(), skin);
    }

    @Override
    public CompatRenderState createRenderState() {
        return new CompatRenderState();
    }

    @Override
    public void extract(TileEntityMachineSatDock dock, CompatRenderState state, float partialTicks, Vec3 cameraPosition) {
    }
}
