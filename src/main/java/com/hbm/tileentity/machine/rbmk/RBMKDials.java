package com.hbm.tileentity.machine.rbmk;

import com.hbm.config.GeneralConfig;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.WorldRules;
import dev.golem.core.world.WorldRules.BoolRule;
import dev.golem.core.world.WorldRules.DoubleRule;
import dev.golem.core.world.WorldRules.IntRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

/**
 * 1.7's RBMK dials: world rules set with {@code /gamerule dialXxx <value>}, saved with the world, read per call.
 * Ranges are 1.7's and apply when read. {@code getColumnHeight} is the rule minus one (blocks above the core).
 */
public final class RBMKDials {

    public static final BoolRule SAVE_DIALS = WorldRules.bool("dialSaveDials", true);
    public static final DoubleRule PASSIVE_COOLING = WorldRules.decimal("dialPassiveCooling", 2.5D, 0D, Double.MAX_VALUE);
    public static final DoubleRule PASSIVE_COOLING_INNER = WorldRules.decimal("dialPassiveCoolingInner", 0.1D, 0D, Double.MAX_VALUE);
    public static final DoubleRule COLUMN_HEAT_FLOW = WorldRules.decimal("dialColumnHeatFlow", 0.2D, 0D, 1D);
    public static final DoubleRule FUEL_DIFFUSION_MOD = WorldRules.decimal("dialDiffusionMod", 1D, 0D, Double.MAX_VALUE);
    public static final DoubleRule HEAT_PROVISION = WorldRules.decimal("dialHeatProvision", 0.2D, 0D, 1D);
    public static final IntRule COLUMN_HEIGHT = WorldRules.integer("dialColumnHeight", 4, 2, 16);
    public static final BoolRule PERMANENT_SCRAP = WorldRules.bool("dialEnablePermaScrap", true);
    public static final DoubleRule BOILER_HEAT_CONSUMPTION = WorldRules.decimal("dialBoilerHeatConsumption", 0.1D, 0D, Double.MAX_VALUE);
    public static final DoubleRule CONTROL_SPEED = WorldRules.decimal("dialControlSpeed", 1D, 0D, Double.MAX_VALUE);
    public static final DoubleRule REACTIVITY_MOD = WorldRules.decimal("dialReactivityMod", 1D, 0D, Double.MAX_VALUE);
    public static final DoubleRule OUTGASSER_MOD = WorldRules.decimal("dialOutgasserSpeedMod", 1D, 0D, Double.MAX_VALUE);
    public static final DoubleRule SURGE_MOD = WorldRules.decimal("dialControlSurgeMod", 1D, 0D, Double.MAX_VALUE);
    public static final IntRule FLUX_RANGE = WorldRules.integer("dialFluxRange", 5, 1, 100);
    public static final IntRule REASIM_RANGE = WorldRules.integer("dialReasimRange", 10, 1, 100);
    public static final BoolRule REASIM_BOILERS = WorldRules.bool("dialReasimBoilers", false);
    public static final DoubleRule REASIM_BOILER_SPEED = WorldRules.decimal("dialReasimBoilerSpeed", 0.05D, 0D, 1D);
    public static final BoolRule DISABLE_MELTDOWNS = WorldRules.bool("dialDisableMeltdowns", false);
    public static final BoolRule MELTDOWN_OVERPRESSURE = WorldRules.bool("dialEnableMeltdownOverpressure", false);
    public static final DoubleRule MODERATOR_EFFICIENCY = WorldRules.decimal("dialModeratorEfficiency", 1D, 0D, 1D);
    public static final DoubleRule ABSORBER_EFFICIENCY = WorldRules.decimal("dialAbsorberEfficiency", 1D, 0D, 1D);
    public static final DoubleRule REFLECTOR_EFFICIENCY = WorldRules.decimal("dialReflectorEfficiency", 1D, 0D, 1D);
    public static final BoolRule DISABLE_DEPLETION = WorldRules.bool("dialDisableDepletion", false);
    public static final BoolRule DISABLE_XENON = WorldRules.bool("dialDisableXenon", false);
    public static final DoubleRule ABSORBER_HEAT_CONVERSION = WorldRules.decimal("dialAbsorberHeatConversion", 0.05D, 0D, 1D);

    private static final WorldRules.Rule<?>[] ALL = {SAVE_DIALS, PASSIVE_COOLING, PASSIVE_COOLING_INNER, COLUMN_HEAT_FLOW,
            FUEL_DIFFUSION_MOD, HEAT_PROVISION, COLUMN_HEIGHT, PERMANENT_SCRAP, BOILER_HEAT_CONSUMPTION, CONTROL_SPEED,
            REACTIVITY_MOD, OUTGASSER_MOD, SURGE_MOD, FLUX_RANGE, REASIM_RANGE, REASIM_BOILERS, REASIM_BOILER_SPEED,
            DISABLE_MELTDOWNS, MELTDOWN_OVERPRESSURE, MODERATOR_EFFICIENCY, ABSORBER_EFFICIENCY, REFLECTOR_EFFICIENCY,
            DISABLE_DEPLETION, DISABLE_XENON, ABSORBER_HEAT_CONVERSION};

    private RBMKDials() {}

    /** Declares the rules at mod construction; with dialSaveDials off every dial returns to its default on world load. */
    public static void init() {
        Lifecycle.onServerStarted(server -> {
            if (!SAVE_DIALS.get(server.overworld())) resetAll(server);
        });
    }

    /** Every dial back to its default. */
    public static void resetAll(MinecraftServer server) {
        WorldRules.resetAll(server, ALL);
    }

    public static double getPassiveCooling(@Nullable LevelReader world) {
        return PASSIVE_COOLING.get(world);
    }

    public static double getPassiveCoolingInner(@Nullable LevelReader world) {
        return PASSIVE_COOLING_INNER.get(world);
    }

    public static double getColumnHeatFlow(@Nullable LevelReader world) {
        return COLUMN_HEAT_FLOW.get(world);
    }

    public static double getFuelDiffusionMod(@Nullable LevelReader world) {
        return FUEL_DIFFUSION_MOD.get(world);
    }

    public static double getFuelHeatProvision(@Nullable LevelReader world) {
        return HEAT_PROVISION.get(world);
    }

    /** Blocks above the core (the rule minus one); also valid during worldgen. */
    public static int getColumnHeight(@Nullable LevelReader world) {
        return COLUMN_HEIGHT.get(world) - 1;
    }

    /** The rule itself: block count including the core. */
    public static int getColumnHeightRuleValue(@Nullable LevelReader world) {
        return COLUMN_HEIGHT.get(world);
    }

    public static boolean getPermaScrap(@Nullable LevelReader world) {
        return PERMANENT_SCRAP.get(world);
    }

    public static double getBoilerHeatConsumption(@Nullable LevelReader world) {
        return BOILER_HEAT_CONSUMPTION.get(world);
    }

    public static double getControlSpeed(@Nullable LevelReader world) {
        return CONTROL_SPEED.get(world);
    }

    public static double getReactivityMod(@Nullable LevelReader world) {
        return REACTIVITY_MOD.get(world);
    }

    public static double getOutgasserMod(@Nullable LevelReader world) {
        return OUTGASSER_MOD.get(world);
    }

    public static double getSurgeMod(@Nullable LevelReader world) {
        return SURGE_MOD.get(world);
    }

    public static int getFluxRange(@Nullable LevelReader world) {
        return FLUX_RANGE.get(world);
    }

    public static int getReaSimRange(@Nullable LevelReader world) {
        return REASIM_RANGE.get(world);
    }

    /** The rule or hbm.cfg's forced ReaSim boilers (only on in 528 mode). */
    public static boolean getReasimBoilers(@Nullable LevelReader world) {
        return REASIM_BOILERS.get(world) || GeneralConfig.enable528ReasimBoilers;
    }

    public static double getReaSimBoilerSpeed(@Nullable LevelReader world) {
        return REASIM_BOILER_SPEED.get(world);
    }

    public static boolean getMeltdownsDisabled(@Nullable LevelReader world) {
        return DISABLE_MELTDOWNS.get(world);
    }

    public static boolean getOverpressure(@Nullable LevelReader world) {
        return MELTDOWN_OVERPRESSURE.get(world);
    }

    public static double getModeratorEfficiency(@Nullable LevelReader world) {
        return MODERATOR_EFFICIENCY.get(world);
    }

    public static double getAbsorberEfficiency(@Nullable LevelReader world) {
        return ABSORBER_EFFICIENCY.get(world);
    }

    public static double getAbsorberHeatConversion(@Nullable LevelReader world) {
        return ABSORBER_HEAT_CONVERSION.get(world);
    }

    public static double getReflectorEfficiency(@Nullable LevelReader world) {
        return REFLECTOR_EFFICIENCY.get(world);
    }

    public static boolean getDepletion(@Nullable LevelReader world) {
        return !DISABLE_DEPLETION.get(world);
    }

    public static boolean getXenon(@Nullable LevelReader world) {
        return !DISABLE_XENON.get(world);
    }
}
