package com.hbm.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.inventory.recipes.SawmillRecipes.CraftingResult;
import com.hbm.inventory.recipes.SawmillRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemData;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.ModItems;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * 1.7 {@code TileEntitySawmill.getOutput}, which was code rather than a table: one row per ore-dictionary branch, in
 * its order, with {@code sawdustChance} 0 for sawdust, 0.1 for sticks and 0.5 otherwise.
 */
public final class SawmillTable extends RecipeSource<Row> {

    public SawmillTable() {
        super(SawmillRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        // if(names.contains("stickWood")) return new ItemStack(ModItems.powder_sawdust);
        put("wooden_rods", new TagStack(SawmillRecipes.WOODEN_RODS), ModItems.powder_sawdust.get(), 1, null, 0F, false);
        put("stick", new ComparableStack(Items.STICK), ModItems.powder_sawdust.get(), 1, null, 0F, true); // hidden; c:rods/wooden (1.7 "stickWood") already holds minecraft:stick and comes first, so this row never decides

        // if(names.contains("logWood")) the log's crafted planks, 4 planks become 6
        put("logs", new TagStack(ItemTags.LOGS), Items.OAK_PLANKS, 6, new CraftingResult(ItemTags.PLANKS, 6, 4), 0.5F, false);

        // if(names.contains("plankWood")) return new ItemStack(Items.stick, 6);
        put("planks", new TagStack(ItemTags.PLANKS), Items.STICK, 6, null, 0.1F, false);

        // if(names.contains("treeSapling")) return new ItemStack(Items.stick, 1);
        put("saplings", new TagStack(ItemTags.SAPLINGS), Items.STICK, 1, null, 0.1F, false);
    }

    /** One {@code getOutput} branch; {@code output} is what the recipe viewer shows when {@code crafting} decides it. */
    private void put(String id, AStack input, Item output, int count, CraftingResult crafting, float sawdustChance,
                     boolean hidden) {
        register(id, new Row(input, new ItemSpec(output, count, ItemData.NONE), Optional.ofNullable(crafting),
                sawdustChance, hidden, nextOrder()));
    }
}
