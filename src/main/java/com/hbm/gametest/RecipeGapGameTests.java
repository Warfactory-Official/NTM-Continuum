package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.SILEXRecipes.SILEXRecipe;
import com.hbm.inventory.recipes.SILEXRecipes.WeightedOutput;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.ModWatzItems;
import com.hbm.items.machine.EnumWatzType;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.items.special.ItemWasteStorage;
import com.hbm.items.special.ItemWasteStorage.LongWasteClass;
import com.hbm.items.special.ItemWasteStorage.ShortWasteClass;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
import dev.golem.core.Ids;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * The 2026-09-30 machine-recipe gap closure, checked against values computed by hand from the 1.7 tables
 * (original-hbm PUREXRecipes, SILEXRecipes, CrystallizerRecipes, CyclotronRecipes, SolidificationRecipes)
 * and the crafting declarations corrected back to 1.7.
 */
public final class RecipeGapGameTests {

    private RecipeGapGameTests() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", id))
                .orElseThrow(() -> new IllegalStateException("missing hbm:" + id));
    }

    // ------------------------------------------------------------------ PUREX

    /** Recipe data of the new families: pile, plate, Watz, flash billets, schrabidic acid and the naquadria gate. */
    public static void purexTable(GameTestHelper helper) {
        PUREXRecipes.INSTANCE.registerRecipes();
        Fluids.init();

        // 1.7 total 58 (uzh 1, flash 2, pile 3, zirnox 9, plate 7, pwr 15, salt 1, watz 10 (+2 naquadria-gated), icf 1, vit 3, schrab 4)
        int total = PUREXRecipes.INSTANCE.recipeOrderedList.size();
        helper.assertTrue(total == 56, "PUREX must hold 56 recipes (1.7's 58 minus the two naquadria-gated ones), has " + total);
        helper.assertTrue(PUREXRecipes.INSTANCE.getRecipe("purex.watznaqadah") == null
                        && PUREXRecipes.INSTANCE.getRecipe("purex.watznaqadria") == null,
                "without a c:nuggets/naquadria item the two naquadah Watz recipes stay unregistered (1.7 gate)");

        // purex.thoriumsalt: 1.7 setup(20, 10_000) (1.12.2 had 100 t): 20 t x 10 000 HE/t = 200 000 HE per cycle
        GenericRecipe salt = PUREXRecipes.INSTANCE.getRecipe("purex.thoriumsalt");
        helper.assertTrue(salt != null && salt.duration == 20 && salt.power == 10_000
                        && salt.duration * salt.power == 200_000L,
                "thoriumsalt must be 1.7's 20 t at 10 000 HE/t, is " + (salt == null ? "missing" : salt.duration + " t at " + salt.power));

        // purex.watzhen: 60 t, 10 000 HE/t, 500 kerosene + 250 nitric -> 12 Pu-239, 6 Tc-99, 2 waste + 1000 mB Watz mud
        GenericRecipe hen = PUREXRecipes.INSTANCE.getRecipe("purex.watzhen");
        helper.assertTrue(hen != null && hen.duration == 60 && hen.power == 10_000, "watzhen must be 60 t at 10 000 HE/t");
        helper.assertTrue(hen.inputItem[0].matchesRecipe(new ItemStack(ModWatzItems.depleted(EnumWatzType.HEN)), false)
                        && !hen.inputItem[0].matchesRecipe(new ItemStack(ModWatzItems.fresh(EnumWatzType.HEN)), false),
                "watzhen takes the depleted HEN pellet only");
        assertStack(helper, hen.outputItem[0], ModItems.nugget_pu239.get(), 12);
        assertStack(helper, hen.outputItem[1], ModItems.nugget_technetium.get(), 6);
        assertStack(helper, hen.outputItem[2], ModItems.nuclear_waste.get(), 2);
        assertFluid(helper, hen.inputFluid[0], Fluids.KEROSENE, 500);
        assertFluid(helper, hen.inputFluid[1], Fluids.NITRIC_ACID, 250);
        assertFluid(helper, hen.outputFluid[0], Fluids.WATZ, 1_000);

        // purex.pilepu239: 40 t, 100 HE/t, 100 mB sulfuric -> 2 Pu-239 billets + 1 uranium billet; any burn-up data
        GenericRecipe pile = PUREXRecipes.INSTANCE.getRecipe("purex.pilepu239");
        ItemStack rod = ModPileBlocks.pile_rod.stack(EnumPileRod.PU239);
        ItemPileRodMK2.setDepletion(rod, 123D);
        helper.assertTrue(pile != null && pile.duration == 40 && pile.power == 100
                        && pile.inputItem[0].matchesRecipe(rod, false)
                        && !pile.inputItem[0].matchesRecipe(ModPileBlocks.pile_rod.stack(EnumPileRod.NU), false),
                "pilepu239 matches the PU239 rod (with burn-up) and not the NU rod");
        assertStack(helper, pile.outputItem[0], ModItems.billet_pu239.get(), 2);
        assertStack(helper, pile.outputItem[1], ModItems.billet_uranium.get(), 1);
        assertFluid(helper, pile.inputFluid[0], Fluids.SULFURIC_ACID, 100);
        helper.assertTrue(PUREXRecipes.INSTANCE.getRecipe("purex.pilethorium") == null,
                "pilethorium is a 1.12.2 recipe, absent from 1.7");

        // purex.flashgold: Au-198 billet + charged pellet + 1000 mB antimatter -> 2 balefire gold billets
        GenericRecipe flash = PUREXRecipes.INSTANCE.getRecipe("purex.flashgold");
        helper.assertTrue(flash != null && flash.inputItem[0].matchesRecipe(new ItemStack(item("billet_au198")), false)
                        && flash.inputItem[1].matchesRecipe(new ItemStack(ModItems.pellet_charged.get()), false),
                "flashgold takes an Au-198 billet and a charged pellet");
        assertStack(helper, flash.outputItem[0], ModItems.billet_balefire_gold.get(), 2);
        assertFluid(helper, flash.inputFluid[0], Fluids.AMAT, 1_000);

        // 1.7: 250 mB schrabidic acid for all three extractions
        for (String name : List.of("purex.schrabzirnox", "purex.schrabpwr", "purex.schrabmen")) {
            assertFluid(helper, PUREXRecipes.INSTANCE.getRecipe(name).inputFluid[1], Fluids.SCHRABIDIC, 250);
        }
        helper.succeed();
    }

    /** A real PUREX running purex.platemox: 100 t at 1 500 HE/t, hot plate rejected. */
    public static void purexPlateCycle(GameTestHelper helper) {
        Fluids.init();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.AIR.defaultBlockState());
        helper.setBlock(pos, ModMachineBlocks.machine_purex.get().defaultBlockState());
        TileEntityMachinePUREX purex = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, TileEntityMachinePUREX.class);

        purex.purexModule.recipe = "purex.platemox";
        tick(helper, pos, purex);
        ItemStack hot = ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_MOX.get(), 1);
        helper.assertFalse(purex.isItemValidForSlot(4, hot), "a hot MOX plate must be cooled first");

        purex.inputTanks[0].setFill(24_000);
        purex.inputTanks[1].setFill(24_000);
        purex.inventory.setStackInSlot(4, new ItemStack(ModNuclearMiscItems.WASTE_PLATE_MOX.get()));
        purex.power = 1_000_000;
        for (int i = 0; i < 100; i++) tick(helper, pos, purex);

        helper.assertTrue(purex.power == 850_000, "platemox must draw 1500 HE/t for 100 ticks, power is " + purex.power);
        helper.assertTrue(purex.inputTanks[0].getFill() == 23_500 && purex.inputTanks[1].getFill() == 23_750,
                "platemox consumes 500 mB kerosene + 250 mB nitric acid");
        helper.assertTrue(purex.inventory.getStackInSlot(4).isEmpty(), "the plate is consumed");
        assertStack(helper, purex.inventory.getStackInSlot(7), ModItems.powder_sr90_tiny.get(), 1);
        assertStack(helper, purex.inventory.getStackInSlot(8), ModItems.nugget_pu_mix.get(), 3);
        assertStack(helper, purex.inventory.getStackInSlot(9), ModItems.powder_cs137_tiny.get(), 1);
        assertStack(helper, purex.inventory.getStackInSlot(10), ModItems.nuclear_waste_tiny.get(), 4);
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, BlockPos pos, TileEntityMachinePUREX purex) {
        TileEntityMachinePUREX.serverTick(helper.getLevel(), helper.absolutePos(pos), purex.getBlockState(), purex);
    }

    // ------------------------------------------------------------------ SILEX

    public static void silexTable(GameTestHelper helper) {
        SILEXRecipes.register();

        // MEU pellet, meta 7 = i 2 xenon: 1 Xe, 83-32 = 51 U fuel, 6+8 = 14 Pu mix, 4+10 = 14 long U235, 6+14 = 20 short U235
        SILEXRecipe meu = SILEXRecipes.getOutput(pellet("meu", 7));
        helper.assertTrue(meu != null && meu.fluidProduced == 600 && meu.fluidConsumed == 100
                && meu.laserStrength == EnumWavelengths.IR, "MEU pellet: 600/100 on IR (1.7 int 1)");
        assertOutputs(helper, meu, List.of(
                new WeightedOutput(new ItemStack(ModItems.powder_xe135_tiny.get()), 1),
                new WeightedOutput(new ItemStack(ModItems.nugget_uranium_fuel.get()), 51),
                new WeightedOutput(new ItemStack(ModItems.nugget_pu_mix.get()), 14),
                new WeightedOutput(ItemWasteStorage.make(ModStorageDrumItems.nuclear_waste_long_tiny.get(), LongWasteClass.URANIUM235.ordinal()), 14),
                new WeightedOutput(ItemWasteStorage.make(ModStorageDrumItems.nuclear_waste_short_tiny.get(), ShortWasteClass.URANIUM235.ordinal()), 20)));

        // UEU meta 5 (i 0 xenon): the 1.7 quirk keeps U at 86 and drops the short waste to 1
        SILEXRecipe ueu = SILEXRecipes.getOutput(pellet("ueu", 5));
        helper.assertTrue(ueu != null && ueu.outputs.get(1).weight() == 86 && ueu.outputs.get(4).weight() == 1
                        && ueu.outputs.get(2).stack().is(ModItems.nugget_pu239.get()) && ueu.outputs.get(2).weight() == 10,
                "UEU xenon pellet keeps 86 uranium, 10 Pu-239, 1 short waste");

        // ZFB Pu-241 meta 9 (i 4 xenon): 3 Xe, 10 U235, 10 Pu240, 130 Pu241, 147 Zr, VISIBLE
        SILEXRecipe zfb = SILEXRecipes.getOutput(pellet("zfb_pu241", 9));
        helper.assertTrue(zfb != null && zfb.laserStrength == EnumWavelengths.VISIBLE && zfb.totalWeight() == 3 + 10 + 10 + 130 + 147,
                "ZFB Pu-241 xenon pellet weights 3/10/10/130/147 on VISIBLE");
        helper.assertTrue(SILEXRecipes.getOutput(pellet("balefire", 5)) == null,
                "1.7 registers no xenon variant for balefire pellets");

        // Depleted short Schrabidium waste: the 1.7 nugget_mercury FIELD is hbm:nugget_mercury_tiny
        SILEXRecipe sch = SILEXRecipes.getOutput(ItemWasteStorage.make(ModStorageDrumItems.nuclear_waste_short_depleted.get(),
                ShortWasteClass.SCHRABIDIUM.ordinal()));
        helper.assertTrue(sch != null && sch.fluidProduced == 900 && sch.laserStrength == EnumWavelengths.IR
                        && sch.outputs.get(1).stack().is(ModItems.nugget_mercury_tiny.get()) && sch.outputs.get(1).weight() == 12
                        && sch.totalWeight() == 100,
                "depleted short Sa-326 waste: 900 mB on IR, 12 tiny mercury, weights sum to 100");

        // Tiny long U235 waste runs the full recipe with (900/900)*100 = 100 mB solution
        ItemStack tiny = ItemWasteStorage.make(ModStorageDrumItems.nuclear_waste_long_tiny.get(), LongWasteClass.URANIUM235.ordinal());
        SILEXRecipe tinyRecipe = SILEXRecipes.getOutput(tiny);
        helper.assertTrue(tinyRecipe != null && tinyRecipe.fluidProduced == 100 && tinyRecipe.totalWeight() == 100
                        && tinyRecipe.outputs.get(1).stack().is(ModItems.nugget_pu239.get()) && tinyRecipe.outputs.get(1).weight() == 45
                        && tinyRecipe.input.matchesRecipe(tiny, false),
                "tiny long U235 waste: 100 mB of the full recipe (Np 20 / Pu239 45 / Pu240 20 / Tc 15)");
        helper.assertTrue(SILEXRecipes.getOutput(ItemWasteStorage.make(ModStorageDrumItems.nuclear_waste_long.get(),
                        LongWasteClass.URANIUM233.ordinal())).outputs.get(0).stack().is(ModItems.nugget_u235.get()),
                "waste classes are matched by 1.7 name, not the port's enum ordinal");

        // Wavelength ints: 3 = UV, 4 = GAMMA
        helper.assertTrue(SILEXRecipes.getOutput(new ItemStack(ModItems.crystal_schraranium.get())).laserStrength == EnumWavelengths.UV,
                "schraranium crystal needs UV (1.7 int 3)");
        helper.assertTrue(SILEXRecipes.getOutput(ItemFluidIcon.make(Fluids.DEATH)).laserStrength == EnumWavelengths.GAMMA,
                "osmiridium solution needs GAMMA (1.7 int 4)");
        helper.succeed();
    }

    private static ItemStack pellet(String name, int meta) {
        return ((com.hbm.items.machine.ItemRBMKPellet) item("rbmk_pellet_" + name)).of(meta);
    }

    private static void assertOutputs(GameTestHelper helper, SILEXRecipe recipe, List<WeightedOutput> expected) {
        List<String> got = new ArrayList<>();
        for (WeightedOutput o : recipe.outputs) got.add(o.stack() + "@" + o.weight());
        helper.assertTrue(recipe.outputs.size() == expected.size(), "SILEX outputs " + got);
        for (int i = 0; i < expected.size(); i++) {
            WeightedOutput e = expected.get(i), g = recipe.outputs.get(i);
            helper.assertTrue(dev.golem.core.world.ItemStacks.sameItemAndData(e.stack(), g.stack()) && e.weight() == g.weight(),
                    "SILEX output " + i + " is " + got.get(i) + ", expected " + e.stack() + "@" + e.weight());
        }
    }

    // ------------------------------------------------------------------ crystallizer / cyclotron / solidifier

    public static void mercuryAndCadmium(GameTestHelper helper) {
        CrystallizerRecipes.register();
        var redstone = CrystallizerRecipes.getOutput(new ItemStack(Items.REDSTONE_BLOCK), Fluids.PEROXIDE);
        helper.assertTrue(redstone != null && redstone.output.is(ModItems.nugget_mercury.get()) && redstone.output.getCount() == 1
                        && redstone.duration == 600 && redstone.acidAmount == 500 && redstone.productivity == 0.25F,
                "redstone block + 500 mB peroxide -> 1 mercury (1.7 ingot_mercury), 600 t");
        var cinnabar = CrystallizerRecipes.getOutput(new ItemStack(ModItems.cinnabar.get()), Fluids.PEROXIDE);
        helper.assertTrue(cinnabar != null && cinnabar.output.is(ModItems.nugget_mercury.get()) && cinnabar.output.getCount() == 3,
                "cinnabar -> 3 mercury");
        var willow = CrystallizerRecipes.getOutput(new ItemStack(com.hbm.items.ModTerrainItems.plant_item_mustardwillow.get(), 10),
                Fluids.RADIOSOLVENT);
        helper.assertTrue(willow != null && willow.output.is(ModItems.powder_cadmium.get()) && willow.input.stacksize == 10
                        && willow.acidAmount == 250 && willow.duration == 100,
                "10 mustard willow + 250 mB radiosolvent -> cadmium, 100 t");
        var rubber = CrystallizerRecipes.getOutput(new ItemStack(ModItems.powder_cadmium.get()), Fluids.FISHOIL);
        helper.assertTrue(rubber != null && rubber.output.getCount() == 16 && rubber.acidAmount == 4_000,
                "cadmium + 4000 mB fish oil -> 16 rubber");

        Object[] hg = CyclotronRecipes.getOutput(new ItemStack(ModItems.powder_gold.get()), new ItemStack(ModItems.part_lithium.get()));
        helper.assertTrue(hg != null && ((ItemStack) hg[0]).is(ModItems.nugget_mercury.get()) && (Integer) hg[1] == 50,
                "lithium particle + gold dust -> mercury, 50 mB");
        Object[] po = CyclotronRecipes.getOutput(new ItemStack(ModItems.nugget_mercury.get()), new ItemStack(ModItems.part_carbon.get()));
        helper.assertTrue(po != null && ((ItemStack) po[0]).is(ModItems.powder_polonium.get()) && (Integer) po[1] == 10,
                "carbon particle + mercury -> polonium, 10 mB");

        var solidHg = SolidificationRecipes.getOutput(Fluids.MERCURY);
        helper.assertTrue(solidHg != null && solidHg.amount() == 125 && solidHg.output().is(ModItems.nugget_mercury.get()),
                "125 mB mercury solidifies into one mercury");
        var biogas = SolidificationRecipes.getOutput(Fluids.BIOGAS);
        helper.assertTrue(biogas != null && biogas.amount() == 250 && biogas.output().getCount() == 4, "250 mB biogas -> 4 compressed biomass");
        var salient = SolidificationRecipes.getOutput(Fluids.SALIENT);
        helper.assertTrue(salient != null && salient.amount() == 1_280 && salient.output().getCount() == 8, "1280 mB salient -> 8 bio wafers");
        helper.succeed();
    }

    // ------------------------------------------------------------------ crafting corrected to 1.7

    public static void craftingCorrections(GameTestHelper helper) {
        ItemStack e = ItemStack.EMPTY;
        ItemStack two = craft(helper, 2, 1, List.of(new ItemStack(item("falloutitem")), new ItemStack(item("falloutitem"))));
        helper.assertTrue(two.is(item("fallout")) && two.getCount() == 2, "two fallout items -> two fallout layers, got " + two);

        ItemStack str = new ItemStack(Items.STRING), circ = new ItemStack(item("circuit_advanced"));
        ItemStack star = new ItemStack(item("ingot_starmetal"));
        ItemStack bj = craft(helper, 3, 3, List.of(str, new ItemStack(Items.BLACK_WOOL), str, e, circ, e, e, star, e));
        helper.assertTrue(bj.is(item("bj_helmet")), "BJ helmet crafts with black wool");
        ItemStack white = craft(helper, 3, 3, List.of(str, new ItemStack(Items.WHITE_WOOL), str, e, circ, e, e, star, e));
        helper.assertTrue(white.isEmpty(), "1.7 BJ helmet needs black wool (Blocks.wool meta 15), not any wool");
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, int w, int h, List<ItemStack> grid) {
        return Recipes.craftingResult(helper.getLevel(), w, h, grid).orElse(ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ helpers

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, "expected " + count + "x " + item + ", got " + stack);
    }

    private static void assertFluid(GameTestHelper helper, FluidStack stack, com.hbm.inventory.fluid.FluidType type, int fill) {
        helper.assertTrue(stack != null && stack.type == type && stack.fill == fill,
                "expected " + fill + " mB " + type.getName() + ", got " + (stack == null ? "null" : stack.fill + " mB " + stack.type.getName()));
    }
}
