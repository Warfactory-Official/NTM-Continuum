package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.MachineFan;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Six-directional OBJ declaration for the redstone fan.
 *
 * <p>The facing is a quarter turn about X or about Z and never about both, so both rotations are
 * declared and the one the facing does not use reads zero.
 */
public final class FanRenderer extends GemBER<MachineFan.TileEntityFan, FanRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/fan.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/fan.png");

    @Override
    protected void setup(Gem.Model<FanRenderState> declaration) {
        Gem.Part<FanRenderState> housing = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0.5F, 0.5F)
                .angle(Gem.Axes.X, state -> pitchOf(state.facing))
                .angle(Gem.Axes.Z, state -> rollOf(state.facing))
                .translate(0F, -0.5F, 0F);

        housing.child(model.part("Frame"), surface);
        housing.child(model.part("Blades"), surface)
                .angle(Gem.Axes.Y, state -> -state.rotation);
    }

    private static float pitchOf(Direction facing) {
        return switch (facing) {
            case DOWN -> 180F;
            case NORTH -> -90F;
            case SOUTH -> 90F;
            default -> 0F;
        };
    }

    private static float rollOf(Direction facing) {
        return switch (facing) {
            case WEST -> 90F;
            case EAST -> -90F;
            default -> 0F;
        };
    }

    @Override
    public FanRenderState createRenderState() {
        return new FanRenderState();
    }

    @Override
    public void extract(MachineFan.TileEntityFan be, FanRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        state.facing = blockState.hasProperty(MachineFan.FACING)
                ? blockState.getValue(MachineFan.FACING) : Direction.NORTH;
        state.rotation = be.prevSpin + (be.spin - be.prevSpin) * partialTicks;
    }
}
