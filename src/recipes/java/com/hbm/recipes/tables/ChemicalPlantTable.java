package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModGlassBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumPartType;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.items.machine.ItemFluidContainerNT;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.material.FuelAdditiveItems;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import dev.golem.core.Ids;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import com.hbm.items.ModMachineItems;

/**
 * 1.7 {@code ChemicalPlantRecipes.registerDefaults}. Where 1.7 chose a value by a config switch at registration (LBSM
 * simple chemistry, 528 pressurized recipes), both rows are written, gated by {@code requires}: the default keeps its id,
 * the other is {@code <id>.<flag>} at the same position.
 */
public final class ChemicalPlantTable extends GenericTableSource<GenericRecipe> {

    // 1.7 OreDictManager names the port's OreDictManager has no alias for
    private static final NTMMaterial COAL = Mats.MAT_COAL;
    private static final NTMMaterial COLTAN = new NTMMaterial(-1, "coltan");
    private static final TagKey<Item> ANY_COKE = tag("c", "cokes");
    private static final TagKey<Item> ANY_TAR = tag("c", "tars");
    /** 1.7 {@code KEY_SAND}, the "sand" ore name: {@code c:sands} (sand and red sand, not suspicious sand). */
    private static final TagKey<Item> KEY_SAND = tag("c", "sands");
    private static final TagKey<Item> KEY_ANYGLASS = tag("c", "glass_blocks");
    private static final TagKey<Item> KEY_ANYPANE = tag("c", "glass_panes");
    private static final TagKey<Item> KEY_GLYPHID_MEAT = tag("c", "glyphid_meats");

    private static final String LBSM_CHEMISTRY = "lbsm_simple_chemistry";
    private static final String PRESSURIZED = "enable_528_pressurized";

    public ChemicalPlantTable() {
        super(ChemicalPlantRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {

        /// REGULAR FLUIDS ///
        this.register(new GenericRecipe("chem.hydrogen").setupNamed(20, 400).setIcon(gasFull(Fluids.HYDROGEN))
                .inputItems(COAL.gem())
                .inputFluids(new FluidStack(Fluids.WATER, 8_000))
                .outputFluids(new FluidStack(Fluids.HYDROGEN, 500)));

        this.register(new GenericRecipe("chem.hydrogencoke").setupNamed(20, 400).setIcon(gasFull(Fluids.HYDROGEN))
                .inputItems(new TagStack(ANY_COKE).count(1))
                .inputFluids(new FluidStack(Fluids.WATER, 8_000))
                .outputFluids(new FluidStack(Fluids.HYDROGEN, 500)));

        this.register(new GenericRecipe("chem.oxygen").setupNamed(20, 400).setIcon(gasFull(Fluids.OXYGEN))
                .inputFluids(new FluidStack(Fluids.AIR, 8_000))
                .outputFluids(new FluidStack(Fluids.OXYGEN, 500)));

        this.register(new GenericRecipe("chem.xenon").setupNamed(300, 1_000).setIcon(gasFull(Fluids.XENON))
                .inputFluids(new FluidStack(Fluids.AIR, 16_000))
                .outputFluids(new FluidStack(Fluids.XENON, 50)));

        this.register(new GenericRecipe("chem.xenonoxy").setupNamed(20, 1_000).setIcon(gasFull(Fluids.XENON))
                .inputFluids(new FluidStack(Fluids.AIR, 8_000), new FluidStack(Fluids.OXYGEN, 250))
                .outputFluids(new FluidStack(Fluids.XENON, 50)).setPools(GenericRecipes.POOL_PREFIX_ALT + ".xenonoxy"));

        this.register(new GenericRecipe("chem.helium3").setupNamed(25, 2_000).setIcon(gasFull(Fluids.HELIUM3))
                .inputItems(new ComparableStack(ModFallingBlocks.moon_turf.get().asItem(), 1))
                .outputFluids(new FluidStack(Fluids.HELIUM3, 500)));

        this.register(new GenericRecipe("chem.co2").setup(60, 100)
                .inputFluids(new FluidStack(Fluids.GAS, 1_000))
                .outputFluids(new FluidStack(Fluids.CARBONDIOXIDE, 1_000)));

        this.register(new GenericRecipe("chem.perfluoromethyl").setup(20, 100)
                .inputItems(F.dust())
                .inputFluids(new FluidStack(Fluids.PETROLEUM, 1_000), new FluidStack(Fluids.UNSATURATEDS, 500))
                .outputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 1_000)));

        this.register(new GenericRecipe("chem.cccentrifuge").setup(200, 100)
                .inputFluids(new FluidStack(Fluids.CHLOROCALCITE_CLEANED, 500), new FluidStack(Fluids.SULFURIC_ACID, 8_000))
                .outputFluids(new FluidStack(Fluids.POTASSIUM_CHLORIDE, 250), new FluidStack(Fluids.CALCIUM_CHLORIDE, 250)));

        this.register(new GenericRecipe("chem.ticl4").setup(60, 500)
                .inputItems(ILMENITE.ingot().count(4), new TagStack(ANY_COKE).count(1))
                .inputFluids(new FluidStack(Fluids.CHLORINE, 500))
                .outputFluids(new FluidStack(Fluids.TITANIUM_TETRACHLORIDE, 4_000)));

        /// OILS ///
        this.register(new GenericRecipe("chem.ethanol").setupNamed(50, 100).setIcon(canisterFull(Fluids.ETHANOL))
                .inputItems(new ComparableStack(Items.SUGAR, 10))
                .outputFluids(new FluidStack(Fluids.ETHANOL, 1000)));

        this.register(new GenericRecipe("chem.biogas").setupNamed(60, 100).setIcon(gasFull(Fluids.BIOGAS))
                .inputItems(new ComparableStack(ModFuelItems.biomass.get(), 16))
                .inputFluids(new FluidStack(Fluids.AIR, 4_000))
                .outputFluids(new FluidStack(Fluids.BIOGAS, 2_000)));

        this.register(new GenericRecipe("chem.biofuel").setupNamed(60, 100).setIcon(canisterFull(Fluids.BIOFUEL))
                .inputFluids(new FluidStack(Fluids.BIOGAS, 1_500), new FluidStack(Fluids.ETHANOL, 250))
                .outputFluids(new FluidStack(Fluids.BIOFUEL, 1_000)));

        this.register(new GenericRecipe("chem.reoil").setupNamed(40, 100).setIcon(canisterFull(Fluids.RECLAIMED))
                .inputFluids(new FluidStack(Fluids.SMEAR, 1_000))
                .outputFluids(new FluidStack(Fluids.RECLAIMED, 800)));

        this.register(new GenericRecipe("chem.gasoline").setupNamed(40, 100).setIcon(canisterFull(Fluids.GASOLINE))
                .inputFluids(new FluidStack(Fluids.NAPHTHA, 1000))
                .outputFluids(new FluidStack(Fluids.GASOLINE, 800)));

        this.register(new GenericRecipe("chem.coallube").setupNamed(40, 100).setIcon(canisterFull(Fluids.LUBRICANT))
                .inputFluids(new FluidStack(Fluids.COALCREOSOTE, 1_000))
                .outputFluids(new FluidStack(Fluids.LUBRICANT, 1_000)).setPools(GenericRecipes.POOL_PREFIX_ALT + ".lube"));

        this.register(new GenericRecipe("chem.heavylube").setupNamed(40, 100).setIcon(canisterFull(Fluids.LUBRICANT))
                .inputFluids(new FluidStack(Fluids.HEAVYOIL, 2_000))
                .outputFluids(new FluidStack(Fluids.LUBRICANT, 1_000)).setPools(GenericRecipes.POOL_PREFIX_ALT + ".lube"));

        this.register(new GenericRecipe("chem.tarsand").setupNamed(200, 100).setIcon(new ItemStack(ModFallingBlocks.ore_oil_sand.get().asItem()))
                .inputItems(new ComparableStack(ModFallingBlocks.ore_oil_sand.get().asItem(), 16), new TagStack(ANY_TAR).count(1))
                .outputItems(new ItemStack(Blocks.SAND.asItem(), 16))
                .outputFluids(new FluidStack(Fluids.BITUMEN, 1_000)));

        this.register(new GenericRecipe("chem.tel").setup(40, 100)
                .inputItems(new TagStack(ANY_TAR), PB.dust())
                .inputFluids(new FluidStack(Fluids.PETROLEUM, 100), new FluidStack(Fluids.STEAM, 1000))
                .outputItems(new ItemStack(FuelAdditiveItems.get(FuelAdditiveItems.Variant.ANTIKNOCK))));

        this.register(new GenericRecipe("chem.deicer").setup(40, 100)
                .inputFluids(new FluidStack(Fluids.GAS, 100), new FluidStack(Fluids.HYDROGEN, 50))
                .outputItems(new ItemStack(FuelAdditiveItems.get(FuelAdditiveItems.Variant.DEICER))));

        /// THE CONC AND ASPHALE ///
        this.register(new GenericRecipe("chem.cobble").setup(20, 100)
                .inputFluids(new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 25))
                .outputItems(new ItemStack(Blocks.COBBLESTONE.asItem())));
        this.register(new GenericRecipe("chem.stone").setup(60, 500).setPools(GenericRecipes.POOL_PREFIX_DISCOVER + ".stone")
                .inputFluids(new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 25), new FluidStack(Fluids.AIR, 4_000))
                .outputItems(new ItemStack(Blocks.STONE.asItem())));
        this.register(new GenericRecipe("chem.obsidian").setup(60, 500).setPools(GenericRecipes.POOL_PREFIX_DISCOVER + ".stone")
                .inputFluids(new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 500), new FluidStack(Fluids.AIR, 4_000))
                .outputItems(new ItemStack(Blocks.OBSIDIAN.asItem())));
        this.register(new GenericRecipe("chem.aggregate").setupNamed(320, 500).setPools(GenericRecipes.POOL_PREFIX_DISCOVER + ".stone")
                .inputItems(new ComparableStack(Blocks.COBBLESTONE.asItem(), 16))
                .outputItems(new ItemStack(Blocks.GRAVEL.asItem(), 8), new ItemStack(Blocks.SAND.asItem(), 8)));
        this.register(new GenericRecipe("chem.concrete").setup(100, 100)
                .inputItems(new ComparableStack(ModFuelItems.powder_cement.get(), 1), new ComparableStack(Blocks.GRAVEL.asItem(), 8), new TagStack(KEY_SAND).count(8))
                .inputFluids(new FluidStack(Fluids.WATER, 2_000))
                .outputItems(new ItemStack(ModBlocks.concrete_smooth.get().asItem(), 16)));

        this.registerBranch(LBSM_CHEMISTRY, lbsm -> new GenericRecipe("chem.concreteasbestos").setup(100, 100)
                .inputItems(new ComparableStack(ModFuelItems.powder_cement.get(), 4), ASBESTOS.ingot().count(lbsm ? 1 : 4), new TagStack(KEY_SAND).count(8))
                .inputFluids(new FluidStack(Fluids.WATER, 2_000))
                .outputItems(new ItemStack(ModBlocks.concrete_asbestos.get().asItem(), 16)));

        this.register(new GenericRecipe("chem.ducrete").setup(150, 100)
                .inputItems(new ComparableStack(ModFuelItems.powder_cement.get(), 4), FERRO.ingot(), new TagStack(KEY_SAND).count(8))
                .inputFluids(new FluidStack(Fluids.WATER, 2_000))
                .outputItems(new ItemStack(ModHazardBlocks.ducrete_smooth.get().asItem(), 8)));

        this.register(new GenericRecipe("chem.liquidconk").setup(100, 100)
                .inputItems(new ComparableStack(ModFuelItems.powder_cement.get(), 1), new ComparableStack(Blocks.GRAVEL.asItem(), 8), new TagStack(KEY_SAND).count(8))
                .inputFluids(new FluidStack(Fluids.WATER, 2_000))
                .outputFluids(new FluidStack(Fluids.CONCRETE, 16_000)));

        this.register(new GenericRecipe("chem.asphalt").setup(100, 100)
                .inputItems(new ComparableStack(Blocks.GRAVEL.asItem(), 2), new TagStack(KEY_SAND).count(6))
                .inputFluids(new FluidStack(Fluids.BITUMEN, 1_000))
                .outputItems(new ItemStack(ModDecoContent.asphalt.get().asItem(), 16)));

        /// BATTERIES ///
        this.register(new GenericRecipe("chem.batterylead").setup(100, 100)
                .inputItems(STEEL.plate().count(4),
                        PB.ingot().count(4))
                .inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 8_000))
                .outputItems(new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LEAD).get(), 1)));
        this.register(new GenericRecipe("chem.batterylithium").setup(100, 1_000)
                .inputItems(LI.dust().count(12),
                        CO.dust().count(8),
                        ANY_PLASTIC.ingot().count(4))
                .inputFluids(new FluidStack(Fluids.OXYGEN, 2_000))
                .outputItems(new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LITHIUM).get(), 1)));
        this.register(new GenericRecipe("chem.batterysodium").setup(100, 10_000)
                .inputItems(NA.dust().count(24),
                        IRON.dust().count(24),
                        ANY_HARDPLASTIC.ingot().count(12))
                .outputItems(new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_SODIUM).get(), 1)));
        this.register(new GenericRecipe("chem.batteryschrabidium").setup(100, 25_000)
                .inputItems(SA326.dust().count(24),
                        ANY_BISMOIDBRONZE.plateCast().count(8))
                .inputFluids(new FluidStack(Fluids.HELIUM4, 8_000))
                .outputItems(new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_SCHRABIDIUM).get(), 1)));
        this.register(new GenericRecipe("chem.batteryquantum").setup(100, 100_000)
                .inputItems(BSCCO.wireDense().count(24),
                        new ComparableStack(ModItems.pellet_charged.get(), 32),
                        new ComparableStack(ModItems.ingot_cft.get(), 16))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 8_000))
                .outputItems(new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_QUANTUM).get(), 1))
                .outputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 8_000)));

        /// SOLIDS ///
        this.registerBranch(LBSM_CHEMISTRY, lbsm -> new GenericRecipe("chem.desh").setup(100, 100)
                .inputItems(new ComparableStack(ModItems.powder_desh_mix.get()))
                .inputFluids(lbsm ?
                                new FluidStack[] {new FluidStack(Fluids.LIGHTOIL, 200)} :
                                new FluidStack[] {new FluidStack(Fluids.LIGHTOIL, 200), new FluidStack(Fluids.MERCURY, 200)})
                .outputItems(new ItemStack(ModItems.ingot_desh.get())));
        this.registerBranch(LBSM_CHEMISTRY, lbsm -> new GenericRecipe("chem.deshcracked").setupNamed(100, 100)
                .inputItems(new ComparableStack(ModItems.powder_desh_mix.get()))
                .inputFluids(lbsm ?
                                new FluidStack[] {new FluidStack(Fluids.LIGHTOIL_CRACK, 500)} :
                                new FluidStack[] {new FluidStack(Fluids.LIGHTOIL_CRACK, 500, 1), new FluidStack(Fluids.MERCURY, 100)})
                .outputItems(new ItemStack(ModItems.ingot_desh.get())));

        this.registerBranch(PRESSURIZED, p528 -> new GenericRecipe("chem.polymer").setup(100, 100)
                .inputItems(COAL.dust().count(2), F.dust())
                .inputFluids(new FluidStack(Fluids.PETROLEUM, 1_000, p528 ? 1 : 0))
                .outputItems(new ItemStack(ModItems.ingot_polymer.get(), 4)));

        this.registerBranch(PRESSURIZED, p528 -> new GenericRecipe("chem.bakelite").setup(100, 100)
                .inputFluids(new FluidStack(Fluids.AROMATICS, 500, p528 ? 1 : 0), new FluidStack(Fluids.PETROLEUM, 500, p528 ? 1 : 0))
                .outputItems(new ItemStack(ModItems.ingot_bakelite.get())));

        this.registerBranch(PRESSURIZED, p528 -> new GenericRecipe("chem.rubber").setup(100, 200)
                .inputItems(S.dust())
                .inputFluids(new FluidStack(Fluids.UNSATURATEDS, 500, p528 ? 2 : 0))
                .outputItems(new ItemStack(ModItems.ingot_rubber.get(), 2)));

        this.registerBranch(PRESSURIZED, p528 -> new GenericRecipe("chem.hardplastic").setup(100, 1_000)
                .inputFluids(new FluidStack(Fluids.XYLENE, 500, p528 ? 2 : 0), new FluidStack(Fluids.PHOSGENE, 500, p528 ? 2 : 0))
                .outputItems(new ItemStack(ModItems.ingot_pc.get())));

        this.registerBranch(PRESSURIZED, p528 -> new GenericRecipe("chem.pvc").setup(100, 1_000)
                .inputItems(CD.dust())
                .inputFluids(new FluidStack(Fluids.UNSATURATEDS, 250, p528 ? 2 : 0), new FluidStack(Fluids.CHLORINE, 250, p528 ? 2 : 0))
                .outputItems(new ItemStack(ModItems.ingot_pvc.get(), 2)));

        this.registerBranch(PRESSURIZED, p528 -> new GenericRecipe("chem.kevlar").setup(60, 300)
                .inputFluids(new FluidStack(Fluids.AROMATICS, 200), new FluidStack(Fluids.NITRIC_ACID, 100), new FluidStack(p528 ? Fluids.PHOSGENE : Fluids.CHLORINE, 100))
                .outputItems(new ItemStack(ModItems.plate_kevlar.get(), 4)));

        this.register(new GenericRecipe("chem.lightconstruction").setupNamed(400, 25_000).outputItems(ModIngredientItems.part(EnumPartType.ALCLAD, 1))
                .inputItems(DURA.plate().count(2))
                .inputFluids(new FluidStack(Fluids.XYLENE, 1_000), new FluidStack(Fluids.PHOSGENE, 1_000), new FluidStack(Fluids.SOURGAS, 4_000)));
        this.register(new GenericRecipe("chem.carboncarbon").setupNamed(400, 100_000).outputItems(ModIngredientItems.part(EnumPartType.ALCLAD, 1))
                .inputItems(new TagStack(ANY_COKE).count(8), TI.plate().count(1))
                .inputFluids(new FluidStack(Fluids.HEAVYOIL_VACUUM, 1_000), new FluidStack(Fluids.SYNGAS, 8_000)));

        this.register(new GenericRecipe("chem.meth").setup(60, 300)
                .inputItems(new ComparableStack(Items.WHEAT), new ComparableStack(Items.COCOA_BEANS, 2))
                .inputFluids(new FluidStack(Fluids.LUBRICANT, 400), new FluidStack(Fluids.PEROXIDE, 500))
                .outputItems(new ItemStack(ModMedicineItems.chocolate.get(), 4)));

        this.register(new GenericRecipe("chem.epearl").setup(100, 300)
                .inputItems(DIAMOND.dust().count(1))
                .inputFluids(new FluidStack(Fluids.XPJUICE, 500))
                .outputFluids(new FluidStack(Fluids.ENDERJUICE, 100)));

        this.register(new GenericRecipe("chem.meatprocessing").setupNamed(200, 200).setIcon(new ItemStack(ModFoodItems.glyphid_meat.get()))
                .inputItems(new TagStack(KEY_GLYPHID_MEAT).count(3))
                .inputFluids(new FluidStack(Fluids.WATER, 1_000))
                .outputItems(new ItemStack(ModItems.sulfur.get(), 4), new ItemStack(ModItems.niter.get(), 3))
                .outputFluids(new FluidStack(Fluids.SALIENT, 250)));

        this.register(new GenericRecipe("chem.rustysteel").setup(40, 100)
                .inputItems(new ComparableStack(ModBlocks.deco_steel.get().asItem(), 8))
                .inputFluids(new FluidStack(Fluids.WATER, 1000))
                .outputItems(new ItemStack(ModBlocks.deco_rusty_steel.get().asItem(), 8)));

        this.register(new GenericRecipe("chem.biosolidfuel").setupNamed(40, 100)
                .inputItems(new ComparableStack(ModFuelItems.biomass_compressed.get(), 4))
                .outputItems(new ItemStack(ModFuelItems.solid_fuel.get(), 1)).setPools(GenericRecipes.POOL_PREFIX_ALT + ".biosolidfuel"));

        this.register(new GenericRecipe("chem.biooilsolidfuel").setupNamed(40, 100)
                .inputItems(new ComparableStack(ModFuelItems.biomass_compressed.get(), 2))
                .inputFluids(new FluidStack(Fluids.HEATINGOIL, 100))
                .outputItems(new ItemStack(ModFuelItems.solid_fuel.get(), 1)).setPools(GenericRecipes.POOL_PREFIX_ALT + ".biosolidfuel"));

        this.register(new GenericRecipe("chem.oilelectrodes").setupNamed(600, 100)
                .inputFluids(new FluidStack(Fluids.HEATINGOIL, 4_000))
                .outputItems(new ItemStack(ModMachineItems.arc_electrode.get(), 1)).setPools(GenericRecipes.POOL_PREFIX_ALT + ".electrodes"));

        this.register(new GenericRecipe("chem.lubeelectrodes").setupNamed(600, 100)
                .inputFluids(new FluidStack(Fluids.LUBRICANT, 8_000))
                .outputItems(new ItemStack(ModMachineItems.arc_electrode.get(), 1)).setPools(GenericRecipes.POOL_PREFIX_ALT + ".electrodes"));

        /// ACIDS ///
        this.register(new GenericRecipe("chem.peroxide").setup(50, 100)
                .inputFluids(new FluidStack(Fluids.WATER, 1_000))
                .outputFluids(new FluidStack(Fluids.PEROXIDE, 1_000)));

        this.register(new GenericRecipe("chem.sulfuricacid").setup(50, 100)
                .inputItems(S.dust())
                .inputFluids(new FluidStack(Fluids.PEROXIDE, 1_000), new FluidStack(Fluids.WATER, 1_000))
                .outputFluids(new FluidStack(Fluids.SULFURIC_ACID, 2_000)));

        this.register(new GenericRecipe("chem.nitricacid").setup(50, 100)
                .inputItems(KNO.dust())
                .inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 500))
                .outputFluids(new FluidStack(Fluids.NITRIC_ACID, 1_000)));

        this.register(new GenericRecipe("chem.birkeland").setupNamed(200, 5_000)
                .inputFluids(new FluidStack(Fluids.AIR, 8_000), new FluidStack(Fluids.WATER, 2_000))
                .outputFluids(new FluidStack(Fluids.NITRIC_ACID, 1_000)).setPools(GenericRecipes.POOL_PREFIX_ALT + ".birkeland"));

        this.register(new GenericRecipe("chem.schrabidic").setup(60, 5_000)
                .inputItems(new ComparableStack(ModItems.pellet_charged.get()))
                .inputFluids(new FluidStack(Fluids.SAS3, 2000), new FluidStack(Fluids.PEROXIDE, 2000))
                .outputFluids(new FluidStack(Fluids.SCHRABIDIC, 2000)));

        this.register(new GenericRecipe("chem.schrabidate").setup(150, 5_000)
                .inputItems(IRON.dust())
                .inputFluids(new FluidStack(Fluids.SCHRABIDIC, 250))
                .outputItems(new ItemStack(ModItems.powder_schrabidate.get())));

        /// COLTAN ///
        this.register(new GenericRecipe("chem.coltancleaning").setup(60, 100)
                .inputItems(COLTAN.dust().count(2), COAL.dust())
                .inputFluids(new FluidStack(Fluids.PEROXIDE, 250), new FluidStack(Fluids.HYDROGEN, 500))
                .outputItems(new ItemStack(ModItems.powder_coltan.get()), new ItemStack(ModItems.powder_niobium.get()), new ItemStack(ModItems.dust.get()))
                .outputFluids(new FluidStack(Fluids.WATER, 500)));

        this.register(new GenericRecipe("chem.coltanpain").setup(120, 100)
                .inputItems(new ComparableStack(ModItems.powder_coltan.get()), F.dust())
                .inputFluids(new FluidStack(Fluids.GAS, 1000), new FluidStack(Fluids.OXYGEN, 500))
                .outputFluids(new FluidStack(Fluids.PAIN, 1000)));

        this.register(new GenericRecipe("chem.coltancrystal").setup(80, 100)
                .inputFluids(new FluidStack(Fluids.PAIN, 1000), new FluidStack(Fluids.PEROXIDE, 500))
                .outputItems(new ItemStack(ModItems.gem_tantalium.get()), new ItemStack(ModItems.dust.get(), 3))
                .outputFluids(new FluidStack(Fluids.WATER, 250)));

        /// EXPLOSIVES ///
        this.registerBranch(LBSM_CHEMISTRY, lbsm -> new GenericRecipe("chem.cordite").setup(40, 100)
                .inputItems(KNO.dust().count(2), new ComparableStack(ModItems.powder_sawdust.get(), 2))
                .inputFluids(lbsm ? new FluidStack(Fluids.HEATINGOIL, 200) : new FluidStack(Fluids.GAS, 200))
                .outputItems(new ItemStack(ModItems.cordite.get(), 4)));

        this.registerBranch(PRESSURIZED, p528 -> new GenericRecipe("chem.rocketfuel").setup(200, 100)
                .inputItems(new ComparableStack(ModFuelItems.solid_fuel.get(), 2))
                .inputFluids(new FluidStack(Fluids.PETROLEUM, 200, p528 ? 1 : 0), new FluidStack(Fluids.NITRIC_ACID, 100))
                .outputItems(new ItemStack(ModFuelItems.rocket_fuel.get(), 4)));

        this.register(new GenericRecipe("chem.dynamite").setup(50, 100)
                .inputItems(new ComparableStack(Items.SUGAR), KNO.dust(), new TagStack(KEY_SAND))
                .outputItems(new ItemStack(ModItems.ball_dynamite.get(), 2)));

        this.registerBranch(PRESSURIZED, p528 -> new GenericRecipe("chem.tnt").setup(100, 1_000)
                .inputItems(KNO.dust())
                .inputFluids(new FluidStack(Fluids.AROMATICS, 500, p528 ? 1 : 0))
                .outputItems(new ItemStack(ModItems.ball_tnt.get(), 4)));

        this.register(new GenericRecipe("chem.tatb").setup(50, 5_000)
                .inputItems(new ComparableStack(ModItems.ball_tnt.get()))
                .inputFluids(new FluidStack(Fluids.SOURGAS, 200, 1), new FluidStack(Fluids.NITRIC_ACID, 10))
                .outputItems(new ItemStack(ModItems.ball_tatb.get())));

        this.registerBranch(PRESSURIZED, p528 -> new GenericRecipe("chem.c4").setup(100, 1_000)
                .inputItems(KNO.dust())
                .inputFluids(new FluidStack(Fluids.UNSATURATEDS, 500, p528 ? 1 : 0))
                .outputItems(new ItemStack(ModFuelItems.ingot_c4.get(), 4)));

        this.register(new GenericRecipe("chem.napalm").setup(40, 100)
                .inputItems(new ComparableStack(ModItems.canister_empty.get()))
                .inputFluids(new FluidStack(Fluids.GASOLINE, 100), new FluidStack(Fluids.AROMATICS, 50)) // aromatics to emulate polystyrene
                .outputItems(new ItemStack(ModItems.canister_napalm.get(), 1)));

        /// GLASS ///
        this.register(new GenericRecipe("chem.laminate").setup(20, 100)
                .inputFluids(new FluidStack(Fluids.XYLENE, 50), new FluidStack(Fluids.PHOSGENE, 50))
                .inputItems(new TagStack(KEY_ANYGLASS), STEEL.bolt().count(4))
                .outputItems(new ItemStack(ModGlassBlocks.reinforced_laminate.get().asItem())));

        this.register(new GenericRecipe("chem.polarized").setup(100, 500)
                .inputFluids(new FluidStack(Fluids.PETROLEUM, 1_000))
                .inputItems(new TagStack(KEY_ANYPANE))
                .outputItems(new ItemStack(ModIngredientItems.part(EnumPartType.GLASS_POLARIZED), 16)));

        /// NUCLEAR PROCESSING ///
        this.register(new GenericRecipe("chem.yellowcake").setup(250, 500)
                .inputItems(U.billet().count(2), S.dust().count(2))
                .inputFluids(new FluidStack(Fluids.PEROXIDE, 500))
                .outputItems(new ItemStack(ModItems.powder_yellowcake.get())));

        this.register(new GenericRecipe("chem.uf6").setup(100, 500).setIcon(ItemFluidIcon.make(Fluids.UF6))
                .inputItems(new ComparableStack(ModItems.powder_yellowcake.get()), F.dust().count(4))
                .inputFluids(new FluidStack(Fluids.WATER, 1_000))
                .outputItems(new ItemStack(ModItems.sulfur.get(), 2))
                .outputFluids(new FluidStack(Fluids.UF6, 1_200)));

        this.register(new GenericRecipe("chem.puf6").setup(200, 500)
                .inputItems(PU.dust(), F.dust().count(3))
                .inputFluids(new FluidStack(Fluids.WATER, 1_000))
                .outputFluids(new FluidStack(Fluids.PUF6, 900)));

        this.register(new GenericRecipe("chem.sas3").setup(200, 5_000)
                .inputItems(SA326.dust(), S.dust().count(2))
                .inputFluids(new FluidStack(Fluids.PEROXIDE, 2_000))
                .outputFluids(new FluidStack(Fluids.SAS3, 1_000)));

        this.register(new GenericRecipe("chem.balefire").setup(100, 10_000).setIcon(ItemFluidIcon.make(Fluids.BALEFIRE))
                .inputItems(new ComparableStack(ModItems.egg_balefire_shard.get()))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 6_000))
                .outputItems(new ItemStack(ModItems.powder_balefire.get()))
                .outputFluids(new FluidStack(Fluids.BALEFIRE, 8_000)));

        this.register(new GenericRecipe("chem.dhc").setup(400, 500).setIcon(ItemFluidIcon.make(Fluids.DHC))
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 500), new FluidStack(Fluids.REFORMGAS, 250), new FluidStack(Fluids.SYNGAS, 250))
                .outputFluids(new FluidStack(Fluids.DHC, 500)));

        /// OSMIRIDIUM ///
        this.register(new GenericRecipe("chem.osmiridiumdeath").setup(240, 1_000)
                .inputItems(new ComparableStack(ModItems.powder_paleogenite.get()), F.dust().count(8), new ComparableStack(ModItems.nugget_bismuth.get(), 4))
                .inputFluids(new FluidStack(Fluids.PEROXIDE, 1_000, 5))
                .outputFluids(new FluidStack(Fluids.DEATH, 1_000, 0)));
    }

    /**
     * 1.7 {@code this.register(...)} of a row with a {@code flag ? a : b} inside: {@code row.apply(false)} under
     * {@code !flag} with the row's id, {@code row.apply(true)} under {@code flag} as {@code <id>.<flag>}, one position.
     */
    private void registerBranch(String flag, Function<Boolean, GenericRecipe> row) {
        GenericRecipe off = row.apply(false).requires("!" + flag);
        this.register(off);
        GenericRecipe on = row.apply(true).requires(flag);
        on.order = off.order;
        add(id(on) + "." + flag, on);
    }

    /** 1.7 {@code new ItemStack(ModItems.gas_full, 1, fluid.getID())}: a full gas tank of the fluid. */
    private static ItemStack gasFull(FluidType fluid) {
        ItemFluidContainerNT tank = ModFluidNetItems.gas_full.get();
        return tank.make(fluid, tank.capacity);
    }

    /** 1.7 {@code new ItemStack(ModItems.canister_full, 1, fluid.getID())}: a full canister of the fluid. */
    private static ItemStack canisterFull(FluidType fluid) {
        ItemFluidContainerNT canister = ModFluidNetItems.canister_full.get();
        return canister.make(fluid, canister.capacity);
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Ids.of(namespace, path));
    }
}
