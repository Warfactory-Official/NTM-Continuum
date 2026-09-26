package com.hbm.platform.forge;

//? if forge {

/*import com.hbm.handler.radiation.RadiationSystemNT;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/^* Loader-only subscriptions; constructed by the generated entry after {@code HbmMod} boots. Client code only after the side check. ^/
public final class ForgeInit {

    public ForgeInit() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientInit.init(modBus);
        }

        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) {
                com.hbm.tileentity.network.RTTYSystem.updateBroadcastQueue(event.getServer());
            }
            if (event.phase == TickEvent.Phase.END) {
                RadiationSystemNT.tickServer(event.getServer());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                RadiationSystemNT.closeWorld(level);
                com.hbm.tileentity.network.RTTYSystem.clearLevel(level);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
                RadiationSystemNT.onChunkLoaded(level, chunk);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
                RadiationSystemNT.onChunkUnloaded(level, chunk);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            com.hbm.items.special.ItemNuclearWaste.preserveDroppedEntity(event.getEntity());
            com.hbm.handler.pollution.PollutionHandler.decorateMob(event.getEntity());
        });
        // Forge 1.20.1 has no per-entity tick event: scan each level's item entities at the END of its tick,
        // over a snapshot because the tickers may spawn new drops (screwdriver, singularity effects).
        MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
            List<ItemEntity> items = new java.util.ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity item) items.add(item);
                else com.hbm.handler.ExplosivesEntityHooks.tick(entity); // items: below, off the live iterator
            }
            for (ItemEntity item : items) {
                com.hbm.handler.ExplosivesEntityHooks.tick(item);
                com.hbm.items.special.ItemSingularity.tickDropped(item);
                com.hbm.items.special.ItemCell.tickDropped(item);
                com.hbm.items.special.ItemAntimatterPellet.tickDropped(item);
                com.hbm.items.special.ItemDemonCore.tickDropped(item);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                com.hbm.command.HbmCommands.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            com.hbm.config.AutocalConfig.load(event.getServer());
            com.hbm.Constants.LOG.info("[HBM] Loaded {} total recipes",
                    event.getServer().getRecipeManager().getRecipes().size());
            auditWorldgen(event.getServer());
        });
    }

    private static void auditWorldgen(MinecraftServer server) {
        Set<Identifier> features = new TreeSet<>();
        server.registryAccess().registryOrThrow(Registries.BIOME).holders().forEach(biome ->
                biome.value().getGenerationSettings().features().forEach(step -> step.forEach(feature ->
                        feature.unwrapKey().map(ResourceKey::location)
                                .filter(id -> id.getNamespace().equals(com.hbm.Constants.MOD_ID))
                                .ifPresent(features::add))));
        if (features.size() == 9) {
            com.hbm.Constants.LOG.info("[HBM] Forge biome modifiers injected all 9 placed features: {}", features);
        } else {
            com.hbm.Constants.LOG.error("[HBM] Forge biome modifiers injected {}/9 placed features: {}",
                    features.size(), features);
        }
    }
}
*///?}
