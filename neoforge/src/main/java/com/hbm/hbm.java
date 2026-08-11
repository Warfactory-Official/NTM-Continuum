package com.hbm;


import java.util.List;
import java.util.function.Supplier;
import com.hbm.creativetabs.ModTabs;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import com.hbm.client.HbmClientNeoForge;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.ControlEventPayload;
import com.hbm.packet.EntityPropsSyncPayload;
import com.hbm.packet.HbmPackets;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.Hazards;
import net.minecraft.server.level.ServerPlayer;
import com.hbm.uninos.UniNodespace;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(Constants.MOD_ID)
public class hbm {

    public hbm(IEventBus eventBus) {

        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        Constants.LOG.info("Hello NeoForge world!");

        // Test functions are a NeoForge-only dev registry. Do not initialize this common class on Fabric.
        com.hbm.gametest.HbmGameTestFunctions.init();
        // Bootstrap the Common mod: this populates all DeferredRegisters before RegisterEvent fires.
        CommonClass.init();

        // Flush common registrations through NeoForge's RegisterEvent, the idiomatic mod-bus path.
        eventBus.addListener(this::onRegister);

        // Populate our creative tabs from the common contents map.
        eventBus.addListener(this::onBuildTabContents);

        // Register HBM's custom S2C payloads (the BufPacket-equivalent BlockEntity sync).
        eventBus.addListener(this::onRegisterPayloads);
        eventBus.addListener(com.hbm.gametest.HbmNeoForgeGameTests::register);

        // Client-only setup (screen registration). Isolated so the server never loads client classes.
        if (FMLLoader.getCurrent().getDist() == Dist.CLIENT) {
            HbmClientNeoForge.init(eventBus);
        }

        // Power/network engine driver: tick the nodespace each server tick, clear it on level unload.
        // The radiation engine ticks on the same hook (both are server-thread, end-of-tick).
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            UniNodespace.updateNodespace(event.getServer());
            RadiationSystemNT.tickServer(event.getServer());
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                UniNodespace.clearLevel(level);
                RadiationSystemNT.clearWorld(level);
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

        // Debug commands (/hbm rad ...).
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) ->
                com.hbm.command.HbmCommands.register(event.getDispatcher()));
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(Constants.MOD_ID).playToClient(
                BlockEntitySyncPayload.TYPE,
                BlockEntitySyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> HbmPackets.handleSync(context.player().level(), payload)));
        event.registrar(Constants.MOD_ID).playToServer(
                ControlEventPayload.TYPE,
                ControlEventPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> HbmPackets.handleControl((ServerPlayer) context.player(), payload)));
        event.registrar(Constants.MOD_ID).playToServer(
                com.hbm.packet.AnvilCraftPayload.TYPE,
                com.hbm.packet.AnvilCraftPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> HbmPackets.handleAnvilCraft((ServerPlayer) context.player(), payload)));
        event.registrar(Constants.MOD_ID).playToClient(
                EntityPropsSyncPayload.TYPE,
                EntityPropsSyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> HbmPackets.handlePropsSync(context.player(), payload)));
    }

    private void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        List<RegistryObject<? extends Item>> contents = ModTabs.CONTENTS.get(event.getTabKey());
        if (contents != null) {
            contents.forEach(item -> event.accept(item.get()));
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
                        event.register((ResourceKey) registryKey, entryKey.identifier(), (Supplier) value));
            }
        }
    }
}
