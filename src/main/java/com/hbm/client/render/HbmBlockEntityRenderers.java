package com.hbm.client.render;

import dev.golem.core.render.CompatBERProvider;
import dev.golem.core.render.CompatRenderState;
import com.hbm.Constants;
import com.hbm.client.render.MachineObjRenderer.ModelDef;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.ModRBMKBlockEntities;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * Cross-loader registration of HBM's BlockEntityRenderers. Common defines the wiring (which BE type
 * gets which OBJ model + texture); each loader passes a {@link Sink} that performs the actual
 * registration (NeoForge via {@code EntityRenderersEvent.RegisterRenderers}, Fabric via the vanilla
 * {@code BlockEntityRenderers.register}). Client-only — never loaded on a dedicated server.
 *
 * <p>All four multiblock machines share base yaw 90° + the default facing map (NORTH→0, WEST→90,
 * SOUTH→180, EAST→270), matching their 1.12.2 TESRs.
 */
public final class HbmBlockEntityRenderers {

    private HbmBlockEntityRenderers() {}

    /** Loader-provided registration callback. */
    public interface Sink {
        <T extends BlockEntity, S extends CompatRenderState> void register(
                BlockEntityType<? extends T> type, CompatBERProvider<T, S> provider);
    }

    /** The 1.12.2 "frame" appears only when a block sits this many blocks above the core. */
    private static final int FRAME_HEIGHT = 3;
    private static final Set<String> FRAME = Set.of("Frame");

    public static void registerAll(Sink sink) {
        // RBMK columns render their dynamic fuel bundle separately from the baked column shell.
        regCustom(sink, ModRBMKBlockEntities.RBMK_ROD.get(), ctx -> new RBMKFuelRodRenderer<>(false));
        regCustom(sink, ModRBMKBlockEntities.RBMK_ROD_REASIM.get(), ctx -> new RBMKFuelRodRenderer<>(true));
        regCustom(sink, ModRBMKBlockEntities.RBMK_CONTROL.get(), ctx -> new RBMKControlRodRenderer<>("rbmk_control"));
        regCustom(sink, ModRBMKBlockEntities.RBMK_CONTROL_AUTO.get(), ctx -> new RBMKControlRodRenderer<>("rbmk_control_auto"));
        regCustom(sink, ModRBMKBlockEntities.RBMK_BOILER.get(), ctx -> new RBMKColumnShellRenderer<>("rbmk_boiler", RBMKColumnShellRenderer.Kind.BOILER));
        regCustom(sink, ModRBMKBlockEntities.RBMK_MODERATOR.get(), ctx -> new RBMKColumnShellRenderer<>("rbmk_moderator", RBMKColumnShellRenderer.Kind.PLAIN));
        regCustom(sink, ModRBMKBlockEntities.RBMK_REFLECTOR.get(), ctx -> new RBMKColumnShellRenderer<>("rbmk_reflector", RBMKColumnShellRenderer.Kind.PLAIN));
        regCustom(sink, ModRBMKBlockEntities.RBMK_ABSORBER.get(), ctx -> new RBMKColumnShellRenderer<>("rbmk_absorber", RBMKColumnShellRenderer.Kind.PLAIN));
        regCustom(sink, ModRBMKBlockEntities.RBMK_BLANK.get(), ctx -> new RBMKColumnShellRenderer<>("rbmk_blank", RBMKColumnShellRenderer.Kind.PLAIN));
        regCustom(sink, ModRBMKBlockEntities.RBMK_COOLER.get(), ctx -> new RBMKColumnShellRenderer<>("rbmk_cooler", RBMKColumnShellRenderer.Kind.PLAIN));
        regCustom(sink, ModRBMKBlockEntities.RBMK_STORAGE.get(), ctx -> new RBMKColumnShellRenderer<>("rbmk_storage", RBMKColumnShellRenderer.Kind.PLAIN));
        regCustom(sink, ModRBMKBlockEntities.RBMK_HEATER.get(), ctx -> new RBMKColumnShellRenderer<>("rbmk_heater", RBMKColumnShellRenderer.Kind.BOILER));
        regCustom(sink, ModRBMKBlockEntities.RBMK_OUTGASSER.get(), ctx -> new RBMKColumnShellRenderer<>("rbmk_outgasser", RBMKColumnShellRenderer.Kind.PLAIN));
        regCustom(sink, ModRBMKBlockEntities.RBMK_CONSOLE.get(), RBMKConsoleRenderer::new);
        regCustom(sink, ModRBMKBlockEntities.RBMK_GAUGE.get(), RBMKGaugeRenderer::new);
        regCustom(sink, ModRBMKBlockEntities.RBMK_TERMINAL.get(), RBMKTerminalRenderer::new);
        regCustom(sink, ModBlockEntities.CARGO_ELEVATOR.get(), CargoElevatorRenderer::new);
        regCustom(sink, ModBlockEntities.BARREL.get(), BarrelRenderer::new);
        regCustom(sink, ModRBMKBlockEntities.RBMK_CRANE_CONSOLE.get(), ctx -> new RBMKCraneConsoleRenderer());
        regCustom(sink, ModRBMKBlockEntities.RBMK_DEBRIS.get(), ctx -> new RBMKDebrisBlockRenderer());
         // The assembly machine has its own animated renderer (spinning ring + two robotic arms).
         regCustom(sink, ModBlockEntities.MACHINE_ASSEMBLY_MACHINE.get(), ctx -> new AssemblyMachineRenderer());
         // Precision assembler: the legacy four-striker variant of the same assembly model.
         regCustom(sink, ModBlockEntities.MACHINE_PREC_ASS.get(), ctx -> new PrecisionAssemblerRenderer());
        // The chemical plant has its own animated renderer too: a sliding Slider + rotating Spinner + the
        // Fluid mixer overlay (its own texture, tinted by the recipe fluid, only while processing).
          regCustom(sink, ModBlockEntities.MACHINE_CHEMICAL_PLANT.get(), ctx -> new ChemicalPlantRenderer());
          regCustom(sink, ModBlockEntities.MACHINE_CONDENSER_POWERED.get(), ctx -> new CondenserRenderer());
          reg(sink, ModBlockEntities.MACHINE_STORAGE_DRUM.get(), 0F, 1F,
                  new ModelDef(obj("machines/drum.obj"), tex("machines/drum_gray.png")));
         regCustom(sink, ModBlockEntities.MACHINE_MIXER.get(), ctx -> new MixerRenderer());
         // Mini and polonium RTGs share the legacy body; the renderer adds the four optional connectors.
         regCustom(sink, ModBlockEntities.MACHINE_MINI_RTG.get(), ctx -> new RTGRenderer());
         // RadGen is entirely custom-rendered, including its glass shell and running indicator.
         regCustom(sink, ModBlockEntities.MACHINE_RAD_GEN.get(), ctx -> new RadGenRenderer());
          regCustom(sink, ModBlockEntities.FUSION_PLASMA_FORGE.get(), ctx -> new PlasmaForgeRenderer());
         regCustom(sink, ModBlockEntities.MACHINE_EXPOSURE_CHAMBER.get(), ctx -> new ExposureChamberRenderer());
        // Blast furnace: static body, with the legacy yaw map (base 90° + the standard facing map).
        regCustom(sink, ModBlockEntities.MACHINE_BLAST_FURNACE.get(), ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityMachineBlastFurnace>(90F, 1F,
                new ModelDef(obj("machines/blast_furnace.obj"), tex("machines/blast_furnace.png"))).doubleSided());
        regCustom(sink, ModBlockEntities.MACHINE_INDUSTRIAL_TURBINE.get(), ctx -> new IndustrialTurbineRenderer());
        regCustom(sink, ModBlockEntities.MACHINE_CHUNGUS.get(), ctx -> new ChungusRenderer());
         regCustom(sink, ModBlockEntities.MACHINE_LARGE_TURBINE.get(), ctx -> new LargeTurbineRenderer());
         regCustom(sink, ModBlockEntities.MACHINE_TURBINE_GAS.get(), ctx -> new GasTurbineRenderer());
         regCustom(sink, ModBlockEntities.MACHINE_TURBOFAN.get(), ctx -> new TurbofanRenderer());
         regCustom(sink, ModBlockEntities.MACHINE_STEAM_ENGINE.get(), ctx -> new SteamEngineRenderer());
         regCustom(sink, ModBlockEntities.MACHINE_PUMP_ELECTRIC.get(), ctx -> new PumpRenderer<com.hbm.tileentity.machine.TileEntityMachinePumpElectric>(
                 tex("machines/pump_electric.png")));
         regCustom(sink, ModBlockEntities.MACHINE_PUMP_STEAM.get(), ctx -> new PumpRenderer<com.hbm.tileentity.machine.TileEntityMachinePumpSteam>(
                 tex("machines/pump_steam.png")));
         regCustom(sink, ModBlockEntities.MACHINE_STIRLING.get(), ctx -> new StirlingRenderer());
         regCustom(sink, ModBlockEntities.MACHINE_PUREX.get(), ctx -> new PUREXRenderer());
        // Presses: a static body + a head that descends with the machine's progress.
        regCustom(sink, ModBlockEntities.MACHINE_EPRESS.get(), ctx -> new PressRenderer<>(
                obj("epress_body.obj"), tex("machines/epress_body.png"),
                obj("epress_head.obj"), tex("machines/epress_head.png"),
                90F, true, 1.875F, 1.0F, 200, te -> te.progress));
         regCustom(sink, ModBlockEntities.MACHINE_PRESS.get(), ctx -> new PressRenderer<>(
                 obj("press_body.obj"), tex("machines/press_body.png"),
                 obj("press_head.obj"), tex("machines/press_head.png"),
                  180F, false, 0.875F, 0.95F, 200, te -> te.progress));
         regCustom(sink, ModBlockEntities.MACHINE_AMMO_PRESS.get(), ctx -> new AmmoPressRenderer());
         regCustom(sink, ModBlockEntities.MACHINE_SAWMILL.get(), ctx -> new SawmillRenderer());
        // The launch pad draws the missile it is holding on top of the silo, so it needs its own
        // renderer rather than the generic static-OBJ one.
        regCustom(sink, ModBlockEntities.LAUNCH_PAD.get(), ctx -> new LaunchPadRenderer());

        // Soldering station: its own renderer — the static OBJ (with the asymmetric-footprint offset) plus
        // the current recipe's output item floating above (the 1.12.2 product display).
        regCustom(sink, ModBlockEntities.MACHINE_SOLDERING_STATION.get(), ctx -> new SolderingStationRenderer());

        // Ore acidizer: static Body + rotating Spinner + the tinted acid overlay while processing.
         regCustom(sink, ModBlockEntities.MACHINE_CRYSTALLIZER.get(), ctx -> new CrystallizerRenderer());
         // Standard centrifuge: static tower, with the legacy N/S/W/E core yaw map.
           reg(sink, ModBlockEntities.MACHINE_CENTRIFUGE.get(), 0F, 1F,
                   new ModelDef(obj("machines/centrifuge.obj"), tex("machines/centrifuge_new.png")));
           regCustom(sink, ModBlockEntities.MACHINE_CYCLOTRON.get(), ctx -> new CyclotronRenderer());
           // Gas centrifuge: old yaw map N270/S90/W0/E180 = built-in facing map + 270 degrees.
          reg(sink, ModBlockEntities.MACHINE_GAS_CENT.get(), 270F, 1F,
                   new ModelDef(obj("machines/centrifuge_gas.obj"), tex("machines/centrifuge_gas.png")));
           reg(sink, ModBlockEntities.MACHINE_ELECTROLYSER.get(), 180F, 1F,
                   new ModelDef(obj("machines/electrolyser.obj"), tex("machines/electrolyser.png")));
         regCustom(sink, ModBlockEntities.MACHINE_MINING_LASER.get(), ctx -> new MiningLaserRenderer());

        // Watz: upstream had no TESR for it at all — it was a StaticTesrBakedModels entry whose yaw map
        // is all zeros, i.e. the model does not turn with the machine's facing.
        regCustom(sink, ModBlockEntities.WATZ.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityWatz>(0F, 1F,
                        new ModelDef(obj("reactors/watz.obj"), tex("machines/watz.png"))).ignoreFacing());
        regCustom(sink, ModBlockEntities.WATZ_PUMP.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityWatzPump>(0F, 1F,
                        new ModelDef(obj("machines/watz_pump.obj"), tex("machines/watz_pump.png"))).ignoreFacing());
        // The struct seed draws a translucent ghost of whatever its pattern still needs.
        regCustom(sink, ModBlockEntities.STRUCT_WATZ_CORE.get(),
                ctx -> new StructPreviewRenderer<com.hbm.tileentity.machine.struct.TileEntityWatzStruct>());

        // Fusion torus: spinning magnet ring, per-arm bolt covers and the tinted plasma glow.
        regCustom(sink, ModBlockEntities.FUSION_TORUS.get(), ctx -> new FusionTorusRenderer());

        // The rest of the fusion family. Base yaw 90 for all but the coupler, whose static yaw map
        // upstream is a flat 180 offset instead.
        reg(sink, ModBlockEntities.FUSION_COUPLER.get(), 180F, 1F,
                new ModelDef(obj("fusion/coupler.obj"), tex("fusion/coupler.png")));
        reg(sink, ModBlockEntities.FUSION_COLLECTOR.get(), 90F, 1F,
                new ModelDef(obj("fusion/collector.obj"), tex("fusion/collector.png")));
        reg(sink, ModBlockEntities.FUSION_BOILER.get(), 90F, 1F,
                new ModelDef(obj("fusion/boiler.obj"), tex("fusion/boiler.png")));
        reg(sink, ModBlockEntities.FUSION_BREEDER.get(), 90F, 1F,
                new ModelDef(obj("fusion/breeder.obj"), tex("fusion/breeder.png")));

        // The klystron's model is authored one block off its core, and its fan spins about the machine
        // axis at a pivot 2.5 up. The "Pipes" group is deliberately not drawn, as upstream does not.
        regCustom(sink, ModBlockEntities.FUSION_KLYSTRON.get(),
                ctx -> new FusionSpinRenderer<com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron>(
                        "fusion/klystron.obj", "fusion/klystron.png", "Klystron", "Rotor", 2.5F, -1F, 90F,
                        (te, partial) -> te.prevFan + (te.fan - te.prevFan) * partial));
        regCustom(sink, ModBlockEntities.FUSION_KLYSTRON_CREATIVE.get(),
                ctx -> new FusionSpinRenderer<com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative>(
                        "fusion/klystron.obj", "fusion/klystron_creative.png", "Klystron", "Rotor", 2.5F, -1F, 90F,
                        (te, partial) -> te.prevFan + (te.fan - te.prevFan) * partial));
        // The turbine's coils rock at a pivot 1.5 up, wrapped to 15 degrees as upstream does - they are
        // a partial arc, and a full rotation swings them clear of the housing.
        regCustom(sink, ModBlockEntities.FUSION_MHDT.get(),
                ctx -> new FusionSpinRenderer<com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT>(
                        "fusion/mhdt.obj", "fusion/mhdt.png", "Turbine", "Coils", 1.5F, 0F, 90F, 15F,
                        (te, partial) -> te.prevRotor + (te.rotor - te.prevRotor) * partial));
        regCustom(sink, ModBlockEntities.STRUCT_TORUS_CORE.get(),
                ctx -> new StructPreviewRenderer<com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct>());
        // SILEX: fully static upstream (it had no TESR at all — it was a StaticTesrBakedModels entry).
        // Old yaw map N90/S270/W180/E0 = base yaw 90 + the standard map, same as the compressor's.
        reg(sink, ModBlockEntities.MACHINE_SILEX.get(), 90F, 1F,
                new ModelDef(obj("machines/silex.obj"), tex("machines/silex.png")));
        // FEL: its own renderer — the static emitter plus the BeamPronter beam out to the hit block.
        regCustom(sink, ModBlockEntities.MACHINE_FEL.get(), ctx -> new FELRenderer());
        // Oil derrick: static OBJ; old yaw map (N180/S0/W270/E90) = base yaw 180 + the standard map.
        reg(sink, ModBlockEntities.MACHINE_OIL_WELL.get(), 180F, 1F,
                new ModelDef(obj("machines/derrick.obj"), tex("machines/derrick.png")));
        // Pumpjack: animated rotor/head/carriage + the untextured crank/rope geometry.
        regCustom(sink, ModBlockEntities.MACHINE_PUMPJACK.get(), ctx -> new PumpjackRenderer());
        // Diesel generator: single INVISIBLE facing block, OBJ TESR (Generator + jittering Engine).
        regCustom(sink, ModBlockEntities.MACHINE_DIESEL.get(), ctx -> new DieselGenRenderer());
        // Flare stack: static OBJ, old yaw map is a constant 180 for all facings.
        regCustom(sink, ModBlockEntities.MACHINE_GAS_FLARE.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare>(180F, 1F,
                        new ModelDef(obj("machines/flare_stack.obj"), tex("machines/flare_stack.png"))).ignoreFacing());
        // Fraction tower + spacer: static OBJs, facing-independent (all-zero yaw map).
        regCustom(sink, ModBlockEntities.MACHINE_FRACTION_TOWER.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower>(0F, 1F,
                        new ModelDef(obj("machines/fraction_tower.obj"), tex("machines/fraction_tower.png"))).ignoreFacing());
        regCustom(sink, ModBlockEntities.FRACTION_SPACER.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntitySpacer>(0F, 1F,
                        new ModelDef(obj("machines/fraction_spacer.obj"), tex("machines/fraction_spacer.png"))).ignoreFacing());
        // Liquefactor + solidifier: Main body + Y-scaled tinted Fluid + translucent Glass.
        regCustom(sink, ModBlockEntities.MACHINE_LIQUEFACTOR.get(),
                ctx -> new TankGlassRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor>("liquefactor", "liquefactor", te -> te.tank));
        regCustom(sink, ModBlockEntities.MACHINE_SOLIDIFIER.get(),
                ctx -> new TankGlassRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier>("solidifier", "solidifier", te -> te.tank));
        // Refinery: static OBJ; the old yaw map is a constant 180 for all facings (facing-independent).
        regCustom(sink, ModBlockEntities.MACHINE_REFINERY.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineRefinery>(180F, 1F,
                        new ModelDef(obj("machines/refinery.obj"), tex("machines/refinery.png"))).ignoreFacing());
        // Advanced oil machines — static OBJ renders (facing-dependent animations deferred, §20).
        regCustom(sink, ModBlockEntities.MACHINE_FRACKING_TOWER.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower>(180F, 1F,
                        new ModelDef(obj("machines/fracking_tower.obj"), tex("machines/fracking_tower.png"))).ignoreFacing());
        regCustom(sink, ModBlockEntities.MACHINE_VACUUM_DISTILL.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill>(0F, 1F,
                        new ModelDef(obj("machines/vacuum_distill.obj"), tex("machines/vacuum_distill.png"))).ignoreFacing());
        // Reformer: old RenderCatalyticReformer yaw map N90/W180/S270/E0 = the built-in map + 90.
        regCustom(sink, ModBlockEntities.MACHINE_CATALYTIC_REFORMER.get(), ctx -> new CatalyticReformerRenderer());
        // Cracker: old StaticTesrBakedModels yaw map meta12(N)=90/13(S)=270/14(W)=180/15(E)=0 = built-in + 90.
        regCustom(sink, ModBlockEntities.MACHINE_CATALYTIC_CRACKER.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker>(90F, 1F,
                        new ModelDef(obj("machines/cracking_tower.obj"), tex("machines/cracking_tower.png"))));
        regCustom(sink, ModBlockEntities.MACHINE_HYDROTREATER.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater>(0F, 1F,
                        new ModelDef(obj("machines/hydrotreater.obj"), tex("machines/hydrotreater.png"))).ignoreFacing());
        regCustom(sink, ModBlockEntities.MACHINE_COKER.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineCoker>(0F, 1F,
                        new ModelDef(obj("machines/coker.obj"), tex("machines/coker.png"))).ignoreFacing());
        regCustom(sink, ModBlockEntities.FURNACE_COMBINATION.get(), ctx -> new CombinationFurnaceRenderer());
        // Pyro oven: old RenderPyroOven yaw map N180/W270/S0/E90 = the built-in map + 180. (The animated
        // "Slider"/"Fan" sub-parts render at their model origin — the Slider's rest transform is a −0.5 X
        // translate, so it sits half a block off until a custom animated renderer lands. §20)
        regCustom(sink, ModBlockEntities.MACHINE_PYROOVEN.get(), ctx -> new PyroOvenRenderer());
        // ZIRNOX: old yaw map meta12(N)=90/13(S)=270/14(W)=180/15(E)=0 = built-in + 90.
        regCustom(sink, ModBlockEntities.MACHINE_ZIRNOX.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityReactorZirnox>(90F, 1F,
                        new ModelDef(obj("zirnox.obj"), tex("machines/zirnox.png"))).doubleSided());
        regCustom(sink, ModBlockEntities.ZIRNOX_DESTROYED.get(),
                ctx -> new ZirnoxDestroyedRenderer());
        // Combustion engine: the old RenderCombustionEngine yaw map is N90/S270/W180/E0 — exactly the
        // generic renderer's built-in map (N0/S180/W90/E270) + 90 — then a post-rotation
        // translate(-0.5, 0, 3) (the model isn't centred on the core cell).
        regCustom(sink, ModBlockEntities.MACHINE_COMBUSTION_ENGINE.get(), ctx -> new CombustionEngineRenderer());
        // Refueler: a single FACING block (not a BlockDummyable), so the generic renderer can't read its
        // facing — own renderer (yaw from FACING + the fill-level fluid part).
        regCustom(sink, ModBlockEntities.REFUELER.get(), ctx -> new RefuelerRenderer());

        // Compressor: own renderer — the Pump part sits at −3 blocks at rest (lift·3−3) and the Fan spins,
        // so a static render would drop the pump through the floor.
        regCustom(sink, ModBlockEntities.MACHINE_COMPRESSOR.get(), ctx -> new CompressorRenderer());

        // Heat boiler: static OBJ + the burst husk + the near-full wobble (own renderer).
         regCustom(sink, ModBlockEntities.HEAT_BOILER.get(), ctx -> new HeatBoilerRenderer());
         regCustom(sink, ModBlockEntities.MACHINE_INDUSTRIAL_BOILER.get(),
                 ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial>(0F, 1F,
                         new ModelDef(obj("machines/industrial_boiler.obj"), tex("machines/industrial_boiler.png"))).ignoreFacing());
        // Oilburner: static OBJ; the old yaw map is all zeros -> facing-independent.
        regCustom(sink, ModBlockEntities.HEATER_OILBURNER.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityHeaterOilburner>(0F, 1F,
                        new ModelDef(obj("machines/oilburner.obj"), tex("machines/oilburner.png"))).ignoreFacing());

         // Bombs: each static OBJ keeps the legacy model-specific yaw map and double-sided geometry.
         regCustom(sink, ModBlockEntities.NUKE_MAN.get(),
                 ctx -> new BombObjRenderer<>(obj("bombs/fatman.obj"), tex("bombs/fatman.png"), 1F));
         regCustom(sink, ModBlockEntities.NUKE_N2.get(),
                 ctx -> new BombObjRenderer<>(obj("bombs/n2.obj"), tex("bombs/n2.png"), 1F,
                         com.hbm.blocks.bomb.NukeBombBlock.FACING, 0F, 0F, 0F, 180F, 0F, 270F, 90F));
         regCustom(sink, ModBlockEntities.NUKE_BOY.get(),
                 ctx -> new BombObjRenderer<>(obj("bombs/lilboy.obj"), tex("bombs/lilboy.png"), 1F,
                         com.hbm.blocks.bomb.NukeBombBlock.FACING, -2F, 0F, 0F, 0F, 180F, 90F, 270F));
         regCustom(sink, ModBlockEntities.NUKE_GADGET.get(),
                 ctx -> new BombObjRenderer<>(obj("bombs/gadget.obj"), tex("bombs/gadget.png"), 1F,
                         com.hbm.blocks.bomb.NukeBombBlock.FACING, 0F, 0F, 0F, 0F, 180F, 90F, 270F));
         regCustom(sink, ModBlockEntities.NUKE_TSAR.get(),
                 ctx -> new BombObjRenderer<>(obj("bombs/tsar.obj"), tex("bombs/tsar.png"), 1F,
                         com.hbm.blocks.bomb.NukeBombBlock.FACING, 0F, 0F, 0F, 0F, 180F, 90F, 270F));

        // Red cable: the real cable_neo.obj, parts chosen from the connection state.
        regCustom(sink, ModBlockEntities.RED_CABLE.get(), ctx -> new CableObjRenderer<>());

        // Fluid duct: the real pipe_neo.obj, parts chosen from the connection state (like the cable).
        regCustom(sink, ModBlockEntities.FLUID_DUCT.get(), ctx -> new FluidDuctObjRenderer());

         // Fluid tank: a BlockDummyable multiblock with its own renderer — the cylinder body is textured
         // per-fluid (tank/tank_<name>.png, NONE -> tank_none) while the frame stays machines/tank.png, so
         // it can't use the single-texture-per-part generic renderer. (Base yaw 180°; see FluidTankRenderer.)
          regCustom(sink, ModBlockEntities.FLUID_TANK.get(), ctx -> new FluidTankRenderer());
          reg(sink, ModBlockEntities.MACHINE_UF6_TANK.get(), 0F, 1F,
                  new ModelDef(obj("tank.obj"), tex("machines/uf6tank.png")));
           reg(sink, ModBlockEntities.MACHINE_PUF6_TANK.get(), 0F, 1F,
                   new ModelDef(obj("tank.obj"), tex("machines/puf6tank.png")));
           regCustom(sink, ModBlockEntities.MACHINE_BIG_ASS_TANK.get(), ctx -> new BigAssTankRenderer());
           regCustom(sink, ModBlockEntities.MACHINE_BAT9000.get(), ctx -> new BAT9000Renderer());
           reg(sink, ModBlockEntities.MACHINE_DEUTERIUM_TOWER.get(), 180F, 1F, 0.5F, -0.5F,
                   new ModelDef(obj("machines/machine_deuterium_tower.obj"),
                           tex("machines/machine_deuterium_tower.png")));
         // Drain: legacy yaw map is the standard facing map + 90 degrees.
         regCustom(sink, ModBlockEntities.MACHINE_DRAIN.get(), ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityMachineDrain>(90F, 1F,
                 new ModelDef(obj("machines/drain.obj"), tex("machines/drain.png"))));

         // Battery socket: draws the cradle + the inserted battery (per-variant texture).
         regCustom(sink, ModBlockEntities.BATTERY_SOCKET.get(), ctx -> new BatterySocketRenderer());
          // FEnSU: colored body + X-axis flywheel, with its lights submitted fullbright.
          regCustom(sink, ModBlockEntities.FENSU.get(), ctx -> new FensuRenderer());
          // Current FEnSU: the separate fensu2 flywheel bank, including additive plasma and zaps.
          regCustom(sink, ModBlockEntities.BATTERY_REDD.get(), ctx -> new BatteryReddRenderer());

        // Crucible: basin OBJ + molten pool surface + the two pour streams.
        regCustom(sink, ModBlockEntities.MACHINE_CRUCIBLE.get(), ctx -> new CrucibleRenderer());

         // Firebox: body + door + the burning/empty interior.
          regCustom(sink, ModBlockEntities.HEATER_FIREBOX.get(), ctx -> new FireboxRenderer<>());
           regCustom(sink, ModBlockEntities.HEATER_OVEN.get(), ctx -> new FireboxRenderer<com.hbm.tileentity.machine.TileEntityHeaterOven>(
                   "models/machines/heating_oven.obj", "textures/models/machines/heating_oven.png", "Inner", true));
          regCustom(sink, ModBlockEntities.FURNACE_IRON.get(), ctx -> new FurnaceIronRenderer());
         regCustom(sink, ModBlockEntities.FURNACE_STEEL.get(), ctx -> new FurnaceSteelRenderer());
         regCustom(sink, ModBlockEntities.MACHINE_WOOD_BURNER.get(),
                 ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityMachineWoodBurner>(180F, 1F,
                         -0.5F, -0.5F, Set.of(), 0,
                         new ModelDef(obj("machines/wood_burner.obj"), tex("machines/wood_burner.png"))));

        // Electric heater: a static OBJ (the old CE fork used a static baked model).
        reg(sink, ModBlockEntities.HEATER_ELECTRIC.get(), 90F, 1F,
                new ModelDef(obj("machines/electric_heater.obj"), tex("machines/electric_heater.png")));
        reg(sink, ModBlockEntities.HEATER_HEATEX.get(), 90F, 1F,
                new ModelDef(obj("machines/heatex.obj"), tex("machines/heater_heatex.png")));

        // Foundry: channel melt level, casters (mold JSON-modelled / basin OBJ), outlet + slagtap OBJs, slag pile.
        regCustom(sink, ModBlockEntities.FOUNDRY_CHANNEL.get(), ctx -> new FoundryChannelRenderer());
        regCustom(sink, ModBlockEntities.FOUNDRY_MOLD.get(), ctx -> new FoundryCastingRenderer<>(null));
        regCustom(sink, ModBlockEntities.FOUNDRY_BASIN.get(), ctx -> new FoundryCastingRenderer<>("models/block/foundry_basin.obj"));
        regCustom(sink, ModBlockEntities.FOUNDRY_OUTLET.get(), ctx -> new FoundryOutletRenderer<>("models/block/foundry_outlet.obj"));
        regCustom(sink, ModBlockEntities.FOUNDRY_SLAGTAP.get(), ctx -> new FoundryOutletRenderer<>("models/block/foundry_slagtap.obj"));
        regCustom(sink, ModBlockEntities.SLAG.get(), ctx -> new SlagRenderer());

        // Decorative OBJ blocks (boxcar / boat / satellites): one renderer picks the model per block.
        regCustom(sink, ModBlockEntities.DECO.get(), ctx -> new DecoObjRenderer());

        // The 12 tiered anvils: one renderer picks the per-anvil OBJ + texture from the block.
        regCustom(sink, ModBlockEntities.ANVIL.get(), ctx -> new AnvilObjRenderer());
        regCustom(sink, ModBlockEntities.MACHINE_ARC_WELDER.get(), ctx -> new ArcWelderRenderer());
        regCustom(sink, com.hbm.assemblyfactory.AssemblyFactoryRegistrations.MACHINE_ASSEMBLY_FACTORY_BE.get(),
                ctx -> new AssemblyFactoryRenderer());
        com.hbm.client.chemicalfactory.ChemicalFactoryClientRegistrations.registerRenderer(sink);
        regCustom(sink, com.hbm.blocks.machine.AutosawFeature.MACHINE_AUTOSAW.get(), ctx -> new AutosawRenderer());
        regCustom(sink, com.hbm.tileentity.machine.solar.ModSolarBlockEntities.SOLAR_BOILER.get(),
                ctx -> new com.hbm.client.render.solar.SolarBoilerRenderer());
        regCustom(sink, com.hbm.tileentity.machine.solar.ModSolarBlockEntities.SOLAR_MIRROR.get(),
                ctx -> new com.hbm.client.render.solar.SolarMirrorRenderer());
        IntakeFanRenderers.register(sink, ModBlockEntities.MACHINE_INTAKE.get(), ModBlockEntities.MACHINE_FAN.get());
        com.hbm.smoke.SmokeChainClientRegistrations.registerRenderers(sink);
        com.hbm.foundry.FoundryExpansionClient.registerRenderers(sink);
        reg(sink, ModBlockEntities.PA_SOURCE.get(), 90F, 1F,
                new ModelDef(obj("particleaccelerator/source.obj"), tex("particleaccelerator/source.png"), -1F));
        reg(sink, ModBlockEntities.PA_BEAMLINE.get(), 90F, 1F,
                new ModelDef(obj("particleaccelerator/beamline.obj"), tex("particleaccelerator/beamline.png")));
        reg(sink, ModBlockEntities.PA_RFC.get(), 90F, 1F,
                new ModelDef(obj("particleaccelerator/rfc.obj"), tex("particleaccelerator/rfc.png"), -1F));
        reg(sink, ModBlockEntities.PA_QUADRUPOLE.get(), 90F, 1F,
                new ModelDef(obj("particleaccelerator/quadrupole.obj"), tex("particleaccelerator/quadrupole.png"), -1F));
        regCustom(sink, ModBlockEntities.PA_DIPOLE.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.albion.TileEntityPADipole>(0F, 1F,
                        new ModelDef(obj("particleaccelerator/dipole.obj"), tex("particleaccelerator/dipole.png"), -1F))
                        .ignoreFacing());
        reg(sink, ModBlockEntities.PA_DETECTOR.get(), 90F, 1F,
                new ModelDef(obj("particleaccelerator/detector.obj"), tex("particleaccelerator/detector.png"), -2F));

        regCustom(sink, ModBlockEntities.TURRET_CHEKHOV.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretChekhov());
        regCustom(sink, ModBlockEntities.TURRET_FRIENDLY.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretFriendly());
        regCustom(sink, ModBlockEntities.TURRET_SENTRY.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretSentry<>());
        regCustom(sink, ModBlockEntities.TURRET_SENTRY_DAMAGED.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretSentryDamaged());
        regCustom(sink, ModBlockEntities.TURRET_HOWARD.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretHoward());
        regCustom(sink, ModBlockEntities.TURRET_HOWARD_DAMAGED.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretHowardDamaged());
        regCustom(sink, ModBlockEntities.TURRET_TAUON.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretTauon());
        regCustom(sink, ModBlockEntities.TURRET_MAXWELL.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretMaxwell());
        regCustom(sink, ModBlockEntities.TURRET_JEREMY.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretJeremy());
        regCustom(sink, ModBlockEntities.TURRET_RICHARD.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretRichard());
        regCustom(sink, ModBlockEntities.TURRET_FRITZ.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretFritz());
        regCustom(sink, ModBlockEntities.TURRET_HIMARS.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretHIMARS());
        regCustom(sink, ModBlockEntities.TURRET_ARTY.get(),
                ctx -> new com.hbm.client.render.turret.RenderTurretArty());
    }

    private static <T extends BlockEntity> void reg(Sink sink, BlockEntityType<T> type, float yaw, float scale, ModelDef... defs) {
        reg(sink, type, yaw, scale, Set.of(), 0, defs);
    }

    /** Generic renderer with a post-rotation footprint offset (for asymmetric machines like the soldering station). */
    private static <T extends BlockEntity> void reg(Sink sink, BlockEntityType<T> type, float yaw, float scale,
                                                    float offsetX, float offsetZ, ModelDef... defs) {
        CompatBERProvider<T, MachineRenderState> provider =
                ctx -> new MachineObjRenderer<>(yaw, scale, offsetX, offsetZ, Set.of(), 0, defs);
        sink.register(type, provider);
    }

    /** Registers a fully custom renderer (e.g. an animated machine) rather than the generic OBJ one. */
    private static <T extends BlockEntity, S extends CompatRenderState> void regCustom(
            Sink sink, BlockEntityType<T> type, CompatBERProvider<T, S> provider) {
        sink.register(type, provider);
    }

    private static <T extends BlockEntity> void reg(Sink sink, BlockEntityType<T> type, float yaw, float scale,
                                                    Set<String> frameParts, int frameHeight, ModelDef... defs) {
        CompatBERProvider<T, MachineRenderState> provider =
                ctx -> new MachineObjRenderer<>(yaw, scale, frameParts, frameHeight, defs);
        sink.register(type, provider);
    }

    private static Identifier obj(String path) {
        return dev.golem.core.Ids.of(Constants.MOD_ID, "models/" + path);
    }

    private static Identifier tex(String path) {
        return dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/" + path);
    }
}
