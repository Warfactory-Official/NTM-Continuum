package com.hbm.gametest;

import dev.golem.core.platform.Platform;

/** Loads every GameTest suite class; Golem Core registers each constructed suite. */
public final class HbmGameTests {

    private HbmGameTests() {}

    /** Before registries flush: IntakeFanGameTests owns a test-only block. */
    public static void init() {
        HbmGameTestFunctions.init();
        FoodCatalystGameTests.init();
        OutgasGameTests.init();
        SteelStructureGameTests.init();
        HazardGameTests.init();
        ToolingGameTests.init();
        AssemblyFactoryGameTests.init();
        ChemicalFactoryGameTests.init();
        AutosawGameTests.init();
        SolarGameTests.init();
        IntakeFanGameTests.init();
        DiFurnaceGameTests.init();
        MaterialCompletionGameTests.init();
        BedrockOreGameTests.init();
        ExplosiveContentGameTests.init();
        Bombs2GameTests.init();
        NtmEntityPortGameTests.init();
        Gen3SednaGameTests.init();
        RadioGameTests.init();
        CounterControllerGameTests.init();
        TelexGameTests.init();
        AutocalGameTests.init();
        RadioProviderGameTests.init();
        RtgGameTests.init();
        SingularityGameTests.init();
        TurretGameTests.init();
        PowerNetworkGameTests.init();
        NetworkReachGameTests.init();
        FluidNetworkGameTests.init();
        ForeignEnergyGameTests.init();
        FoldedGeometryGameTests.init();
        NetworkSyncGameTests.init();
        ChunkPromotionGameTests.init();
        TickPhaseGameTests.init();
        MultiblockMenuGameTests.init();
        RadiationPersistenceGameTests.init();
        FoldedPortParityGameTests.init();
        WeaponTableGameTests.init();
        GunSupportGameTests.init();
        PollutionGameTests.init();
        FlywheelGameTests.init();
        ContentIdGameTests.init();
        PipelineGameTests.init();
        if (Platform.isModLoaded("kubejs")) {
            ScriptingGameTests.init();
            ScriptingMaterialsGameTests.init();
            ScriptingFuelsGameTests.init();
            ScriptingEventsGameTests.init();
        }
    }
}
