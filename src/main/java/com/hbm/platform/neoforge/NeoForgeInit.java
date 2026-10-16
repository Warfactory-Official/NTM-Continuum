package com.hbm.platform.neoforge;

//? if neoforge {


import com.hbm.Constants;
import java.util.List;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.RegistryObject;

import com.hbm.hazard.HazardSystem;
import com.hbm.util.TooltipColours;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@Mod(Constants.MOD_ID)
public class NeoForgeInit {

    public NeoForgeInit(IEventBus eventBus) {

        Constants.LOG.info("Hello NeoForge world!");

        // Test functions are a NeoForge-only dev registry. Do not initialize this common class on Fabric.
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
         com.hbm.gametest.ReloadedGunGameTests.init();
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
        // HBM's common start-up (Golem Core fills every mod's registers from its own RegisterEvent listener).
        dev.golem.core.platform.ModEntries.start(new com.hbm.HbmEntry());

        // Populate our creative tabs from the common contents map.
        eventBus.addListener(this::onBuildTabContents);

        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.HbmGameTestFunctions.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FoodCatalystGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.OutgasGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.DosimeterGasGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.GeigerCounterGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.SteelStructureGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.HazardGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ToolingGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AbilityToolGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.MeleeKitGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FinalCellHudFixGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FinalGapsAGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FinalGapsBGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AdvancementCommandGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.DecoCrateGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FinalConsumableGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FinalRecipeGapGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix1AGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.IdsGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ForeignFluidGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.PortClaimGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix4GameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix5GameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix1BGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix3GameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix7GameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.SmallGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix7GlyphidGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix7BossGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Fix7MobGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RecipeContentGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.UtilityToolGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AssemblyFactoryGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ChemicalFactoryGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AutosawGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.SolarGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.IntakeFanGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.DiFurnaceGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RecipeTableGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FluidPropertyGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.DataMapGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AddonEnumGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.EnvironmentGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ScriptHookGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AddonContentGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.GenericRecipeTableGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ItemProcessingTableGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ValueMapGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AddonSeamGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FluidRecipeTableGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ItemRecipeTableGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.MaterialCompletionGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.BedrockOreGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ExplosiveContentGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Bombs2GameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.NtmEntityPortGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Gen3SednaGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ReloadedGunGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.RadioGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.CounterControllerGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.TelexGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.AutocalGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.RadioProviderGameTests.TESTS));
          eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                  com.hbm.gametest.RtgGameTests.TESTS));
          eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                  com.hbm.gametest.SingularityGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.TurretGameTests.TESTS));
        // CC: Tweaked peripheral suites, present only when CC is loaded (their bodies name CC types)
        for (dev.golem.core.gametest.GameTests.Suite suite : com.hbm.compat.computercraft.ComputerCraft.gameTestSuites()) {
            eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event, suite));
        }
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.PowerNetworkGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.NetworkReachGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.FluidNetworkGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.ForeignEnergyGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.FoldedGeometryGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.NetworkSyncGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.ChunkPromotionGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.TickPhaseGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.MultiblockMenuGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.RadiationPersistenceGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.WorldgenImpactGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.ConfigGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.HooksGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.FoldedPortParityGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.GunSupportGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.FlywheelGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.WeaponTableGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.PollutionGameTests.TESTS));
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.ToxinGameTests.TESTS));

        // Client-only setup (screen registration). Isolated so the server never loads client classes.
        if (dev.golem.core.platform.Platform.isClient()) {
            NeoForgeClientInit.init(eventBus);
        }

        // Power/network engine driver: tick the nodespace each server tick, clear it on level unload.
        // The radiation engine ticks on the same hook (both are server-thread, end-of-tick).
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) ->
                com.hbm.tileentity.network.RTTYSystem.updateBroadcastQueue(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            RadiationSystemNT.tickServer(event.getServer());
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                RadiationSystemNT.closeWorld(level);
                com.hbm.tileentity.network.RTTYSystem.clearLevel(level);
            }
        });

        // Radiation chunk lifecycle: (re)build pockets on load, persist on unload.
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk)
                RadiationSystemNT.onChunkLoaded(level, chunk);
        });
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk)
                RadiationSystemNT.onChunkUnloaded(level, chunk);
        });

        // Hazard tooltips (radioactive/hot/etc. traits on items).
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> {
            TooltipColours.apply(event.getItemStack(), event.getToolTip());
            HazardSystem.addHazardInfo(event.getItemStack(), event.getEntity(), event.getToolTip());
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            com.hbm.items.special.ItemNuclearWaste.preserveDroppedEntity(event.getEntity());
            com.hbm.handler.pollution.PollutionHandler.decorateMob(event.getEntity());
        });
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post event) -> {
            com.hbm.handler.ExplosivesEntityHooks.tick(event.getEntity());
            if (event.getEntity() instanceof ItemEntity item) {
                com.hbm.items.special.ItemSingularity.tickDropped(item);
                com.hbm.items.special.ItemCell.tickDropped(item);
                com.hbm.items.special.ItemAntimatterPellet.tickDropped(item);
                com.hbm.items.special.ItemDemonCore.tickDropped(item);
            }
        });

        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            com.hbm.config.AutocalConfig.load(event.getServer());
            Constants.LOG.info("[HBM] Loaded {} total recipes", event.getServer().getRecipeManager().getRecipes().size());
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
}
//?}
