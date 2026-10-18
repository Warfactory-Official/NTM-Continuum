package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.machine.TileEntityMachineRadarLarge;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.7 {@code MachineRadarLarge}: a 3x3 footprint, five tall ({@code {4, 0, 1, 1, 1, 1}}, offset 1), whose four
 * edge-centre cells at the core's height are power proxies ({@code makeExtra} with
 * {@code TileEntityProxyCombo().power()}). Those four cells are also the radar's redstone outputs: 1.7 asked
 * the tile one step further in the queried direction, which from a neighbour outside the footprint is the core.
 */
public class MachineRadarLarge extends BlockMultiblockMachine implements IEnergyConnectorBlock {

    public MachineRadarLarge(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineRadarLarge(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /** The box, then the four {@code makeExtra} cells, each open outward for power. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.east(), MASK_EAST, ROLE_POWER);
        visitor.cell(core.west(), MASK_WEST, ROLE_POWER);
        visitor.cell(core.south(), MASK_SOUTH, ROLE_POWER);
        visitor.cell(core.north(), MASK_NORTH, ROLE_POWER);
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return MachineRadar.use(level, hit.getBlockPos(), core, player);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * 1.7: only the "extra" cells answer, by asking the tile at {@code pos + dir}. {@code direction} points from
     * the asking neighbour into this cell, as 1.7's side did, so {@code pos + direction} is the core exactly
     * when the neighbour is outside an edge-centre cell.
     */
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!state.getValue(CELL)) return 0;
        BlockPos core = MultiblockSurface.coreOfCell(level, pos);
        if (core == null || core.getY() != pos.getY()) return 0;
        if (Math.abs(core.getX() - pos.getX()) + Math.abs(core.getZ() - pos.getZ()) != 1) return 0;
        return level.getBlockEntity(pos.relative(direction)) instanceof TileEntityMachineRadarNT radar ? radar.getRedPower() : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {1, 0, 0, ForgeDirection.EAST.ordinal()},
                {-1, 0, 0, ForgeDirection.WEST.ordinal()},
                {0, 0, 1, ForgeDirection.SOUTH.ordinal()},
                {0, 0, -1, ForgeDirection.NORTH.ordinal()},
        }, dir);
    }
}
