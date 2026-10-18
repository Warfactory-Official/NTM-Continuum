package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModItems;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.lib.ModDamageSources.DamageClass;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.BiConsumer;

/** The source copy of the 7.62 mm and Lacunae rounds, adapted to flat ammo/Core registration. */
public final class XFactory762mm {

    public static final ConfigWrapper<Float> GUN_CARBINE_DAMAGE = new ConfigWrapper<>(15F);
    public static final ConfigWrapper<Float> GUN_CARBINE_BAYONET_DAMAGE = new ConfigWrapper<>(15F);
    public static final ConfigWrapper<Float> GUN_MINIGUN_DAMAGE = new ConfigWrapper<>(6F);
    public static final ConfigWrapper<Float> GUN_LACUNAE_DAMAGE = new ConfigWrapper<>(12F);
    public static final ConfigWrapper<Float> GUN_MINIGUN_DUAL_LEFT_DAMAGE = new ConfigWrapper<>(6F);
    public static final ConfigWrapper<Float> GUN_MINIGUN_DUAL_RIGHT_DAMAGE = new ConfigWrapper<>(6F);
    public static final ConfigWrapper<Float> GUN_SOUTH_STAR_DAMAGE = new ConfigWrapper<>(30F);
    public static final ConfigWrapper<Float> GUN_SOUTH_STAR_BAYONET_DAMAGE = new ConfigWrapper<>(15F);

    public static void initConfig() {
        ServerConfig.configMap.put("GUN_CARBINE_DAMAGE", GUN_CARBINE_DAMAGE);
        ServerConfig.configMap.put("GUN_CARBINE_BAYONET_DAMAGE", GUN_CARBINE_BAYONET_DAMAGE);
        ServerConfig.configMap.put("GUN_MINIGUN_DAMAGE", GUN_MINIGUN_DAMAGE);
        ServerConfig.configMap.put("GUN_LACUNAE_DAMAGE", GUN_LACUNAE_DAMAGE);
        ServerConfig.configMap.put("GUN_MINIGUN_DUAL_LEFT_DAMAGE", GUN_MINIGUN_DUAL_LEFT_DAMAGE);
        ServerConfig.configMap.put("GUN_MINIGUN_DUAL_RIGHT_DAMAGE", GUN_MINIGUN_DUAL_RIGHT_DAMAGE);
        ServerConfig.configMap.put("GUN_SOUTH_STAR_DAMAGE", GUN_SOUTH_STAR_DAMAGE);
        ServerConfig.configMap.put("GUN_SOUTH_STAR_BAYONET_DAMAGE", GUN_SOUTH_STAR_BAYONET_DAMAGE);
    }

    public static BulletConfig r762_sp;
    public static BulletConfig r762_fmj;
    public static BulletConfig r762_jhp;
    public static BulletConfig r762_ap;
    public static BulletConfig r762_du;
    public static BulletConfig r762_he;

    public static BulletConfig energy_lacunae;
    public static BulletConfig energy_lacunae_overcharge;
    public static BulletConfig energy_lacunae_ir;

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_TINY_EXPLODE = (bullet, hit) -> {
        if (hit instanceof EntityHitResult entityHit && bullet.tickCount < 3
                && entityHit.getEntity() == bullet.getThrower()) return;
        Lego.tinyExplode(bullet, hit, 1.5F);
        bullet.discard();
    };

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_SECOND_MINIGUN = (stack, ctx) -> {
        int index = ctx.configIndex;
        GunState lastState = ItemGunBaseNT.getState(stack, index);
        GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
        GunStateDecider.deciderStandardClearJam(stack, lastState, index);
        GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
        GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 0, index,
                () -> ItemGunBaseNT.getSecondary(stack, index) && ItemGunBaseNT.getMode(stack, index) == 0);
    };

    /** Source XFactory762mm smoke cadence and offset, kept separate from the generic smoke helper. */
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE = (stack, ctx) ->
            Lego.handleStandardSmoke(ctx.entity, stack, 1500, 0.075D, 1.1D, ctx.hand, ctx.configIndex);

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_CARBINE = (stack, ctx) ->
            ItemGunBaseNT.setupRecoil(5F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_MINIGUN = (stack, ctx) ->
            ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5),
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_LACUNAE = (stack, ctx) -> { };

    public static void init() {
        if (r762_sp != null) return;
        r762_sp = BulletConfigs.register("r762_sp", new BulletConfig().setItem(ModAmmoItems.r762_sp)
                .setCasing(EnumCasingType.SMALL, 6));
        r762_fmj = BulletConfigs.register("r762_fmj", new BulletConfig().setItem(ModAmmoItems.r762_fmj)
                .setCasing(EnumCasingType.SMALL, 6).setDamage(0.8F).setThresholdNegation(5F)
                .setArmorPiercing(0.1F));
        r762_jhp = BulletConfigs.register("r762_jhp", new BulletConfig().setItem(ModAmmoItems.r762_jhp)
                .setCasing(EnumCasingType.SMALL, 6).setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F));
        r762_ap = BulletConfigs.register("r762_ap", new BulletConfig().setItem(ModAmmoItems.r762_ap)
                .setCasing(EnumCasingType.SMALL_STEEL, 6).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.25F).setThresholdNegation(12.5F).setArmorPiercing(0.15F));
        r762_du = BulletConfigs.register("r762_du", new BulletConfig().setItem(ModAmmoItems.r762_du)
                .setCasing(EnumCasingType.SMALL_STEEL, 6).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.5F).setThresholdNegation(15F).setArmorPiercing(0.25F));
        r762_he = BulletConfigs.register("r762_he", new BulletConfig().setItem(ModAmmoItems.r762_he)
                .setCasing(EnumCasingType.SMALL_STEEL, 6).setWear(3F).setDamage(1.75F)
                .setOnImpact(LAMBDA_TINY_EXPLODE));

        energy_lacunae = BulletConfigs.register("energy_lacunae", new BulletConfig()
                .setItem(ModAmmoItems.capacitor).setCasing(() -> new ItemStack(ModItems.ingot_polymer.get(), 2), 4 * 40)
                .setupDamageClass(DamageClass.LASER).setBeam().setReloadCount(40).setSpread(0F).setLife(5)
                .setRenderRotations(false).setOnBeamImpact(XFactoryEnergy.LAMBDA_STANDARD_BEAM_HIT));
        energy_lacunae_overcharge = BulletConfigs.register("energy_lacunae_overcharge", new BulletConfig()
                .setItem(ModAmmoItems.capacitor_overcharge)
                .setCasing(() -> new ItemStack(ModItems.ingot_polymer.get(), 2), 4 * 40)
                .setupDamageClass(DamageClass.LASER).setBeam().setReloadCount(40).setSpread(0F).setLife(5)
                .setRenderRotations(false).setDoesPenetrate(true)
                .setOnBeamImpact(XFactoryEnergy.LAMBDA_STANDARD_BEAM_HIT));
        energy_lacunae_ir = BulletConfigs.register("energy_lacunae_ir", new BulletConfig()
                .setItem(ModAmmoItems.capacitor_ir).setCasing(() -> new ItemStack(ModItems.ingot_polymer.get(), 2), 4 * 40)
                .setupDamageClass(DamageClass.FIRE).setBeam().setReloadCount(40).setSpread(0F).setLife(5)
                .setRenderRotations(false).setOnBeamImpact(XFactoryEnergy.LAMBDA_IR_HIT));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he};
    }
}
