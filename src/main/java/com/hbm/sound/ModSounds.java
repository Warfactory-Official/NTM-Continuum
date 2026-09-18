package com.hbm.sound;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * HBM sound events. The registry path is dotted + <b>all-lowercase</b> (e.g. {@code hbm:block.motor})
 * — 26.1 Identifier paths reject the 1.12.2 camelCase names (only {@code [a-z0-9/._-]} allowed), so the
 * old {@code block.assemblerStrike} becomes {@code block.assembler_strike}. The {@code sounds.json} keys
 * must match these ids; the actual {@code .ogg} mapping lives in {@code assets/hbm/sounds.json}.
 */
public final class ModSounds {

    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);

    public static final RegistryObject<SoundEvent> MOTOR = register("block.motor");
    /** Shared switch-on sound used by the legacy fluid valve and other toggles. */
    public static final RegistryObject<SoundEvent> REACTOR_START = register("block.reactor_start");
    public static final RegistryObject<SoundEvent> REACTOR_LOOP = register("block.reactor_loop");
    public static final RegistryObject<SoundEvent> SOYUZ_READY = register("block.soyuz_ready");
    public static final RegistryObject<SoundEvent> SOYUZ_TAKEOFF = register("entity.soyuz_takeoff");
    public static final RegistryObject<SoundEvent> ALARM_HATCH = register("alarm.hatch");
    public static final RegistryObject<SoundEvent> ASSEMBLER_STRIKE = register("block.assembler_strike");
    public static final RegistryObject<SoundEvent> ASSEMBLER_START = register("block.assembler_start");
    public static final RegistryObject<SoundEvent> ASSEMBLER_STOP = register("block.assembler_stop");
    public static final RegistryObject<SoundEvent> ASSEMBLER_CUT = register("block.assembler_cut");
    public static final RegistryObject<SoundEvent> CHEMICAL_PLANT = register("block.chemical_plant");
    public static final RegistryObject<SoundEvent> CENTRIFUGE_OPERATE = register("block.centrifuge_operate");
    public static final RegistryObject<SoundEvent> PRESS_OPERATE = register("block.pressoperate");
    public static final RegistryObject<SoundEvent> WARN_OVERSPEED = register("block.warnoverspeed");
    /** The electric heater's hum loop (old {@code block.electricHum}). */
    public static final RegistryObject<SoundEvent> ELECTRIC_HUM = register("block.electric_hum");
    /** The FEL's looping laser hum; its volume and pitch ramp with the machine's audioDuration. */
    public static final RegistryObject<SoundEvent> FEL = register("block.fel");
    public static final RegistryObject<SoundEvent> STORAGE_OPEN = register("block.storage_open");
    public static final RegistryObject<SoundEvent> STORAGE_CLOSE = register("block.storage_close");

    /** Machine-upgrade insertion "plug" click (old {@code item.upgradePlug}). */
    public static final RegistryObject<SoundEvent> UPGRADE_PLUG = register("item.upgrade_plug");

    /** Handheld-gadget confirmation bleep (old {@code item.techBleep}) — oil detector etc. */
    public static final RegistryObject<SoundEvent> TECH_BLEEP = register("item.tech_bleep");
    /** The detonator's bind confirmation. Old id {@code hbm:item.techBoop}. */
    public static final RegistryObject<SoundEvent> TECH_BOOP = register("item.tech_boop");
    /** Ammo-container release sound from ntm-next's {@code item.unpack} pool. */
    public static final RegistryObject<SoundEvent> ITEM_UNPACK = register("item.unpack");
    public static final RegistryObject<SoundEvent> BOLTGUN = register("item.boltgun");
    public static final RegistryObject<SoundEvent> SYRINGE = register("item.syringe");
    public static final RegistryObject<SoundEvent> RADAWAY = register("item.radaway");
    public static final RegistryObject<SoundEvent> JETPACK_TANK = register("item.jetpack_tank");
    /** Fuel-jetpack thrust loop/effect sound (old {@code weapon.flamethrowerShoot}). */
    public static final RegistryObject<SoundEvent> FLAMETHROWER_IGNITE = register("weapon.flamethrower_ignite");
    public static final RegistryObject<SoundEvent> FLAMETHROWER_SHOOT = register("weapon.flamethrower_shoot");
    /** Powered-suit jetpack thrust sound (old {@code weapon.immolatorShoot}). */
    public static final RegistryObject<SoundEvent> IMMOLATOR_IGNITE = register("weapon.immolator_ignite");
    public static final RegistryObject<SoundEvent> IMMOLATOR_SHOOT = register("weapon.immolator_shoot");
    /** Advanced glider engine loop (old {@code weapon.jetpack}). */
    public static final RegistryObject<SoundEvent> JETPACK = register("weapon.jetpack");
    /** Block fragment impact sounds used by the black-hole rubble entity. */
    public static final RegistryObject<SoundEvent> DEBRIS = register("block.debris");
    public static final RegistryObject<SoundEvent> VICE = register("item.vice");
    public static final RegistryObject<SoundEvent> GUN_SMACK = register("weapon.fire.smack");
    public static final RegistryObject<SoundEvent> GUN_STAB = register("weapon.fire.stab");
    /** Looping hum of the current BigInteger FEnSU. */
    public static final RegistryObject<SoundEvent> FENSU_HUM = SOUNDS.register("block.fensu_hum",
            () -> SoundEvent.createFixedRangeEvent(
                    dev.golem.core.Ids.of(Constants.MOD_ID, "block.fensu_hum"), 25F));

    /** The Scream upgrade's screaming-scientist sound (old {@code block.screm}). */
    public static final RegistryObject<SoundEvent> SCREM = register("block.screm");

    public static final RegistryObject<SoundEvent> GEIGER_1 = register("item.geiger1");
    public static final RegistryObject<SoundEvent> GEIGER_2 = register("item.geiger2");
    public static final RegistryObject<SoundEvent> GEIGER_3 = register("item.geiger3");
    public static final RegistryObject<SoundEvent> GEIGER_4 = register("item.geiger4");
    public static final RegistryObject<SoundEvent> GEIGER_5 = register("item.geiger5");
    public static final RegistryObject<SoundEvent> GEIGER_6 = register("item.geiger6");
    public static final RegistryObject<SoundEvent> VOMIT = register("entity.vomit");

    /** The nuclear detonation roar played by the Torex cloud when its shockwave reaches the player. */
    public static final RegistryObject<SoundEvent> NUCLEAR_EXPLOSION = register("weapon.nuclear_explosion");
    /** The short detonation sound used by {@code ExplosionNukeSmall}. */
    public static final RegistryObject<SoundEvent> MUKE_EXPLOSION = register("weapon.muke_explosion");
    /** Near/far report used by the old controller's small VNT explosion effect. */
    public static final RegistryObject<SoundEvent> EXPLOSION_SMALL_NEAR = register("weapon.explosion_small_near");
    public static final RegistryObject<SoundEvent> EXPLOSION_SMALL_FAR = register("weapon.explosion_small_far");
    /** Near/far report of a large blast, arriving at the speed of sound. */
    public static final RegistryObject<SoundEvent> EXPLOSION_LARGE_NEAR = register("weapon.explosion_large_near");
    public static final RegistryObject<SoundEvent> EXPLOSION_LARGE_FAR = register("weapon.explosion_large_far");
    /** A missile leaving the pad. Old id {@code hbm:weapon.missileTakeoff}. */
    public static final RegistryObject<SoundEvent> MISSILE_TAKEOFF = register("weapon.missile_takeoff");
    /** The incoming-shell whistle. Played at the target, not at the shell. */
    public static final RegistryObject<SoundEvent> MORTAR_WHISTLE = register("weapon.mortar_whistle");

    /** Original RBMK/ZIRNOX reactor accident sound; used only by the vanilla ZIRNOX meltdown blast. */
    public static final RegistryObject<SoundEvent> RBMK_EXPLOSION = register("rbmk_explosion");
    public static final RegistryObject<SoundEvent> RBMK_AZ5_COVER = register("rbmk_az5_cover");
    public static final RegistryObject<SoundEvent> RBMK_SHUTDOWN = register("rbmk_shutdown");
    public static final RegistryObject<SoundEvent> STEAM_ENGINE_OPERATE = register("steam_engine_operate");
    public static final RegistryObject<SoundEvent> TURBOFAN_OPERATE = register("block.turbofan_operate");
    public static final RegistryObject<SoundEvent> TURBINE_GAS_STARTUP = register("block.turbinegas_startup");
    public static final RegistryObject<SoundEvent> TURBINE_GAS_SHUTDOWN = register("block.turbinegas_shutdown");
    public static final RegistryObject<SoundEvent> TURBINE_GAS_RUNNING = register("block.turbinegas_running");
    public static final RegistryObject<SoundEvent> INDUSTRIAL_TURBINE = register("block.industrial_turbine");
    public static final RegistryObject<SoundEvent> CHUNGUS_LEVER = register("block.chungus_lever");
    public static final RegistryObject<SoundEvent> CHUNGUS_TURBINE = register("block.chungus_turbine");
    public static final RegistryObject<SoundEvent> PYRO_OPERATE = SOUNDS.register("block.pyro_operate",
            () -> SoundEvent.createFixedRangeEvent(dev.golem.core.Ids.of(Constants.MOD_ID, "block.pyro_operate"), 15F));
    public static final RegistryObject<SoundEvent> IGENERATOR_OPERATE = SOUNDS.register("block.igenerator_operate",
            () -> SoundEvent.createFixedRangeEvent(dev.golem.core.Ids.of(Constants.MOD_ID, "block.igenerator_operate"), 10F));

    /** Weapons. Old ids {@code weapon.fire.pistol}, {@code weapon.reload.dryFireClick} and
     * {@code weapon.revolverReload}, lowercased to satisfy 26.1's id charset. */
    public static final RegistryObject<SoundEvent> FIRE_PISTOL = register("weapon.fire.pistol");
    public static final RegistryObject<SoundEvent> FIRE_PISTOL_LIGHT = register("weapon.fire.pistollight");
    public static final RegistryObject<SoundEvent> FIRE_GREASE_GUN = register("weapon.fire.greasegun");
    public static final RegistryObject<SoundEvent> FIRE_UZI = register("weapon.fire.uzi");
    public static final RegistryObject<SoundEvent> FIRE_RIFLE = register("weapon.fire.rifle");
    public static final RegistryObject<SoundEvent> FIRE_RIFLE_HEAVY = register("weapon.fire.rifleheavy");
    public static final RegistryObject<SoundEvent> SHOOT_44 = register("weapon.44shoot");
    public static final RegistryObject<SoundEvent> DRY_FIRE_CLICK = register("weapon.reload.dryfireclick");
    public static final RegistryObject<SoundEvent> RELOAD_REVOLVER = register("weapon.revolverreload");
    public static final RegistryObject<SoundEvent> LASER_BANG = register("weapon.laser_bang");

    /**
     * Power-armour footsteps (old {@code metalStep}, {@code ironJump}, {@code ironLand}). Played by
     * {@code ArmorFSB} on top of the vanilla step, which is why the step one is a single clank rather
     * than a variant set.
     */
    public static final RegistryObject<SoundEvent> METAL_STEP = register("step.metal");
    /** The four-variant step sound used by the old steel grate's custom SoundType. */
    public static final SoundEvent METAL_BLOCK_EVENT = SoundEvent.createVariableRangeEvent(
            dev.golem.core.Ids.of(Constants.MOD_ID, "step.metal_block"));
    public static final RegistryObject<SoundEvent> METAL_BLOCK = SOUNDS.register("step.metal_block",
            () -> METAL_BLOCK_EVENT);
    public static final RegistryObject<SoundEvent> IRON_JUMP = register("step.iron_jump");
    public static final RegistryObject<SoundEvent> IRON_LAND = register("step.iron_land");

    public static final RegistryObject<SoundEvent> TURRET_CHEKHOV_FIRE = register("turret.chekhov_fire");
    public static final RegistryObject<SoundEvent> TURRET_HOWARD_FIRE = register("turret.howard_fire");
    public static final RegistryObject<SoundEvent> TURRET_HOWARD_RELOAD = register("turret.howard_reload");
    public static final RegistryObject<SoundEvent> TURRET_JEREMY_FIRE = register("turret.jeremy_fire");
    public static final RegistryObject<SoundEvent> TURRET_JEREMY_RELOAD = register("turret.jeremy_reload");
    public static final RegistryObject<SoundEvent> TURRET_RICHARD_FIRE = register("turret.richard_fire");
    public static final RegistryObject<SoundEvent> TURRET_SENTRY_FIRE = register("turret.sentry_fire");
    public static final RegistryObject<SoundEvent> TURRET_SENTRY_LOCKON = register("turret.sentry_lockon");
    public static final RegistryObject<SoundEvent> WEAPON_TAU_SHOOT = register("weapon.tau_shoot");
    public static final RegistryObject<SoundEvent> EXPLOSION_TINY = register("weapon.explosion_tiny");
    /** Geiger click ladder, index 0..5 = old {@code geigerSounds}. */
    @SuppressWarnings("unchecked")
    public static final RegistryObject<SoundEvent>[] GEIGER_SOUNDS =
            new RegistryObject[] {GEIGER_1, GEIGER_2, GEIGER_3, GEIGER_4, GEIGER_5, GEIGER_6};

    // --- ntm-next entity port (gen_entity_sounds.py) ---
    public static final RegistryObject<SoundEvent> ALARM_CHIME = register("alarm.chime");
    public static final RegistryObject<SoundEvent> ALARM_GAMBIT = register("alarm.gambit");
    public static final RegistryObject<SoundEvent> ALARM_SOYUZED = register("alarm.soyuzed");
    public static final RegistryObject<SoundEvent> BALLS_LASER = register("weapon.balls_laser");
    public static final RegistryObject<SoundEvent> BOMBER_WHISTLE = register("entity.bomb_whistle");
    public static final RegistryObject<SoundEvent> BOMB_DET = register("entity.bomb_det");
    public static final RegistryObject<SoundEvent> CHOPPER_CHARGE = register("entity.chopper_charge");
    public static final RegistryObject<SoundEvent> CHOPPER_CRASHING_LOOP = register("entity.chopper_crashing_loop");
    public static final RegistryObject<SoundEvent> CHOPPER_DAMAGE = register("entity.chopper_damage");
    public static final RegistryObject<SoundEvent> CHOPPER_DROP = register("entity.chopper_drop");
    public static final RegistryObject<SoundEvent> CHOPPER_FLYING_LOOP = register("entity.chopper_flying_loop");
    public static final RegistryObject<SoundEvent> CRATE_CLOSE = register("crate_close");
    public static final RegistryObject<SoundEvent> ENTITY_BOMBER_LOOP = register("entity.bomber_loop");
    public static final RegistryObject<SoundEvent> ENTITY_BOMBER_SMALL_LOOP = register("entity.bomber_small_loop");
    public static final RegistryObject<SoundEvent> ENTITY_CYBERCRAB = register("entity.cybercrab");
    public static final RegistryObject<SoundEvent> ENTITY_DUCC = register("entity.ducc");
    public static final RegistryObject<SoundEvent> ENTITY_MEGAQUACC = register("entity.megaquacc");
    public static final RegistryObject<SoundEvent> ENTITY_PLANE_CRASH = register("entity.plane_crash");
    public static final RegistryObject<SoundEvent> ENTITY_PLANE_SHOT_DOWN = register("entity.plane_shot_down");
    public static final RegistryObject<SoundEvent> FSTBMB_PING = register("weapon.fstbmb_ping");
    public static final RegistryObject<SoundEvent> GRENADE_BOUNCE = register("weapon.grenade_bounce");
    public static final RegistryObject<SoundEvent> GUN_MINIGUN_FIRE = register("weapon.cal_shoot");
    public static final RegistryObject<SoundEvent> GUN_UNDERBARREL_FIRE = register("weapon.fire.underbarrel");
    public static final RegistryObject<SoundEvent> G_BOUNCE = register("weapon.g_bounce");
    public static final RegistryObject<SoundEvent> METEORITE_FALLING_LOOP = register("entity.meteorite_falling_loop");
    public static final RegistryObject<SoundEvent> MISSILE_TAKE_OFF = register("missile_take_off");
    public static final RegistryObject<SoundEvent> NULL_MINE = register("misc.null_mine");
    public static final RegistryObject<SoundEvent> OLD_EXPLOSION = register("entity.old_explosion");
    public static final RegistryObject<SoundEvent> ROBIN_EXPLOSION = register("weapon.robin_explosion");
    public static final RegistryObject<SoundEvent> SAW_SHOOT = register("weapon.saw_shoot");
    public static final RegistryObject<SoundEvent> TRAIN_IMPACT = register("weapon.train_impact");
    public static final RegistryObject<SoundEvent> UFO_BEAM = register("entity.ufo_beam");
    public static final RegistryObject<SoundEvent> UFO_BLAST = register("ufo_blast");
    public static final RegistryObject<SoundEvent> SQUEAKY_TOY = register("block.squeakytoy");

    // --- ntm-next entity port (gen_entity_sounds.py) ---
    public static final RegistryObject<SoundEvent> GRENADE_NUKA = register("weapon.reload.grenade_nuka");
    public static final RegistryObject<SoundEvent> GRENADE_TECH = register("weapon.reload.grenade_tech");

    // --- ntm-next entity port (gen_entity_sounds.py) ---
    public static final RegistryObject<SoundEvent> TESLA_ZAP = register("weapon.tesla");

    // --- ntm-next entity port (gen_entity_sounds.py) ---
    public static final RegistryObject<SoundEvent> CRATE_BREAK = register("crate_break");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                dev.golem.core.Ids.of(Constants.MOD_ID, name)));
    }

    public static void init() {}
}
