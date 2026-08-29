package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.function.BiConsumer;
import net.minecraft.world.item.ItemStack;

/** The Gen-3 9 mm rounds, ported from upstream's {@code XFactory9mm}. */
public final class XFactory9mm {

    private XFactory9mm() {}

    public static BulletConfig p9_sp;
    public static BulletConfig p9_fmj;
    public static BulletConfig p9_jhp;
    public static BulletConfig p9_ap;

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_GREASEGUN =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(2F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_LAG =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_UZI =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(1F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));

    public static void init() {
        // Spent casings are not part of the shared projectile surface yet.
        p9_sp = BulletConfigs.register("p9_sp", new BulletConfig()
                .setItem(ModAmmoItems.p9_sp));
        p9_fmj = BulletConfigs.register("p9_fmj", new BulletConfig()
                .setItem(ModAmmoItems.p9_fmj)
                .setDamage(0.8F).setThresholdNegation(2F).setArmorPiercing(0.1F));
        p9_jhp = BulletConfigs.register("p9_jhp", new BulletConfig()
                .setItem(ModAmmoItems.p9_jhp)
                .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F));
        p9_ap = BulletConfigs.register("p9_ap", new BulletConfig()
                .setItem(ModAmmoItems.p9_ap)
                .setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.5F).setThresholdNegation(5F).setArmorPiercing(0.15F));
    }

    /** Upstream declaration order, which is also magazine reload priority. */
    public static BulletConfig[] all() {
        return new BulletConfig[] {p9_sp, p9_fmj, p9_jhp, p9_ap};
    }

    /** The second akimbo receiver listens to the secondary trigger, not the primary one. */
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_SECOND_UZI = (stack, ctx) -> {
        int index = ctx.configIndex;
        ItemGunBaseNT.GunState lastState = ItemGunBaseNT.getState(stack, index);
        GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
        GunStateDecider.deciderStandardClearJam(stack, lastState, index);
        GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
        GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 0, index,
                () -> ItemGunBaseNT.getSecondary(stack, index) && ItemGunBaseNT.getMode(stack, index) == 0);
    };
}
