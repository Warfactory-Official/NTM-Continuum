package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Industrial turbine: a static housing, the steam-type gauge needle and the spinning flywheel. */
public final class IndustrialTurbineRenderer
        extends GemBER<TileEntityMachineIndustrialTurbine, IndustrialTurbineRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/industrial_turbine.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/industrial_turbine.png");

    @Override
    protected void setup(Gem.Model<IndustrialTurbineRenderState> declaration) {
        Gem.Part<IndustrialTurbineRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Turbine"), surface);
        machine.child(model.part("Gauge"), surface)
                .spin(Gem.Axes.Z, 0F, 1.5F, 0F, state -> state.gauge);
        machine.child(model.part("Flywheel"), surface)
                .spin(Gem.Axes.Z, 0F, 1.5F, 0F, state -> -state.rotor);
    }

    @Override
    public IndustrialTurbineRenderState createRenderState() {
        return new IndustrialTurbineRenderState();
    }

    @Override
    public void extract(TileEntityMachineIndustrialTurbine be, IndustrialTurbineRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = 180F + switch (ForgeDirection.getOrientation(meta)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        state.gauge = 135F - (be.tanks[0].getTankType().index() - Fluids.STEAM.index()) * 90F;
        state.rotor = be.lastRotor + (be.rotor - be.lastRotor) * partialTicks;
    }
}
