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
    public static final RegistryObject<SoundEvent> ASSEMBLER_STRIKE = register("block.assembler_strike");
    public static final RegistryObject<SoundEvent> ASSEMBLER_START = register("block.assembler_start");
    public static final RegistryObject<SoundEvent> ASSEMBLER_STOP = register("block.assembler_stop");
    public static final RegistryObject<SoundEvent> CHEMICAL_PLANT = register("block.chemical_plant");
    public static final RegistryObject<SoundEvent> PRESS_OPERATE = register("block.pressoperate");
    /** The electric heater's hum loop (old {@code block.electricHum}). */
    public static final RegistryObject<SoundEvent> ELECTRIC_HUM = register("block.electric_hum");
    /** The FEL's looping laser hum; its volume and pitch ramp with the machine's audioDuration. */
    public static final RegistryObject<SoundEvent> FEL = register("block.fel");

    /** Machine-upgrade insertion "plug" click (old {@code item.upgradePlug}). */
    public static final RegistryObject<SoundEvent> UPGRADE_PLUG = register("item.upgrade_plug");

    /** Handheld-gadget confirmation bleep (old {@code item.techBleep}) — oil detector etc. */
    public static final RegistryObject<SoundEvent> TECH_BLEEP = register("item.tech_bleep");
    /** The detonator's bind confirmation. Old id {@code hbm:item.techBoop}. */
    public static final RegistryObject<SoundEvent> TECH_BOOP = register("item.tech_boop");
    public static final RegistryObject<SoundEvent> BOLTGUN = register("item.boltgun");

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
    /** A missile leaving the pad. Old id {@code hbm:weapon.missileTakeoff}. */
    public static final RegistryObject<SoundEvent> MISSILE_TAKEOFF = register("weapon.missile_takeoff");

    /** Original RBMK/ZIRNOX reactor accident sound; used only by the vanilla ZIRNOX meltdown blast. */
    public static final RegistryObject<SoundEvent> RBMK_EXPLOSION = register("rbmk_explosion");
    public static final RegistryObject<SoundEvent> RBMK_AZ5_COVER = register("rbmk_az5_cover");
    public static final RegistryObject<SoundEvent> RBMK_SHUTDOWN = register("rbmk_shutdown");
    public static final RegistryObject<SoundEvent> STEAM_ENGINE_OPERATE = register("steam_engine_operate");

    /** Weapons. Old ids {@code weapon.fire.pistol}, {@code weapon.reload.dryFireClick} and
     * {@code weapon.revolverReload}, lowercased to satisfy 26.1's id charset. */
    public static final RegistryObject<SoundEvent> FIRE_PISTOL = register("weapon.fire.pistol");
    public static final RegistryObject<SoundEvent> DRY_FIRE_CLICK = register("weapon.reload.dryfireclick");
    public static final RegistryObject<SoundEvent> RELOAD_REVOLVER = register("weapon.revolverreload");

    /**
     * Power-armour footsteps (old {@code metalStep}, {@code ironJump}, {@code ironLand}). Played by
     * {@code ArmorFSB} on top of the vanilla step, which is why the step one is a single clank rather
     * than a variant set.
     */
    public static final RegistryObject<SoundEvent> METAL_STEP = register("step.metal");
    public static final RegistryObject<SoundEvent> IRON_JUMP = register("step.iron_jump");
    public static final RegistryObject<SoundEvent> IRON_LAND = register("step.iron_land");

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
