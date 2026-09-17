package com.hbm.items.armor;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.factory.XFactoryPA;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class ArmorPAMeleeBase implements IPAMelee {
    protected static final float LIGHT_DAMAGE = 15F;
    protected static final float HEAVY_DAMAGE = 35F;

    protected abstract boolean isLightHit(int timer);
    protected abstract boolean isHeavyHit(int timer);
    protected abstract SoundEvent hitSound();
    protected abstract int lightCooldown();
    protected abstract int heavyCooldown();

    protected boolean refiresOnHold() { return false; }

    @Override public void clickPrimary(ItemStack stack, LambdaContext ctx) {
        XFactoryPA.doSwing(stack, ctx, GunAnimation.CYCLE, lightCooldown());
    }

    @Override public void clickSecondary(ItemStack stack, LambdaContext ctx) {
        XFactoryPA.doSwing(stack, ctx, GunAnimation.ALT_CYCLE, heavyCooldown());
    }

    @Override public void orchestra(ItemStack stack, LambdaContext ctx) {
        if (ctx.entity.level().isClientSide()) return;
        GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
        int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
        if (refiresOnHold() && type == GunAnimation.CYCLE && timer == lightCooldown()
                && ItemGunBaseNT.getPrimary(stack, 0)) clickPrimary(stack, ctx);

        boolean light = type == GunAnimation.CYCLE && isLightHit(timer);
        boolean heavy = type == GunAnimation.ALT_CYCLE && isHeavyHit(timer);
        Player player = ctx.getPlayer();
        if ((!light && !heavy) || player == null) return;
        HitResult hit = EntityDamageUtil.getMouseOver(player, 3D, 0.5D);
        if (hit instanceof EntityHitResult entityHit) hitEntity(player, entityHit.getEntity(), light);
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = player.level().getBlockState(pos);
            Vec3 at = hit.getLocation();
            player.level().playSound(null, at.x, at.y, at.z, state.getSoundType().getStepSound(),
                    SoundSource.PLAYERS, 2F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        }
    }

    private void hitEntity(Player player, Entity hit, boolean light) {
        float damage = light ? LIGHT_DAMAGE : HEAVY_DAMAGE;
        float threshold = light ? 5F : 15F;
        float piercing = light ? 0.1F : 0.25F;
        if (hit instanceof LivingEntity living) {
            if (living.getMaxHealth() >= 100F) damage *= 2.5F;
            Vec3 before = living.getDeltaMovement();
            living.invulnerableTime = 0;
            DamageResistanceHandler.hurt((ServerLevel) player.level(), living,
                    player.damageSources().playerAttack(player), damage, threshold, piercing);
            if (!light) {
                Vec3 delta = living.getDeltaMovement().subtract(before);
                living.setDeltaMovement(before.add(delta.scale(1.5D)));
                living.hurtMarked = true;
            }
        } else if (player.level() instanceof ServerLevel server) {
            Compat.hurt(hit, server, player.damageSources().playerAttack(player), damage);
        }
        player.level().playSound(null, hit.getX(), hit.getY(), hit.getZ(), hitSound(), SoundSource.PLAYERS,
                1F, 0.9F + player.getRandom().nextFloat() * 0.2F);
    }

    @Override public abstract BusAnimation playAnim(ItemStack stack, GunAnimation type);
}
