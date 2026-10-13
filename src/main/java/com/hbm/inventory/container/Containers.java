package com.hbm.inventory.container;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

/**
 * Shared {@code stillValid} logic for HBM's machine menus.
 *
 * <p>Vanilla's {@link net.minecraft.world.inventory.AbstractContainerMenu#stillValid} measures the
 * player's block-interaction range against the position the menu is <i>bound</i> to. For a normal block
 * that is the block you clicked. For a multiblock it is the <b>core</b>, and the player stands next to a
 * dummy cell that may be many blocks away from it (on the fusion torus the core sits seven blocks inside
 * the footprint), so the menu would report out-of-range.
 *
 * <p>{@link #stillValidMultiblock} widens the radius to cover the structure, using the range the block
 * itself declares. Non-multiblock blocks use vanilla's behaviour.
 */
public final class Containers {

    private Containers() {}

    /**
     * Whether {@code player} may keep a menu bound to {@code access} open, where {@code block} is the
     * machine that owns it. Identical to vanilla's check for an ordinary block; for a multiblock the
     * range covers the whole footprint rather than just the core cell.
     */
    /** Whether {@code player} is in range of the multiblock cored at {@code pos}. */
    public static boolean inRangeOfMultiblock(BlockGetter level,
                                              BlockPos pos, Player player) {
        return level.getBlockState(pos).getBlock() instanceof BlockMultiblockCore
                && BlockMultiblockCore.isWithinInteractionRange(level, pos, player);
    }

    public static boolean stillValidMultiblock(ContainerLevelAccess access, Player player, Block block) {
        return access.evaluate((level, pos) -> {
            if (!level.getBlockState(pos).is(block)) return false;
            if (block instanceof BlockMultiblockCore) {
                return inRangeOfMultiblock(level, pos, player);
            }
            // Not a multiblock: vanilla's plain 8-block radius, spelled the era-neutral way
            // (26.1's isWithinBlockInteractionRange has no counterpart below it).
            return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
        }, true);
    }
}
