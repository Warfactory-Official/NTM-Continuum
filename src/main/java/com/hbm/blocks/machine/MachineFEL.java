package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFEL;
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

/**
 * Free Electron Laser (1.12.2 {@code machine_fel}): a long, low emitter that fires a beam up to 24
 * blocks out, driving any correctly-placed {@link com.hbm.blocks.machine.MachineSILEX} in its path and
 * burning through anything else.
 *
 * <p>The footprint is lopsided: {@code {2, 0, 4, 2, 1, 1}} means it extends <b>4 blocks
 * forward and 2 back</b> from the core, with the core itself sitting {@code getOffset() == 2} blocks in
 * front of where the player clicked. Power enters at the single rear port {@link #fillSpace} places
 * behind the machine, {@code o - 4} along the facing axis at {@code y+1}.
 */
public class MachineFEL extends BlockMultiblockMachine implements IEnergyConnectorBlock, WorldlyContainerHolder {

    public MachineFEL(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFEL(pos, state);
    }

    /** Exposes the battery/crystal slots to hoppers, like the other machines. */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityFEL te ? te : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 4, 2, 1, 1};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    /** The single rear power port, four back along the facing axis and one up. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.offset(-facing.getStepX() * 4, 1, -facing.getStepZ() * 4), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(-facing.getStepX() * 4, 1, -facing.getStepZ() * 4), MASK_ALL,
                PASSIVE_POWER_IN);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {0, 1, -4, ForgeDirection.NORTH.ordinal()},
        }, dir);
    }

    @Override
    public double[][] getAABBExtras() {
        return new double[][] {
                {1.9, 1.1, 2.5, 2.5, 0.4, -0.4},
        };
    }
}
