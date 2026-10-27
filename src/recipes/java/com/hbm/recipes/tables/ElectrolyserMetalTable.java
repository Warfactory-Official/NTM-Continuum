package com.hbm.recipes.tables;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.BedrockOreType.BedrockOreOutput;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import com.hbm.items.ItemEnums.EnumChunkType;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ElectrolyserMetalRecipes.registerDefaults}, with the bedrock ore grade loop (1.7 {@code ItemBedrockOreNew}
 * grades are the port's {@code bedrock_ore_<grade>_<type>} items).
 */
public final class ElectrolyserMetalTable extends RecipeSource<Row> {

    public ElectrolyserMetalTable() {
        super(ElectrolyserMetalRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        put(new ComparableStack(ModItems.crystal_iron.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_IRON, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_TITANIUM, q(MaterialShapes.INGOT, 2)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3)));

        // 1.7 ingot_mercury is the port's nugget_mercury (1.7's nugget_mercury field is nugget_mercury_tiny)
        put(new ComparableStack(ModItems.crystal_gold.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_GOLD, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_LEAD, q(MaterialShapes.INGOT, 2)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3),
                new ItemStack(ModItems.nugget_mercury.get(), 2)));

        put(new ComparableStack(ModItems.crystal_uranium.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_URANIUM, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_RADIUM, q(MaterialShapes.NUGGET, 4)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3)));

        put(new ComparableStack(ModItems.crystal_thorium.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_THORIUM, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_URANIUM, q(MaterialShapes.INGOT, 2)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3)));

        put(new ComparableStack(ModItems.crystal_plutonium.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_PLUTONIUM, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_POLONIUM, q(MaterialShapes.INGOT, 2)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3)));

        put(new ComparableStack(ModItems.crystal_titanium.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_TITANIUM, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_IRON, q(MaterialShapes.INGOT, 2)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3)));

        put(new ComparableStack(ModItems.crystal_copper.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_COPPER, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_LEAD, q(MaterialShapes.NUGGET, 4)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3),
                new ItemStack(ModItems.sulfur.get(), 2)));

        put(new ComparableStack(ModItems.crystal_tungsten.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_TUNGSTEN, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_IRON, q(MaterialShapes.INGOT, 2)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3)));

        put(new ComparableStack(ModItems.crystal_aluminium.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_ALUMINIUM, q(MaterialShapes.INGOT, 2)),
                new MaterialStack(Mats.MAT_IRON, q(MaterialShapes.INGOT, 2)),
                ModIntermediateItems.chunk(EnumChunkType.CRYOLITE, 4),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3)));


        put(new ComparableStack(ModItems.crystal_beryllium.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_BERYLLIUM, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_LEAD, q(MaterialShapes.NUGGET, 4)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3),
                new ItemStack(ModItems.powder_quartz.get(), 2)));

        put(new ComparableStack(ModItems.crystal_lead.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_LEAD, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_GOLD, q(MaterialShapes.INGOT, 2)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3)));

        put(new ComparableStack(ModItems.crystal_schraranium.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_SCHRABIDIUM, q(MaterialShapes.NUGGET, 5)),
                new MaterialStack(Mats.MAT_URANIUM, q(MaterialShapes.NUGGET, 2)),
                new ItemStack(ModItems.nugget_neptunium.get(), 2)));

        put(new ComparableStack(ModItems.crystal_schrabidium.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_SCHRABIDIUM, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_PLUTONIUM, q(MaterialShapes.INGOT, 2)),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3)));

        put(new ComparableStack(ModItems.crystal_rare.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_ZIRCONIUM, q(MaterialShapes.NUGGET, 6)),
                new MaterialStack(Mats.MAT_BORON, q(MaterialShapes.NUGGET, 2)),
                new ItemStack(ModItems.powder_desh_mix.get(), 3)));

        put(new ComparableStack(ModItems.crystal_trixite.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_PLUTONIUM, q(MaterialShapes.INGOT, 3)),
                new MaterialStack(Mats.MAT_COBALT, q(MaterialShapes.INGOT, 4)),
                new ItemStack(ModItems.powder_niobium.get(), 4),
                new ItemStack(ModItems.powder_nitan_mix.get(), 2)));

        put(new ComparableStack(ModItems.crystal_lithium.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_LITHIUM, q(MaterialShapes.INGOT, 6)),
                new MaterialStack(Mats.MAT_BORON, q(MaterialShapes.INGOT, 2)),
                new ItemStack(ModItems.powder_quartz.get(), 2),
                new ItemStack(ModItems.fluorite.get(), 2)));

        put(new ComparableStack(ModItems.crystal_starmetal.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_DURA, q(MaterialShapes.INGOT, 4)),
                new MaterialStack(Mats.MAT_COBALT, q(MaterialShapes.INGOT, 4)),
                new ItemStack(ModItems.powder_astatine.get(), 3),
                new ItemStack(ModItems.nugget_mercury.get(), 8)));

        put(new ComparableStack(ModItems.crystal_cobalt.get()), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_COBALT, q(MaterialShapes.INGOT, 3)),
                new MaterialStack(Mats.MAT_IRON, q(MaterialShapes.INGOT, 4)),
                new ItemStack(ModItems.powder_copper.get(), 4),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 3)));

        for (BedrockOreType type : BedrockOreType.VALUES) {
            List<Object[]> productsF = new ArrayList<>();
            productsF.add(new Object[]{type.primary1, 8});
            productsF.add(new Object[]{type.primary2, 4});
            productsF.add(new Object[]{ModBedrockOreItems.make(Grade.CRUMBS, type), 1});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_FIRST, type)), makeBedrockOreProduct(productsF));

            List<Object[]> productsS = new ArrayList<>();
            productsS.add(new Object[]{type.primary1, 4});
            productsS.add(new Object[]{type.primary2, 8});
            productsS.add(new Object[]{ModBedrockOreItems.make(Grade.CRUMBS, type), 1});

            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_SECOND, type)), makeBedrockOreProduct(productsS));

            List<Object[]> productsC = new ArrayList<>();
            productsC.add(new Object[]{type.primary1, 2});
            productsC.add(new Object[]{type.primary2, 2});

            put(new ComparableStack(ModBedrockOreItems.get(Grade.CRUMBS, type)), makeBedrockOreProduct(productsC));
        }
    }

    /** 1.7 {@code makeBedrockOreProduct}; a 1.7 {@code Pair<Object, Integer>} is an {@code Object[]{output, amount}}. */
    private static ElectrolysisMetalRecipe makeBedrockOreProduct(List<Object[]> products) {
        List<MaterialStack> moltenProducts = new ArrayList<>();
        List<ItemStack> solidProducts = new ArrayList<>();

        for (Object[] product : products) {
            int amount = (Integer) product[1];
            if (moltenProducts.size() < 2 && product[0] instanceof BedrockOreOutput output) {
                MaterialStack melt = BedrockOreType.molten(output, amount);
                if (melt != null) {
                    moltenProducts.add(melt);
                    continue;
                }
            }

            if (product[0] instanceof BedrockOreOutput output) solidProducts.add(BedrockOreType.fragment(output, amount));
            if (product[0] instanceof ItemStack stack) solidProducts.add(stack.copy());
        }
        if (moltenProducts.isEmpty()) moltenProducts.add(new MaterialStack(Mats.MAT_SLAG, q(MaterialShapes.INGOT, 2)));

        return new ElectrolysisMetalRecipe(
                moltenProducts.get(0),
                moltenProducts.size() > 1 ? moltenProducts.get(1) : null,
                20,
                solidProducts.toArray(new ItemStack[0]));
    }

    /** 1.7 {@code MaterialShapes.q(amount)}. */
    private static int q(MaterialShapes shape, int amount) {
        return shape.quantity * amount;
    }

    /** 1.7 {@code recipes.put(input, recipe)}; the id is the input item's path. */
    private void put(ComparableStack input, ElectrolysisMetalRecipe recipe) {
        List<MaterialStack> molten = new ArrayList<>();
        molten.add(recipe.output1);
        if (recipe.output2 != null) molten.add(recipe.output2);
        List<ItemOut> byproducts = new ArrayList<>();
        for (ItemStack stack : recipe.byproduct) byproducts.add(ItemOut.of(stack));
        register(BuiltInRegistries.ITEM.getKey(input.item).getPath(), new Row(input.item, molten, byproducts, recipe.duration, nextOrder()));
    }

    /** 1.7 {@code ElectrolysisMetalRecipe}'s two constructors (600 ticks unless given). */
    private static final class ElectrolysisMetalRecipe {
        final MaterialStack output1;
        final @Nullable MaterialStack output2;
        final int duration;
        final ItemStack[] byproduct;

        ElectrolysisMetalRecipe(MaterialStack output1, @Nullable MaterialStack output2, ItemStack... byproduct) {
            this(output1, output2, 600, byproduct);
        }

        ElectrolysisMetalRecipe(MaterialStack output1, @Nullable MaterialStack output2, int duration, ItemStack... byproduct) {
            this.output1 = output1;
            this.output2 = output2;
            this.duration = duration;
            this.byproduct = byproduct;
        }
    }
}
