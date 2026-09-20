package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.machine.MachineMicrowave;
import com.hbm.tileentity.machine.TileEntityMicrowave;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Microwave: body and window, and the turntable spinning at {@code speed / 10} degrees per millisecond
 * while something is cooking, as the original {@code RenderMicrowave} drew it, including its odd
 * {@code y - 0.785} base and {@code (-0.5, 0, 0.65)} model offset.
 */
public final class RenderMicrowave extends GemBER<TileEntityMicrowave, MicrowaveRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/microwave.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/microwave.png");

    @Override
    protected void setup(Gem.Model<MicrowaveRenderState> declaration) {
        Gem.Part<MicrowaveRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, -0.785F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(-0.5F, 0F, 0.65F);
        machine.child(model.part("mainbody_Cube.001"), surface);
        machine.child(model.part("window_Cube.002"), surface);
        machine.joint()
                .translate(0.575F, 0F, -0.45F)
                .angle(Gem.Axes.Y, state -> state.plate)
                .translate(-0.575F, 0F, 0.45F)
                .child(model.part("plate_Cylinder"), surface);
    }

    @Override
    public MicrowaveRenderState createRenderState() {
        return new MicrowaveRenderState();
    }

    /** Old meta yaw map: N(2)=0, W(4)=90, S(3)=180, E(5)=270. */
    @Override
    public void extract(TileEntityMicrowave be, MicrowaveRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        Direction facing = bs.hasProperty(MachineMicrowave.FACING) ? bs.getValue(MachineMicrowave.FACING) : Direction.NORTH;
        state.yaw = switch (facing) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        state.plate = be.time > 0 ? (float) ((System.currentTimeMillis() * be.speed / 10D) % 360) : 0F;
    }
}
