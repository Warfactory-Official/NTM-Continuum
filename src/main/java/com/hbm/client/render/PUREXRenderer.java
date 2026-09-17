package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** PUREX plant: the base, the upper frame it only shows when built, the fan and the travelling pump. */
public class PUREXRenderer extends GemBER<TileEntityMachinePUREX, PurexRenderState> {

    private static final float BASE_YAW = 90F;

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/purex.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/purex.png");

    @Override
    protected void setup(Gem.Model<PurexRenderState> declaration) {
        Gem.Part<PurexRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw);

        machine.child(model.part("Base"), surface);
        machine.child(model.part("Frame"), surface)
                .visibleIf(state -> state.showFrame);
        machine.child(model.part("Fan"), surface)
                .spin(Gem.Axes.Z, 1.5F, 1.25F, 0F, state -> (float) ((state.anim * 45) % 360D));
        machine.child(model.part("Pump"), surface)
                .slide(Gem.Axes.X, state -> (float) (sps(state.anim * 0.25) * 0.5));
    }

    /** Soft-peak sine (1.12.2 {@code BobMathUtil.sps}), the pump's travel curve. */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    @Override
    public PurexRenderState createRenderState() {
        return new PurexRenderState();
    }

    @Override
    public void extract(TileEntityMachinePUREX be, PurexRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.showFrame = be.frame;
        state.anim = be.prevAnim + (be.anim - be.prevAnim) * partialTicks;
    }
}
