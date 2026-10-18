package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.sound.ModSounds;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.world.Compat;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Shared source bayonet inspection action. */
abstract class WeaponModBayonet extends WeaponModBase {
    WeaponModBayonet(int id) { super(id, "BAYONET"); }

    private void stab(Player player, LivingEntity entity) {
        HitResult hit = EntityDamageUtil.getMouseOver(player, 3D, 0.3D);
        if (hit instanceof EntityHitResult entityHit) {
            Entity victim = entityHit.getEntity();
            if (entity.level() instanceof ServerLevel level)
                Compat.hurt(victim, level, player.damageSources().playerAttack(player), stabDamage());
            victim.setDeltaMovement(victim.getDeltaMovement().multiply(2D, 1D, 2D));
            entity.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.GUN_STAB.get(),
                    SoundSource.PLAYERS, 1F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
        } else if (hit instanceof BlockHitResult blockHit) {
            BlockState state = entity.level().getBlockState(blockHit.getBlockPos());
            entity.level().playSound(null, blockHit.getLocation().x, blockHit.getLocation().y,
                    blockHit.getLocation().z, state.getSoundType().getStepSound(), SoundSource.BLOCKS,
                    2F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
        }
    }

    abstract float stabDamage();
    abstract BiFunction<ItemStack, GunAnimation, BusAnimation> inspectionAnimation();
    abstract BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> normalOrchestra();

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == GunConfig.FUN_ANIMNATIONS) {
            @SuppressWarnings("unchecked") BiFunction<ItemStack, GunAnimation, BusAnimation> original =
                    (BiFunction<ItemStack, GunAnimation, BusAnimation>) base;
            BiFunction<ItemStack, GunAnimation, BusAnimation> replacement = (stack, type) -> type == GunAnimation.INSPECT
                    ? inspectionAnimation().apply(stack, type)
                    : original == null ? null : original.apply(stack, type);
            return cast(replacement, base);
        }
        if (key == GunConfig.I_INSPECTDURATION) return cast(30, base);
        if (key == GunConfig.CON_ONPRESSSECONDARY) return cast(XFactory44.SMACK_A_FUCKER, base);
        if (key == GunConfig.CON_ORCHESTRA) {
            BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> replacement = (stack, ctx) -> {
                if (ctx.entity.level().isClientSide()) return;
                if (ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) == GunAnimation.INSPECT) {
                    if (ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex) == 15 && ctx.getPlayer() != null)
                        stab(ctx.getPlayer(), ctx.entity);
                    return;
                }
                normalOrchestra().accept(stack, ctx);
            };
            return cast(replacement, base);
        }
        if (key == GunConfig.I_INSPECTCANCEL) return cast(false, base);
        return base;
    }

    static BusAnimation stabAnimation() {
        return new BusAnimation().addBus("STAB", new BusAnimationSequence()
                .addPos(0, 1, -2, 250, IType.SIN_DOWN).hold(250)
                .addPos(0, 1, 5, 250, IType.SIN_UP).hold(250)
                .addPos(0, 0, 0, 500, IType.SIN_FULL));
    }
}
