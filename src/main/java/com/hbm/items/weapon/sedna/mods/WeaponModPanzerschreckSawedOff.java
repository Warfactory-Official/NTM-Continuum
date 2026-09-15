package com.hbm.items.weapon.sedna.mods;

import com.hbm.capability.HbmLivingProps;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;

/** Panzerschreck shield removal callback, including its incendiary fire action. */
public final class WeaponModPanzerschreckSawedOff extends WeaponModBase {
    private static final BiFunction<ItemStack, GunAnimation, BusAnimation> ANIMS = (stack, type) ->
            type == GunAnimation.EQUIP
                    ? new BusAnimation().addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 250, IType.SIN_DOWN))
                    : new BusAnimation();

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> FIRE = (stack, ctx) -> {
        Lego.LAMBDA_STANDARD_FIRE.accept(stack, ctx);
        if (ctx.entity != null) {
            HbmLivingProps.getData(ctx.entity).fire += 100;
            if (ctx.entity.level() instanceof net.minecraft.server.level.ServerLevel level)
                EntityDamageUtil.attackEntityFromIgnoreIFrame(ctx.entity,
                        ModDamageSources.bullet(level, ModDamageSources.DamageClass.FIRE.type(), ctx.entity, ctx.entity), 4F);
        }
    };

    public WeaponModPanzerschreckSawedOff(int id) { super(id, "SHIELD"); }

    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == GunConfig.I_DRAWDURATION) return cast(5, base);
        if (key == Receiver.CON_ONFIRE) return cast(FIRE, base);
        if (key == GunConfig.FUN_ANIMNATIONS) {
            @SuppressWarnings("unchecked") BiFunction<ItemStack, GunAnimation, BusAnimation> original =
                    (BiFunction<ItemStack, GunAnimation, BusAnimation>) base;
            BiFunction<ItemStack, GunAnimation, BusAnimation> replacement = (stack, type) -> type == GunAnimation.EQUIP
                    ? ANIMS.apply(stack, type)
                    : original == null ? new BusAnimation() : original.apply(stack, type);
            return cast(replacement, base);
        }
        return base;
    }
}
