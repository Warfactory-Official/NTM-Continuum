package com.hbm.tileentity.machine;

import dev.golem.core.world.Explosions;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;

/** 2,048,000 mB BAT-9000 controller. */
public class TileEntityMachineBAT9000 extends TileEntityMachineGiantTank {

    public static final int CAPACITY = 2_048_000;

    public TileEntityMachineBAT9000(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_BAT9000.get(), pos, state, CAPACITY);
    }

    @Override
    public String getDefaultName() {
        return "container.bat9000";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineBAT9000 tank) {
        TileEntityMachineGiantTank.serverTick(level, pos, state, tank);
    }

    @Override
    protected boolean checkFluidInteraction(Level level) {
        if (tankNew.getTankType() != Fluids.AMAT
                && tankNew.getTankType() != Fluids.ASCHRAB) return false;
        level.removeBlock(worldPosition, false);
        Explosions.explode(level, null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D, 10F, true, Level.ExplosionInteraction.TNT);
        return true;
    }

    protected PortDef[] cachedPorts;

    @Override
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + 1, y, z + 2, Library.POS_Z),
                    PortDef.make(x - 1, y, z + 2, Library.POS_Z),
                    PortDef.make(x + 1, y, z - 2, Library.NEG_Z),
                    PortDef.make(x - 1, y, z - 2, Library.NEG_Z),
                    PortDef.make(x + 2, y, z + 1, Library.POS_X),
                    PortDef.make(x - 2, y, z + 1, Library.NEG_X),
                    PortDef.make(x + 2, y, z - 1, Library.POS_X),
                    PortDef.make(x - 2, y, z - 1, Library.NEG_X)
            };
        }
        return cachedPorts;
    }
}
