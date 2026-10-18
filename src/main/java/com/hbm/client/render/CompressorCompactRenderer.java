package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineCompressorCompact;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CompressorCompactRenderer
        extends GemBER<TileEntityMachineCompressorCompact, CompressorCompactRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/compressor_compact.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/compressor_compact.png");

    @Override
    protected void setup(Gem.Model<CompressorCompactRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .child(model.all(), surface);
    }

    @Override public CompressorCompactRenderState createRenderState() {
        return new CompressorCompactRenderState();
    }

    @Override
    public void extract(TileEntityMachineCompressorCompact be, CompressorCompactRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        ForgeDirection facing = ForgeDirection.getOrientation(meta);
        state.yaw = 90F + facing.getRotationDegrees();
    }
}
