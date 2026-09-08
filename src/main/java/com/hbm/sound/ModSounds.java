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
    public static final RegistryObject<SoundEvent> BOLTGUN = register("item.boltgun");
    public static final RegistryObject<SoundEvent> SYRINGE = register("item.syringe");
    public static final RegistryObject<SoundEvent> RADAWAY = register("item.radaway");
    public static final RegistryObject<SoundEvent> JETPACK_TANK = register("item.jetpack_tank");
    public static final RegistryObject<SoundEvent> VICE = register("item.vice");
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
    /** A missile leaving the pad. Old id {@code hbm:weapon.missileTakeoff}. */
    public static final RegistryObject<SoundEvent> MISSILE_TAKEOFF = register("weapon.missile_takeoff");

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

    /** Geiger click ladder, index 0..5 = old {@code geigerSounds}. */
    @SuppressWarnings("unchecked")
    public static final RegistryObject<SoundEvent>[] GEIGER_SOUNDS =
            new RegistryObject[] {GEIGER_1, GEIGER_2, GEIGER_3, GEIGER_4, GEIGER_5, GEIGER_6};

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                dev.golem.core.Ids.of(Constants.MOD_ID, name)));
    }

    public static void init() {}
}
