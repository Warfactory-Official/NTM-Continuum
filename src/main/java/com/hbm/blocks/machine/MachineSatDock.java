package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.tileentity.machine.TileEntityMachineSatDock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.7 {@code MachineSatDock}, the "Cargo Landing Pad": a 3x3 pad whose eight outer cells were
 * {@code dummy_plate_cargo} ({@code DummyBlockMachine}, 8 px tall) around a 12 px core. Folded here as the pad's
 * own cells, so {@code dummy_plate_cargo} is not a separate registry entry. The miner rocket lands on the core
 * cell only. A plain click on any cell opens the core's GUI; sneaking passes.
 */
public class MachineSatDock extends BlockMultiblockMachine {

    public MachineSatDock(Properties properties) {
        super(properties);
        // core: 1.7 setBlockBounds(0, 0, 0, 1, 12/16, 1)
        this.bounding.add(new AABB(-0.5D, 0D, -0.5D, 0.5D, 0.75D, 0.5D));
        // the eight dummies: DummyBlockMachine.setBounds(0, 0, 0, 16, 8, 16)
        this.bounding.add(new AABB(-1.5D, 0D, -1.5D, 1.5D, 0.5D, 1.5D));
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineSatDock(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 0; }

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
}
