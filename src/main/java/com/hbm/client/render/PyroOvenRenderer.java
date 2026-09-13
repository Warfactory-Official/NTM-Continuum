package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Pyro oven: the housing, the door sliding across it, and the extractor fan in the far corner.
 *
 */
public final class PyroOvenRenderer extends GemBER<TileEntityMachinePyroOven, PyroOvenRenderState> {

    private static final float BASE_YAW = 180F;

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/pyrooven.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/pyrooven.png");

    @Override
    protected void setup(Gem.Model<PyroOvenRenderState> declaration) {
        Gem.Part<PyroOvenRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Oven"), surface);
        machine.child(model.part("Slider"), surface)
                .slide(Gem.Axes.X, state -> (float) (sps(state.anim * 0.125D) / 2D - 0.5D));
        machine.child(model.part("Fan"), surface)
                .spin(Gem.Axes.Y, 1.5F, 0F, 1.5F, state -> (float) (state.anim * 45D % 360D));
    }

    /** The old {@code BobMathUtil.sps}: a sine of a cosine, not a plain sine wave. */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    @Override
    public PyroOvenRenderState createRenderState() {
        return new PyroOvenRenderState();
    }

    @Override
    public void extract(TileEntityMachinePyroOven be, PyroOvenRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = BASE_YAW + switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.anim = be.prevAnim + (be.anim - be.prevAnim) * partialTicks;
    }
}
