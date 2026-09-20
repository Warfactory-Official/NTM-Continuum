package com.hbm.items.weapon;

import dev.golem.core.data.Tags;

import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityB92Beam;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModGunSounds;
import dev.golem.core.world.CompatItem;
import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * The original-hbm B92 energy pistol.
 *
 * <p>This is intentionally not a Sedna gun. The source weapon stores charge in its own two integer
 * values, reloads by holding right click, and creates one explosive beam per stored charge.
 */
public final class GunB92 extends CompatItem {

    private static final String ANIMATION = "b92_animation";
    private static final String ENERGY = "b92_energy";
    private static final Random RANDOM = new Random();

    public GunB92(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && getPower(stack) > 0) {
            player.startUsingItem(hand);
        } else if (getAnim(stack) == 0) {
            setAnim(stack, 1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return UseAnimation.BOW;
    }

    @Override
    protected boolean compatReleaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || player.isShiftKeyDown() || level.isClientSide()) return false;

        int chargeTicks = compatUseDuration(stack, player) - timeLeft;
        if (chargeTicks < 10) return false;

        int power = getPower(stack);
        for (int i = 0; i < power; i++) {
            EntityB92Beam beam = new EntityB92Beam(level, player, 3.0F);
            float divergence = Math.min(i * 0.2F, 1.0F);
            if (i > 0) {
                beam.setDeltaMovement(beam.getDeltaMovement().add(
                        RANDOM.nextGaussian() * divergence,
                        RANDOM.nextGaussian() * divergence,
                        RANDOM.nextGaussian() * divergence));
            }
            level.addFreshEntity(beam);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModGunSounds.SPARK_SHOOT[RANDOM.nextInt(3)].get(), SoundSource.PLAYERS, 5.0F, 1.0F);
        setAnim(stack, 1);
        setPower(stack, 0);
        return true;
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
                                       Entity owner, EquipmentSlot slot, boolean held) {
        int animation = getAnim(stack);
        if (animation <= 0) return;

        if (animation == 15) {
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModGunSounds.B92_RELOAD.get(), SoundSource.PLAYERS, 2.0F, 0.9F);
            int power = getPower(stack) + 1;
            setPower(stack, power);
            if (power > 10) {
                setPower(stack, 0);
                EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFac(
                        level, 50, owner.getX(), owner.getY(), owner.getZ());
                if (level.addFreshEntity(blast)) blast.startChunkLoading();
                level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 100.0F,
                        level.getRandom().nextFloat() * 0.1F + 0.9F);
            }
        }

        setAnim(stack, animation < 30 ? animation + 1 : 0);
    }

    @Override
    protected boolean compatCanDestroyBlock(ItemStack stack, net.minecraft.world.level.block.state.BlockState state,
                                             Level level, net.minecraft.core.BlockPos pos, LivingEntity user) {
        return false;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, dev.golem.core.world.CompatTooltipContext context,
                                         java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        for (int i = 0; i < 11; i++) tooltip.add(Component.translatable("item.hbm.gun_b92.desc." + i));
        tooltip.add(Component.translatable("item.hbm.gun_b92.legendary"));
    }

    public static int getAnim(ItemStack stack) {
        return getValue(stack, ANIMATION);
    }

    public static void setAnim(ItemStack stack, int value) {
        setValue(stack, ANIMATION, value);
    }

    public static int getPower(ItemStack stack) {
        return getValue(stack, ENERGY);
    }

    public static void setPower(ItemStack stack, int value) {
        setValue(stack, ENERGY, value);
    }

    public static float getRotationFromAnim(ItemStack stack) {
        float radians = (float) Math.toRadians(7.5F);
        int animation = getAnim(stack);
        if (animation < 10) return 0F;
        int i = animation - 10;
        if (i < 6) return radians * i;
        if (i > 14) return radians * (5 - (i - 15));
        return radians * 5;
    }

    public static float getTransFromAnim(ItemStack stack) {
        int animation = getAnim(stack);
        if (animation < 10) return 0F;
        int i = animation - 10;
        if (i > 4 && i < 10) return (i - 5) * 0.05F;
        if (i > 9 && i < 15) return (10 * 0.05F) - ((i - 5) * 0.05F);
        return 0F;
    }

    private static int getValue(ItemStack stack, String key) {
        CompoundTag tag = ModDataComponents.GUN_STATE.getOr(stack, new CompoundTag());
        return dev.golem.core.data.Tags.getIntOr(tag, key, 0);
    }

    private static void setValue(ItemStack stack, String key, int value) {
        CompoundTag current = ModDataComponents.GUN_STATE.getOr(stack, new CompoundTag());
        CompoundTag next = current.copy();
        next.putInt(key, value);
        ModDataComponents.GUN_STATE.set(stack, next);
    }
}
