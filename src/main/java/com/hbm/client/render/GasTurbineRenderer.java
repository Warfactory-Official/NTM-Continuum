package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The gas turbine stands still; only its facing is read. */
public final class GasTurbineRenderer extends GemBER<TileEntityMachineTurbineGas, GasTurbineRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/turbinegas.obj"));
    private final Surface surface = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/turbinegas.png");

    @Override
    protected void setup(Gem.Model<GasTurbineRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(model.all(true), surface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
    }

    @Override
    public GasTurbineRenderState createRenderState() {
        return new GasTurbineRenderState();
    }

    @Override
    public void extract(TileEntityMachineTurbineGas be, GasTurbineRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = 90F + switch (ForgeDirection.getOrientation(meta)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
    }
}
