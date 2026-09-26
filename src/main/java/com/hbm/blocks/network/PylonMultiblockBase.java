package com.hbm.blocks.network;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.hbm.tileentity.network.TileEntityPylonBase;

/**
 * What the four 1.7 {@code BlockDummyable} pylons share: a right-click with a dye recolours the wires
 * (from any cell, resolved to the core), and nothing else opens. Wire teardown lives on the tile, which
 * learns that its block is gone from {@code setRemoved}.
 */
public abstract class PylonMultiblockBase extends BlockMultiblockMachine {

    protected PylonMultiblockBase(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) {
            return PylonDyes.colorFromDye(stack) != 0 ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityPylonBase pylon && pylon.setColor(stack)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
