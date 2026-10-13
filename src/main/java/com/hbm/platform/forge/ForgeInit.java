package com.hbm.platform.forge;

//? if forge {

/*import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.HazardSystem;
import com.hbm.packet.AnvilCraftPayload;
import com.hbm.packet.ControlEventPayload;
import com.hbm.handler.radiation.RadiationSystemNT;
import dev.golem.core.registry.RegistryObject;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/^* Forge 1.20.1 bootstrap; client code is only referenced after the physical-side check. ^/
@Mod(com.hbm.Constants.MOD_ID)
public final class ForgeInit {

    public ForgeInit() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        dev.golem.core.platform.ModEntries.start(new com.hbm.HbmEntry());
        com.hbm.gametest.HbmGameTestFunctions.init();
        com.hbm.gametest.FoodCatalystGameTests.init();
        com.hbm.gametest.OutgasGameTests.init();
        com.hbm.gametest.DosimeterGasGameTests.init();
        com.hbm.gametest.GeigerCounterGameTests.init();
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
        com.hbm.gametest.PortClaimGameTests.init();
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
        com.hbm.gametest.RecipeTableGameTests.init();
        com.hbm.gametest.FluidPropertyGameTests.init();
        com.hbm.gametest.DataMapGameTests.init();
        com.hbm.gametest.AddonEnumGameTests.init();
        com.hbm.gametest.EnvironmentGameTests.init();
        com.hbm.gametest.ScriptHookGameTests.init();
        com.hbm.gametest.AddonContentGameTests.init();
        com.hbm.gametest.GenericRecipeTableGameTests.init();
        com.hbm.gametest.ItemProcessingTableGameTests.init();
        com.hbm.gametest.ValueMapGameTests.init();
        com.hbm.gametest.AddonSeamGameTests.init();
        com.hbm.gametest.FluidRecipeTableGameTests.init();
        com.hbm.gametest.ItemRecipeTableGameTests.init();
        com.hbm.gametest.MaterialCompletionGameTests.init();
        com.hbm.gametest.BedrockOreGameTests.init();
        com.hbm.gametest.ExplosiveContentGameTests.init();
        com.hbm.gametest.Bombs2GameTests.init();
        com.hbm.gametest.NtmEntityPortGameTests.init();
        com.hbm.gametest.Gen3SednaGameTests.init();
        com.hbm.gametest.RadioGameTests.init();
        com.hbm.gametest.CounterControllerGameTests.init();
        com.hbm.gametest.TelexGameTests.init();
        com.hbm.gametest.FlywheelGameTests.init();
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

        modBus.addListener(this::onBuildTabContents);
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.HbmGameTestFunctions.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FoodCatalystGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.OutgasGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.DosimeterGasGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.GeigerCounterGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.SteelStructureGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.HazardGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ToolingGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AbilityToolGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.MeleeKitGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FinalCellHudFixGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FinalGapsAGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FinalGapsBGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AdvancementCommandGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.DecoCrateGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FinalConsumableGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FinalRecipeGapGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix1AGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.IdsGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ForeignFluidGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.PortClaimGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix4GameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix5GameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix1BGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix3GameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix7GameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.SmallGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix7GlyphidGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix7BossGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix7MobGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RecipeContentGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.UtilityToolGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AssemblyFactoryGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ChemicalFactoryGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AutosawGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.SolarGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.IntakeFanGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.DiFurnaceGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RecipeTableGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FluidPropertyGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.DataMapGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AddonEnumGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.EnvironmentGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ScriptHookGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AddonContentGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.GenericRecipeTableGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ItemProcessingTableGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ValueMapGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AddonSeamGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FluidRecipeTableGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ItemRecipeTableGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.MaterialCompletionGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.BedrockOreGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ExplosiveContentGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Bombs2GameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.NtmEntityPortGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Gen3SednaGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RadioGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.CounterControllerGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.TelexGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AutocalGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RadioProviderGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RtgGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.SingularityGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.TurretGameTests.TESTS));
        // CC: Tweaked peripheral suites, present only when CC is loaded (their bodies name CC types)
        for (dev.golem.core.gametest.GameTests.Suite suite : com.hbm.compat.computercraft.ComputerCraft.gameTestSuites()) {
            modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event, suite));
        }
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.PowerNetworkGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.NetworkReachGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FluidNetworkGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ForeignEnergyGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FoldedGeometryGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.NetworkSyncGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ChunkPromotionGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.TickPhaseGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.MultiblockMenuGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RadiationPersistenceGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.WorldgenImpactGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ConfigGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.HooksGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FoldedPortParityGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.WeaponTableGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.GunSupportGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FlywheelGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.PollutionGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ToxinGameTests.TESTS));

        // Forge 1.20.1 has no furnace-fuel data map: the burn times answer FurnaceFuelBurnTimeEvent (lazily
        // resolved, so registering before the registries flush is fine).
        dev.golem.core.registry.Fuels.register(com.hbm.items.ModFuelItems.BURN_TIMES);


        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientInit.init(modBus);
        }

        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) {
                com.hbm.tileentity.network.RTTYSystem.updateBroadcastQueue(event.getServer());
            }
            if (event.phase == TickEvent.Phase.END) {
                RadiationSystemNT.tickServer(event.getServer());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                RadiationSystemNT.closeWorld(level);
                com.hbm.tileentity.network.RTTYSystem.clearLevel(level);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
                RadiationSystemNT.onChunkLoaded(level, chunk);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
                RadiationSystemNT.onChunkUnloaded(level, chunk);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ItemTooltipEvent event) ->
                HazardSystem.addHazardInfo(event.getItemStack(), event.getEntity(), event.getToolTip()));
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            com.hbm.items.special.ItemNuclearWaste.preserveDroppedEntity(event.getEntity());
            com.hbm.handler.pollution.PollutionHandler.decorateMob(event.getEntity());
        });
        // Forge 1.20.1 has no per-entity tick event: scan each level's item entities at the END of its tick,
        // over a snapshot because the tickers may spawn new drops (screwdriver, singularity effects).
        MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
            List<ItemEntity> items = new java.util.ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity item) items.add(item);
                else com.hbm.handler.ExplosivesEntityHooks.tick(entity); // items: below, off the live iterator
            }
            for (ItemEntity item : items) {
                com.hbm.handler.ExplosivesEntityHooks.tick(item);
                com.hbm.items.special.ItemSingularity.tickDropped(item);
                com.hbm.items.special.ItemCell.tickDropped(item);
                com.hbm.items.special.ItemAntimatterPellet.tickDropped(item);
                com.hbm.items.special.ItemDemonCore.tickDropped(item);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            com.hbm.config.AutocalConfig.load(event.getServer());
            com.hbm.Constants.LOG.info("[HBM] Loaded {} total recipes",
                    event.getServer().getRecipeManager().getRecipes().size());
            auditWorldgen(event.getServer());
        });
    }

    private void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        List<RegistryObject<? extends Item>> contents = ModTabs.CONTENTS.get(event.getTabKey());
        if (contents != null) {
            contents.forEach(item -> {
                Item resolved = item.get();
                if (ModTabs.shouldDisplay(event.getTabKey(), resolved)) event.accept(resolved);
            });
        }
        var stacks = ModTabs.STACK_CONTENTS.get(event.getTabKey());
        if (stacks != null) {
            stacks.forEach(supplier -> supplier.get().forEach(event::accept));
        }
    }

    private static void auditWorldgen(MinecraftServer server) {
        Set<Identifier> features = new TreeSet<>();
        server.registryAccess().registryOrThrow(Registries.BIOME).holders().forEach(biome ->
                biome.value().getGenerationSettings().features().forEach(step -> step.forEach(feature ->
                        feature.unwrapKey().map(ResourceKey::location)
                                .filter(id -> id.getNamespace().equals(com.hbm.Constants.MOD_ID))
                                .ifPresent(features::add))));
        // every hbm placed feature named by a loaded forge:add_features modifier must reach a biome
        Set<Identifier> expected = new TreeSet<>();
        server.registryAccess().registryOrThrow(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS)
                .forEach(modifier -> {
                    if (modifier instanceof net.minecraftforge.common.world.ForgeBiomeModifiers.AddFeaturesBiomeModifier add) {
                        add.features().forEach(feature -> feature.unwrapKey().map(ResourceKey::location)
                                .filter(id -> id.getNamespace().equals(com.hbm.Constants.MOD_ID))
                                .ifPresent(expected::add));
                    }
                });
        Set<Identifier> missing = new TreeSet<>(expected);
        missing.removeAll(features);
        if (missing.isEmpty()) {
            com.hbm.Constants.LOG.info("[HBM] Forge biome modifiers injected all {} modifier features ({} hbm placed features in biomes): {}",
                    expected.size(), features.size(), features);
        } else {
            com.hbm.Constants.LOG.error("[HBM] Forge biome modifiers injected {}/{} modifier features; missing {}",
                    expected.size() - missing.size(), expected.size(), missing);
        }
    }
}
*///?}
