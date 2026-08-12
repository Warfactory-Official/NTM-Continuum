package com.hbm.handler;

import java.util.Iterator;
import java.util.Random;

import com.hbm.capability.HbmLivingProps;
import com.hbm.config.GeneralConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.EntityHbmProps.ContaminationEffect;
import com.hbm.hazard.HazardSystem;
import com.hbm.lib.ModDamageSources;
import com.hbm.packet.EntityPropsSyncPayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Per-living-entity tick logic (radiation sickness, contamination effects, digamma, lung disease),
 * ported from 1.12.2's {@code EntityEffectHandler}. Driven by the {@code MixinLivingEntity} tick
 * injection — the cross-loader stand-in for Forge's {@code LivingUpdateEvent} (runs on both sides).
 *
 * <p>Deferred vs the original: all particle effects (vomit/sweat/radiation-flash — the HBM particle
 * system isn't ported), the crater-biome ambient radiation, contagion/oil/pollution/temperature
 * handlers (fields persist but the loops are stubbed), and the un-ported mob transforms (nuclear
 * creeper / quackos / RAD beast).
 */
public class EntityEffectHandler {

    private static final int SYNC_INTERVAL = 5;

    public static void onUpdate(LivingEntity entity) {

        // Placed first to prevent picking up transient radiation from the handlers below.
        handleRadiationEffect(entity);

        if (!entity.level().isClientSide()) {

            if (entity.tickCount % 20 == 0) {
                HbmLivingProps.setRadBuf(entity, HbmLivingProps.getRadEnv(entity));
                HbmLivingProps.setRadEnv(entity, 0);
            }

            HazardSystem.tickEntity(entity);

            handleContamination(entity);
            handleRadiation(entity);
            handleDigamma(entity);
            handleLungDisease(entity);

            if (entity instanceof ServerPlayer player && entity.tickCount % SYNC_INTERVAL == 0) {
                syncToPlayer(player);
            }
        }
    }

    private static void handleContamination(LivingEntity entity) {
        Iterator<ContaminationEffect> iterator = HbmLivingProps.getCont(entity).iterator();
        while (iterator.hasNext()) {
            ContaminationEffect con = iterator.next();
            ContaminationUtil.contaminate(entity, HazardType.RADIATION,
                    con.ignoreArmor ? ContaminationType.RAD_BYPASS : ContaminationType.CREATIVE, con.getRad());

            con.time--;
            if (con.time <= 0) iterator.remove();
        }
    }

    private static void handleRadiation(LivingEntity entity) {

        if (ContaminationUtil.isRadImmune(entity))
            return;

        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        double offset = com.hbm.handler.radiation.ChunkRadiationManager.proxy.getRadiation(level, pos);
        double radD = Math.max(0D, offset);
        if (radD > 0D) {
            ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, radD / 20D);
        }

        // Fallout rain radiation is deferred with the fallout system.

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator()))
            return;

        // Vomit/sweat particle bursts (>600/>200/>900 rads) deferred with the particle system; the
        // hunger + vomit-sound tick is kept.
        Random rand = new Random(entity.getId());
        int r600 = rand.nextInt(600);
        int r1200 = rand.nextInt(1200);
        long time = level.getGameTime();

        if (HbmLivingProps.getRadiation(entity) > 600 && (time + r600) % 600 < 20 && canVomit(entity)) {
            if ((time + r600) % 600 == 1 && level instanceof ServerLevel) {
                level.playSound(null, pos, ModSounds.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 19));
            }
        } else if (HbmLivingProps.getRadiation(entity) > 200 && (time + r1200) % 1200 < 20 && canVomit(entity)) {
            if ((time + r1200) % 1200 == 1 && level instanceof ServerLevel) {
                level.playSound(null, pos, ModSounds.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 19));
            }
        }
    }

    private static boolean canVomit(LivingEntity entity) {
        return true; // old check excluded skeletons/etc via a mouth flag; kept simple
    }

    private static void handleRadiationEffect(LivingEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!GeneralConfig.enableRads) return;
        if (entity instanceof ServerPlayer player && player.isSpectator()) return;

        double eRad = HbmLivingProps.getRadiation(entity);
        if (eRad < 50) return;
        int rng = level.getRandom().nextInt(21000);
        DamageSource radiation = ModDamageSources.radiation(serverLevel);

        // Mob transforms (only those whose target mob is vanilla / ported)
        if (eRad >= 50 && entity instanceof Cow cow && !(entity instanceof MushroomCow)) {
            convert(cow, EntityType.MOOSHROOM, serverLevel);
            return;
        } else if (eRad >= 500 && entity instanceof Villager villager) {
            convert(villager, EntityType.ZOMBIE_VILLAGER, serverLevel);
            return;
        } else if (eRad >= 800 && entity instanceof Horse horse) {
            convert(horse, EntityType.ZOMBIE_HORSE, serverLevel);
            return;
        }

        if (eRad < 200) return;
        if (eRad > 2500000) HbmLivingProps.setRadiation(entity, 2500000);

        if (eRad >= 1000) {
            entity.hurtServer(serverLevel, radiation, 1000F);
            HbmLivingProps.setRadiation(entity, 0);
            if (entity.getHealth() > 0) {
                entity.setHealth(0);
                entity.die(radiation);
            }
        } else if (eRad >= 800) {
            if (rng % 300 == 0) addEffect(entity, MobEffects.NAUSEA, 5 * 30, 0);
            if (rng % 300 == 50) addEffect(entity, MobEffects.SLOWNESS, 10 * 20, 2);
            if (rng % 300 == 100) addEffect(entity, MobEffects.WEAKNESS, 10 * 20, 2);
            if (rng % 500 == 0) addEffect(entity, MobEffects.POISON, 3 * 20, 2);
            if (rng % 700 == 0) addEffect(entity, MobEffects.WITHER, 3 * 20, 1);
            if (rng % 300 == 150) addEffect(entity, MobEffects.HUNGER, 5 * 20, 3);
            if (rng % 300 == 200) addEffect(entity, MobEffects.MINING_FATIGUE, 5 * 20, 3);
        } else if (eRad >= 600) {
            if (rng % 300 == 0) addEffect(entity, MobEffects.NAUSEA, 5 * 30, 0);
            if (rng % 300 == 50) addEffect(entity, MobEffects.SLOWNESS, 10 * 20, 2);
            if (rng % 300 == 100) addEffect(entity, MobEffects.WEAKNESS, 10 * 20, 2);
            if (rng % 500 == 0) addEffect(entity, MobEffects.POISON, 3 * 20, 1);
            if (rng % 300 == 150) addEffect(entity, MobEffects.HUNGER, 3 * 20, 3);
            if (rng % 400 == 0) addEffect(entity, MobEffects.MINING_FATIGUE, 6 * 20, 2);
        } else if (eRad >= 400) {
            if (rng % 300 == 0) addEffect(entity, MobEffects.NAUSEA, 5 * 30, 0);
            if (rng % 500 == 50) addEffect(entity, MobEffects.SLOWNESS, 5 * 20, 0);
            if (rng % 300 == 100) addEffect(entity, MobEffects.WEAKNESS, 5 * 20, 1);
            if (rng % 500 == 150) addEffect(entity, MobEffects.HUNGER, 3 * 20, 2);
            if (rng % 600 == 0) addEffect(entity, MobEffects.MINING_FATIGUE, 4 * 20, 1);
        } else if (eRad >= 200) {
            if (rng % 300 == 0) addEffect(entity, MobEffects.NAUSEA, 5 * 20, 0);
            if (rng % 500 == 0) addEffect(entity, MobEffects.WEAKNESS, 5 * 20, 0);
            if (rng % 700 == 0) addEffect(entity, MobEffects.HUNGER, 3 * 20, 2);
            if (rng % 800 == 0) addEffect(entity, MobEffects.MINING_FATIGUE, 4 * 20, 0);
        }
    }

    private static void convert(Mob from, EntityType<? extends Mob> toType, ServerLevel level) {
        Mob to = toType.create(level, EntitySpawnReason.CONVERSION);
        if (to == null) return;
        to.snapTo(from.getX(), from.getY(), from.getZ(), from.getYRot(), from.getXRot());
        if (from.isAlive() && level.addFreshEntity(to)) {
            from.discard();
        }
    }

    private static void handleDigamma(LivingEntity entity) {
        double digamma = HbmLivingProps.getDigamma(entity);
        if (digamma < 0.01F) return;
        // Sweat particle burst deferred (needs the HBM particle system).
    }

    private static void handleLungDisease(LivingEntity entity) {

        if (entity instanceof Player player && player.isCreative()) {
            HbmLivingProps.setBlackLung(entity, 0);
            HbmLivingProps.setAsbestos(entity, 0);
            return;
        }

        int bl = HbmLivingProps.getBlackLung(entity);
        if (bl > 0 && bl < EntityHbmProps.MAX_BLACKLUNG * 0.25)
            HbmLivingProps.setBlackLung(entity, HbmLivingProps.getBlackLung(entity) - 1);

        double blacklung = Math.min(HbmLivingProps.getBlackLung(entity), EntityHbmProps.MAX_BLACKLUNG);
        double asbestos = Math.min(HbmLivingProps.getAsbestos(entity), EntityHbmProps.MAX_ASBESTOS);

        boolean coughs = blacklung / EntityHbmProps.MAX_BLACKLUNG > 0.25D || asbestos / EntityHbmProps.MAX_ASBESTOS > 0.25D;
        if (!coughs) return;

        double blacklungDelta = 1D - (blacklung / (double) EntityHbmProps.MAX_BLACKLUNG);
        double asbestosDelta = 1D - (asbestos / (double) EntityHbmProps.MAX_ASBESTOS);
        double total = 1 - (blacklungDelta * asbestosDelta);

        if (total > 0.8D) {
            addEffect(entity, MobEffects.WEAKNESS, 100, 6);
            addEffect(entity, MobEffects.NAUSEA, 100, 0);
            addEffect(entity, MobEffects.SLOWNESS, 100, 3);
            if (entity.getRandom().nextInt(250) == 0) addEffect(entity, MobEffects.POISON, 100, 2);
        } else if (total > 0.65D) {
            addEffect(entity, MobEffects.WEAKNESS, 100, 2);
            addEffect(entity, MobEffects.SLOWNESS, 100, 2);
            if (entity.getRandom().nextInt(500) == 0) addEffect(entity, MobEffects.POISON, 100, 0);
        } else if (total > 0.45D) {
            addEffect(entity, MobEffects.WEAKNESS, 100, 1);
            addEffect(entity, MobEffects.SLOWNESS, 100, 1);
        } else if (total > 0.25D) {
            addEffect(entity, MobEffects.SLOWNESS, 100, 0);
        }
        // Cough sound + particles deferred.
    }

    private static void addEffect(LivingEntity entity, Holder<MobEffect> effect, int duration, int amplifier) {
        if (amplifier >= 0) entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    private static void syncToPlayer(ServerPlayer player) {
        EntityHbmProps props = HbmLivingProps.getData(player);
        ByteBuf buf = Unpooled.buffer();
        props.serialize(buf);
        byte[] data = ByteBufUtil.getBytes(buf);
        buf.release();
        Services.PLATFORM.sendToPlayer(player, new EntityPropsSyncPayload(data));
    }
}
