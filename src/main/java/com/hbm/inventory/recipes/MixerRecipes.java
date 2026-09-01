package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.blocks.ModOreBlocks;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The feasible portion of the legacy Mixer catalogue. Mixer selection is keyed by output fluid and
 * recipe index, so this keeps the old lookup shape while also exposing the recipes to the generic JEI
 * category. Inputs that depend on unported legacy metadata items remain deferred rather than replaced
 * with a different ingredient.
 */
public final class MixerRecipes extends GenericRecipes {

    public static final MixerRecipes INSTANCE = new MixerRecipes();
    public static final Map<FluidType, MixerRecipe[]> recipes = new LinkedHashMap<>();

    private MixerRecipes() {}

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        register(Fluids.COOLANT, recipe(2_000, 50).fluid1(Fluids.WATER, 1_800).solid(OreDictManager.KNO.dust()));
        register(Fluids.CRYOGEL, recipe(2_000, 50).fluid1(Fluids.COOLANT, 1_800).solid(item(ModItems.powder_ice.get())));
        register(Fluids.NITAN, recipe(1_000, 50).fluid1(Fluids.KEROSENE, 600).fluid2(Fluids.MERCURY, 200)
                .solid(item(ModItems.powder_nitan_mix.get())));
        register(Fluids.FRACKSOL,
                recipe(1_000, 20).fluid1(Fluids.SULFURIC_ACID, 900).fluid2(Fluids.PETROLEUM, 100),
                recipe(1_000, 20).fluid1(Fluids.WATER, 1_000).fluid2(Fluids.PETROLEUM, 100)
                        .solid(OreDictManager.S.dust()));
        register(Fluids.ENDERJUICE, recipe(100, 100).fluid1(Fluids.XPJUICE, 500).solid(item(ModItems.powder_diamond.get())));
        register(Fluids.SALIENT, recipe(1_000, 20).fluid1(Fluids.SEEDSLURRY, 500).fluid2(Fluids.BLOOD, 500));
        register(Fluids.COLLOID, recipe(500, 20).fluid1(Fluids.WATER, 500).solid(item(ModItems.dust.get())));
        register(Fluids.PHOSGENE, recipe(1_000, 20).fluid1(Fluids.UNSATURATEDS, 500).fluid2(Fluids.CHLORINE, 500));
        register(Fluids.MUSTARDGAS, recipe(1_000, 20).fluid1(Fluids.REFORMGAS, 750).fluid2(Fluids.CHLORINE, 250)
                .solid(OreDictManager.S.dust()));
        register(Fluids.IONGEL, recipe(1_000, 50).fluid1(Fluids.WATER, 1_000).fluid2(Fluids.HYDROGEN, 200)
                .solid(item(ModItems.pellet_charged.get())));
        register(Fluids.EGG, recipe(1_000, 50).fluid1(Fluids.RADIOSOLVENT, 500).solid(item(Items.EGG)));
        register(Fluids.FISHOIL, recipe(100, 50).solid(new TagStack(ItemTags.FISHES)));
        register(Fluids.SUNFLOWEROIL, recipe(100, 50).solid(item(Blocks.SUNFLOWER.asItem())));
        register(Fluids.FULLERENE, recipe(250, 50).fluid1(Fluids.RADIOSOLVENT, 500)
                .solid(item(ModIngredientItems.ash(EnumAshType.SOOT))));

        register(Fluids.SOLVENT,
                recipe(1_000, 50).fluid1(Fluids.NAPHTHA, 500).fluid2(Fluids.AROMATICS, 500),
                recipe(1_000, 50).fluid1(Fluids.NAPHTHA_CRACK, 500).fluid2(Fluids.AROMATICS, 500),
                recipe(1_000, 50).fluid1(Fluids.NAPHTHA_DS, 500).fluid2(Fluids.AROMATICS, 500),
                recipe(1_000, 50).fluid1(Fluids.NAPHTHA_COKER, 500).fluid2(Fluids.AROMATICS, 500));
        register(Fluids.SULFURIC_ACID, recipe(500, 50).fluid1(Fluids.PEROXIDE, 800).solid(OreDictManager.S.dust()));
        register(Fluids.NITRIC_ACID, recipe(1_000, 50).fluid1(Fluids.SULFURIC_ACID, 500)
                .solid(OreDictManager.KNO.dust()));
        register(Fluids.RADIOSOLVENT, recipe(1_000, 50).fluid1(Fluids.REFORMGAS, 750).fluid2(Fluids.CHLORINE, 250));
        register(Fluids.SCHRABIDIC, recipe(16_000, 100).fluid1(Fluids.SAS3, 8_000).fluid2(Fluids.PEROXIDE, 6_000)
                .solid(item(ModItems.pellet_charged.get())));

        register(Fluids.PETROIL, recipe(1_000, 30).fluid1(Fluids.RECLAIMED, 800).fluid2(Fluids.LUBRICANT, 200));
        register(Fluids.LUBRICANT,
                recipe(1_000, 20).fluid1(Fluids.HEATINGOIL, 500).fluid2(Fluids.UNSATURATEDS, 500),
                recipe(1_000, 20).fluid1(Fluids.FISHOIL, 800).fluid2(Fluids.ETHANOL, 200),
                recipe(1_000, 20).fluid1(Fluids.SUNFLOWEROIL, 800).fluid2(Fluids.ETHANOL, 200));
        register(Fluids.BIOFUEL,
                recipe(250, 20).fluid1(Fluids.FISHOIL, 500).fluid2(Fluids.WOODOIL, 500),
                recipe(200, 20).fluid1(Fluids.SUNFLOWEROIL, 500).fluid2(Fluids.WOODOIL, 500));
        register(Fluids.NITROGLYCERIN,
                recipe(1_000, 20).fluid1(Fluids.PETROLEUM, 1_000).fluid2(Fluids.NITRIC_ACID, 1_000),
                recipe(1_000, 20).fluid1(Fluids.FISHOIL, 500).fluid2(Fluids.NITRIC_ACID, 500));
        register(Fluids.THORIUM_SALT, recipe(1_000, 30).fluid1(Fluids.CHLORINE, 1_000).solid(OreDictManager.TH232.dust()));
        register(Fluids.SYNGAS, recipe(1_000, 50).fluid1(Fluids.COALOIL, 500).fluid2(Fluids.STEAM, 500));
        register(Fluids.OXYHYDROGEN,
                recipe(1_000, 50).fluid1(Fluids.HYDROGEN, 500).fluid2(Fluids.AIR, 2_000),
                recipe(1_000, 50).fluid1(Fluids.HYDROGEN, 500).fluid2(Fluids.OXYGEN, 500));

        register(Fluids.DIESEL_REFORM, recipe(1_000, 50).fluid1(Fluids.DIESEL, 900).fluid2(Fluids.REFORMATE, 100));
        register(Fluids.DIESEL_CRACK_REFORM, recipe(1_000, 50).fluid1(Fluids.DIESEL_CRACK, 900).fluid2(Fluids.REFORMATE, 100));
        register(Fluids.KEROSENE_REFORM, recipe(1_000, 50).fluid1(Fluids.KEROSENE, 900).fluid2(Fluids.REFORMATE, 100));
        register(Fluids.CHLOROCALCITE_SOLUTION, recipe(500, 50).fluid1(Fluids.WATER, 250)
                .fluid2(Fluids.NITRIC_ACID, 250).solid(OreDictManager.CHLOROCALCITE.dust()));
        register(Fluids.CHLOROCALCITE_MIX, recipe(1_000, 50).fluid1(Fluids.CHLOROCALCITE_SOLUTION, 500)
                .fluid2(Fluids.SULFURIC_ACID, 500).solid(item(ModItems.powder_flux.get())));
        register(Fluids.PHEROMONE_M, recipe(2_000, 10).fluid1(Fluids.PHEROMONE, 1_500).fluid2(Fluids.BLOOD, 500)
                .solid(item(ModMedicineItems.pill_herbal.get())));
        register(Fluids.BAUXITE_SOLUTION, recipe(300, 80).fluid1(Fluids.LYE, 50)
                .solid(item(ModOreBlocks.stone_resource_bauxite.get().asItem())));
        register(Fluids.LYE, recipe(50, 100).fluid1(Fluids.WATER, 500).solid(item(ModIngredientItems.ash(EnumAshType.WOOD), 2)));
        register(Fluids.ALUMINA,
                recipe(200, 40).fluid1(Fluids.SODIUM_ALUMINATE, 150).solid(OreDictManager.F.dust().count(3)),
                recipe(300, 40).fluid1(Fluids.SODIUM_ALUMINATE, 150).solid(item(ModIntermediateItems.chunk(EnumChunkType.CRYOLITE))));
        register(Fluids.PERFLUOROMETHYL, recipe(1_000, 20).fluid1(Fluids.PETROLEUM, 1_000)
                .fluid2(Fluids.UNSATURATEDS, 500).solid(OreDictManager.F.dust()));
        register(Fluids.BITUMEN, recipe(50, 20).solid(new TagStack(PyroOvenRecipes.TARS)));

        for (com.hbm.inventory.recipes.material.FuelAdditiveRecipes.MixerRecipeSpec spec
                : com.hbm.inventory.recipes.material.FuelAdditiveRecipes.mixerRecipes()) {
            register(spec.output(), recipe(spec.outputAmount(), spec.processTime())
                    .fluid1(spec.input(), spec.inputAmount())
                    .solid(new ComparableStack(spec.solid().getItem())));
        }
    }

    private void register(FluidType output, MixerRecipe... entries) {
        recipes.put(output, entries);
        for (int i = 0; i < entries.length; i++) {
            MixerRecipe entry = entries[i];
            GenericRecipe generic = new GenericRecipe("mixer." + output.getName().toLowerCase() + "." + i)
                    .setup(entry.processTime, 0)
                    .outputFluids(new FluidStack(output, entry.output));
            if (entry.solidInput != null) generic.inputItems(entry.solidInput);
            if (entry.input1 != null) generic.inputFluids(entry.input1);
            if (entry.input2 != null) {
                if (generic.inputFluid == null) generic.inputFluids(entry.input2);
                else generic.inputFluids(entry.input1, entry.input2);
            }
            register(generic);
        }
    }

    public static MixerRecipe[] getOutput(FluidType type) {
        INSTANCE.registerRecipes();
        return recipes.get(type);
    }

    public static MixerRecipe getOutput(FluidType type, int index) {
        MixerRecipe[] entries = getOutput(type);
        return entries == null || entries.length == 0 ? null : entries[Math.floorMod(index, entries.length)];
    }

    private static MixerRecipe recipe(int output, int processTime) {
        return new MixerRecipe(output, processTime);
    }

    private static AStack item(net.minecraft.world.item.Item item) {
        return new ComparableStack(item);
    }

    private static AStack item(net.minecraft.world.item.Item item, int count) {
        return new ComparableStack(item, count);
    }

    public static final class MixerRecipe {
        public FluidStack input1;
        public FluidStack input2;
        public AStack solidInput;
        public final int processTime;
        public final int output;

        private MixerRecipe(int output, int processTime) {
            this.output = output;
            this.processTime = processTime;
        }

        private MixerRecipe fluid1(FluidType type, int amount) {
            input1 = new FluidStack(type, amount);
            return this;
        }

        private MixerRecipe fluid2(FluidType type, int amount) {
            input2 = new FluidStack(type, amount);
            return this;
        }

        private MixerRecipe solid(AStack input) {
            solidInput = input;
            return this;
        }
    }
}
