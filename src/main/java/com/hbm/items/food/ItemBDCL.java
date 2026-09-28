package com.hbm.items.food;

import com.hbm.sound.ModGunSounds;
import dev.golem.core.world.CompatItem;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemBDCL}: a 40-tick drink that does nothing but gulp and groan. Its {@code onUsingTick} reads 1.7's
 * {@code itemInUseCount}, gulping every fifth count while at least ten remain, and from 24 down it steals one
 * extra count every fourth count, so the drink really lasts 34 ticks. Modern use cannot take ticks away, so the
 * use lasts those 34 ticks and {@link #COUNTS} maps each real tick back to the count 1.7 saw on it.
 */
public class ItemBDCL extends CompatItem {

    /** The 1.7 {@code itemInUseCount} seen on each successive tick of the drink. */
    private static final int[] COUNTS = {40, 39, 38, 37, 36, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26, 25,
            24, 22, 21, 20, 18, 17, 16, 14, 13, 12, 10, 9, 8, 6, 5, 4, 2, 1};

    public ItemBDCL(Properties properties) {
        super(properties);
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return COUNTS.length;
    }

    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return UseAnimation.DRINK;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int index = COUNTS.length - remaining;
        if (index < 0 || index >= COUNTS.length || level.isClientSide()) return;
        int count = COUNTS[index];
        if (count % 5 == 0 && count >= 10) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModGunSounds.PLAYER_GULP.get(),
                    SoundSource.PLAYERS, 1F, 1F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player) || !player.getAbilities().instabuild) stack.shrink(1);
        if (!level.isClientSide()) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModGunSounds.PLAYER_GROAN.get(),
                    SoundSource.PLAYERS, 1F, 1F);
        }
        return stack;
    }
}
