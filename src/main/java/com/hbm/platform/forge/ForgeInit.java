package com.hbm.platform.forge;

//? if forge {

/*import com.hbm.CommonClass;
import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.Hazards;
import com.hbm.packet.AnvilCraftPayload;
import com.hbm.packet.ControlEventPayload;
import com.hbm.packet.HbmPackets;
import com.hbm.handler.radiation.RadiationSystemNT;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

/^* Forge 1.20.1 bootstrap; client code is only referenced after the physical-side check. ^/
@Mod(com.hbm.Constants.MOD_ID)
public final class ForgeInit {

    public ForgeInit() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        CommonClass.init();
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
        com.hbm.gametest.TurretGameTests.init();
        com.hbm.gametest.PowerNetworkGameTests.init();
        com.hbm.gametest.NetworkReachGameTests.init();
        com.hbm.gametest.FluidNetworkGameTests.init();
        com.hbm.gametest.ForeignEnergyGameTests.init();
        com.hbm.gametest.FoldedGeometryGameTests.init();
        com.hbm.gametest.FoldedPortParityGameTests.init();

        modBus.addListener(this::onRegister);
        modBus.addListener(this::onBuildTabContents);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(Hazards::init));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.HbmGameTestFunctions.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FoodCatalystGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.OutgasGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.SteelStructureGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.HazardGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ToolingGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AssemblyFactoryGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ChemicalFactoryGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AutosawGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.SolarGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.IntakeFanGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.DiFurnaceGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.MaterialCompletionGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.BedrockOreGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ExplosiveContentGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.Gen3SednaGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RadioGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.CounterControllerGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.TelexGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.AutocalGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RadioProviderGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.RtgGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.TurretGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.PowerNetworkGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.NetworkReachGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FluidNetworkGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.ForeignEnergyGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FoldedGeometryGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FoldedPortParityGameTests.TESTS));
        modBus.addListener((RegisterGameTestsEvent event) -> dev.golem.core.gametest.GameTests.register(event,
                com.hbm.gametest.FlywheelGameTests.TESTS));

        HbmPackets.declare();
        GolemNetwork.flushCommonRegistrations();

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
                RadiationSystemNT.clearWorld(level);
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
        MinecraftForge.EVENT_BUS.addListener((ItemTooltipEvent event) ->
                HazardSystem.addHazardInfo(event.getItemStack(), event.getEntity(), event.getToolTip()));
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) ->
                com.hbm.items.special.ItemNuclearWaste.preserveDroppedEntity(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                com.hbm.command.HbmCommands.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            com.hbm.config.AutocalConfig.load(event.getServer());
            com.hbm.Constants.LOG.info("[HBM] Loaded {} total recipes",
                    event.getServer().getRecipeManager().getRecipes().size());
            auditWorldgen(event.getServer());
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
*///?}
