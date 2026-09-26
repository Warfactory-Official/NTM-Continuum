package com.hbm.api.content;

import com.hbm.scripting.NTMEvents;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.api.content.recipes.*;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * {@code NTMEvents.recipes}: one field per machine. Edits apply when that machine's table first
 * builds, identically on client and server; each field's class documents its calls. Recipe ids are
 * {@code hbm:<machine>/<key>} with the machine in snake case ({@code chemicalPlant -> chemical_plant}).
 *
 * <pre>
 * e.remove('hbm:shredder/minecraft.stone')                   any machine, by id
 * e.removeIf('centrifuge', r => r.hasInput('#c:ores/iron'))  r: RecipeView
 * e.removeIf(r => r.hasOutput('minecraft:diamond'))          every machine
 * </pre>
 */
public final class RecipesEvent {

    private final Map<String, TableHandler> tables = new LinkedHashMap<>();

    public final GenericHandler<GenericBuilder> assembly = GenericHandler.of(AssemblyMachineRecipes.INSTANCE);
    public final GenericHandler<GenericBuilder> precisionAssembler = GenericHandler.of(PrecAssRecipes.INSTANCE);
    public final GenericHandler<GenericBuilder> chemicalPlant = GenericHandler.of(ChemicalPlantRecipes.INSTANCE);
    public final GenericHandler<GenericBuilder> purex = GenericHandler.of(PUREXRecipes.INSTANCE);
    public final GenericHandler<GenericBuilder> fluidBreeder = GenericHandler.of(FluidBreederRecipes.INSTANCE);
    public final GenericHandler<GenericBuilder> blastFurnace = GenericHandler.of(BlastFurnaceRecipesNT.INSTANCE);
    public final GenericHandler<GenericBuilder> alloyFurnace = GenericHandler.of(DiFurnaceRecipes.INSTANCE);
    public final GenericHandler<FusionBuilder> fusion = new GenericHandler<>(FusionRecipes.INSTANCE, FusionBuilder::new);
    public final GenericHandler<PlasmaForgeBuilder> plasmaForge = new GenericHandler<>(PlasmaForgeRecipes.INSTANCE, PlasmaForgeBuilder::new);
    public final GenericHandler<OutgasserBuilder> outgasser = new GenericHandler<>(OutgasserRecipes.INSTANCE, OutgasserBuilder::new);
    public final GenericHandler<CrucibleBuilder> crucible = new GenericHandler<>(CrucibleRecipes.INSTANCE, CrucibleBuilder::new);

    public final MixerHandler mixer = new MixerHandler();
    public final ExposureChamberHandler exposureChamber = new ExposureChamberHandler();
    public final AmmoPressHandler ammoPress = new AmmoPressHandler();
    public final AnnihilatorHandler annihilator = new AnnihilatorHandler();
    public final AnvilHandler anvil = new AnvilHandler();
    public final ArcWelderHandler arcWelder = new ArcWelderHandler();
    public final ArcFurnaceHandler arcFurnace = new ArcFurnaceHandler();
    public final BreederHandler breeder = new BreederHandler();
    public final CentrifugeHandler centrifuge = new CentrifugeHandler();
    public final CokerHandler coker = new CokerHandler();
    public final CombinationHandler combinationOven = new CombinationHandler();
    public final CompressorHandler compressor = new CompressorHandler();
    public final CoolingHandler cooling = new CoolingHandler();
    public final CrackingHandler cracking = new CrackingHandler();
    public final CrystallizerHandler crystallizer = new CrystallizerHandler();
    public final CyclotronHandler cyclotron = new CyclotronHandler();
    public final ElectrolyserFluidHandler electrolyserFluid = new ElectrolyserFluidHandler();
    public final ElectrolyserMetalHandler electrolyserMetal = new ElectrolyserMetalHandler();
    public final CombustionHandler combustion = new CombustionHandler();
    public final FractionHandler fractionTower = new FractionHandler();
    public final GasCentrifugeHandler gasCentrifuge = new GasCentrifugeHandler();
    public final HeatHandler heating = new HeatHandler();
    public final HydrotreatingHandler hydrotreater = new HydrotreatingHandler();
    public final LiquefactionHandler liquefactor = new LiquefactionHandler();
    public final ParticleAcceleratorHandler particleAccelerator = new ParticleAcceleratorHandler();
    public final PressHandler press = new PressHandler();
    public final PyroOvenHandler pyroOven = new PyroOvenHandler();
    public final RadiolysisHandler radiolysis = new RadiolysisHandler();
    public final RefineryHandler refinery = new RefineryHandler();
    public final ReformingHandler reformer = new ReformingHandler();
    public final RotaryFurnaceHandler rotaryFurnace = new RotaryFurnaceHandler();
    public final SawmillHandler sawmill = new SawmillHandler();
    public final ShredderHandler shredder = new ShredderHandler();
    public final SILEXHandler silex = new SILEXHandler();
    public final SolderingHandler soldering = new SolderingHandler();
    public final SolidificationHandler solidifier = new SolidificationHandler();
    public final StorageDrumHandler storageDrum = new StorageDrumHandler();

    RecipesEvent() {
        for (Field field : RecipesEvent.class.getFields()) {
            if (!TableHandler.class.isAssignableFrom(field.getType())) continue;
            try {
                TableHandler table = (TableHandler) field.get(this);
                String machine = snake(field.getName());
                table.bind(machine);
                tables.put(machine, table);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    Map<String, TableHandler> tables() {
        return tables;
    }

    /** Every machine's handler by name, for queries outside the recipes phase. */
    public static Map<String, TableHandler> handlers() {
        return new RecipesEvent().tables;
    }

    public void remove(String id) {
        table(machineOf(id)).removeById(id);
    }

    public void removeIf(String machine, Predicate<RecipeView> filter) {
        table(machine).removeIf(filter);
    }

    public void removeIf(Predicate<RecipeView> filter) {
        tables.values().forEach(table -> table.removeIf(filter));
    }

    private TableHandler table(String machine) {
        TableHandler table = tables.get(machine);
        if (table == null) throw new IllegalArgumentException("Unknown machine " + machine + "; known: " + tables.keySet());
        return table;
    }

    public static String machineOf(String id) {
        int colon = id.indexOf(':');
        int slash = id.indexOf('/', colon + 1);
        if (colon < 0 || slash < 0) throw new IllegalArgumentException("Not a recipe id: " + id);
        return id.substring(colon + 1, slash);
    }

    private static String snake(String camel) {
        StringBuilder out = new StringBuilder();
        for (char c : camel.toCharArray()) {
            if (Character.isUpperCase(c)) out.append('_').append(Character.toLowerCase(c));
            else out.append(c);
        }
        return out.toString();
    }
}
