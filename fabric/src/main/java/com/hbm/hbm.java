package com.hbm;

import java.util.function.Supplier;
import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.Hazards;
import com.hbm.items.ModFuelItems;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.ControlEventPayload;
import com.hbm.packet.EntityPropsSyncPayload;
import com.hbm.packet.HbmPackets;
import com.hbm.registry.DeferredRegister;
import com.hbm.uninos.UniNodespace;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;

public class hbm implements ModInitializer {

    @Override
    public void onInitialize() {

        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.

        Constants.LOG.info("Hello Fabric world!");

        // Bootstrap the Common mod: this populates all DeferredRegisters.
        CommonClass.init();

        // Fabric has no deferred registration phase; register everything now via vanilla Registry.register.
        DeferredRegister.flushAll(this::register);

        // Furnace burn times for the ItemFuel family. NeoForge reads these from the `neoforge:furnace_fuels`
        // data map (plain JSON in common resources, ignored by Fabric); Fabric has no data map for fuels, so
        // add them to the FuelValues builder as it is assembled.
        FuelValueEvents.BUILD.register((builder, context) ->
                ModFuelItems.BURN_TIMES.forEach((item, ticks) -> builder.add(item.get(), ticks)));

        // Register HBM's custom S2C payloads (the BufPacket-equivalent BlockEntity sync). The type must
        // be known on both sides to encode/decode; the client-side receiver lives in HbmClient.
        PayloadTypeRegistry.clientboundPlay().register(BlockEntitySyncPayload.TYPE, BlockEntitySyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EntityPropsSyncPayload.TYPE, EntityPropsSyncPayload.CODEC);

        // C2S control packet (recipe selector etc.): register the type on both sides; the server receiver
        // runs on the server thread (Fabric schedules play receivers there).
        PayloadTypeRegistry.serverboundPlay().register(ControlEventPayload.TYPE, ControlEventPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ControlEventPayload.TYPE,
                (payload, context) -> HbmPackets.handleControl(context.player(), payload));

        // C2S anvil construction-craft packet (same pattern as the control packet).
        PayloadTypeRegistry.serverboundPlay().register(com.hbm.packet.AnvilCraftPayload.TYPE, com.hbm.packet.AnvilCraftPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(com.hbm.packet.AnvilCraftPayload.TYPE,
                (payload, context) -> HbmPackets.handleAnvilCraft(context.player(), payload));

        // Worldgen: inject the oil bubble placed feature into all overworld biomes (the NeoForge side
        // does this with a data-driven neoforge:add_features biome modifier; Fabric uses the biome API).
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "oil_bubble")));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "oil_sand_bubble")));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bedrock_oil")));

        // Ore worldgen (the 1:1 HbmWorldGen port). The three noise features go in at
        // LOCAL_MODIFICATIONS, i.e. BEFORE the UNDERGROUND_ORES step: the gneiss veins in
        // overworld_ores target the stone_gneiss band that schist_stratum lays down, and would find
        // nothing if it ran later. Mirrors neoforge/.../biome_modifier/*.json — keep the two in sync.
        addOverworld("schist_stratum", GenerationStep.Decoration.LOCAL_MODIFICATIONS);
        addOverworld("ore_caves", GenerationStep.Decoration.LOCAL_MODIFICATIONS);
        addOverworld("ore_layers", GenerationStep.Decoration.LOCAL_MODIFICATIONS);
        addOverworld("overworld_ores", GenerationStep.Decoration.UNDERGROUND_ORES);
        BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "nether_ores")));
        BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "end_ores")));

        // Populate our creative tabs from the common contents map.
        ModTabs.CONTENTS.forEach((tabKey, contents) ->
                CreativeModeTabEvents.modifyOutputEvent(tabKey).register(output -> {
                    contents.forEach(item -> output.accept(item.get()));
                    var stacks = ModTabs.STACK_CONTENTS.get(tabKey);
                    if (stacks != null) stacks.forEach(supplier -> supplier.get().forEach(output::accept));
                }));

        // Power/network engine driver: tick the nodespace each server tick, clear it on level unload.
        // The radiation engine ticks on the same hook.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            UniNodespace.updateNodespace(server);
            RadiationSystemNT.tickServer(server);
        });
        ServerLevelEvents.UNLOAD.register((server, level) -> {
            UniNodespace.clearLevel(level);
            RadiationSystemNT.clearWorld(level);
        });

        // Radiation chunk lifecycle: (re)build pockets on load, persist on unload. (This fabric-api's
        // Load SAM carries an extra boolean; Unload is 2-arg.)
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) -> RadiationSystemNT.onChunkLoaded(level, chunk));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> RadiationSystemNT.onChunkUnloaded(level, chunk));

        // Debug commands (/hbm rad ...).
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> com.hbm.command.HbmCommands.register(dispatcher));

        // Register all item/material hazards (registration has flushed above, so items exist).
        Hazards.init();
    }

    /** Inject an {@code hbm:} placed feature into every overworld biome at the given decoration step. */
    private static void addOverworld(String feature, GenerationStep.Decoration step) {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), step,
                ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, feature)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void register(ResourceKey<? extends Registry<?>> registryKey, ResourceKey<?> entryKey, Supplier<?> value) {
        Registry<Object> registry = (Registry<Object>) (Registry<?>) BuiltInRegistries.REGISTRY.getValueOrThrow((ResourceKey) registryKey);
        Registry.register(registry, (ResourceKey<Object>) (ResourceKey<?>) entryKey, value.get());
    }
}
