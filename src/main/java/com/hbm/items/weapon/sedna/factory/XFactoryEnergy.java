package com.hbm.items.weapon.sedna.factory;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.BulletImpact;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.mixin.FireBlockAccessor;
import com.hbm.sound.ModGunSounds;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;

/**
 * The energy and laser calibres. The declarations and animation tables follow ntm-next's
 * {@code XFactoryEnergy}; registry-backed gun construction lives in {@link NtmEnergyGunCatalog}.
 */
public final class XFactoryEnergy {

    public static BulletConfig energy_tesla;
    public static BulletConfig energy_tesla_overcharge;
    public static BulletConfig energy_tesla_ir;
    public static BulletConfig energy_tesla_ir_sub;

    public static BulletConfig energy_las;
    public static BulletConfig energy_las_overcharge;
    public static BulletConfig energy_las_ir;
    public static BulletConfig energy_emerald;
    public static BulletConfig energy_emerald_overcharge;
    public static BulletConfig energy_emerald_ir;

    public static final BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_LIGHTNING_HIT = (beam, mop) -> {
        Vec3 hit = mop.getLocation();
        if (mop instanceof BlockHitResult blockHit) {
            Direction dir = blockHit.getDirection();
            hit = hit.add(dir.getStepX() * 0.5, dir.getStepY() * 0.5, dir.getStepZ() * 0.5);
        }

        ExplosionVNT vnt = new ExplosionVNT(beam.level(), hit.x, hit.y, hit.z, 2F, beam.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, beam.damage).setDamageClass(beam.config.dmgClass));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        beam.level().playSound(null, hit.x, hit.y, hit.z, ModGunSounds.TESLA_BLAST.get(),
                SoundSource.PLAYERS, 5.0F,
                0.9F + beam.level().getRandom().nextFloat() * 0.2F);

        if (mop instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 60, 9));
            living.addEffect(new MobEffectInstance(
                    MobEffects.MINING_FATIGUE, 60, 9));
        }
    };

    public static final BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_STANDARD_BEAM_HIT =
            BulletImpact::standardBeamHit;

    public static final BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_LIGHTNING_SPLIT = (beam, mop) -> {
        LAMBDA_LIGHTNING_HIT.accept(beam, mop);
        if (!(mop instanceof EntityHitResult primaryHit)) return;

        Vec3 hit = mop.getLocation();
        double range = 20D;
        List<LivingEntity> potentialTargets = beam.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(hit.x, hit.y, hit.z, hit.x, hit.y, hit.z).inflate(range, range, range));
        Collections.shuffle(potentialTargets);

        for (LivingEntity target : potentialTargets) {
            if (target == beam.getThrower() || target == primaryHit.getEntity()) continue;
            Vec3 delta = new Vec3(target.getX() - hit.x, target.getY() + target.getBbHeight() / 2D - hit.y,
                    target.getZ() - hit.z);
            if (delta.length() > range) continue;

            EntityBulletBeamBase sub = new EntityBulletBeamBase(beam.level(), energy_tesla_ir_sub, beam.damage);
            sub.thrower = beam.getThrower();
            sub.setPos(hit);
            sub.setRotationsFromVector(delta);
            sub.setImpactCallback(LAMBDA_STANDARD_BEAM_HIT);
            sub.performHitscanExternal(delta.length());
            beam.level().addFreshEntity(sub);
        }
    };

    /** Standard beam hit followed by the old typed-fire and block-fire behavior. */
    public static final BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_IR_HIT = (beam, mop) -> {
        BulletImpact.standardBeamHit(beam, mop);

        if (mop instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            HbmLivingProps.setFireAtLeast(living, 100);
        }

        if (mop instanceof BlockHitResult blockHit) {
            Level world = beam.level();
            BlockPos pos = blockHit.getBlockPos();
            Direction dir = blockHit.getDirection();
            BlockState state = world.getBlockState(pos);
            BlockPos firePos = pos.relative(dir);
            if (((FireBlockAccessor) Blocks.FIRE).hbm$getBurnOdds(state) > 0 && world.getBlockState(firePos).isAir()) {
                world.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                return;
            }

            EntityFireLingering fire = new EntityFireLingering(world).setArea(2, 1).setDuration(100)
                    .setFireType(EntityFireLingering.TYPE_DIESEL);
            fire.setPos(mop.getLocation());
            world.addFreshEntity(fire);
        }
    };

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_ENERGY =
            (stack, ctx) -> { };

    /* Kept as callable GEM tables; client registration uses GunAnimations, not a common renderer hook. */
    @SuppressWarnings("incomplete-switch")
    public static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_TESLA_ANIMS = (stack, type) -> {
        int amount = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0]
                .getMagazine(stack).getAmount(stack, null);
        switch (type) {
            case EQUIP:
                return new BusAnimation().addBus("EQUIP",
                        new BusAnimationSequence().addPos(60, 0, 0, 0)
                                .addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case CYCLE:
                return new BusAnimation()
                        .addBus("RECOIL", new BusAnimationSequence()
                                .addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 100, IType.SIN_DOWN)
                                .addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 0, 150)
                                .addPos(0, 0, 22.5, 350))
                        .addBus("COUNT", new BusAnimationSequence().addPos(amount, 0, 0, 0));
            case CYCLE_DRY:
                return new BusAnimation().addBus("CYCLE", new BusAnimationSequence()
                        .addPos(0, 0, 0, 150).addPos(0, 0, 22.5, 350));
            case INSPECT:
                return new BusAnimation()
                        .addBus("YOMI", new BusAnimationSequence().addPos(8, -4, 0, 0)
                                .addPos(4, -1, 0, 500, IType.SIN_DOWN).addPos(4, -1, 0, 1000)
                                .addPos(6, -6, 0, 500, IType.SIN_UP))
                        .addBus("SQUEEZE", new BusAnimationSequence().addPos(1, 1, 1, 0)
                                .addPos(1, 1, 1, 750).addPos(1, 1, 0.5, 125)
                                .addPos(1, 1, 1, 125));
        }
        return null;
    };

    @SuppressWarnings("incomplete-switch")
    public static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LASER_PISTOL = (stack, type) -> {
        switch (type) {
            case EQUIP:
                return new BusAnimation().addBus("EQUIP", new BusAnimationSequence()
                        .addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE:
                return new BusAnimation().addBus("RECOIL", new BusAnimationSequence()
                        .addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL));
            case RELOAD:
                return new BusAnimation()
                        .addBus("LATCH", new BusAnimationSequence().addPos(0, -20, 0, 100)
                                .hold(1900).addPos(0, 0, 0, 100))
                        .addBus("LIFT", new BusAnimationSequence().hold(100)
                                .addPos(-45, 0, 0, 250, IType.SIN_FULL).hold(500)
                                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("JOLT", new BusAnimationSequence().hold(350)
                                .addPos(0, 0, 0.5, 100, IType.SIN_FULL)
                                .addPos(0, 0, -1.5, 100, IType.SIN_UP)
                                .addPos(0, 0, 0, 150, IType.SIN_FULL).holdUntil(2100)
                                .addPos(-0.0625, 0, 0, 50, IType.SIN_UP)
                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("BATTERY", new BusAnimationSequence().hold(550)
                                .addPos(0, 0, 5, 250).hold(550).setPos(0, -2, -2)
                                .addPos(0, 0, -2, 250, IType.SIN_FULL)
                                .addPos(0, 0, 0, 250, IType.SIN_UP));
            case JAMMED:
                return new BusAnimation().addBus("LATCH", new BusAnimationSequence()
                                .hold(500).addPos(0, -20, 0, 100).hold(250).addPos(0, 0, 0, 100))
                        .addBus("JOLT", new BusAnimationSequence().hold(950)
                                .addPos(-0.0625, 0, 0, 50, IType.SIN_UP)
                                .addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("EQUIP", new BusAnimationSequence().hold(1500)
                                .addPos(7.5, 0, 0, 100, IType.SIN_DOWN)
                                .addPos(0, 0, 0, 250, IType.SIN_FULL));
            case INSPECT:
                return new BusAnimation().addBus("SWIRL", new BusAnimationSequence()
                        .addPos(-720, 0, 0, 750, IType.SIN_FULL).hold(500)
                        .addPos(0, 0, 0, 750, IType.SIN_FULL));
        }
        return null;
    };

    @SuppressWarnings("incomplete-switch")
    public static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LASRIFLE = (stack, type) -> {
        switch (type) {
            case EQUIP:
                return new BusAnimation().addBus("EQUIP", new BusAnimationSequence()
                        .addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE:
                return new BusAnimation().addBus("RECOIL", new BusAnimationSequence()
                        .addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL));
            case RELOAD:
                return new BusAnimation()
                        .addBus("LEVER", new BusAnimationSequence().addPos(-90, 0, 0, 350, IType.SIN_UP)
                                .addPos(-90, 0, 0, 1500).addPos(0, 0, 0, 350, IType.SIN_UP))
                        .addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 350)
                                .addPos(0, -5, 0, 350, IType.SIN_UP).addPos(0, -5, 0, 500)
                                .addPos(0, -0.25, 0, 500, IType.SIN_FULL)
                                .addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                        .addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 1700)
                                .addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
            case JAMMED:
                return new BusAnimation().addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 500)
                                .addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 600)
                                .addPos(0, 0, 0, 350, IType.SIN_UP))
                        .addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 500)
                                .addPos(0, 0, 0, 350).addPos(0, -2, 0, 200, IType.SIN_UP)
                                .addPos(0, -0.25, 0, 250, IType.SIN_FULL)
                                .addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                        .addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 500)
                                .addPos(0, 0, 0, 800).addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
            case INSPECT:
                return new BusAnimation().addBus("LEVER", new BusAnimationSequence()
                                .addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 600)
                                .addPos(0, 0, 0, 350, IType.SIN_UP))
                        .addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 350)
                                .addPos(0, -2, 0, 200, IType.SIN_UP)
                                .addPos(0, -0.25, 0, 250, IType.SIN_FULL)
                                .addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                        .addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 800)
                                .addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                                .addPos(0, 0, 0, 100, IType.SIN_FULL));
        }
        return null;
    };

    private static ItemStack ingotPolymer(int count) {
        return new ItemStack(ModItems.ingot_polymer.get(), count);
    }

    private static BulletConfig beam(String id, String ammo, DamageClass type) {
        return BulletConfigs.register(id, new BulletConfig()
                .setItem(SpecialGunCatalog.standardAmmo(ammo)).setupDamageClass(type)
                .setBeam().setSpread(0F).setLife(5).setRenderRotations(false));
    }

    public static void init() {
        energy_tesla = beam("energy_tesla", "capacitor", DamageClass.ELECTRIC)
                .setCasing(() -> ingotPolymer(2), 4).setDoesPenetrate(true)
                .setOnBeamImpact(LAMBDA_LIGHTNING_HIT);
        energy_tesla_overcharge = beam("energy_tesla_overcharge", "capacitor_overcharge", DamageClass.ELECTRIC)
                .setCasing(() -> ingotPolymer(2), 4).setDoesPenetrate(true).setDamage(1.5F)
                .setOnBeamImpact(LAMBDA_LIGHTNING_HIT);
        energy_tesla_ir = beam("energy_tesla_ir", "capacitor_ir", DamageClass.ELECTRIC)
                .setCasing(() -> ingotPolymer(2), 4).setDamage(0.8F)
                .setOnBeamImpact(LAMBDA_LIGHTNING_SPLIT);
        energy_tesla_ir_sub = beam("energy_tesla_ir_sub", "capacitor_ir", DamageClass.ELECTRIC)
                .setLife(3).setWear(3F).setDoesPenetrate(true).setDamage(0.5F)
                .setOnBeamImpact(LAMBDA_STANDARD_BEAM_HIT);

        energy_las = beam("energy_las", "capacitor", DamageClass.LASER)
                .setCasing(() -> ingotPolymer(2), 4).setOnBeamImpact(LAMBDA_STANDARD_BEAM_HIT);
        energy_las_overcharge = beam("energy_las_overcharge", "capacitor_overcharge", DamageClass.LASER)
                .setCasing(() -> ingotPolymer(2), 4).setDoesPenetrate(true)
                .setOnBeamImpact(LAMBDA_STANDARD_BEAM_HIT);
        energy_las_ir = beam("energy_las_ir", "capacitor_ir", DamageClass.FIRE)
                .setCasing(() -> ingotPolymer(2), 4).setOnBeamImpact(LAMBDA_IR_HIT);

        energy_emerald = beam("energy_emerald", "capacitor", DamageClass.LASER)
                .setCasing(() -> ingotPolymer(2), 4).setArmorPiercing(0.5F).setThresholdNegation(10F)
                .setOnBeamImpact(LAMBDA_STANDARD_BEAM_HIT);
        energy_emerald_overcharge = beam("energy_emerald_overcharge", "capacitor_overcharge", DamageClass.LASER)
                .setCasing(() -> ingotPolymer(2), 4).setArmorPiercing(0.5F).setThresholdNegation(15F)
                .setDoesPenetrate(true)
                .setOnBeamImpact(LAMBDA_STANDARD_BEAM_HIT);
        energy_emerald_ir = beam("energy_emerald_ir", "capacitor_ir", DamageClass.FIRE)
                .setCasing(() -> ingotPolymer(2), 4).setArmorPiercing(0.5F).setThresholdNegation(10F)
                .setOnBeamImpact(LAMBDA_IR_HIT);
    }
}
