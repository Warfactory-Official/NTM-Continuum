package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityMachineOrbus}, the Heavy Magnetic Storage Tank: a 512,000 mB barrel with the barrel's
 * menu, modes and item form, whose eight connectors sit under and over the four corner columns of the 2x2
 * middle, and whose transfer rate is a hundredth of what it holds or has room for, at least 1,000 mB a tick.
 * It never reacts to what it holds ({@code checkFluidInteraction}: "NO!").
 */
public class TileEntityMachineOrbus extends TileEntityBarrel {

    protected DirPos[] conPos;

    public TileEntityMachineOrbus(BlockPos pos, BlockState state) {
        super(ModRadarUtilityBlockEntities.ORBUS.get(), pos, state, 512000);
    }

    @Override
    public String getDefaultName() {
        return "container.orbus";
    }

    @Override public long getReceiverSpeed(FluidType type, int pressure) { return Math.max(1_000, (tank.getMaxFill() - tank.getFill()) / 100); }
    @Override public long getProviderSpeed(FluidType type, int pressure) { return Math.max(1_000, tank.getFill() / 100); }

    @Override
    public void checkFluidInteraction() { } // NO!

    @Override
    public DirPos[] getConPos() {

        if (conPos != null) return conPos;

        conPos = new DirPos[8];

        ForgeDirection dir = MachineCore.facing(getBlockState()).getOpposite();
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        for (int i = -1; i < 6; i += 6) {
            ForgeDirection out = i == -1 ? ForgeDirection.DOWN : ForgeDirection.UP;
            int index = i == -1 ? 0 : 4;
            conPos[index] = new DirPos(x, y + i, z, out);
            conPos[index + 1] = new DirPos(x + dir.offsetX, y + i, z + dir.offsetZ, out);
            conPos[index + 2] = new DirPos(x + rot.offsetX, y + i, z + rot.offsetZ, out);
            conPos[index + 3] = new DirPos(x + dir.offsetX + rot.offsetX, y + i, z + dir.offsetZ + rot.offsetZ, out);
        }

        return conPos;
    }
}
