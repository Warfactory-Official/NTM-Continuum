package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidContainerNT;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Assembly machine recipes (1.7 {@code AssemblyMachineRecipes}): the datapack table {@code hbm:assembly_machine}, one
 * file per row in {@code data/hbm/recipe/assembly_machine/}. The rows cover the curated recipes, the
 * generated 1.12.2 bulk of {@code gen/AssemblyMachineRecipesGenerated} and the contributor classes (custom missiles,
 * bombs, RTG, doors, PWR, radars, caps...). 1.7's expensive-mode inputs and 528 pools are the rows'
 * {@code input_items_expensive} and {@code blueprint_pools_528}; the overdrive upgrades exist only outside 528
 * mode ({@code "requires": ["!enable_528"]}), where the precision assembler makes them instead.
 *
 * <p>The fluid packs stay a rule ({@link #arrange}): 1.7 AssemblyMachineRecipes 1058-1066 packs and unpacks every fluid
 * the pack allows (32,000 mB, no {@code FT_NoContainer}), so an addon's fluids get theirs too. As in 1.7 they come right
 * after {@code ass.emptypackage}, the last row of 1.7's table, in {@link Fluids#getInNiceOrder()} from index 1. Recipes are picked in the in-GUI selector
 * ({@link com.hbm.inventory.gui.GUIScreenRecipeSelector}); the {@code blueprint_plate} template gates the pooled plates.
 */
public class AssemblyMachineRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final AssemblyMachineRecipes INSTANCE = new AssemblyMachineRecipes();

    public static final String POOL_PLATES = POOL_PREFIX_ALT + "plates";

    private AssemblyMachineRecipes() {
        super(Constants.MOD_ID, "assembly_machine");
    }

    /** Declares the table at mod construction (its recipe type and serializer register with HBM's content). */
    public static void declare() {}

    @Override
    protected void arrange(List<GenericRecipe> rows) {
        // 1.7 registers the pairs right after ass.emptypackage (its last row); at the end if a datapack removed it
        int at = rows.size();
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).getInternalName().equals("ass.emptypackage")) at = index + 1;
        }
        rows.addAll(at, fluidPacks());
    }

    /** 1.7's package/unpackage pair per fluid the pack allows; ids "ass.package" + 1.7's {@code hbmfluid.<name>}. */
    private static List<GenericRecipe> fluidPacks() {
        List<GenericRecipe> rows = new ArrayList<>();
        ItemFluidContainerNT pack = ModFluidNetItems.fluid_pack_full.get();
        Item empty = ModItems.fluid_pack_empty.get();
        int amount = ModFluidNetItems.FLUID_PACK_CAPACITY;
        FluidType[] order = Fluids.getInNiceOrder();
        for (int i = 1; i < order.length; ++i) { // 1.7: from 1, skipping NONE
            FluidType type = order[i];
            if (type == Fluids.NONE || !pack.allows(type)) continue;
            String suffix = "hbmfluid." + type.getName().toLowerCase(Locale.ROOT);
            rows.add(new GenericRecipe("ass.package" + suffix).setup(40, 100)
                    .inputItems(new ComparableStack(empty))
                    .inputFluids(new FluidStack(type, amount))
                    .outputItems(pack.make(type, amount)));
            rows.add(new GenericRecipe("ass.unpackage" + suffix).setup(40, 100)
                    .setIcon(ItemFluidIcon.make(new FluidStack(type, amount)))
                    .inputItems(new ComparableStack(pack.make(type, amount)))
                    .outputItems(new ItemStack(empty))
                    .outputFluids(new FluidStack(type, amount)));
        }
        return rows;
    }

    /**
     * True if the shape's per-material item is registered ({@code hbm:<prefix>_<name>}): the tag-system
     * equivalent of the 1.12.2 {@code OreDictionary.doesOreNameExist} guard. Vanilla iron/gold/copper also count for
     * INGOT (they back {@code c:ingots/<name>}).
     */
    static boolean hasItem(MaterialShapes shape, NTMMaterial mat) {
        if (shape == MaterialShapes.INGOT
                && (mat.name.equals("iron") || mat.name.equals("gold") || mat.name.equals("copper"))) return true;
        return BuiltInRegistries.ITEM.containsKey(
                Ids.of("hbm", shape.itemPrefix + "_" + mat.name));
    }
}
