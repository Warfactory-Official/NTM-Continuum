package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.sound.ModGunSounds;
import java.util.function.BiConsumer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** The ordinary 5.56 mm family and G3-specific trigger behavior. */
public final class XFactory556mm {

    public static BulletConfig r556_sp;
    public static BulletConfig r556_fmj;
    public static BulletConfig r556_jhp;
    public static BulletConfig r556_ap;

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_G3 =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25),
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));

    /** If idle, switches the G3 between held automatic fire (mode 0) and one shot per press (mode 1). */
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_STANDARD_CLICK_SECONDARY =
            (stack, ctx) -> {
                int index = ctx.configIndex;
                if (ItemGunBaseNT.getState(stack, index) != GunState.IDLE) return;
                int mode = ItemGunBaseNT.getMode(stack, 0);
                ItemGunBaseNT.setMode(stack, index, 1 - mode);
                ctx.entity.level().playSound(null, ctx.entity.getX(), ctx.entity.getY(), ctx.entity.getZ(),
                        (mode == 0 ? ModGunSounds.SWITCH_MODE_1 : ModGunSounds.SWITCH_MODE_2).get(),
                        SoundSource.PLAYERS, 1F, 1F);
            };

    private XFactory556mm() {}

    public static void init() {
        r556_sp = BulletConfigs.register("r556_sp", new BulletConfig().setItem(ModAmmoItems.r556_sp));
        r556_fmj = BulletConfigs.register("r556_fmj", new BulletConfig()
                .setItem(ModAmmoItems.r556_fmj).setDamage(0.8F)
                .setThresholdNegation(4F).setArmorPiercing(0.1F));
        r556_jhp = BulletConfigs.register("r556_jhp", new BulletConfig()
                .setItem(ModAmmoItems.r556_jhp).setDamage(1.5F)
                .setHeadshot(1.5F).setArmorPiercing(-0.25F));
        r556_ap = BulletConfigs.register("r556_ap", new BulletConfig()
                .setItem(ModAmmoItems.r556_ap).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.5F).setThresholdNegation(10F).setArmorPiercing(0.15F));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {r556_sp, r556_fmj, r556_jhp, r556_ap};
    }
}
