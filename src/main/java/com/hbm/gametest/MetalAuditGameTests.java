package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.lib.ForgeDirection;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.packet.HbmPackets;
import com.hbm.tileentity.machine.TileEntityAshpit;
import com.hbm.tileentity.machine.TileEntityCrucible;
import com.hbm.tileentity.machine.TileEntityFurnaceCombination;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import com.hbm.tileentity.machine.TileEntityFoundryChannel;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.items.machine.ModMoldItems;
import com.hbm.tileentity.machine.TileEntityMachineBlastFurnace;
import com.hbm.tileentity.machine.TileEntityMachineStrandCaster;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Metallurgy figures that 1.7 fixes and no other test pins. */
public final class MetalAuditGameTests {

    private MetalAuditGameTests() {}

    public static void channelClog(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos b = a.east();
        level.setBlock(a, ModMachineBlocks.foundry_channel.get().defaultBlockState(), 3);
        level.setBlock(b, ModMachineBlocks.foundry_channel.get().defaultBlockState(), 3);
        TileEntityFoundryChannel first = (TileEntityFoundryChannel) level.getBlockEntity(a);
        TileEntityFoundryChannel second = (TileEntityFoundryChannel) level.getBlockEntity(b);
        MaterialStack iron = new MaterialStack(Mats.MAT_IRON, 24);
        MaterialStack copper = new MaterialStack(Mats.MAT_COPPER, 24);

        first.type = Mats.MAT_IRON;
        first.amount = 10;
        helper.assertTrue(second.canAcceptPartialPour(level, b, 0, 0, 0, ForgeDirection.UP, iron),
                "a run holding iron must accept more iron");
        helper.assertFalse(second.canAcceptPartialPour(level, b, 0, 0, 0, ForgeDirection.UP, copper),
                "a run holding iron must refuse copper");
        first.amount = 0;
        helper.assertTrue(second.canAcceptPartialPour(level, b, 0, 0, 0, ForgeDirection.UP, copper),
                "an emptied run must accept another material at once");
        helper.succeed();
    }

    public static void crucibleWithoutRecipeMakesWaste(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(pos, ModMachineBlocks.machine_crucible.get().defaultBlockState(), 3);
        TileEntityCrucible crucible = (TileEntityCrucible) level.getBlockEntity(pos);
        crucible.heat = TileEntityCrucible.maxHeat;
        crucible.inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
        for (int i = 0; i < 80 && crucible.wasteStack.isEmpty(); i++) {
            crucible.heat = TileEntityCrucible.maxHeat;
            TileEntityCrucible.serverTick(level, pos, crucible.getBlockState(), crucible);
        }
        helper.assertTrue(crucible.recipeStack.isEmpty() && crucible.wasteStack.size() == 1
                        && crucible.wasteStack.get(0).material == Mats.MAT_IRON
                        && crucible.wasteStack.get(0).amount == MaterialShapes.INGOT.quantity,
                "with no recipe the melt goes to the waste stack, got recipe=" + crucible.recipeStack + " waste=" + crucible.wasteStack);
        helper.succeed();
    }

    public static void blastFurnaceFuelCap(GameTestHelper helper) {
        helper.assertTrue(TileEntityMachineBlastFurnace.MAX_FUEL == TileEntityMachineBlastFurnace.FUEL_COAL * 24,
                "the blast furnace holds 24 coal of fuel");
        helper.succeed();
    }

    public static void boronMelts(GameTestHelper helper) {
        helper.assertTrue(Mats.MAT_BORON.smeltable == SmeltingBehavior.SMELTABLE, "boron is a smeltable material");
        helper.succeed();
    }

    public static void fireboxFeedsAshpit(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos ashpitPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos fireboxPos = ashpitPos.above();
        level.setBlock(ashpitPos, ModMachineBlocks.machine_ashpit.get().defaultBlockState(), 3);
        level.setBlock(fireboxPos, ModMachineBlocks.heater_firebox.get().defaultBlockState(), 3);
        TileEntityAshpit ashpit = (TileEntityAshpit) level.getBlockEntity(ashpitPos);
        TileEntityFireboxBase firebox = (TileEntityFireboxBase) level.getBlockEntity(fireboxPos);
        firebox.inventory.setStackInSlot(0, new ItemStack(Items.COAL));
        TileEntityFireboxBase.serverTick(level, fireboxPos, firebox.getBlockState(), firebox);
        helper.assertTrue(firebox.maxBurnTime > 0 && ashpit.ashLevelCoal == firebox.maxBurnTime,
                "loading coal adds its burn time to the ashpit below, got " + ashpit.ashLevelCoal);
        helper.succeed();
    }

    public static void anvilShiftCraftCount(GameTestHelper helper) {
        AnvilRecipes.AnvilConstructionRecipe decoSteel = null;
        for (AnvilRecipes.AnvilConstructionRecipe recipe : AnvilRecipes.getConstruction()) {
            if (recipe.output.size() == 1 && recipe.output.get(0).stack.is(ModBlocks.deco_steel.get().asItem())) decoSteel = recipe;
        }
        helper.assertTrue(decoSteel != null, "the steel ingot to deco steel recipe exists");
        helper.assertTrue(HbmPackets.anvilCraftCount(decoSteel, 0) == 1, "a plain click crafts once");
        helper.assertTrue(HbmPackets.anvilCraftCount(decoSteel, 1) == 16,
                "shift fills one stack of 64 from an output of 4: " + HbmPackets.anvilCraftCount(decoSteel, 1));
        helper.succeed();
    }

    public static void strandCasterTakesLargeMold(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(pos, ModMachineBlocks.machine_strand_caster.get().defaultBlockState(), 3);
        TileEntityMachineStrandCaster caster = (TileEntityMachineStrandCaster) level.getBlockEntity(pos);
        caster.inventory.setStackInSlot(0, new ItemStack(ModMoldItems.mold_block.get()));
        helper.assertTrue(caster.getInstalledMold() != null && caster.getCapacity() == MaterialShapes.BLOCK.quantity * 10,
                "the strand caster accepts any mold size, capacity ten casts, got " + caster.getCapacity());
        helper.succeed();
    }

    public static void combinationCoal(GameTestHelper helper) {
        CombinationRecipes.CombinationRecipe recipe = CombinationRecipes.getOutput(new ItemStack(Items.COAL));
        helper.assertTrue(recipe != null && recipe.itemOutput() != null && recipe.fluidOutput() != null && recipe.fluidOutput().fill == 100,
                "coal bakes into coke and 100 mB creosote, got " + recipe);
        helper.succeed();
    }

    public static void combinationFurnaceSmelts(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(pos, ModMachineBlocks.machine_furnace_combination.get().defaultBlockState(), 3);
        TileEntityFurnaceCombination furnace = (TileEntityFurnaceCombination) level.getBlockEntity(pos);
        furnace.inventory.setStackInSlot(0, new ItemStack(Items.COAL));
        for (int i = 0; i < 60 && furnace.inventory.getStackInSlot(0).getCount() > 0; i++) {
            furnace.heat = TileEntityFurnaceCombination.maxHeat;
            TileEntityFurnaceCombination.serverTick(level, pos, furnace.getBlockState(), furnace);
        }
        helper.assertTrue(furnace.inventory.getStackInSlot(0).isEmpty() && furnace.inventory.getStackInSlot(1).getCount() == 1
                        && furnace.tank.getFill() == 100,
                "coal bakes into one coke and 100 mB creosote, got slot1 " + furnace.inventory.getStackInSlot(1) + " tank " + furnace.tank.getFill());
        helper.succeed();
    }
}
