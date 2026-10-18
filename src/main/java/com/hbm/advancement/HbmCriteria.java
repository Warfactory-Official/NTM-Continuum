package com.hbm.advancement;

import com.hbm.Constants;
import dev.golem.core.advancement.Trigger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The 1.7 {@code triggerAchievement} sites a stock trigger cannot observe, as real advancement criterion
 * triggers. Each is a Golem Core {@link Trigger}, which registers it on every node and gives every one of
 * them the same optional JSON conditions ({@code player}, {@code kinds}, {@code item}, {@code value});
 * {@link HbmAdvancementDeclarations} says which event grants which advancement.
 *
 * <p>Who is credited, within what radius, stays at the call site, exactly as 1.7 had it: this class only
 * turns "this player saw this happen" into a criterion. Nothing here grants an advancement 1.7 did not.
 */
public final class HbmCriteria {

    private HbmCriteria() {}

    /** 1.7 {@code AchievementHandler.fire}: an item taken from a machine output or made on the anvil. */
    public static final Trigger ITEM_PRODUCED = Trigger.register(Constants.MOD_ID, "item_produced");
    /** 1.7 {@code EntityEffectHandler.handleRadiation}: kind {@code poison} at 200 RAD, {@code death} at 1000. */
    public static final Trigger RADIATION = Trigger.register(Constants.MOD_ID, "radiation");
    /** 1.7 {@code HbmLivingProps.setDigamma}: value is the new digamma level. */
    public static final Trigger DIGAMMA = Trigger.register(Constants.MOD_ID, "digamma");
    /** A blast or meltdown the player was near; kinds are {@link DetonationTrigger.Kind} names. */
    public static final Trigger DETONATION = Trigger.register(Constants.MOD_ID, "detonation");
    /** A boss died near the player; kinds are {@link BossKilledTrigger.Kind} names. */
    public static final Trigger BOSS_KILLED = Trigger.register(Constants.MOD_ID, "boss_killed");
    /** A payload reached orbit (the stack is the payload). */
    public static final Trigger ORBIT = Trigger.register(Constants.MOD_ID, "orbit");
    /** A satellite already in orbit carried out its command (the stack names the satellite). */
    public static final Trigger SATELLITE_ACTION = Trigger.register(Constants.MOD_ID, "satellite_action");
    /** 1.7 {@code TileEntityPADetector}: a collision produced a particle (the stack) near the player. */
    public static final Trigger PARTICLE_PRODUCED = Trigger.register(Constants.MOD_ID, "particle_produced");
    /** 1.7 {@code ItemCigarette.onEaten}: the player finished smoking the stack. */
    public static final Trigger SMOKED = Trigger.register(Constants.MOD_ID, "smoked");
    /** 1.7 {@code ItemEnergy.onEaten}: the player finished drinking the stack. */
    public static final Trigger CONSUMED = Trigger.register(Constants.MOD_ID, "consumed");
    /** 1.7 {@code BlockKeyhole}/{@code BlockRedBrickKeyhole}: the player opened a red room. */
    public static final Trigger ROOM_OPENED = Trigger.register(Constants.MOD_ID, "room_opened");
    /** 1.7 {@code ItemModKnife}: the Ghost of Kauai-Moho's bleeding took the player's last max health. */
    public static final Trigger BLED_OUT = Trigger.register(Constants.MOD_ID, "bled_out");
    /** 1.7 {@code ItemBoltgun}: the player was killed by a boltgun bolt (credited to the victim). */
    public static final Trigger BOLTED = Trigger.register(Constants.MOD_ID, "bolted");
    /** 1.7 {@code GenericFluidBlock}: an item (the stack) dissolved in acid near the player. */
    public static final Trigger DISSOLVED = Trigger.register(Constants.MOD_ID, "dissolved");
    /** 1.7 {@code ModEventHandler.onEntityDeath}: a falling boxcar crushed an entity (kind: its type id) nearby. */
    public static final Trigger CRUSHED = Trigger.register(Constants.MOD_ID, "crushed");

    /** Forces class initialisation, which registers every trigger; called from {@code MainRegistry}. */
    public static void init() {}

    /** 1.7 {@code EntityRocketSoyuz.deployPayload} and {@code SatelliteHorizons.onOrbit}. */
    public static void orbit(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) ORBIT.fire(player, stack);
    }

    /** 1.7 {@code SatelliteHorizons:83} - a satellite already in orbit carried out its command. */
    public static void satelliteAction(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) SATELLITE_ACTION.fire(player, stack);
    }

    public static void detonation(ServerPlayer player, DetonationTrigger.Kind kind) {
        DETONATION.fire(player, kind.getSerializedName());
    }

    /** 1.7 {@code AchievementHandler.fire}; the empty and client cases are ignored as 1.7 did. */
    public static void produced(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer server && stack != null && !stack.isEmpty()) ITEM_PRODUCED.fire(server, stack);
    }

    /**
     * Credits every player inside the boss's bounding box grown by {@code radius} on each axis: 1.7's
     * {@code boundingBox.expand(r, r, r)}, a cube, not a sphere.
     */
    public static void bossKilled(Entity boss, BossKilledTrigger.Kind kind, double radius) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        AwardRegions.nearby(level, boss.getBoundingBox().inflate(radius),
                player -> BOSS_KILLED.fire(player, kind.getSerializedName()));
    }
}
