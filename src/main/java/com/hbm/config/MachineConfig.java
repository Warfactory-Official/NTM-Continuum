package com.hbm.config;

/**
 * 1.7 {@code MachineConfig}, category {@code 09_machines} of {@code hbm.cfg}, plus the two {@code VersatileConfig}
 * questions that combine it with 528 mode. Plain Java: {@code ModRtgContent} (a build-time declaration class) reads
 * {@link #rtgDecay()} for the gold and lead pellet heat. History §19ej-8.
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
    }
}
