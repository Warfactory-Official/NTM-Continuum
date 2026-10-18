package com.hbm.blocks.bomb;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.TileEntityLaunchPadLarge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code LaunchPadLarge}: the 9x9 erector launch pad (dimensions 0/0/4/4/4/4, offset 4). Its collision is
 * upstream's three boxes: the two pad halves and the recessed rail between them, one eighth lower.
 *
 * <p>1.7 stamped an inventory proxy on every cell and a full power/fluid/inventory proxy on the eight
 * {@code makeExtra} cells at (+-4, +-2) and (+-2, +-4), the large pad's ports.
 * Redstone on any cell counts once toward the pad's rising edge, as for the small pad.
 */
public class LaunchPadLarge extends BlockMultiblockMachine implements IBomb, IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public LaunchPadLarge(Properties properties) {
        super(properties);
        this.bounding.add(new AABB(-4.5D, 0D, -4.5D, 4.5D, 1D, -0.5D));
        this.bounding.add(new AABB(-4.5D, 0D, 0.5D, 4.5D, 1D, 4.5D));
        this.bounding.add(new AABB(-4.5D, 0.875D, -0.5D, 4.5D, 1D, 0.5D));
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLaunchPadLarge(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 4, 4, 4, 4}; }
    @Override public int getOffset() { return 4; }

    private static final int[][] EXTRA = {{4, 2}, {4, -2}, {-4, 2}, {-4, -2}, {2, 4}, {-2, 4}, {2, -4}, {-2, -4}};

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] e : EXTRA) visitor.cell(core.offset(e[0], 0, e[1]), outwardMask(e[0], e[1], 4));
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_ITEMS);
        for (int[] e : EXTRA) {
            visitor.passiveCell(core.offset(e[0], 0, e[1]), MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityLaunchPadLarge pad) {
            pad.detonator = detonator instanceof LivingEntity living ? living : null;
            return pad.launchFromDesignator();
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighborBlock, movedByPiston);
        if (level.isClientSide()) return;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityLaunchPadLarge pad) {
            pad.updateRedstonePower(level, pos);
        }
    }
}
