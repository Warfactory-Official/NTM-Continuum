package com.hbm.blocks.generic;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import dev.golem.core.multiblock.MultiblockMachine;
import com.hbm.tileentity.deco.TileEntityLantern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.7 {@code BlockLantern}: a five-tall post whose lamp keeps glyphids blinded.
 *
 * <p>Dimensions {@code {4,0,0,0,0,0}} with offset 0 - four cells straight up from the core and nothing
 * sideways, so the folded footprint is a column. No ports, no menu; the block entity exists for the
 * glyphid sweep and the renderer.
 */
public class BlockLantern extends BlockMultiblockMachine {

    public BlockLantern(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLantern(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /** 1.7 {@code BlockDummyable} default: no {@code onBlockActivated} override, so the click falls through. */
    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
