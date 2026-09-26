package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * MachineLiquefactor block - 1:1 port of the 1.12.2 {@code MachineLiquefactor}: a 4-tall 3x3 the folded core
 * (dims {@code {3,0,1,1,1,1}}, offset 1) with the top-centre + the 4 mid-side cells marked extras
 * (the fluid/power ports on {@code getConPos}).
 */
public class MachineLiquefactor extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineLiquefactor(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineLiquefactor(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    public int coreMask() {
        return MASK_DOWN;
    }

    /** The spout three above and the four waist inlets; the set is its own rotation. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        visitor.cell(core.above(3), MASK_UP);
        visitor.cell(core.offset(1, 1, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 1, 0), MASK_WEST);
        visitor.cell(core.offset(0, 1, 1), MASK_SOUTH);
        visitor.cell(core.offset(0, 1, -1), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.above(3), MASK_ALL, domains);
        visitor.passiveCell(core.offset(1, 1, 0), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-1, 1, 0), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 1, 1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 1, -1), MASK_ALL, domains);
    }
}
