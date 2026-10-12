package com.hbm.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import dev.golem.core.world.RecipeSource;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

/**
 * 1.7 {@code CyclotronRecipes.registerDefaults}. 1.7's {@code OreDictStack("dustX")} names are the port's
 * {@code c:dusts/x} tags; 1.7's {@code ingot_mercury} is the port's {@code nugget_mercury} (1.7's own
 * {@code nugget_mercury} field is {@code nugget_mercury_tiny}).
 */
public final class CyclotronTable extends RecipeSource<Row> {

    public CyclotronTable() {
        super(CyclotronRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        /// LITHIUM START ///
        int liA = 50;

        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), LI.dust(), new ItemStack(ModItems.powder_beryllium.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), BE.dust(), new ItemStack(ModItems.powder_boron.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), B.dust(), new ItemStack(ModFuelItems.powder_coal.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), dust("quartz"), new ItemStack(ModFuelItems.powder_fire.get()), liA);
        // 1.7 dustPhosphorus: HBM 1.7 registers nothing under that name, so this matches only another mod's phosphorus dust
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), dust("phosphorus"), new ItemStack(ModItems.sulfur.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), IRON.dust(), new ItemStack(ModItems.powder_cobalt.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), new ComparableStack(ModItems.powder_strontium.get()), new ItemStack(ModItems.powder_zirconium.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), GOLD.dust(), new ItemStack(ModItems.nugget_mercury.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), PO210.dust(), new ItemStack(ModItems.powder_astatine.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), LA.dust(), new ItemStack(ModItems.powder_cerium.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), AC227.dust(), new ItemStack(ModItems.powder_thorium.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), U.dust(), new ItemStack(ModItems.powder_neptunium.get()), liA);
        makeRecipe(new ComparableStack(ModItems.part_lithium.get()), NP237.dust(), new ItemStack(ModItems.powder_plutonium.get()), liA);
        /// LITHIUM END ///

        /// BERYLLIUM START ///
        int beA = 25;

        makeRecipe(new ComparableStack(ModItems.part_beryllium.get()), LI.dust(), new ItemStack(ModItems.powder_boron.get()), beA);
        makeRecipe(new ComparableStack(ModItems.part_beryllium.get()), dust("quartz"), new ItemStack(ModItems.sulfur.get()), beA);
        makeRecipe(new ComparableStack(ModItems.part_beryllium.get()), TI.dust(), new ItemStack(ModItems.powder_iron.get()), beA);
        makeRecipe(new ComparableStack(ModItems.part_beryllium.get()), CO.dust(), new ItemStack(ModItems.powder_copper.get()), beA);
        makeRecipe(new ComparableStack(ModItems.part_beryllium.get()), new ComparableStack(ModItems.powder_strontium.get()), new ItemStack(ModItems.powder_niobium.get()), beA);
        makeRecipe(new ComparableStack(ModItems.part_beryllium.get()), new ComparableStack(ModItems.powder_cerium.get()), new ItemStack(ModItems.powder_neodymium.get()), beA);
        makeRecipe(new ComparableStack(ModItems.part_beryllium.get()), TH232.dust(), new ItemStack(ModItems.powder_uranium.get()), beA);
        /// BERYLLIUM END ///

        /// CARBON START ///
        int caA = 10;

        makeRecipe(new ComparableStack(ModItems.part_carbon.get()), B.dust(), new ItemStack(ModItems.powder_aluminium.get()), caA);
        makeRecipe(new ComparableStack(ModItems.part_carbon.get()), S.dust(), new ItemStack(ModItems.powder_titanium.get()), caA);
        makeRecipe(new ComparableStack(ModItems.part_carbon.get()), TI.dust(), new ItemStack(ModItems.powder_cobalt.get()), caA);
        makeRecipe(new ComparableStack(ModItems.part_carbon.get()), new ComparableStack(ModItems.powder_caesium.get()), new ItemStack(ModItems.powder_lanthanium.get()), caA);
        makeRecipe(new ComparableStack(ModItems.part_carbon.get()), new ComparableStack(ModItems.powder_neodymium.get()), new ItemStack(ModItems.powder_gold.get()), caA);
        makeRecipe(new ComparableStack(ModItems.part_carbon.get()), new ComparableStack(ModItems.nugget_mercury.get()), new ItemStack(ModItems.powder_polonium.get()), caA);
        makeRecipe(new ComparableStack(ModItems.part_carbon.get()), PB.dust(), new ItemStack(ModItems.powder_ra226.get()), caA);
        makeRecipe(new ComparableStack(ModItems.part_carbon.get()), new ComparableStack(ModItems.powder_astatine.get()), new ItemStack(ModItems.powder_actinium.get()), caA);
        /// CARBON END ///

        /// COPPER START ///
        int coA = 15;

        makeRecipe(new ComparableStack(ModItems.part_copper.get()), BE.dust(), new ItemStack(ModItems.powder_quartz.get()), coA);
        makeRecipe(new ComparableStack(ModItems.part_copper.get()), dust("coal"), new ItemStack(ModItems.powder_bromine.get()), coA);
        makeRecipe(new ComparableStack(ModItems.part_copper.get()), TI.dust(), new ItemStack(ModItems.powder_strontium.get()), coA);
        makeRecipe(new ComparableStack(ModItems.part_copper.get()), IRON.dust(), new ItemStack(ModItems.powder_niobium.get()), coA);
        makeRecipe(new ComparableStack(ModItems.part_copper.get()), new ComparableStack(ModItems.powder_bromine.get()), new ItemStack(ModItems.powder_iodine.get()), coA);
        makeRecipe(new ComparableStack(ModItems.part_copper.get()), new ComparableStack(ModItems.powder_strontium.get()), new ItemStack(ModItems.powder_neodymium.get()), coA);
        makeRecipe(new ComparableStack(ModItems.part_copper.get()), new ComparableStack(ModItems.powder_niobium.get()), new ItemStack(ModItems.powder_caesium.get()), coA);
        makeRecipe(new ComparableStack(ModItems.part_copper.get()), new ComparableStack(ModItems.powder_iodine.get()), new ItemStack(ModItems.powder_polonium.get()), coA);
        makeRecipe(new ComparableStack(ModItems.part_copper.get()), new ComparableStack(ModItems.powder_caesium.get()), new ItemStack(ModItems.powder_actinium.get()), coA);
        makeRecipe(new ComparableStack(ModItems.part_copper.get()), GOLD.dust(), new ItemStack(ModItems.powder_uranium.get()), coA);
        /// COPPER END ///

        /// PLUTONIUM START ///
        int plA = 100;

        makeRecipe(new ComparableStack(ModItems.part_plutonium.get()), dust("phosphorus"), new ItemStack(ModItems.powder_tennessine.get()), plA);
        makeRecipe(new ComparableStack(ModItems.part_plutonium.get()), PU.dust(), new ItemStack(ModItems.powder_tennessine.get()), plA);
        makeRecipe(new ComparableStack(ModItems.part_plutonium.get()), new ComparableStack(ModItems.powder_tennessine.get()), new ItemStack(ModItems.powder_australium.get()), plA);
        makeRecipe(new ComparableStack(ModItems.part_plutonium.get()), new ComparableStack(ModItems.pellet_charged.get()), new ItemStack(ModItems.nugget_schrabidium.get()), 1000);
        /// PLUTONIUM END ///
    }

    /** 1.7 {@code OreDictStack("dust<Name>")} for a name with no port material alias. */
    private static AStack dust(String name) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", "dusts/" + name)));
    }

    /**
     * 1.7 {@code makeRecipe(part, in, out, amat)}; the id is the particle (without {@code part_}) and the input:
     * the item's path, or {@code tag_} and the tag path with slashes as underscores.
     */
    private void makeRecipe(ComparableStack part, AStack in, ItemStack out, int amat) {
        String particle = BuiltInRegistries.ITEM.getKey(part.item).getPath().substring("part_".length());
        String input = in instanceof TagStack tag ? "tag_" + tag.tag.location().getPath().replace('/', '_')
                : BuiltInRegistries.ITEM.getKey(((ComparableStack) in).item).getPath();
        register(particle + "_" + input, new Row(part.item, in, ItemOut.of(out), amat, nextOrder()));
    }
}
