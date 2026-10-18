package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The four-wide legacy turbofan multiblock. */
public class MachineTurbofan extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineTurbofan(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineTurbofan(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 1, 1, 3, 3}; }
    @Override public int getOffset() { return 1; }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.onUseWithoutItem(state, level, pos, player, hit);
    }

    /** Four intake anchors, two in front of the core and two behind. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(dx, 0, dz), MASK_SOUTH);
        visitor.cell(core.offset(dx - rx, 0, dz - rz), MASK_SOUTH);
        visitor.cell(core.offset(-dx, 0, -dz), MASK_NORTH);
        visitor.cell(core.offset(-dx - rx, 0, -dz - rz), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.passiveCell(core.offset(dx, 0, dz), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(dx - rx, 0, dz - rz), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-dx, 0, -dz), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-dx - rx, 0, -dz - rz), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {0, 0, 1, ForgeDirection.SOUTH.ordinal()},
                {-1, 0, 1, ForgeDirection.SOUTH.ordinal()},
                {0, 0, -1, ForgeDirection.NORTH.ordinal()},
                {-1, 0, -1, ForgeDirection.NORTH.ordinal()},
        }, dir);
    }
}
