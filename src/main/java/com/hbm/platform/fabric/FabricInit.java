package com.hbm.platform.fabric;

//? if fabric {

/*import java.util.function.Supplier;
import com.hbm.CommonClass;
import com.hbm.Constants;
import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.Hazards;
import com.hbm.items.ModFuelItems;
import com.hbm.packet.HbmPackets;
import com.hbm.packet.AnvilCraftPayload;
import dev.golem.core.registry.DeferredRegister;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.ControlEventPayload;
import com.hbm.packet.EntityPropsSyncPayload;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class FabricInit implements ModInitializer {

    @Override
    public void onInitialize() {

        Constants.LOG.info("Hello Fabric world!");

        // Bootstrap the Common mod: this populates all DeferredRegisters.
        CommonClass.init();

        // Fabric has no deferred registration phase; register everything now via vanilla Registry.register.
        // GameTest suites first: one of them owns a test-only block, whose register must exist to be flushed.
        FabricGameTests.initAll();
        DeferredRegister.flushAllToVanilla();
        // Living entity attribute sets (ModNtmEntities and the self-registering mobs); Forge-like loaders
        // deliver these through EntityAttributeCreationEvent, Fabric writes them once the types exist.
        dev.golem.core.registry.LivingAttributes.flushFabric();
        dev.golem.core.world.Spawns.flushFabricPlacements(); // ModMobSpawns placements, once the types exist

        // Furnace burn times for the ItemFuel family. NeoForge reads these from the `neoforge:furnace_fuels`
        // data map (plain JSON in common resources, ignored by Fabric); Fabric has no data map for fuels, so
        // add them to the FuelValues builder as it is assembled.
        dev.golem.core.registry.Fuels.register(ModFuelItems.BURN_TIMES);

        HbmPackets.declare();
        GolemNetwork.flushCommonRegistrations();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            com.hbm.config.AutocalConfig.load(server);
            Constants.LOG.info("[HBM] Loaded {} total recipes", server.getRecipeManager().getRecipes().size());
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            com.hbm.items.special.ItemNuclearWaste.preserveDroppedEntity(entity);
            com.hbm.handler.pollution.PollutionHandler.decorateMob(entity);
        });
        dev.golem.core.platform.Lifecycle.onServerTick(server -> server.getAllLevels().forEach(level ->
                level.getAllEntities().forEach(entity -> {
                    com.hbm.handler.ExplosivesEntityHooks.tick(entity);
                    if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) {
                        com.hbm.items.special.ItemSingularity.tickDropped(item);
                        com.hbm.items.special.ItemCell.tickDropped(item);
                        com.hbm.items.special.ItemAntimatterPellet.tickDropped(item);
                        com.hbm.items.special.ItemDemonCore.tickDropped(item);
                    }
                })));

        // Worldgen: the biome modifiers ModBiomeModifiers declares reach Fabric through Golem Core's
        // BiomeFeatures.install(), called from MainRegistry right after the declaration.

        // Populate our creative tabs from the common contents map.
        ModTabs.CONTENTS.forEach((tabKey, contents) ->
                dev.golem.core.registry.Tabs.modifyEntries(tabKey, output -> {
                    contents.forEach(item -> {
                        net.minecraft.world.item.Item resolved = item.get();
                        if (ModTabs.shouldDisplay(tabKey, resolved)) {
                            output.accept(new net.minecraft.world.item.ItemStack(resolved));
                        }
                    });
                    var stacks = ModTabs.STACK_CONTENTS.get(tabKey);
                    if (stacks != null) stacks.forEach(supplier -> supplier.get().forEach(output::accept));
                }));

        // Power/network engine driver: tick the nodespace each server tick, clear it on level unload.
        // The radiation engine ticks on the same hook.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(
                com.hbm.tileentity.network.RTTYSystem::updateBroadcastQueue);
        dev.golem.core.platform.Lifecycle.onServerTick(server -> {
            RadiationSystemNT.tickServer(server);
        });
        dev.golem.core.platform.Lifecycle.onLevelUnload((server, level) -> {
            RadiationSystemNT.closeWorld(level);
            com.hbm.tileentity.network.RTTYSystem.clearLevel(level);
        });

        // Radiation chunk lifecycle: (re)build pockets on load, persist on unload. Unload is 2-arg on
        // both lines; Load grew a "newly generated" flag in the 26.1 fabric-api, so its SAM differs.
        dev.golem.core.platform.Lifecycle.onChunkLoad((level, chunk) -> RadiationSystemNT.onChunkLoaded(level, chunk));
        dev.golem.core.platform.Lifecycle.onChunkUnload((level, chunk) -> RadiationSystemNT.onChunkUnloaded(level, chunk));


        // Register all item/material hazards (registration has flushed above, so items exist).
        Hazards.init();
    }

}
*///?}
