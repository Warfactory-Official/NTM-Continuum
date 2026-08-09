package com.hbm.client;

import com.hbm.client.render.HbmBlockColors;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.HbmEntityRenderers;
import com.hbm.client.render.MachineItemModelRenderer;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.gui.GUIMachineAssemblyMachine;
import com.hbm.inventory.gui.GUIBatterySocket;
import com.hbm.inventory.gui.GUICrate;
import com.hbm.inventory.gui.GUIMachineBattery;
import com.hbm.inventory.gui.GUICrystallizer;
import com.hbm.inventory.gui.GUIMachineChemicalPlant;
import com.hbm.inventory.gui.GUIMachineSolderingStation;
import com.hbm.inventory.gui.GUIMachineElectricFurnace;
import com.hbm.inventory.gui.GUIMachineEPress;
import com.hbm.inventory.gui.GUIMachineFluidTank;
import com.hbm.inventory.gui.GUIMachinePress;
import com.hbm.inventory.gui.GUIMachineShredder;
import com.hbm.inventory.gui.GUINukeMan;
import com.hbm.packet.HbmClientPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.minecraft.resources.Identifier;

/**
 * Client-only NeoForge setup. Isolated in its own class so it is never loaded on a dedicated server
 * (it references client-only screen/renderer classes). Wired from the mod constructor behind a
 * Dist.CLIENT guard.
 */
public final class HbmClientNeoForge {

    private HbmClientNeoForge() {}

    public static void init(IEventBus modBus) {
        // Client→server control packet send path (recipe selector etc.).
        HbmClientPackets.setSender(payload -> {
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null) conn.send(new ServerboundCustomPayloadPacket(payload));
        });

        // Look-overlay HUD (battery charge / duct type / ...) — a GUI layer drawn above the vanilla HUD,
        // so its text is extracted/submitted in the same pass (mirrors Fabric's HudElementRegistry).
        modBus.addListener((RegisterGuiLayersEvent event) -> {
            event.registerAboveAll(
                    Identifier.fromNamespaceAndPath("hbm", "look_overlay"),
                    (graphics, deltaTracker) -> HbmLookOverlay.render(graphics));
            event.registerAboveAll(
                    Identifier.fromNamespaceAndPath("hbm", "rad_counter"),
                    (graphics, deltaTracker) -> RadCounterOverlay.render(graphics));
            event.registerAboveAll(
                    Identifier.fromNamespaceAndPath("hbm", "nuke_flash"),
                    (graphics, deltaTracker) -> com.hbm.client.NukeScreenEffects.render(graphics));
        });

        modBus.addListener((RegisterMenuScreensEvent event) -> {
            event.register(ModMenus.MACHINE_BATTERY.get(), GUIMachineBattery::new);
            event.register(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), GUIMachineAssemblyMachine::new);
            event.register(ModMenus.MACHINE_CHEMICAL_PLANT.get(), GUIMachineChemicalPlant::new);
            event.register(ModMenus.MACHINE_SOLDERING_STATION.get(), GUIMachineSolderingStation::new);
            event.register(ModMenus.MACHINE_CRYSTALLIZER.get(), GUICrystallizer::new);
            event.register(ModMenus.MACHINE_MINING_LASER.get(), com.hbm.inventory.gui.GUIMachineMiningLaser::new);
            event.register(ModMenus.MACHINE_OIL_WELL.get(), com.hbm.inventory.gui.GUIMachineOilWell::new);
            event.register(ModMenus.HEATER_OILBURNER.get(), com.hbm.inventory.gui.GUIOilburner::new);
            event.register(ModMenus.MACHINE_REFINERY.get(), com.hbm.inventory.gui.GUIMachineRefinery::new);
            event.register(ModMenus.MACHINE_DIESEL.get(), com.hbm.inventory.gui.GUIMachineDiesel::new);
            event.register(ModMenus.MACHINE_GAS_FLARE.get(), com.hbm.inventory.gui.GUIMachineGasFlare::new);
            event.register(ModMenus.MACHINE_LIQUEFACTOR.get(), com.hbm.inventory.gui.GUILiquefactor::new);
            event.register(ModMenus.MACHINE_SOLIDIFIER.get(), com.hbm.inventory.gui.GUISolidifier::new);
            event.register(ModMenus.NUKE_MAN.get(), GUINukeMan::new);
            event.register(ModMenus.FLUID_TANK.get(), GUIMachineFluidTank::new);
            event.register(ModMenus.BATTERY_SOCKET.get(), GUIBatterySocket::new);
            event.register(ModMenus.CRATE.get(), GUICrate::new);
            event.register(ModMenus.MACHINE_PRESS.get(), GUIMachinePress::new);
            event.register(ModMenus.MACHINE_EPRESS.get(), GUIMachineEPress::new);
            event.register(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), GUIMachineElectricFurnace::new);
            event.register(ModMenus.MACHINE_SHREDDER.get(), GUIMachineShredder::new);
            event.register(ModMenus.ANVIL.get(), com.hbm.inventory.gui.GUIAnvil::new);
            event.register(ModMenus.MACHINE_CRUCIBLE.get(), com.hbm.inventory.gui.GUICrucible::new);
            event.register(ModMenus.HEATER_FIREBOX.get(), com.hbm.inventory.gui.GUIFirebox::new);
            event.register(ModMenus.MACHINE_VACUUM_DISTILL.get(), com.hbm.inventory.gui.GUIMachineVacuumDistill::new);
            event.register(ModMenus.MACHINE_CATALYTIC_REFORMER.get(), com.hbm.inventory.gui.GUIMachineCatalyticReformer::new);
            event.register(ModMenus.MACHINE_HYDROTREATER.get(), com.hbm.inventory.gui.GUIMachineHydrotreater::new);
            event.register(ModMenus.MACHINE_COKER.get(), com.hbm.inventory.gui.GUIMachineCoker::new);
            event.register(ModMenus.MACHINE_PYROOVEN.get(), com.hbm.inventory.gui.GUIPyroOven::new);
            event.register(ModMenus.MACHINE_COMBUSTION_ENGINE.get(), com.hbm.inventory.gui.GUICombustionEngine::new);
            event.register(ModMenus.MACHINE_COMPRESSOR.get(), com.hbm.inventory.gui.GUICompressor::new);
        });

        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            HbmBlockEntityRenderers.registerAll(new HbmBlockEntityRenderers.Sink() {
                @Override
                public <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, ?> provider) {
                    event.registerBlockEntityRenderer(type, provider);
                }
            });
            HbmEntityRenderers.registerAll(new HbmEntityRenderers.Sink() {
                @Override
                public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                    event.registerEntityRenderer(type, provider);
                }
            });
        });

        // Block tint sources (sellafite shade tint).
        modBus.addListener((RegisterColorHandlersEvent.BlockTintSources event) ->
                HbmBlockColors.registerAll(event::register));

        modBus.addListener((RegisterSpecialModelRendererEvent event) ->
                event.register(MachineItemModelRenderer.ID, MachineItemModelRenderer.Unbaked.MAP_CODEC));
    }
}
