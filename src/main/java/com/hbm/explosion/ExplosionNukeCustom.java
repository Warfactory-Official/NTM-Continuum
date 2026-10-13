package com.hbm.explosion;

import com.hbm.entity.ModNtmEntities;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import dev.golem.core.hook.Hook;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** 1.7 held this on the {@code NukeCustom} block, out of {@code EntityFallingNuke}'s reach. */
public final class ExplosionNukeCustom {

    public static final int MAX_TNT = 150;
    public static final int MAX_NUKE = 200;
    public static final int MAX_HYDRO = 350;
    public static final int MAX_AMAT = 350;
    public static final int MAX_SCHRAB = 250;
    /** 1.7 states this one at its two use sites rather than beside the five above. */
    public static final int MAX_DIRTY = 100;

    private ExplosionNukeCustom() {
    }

    /** Replaces the custom nuke's antimatter stage (ADDON-API-INVENTORY H-67). */
    @FunctionalInterface
    public interface AntimatterStage {
        /**
         * {@code x, y, z} are what {@link #explodeCustom} received (1.7 adds another half block when spawning) and
         * {@code amat} the final, capped strength. Return true when the listener detonated the stage.
         */
        boolean detonate(Level level, double x, double y, double z, float amat);
    }

    /** The first listener that answers true replaces 1.7's balefire; HBM registers none. NTM: Space's is antimatter. */
    public static final Hook<AntimatterStage> ANTIMATTER = Hook.notifying();

    public static void explodeCustom(Level level, double x, double y, double z, float tnt, float nuke, float hydro,
                                     float amat, float dirty, float schrab, float euph) {

        dirty = Math.min(dirty, MAX_DIRTY);

        /// EUPHEMIUM ///
        if(euph > 0) {

            EntityNukeExplosionMK3 ex = new EntityNukeExplosionMK3(ModNtmEntities.NUKE_EXPLOSION_MK3.get(), level);
            ex.setPos(x, y, z);
            ex.destructionRange = 150;
            ex.speed = BombConfig.blastSpeed;
            ex.coefficient = 1.0F;
            // 1.7 also set waste = false here. The ported MK3 only implements the Fleija/Solinium half of that
            // flag (the waste=true ExplosionNukeAdvanced path is unported), so it already behaves as waste=false.
            level.addFreshEntity(ex);

            level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 100000.0F, 1.0F);

            level.addFreshEntity(EntityCloudFleijaRainbow.statFac(level, 50, x, y, z));

        /// SCHRABIDIUM ///
        } else if(schrab > 0) {

            schrab += amat / 2 + hydro / 4 + nuke / 8 + tnt / 16;
            schrab = Math.min(schrab, MAX_SCHRAB);

            EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(level, x + 0.5, y + 0.5, z + 0.5,
                    (int) schrab);
            if(!ex.isRemoved()) {
                level.addFreshEntity(ex);
                level.addFreshEntity(EntityCloudFleija.statFac(level, (int) schrab, x + 0.5, y + 0.5, z + 0.5));
            }

        /// ANTIMATTER ///
        } else if(amat > 0) {

            amat += hydro / 2 + nuke / 4 + tnt / 8;
            amat = Math.min(amat, MAX_AMAT);

            for (AntimatterStage listener : ANTIMATTER) {
                if (listener.detonate(level, x, y, z, amat)) return;
            }

            EntityBalefire bf = new EntityBalefire(ModNtmEntities.BALEFIRE.get(), level);
            bf.setPos(x + 0.5, y + 0.5, z + 0.5);
            bf.destructionRange = (int) amat;
            level.addFreshEntity(bf);
            spawnCloud(level, EntityNukeTorex.statFacBale(level, x + 0.5, y + 5, z + 0.5, amat));

        /// HYDROGEN ///
        } else if(hydro > 0) {

            hydro += nuke / 2 + tnt / 4;
            hydro = Math.min(hydro, MAX_HYDRO);
            dirty *= 0.25F;

            level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, (int) hydro, x + 0.5, y + 0.5, z + 0.5)
                                                       .moreFallout((int) dirty));
            spawnCloud(level, EntityNukeTorex.statFac(level, x + 0.5, y + 5, z + 0.5, hydro));

        /// NUCLEAR ///
        } else if(nuke > 0) {

            nuke += tnt / 2;
            nuke = Math.min(nuke, MAX_NUKE);

            // 1.7 spawns the nuclear blast five blocks up where hydrogen spawns it at the bomb
            level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, (int) nuke, x + 0.5, y + 5, z + 0.5)
                                                       .moreFallout((int) dirty));
            spawnCloud(level, EntityNukeTorex.statFac(level, x + 0.5, y + 5, z + 0.5, nuke));

        /// NON-NUCLEAR ///
        } else if(tnt >= 75) {

            tnt = Math.min(tnt, MAX_TNT);

            level.addFreshEntity(EntityNukeExplosionMK5.statFacNoRad(level, (int) tnt, x + 0.5, y + 0.5, z + 0.5));
            spawnCloud(level, EntityNukeTorex.statFac(level, x + 0.5, y + 5, z + 0.5, tnt));

        } else if(tnt > 0) {

            ExplosionLarge.explode(level, x + 0.5, y + 0.5, z + 0.5, tnt, true, true, true);
        }
    }

    /**
     * 1.7's Torex factories spawned the cloud themselves; the port's build it and leave spawning to the
     * caller, which this class had not been doing. 1.7's custom nuke did not consult enableNukeClouds.
     */
    private static void spawnCloud(Level level, EntityNukeTorex torex) {
        if (level.addFreshEntity(torex)) torex.startChunkLoading();
    }
}
