package com.hbm.inventory.recipes;

import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.items.ModPwrItems;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.ModWatzItems;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.EnumWatzType;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.items.machine.ZirnoxFuel;
import dev.golem.core.Ids;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.AU198;
import static com.hbm.inventory.OreDictManager.PB209;
import static com.hbm.inventory.OreDictManager.TH232;
import static com.hbm.inventory.OreDictManager.ZR;

import java.util.List;

/**
 * PUREX recipe data, ported from the 1.7 {@code PUREXRecipes} in its order: the uranium-zirconium-hydride
 * billet line, balefire gold / flashlead, the three CP-1 pile rods, the nine ZIRNOX and seven plate-fuel
 * reprocessing recipes, the fifteen PWR ones, thorium molten-salt regeneration, the Watz pellets (the two
 * naquadah ones only when a {@code c:nuggets/naquadria} item exists, as 1.7's {@code nuggetNaquadria} gate),
 * ICF pellet recovery, waste vitrification, and the schrabidium extractions. ZIRNOX and plate spent fuel
 * must first be cooled in a waste drum.
 */
public final class PUREXRecipes extends GenericRecipes {

    public static final PUREXRecipes INSTANCE = new PUREXRecipes();
    private static final String ZIRNOX_GROUP = "autoswitch.zirnox";
    private static final String PWR_GROUP = "autoswitch.pwr";
    private static final String SCHRAB_GROUP = "autoswitch.schrab";
    /** Display-name wrappers: "Reprocessing of %s" / "Schrabidium extraction from %s". */
    private static final String RECYCLE = "purex.recycle";
    private static final String SCHRAB = "purex.schrab";
    private static final long ZIRNOX_POWER = 1_000;
    private static final long PWR_POWER = 2_500;
    private static final long PILE_POWER = 100;
    private static final long PLATE_POWER = 1_500;
    private static final long WATZ_POWER = 10_000;
    private static final String PILE_GROUP = "autoswitch.pile";
    private static final String PLATE_GROUP = "autoswitch.plate";
    private static final String WATZ_GROUP = "autoswitch.watz";
    /** 1.7 uses 250 mB of schrabidic acid for all three extractions (1.12.2 raised it to 500). */
    private static final int SCHRABIDIC_PER_EXTRACTION = 250;
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

        // Balefire gold / flashlead billets from antimatter.
        register(new GenericRecipe("purex.flashgold").setup(600, 1_000)
                .inputItems(AU198.billet(), new ComparableStack(ModItems.pellet_charged.get()))
                .inputFluids(new FluidStack(Fluids.AMAT, 1_000))
                .outputItems(out(ModItems.billet_balefire_gold.get(), 2)));
        register(new GenericRecipe("purex.flashlead").setup(600, 1_000)
                .inputItems(PB209.billet(), new ComparableStack(ModItems.billet_balefire_gold.get()))
                .inputFluids(new FluidStack(Fluids.AMAT, 1_000))
                .outputItems(out(ModItems.billet_flashlead.get(), 1)));

        // CP-1 spent pile rods (1.7 has no pilethorium; that recipe is a 1.12.2 addition).
        registerPile("purex.pilepu239", EnumPileRod.PU239,
                out(ModItems.billet_pu239.get(), 2), out(ModItems.billet_uranium.get(), 1));
        registerPile("purex.pilergp", EnumPileRod.RGP,
                out(ModItems.billet_pu_mix.get(), 2), out(ModItems.billet_nuclear_waste.get(), 1));
        registerPile("purex.pilewaste", EnumPileRod.WASTE,
                out(ModItems.billet_nuclear_waste.get(), 2), out(ModItems.billet_polonium.get(), 1));

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

        // Research-reactor plate fuel, once cooled (1.7 ComparableStack meta 0 = the cooled subtype).
        registerPlate("purex.platemox", ModNuclearMiscItems.WASTE_PLATE_MOX.get(),
                out(ModItems.powder_sr90_tiny.get(), 1), out(ModItems.nugget_pu_mix.get(), 3),
                out(ModItems.powder_cs137_tiny.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 4));
        registerPlate("purex.platepu238be", ModNuclearMiscItems.WASTE_PLATE_PU238BE.get(),
                out(ModItems.nugget_beryllium.get(), 1), out(ModItems.nugget_pu238.get(), 1),
                out(ModFuelItems.powder_coal_tiny.get(), 2), out(ModItems.nugget_lead.get(), 2));
        registerPlate("purex.platepu239", ModNuclearMiscItems.WASTE_PLATE_PU239.get(),
                out(ModItems.nugget_pu240.get(), 2), out(ModItems.nugget_technetium.get(), 1),
                out(ModItems.powder_cs137_tiny.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 5));
        registerPlate("purex.platera226be", ModNuclearMiscItems.WASTE_PLATE_RA226BE.get(),
                out(ModItems.nugget_beryllium.get(), 2), out(ModItems.nugget_polonium.get(), 2),
                out(ModFuelItems.powder_coal_tiny.get(), 1), out(ModItems.nugget_lead.get(), 1));
        registerPlate("purex.platesa326", ModNuclearMiscItems.WASTE_PLATE_SA326.get(),
                out(ModItems.nugget_solinium.get(), 1), out(ModItems.powder_neodymium_tiny.get(), 1),
                out(ModItems.nugget_tantalium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 6));
        registerPlate("purex.plateu233", ModNuclearMiscItems.WASTE_PLATE_U233.get(),
                out(ModItems.nugget_u235.get(), 1), out(ModItems.powder_i131_tiny.get(), 1),
                out(ModItems.powder_sr90_tiny.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 6));
        registerPlate("purex.plateu235", ModNuclearMiscItems.WASTE_PLATE_U235.get(),
                out(ModItems.nugget_neptunium.get(), 1), out(ModItems.nugget_pu238.get(), 1),
                out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 6));

        // PWR spent fuel, once cooled in a waste drum.
        registerPWR("purex.pwrmeu", EnumPWRFuel.MEU,
                out(ModItems.nugget_u238.get(), 3), out(ModItems.nugget_plutonium.get(), 4), out(ModItems.nugget_technetium.get(), 2), out(ModItems.nuclear_waste_tiny.get(), 3));
        registerPWR("purex.pwrheu233", EnumPWRFuel.HEU233,
                out(ModItems.nugget_u235.get(), 3), out(ModItems.nugget_pu238.get(), 3), out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 5));
        registerPWR("purex.pwrheu235", EnumPWRFuel.HEU235,
                out(ModItems.nugget_neptunium.get(), 3), out(ModItems.nugget_pu238.get(), 3), out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 5));
        registerPWR("purex.pwrmen", EnumPWRFuel.MEN,
                out(ModItems.nugget_u238.get(), 3), out(ModItems.nugget_pu239.get(), 4), out(ModItems.nugget_technetium.get(), 2), out(ModItems.nuclear_waste_tiny.get(), 3));
        registerPWR("purex.pwrhen237", EnumPWRFuel.HEN237,
                out(ModItems.nugget_pu238.get(), 2), out(ModItems.nugget_pu239.get(), 4), out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 5));
        registerPWR("purex.pwrmox", EnumPWRFuel.MOX,
                out(ModItems.nugget_u238.get(), 3), out(ModItems.nugget_pu240.get(), 4), out(ModItems.nugget_technetium.get(), 2), out(ModItems.nuclear_waste_tiny.get(), 3));
        registerPWR("purex.pwrmep", EnumPWRFuel.MEP,
                out(ModItems.nugget_lead.get(), 2), out(ModItems.nugget_pu_mix.get(), 4), out(ModItems.nugget_technetium.get(), 2), out(ModItems.nuclear_waste_tiny.get(), 3));
        registerPWR("purex.pwrhep239", EnumPWRFuel.HEP239,
                out(ModItems.nugget_pu_mix.get(), 2), out(ModItems.nugget_pu240.get(), 4), out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 5));
        registerPWR("purex.pwrhep241", EnumPWRFuel.HEP241,
                out(ModItems.nugget_lead.get(), 3), out(ModItems.nugget_zirconium.get(), 2), out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 6));
        registerPWR("purex.pwrmea", EnumPWRFuel.MEA,
                out(ModItems.nugget_lead.get(), 3), out(ModItems.nugget_zirconium.get(), 2), out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 6));
        registerPWR("purex.pwrhea242", EnumPWRFuel.HEA242,
                out(ModItems.nugget_lead.get(), 3), out(ModItems.nugget_zirconium.get(), 2), out(ModItems.nugget_technetium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 6));
        registerPWR("purex.pwrhes326", EnumPWRFuel.HES326,
                out(ModItems.nugget_solinium.get(), 3), out(ModItems.nugget_lead.get(), 2), out(ModItems.nugget_euphemium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 6));
        registerPWR("purex.pwrhes327", EnumPWRFuel.HES327,
                out(ModItems.nugget_australium.get(), 4), out(ModItems.nugget_lead.get(), 1), out(ModItems.nugget_euphemium.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 6));
        registerPWR("purex.pwrbfbam", EnumPWRFuel.BFB_AM_MIX,
                out(ModItems.nugget_am_mix.get(), 9), out(ModItems.nugget_pu_mix.get(), 2), out(ModItems.nugget_bismuth.get(), 6), out(ModItems.nuclear_waste_tiny.get(), 1));
        registerPWR("purex.pwrbfpu241", EnumPWRFuel.BFB_PU241,
                out(ModItems.nugget_pu241.get(), 9), out(ModItems.nugget_pu_mix.get(), 2), out(ModItems.nugget_bismuth.get(), 6), out(ModItems.nuclear_waste_tiny.get(), 1));

        // Thorium molten-salt regeneration — the port's first chance-output recipe.
        register(new GenericRecipe("purex.thoriumsalt").setup(20, 10_000)
                .inputItems(TH232.nugget().count(2))
                .inputFluids(new FluidStack(Fluids.THORIUM_SALT_DEPLETED, 16_000))
                .outputFluids(new FluidStack(Fluids.THORIUM_SALT, 16_000))
                .outputItems(out(ModItems.nugget_u233.get(), 1), out(ModItems.nuclear_waste_tiny.get(), 1))
                .outputChances(0.5F, 0.25F)
                .setIcon(ItemFluidIcon.make(Fluids.THORIUM_SALT)));

        // Watz depleted pellets: two product stacks + 2 nuclear waste, and 1 000 mB of Watz mud each.
        registerWatz("purex.watzschrab", EnumWatzType.SCHRABIDIUM,
                out(ModItems.nugget_solinium.get(), 15), out(ModItems.nugget_euphemium.get(), 3));
        registerWatz("purex.watzhes", EnumWatzType.HES,
                out(ModItems.nugget_solinium.get(), 17), out(ModItems.nugget_euphemium.get(), 1));
        registerWatz("purex.watzmes", EnumWatzType.MES,
                out(ModItems.nugget_solinium.get(), 12), out(ModItems.nugget_tantalium.get(), 6));
        registerWatz("purex.watzles", EnumWatzType.LES,
                out(ModItems.nugget_solinium.get(), 9), out(ModItems.nugget_tantalium.get(), 9));
        registerWatz("purex.watzhen", EnumWatzType.HEN,
                out(ModItems.nugget_pu239.get(), 12), out(ModItems.nugget_technetium.get(), 6));
        registerWatz("purex.watzmeu", EnumWatzType.MEU,
                out(ModItems.nugget_pu239.get(), 12), out(ModItems.nugget_bismuth.get(), 6));
        registerWatz("purex.watzmep", EnumWatzType.MEP,
                out(ModItems.nugget_pu241.get(), 12), out(ModItems.nugget_bismuth.get(), 6));
        registerWatz("purex.watzlead", EnumWatzType.LEAD,
                out(ModItems.nugget_lead.get(), 6), out(ModItems.nugget_bismuth.get(), 12));
        registerWatz("purex.watzboron", EnumWatzType.BORON,
                out(ModFuelItems.powder_coal_tiny.get(), 12), out(ModItems.nugget_co60.get(), 6));
        registerWatz("purex.watzdu", EnumWatzType.DU,
                out(ModItems.nugget_polonium.get(), 12), out(ModItems.nugget_pu238.get(), 6));

        // 1.7 registers both naquadah recipes only when another mod supplies "nuggetNaquadria" (HBM has
        // none); the NQD recipe outputs twelve of that ore-dictionary entry's first item. The tag lookup
        // runs at lazy recipe registration (machine/JEI first use), after tags have bound.
        Item naquadria = firstInTag(TagKey.create(Registries.ITEM, Ids.of("c", "nuggets/naquadria")));
        if (naquadria != null) {
            registerWatz("purex.watznaqadah", EnumWatzType.NQD,
                    out(naquadria, 12), out(ModItems.nugget_euphemium.get(), 6));
            registerWatz("purex.watznaqadria", EnumWatzType.NQR,
                    out(ModItems.nugget_co60.get(), 12), out(ModItems.nugget_euphemium.get(), 6));
        }

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
                .inputItems(new ColdWasteStack(ModZirnoxItems.waste(ZirnoxFuel.PLUTONIUM)))
                .inputFluids(new FluidStack(Fluids.SOLVENT, 4_000), new FluidStack(Fluids.SCHRABIDIC, SCHRABIDIC_PER_EXTRACTION))
                .outputItems(out(ModItems.powder_schrabidium.get(), 1), out(ModItems.nugget_technetium.get(), 3),
                        out(ModItems.nuclear_waste_tiny.get(), 4))
                .setGroup(SCHRAB_GROUP, this)
                .setIcon(new ItemStack(ModZirnoxItems.waste(ZirnoxFuel.PLUTONIUM))));

        registerSchrabPWR("purex.schrabpwr", EnumPWRFuel.MEP);
        registerSchrabPWR("purex.schrabmen", EnumPWRFuel.MEN);
    }

    public boolean matchesInput(ItemStack stack) {
        registerRecipes();
        return recipeOrderedList.stream().anyMatch(recipe -> recipe.inputItem != null
                && recipe.inputItem.length > 0 && recipe.inputItem[0].matchesRecipe(stack, true));
    }

    private void registerPWR(String name, EnumPWRFuel fuel, ItemStack... outputs) {
        ItemStack spent = new ItemStack(ModPwrItems.depleted(fuel));
        register(new GenericRecipe(name).setup(100, PWR_POWER).setNameWrapper(RECYCLE)
                .inputItems(new ComparableStack(spent))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(outputs).setGroup(PWR_GROUP, this)
                .setIcon(spent));
    }

    private void registerSchrabPWR(String name, EnumPWRFuel fuel) {
        ItemStack spent = new ItemStack(ModPwrItems.depleted(fuel));
        register(new GenericRecipe(name).setup(200, 50_000).setNameWrapper(SCHRAB)
                .inputItems(new ComparableStack(spent))
                .inputFluids(new FluidStack(Fluids.SOLVENT, 4_000), new FluidStack(Fluids.SCHRABIDIC, SCHRABIDIC_PER_EXTRACTION))
                .outputItems(out(ModItems.powder_schrabidium.get(), 1), out(ModItems.nugget_technetium.get(), 3),
                        out(ModItems.nuclear_waste_tiny.get(), 4))
                .setGroup(SCHRAB_GROUP, this)
                .setIcon(spent));
    }

    private void registerZirnox(String name, ZirnoxFuel fuel, ItemStack... outputs) {
        Item waste = ModZirnoxItems.waste(fuel);
        register(new GenericRecipe(name).setup(100, ZIRNOX_POWER).setNameWrapper(RECYCLE)
                .inputItems(new ColdWasteStack(waste))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(outputs).setGroup(ZIRNOX_GROUP, this)
                .setIcon(new ItemStack(waste)));
    }

    private void registerPlate(String name, Item waste, ItemStack... outputs) {
        register(new GenericRecipe(name).setup(100, PLATE_POWER).setNameWrapper(RECYCLE)
                .inputItems(new ColdWasteStack(waste))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(outputs).setGroup(PLATE_GROUP, this)
                .setIcon(new ItemStack(waste)));
    }

    /** 1.7 {@code ComparableStack(pile_rod, 1, rod)}: the rod's variant only, whatever its burn-up data. */
    private void registerPile(String name, EnumPileRod rod, ItemStack... outputs) {
        Item item = ModPileBlocks.pile_rod.get();
        register(new GenericRecipe(name).setup(40, PILE_POWER).setNameWrapper(RECYCLE)
                .inputItems(new VariantStack(item, rod.ordinal()))
                .inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 100))
                .outputItems(outputs).setGroup(PILE_GROUP, this)
                .setIcon(ModPileBlocks.pile_rod.get().of(rod)));
    }

    /** Every 1.7 Watz recipe: the two product stacks, then 2 nuclear waste, plus 1 000 mB of Watz mud. */
    private void registerWatz(String name, EnumWatzType type, ItemStack first, ItemStack second) {
        Item spent = ModWatzItems.depleted(type);
        register(new GenericRecipe(name).setup(60, WATZ_POWER).setNameWrapper(RECYCLE)
                .inputItems(new ComparableStack(spent))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(first, second, out(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))
                .setGroup(WATZ_GROUP, this)
                .setIcon(new ItemStack(spent)));
    }

    /** The first item of a tag, or null: 1.7's {@code OreDictionary.getOres(name).get(0)} guard. */
    private static Item firstInTag(TagKey<Item> tag) {
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) return holder.value();
        return null;
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
    private static final class ColdWasteStack extends AStack {
        private final Item waste;

        private ColdWasteStack(Item waste) {
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

    /** 1.7 {@code new ComparableStack(item, 1, meta)} of a metadata family kept on one item via ITEM_VARIANT. */
    private static final class VariantStack extends AStack {
        private final Item item;
        private final int variant;

        private VariantStack(Item item, int variant) {
            this.item = item;
            this.variant = variant;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return !stack.isEmpty() && stack.getItem() == item
                    && ModDataComponents.ITEM_VARIANT.getOr(stack, 0) == variant
                    && (ignoreSize || stack.getCount() >= stacksize);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            ItemStack stack = new ItemStack(item, stacksize);
            if (variant != 0) ModDataComponents.ITEM_VARIANT.set(stack, variant);
            return List.of(stack);
        }
    }
}
