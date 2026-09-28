package com.hbm.inventory.recipes;

import com.hbm.foundry.ArcFurnaceRecipes;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.CrystallizerRecipes.CrystallizerRecipe;
import com.hbm.inventory.recipes.PyroOvenRecipes.PyroOvenRecipe;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.BedrockOreType.BedrockOreOutput;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static com.hbm.items.special.ItemBedrockOreGraded.Grade.*;

/**
 * The 1.7 bedrock ore processing chain ({@code ItemBedrockOreNew} grades), one method per machine table, each fed
 * by a one-line hook in that machine's recipe class. Every entry is the 1.7 loop over {@code BedrockOreType},
 * in 1.7 order:
 *
 * <ul>
 *   <li>combination oven / pyro oven: roasting BASE, PRIMARY and the three byproducts, 50 mB vitriol each;</li>
 *   <li>ore acidizer (crystallizer): washing with water, sulfuric acid / solvent / radiosolvent separation,
 *       hydrogen/chlorine weighting, and slop recycling of 64 crumbs;</li>
 *   <li>centrifuge: fraction separation and the fragment outputs;</li>
 *   <li>arc furnace: searing the byproducts, and melting the endpoints straight to molten material;</li>
 *   <li>electrolyser (metal): the weighted primaries and crumbs, 20 ticks each;</li>
 *   <li>shredder: every grade back to gravel.</li>
 * </ul>
 */
public final class BedrockOreChainRecipes {

    private BedrockOreChainRecipes() {}

    private static ItemStack make(Grade grade, BedrockOreType type, int amount) {
        return ModBedrockOreItems.make(grade, type, amount);
    }

    private static ItemStack make(Grade grade, BedrockOreType type) {
        return make(grade, type, 1);
    }

    private static ComparableStack in(Grade grade, BedrockOreType type) {
        return new ComparableStack(ModBedrockOreItems.get(grade, type));
    }

    /** 1.7 {@code ItemBedrockOreNew.extract}: ceil(table amount * multiplier) fragments, capped at a stack. */
    private static ItemStack extract(BedrockOreOutput output, double amount) {
        return BedrockOreType.fragment(output, amount);
    }

    /** 1.7 {@code ItemBedrockOreNew.toFluid}: the molten material of a smeltable output, else nothing. */
    private static Mats.@Nullable MaterialStack toFluid(BedrockOreOutput output, double amount) {
        NTMMaterial material = output.material();
        if (material != null && material.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) {
            return new Mats.MaterialStack(material, (int) Math.ceil(MaterialShapes.FRAGMENT.quantity * output.amount() * amount));
        }
        return null;
    }

    // --- 1.7 CombinationRecipes -------------------------------------------------------------------------

    public static void combination(FinalConsumableMachineRecipes.CombinationSink sink) {
        for (BedrockOreType type : BedrockOreType.VALUES) {
            sink.add(in(BASE, type), make(BASE_ROASTED, type), new FluidStack(Fluids.VITRIOL, 50));
            sink.add(in(PRIMARY, type), make(PRIMARY_ROASTED, type), new FluidStack(Fluids.VITRIOL, 50));
            sink.add(in(SULFURIC_BYPRODUCT, type), make(SULFURIC_ROASTED, type), new FluidStack(Fluids.VITRIOL, 50));
            sink.add(in(SOLVENT_BYPRODUCT, type), make(SOLVENT_ROASTED, type), new FluidStack(Fluids.VITRIOL, 50));
            sink.add(in(RAD_BYPRODUCT, type), make(RAD_ROASTED, type), new FluidStack(Fluids.VITRIOL, 50));
        }
    }

    // --- 1.7 PyroOvenRecipes ------------------------------------------------------------------------------

    public static void pyroOven(List<PyroOvenRecipe> recipes) {
        for (BedrockOreType type : BedrockOreType.VALUES) {
            recipes.add(new PyroOvenRecipe(10).in(in(BASE, type)).out(new FluidStack(Fluids.VITRIOL, 50)).out(make(BASE_ROASTED, type)));
            recipes.add(new PyroOvenRecipe(10).in(in(PRIMARY, type)).out(new FluidStack(Fluids.VITRIOL, 50)).out(make(PRIMARY_ROASTED, type)));
            recipes.add(new PyroOvenRecipe(10).in(in(SULFURIC_BYPRODUCT, type)).out(new FluidStack(Fluids.VITRIOL, 50)).out(make(SULFURIC_ROASTED, type)));
            recipes.add(new PyroOvenRecipe(10).in(in(SOLVENT_BYPRODUCT, type)).out(new FluidStack(Fluids.VITRIOL, 50)).out(make(SOLVENT_ROASTED, type)));
            recipes.add(new PyroOvenRecipe(10).in(in(RAD_BYPRODUCT, type)).out(new FluidStack(Fluids.VITRIOL, 50)).out(make(RAD_ROASTED, type)));
        }
    }

    // --- 1.7 CrystallizerRecipes ---------------------------------------------------------------------------

    public static void crystallizer(List<CrystallizerRecipe> recipes) {
        final int bedrock = 200;
        final int washing = 100;
        for (BedrockOreType type : BedrockOreType.VALUES) {
            acid(recipes, BASE, type, 1, BASE_WASHED, washing, Fluids.WATER);
            acid(recipes, BASE_ROASTED, type, 1, BASE_WASHED, washing, Fluids.WATER);

            acid(recipes, PRIMARY, type, 1, PRIMARY_SULFURIC, bedrock, Fluids.SULFURIC_ACID);
            acid(recipes, PRIMARY_ROASTED, type, 1, PRIMARY_SULFURIC, bedrock, Fluids.SULFURIC_ACID);

            acid(recipes, PRIMARY, type, 1, PRIMARY_SOLVENT, bedrock, Fluids.SOLVENT);
            acid(recipes, PRIMARY_ROASTED, type, 1, PRIMARY_SOLVENT, bedrock, Fluids.SOLVENT);
            acid(recipes, PRIMARY_NOSULFURIC, type, 1, PRIMARY_SOLVENT, bedrock, Fluids.SOLVENT);

            acid(recipes, PRIMARY, type, 1, PRIMARY_RAD, bedrock, Fluids.RADIOSOLVENT);
            acid(recipes, PRIMARY_ROASTED, type, 1, PRIMARY_RAD, bedrock, Fluids.RADIOSOLVENT);
            acid(recipes, PRIMARY_NOSULFURIC, type, 1, PRIMARY_RAD, bedrock, Fluids.RADIOSOLVENT);
            acid(recipes, PRIMARY_NOSOLVENT, type, 1, PRIMARY_RAD, bedrock, Fluids.RADIOSOLVENT);

            final int sulf = 4;
            acid(recipes, SULFURIC_BYPRODUCT, type, sulf, SULFURIC_WASHED, washing, Fluids.WATER);
            acid(recipes, SULFURIC_ROASTED, type, sulf, SULFURIC_WASHED, washing, Fluids.WATER);
            acid(recipes, SULFURIC_ARC, type, sulf, SULFURIC_WASHED, washing, Fluids.WATER);

            final int solv = 4;
            acid(recipes, SOLVENT_BYPRODUCT, type, solv, SOLVENT_WASHED, washing, Fluids.WATER);
            acid(recipes, SOLVENT_ROASTED, type, solv, SOLVENT_WASHED, washing, Fluids.WATER);
            acid(recipes, SOLVENT_ARC, type, solv, SOLVENT_WASHED, washing, Fluids.WATER);

            final int rad = 4;
            acid(recipes, RAD_BYPRODUCT, type, rad, RAD_WASHED, washing, Fluids.WATER);
            acid(recipes, RAD_ROASTED, type, rad, RAD_WASHED, washing, Fluids.WATER);
            acid(recipes, RAD_ARC, type, rad, RAD_WASHED, washing, Fluids.WATER);

            for (Grade from : new Grade[]{PRIMARY, PRIMARY_ROASTED, PRIMARY_SULFURIC, PRIMARY_NOSULFURIC,
                    PRIMARY_SOLVENT, PRIMARY_NOSOLVENT, PRIMARY_RAD, PRIMARY_NORAD}) {
                acid(recipes, from, type, 1, PRIMARY_FIRST, bedrock, Fluids.HYDROGEN);
            }
            for (Grade from : new Grade[]{PRIMARY, PRIMARY_ROASTED, PRIMARY_SULFURIC, PRIMARY_NOSULFURIC,
                    PRIMARY_SOLVENT, PRIMARY_NOSOLVENT, PRIMARY_RAD, PRIMARY_NORAD}) {
                acid(recipes, from, type, 1, PRIMARY_SECOND, bedrock, Fluids.CHLORINE);
            }

            // 64 crumbs and a bucket of slop back to raw ore
            recipes.add(new CrystallizerRecipe(new ComparableStack(ModBedrockOreItems.get(CRUMBS, type), 64),
                    Fluids.SLOP, 1000, bedrock, 0F, make(BASE, type)));
        }
    }

    /** 1.7 {@code registerRecipe(stack, new CrystallizerRecipe(out, time).setReq(count), new FluidStack(acid, 250))}. */
    private static void acid(List<CrystallizerRecipe> recipes, Grade from, BedrockOreType type, int count, Grade to,
                             int duration, FluidType acid) {
        recipes.add(new CrystallizerRecipe(new ComparableStack(ModBedrockOreItems.get(from, type), count),
                acid, 250, duration, 0F, make(to, type)));
    }

    // --- 1.7 CentrifugeRecipes ------------------------------------------------------------------------------

    public interface CentrifugeSink {
        void add(AStack input, ItemStack... output);
    }

    public static void centrifuge(CentrifugeSink sink) {
        ItemStack gravel = new ItemStack(Items.GRAVEL);
        for (BedrockOreType type : BedrockOreType.VALUES) {
            sink.add(in(BASE, type), make(PRIMARY, type), gravel.copy());
            sink.add(in(BASE_ROASTED, type), make(PRIMARY, type), gravel.copy());
            sink.add(in(BASE_WASHED, type), make(PRIMARY, type), make(PRIMARY, type), gravel.copy());

            sink.add(in(PRIMARY_SULFURIC, type), make(PRIMARY_NOSULFURIC, type, 2), make(SULFURIC_BYPRODUCT, type, 2));
            sink.add(in(PRIMARY_SOLVENT, type), make(PRIMARY_NOSOLVENT, type, 2), make(SULFURIC_BYPRODUCT, type, 2),
                    make(SOLVENT_BYPRODUCT, type, 2));
            sink.add(in(PRIMARY_RAD, type), make(PRIMARY_NORAD, type, 2), make(SULFURIC_BYPRODUCT, type, 2),
                    make(SOLVENT_BYPRODUCT, type, 2), make(RAD_BYPRODUCT, type, 2));

            sink.add(in(PRIMARY, type), extract(type.primary1, 1), extract(type.primary2, 1));
            sink.add(in(PRIMARY_ROASTED, type), extract(type.primary1, 1), extract(type.primary2, 1));
            sink.add(in(PRIMARY_NOSULFURIC, type), extract(type.primary1, 1), extract(type.primary2, 1), make(CRUMBS, type));
            sink.add(in(PRIMARY_NOSOLVENT, type), extract(type.primary1, 1), extract(type.primary2, 1), make(CRUMBS, type));
            sink.add(in(PRIMARY_NORAD, type), extract(type.primary1, 1), extract(type.primary2, 1), make(CRUMBS, type));
            sink.add(in(PRIMARY_FIRST, type), extract(type.primary1, 1), extract(type.primary1, 1),
                    extract(type.primary2, 1), make(CRUMBS, type, 1));
            sink.add(in(PRIMARY_SECOND, type), extract(type.primary1, 1), extract(type.primary2, 1),
                    extract(type.primary2, 1), make(CRUMBS, type, 1));

            sink.add(in(SULFURIC_WASHED, type), extract(type.byproductAcid1, 1), extract(type.byproductAcid2, 1),
                    extract(type.byproductAcid3, 1), make(CRUMBS, type));
            sink.add(in(SOLVENT_WASHED, type), extract(type.byproductSolvent1, 1), extract(type.byproductSolvent2, 1),
                    extract(type.byproductSolvent3, 1), make(CRUMBS, type));
            sink.add(in(RAD_WASHED, type), extract(type.byproductRad1, 1), extract(type.byproductRad2, 1),
                    extract(type.byproductRad3, 1), make(CRUMBS, type));
        }
    }

    // --- 1.7 ArcFurnaceRecipes ------------------------------------------------------------------------------

    public static void arcFurnace() {
        for (BedrockOreType type : BedrockOreType.VALUES) {
            ArcFurnaceRecipes.register(in(SULFURIC_BYPRODUCT, type), make(SULFURIC_ARC, type, 2));
            ArcFurnaceRecipes.register(in(SULFURIC_ROASTED, type), make(SULFURIC_ARC, type, 4));
            ArcFurnaceRecipes.register(in(SOLVENT_BYPRODUCT, type), make(SOLVENT_ARC, type, 2));
            ArcFurnaceRecipes.register(in(SOLVENT_ROASTED, type), make(SOLVENT_ARC, type, 4));
            ArcFurnaceRecipes.register(in(RAD_BYPRODUCT, type), make(RAD_ARC, type, 2));
            ArcFurnaceRecipes.register(in(RAD_ROASTED, type), make(RAD_ARC, type, 4));

            ArcFurnaceRecipes.register(in(PRIMARY_FIRST, type), null, fluidNull(toFluid(type.primary1, 5), toFluid(type.primary2, 2)));
            ArcFurnaceRecipes.register(in(PRIMARY_SECOND, type), null, fluidNull(toFluid(type.primary1, 2), toFluid(type.primary2, 5)));
            ArcFurnaceRecipes.register(in(CRUMBS, type), null, fluidNull(toFluid(type.primary1, 1), toFluid(type.primary2, 1)));

            int i3 = 3;
            ArcFurnaceRecipes.register(in(SULFURIC_WASHED, type), null, fluidNull(toFluid(type.byproductAcid1, i3),
                    toFluid(type.byproductAcid2, i3), toFluid(type.byproductAcid3, i3)));
            ArcFurnaceRecipes.register(in(SOLVENT_WASHED, type), null, fluidNull(toFluid(type.byproductSolvent1, i3),
                    toFluid(type.byproductSolvent2, i3), toFluid(type.byproductSolvent3, i3)));
            ArcFurnaceRecipes.register(in(RAD_WASHED, type), null, fluidNull(toFluid(type.byproductRad1, i3),
                    toFluid(type.byproductRad2, i3), toFluid(type.byproductRad3, i3)));
        }
    }

    /** 1.7 {@code ArcFurnaceRecipe.fluidNull}: the non-null outputs only. */
    private static Mats.MaterialStack[] fluidNull(Mats.MaterialStack... outputs) {
        List<Mats.MaterialStack> kept = new ArrayList<>();
        for (Mats.MaterialStack output : outputs) if (output != null) kept.add(output);
        return kept.toArray(new Mats.MaterialStack[0]);
    }

    // --- 1.7 ElectrolyserMetalRecipes -----------------------------------------------------------------------

    public interface ElectrolyserSink {
        void add(Item input, Mats.MaterialStack output1, Mats.@Nullable MaterialStack output2, int duration, ItemStack... byproduct);
    }

    public static void electrolyser(ElectrolyserSink sink) {
        for (BedrockOreType type : BedrockOreType.VALUES) {
            List<Object[]> first = List.of(new Object[]{type.primary1, 8}, new Object[]{type.primary2, 4},
                    new Object[]{make(CRUMBS, type), 1});
            product(sink, ModBedrockOreItems.get(PRIMARY_FIRST, type), first);

            List<Object[]> second = List.of(new Object[]{type.primary1, 4}, new Object[]{type.primary2, 8},
                    new Object[]{make(CRUMBS, type), 1});
            product(sink, ModBedrockOreItems.get(PRIMARY_SECOND, type), second);

            List<Object[]> crumbs = List.of(new Object[]{type.primary1, 2}, new Object[]{type.primary2, 2});
            product(sink, ModBedrockOreItems.get(CRUMBS, type), crumbs);
        }
    }

    /**
     * 1.7 {@code makeBedrockOreProduct}: the first two smeltable outputs become molten, everything else (and any
     * output that cannot melt) comes out as fragments or items; no molten output at all means two ingots of slag.
     */
    private static void product(ElectrolyserSink sink, Item input, List<Object[]> products) {
        List<Mats.MaterialStack> molten = new ArrayList<>();
        List<ItemStack> solid = new ArrayList<>();
        for (Object[] product : products) {
            int amount = (Integer) product[1];
            if (molten.size() < 2 && product[0] instanceof BedrockOreOutput output) {
                Mats.MaterialStack melt = toFluid(output, amount);
                if (melt != null) {
                    molten.add(melt);
                    continue;
                }
            }
            if (product[0] instanceof BedrockOreOutput output) solid.add(extract(output, amount));
            if (product[0] instanceof ItemStack stack) solid.add(stack.copy());
        }
        if (molten.isEmpty()) molten.add(new Mats.MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.quantity * 2));
        sink.add(input, molten.get(0), molten.size() > 1 ? molten.get(1) : null, 20, solid.toArray(new ItemStack[0]));
    }

    // --- 1.7 ShredderRecipes ---------------------------------------------------------------------------------

    /** 1.7 {@code setRecipe(new ItemStack(bedrock_ore, 1, WILDCARD), gravel)}: every grade and type. */
    public static void shredder(BiConsumer<Item, ItemStack> set) {
        for (Grade grade : Grade.values()) {
            for (BedrockOreType type : BedrockOreType.VALUES) {
                set.accept(ModBedrockOreItems.get(grade, type), new ItemStack(Items.GRAVEL));
            }
        }
    }
}
