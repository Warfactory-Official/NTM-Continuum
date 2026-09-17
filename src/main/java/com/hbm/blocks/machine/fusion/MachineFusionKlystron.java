package com.hbm.blocks.machine.fusion;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Fusion klystron - the microwave emitter that ignites a torus. Power and compressed air come in at
 * the two side ports; the beam leaves from the muzzle port at +2. Behaviour lives in
 * {@link TileEntityFusionKlystron}.
 */
public class MachineFusionKlystron extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineFusionKlystron(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionKlystron(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 4, 3, 2, 2}; }

    @Override public int getOffset() { return 3; }

    /** 1.7: the waveguide arm reaching out past the body. */
    private static final int[] WAVEGUIDE = {4, -3, 4, 3, 1, 1};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return super.checkRequirement(level, core, facing, clicked)
                && MultiblockHandlerXR.checkSpace(level, core, WAVEGUIDE, clicked, facing);
    }

    /** The waveguide mouth at the muzzle, and the power and air laterals one step out on each flank. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, WAVEGUIDE, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.offset(facing.getStepX() * 3, 2, facing.getStepZ() * 3), MASK_SOUTH);
        visitor.cell(core.offset(rot.getStepX() * 2, 0, rot.getStepZ() * 2), MASK_WEST);
        visitor.cell(core.offset(-rot.getStepX() * 2, 0, -rot.getStepZ() * 2), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.offset(facing.getStepX() * 3, 2, facing.getStepZ() * 3), MASK_ALL, domains);
        visitor.passiveCell(core.offset(rot.getStepX() * 2, 0, rot.getStepZ() * 2), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-rot.getStepX() * 2, 0, -rot.getStepZ() * 2), MASK_ALL, domains);
    }
}
