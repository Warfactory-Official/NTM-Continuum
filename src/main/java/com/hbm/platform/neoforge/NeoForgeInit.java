package com.hbm.platform.neoforge;

//? if neoforge {


import com.hbm.CommonClass;
import com.hbm.Constants;
import java.util.List;
import java.util.function.Supplier;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import com.hbm.packet.HbmPackets;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.Hazards;
import dev.golem.core.net.GolemNetwork;
import com.hbm.uninos.UniNodespace;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class NeoForgeInit {

    public NeoForgeInit(IEventBus eventBus) {

        Constants.LOG.info("Hello NeoForge world!");

        // Test functions are a NeoForge-only dev registry. Do not initialize this common class on Fabric.
        //? if >=26.1 {
        com.hbm.gametest.HbmGameTestFunctions.init();
        com.hbm.gametest.FoodCatalystGameTests.init();
        com.hbm.gametest.OutgasGameTests.init();
        com.hbm.gametest.SteelStructureGameTests.init();
        com.hbm.gametest.HazardGameTests.init();
        com.hbm.gametest.ToolingGameTests.init();
        com.hbm.gametest.AssemblyFactoryGameTests.init();
        com.hbm.gametest.ChemicalFactoryGameTests.init();
        com.hbm.gametest.AutosawGameTests.init();
        com.hbm.gametest.SolarGameTests.init();
        com.hbm.gametest.IntakeFanGameTests.init();
        com.hbm.gametest.DiFurnaceGameTests.init();
        com.hbm.gametest.MaterialCompletionGameTests.init();
        com.hbm.gametest.BedrockOreGameTests.init();
        com.hbm.gametest.ExplosiveContentGameTests.init();
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
        //?}
        // Bootstrap the Common mod: this populates all DeferredRegisters before RegisterEvent fires.
        CommonClass.init();

        // Flush common registrations through NeoForge's RegisterEvent, the idiomatic mod-bus path.
        eventBus.addListener(this::onRegister);

        // Populate our creative tabs from the common contents map.
        eventBus.addListener(this::onBuildTabContents);

        HbmPackets.declare();
        eventBus.addListener(this::onRegisterPayloads);
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.HbmGameTestFunctions.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FoodCatalystGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.OutgasGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.SteelStructureGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.HazardGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ToolingGameTests.TESTS));
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
                com.hbm.gametest.MaterialCompletionGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.BedrockOreGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ExplosiveContentGameTests.TESTS));
        eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Gen3SednaGameTests.TESTS));
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
         eventBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                 com.hbm.gametest.FlywheelGameTests.TESTS));

        // Client-only setup (screen registration). Isolated so the server never loads client classes.
        if (dev.golem.core.platform.Platform.isClient()) {
            NeoForgeClientInit.init(eventBus);
        }

        // Power/network engine driver: tick the nodespace each server tick, clear it on level unload.
        // The radiation engine ticks on the same hook (both are server-thread, end-of-tick).
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) ->
                com.hbm.tileentity.network.RTTYSystem.updateBroadcastQueue(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            UniNodespace.updateNodespace(event.getServer());
            RadiationSystemNT.tickServer(event.getServer());
            com.hbm.saveddata.satellites.SatelliteSystems.tickServer(event.getServer());
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                UniNodespace.clearLevel(level);
                RadiationSystemNT.clearWorld(level);
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

        // Register all item/material hazards after registration has flushed (items must exist).
        eventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(Hazards::init));

        // Hazard tooltips (radioactive/hot/etc. traits on items).
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) ->
                HazardSystem.addHazardInfo(event.getItemStack(), event.getEntity(), event.getToolTip()));
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) ->
                 com.hbm.items.special.ItemNuclearWaste.preserveDroppedEntity(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post event) -> {
            if (event.getEntity() instanceof ItemEntity item) {
                com.hbm.items.special.ItemSingularity.tickDropped(item);
                com.hbm.items.special.ItemCell.tickDropped(item);
                com.hbm.items.special.ItemAntimatterPellet.tickDropped(item);
            }
        });

        // Debug commands (/hbm rad ...).
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) ->
                com.hbm.command.HbmCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            com.hbm.config.AutocalConfig.load(event.getServer());
            Constants.LOG.info("[HBM] Loaded {} total recipes", event.getServer().getRecipeManager().getRecipes().size());
        });
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        GolemNetwork.flushRegistrations(event);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void onRegister(RegisterEvent event) {
        for (DeferredRegister<?> register : DeferredRegister.all()) {
            if (event.getRegistryKey().equals(register.getRegistryKey())) {
                register.flushTo((registryKey, entryKey, value) ->
                        event.register((ResourceKey) registryKey, dev.golem.core.Ids.keyId(entryKey), (Supplier) value));
            }
        }
    }
}
//?}
