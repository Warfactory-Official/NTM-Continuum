package com.hbm.tileentity.machine;

import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
        if (tankNew.getTankType() != com.hbm.inventory.fluid.Fluids.AMAT
                && tankNew.getTankType() != com.hbm.inventory.fluid.Fluids.ASCHRAB) return false;
        level.removeBlock(worldPosition, false);
        level.explode(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D, 10F, true, Level.ExplosionInteraction.TNT);
        return true;
    }

    @Override
    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() + 3, Library.POS_Z),
                new DirPos(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() + 3, Library.POS_Z),
                new DirPos(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() - 3, Library.NEG_Z),
                new DirPos(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 3, Library.NEG_Z),
                new DirPos(worldPosition.getX() + 3, worldPosition.getY(), worldPosition.getZ() + 1, Library.POS_X),
                new DirPos(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() + 1, Library.NEG_X),
                new DirPos(worldPosition.getX() + 3, worldPosition.getY(), worldPosition.getZ() - 1, Library.POS_X),
                new DirPos(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 1, Library.NEG_X)
        };
    }
}
