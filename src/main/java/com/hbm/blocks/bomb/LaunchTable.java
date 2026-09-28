package com.hbm.blocks.bomb;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.TileEntityLaunchTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code LaunchTable}: the 9x9 custom-missile table. {@link #FACING} is the side its scaffold stands on
 * (1.7 meta 0/1/2/3 = +X/-X/+Z/-Z). Placed by hand, a player looking south gets meta 0 (+X), and so on round,
 * which is the placer's facing turned once clockwise.
 *
 * <p>Upstream's two dummy blocks are folded into cells as for the compact launcher: the line of cells across
 * the scaffold axis is {@code dummy_plate_launch_table}, every other cell {@code dummy_port_launch_table}. Port
 * cells take power and fluid; the edge ports open outward, where 1.7 subscribed its connections.
 */
public class LaunchTable extends BlockMultiblockMachine implements IBomb, IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public LaunchTable(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLaunchTable(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 4, 4, 4, 4}; }
    @Override public int getOffset() { return 0; }
    @Override public Direction getDirModified(Direction direction) { return direction.getClockWise(); }

    /** Whether the cell at this offset is one of 1.7's plate dummies: the line across the scaffold axis. */
    public static boolean isPlate(Direction facing, int dx, int dz) {
        return facing.getAxis() == Direction.Axis.X ? dx == 0 && dz != 0 : dz == 0 && dx != 0;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.abs(dx) != 4 && Math.abs(dz) != 4) continue;
                if (isPlate(facing, dx, dz)) continue;
                visitor.cell(core.offset(dx, 0, dz), outwardMask(dx, dz, 4));
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if ((dx == 0 && dz == 0) || isPlate(facing, dx, dz)) continue;
                visitor.passiveCell(core.offset(dx, 0, dz), MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
            }
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (level.getBlockState(pos).getValue(CELL)) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof TileEntityLaunchTable table) {
            if (table.canLaunch()) {
                table.launchFromDesignator();
                return BombReturnCode.LAUNCHED;
            }
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        return BombReturnCode.UNDEFINED;
    }
}
