package com.hbm.platform.fabric;

/**
 * The {@code fabric-gametest} entrypoint. Fabric API registers this class by reflection and runs its
 * {@code @GameTestGenerator} when the server starts with {@code -Dfabric-api.gametest}
 * ({@code gradlew :<node>-fabric:runGametest}); the generator hands over every suite Golem collected, the
 * same list the Forge init registers through {@code RegisterGameTestsEvent}, after each suite's
 * {@code init()} (the side hooks some suites listen through, which ForgeInit calls at construction).
 * 26.1 has no generator API; its Fabric API registry hook is forwarded to Golem Core by the
 * loader-specific mixin, while this entrypoint still owns suite initialisation.
 */
public final class FabricGameTests {

    public FabricGameTests() {}

    private static boolean initialised;

    /**
     * Runs every suite's {@code init()} once. FabricInit calls this before the registries are flushed,
     * because a suite that owns a test-only block (IntakeFanGameTests) must have created its register by
     * then; the legacy generator below calls it as well in case the gametest entrypoint runs first.
     */
    public static synchronized void initAll() {
        if (initialised) return;
        initialised = true;
        com.hbm.gametest.HbmGameTestFunctions.init();
        com.hbm.gametest.FoodCatalystGameTests.init();
        com.hbm.gametest.OutgasGameTests.init();
        com.hbm.gametest.SteelStructureGameTests.init();
        com.hbm.gametest.HazardGameTests.init();
        com.hbm.gametest.ToolingGameTests.init();
        com.hbm.gametest.AbilityToolGameTests.init();
        com.hbm.gametest.MeleeKitGameTests.init();
        com.hbm.gametest.FinalCellHudFixGameTests.init();
        com.hbm.gametest.FinalGapsAGameTests.init();
        com.hbm.gametest.FinalGapsBGameTests.init();
        com.hbm.gametest.AdvancementCommandGameTests.init();
        com.hbm.gametest.DecoCrateGameTests.init();
        com.hbm.gametest.FinalConsumableGameTests.init();
        com.hbm.gametest.FinalRecipeGapGameTests.init();
        com.hbm.gametest.Fix1AGameTests.init();
        com.hbm.gametest.IdsGameTests.init();
        com.hbm.gametest.ForeignFluidGameTests.init();
        com.hbm.gametest.Fix4GameTests.init();
        com.hbm.gametest.Fix5GameTests.init();
        com.hbm.gametest.Fix1BGameTests.init();
        com.hbm.gametest.Fix3GameTests.init();
        com.hbm.gametest.Fix7GameTests.init();
        com.hbm.gametest.SmallGameTests.init();
        com.hbm.gametest.Fix7GlyphidGameTests.init();
        com.hbm.gametest.Fix7BossGameTests.init();
        com.hbm.gametest.Fix7MobGameTests.init();
        com.hbm.gametest.RecipeContentGameTests.init();
        com.hbm.gametest.UtilityToolGameTests.init();
        com.hbm.gametest.AssemblyFactoryGameTests.init();
        com.hbm.gametest.ChemicalFactoryGameTests.init();
        com.hbm.gametest.AutosawGameTests.init();
        com.hbm.gametest.SolarGameTests.init();
        com.hbm.gametest.IntakeFanGameTests.init();
        com.hbm.gametest.DiFurnaceGameTests.init();
        com.hbm.gametest.MaterialCompletionGameTests.init();
        com.hbm.gametest.BedrockOreGameTests.init();
        com.hbm.gametest.ExplosiveContentGameTests.init();
        com.hbm.gametest.Bombs2GameTests.init();
        com.hbm.gametest.NtmEntityPortGameTests.init();
        com.hbm.gametest.Gen3SednaGameTests.init();
        com.hbm.gametest.RadioGameTests.init();
        com.hbm.gametest.CounterControllerGameTests.init();
        com.hbm.gametest.TelexGameTests.init();
        com.hbm.gametest.AutocalGameTests.init();
        com.hbm.gametest.RadioProviderGameTests.init();
        com.hbm.gametest.RtgGameTests.init();
        com.hbm.gametest.SingularityGameTests.init();
        com.hbm.gametest.TurretGameTests.init();
        com.hbm.gametest.PowerNetworkGameTests.init();
        com.hbm.gametest.NetworkReachGameTests.init();
        com.hbm.gametest.FluidNetworkGameTests.init();
        com.hbm.gametest.ForeignEnergyGameTests.init();
        com.hbm.gametest.FoldedGeometryGameTests.init();
        com.hbm.gametest.NetworkSyncGameTests.init();
        com.hbm.gametest.ChunkPromotionGameTests.init();
        com.hbm.gametest.TickPhaseGameTests.init();
        com.hbm.gametest.MultiblockMenuGameTests.init();
        com.hbm.gametest.RadiationPersistenceGameTests.init();
        com.hbm.gametest.WorldgenImpactGameTests.init();
        com.hbm.gametest.ConfigGameTests.init();
        com.hbm.gametest.HooksGameTests.init();
        com.hbm.gametest.FoldedPortParityGameTests.init();
        com.hbm.gametest.WeaponTableGameTests.init();
        com.hbm.gametest.GunSupportGameTests.init();
        com.hbm.gametest.PollutionGameTests.init();
        com.hbm.gametest.ToxinGameTests.init();
        com.hbm.gametest.FlywheelGameTests.init();
    }

    //? if <26.1 {
    /*@net.minecraft.gametest.framework.GameTestGenerator
    public static java.util.Collection<net.minecraft.gametest.framework.TestFunction> generate() {
        initAll();
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.HbmGameTestFunctions.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FoodCatalystGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.OutgasGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.SteelStructureGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.HazardGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ToolingGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.AbilityToolGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.MeleeKitGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FinalCellHudFixGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FinalGapsAGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FinalGapsBGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.AdvancementCommandGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.DecoCrateGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FinalConsumableGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FinalRecipeGapGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Fix1AGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.IdsGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ForeignFluidGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Fix4GameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Fix5GameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Fix1BGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Fix3GameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Fix7GameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.SmallGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Fix7GlyphidGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Fix7BossGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Fix7MobGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.RecipeContentGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.UtilityToolGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.AssemblyFactoryGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ChemicalFactoryGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.AutosawGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.SolarGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.IntakeFanGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.DiFurnaceGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.MaterialCompletionGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.BedrockOreGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ExplosiveContentGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Bombs2GameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.NtmEntityPortGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Gen3SednaGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.RadioGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.CounterControllerGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.TelexGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.AutocalGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.RadioProviderGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.RtgGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.SingularityGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.TurretGameTests.TESTS);
        // CC: Tweaked peripheral suites, present only when CC is loaded (their bodies name CC types)
        for (dev.golem.core.gametest.GameTests.Suite suite : com.hbm.compat.computercraft.ComputerCraft.gameTestSuites()) {
            dev.golem.core.gametest.GameTests.register(suite);
        }
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.PowerNetworkGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.NetworkReachGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FluidNetworkGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ForeignEnergyGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FoldedGeometryGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.NetworkSyncGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ChunkPromotionGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.TickPhaseGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.MultiblockMenuGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.RadiationPersistenceGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.WorldgenImpactGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ConfigGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.HooksGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FoldedPortParityGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.WeaponTableGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.GunSupportGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FlywheelGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.PollutionGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ToxinGameTests.TESTS);
        return dev.golem.core.gametest.GameTests.pending();
    }
    *///?}
}
