package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import dev.golem.core.multiblock.MultiblockMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.hbm.tileentity.machine.TileEntityMachineLPW2;

/**
 * 1.7 {@code MachineLPW2}: a 20x7x7 structure (dims {@code {6,0,3,3,9,10}}, offset 3) with no ports, no
 * menu and no logic of any kind; the whole machine is the animated renderer.
 */
public class MachineLPW2 extends BlockMultiblockMachine {

    public MachineLPW2(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineLPW2(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{6, 0, 3, 3, 9, 10};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    /** 1.7 {@code BlockDummyable} default: no {@code onBlockActivated} override, so the click falls through. */
    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
