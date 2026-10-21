package com.hbm.platform.fabric;

//? if fabric {

/*import com.hbm.Constants;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModFuelItems;
import com.hbm.packet.AnvilCraftPayload;
import com.hbm.handler.radiation.RadiationSystemNT;
import dev.golem.core.platform.ModEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.ControlEventPayload;
import com.hbm.packet.EntityPropsSyncPayload;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/^*
 * HBM on Fabric: the {@code golemcore:entry} entrypoint (not {@code main}). Golem Core runs every Golem-based mod's
 * declarations in dependency order, then fills the registries, entity attributes, spawn placements and payloads once
 * for all of them, then runs {@link #registered} (see {@code dev.golem.core.platform.ModEntries}).
 ^/
public class FabricInit implements ModEntry {

    private final com.hbm.HbmEntry common = new com.hbm.HbmEntry();

    @Override
    public void declare() {
        Constants.LOG.info("Hello Fabric world!");
        common.declare();
    }

    @Override
    public void registered() {
        // Furnace burn times for the ItemFuel family. NeoForge reads these from the `neoforge:furnace_fuels`
        // data map (plain JSON in common resources, ignored by Fabric); Fabric has no data map for fuels, so
        // add them to the FuelValues builder as it is assembled.
        dev.golem.core.registry.Fuels.register(ModFuelItems.BURN_TIMES);

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


        // Item and material hazards, now that items exist.
        common.registered();
    }

}
*///?}
