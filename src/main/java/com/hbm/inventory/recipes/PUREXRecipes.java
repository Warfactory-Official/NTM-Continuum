package com.hbm.inventory.recipes;

import com.hbm.blocks.ModFallingBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemZirnoxWaste;
import com.hbm.items.machine.ZirnoxFuel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.TH232;
import static com.hbm.inventory.OreDictManager.ZR;

import java.util.List;

/**
 * PUREX recipe data. ZIRNOX spent fuel must first be cooled in a waste drum. Ported family by family from
 * the 1.12.2 {@code PUREXRecipes}: the uranium-zirconium-hydride billet line, the nine ZIRNOX reprocessing
 * recipes, thorium molten-salt regeneration, ICF pellet recovery, waste vitrification, and the schrabidium
 * extractions.
 *
 * <p><b>Deferred</b> (each blocked on a whole un-ported fuel item family, not on the machine — see
 * IMPLEMENTATION.md §20): the CP-1 pile rods, the seven plate-fuel wastes, the fifteen PWR recipes plus
 * {@code purex.schrabpwr}/{@code purex.schrabmen}, and the twelve Watz pellet recipes.
 */
public final class PUREXRecipes extends GenericRecipes {

    public static final PUREXRecipes INSTANCE = new PUREXRecipes();
    private static final String ZIRNOX_GROUP = "autoswitch.zirnox";
    private static final String SCHRAB_GROUP = "autoswitch.schrab";
    /** Display-name wrappers: "Reprocessing of %s" / "Schrabidium extraction from %s". */
    private static final String RECYCLE = "purex.recycle";
    private static final String SCHRAB = "purex.schrab";
    private static final long ZIRNOX_POWER = 1_000;
    private static final long VITRIFICATION = 1_000;

    private PUREXRecipes() {}

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        // Uranium-zirconium-hydride fuel billets (no group, no name wrapper).
        register(new GenericRecipe("purex.uzh").setup(600, 1_000)
                .inputItems(new ComparableStack(ModItems.billet_uranium_fuel.get()), ZR.billet().count(3))
                .inputFluids(new FluidStack(Fluids.NITRIC_ACID, 1_000), new FluidStack(Fluids.HYDROGEN, 4_000))
                .outputItems(out(ModItems.billet_uzh.get(), 4)));

        registerZirnox("purex.zirnoxnu", ZirnoxFuel.NATURAL_URANIUM,
                out(ModItems.nugget_u238.get(), 1), out(ModItems.nugget_pu_mix.get(), 2),
                out(ModItems.nugget_pu239.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 2));
        registerZirnox("purex.zirnoxmeu", ZirnoxFuel.URANIUM,
                out(ModItems.nugget_pu_mix.get(), 1), out(ModItems.nugget_plutonium.get(), 2),
                out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 2));
        registerZirnox("purex.zirnoxthmeu", ZirnoxFuel.THORIUM,
                out(ModItems.nugget_u238.get(), 1), out(ModItems.nugget_th232.get(), 1),
                out(ModItems.nugget_u233.get(), 2), out(ModItems.nuclear_waste_tiny.get(), 2));
        registerZirnox("purex.zirnoxmox", ZirnoxFuel.MOX,
                out(ModItems.nugget_pu_mix.get(), 1), out(ModItems.nugget_technetium.get(), 1),
                out(ModItems.nugget_u238.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 3));
        registerZirnox("purex.zirnoxmep", ZirnoxFuel.PLUTONIUM,
                out(ModItems.nugget_pu_mix.get(), 2), out(ModItems.nugget_technetium.get(), 1),
                out(ModItems.nuclear_waste_tiny.get(), 3));
        registerZirnox("purex.zirnoxheu233", ZirnoxFuel.U233,
                out(ModItems.nugget_u235.get(), 1), out(ModItems.nugget_neptunium.get(), 1),
                out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 3));
        registerZirnox("purex.zirnoxheu235", ZirnoxFuel.U235,
                out(ModItems.nugget_pu238.get(), 1), out(ModItems.nugget_neptunium.get(), 1),
                out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 3));
        registerZirnox("purex.zirnoxles", ZirnoxFuel.LES,
                out(ModItems.nugget_beryllium.get(), 2), out(ModItems.nugget_pu239.get(), 1),
                out(ModItems.nuclear_waste_tiny.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 2));
        registerZirnox("purex.zirnoxzfbmox", ZirnoxFuel.ZFB_MOX,
                out(ModItems.nugget_zirconium.get(), 3), out(ModItems.nugget_technetium.get(), 1),
                out(ModItems.nugget_pu_mix.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 1));

        // Thorium molten-salt regeneration — the port's first chance-output recipe.
        register(new GenericRecipe("purex.thoriumsalt").setup(100, 10_000)
                .inputItems(TH232.nugget().count(2))
                .inputFluids(new FluidStack(Fluids.THORIUM_SALT_DEPLETED, 16_000))
                .outputFluids(new FluidStack(Fluids.THORIUM_SALT, 16_000))
                .outputItems(out(ModItems.nugget_u233.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 1))
                .outputChances(0.5F, 0.25F)
                .setIcon(ItemFluidIcon.make(Fluids.THORIUM_SALT)));

        // ICF pellet recovery: the spent pellet yields its casing, a fresh charge and iron, plus enough
        // helium-4 for another pellet with 25% surplus.
        register(new GenericRecipe("purex.icf").setup(300, 10_000).setNameWrapper(RECYCLE)
                .inputItems(new ComparableStack(ModItems.icf_pellet_depleted.get()))
                .outputItems(out(ModItems.icf_pellet_empty.get(), 1), out(ModItems.pellet_charged.get(), 1),
                        out(ModItems.powder_iron.get(), 1))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_250))
                .setIcon(new ItemStack(ModItems.icf_pellet_depleted.get())));

        // Vitrification: lead sand binds liquid, gaseous or solid waste into glass.
        registerVitrification("purex.vitliquid", 100, new FluidStack(Fluids.WASTEFLUID, 1_000),
                new AStack[]{new ComparableStack(ModFallingBlocks.sand_lead.get().asItem())}, 1);
        registerVitrification("purex.vitgaseous", 100, new FluidStack(Fluids.WASTEGAS, 1_000),
                new AStack[]{new ComparableStack(ModFallingBlocks.sand_lead.get().asItem())}, 1);
        registerVitrification("purex.vitsolid", 300, null,
                new AStack[]{new ComparableStack(ModFallingBlocks.sand_lead.get().asItem()),
                        new ComparableStack(ModItems.nuclear_waste.get(), 4)}, 4);

        register(new GenericRecipe("purex.schraranium").setup(200, 1_000).setNameWrapper(SCHRAB)
                .inputItems(new ComparableStack(ModFuelItems.ingot_schraranium.get()))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 2_000), new FluidStack(Fluids.NITRIC_ACID, 1_000))
                .outputItems(out(ModItems.nugget_schrabidium.get(), 3), out(ModItems.nugget_uranium.get(), 3),
                        out(ModItems.nugget_neptunium.get(), 2))
                .setIcon(new ItemStack(ModFuelItems.ingot_schraranium.get())));

        register(new GenericRecipe("purex.schrabzirnox").setup(200, 50_000).setNameWrapper(SCHRAB)
                .inputItems(new ColdZirnoxWasteStack(ModZirnoxItems.waste(ZirnoxFuel.PLUTONIUM)))
                .inputFluids(new FluidStack(Fluids.SOLVENT, 4_000), new FluidStack(Fluids.SCHRABIDIC, 500))
                .outputItems(out(ModItems.powder_schrabidium.get(), 1), out(ModItems.nugget_technetium.get(), 3),
                        out(ModItems.nuclear_waste_tiny.get(), 4))
                .setGroup(SCHRAB_GROUP, this)
                .setIcon(new ItemStack(ModZirnoxItems.waste(ZirnoxFuel.PLUTONIUM))));
    }

    public boolean matchesInput(ItemStack stack) {
        registerRecipes();
        return recipeOrderedList.stream().anyMatch(recipe -> recipe.inputItem != null
                && recipe.inputItem.length > 0 && recipe.inputItem[0].matchesRecipe(stack, true));
    }

    private void registerZirnox(String name, ZirnoxFuel fuel, ItemStack... outputs) {
        ItemZirnoxWaste waste = ModZirnoxItems.waste(fuel);
        GenericRecipe recipe = new GenericRecipe(name).setup(100, ZIRNOX_POWER).setNameWrapper(RECYCLE)
                .inputItems(new ColdZirnoxWasteStack(waste))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(outputs).setGroup(ZIRNOX_GROUP, this)
                .setIcon(new ItemStack(waste));
        register(recipe);
    }

    /** A vitrification recipe: lead sand (+ optional solid waste / waste fluid) → vitrified waste. */
    private void registerVitrification(String name, int duration, FluidStack inputFluid, AStack[] inputs, int outputCount) {
        GenericRecipe recipe = new GenericRecipe(name).setup(duration, VITRIFICATION)
                .inputItems(inputs)
                .outputItems(out(ModItems.nuclear_waste_vitrified.get(), outputCount));
        if (inputFluid != null) recipe.inputFluids(inputFluid);
        register(recipe);
    }

    private static ItemStack out(Item item, int count) {
        return new ItemStack(item, count);
    }

    /** Exact spent-fuel identity plus the absence of the hot-state component; explicit false is not cold. */
    private static final class ColdZirnoxWasteStack extends AStack {
        private final ItemZirnoxWaste waste;

        private ColdZirnoxWasteStack(ItemZirnoxWaste waste) {
            this.waste = waste;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return !stack.isEmpty() && stack.getItem() == waste && !ModDataComponents.hasWasteHot(stack)
                    && (ignoreSize || stack.getCount() >= stacksize);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            return List.of(new ItemStack(waste, stacksize));
        }
    }
}
