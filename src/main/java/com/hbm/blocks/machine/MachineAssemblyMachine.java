package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Assembly Machine: a 3×3×3 dummyable multiblock (core + 8 ringed "extra" dummies + fill); power and fluid ports on the ring, as 1.7. */
public class MachineAssemblyMachine extends BlockMultiblockMachine implements WorldlyContainerHolder, IEnergyConnectorBlock,
        IFluidConnectorBlockMK2 {

    public MachineAssemblyMachine(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineAssemblyMachine(pos, state);
    }

    /** Any face of the multiblock exposes the core's inventory to hoppers/pipes. */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineAssemblyMachine te ? te : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /** The eight cells around the core are the port ring; each opens on the side it faces outward. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 || j != 0) visitor.cell(core.offset(i, 0, j), outwardMask(i, j, 1));
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 || j != 0) visitor.passiveCell(core.offset(i, 0, j), MASK_ALL, domains);
            }
        }
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {-1, 0, -1, ForgeDirection.WEST.ordinal()},
                {-1, 0, -1, ForgeDirection.NORTH.ordinal()},
                {-1, 0, 0, ForgeDirection.WEST.ordinal()},
                {-1, 0, 1, ForgeDirection.WEST.ordinal()},
                {-1, 0, 1, ForgeDirection.SOUTH.ordinal()},
                {0, 0, -1, ForgeDirection.NORTH.ordinal()},
                {0, 0, 1, ForgeDirection.SOUTH.ordinal()},
                {1, 0, -1, ForgeDirection.EAST.ordinal()},
                {1, 0, -1, ForgeDirection.NORTH.ordinal()},
                {1, 0, 0, ForgeDirection.EAST.ordinal()},
                {1, 0, 1, ForgeDirection.EAST.ordinal()},
                {1, 0, 1, ForgeDirection.SOUTH.ordinal()},
        }, dir);
    }
}
