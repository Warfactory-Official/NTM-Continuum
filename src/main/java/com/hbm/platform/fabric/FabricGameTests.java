package com.hbm.platform.fabric;

import com.hbm.gametest.AbilityToolGameTests;
import com.hbm.gametest.AddonContentGameTests;
import com.hbm.gametest.AddonEnumGameTests;
import com.hbm.gametest.AddonSeamGameTests;
import com.hbm.gametest.AdvancementCommandGameTests;
import com.hbm.gametest.AssemblyFactoryGameTests;
import com.hbm.gametest.AutocalGameTests;
import com.hbm.gametest.AutosawGameTests;
import com.hbm.gametest.BedrockOreGameTests;
import com.hbm.gametest.Bombs2GameTests;
import com.hbm.gametest.ChemicalFactoryGameTests;
import com.hbm.gametest.ChunkPromotionGameTests;
import com.hbm.gametest.ConfigGameTests;
import com.hbm.gametest.CounterControllerGameTests;
import com.hbm.gametest.DataMapGameTests;
import com.hbm.gametest.DecoCrateGameTests;
import com.hbm.gametest.DiFurnaceGameTests;
import com.hbm.gametest.DosimeterGasGameTests;
import com.hbm.gametest.EnvironmentGameTests;
import com.hbm.gametest.ExplosiveContentGameTests;
import com.hbm.gametest.FinalCellHudFixGameTests;
import com.hbm.gametest.FinalConsumableGameTests;
import com.hbm.gametest.FinalGapsAGameTests;
import com.hbm.gametest.FinalGapsBGameTests;
import com.hbm.gametest.FinalRecipeGapGameTests;
import com.hbm.gametest.Fix1AGameTests;
import com.hbm.gametest.Fix1BGameTests;
import com.hbm.gametest.Fix3GameTests;
import com.hbm.gametest.Fix4GameTests;
import com.hbm.gametest.Fix5GameTests;
import com.hbm.gametest.Fix7BossGameTests;
import com.hbm.gametest.Fix7GameTests;
import com.hbm.gametest.Fix7GlyphidGameTests;
import com.hbm.gametest.Fix7MobGameTests;
import com.hbm.gametest.FluidNetworkGameTests;
import com.hbm.gametest.FluidPropertyGameTests;
import com.hbm.gametest.FluidRecipeTableGameTests;
import com.hbm.gametest.FlywheelGameTests;
import com.hbm.gametest.FoldedGeometryGameTests;
import com.hbm.gametest.FoldedPortParityGameTests;
import com.hbm.gametest.FoodCatalystGameTests;
import com.hbm.gametest.ForeignEnergyGameTests;
import com.hbm.gametest.ForeignFluidGameTests;
import com.hbm.gametest.GeigerCounterGameTests;
import com.hbm.gametest.Gen3SednaGameTests;
import com.hbm.gametest.ReloadedGunGameTests;
import com.hbm.gametest.GenericRecipeTableGameTests;
import com.hbm.gametest.GunSupportGameTests;
import com.hbm.gametest.HazardGameTests;
import com.hbm.gametest.HbmGameTestFunctions;
import com.hbm.gametest.HooksGameTests;
import com.hbm.gametest.IdsGameTests;
import com.hbm.gametest.IntakeFanGameTests;
import com.hbm.gametest.ItemProcessingTableGameTests;
import com.hbm.gametest.ItemRecipeTableGameTests;
import com.hbm.gametest.MaterialCompletionGameTests;
import com.hbm.gametest.MeleeKitGameTests;
import com.hbm.gametest.MultiblockMenuGameTests;
import com.hbm.gametest.NetworkReachGameTests;
import com.hbm.gametest.NetworkSyncGameTests;
import com.hbm.gametest.NtmEntityPortGameTests;
import com.hbm.gametest.OutgasGameTests;
import com.hbm.gametest.PollutionGameTests;
import com.hbm.gametest.TilePortGameTests;
import com.hbm.gametest.PowerNetworkGameTests;
import com.hbm.gametest.RadiationPersistenceGameTests;
import com.hbm.gametest.RadioGameTests;
import com.hbm.gametest.RadioProviderGameTests;
import com.hbm.gametest.RecipeContentGameTests;
import com.hbm.gametest.RecipeViewerGameTests;
import com.hbm.gametest.RecipeTableGameTests;
import com.hbm.gametest.RtgGameTests;
import com.hbm.gametest.ScriptHookGameTests;
import com.hbm.gametest.SingularityGameTests;
import com.hbm.gametest.SmallGameTests;
import com.hbm.gametest.SolarGameTests;
import com.hbm.gametest.SteelStructureGameTests;
import com.hbm.gametest.TelexGameTests;
import com.hbm.gametest.TickPhaseGameTests;
import com.hbm.gametest.ToolingGameTests;
import com.hbm.gametest.ToxinGameTests;
import com.hbm.gametest.TurretGameTests;
import com.hbm.gametest.UtilityToolGameTests;
import com.hbm.gametest.ValueMapGameTests;
import com.hbm.gametest.WeaponTableGameTests;
import com.hbm.gametest.WorldgenImpactGameTests;
import dev.golem.core.gametest.GameTests;

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
        HbmGameTestFunctions.init();
        FoodCatalystGameTests.init();
        OutgasGameTests.init();
        DosimeterGasGameTests.init();
        GeigerCounterGameTests.init();
        SteelStructureGameTests.init();
        HazardGameTests.init();
        ToolingGameTests.init();
        AbilityToolGameTests.init();
        MeleeKitGameTests.init();
        FinalCellHudFixGameTests.init();
        FinalGapsAGameTests.init();
        FinalGapsBGameTests.init();
        AdvancementCommandGameTests.init();
        DecoCrateGameTests.init();
        FinalConsumableGameTests.init();
        FinalRecipeGapGameTests.init();
        Fix1AGameTests.init();
        IdsGameTests.init();
        ForeignFluidGameTests.init();
        TilePortGameTests.init();
        Fix4GameTests.init();
        Fix5GameTests.init();
        Fix1BGameTests.init();
        Fix3GameTests.init();
        Fix7GameTests.init();
        SmallGameTests.init();
        Fix7GlyphidGameTests.init();
        Fix7BossGameTests.init();
        Fix7MobGameTests.init();
        RecipeContentGameTests.init();
        RecipeViewerGameTests.init();
        UtilityToolGameTests.init();
        AssemblyFactoryGameTests.init();
        ChemicalFactoryGameTests.init();
        AutosawGameTests.init();
        SolarGameTests.init();
        IntakeFanGameTests.init();
        DiFurnaceGameTests.init();
        RecipeTableGameTests.init();
        FluidPropertyGameTests.init();
        DataMapGameTests.init();
        AddonEnumGameTests.init();
        EnvironmentGameTests.init();
        ScriptHookGameTests.init();
        AddonContentGameTests.init();
        GenericRecipeTableGameTests.init();
        ItemProcessingTableGameTests.init();
        ValueMapGameTests.init();
        AddonSeamGameTests.init();
        FluidRecipeTableGameTests.init();
        ItemRecipeTableGameTests.init();
        MaterialCompletionGameTests.init();
        BedrockOreGameTests.init();
        ExplosiveContentGameTests.init();
        Bombs2GameTests.init();
        NtmEntityPortGameTests.init();
        Gen3SednaGameTests.init();
        ReloadedGunGameTests.init();
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
        WorldgenImpactGameTests.init();
        ConfigGameTests.init();
        HooksGameTests.init();
        FoldedPortParityGameTests.init();
        WeaponTableGameTests.init();
        GunSupportGameTests.init();
        PollutionGameTests.init();
        ToxinGameTests.init();
        FlywheelGameTests.init();
    }

    //? if <26.1 {
    /*@net.minecraft.gametest.framework.GameTestGenerator
    public static java.util.Collection<net.minecraft.gametest.framework.TestFunction> generate() {
        initAll();
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.HbmGameTestFunctions.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FoodCatalystGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.OutgasGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.DosimeterGasGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.GeigerCounterGameTests.TESTS);
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
        GameTests.register(TilePortGameTests.TESTS);
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
        GameTests.register(RecipeViewerGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.UtilityToolGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.AssemblyFactoryGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ChemicalFactoryGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.AutosawGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.SolarGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.IntakeFanGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.DiFurnaceGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.RecipeTableGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FluidPropertyGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.DataMapGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.AddonEnumGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.EnvironmentGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ScriptHookGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.AddonContentGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.GenericRecipeTableGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ItemProcessingTableGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ValueMapGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.AddonSeamGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.FluidRecipeTableGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ItemRecipeTableGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.MaterialCompletionGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.BedrockOreGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ExplosiveContentGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Bombs2GameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.NtmEntityPortGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.Gen3SednaGameTests.TESTS);
        dev.golem.core.gametest.GameTests.register(com.hbm.gametest.ReloadedGunGameTests.TESTS);
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
