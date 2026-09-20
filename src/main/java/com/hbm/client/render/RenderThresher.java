package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.machine.MachineThresher;
import com.hbm.tileentity.machine.TileEntityMachineThresher;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Thresher: base and jittering engine, then the upper arm, lower arm (folded back twice as far), the front
 * section and the spinning wheel, walked as the single un-pushed chain of the original
 * {@code RenderThresher.renderCommon}.
 */
public final class RenderThresher extends GemBER<TileEntityMachineThresher, ThresherRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/thresher.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/thresher.png");

    @Override
    protected void setup(Gem.Model<ThresherRenderState> declaration) {
        Gem.Part<ThresherRenderState> ground = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        ground.child(model.part("Base"), surface);
        ground.child(model.part("Engine"), surface)
                .slide(Gem.Axes.Y, state -> (float) (state.engine * 0.01D));

        Gem.Part<ThresherRenderState> upper = ground.joint()
                .translate(0F, 0.5F, -1F)
                .angle(Gem.Axes.X, state -> state.angle)
                .translate(0F, -0.5F, 1F);
        upper.child(model.part("ArmUpper"), surface);

        Gem.Part<ThresherRenderState> lower = upper.joint()
                .translate(0F, 0.5F, -5F)
                .angle(Gem.Axes.X, state -> state.angle * -2F)
                .translate(0F, -0.5F, 5F)
                .translate(-0.01F, 0F, 0F);
        lower.child(model.part("ArmLower"), surface);

        Gem.Part<ThresherRenderState> front = lower.joint()
                .translate(0.01F, 0F, 0F)
                .translate(0F, 0.5F, -9F)
                .angle(Gem.Axes.X, state -> state.angle)
                .translate(0F, -0.5F, 9F)
                .translate(0.01F, 0F, 0F);
        front.child(model.part("Front"), surface);

        front.joint()
                .translate(0F, 0.5F, -11F)
                .angle(Gem.Axes.X, state -> -state.spin)
                .translate(0F, -0.5F, 11F)
                .child(model.part("Wheel"), surface);
    }

    @Override
    public ThresherRenderState createRenderState() {
        return new ThresherRenderState();
    }

    /** Old meta yaw map: S(3)=180, E(5)=270, N(2)=0, W(4)=90. */
    @Override
    public void extract(TileEntityMachineThresher be, ThresherRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        Direction facing = bs.hasProperty(MachineThresher.FACING) ? bs.getValue(MachineThresher.FACING) : Direction.NORTH;
        state.yaw = switch (facing) {
            case SOUTH -> 180F;
            case EAST -> 270F;
            case WEST -> 90F;
            default -> 0F;
        };
        double angle = be.prevAngle + (be.angle - be.prevAngle) * partialTicks;
        state.angle = (float) (82.5D - angle);
        state.spin = be.lastSpin + (be.spin - be.lastSpin) * partialTicks;
        Level level = be.getLevel();
        state.engine = be.isOn && level != null ? Math.sin(level.getGameTime() * 2D % (Math.PI * 2D) + partialTicks) : 0D;
    }
}
