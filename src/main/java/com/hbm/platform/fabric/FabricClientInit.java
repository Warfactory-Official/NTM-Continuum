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
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
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
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("hbm", "look_overlay"),
                (graphics, deltaTracker) -> HbmLookOverlay.render(graphics));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("hbm", "rad_counter"),
                (graphics, deltaTracker) -> RadCounterOverlay.render(graphics));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("hbm", "nuke_flash"),
                (graphics, deltaTracker) -> com.hbm.client.NukeScreenEffects.render(graphics));

        HbmBlockEntityRenderers.registerAll(new HbmBlockEntityRenderers.Sink() {
            @Override
            public <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, ?> provider) {
                BlockEntityRendererRegistry.register(type, provider);
            }
        });

        HbmEntityRenderers.registerAll(new HbmEntityRenderers.Sink() {
            @Override
            public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                EntityRendererRegistry.register(type, provider);
            }
        });

        // Block tint sources (sellafite shade tint) — fabric-api has no color registry on 26.1; register
        // directly on the vanilla BlockColors once the client instance is fully constructed.
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                HbmBlockColors.registerAll((sources, blocks) -> client.getBlockColors().register(sources, blocks)));

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
