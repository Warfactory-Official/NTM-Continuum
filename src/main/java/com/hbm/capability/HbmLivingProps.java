package com.hbm.capability;

import com.hbm.advancement.HbmCriteria;
import com.hbm.config.ServerConfig;
import com.hbm.particle.helper.LivingEffects;
import dev.golem.core.Ids;
import dev.golem.core.data.EntityData;
import dev.golem.core.world.Compat;
import java.util.List;

import com.hbm.Constants;
import com.hbm.config.RadiationConfig;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.EntityHbmProps.ContaminationEffect;
import com.hbm.lib.ModDamageSources;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Static facade over each living entity's {@link EntityHbmProps}, ported ~1:1 from 1.12.2's
 * {@code HbmLivingProps}. All gameplay code goes through here: the setters carry the old clamps and
 * side effects (digamma max-health modifier + death, asbestos/blacklung kill at max).
 *
 * <p>Deferred vs the original: the asbestos/coal-dust on-screen inform toasts.
 */
public class HbmLivingProps {

    /** Identifier-keyed replacement for the old digamma AttributeModifier UUID. */
    public static final Identifier DIGAMMA_ID = Ids.of(Constants.MOD_ID, "digamma");
    public static final int maxAsbestos = 60 * 60 * 20;
    public static final int maxBlacklung = EntityHbmProps.MAX_BLACKLUNG;

    /**
     * Every living entity's HBM state, in the loader's own per-entity storage (NeoForge/Fabric data attachment,
     * Forge capability) through Golem Core, saved as {@code hbm:living_props}. An entity saved in the earlier
     * {@code HbmLivingProps} child-tag format is read once on load. Not copied on respawn:
     * {@code HbmPlayerProps.init} copies the player half, as 1.7's {@code onPlayerClone} did.
     */
    public static final EntityData<EntityHbmProps> DATA = EntityData.of(Constants.MOD_ID, "living_props",
            LivingEntity.class, EntityHbmProps::new, EntityHbmProps::save, EntityHbmProps::load)
            .migrateFrom("HbmLivingProps");

    /** Declares {@link #DATA}; must run before the loader flushes registrations (NeoForge registers it). */
    public static void init() {}

    public static EntityHbmProps getData(LivingEntity entity) {
        return DATA.get(entity);
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

        // 1.7 HbmLivingProps.incrementRadiation caps at 2500 (the 25,000,000 here came from the 1.12.2 fork).
        if (radiation > 2500)
            radiation = 2500;
        if (radiation < 0)
            radiation = 0;

        setRadiation(entity, radiation);
    }

    // Neutron Radiation
    public static double getNeutron(LivingEntity entity) {
        var system = com.hbm.util.ContaminationUtil.neutronSystem();
        if (system != null && !system.enabled()) return 0D;
        return getData(entity).getNeutrons();
    }

    public static void setNeutron(LivingEntity entity, double rad) {
        var system = com.hbm.util.ContaminationUtil.neutronSystem();
        if (system != null && !system.enabled()) return;
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
            // 1.7 setDigamma: a 50-particle soul-sand sweat burst on the digamma death, sent 50 blocks
            LivingEffects.sweat(entity, 50, Blocks.SOUL_SAND, 50D);
        }
        // 1.7 setDigamma: digammaSee above 0, digammaFeel from 2, digammaKnow from 10
        if (entity instanceof ServerPlayer player) {
            double di = getDigamma(entity);
            if (di > 0D) HbmCriteria.DIGAMMA.fire(player, "see", ItemStack.EMPTY, di);
            if (di >= 2D) HbmCriteria.DIGAMMA.fire(player, "feel", ItemStack.EMPTY, di);
            if (di >= 10D) HbmCriteria.DIGAMMA.fire(player, "know", ItemStack.EMPTY, di);
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
        if (RadiationConfig.disableAsbestos) return 0;
        return getData(entity).getAsbestos();
    }

    public static void setAsbestos(LivingEntity entity, int asbestos) {
        if (RadiationConfig.disableAsbestos) return;
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

    public static int getBlackFire(LivingEntity entity) {
        return getData(entity).getBlackFire();
    }

    /**
     * 1.7 {@code EntityFireLingering} TYPE_BLACK: a fresh burn starts at 200 ticks, an existing one of at least 200
     * is extended by 5 (bullets of the 358.00 BL add 200 directly through {@link #addBlackFire}).
     */
    public static void igniteBlackFire(LivingEntity entity) {
        EntityHbmProps props = getData(entity);
        if (props.getBlackFire() < 200) props.setBlackFire(200);
        else props.setBlackFire(props.getBlackFire() + 5);
    }

    /** 1.7 {@code XFactory35800}: {@code blackFire += ticks}. */
    public static void addBlackFire(LivingEntity entity, int ticks) {
        EntityHbmProps props = getData(entity);
        props.setBlackFire(props.getBlackFire() + ticks);
    }

    public static void addCont(LivingEntity entity, ContaminationEffect cont) {
        getData(entity).getContaminationEffectList().add(cont);
    }

    /// BLACK LUNG DISEASE ///
    public static int getBlackLung(LivingEntity entity) {
        if (RadiationConfig.disableCoal) return 0;
        return getData(entity).getBlacklung();
    }

    public static void setBlackLung(LivingEntity entity, int blacklung) {
        if (RadiationConfig.disableCoal) return;
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

    /// CONTAGION ///
    public static int getContagion(LivingEntity entity) {
        if (!ServerConfig.ENABLE_MKU.get()) return 0; // 1.7: no MKU, no infection
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
