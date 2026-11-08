package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.tileentity.machine.TileEntityMachineSatDock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code MachineSatDock}, the "Cargo Landing Pad": a 3x3 multiblock of full cells placed one block ahead of the
 * player, every cell an inventory proxy. The drop pod lands on the core cell. A plain click on any cell opens the
 * core's GUI; sneaking passes.
 */
public class MachineSatDock extends BlockMultiblockMachine implements WorldlyContainerHolder {

    public MachineSatDock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineSatDock(pos, state);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(CELL) && !MultiblockSurface.carriesPassive(level, pos, PASSIVE_ITEMS)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineSatDock dock ? dock : null;
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_ITEMS);
    }

    /** Whether {@code state} is the pad's core cell, the one 1.7's rocket compared against {@code sat_dock}. */
    public static boolean isCore(BlockState state) {
        return state.getBlock() instanceof MachineSatDock && !state.getValue(CELL);
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player,
                                          BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.useAtCore(state, level, core, player, hit);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {-1, 0, -1, ForgeDirection.WEST.ordinal()},
                {-1, 0, -1, ForgeDirection.NORTH.ordinal()},
                {-1, 0, 0, ForgeDirection.WEST.ordinal()},
                {-1, 0, 1, ForgeDirection.WEST.ordinal()},
                {-1, 0, 1, ForgeDirection.SOUTH.ordinal()},
                {0, 0, -1, ForgeDirection.NORTH.ordinal()},
                {0, 0, 1, ForgeDirection.SOUTH.ordinal()},
                {1, 0, -1, ForgeDirection.EAST.ordinal()},
                {1, 0, -1, ForgeDirection.NORTH.ordinal()},
                {1, 0, 0, ForgeDirection.EAST.ordinal()},
                {1, 0, 1, ForgeDirection.EAST.ordinal()},
                {1, 0, 1, ForgeDirection.SOUTH.ordinal()},
        }, dir);
    }
}
