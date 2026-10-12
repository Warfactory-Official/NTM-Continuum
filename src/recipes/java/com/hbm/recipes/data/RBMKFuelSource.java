package com.hbm.recipes.data;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.ReactorDataMaps.RBMKFuel;
import com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc;
import com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc;
import com.hbm.items.machine.ItemRBMKRod.NType;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/**
 * 1.7 {@code ModItems.initializeItem}: the {@code new ItemRBMKRod(pellet)} setter chains, in their order, with
 * {@code ItemRBMKRod}'s field defaults. A rod's id is its unlocalized name ({@code rbmk_fuel_hep239} is
 * {@code rbmk_fuel_hep}); the pellet is the recycling pellet.
 */
public final class RBMKFuelSource extends DataMapSource<RBMKFuel> {

    public RBMKFuelSource() {
        super(ReactorDataMaps.RBMK_FUEL, false);
    }

    /** 1.7 {@code ItemRBMKRod}'s fields and setters. */
    private final class Rod {
        private final String name;
        private final String pellet;
        double reactivity;                                   //endpoint of the function
        double selfRate;                                     //self-inflicted flux from self-igniting fuels
        EnumBurnFunc function = EnumBurnFunc.LOG_TEN;
        EnumDepleteFunc depFunc = EnumDepleteFunc.GENTLE_SLOPE;
        double xGen = 0.5D;                                  //multiplier for xenon production
        double xBurn = 50D;                                  //divider for xenon burnup
        double heat = 1D;                                    //heat produced per outFlux
        double yield;                                        //total potential inFlux the rod can take in its lifetime
        double meltingPoint = 1000D;                         //the maximum heat of the rod's hull before shit hits the fan
        double diffusion = 0.02D;                            //the speed at which the core heats the hull
        NType nType = NType.SLOW;                            //neutronType, the most efficient neutron type for fission
        NType rType = NType.FAST;                            //releaseType, the type of neutrons released by this fuel
        int colorTint = 0x304825;                            //RGB color of the rod when rendered in the fuel channel
        double heatCoeffStart = 0D;                          //when the heat coefficient starts acting
        double heatCoeffLength = 0D;                         //when the reaction multiplier of the coefficient hits 0

        Rod(String name, String pellet) {
            this.name = name;
            this.pellet = pellet;
        }

        Rod setYield(double yield) { this.yield = yield; return this; }
        Rod setStats(double funcEnd) { return setStats(funcEnd, 0); }
        Rod setStats(double funcEnd, double selfRate) { this.reactivity = funcEnd; this.selfRate = selfRate; return this; }
        Rod setFunction(EnumBurnFunc func) { this.function = func; return this; }
        Rod setDepletionFunction(EnumDepleteFunc func) { this.depFunc = func; return this; }
        Rod setXenon(double gen, double burn) { this.xGen = gen; this.xBurn = burn; return this; }
        Rod setHeat(double heat) { this.heat = heat; return this; }
        Rod setHeatCoeff(double start, double length) { this.heatCoeffStart = start; this.heatCoeffLength = length; return this; }
        Rod setDiffusion(double diffusion) { this.diffusion = diffusion; return this; }
        Rod setMeltingPoint(double meltingPoint) { this.meltingPoint = meltingPoint; return this; }
        Rod setNeutronTypes(NType nType, NType rType) { this.nType = nType; this.rType = rType; return this; }

        /** The chain's end ({@code setUnlocalizedName}): the rod is registered. */
        void setTint(int tint) {
            this.colorTint = tint;
            register();
        }

        void register() {
            put("hbm:" + name, new RBMKFuel(yield, reactivity, selfRate, function, depFunc, xGen, xBurn, heat, diffusion,
                    meltingPoint, heatCoeffStart, heatCoeffLength, nType, rType, colorTint,
                    Optional.ofNullable(pellet).map(p -> "hbm:" + p)));
        }
    }

    /** 1.7 {@code new ItemRBMKRod(pellet)}, named by the rod's unlocalized name. */
    private Rod rod(String name, String pellet) {
        return new Rod(name, pellet);
    }

    @Override
    protected void values() {
        int tintUranium = 0x868D82;
        int tintNeptunium = 0x757E73;
        int tintPlutonium = 0x656E6B;
        int tintAmericium = 0xA88A8F;
        int tintThorium = 0x665448;
        int tintZirconium = 0xAAA36A;
        int tintSchrabidium = 0x2D9A94;
        int tintPolonium = 0x563A26;
        int tintRadium = 0xB3B6AD;
        int tintAustralium = 0xFFEE00;
        int tintFlashgold = 0xDC9613;
        int tintFlashlead = 0x7B7B87;
        int tintBalefire = 0xB2FF1B;
        int tintDRX = 0xD77276;

        rod("rbmk_fuel_ueu", "rbmk_pellet_ueu")
                .setYield(100_000_000D)
                .setStats(15)
                .setFunction(EnumBurnFunc.LOG_TEN)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                .setHeat(0.65) //0.5 is too much of a nerf in heat; pu239 buildup justifies it being on par with MEU ig
                .setMeltingPoint(2865)
                .setTint(tintUranium);
        rod("rbmk_fuel_meu", "rbmk_pellet_meu")
                .setYield(100_000_000D)
                .setStats(20)
                .setFunction(EnumBurnFunc.LOG_TEN)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                .setHeat(0.65) //0.75 was a bit too much...
                .setMeltingPoint(2865)
                .setTint(tintUranium);
        rod("rbmk_fuel_heu233", "rbmk_pellet_heu233")
                .setYield(100_000_000D)
                .setStats(27.5D)
                .setFunction(EnumBurnFunc.LINEAR)
                .setHeat(1.25D)
                .setMeltingPoint(2865)
                .setTint(tintUranium);
        rod("rbmk_fuel_heu235", "rbmk_pellet_heu235")
                .setYield(100_000_000D)
                .setStats(50) //Consistency with HEN; its critical mass is too high to justify a linear function
                .setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setMeltingPoint(2865)
                .setTint(tintUranium);
        rod("rbmk_fuel_uzh", "rbmk_pellet_uzh")
                .setYield(50_000_000D)
                .setStats(30)
                .setFunction(EnumBurnFunc.LOG_TEN)
                .setDepletionFunction(EnumDepleteFunc.GENTLE_SLOPE)
                .setHeat(0.75)
                .setHeatCoeff(1_000D, 500D)
                .setDiffusion(0.1D)
                .setMeltingPoint(1845)
                .setTint(0x7077AF);
        rod("rbmk_fuel_thmeu", "rbmk_pellet_thmeu")
                .setYield(100000000D)
                .setStats(20)
                .setFunction(EnumBurnFunc.PLATEU)
                .setDepletionFunction(EnumDepleteFunc.BOOSTED_SLOPE)
                .setHeat(0.65D) //Consistency with MEU
                .setMeltingPoint(3350)
                .setTint(tintThorium);
        rod("rbmk_fuel_lep", "rbmk_pellet_lep")
                .setYield(100000000D)
                .setStats(35)
                .setFunction(EnumBurnFunc.LOG_TEN)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                .setHeat(0.75D)
                .setMeltingPoint(2744)
                .setTint(tintPlutonium);
        rod("rbmk_fuel_mep", "rbmk_pellet_mep")
                .setYield(100000000D)
                .setStats(35)
                .setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setMeltingPoint(2744)
                .setTint(tintPlutonium);
        // 1.7 rbmk_fuel_hep239 (unlocalized name rbmk_fuel_hep)
        rod("rbmk_fuel_hep", "rbmk_pellet_hep")
                .setYield(100000000D)
                .setStats(30)
                .setFunction(EnumBurnFunc.LINEAR)
                .setHeat(1.25D)
                .setMeltingPoint(2744)
                .setTint(tintPlutonium);
        rod("rbmk_fuel_hep241", "rbmk_pellet_hep241")
                .setYield(100000000D)
                .setStats(40)
                .setFunction(EnumBurnFunc.LINEAR)
                .setHeat(1.75D)
                .setMeltingPoint(2744)
                .setTint(tintPlutonium);
        rod("rbmk_fuel_lea", "rbmk_pellet_lea")
                .setYield(100000000D)
                .setStats(60, 10)
                .setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                .setHeat(1.5D)
                .setMeltingPoint(2386)
                .setTint(tintAmericium);
        rod("rbmk_fuel_mea", "rbmk_pellet_mea")
                .setYield(100000000D)
                .setStats(35D, 20)
                .setFunction(EnumBurnFunc.ARCH)
                .setHeat(1.75D)
                .setMeltingPoint(2386)
                .setTint(tintAmericium);
        rod("rbmk_fuel_hea241", "rbmk_pellet_hea241")
                .setYield(100000000D)
                .setStats(65, 15)
                .setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setHeat(1.85D)
                .setMeltingPoint(2386)
                .setNeutronTypes(NType.FAST, NType.FAST)
                .setTint(tintAmericium);
        rod("rbmk_fuel_hea242", "rbmk_pellet_hea242")
                .setYield(100000000D)
                .setStats(45)
                .setFunction(EnumBurnFunc.LINEAR)
                .setHeat(2D)
                .setMeltingPoint(2386)
                .setTint(tintAmericium);
        rod("rbmk_fuel_men", "rbmk_pellet_men")
                .setYield(100000000D)
                .setStats(30)
                .setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                .setHeat(0.75)
                .setMeltingPoint(2800)
                .setNeutronTypes(NType.ANY, NType.FAST) //Build-up of Pu-239 leads to both speeds of neutrons grooving
                .setTint(tintNeptunium);
        rod("rbmk_fuel_hen", "rbmk_pellet_hen")
                .setYield(100000000D)
                .setStats(40)
                .setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setMeltingPoint(2800)
                .setNeutronTypes(NType.FAST, NType.FAST)
                .setTint(tintNeptunium);
        rod("rbmk_fuel_mox", "rbmk_pellet_mox")
                .setYield(100000000D)
                .setStats(40)
                .setFunction(EnumBurnFunc.LOG_TEN)
                .setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE)
                .setMeltingPoint(2815)
                .setTint(tintUranium);
        rod("rbmk_fuel_les", "rbmk_pellet_les")
                .setYield(100000000D)
                .setStats(50)
                .setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setHeat(1.25D)
                .setMeltingPoint(2500)
                .setNeutronTypes(NType.SLOW, NType.SLOW) //Beryllium Moderation
                .setTint(tintSchrabidium);
        rod("rbmk_fuel_mes", "rbmk_pellet_mes")
                .setYield(100000000D)
                .setStats(75D)
                .setFunction(EnumBurnFunc.ARCH)
                .setHeat(1.5D)
                .setMeltingPoint(2750)
                .setTint(tintSchrabidium);
        rod("rbmk_fuel_hes", "rbmk_pellet_hes")
                .setYield(100000000D)
                .setStats(90)
                .setFunction(EnumBurnFunc.LINEAR)
                .setDepletionFunction(EnumDepleteFunc.LINEAR)
                .setHeat(1.75D)
                .setMeltingPoint(3000)
                .setTint(tintSchrabidium);
        rod("rbmk_fuel_leaus", "rbmk_pellet_leaus")
                .setYield(100000000D)
                .setStats(30)
                .setFunction(EnumBurnFunc.SIGMOID)
                .setDepletionFunction(EnumDepleteFunc.LINEAR)
                .setXenon(0.05D, 50D)
                .setHeat(1.5D)
                .setMeltingPoint(7029) // 1.7: setTint(tintAustralium).setMeltingPoint(7029)
                .setTint(tintAustralium);
        rod("rbmk_fuel_heaus", "rbmk_pellet_heaus")
                .setYield(100000000D)
                .setStats(35)
                .setFunction(EnumBurnFunc.LINEAR)
                .setXenon(0.05D, 50D)
                .setHeat(1.5D)
                .setMeltingPoint(5211) // 1.7: setTint(tintAustralium).setMeltingPoint(5211)
                .setTint(tintAustralium);
        rod("rbmk_fuel_po210be", "rbmk_pellet_po210be")
                .setYield(25000000D)
                .setStats(0D, 50)
                .setFunction(EnumBurnFunc.PASSIVE)
                .setDepletionFunction(EnumDepleteFunc.LINEAR)
                .setXenon(0.0D, 50D)
                .setHeat(0.1D)
                .setDiffusion(0.05D)
                .setMeltingPoint(1287)
                .setNeutronTypes(NType.SLOW, NType.SLOW) //Beryllium Moderation
                .setTint(tintPolonium);
        rod("rbmk_fuel_ra226be", "rbmk_pellet_ra226be")
                .setYield(100000000D)
                .setStats(0D, 20)
                .setFunction(EnumBurnFunc.PASSIVE)
                .setDepletionFunction(EnumDepleteFunc.LINEAR)
                .setXenon(0.0D, 50D)
                .setHeat(0.035D)
                .setDiffusion(0.5D)
                .setMeltingPoint(700)
                .setNeutronTypes(NType.SLOW, NType.SLOW) //Beryllium Moderation
                .setTint(tintRadium);
        rod("rbmk_fuel_pu238be", "rbmk_pellet_pu238be")
                .setYield(50000000D)
                .setStats(40, 40)
                .setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setHeat(0.1D)
                .setDiffusion(0.05D)
                .setMeltingPoint(1287)
                .setNeutronTypes(NType.SLOW, NType.SLOW) //Beryllium Moderation
                .setTint(tintPlutonium);
        rod("rbmk_fuel_balefire_gold", "rbmk_pellet_balefire_gold")
                .setYield(100000000D)
                .setStats(50, 10)
                .setFunction(EnumBurnFunc.ARCH)
                .setDepletionFunction(EnumDepleteFunc.LINEAR)
                .setXenon(0.0D, 50D)
                .setMeltingPoint(2000)
                .setTint(tintFlashgold);
        rod("rbmk_fuel_flashlead", "rbmk_pellet_flashlead")
                .setYield(250000000D)
                .setStats(40, 50)
                .setFunction(EnumBurnFunc.ARCH)
                .setDepletionFunction(EnumDepleteFunc.LINEAR)
                .setXenon(0.0D, 50D)
                .setMeltingPoint(2050)
                .setTint(tintFlashlead);
        rod("rbmk_fuel_balefire", "rbmk_pellet_balefire")
                .setYield(100000000D)
                .setStats(100, 35)
                .setFunction(EnumBurnFunc.LINEAR)
                .setXenon(0.0D, 50D)
                .setHeat(3D)
                .setMeltingPoint(3652)
                .setTint(tintBalefire);
        rod("rbmk_fuel_zfb_bismuth", "rbmk_pellet_zfb_bismuth")
                .setYield(50000000D)
                .setStats(20)
                .setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setHeat(1.75D)
                .setMeltingPoint(2744)
                .setTint(tintZirconium);
        rod("rbmk_fuel_zfb_pu241", "rbmk_pellet_zfb_pu241")
                .setYield(50000000D)
                .setStats(20)
                .setFunction(EnumBurnFunc.SQUARE_ROOT)
                .setMeltingPoint(2865)
                .setTint(tintZirconium);
        rod("rbmk_fuel_zfb_am_mix", "rbmk_pellet_zfb_am_mix")
                .setYield(50000000D)
                .setStats(20)
                .setFunction(EnumBurnFunc.LINEAR)
                .setHeat(1.75D)
                .setMeltingPoint(2744)
                .setTint(tintZirconium);
        rod("rbmk_fuel_drx", "rbmk_pellet_drx")
                .setYield(10000000D)
                .setStats(1000, 10)
                .setFunction(EnumBurnFunc.QUADRATIC)
                .setHeat(0.1D)
                .setMeltingPoint(100000)
                .setTint(tintDRX);
        // 1.7 new ItemRBMKRod("THE VOICES"): no pellet, no tint
        rod("rbmk_fuel_test", null)
                .setYield(1000000D)
                .setStats(100)
                .setFunction(EnumBurnFunc.EXPERIMENTAL)
                .setHeat(1.0D)
                .setMeltingPoint(100000)
                .register();
    }
}
