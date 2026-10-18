package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.function.BiConsumer;
import net.minecraft.world.item.ItemStack;

/** The four ordinary black-powder loads used by the Pepperbox. */
public final class XFactoryBlackPowder {

    public static final ConfigWrapper<Float> GUN_PEPPERBOX_DAMAGE = new ConfigWrapper<>(5F);

    public static void initConfig() {
        ServerConfig.configMap.put("GUN_PEPPERBOX_DAMAGE", GUN_PEPPERBOX_DAMAGE);
    }

    public static BulletConfig stone;
    public static BulletConfig flint;
    public static BulletConfig iron;
    public static BulletConfig shot;

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_PEPPERBOX =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));

    private XFactoryBlackPowder() {}

    public static void init() {
        stone = BulletConfigs.register("stone", new BulletConfig()
                .setItem(ModAmmoItems.stone).setBlackPowder(true).setHeadshot(1F)
                .setSpread(0.025F).setRicochetAngle(15));
        flint = BulletConfigs.register("stone_ap", new BulletConfig()
                .setItem(ModAmmoItems.stone_ap).setBlackPowder(true).setHeadshot(1F)
                .setSpread(0.01F).setRicochetAngle(5).setDoesPenetrate(true).setDamage(1.5F));
        iron = BulletConfigs.register("stone_iron", new BulletConfig()
                .setItem(ModAmmoItems.stone_iron).setBlackPowder(true).setHeadshot(1F)
                .setSpread(0F).setRicochetAngle(90).setRicochetCount(5)
                .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.5F));
        shot = BulletConfigs.register("stone_shot", new BulletConfig()
                .setItem(ModAmmoItems.stone_shot).setBlackPowder(true).setHeadshot(1F)
                .setSpread(0.1F).setRicochetAngle(45).setProjectiles(6, 6).setDamage(1F / 6F));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {stone, flint, iron, shot};
    }
}
