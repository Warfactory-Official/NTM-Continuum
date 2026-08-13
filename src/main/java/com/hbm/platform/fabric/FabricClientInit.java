package com.hbm.platform.fabric;

//? if fabric {

/*import com.hbm.client.HbmLookOverlay;
import com.hbm.client.RadCounterOverlay;
import com.hbm.client.render.HbmBlockColors;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.HbmEntityRenderers;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.gui.GUIMachineAssemblyMachine;
import com.hbm.inventory.gui.GUIBatterySocket;
import com.hbm.inventory.gui.GUICrate;
import com.hbm.inventory.gui.GUIMachineBattery;
import com.hbm.inventory.gui.GUICrystallizer;
import com.hbm.inventory.gui.GUIMachineChemicalPlant;
import com.hbm.inventory.gui.GUIMachinePUREX;
import com.hbm.inventory.gui.GUIMachineSolderingStation;
import com.hbm.inventory.gui.GUIMachineElectricFurnace;
import com.hbm.inventory.gui.GUIMachineEPress;
import com.hbm.inventory.gui.GUIMachineFluidTank;
import com.hbm.inventory.gui.GUIMachinePress;
import com.hbm.inventory.gui.GUIMachineShredder;
import com.hbm.inventory.gui.GUINukeMan;
import com.hbm.inventory.gui.GUIWasteDrum;
import com.hbm.inventory.gui.GUIReactorZirnox;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.EntityPropsSyncPayload;
import com.hbm.packet.HbmClientPackets;
import com.hbm.packet.HbmPackets;
import com.hbm.hazard.HazardSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
// The HUD hook. 26.1's fabric-api models the HUD as an ordered list of named elements; 1.21.1 has a
// single "after the vanilla HUD" callback, which is where all three of ours would land anyway.
//? if >=26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else {
/^import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
^///?}
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/^* Fabric client entrypoint: registers HBM menu screens, BlockEntity renderers and packet receivers. ^/
public class FabricClientInit implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Client→server control packet send path (recipe selector etc.).
        HbmClientPackets.setSender(ClientPlayNetworking::send);

        // Item tint colours (fluid icons, scraps, ...). 26.1 declares these in the item definitions and
        // the engine applies them; below it Golem Core wires the loader's colour registry to the same
        // stored colour. One call, every version, both loaders.
        dev.golem.core.client.Tints.install(com.hbm.Constants.MOD_ID);

        MenuScreens.register(ModMenus.MACHINE_BATTERY.get(), GUIMachineBattery::new);
        MenuScreens.register(ModMenus.MACHINE_WASTE_DRUM.get(), GUIWasteDrum::new);
        MenuScreens.register(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), GUIMachineAssemblyMachine::new);
        MenuScreens.register(ModMenus.MACHINE_CHEMICAL_PLANT.get(), GUIMachineChemicalPlant::new);
        MenuScreens.register(ModMenus.MACHINE_PUREX.get(), GUIMachinePUREX::new);
        MenuScreens.register(ModMenus.MACHINE_SOLDERING_STATION.get(), GUIMachineSolderingStation::new);
        MenuScreens.register(ModMenus.MACHINE_CRYSTALLIZER.get(), GUICrystallizer::new);
        MenuScreens.register(ModMenus.MACHINE_SILEX.get(), com.hbm.inventory.gui.GUISILEX::new);
        MenuScreens.register(ModMenus.MACHINE_FEL.get(), com.hbm.inventory.gui.GUIFEL::new);
        MenuScreens.register(ModMenus.MACHINE_MINING_LASER.get(), com.hbm.inventory.gui.GUIMachineMiningLaser::new);
        MenuScreens.register(ModMenus.MACHINE_OIL_WELL.get(), com.hbm.inventory.gui.GUIMachineOilWell::new);
        MenuScreens.register(ModMenus.HEATER_OILBURNER.get(), com.hbm.inventory.gui.GUIOilburner::new);
        MenuScreens.register(ModMenus.MACHINE_REFINERY.get(), com.hbm.inventory.gui.GUIMachineRefinery::new);
        MenuScreens.register(ModMenus.MACHINE_DIESEL.get(), com.hbm.inventory.gui.GUIMachineDiesel::new);
        MenuScreens.register(ModMenus.MACHINE_TURBINE.get(), com.hbm.inventory.gui.GUIMachineTurbine::new);
        MenuScreens.register(ModMenus.MACHINE_GAS_FLARE.get(), com.hbm.inventory.gui.GUIMachineGasFlare::new);
        MenuScreens.register(ModMenus.MACHINE_LIQUEFACTOR.get(), com.hbm.inventory.gui.GUILiquefactor::new);
        MenuScreens.register(ModMenus.MACHINE_SOLIDIFIER.get(), com.hbm.inventory.gui.GUISolidifier::new);
        MenuScreens.register(ModMenus.NUKE_MAN.get(), GUINukeMan::new);
        MenuScreens.register(ModMenus.REACTOR_ZIRNOX.get(), GUIReactorZirnox::new);
        MenuScreens.register(ModMenus.FLUID_TANK.get(), GUIMachineFluidTank::new);
        MenuScreens.register(ModMenus.BATTERY_SOCKET.get(), GUIBatterySocket::new);
        MenuScreens.register(ModMenus.CRATE.get(), GUICrate::new);
        MenuScreens.register(ModMenus.MACHINE_PRESS.get(), GUIMachinePress::new);
        MenuScreens.register(ModMenus.MACHINE_EPRESS.get(), GUIMachineEPress::new);
        MenuScreens.register(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), GUIMachineElectricFurnace::new);
        MenuScreens.register(ModMenus.MACHINE_SHREDDER.get(), GUIMachineShredder::new);
        MenuScreens.register(ModMenus.ANVIL.get(), com.hbm.inventory.gui.GUIAnvil::new);
        MenuScreens.register(ModMenus.MACHINE_CRUCIBLE.get(), com.hbm.inventory.gui.GUICrucible::new);
        MenuScreens.register(ModMenus.HEATER_FIREBOX.get(), com.hbm.inventory.gui.GUIFirebox::new);
        MenuScreens.register(ModMenus.MACHINE_VACUUM_DISTILL.get(), com.hbm.inventory.gui.GUIMachineVacuumDistill::new);
        MenuScreens.register(ModMenus.MACHINE_CATALYTIC_REFORMER.get(), com.hbm.inventory.gui.GUIMachineCatalyticReformer::new);
        MenuScreens.register(ModMenus.MACHINE_HYDROTREATER.get(), com.hbm.inventory.gui.GUIMachineHydrotreater::new);
        MenuScreens.register(ModMenus.MACHINE_COKER.get(), com.hbm.inventory.gui.GUIMachineCoker::new);
        MenuScreens.register(ModMenus.MACHINE_PYROOVEN.get(), com.hbm.inventory.gui.GUIPyroOven::new);
        MenuScreens.register(ModMenus.MACHINE_COMBUSTION_ENGINE.get(), com.hbm.inventory.gui.GUICombustionEngine::new);
        MenuScreens.register(ModMenus.MACHINE_COMPRESSOR.get(), com.hbm.inventory.gui.GUICompressor::new);

        // Look-overlay HUD (battery charge / duct type / ...) — drawn after the vanilla HUD each frame.
        //? if >=26.1 {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("hbm", "look_overlay"),
                (graphics, deltaTracker) -> HbmLookOverlay.render(graphics));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("hbm", "rad_counter"),
                (graphics, deltaTracker) -> RadCounterOverlay.render(graphics));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("hbm", "nuke_flash"),
                (graphics, deltaTracker) -> com.hbm.client.NukeScreenEffects.render(graphics));
        //?} else {
        /^// One callback, three draws, in the same order the named elements would run in.
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            HbmLookOverlay.render(graphics);
            RadCounterOverlay.render(graphics);
            com.hbm.client.NukeScreenEffects.render(graphics);
        });
        ^///?}

        HbmBlockEntityRenderers.registerAll(new HbmBlockEntityRenderers.Sink() {
            @Override
            public <T extends BlockEntity, S extends dev.golem.core.render.CompatRenderState> void register(
                    BlockEntityType<? extends T> type, dev.golem.core.render.CompatBERProvider<T, S> provider) {
                BlockEntityRendererRegistry.register(type, dev.golem.core.render.Renderers.toVanilla(provider));
            }
        });

        HbmEntityRenderers.registerAll(new HbmEntityRenderers.Sink() {
            @Override
            public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                EntityRendererRegistry.register(type, provider);
            }
        });
        // 26.1: block tint sources (sellafite shade tint) — fabric-api has no colour registry there, so
        // register directly on the vanilla BlockColors once the client instance is fully constructed.
        // Below 26.1: OBJ machine item models. 26.1 drives those through its special-model system
        // (registered by MixinSpecialModelRenderers); here the item definitions are parsed on reload and
        // every HBM item gets a dynamic renderer. Only items whose generated model parents onto
        // builtin/entity actually reach it, so registering broadly is free.
        HbmBlockColors.registerAll();
//? if <26.1 {

        /^net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                    @Override
                    public net.minecraft.resources.Identifier getFabricId() {
                        return net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                com.hbm.Constants.MOD_ID, "machine_item_models");
                    }

                    @Override
                    public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager resources) {
                        com.hbm.client.render.MachineItemModels.reload(resources);
                    }
                });

        net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet().stream()
                .filter(e -> e.getKey().location().getNamespace().equals(com.hbm.Constants.MOD_ID))
                .forEach(e -> net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE
                        .register(e.getValue(), (stack, mode, poseStack, buffers, light, overlay) ->
                                com.hbm.client.render.MachineItemRenderers.render(stack, mode, poseStack, buffers, light, overlay)));
        ^///?}

        // BufPacket-equivalent BlockEntity sync: Fabric runs the receiver on the client game thread.
        ClientPlayNetworking.registerGlobalReceiver(BlockEntitySyncPayload.TYPE,
                (payload, context) -> HbmPackets.handleSync(context.player().level(), payload));

        // HBM living-props sync (radiation HUD data) for the local player.
        ClientPlayNetworking.registerGlobalReceiver(EntityPropsSyncPayload.TYPE,
                (payload, context) -> HbmPackets.handlePropsSync(context.player(), payload));

        // Hazard tooltips (radioactive/hot/etc. traits on items).
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) ->
                HazardSystem.addHazardInfo(stack, net.minecraft.client.Minecraft.getInstance().player, lines));
    }
}
*///?}
