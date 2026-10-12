package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.CHLOROCALCITE;
import static com.hbm.inventory.OreDictManager.DIAMOND;
import static com.hbm.inventory.OreDictManager.F;
import static com.hbm.inventory.OreDictManager.KNO;
import static com.hbm.inventory.OreDictManager.S;
import static com.hbm.inventory.OreDictManager.TH232;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.items.material.FuelAdditiveItems;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import java.util.Locale;
import net.minecraft.world.item.Items;

/**
 * 1.7 {@code MixerRecipes.registerDefaults}. 1.7 keeps an array of recipes per output fluid; the port writes each as a
 * row named {@code mixer.<output>.<index in that array>}.
 */
public final class MixerTable extends GenericTableSource<GenericRecipe> {

    public MixerTable() {
        super(MixerRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        register(Fluids.COOLANT, new MixerRecipe(2_000, 50).setStack1(new FluidStack(Fluids.WATER, 1_800)).setSolid(KNO.dust()));
        register(Fluids.CRYOGEL, new MixerRecipe(2_000, 50).setStack1(new FluidStack(Fluids.COOLANT, 1_800)).setSolid(new ComparableStack(ModItems.powder_ice.get())));
        register(Fluids.NITAN, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.KEROSENE, 600)).setStack2(new FluidStack(Fluids.MERCURY, 200)).setSolid(new ComparableStack(ModItems.powder_nitan_mix.get())));
        register(Fluids.FRACKSOL,
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.SULFURIC_ACID, 900)).setStack2(new FluidStack(Fluids.PETROLEUM, 100)),
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.WATER, 1000)).setStack2(new FluidStack(Fluids.PETROLEUM, 100)).setSolid(S.dust()));
        register(Fluids.ENDERJUICE, new MixerRecipe(100, 100).setStack1(new FluidStack(Fluids.XPJUICE, 500)).setSolid(DIAMOND.dust()));
        register(Fluids.SALIENT, new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.SEEDSLURRY, 500)).setStack2(new FluidStack(Fluids.BLOOD, 500)));
        register(Fluids.COLLOID, new MixerRecipe(500, 20).setStack1(new FluidStack(Fluids.WATER, 500)).setSolid(new ComparableStack(ModItems.dust.get())));
        register(Fluids.PHOSGENE, new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.UNSATURATEDS, 500)).setStack2(new FluidStack(Fluids.CHLORINE, 500)));
        register(Fluids.MUSTARDGAS, new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.REFORMGAS, 750)).setStack2(new FluidStack(Fluids.CHLORINE, 250)).setSolid(S.dust()));
        register(Fluids.IONGEL, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.WATER, 1000)).setStack2(new FluidStack(Fluids.HYDROGEN, 200)).setSolid(new ComparableStack(ModItems.pellet_charged.get())));
        register(Fluids.EGG, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.RADIOSOLVENT, 500)).setSolid(new ComparableStack(Items.EGG)));
        // 1.7 Items.fish at any meta: the raw fishes tag
        register(Fluids.FISHOIL, new MixerRecipe(100, 50).setSolid(new TagStack(MixerRecipes.RAW_FISHES)));
        // 1.7 Blocks.double_plant meta 0: the sunflower
        register(Fluids.SUNFLOWEROIL, new MixerRecipe(100, 50).setSolid(new ComparableStack(Items.SUNFLOWER)));
        register(Fluids.FULLERENE, new MixerRecipe(250, 50).setStack1(new FluidStack(Fluids.RADIOSOLVENT, 500)).setSolid(new ComparableStack(ModIngredientItems.ash(EnumAshType.SOOT))));

        register(Fluids.SOLVENT,
                new MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.NAPHTHA, 500)).setStack2(new FluidStack(Fluids.AROMATICS, 500)),
                new MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.NAPHTHA_CRACK, 500)).setStack2(new FluidStack(Fluids.AROMATICS, 500)),
                new MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.NAPHTHA_DS, 500)).setStack2(new FluidStack(Fluids.AROMATICS, 500)),
                new MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.NAPHTHA_COKER, 500)).setStack2(new FluidStack(Fluids.AROMATICS, 500)));
        register(Fluids.SULFURIC_ACID, new MixerRecipe(500, 50).setStack1(new FluidStack(Fluids.PEROXIDE, 800)).setSolid(S.dust()));
        register(Fluids.NITRIC_ACID, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.SULFURIC_ACID, 500)).setSolid(KNO.dust()));
        register(Fluids.RADIOSOLVENT, new MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.REFORMGAS, 750)).setStack2(new FluidStack(Fluids.CHLORINE, 250)));
        register(Fluids.SCHRABIDIC, new MixerRecipe(16_000, 100).setStack1(new FluidStack(Fluids.SAS3, 8_000)).setStack2(new FluidStack(Fluids.PEROXIDE, 6_000)).setSolid(new ComparableStack(ModItems.pellet_charged.get())));

        register(Fluids.PETROIL, new MixerRecipe(1_000, 30).setStack1(new FluidStack(Fluids.RECLAIMED, 800)).setStack2(new FluidStack(Fluids.LUBRICANT, 200)));
        register(Fluids.LUBRICANT,
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.HEATINGOIL, 500)).setStack2(new FluidStack(Fluids.UNSATURATEDS, 500)),
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.FISHOIL, 800)).setStack2(new FluidStack(Fluids.ETHANOL, 200)),
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.SUNFLOWEROIL, 800)).setStack2(new FluidStack(Fluids.ETHANOL, 200)));
        register(Fluids.BIOFUEL,
                new MixerRecipe(250, 20).setStack1(new FluidStack(Fluids.FISHOIL, 500)).setStack2(new FluidStack(Fluids.WOODOIL, 500)),
                new MixerRecipe(200, 20).setStack1(new FluidStack(Fluids.SUNFLOWEROIL, 500)).setStack2(new FluidStack(Fluids.WOODOIL, 500)));
        register(Fluids.NITROGLYCERIN,
                new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.PETROLEUM, 1_000)).setStack2(new FluidStack(Fluids.NITRIC_ACID, 1_000)),
                new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.FISHOIL, 500)).setStack2(new FluidStack(Fluids.NITRIC_ACID, 500)));

        register(Fluids.THORIUM_SALT, new MixerRecipe(1_000, 30).setStack1(new FluidStack(Fluids.CHLORINE, 1000)).setSolid(TH232.dust()));

        register(Fluids.SYNGAS, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.COALOIL, 500)).setStack2(new FluidStack(Fluids.STEAM, 500)));
        register(Fluids.OXYHYDROGEN,
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.HYDROGEN, 500)).setStack2(new FluidStack(Fluids.AIR, 2_000)),
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.HYDROGEN, 500)).setStack2(new FluidStack(Fluids.OXYGEN, 500)));

        // 1.7 fuel_additive meta 0: the antiknock additive
        register(Fluids.PETROIL_LEADED, new MixerRecipe(12_000, 40).setStack1(new FluidStack(Fluids.PETROIL, 10_000)).setSolid(new ComparableStack(antiknock())));
        register(Fluids.GASOLINE_LEADED, new MixerRecipe(12_000, 40).setStack1(new FluidStack(Fluids.GASOLINE, 10_000)).setSolid(new ComparableStack(antiknock())));
        register(Fluids.COALGAS_LEADED, new MixerRecipe(12_000, 40).setStack1(new FluidStack(Fluids.COALGAS, 10_000)).setSolid(new ComparableStack(antiknock())));

        register(Fluids.DIESEL_REFORM, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.DIESEL, 900)).setStack2(new FluidStack(Fluids.REFORMATE, 100)));
        register(Fluids.DIESEL_CRACK_REFORM, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.DIESEL_CRACK, 900)).setStack2(new FluidStack(Fluids.REFORMATE, 100)));
        register(Fluids.KEROSENE_REFORM, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.KEROSENE, 900)).setStack2(new FluidStack(Fluids.REFORMATE, 100)));

        register(Fluids.CHLOROCALCITE_SOLUTION, new MixerRecipe(500, 50).setStack1(new FluidStack(Fluids.WATER, 250)).setStack2(new FluidStack(Fluids.NITRIC_ACID, 250)).setSolid(CHLOROCALCITE.dust()));
        register(Fluids.CHLOROCALCITE_MIX, new MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.CHLOROCALCITE_SOLUTION, 500)).setStack2(new FluidStack(Fluids.SULFURIC_ACID, 500)).setSolid(new ComparableStack(ModItems.powder_flux.get())));
        register(Fluids.PHEROMONE_M, new MixerRecipe(2000, 10).setStack1(new FluidStack(Fluids.PHEROMONE, 1500)).setStack2(new FluidStack(Fluids.BLOOD, 500)).setSolid(new ComparableStack(ModMedicineItems.pill_herbal.get())));

        register(Fluids.BAUXITE_SOLUTION, new MixerRecipe(300, 80).setStack1(new FluidStack(Fluids.LYE, 50)).setSolid(new ComparableStack(ModOreBlocks.stone_resource_bauxite.get().asItem())));
        register(Fluids.LYE, new MixerRecipe(100, 100).setStack1(new FluidStack(Fluids.WATER, 100)).setSolid(new ComparableStack(ModIngredientItems.ash(EnumAshType.WOOD))));
        register(Fluids.ALUMINA, new MixerRecipe(200, 40).setStack1(new FluidStack(Fluids.SODIUM_ALUMINATE, 150)).setSolid(F.dust().count(3)),
                                 new MixerRecipe(300, 40).setStack1(new FluidStack(Fluids.SODIUM_ALUMINATE, 150)).setSolid(new ComparableStack(ModIntermediateItems.chunk(EnumChunkType.CRYOLITE))));

        register(Fluids.PERFLUOROMETHYL, new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.PETROLEUM, 1000)).setStack2(new FluidStack(Fluids.UNSATURATEDS, 500)).setSolid(F.dust()));

        // 1.7 ANY_TAR.any(): the c:tars tag
        register(Fluids.BITUMEN, new MixerRecipe(50, 20).setSolid(new TagStack(PyroOvenRecipes.TARS)));
    }

    /** 1.7 {@code register(type, recipes...)}: one row per recipe, named by the output and the recipe's index. */
    private void register(FluidType type, MixerRecipe... rec) {
        for (int i = 0; i < rec.length; i++) {
            MixerRecipe entry = rec[i];
            GenericRecipe row = new GenericRecipe("mixer." + type.getName().toLowerCase(Locale.ROOT) + "." + i)
                    .setup(entry.processTime, 0)
                    .outputFluids(new FluidStack(type, entry.output));
            if (entry.solidInput != null) row.inputItems(entry.solidInput);
            if (entry.input1 != null && entry.input2 != null) row.inputFluids(entry.input1, entry.input2);
            else if (entry.input1 != null) row.inputFluids(entry.input1);
            else if (entry.input2 != null) row.inputFluids(entry.input2);
            register(row);
        }
    }

    private static net.minecraft.world.item.Item antiknock() {
        return FuelAdditiveItems.get(FuelAdditiveItems.Variant.ANTIKNOCK);
    }

    /** 1.7 {@code MixerRecipes.MixerRecipe}, the builder 1.7's lines use. */
    private static final class MixerRecipe {
        FluidStack input1;
        FluidStack input2;
        AStack solidInput;
        final int processTime;
        final int output;

        MixerRecipe(int output, int processTime) {
            this.output = output;
            this.processTime = processTime;
        }

        MixerRecipe setStack1(FluidStack stack) { input1 = stack; return this; }
        MixerRecipe setStack2(FluidStack stack) { input2 = stack; return this; }
        MixerRecipe setSolid(AStack stack) { solidInput = stack; return this; }
    }
}
