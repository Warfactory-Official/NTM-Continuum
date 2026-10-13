package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineRadarLarge;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderRadarLarge}: {@code T(0.5, 0, 0.5) Ry(180)}, the mast, then the dish turned by the interpolated
 * rotation about -Y, one skin, no culling. The model is symmetric about the core, so the facing is not read.
 */
public final class RenderRadarLarge extends GemBER<TileEntityMachineRadarLarge, RenderRadarLarge.State> {

    public static final class State extends CompatRenderState {
        public float rotation;
    }

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/radar_large.obj"));
    private final Surface skin = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/machines/radar_large.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> root = declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 180F);
        root.child(model.part("Radar"), skin);
        root.child(model.part("Dish"), skin)
                .angle(Gem.Axes.Y, state -> -state.rotation);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityMachineRadarLarge radar, State state, float partialTicks, Vec3 cameraPosition) {
        state.rotation = radar.prevRotation + (radar.rotation - radar.prevRotation) * partialTicks;
    }
}
