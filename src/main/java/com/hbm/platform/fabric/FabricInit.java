package com.hbm.platform.fabric;

//? if fabric {

/*import com.hbm.Constants;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;

/^* Fabric-only event subscriptions; the mod itself boots as {@code HbmMod} through Golem Core. ^/
public class FabricInit implements ModInitializer {

    @Override
    public void onInitialize() {

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

        // Power/network engine driver: tick the nodespace each server tick, clear it on level unload.
        // The radiation engine ticks on the same hook.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(
                com.hbm.tileentity.network.RTTYSystem::updateBroadcastQueue);
        dev.golem.core.platform.Lifecycle.onServerTick(server -> {
            RadiationSystemNT.tickServer(server);
            com.hbm.saveddata.satellites.SatelliteSystems.tickServer(server);
        });
        dev.golem.core.platform.Lifecycle.onLevelUnload((server, level) -> {
            RadiationSystemNT.closeWorld(level);
            com.hbm.tileentity.network.RTTYSystem.clearLevel(level);
        });

        // Radiation chunk lifecycle: (re)build pockets on load, persist on unload. Unload is 2-arg on
        // both lines; Load grew a "newly generated" flag in the 26.1 fabric-api, so its SAM differs.
        dev.golem.core.platform.Lifecycle.onChunkLoad((level, chunk) -> RadiationSystemNT.onChunkLoaded(level, chunk));
        dev.golem.core.platform.Lifecycle.onChunkUnload((level, chunk) -> RadiationSystemNT.onChunkUnloaded(level, chunk));

        // Debug commands (/hbm rad ...).
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> com.hbm.command.HbmCommands.register(dispatcher));
    }

    /^* Inject an {@code hbm:} placed feature into every overworld biome at the given decoration step. ^/
    private static void addOverworld(String feature, GenerationStep.Decoration step) {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), step,
                ResourceKey.create(Registries.PLACED_FEATURE, dev.golem.core.Ids.of(Constants.MOD_ID, feature)));
    }

}
*///?}
