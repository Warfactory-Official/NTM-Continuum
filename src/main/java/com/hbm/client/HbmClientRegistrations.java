package com.hbm.client;

import com.hbm.client.render.HbmBlockColors;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.HbmEntityRenderers;
import com.hbm.blocks.machine.rbmk.RBMKGauge;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.gui.GUIAnvil;
import com.hbm.inventory.gui.GUIArmorTable;
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
        com.hbm.items.tool.ItemDesignatorManual.setClientScreenOpener(stack ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.hbm.inventory.gui.GUIScreenDesignator(stack)));
        com.hbm.items.tool.ItemTurretMobFilter.setClientScreenOpener(turret ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.hbm.inventory.gui.GUITurretMobFilter(turret)));
        com.hbm.client.sound.AssemblyFactorySoundHandler.setAssemblerCut(com.hbm.sound.ModSounds.ASSEMBLER_CUT.get());
        sink.register(ModMenus.MACHINE_BATTERY.get(), GUIMachineBattery::new);
        sink.register(ModMenus.MACHINE_WASTE_DRUM.get(), GUIWasteDrum::new);
        sink.register(ModMenus.MACHINE_STORAGE_DRUM.get(), GUIStorageDrum::new);
        sink.register(ModMenus.MACHINE_RAD_GEN.get(), GUIMachineRadGen::new);
        sink.register(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), GUIMachineAssemblyMachine::new);
        sink.register(ModMenus.MACHINE_PREC_ASS.get(), GUIMachinePrecAss::new);
        sink.register(ModMenus.MACHINE_CHEMICAL_PLANT.get(), GUIMachineChemicalPlant::new);
        sink.register(ModMenus.MACHINE_MIXER.get(), GUIMixer::new);
        sink.register(ModMenus.MACHINE_EXPOSURE_CHAMBER.get(), GUIMachineExposureChamber::new);
        sink.register(ModMenus.PA_SOURCE.get(), com.hbm.inventory.gui.GUIPASource::new);
        sink.register(ModMenus.PA_RFC.get(), com.hbm.inventory.gui.GUIPARFC::new);
        sink.register(ModMenus.PA_QUADRUPOLE.get(), com.hbm.inventory.gui.GUIPAQuadrupole::new);
        sink.register(ModMenus.PA_DIPOLE.get(), com.hbm.inventory.gui.GUIPADipole::new);
        sink.register(ModMenus.PA_DETECTOR.get(), com.hbm.inventory.gui.GUIPADetector::new);
        sink.register(ModMenus.SOYUZ_LAUNCHER.get(), com.hbm.inventory.gui.GUISoyuzLauncher::new);
        sink.register(ModMenus.DFC_CORE.get(), com.hbm.inventory.gui.GUICore::new);
        sink.register(ModMenus.DFC_EMITTER.get(), com.hbm.inventory.gui.GUICoreEmitter::new);
        sink.register(ModMenus.DFC_RECEIVER.get(), com.hbm.inventory.gui.GUICoreReceiver::new);
        sink.register(ModMenus.DFC_INJECTOR.get(), com.hbm.inventory.gui.GUICoreInjector::new);
        sink.register(ModMenus.DFC_STABILIZER.get(), com.hbm.inventory.gui.GUICoreStabilizer::new);
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
        sink.register(ModMenus.LAUNCH_PAD.get(), com.hbm.inventory.gui.GUILaunchPad::new);
        sink.register(ModMenus.TURRET_BASE.get(), com.hbm.inventory.gui.GUITurretBase::new);
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
         sink.register(ModMenus.CART_CRATE.get(), com.hbm.inventory.gui.GUICartCrate::new);
         sink.register(ModMenus.CART_DESTROYER.get(), com.hbm.inventory.gui.GUICartDestroyer::new);
         sink.register(ModMenus.TRAIN_CARGO_TRAM.get(), com.hbm.inventory.gui.GUITrainCargoTram::new);
         sink.register(ModMenus.TRAIN_CARGO_TRAM_TRAILER.get(), com.hbm.inventory.gui.GUITrainCargoTramTrailer::new);
          sink.register(ModMenus.ARMOR_TABLE.get(), GUIArmorTable::new);
          sink.register(ModMenus.WEAPON_TABLE.get(), GUIWeaponTable::new);
         sink.register(ModMenus.MACHINE_PRESS.get(), GUIMachinePress::new);
          sink.register(ModMenus.MACHINE_AMMO_PRESS.get(), GUIMachineAmmoPress::new);
          com.hbm.inventory.gui.GunSupportClientRegistrations.registerScreens(sink);
         sink.register(ModMenus.MACHINE_EPRESS.get(), GUIMachineEPress::new);
        sink.register(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), GUIMachineElectricFurnace::new);
        sink.register(ModMenus.FURNACE_BRICK.get(), GUIFurnaceBrick::new);
        sink.register(ModMenus.FURNACE_IRON.get(), GUIFurnaceIron::new);
        sink.register(ModMenus.FURNACE_STEEL.get(), GUIFurnaceSteel::new);
        sink.register(ModMenus.MACHINE_SHREDDER.get(), GUIMachineShredder::new);
        sink.register(ModMenus.ANVIL.get(), GUIAnvil::new);
        sink.register(ModMenus.MACHINE_CRUCIBLE.get(), com.hbm.inventory.gui.GUICrucible::new);
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
        sink.register(ModMenus.MACHINE_ARC_WELDER.get(), com.hbm.inventory.gui.GUIMachineArcWelder::new);
        sink.register(com.hbm.assemblyfactory.AssemblyFactoryRegistrations.MACHINE_ASSEMBLY_FACTORY_MENU.get(),
                com.hbm.inventory.gui.GUIMachineAssemblyFactory::new);
        com.hbm.client.chemicalfactory.ChemicalFactoryClientRegistrations.registerScreen(sink);
        com.hbm.smoke.SmokeChainClientRegistrations.registerScreens(sink);
        com.hbm.foundry.FoundryExpansionScreens.register(sink);
        com.hbm.client.BombClientRegistrations.registerScreens(sink);
        com.hbm.client.Bombs2ClientRegistrations.registerScreens(sink);
        com.hbm.client.CustomMissileClientRegistrations.registerScreens(sink);
        com.hbm.client.RadarUtilityClientRegistrations.registerScreens(sink);
        com.hbm.client.SatelliteClientRegistrations.registerScreens(sink);
        com.hbm.client.DecoCrateClientRegistrations.registerScreens(sink);
        com.hbm.client.GapsBClientRegistrations.registerScreens(sink); // G-B: held crates, wrench readout
        com.hbm.client.IcfClientRegistrations.registerScreens(sink);
        com.hbm.client.DungeonClientRegistrations.registerScreens(sink);
        com.hbm.client.render.RtgClientRegistrations.registerScreen(sink);
        com.hbm.radio.client.RadioClientRegistrations.registerScreens(sink);
        com.hbm.client.FluidNetClientRegistrations.registerScreens(sink);
        com.hbm.client.ConveyorClientRegistrations.registerScreens(sink);
        com.hbm.client.EnergyNetClientRegistrations.registerScreens(sink);
        com.hbm.client.PneumaticClientRegistrations.registerScreens(sink);
        com.hbm.client.DroneClientRegistrations.registerScreens(sink);
        com.hbm.client.ProductionClientRegistrations.registerScreens(sink);
        com.hbm.client.Production2ClientRegistrations.registerScreens(sink);
        com.hbm.client.SecurityClientRegistrations.registerScreens(sink);
        com.hbm.client.NuclearMiscClientRegistrations.registerScreens(sink);
        com.hbm.client.PwrClientRegistrations.registerScreens(sink);
        com.hbm.client.PowerGenClientRegistrations.registerScreens(sink);
        com.hbm.client.ExplosivesClientRegistrations.registerScreens(sink);
        com.hbm.client.MeleeKitClientRegistrations.registerScreens(sink);
        com.hbm.client.UtilityToolClientRegistrations.registerScreens(sink);
        com.hbm.client.CustomMachineClientRegistrations.registerScreens(sink);
        sink.register(com.hbm.blocks.machine.DiFurnaceContent.DI_FURNACE_MENU.get(),
                com.hbm.inventory.gui.GUIDiFurnace::new);
        sink.register(ModMenus.MASS_STORAGE.get(), com.hbm.inventory.gui.GUIMassStorage::new);
        sink.register(ModMenus.CRATE_TEMPLATE.get(), com.hbm.inventory.gui.GUICrateTemplate::new);
        RBMKClientScreens.register(sink);
        RBMKGauge.OPEN_SCREEN = gauge -> net.minecraft.client.Minecraft.getInstance().setScreen(new com.hbm.inventory.gui.ScreenRBMKGauge(gauge));
        com.hbm.blocks.machine.rbmk.RBMKTerminal.OPEN_SCREEN = terminal ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.hbm.inventory.gui.ScreenRBMKTerminal(terminal));
    }

    public static void registerHud(HudSink sink) {
        com.hbm.items.weapon.sedna.factory.GunFactoryClient.init();
        // final armor B: goggles blur first, as Forge drew helmet overlays beneath the rest of the HUD
        ArmorSetBClient.registerHud(sink);
        sink.register("look_overlay", HbmLookOverlay::render);
        sink.register("boss_status", BossStatus::render); // 1.7 BossStatus: render-pushed boss bar
        sink.register("rad_counter", RadCounterOverlay::render);
        com.hbm.client.EnergyNetClientRegistrations.registerHud(sink);
        sink.register("nuke_flash", NukeScreenEffects::render);
        sink.register("gun_hud", GunHudOverlay::render);
        sink.register("gun_scope", GunScopeOverlay::render);
        sink.register("armor_hud", ArmorHudOverlay::render);
        ArmorSetClient.registerHud(sink); // final armor sets A: dash bar, HEV health/armour replacement
        sink.register("jetpack_hud", com.hbm.client.jetpack.JetpackHudOverlay::render);
        AbilityToolClientRegistrations.registerHud(sink);
        com.hbm.client.RenderInputClientRegistrations.registerHud(sink); // G-C: armour-mod shield bar
        if (System.getenv("HBM_JETPACK_SCREENSHOTS") != null && dev.golem.core.platform.Platform.isDevelopment())
            sink.register("jetpack_visual_check", com.hbm.client.jetpack.JetpackSmokeClient::render);
        if (com.hbm.client.gun.GunSmokeClient.enabled() && dev.golem.core.platform.Platform.isDevelopment())
            sink.register("gun_smoke_capture", com.hbm.client.gun.GunSmokeClient::render);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink blockEntities,
            HbmEntityRenderers.Sink downstream) {
        HbmBlockEntityRenderers.registerAll(blockEntities);
        RailClientRegistrations.registerRenderers(blockEntities);
        java.util.Set<net.minecraft.world.entity.EntityType<?>> bound = new java.util.HashSet<>();
        HbmEntityRenderers.Sink entities = HbmEntityRenderers.recording(downstream, bound);
        HbmEntityRenderers.registerAll(entities);
        com.hbm.radio.client.RadioClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.FluidNetClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.ConveyorClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.EnergyNetClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.PneumaticClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.ProductionClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.Production2ClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.DoorClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.SecurityClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.NuclearMiscClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.PowerGenClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.ExplosivesClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.DecoClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.BombClientRegistrations.registerBlockEntityRenderers(blockEntities);
        com.hbm.client.Bombs2ClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.CustomMissileClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.RadarUtilityClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.SatelliteClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.CustomMachineClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.DecoCrateClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.GapsBClientRegistrations.registerRenderers(blockEntities); // G-B: pipe anchor + pipelines
        HbmBlockEntityRenderers.regGem(blockEntities, com.hbm.tileentity.ModArmorSetBBlockEntities.STATUE_ELB_F.get(),
                com.hbm.client.render.RenderDecoBlockAlt::new); // final armor B: statue_elb_f
        com.hbm.client.IcfClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.EnergyBedrockClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.DungeonClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.StructureWandClientRegistrations.registerRenderers(blockEntities);
        com.hbm.client.BombClientRegistrations.registerEntityRenderers(entities);
        com.hbm.client.SatelliteClientRegistrations.registerEntityRenderers(entities);
        com.hbm.client.render.RtgClientRegistrations.registerRenderer(blockEntities);
        HbmEntityRenderers.verifyCoverage(bound);

        // Every instanced visual is optional acceleration beside an already-registered vanilla BER.
        if (com.hbm.client.render.flywheel.FlywheelCompat.isSupportedOnThisVersion()) {
            com.hbm.client.render.flywheel.FlywheelCompat.registerVisuals();
        }

    }

    /**
     * The instancing engine's own count, on the F3 screen.
     *
     * <p>An instanced draw and a vanilla draw are pixel-identical when both are right, so the only way
     * to tell an engine that is working from one that silently received nothing is to count what it
     * holds. Models against models drawn is that count; draws against parts is the claim instancing
     * actually makes, which is that many copies of a machine cost one draw rather than many.
     */
    public static void installDebugLines() {
        dev.golem.core.client.DebugLines.register(com.hbm.Constants.MOD_ID, "gemrender", out -> {
            dev.golem.core.render.Instancing.Report report = dev.golem.core.render.Instancing.report();
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
        dev.golem.core.client.Tints.install(com.hbm.Constants.MOD_ID);
        dev.golem.core.client.BlockLayers.install(com.hbm.Constants.MOD_ID);
        com.hbm.client.EnergyBedrockClientRegistrations.installSprites(); // computed bedrock ore grade art
        dev.golem.core.fluid.TokenFluids.installClient(); // how other mods draw HBM fluids (History section 19ek-fluids)
    }

    /**
     * {@code hbm:model_state}: the sprite an item with typed state draws ({@link com.hbm.items.ModelState}), computed
     * from that state and never saved. It replaced the saved {@code hbm:item_variant} int (History §19es-itemvariant).
     */
    public static void installItemModelProperties() {
        com.hbm.client.UtilityToolClientRegistrations.installItemModelProperties();
        dev.golem.core.client.ItemModelProperties.register(com.hbm.Constants.MOD_ID, "model_state",
                com.hbm.items.ModelState::of);
    }

    public static void registerBlockColors() {
        HbmBlockColors.registerAll();
        com.hbm.client.ConveyorClientRegistrations.registerBlockColors();
        com.hbm.client.TerrainClientRegistrations.registerBlockColors();
    }

    /**
     * Attaches the worn-armour layer to every humanoid renderer, and declares the suits' part trees.
     *
     * <p>Golem Core owns the attaching: the hook is a mod-bus event on NeoForge/Forge and a per-renderer
     * callback on Fabric, and the callback's class was renamed at 26.x. Read the layer count it reports
     * (see {@code HumanoidLayers.attachedCount}) — a layer attached to nothing draws nothing and throws
     * nothing.
     */
    public static void installArmorLayers() {
        com.hbm.client.render.armor.ArmorRenderers.init();
        installJetpackBridge();
        dev.golem.core.render.HeatDistortionRenderType.configure(
                "hbm:pipeline/heat_distortion", "hbm:heat_distortion");
        // 1.12 HbmShaderManager2 bloom/distortion layers, composited after the hand (first-person crucible blade)
        dev.golem.core.render.ScreenEffects.init();
        // old-hbm crucible/sword cutters: first-person Collada scene, slicing input, cut mobs
        com.hbm.client.crucible.CrucibleClient.register();
        com.hbm.client.jetpack.JetpackClientVisuals.init();
         com.hbm.client.jetpack.JetpackSmokeClient.register();
         com.hbm.client.render.entity.EntityRenderSmokeClient.register();
         com.hbm.client.gun.GunSmokeClient.register();
         com.hbm.client.render.TorexBenchmarkClient.register();
        dev.golem.core.client.ClientResourceReloads.register(com.hbm.Constants.MOD_ID, "jetpack_visuals",
                () -> {
                    com.hbm.client.render.armor.ArmorRenderers.reload();
                    com.hbm.client.jetpack.JetpackClientVisuals.reload();
                });
        dev.golem.core.client.PlayerRenderHooks.register(
                com.hbm.client.jetpack.JetpackClientVisuals::applyPlayerPose);
        dev.golem.core.client.PlayerRenderHooks.registerModel(parts -> {
            // The source applies gun aimedBow during model setup; keep its arm pose after the glider's
            // flight freeze while retaining the glider's body, head and leg precedence.
            com.hbm.client.jetpack.JetpackClientVisuals.applyPlayerModelPose(parts);
            com.hbm.client.render.weapon.GunPlayerPose.apply(parts);
        });
        dev.golem.core.client.CameraHooks.register(
                com.hbm.client.jetpack.JetpackClientVisuals::lowerThirdPersonCamera);
        dev.golem.core.render.HumanoidLayers.register(
                com.hbm.client.render.armor.ArmorRenderLayer::new);
        dev.golem.core.render.HumanoidLayers.register(
                com.hbm.client.jetpack.JetpackRenderLayer::new);
        dev.golem.core.render.HumanoidLayers.register(com.hbm.client.render.armor.ArmorModRenderLayer::new);
        dev.golem.core.render.HumanoidLayers.register(com.hbm.client.render.armor.WornModelRenderLayer::new); // final armor B: goggles, glasses, hat, no9
        // 1.7 RenderAccessoryUtility: special players' wings, axe pack and tail, and the HBM capes
        dev.golem.core.render.HumanoidLayers.register(com.hbm.client.render.util.SpecialPlayerRenderLayer::new);
        dev.golem.core.client.Capes.register(com.hbm.client.render.util.RenderAccessoryUtility::getCloakFromPlayer);
        SpecialPlayerHarnessQueries.register();
        com.hbm.client.render.RenderBobble.preloadSkins(); // old-hbm ClientHttpHandler.preinit: skinned bobbleheads
        ArmorModHarnessQueries.register();
        StructureHarnessQueries.register(); // harness query hbm_structure: NBT structure starts, placement score, loot state
        WorldFeatureHarnessQueries.register(); // harness query hbm_worldgen: block counts/finds, biome, meteor scheduler
        SmallHarnessQueries.register(); // hbm_small: boss bar, rad fog, impact spawn refusals, sulfur columns (§19ek-small)
        ComponentHarnessQueries.register(); // harness query hbm_wgc: features/bunker/meteor_dungeon starts, hardcoded piece builds (WG-C)
        LegacyDungeonHarnessQueries.register(); // harness query hbm_wgd: legacy dungeon starts, placement score, loot/spawners (WG-D)
        GapsAHarnessQueries.register(); // harness query hbm_gapsa: held stack/attack damage/cells, hurt jolt, amat flashes (G-A)
        GapsBHarnessQueries.register(); // harness query hbm_gapsb: boss spawns, reputation, machine damage, structure tiles, radar (G-B)
        ForeignFluidHarnessQueries.register(); // harness query hbm_fluidcaps: HBM fluids through the loader fluid capability (19ek-fluids)
    }

    /** Connects the client-owned visuals to the common JetpackHandler without making it import client APIs. */
    private static void installJetpackBridge() {
        com.hbm.handler.JetpackHandler.setClientExhaustCallback((level, payload) ->
                com.hbm.client.jetpack.JetpackClientVisuals.spawnExhaustCue(level, payload.playerId(), payload.mode()));
        com.hbm.client.jetpack.JetpackClientBridge.install(player -> {
            com.hbm.handler.JetpackHandler.JetpackInfo info = com.hbm.handler.JetpackHandler.getState(player);
            if (info == null) return null;
            return new com.hbm.client.jetpack.JetpackClientBridge.State() {
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
                    return com.hbm.handler.JetpackHandler.isJetpackActive(player);
                }
            };
        }, stack -> {
            if (stack.isEmpty()) return null;
            com.hbm.inventory.fluid.FluidType type = com.hbm.handler.JetpackHandler.getFuelType(stack);
            return new com.hbm.client.jetpack.JetpackClientBridge.Fuel(type == null ? "" : type.getName(),
                    com.hbm.handler.JetpackHandler.getFuel(stack),
                    com.hbm.handler.JetpackHandler.getMaxFuel(stack));
        });
    }

    /** Registers HBM's key mappings and starts the per-tick input poll that drives guns. */
    public static void installKeybinds() {
        HbmKeybindsClient.register();
        ArmorSetClient.registerKeys(); // final armor sets A: bismuth dash key
        QMAWClient.register();
        CanneryClient.register(); // 1.7 WIAJ presentations: Left Shift + QMAW key on a hovered item
        ArmorModTooltips.register();
        // 1.7 FluidType.addInfo polls physical left Shift for the hidden trait lines (FT_Polluting et al.).
        com.hbm.inventory.fluid.FluidType.setShiftHeldPoll(() -> dev.golem.core.client.KeyMappings.isHeld(
                net.minecraft.client.Minecraft.getInstance().options.keyShift));
        com.hbm.client.anim.GunRecoil.register();
        GrenadeUniversalClient.register(); // 1.7 ItemGrenadeUniversal.onUpdate's client half (arm swing)
        CraneKeybindHandler.register();
        com.hbm.client.UtilityToolClientRegistrations.installKeybinds();
        AbilityToolClientRegistrations.registerKeys();
        com.hbm.client.RenderInputClientRegistrations.installKeybinds(); // G-C: magnet toggle key
    }

    /**
     * Client halves of the ported ntm-next entities: the looped-sound factory behind
     * {@link com.hbm.sound.AudioSystem} (UFO, hunter chopper, worm), the entity particle handlers, and the
     * conveyor wand's world-frame ghost preview.
     */
    public static void installEntityEffects() {
        com.hbm.sound.AudioSystem.installClientFactory(com.hbm.client.sound.ClientAudioFactory.INSTANCE);
        com.hbm.client.ClientEffects.install();
        // 1.7 RenderOverhead.renderActionPreview: the conveyor wand's ghost run, drawn at world-frame end.
        com.hbm.client.render.ConveyorPreviewRenderer.init();
        // 1.7 BlockDummyable.drawPlacementHighlight: the held multiblock's footprint outline, green/red.
        com.hbm.client.render.MultiblockPlacementHighlight.init();
        // 1.7 RenderNTMSkyboxImpact / RenderNTMSkyboxChainloader and the tick that installs them (1.31_enableSkyboxes).
        com.hbm.client.render.world.NTMSkyboxes.register();
        com.hbm.client.SmogFog.register(); // 1.7 ModEventHandlerRenderer smog (POL_03/06/07, History §19ek-hooks)
        com.hbm.commands.HbmClientCommands.register(); // 1.7 client commands: /ntmclient, /ntmwikirender, /dumpthreadsandcrashgame
        com.hbm.client.RenderInputClientRegistrations.install(); // G-C: overhead markers, HUD, world text, particles
    }

    /** Declares which animation each gun plays for each cue. Items resolve lazily, at render time. */
    public static void installGunAnimations() {
        GunEffectState.install();
        com.hbm.client.render.projectile.TracerRegistry.init();
        com.hbm.client.particle.CasingParticles.init();
        com.hbm.client.particle.DisintegrationParticles.init();
        com.hbm.client.particle.DebrisParticles.init();
        GunEffectState.setCasingHandler(event -> com.hbm.client.particle.CasingParticles.spawn(event));
        GunEffectState.setGunCasingHandler(payload -> com.hbm.client.particle.CasingParticles.spawn(
                new com.hbm.particle.helper.CasingCreator.Event(payload.x(), payload.y(), payload.z(), payload.mX(),
                        payload.mY(), payload.mZ(), payload.yaw(), payload.pitch(), payload.mPitch(), payload.mYaw(),
                        payload.casing(), payload.smoking(), payload.smokeLife(), payload.smokeLift(), payload.nodeLife(),
                        true, payload.legacy())));
        com.hbm.client.anim.GunAnimations.init();
        com.hbm.client.render.weapon.DrillHighlightClient.init();
        dev.golem.core.client.ClientResourceReloads.register(com.hbm.Constants.MOD_ID,
                "lag_animations", com.hbm.client.anim.LagAnimationLoader::invalidate);
        // The weapon renderers a gun item definition can name. Both eras read the same definitions:
        // 26.1 through its item-model system, older versions through GunLegacyModels' own parse.
        com.hbm.client.render.weapon.WeaponRenderers.init();
        dev.golem.core.client.Fov.register(com.hbm.client.render.weapon.WeaponRenderers::viewFov);
        dev.golem.core.client.Crosshairs.register(GunCrosshairOverlay::render);
    }

}
