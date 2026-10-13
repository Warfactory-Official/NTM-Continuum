package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineCompressor;
import com.hbm.tileentity.machine.TileEntityMachineCompressorBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Compressor block: 1:1 port of the 1.12.2 {@code MachineCompressor} (dims {@code {2,0,1,2,1,1}},
 * offset 2; two extra body fills + 3 makeExtra power/fluid ports). */
public class MachineCompressor extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineCompressor(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCompressor(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 1, 2, 1, 1}; }
    @Override public int getOffset() { return 2; }

    private static final int[] FAN_PLATFORM = {3, -3, 1, 1, 1, 1};
    private static final int[] CHIMNEY = {8, -4, 0, 0, 1, 1};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return super.checkRequirement(level, core, facing, clicked)
                && MultiblockHandlerXR.checkSpace(level, core, FAN_PLATFORM, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core, CHIMNEY, clicked, facing);
    }

    /** The body is three boxes, and three cells around the core's waist are the power and fluid surface. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, FAN_PLATFORM, facing, visitor);
        MultiblockHandlerXR.visitBox(core, CHIMNEY, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.offset(-facing.getStepX(), 0, -facing.getStepZ()), MASK_NORTH);
        visitor.cell(core.offset(rot.getStepX(), 0, rot.getStepZ()), MASK_WEST);
        visitor.cell(core.offset(-rot.getStepX(), 0, -rot.getStepZ()), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.offset(-facing.getStepX(), 0, -facing.getStepZ()), MASK_ALL, domains);
        visitor.passiveCell(core.offset(rot.getStepX(), 0, rot.getStepZ()), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-rot.getStepX(), 0, -rot.getStepZ()), MASK_ALL, domains);
    }

    /** 1.7 {@code MachineCompressor.getAllDimensions}, read only by the placement highlight. */
    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
            new int[] {2, 0, 1, 2, 1, 1},
            new int[] {3, -3, 1, 1, 1, 1},
            new int[] {8, -4, 0, 0, 1, 1}
        };
    }
}
