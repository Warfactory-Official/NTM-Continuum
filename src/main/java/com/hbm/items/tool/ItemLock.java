package com.hbm.items.tool;

import com.hbm.tileentity.machine.TileEntityLockableBase;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.hbm.sound.ModSecuritySounds;
import com.hbm.util.LockTargets;

/**
 * A padlock. Port of 1.7.10's {@code ItemLock}: used on an unlocked {@link TileEntityLockableBase}
 * (resolved through its multiblock core, the old {@code CompatExternal.getCoreFromPos}) it copies its
 * pins onto the block, locks it, stamps its pick modifier and is consumed. {@code lockMod} is the
 * lockpick success chance (1 = 100%, 0 = unpickable).
 */
public class ItemLock extends ItemKeyPin {

    public double lockMod = 0.1D;

    public ItemLock(Properties properties, double mod) {
        super(properties);
        lockMod = mod;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (pins(stack) != 0) {
            BlockEntity te = LockTargets.coreAt(level, pos);

            if (te instanceof TileEntityLockableBase tile) {

                if (tile.isLocked())
                    return InteractionResult.TRY_WITH_EMPTY_HAND;

                if (!level.isClientSide()) {
                    tile.setPins(pins(stack));
                    tile.lock();
                    tile.setMod(lockMod);

                    level.playSound(null, pos, ModSecuritySounds.LOCK_HANG.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    stack.shrink(1); // 1.7.10 decrements unconditionally, creative included
                }

                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
