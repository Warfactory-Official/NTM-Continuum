package com.hbm.blocks.generic;

import com.hbm.config.ServerConfig;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockStorageCrate.removedByPlayer} / {@code PneumoStorageClutter.removedByPlayer} with
 * {@code /ntmserver CRATE_KEEP_CONTENTS} off: the crate breaks like a chest - a bare crate item (not in creative) and
 * the contents spilled, except that a locked crate's contents are lost (1.7 {@code dropInv = false}). With the key on
 * (the default) nothing here runs and the contents ride on the dropped item.
 */
public final class CrateKeepContents {

    private CrateKeepContents() {}

    /**
     * Call from the block's {@code onPlayerWillDestroy}. Returns true when the key is off and the block was broken
     * here (it is air afterwards, so vanilla's own drop never happens - 1.7 set the block to air itself too).
     */
    public static boolean breakLoose(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide() || ServerConfig.CRATE_KEEP_CONTENTS.get()) return false;
        if (!player.isCreative()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(state.getBlock())));
        }
        BlockEntity be = level.getBlockEntity(pos);
        boolean dropInv = !(be instanceof TileEntityLockableBase lockable && lockable.isLocked());
        if (be instanceof Container container) {
            if (dropInv) Containers.dropContents(level, pos, container);
            container.clearContent();
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return true;
    }
}
