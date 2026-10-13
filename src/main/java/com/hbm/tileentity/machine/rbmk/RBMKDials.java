package com.hbm.tileentity.machine.rbmk;

import com.hbm.config.GeneralConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * 1.7/1.12.2 gamerule "dials" ported as static config constants (no gamerule-dial system here), keeping the 1.7 getter API (Level params ignored; getColumnHeight takes LevelAccessor so RBMKBase.fillSpace can call it from worldgen); defaults byte-identical between the two authorities; getColumnHeight returns columnHeight-1 ("blocks above the core").
 */
public final class RBMKDials {

    // 1.12.2: gamerule defaults verbatim
    public static double passiveCooling = 2.5D;
    public static double passiveCoolingInner = 0.1D;
    public static double columnHeatFlow = 0.2D;
    public static double fuelDiffusionMod = 1.0D;
    public static double heatProvision = 0.2D;
    public static int columnHeight = 4;           // clamp [2,16]; block count incl. core
    public static boolean permaScrap = true;
    public static double boilerHeatConsumption = 0.1D;
    public static double controlSpeed = 1.0D;
    public static double reactivityMod = 1.0D;
    public static double outgasserMod = 1.0D;
    public static double surgeMod = 1.0D;
    public static int fluxRange = 5;
    public static boolean reasimBoilers = false;
    public static double reasimBoilerSpeed = 0.05D;
    public static boolean disableMeltdowns = false;
    public static boolean enableMeltdownOverpressure = false;
    public static double moderatorEfficiency = 1.0D;
    public static double absorberEfficiency = 1.0D;
    public static double reflectorEfficiency = 1.0D;
    public static boolean disableDepletion = false;
    public static boolean disableXenon = false;
    public static double absorberHeatConversion = 0.05D;
    private RBMKDials() {
    }

    public static double getPassiveCooling(Level world) {
        return passiveCooling;
    }

    public static double getPassiveCoolingInner(Level world) {
        return passiveCoolingInner;
    }

    public static double getColumnHeatFlow(Level world) {
        return columnHeatFlow;
    }

    public static double getFuelDiffusionMod(Level world) {
        return fuelDiffusionMod;
    }

    public static double getFuelHeatProvision(Level world) {
        return heatProvision;
    }

    /**
     * "Blocks above the core" (= ruleValue - 1); used by fill/melt/TESR loops.
     */
    public static int getColumnHeight(LevelAccessor world) {
        return Mth.clamp(columnHeight, 2, 16) - 1;
    }

    /**
     * Raw block count incl. the core (= ruleValue, clamp [2,16]); used by the baked model for lid Y.
     */
    public static int getColumnHeightRuleValue(Level world) {
        return Mth.clamp(columnHeight, 2, 16);
    }

    public static boolean getPermaScrap(Level world) {
        return permaScrap;
    }

    public static double getBoilerHeatConsumption(Level world) {
        return boilerHeatConsumption;
    }

    public static double getControlSpeed(Level world) {
        return controlSpeed;
    }

    public static double getReactivityMod(Level world) {
        return reactivityMod;
    }

    public static double getOutgasserMod(Level world) {
        return outgasserMod;
    }

    public static double getSurgeMod(Level world) {
        return surgeMod;
    }

    public static int getFluxRange(Level world) {
        return fluxRange;
    }

    public static boolean getReasimBoilers(Level world) {
        // 1.7: the gamerule OR hbm.cfg X528_forceReasimBoilers (forced off outside 528 mode)
        return reasimBoilers || GeneralConfig.enable528ReasimBoilers;
    }

    public static double getReaSimBoilerSpeed(Level world) {
        return reasimBoilerSpeed;
    }

    public static boolean getMeltdownsDisabled(Level world) {
        return disableMeltdowns;
    }

    public static boolean getOverpressure(Level world) {
        return enableMeltdownOverpressure;
    }

    public static double getModeratorEfficiency(Level world) {
        return moderatorEfficiency;
    }

    public static double getAbsorberEfficiency(Level world) {
        return absorberEfficiency;
    }

    public static double getReflectorEfficiency(Level world) {
        return reflectorEfficiency;
    }

    public static double getAbsorberHeatConversion(Level world) {
        return absorberHeatConversion;
    }

    public static boolean getDepletion(Level world) {
        return !disableDepletion;
    }

    public static boolean getXenon(Level world) {
        return !disableXenon;
    }
}
