package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.BIGMT;
import static com.hbm.inventory.OreDictManager.CO;
import static com.hbm.inventory.OreDictManager.CU;
import static com.hbm.inventory.OreDictManager.GOLD;
import static com.hbm.inventory.OreDictManager.IRON;
import static com.hbm.inventory.OreDictManager.SA326;
import static com.hbm.inventory.OreDictManager.STEEL;
import static com.hbm.inventory.OreDictManager.TC99;
import static com.hbm.inventory.OreDictManager.W;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.RecipeIngredients;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidContainerNT;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import dev.golem.core.Ids;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code BlastFurnaceRecipes.registerDefaults} (the legacy alloy furnace). 1.7's rows have no names; the port names
 * them {@code difurnace.<product>}, and every row states the furnace's fixed processing time (1.7
 * {@code TileEntityDiFurnace.processingSpeed}) as its duration.
 */
public final class DiFurnaceTable extends GenericTableSource<GenericRecipe> {

    private static final NTMMaterial COAL = Mats.MAT_COAL;
    private static final NTMMaterial REDSTONE = Mats.MAT_REDSTONE;
    // 1.7 DictFrame ANY_COKE: of its ingot/plate/gem/dust names only "gemAnyCoke" has members, the three cokes, which are c:cokes
    private static final TagKey<Item> ANY_COKE = TagKey.create(Registries.ITEM, Ids.of("c", "cokes"));
    // 1.7 KEY_SLIME, the ore name "slimeball"
    private static final TagKey<Item> SLIME_BALLS = TagKey.create(Registries.ITEM, Ids.of("c", "slime_balls"));
    private static final int PROCESSING_SPEED = 400;
    private static final String LBSM_CHEMISTRY = "lbsm_simple_chemistry";

    public DiFurnaceTable() {
        super(DiFurnaceRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        // STEEL
        addRecipe("steelFromIron", IRON, COAL, new ItemStack(ModItems.ingot_steel.get(), 1));
        addRecipe("steelFromCoke", IRON, ANY_COKE, new ItemStack(ModItems.ingot_steel.get(), 1));
        addRecipe("steelFromOre", IRON.ore(), COAL, new ItemStack(ModItems.ingot_steel.get(), 2));
        addRecipe("steelFromOreCoke", IRON.ore(), ANY_COKE, new ItemStack(ModItems.ingot_steel.get(), 3));
        addRecipe("steelWithFlux", IRON.ore(), new ComparableStack(ModItems.powder_flux.get()), new ItemStack(ModItems.ingot_steel.get(), 3));

        addRecipe("redCopper", CU, REDSTONE, new ItemStack(ModItems.ingot_red_copper.get(), 2));
        // 1.7 canister_full of gasoline matched by its fluid metadata only (any fill); the ore name "slimeball" is c:slime_balls
        addRecipe("napalm", new RecipeIngredients.FluidItem(ModFluidNetItems.canister_full.get(), Fluids.GASOLINE, 1), SLIME_BALLS, new ItemStack(ModItems.canister_napalm.get()));
        addRecipe("magnetizedTungsten", W, SA326.nugget(), new ItemStack(ModItems.ingot_magnetized_tungsten.get()));
        addRecipe("tcalloy", STEEL, TC99.nugget(), new ItemStack(ModItems.ingot_tcalloy.get()));
        addRecipe("paa", GOLD.plate(), ModItems.plate_mixed.get(), new ItemStack(ModItems.plate_paa.get(), 2));
        addRecipe("starmetal", BIGMT, ModAbilityToolItems.ingot_meteorite.get(), new ItemStack(ModItems.ingot_starmetal.get(), 2));
        addRecipe("meteorite", CO, ModItems.powder_meteorite.get(), new ItemStack(ModAbilityToolItems.ingot_meteorite.get()));
        addRecipe("meteorSword", ModAbilityToolItems.meteorite_sword_hardened.get(), CO, new ItemStack(ModAbilityToolItems.meteorite_sword_alloyed.get()));

        // 1.7: if(GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleChemsitry); the row carries "requires": ["lbsm_simple_chemistry"]
        addRecipe("canisterOil", LBSM_CHEMISTRY, ModItems.canister_empty.get(), COAL, canisterFull(Fluids.OIL));

        // 1.7's IMC buffer (IMCBlastFurnace) is another mod's rows: here those are data files of the table
    }

    /** 1.7 {@code addRecipe(in1, in2, out)}: a material stands for its ingot, plate, gem and dust; an item for itself. */
    private void addRecipe(String name, Object in1, Object in2, ItemStack out) {
        register(recipe(name, in1, in2, out));
    }

    /** A row 1.7 added only under a config branch: {@code requires} the flag, id {@code <name>.<flag>}. */
    private void addRecipe(String name, String flag, Object in1, Object in2, ItemStack out) {
        GenericRecipe recipe = recipe(name, in1, in2, out).requires(flag);
        register(id(recipe) + "." + flag, recipe);
    }

    private static GenericRecipe recipe(String name, Object in1, Object in2, ItemStack out) {
        return new GenericRecipe("difurnace." + name).setup(PROCESSING_SPEED, 0)
                .inputItems(stack(in1), stack(in2)).outputItems(out);
    }

    /** 1.7 {@code new ItemStack(ModItems.canister_full, 1, fluid.getID())}: a full canister of the fluid. */
    private static ItemStack canisterFull(FluidType fluid) {
        ItemFluidContainerNT canister = ModFluidNetItems.canister_full.get();
        return canister.make(fluid, canister.capacity);
    }

    /** 1.7 {@code getRecipeStacks}. */
    @SuppressWarnings("unchecked")
    private static AStack stack(Object in) {
        if (in instanceof NTMMaterial frame) {
            return new RecipeIngredients.AnyOf(List.of(frame.ingot(), frame.plate(), frame.gem(), frame.dust()), 1);
        }
        if (in instanceof AStack stack) return stack;
        if (in instanceof TagKey<?> tag) return new TagStack((TagKey<Item>) tag);
        if (in instanceof Item item) return new ComparableStack(item);
        throw new IllegalArgumentException("not a furnace input: " + in);
    }
}
