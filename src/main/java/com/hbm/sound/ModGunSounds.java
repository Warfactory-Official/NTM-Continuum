package com.hbm.sound;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.sounds.SoundEvent;

/** Sound events used by the Sedna firearm batch and its animation timelines. */
public final class ModGunSounds {

    private ModGunSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.createSounds(Constants.MOD_ID);
    private static final Reg REG = Reg.of(SOUNDS);

    public static final RegistryObject<SoundEvent> FIRE_BLACK_POWDER = register("weapon.fire.blackpowder");
    public static final RegistryObject<SoundEvent> DISINTEGRATION = register("weapon.fire.disintegration");
    public static final RegistryObject<SoundEvent> CASING_SHELL = register("weapon.casing.shell");
    public static final RegistryObject<SoundEvent> CASING_SMALL = register("weapon.casing.small");
    public static final RegistryObject<SoundEvent> CASING_MEDIUM = register("weapon.casing.medium");
    public static final RegistryObject<SoundEvent> CASING_LARGE = register("weapon.casing.large");
    public static final RegistryObject<SoundEvent> FIRE_ASSAULT = register("weapon.fire.assault");
    /** 1.7 NTMSounds.GUN_SOLDIER_TF2_BOAT_EXE_WAV_MP3, played by the 12 gauge equestrian round. */
    public static final RegistryObject<SoundEvent> BOAT = register("weapon.boat");
    public static final RegistryObject<SoundEvent> SWITCH_MODE_1 = register("weapon.switchmode1");
    public static final RegistryObject<SoundEvent> SWITCH_MODE_2 = register("weapon.switchmode2");
    public static final RegistryObject<SoundEvent> B92_RELOAD = register("weapon.b92reload");
    @SuppressWarnings("unchecked")
    public static final RegistryObject<SoundEvent>[] SPARK_SHOOT = new RegistryObject[] {
            register("weapon.sparkshoot1"), register("weapon.sparkshoot2"), register("weapon.sparkshoot3")};

    // 1.7's reload/foley names are lower-cased for the modern Identifier charset.  The
    // resource generator copies the source files with the same lower-case path.
    public static final RegistryObject<SoundEvent> REVOLVER_COCK = register("weapon.reload.revolvercock");
    public static final RegistryObject<SoundEvent> REVOLVER_CLOSE = register("weapon.reload.revolverclose");
    public static final RegistryObject<SoundEvent> REVOLVER_SPIN = register("weapon.reload.revolverspin");
    public static final RegistryObject<SoundEvent> PISTOL_COCK = register("weapon.reload.pistolcock");
    public static final RegistryObject<SoundEvent> MAG_SMALL_REMOVE = register("weapon.reload.magsmallremove");
    public static final RegistryObject<SoundEvent> MAG_SMALL_INSERT = register("weapon.reload.magsmallinsert");
    public static final RegistryObject<SoundEvent> MAG_REMOVE = register("weapon.reload.magremove");
    public static final RegistryObject<SoundEvent> MAG_INSERT = register("weapon.reload.maginsert");
    public static final RegistryObject<SoundEvent> CANISTER_INSERT = register("weapon.reload.insertcanister");
    public static final RegistryObject<SoundEvent> ROCKET_INSERT = register("weapon.reload.insertrocket");
    public static final RegistryObject<SoundEvent> BOLT_OPEN = register("weapon.reload.boltopen");
    public static final RegistryObject<SoundEvent> BOLT_CLOSE = register("weapon.reload.boltclose");
    public static final RegistryObject<SoundEvent> RIFLE_COCK = register("weapon.reload.riflecock");
    public static final RegistryObject<SoundEvent> LEVER_COCK = register("weapon.reload.levercock");
    public static final RegistryObject<SoundEvent> SHOTGUN_LOAD = register("weapon.reload.shotgunreload");
    public static final RegistryObject<SoundEvent> SHOTGUN_COCK = register("weapon.reload.shotguncock");
    public static final RegistryObject<SoundEvent> SHOTGUN_OPEN = register("weapon.reload.shotguncockopen");
    public static final RegistryObject<SoundEvent> SHOTGUN_CLOSE = register("weapon.reload.shotguncockclose");
    public static final RegistryObject<SoundEvent> SCREW = register("weapon.reload.screw");
    public static final RegistryObject<SoundEvent> COIL_RELOAD = register("weapon.coilgunreload");
    public static final RegistryObject<SoundEvent> IMPACT = register("weapon.reload.impact");
    public static final RegistryObject<SoundEvent> LATCH_OPEN = register("weapon.reload.openlatch");
    public static final RegistryObject<SoundEvent> VALVE = register("weapon.reload.pressurevalve");
    public static final RegistryObject<SoundEvent> FATMAN_RELOAD = register("weapon.reload.fatmanfull");
    public static final RegistryObject<SoundEvent> GRENADE_RELOAD = register("weapon.glreload");
    public static final RegistryObject<SoundEvent> GRENADE_OPEN = register("weapon.glopen");
    public static final RegistryObject<SoundEvent> GRENADE_CLOSE = register("weapon.glclose");
    public static final RegistryObject<SoundEvent> SHREDDER_CYCLE = register("weapon.fire.shreddercycle");

    public static final RegistryObject<SoundEvent> FIRE_SILENCED = register("weapon.fire.silenced");
    /** AMAT's distinct silencer report from ntm-next's sound table. */
    public static final RegistryObject<SoundEvent> FIRE_AMAT_SILENCER = register("weapon.silencer_shoot");
    public static final RegistryObject<SoundEvent> FIRE_AMAT = register("weapon.fire.amat");
    public static final RegistryObject<SoundEvent> FIRE_SILENCER = register("weapon.silencershoot");
    public static final RegistryObject<SoundEvent> FIRE_SHOTGUN = register("weapon.fire.shotgun");
    public static final RegistryObject<SoundEvent> FIRE_SPAS = register("weapon.shotgunshoot");
    public static final RegistryObject<SoundEvent> FIRE_LIBERATOR = register("weapon.fire.shotgunalt");
    public static final RegistryObject<SoundEvent> FIRE_SHREDDER = register("weapon.fire.shotgunauto");
    public static final RegistryObject<SoundEvent> FIRE_ABERRATOR = register("weapon.fire.aberrator");
    public static final RegistryObject<SoundEvent> FIRE_UNDERBARREL = register("weapon.hkshoot");
    public static final RegistryObject<SoundEvent> FIRE_CONGO = register("weapon.glshoot");
    public static final RegistryObject<SoundEvent> FIRE_CHARGE = register("weapon.fire.grenade");
    public static final RegistryObject<SoundEvent> FIRE_FLAMER_LOOP = register("weapon.fire.flameloop");
    public static final RegistryObject<SoundEvent> FIRE_FATMAN = register("weapon.fire.fatman");
    public static final RegistryObject<SoundEvent> FIRE_MINIGUN = register("weapon.calshoot");
    public static final RegistryObject<SoundEvent> FIRE_LASER_GATLING = register("weapon.fire.lasergatling");
    public static final RegistryObject<SoundEvent> FIRE_LASER_PISTOL = register("weapon.fire.laserpistol");
    public static final RegistryObject<SoundEvent> FIRE_LASER_RIFLE = register("weapon.fire.laser");
    public static final RegistryObject<SoundEvent> FIRE_COIL = register("weapon.coilgunshoot");
    public static final RegistryObject<SoundEvent> FIRE_TAU = register("weapon.fire.tau");
    public static final RegistryObject<SoundEvent> TAU_RELEASE = register("weapon.fire.taurelease");
    public static final RegistryObject<SoundEvent> TAU_LOOP = register("weapon.fire.tauloop");
    public static final RegistryObject<SoundEvent> FIRE_TESLA = register("weapon.fire.tesla");
    public static final RegistryObject<SoundEvent> TESLA_BLAST = register("entity.ufoblast");
    public static final RegistryObject<SoundEvent> FIRE_MK108 = register("weapon.fire.mk108");
    public static final RegistryObject<SoundEvent> FIRE_ROCKET = register("weapon.rpgshoot");
    public static final RegistryObject<SoundEvent> FIRE_EXTINGUISHER = register("weapon.extinguisher");
    public static final RegistryObject<SoundEvent> FIRE_FOLLY = register("weapon.fire.loudestnoiseonearth");
    public static final RegistryObject<SoundEvent> FIRE_VSTAR = register("weapon.fire.vstar");
    public static final RegistryObject<SoundEvent> WHACK = register("weapon.whack");
    public static final RegistryObject<SoundEvent> LOCKON = register("weapon.fire.lockon");
    public static final RegistryObject<SoundEvent> PLAYER_GULP = register("player.gulp");
    public static final RegistryObject<SoundEvent> PLAYER_GROAN = register("player.groan");

    // 1.12.2 HBMSoundHandler: the gluon gun and the jade shotgun
    public static final RegistryObject<SoundEvent> GLUON_START = register("weapon.gluonstart");
    public static final RegistryObject<SoundEvent> GLUON_LOOP = register("weapon.gluonloop");
    public static final RegistryObject<SoundEvent> GLUON_END = register("weapon.gluonend");
    public static final RegistryObject<SoundEvent> GLUON_HIT = register("weapon.gluonhit");
    public static final RegistryObject<SoundEvent> JSG_RELOAD_0 = register("weapon.jsg_reload0");
    public static final RegistryObject<SoundEvent> JSG_RELOAD_1 = register("weapon.jsg_reload1");
    public static final RegistryObject<SoundEvent> FIRE_SHOTTY = register("weapon.shottyshoot");

    private static RegistryObject<SoundEvent> register(String name) {
        return REG.sound(name);
    }

    public static void init() {}
}
