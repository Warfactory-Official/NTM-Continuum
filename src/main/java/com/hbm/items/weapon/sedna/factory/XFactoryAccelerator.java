package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.entity.projectile.EntityCoin;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.BulletImpact;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.impl.ItemGunNI4NI;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineInfinite;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.sound.ModGunSounds;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The particle-accelerator calibres, copied from ntm-next's XFactoryAccelerator. Gun item registration
 * is kept in {@link NtmEnergyGunCatalog}; the public config names remain stable for turret callers.
 */
public final class XFactoryAccelerator {

    public static final MagazineBelt tauChargeMag = new MagazineBelt();

    public static BulletConfig tau_uranium;
    public static BulletConfig tau_uranium_charge;
    public static BulletConfig coil_tungsten;
    public static BulletConfig coil_ferrouranium;
    public static BulletConfig ni4ni_arc;

    public static final Consumer<Entity> LAMBDA_UPDATE_TUNGSTEN = entity -> breakInPath(entity, 1.25F);
    public static final Consumer<Entity> LAMBDA_UPDATE_FERRO = entity -> breakInPath(entity, 2.5F);

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_TAU_PRIMARY_RELEASE = (stack, ctx) -> {
        if (ctx.getPlayer() == null || ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) != GunAnimation.CYCLE) return;
        ctx.getPlayer().level().playSound(null, ctx.getPlayer().getX(), ctx.getPlayer().getY(), ctx.getPlayer().getZ(),
                ModGunSounds.TAU_RELEASE.get(), SoundSource.PLAYERS, 1F, 1F);
    };

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_TAU_SECONDARY_PRESS = (stack, ctx) -> {
        if (ctx.getPlayer() == null) return;
        if (ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) <= 0) return;
        ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.SPINUP, ctx.configIndex);
        MagazineBelt.getMagType(stack);
    };

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_TAU_SECONDARY_RELEASE = (stack, ctx) -> {
        if (ctx.getPlayer() == null) return;
        int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

        if (timer >= 10 && ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) == GunAnimation.SPINUP) {
            ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.ALT_CYCLE, ctx.configIndex);
            int unitsUsed = 1 + Math.min(12, timer / 10);

            LivingEntity entity = ctx.entity;
            int index = ctx.configIndex;
            Receiver primary = ctx.config.getReceivers(stack)[0];
            BulletConfig config = tauChargeMag.getFirstConfig(stack, ctx.inventory);
            if (config == null) return;

            Vec3 offset = primary.getProjectileOffset(stack);
            float damage = Lego.getStandardWearDamage(stack, ctx.config, index) * unitsUsed * 5;
            float spread = Lego.calcSpread(ctx, stack, primary, config, true, index, false);
            EntityBulletBeamBase mk4 = new EntityBulletBeamBase(entity, config, damage, spread,
                    offset.z, offset.y, offset.x);
            entity.level().addFreshEntity(mk4);

            ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + config.wear * unitsUsed,
                    ctx.config.getDurability(stack)));
        } else {
            ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.CYCLE_DRY, ctx.configIndex);
        }
    };

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_TAU = (stack, ctx) -> { };
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_COILGUN = (stack, ctx) ->
            ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_NI4NI_SECONDARY_PRESS = (stack, ctx) -> {
        Player player = ctx.getPlayer();
        if (player == null || ItemGunNI4NI.getCoinCount(stack) <= 0) return;
        Vec3 velocity = player.getLookAngle().scale(0.8D);
        EntityCoin coin = new EntityCoin(player.level());
        coin.setPos(player.getX(), player.getY() + player.getEyeHeight() - 0.5D - 0.125D, player.getZ());
        coin.setDeltaMovement(velocity.x, velocity.y + 0.5D, velocity.z);
        coin.setYRot(player.getYRot());
        coin.setThrower(player);
        player.level().addFreshEntity(coin);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1F,
                1F + player.getRandom().nextFloat() * 0.25F);
        ItemGunNI4NI.setCoinCount(stack, ItemGunNI4NI.getCoinCount(stack) - 1);
    };

    @SuppressWarnings("incomplete-switch")
    public static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_TAU_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP:
                return new BusAnimation().addBus("EQUIP", new BusAnimationSequence()
                        .addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE:
                return new BusAnimation().addBus("RECOIL", new BusAnimationSequence()
                                .addPos(0, 0, -0.5, 50).addPos(0, 0, 0, 150, IType.SIN_FULL))
                        .addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, -5, 50, IType.SIN_DOWN)
                                .addPos(0, 0, 5, 100, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
            case ALT_CYCLE:
                return new BusAnimation().addBus("RECOIL", new BusAnimationSequence()
                                .addPos(0, 0, -3, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, -5, 50, IType.SIN_DOWN)
                                .addPos(0, 0, 5, 100, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
            case CYCLE_DRY:
                return new BusAnimation();
            case INSPECT:
                return new BusAnimation().addBus("EQUIP", new BusAnimationSequence()
                                .addPos(2, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, -360 * 3, 500 * 3, IType.SIN_DOWN));
            case SPINUP:
                return new BusAnimation().addBus("ROTATE", new BusAnimationSequence()
                        .addPos(0, 0, 360 * 6, 3000, IType.SIN_UP).addPos(0, 0, 0, 0)
                        .addPos(0, 0, 360 * 40, 500 * 20));
        }
        return null;
    };

    public static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_COILGUN_ANIMS = (stack, type) -> {
        if (type == GunAnimation.EQUIP) return new BusAnimation().addBus("RELOAD",
                new BusAnimationSequence().addPos(1, 0, 0, 0).addPos(0, 0, 0, 250));
        if (type == GunAnimation.CYCLE) return new BusAnimation().addBus("RECOIL",
                new BusAnimationSequence().addPos(ItemGunBaseNT.getIsAiming(stack) ? 0.5 : 1, 0, 0, 100)
                        .addPos(0, 0, 0, 200));
        if (type == GunAnimation.RELOAD) return new BusAnimation().addBus("RELOAD",
                new BusAnimationSequence().addPos(1, 0, 0, 250).addPos(1, 0, 0, 500)
                        .addPos(0, 0, 0, 250));
        return null;
    };

    public static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_NI4NI_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP:
                return new BusAnimation().addBus("EQUIP", new BusAnimationSequence().addPos(-360 * 2, 0, 0, 500));
            case CYCLE:
                boolean aiming = ItemGunBaseNT.getIsAiming(stack);
                return new BusAnimation().addBus("RECOIL", new BusAnimationSequence()
                                .addPos(aiming ? -5 : -30, 0, 0, 100, IType.SIN_DOWN)
                                .addPos(0, 0, 0, 150, IType.SIN_FULL))
                        .addBus("DRUM", new BusAnimationSequence().hold(50)
                                .addPos(0, 0, 120, 300, IType.SIN_FULL));
            case INSPECT:
                return new BusAnimation().addBus("EQUIP", new BusAnimationSequence()
                        .addPos(-360 * 3, 0, 0, 750).hold(100).addPos(0, 0, 0, 750));
        }
        return null;
    };

    public static void breakInPath(Entity entity, float threshold) {
        Vec3 vec = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        double motion = Math.max(vec.length(), 0.1D);
        vec = vec.normalize();

        for (double d = 0; d < motion; d += 0.5D) {
            double dX = entity.getX() - vec.x * d;
            double dY = entity.getY() - vec.y * d;
            double dZ = entity.getZ() - vec.z * d;

            if (entity.level().isClientSide()) {
                entity.level().addParticle(ParticleTypes.FIREWORK, dX, dY, dZ, 0, 0, 0);
            } else {
                BlockPos pos = BlockPos.containing(dX, dY, dZ);
                BlockState state = entity.level().getBlockState(pos);
                float hardness = state.getDestroySpeed(entity.level(), pos);
                if (!state.isAir() && hardness >= 0 && hardness < threshold) {
                    entity.level().destroyBlock(pos, false);
                }
            }
        }
    }

    public static void init() {
        tau_uranium = BulletConfigs.register("tau_uranium", new BulletConfig()
                .setItem(ModAmmoItems.tau_uranium).setCasing(() -> new ItemStack(ModItems.plate_lead.get(), 2), 16)
                .setupDamageClass(DamageClass.SUBATOMIC).setBeam().setLife(5)
                .setRenderRotations(false).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setOnBeamImpact(BulletImpact::standardBeamHit));
        tau_uranium_charge = BulletConfigs.register("tau_uranium_charge", new BulletConfig()
                .setItem(ModAmmoItems.tau_uranium).setCasing(() -> new ItemStack(ModItems.plate_lead.get(), 2), 16)
                .setupDamageClass(DamageClass.SUBATOMIC).setBeam().setLife(5)
                .setRenderRotations(false).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setSpectral(true).setOnBeamImpact(BulletImpact::standardBeamHit));

        coil_tungsten = BulletConfigs.register("coil_tungsten", new BulletConfig()
                .setItem(SpecialGunCatalog.standardAmmo("coil_tungsten")).setVel(7.5F).setLife(50)
                .setDoesPenetrate(true).setDamageFalloffByPen(false).setSpectral(true)
                .setOnUpdate(LAMBDA_UPDATE_TUNGSTEN));
        coil_ferrouranium = BulletConfigs.register("coil_ferrouranium", new BulletConfig()
                .setItem(SpecialGunCatalog.standardAmmo("coil_ferrouranium")).setVel(7.5F).setLife(50)
                .setDoesPenetrate(true).setDamageFalloffByPen(false).setSpectral(true)
                .setOnUpdate(LAMBDA_UPDATE_FERRO));
        ni4ni_arc = BulletConfigs.register("ni4ni_arc", new BulletConfig()
                .setupDamageClass(DamageClass.PHYSICAL).setBeam().setLife(5)
                .setThresholdNegation(10F).setArmorPiercing(0.2F).setRenderRotations(false)
                .setDoesPenetrate(false).setOnBeamImpact(BulletImpact::standardBeamHit));
        tauChargeMag.addConfigs(tau_uranium_charge);
    }

    /** The callable NI4NI config, with the source's infinite arc and coin secondary. */
    public static GunConfig ni4niGunConfig() {
        return new GunConfig().dura(0).draw(5).inspect(39).crosshair(Crosshair.CIRCLE)
                .rec(new Receiver(0).dmg(35F).delay(10).sound(ModGunSounds.FIRE_COIL::get, 1.0F, 1.0F)
                        .mag(new MagazineInfinite(ni4ni_arc)).offset(0.75, -0.0625, -0.1875D)
                        .setupStandardFire().fire(Lego.LAMBDA_NOWEAR_FIRE))
                .ps(LAMBDA_NI4NI_SECONDARY_PRESS)
                .setupStandardConfiguration();
    }
}
