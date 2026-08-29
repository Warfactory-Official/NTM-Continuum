package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.function.BiConsumer;
import net.minecraft.world.item.ItemStack;

/** The Gen-3 .44 Magnum family, ported from upstream's {@code XFactory44}. */
public final class XFactory44 {

    private XFactory44() {}

    public static BulletConfig m44_bp;
    public static BulletConfig m44_sp;
    public static BulletConfig m44_fmj;
    public static BulletConfig m44_jhp;
    public static BulletConfig m44_ap;
    public static BulletConfig m44_express;
    public static BulletConfig m44_equestrian_pip;
    public static BulletConfig m44_equestrian_mn7;

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_HENRY =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_NOPIP =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_HANGMAN =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1F));

    /** Upstream's secondary-trigger inspection action for Hangman. */
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> SMACK_A_FUCKER = (stack, ctx) -> {
        if (ItemGunBaseNT.getState(stack, ctx.configIndex) == ItemGunBaseNT.GunState.IDLE
                || ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) == GunAnimation.CYCLE) {
            ItemGunBaseNT.setState(stack, ctx.configIndex, ItemGunBaseNT.GunState.DRAWING);
            ItemGunBaseNT.setTimer(stack, ctx.configIndex, ctx.config.getInspectDuration(stack));
            ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.INSPECT, ctx.configIndex);
        }
    };

    public static void init() {
        // Spent casing particles are not part of the shared projectile surface yet.
        m44_bp = BulletConfigs.register("m44_bp", new BulletConfig()
                .setItem(ModAmmoItems.m44_bp)
                .setDamage(0.75F).setBlackPowder(true));
        m44_sp = BulletConfigs.register("m44_sp", new BulletConfig()
                .setItem(ModAmmoItems.m44_sp));
        m44_fmj = BulletConfigs.register("m44_fmj", new BulletConfig()
                .setItem(ModAmmoItems.m44_fmj)
                .setDamage(0.8F).setThresholdNegation(3F).setArmorPiercing(0.1F));
        m44_jhp = BulletConfigs.register("m44_jhp", new BulletConfig()
                .setItem(ModAmmoItems.m44_jhp)
                .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F));
        m44_ap = BulletConfigs.register("m44_ap", new BulletConfig()
                .setItem(ModAmmoItems.m44_ap)
                .setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.5F).setThresholdNegation(7.5F).setArmorPiercing(0.15F));
        m44_express = BulletConfigs.register("m44_express", new BulletConfig()
                .setItem(ModAmmoItems.m44_express)
                .setDoesPenetrate(true).setDamage(1.5F)
                .setThresholdNegation(3F).setArmorPiercing(0.1F).setWear(1.5F));

        /*
         * Both upstream Equestrian configs consume the same metadata item but replace the hit with a
         * Boxcar or Torpedo entity. Those entity families are not in the new entity registry, so retain
         * the zero-damage configs and item binding without inventing a different impact effect.
         */
        m44_equestrian_pip = BulletConfigs.register("m44_equestrian_pip", new BulletConfig()
                .setItem(ModAmmoItems.m44_equestrian).setDamage(0F));
        m44_equestrian_mn7 = BulletConfigs.register("m44_equestrian_mn7", new BulletConfig()
                .setItem(ModAmmoItems.m44_equestrian).setDamage(0F));
    }

    /** Standard rounds in upstream declaration order, which is also magazine priority order. */
    public static BulletConfig[] all() {
        return new BulletConfig[] {m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express};
    }
}
