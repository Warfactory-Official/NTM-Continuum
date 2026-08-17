package com.hbm.client;

import com.hbm.client.render.HbmBlockColors;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.HbmEntityRenderers;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.gui.GUIAnvil;
import com.hbm.inventory.gui.GUIBatterySocket;
import com.hbm.inventory.gui.GUICrate;
import com.hbm.inventory.gui.GUICrystallizer;
import com.hbm.inventory.gui.GUICombustionEngine;
import com.hbm.inventory.gui.GUICompressor;
import com.hbm.inventory.gui.GUIFEL;
import com.hbm.inventory.gui.GUIFirebox;
import com.hbm.inventory.gui.GUILiquefactor;
import com.hbm.inventory.gui.GUIMachineAssemblyMachine;
import com.hbm.inventory.gui.GUIMachineBattery;
import com.hbm.inventory.gui.GUIMachineCatalyticReformer;
import com.hbm.inventory.gui.GUIMachineChemicalPlant;
import com.hbm.inventory.gui.GUIMachineCoker;
import com.hbm.inventory.gui.GUIMachineDiesel;
import com.hbm.inventory.gui.GUIMachineEPress;
import com.hbm.inventory.gui.GUIMachineElectricFurnace;
import com.hbm.inventory.gui.GUIMachineFluidTank;
import com.hbm.inventory.gui.GUIMachineGasFlare;
import com.hbm.inventory.gui.GUIMachineHydrotreater;
import com.hbm.inventory.gui.GUIMachineMiningLaser;
import com.hbm.inventory.gui.GUIMachineOilWell;
import com.hbm.inventory.gui.GUIMachinePUREX;
import com.hbm.inventory.gui.GUIMachinePress;
import com.hbm.inventory.gui.GUIMachineRefinery;
import com.hbm.inventory.gui.GUIMachineShredder;
import com.hbm.inventory.gui.GUIMachineSolderingStation;
import com.hbm.inventory.gui.GUIMachineTurbine;
import com.hbm.inventory.gui.GUIMachineVacuumDistill;
import com.hbm.inventory.gui.GUINukeMan;
import com.hbm.inventory.gui.GUIOilburner;
import com.hbm.inventory.gui.GUIReactorZirnox;
import com.hbm.inventory.gui.GUISILEX;
import com.hbm.inventory.gui.GUISolidifier;
import com.hbm.inventory.gui.GUIWasteDrum;
import com.hbm.inventory.gui.GUIPyroOven;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Loader-neutral inventory of HBM's client registrations. This class is loaded only from client
 * entrypoints; loaders supply the small API-specific sinks that perform each registration.
 */
public final class HbmClientRegistrations {

    private HbmClientRegistrations() {}

    public interface ScreenSink {
        <M extends AbstractContainerMenu, U extends AbstractContainerScreen<M>> void register(
                MenuType<M> menu, MenuScreens.ScreenConstructor<M, U> factory);
    }

    public interface HudSink {
        void register(String id, java.util.function.Consumer<GuiGraphicsExtractor> renderer);
    }

    public static void registerScreens(ScreenSink sink) {
        sink.register(ModMenus.MACHINE_BATTERY.get(), GUIMachineBattery::new);
        sink.register(ModMenus.MACHINE_WASTE_DRUM.get(), GUIWasteDrum::new);
        sink.register(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), GUIMachineAssemblyMachine::new);
        sink.register(ModMenus.MACHINE_CHEMICAL_PLANT.get(), GUIMachineChemicalPlant::new);
        sink.register(ModMenus.MACHINE_PUREX.get(), GUIMachinePUREX::new);
        sink.register(ModMenus.MACHINE_SOLDERING_STATION.get(), GUIMachineSolderingStation::new);
        sink.register(ModMenus.MACHINE_CRYSTALLIZER.get(), GUICrystallizer::new);
        sink.register(ModMenus.MACHINE_SILEX.get(), GUISILEX::new);
        sink.register(ModMenus.MACHINE_FEL.get(), GUIFEL::new);
        sink.register(ModMenus.MACHINE_MINING_LASER.get(), GUIMachineMiningLaser::new);
        sink.register(ModMenus.MACHINE_OIL_WELL.get(), GUIMachineOilWell::new);
        sink.register(ModMenus.HEATER_OILBURNER.get(), GUIOilburner::new);
        sink.register(ModMenus.MACHINE_REFINERY.get(), GUIMachineRefinery::new);
        sink.register(ModMenus.MACHINE_DIESEL.get(), GUIMachineDiesel::new);
        sink.register(ModMenus.MACHINE_TURBINE.get(), GUIMachineTurbine::new);
        sink.register(ModMenus.MACHINE_GAS_FLARE.get(), GUIMachineGasFlare::new);
        sink.register(ModMenus.MACHINE_LIQUEFACTOR.get(), GUILiquefactor::new);
        sink.register(ModMenus.MACHINE_SOLIDIFIER.get(), GUISolidifier::new);
        sink.register(ModMenus.NUKE_MAN.get(), GUINukeMan::new);
        sink.register(ModMenus.REACTOR_ZIRNOX.get(), GUIReactorZirnox::new);
        sink.register(ModMenus.FLUID_TANK.get(), GUIMachineFluidTank::new);
        sink.register(ModMenus.BATTERY_SOCKET.get(), GUIBatterySocket::new);
        sink.register(ModMenus.CRATE.get(), GUICrate::new);
        sink.register(ModMenus.MACHINE_PRESS.get(), GUIMachinePress::new);
        sink.register(ModMenus.MACHINE_EPRESS.get(), GUIMachineEPress::new);
        sink.register(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), GUIMachineElectricFurnace::new);
        sink.register(ModMenus.MACHINE_SHREDDER.get(), GUIMachineShredder::new);
        sink.register(ModMenus.ANVIL.get(), GUIAnvil::new);
        sink.register(ModMenus.MACHINE_CRUCIBLE.get(), com.hbm.inventory.gui.GUICrucible::new);
        sink.register(ModMenus.HEATER_FIREBOX.get(), GUIFirebox::new);
        sink.register(ModMenus.MACHINE_VACUUM_DISTILL.get(), GUIMachineVacuumDistill::new);
        sink.register(ModMenus.MACHINE_CATALYTIC_REFORMER.get(), GUIMachineCatalyticReformer::new);
        sink.register(ModMenus.MACHINE_HYDROTREATER.get(), GUIMachineHydrotreater::new);
        sink.register(ModMenus.MACHINE_COKER.get(), GUIMachineCoker::new);
        sink.register(ModMenus.MACHINE_PYROOVEN.get(), GUIPyroOven::new);
        sink.register(ModMenus.MACHINE_COMBUSTION_ENGINE.get(), GUICombustionEngine::new);
        sink.register(ModMenus.MACHINE_COMPRESSOR.get(), GUICompressor::new);
    }

    public static void registerHud(HudSink sink) {
        sink.register("look_overlay", HbmLookOverlay::render);
        sink.register("rad_counter", RadCounterOverlay::render);
        sink.register("nuke_flash", NukeScreenEffects::render);
        sink.register("gun_hud", GunHudOverlay::render);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink blockEntities,
            HbmEntityRenderers.Sink entities) {
        HbmBlockEntityRenderers.registerAll(blockEntities);
        HbmEntityRenderers.registerAll(entities);
    }

    public static void installTints() {
        dev.golem.core.client.Tints.install(com.hbm.Constants.MOD_ID);
    }

    public static void registerBlockColors() {
        HbmBlockColors.registerAll();
    }

    /** Registers HBM's key mappings and starts the per-tick input poll that drives guns. */
    public static void installKeybinds() {
        HbmKeybindsClient.register();
    }

    /** Declares which animation each gun plays for each cue. Items resolve lazily, at render time. */
    public static void installGunAnimations() {
        com.hbm.client.anim.GunAnimations.init();
        // The weapon renderers a gun item definition can name. Both eras read the same definitions:
        // 26.1 through its item-model system, older versions through GunLegacyModels' own parse.
        com.hbm.client.render.weapon.WeaponRenderers.init();
    }
}
