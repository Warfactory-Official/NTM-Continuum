package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
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

/** Pyro Oven block — 1:1 port of the 1.12.2 {@code MachinePyroOven} (dims {@code {2,0,3,3,2,2}}, offset 3;
 * a row of 5 makeExtra ports along one side + one at y+2). */
public class MachinePyroOven extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachinePyroOven(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachinePyroOven(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 3, 3, 2, 2}; }
    @Override public int getOffset() { return 3; }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachinePyroOven te ? te : null;
    }

    /**
     * A row of five ports down one flank, and the chimney mouth above. 1.7 sends only smoke to the
     * chimney -- the getConPos loop's power and fluid-in never reach it -- so it is stated as a fluid
     * outlet alone rather than folded into the union with the flank.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getCounterClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        for (int i = -2; i <= 2; i++) {
            visitor.cell(core.offset(dx * i + rx * 2, 0, dz * i + rz * 2), MASK_EAST);
        }
        visitor.cell(core.offset(-rx, 2, -rz), MASK_UP, ROLE_FLUID_OUT);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getCounterClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;

        for (int i = -2; i <= 2; i++) {
            visitor.passiveCell(core.offset(dx * i + rx * 2, 0, dz * i + rz * 2), MASK_ALL, domains);
        }
        visitor.passiveCell(core.offset(-rx, 2, -rz), MASK_ALL, domains);
    }
}
