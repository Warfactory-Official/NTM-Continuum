package com.hbm.client.render;

import dev.golem.core.render.CompatBERProvider;
import dev.golem.core.render.CompatRenderState;
import com.hbm.Constants;
import com.hbm.client.render.MachineObjRenderer.ModelDef;
import com.hbm.tileentity.ModBlockEntities;
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
        // The assembly machine has its own animated renderer (spinning ring + two robotic arms).
        regCustom(sink, ModBlockEntities.MACHINE_ASSEMBLY_MACHINE.get(), ctx -> new AssemblyMachineRenderer());
        // The chemical plant has its own animated renderer too: a sliding Slider + rotating Spinner + the
        // Fluid mixer overlay (its own texture, tinted by the recipe fluid, only while processing).
        regCustom(sink, ModBlockEntities.MACHINE_CHEMICAL_PLANT.get(), ctx -> new ChemicalPlantRenderer());
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
        reg(sink, ModBlockEntities.LAUNCH_PAD.get(), 90F, 1F,
                new ModelDef(obj("weapons/launch_pad_silo.obj"), tex("launchpad/silo.png")));

        // Soldering station: its own renderer — the static OBJ (with the asymmetric-footprint offset) plus
        // the current recipe's output item floating above (the 1.12.2 product display).
        regCustom(sink, ModBlockEntities.MACHINE_SOLDERING_STATION.get(), ctx -> new SolderingStationRenderer());

        // Ore acidizer: static Body + rotating Spinner + the tinted acid overlay while processing.
        regCustom(sink, ModBlockEntities.MACHINE_CRYSTALLIZER.get(), ctx -> new CrystallizerRenderer());
        regCustom(sink, ModBlockEntities.MACHINE_MINING_LASER.get(), ctx -> new MiningLaserRenderer());
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
        regCustom(sink, ModBlockEntities.MACHINE_CATALYTIC_REFORMER.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer>(90F, 1F,
                        new ModelDef(obj("machines/catalytic_reformer.obj"), tex("machines/catalytic_reformer.png"))));
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
        // Pyro oven: old RenderPyroOven yaw map N180/W270/S0/E90 = the built-in map + 180. (The animated
        // "Slider"/"Fan" sub-parts render at their model origin — the Slider's rest transform is a −0.5 X
        // translate, so it sits half a block off until a custom animated renderer lands. §20)
        regCustom(sink, ModBlockEntities.MACHINE_PYROOVEN.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven>(180F, 1F,
                        new ModelDef(obj("machines/pyrooven.obj"), tex("machines/pyrooven.png"))));
        // ZIRNOX: old yaw map meta12(N)=90/13(S)=270/14(W)=180/15(E)=0 = built-in + 90.
        regCustom(sink, ModBlockEntities.MACHINE_ZIRNOX.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityReactorZirnox>(90F, 1F,
                        new ModelDef(obj("zirnox.obj"), tex("machines/zirnox.png"))).doubleSided());
        regCustom(sink, ModBlockEntities.ZIRNOX_DESTROYED.get(),
                ctx -> new ZirnoxDestroyedRenderer());
        // Combustion engine: the old RenderCombustionEngine yaw map is N90/S270/W180/E0 — exactly the
        // generic renderer's built-in map (N0/S180/W90/E270) + 90 — then a post-rotation
        // translate(-0.5, 0, 3) (the model isn't centred on the core cell).
        regCustom(sink, ModBlockEntities.MACHINE_COMBUSTION_ENGINE.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityMachineCombustionEngine>(90F, 1F, -0.5F, 3F, Set.of(), 0,
                        new ModelDef(obj("machines/combustion_engine.obj"), tex("machines/combustion_engine.png"))));
        // Refueler: a single FACING block (not a BlockDummyable), so the generic renderer can't read its
        // facing — own renderer (yaw from FACING + the fill-level fluid part).
        regCustom(sink, ModBlockEntities.REFUELER.get(), ctx -> new RefuelerRenderer());

        // Compressor: own renderer — the Pump part sits at −3 blocks at rest (lift·3−3) and the Fan spins,
        // so a static render would drop the pump through the floor.
        regCustom(sink, ModBlockEntities.MACHINE_COMPRESSOR.get(), ctx -> new CompressorRenderer());

        // Heat boiler: static OBJ + the burst husk + the near-full wobble (own renderer).
        regCustom(sink, ModBlockEntities.HEAT_BOILER.get(), ctx -> new HeatBoilerRenderer());
        // Oilburner: static OBJ; the old yaw map is all zeros -> facing-independent.
        regCustom(sink, ModBlockEntities.HEATER_OILBURNER.get(),
                ctx -> new MachineObjRenderer<com.hbm.tileentity.machine.TileEntityHeaterOilburner>(0F, 1F,
                        new ModelDef(obj("machines/oilburner.obj"), tex("machines/oilburner.png"))).ignoreFacing());

        // Bombs: the Fat Man draws a static OBJ keyed on its own FACING property (own yaw map).
        regCustom(sink, ModBlockEntities.NUKE_MAN.get(),
                ctx -> new BombObjRenderer<>(obj("bombs/fatman.obj"), tex("bombs/fatman.png"), 1F));

        // Red cable: the real cable_neo.obj, parts chosen from the connection state.
        regCustom(sink, ModBlockEntities.RED_CABLE.get(), ctx -> new CableObjRenderer<>());

        // Fluid duct: the real pipe_neo.obj, parts chosen from the connection state (like the cable).
        regCustom(sink, ModBlockEntities.FLUID_DUCT.get(), ctx -> new FluidDuctObjRenderer());

        // Fluid tank: a BlockDummyable multiblock with its own renderer — the cylinder body is textured
        // per-fluid (tank/tank_<name>.png, NONE -> tank_none) while the frame stays machines/tank.png, so
        // it can't use the single-texture-per-part generic renderer. (Base yaw 180°; see FluidTankRenderer.)
        regCustom(sink, ModBlockEntities.FLUID_TANK.get(), ctx -> new FluidTankRenderer());

        // Battery socket: draws the cradle + the inserted battery (per-variant texture).
        regCustom(sink, ModBlockEntities.BATTERY_SOCKET.get(), ctx -> new BatterySocketRenderer());

        // Crucible: basin OBJ + molten pool surface + the two pour streams.
        regCustom(sink, ModBlockEntities.MACHINE_CRUCIBLE.get(), ctx -> new CrucibleRenderer());

        // Firebox: body + door + the burning/empty interior.
        regCustom(sink, ModBlockEntities.HEATER_FIREBOX.get(), ctx -> new FireboxRenderer<>());

        // Electric heater: a static OBJ (the old CE fork used a static baked model).
        reg(sink, ModBlockEntities.HEATER_ELECTRIC.get(), 90F, 1F,
                new ModelDef(obj("machines/electric_heater.obj"), tex("machines/electric_heater.png")));

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
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/" + path);
    }

    private static Identifier tex(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/" + path);
    }
}
