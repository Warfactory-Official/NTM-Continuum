package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.render.MachineObjRenderer.ModelDef;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.ModRBMKBlockEntities;
import dev.golem.core.render.CompatBERProvider;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.Visuals;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

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
        regGem(sink, ModRBMKBlockEntities.RBMK_ROD.get(), () -> new RBMKFuelRodRenderer<>(false));
        regGem(sink, ModRBMKBlockEntities.RBMK_ROD_REASIM.get(), () -> new RBMKFuelRodRenderer<>(true));
        regGem(sink, ModRBMKBlockEntities.RBMK_CONTROL.get(), () -> new RBMKControlRodRenderer<>("rbmk_control"));
        regGem(sink, ModRBMKBlockEntities.RBMK_CONTROL_AUTO.get(), () -> new RBMKControlRodRenderer<>("rbmk_control_auto"));
        regGem(sink, ModRBMKBlockEntities.RBMK_BOILER.get(), () -> new RBMKColumnShellRenderer<>("rbmk_boiler", RBMKColumnShellRenderer.Kind.BOILER));
        regGem(sink, ModRBMKBlockEntities.RBMK_MODERATOR.get(), () -> new RBMKColumnShellRenderer<>("rbmk_moderator", RBMKColumnShellRenderer.Kind.PLAIN));
        regGem(sink, ModRBMKBlockEntities.RBMK_REFLECTOR.get(), () -> new RBMKColumnShellRenderer<>("rbmk_reflector", RBMKColumnShellRenderer.Kind.PLAIN));
        regGem(sink, ModRBMKBlockEntities.RBMK_ABSORBER.get(), () -> new RBMKColumnShellRenderer<>("rbmk_absorber", RBMKColumnShellRenderer.Kind.PLAIN));
        regGem(sink, ModRBMKBlockEntities.RBMK_BLANK.get(), () -> new RBMKColumnShellRenderer<>("rbmk_blank", RBMKColumnShellRenderer.Kind.PLAIN));
        regGem(sink, ModRBMKBlockEntities.RBMK_COOLER.get(), () -> new RBMKColumnShellRenderer<>("rbmk_cooler", RBMKColumnShellRenderer.Kind.PLAIN));
        regGem(sink, ModRBMKBlockEntities.RBMK_STORAGE.get(), () -> new RBMKColumnShellRenderer<>("rbmk_storage", RBMKColumnShellRenderer.Kind.PLAIN));
        regGem(sink, ModRBMKBlockEntities.RBMK_HEATER.get(), () -> new RBMKColumnShellRenderer<>("rbmk_heater", RBMKColumnShellRenderer.Kind.BOILER));
        regGem(sink, ModRBMKBlockEntities.RBMK_OUTGASSER.get(), () -> new RBMKColumnShellRenderer<>("rbmk_outgasser", RBMKColumnShellRenderer.Kind.PLAIN));
        regGem(sink, ModRBMKBlockEntities.RBMK_CONSOLE.get(), RBMKConsoleRenderer::new);
        regGem(sink, ModRBMKBlockEntities.RBMK_GAUGE.get(), RBMKGaugeRenderer::new);
        regGem(sink, ModRBMKBlockEntities.RBMK_CRANE_CONSOLE.get(), RBMKCraneConsoleRenderer::new);
        regGem(sink, ModRBMKBlockEntities.RBMK_DEBRIS.get(), RBMKDebrisBlockRenderer::new);
         // The assembly machine has its own animated renderer (spinning ring + two robotic arms).
         regGem(sink, ModBlockEntities.MACHINE_ASSEMBLY_MACHINE.get(), AssemblyMachineRenderer::new);
         // Precision assembler: the legacy four-striker variant of the same assembly model.
         regGem(sink, ModBlockEntities.MACHINE_PREC_ASS.get(), PrecisionAssemblerRenderer::new);
        // The chemical plant has its own animated renderer too: a sliding Slider + rotating Spinner + the
        // Fluid mixer overlay (its own texture, tinted by the recipe fluid, only while processing).
          regGem(sink, ModBlockEntities.MACHINE_CHEMICAL_PLANT.get(), ChemicalPlantRenderer::new);
          regGem(sink, ModBlockEntities.MACHINE_CONDENSER_POWERED.get(), CondenserRenderer::new);
          regGem(sink, ModBlockEntities.MACHINE_STORAGE_DRUM.get(),
                  () -> new GemMachineRenderer<>(0F, "machines/drum.obj", "machines/drum_gray.png"));
         regGem(sink, ModBlockEntities.MACHINE_MIXER.get(), MixerRenderer::new);
         // Mini and polonium RTGs share the legacy body; the renderer adds the four optional connectors.
         regGem(sink, ModBlockEntities.MACHINE_MINI_RTG.get(), RTGRenderer::new);
         // RadGen is entirely custom-rendered, including its glass shell and running indicator.
         regGem(sink, ModBlockEntities.MACHINE_RAD_GEN.get(), RadGenRenderer::new);
          regGem(sink, ModBlockEntities.FUSION_PLASMA_FORGE.get(), PlasmaForgeRenderer::new);
         regGem(sink, ModBlockEntities.MACHINE_EXPOSURE_CHAMBER.get(), ExposureChamberRenderer::new);
        // Blast furnace extension: the legacy flue, a truncated pyramid drawn from one 32x32 sheet whose
        // quadrants are the three block icons 1.7 bound per OBJ part.
        regGem(sink, com.hbm.blocks.machine.DiFurnaceContent.DI_FURNACE_EXTENSION.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityDiFurnaceExtension>(0F, 1F,
                        new ModelDef(obj("block/difurnace_extension.obj"),
                                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/blocks/difurnace_extension.png")))
                        .ignoreFacing());
        // Blast furnace: static body, with the legacy yaw map (base 90° + the standard facing map).
        regGem(sink, ModBlockEntities.MACHINE_BLAST_FURNACE.get(), () -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityMachineBlastFurnace>(90F, 1F,
                new ModelDef(obj("machines/blast_furnace.obj"), tex("machines/blast_furnace.png"))).doubleSided());
        regGem(sink, ModBlockEntities.MACHINE_INDUSTRIAL_TURBINE.get(), IndustrialTurbineRenderer::new);
         regGem(sink, ModBlockEntities.MACHINE_LARGE_TURBINE.get(), LargeTurbineRenderer::new);
         regGem(sink, ModBlockEntities.MACHINE_TURBINE_GAS.get(), GasTurbineRenderer::new);
         regGem(sink, ModBlockEntities.MACHINE_TURBOFAN.get(), TurbofanRenderer::new);
         regGem(sink, ModBlockEntities.MACHINE_STEAM_ENGINE.get(), SteamEngineRenderer::new);
         regGem(sink, ModBlockEntities.MACHINE_PUMP_ELECTRIC.get(), () -> new PumpRenderer<com.hbm.tileentity.machine.TileEntityMachinePumpElectric>(
                 tex("machines/pump_electric.png")));
         regGem(sink, ModBlockEntities.MACHINE_PUMP_STEAM.get(), () -> new PumpRenderer<com.hbm.tileentity.machine.TileEntityMachinePumpSteam>(
                 tex("machines/pump_steam.png")));
         regGem(sink, ModBlockEntities.MACHINE_STIRLING.get(), StirlingRenderer::new);
         regGem(sink, ModBlockEntities.MACHINE_PUREX.get(), PUREXRenderer::new);
        // Presses: a static body + a head that descends with the machine's progress, and the ingredient
        // lying on the anvil between them.
        regGem(sink, ModBlockEntities.MACHINE_EPRESS.get(), () -> new PressRenderer<>(
                obj("epress_body.obj"), tex("machines/epress_body.png"),
                obj("epress_head.obj"), tex("machines/epress_head.png"),
                90F, true, 1.875F, 1.0F, true, 200, te -> te.progress, te -> te.lastProgress,
                0.128125F, 90F, te -> te.syncStack));
         regGem(sink, ModBlockEntities.MACHINE_PRESS.get(), () -> new PressRenderer<>(
                 obj("press_body.obj"), tex("machines/press_body.png"),
                 obj("press_head.obj"), tex("machines/press_head.png"),
                  180F, false, 0.875F, 0.99F, false, 200, te -> te.progress, te -> te.lastProgress,
                  0F, 180F, te -> te.syncStack));
         regGem(sink, ModBlockEntities.MACHINE_AMMO_PRESS.get(), AmmoPressRenderer::new);
         regGem(sink, ModBlockEntities.MACHINE_SAWMILL.get(), SawmillRenderer::new);
        // The launch pad draws the missile it is holding on top of the silo, so it needs its own
        // renderer rather than the generic static-OBJ one.
        regGem(sink, ModBlockEntities.LAUNCH_PAD.get(), LaunchPadRenderer::new);

        // Soldering station: its own renderer — the static OBJ (with the asymmetric-footprint offset) plus
        // the current recipe's output item floating above (the 1.12.2 product display).
        regGem(sink, ModBlockEntities.MACHINE_SOLDERING_STATION.get(), SolderingStationRenderer::new);

        // Ore acidizer: static Body + rotating Spinner + the tinted acid overlay while processing.
         regGem(sink, ModBlockEntities.MACHINE_CRYSTALLIZER.get(), CrystallizerRenderer::new);
         // Standard centrifuge: static tower, with the legacy N/S/W/E core yaw map.
           reg(sink, ModBlockEntities.MACHINE_CENTRIFUGE.get(), 0F, 1F,
                   new ModelDef(obj("machines/centrifuge.obj"), tex("machines/centrifuge_new.png")));
           regGem(sink, ModBlockEntities.MACHINE_CYCLOTRON.get(), CyclotronRenderer::new);
           // Gas centrifuge: old yaw map N270/S90/W0/E180 = built-in facing map + 270 degrees.
          reg(sink, ModBlockEntities.MACHINE_GAS_CENT.get(), 270F, 1F,
                   new ModelDef(obj("machines/centrifuge_gas.obj"), tex("machines/centrifuge_gas.png")));
           reg(sink, ModBlockEntities.MACHINE_ELECTROLYSER.get(), 180F, 1F,
                   new ModelDef(obj("machines/electrolyser.obj"), tex("machines/electrolyser.png")));
         regGem(sink, ModBlockEntities.MACHINE_MINING_LASER.get(), MiningLaserRenderer::new);

        // Watz: upstream had no TESR for it at all — it was a StaticTesrBakedModels entry whose yaw map
        // is all zeros, i.e. the model does not turn with the machine's facing.
        regGem(sink, ModBlockEntities.WATZ.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityWatz>(0F, 1F,
                        new ModelDef(obj("reactors/watz.obj"), tex("machines/watz.png"))).ignoreFacing());
        regGem(sink, ModBlockEntities.WATZ_PUMP.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityWatzPump>(0F, 1F,
                        new ModelDef(obj("machines/watz_pump.obj"), tex("machines/watz_pump.png"))).ignoreFacing());
        // The struct seed draws a translucent ghost of whatever its pattern still needs.
        regCustom(sink, ModBlockEntities.STRUCT_WATZ_CORE.get(),
                ctx -> new StructPreviewRenderer<com.hbm.tileentity.machine.struct.TileEntityWatzStruct>());

        // Fusion torus: spinning magnet ring, per-arm bolt covers and the tinted plasma glow.
        regGem(sink, ModBlockEntities.FUSION_TORUS.get(), FusionTorusRenderer::new);

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
        regGem(sink, ModBlockEntities.FUSION_KLYSTRON.get(),
                () -> new FusionSpinRenderer<com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron>(
                        "fusion/klystron.obj", "fusion/klystron.png", "Klystron", "Rotor", 2.5F, -1F, 90F,
                        (te, partial) -> te.prevFan + (te.fan - te.prevFan) * partial));
        regGem(sink, ModBlockEntities.FUSION_KLYSTRON_CREATIVE.get(),
                () -> new FusionSpinRenderer<com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative>(
                        "fusion/klystron.obj", "fusion/klystron_creative.png", "Klystron", "Rotor", 2.5F, -1F, 90F,
                        (te, partial) -> te.prevFan + (te.fan - te.prevFan) * partial));
        // The turbine's coils rock at a pivot 1.5 up, wrapped to 15 degrees as upstream does - they are
        // a partial arc, and a full rotation swings them clear of the housing.
        regGem(sink, ModBlockEntities.FUSION_MHDT.get(),
                () -> new FusionSpinRenderer<com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT>(
                        "fusion/mhdt.obj", "fusion/mhdt.png", "Turbine", "Coils", 1.5F, 0F, 90F, 15F,
                        (te, partial) -> te.prevRotor + (te.rotor - te.prevRotor) * partial));
        regCustom(sink, ModBlockEntities.STRUCT_TORUS_CORE.get(),
                ctx -> new StructPreviewRenderer<com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct>());

        regGem(sink, ModRBMKBlockEntities.RBMK_TERMINAL.get(), RBMKTerminalRenderer::new);
        regGem(sink, ModBlockEntities.CARGO_ELEVATOR.get(), CargoElevatorRenderer::new);
        regGem(sink, ModBlockEntities.BARREL.get(), BarrelRenderer::new);
        regGem(sink, ModBlockEntities.MACHINE_CHUNGUS.get(), ChungusRenderer::new);
        regGem(sink, ModBlockEntities.MACHINE_SATLINK.get(), SatLinkRenderer::new);
        regGem(sink, ModBlockEntities.SOYUZ_LAUNCHER.get(), SoyuzLauncherRenderer::new);
        regGem(sink, ModBlockEntities.PILE_CONTROL.get(), PileDeviceRenderers.Control::new);
        regGem(sink, ModBlockEntities.PILE_LOADER.get(), PileDeviceRenderers.Loader::new);
        regGem(sink, ModBlockEntities.PILE_VENT.get(), PileDeviceRenderers.Vent::new);

        // 1.7 draws both of these from an OBJ and leaves the block itself with no model: the geiger on a
        // TESR, the supply crate on a chunk-mesh handler that a modern port has no equivalent of. Both
        // meshes are authored about the origin, which BlockObjRenderer's centring translate answers.
        regGem(sink, ModBlockEntities.GEIGER.get(),
                () -> new com.hbm.client.render.BlockObjRenderer<com.hbm.tileentity.machine.TileEntityGeiger>(
                        "blocks/geiger_counter.obj", "textures/blocks/geiger.png", true));
        regGem(sink, ModBlockEntities.CRATE_SUPPLY.get(),
                () -> new com.hbm.client.render.BlockObjRenderer<com.hbm.tileentity.machine.storage.TileEntityCrateSupply>(
                        "blocks/conservecrate.obj", "textures/block/crate_can.png"));

        regGem(sink, ModBlockEntities.DFC_EMITTER.get(), CoreComponentRenderers.Emitter::new);
        regGem(sink, ModBlockEntities.DFC_RECEIVER.get(), CoreComponentRenderers.Receiver::new);
        regGem(sink, ModBlockEntities.DFC_INJECTOR.get(), CoreComponentRenderers.Injector::new);
        regGem(sink, ModBlockEntities.DFC_STABILIZER.get(), CoreComponentRenderers.Stabilizer::new);
        regCustom(sink, ModBlockEntities.DFC_CORE.get(), ctx -> new CoreRenderer());

        reg(sink, ModBlockEntities.PA_SOURCE.get(), 90F, 1F,
                new ModelDef(obj("particleaccelerator/source.obj"), tex("particleaccelerator/source.png"), -1F));
        reg(sink, ModBlockEntities.PA_BEAMLINE.get(), 90F, 1F,
                new ModelDef(obj("particleaccelerator/beamline.obj"), tex("particleaccelerator/beamline.png")));
        reg(sink, ModBlockEntities.PA_RFC.get(), 90F, 1F,
                new ModelDef(obj("particleaccelerator/rfc.obj"), tex("particleaccelerator/rfc.png"), -1F));
        reg(sink, ModBlockEntities.PA_QUADRUPOLE.get(), 90F, 1F,
                new ModelDef(obj("particleaccelerator/quadrupole.obj"), tex("particleaccelerator/quadrupole.png"), -1F));
        regGem(sink, ModBlockEntities.PA_DIPOLE.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.albion.TileEntityPADipole>(0F, 1F,
                        new ModelDef(obj("particleaccelerator/dipole.obj"), tex("particleaccelerator/dipole.png"), -1F))
                        .ignoreFacing());
        reg(sink, ModBlockEntities.PA_DETECTOR.get(), 90F, 1F,
                new ModelDef(obj("particleaccelerator/detector.obj"), tex("particleaccelerator/detector.png"), -2F));
        // SILEX: fully static upstream (it had no TESR at all — it was a StaticTesrBakedModels entry).
        // Old yaw map N90/S270/W180/E0 = base yaw 90 + the standard map, same as the compressor's.
        reg(sink, ModBlockEntities.MACHINE_SILEX.get(), 90F, 1F,
                new ModelDef(obj("machines/silex.obj"), tex("machines/silex.png")));
        // FEL: its own renderer — the static emitter plus the BeamPronter beam out to the hit block.
        regGem(sink, ModBlockEntities.MACHINE_FEL.get(), FELRenderer::new);
        // Oil derrick: static OBJ; old yaw map (N180/S0/W270/E90) = base yaw 180 + the standard map.
        reg(sink, ModBlockEntities.MACHINE_OIL_WELL.get(), 180F, 1F,
                new ModelDef(obj("machines/derrick.obj"), tex("machines/derrick.png")));
        // Pumpjack: animated rotor/head/carriage + the untextured crank/rope geometry.
        regGem(sink, ModBlockEntities.MACHINE_PUMPJACK.get(), PumpjackRenderer::new);
        // Diesel generator: single INVISIBLE facing block, OBJ TESR (Generator + jittering Engine).
        regGem(sink, ModBlockEntities.MACHINE_DIESEL.get(), DieselGenRenderer::new);
        // Flare stack: static OBJ, old yaw map is a constant 180 for all facings.
        regGem(sink, ModBlockEntities.MACHINE_GAS_FLARE.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare>(180F, 1F,
                        new ModelDef(obj("machines/flare_stack.obj"), tex("machines/flare_stack.png"))).ignoreFacing());
        // Fraction tower + spacer: static OBJs, facing-independent (all-zero yaw map).
        regGem(sink, ModBlockEntities.MACHINE_FRACTION_TOWER.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower>(0F, 1F,
                        new ModelDef(obj("machines/fraction_tower.obj"), tex("machines/fraction_tower.png"))).ignoreFacing());
        regGem(sink, ModBlockEntities.FRACTION_SPACER.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntitySpacer>(0F, 1F,
                        new ModelDef(obj("machines/fraction_spacer.obj"), tex("machines/fraction_spacer.png"))).ignoreFacing());
        // Liquefactor + solidifier: Main body + Y-scaled tinted Fluid + translucent Glass.
        regGem(sink, ModBlockEntities.MACHINE_LIQUEFACTOR.get(),
                () -> new TankGlassRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor>("liquefactor", "liquefactor", te -> te.tank));
        regGem(sink, ModBlockEntities.MACHINE_SOLIDIFIER.get(),
                () -> new TankGlassRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier>("solidifier", "solidifier", te -> te.tank));
        // Refinery: static OBJ; the old yaw map is a constant 180 for all facings (facing-independent).
        regGem(sink, ModBlockEntities.MACHINE_REFINERY.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineRefinery>(180F, 1F,
                        new ModelDef(obj("machines/refinery.obj"), tex("machines/refinery.png"))).ignoreFacing());
        // Advanced oil machines — static OBJ renders (facing-dependent animations deferred, §20).
        regGem(sink, ModBlockEntities.MACHINE_FRACKING_TOWER.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower>(180F, 1F,
                        new ModelDef(obj("machines/fracking_tower.obj"), tex("machines/fracking_tower.png"))).ignoreFacing());
        regGem(sink, ModBlockEntities.MACHINE_VACUUM_DISTILL.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill>(0F, 1F,
                        new ModelDef(obj("machines/vacuum_distill.obj"), tex("machines/vacuum_distill.png"))).ignoreFacing());
        // Reformer: old RenderCatalyticReformer yaw map N90/W180/S270/E0 = the built-in map + 90.
        regGem(sink, ModBlockEntities.MACHINE_CATALYTIC_REFORMER.get(), CatalyticReformerRenderer::new);
        // Cracker: old StaticTesrBakedModels yaw map meta12(N)=90/13(S)=270/14(W)=180/15(E)=0 = built-in + 90.
        regGem(sink, ModBlockEntities.MACHINE_CATALYTIC_CRACKER.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker>(90F, 1F,
                        new ModelDef(obj("machines/cracking_tower.obj"), tex("machines/cracking_tower.png"))));
        regGem(sink, ModBlockEntities.MACHINE_HYDROTREATER.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater>(0F, 1F,
                        new ModelDef(obj("machines/hydrotreater.obj"), tex("machines/hydrotreater.png"))).ignoreFacing());
        regGem(sink, ModBlockEntities.MACHINE_COKER.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineCoker>(0F, 1F,
                        new ModelDef(obj("machines/coker.obj"), tex("machines/coker.png"))).ignoreFacing());
        regGem(sink, ModBlockEntities.FURNACE_COMBINATION.get(), CombinationFurnaceRenderer::new);
        // Pyro oven: old RenderPyroOven yaw map N180/W270/S0/E90 = the built-in map + 180. (The animated
        // "Slider"/"Fan" sub-parts render at their model origin — the Slider's rest transform is a −0.5 X
        // translate, so it sits half a block off until a custom animated renderer lands. §20)
        regGem(sink, ModBlockEntities.MACHINE_PYROOVEN.get(), PyroOvenRenderer::new);
        // ZIRNOX: old yaw map meta12(N)=90/13(S)=270/14(W)=180/15(E)=0 = built-in + 90.
        regGem(sink, ModBlockEntities.MACHINE_ZIRNOX.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityReactorZirnox>(90F, 1F,
                        new ModelDef(obj("zirnox.obj"), tex("machines/zirnox.png"))).doubleSided());
        regGem(sink, ModBlockEntities.ZIRNOX_DESTROYED.get(), ZirnoxDestroyedRenderer::new);
        // Combustion engine: the old RenderCombustionEngine yaw map is N90/S270/W180/E0 — exactly the
        // generic renderer's built-in map (N0/S180/W90/E270) + 90 — then a post-rotation
        // translate(-0.5, 0, 3) (the model isn't centred on the core cell).
        regGem(sink, ModBlockEntities.MACHINE_COMBUSTION_ENGINE.get(), CombustionEngineRenderer::new);
        // Refueler: a single FACING block (not a multiblock), so the generic renderer can't read its
        // facing — own renderer (yaw from FACING + the fill-level fluid part).
        regGem(sink, ModBlockEntities.REFUELER.get(), RefuelerRenderer::new);

        // Compressor: own renderer — the Pump part sits at −3 blocks at rest (lift·3−3) and the Fan spins,
        // so a static render would drop the pump through the floor.
        regGem(sink, ModBlockEntities.MACHINE_COMPRESSOR.get(), CompressorRenderer::new);

        // Heat boiler: static OBJ + the burst husk + the near-full wobble (own renderer).
         regGem(sink, ModBlockEntities.HEAT_BOILER.get(), HeatBoilerRenderer::new);
         regGem(sink, ModBlockEntities.MACHINE_INDUSTRIAL_BOILER.get(),
                 () -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial>(0F, 1F,
                         new ModelDef(obj("machines/industrial_boiler.obj"), tex("machines/industrial_boiler.png"))).ignoreFacing());
        // Oilburner: static OBJ; the old yaw map is all zeros -> facing-independent.
        regGem(sink, ModBlockEntities.HEATER_OILBURNER.get(),
                () -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityHeaterOilburner>(0F, 1F,
                        new ModelDef(obj("machines/oilburner.obj"), tex("machines/oilburner.png"))).ignoreFacing());

         // Bombs: each static OBJ keeps the legacy model-specific yaw map and double-sided geometry.
         regGem(sink, ModBlockEntities.NUKE_MAN.get(),
                 () -> new BombObjRenderer<>(obj("bombs/fatman.obj"), tex("bombs/fatman.png"), 1F));
         regGem(sink, ModBlockEntities.NUKE_N2.get(),
                 () -> new BombObjRenderer<>(obj("bombs/n2.obj"), tex("bombs/n2.png"), 1F,
                         com.hbm.blocks.bomb.NukeBombBlock.FACING, 0F, 0F, 0F, 180F, 0F, 270F, 90F));
         regGem(sink, ModBlockEntities.NUKE_BOY.get(),
                 () -> new BombObjRenderer<>(obj("bombs/lilboy.obj"), tex("bombs/lilboy.png"), 1F,
                         com.hbm.blocks.bomb.NukeBombBlock.FACING, -2F, 0F, 0F, 0F, 180F, 90F, 270F));
         regGem(sink, ModBlockEntities.NUKE_GADGET.get(),
                 () -> new BombObjRenderer<>(obj("bombs/gadget.obj"), tex("bombs/gadget.png"), 1F,
                         com.hbm.blocks.bomb.NukeBombBlock.FACING, 0F, 0F, 0F, 0F, 180F, 90F, 270F));
         regGem(sink, ModBlockEntities.NUKE_TSAR.get(),
                 () -> new BombObjRenderer<>(obj("bombs/tsar.obj"), tex("bombs/tsar.png"), 1F,
                         com.hbm.blocks.bomb.NukeBombBlock.FACING, 0F, 0F, 0F, 0F, 180F, 90F, 270F));

        // Red cable: the real cable_neo.obj, parts chosen from the connection state.
        regGem(sink, ModBlockEntities.RED_CABLE.get(), CableObjRenderer::new);

        // Fluid duct: the real pipe_neo.obj, parts chosen from the connection state (like the cable).
        regGem(sink, ModBlockEntities.FLUID_DUCT.get(), FluidDuctObjRenderer::new);

         // Fluid tank: a multiblock with its own renderer — the cylinder body is textured
         // per-fluid (tank/tank_<name>.png, NONE -> tank_none) while the frame stays machines/tank.png, so
         // it can't use the single-texture-per-part generic renderer. (Base yaw 180°; see FluidTankRenderer.)
          regGem(sink, ModBlockEntities.FLUID_TANK.get(), FluidTankRenderer::new);
          reg(sink, ModBlockEntities.MACHINE_UF6_TANK.get(), 0F, 1F,
                  new ModelDef(obj("tank.obj"), tex("machines/uf6tank.png")));
           reg(sink, ModBlockEntities.MACHINE_PUF6_TANK.get(), 0F, 1F,
                   new ModelDef(obj("tank.obj"), tex("machines/puf6tank.png")));
           regGem(sink, ModBlockEntities.MACHINE_BIG_ASS_TANK.get(), BigAssTankRenderer::new);
           regGem(sink, ModBlockEntities.MACHINE_BAT9000.get(), BAT9000Renderer::new);
           reg(sink, ModBlockEntities.MACHINE_DEUTERIUM_TOWER.get(), 180F, 1F, 0.5F, -0.5F,
                   new ModelDef(obj("machines/machine_deuterium_tower.obj"),
                           tex("machines/machine_deuterium_tower.png")));
         // Drain: legacy yaw map is the standard facing map + 90 degrees.
         regGem(sink, ModBlockEntities.MACHINE_DRAIN.get(), () -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityMachineDrain>(90F, 1F,
                 new ModelDef(obj("machines/drain.obj"), tex("machines/drain.png"))));

         // Battery socket: draws the cradle + the inserted battery (per-variant texture).
         regGem(sink, ModBlockEntities.BATTERY_SOCKET.get(), BatterySocketRenderer::new);
          // FEnSU: colored body + X-axis flywheel, with its lights submitted fullbright.
          regGem(sink, ModBlockEntities.FENSU.get(), FensuRenderer::new);
          // Current FEnSU: the separate fensu2 flywheel bank, including additive plasma and zaps.
          regGem(sink, ModBlockEntities.BATTERY_REDD.get(), BatteryReddRenderer::new);

        // Crucible: basin OBJ + molten pool surface + the two pour streams.
        regGem(sink, ModBlockEntities.MACHINE_CRUCIBLE.get(), CrucibleRenderer::new);

         // Firebox: body + door + the burning/empty interior.
          regGem(sink, ModBlockEntities.HEATER_FIREBOX.get(), () -> new FireboxRenderer<>());
           regGem(sink, ModBlockEntities.HEATER_OVEN.get(), () -> new FireboxRenderer<com.hbm.tileentity.machine.TileEntityHeaterOven>(
                   "models/machines/heating_oven.obj", "textures/models/machines/heating_oven.png", "Inner", true));
          regGem(sink, ModBlockEntities.FURNACE_IRON.get(), FurnaceIronRenderer::new);
         regGem(sink, ModBlockEntities.FURNACE_STEEL.get(), FurnaceSteelRenderer::new);
         regGem(sink, ModBlockEntities.MACHINE_WOOD_BURNER.get(),
                 () -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityMachineWoodBurner>(180F, 1F,
                         -0.5F, -0.5F, Set.of(), 0,
                         new ModelDef(obj("machines/wood_burner.obj"), tex("machines/wood_burner.png"))));

        // Electric heater: a static OBJ (the old CE fork used a static baked model).
        reg(sink, ModBlockEntities.HEATER_ELECTRIC.get(), 90F, 1F,
                new ModelDef(obj("machines/electric_heater.obj"), tex("machines/electric_heater.png")));
        reg(sink, ModBlockEntities.HEATER_HEATEX.get(), 90F, 1F,
                new ModelDef(obj("machines/heatex.obj"), tex("machines/heater_heatex.png")));

        // Foundry: channel melt level, casters (mold JSON-modelled / basin OBJ), outlet + slagtap OBJs, slag pile.
        regGem(sink, ModBlockEntities.FOUNDRY_CHANNEL.get(), FoundryChannelRenderer::new);
        regGem(sink, ModBlockEntities.FOUNDRY_MOLD.get(), () -> new FoundryCastingRenderer<>(null));
        regGem(sink, ModBlockEntities.FOUNDRY_BASIN.get(), () -> new FoundryCastingRenderer<>("models/block/foundry_basin.obj"));
        regGem(sink, ModBlockEntities.FOUNDRY_OUTLET.get(), () -> new FoundryOutletRenderer<>("models/block/foundry_outlet.obj"));
        regGem(sink, ModBlockEntities.FOUNDRY_SLAGTAP.get(), () -> new FoundryOutletRenderer<>("models/block/foundry_slagtap.obj"));
        regGem(sink, ModBlockEntities.SLAG.get(), SlagRenderer::new);

        // Decorative OBJ blocks (boxcar / boat / satellites): one renderer picks the model per block.
        regGem(sink, ModBlockEntities.DECO.get(), DecoObjRenderer::new);

        // The 12 tiered anvils: one renderer picks the per-anvil OBJ + texture from the block.
        regGem(sink, ModBlockEntities.ANVIL.get(), AnvilObjRenderer::new);
        regGem(sink, ModBlockEntities.MACHINE_ARC_WELDER.get(), ArcWelderRenderer::new);
        regGem(sink, com.hbm.assemblyfactory.AssemblyFactoryRegistrations.MACHINE_ASSEMBLY_FACTORY_BE.get(),
                AssemblyFactoryRenderer::new);
        com.hbm.client.chemicalfactory.ChemicalFactoryClientRegistrations.registerRenderer(sink);
        regGem(sink, com.hbm.blocks.machine.AutosawFeature.MACHINE_AUTOSAW.get(), AutosawRenderer::new);
        regGem(sink, com.hbm.tileentity.machine.solar.ModSolarBlockEntities.SOLAR_BOILER.get(),
                com.hbm.client.render.solar.SolarBoilerRenderer::new);
        regGem(sink, com.hbm.tileentity.machine.solar.ModSolarBlockEntities.SOLAR_MIRROR.get(),
                com.hbm.client.render.solar.SolarMirrorRenderer::new);
        IntakeFanRenderers.register(sink, ModBlockEntities.MACHINE_INTAKE.get(), ModBlockEntities.MACHINE_FAN.get());
        com.hbm.smoke.SmokeChainClientRegistrations.registerRenderers(sink);
        com.hbm.foundry.FoundryExpansionClient.registerRenderers(sink);
        regGem(sink, ModBlockEntities.TURRET_CHEKHOV.get(),
                com.hbm.client.render.turret.RenderTurretChekhov::new);
        regGem(sink, ModBlockEntities.TURRET_FRIENDLY.get(),
                com.hbm.client.render.turret.RenderTurretFriendly::new);
        regGem(sink, ModBlockEntities.TURRET_SENTRY.get(),
                com.hbm.client.render.turret.RenderTurretSentry::new);
        regGem(sink, ModBlockEntities.TURRET_SENTRY_DAMAGED.get(),
                com.hbm.client.render.turret.RenderTurretSentryDamaged::new);
        regGem(sink, ModBlockEntities.TURRET_HOWARD.get(),
                com.hbm.client.render.turret.RenderTurretHoward::new);
        regGem(sink, ModBlockEntities.TURRET_HOWARD_DAMAGED.get(),
                com.hbm.client.render.turret.RenderTurretHowardDamaged::new);
        regGem(sink, ModBlockEntities.TURRET_TAUON.get(),
                com.hbm.client.render.turret.RenderTurretTauon::new);
        regGem(sink, ModBlockEntities.TURRET_MAXWELL.get(),
                com.hbm.client.render.turret.RenderTurretMaxwell::new);
        regGem(sink, ModBlockEntities.TURRET_JEREMY.get(),
                com.hbm.client.render.turret.RenderTurretJeremy::new);
        regGem(sink, ModBlockEntities.TURRET_RICHARD.get(),
                com.hbm.client.render.turret.RenderTurretRichard::new);
        regGem(sink, ModBlockEntities.TURRET_FRITZ.get(),
                com.hbm.client.render.turret.RenderTurretFritz::new);
        regGem(sink, ModBlockEntities.TURRET_HIMARS.get(),
                com.hbm.client.render.turret.RenderTurretHIMARS::new);
        regGem(sink, ModBlockEntities.TURRET_ARTY.get(),
                com.hbm.client.render.turret.RenderTurretArty::new);
    }

    private static <T extends BlockEntity> void reg(Sink sink, BlockEntityType<T> type, float yaw, float scale, ModelDef... defs) {
        reg(sink, type, yaw, scale, Set.of(), 0, defs);
    }

    /** Generic renderer with a post-rotation footprint offset (for asymmetric machines like the soldering station). */
    private static <T extends BlockEntity> void reg(Sink sink, BlockEntityType<T> type, float yaw, float scale,
                                                    float offsetX, float offsetZ, ModelDef... defs) {
        regGem(sink, type, () -> new MachineObjRenderer<T>(yaw, scale, offsetX, offsetZ, Set.of(), 0, defs));
    }

    /**
     * Registers a renderer that declares its model, binding it to both backends.
     *
     * <p>Both registries must end up holding the <b>same</b> renderer instance, because the model is
     * built on first use and the instanced backend allocates against the tree that build produced. The
     * supplier is memoized here so the vanilla registry and {@link Visuals} cannot end up with two.
     */
    public static <T extends BlockEntity, S extends CompatRenderState> void regGem(
            Sink sink, BlockEntityType<T> type, Supplier<? extends GemBER<T, S>> factory) {
        Supplier<GemBER<T, S>> shared = memoize(() -> {
            GemBER<T, S> renderer = factory.get();
            // Build the declaration here rather than leaving it to the first frame. A part naming a
            // mesh group the model does not have throws, and this is the moment where that is a
            // startup failure naming the renderer instead of a machine that quietly draws nothing.
            renderer.gem();
            return renderer;
        });
        sink.register(type, ctx -> shared.get());
        Visuals.register(type, shared);
        GEM_DECLARED.add(new GemDeclared<>(type, shared));
    }

    /**
     * Every gem-declared block entity, kept so a client with an engine Core does not ship can bind the
     * same declarations to it. Core's {@link Visuals} already binds the vendored engine; the published
     * Flywheel on the older nodes is reached through this instead, because naming it is the consumer's
     * business and not the library's.
     */
    public record GemDeclared<T extends BlockEntity, S extends CompatRenderState>(
            BlockEntityType<T> type, Supplier<GemBER<T, S>> renderer) {}

    private static final List<GemDeclared<?, ?>> GEM_DECLARED = new ArrayList<>();

    public static List<GemDeclared<?, ?>> gemDeclared() {
        return Collections.unmodifiableList(GEM_DECLARED);
    }

    private static <V> Supplier<V> memoize(Supplier<V> factory) {
        return new Supplier<>() {
            private V value;

            @Override
            public V get() {
                V held = value;
                if (held == null) value = held = factory.get();
                return held;
            }
        };
    }

    /** Registers a fully custom renderer (e.g. an animated machine) rather than the generic OBJ one. */
    private static <T extends BlockEntity, S extends CompatRenderState> void regCustom(
            Sink sink, BlockEntityType<T> type, CompatBERProvider<T, S> provider) {
        sink.register(type, provider);
    }

    private static <T extends BlockEntity> void reg(Sink sink, BlockEntityType<T> type, float yaw, float scale,
                                                    Set<String> frameParts, int frameHeight, ModelDef... defs) {
        regGem(sink, type, () -> new MachineObjRenderer<T>(yaw, scale, frameParts, frameHeight, defs));
    }

    private static Identifier obj(String path) {
        return dev.golem.core.Ids.of(Constants.MOD_ID, "models/" + path);
    }

    private static Identifier tex(String path) {
        return dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/" + path);
    }
}
