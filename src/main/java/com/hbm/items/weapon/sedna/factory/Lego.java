package com.hbm.items.weapon.sedna.factory;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.particle.helper.ParticleEffects;
import com.hbm.sound.ModGunSounds;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBaseMK4CL;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.SmokeNode;
import com.hbm.util.GameTime;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Standardised building blocks a {@link GunConfig} is assembled from, upstream's "LEGO" bricks.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.items.weapon.sedna.factory.Lego}. The projectile and explosion
 * seams retain the target's current entity contracts, while smoke nodes and fire ordering follow
 * ntm-next exactly.
 */
public class Lego {

    public static final Random ANIM_RAND = new Random();

    /** If IDLE and a reload is possible, begin one; otherwise play the "nothing to load" inspect. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_RELOAD = (stack, ctx) -> {
        Player player = ctx.getPlayer();
        Receiver rec = ctx.config.getReceivers(stack)[0];

        if (ItemGunBaseNT.getState(stack, ctx.configIndex) != GunState.IDLE) return;

        ItemGunBaseNT.setIsAiming(stack, false);
        IMagazine<BulletConfig> mag = rec.getMagazine(stack);

        if (mag.canReload(stack, ctx.inventory)) {
            int loaded = mag.getAmount(stack, ctx.inventory);
            mag.setAmountBeforeReload(stack, loaded);
            ItemGunBaseNT.setState(stack, ctx.configIndex, GunState.RELOADING);
            // Reloading from empty costs extra: the action has to be cycled before the first round.
            ItemGunBaseNT.setTimer(stack, ctx.configIndex, rec.getReloadBeginDuration(stack)
                    + (loaded <= 0 ? rec.getReloadCockOnEmptyPre(stack) : 0));
            ItemGunBaseNT.playAnimation(player, stack, GunAnimation.RELOAD, ctx.configIndex);
            if (ctx.config.getReloadChangesType(stack)) mag.initNewType(stack, ctx.inventory);
        } else {
            ItemGunBaseNT.playAnimation(player, stack, GunAnimation.INSPECT, ctx.configIndex);
            if (!ctx.config.getInspectCancel(stack)) {
                ItemGunBaseNT.setState(stack, ctx.configIndex, GunState.DRAWING);
                ItemGunBaseNT.setTimer(stack, ctx.configIndex, ctx.config.getInspectDuration(stack));
            }
        }
    };

    /** Pull the trigger on receiver 0. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_CLICK_PRIMARY =
            (stack, ctx) -> clickReceiver(stack, ctx, 0);

    /** The second receiver trigger used by underbarrels and dual weapons. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_CLICK_SECONDARY =
            (stack, ctx) -> {
                if (ItemGunBaseNT.getState(stack, ctx.configIndex) != GunState.IDLE) return;
                int mode = ItemGunBaseNT.getMode(stack, 0);
                ItemGunBaseNT.setMode(stack, ctx.configIndex, 1 - mode);
                ctx.entity.level().playSound(null, ctx.entity.getX(), ctx.entity.getY(), ctx.entity.getZ(),
                        mode == 0 ? ModGunSounds.SWITCH_MODE_1.get()
                                : ModGunSounds.SWITCH_MODE_2.get(),
                        SoundSource.PLAYERS, 1F, 1F);
            };

    /**
     * One trigger pull: fire if able, dry-fire if not, and cancel a reload in progress.
     *
     * <p>Firing mid-reload does not fire: it flags the reload for cancellation, which is what lets you
     * break off topping up a shotgun tube to shoot.
     */
    public static void clickReceiver(ItemStack stack, LambdaContext ctx, int receiver) {
        LivingEntity entity = ctx.entity;
        Player player = ctx.getPlayer();
        Receiver rec = ctx.config.getReceivers(stack)[receiver];
        int index = ctx.configIndex;
        GunState state = ItemGunBaseNT.getState(stack, index);

        if (state == GunState.IDLE) {
            if (rec.getCanFire(stack).apply(stack, ctx)) {
                rec.getOnFire(stack).accept(stack, ctx);
                playFireSound(entity, rec, stack);

                // Multi-round cycles (burst, double barrel) fire the rest immediately.
                int remaining = rec.getRoundsPerCycle(stack) - 1;
                for (int i = 0; i < remaining; i++) {
                    if (rec.getCanFire(stack).apply(stack, ctx)) rec.getOnFire(stack).accept(stack, ctx);
                }

                ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterFire(stack));
            } else if (rec.getDoesDryFire(stack)) {
                ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE_DRY, index);
                ItemGunBaseNT.setState(stack, index, rec.getRefireAfterDry(stack) ? GunState.COOLDOWN : GunState.DRAWING);
                ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
            }
        }

        if (state == GunState.RELOADING) ItemGunBaseNT.setReloadCancel(stack, true);
    }

    static void playFireSound(LivingEntity entity, Receiver rec, ItemStack stack) {
        if (rec.getFireSound(stack) == null) return;
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                rec.getFireSound(stack), SoundSource.PLAYERS,
                rec.getFireVolume(stack), rec.getFirePitch(stack));
    }

    /** Toggles aiming down the sights. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TOGGLE_AIM =
            (stack, ctx) -> ItemGunBaseNT.setIsAiming(stack, !ItemGunBaseNT.getIsAiming(stack));

    /** True when the magazine has something in it. */
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_STANDARD_CAN_FIRE =
            (stack, ctx) -> ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0;

    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_SECOND_CAN_FIRE =
            (stack, ctx) -> ctx.config.getReceivers(stack)[1].getMagazine(stack).getAmount(stack, ctx.inventory) > 0;

    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_LOCKON_CAN_FIRE =
            (stack, ctx) -> ItemGunBaseNT.getIsLockedOn(stack)
                    && ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0;

    /** Debug weapons bypass magazine checks, matching the source factory's test path. */
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_DEBUG_CAN_FIRE = (stack, ctx) -> true;

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_SMOKE = (stack, ctx) ->
            handleStandardSmoke(ctx.entity, stack, 2000, 0.025D, 1.15D, ctx.hand, ctx.configIndex);

    /** Spawns bullets for the loaded round. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_FIRE =
            (stack, ctx) -> doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, true);

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SECOND_FIRE =
            (stack, ctx) -> doStandardFire(stack, ctx, GunAnimation.CYCLE, 1, true);

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_LOCKON_FIRE = (stack, ctx) -> {
        doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, true);
        ItemGunBaseNT.setIsLockedOn(stack, false);
    };

    /** As {@link #LAMBDA_STANDARD_FIRE}, but the shot causes no wear. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_NOWEAR_FIRE =
            (stack, ctx) -> doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, false);

    /**
     * Fires one cycle from {@code receiver}: rolls the projectile count, spawns them with their own
     * damage and spread, spends a round, and adds wear.
     */
    public static void doStandardFire(ItemStack stack, LambdaContext ctx, GunAnimation anim,
            int receiver, boolean calcWear) {
        LivingEntity entity = ctx.entity;
        Player player = ctx.getPlayer();
        int index = ctx.configIndex;
        if (anim != null) ItemGunBaseNT.playAnimation(player, stack, anim, index);

        boolean aim = ItemGunBaseNT.getIsAiming(stack);
        Receiver primary = ctx.config.getReceivers(stack)[receiver];
        IMagazine<BulletConfig> mag = primary.getMagazine(stack);
        BulletConfig config = mag.getType(stack, ctx.inventory);
        if (config == null) return;

        Vec3 offset = aim ? primary.getProjectileOffsetScoped(stack) : primary.getProjectileOffset(stack);
        double forwardOffset = offset.x;
        double heightOffset = offset.y;
        double sideOffset = offset.z;

        // Split is a receiver modifier (shotgun/choke/laser upgrades), not merely an ammo property.
        float split = primary.getSplitProjectiles(stack);
        int projectileMin = (int) (config.projectilesMin * split);
        int projectileMax = (int) (config.projectilesMax * split);
        int projectiles = projectileMin;
        if (projectileMax > projectileMin) {
            projectiles += entity.getRandom().nextInt(projectileMax - projectileMin + 1);
        }

        Level level = entity.level();
        for (int i = 0; i < projectiles; i++) {
            // Damage and spread are rolled per pellet, so a shotgun's spread is a real cone rather than
            // one direction shared by the whole cluster.
            float damage = calcDamage(ctx, stack, primary, calcWear, index);
            float spread = calcSpread(ctx, stack, primary, config, calcWear, index, aim);

            if (config.pType == BulletConfig.ProjectileType.BULLET) {
                EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(entity, config, damage, spread,
                        sideOffset, heightOffset, forwardOffset);
                // 1.7 Lego.doStandardFire: a locked-on gun (Stinger, missile launcher) hands its target to the rocket
                if (ItemGunBaseNT.getIsLockedOn(stack)) bullet.lockonTarget = ItemGunBaseNT.resolveLockonTarget(level, stack);

                // Muzzle discharge, on the first pellet only: one puff per shot, not one per pellet.
                // Position and heading come from the spawned bullet rather than the shooter, so the
                // smoke sits at the barrel and points where the round actually went.
                if (i == 0 && config.blackPowder) {
                    ParticleEffects.blackPowder(level,
                            bullet.getX(), bullet.getY(), bullet.getZ(),
                            bullet.getDeltaMovement().x, bullet.getDeltaMovement().y,
                            bullet.getDeltaMovement().z,
                            10, 0.25F, 0.5F, 10, 0.25F);
                }

                level.addFreshEntity(bullet);
            } else if (config.pType == BulletConfig.ProjectileType.BULLET_CHUNKLOADING) {
                EntityBulletBaseMK4CL bullet = new EntityBulletBaseMK4CL(entity, config, damage, spread,
                        sideOffset, heightOffset, forwardOffset);
                if (ItemGunBaseNT.getIsLockedOn(stack)) bullet.lockonTarget = ItemGunBaseNT.resolveLockonTarget(level, stack);
                level.addFreshEntity(bullet);
            } else if (config.pType == BulletConfig.ProjectileType.BEAM) {
                EntityBulletBeamBase beam = new EntityBulletBeamBase(entity, config, damage, spread,
                        sideOffset, heightOffset, forwardOffset);
                beam.setImpactCallback(config.onImpactBeam);
                level.addFreshEntity(beam);
            }
        }

        mag.useUpAmmo(stack, ctx.inventory, 1);
        if (calcWear) {
            ItemGunBaseNT.setWear(stack, index,
                    Math.min(ItemGunBaseNT.getWear(stack, index) + config.wear, ctx.config.getDurability(stack)));
        }
    }

    /**
     * Evolves the attached smoke ribbon exactly once per client tick while the source shot timestamp is live.
     * The renderer owns drawing these nodes; this method owns their lifetime and motion.
     */
    public static void handleStandardSmoke(LivingEntity entity, ItemStack stack, int smokeDuration,
            double alphaDecay, double widthGrowth, InteractionHand hand, int index) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        long lastShot = gun.lastShotByHand[hand.ordinal()][index];
        List<SmokeNode> smokeNodes = gun.getConfig(stack, index).getSmokeNodes(hand);
        long now = GameTime.millis(entity.level());
        boolean smoking = lastShot + smokeDuration > now;
        if (!smoking && !smokeNodes.isEmpty()) smokeNodes.clear();
        if (!smoking) return;

        Vec3 previous = new Vec3(-entity.getDeltaMovement().x, -entity.getDeltaMovement().y,
                -entity.getDeltaMovement().z).yRot((float) (entity.getYRot() * Math.PI / 180D));
        double accel = 15D;
        double side = (entity.getYRot() - entity.yHeadRotO) * 0.1D;
        double waggle = 0.025D;
        for (SmokeNode node : smokeNodes) {
            node.forward += -previous.z * accel + entity.getRandom().nextGaussian() * waggle;
            node.lift += previous.y + 1.5D;
            node.side += previous.x * accel + entity.getRandom().nextGaussian() * waggle + side;
            if (node.alpha > 0) node.alpha -= alphaDecay;
            node.width *= widthGrowth;
        }

        double alpha = (now - lastShot) / (double) smokeDuration;
        alpha = (1 - alpha) * 0.5D;
        if (ItemGunBaseNT.getState(stack, index) == GunState.RELOADING || smokeNodes.isEmpty()) alpha = 0;
        smokeNodes.add(new SmokeNode(alpha));
    }

    /** Extra spread from wear: nothing until half-worn, then ramping to full at the end of its life. */
    /**
     * The warhead every explosive round detonates with: linear falloff from a damage the round states
     * itself, the shooter still liable, and a small fireball.
     */
    public static void standardExplode(EntityBulletBaseMK4 bullet, HitResult mop, float range) {
        standardExplode(bullet, mop, range, 1F);
    }

    public static void standardExplode(EntityBulletBaseMK4 bullet, HitResult mop, float range, float damageMod) {
        Vec3 hit = mop.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, range, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage * damageMod)
                .setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    public static void tinyExplode(EntityBulletBaseMK4 bullet, HitResult mop, float range) {
        tinyExplode(bullet, mop, range, 1F);
    }

    /** Nudged a quarter block off the face it hit, so a blast against a wall is not swallowed by it. */
    public static void tinyExplode(EntityBulletBaseMK4 bullet, HitResult mop, float range, float damageMod) {
        Direction dir = mop instanceof BlockHitResult block ? block.getDirection() : Direction.UP;
        Vec3 hit = mop.getLocation();
        double x = hit.x + dir.getStepX() * 0.25D;
        double y = hit.y + dir.getStepY() * 0.25D;
        double z = hit.z + dir.getStepZ() * 0.25D;

        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), x, y, z, range, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, bullet.damage * damageMod)
                .setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent)
                .setKnockback(0.25D));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectTiny());
        vnt.explode();
    }

    public static float getStandardWearSpread(ItemStack stack, GunConfig config, int index) {
        float percent = ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack);
        if (percent < 0.5F) return 0F;
        return (percent - 0.5F) * 2F;
    }

    /** Damage multiplier from wear: full damage until three-quarters worn, then falling away. */
    public static float getStandardWearDamage(ItemStack stack, GunConfig config, int index) {
        float percent = ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack);
        if (percent < 0.75F) return 1F;
        return 1F - (percent - 0.75F) * 2F;
    }

    public static float calcDamage(LambdaContext ctx, ItemStack stack, Receiver primary, boolean calcWear, int index) {
        return primary.getBaseDamage(stack) * (calcWear ? getStandardWearDamage(stack, ctx.config, index) : 1F);
    }

    /** Innate + ammo + hipfire + wear, all additive, exactly as upstream stacks them. */
    public static float calcSpread(LambdaContext ctx, ItemStack stack, Receiver primary,
            BulletConfig config, boolean calcWear, int index, boolean aim) {
        float spreadInnate = primary.getInnateSpread(stack);
        float spreadAmmo = config.spread * primary.getAmmoSpread(stack);
        float spreadHipfire = aim ? 0F : primary.getHipfireSpread(stack);
        float spreadWear = !calcWear ? 0F
                : getStandardWearSpread(stack, ctx.config, index) * primary.getDurabilitySpread(stack);
        return spreadInnate + spreadAmmo + spreadHipfire + spreadWear;
    }
}
