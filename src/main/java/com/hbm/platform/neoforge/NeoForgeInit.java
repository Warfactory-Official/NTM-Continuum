package com.hbm.platform.neoforge;

//? if neoforge {

import com.hbm.Constants;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Loader-only subscriptions; constructed by the generated entry after {@code HbmMod} boots. */
public class NeoForgeInit {

    public NeoForgeInit(IEventBus eventBus) {
        // Loader-only client hooks. Isolated so the server never loads client classes.
        if (dev.golem.core.platform.Platform.isClient()) {
            NeoForgeClientInit.init(eventBus);
        }

        // Power/network engine driver: tick the nodespace each server tick, clear it on level unload.
        // The radiation engine ticks on the same hook (both are server-thread, end-of-tick).
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) ->
                com.hbm.tileentity.network.RTTYSystem.updateBroadcastQueue(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            RadiationSystemNT.tickServer(event.getServer());
            com.hbm.saveddata.satellites.SatelliteSystems.tickServer(event.getServer());
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

        // Debug commands (/hbm rad ...).
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) ->
                com.hbm.command.HbmCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            com.hbm.config.AutocalConfig.load(event.getServer());
            Constants.LOG.info("[HBM] Loaded {} total recipes", event.getServer().getRecipeManager().getRecipes().size());
        });
    }
}
//?}
