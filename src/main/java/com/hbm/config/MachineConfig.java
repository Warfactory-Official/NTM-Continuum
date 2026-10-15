package com.hbm.config;

/**
 * 1.7 {@code MachineConfig}, category {@code 09_machines} of {@code hbm.cfg}, plus the two {@code VersatileConfig}
 * questions that combine it with 528 mode. The port-only {@code 9.5x} keys set the HE:FE exchange in
 * {@link GeneralConfig}. Plain Java: {@code ModRtgContent} (a build-time declaration class) reads {@link #rtgDecay()}
 * for the gold and lead pellet heat.
 */
public final class MachineConfig {

    private MachineConfig() {}

    /** 1.7 {@code 9.00_scaleRTGPower}: RTG/betavoltaic output falls with the pellet's decay. Off by default. */
    public static boolean scaleRTGPower = false;
    /** 1.7 {@code 9.01_doRTGsDecay}: RTG/betavoltaic pellets decay at all. */
    public static boolean doRTGsDecay = true;

    /** 1.7 {@code VersatileConfig.rtgDecay()}. */
    public static boolean rtgDecay() {
        return GeneralConfig.enable528 || doRTGsDecay;
    }

    /** 1.7 {@code VersatileConfig.scaleRTGPower()}. */
    public static boolean scaleRTGPowerActive() {
        return GeneralConfig.enable528 || scaleRTGPower;
    }

    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_MACHINE = CommonConfig.CATEGORY_MACHINES;

        scaleRTGPower = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.00_scaleRTGPower", "Should RTG/Betavoltaic fuel power scale down as it decays?", false);
        doRTGsDecay = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.01_doRTGsDecay", "Should RTG/Betavoltaic fuel decay at all?", true);

        // port: cables convert HE to FE for other mods' machines; 1.7 had only the converter blocks.
        GeneralConfig.energyRatioHe = Math.max(1, CommonConfig.createConfigInt(config, CATEGORY_MACHINE, "9.50_energyRatioHE", "HE side of the HE:FE exchange used where HBM cables meet other mods' FE/RF machines. energyRatioHE HE is worth energyRatioFE FE, both ways. Minimum 1.", 5));
        GeneralConfig.energyRatioFe = Math.max(1, CommonConfig.createConfigInt(config, CATEGORY_MACHINE, "9.51_energyRatioFE", "FE side of the HE:FE exchange. See 9.50_energyRatioHE. Minimum 1.", 1));
        GeneralConfig.foreignExportReserve = Math.min(1D, Math.max(0D, CommonConfig.createConfigDouble(config, CATEGORY_MACHINE, "9.52_foreignExportReserve", "Fraction (0 to 1) of a power net's supply that other mods' FE machines may take at normal priority, ahead of HBM machines. 0 feeds them only from the surplus HBM machines leave.", 0D)));
    }
}
