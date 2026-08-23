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
import com.hbm.uninos.UniNodespace;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
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
import net.minecraft.world.level.levelgen.GenerationStep;

public class FabricInit implements ModInitializer {

    @Override
    public void onInitialize() {

        Constants.LOG.info("Hello Fabric world!");

        // Bootstrap the Common mod: this populates all DeferredRegisters.
        CommonClass.init();

        // Fabric has no deferred registration phase; register everything now via vanilla Registry.register.
        DeferredRegister.flushAllToVanilla();

        // Furnace burn times for the ItemFuel family. NeoForge reads these from the `neoforge:furnace_fuels`
        // data map (plain JSON in common resources, ignored by Fabric); Fabric has no data map for fuels, so
        // add them to the FuelValues builder as it is assembled.
        dev.golem.core.registry.Fuels.register(ModFuelItems.BURN_TIMES);

        HbmPackets.declare();
        GolemNetwork.flushCommonRegistrations();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> Constants.LOG.info(
                "[HBM] Loaded {} total recipes", server.getRecipeManager().getRecipes().size()));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) ->
                com.hbm.items.special.ItemNuclearWaste.preserveDroppedEntity(entity));

        // Worldgen: inject the oil bubble placed feature into all overworld biomes (the NeoForge side
        // does this with a data-driven neoforge:add_features biome modifier; Fabric uses the biome API).
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, dev.golem.core.Ids.of(Constants.MOD_ID, "oil_bubble")));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, dev.golem.core.Ids.of(Constants.MOD_ID, "oil_sand_bubble")));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, dev.golem.core.Ids.of(Constants.MOD_ID, "bedrock_oil")));

        // Ore worldgen (the 1:1 HbmWorldGen port). The three noise features go in at
        // LOCAL_MODIFICATIONS, i.e. BEFORE the UNDERGROUND_ORES step: the gneiss veins in
        // overworld_ores target the stone_gneiss band that schist_stratum lays down, and would find
        // nothing if it ran later. Mirrors neoforge/.../biome_modifier/^.json — keep the two in sync.
        addOverworld("schist_stratum", GenerationStep.Decoration.LOCAL_MODIFICATIONS);
        addOverworld("ore_caves", GenerationStep.Decoration.LOCAL_MODIFICATIONS);
        addOverworld("ore_layers", GenerationStep.Decoration.LOCAL_MODIFICATIONS);
        addOverworld("overworld_ores", GenerationStep.Decoration.UNDERGROUND_ORES);
        BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, dev.golem.core.Ids.of(Constants.MOD_ID, "nether_ores")));
        BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, dev.golem.core.Ids.of(Constants.MOD_ID, "end_ores")));

        // Populate our creative tabs from the common contents map.
        ModTabs.CONTENTS.forEach((tabKey, contents) ->
                dev.golem.core.registry.Tabs.modifyEntries(tabKey, output -> {
                    contents.forEach(item -> output.accept(new net.minecraft.world.item.ItemStack(item.get())));
                    var stacks = ModTabs.STACK_CONTENTS.get(tabKey);
                    if (stacks != null) stacks.forEach(supplier -> supplier.get().forEach(output::accept));
                }));

        // Power/network engine driver: tick the nodespace each server tick, clear it on level unload.
        // The radiation engine ticks on the same hook.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(
                com.hbm.tileentity.network.RTTYSystem::updateBroadcastQueue);
        dev.golem.core.platform.Lifecycle.onServerTick(server -> {
            UniNodespace.updateNodespace(server);
            RadiationSystemNT.tickServer(server);
        });
        dev.golem.core.platform.Lifecycle.onLevelUnload((server, level) -> {
            UniNodespace.clearLevel(level);
            RadiationSystemNT.clearWorld(level);
            com.hbm.tileentity.network.RTTYSystem.clearLevel(level);
        });

        // Radiation chunk lifecycle: (re)build pockets on load, persist on unload. Unload is 2-arg on
        // both lines; Load grew a "newly generated" flag in the 26.1 fabric-api, so its SAM differs.
        dev.golem.core.platform.Lifecycle.onChunkLoad((level, chunk) -> RadiationSystemNT.onChunkLoaded(level, chunk));
        dev.golem.core.platform.Lifecycle.onChunkUnload((level, chunk) -> RadiationSystemNT.onChunkUnloaded(level, chunk));

        // Debug commands (/hbm rad ...).
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> com.hbm.command.HbmCommands.register(dispatcher));

        // Register all item/material hazards (registration has flushed above, so items exist).
        Hazards.init();
    }

    /^* Inject an {@code hbm:} placed feature into every overworld biome at the given decoration step. ^/
    private static void addOverworld(String feature, GenerationStep.Decoration step) {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), step,
                ResourceKey.create(Registries.PLACED_FEATURE, dev.golem.core.Ids.of(Constants.MOD_ID, feature)));
    }

}
*///?}
