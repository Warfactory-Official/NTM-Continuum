package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.RecipeIngredients;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.ModPwrItems;
import com.hbm.items.ModWatzItems;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.EnumWatzType;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import dev.golem.core.world.ItemStacks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code PUREXRecipes.registerDefaults}. */
public final class PurexTable extends GenericTableSource<GenericRecipe> {

    /** 1.7 {@code OreDictionary.getOres("nuggetNaquadria").size() != 0}: another mod supplies a naquadria nugget. */
    private static final String NAQUADRIA = "#c:nuggets/naquadria";
    private static final net.minecraft.tags.TagKey<Item> NAQUADRIA_NUGGETS = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, dev.golem.core.Ids.of("c", "nuggets/naquadria"));

    public PurexTable() {
        super(PUREXRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {

        long pilePower = 100;
        long zirnoxPower = 1_000;
        long platePower = 1_500;
        long pwrPower = 2_500;
        long watzPower = 10_000;
        long vitrification = 1_000;

        this.register(new GenericRecipe("purex.uzh").setup(600, 1_000)
                .inputItems(new ComparableStack(ModItems.billet_uranium_fuel.get()),
                        ZR.billet().count(3))
                .inputFluids(new FluidStack(Fluids.NITRIC_ACID, 1_000), new FluidStack(Fluids.HYDROGEN, 4000))
                .outputItems(new ItemStack(ModItems.billet_uzh.get(), 4)));

        this.register(new GenericRecipe("purex.flashgold").setup(600, 1_000)
                .inputItems(AU198.billet(),
                        new ComparableStack(ModItems.pellet_charged.get()))
                .inputFluids(new FluidStack(Fluids.AMAT, 1_000))
                .outputItems(new ItemStack(ModItems.billet_balefire_gold.get(), 2)));

        this.register(new GenericRecipe("purex.flashlead").setup(600, 1_000)
                .inputItems(PB209.billet(),
                        new ComparableStack(ModItems.billet_balefire_gold.get()))
                .inputFluids(new FluidStack(Fluids.AMAT, 1_000))
                .outputItems(new ItemStack(ModItems.billet_flashlead.get(), 1)));

        //CP-1
        String autoPile = "autoswitch.pile";
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pilepu239").setup(40, pilePower).setNameWrapper("purex.recycle").setGroup(autoPile, this)
                .inputItems(new ComparableStack(ModPileBlocks.pile_rod.get(EnumPileRod.PU239), 1))
                .inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 100))
                .outputItems(new ItemStack(ModItems.billet_pu239.get(), 2),
                        new ItemStack(ModItems.billet_uranium.get(), 1))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pilergp").setup(40, pilePower).setNameWrapper("purex.recycle").setGroup(autoPile, this)
                .inputItems(new ComparableStack(ModPileBlocks.pile_rod.get(EnumPileRod.RGP), 1))
                .inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 100))
                .outputItems(new ItemStack(ModItems.billet_pu_mix.get(), 2),
                        new ItemStack(ModItems.billet_nuclear_waste.get(), 1))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pilewaste").setup(40, pilePower).setNameWrapper("purex.recycle").setGroup(autoPile, this)
                .inputItems(new ComparableStack(ModPileBlocks.pile_rod.get(EnumPileRod.WASTE), 1))
                .inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 100))
                .outputItems(new ItemStack(ModItems.billet_nuclear_waste.get(), 2),
                        new ItemStack(ModItems.billet_polonium.get(), 1))));

        // ZIRNOX
        String autoZirnox = "autoswitch.zirnox";
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.zirnoxnu").setup(100, zirnoxPower).setNameWrapper("purex.recycle").setGroup(autoZirnox, this)
                .inputItems(cold(ModZirnoxItems.waste(ZirnoxFuel.NATURAL_URANIUM)))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_u238.get(), 1),
                        new ItemStack(ModItems.nugget_pu_mix.get(), 2),
                        new ItemStack(ModItems.nugget_pu239.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 2))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.zirnoxmeu").setup(100, zirnoxPower).setNameWrapper("purex.recycle").setGroup(autoZirnox, this)
                .inputItems(cold(ModZirnoxItems.waste(ZirnoxFuel.URANIUM)))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu_mix.get(), 1),
                        new ItemStack(ModItems.nugget_plutonium.get(), 2),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 2))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.zirnoxthmeu").setup(100, zirnoxPower).setNameWrapper("purex.recycle").setGroup(autoZirnox, this)
                .inputItems(cold(ModZirnoxItems.waste(ZirnoxFuel.THORIUM)))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_u238.get(), 1),
                        new ItemStack(ModItems.nugget_th232.get(), 1),
                        new ItemStack(ModItems.nugget_u233.get(), 2),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 2))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.zirnoxmox").setup(100, zirnoxPower).setNameWrapper("purex.recycle").setGroup(autoZirnox, this)
                .inputItems(cold(ModZirnoxItems.waste(ZirnoxFuel.MOX)))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu_mix.get(), 1),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nugget_u238.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 3))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.zirnoxmep").setup(100, zirnoxPower).setNameWrapper("purex.recycle").setGroup(autoZirnox, this)
                .inputItems(cold(ModZirnoxItems.waste(ZirnoxFuel.PLUTONIUM)))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu_mix.get(), 2),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 3))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.zirnoxheu233").setup(100, zirnoxPower).setNameWrapper("purex.recycle").setGroup(autoZirnox, this)
                .inputItems(cold(ModZirnoxItems.waste(ZirnoxFuel.U233)))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_u235.get(), 1),
                        new ItemStack(ModItems.nugget_neptunium.get(), 1),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 3))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.zirnoxheu235").setup(100, zirnoxPower).setNameWrapper("purex.recycle").setGroup(autoZirnox, this)
                .inputItems(cold(ModZirnoxItems.waste(ZirnoxFuel.U235)))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu238.get(), 1),
                        new ItemStack(ModItems.nugget_neptunium.get(), 1),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 3))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.zirnoxles").setup(100, zirnoxPower).setNameWrapper("purex.recycle").setGroup(autoZirnox, this)
                .inputItems(cold(ModZirnoxItems.waste(ZirnoxFuel.LES)))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_beryllium.get(), 2),
                        new ItemStack(ModItems.nugget_pu239.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 2))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.zirnoxzfbmox").setup(100, zirnoxPower).setNameWrapper("purex.recycle").setGroup(autoZirnox, this)
                .inputItems(cold(ModZirnoxItems.waste(ZirnoxFuel.ZFB_MOX)))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_zirconium.get(), 3),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nugget_pu_mix.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 1))));

        // Plate Fuel
        String autoPlate = "autoswitch.plate";
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.platemox").setup(100, platePower).setNameWrapper("purex.recycle").setGroup(autoPlate, this)
                .inputItems(cold(ModNuclearMiscItems.WASTE_PLATE_MOX.get()))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.powder_sr90_tiny.get(), 1),
                        new ItemStack(ModItems.nugget_pu_mix.get(), 3),
                        new ItemStack(ModItems.powder_cs137_tiny.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 4))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.platepu238be").setup(100, platePower).setNameWrapper("purex.recycle").setGroup(autoPlate, this)
                .inputItems(cold(ModNuclearMiscItems.WASTE_PLATE_PU238BE.get()))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_beryllium.get(), 1),
                        new ItemStack(ModItems.nugget_pu238.get(), 1),
                        new ItemStack(ModFuelItems.powder_coal_tiny.get(), 2),
                        new ItemStack(ModItems.nugget_lead.get(), 2))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.platepu239").setup(100, platePower).setNameWrapper("purex.recycle").setGroup(autoPlate, this)
                .inputItems(cold(ModNuclearMiscItems.WASTE_PLATE_PU239.get()))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu240.get(), 2),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.powder_cs137_tiny.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 5))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.platera226be").setup(100, platePower).setNameWrapper("purex.recycle").setGroup(autoPlate, this)
                .inputItems(cold(ModNuclearMiscItems.WASTE_PLATE_RA226BE.get()))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_beryllium.get(), 2),
                        new ItemStack(ModItems.nugget_polonium.get(), 2),
                        new ItemStack(ModFuelItems.powder_coal_tiny.get(), 1),
                        new ItemStack(ModItems.nugget_lead.get(), 1))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.platesa326").setup(100, platePower).setNameWrapper("purex.recycle").setGroup(autoPlate, this)
                .inputItems(cold(ModNuclearMiscItems.WASTE_PLATE_SA326.get()))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_solinium.get(), 1),
                        new ItemStack(ModItems.powder_neodymium_tiny.get(), 1),
                        new ItemStack(ModItems.nugget_tantalium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 6))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.plateu233").setup(100, platePower).setNameWrapper("purex.recycle").setGroup(autoPlate, this)
                .inputItems(cold(ModNuclearMiscItems.WASTE_PLATE_U233.get()))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_u235.get(), 1),
                        new ItemStack(ModItems.powder_i131_tiny.get(), 1),
                        new ItemStack(ModItems.powder_sr90_tiny.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 6))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.plateu235").setup(100, platePower).setNameWrapper("purex.recycle").setGroup(autoPlate, this)
                .inputItems(cold(ModNuclearMiscItems.WASTE_PLATE_U235.get()))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_neptunium.get(), 1),
                        new ItemStack(ModItems.nugget_pu238.get(), 1),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 6))));

        // PWR
        String autoPWR = "autoswitch.pwr";
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrmeu").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.MEU))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_u238.get(), 3),
                        new ItemStack(ModItems.nugget_plutonium.get(), 4),
                        new ItemStack(ModItems.nugget_technetium.get(), 2),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 3))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrheu233").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.HEU233))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_u235.get(), 3),
                        new ItemStack(ModItems.nugget_pu238.get(), 3),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 5))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrheu235").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.HEU235))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_neptunium.get(), 3),
                        new ItemStack(ModItems.nugget_pu238.get(), 3),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 5))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrmen").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.MEN))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_u238.get(), 3),
                        new ItemStack(ModItems.nugget_pu239.get(), 4),
                        new ItemStack(ModItems.nugget_technetium.get(), 2),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 3))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrhen237").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.HEN237))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu238.get(), 2),
                        new ItemStack(ModItems.nugget_pu239.get(), 4),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 5))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrmox").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.MOX))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_u238.get(), 3),
                        new ItemStack(ModItems.nugget_pu240.get(), 4),
                        new ItemStack(ModItems.nugget_technetium.get(), 2),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 3))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrmep").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.MEP))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_lead.get(), 2),
                        new ItemStack(ModItems.nugget_pu_mix.get(), 4),
                        new ItemStack(ModItems.nugget_technetium.get(), 2),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 3))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrhep239").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.HEP239))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu_mix.get(), 2),
                        new ItemStack(ModItems.nugget_pu240.get(), 4),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 5))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrhep241").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.HEP241))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_lead.get(), 3),
                        new ItemStack(ModItems.nugget_zirconium.get(), 2),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 6))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrmea").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.MEA))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_lead.get(), 3),
                        new ItemStack(ModItems.nugget_zirconium.get(), 2),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 6))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrhea242").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.HEA242))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_lead.get(), 3),
                        new ItemStack(ModItems.nugget_zirconium.get(), 2),
                        new ItemStack(ModItems.nugget_technetium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 6))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrhes326").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.HES326))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_solinium.get(), 3),
                        new ItemStack(ModItems.nugget_lead.get(), 2),
                        new ItemStack(ModItems.nugget_euphemium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 6))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrhes327").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.HES327))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_australium.get(), 4),
                        new ItemStack(ModItems.nugget_lead.get(), 1),
                        new ItemStack(ModItems.nugget_euphemium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 6))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrbfbam").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.BFB_AM_MIX))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_am_mix.get(), 9),
                        new ItemStack(ModItems.nugget_pu_mix.get(), 2),
                        new ItemStack(ModItems.nugget_bismuth.get(), 6),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 1))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.pwrbfpu241").setup(100, pwrPower).setNameWrapper("purex.recycle").setGroup(autoPWR, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.BFB_PU241))))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu241.get(), 9),
                        new ItemStack(ModItems.nugget_pu_mix.get(), 2),
                        new ItemStack(ModItems.nugget_bismuth.get(), 6),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 1))));

        // Molten Salt
        this.register(new GenericRecipe("purex.thoriumsalt").setup(20, 10_000).setIcon(ItemFluidIcon.make(Fluids.THORIUM_SALT))
                .inputFluids(new FluidStack(Fluids.THORIUM_SALT_DEPLETED, 16_000))
                .inputItems(TH232.nugget().count(2))
                .outputFluids(new FluidStack(Fluids.THORIUM_SALT, 16_000))
                .outputItems(
                        new ItemStack(ModItems.nugget_u233.get(), 1),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 1))
                .outputChances(0.5F, 0.25F)); // 1.7 ChanceOutput

        // Watz
        String autoWatz = "autoswitch.watz";
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzschrab").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.SCHRABIDIUM), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_solinium.get(), 15),
                        new ItemStack(ModItems.nugget_euphemium.get(), 3),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzhes").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.HES), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_solinium.get(), 17),
                        new ItemStack(ModItems.nugget_euphemium.get(), 1),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzmes").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.MES), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_solinium.get(), 12),
                        new ItemStack(ModItems.nugget_tantalium.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzles").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.LES), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_solinium.get(), 9),
                        new ItemStack(ModItems.nugget_tantalium.get(), 9),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzhen").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.HEN), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu239.get(), 12),
                        new ItemStack(ModItems.nugget_technetium.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzmeu").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.MEU), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu239.get(), 12),
                        new ItemStack(ModItems.nugget_bismuth.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzmep").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.MEP), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_pu241.get(), 12),
                        new ItemStack(ModItems.nugget_bismuth.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzlead").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.LEAD), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_lead.get(), 6),
                        new ItemStack(ModItems.nugget_bismuth.get(), 12),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzboron").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.BORON), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModFuelItems.powder_coal_tiny.get(), 12),
                        new ItemStack(ModItems.nugget_co60.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzdu").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.DU), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_polonium.get(), 12),
                        new ItemStack(ModItems.nugget_pu238.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));

        // 1.7: only if OreDictionary.getOres("nuggetNaquadria") is not empty; the rows carry "requires": ["#c:nuggets/naquadria"],
        // and watznaqadah's first output is 1.7's copy of the first nuggetNaquadria item, 12 of it (a tag output)
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watznaqadah").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.NQD), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(ItemStack.EMPTY,
                        new ItemStack(ModItems.nugget_euphemium.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFirstOf(0, NAQUADRIA_NUGGETS, 12)
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))).requires(NAQUADRIA));

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watznaqadria").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(ModWatzItems.depleted(EnumWatzType.NQR), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_co60.get(), 12),
                        new ItemStack(ModItems.nugget_euphemium.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))).requires(NAQUADRIA));

        //ICF
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.icf").setup(300, 10_000).setNameWrapper("purex.recycle")
                .inputItems(new ComparableStack(ModItems.icf_pellet_depleted.get()))
                .outputItems(new ItemStack(ModItems.icf_pellet_empty.get(), 1),
                        new ItemStack(ModItems.pellet_charged.get(), 1),
                        new ItemStack(ModItems.powder_iron.get(), 1))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_250)) // enough for another pellet + 25% surplus
                ));

        /// Vitrification
        this.register(new GenericRecipe("purex.vitliquid").setup(100, vitrification)
                .inputItems(new ComparableStack(ModFallingBlocks.sand_lead.get().asItem(), 1))
                .inputFluids(new FluidStack(Fluids.WASTEFLUID, 1_000))
                .outputItems(new ItemStack(ModItems.nuclear_waste_vitrified.get())));

        this.register(new GenericRecipe("purex.vitgaseous").setup(100, vitrification)
                .inputItems(new ComparableStack(ModFallingBlocks.sand_lead.get().asItem(), 1))
                .inputFluids(new FluidStack(Fluids.WASTEGAS, 1_000))
                .outputItems(new ItemStack(ModItems.nuclear_waste_vitrified.get())));

        this.register(new GenericRecipe("purex.vitsolid").setup(300, vitrification)
                .inputItems(new ComparableStack(ModFallingBlocks.sand_lead.get().asItem(), 1), new ComparableStack(ModItems.nuclear_waste.get(), 4))
                .outputItems(new ItemStack(ModItems.nuclear_waste_vitrified.get(), 4)));

        // Schrabidium
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.schraranium").setup(200, 1_000).setNameWrapper("purex.schrab")
                .inputItems(new ComparableStack(ModFuelItems.ingot_schraranium.get()))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 2_000), new FluidStack(Fluids.NITRIC_ACID, 1_000))
                .outputItems(new ItemStack(ModItems.nugget_schrabidium.get(), 3),
                        new ItemStack(ModItems.nugget_uranium.get(), 3),
                        new ItemStack(ModItems.nugget_neptunium.get(), 2))));

        String autoSchrab = "autoswitch.schrab";
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.schrabzirnox").setup(200, 50_000).setNameWrapper("purex.schrab").setGroup(autoSchrab, this)
                .inputItems(cold(ModZirnoxItems.waste(ZirnoxFuel.PLUTONIUM)))
                .inputFluids(new FluidStack(Fluids.SOLVENT, 4_000), new FluidStack(Fluids.SCHRABIDIC, 250))
                .outputItems(new ItemStack(ModItems.powder_schrabidium.get(), 1),
                        new ItemStack(ModItems.nugget_technetium.get(), 3),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 4))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.schrabpwr").setup(200, 50_000).setNameWrapper("purex.schrab").setGroup(autoSchrab, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.MEP))))
                .inputFluids(new FluidStack(Fluids.SOLVENT, 4_000), new FluidStack(Fluids.SCHRABIDIC, 250))
                .outputItems(new ItemStack(ModItems.powder_schrabidium.get(), 1),
                        new ItemStack(ModItems.nugget_technetium.get(), 3),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 4))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.schrabmen").setup(200, 50_000).setNameWrapper("purex.schrab").setGroup(autoSchrab, this)
                .inputItems(new ComparableStack(new ItemStack(ModPwrItems.depleted(EnumPWRFuel.MEN))))
                .inputFluids(new FluidStack(Fluids.SOLVENT, 4_000), new FluidStack(Fluids.SCHRABIDIC, 250))
                .outputItems(new ItemStack(ModItems.powder_schrabidium.get(), 1),
                        new ItemStack(ModItems.nugget_technetium.get(), 3),
                        new ItemStack(ModItems.nuclear_waste_tiny.get(), 4))));
    }

    /** 1.7 {@code setIconToFirstIngredient()}: the row's icon is its first item ingredient. */
    private static GenericRecipe setIconToFirstIngredient(GenericRecipe recipe) {
        return recipe.setIcon(ItemStacks.copyWithCount(recipe.inputItem[0].getDisplayStack(), 1));
    }

    /**
     * 1.7 {@code new ComparableStack(waste)}: metadata 0, the cooled spent fuel; the port's waste is hot while it carries
     * the hot-state component, so the ingredient is the item without it.
     */
    private static AStack cold(Item waste) {
        return new RecipeIngredients.Cold(waste, 1);
    }
}
