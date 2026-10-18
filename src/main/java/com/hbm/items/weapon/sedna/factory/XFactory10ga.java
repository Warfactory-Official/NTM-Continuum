package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.sound.ModGunSounds;
import java.util.function.BiConsumer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** The conventional 10 gauge rounds from 1.7's XFactory10ga. */
public final class XFactory10ga {

    public static final ConfigWrapper<Float> GUN_DOUBLE_BARREL_DAMAGE = new ConfigWrapper<>(30F);
    public static final ConfigWrapper<Float> GUN_SACRED_DRAGON_DAMAGE = new ConfigWrapper<>(45F);
    public static final ConfigWrapper<Float> GUN_HERETIC_DAMAGE = new ConfigWrapper<>(100F);

    public static void initConfig() {
        ServerConfig.configMap.put("GUN_DOUBLE_BARREL_DAMAGE", GUN_DOUBLE_BARREL_DAMAGE);
        ServerConfig.configMap.put("GUN_SACRED_DRAGON_DAMAGE", GUN_SACRED_DRAGON_DAMAGE);
        ServerConfig.configMap.put("GUN_HERETIC_DAMAGE", GUN_HERETIC_DAMAGE);
    }

    public static BulletConfig g10;
    public static BulletConfig g10_shrapnel;
    public static BulletConfig g10_du;
    public static BulletConfig g10_slug;
    public static BulletConfig g10_explosive;
    /** The jade shotgun's 10 gauge DU, see {@link #jadeConfig}. */
    public static BulletConfig g10_jade;

    private XFactory10ga() {}

    public static void init() {
        if (g10 != null) return;
        float spread = 0.035F;
        g10 = BulletConfigs.register("g10", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g10"))
                .setProjectiles(10).setDamage(1F / 10F).setSpread(spread)
                .setRicochetAngle(15F).setThresholdNegation(5F));
        g10_shrapnel = BulletConfigs.register("g10_shrapnel", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g10_shrapnel"))
                .setProjectiles(10).setDamage(1F / 10F).setSpread(spread)
                .setRicochetAngle(90F).setRicochetCount(15).setThresholdNegation(5F));
        g10_du = BulletConfigs.register("g10_du", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g10_du"))
                .setProjectiles(10).setDamage(1F / 4F).setSpread(spread)
                .setRicochetAngle(15F).setThresholdNegation(10F)
                .setArmorPiercing(0.2F).setDoesPenetrate(true).setDamageFalloffByPen(false));
        g10_slug = BulletConfigs.register("g10_slug", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g10_slug"))
                .setRicochetAngle(15F).setThresholdNegation(10F)
                .setArmorPiercing(0.1F).setDoesPenetrate(true));
        g10_explosive = BulletConfigs.register("g10_explosive", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g10_explosive"))
                .setWear(3F).setProjectiles(10).setDamage(1F / 4F).setSpread(spread)
                .setOnImpact((bullet, hit) -> {
                    if (hit instanceof EntityHitResult entityHit && bullet.tickCount < 3
                            && entityHit.getEntity() == bullet.getThrower()) return;
                    Lego.tinyExplode(bullet, hit, 1.5F);
                    bullet.discard();
                }));
        // port: 1.12.2's jade shotgun loaded only 12 gauge DU, which the Sedna rounds have only as 10 gauge. Its
        // pellets were standard buckshot moving 5 blocks along a heading doubled by shoot(); damage and life are the gun's.
        g10_jade = BulletConfigs.register("g10_jade", g10_du.copy()
                .setVel(10F).setSpread(0F).setWear(20F).setProjectiles(8, 12).setLife(100).setRenderRotations(false));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {g10, g10_shrapnel, g10_du, g10_slug, g10_explosive};
    }

    /** The secondary trigger fires one barrel; primary uses the receiver's two-round cycle. */
    public static final BiConsumer<ItemStack,
            ItemGunBaseNT.LambdaContext> LAMBDA_DOUBLE_SECONDARY = (stack, ctx) -> {
        var receiver = ctx.config.getReceivers(stack)[0];
        var state = ItemGunBaseNT.getState(stack, ctx.configIndex);
        if (state == ItemGunBaseNT.GunState.IDLE) {
            if (receiver.getCanFire(stack).apply(stack, ctx)) {
                receiver.getOnFire(stack).accept(stack, ctx);
                if (receiver.getFireSound(stack) != null) {
                    ctx.entity.level().playSound(null, ctx.entity.getX(), ctx.entity.getY(), ctx.entity.getZ(),
                            receiver.getFireSound(stack), SoundSource.PLAYERS,
                            receiver.getFireVolume(stack), receiver.getFirePitch(stack));
                }
                ItemGunBaseNT.setState(stack, ctx.configIndex,
                        ItemGunBaseNT.GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, ctx.configIndex,
                        receiver.getDelayAfterFire(stack));
            } else if (receiver.getDoesDryFire(stack)) {
                ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack,
                        GunAnimation.CYCLE_DRY, ctx.configIndex);
                ItemGunBaseNT.setState(stack, ctx.configIndex,
                        ItemGunBaseNT.GunState.DRAWING);
                ItemGunBaseNT.setTimer(stack, ctx.configIndex,
                        receiver.getDelayAfterDryFire(stack));
            }
        }
        if (state == ItemGunBaseNT.GunState.RELOADING) {
            ItemGunBaseNT.setReloadCancel(stack, true);
        }
    };

    /** 1.12.2 {@code EntityBulletBase.shoot}'s gaussian 0.05 per axis, in {@code EntityBulletBaseMK4.shoot}'s units. */
    private static final float JADE_SPREAD = 0.05F / 0.0075F;

    /**
     * 1.12.2 {@code Gun12GaugeFactory.getJShotgunConfig} with {@code ItemGunJShotty}: two shells loaded one at a time
     * (1.12's single reload took a shell every 31 ticks after the first 32), eight ticks between shots, no dry fire,
     * and the flashlight on the secondary button.
     */
    public static GunConfig jadeConfig() {
        return new GunConfig().dura(3_000)
                .rec(new Receiver(0).delay(8).dryfire(false).reload(0, 32, 31, 0, 0)
                        .sound(ModGunSounds.FIRE_SHOTTY::get, 1F, 1F)
                        .mag(new MagazineSingleReload(0, 2).addConfigs(g10_jade))
                        .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(LAMBDA_JADE_FIRE))
                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).ps(LAMBDA_JADE_FLASHLIGHT).pr(Lego.LAMBDA_STANDARD_RELOAD)
                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER);
    }

    /**
     * 1.12.2 {@code ItemGunBase.fire} as {@code ItemGunJShotty} overrode it: 8 to 12 pellets from 0.16 to the side of
     * and 0.1 below the eye, unturned by pitch, each with a damage of 4 to 10 and a life of 4 to 6 ticks.
     */
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_JADE_FIRE = (stack, ctx) -> {
        LivingEntity entity = ctx.entity;
        Level level = entity.level();
        ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.CYCLE, ctx.configIndex);
        IMagazine<BulletConfig> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
        BulletConfig config = mag.getType(stack, ctx.inventory);
        if (config == null) return;

        float yaw = entity.getYRot() / 180F * Mth.PI;
        float pitch = entity.getXRot() / 180F * Mth.PI;
        float side = ctx.hand == InteractionHand.MAIN_HAND ? -0.16F : 0.16F;
        double x = entity.getX() + Mth.cos(yaw) * side;
        double y = entity.getEyeY() - 0.10000000149011612D;
        double z = entity.getZ() + Mth.sin(yaw) * side;
        double motionX = -Mth.sin(yaw) * Mth.cos(pitch);
        double motionZ = Mth.cos(yaw) * Mth.cos(pitch);
        double motionY = -Mth.sin(pitch);

        RandomSource rand = entity.getRandom();
        int pellets = config.projectilesMin + rand.nextInt(config.projectilesMax - config.projectilesMin + 1);
        for (int i = 0; i < pellets; i++) {
            EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level, entity, config, 4 + rand.nextInt(7), JADE_SPREAD,
                    x, y, z, motionX, motionY, motionZ);
            bullet.expiresOverride = 4 + rand.nextInt(3);
            level.addFreshEntity(bullet);
        }
        mag.useUpAmmo(stack, ctx.inventory, 1);
        ItemGunBaseNT.setWear(stack, ctx.configIndex, Math.min(ItemGunBaseNT.getWear(stack, ctx.configIndex) + config.wear,
                ctx.config.getDurability(stack)));
    };

    public static final String KEY_FLASHLIGHT = "flashlight";

    /** 1.12.2 {@code ItemGunJShotty.onItemRightClick}: the flashlight toggles with a lever click. */
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_JADE_FLASHLIGHT = (stack, ctx) -> {
        setFlashlightActive(stack, !getFlashlightActive(stack));
        LivingEntity entity = ctx.entity;
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.LEVER_CLICK,
                SoundSource.PLAYERS, 1F, 1F);
    };

    public static void setFlashlightActive(ItemStack stack, boolean active) {
        ItemGunBaseNT.setValueBool(stack, KEY_FLASHLIGHT, active);
    }

    public static boolean getFlashlightActive(ItemStack stack) {
        return ItemGunBaseNT.getValueBool(stack, KEY_FLASHLIGHT);
    }
}
