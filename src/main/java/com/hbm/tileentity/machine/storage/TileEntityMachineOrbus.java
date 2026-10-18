package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.lib.Library;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * 1.7 {@code TileEntityMachineOrbus}, the Heavy Magnetic Storage Tank: a 512,000 mB barrel with the barrel's
 * menu, modes and item form, whose eight connectors sit under and over the four corner columns of the 2x2
 * middle, and whose transfer rate is a hundredth of what it holds or has room for, at least 1,000 mB a tick.
 * It never reacts to what it holds ({@code checkFluidInteraction}: "NO!").
 */
public class TileEntityMachineOrbus extends TileEntityBarrel {

    public TileEntityMachineOrbus(BlockPos pos, BlockState state) {
        super(ModRadarUtilityBlockEntities.ORBUS.get(), pos, state, 512000);
    }

    @Override
    public String getDefaultName() {
        return "container.orbus";
    }

    @Override public long getReceiverSpeed(FluidType type, int pressure) { return (mode == 0 || mode == 1) ? Math.max(1_000, (tank.getMaxFill() - tank.getFill()) / 100) : 0; }
    @Override public long getProviderSpeed(FluidType type, int pressure) { return (mode == 1 || mode == 2) ? Math.max(1_000, tank.getFill() / 100) : 0; }

    @Override
    public void checkFluidInteraction() { } // NO!

    @Override
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            // 1.7: the top row sits at y + 3, so its connections land inside the orbus's own top layer
            ForgeDirection dir = MachineCore.facing(getBlockState()).getOpposite();
            ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x, y, z, Library.NEG_Y),
                    PortDef.make(x + dir.offsetX, y, z + dir.offsetZ, Library.NEG_Y),
                    PortDef.make(x + rot.offsetX, y, z + rot.offsetZ, Library.NEG_Y),
                    PortDef.make(x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ, Library.NEG_Y),
                    PortDef.make(x, y + 3, z, Library.POS_Y),
                    PortDef.make(x + dir.offsetX, y + 3, z + dir.offsetZ, Library.POS_Y),
                    PortDef.make(x + rot.offsetX, y + 3, z + rot.offsetZ, Library.POS_Y),
                    PortDef.make(x + dir.offsetX + rot.offsetX, y + 3, z + dir.offsetZ + rot.offsetZ, Library.POS_Y),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }
}
