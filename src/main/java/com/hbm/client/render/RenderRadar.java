package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderRadar}: {@code T(0.5, 0, 0.5) Ry(180)}, the base, then the dish turned by the interpolated
 * rotation about -Y and shifted {@code -0.125} on X, each in its own skin, drawn without culling.
 */
public final class RenderRadar extends GemBER<TileEntityMachineRadarNT, RenderRadar.State> {

    public static final class State extends CompatRenderState {
        public float rotation;
    }

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/radar.obj"));
    private final Surface base = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/machines/radar_base.png");
    private final Surface dish = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/machines/radar_dish.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> root = declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 180F);
        root.child(model.part("Base"), base);
        root.child(model.part("Dish"), dish)
                .angle(Gem.Axes.Y, state -> -state.rotation)
                .translate(-0.125F, 0F, 0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityMachineRadarNT radar, State state, float partialTicks, Vec3 cameraPosition) {
        state.rotation = radar.prevRotation + (radar.rotation - radar.prevRotation) * partialTicks;
    }
}
