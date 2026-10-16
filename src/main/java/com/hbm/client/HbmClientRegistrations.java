package com.hbm.client;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKTerminal;
import com.hbm.client.anim.GunAnimations;
import com.hbm.client.anim.GunRecoil;
import com.hbm.client.anim.LagAnimationLoader;
import com.hbm.client.crucible.CrucibleClient;
import com.hbm.client.gun.GunSmokeClient;
import com.hbm.client.jetpack.JetpackClientBridge;
import com.hbm.client.jetpack.JetpackClientVisuals;
import com.hbm.client.jetpack.JetpackHudOverlay;
import com.hbm.client.jetpack.JetpackRenderLayer;
import com.hbm.client.gun.GluonGunClient;
import com.hbm.client.gun.JadeShotgunClient;
import com.hbm.client.render.EgonBackpackRenderLayer;
import com.hbm.client.jetpack.JetpackSmokeClient;
import com.hbm.client.particle.CasingParticles;
import com.hbm.client.particle.DebrisParticles;
import com.hbm.client.particle.DisintegrationParticles;
import com.hbm.client.render.ConveyorPreviewRenderer;
import com.hbm.client.render.HbmBlockColors;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.HbmEntityRenderers;
import com.hbm.blocks.machine.rbmk.RBMKGauge;
import com.hbm.client.render.MultiblockPlacementHighlight;
import com.hbm.client.render.RenderBobble;
import com.hbm.client.render.RenderDecoBlockAlt;
import com.hbm.client.render.RtgClientRegistrations;
import com.hbm.client.render.TorexBenchmarkClient;
import com.hbm.client.render.armor.ArmorModRenderLayer;
import com.hbm.client.render.armor.ArmorRenderLayer;
import com.hbm.client.render.armor.ArmorRenderers;
import com.hbm.client.render.armor.WornModelRenderLayer;
import com.hbm.client.render.entity.EntityRenderSmokeClient;
import com.hbm.client.render.flywheel.FlywheelCompat;
import com.hbm.client.render.projectile.TracerRegistry;
import com.hbm.client.render.util.RenderAccessoryUtility;
import com.hbm.client.render.util.SpecialPlayerRenderLayer;
import com.hbm.client.render.weapon.DrillHighlightClient;
import com.hbm.client.render.weapon.GunPlayerPose;
import com.hbm.client.render.weapon.WeaponRenderers;
import com.hbm.client.render.world.NTMSkyboxes;
import com.hbm.client.sound.AssemblyFactorySoundHandler;
import com.hbm.client.sound.ClientAudioFactory;
import com.hbm.commands.HbmClientCommands;
import com.hbm.handler.JetpackHandler;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.blocks.StandardInfo;
import com.hbm.inventory.gui.GUIAnvil;
import com.hbm.inventory.gui.GUIArmorTable;
import com.hbm.inventory.gui.GUIAshpit;
import com.hbm.inventory.gui.GUICartCrate;
import com.hbm.inventory.gui.GUICartDestroyer;
import com.hbm.inventory.gui.GUICore;
import com.hbm.inventory.gui.GUICoreEmitter;
import com.hbm.inventory.gui.GUICoreInjector;
import com.hbm.inventory.gui.GUICoreReceiver;
import com.hbm.inventory.gui.GUICoreStabilizer;
import com.hbm.inventory.gui.GUICrateTemplate;
import com.hbm.inventory.gui.GUICrucible;
import com.hbm.inventory.gui.GUIDiFurnace;
import com.hbm.inventory.gui.GUILaunchPad;
import com.hbm.inventory.gui.GUIMachineArcFurnaceLarge;
import com.hbm.inventory.gui.GUIMachineArcWelder;
import com.hbm.inventory.gui.GUIMachineAssemblyFactory;
import com.hbm.inventory.gui.GUIMachineChemicalFactory;
import com.hbm.inventory.gui.GUIMachineStrandCaster;
import com.hbm.inventory.gui.GUIMassStorage;
import com.hbm.inventory.gui.GUIPADetector;
import com.hbm.inventory.gui.GUIPADipole;
import com.hbm.inventory.gui.GUIPAQuadrupole;
import com.hbm.inventory.gui.GUIPARFC;
import com.hbm.inventory.gui.GUIPASource;
import com.hbm.inventory.gui.GUIScreenDesignator;
import com.hbm.inventory.gui.GUISoyuzLauncher;
import com.hbm.inventory.gui.GUITrainCargoTram;
import com.hbm.inventory.gui.GUITrainCargoTramTrailer;
import com.hbm.inventory.gui.GUITurretBase;
import com.hbm.inventory.gui.GUITurretMobFilter;
import com.hbm.inventory.gui.GUIWeaponTable;
import com.hbm.inventory.gui.GUIBatterySocket;
import com.hbm.inventory.gui.GUIBatteryREDD;
import com.hbm.inventory.gui.GUIMachineFENSU;
import com.hbm.inventory.gui.GUICrate;
import com.hbm.inventory.gui.GUICrystallizer;
import com.hbm.inventory.gui.GUICentrifuge;
import com.hbm.inventory.gui.GUIMachineCyclotron;
import com.hbm.inventory.gui.GUIMachineGasCent;
import com.hbm.inventory.gui.GUIMachineElectrolyser;
import com.hbm.inventory.gui.GUIMixer;
import com.hbm.inventory.gui.GUIMachineExposureChamber;
import com.hbm.inventory.gui.GUICombustionEngine;
import com.hbm.inventory.gui.GUICompressor;
import com.hbm.inventory.gui.GUIFEL;
import com.hbm.inventory.gui.GUIFirebox;
import com.hbm.inventory.gui.GUIHeaterHeatex;
import com.hbm.inventory.gui.GUIMachineWoodBurner;
import com.hbm.inventory.gui.GUILiquefactor;
import com.hbm.inventory.gui.GUIMachineAssemblyMachine;
import com.hbm.inventory.gui.GUIMachinePrecAss;
import com.hbm.inventory.gui.GUIMachineBattery;
import com.hbm.inventory.gui.GUIMachineCatalyticReformer;
import com.hbm.inventory.gui.GUIMachineChemicalPlant;
import com.hbm.inventory.gui.GUIMachineCoker;
import com.hbm.inventory.gui.GUIFurnaceCombination;
import com.hbm.inventory.gui.GUIMachineDiesel;
import com.hbm.inventory.gui.GUIMachineEPress;
import com.hbm.inventory.gui.GUIMachineElectricFurnace;
import com.hbm.inventory.gui.GUIMachineFluidTank;
import com.hbm.inventory.gui.GUIMachineGiantTank;
import com.hbm.inventory.gui.GUIF6Tank;
import com.hbm.inventory.gui.GUIMachineGasFlare;
import com.hbm.inventory.gui.GUIMachineHydrotreater;
import com.hbm.inventory.gui.GUIMachineMiningLaser;
import com.hbm.inventory.gui.GUIMachineOilWell;
import com.hbm.inventory.gui.GUIMachinePUREX;
import com.hbm.inventory.gui.GUIMachinePress;
import com.hbm.inventory.gui.GUIMachineAmmoPress;
import com.hbm.inventory.gui.GUIMachineRefinery;
import com.hbm.inventory.gui.GUIMachineShredder;
import com.hbm.inventory.gui.GUIMachineSolderingStation;
import com.hbm.inventory.gui.GUIMachineTurbine;
import com.hbm.inventory.gui.GUIMachineLargeTurbine;
import com.hbm.inventory.gui.GUIMachineTurbineGas;
import com.hbm.inventory.gui.GUIMachineTurbofan;
import com.hbm.inventory.gui.GUIMachineVacuumDistill;
import com.hbm.inventory.gui.GUINukeMan;
import com.hbm.inventory.gui.GUINukeN2;
import com.hbm.inventory.gui.GUINukeBoy;
import com.hbm.inventory.gui.GUINukeGadget;
import com.hbm.inventory.gui.GUINukeTsar;
import com.hbm.inventory.gui.GUIOilburner;
import com.hbm.inventory.gui.GUIReactorZirnox;
import com.hbm.inventory.gui.GUISILEX;
import com.hbm.inventory.gui.GUISolidifier;
import com.hbm.inventory.gui.GUIWasteDrum;
import com.hbm.inventory.gui.GUIStorageDrum;
import com.hbm.inventory.gui.GUIMachineRadGen;
import com.hbm.inventory.gui.GUIFusionKlystron;
import com.hbm.inventory.gui.GUIFusionTorus;
import com.hbm.inventory.gui.GUIFusionBreeder;
import com.hbm.inventory.gui.GUIMachinePlasmaForge;
import com.hbm.inventory.gui.GUIWatz;
import com.hbm.inventory.gui.GUIPyroOven;
import com.hbm.inventory.gui.GUIBlastFurnace;
import com.hbm.inventory.gui.GUIFurnaceBrick;
import com.hbm.inventory.gui.GUIFurnaceIron;
import com.hbm.inventory.gui.GUIFurnaceSteel;
import com.hbm.inventory.gui.GunSupportClientRegistrations;
import com.hbm.inventory.gui.RadioAutocalScreen;
import com.hbm.inventory.gui.RadioControllerScreen;
import com.hbm.inventory.gui.RadioCounterScreen;
import com.hbm.inventory.gui.RadioLogicScreen;
import com.hbm.inventory.gui.RadioReaderScreen;
import com.hbm.inventory.gui.RadioTelexScreen;
import com.hbm.inventory.gui.RadioTorchScreen;
import com.hbm.inventory.gui.ScreenRBMKGauge;
import com.hbm.inventory.gui.ScreenRBMKTerminal;
import com.hbm.items.ModelState;
import com.hbm.items.tool.ItemDesignatorManual;
import com.hbm.items.tool.ItemTurretMobFilter;
import com.hbm.items.weapon.sedna.factory.GunFactoryClient;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModArmorSetBBlockEntities;
import dev.golem.core.client.BlockLayers;
import dev.golem.core.client.CameraHooks;
import dev.golem.core.client.Capes;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.Crosshairs;
import dev.golem.core.client.DebugLines;
import dev.golem.core.client.Fov;
import dev.golem.core.client.ItemModelProperties;
import dev.golem.core.client.PlayerRenderHooks;
import dev.golem.core.client.Tints;
import dev.golem.core.fluid.TokenFluids;
import dev.golem.core.fluid.WorldFluids;
import dev.golem.core.hook.Hook;
import dev.golem.core.platform.Platform;
import dev.golem.core.render.CompatBERProvider;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.HeatDistortionRenderType;
import dev.golem.core.render.HumanoidLayers;
import dev.golem.core.render.Instancing;
import dev.golem.core.render.SceneWarp;
import dev.golem.core.render.ScreenEffects;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

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
        void register(String id, Consumer<GuiGraphicsExtractor> renderer);
    }

    public static void registerScreens(ScreenSink sink) {
        ItemDesignatorManual.setClientScreenOpener(stack ->
                Minecraft.getInstance().setScreen(
                        new GUIScreenDesignator(stack)));
        ItemTurretMobFilter.setClientScreenOpener(turret ->
                Minecraft.getInstance().setScreen(
                        new GUITurretMobFilter(turret)));
        AssemblyFactorySoundHandler.setAssemblerCut(ModSounds.ASSEMBLER_CUT.get());
        sink.register(ModMenus.MACHINE_BATTERY.get(), GUIMachineBattery::new);
        sink.register(ModMenus.MACHINE_WASTE_DRUM.get(), GUIWasteDrum::new);
        sink.register(ModMenus.MACHINE_STORAGE_DRUM.get(), GUIStorageDrum::new);
        sink.register(ModMenus.MACHINE_RAD_GEN.get(), GUIMachineRadGen::new);
        sink.register(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), GUIMachineAssemblyMachine::new);
        sink.register(ModMenus.MACHINE_PREC_ASS.get(), GUIMachinePrecAss::new);
        sink.register(ModMenus.MACHINE_CHEMICAL_PLANT.get(), GUIMachineChemicalPlant::new);
        sink.register(ModMenus.MACHINE_MIXER.get(), GUIMixer::new);
        sink.register(ModMenus.MACHINE_EXPOSURE_CHAMBER.get(), GUIMachineExposureChamber::new);
        sink.register(ModMenus.PA_SOURCE.get(), GUIPASource::new);
        sink.register(ModMenus.PA_RFC.get(), GUIPARFC::new);
        sink.register(ModMenus.PA_QUADRUPOLE.get(), GUIPAQuadrupole::new);
        sink.register(ModMenus.PA_DIPOLE.get(), GUIPADipole::new);
        sink.register(ModMenus.PA_DETECTOR.get(), GUIPADetector::new);
        sink.register(ModMenus.SOYUZ_LAUNCHER.get(), GUISoyuzLauncher::new);
        sink.register(ModMenus.DFC_CORE.get(), GUICore::new);
        sink.register(ModMenus.DFC_EMITTER.get(), GUICoreEmitter::new);
        sink.register(ModMenus.DFC_RECEIVER.get(), GUICoreReceiver::new);
        sink.register(ModMenus.DFC_INJECTOR.get(), GUICoreInjector::new);
        sink.register(ModMenus.DFC_STABILIZER.get(), GUICoreStabilizer::new);
        sink.register(ModMenus.MACHINE_BLAST_FURNACE.get(), GUIBlastFurnace::new);
        sink.register(ModMenus.MACHINE_PUREX.get(), GUIMachinePUREX::new);
        sink.register(ModMenus.MACHINE_SOLDERING_STATION.get(), GUIMachineSolderingStation::new);
        sink.register(ModMenus.MACHINE_CRYSTALLIZER.get(), GUICrystallizer::new);
        sink.register(ModMenus.MACHINE_CENTRIFUGE.get(), GUICentrifuge::new);
        sink.register(ModMenus.MACHINE_CYCLOTRON.get(), GUIMachineCyclotron::new);
        sink.register(ModMenus.MACHINE_GAS_CENT.get(), GUIMachineGasCent::new);
        sink.register(ModMenus.MACHINE_ELECTROLYSER_FLUID.get(), GUIMachineElectrolyser::new);
        sink.register(ModMenus.MACHINE_ELECTROLYSER_METAL.get(), GUIMachineElectrolyser::new);
        sink.register(ModMenus.MACHINE_SILEX.get(), GUISILEX::new);
        sink.register(ModMenus.MACHINE_FEL.get(), GUIFEL::new);
        sink.register(ModMenus.MACHINE_MINING_LASER.get(), GUIMachineMiningLaser::new);
        sink.register(ModMenus.MACHINE_OIL_WELL.get(), GUIMachineOilWell::new);
        sink.register(ModMenus.HEATER_OILBURNER.get(), GUIOilburner::new);
        sink.register(ModMenus.MACHINE_REFINERY.get(), GUIMachineRefinery::new);
        sink.register(ModMenus.MACHINE_DIESEL.get(), GUIMachineDiesel::new);
        sink.register(ModMenus.LAUNCH_PAD.get(), GUILaunchPad::new);
        sink.register(ModMenus.TURRET_BASE.get(), GUITurretBase::new);
        sink.register(ModMenus.MACHINE_TURBINE.get(), GUIMachineTurbine::new);
        sink.register(ModMenus.MACHINE_LARGE_TURBINE.get(), GUIMachineLargeTurbine::new);
        sink.register(ModMenus.MACHINE_TURBINE_GAS.get(), GUIMachineTurbineGas::new);
        sink.register(ModMenus.MACHINE_TURBOFAN.get(), GUIMachineTurbofan::new);
        sink.register(ModMenus.MACHINE_GAS_FLARE.get(), GUIMachineGasFlare::new);
        sink.register(ModMenus.MACHINE_LIQUEFACTOR.get(), GUILiquefactor::new);
        sink.register(ModMenus.MACHINE_SOLIDIFIER.get(), GUISolidifier::new);
         sink.register(ModMenus.NUKE_MAN.get(), GUINukeMan::new);
         sink.register(ModMenus.NUKE_N2.get(), GUINukeN2::new);
         sink.register(ModMenus.NUKE_BOY.get(), GUINukeBoy::new);
         sink.register(ModMenus.NUKE_GADGET.get(), GUINukeGadget::new);
         sink.register(ModMenus.NUKE_TSAR.get(), GUINukeTsar::new);
        sink.register(ModMenus.REACTOR_ZIRNOX.get(), GUIReactorZirnox::new);
        sink.register(ModMenus.WATZ.get(), GUIWatz::new);
        sink.register(ModMenus.FUSION_TORUS.get(), GUIFusionTorus::new);
        sink.register(ModMenus.FUSION_BREEDER.get(), GUIFusionBreeder::new);
        sink.register(ModMenus.FUSION_KLYSTRON.get(), GUIFusionKlystron::new);
        sink.register(ModMenus.FUSION_PLASMA_FORGE.get(), GUIMachinePlasmaForge::new);
         sink.register(ModMenus.FLUID_TANK.get(), GUIMachineFluidTank::new);
         sink.register(ModMenus.F6_TANK.get(), GUIF6Tank::new);
         sink.register(ModMenus.GIANT_TANK.get(), GUIMachineGiantTank::new);
         sink.register(ModMenus.BATTERY_SOCKET.get(), GUIBatterySocket::new);
         sink.register(ModMenus.FENSU.get(), GUIMachineFENSU::new);
         sink.register(ModMenus.BATTERY_REDD.get(), GUIBatteryREDD::new);
         sink.register(ModMenus.CRATE.get(), GUICrate::new);
         // the ntm-next entity port's vehicle menus
         sink.register(ModMenus.CART_CRATE.get(), GUICartCrate::new);
         sink.register(ModMenus.CART_DESTROYER.get(), GUICartDestroyer::new);
         sink.register(ModMenus.TRAIN_CARGO_TRAM.get(), GUITrainCargoTram::new);
         sink.register(ModMenus.TRAIN_CARGO_TRAM_TRAILER.get(), GUITrainCargoTramTrailer::new);
          sink.register(ModMenus.ARMOR_TABLE.get(), GUIArmorTable::new);
          sink.register(ModMenus.WEAPON_TABLE.get(), GUIWeaponTable::new);
         sink.register(ModMenus.MACHINE_PRESS.get(), GUIMachinePress::new);
          sink.register(ModMenus.MACHINE_AMMO_PRESS.get(), GUIMachineAmmoPress::new);
          GunSupportClientRegistrations.registerScreens(sink);
         sink.register(ModMenus.MACHINE_EPRESS.get(), GUIMachineEPress::new);
        sink.register(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), GUIMachineElectricFurnace::new);
        sink.register(ModMenus.FURNACE_BRICK.get(), GUIFurnaceBrick::new);
        sink.register(ModMenus.FURNACE_IRON.get(), GUIFurnaceIron::new);
        sink.register(ModMenus.FURNACE_STEEL.get(), GUIFurnaceSteel::new);
        sink.register(ModMenus.MACHINE_SHREDDER.get(), GUIMachineShredder::new);
        sink.register(ModMenus.ANVIL.get(), GUIAnvil::new);
        sink.register(ModMenus.MACHINE_CRUCIBLE.get(), GUICrucible::new);
         sink.register(ModMenus.HEATER_FIREBOX.get(), GUIFirebox::new);
         sink.register(ModMenus.HEATER_HEATEX.get(), GUIHeaterHeatex::new);
         sink.register(ModMenus.MACHINE_WOOD_BURNER.get(), GUIMachineWoodBurner::new);
        sink.register(ModMenus.MACHINE_VACUUM_DISTILL.get(), GUIMachineVacuumDistill::new);
        sink.register(ModMenus.MACHINE_CATALYTIC_REFORMER.get(), GUIMachineCatalyticReformer::new);
        sink.register(ModMenus.MACHINE_HYDROTREATER.get(), GUIMachineHydrotreater::new);
        sink.register(ModMenus.MACHINE_COKER.get(), GUIMachineCoker::new);
        sink.register(ModMenus.FURNACE_COMBINATION.get(), GUIFurnaceCombination::new);
        sink.register(ModMenus.MACHINE_PYROOVEN.get(), GUIPyroOven::new);
        sink.register(ModMenus.MACHINE_COMBUSTION_ENGINE.get(), GUICombustionEngine::new);
        sink.register(ModMenus.MACHINE_COMPRESSOR.get(), GUICompressor::new);
        sink.register(ModMenus.MACHINE_ARC_WELDER.get(), GUIMachineArcWelder::new);
        sink.register(ModMenus.MACHINE_ASSEMBLY_FACTORY.get(), GUIMachineAssemblyFactory::new);
        sink.register(ModMenus.MACHINE_CHEMICAL_FACTORY.get(), GUIMachineChemicalFactory::new);
        sink.register(ModMenus.ASHPIT.get(), GUIAshpit::new);
        sink.register(ModMenus.MACHINE_STRAND_CASTER.get(), GUIMachineStrandCaster::new);
        sink.register(ModMenus.MACHINE_ARC_FURNACE.get(), GUIMachineArcFurnaceLarge::new);
        BombClientRegistrations.registerScreens(sink);
        Bombs2ClientRegistrations.registerScreens(sink);
        CustomMissileClientRegistrations.registerScreens(sink);
        RadarUtilityClientRegistrations.registerScreens(sink);
        SatelliteClientRegistrations.registerScreens(sink);
        DecoCrateClientRegistrations.registerScreens(sink);
        GapsBClientRegistrations.registerScreens(sink); // G-B: held crates, wrench readout
        IcfClientRegistrations.registerScreens(sink);
        DungeonClientRegistrations.registerScreens(sink);
        RtgClientRegistrations.registerScreen(sink);
        sink.register(ModMenus.RADIO_TORCH.get(), RadioTorchScreen::new);
        sink.register(ModMenus.RADIO_READER.get(), RadioReaderScreen::new);
        sink.register(ModMenus.RADIO_LOGIC.get(), RadioLogicScreen::new);
        sink.register(ModMenus.RADIO_COUNTER.get(), RadioCounterScreen::new);
        sink.register(ModMenus.RADIO_CONTROLLER.get(), RadioControllerScreen::new);
        sink.register(ModMenus.RADIO_TELEX.get(), RadioTelexScreen::new);
        sink.register(ModMenus.RADIO_AUTOCAL.get(), RadioAutocalScreen::new);
        FluidNetClientRegistrations.registerScreens(sink);
        ConveyorClientRegistrations.registerScreens(sink);
        EnergyNetClientRegistrations.registerScreens(sink);
        PneumaticClientRegistrations.registerScreens(sink);
        DroneClientRegistrations.registerScreens(sink);
        ProductionClientRegistrations.registerScreens(sink);
        Production2ClientRegistrations.registerScreens(sink);
        SecurityClientRegistrations.registerScreens(sink);
        NuclearMiscClientRegistrations.registerScreens(sink);
        PwrClientRegistrations.registerScreens(sink);
        PowerGenClientRegistrations.registerScreens(sink);
        ExplosivesClientRegistrations.registerScreens(sink);
        MeleeKitClientRegistrations.registerScreens(sink);
        UtilityToolClientRegistrations.registerScreens(sink);
        CustomMachineClientRegistrations.registerScreens(sink);
        sink.register(ModMenus.MACHINE_DIFURNACE.get(), GUIDiFurnace::new);
        sink.register(ModMenus.MASS_STORAGE.get(), GUIMassStorage::new);
        sink.register(ModMenus.CRATE_TEMPLATE.get(), GUICrateTemplate::new);
        RBMKClientScreens.register(sink);
        RBMKGauge.OPEN_SCREEN = gauge -> Minecraft.getInstance().setScreen(new ScreenRBMKGauge(gauge));
        RBMKTerminal.OPEN_SCREEN = terminal ->
                Minecraft.getInstance().setScreen(
                        new ScreenRBMKTerminal(terminal));
    }

    public static void registerHud(HudSink sink) {
        GunFactoryClient.init();
        // final armor B: goggles blur first, as Forge drew helmet overlays beneath the rest of the HUD
        ArmorSetBClient.registerHud(sink);
        sink.register("look_overlay", HbmLookOverlay::render);
        sink.register("boss_status", BossStatus::render); // 1.7 BossStatus: render-pushed boss bar
        sink.register("rad_counter", RadCounterOverlay::render);
        EnergyNetClientRegistrations.registerHud(sink);
        sink.register("nuke_flash", NukeScreenEffects::render);
        sink.register("gun_hud", GunHudOverlay::render);
        sink.register("gun_scope", GunScopeOverlay::render);
        sink.register("armor_hud", ArmorHudOverlay::render);
        ArmorSetClient.registerHud(sink); // final armor sets A: dash bar, HEV health/armour replacement
        sink.register("jetpack_hud", JetpackHudOverlay::render);
        AbilityToolClientRegistrations.registerHud(sink);
        RenderInputClientRegistrations.registerHud(sink); // armour-mod shield bar
        if (System.getenv("HBM_JETPACK_SCREENSHOTS") != null && Platform.isDevelopment())
            sink.register("jetpack_visual_check", JetpackSmokeClient::render);
        if (GunSmokeClient.enabled() && Platform.isDevelopment())
            sink.register("gun_smoke_capture", GunSmokeClient::render);
    }

    /**
     * Every HBM screen and renderer through Golem Core's loader-neutral {@code ClientRegistrations}: the same path an
     * addon's client start-up takes. Called once from each loader's client init.
     */
    public static void installScreensAndRenderers() {
        ClientRegistrations.screens(screens -> registerScreens(screens::register));
        ClientRegistrations.renderers(renderers -> registerRenderers(
                new HbmBlockEntityRenderers.Sink() {
                    @Override
                    public <T extends BlockEntity,
                            S extends CompatRenderState> void register(
                            BlockEntityType<? extends T> type,
                            CompatBERProvider<T, S> provider) {
                        renderers.blockEntity(type, provider);
                    }
                }, new HbmEntityRenderers.Sink() {
                    @Override
                    public <T extends Entity> void register(
                            EntityType<? extends T> type,
                            EntityRendererProvider<T> provider) {
                        renderers.entity(type, provider);
                    }
                }));
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink blockEntities,
            HbmEntityRenderers.Sink downstream) {
        HbmBlockEntityRenderers.registerAll(blockEntities);
        RailClientRegistrations.registerRenderers(blockEntities);
        Set<EntityType<?>> bound = new HashSet<>();
        HbmEntityRenderers.Sink entities = HbmEntityRenderers.recording(downstream, bound);
        HbmEntityRenderers.registerAll(entities);
        FluidNetClientRegistrations.registerRenderers(blockEntities);
        ConveyorClientRegistrations.registerRenderers(blockEntities);
        EnergyNetClientRegistrations.registerRenderers(blockEntities);
        PneumaticClientRegistrations.registerRenderers(blockEntities);
        ProductionClientRegistrations.registerRenderers(blockEntities);
        Production2ClientRegistrations.registerRenderers(blockEntities);
        DoorClientRegistrations.registerRenderers(blockEntities);
        SecurityClientRegistrations.registerRenderers(blockEntities);
        NuclearMiscClientRegistrations.registerRenderers(blockEntities);
        PowerGenClientRegistrations.registerRenderers(blockEntities);
        ExplosivesClientRegistrations.registerRenderers(blockEntities);
        DecoClientRegistrations.registerRenderers(blockEntities);
        BombClientRegistrations.registerBlockEntityRenderers(blockEntities);
        Bombs2ClientRegistrations.registerRenderers(blockEntities);
        CustomMissileClientRegistrations.registerRenderers(blockEntities);
        RadarUtilityClientRegistrations.registerRenderers(blockEntities);
        SatelliteClientRegistrations.registerRenderers(blockEntities);
        CustomMachineClientRegistrations.registerRenderers(blockEntities);
        DecoCrateClientRegistrations.registerRenderers(blockEntities);
        GapsBClientRegistrations.registerRenderers(blockEntities); // G-B: pipe anchor + pipelines
        HbmBlockEntityRenderers.regGem(blockEntities, ModArmorSetBBlockEntities.STATUE_ELB_F.get(),
                RenderDecoBlockAlt::new); // final armor B: statue_elb_f
        IcfClientRegistrations.registerRenderers(blockEntities);
        EnergyBedrockClientRegistrations.registerRenderers(blockEntities);
        DungeonClientRegistrations.registerRenderers(blockEntities);
        StructureWandClientRegistrations.registerRenderers(blockEntities);
        BombClientRegistrations.registerEntityRenderers(entities);
        SatelliteClientRegistrations.registerEntityRenderers(entities);
        RtgClientRegistrations.registerRenderer(blockEntities);
        HbmEntityRenderers.verifyCoverage(bound);

        // Every instanced visual is optional acceleration beside an already-registered vanilla BER.
        if (FlywheelCompat.isSupportedOnThisVersion()) {
            FlywheelCompat.registerVisuals();
        }

    }

    /**
     * The instancing engine's own count, on the F3 screen.
     *
     * <p>An instanced and a vanilla draw are pixel-identical when both are right, so the only way to tell a working
     * engine from one that silently received nothing is to count what it holds: models against models drawn, and
     * draws against parts (many copies of a machine should cost one draw).
     */
    public static void installDebugLines() {
        DebugLines.register(Constants.MOD_ID, "gemrender", out -> {
            Instancing.Report report = Instancing.report();
            out.accept("gemrender: " + report.backend() + ", " + report.owning() + "/" + report.visuals()
                    + " models (" + report.animated() + " animated, " + report.moving() + " moving), "
                    + report.instances() + " parts, " + report.draws() + " draws");
            out.accept("gemrender: " + report.writes() + " transforms written, " + report.skipped()
                    + " parts skipped");
            int fallen = report.visuals() - report.healthy();
            if (fallen > 0) out.accept("gemrender: " + fallen + " models fell back to vanilla");
        });
    }

    public static void installTints() {
        Tints.install(Constants.MOD_ID);
        BlockLayers.install(Constants.MOD_ID);
        EnergyBedrockClientRegistrations.installSprites(); // computed bedrock ore grade art
        TokenFluids.installClient(); // how other mods draw HBM fluids
        WorldFluids.installClient(); // the placeable 1.7 fluids' sprites and layers
    }

    /**
     * {@code hbm:model_state}: the sprite an item with typed state draws ({@link com.hbm.items.ModelState}), computed
     * from that state and never saved.
     */
    public static void installItemModelProperties() {
        UtilityToolClientRegistrations.installItemModelProperties();
        ItemModelProperties.register(Constants.MOD_ID, "model_state",
                ModelState::of);
    }

    public static void registerBlockColors() {
        HbmBlockColors.registerAll();
        ConveyorClientRegistrations.registerBlockColors();
        TerrainClientRegistrations.registerBlockColors();
    }

    /**
     * Attaches the worn-armour layer to every humanoid renderer, and declares the suits' part trees.
     *
     * <p>Golem Core owns the attaching (a mod-bus event on NeoForge/Forge, a per-renderer callback on Fabric). Check
     * the layer count it reports ({@code HumanoidLayers.attachedCount}): a layer attached to nothing draws nothing
     * and throws nothing.
     */
    public static void installArmorLayers() {
        ArmorRenderers.init();
        installJetpackBridge();
        HeatDistortionRenderType.configure(
                "hbm:pipeline/heat_distortion", "hbm:heat_distortion");
        // port-only black hole lensing (BlackHoleRenderer.Lens)
        SceneWarp.configure("hbm:pipeline/black_hole_lens", "hbm:black_hole_lens");
        // 1.12 HbmShaderManager2 bloom/distortion layers, composited after the hand (first-person crucible blade)
        ScreenEffects.init();
        // 1.12.2 crucible/sword cutters: first-person Collada scene, slicing input, cut mobs
        CrucibleClient.register();
        // 1.12.2 gluon gun (beam, flare, burn trail, disintegration, sounds) and jade shotgun (recoil, smoke)
        GluonGunClient.register();
        JadeShotgunClient.register();
        JetpackClientVisuals.init();
         JetpackSmokeClient.register();
         EntityRenderSmokeClient.register();
         GunSmokeClient.register();
         TorexBenchmarkClient.register();
        ClientResourceReloads.register(Constants.MOD_ID, "jetpack_visuals",
                () -> {
                    ArmorRenderers.reload();
                    JetpackClientVisuals.reload();
                });
        // HBM's player hooks run in the phase hbm:player, before the default phase addons register in (their
        // listeners see HBM's pose, as when HBM's was a single slot run first).
        PlayerRenderHooks.POSE.addPhaseOrdering("hbm:player", Hook.DEFAULT_PHASE);
        PlayerRenderHooks.POSE.register("hbm:player",
                JetpackClientVisuals::applyPlayerPose);
        PlayerRenderHooks.MODEL.addPhaseOrdering("hbm:player", Hook.DEFAULT_PHASE);
        PlayerRenderHooks.MODEL.register("hbm:player", parts -> {
            // The source applies gun aimedBow during model setup; keep its arm pose after the glider's
            // flight freeze while retaining the glider's body, head and leg precedence.
            JetpackClientVisuals.applyPlayerModelPose(parts);
            GunPlayerPose.apply(parts);
        });
        CameraHooks.LOWER_THIRD_PERSON.addPhaseOrdering("hbm:player", Hook.DEFAULT_PHASE);
        CameraHooks.LOWER_THIRD_PERSON.register("hbm:player",
                JetpackClientVisuals::lowerThirdPersonCamera);
        HumanoidLayers.register(
                ArmorRenderLayer::new);
        HumanoidLayers.register(
                JetpackRenderLayer::new);
        HumanoidLayers.register(ArmorModRenderLayer::new);
        HumanoidLayers.register(WornModelRenderLayer::new); // final armor B: goggles, glasses, hat, no9
        HumanoidLayers.register(EgonBackpackRenderLayer::new);
        // 1.7 RenderAccessoryUtility: special players' wings, axe pack and tail, and the HBM capes
        HumanoidLayers.register(SpecialPlayerRenderLayer::new);
        Capes.register(RenderAccessoryUtility::getCloakFromPlayer);
        SpecialPlayerHarnessQueries.register();
        RenderBobble.preloadSkins(); // 1.12.2 ClientHttpHandler.preinit: skinned bobbleheads
        ArmorModHarnessQueries.register();
        StructureHarnessQueries.register(); // harness query hbm_structure: NBT structure starts, placement score, loot state
        WorldFeatureHarnessQueries.register(); // harness query hbm_worldgen: block counts/finds, biome, meteor scheduler
        SmallHarnessQueries.register(); // hbm_small: boss bar, rad fog, impact spawn refusals, sulfur columns
        ReloadedGunHarnessQueries.register(); // hbm_reloaded_guns: gluon gun and jade shotgun client state
        ComponentHarnessQueries.register(); // harness query hbm_wgc: features/bunker/meteor_dungeon starts, hardcoded piece builds
        LegacyDungeonHarnessQueries.register(); // harness query hbm_wgd: legacy dungeon starts, placement score, loot/spawners
        GapsAHarnessQueries.register(); // harness query hbm_gapsa: held stack/attack damage/cells, hurt jolt, amat flashes
        GapsBHarnessQueries.register(); // harness query hbm_gapsb: boss spawns, reputation, machine damage, structure tiles, radar
        ForeignFluidHarnessQueries.register(); // harness query hbm_fluidcaps: HBM fluids through the loader fluid capability (19ek-fluids)
    }

    /** Connects the client-owned visuals to the common JetpackHandler without making it import client APIs. */
    private static void installJetpackBridge() {
        JetpackHandler.setClientExhaustCallback((level, payload) ->
                JetpackClientVisuals.spawnExhaustCue(level, payload.playerId(), payload.mode()));
        JetpackClientBridge.install(player -> {
            JetpackHandler.JetpackInfo info = JetpackHandler.getState(player);
            if (info == null) return null;
            return new JetpackClientBridge.State() {
                @Override public boolean opening() { return info.opening; }
                @Override public boolean hover() { return info.hover; }
                @Override public float thrust() { return info.thrust; }
                @Override public float prevThrust() { return info.prevThrust; }
                @Override public int jetpackFlyTime() { return info.jetpackFlyTime; }
                @Override public int prevJetpackFlyTime() { return info.prevJetpackFlyTime; }
                @Override public int failureTicks() { return info.failureTicks; }
                @Override public boolean useCompactHUD() { return info.useCompactHUD; }
                @Override public long animTime() { return info.animTime; }
                @Override public boolean active() {
                    return JetpackHandler.isJetpackActive(player);
                }
            };
        }, stack -> {
            if (stack.isEmpty()) return null;
            FluidType type = JetpackHandler.getFuelType(stack);
            return new JetpackClientBridge.Fuel(type == null ? "" : type.getName(),
                    JetpackHandler.getFuel(stack),
                    JetpackHandler.getMaxFuel(stack));
        });
    }

    /** Registers HBM's key mappings and starts the per-tick input poll that drives guns. */
    public static void installKeybinds() {
        HbmKeybindsClient.register();
        ArmorSetClient.registerKeys(); // final armor sets A: bismuth dash key
        QMAWClient.register();
        CanneryClient.register(); // 1.7 WIAJ presentations: Left Shift + QMAW key on a hovered item
        ArmorModTooltips.register();
        StandardInfo.setShiftHeldPoll(CanneryClient::shiftHeld);
        GunRecoil.register();
        GrenadeUniversalClient.register(); // 1.7 ItemGrenadeUniversal.onUpdate's client half (arm swing)
        CraneKeybindHandler.register();
        UtilityToolClientRegistrations.installKeybinds();
        AbilityToolClientRegistrations.registerKeys();
        RenderInputClientRegistrations.installKeybinds(); // magnet toggle key
    }

    /**
     * Client halves of the ported ntm-next entities: the looped-sound factory behind
     * {@link com.hbm.sound.AudioSystem} (UFO, hunter chopper, worm), the entity particle handlers, and the
     * conveyor wand's world-frame ghost preview.
     */
    public static void installEntityEffects() {
        AudioSystem.installClientFactory(ClientAudioFactory.INSTANCE);
        ClientEffects.install();
        // 1.7 RenderOverhead.renderActionPreview: the conveyor wand's ghost run, drawn at world-frame end.
        ConveyorPreviewRenderer.init();
        // 1.7 BlockDummyable.drawPlacementHighlight: the held multiblock's footprint outline, green/red.
        MultiblockPlacementHighlight.init();
        // 1.7 RenderNTMSkyboxImpact / RenderNTMSkyboxChainloader and the tick that installs them (1.31_enableSkyboxes).
        NTMSkyboxes.register();
        SmogFog.register(); // 1.7 ModEventHandlerRenderer smog (POL_03/06/07)
        HbmClientCommands.register(); // 1.7 client commands: /ntmclient, /ntmwikirender, /dumpthreadsandcrashgame
        RenderInputClientRegistrations.install(); // overhead markers, HUD, world text, particles
    }

    /** Declares which animation each gun plays for each cue. Items resolve lazily, at render time. */
    public static void installGunAnimations() {
        GunEffectState.install();
        TracerRegistry.init();
        CasingParticles.init();
        DisintegrationParticles.init();
        DebrisParticles.init();
        GunEffectState.setCasingHandler(event -> CasingParticles.spawn(event));
        GunEffectState.setGunCasingHandler(payload -> CasingParticles.spawn(
                new CasingCreator.Event(payload.x(), payload.y(), payload.z(), payload.mX(),
                        payload.mY(), payload.mZ(), payload.yaw(), payload.pitch(), payload.mPitch(), payload.mYaw(),
                        payload.casing(), payload.smoking(), payload.smokeLife(), payload.smokeLift(), payload.nodeLife(),
                        true, payload.legacy())));
        GunAnimations.init();
        DrillHighlightClient.init();
        ClientResourceReloads.register(Constants.MOD_ID,
                "lag_animations", LagAnimationLoader::invalidate);
        // The weapon renderers a gun item definition can name. Both eras read the same definitions:
        // 26.1 through its item-model system, older versions through GunLegacyModels' own parse.
        WeaponRenderers.init();
        Fov.register(WeaponRenderers::viewFov);
        Crosshairs.register(GunCrosshairOverlay::render);
    }

}
