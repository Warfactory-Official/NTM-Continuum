package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineCompressor;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Compressor renderer — port of the 1.12.2 {@code RenderCompressor}. Needs more than the generic
 * machine declaration because two parts move: the {@code Pump} translates by {@code lift·3 − 3} (so it
 * is <b>−3 blocks at rest</b> — a static render would drop it through the floor) and the {@code Fan}
 * spins about the X axis at y=1.5.
 *
 * <p>Old yaw map N90/S270/W180/E0 = the standard facing map (N0/S180/W90/E270) + 90.
 */
public class CompressorRenderer extends GemBER<TileEntityMachineCompressor, CompressorRenderState> {

    private final WavefrontObj model =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/compressor.obj"));
    private final Surface surface =
            Surface.cutout(Constants.MOD_ID + ":textures/models/machines/compressor.png");

    @Override
    protected void setup(Gem.Model<CompressorRenderState> declaration) {
        Gem.Part<CompressorRenderState> body = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        body.child(model.part("Compressor"), surface);
        body.child(model.part("Pump"), surface)
                .slide(Gem.Axes.Y, state -> state.lift * 3F - 3F);
        body.child(model.part("Fan"), surface)
                .spin(Gem.Axes.X, 0F, 1.5F, 0F, state -> state.fan);
    }

    @Override
    public CompressorRenderState createRenderState() {
        return new CompressorRenderState();
    }

    @Override
    public void extract(TileEntityMachineCompressor be, CompressorRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = 90F + switch (ForgeDirection.getOrientation(meta)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        state.lift = be.prevPiston + (be.piston - be.prevPiston) * partialTicks;
        state.fan = be.prevFanSpin + (be.fanSpin - be.prevFanSpin) * partialTicks;
    }
}
