package com.hbm.client;

/** Registers every HBM query with the test harness. Release jars drop it and its callers. */
public final class HbmHarnessQueries {

    private HbmHarnessQueries() {}

    public static void registerAll() {
        SpecialPlayerHarnessQueries.register();
        ArmorModHarnessQueries.register();
        StructureHarnessQueries.register();
        WorldFeatureHarnessQueries.register();
        SmallHarnessQueries.register();
        ReloadedGunHarnessQueries.register();
        ComponentHarnessQueries.register();
        LegacyDungeonHarnessQueries.register();
        GapsAHarnessQueries.register();
        GapsBHarnessQueries.register();
        ForeignFluidHarnessQueries.register();
        BlockInfoHarnessQueries.register();
        RbmkConsoleHarnessQueries.register();
        MeleeKitHarnessQueries.register();
        GapsCHarnessQueries.register();
        ConnectedTextureHarnessQueries.register();
        IconAuditHarnessQueries.register();
        UtilityToolHarnessQueries.register();
        AdvancementHarnessQueries.register();
        AnvilHarnessQueries.register();
        FxCablesHarnessQueries.register();
    }
}
