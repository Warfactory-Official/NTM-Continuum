package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineWoodBurner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The old two-port, six-slot wood burner. */
public class MachineWoodBurner extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineWoodBurner(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineWoodBurner(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{1, 0, 1, 0, 1, 0}; }
    @Override public int getOffset() { return 0; }

    /** 1.7 upgrades two cells behind the burner to ports; the pair rides the facing. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(back), MASK_NORTH);
        visitor.cell(core.relative(back).relative(rot), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_ITEMS);

        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.relative(back), MASK_ALL, domains);
        visitor.passiveCell(core.relative(back).relative(rot), MASK_ALL, domains);
    }
}
