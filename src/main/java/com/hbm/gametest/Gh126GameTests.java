package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Ingredients;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * GitHub issue 126: one ingredient filled every chemical plant input slot and starved the others. As in 1.7, input
 * slot i takes ingredient i of the selected recipe and nothing else, spare slots take nothing, and an input slot
 * holding the wrong item can be emptied by automation; what an old save left in the wrong slot is still used.
 */
public final class Gh126GameTests {

    private Gh126GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CHEMICAL_PLANT =
            TESTS.function("gh126_chemical_plant_slot_per_ingredient", 300, () -> Gh126GameTests::chemicalPlant);
    public static final GameTests.Function OLD_LAYOUT =
            TESTS.function("gh126_old_layout_still_runs", 600, () -> Gh126GameTests::oldLayout);
    public static final GameTests.Function ASSEMBLER =
            TESTS.function("gh126_assembler_spare_slots_take_nothing", 40, () -> Gh126GameTests::assembler);

    public static void chemicalPlant(GameTestHelper helper) {
        BlockPos at = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_chemical_plant.get(), new BlockPos(3, 2, 3));
        TileEntityMachineChemicalPlant plant = (TileEntityMachineChemicalPlant) helper.getLevel().getBlockEntity(at);
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        plant.chemplantModule.recipe = "chem.uf6";
        ItemStack yellowcake = new ItemStack(ModItems.powder_yellowcake.get());
        ItemStack fluorite = new ItemStack(Ingredients.tagItems("c:dusts/fluorite").get(0), 4);

        helper.assertTrue(plant.isItemValidForSlot(4, yellowcake) && !plant.isItemValidForSlot(4, fluorite),
                "slot 4 takes yellowcake only");
        helper.assertTrue(plant.isItemValidForSlot(5, fluorite) && !plant.isItemValidForSlot(5, yellowcake),
                "slot 5 takes fluorite only");
        helper.assertTrue(!plant.isItemValidForSlot(6, fluorite) && !plant.isItemValidForSlot(6, yellowcake),
                "a two-ingredient recipe leaves slot 6 closed");
        helper.assertTrue(plant.canPlaceItemThroughFace(5, fluorite, Direction.UP)
                && !plant.canPlaceItemThroughFace(6, fluorite, Direction.UP), "automation follows the same slots");

        plant.inventory.setStackInSlot(4, fluorite.copy());
        helper.assertTrue(plant.canTakeItemThroughFace(4, fluorite, Direction.UP), "fluorite in the yellowcake slot is clogged");
        plant.inventory.setStackInSlot(4, yellowcake.copy());
        plant.inventory.setStackInSlot(5, fluorite.copy());
        helper.assertTrue(!plant.canTakeItemThroughFace(5, fluorite, Direction.UP), "fluorite in its own slot stays");

        GenericRecipe recipe = plant.chemplantModule.getRecipe();
        plant.chemplantModule.setupTanks(recipe);
        plant.chemplantModule.inputTanks[0].setFill(1_000);
        plant.power = 1_000_000L;
        helper.succeedWhen(() -> {
            helper.assertTrue(plant.inventory.getStackInSlot(7).getItem() == ModItems.sulfur.get(), "no sulfur yet");
            helper.assertTrue(plant.inventory.getStackInSlot(4).isEmpty() && plant.inventory.getStackInSlot(5).isEmpty(),
                    "one operation uses the yellowcake and the four fluorite");
            helper.assertTrue(plant.chemplantModule.outputTanks[0].getTankType() == Fluids.UF6, "UF6 tank");
        });
    }

    /**
     * A plant saved under the old any-slot rule, fluorite in the yellowcake slot and yellowcake in the fluorite slot,
     * still runs on what it holds; new fluorite still cannot go into slot 4.
     */
    public static void oldLayout(GameTestHelper helper) {
        BlockPos at = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_chemical_plant.get(), new BlockPos(3, 2, 3));
        TileEntityMachineChemicalPlant plant = (TileEntityMachineChemicalPlant) helper.getLevel().getBlockEntity(at);
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        Item fluorite = Ingredients.tagItems("c:dusts/fluorite").get(0);
        plant.chemplantModule.recipe = "chem.uf6";
        plant.inventory.setStackInSlot(4, new ItemStack(fluorite, 8));
        plant.inventory.setStackInSlot(5, new ItemStack(ModItems.powder_yellowcake.get(), 2));
        helper.assertTrue(!plant.isItemValidForSlot(4, new ItemStack(fluorite)), "new fluorite still stays out of slot 4");
        plant.chemplantModule.setupTanks(plant.chemplantModule.getRecipe());
        plant.chemplantModule.inputTanks[0].setFill(plant.chemplantModule.inputTanks[0].getMaxFill());
        plant.power = 1_000_000L;
        helper.succeedWhen(() -> {
            helper.assertTrue(plant.inventory.getStackInSlot(4).isEmpty() && plant.inventory.getStackInSlot(5).isEmpty(),
                    "two operations use up the misplaced fluorite and yellowcake: " + plant.inventory.getStackInSlot(4)
                            + ", " + plant.inventory.getStackInSlot(5));
            helper.assertTrue(plant.inventory.getStackInSlot(7).getCount() == 4, "4 sulfur from two operations");
        });
    }

    public static void assembler(GameTestHelper helper) {
        BlockPos at = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_assembly_machine.get(), new BlockPos(3, 2, 3));
        TileEntityMachineAssemblyMachine machine = (TileEntityMachineAssemblyMachine) helper.getLevel().getBlockEntity(at);
        GenericRecipe recipe = null;
        for (GenericRecipe candidate : machine.assemblerModule.getRecipeSet().recipeOrderedList) {
            if (candidate.inputItem != null && candidate.inputItem.length == 2 && !candidate.isPooled()) { recipe = candidate; break; }
        }
        helper.assertTrue(recipe != null, "an unpooled two-ingredient assembler recipe");
        machine.assemblerModule.recipe = recipe.getInternalName();
        ItemStack first = recipe.inputItem[0].getDisplayStack();
        int start = TileEntityMachineAssemblyMachine.SLOT_INPUT_START;
        helper.assertTrue(machine.isItemValidForSlot(start, first), "slot " + start + " takes " + first);
        for (int slot = start + 2; slot < start + 12; slot++) {
            helper.assertTrue(!machine.isItemValidForSlot(slot, first), "spare slot " + slot + " takes " + first);
        }
        helper.succeed();
    }
}
