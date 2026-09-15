package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.ModDamageSources;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.sound.ModGunSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The two secret Folly warheads, copied from ntm-next's XFactoryFolly. */
public final class XFactoryFolly {

    public static BulletConfig folly_sm;
    public static BulletConfig folly_nuke;

    /** The original Folly beam update: delayed radiation, a destructive swept line and area damage. */
    public static final Consumer<Entity> LAMBDA_SM_UPDATE = entity -> {
        if (!(entity.level() instanceof ServerLevel server) || !(entity instanceof EntityBulletBeamBase beam)) return;
        Vec3 dir = new Vec3(beam.headingX, beam.headingY, beam.headingZ).normalize();

        if (beam.tickCount != 2) return;

        if (beam.getThrower() != null) {
            ContaminationUtil.contaminate(beam.getThrower(), ContaminationUtil.HazardType.RADIATION,
                    ContaminationUtil.ContaminationType.CREATIVE, 150F);
        }

        List<Entity> entities = server.getEntities(beam,
                beam.getBoundingBox().expandTowards(beam.headingX, beam.headingY, beam.headingZ)
                        .inflate(1.0D, 1.0D, 1.0D));

        for (int i = 1; i < beam.beamLength; i += 2) {
            int x = (int) Math.floor(beam.getX() + dir.x * i);
            int y = (int) Math.floor(beam.getY() + dir.y * i);
            int z = (int) Math.floor(beam.getZ() + dir.z * i);

            for (int ix = x - 1; ix <= x + 1; ix++) {
                for (int iy = y - 1; iy <= y + 1; iy++) {
                    for (int iz = z - 1; iz <= z + 1; iz++) {
                        if (iy > server.getMinY() && iy < server.getMaxY()) {
                            server.setBlockAndUpdate(new net.minecraft.core.BlockPos(ix, iy, iz),
                                    Blocks.AIR.defaultBlockState());
                        }
                        AABB aabb = new AABB(ix - 1, iy - 1, iz - 1, ix + 2, iy + 2, iz + 2);
                        for (Entity victim : entities) {
                            if (victim == beam.getThrower() || !victim.getBoundingBox().intersects(aabb)) continue;
                            victim.invulnerableTime = 0;
                            var source = ModDamageSources.bullet(server, beam.config.dmgClass.type(), beam,
                                    beam.getThrower());
                            if (victim instanceof LivingEntity living) {
                                DamageResistanceHandler.hurt(server, living, source, beam.damage, 100F, 0.99F);
                            } else {
                                EntityDamageUtil.attackEntityFromIgnoreIFrame(victim, source, beam.damage);
                            }
                        }
                    }
                }
            }
        }
    };

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_NUKE_IMPACT = (bullet, mop) -> {
        if (mop instanceof EntityHitResult && bullet.tickCount < 2) return;
        if (bullet.isRemoved()) return;
        bullet.discard();
        Vec3 hit = mop.getLocation();
        bullet.level().addFreshEntity(EntityNukeExplosionMK5.statFac(bullet.level(), 100, hit.x, hit.y, hit.z));
        bullet.level().addFreshEntity(EntityNukeTorex.statFac(bullet.level(), hit.x, hit.y, hit.z, 100F));
    };

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_TOGGLE_AIM = (stack, ctx) -> {
        if (ItemGunBaseNT.getState(stack, ctx.configIndex) == GunState.IDLE) {
            boolean wasAiming = ItemGunBaseNT.getIsAiming(stack);
            ItemGunBaseNT.setIsAiming(stack, !wasAiming);
            if (!wasAiming) ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.SPINUP, ctx.configIndex);
        }
    };

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE = (stack, ctx) ->
            Lego.doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, false);

    public static final BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_CAN_FIRE = (stack, ctx) -> {
        if (ctx.entity instanceof Player) {
            if (!ItemGunBaseNT.getIsAiming(stack)) return false;
            if (ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) != GunAnimation.SPINUP) return false;
            if (ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex) < 100) return false;
        }
        return ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0;
    };

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_FOLLY = (stack, ctx) ->
            ItemGunBaseNT.setupRecoil(25, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));

    public static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FOLLY_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP:
                return new BusAnimation().addBus("EQUIP", new BusAnimationSequence()
                        .addPos(-60, 0, 0, 0).addPos(5, 0, 0, 1500, IType.SIN_DOWN)
                        .addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE:
                return new BusAnimation().addBus("RECOIL", new BusAnimationSequence()
                                .addPos(0, 0, -4.5, 50).addPos(0, 0, -4.5, 500)
                                .addPos(0, 0, 0, 500, IType.SIN_UP))
                        .addBus("LOAD", new BusAnimationSequence().addPos(0, 0, 0, 50)
                                .addPos(-25, 0, 0, 250, IType.SIN_DOWN)
                                .addPos(0, 0, 0, 1000, IType.SIN_FULL));
            case RELOAD:
                return new BusAnimation()
                        .addBus("LOAD", new BusAnimationSequence().addPos(60, 0, 0, 1000, IType.SIN_FULL)
                                .addPos(60, 0, 0, 6000).addPos(0, 0, 0, 1000, IType.SIN_FULL))
                        .addBus("SCREW", new BusAnimationSequence().addPos(0, 0, 0, 1000)
                                .addPos(0, 0, -135, 1000, IType.SIN_FULL).addPos(0, 0, -135, 4000)
                                .addPos(0, 0, 0, 1000, IType.SIN_FULL))
                        .addBus("BREECH", new BusAnimationSequence().addPos(0, 0, 0, 1000)
                                .addPos(0, 0, -0.5, 1000, IType.SIN_FULL)
                                .addPos(0, -4, -0.5, 1000, IType.SIN_FULL).addPos(0, -4, -0.5, 2000)
                                .addPos(0, 0, -0.5, 1000, IType.SIN_FULL)
                                .addPos(0, 0, 0, 1000, IType.SIN_FULL))
                        .addBus("SHELL", new BusAnimationSequence().addPos(0, -4, -4.5, 0)
                                .addPos(0, -4, -4.5, 3000).addPos(0, 0, -4.5, 1000, IType.SIN_FULL)
                                .addPos(0, 0, 0, 500, IType.SIN_UP));
            default:
                return null;
        }
    };

    public static void init() {
        folly_sm = BulletConfigs.register("folly_sm", new BulletConfig()
                .setItem(SpecialGunCatalog.secretAmmo("folly_sm")).setupDamageClass(DamageClass.SUBATOMIC)
                .setBeam().setLife(100).setVel(2F).setGrav(0.015F).setRenderRotations(false)
                .setSpectral(true).setDoesPenetrate(true).setOnUpdate(LAMBDA_SM_UPDATE));
        folly_nuke = BulletConfigs.register("folly_nuke", new BulletConfig()
                .setItem(SpecialGunCatalog.secretAmmo("folly_nuke")).setChunkloading().setLife(600).setVel(4F)
                .setGrav(0.015F).setOnImpact(LAMBDA_NUKE_IMPACT));

        // The common deferred register is intentionally initialized only after all configs exist.
        NtmEnergyGunCatalog.init();
    }

    public static GunConfig gunConfig() {
        return new GunConfig().dura(0).draw(40).crosshair(Crosshair.NONE)
                .rec(new Receiver(0).dmg(1000F).delay(26).dryfire(false).reload(160).jam(0)
                        .sound(ModGunSounds.FIRE_FOLLY::get, 100F, 1F)
                        .mag(new MagazineSingleReload(0, 1).addConfigs(folly_sm, folly_nuke))
                        .offset(0.75, -0.0625, -0.1875D).offsetScoped(0.75, -0.0625, -0.125D)
                        .canFire(LAMBDA_CAN_FIRE).fire(LAMBDA_FIRE).recoil(LAMBDA_RECOIL_FOLLY))
                .setupStandardConfiguration().pt(LAMBDA_TOGGLE_AIM);
    }
}
