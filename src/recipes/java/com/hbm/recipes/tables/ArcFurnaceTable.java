package com.hbm.recipes.tables;

import net.minecraft.world.item.Item;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModGlassBlocks;
import com.hbm.foundry.ArcFurnaceRecipes;
import com.hbm.foundry.ArcFurnaceRecipes.Row;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.ModItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.BedrockOreType.BedrockOreOutput;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import dev.golem.core.world.RecipeSource;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.hbm.inventory.OreDictManager.*;

/**
 * 1.7 {@code ArcFurnaceRecipes.registerDefaults}: the explicit rows and the bedrock ore grade loop (1.7
 * {@code ItemBedrockOreNew} grades are the port's {@code bedrock_ore_<grade>_<type>} items). 1.7's three autogen passes
 * (material shapes, custom smeltables, furnace smeltables) are rules in {@link ArcFurnaceRecipes}, not rows.
 */
public final class ArcFurnaceTable extends RecipeSource<Row> {

    /** 1.7 {@code KEY_SAND}, the "sand" ore name: {@code c:sands} (sand and red sand, not suspicious sand). */
    private static final TagKey<Item> KEY_SAND = TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", "sands"));

    public ArcFurnaceTable() {
        super(ArcFurnaceRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        register(new TagStack(KEY_SAND),           new ArcFurnaceRecipe().solid(new ItemStack(ModItems.nugget_silicon.get()))       .fluid(new MaterialStack(Mats.MAT_SILICON, q(MaterialShapes.NUGGET, 1))));
        register(new ComparableStack(Items.FLINT),      new ArcFurnaceRecipe().solid(new ItemStack(ModItems.nugget_silicon.get(), 4))    .fluid(new MaterialStack(Mats.MAT_SILICON, q(MaterialShapes.INGOT, 1, 2))));
        register(tag("c", "gems/quartz"),               new ArcFurnaceRecipe().solid(new ItemStack(ModItems.nugget_silicon.get(), 3))    .fluid(new MaterialStack(Mats.MAT_SILICON, q(MaterialShapes.NUGGET, 3))));
        register(tag("c", "dusts/quartz"),              new ArcFurnaceRecipe().solid(new ItemStack(ModItems.nugget_silicon.get(), 3))    .fluid(new MaterialStack(Mats.MAT_SILICON, q(MaterialShapes.NUGGET, 3))));
        // 1.7 QUARTZ.block() "blockQuartz" (the quartz block) is c:storage_blocks/quartz
        register(tag("c", "storage_blocks/quartz"), new ArcFurnaceRecipe().solid(new ItemStack(ModItems.nugget_silicon.get(), 12)) .fluid(new MaterialStack(Mats.MAT_SILICON, q(MaterialShapes.NUGGET, 12))));
        // 1.7 FIBER.ingot() and FIBER.block() ("ingotFiberglass", "blockFiberglass")
        register(tag("c", "ingots/fiberglass"), new ArcFurnaceRecipe().solid(new ItemStack(ModItems.nugget_silicon.get(), 4)) .fluid(new MaterialStack(Mats.MAT_SILICON, q(MaterialShapes.INGOT, 1, 2))));
        register(tag("c", "storage_blocks/fiberglass"), new ArcFurnaceRecipe().solid(new ItemStack(ModItems.nugget_silicon.get(), 40)) .fluid(new MaterialStack(Mats.MAT_SILICON, q(MaterialShapes.INGOT, 9, 2))));
        register(ASBESTOS.ingot(),                      new ArcFurnaceRecipe().solid(new ItemStack(ModItems.nugget_silicon.get(), 4))    .fluid(new MaterialStack(Mats.MAT_SILICON, q(MaterialShapes.INGOT, 1, 2))));
        register(ASBESTOS.dust(),                       new ArcFurnaceRecipe().solid(new ItemStack(ModItems.nugget_silicon.get(), 4))    .fluid(new MaterialStack(Mats.MAT_SILICON, q(MaterialShapes.INGOT, 1, 2))));
        register(ASBESTOS.block(),                      new ArcFurnaceRecipe().solid(new ItemStack(ModItems.nugget_silicon.get(), 40))   .fluid(new MaterialStack(Mats.MAT_SILICON, q(MaterialShapes.INGOT, 9, 2))));

        register(new ComparableStack(ModFallingBlocks.sand_quartz.get().asItem()), new ArcFurnaceRecipe().solid(new ItemStack(ModGlassBlocks.glass_quartz.get().asItem())));
        register(BORAX.dust(), new ArcFurnaceRecipe().solid(new ItemStack(ModItems.powder_boron_tiny.get(), 3)).fluid(new MaterialStack(Mats.MAT_BORON, q(MaterialShapes.NUGGET, 3))));

        for (BedrockOreType type : BedrockOreType.VALUES) {
            register(new ComparableStack(ModBedrockOreItems.get(Grade.SULFURIC_BYPRODUCT, type)), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.SULFURIC_ARC, type, 2)));
            register(new ComparableStack(ModBedrockOreItems.get(Grade.SULFURIC_ROASTED, type)), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.SULFURIC_ARC, type, 4)));
            register(new ComparableStack(ModBedrockOreItems.get(Grade.SOLVENT_BYPRODUCT, type)), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.SOLVENT_ARC, type, 2)));
            register(new ComparableStack(ModBedrockOreItems.get(Grade.SOLVENT_ROASTED, type)), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.SOLVENT_ARC, type, 4)));
            register(new ComparableStack(ModBedrockOreItems.get(Grade.RAD_BYPRODUCT, type)), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.RAD_ARC, type, 2)));
            register(new ComparableStack(ModBedrockOreItems.get(Grade.RAD_ROASTED, type)), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.RAD_ARC, type, 4)));

            register(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_FIRST, type)), new ArcFurnaceRecipe().fluidNull(toFluid(type.primary1, 5), toFluid(type.primary2, 2)));
            register(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_SECOND, type)), new ArcFurnaceRecipe().fluidNull(toFluid(type.primary1, 2), toFluid(type.primary2, 5)));
            register(new ComparableStack(ModBedrockOreItems.get(Grade.CRUMBS, type)), new ArcFurnaceRecipe().fluidNull(toFluid(type.primary1, 1), toFluid(type.primary2, 1)));

            int i3 = 3;
            register(new ComparableStack(ModBedrockOreItems.get(Grade.SULFURIC_WASHED, type)), new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductAcid1, i3), toFluid(type.byproductAcid2, i3), toFluid(type.byproductAcid3, i3)));
            register(new ComparableStack(ModBedrockOreItems.get(Grade.SOLVENT_WASHED, type)), new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductSolvent1, i3), toFluid(type.byproductSolvent2, i3), toFluid(type.byproductSolvent3, i3)));
            register(new ComparableStack(ModBedrockOreItems.get(Grade.RAD_WASHED, type)), new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductRad1, i3), toFluid(type.byproductRad2, i3), toFluid(type.byproductRad3, i3)));
        }
    }

    /**
     * 1.7 {@code register(input, recipe)}. A recipe with neither output (a bedrock grade whose outputs cannot melt) is
     * not a row: 1.7 registered it, and it never matched anything. The id is the input: the item (its namespace first
     * unless {@code hbm}), or {@code tag_} and the tag (its namespace first unless {@code c}), slashes as underscores.
     */
    private void register(AStack input, ArcFurnaceRecipe recipe) {
        if (recipe.solidOutput == null && recipe.fluidOutput.isEmpty()) return;
        register(id(input), new Row(input, Optional.ofNullable(recipe.solidOutput).map(ItemOut::of), recipe.fluidOutput, nextOrder()));
    }

    private static String id(AStack input) {
        if (input instanceof TagStack tag) return "tag_" + name(tag.tag.location().getNamespace(), tag.tag.location().getPath(), "c");
        var key = BuiltInRegistries.ITEM.getKey(((ComparableStack) input).item);
        return name(key.getNamespace(), key.getPath(), "hbm");
    }

    private static String name(String namespace, String path, String implied) {
        path = path.replace('/', '_');
        return namespace.equals(implied) ? path : namespace + "_" + path;
    }

    private static TagStack tag(String namespace, String path) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.of(namespace, path)));
    }

    /** 1.7 {@code ItemBedrockOreNew.toFluid}: the molten material of a smeltable output, else nothing. */
    private static @Nullable MaterialStack toFluid(@Nullable BedrockOreOutput o, double amount) {
        if (o == null) return null;
        NTMMaterial mat = o.material();
        if (mat != null && mat.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) {
            return new MaterialStack(mat, (int) Math.ceil(q(MaterialShapes.FRAGMENT, o.amount()) * amount));
        }
        return null;
    }

    /** 1.7 {@code MaterialShapes.q(amount)}. */
    private static int q(MaterialShapes shape, int amount) {
        return shape.quantity * amount;
    }

    /** 1.7 {@code MaterialShapes.q(unitsUsed, itemsProduced)}. */
    private static int q(MaterialShapes shape, int unitsUsed, int itemsProduced) {
        return shape.quantity * unitsUsed / itemsProduced;
    }

    /** 1.7 {@code ArcFurnaceRecipe}'s builder. */
    private static final class ArcFurnaceRecipe {
        @Nullable ItemStack solidOutput;
        List<MaterialStack> fluidOutput = List.of();

        ArcFurnaceRecipe fluid(MaterialStack... outputs) {
            this.fluidOutput = List.of(outputs);
            return this;
        }

        ArcFurnaceRecipe fluidNull(@Nullable MaterialStack... outputs) {
            List<MaterialStack> mat = new ArrayList<>();
            for (MaterialStack stack : outputs) if (stack != null) mat.add(stack);
            if (!mat.isEmpty()) this.fluidOutput = mat;
            return this;
        }

        ArcFurnaceRecipe solid(ItemStack output) {
            this.solidOutput = output;
            return this;
        }
    }
}
