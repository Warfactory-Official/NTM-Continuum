package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.ANY_HARDPLASTIC;
import static com.hbm.inventory.OreDictManager.ANY_PLASTIC;
import static com.hbm.inventory.OreDictManager.ANY_RESISTANTALLOY;
import static com.hbm.inventory.OreDictManager.BI;
import static com.hbm.inventory.OreDictManager.FERRO;
import static com.hbm.inventory.OreDictManager.RUBBER;
import static com.hbm.inventory.OreDictManager.SI;
import static com.hbm.inventory.OreDictManager.SR;
import static com.hbm.inventory.OreDictManager.STEEL;
import static com.hbm.inventory.OreDictManager.U;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.inventory.recipes.AnnihilatorRecipes.MilestoneRow;
import com.hbm.inventory.recipes.AnnihilatorRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.items.weapon.ItemAmmoArty.ArtilleryShellType;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code AnnihilatorRecipes.registerDefaults}. */
public final class AnnihilatorTable extends RecipeSource<Row> {

    /** The 1.7 config branch the rows sit in (a row's {@code requires}). */
    private List<String> requires = List.of();

    public AnnihilatorTable() {
        super(AnnihilatorRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        // if(GeneralConfig.enable528) {
        requires = List.of("enable_528");
        put(STEEL.ingot(),                    new AnnihilatorRecipe(256, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "steel")));
        put(SI.billet(),                      new AnnihilatorRecipe(256, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "chip")));
        put(BI.nugget(),                      new AnnihilatorRecipe(128, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "chip_bismoid")));
        put(ModItems.pellet_charged.get(),    new AnnihilatorRecipe(1024, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "chip_quantum")));

        put(U.billet(),                       new AnnihilatorRecipe(256, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "gascent")));
        put(ANY_PLASTIC.ingot(),              new AnnihilatorRecipe(512, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "plastic")));
        put(RUBBER.ingot(),                   new AnnihilatorRecipe(512, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "rubber")));
        put(FERRO.ingot(),                    new AnnihilatorRecipe(1024, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "ferrouranium")));
        put(SR.dust(),                        new AnnihilatorRecipe(256, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "strontium")));
        put(ANY_HARDPLASTIC.ingot(),          new AnnihilatorRecipe(1024, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "hardplastic")));
        put(ANY_RESISTANTALLOY.ingot(),       new AnnihilatorRecipe(1024, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "tcalloy")));
        put(ModItems.powder_chlorophyte.get(), new AnnihilatorRecipe(1024, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "chlorophyte")));

        put(new ComparableStack(ModIntermediateItems.drive(EnumDriveType.FLASH_FLIGHTSIM)),  new AnnihilatorRecipe(64, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "soyuz")));
        put(new ComparableStack(ModAmmoItems.bmg50_fmj.get()),                           new AnnihilatorRecipe(256, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "bmg")));
        put(new ComparableStack(ModAmmoItems.ammo_arty.get(ArtilleryShellType.NORMAL)),  new AnnihilatorRecipe(128, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "arty")));
        put(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER)), new AnnihilatorRecipe(128, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "controller")));
        // }
        requires = List.of();
    }

    /** 1.7 {@code new AnnihilatorRecipe(new Pair(new BigInteger(amount), payout))}: one milestone. */
    private record AnnihilatorRecipe(long amount, ItemStack payout) {}

    /** 1.7 {@code recipes.put(item, recipe)}; the id is the item's name. */
    private void put(Item item, AnnihilatorRecipe recipe) {
        register(BuiltInRegistries.ITEM.getKey(item).getPath(),
                new Row(Optional.of(item), Optional.empty(), Optional.empty(), milestones(recipe), requires));
    }

    /** 1.7 {@code recipes.put(new ComparableStack(item), recipe)}: keyed by the item. */
    private void put(ComparableStack stack, AnnihilatorRecipe recipe) {
        put(stack.item, recipe);
    }

    /** 1.7 {@code recipes.put(MAT.shape(), recipe)}, keyed by the ore name; the id is the tag path with {@code _} for {@code /}. */
    private void put(RecipesCommon.AStack tag, AnnihilatorRecipe recipe) {
        if (tag instanceof ComparableStack stack) {
            put(stack, recipe);
            return;
        }
        String name = ((TagStack) tag).tag.location().toString();
        register(((TagStack) tag).tag.location().getPath().replace('/', '_'),
                new Row(Optional.empty(), Optional.of(name), Optional.empty(), milestones(recipe), requires));
    }

    private static List<MilestoneRow> milestones(AnnihilatorRecipe recipe) {
        return List.of(new MilestoneRow(recipe.amount(), ItemSpec.of(recipe.payout()).getOrThrow()));
    }
}
