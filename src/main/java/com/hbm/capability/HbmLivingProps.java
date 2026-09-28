package com.hbm.capability;

import dev.golem.core.world.Compat;
import java.util.List;

import com.hbm.Constants;
import com.hbm.config.RadiationConfig;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.EntityHbmProps.ContaminationEffect;
import com.hbm.lib.ModDamageSources;
import com.hbm.entity.IHbmLivingDuck;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Static facade over each living entity's {@link EntityHbmProps}, ported ~1:1 from 1.12.2's
 * {@code HbmLivingProps}. All gameplay code goes through here: the setters carry the old clamps and
 * side effects (digamma max-health modifier + death, asbestos/blacklung kill at max).
 *
 * <p>Deferred vs the original: the asbestos/coal-dust on-screen inform toasts, the digamma death
 * particle burst.
 */
public class HbmLivingProps {

    /** Identifier-keyed replacement for the old digamma AttributeModifier UUID. */
    public static final Identifier DIGAMMA_ID = dev.golem.core.Ids.of(Constants.MOD_ID, "digamma");
    public static final int maxAsbestos = 60 * 60 * 20;
    public static final int maxBlacklung = 2 * 60 * 60 * 20;

    public static EntityHbmProps getData(LivingEntity entity) {
        return ((IHbmLivingDuck) entity).hbm$getProps();
    }

    /// RADIATION ///
    public static double getRadiation(LivingEntity entity) {
        if (!RadiationConfig.enableContamination) return 0;
        return getData(entity).getRads();
    }

    public static void setRadiation(LivingEntity entity, double rad) {
        if (RadiationConfig.enableContamination) getData(entity).setRads(rad);
    }

    public static void incrementRadiation(LivingEntity entity, double rad) {
        if (!RadiationConfig.enableContamination) return;
        double radiation = getRadiation(entity) + rad;

        if (radiation > 25000000)
            radiation = 25000000;
        if (radiation < 0)
            radiation = 0;

        setRadiation(entity, radiation);
    }

    // Neutron Radiation
    public static double getNeutron(LivingEntity entity) {
        return getData(entity).getNeutrons();
    }

    public static void setNeutron(LivingEntity entity, double rad) {
        getData(entity).setNeutrons(rad);
    }

    /// RAD ENV ///
    public static double getRadEnv(LivingEntity entity) {
        return getData(entity).getRadsEnv();
    }

    public static void setRadEnv(LivingEntity entity, double rad) {
        getData(entity).setRadsEnv(rad);
    }

    /// RAD BUF ///
    public static double getRadBuf(LivingEntity entity) {
        return getData(entity).getRadBuf();
    }

    public static void setRadBuf(LivingEntity entity, double rad) {
        getData(entity).setRadBuf(rad);
    }

    /// DIGAMMA ///
    public static double getDigamma(LivingEntity entity) {
        return getData(entity).getDigamma();
    }

    public static void setDigamma(LivingEntity entity, double digamma) {

        getData(entity).setDigamma(digamma);

        // max health *= 0.5^digamma (the modifier value is the delta, ADD_MULTIPLIED_TOTAL)
        double healthMod = Math.pow(0.5, digamma) - 1D;

        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            dev.golem.core.world.Attributes.replacePermanent(attribute, DIGAMMA_ID.toString(), healthMod);
        }

        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }

        if ((entity.getMaxHealth() <= 0 || digamma >= 10.0D) && entity.isAlive()
                && entity.level() instanceof ServerLevel serverLevel) {
            DamageSource source = ModDamageSources.digamma(serverLevel);
            entity.setAbsorptionAmount(0);
            Compat.hurt(entity, serverLevel, source, 5000000F);
            if (entity.isAlive()) {
                entity.setHealth(0);
                entity.die(source);
            }
            // TODO(particles): soul-sand sweat burst on digamma death (needs the HBM particle system)
        }
        // 1.7 setDigamma: digammaSee above 0, digammaFeel from 2, digammaKnow from 10
        if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
            double di = getDigamma(entity);
            if (di > 0D) com.hbm.advancement.HbmCriteria.DIGAMMA.fire(player, "see", net.minecraft.world.item.ItemStack.EMPTY, di);
            if (di >= 2D) com.hbm.advancement.HbmCriteria.DIGAMMA.fire(player, "feel", net.minecraft.world.item.ItemStack.EMPTY, di);
            if (di >= 10D) com.hbm.advancement.HbmCriteria.DIGAMMA.fire(player, "know", net.minecraft.world.item.ItemStack.EMPTY, di);
        }
    }

    public static void incrementDigamma(LivingEntity entity, double digamma) {
        double dRad = getDigamma(entity) + digamma;

        if (dRad > 10)
            dRad = 10;
        if (dRad < 0)
            dRad = 0;

        setDigamma(entity, dRad);
    }

    /// ASBESTOS ///
    public static int getAsbestos(LivingEntity entity) {
        return getData(entity).getAsbestos();
    }

    public static void setAsbestos(LivingEntity entity, int asbestos) {
        EntityHbmProps props = getData(entity);
        props.setAsbestos(asbestos);

        if (asbestos >= EntityHbmProps.MAX_ASBESTOS && entity.level() instanceof ServerLevel serverLevel) {
            props.setAsbestos(0);
            Compat.hurt(entity, serverLevel, ModDamageSources.asbestos(serverLevel), 1000);
        }
    }

    public static void incrementAsbestos(LivingEntity entity, int asbestos) {
        setAsbestos(entity, getAsbestos(entity) + asbestos);
        // TODO(hud): the old red "info.asbestos" inform toast (PlayerInformPacketLegacy)
    }

    public static int getFire(LivingEntity entity) {
        return getData(entity).getFire();
    }

    /** Raises the burn to {@code ticks} without ever shortening one already running. */
    public static void setFireAtLeast(LivingEntity entity, int ticks) {
        EntityHbmProps props = getData(entity);
        if (props.getFire() < ticks) props.setFire(ticks);
    }

    public static int getPhosphorus(LivingEntity entity) {
        return getData(entity).getPhosphorus();
    }

    public static void setPhosphorusAtLeast(LivingEntity entity, int ticks) {
        EntityHbmProps props = getData(entity);
        if (props.getPhosphorus() < ticks) props.setPhosphorus(ticks);
    }

    public static int getBalefire(LivingEntity entity) {
        return getData(entity).getBalefire();
    }

    public static void setBalefireAtLeast(LivingEntity entity, int ticks) {
        EntityHbmProps props = getData(entity);
        if (props.getBalefire() < ticks) props.setBalefire(ticks);
    }

    public static void addCont(LivingEntity entity, ContaminationEffect cont) {
        getData(entity).getContaminationEffectList().add(cont);
    }

    /// BLACK LUNG DISEASE ///
    public static int getBlackLung(LivingEntity entity) {
        return getData(entity).getBlacklung();
    }

    public static void setBlackLung(LivingEntity entity, int blacklung) {
        EntityHbmProps props = getData(entity);
        props.setBlacklung(blacklung);

        if (blacklung >= EntityHbmProps.MAX_BLACKLUNG && entity.level() instanceof ServerLevel serverLevel) {
            props.setBlacklung(0);
            Compat.hurt(entity, serverLevel, ModDamageSources.blacklung(serverLevel), 1000);
        }
    }

    public static void incrementBlackLung(LivingEntity entity, int blacklung) {
        setBlackLung(entity, getBlackLung(entity) + blacklung);
        // TODO(hud): the old red "info.coaldust" inform toast
    }

    /// TIME BOMB ///
    public static int getTimer(LivingEntity entity) {
        return getData(entity).getBombTimer();
    }

    public static void setTimer(LivingEntity entity, int bombTimer) {
        getData(entity).setBombTimer(bombTimer);
    }

    /// CONTAGION ///
    public static int getContagion(LivingEntity entity) {
        if (!com.hbm.config.ServerConfig.ENABLE_MKU.get()) return 0; // 1.7: no MKU, no infection
        return getData(entity).getContagion();
    }

    public static void setContagion(LivingEntity entity, int contagion) {
        getData(entity).setContagion(contagion);
    }

    public static List<ContaminationEffect> getCont(LivingEntity e) {
        return getData(e).getContaminationEffectList();
    }

    /// OIL ///
    public static int getOil(LivingEntity entity) {
        return getData(entity).getOil();
    }

    public static void setOil(LivingEntity entity, int oil) {
        getData(entity).setOil(oil);
    }

    public static boolean getEnableBackpack(LivingEntity entity) {
        return getData(entity).getEnableBackpack();
    }

    public static void setEnableBackpack(LivingEntity entity, boolean enabled) {
        getData(entity).setEnableBackpack(enabled);
    }

    public static boolean getEnableHUD(LivingEntity entity) {
        return getData(entity).getEnableHUD();
    }

    public static void setEnableHUD(LivingEntity entity, boolean enabled) {
        getData(entity).setEnableHUD(enabled);
    }
}
