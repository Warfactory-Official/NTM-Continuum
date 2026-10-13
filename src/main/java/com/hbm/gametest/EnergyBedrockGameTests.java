package com.hbm.gametest;

import com.hbm.blocks.ModEnergyBedrockBlocks;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import com.hbm.tileentity.network.TileEntityConverterHeRf;
import com.hbm.tileentity.network.TileEntityConverterRfHe;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The bedrock ore grade chain against 1.7 values computed by hand from {@code ItemBedrockOreNew}'s tables, and the
 * two converters' exchange rates for one tick. Registered on {@link BedrockOreGameTests#TESTS}.
 */
public final class EnergyBedrockGameTests {

    private EnergyBedrockGameTests() {}

    public static final GameTests.Function CHAIN = BedrockOreGameTests.TESTS.function("bedrock_ore_grade_chain", 20,
            () -> EnergyBedrockGameTests::gradeChain);
    public static final GameTests.Function CONVERTERS = BedrockOreGameTests.TESTS.function("energy_converter_rates", 20,
            () -> EnergyBedrockGameTests::converterRates);

    public static void init() {}

    private static ItemStack ore(Grade grade) {
        return ModBedrockOreItems.make(grade, BedrockOreType.LIGHT_METAL);
    }

    /**
     * Light metal: primary iron 9 / copper 9. Centrifuge PRIMARY gives 9+9 fragments; arc furnace PRIMARY_FIRST
     * melts ceil(8 * 9 * 5) = 360 iron and 144 copper; the electrolyser takes 20 ticks for 576 iron / 288 copper
     * plus a crumb; the acidizer washes BASE with 250 mB water in 100 ticks and wants 4 sulfuric byproducts.
     */
    public static void gradeChain(GameTestHelper helper) {
        ItemStack[] primary = CentrifugeRecipes.getOutput(ore(Grade.PRIMARY));
        helper.assertTrue(primary != null && primary.length == 2, "no centrifuge recipe for the primary fraction");
        helper.assertTrue(primary[0].getCount() == 9 && primary[1].getCount() == 9
                        && primary[0].getItem().toString().contains("iron") && primary[1].getItem().toString().contains("copper"),
                "primary fragments " + primary[0] + " " + primary[1]);

        ItemStack[] rad = CentrifugeRecipes.getOutput(ore(Grade.PRIMARY_RAD));
        helper.assertTrue(rad != null && rad.length == 4 && rad[0].is(ModBedrockOreItems.get(Grade.PRIMARY_NORAD, BedrockOreType.LIGHT_METAL))
                && rad[0].getCount() == 2, "radsolvent split " + Arrays.toString(rad));

        ArcFurnaceRecipes.ArcFurnaceRecipe arc = ArcFurnaceRecipes.getOutput(ore(Grade.PRIMARY_FIRST), true);
        helper.assertTrue(arc != null && arc.fluidOutput.length == 2
                        && arc.fluidOutput[0].material == Mats.MAT_IRON && arc.fluidOutput[0].amount == 360
                        && arc.fluidOutput[1].material == Mats.MAT_COPPER && arc.fluidOutput[1].amount == 144,
                "arc furnace melt of the heavier fraction");
        ArcFurnaceRecipes.ArcFurnaceRecipe sear = ArcFurnaceRecipes.getOutput(ore(Grade.SULFURIC_ROASTED), false);
        helper.assertTrue(sear != null && sear.solidOutput.getCount() == 4
                && sear.solidOutput.is(ModBedrockOreItems.get(Grade.SULFURIC_ARC, BedrockOreType.LIGHT_METAL)), "searing");

        ElectrolyserMetalRecipes.ElectrolysisMetalRecipe electro = ElectrolyserMetalRecipes.getRecipe(ore(Grade.PRIMARY_FIRST));
        helper.assertTrue(electro != null && electro.duration == 20 && electro.output1.amount == 576
                && electro.output2 != null && electro.output2.amount == 288 && electro.byproduct.length == 1
                && electro.byproduct[0].is(ModBedrockOreItems.get(Grade.CRUMBS, BedrockOreType.LIGHT_METAL)), "electrolysis");

        CrystallizerRecipes.CrystallizerRecipe wash = CrystallizerRecipes.getOutput(ore(Grade.BASE), Fluids.WATER);
        helper.assertTrue(wash != null && wash.duration == 100 && wash.acidAmount == 250
                && wash.output.is(ModBedrockOreItems.get(Grade.BASE_WASHED, BedrockOreType.LIGHT_METAL)), "washing");
        CrystallizerRecipes.CrystallizerRecipe byWash = CrystallizerRecipes.getOutput(ore(Grade.SULFURIC_BYPRODUCT), Fluids.WATER);
        helper.assertTrue(byWash != null && byWash.input.stacksize == 4, "byproduct washing needs 4");
        CrystallizerRecipes.CrystallizerRecipe hydrogen = CrystallizerRecipes.getOutput(ore(Grade.PRIMARY_NORAD), Fluids.HYDROGEN);
        helper.assertTrue(hydrogen != null && hydrogen.output.is(ModBedrockOreItems.get(Grade.PRIMARY_FIRST, BedrockOreType.LIGHT_METAL)),
                "hydrogen weighting");

        CombinationRecipes.CombinationRecipe roast = CombinationRecipes.getOutput(ore(Grade.BASE));
        helper.assertTrue(roast != null && roast.itemOutput().is(ModBedrockOreItems.get(Grade.BASE_ROASTED, BedrockOreType.LIGHT_METAL))
                && roast.fluidOutput().type == Fluids.VITRIOL && roast.fluidOutput().fill == 50, "roasting");

        helper.assertTrue(ShredderRecipes.getShredderResult(ore(Grade.RAD_WASHED)).is(Items.GRAVEL), "shredding");
        helper.succeed();
    }

    /** 50 HE buys 10 RF at 5:1; 4 RF buys 10 HE at 2:5, and 1 RF buys 2 (the floor 1.7 had). */
    public static void converterRates(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(3, 1, 1);
        helper.setBlock(a, ModEnergyBedrockBlocks.machine_converter_he_rf.get().defaultBlockState());
        helper.setBlock(b, ModEnergyBedrockBlocks.machine_converter_rf_he.get().defaultBlockState());
        TileEntityConverterHeRf heRf = GameTestHelpers.blockEntity(helper, a, TileEntityConverterHeRf.class);
        TileEntityConverterRfHe rfHe = GameTestHelpers.blockEntity(helper, b, TileEntityConverterRfHe.class);
        helper.assertTrue(heRf != null && rfHe != null, "no converter block entities");

        heRf.power = 50;
        TileEntityConverterHeRf.serverTick(helper.getLevel(), helper.absolutePos(a), heRf.getBlockState(), heRf);
        helper.assertTrue(heRf.stored == 10 && heRf.power == 0, "HE->RF " + heRf.power + " HE / " + heRf.stored + " RF");

        rfHe.receiveRf(4);
        TileEntityConverterRfHe.serverTick(helper.getLevel(), helper.absolutePos(b), rfHe.getBlockState(), rfHe);
        helper.assertTrue(rfHe.stored == 0 && rfHe.power == 10, "RF->HE " + rfHe.stored + " RF / " + rfHe.power + " HE");

        rfHe.power = 0;
        rfHe.receiveRf(1);
        TileEntityConverterRfHe.serverTick(helper.getLevel(), helper.absolutePos(b), rfHe.getBlockState(), rfHe);
        helper.assertTrue(rfHe.power == 2, "1 RF gave " + rfHe.power + " HE");
        helper.succeed();
    }
}
