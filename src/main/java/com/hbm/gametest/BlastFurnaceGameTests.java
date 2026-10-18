package com.hbm.gametest;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.container.ContainerBlastFurnace;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import com.hbm.module.machine.ModuleMachineBase;
import com.hbm.tileentity.machine.TileEntityMachineBlastFurnace;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Regression coverage for the blast furnace and the generic item/fluid completion contract. */
public final class BlastFurnaceGameTests {

    private BlastFurnaceGameTests() {}

    public static void recipes(GameTestHelper helper) {
        BlastFurnaceRecipesNT.INSTANCE.registerRecipes();
        helper.assertTrue(BlastFurnaceRecipesNT.INSTANCE.recipeOrderedList.size() == 15, // 12 + meteorSword/meteor/starmetal (ability tool family)
                "Blast furnace recipe count changed: " + BlastFurnaceRecipesNT.INSTANCE.recipeOrderedList.size());
        GenericRecipe steel = BlastFurnaceRecipesNT.INSTANCE.getRecipe(new ItemStack(Items.IRON_INGOT, 2),
                new ItemStack(Items.SAND));
        helper.assertTrue(steel != null && steel.duration == 800 && steel.outputItem[0].getItem() == ModItems.ingot_steel.get(),
                "The steel blast-furnace recipe or duration changed");
        helper.succeed();
    }

    public static void layout(GameTestHelper helper) {
        var block = ModMachineBlocks.machine_blast_furnace.get();
        var clicked = new BlockPos(4, 1, 2);
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new ItemStack(block));

        var core = clicked.offset(0, 0, 1);
        TileEntityMachineBlastFurnace furnace = GameTestHelpers.blockEntity(helper, core,
                TileEntityMachineBlastFurnace.class);
        helper.assertTrue(furnace != null, "Blast furnace controller setup failed");
        helper.assertBlockProperty(core, BlockMultiblockCore.FACING, Direction.NORTH);
        FoldedTestSupport.assertPort(helper, core, core.above(6));
        FoldedTestSupport.assertPort(helper, core, core.offset(1, 0, 0));
        helper.assertTrue(furnace.getConPos().length == 7, "Blast furnace must expose seven fluid/power ports");
        FoldedTestSupport.assertPort(helper, core, core.offset(0, 0, -1));
        var eastCell = helper.absolutePos(core.offset(1, 0, 0));
        helper.assertTrue(block.canConnect(Fluids.AIRBLAST, helper.getLevel(), eastCell.getX(), eastCell.getY(), eastCell.getZ(), ForgeDirection.EAST),
                "The east airblast port must connect");
        helper.assertFalse(block.canConnect(Fluids.AIRBLAST, helper.getLevel(), eastCell.getX(), eastCell.getY(), eastCell.getZ(), ForgeDirection.WEST),
                "The inside-facing furnace side must not connect");
        helper.succeed();
    }

    public static void slotsAndMenu(GameTestHelper helper) {
        Fluids.init();
        var pos = new BlockPos(1, 1, 1);
        TileEntityMachineBlastFurnace furnace = place(helper, pos);
        ItemStack coal = new ItemStack(Items.COAL);
        ItemStack junk = new ItemStack(Items.STONE);
        helper.assertTrue(furnace.isItemValidForSlot(0, coal) && !furnace.isItemValidForSlot(0, junk),
                "The blast furnace fuel slot must reject non-fuels");
        helper.assertTrue(furnace.canInsertItem(1, new ItemStack(Items.IRON_INGOT))
                        && !furnace.canInsertItem(1, new ItemStack(Items.STONE)),
                "The blast furnace input slots must accept only recipe ingredients");
        helper.assertTrue(furnace.getSlotsForFace(Direction.NORTH).length == 5
                        && furnace.canTakeItemThroughFace(3, ItemStack.EMPTY, Direction.NORTH)
                        && !furnace.canTakeItemThroughFace(1, ItemStack.EMPTY, Direction.NORTH),
                "Blast furnace sided slots changed");

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        var menu = new ContainerBlastFurnace(0, player.getInventory(), furnace);
        helper.assertTrue(menu.slots.size() == 41, "Blast furnace menu must have 5 machine slots + 36 player slots");
        assertSlot(helper, menu, 0, 80, 81);
        assertSlot(helper, menu, 1, 80, 27);
        assertSlot(helper, menu, 2, 80, 45);
        assertSlot(helper, menu, 3, 134, 72);
        assertSlot(helper, menu, 4, 134, 90);
        helper.assertFalse(menu.slots.get(3).mayPlace(new ItemStack(Items.IRON_INGOT)),
                "Blast furnace outputs must be take-only");
        helper.succeed();
    }

    public static void processing(GameTestHelper helper) {
        Fluids.init();
        var pos = new BlockPos(1, 1, 1);
        TileEntityMachineBlastFurnace furnace = place(helper, pos);
        furnace.inventory.setStackInSlot(0, new ItemStack(Items.COAL));
        furnace.inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 2));
        furnace.inventory.setStackInSlot(2, new ItemStack(Items.SAND));

        for (int i = 0; i < 200 && furnace.inventory.getStackInSlot(1).getCount() > 0; i++) {
            furnace.tanks[0].setFill(furnace.tanks[0].getMaxFill());
            TileEntityMachineBlastFurnace.serverTick(helper.getLevel(), helper.absolutePos(pos),
                    furnace.getBlockState(), furnace);
        }

        helper.assertTrue(furnace.inventory.getStackInSlot(1).isEmpty()
                        && furnace.inventory.getStackInSlot(2).isEmpty(),
                "Blast furnace did not consume its two inputs");
        helper.assertTrue(furnace.inventory.getStackInSlot(3).is(ModItems.ingot_steel.get())
                        && furnace.inventory.getStackInSlot(3).getCount() == 2,
                "Blast furnace did not produce two steel ingots");
        // 1.7 BlastFurnaceRecipesNT: the byproduct is ingot_raw[slag], the autogen slag ingot
        ItemStack slag = furnace.inventory.getStackInSlot(4);
        helper.assertTrue(slag.getItem() == ModAutogenItems.get(MaterialShapes.INGOT, Mats.MAT_SLAG)
                        && slag.getCount() == 1,
                "Blast furnace did not produce its slag ingot byproduct");
        helper.assertTrue(furnace.fuel == TileEntityMachineBlastFurnace.FUEL_RATE,
                "One coal must leave exactly half a coal's fuel after one operation");
        // a full air blast runs at speed 5: 800 / 5 = 160 ticks at 8 mB, 1,280 mB into the 1,000 mB tank
        helper.assertTrue(furnace.tanks[1].getFill() == 1_000,
                "Blast furnace must fill its flue tank to 1,000 mB, spilling the other 280");
        helper.succeed();
    }

    public static void genericFluidAtomicity(GameTestHelper helper) {
        Fluids.init();
        TestRecipes recipes = new TestRecipes();
        GenericRecipe recipe = recipes.recipe;
        TestEnergy energy = new TestEnergy(100);
        NTMItemHandler inventory = new NTMItemHandler(2);
        ModuleMachineBase module = new ModuleMachineBase(0, energy, inventory) {
            { inputSlots = new int[]{0}; outputSlots = new int[]{1};
              inputTanks = new FluidTankNTM[]{new FluidTankNTM(Fluids.WATER, 100)};
              outputTanks = new FluidTankNTM[]{new FluidTankNTM(Fluids.STEAM, 100)}; }
            @Override public GenericRecipes getRecipeSet() { return recipes; }
        };
        module.recipe = recipe.name;
        inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        module.inputTanks[0].setFill(100);
        module.outputTanks[0].setFill(100);
        helper.assertFalse(module.canProcess(recipe, 1D, 1D), "A full fluid output must stall a recipe");
        module.update(1D, 1D, true, ItemStack.EMPTY);
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 1 && module.inputTanks[0].getFill() == 100
                        && energy.power == 100, "A blocked fluid output must not mutate inputs or power");

        module.outputTanks[0].setFill(0);
        module.update(1D, 1D, true, ItemStack.EMPTY);
        helper.assertTrue(inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).is(Items.GOLD_INGOT)
                        && module.inputTanks[0].getFill() == 0 && module.outputTanks[0].getFill() == 100
                        && energy.power == 90, "A completed fluid recipe must consume and produce atomically");
        helper.succeed();
    }

    public static void genericChanceSemantics(GameTestHelper helper) {
        TestRecipes recipes = new TestRecipes();
        GenericRecipe recipe = new GenericRecipe("test.zeroChance").setup(1, 0)
                .inputItems(new ComparableStack(Items.IRON_INGOT))
                .outputItems(new ItemStack(Items.GOLD_INGOT))
                .outputChances(0F);
        TestEnergy energy = new TestEnergy(0);
        NTMItemHandler inventory = new NTMItemHandler(2);
        ModuleMachineBase module = new ModuleMachineBase(0, energy, inventory) {
            { inputSlots = new int[]{0}; outputSlots = new int[]{1}; }
            @Override public GenericRecipes getRecipeSet() { return recipes; }
        };
        recipes.registerGenerated(recipe);
        module.recipe = recipe.name;
        inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));

        boolean duplicateRejected = false;
        try {
            recipes.registerGenerated(new GenericRecipe("test.fluid"));
        } catch (IllegalStateException expected) {
            duplicateRejected = true;
        }

        module.update(1D, 1D, true, ItemStack.EMPTY);

        helper.assertTrue(duplicateRejected && recipe.getChance(0) == 0F && inventory.getStackInSlot(0).isEmpty()
                        && inventory.getStackInSlot(1).isEmpty(),
                "Recipe IDs must be unique and an explicit zero chance must suppress an output");
        helper.succeed();
    }

    private static TileEntityMachineBlastFurnace place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachineBlocks.machine_blast_furnace.get().defaultBlockState());
        return GameTestHelpers.blockEntity(helper, pos, TileEntityMachineBlastFurnace.class);
    }

    private static void assertSlot(GameTestHelper helper, AbstractContainerMenu menu,
                                   int index, int x, int y) {
        var slot = menu.slots.get(index);
        helper.assertTrue(slot.x == x && slot.y == y,
                "Blast furnace slot " + index + " sits at " + slot.x + "," + slot.y
                        + ", expected " + x + "," + y);
    }

    private static final class TestRecipes extends GenericRecipes {
        private final GenericRecipe recipe = new GenericRecipe("test.fluid").setup(1, 10)
                .inputItems(new ComparableStack(Items.IRON_INGOT))
                .inputFluids(new FluidStack(Fluids.WATER, 100))
                .outputItems(new ItemStack(Items.GOLD_INGOT))
                .outputFluids(new FluidStack(Fluids.STEAM, 100));

        private TestRecipes() { registerGenerated(recipe); }
        @Override public void registerRecipes() {}
    }

    private static final class TestEnergy implements IEnergyHandlerMK2 {
        private long power;
        private TestEnergy(long power) { this.power = power; }
        @Override public long getPower() { return power; }
        @Override public void setPower(long power) { this.power = power; }
        @Override public long getMaxPower() { return 1_000; }
        @Override public boolean isLoaded() { return true; }
    }
}
