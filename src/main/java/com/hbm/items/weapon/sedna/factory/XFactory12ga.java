package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.entity.projectile.EntityDuchessGambit;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.lib.ModDamageSources;
import com.hbm.packet.PlasmaBlastPayload;
import com.hbm.sound.ModGunSounds;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BiConsumer;

/** The source copy of the 12 gauge family, adapted only at the modern registry/projectile seams. */
public final class XFactory12ga {

    public static final ConfigWrapper<Float> GUN_MARES_LEG_DAMAGE = new ConfigWrapper<>(16F);
    public static final ConfigWrapper<Float> GUN_MARES_LEG_AKIMBO_LEFT_DAMAGE = new ConfigWrapper<>(16F);
    public static final ConfigWrapper<Float> GUN_MARES_LEG_AKIMBO_RIGHT_DAMAGE = new ConfigWrapper<>(16F);
    public static final ConfigWrapper<Float> GUN_BROKEN_DAMAGE = new ConfigWrapper<>(48F);
    public static final ConfigWrapper<Float> GUN_LIBERATOR_DAMAGE = new ConfigWrapper<>(16F);
    public static final ConfigWrapper<Float> GUN_SPAS_DAMAGE = new ConfigWrapper<>(32F);
    public static final ConfigWrapper<Float> GUN_AUTOSHOTGUN_DAMAGE = new ConfigWrapper<>(48F);
    public static final ConfigWrapper<Float> GUN_SHREDDER_DAMAGE = new ConfigWrapper<>(50F);
    public static final ConfigWrapper<Float> GUN_SEXY_DAMAGE = new ConfigWrapper<>(64F);

    public static void initConfig() {
        ServerConfig.configMap.put("GUN_MARES_LEG_DAMAGE", GUN_MARES_LEG_DAMAGE);
        ServerConfig.configMap.put("GUN_MARES_LEG_AKIMBO_LEFT_DAMAGE", GUN_MARES_LEG_AKIMBO_LEFT_DAMAGE);
        ServerConfig.configMap.put("GUN_MARES_LEG_AKIMBO_RIGHT_DAMAGE", GUN_MARES_LEG_AKIMBO_RIGHT_DAMAGE);
        ServerConfig.configMap.put("GUN_BROKEN_DAMAGE", GUN_BROKEN_DAMAGE);
        ServerConfig.configMap.put("GUN_LIBERATOR_DAMAGE", GUN_LIBERATOR_DAMAGE);
        ServerConfig.configMap.put("GUN_SPAS_DAMAGE", GUN_SPAS_DAMAGE);
        ServerConfig.configMap.put("GUN_AUTOSHOTGUN_DAMAGE", GUN_AUTOSHOTGUN_DAMAGE);
        ServerConfig.configMap.put("GUN_SHREDDER_DAMAGE", GUN_SHREDDER_DAMAGE);
        ServerConfig.configMap.put("GUN_SEXY_DAMAGE", GUN_SEXY_DAMAGE);
    }

    public static BulletConfig g12_bp;
    public static BulletConfig g12_bp_magnum;
    public static BulletConfig g12_bp_slug;
    public static BulletConfig g12;
    public static BulletConfig g12_slug;
    public static BulletConfig g12_flechette;
    public static BulletConfig g12_magnum;
    public static BulletConfig g12_explosive;
    public static BulletConfig g12_phosphorus;
    public static BulletConfig g12_equestrian_bj;
    public static BulletConfig g12_equestrian_tkr;

    public static BulletConfig g12_sub;
    public static BulletConfig g12_sub_slug;
    public static BulletConfig g12_sub_flechette;
    public static BulletConfig g12_sub_magnum;
    public static BulletConfig g12_sub_explosive;
    public static BulletConfig g12_sub_phosphorus;
    public static BulletConfig g12_shredder;
    public static BulletConfig g12_shredder_slug;
    public static BulletConfig g12_shredder_flechette;
    public static BulletConfig g12_shredder_magnum;
    public static BulletConfig g12_shredder_explosive;
    public static BulletConfig g12_shredder_phosphorus;

    /** 1.7 {@code LAMBDA_BOAT}: the Duchess Gambit spawned 50 blocks above the hit, with the boat sound. */
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_BOAT = (bullet, hit) -> {
        Entity pippo = XFactory44.dropFromSky(bullet, hit, new EntityDuchessGambit(
                ModNtmEntities.DUCHESS_GAMBIT.get(), bullet.level()));
        // 1.7 plays it at the boat's y + 50 (100 above the hit); kept as written. 1.7 category "player".
        bullet.level().playSound(null, pippo.getX(), pippo.getY() + 50, pippo.getZ(),
                ModGunSounds.BOAT.get(), SoundSource.PLAYERS, 100F, 1F);
        bullet.discard();
    };

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE = (bullet, hit) -> {
        Lego.standardExplode(bullet, hit, 2F);
        bullet.discard();
    };

    /** The source ricochet/impact path for shredder submunitions. */
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_SHREDDER_RICOCHET = (bullet, hit) -> {
        if (!(hit instanceof BlockHitResult blockHit)) return;

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = bullet.level().getBlockState(pos);
        if (!state.canOcclude() || state.getBlock() instanceof StainedGlassPaneBlock
                || state.is(Blocks.GLASS_PANE) || state.is(Blocks.GLOWSTONE)) {
            bullet.level().destroyBlock(pos, false);
            bullet.setPos(hit.getLocation());
            return;
        }
        // BlockDetonatable and deco_crt are source-only block hooks; the target has no corresponding seam.

        Direction direction = blockHit.getDirection();
        Vec3 face = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        Vec3 velocity = bullet.getDeltaMovement().normalize();
        double angle = Math.abs(crossAngle(velocity, face) - 90D);
        if (angle > bullet.config.ricochetAngle) {
            bullet.setPos(hit.getLocation());
            bullet.discard();
            return;
        }

        Vec3 impact = hit.getLocation();
        spawnPulse(bullet.level(), hit, bullet.getYRot(), bullet.getXRot());
        List<Entity> blast = bullet.level().getEntities(bullet,
                new AABB(bullet.getX(), bullet.getY(), bullet.getZ(), bullet.getX(), bullet.getY(), bullet.getZ())
                        .inflate(0.5D));
        if (bullet.level() instanceof ServerLevel server) {
            var source = ModDamageSources.bullet(server, ModDamageSources.DamageClass.PLASMA.type(), bullet,
                    bullet.getThrower());
            for (Entity entity : blast) {
                if (!entity.isAlive()) continue;
                if (entity instanceof LivingEntity living) {
                    living.invulnerableTime = 0;
                    DamageResistanceHandler.hurt(server, living, source, bullet.damage, 0F, 0F);
                } else {
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, bullet.damage);
                }
            }
        }

        bullet.ricochets++;
        if (bullet.ricochets > bullet.config.maxRicochetCount) {
            bullet.setPos(impact);
            bullet.discard();
            return;
        }
        Vec3 motion = bullet.getDeltaMovement();
        bullet.setDeltaMovement(switch (direction.getAxis()) {
            case X -> new Vec3(-motion.x, motion.y, motion.z);
            case Y -> new Vec3(motion.x, -motion.y, motion.z);
            case Z -> new Vec3(motion.x, motion.y, -motion.z);
        });
        bullet.setPos(impact);
    };

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_MARESLEG = (stack, ctx) ->
            ItemGunBaseNT.setupRecoil(10F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_LIBERATOR = (stack, ctx) ->
            ItemGunBaseNT.setupRecoil(5F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_AUTOSHOTGUN = (stack, ctx) ->
            ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5) + 1.5F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_SEXY = (stack, ctx) ->
            ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5),
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));

    /** Source secondary trigger: fires the remaining SPAS rounds in the same cycle. */
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_SPAS_SECONDARY = (stack, ctx) -> {
        LivingEntity entity = ctx.entity;
        Receiver rec = ctx.config.getReceivers(stack)[0];
        int index = ctx.configIndex;
        GunState state = ItemGunBaseNT.getState(stack, index);
        if (state == GunState.IDLE) {
            if (rec.getCanFire(stack).apply(stack, ctx)) {
                rec.getOnFire(stack).accept(stack, ctx);
                int timeFired = 1;
                for (int i = 0; i < rec.getRoundsPerCycle(stack); i++) {
                    if (rec.getCanFire(stack).apply(stack, ctx)) {
                        rec.getOnFire(stack).accept(stack, ctx);
                        timeFired++;
                    }
                }
                if (rec.getFireSound(stack) != null) entity.level().playSound(null, entity.getX(), entity.getY(),
                        entity.getZ(), rec.getFireSound(stack), SoundSource.PLAYERS, rec.getFireVolume(stack),
                        rec.getFirePitch(stack) * (timeFired > 1 ? 0.9F : 1F));
                ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, index, 20);
            } else if (rec.getDoesDryFire(stack)) {
                ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.CYCLE_DRY, index);
                ItemGunBaseNT.setState(stack, index, GunState.DRAWING);
                ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
            }
        }
        if (state == GunState.RELOADING) ItemGunBaseNT.setReloadCancel(stack, true);
    };

    /**
     * Source beam impact, including the radial hit and random-direction beam submunitions. The only
     * removed source side effect is the PlasmaBlast packet; the shared Core/projectile path remains exact.
     */
    public static BulletConfig makeShredderConfig(BulletConfig original, BulletConfig submunition) {
        String id = switch (suffix(original)) {
            case "g12" -> "g12_shredder";
            case "g12_slug" -> "g12_shredder_slug";
            case "g12_flechette" -> "g12_shredder_flechette";
            case "g12_magnum" -> "g12_shredder_magnum";
            case "g12_explosive" -> "g12_shredder_explosive";
            case "g12_phosphorus" -> "g12_shredder_phosphorus";
            default -> "g12_shredder_" + suffix(original);
        };
        BulletConfig config = BulletConfigs.register(id, new BulletConfig()
                .setBeam().setRenderRotations(false).setLife(5)
                .setDamage(original.damageMult * original.projectilesMax)
                .setupDamageClass(ModDamageSources.DamageClass.LASER));
        config.ammo = original.ammo;
        config.casingAmount = original.casingAmount;
        config.casingProfile = original.casingProfile;
        config.casing = original.casing;
        config.setCasing(() -> original.casingStack().copy(), original.casingAmount);
        config.setOnBeamImpact((beam, hit) -> {
            int projectiles = submunition.projectilesMin;
            if (submunition.projectilesMax > submunition.projectilesMin) {
                projectiles += beam.level().getRandom().nextInt(
                        submunition.projectilesMax - submunition.projectilesMin + 1);
            }

            if (hit instanceof BlockHitResult blockHit) {
                Direction direction = blockHit.getDirection();
                Vec3 impact = hit.getLocation().add(direction.getStepX() * 0.1D, direction.getStepY() * 0.1D,
                        direction.getStepZ() * 0.1D);
                spawnPulse(beam.level(), hit, beam.getYRot(), beam.getXRot());
                radialImpact(beam, impact);
                for (int i = 0; i < projectiles; i++) {
                    EntityBulletBaseMK4 bullet = submunition(beam, submunition, impact,
                            new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ()));
                    beam.level().addFreshEntity(bullet);
                }
            }

            if (hit instanceof EntityHitResult) {
                Vec3 impact = hit.getLocation();
                spawnPulse(beam.level(), hit, beam.getYRot(), beam.getXRot());
                for (int i = 0; i < projectiles; i++) {
                    Vec3 direction = new Vec3(beam.level().getRandom().nextGaussian(),
                            beam.level().getRandom().nextGaussian(), beam.level().getRandom().nextGaussian()).normalize();
                    beam.level().addFreshEntity(submunition(beam, submunition, impact, direction));
                }
            }
        });
        return config;
    }

    public static BulletConfig makeShredderSubmunition(BulletConfig original) {
        BulletConfig config = new BulletConfig().setItem(original.ammo)
                .setCasing(() -> original.casingStack().copy(), original.casingAmount)
                .setCasing(original.casingProfile)
                .setReloadCount(original.ammoReloadCount)
                .setVel(0.5F).setSpread(original.spread).setWear(original.wear)
                .setProjectiles(original.projectilesMin, original.projectilesMax).setDamage(original.damageMult)
                .setThresholdNegation(original.armorThresholdNegation).setArmorPiercing(original.armorPiercingPercent)
                .setKnockback(original.knockbackMult).setHeadshot(original.headshotMult)
                .setupDamageClass(ModDamageSources.DamageClass.PLASMA).setRicochetAngle(90F).setRicochetCount(3)
                .setDamageFalloffByPen(original.damageFalloffByPen).setGrav(original.gravity).setLife(50)
                .setImpactsEntities(original.impactsEntities).setDoesPenetrate(original.doesPenetrate)
                .setSpectral(original.isSpectral).setSelfDamageDelay(original.selfDamageDelay)
                .setBlackPowder(original.blackPowder).setRenderRotations(original.renderRotations)
                .setOnUpdate(original.onUpdate).setOnImpact(original.onImpact).setOnEntityHit(original.onEntityHit)
                .setOnRicochet(LAMBDA_SHREDDER_RICOCHET);
        config.casing = original.casing;
        return config;
    }

    private static void radialImpact(EntityBulletBeamBase beam, Vec3 impact) {
        if (!(beam.level() instanceof ServerLevel server)) return;
        List<Entity> blast = beam.level().getEntities(beam, new AABB(impact, impact).inflate(0.75D));
        var source = ModDamageSources.bullet(server, ModDamageSources.DamageClass.LASER.type(), beam,
                beam.getThrower());
        for (Entity entity : blast) {
            if (!entity.isAlive()) continue;
            if (entity instanceof LivingEntity living) {
                living.invulnerableTime = 0;
                DamageResistanceHandler.hurt(server, living, source, beam.damage, 0F, 0F);
            } else {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, beam.damage);
            }
        }
    }

    private static EntityBulletBaseMK4 submunition(EntityBulletBeamBase beam, BulletConfig config, Vec3 position,
            Vec3 direction) {
        float yaw = (float) Math.atan2(-direction.x, direction.z);
        // EntityBulletBaseMK4's angular constructor applies the source pitch negation itself.
        float pitch = (float) Math.asin(direction.y);
        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(beam.level(), config,
                beam.damage, 0.2F, yaw, pitch);
        bullet.thrower = beam.getThrower();
        bullet.setPos(position);
        return bullet;
    }

    private static String suffix(BulletConfig config) {
        return config.id == null ? Integer.toHexString(System.identityHashCode(config)) : config.id.getPath();
    }

    private static double crossAngle(Vec3 a, Vec3 b) {
        double length = a.length() * b.length();
        return length == 0D ? 0D : Math.toDegrees(Math.acos(Math.max(-1D, Math.min(1D, a.dot(b) / length))));
    }

    /** Sends the source's oriented plasma shockwave; this is not a generic explosion. */
    public static void spawnPulse(Level world, HitResult hit, float yaw, float pitch) {
        double x = hit.getLocation().x;
        double y = hit.getLocation().y;
        double z = hit.getLocation().z;

        if (hit instanceof BlockHitResult blockHit) {
            Direction side = blockHit.getDirection();
            switch (side) {
                case UP, DOWN -> { yaw = 0F; pitch = 0F; }
                case NORTH -> { yaw = 0F; pitch = 90F; }
                case SOUTH -> { yaw = 180F; pitch = 90F; }
                case EAST -> { yaw = 90F; pitch = 90F; }
                case WEST -> { yaw = 270F; pitch = 90F; }
            }
            x += side.getStepX() * 0.05D;
            y += side.getStepY() * 0.05D;
            z += side.getStepZ() * 0.05D;
        }

        PlasmaBlastPayload.send(world, x, y, z, 0.5F, 0.5F, 1.0F,
                pitch, yaw, 0.75F);
    }

    public static void init() {
        if (g12_bp != null) return;
        float buckshotSpread = 0.035F;
        float magnumSpread = 0.015F;
        g12_bp = BulletConfigs.register("g12_bp", new BulletConfig().setItem(ModAmmoItems.g12_bp)
                .setCasing(EnumCasingType.SHOTSHELL, 12).setBlackPowder(true).setProjectiles(8)
                .setDamage(0.75F / 8F).setSpread(buckshotSpread).setRicochetAngle(15F));
        g12_bp_magnum = BulletConfigs.register("g12_bp_magnum", new BulletConfig().setItem(ModAmmoItems.g12_bp_magnum)
                .setCasing(EnumCasingType.SHOTSHELL, 12).setBlackPowder(true).setProjectiles(4)
                .setDamage(0.75F / 4F).setSpread(buckshotSpread).setRicochetAngle(25F));
        g12_bp_slug = BulletConfigs.register("g12_bp_slug", new BulletConfig().setItem(ModAmmoItems.g12_bp_slug)
                .setCasing(EnumCasingType.SHOTSHELL, 12).setBlackPowder(true).setDamage(0.75F).setSpread(0.01F)
                .setRicochetAngle(5F));
        g12 = BulletConfigs.register("g12", new BulletConfig().setItem(ModAmmoItems.g12)
                .setCasing(EnumCasingType.BUCKSHOT, 6).setProjectiles(8).setDamage(1F / 8F)
                .setSpread(buckshotSpread).setRicochetAngle(15F).setThresholdNegation(2F));
        g12_slug = BulletConfigs.register("g12_slug", new BulletConfig().setItem(ModAmmoItems.g12_slug)
                .setCasing(EnumCasingType.BUCKSHOT, 6).setHeadshot(1.5F).setSpread(0F).setRicochetAngle(25F)
                .setThresholdNegation(4F).setArmorPiercing(0.15F));
        g12_flechette = BulletConfigs.register("g12_flechette", new BulletConfig().setItem(ModAmmoItems.g12_flechette)
                .setCasing(EnumCasingType.BUCKSHOT, 6).setProjectiles(8).setDamage(1F / 8F)
                .setThresholdNegation(5F).setArmorPiercing(0.2F).setSpread(0.025F).setRicochetAngle(5F));
        g12_magnum = BulletConfigs.register("g12_magnum", new BulletConfig().setItem(ModAmmoItems.g12_magnum)
                .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 6).setProjectiles(4).setDamage(2F / 4F)
                .setSpread(magnumSpread).setRicochetAngle(15F).setThresholdNegation(4F));
        g12_explosive = BulletConfigs.register("g12_explosive", new BulletConfig().setItem(ModAmmoItems.g12_explosive)
                .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 6).setDamage(2.5F).setOnImpact(LAMBDA_STANDARD_EXPLODE)
                .setSpread(0F).setRicochetAngle(15F));
        g12_phosphorus = BulletConfigs.register("g12_phosphorus", new BulletConfig().setItem(ModAmmoItems.g12_phosphorus)
                .setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 6).setProjectiles(8).setDamage(1F / 8F)
                .setSpread(magnumSpread).setRicochetAngle(15F).setOnImpact((bullet, hit) -> {
                    if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
                        HbmLivingProps.setPhosphorusAtLeast(living, 300);
                    }
                }));

        // Both 1.7 secret variants share the same flattened ammo item; only BJ drops the Duchess Gambit (LAMBDA_BOAT).
        g12_equestrian_bj = BulletConfigs.register("g12_equestrian_bj", new BulletConfig()
                .setItem(ModAmmoItems.g12_equestrian).setDamage(0F).setOnImpact(LAMBDA_BOAT));
        g12_equestrian_tkr = BulletConfigs.register("g12_equestrian_tkr", new BulletConfig()
                .setItem(ModAmmoItems.g12_equestrian).setDamage(0F));

        g12_sub = BulletConfigs.register("g12_sub", makeShredderSubmunition(g12));
        g12_sub_slug = BulletConfigs.register("g12_sub_slug", makeShredderSubmunition(g12_slug));
        g12_sub_flechette = BulletConfigs.register("g12_sub_flechette", makeShredderSubmunition(g12_flechette));
        g12_sub_magnum = BulletConfigs.register("g12_sub_magnum", makeShredderSubmunition(g12_magnum));
        g12_sub_explosive = BulletConfigs.register("g12_sub_explosive", makeShredderSubmunition(g12_explosive));
        g12_sub_phosphorus = BulletConfigs.register("g12_sub_phosphorus", makeShredderSubmunition(g12_phosphorus));
        g12_shredder = makeShredderConfig(g12, g12_sub);
        g12_shredder_slug = makeShredderConfig(g12_slug, g12_sub_slug);
        g12_shredder_flechette = makeShredderConfig(g12_flechette, g12_sub_flechette);
        g12_shredder_magnum = makeShredderConfig(g12_magnum, g12_sub_magnum);
        g12_shredder_explosive = makeShredderConfig(g12_explosive, g12_sub_explosive);
        g12_shredder_phosphorus = makeShredderConfig(g12_phosphorus, g12_sub_phosphorus);
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {g12_bp, g12_bp_magnum, g12_bp_slug, g12, g12_slug, g12_flechette,
                g12_magnum, g12_explosive, g12_phosphorus};
    }

    /** The mutated name's translation key: 1.7 returned {@code getUnlocalizedName() + suffix}. */
    static String mutated(ItemStack stack, String suffix) { return stack.getItem().getDescriptionId() + suffix; }

    /** 1.7 {@code LAMBDA_NAME_MARESLEG}. */
    public static final Function<ItemStack, String> LAMBDA_NAME_MARESLEG = stack ->
            XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SAWED_OFF) ? mutated(stack, "_short") : null;
}
