package com.hbm.blocks.bomb;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.TileEntityCompactLauncher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code CompactLauncher}: the 3x3 custom-missile pad. It forms from a {@code struct_launcher_core} ringed by
 * eight {@code struct_launcher} (or is placed directly from the creative tab) and drops the core component back.
 *
 * <p>Upstream built it from its own block plus two dummy blocks, {@code dummy_port_compact_launcher} on the four
 * corners and {@code dummy_plate_compact_launcher} on the four edges. This port folds both into cells of this
 * block: the corner cells are the ports (power and fluid, open on their two outward faces and underneath, where
 * 1.7's {@code getConPos} reached), the edge cells are plain plate. The dummy ids are therefore not registered.
 *
 * <p>Only the centre block answers a detonator — the GUI says as much — and redstone on any of the nine blocks
 * fires it, which the tile checks itself every tick as upstream did.
 */
public class CompactLauncher extends BlockMultiblockMachine implements IBomb, IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public CompactLauncher(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCompactLauncher(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 0; }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.cell(core.offset(dx, 0, dz), outwardMask(dx, dz, 1) | MASK_DOWN);
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.passiveCell(core.offset(dx, 0, dz), MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
            }
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (level.getBlockState(pos).getValue(CELL)) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof TileEntityCompactLauncher launcher) {
            if (launcher.canLaunch()) {
                launcher.launchFromDesignator();
                return BombReturnCode.LAUNCHED;
            }
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        return BombReturnCode.UNDEFINED;
    }
}
