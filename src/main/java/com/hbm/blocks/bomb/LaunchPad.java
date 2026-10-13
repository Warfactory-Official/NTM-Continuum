package com.hbm.blocks.bomb;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Silo Launch Pad: a 3×3 flat dummyable pad (core in the centre, surrounding pad blocks).
 *
 * <p>As an {@link IBomb} it is "detonated" by launching: a detonator aimed at any block of the pad
 * fires the missile at whatever the installed designator points to. Redstone does the same through
 * {@link #onNeighborChanged}: every block of the multiblock reports its own powered state to the core,
 * which counts them so one lever produces one launch rather than nine.
 *
 * <p><b>Cables and ducts attach at the four corner cells only</b>, one block further out: the eight
 * positions in {@link com.hbm.tileentity.bomb.TileEntityLaunchPad#getConPos()}. As upstream, only the corner cells
 * forward power, fluid and inventory to the core. The port has no proxy block entities: a cable asks the
 * neighbouring <em>block</em> whether that face is a port, so the pad answers through
 * {@link IEnergyConnectorBlock}/{@link IFluidConnectorBlockMK2}.
 */
public class LaunchPad extends BlockMultiblockMachine implements IBomb, IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public LaunchPad(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLaunchPad(pos, state);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityLaunchPad pad) {
            pad.detonator = detonator instanceof LivingEntity living ? living : null;
            return pad.launchFromDesignator();
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighborBlock, movedByPiston);
        if (level.isClientSide()) return;

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityLaunchPad pad) {
            pad.updateRedstonePower(level, pos);
        }
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /** The four corner cells, each open on the two faces its corner turns outward. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.cell(core.offset(dx, 0, dz), outwardMask(dx, dz, 1));
            }
        }
    }

    /** 1.7 stamps a full proxy on each corner: power, fluid and the core's inventory. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.passiveCell(core.offset(dx, 0, dz), MASK_ALL, domains);
            }
        }
    }
}
