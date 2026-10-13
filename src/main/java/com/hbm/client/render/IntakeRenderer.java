package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.IntakeFanDirections;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** Animated OBJ declaration for the two-by-two legacy air intake. */
public final class IntakeRenderer extends GemBER<TileEntityMachineIntake, IntakeRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/intake.obj"));
    private final Surface surface = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/intake.png");

    @Override
    protected void setup(Gem.Model<IntakeRenderState> declaration) {
        Gem.Part<IntakeRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(-0.5F, 0F, 0.5F);

        machine.child(model.part("Base"), surface);
        machine.child(model.part("Fan"), surface)
                .angle(Gem.Axes.Y, state -> -state.rotation);
    }

    @Override
    public IntakeRenderState createRenderState() {
        return new IntakeRenderState();
    }

    @Override
    public void extract(TileEntityMachineIntake be, IntakeRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        state.yaw = IntakeFanDirections.intakeYaw(be.getFacing());
        state.rotation = be.prevFan + (be.fan - be.prevFan) * partialTicks;
    }
}
