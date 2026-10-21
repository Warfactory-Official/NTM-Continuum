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
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@Mod(Constants.MOD_ID)
public class NeoForgeInit {

    public NeoForgeInit(IEventBus eventBus) {

        Constants.LOG.info("Hello NeoForge world!");

        // HBM's common start-up (Golem Core fills every mod's registers from its own RegisterEvent listener).
        dev.golem.core.platform.ModEntries.start(new com.hbm.HbmEntry());

        // Populate our creative tabs from the common contents map.
        eventBus.addListener(this::onBuildTabContents);


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
