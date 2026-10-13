package com.hbm.gametest;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.machine.MachineExposureChamber;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineExposureChamber;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Focused contracts for the legacy eight-cycle Exposure Chamber. */
public final class ExposureChamberGameTests {

    private ExposureChamberGameTests() {}

    public static void recipes(GameTestHelper helper) {
        ExposureChamberRecipes.INSTANCE.registerRecipes();
        helper.assertTrue(ExposureChamberRecipes.recipes.size() == 4,
                "Exposure Chamber must retain its four default recipes");

        var uranium = ExposureChamberRecipes.getRecipe(new ItemStack(ModItems.particle_higgs.get()),
                new ItemStack(ModItems.ingot_uranium.get()));
        helper.assertTrue(uranium != null && uranium.output.is(ModFuelItems.ingot_schraranium.get()),
                "Natural uranium must convert to schraranium");

        var plutonium = ExposureChamberRecipes.getRecipe(new ItemStack(ModItems.particle_dark.get()),
                new ItemStack(ModItems.ingot_plutonium.get()));
        helper.assertTrue(plutonium != null && plutonium.output.is(ModItems.ingot_euphemium.get()),
                "Plutonium must convert to euphemium");
        helper.succeed();
    }

    public static void layoutAndPorts(GameTestHelper helper) {
        TileEntityMachineExposureChamber chamber = place(helper, new BlockPos(10, 1, 10));
        MachineExposureChamber block = ModMachineBlocks.machine_exposure_chamber.get();
        BlockPos core = chamber.getBlockPos();

        helper.assertTrue(core.equals(MultiblockSurface.coreOfAny(helper.getLevel(), core)),
                "Exposure Chamber core did not resolve to itself");
        helper.assertTrue(chamber.getConPos().length == 5, "Exposure Chamber must expose five power ports");
        for (DirPos connection : chamber.getConPos()) {
            BlockPos cell = connection.getPos().offset(-connection.getDir().offsetX,
                    -connection.getDir().offsetY, -connection.getDir().offsetZ);
            helper.assertTrue(helper.getLevel().getBlockState(cell).getBlock() == block,
                    "Exposure Chamber power connection is missing its extra cell");
            helper.assertTrue(block.canConnect(helper.getLevel(), cell, connection.getDir()),
                    "Exposure Chamber power port does not connect");
            helper.assertTrue(core.equals(MultiblockSurface.coreOfAny(helper.getLevel(), cell)),
                    "Exposure Chamber extra cell does not resolve to the core");
        }
        helper.succeed();
    }

    public static void processing(GameTestHelper helper) {
        TileEntityMachineExposureChamber chamber = place(helper, new BlockPos(10, 1, 10));
        BlockPos pos = chamber.getBlockPos();
        chamber.inventory.setStackInSlot(0, new ItemStack(ModItems.particle_higgs.get()));
        chamber.inventory.setStackInSlot(3, new ItemStack(ModItems.ingot_uranium.get(), 8));

        for (int tick = 0; tick < TileEntityMachineExposureChamber.PROCESS_TIME_BASE
                * TileEntityMachineExposureChamber.MAX_PARTICLES; tick++) {
            // The real power network refills the one-million-HE buffer between ticks.
            chamber.power = TileEntityMachineExposureChamber.MAX_POWER;
            TileEntityMachineExposureChamber.serverTick(helper.getLevel(), pos, chamber.getBlockState(), chamber);
        }

        helper.assertTrue(chamber.inventory.getStackInSlot(0).isEmpty()
                        && chamber.inventory.getStackInSlot(1).isEmpty()
                        && chamber.inventory.getStackInSlot(3).isEmpty()
                        && chamber.savedParticles == 0,
                "Exposure Chamber did not consume its catalyst and eight ingredients: slots="
                        + chamber.inventory.getStackInSlot(0).getCount() + "/"
                        + chamber.inventory.getStackInSlot(1).getCount() + "/"
                        + chamber.inventory.getStackInSlot(3).getCount() + ", saved="
                        + chamber.savedParticles + ", progress=" + chamber.progress);
        helper.assertTrue(chamber.inventory.getStackInSlot(2).is(ModItems.particle_empty.get()),
                "Exposure Chamber did not preserve the particle capsule remainder");
        helper.assertTrue(chamber.inventory.getStackInSlot(4).is(ModFuelItems.ingot_schraranium.get())
                        && chamber.inventory.getStackInSlot(4).getCount() == 8,
                "Exposure Chamber did not produce eight schraranium ingots");
        helper.succeed();
    }

    public static void constructionRecipe(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.exposurechamber");
        helper.assertTrue(recipe != null && recipe.duration == 200 && recipe.power == 100
                        && recipe.inputItem != null && recipe.inputItem.length == 8,
                "Exposure Chamber assembly recipe is missing or changed");
        helper.assertTrue(recipe.outputItem != null && recipe.outputItem.length == 1
                        && recipe.outputItem[0].is(ModMachineBlocks.machine_exposure_chamber.get().asItem()),
                "Exposure Chamber assembly recipe has the wrong output");
        helper.succeed();
    }

    public static void menu(GameTestHelper helper) {
        TileEntityMachineExposureChamber chamber = place(helper, new BlockPos(2, 1, 2));
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerMachineExposureChamber menu = new ContainerMachineExposureChamber(0, player.getInventory(), chamber);

        helper.assertTrue(menu.slots.size() == ContainerMachineExposureChamber.MACHINE_SLOTS + 36,
                "Exposure Chamber menu must contain seven machine slots and the player inventory");
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(ModItems.particle_higgs.get())),
                "Exposure Chamber catalyst slot must accept a particle");
        helper.assertTrue(menu.getSlot(2).mayPlace(new ItemStack(ModItems.ingot_uranium.get())),
                "Exposure Chamber ingredient slot must accept uranium");
        helper.assertTrue(menu.getSlot(4).mayPlace(new ItemStack(
                        ModBatteryItems.PACKS.get(EnumBatteryPack.CAPACITOR_SPARK).get())),
                "Exposure Chamber battery slot must accept a battery");
        helper.assertFalse(menu.getSlot(3).mayPlace(new ItemStack(ModItems.ingot_uranium.get())),
                "Exposure Chamber output slot must be take-only");
        helper.succeed();
    }

    public static void outputFullStopsCycles(GameTestHelper helper) {
        TileEntityMachineExposureChamber chamber = place(helper, new BlockPos(2, 1, 2));
        chamber.inventory.setStackInSlot(1, new ItemStack(ModItems.particle_higgs.get()));
        chamber.inventory.setStackInSlot(3, new ItemStack(ModItems.ingot_uranium.get(), 8));
        chamber.inventory.setStackInSlot(4, new ItemStack(Items.STONE, 64));
        chamber.savedParticles = TileEntityMachineExposureChamber.MAX_PARTICLES;
        chamber.power = TileEntityMachineExposureChamber.MAX_POWER;

        TileEntityMachineExposureChamber.serverTick(helper.getLevel(), chamber.getBlockPos(),
                chamber.getBlockState(), chamber);

        helper.assertTrue(chamber.progress == 0 && chamber.savedParticles == TileEntityMachineExposureChamber.MAX_PARTICLES,
                "A full incompatible Exposure output must not consume a cycle");
        helper.assertTrue(chamber.inventory.getStackInSlot(3).getCount() == 8,
                "A full Exposure output must not consume the ingredient");
        helper.succeed();
    }

    public static void upgrades(GameTestHelper helper) {
        assertUpgrade(helper, ModUpgradeItems.upgrade_speed_3.get(), 50, 20_000);
        assertUpgrade(helper, ModUpgradeItems.upgrade_power_3.get(), 400, 2_500);
        assertUpgrade(helper, ModUpgradeItems.upgrade_overdrive_3.get(), 50, 70_000);
        helper.succeed();
    }

    private static void assertUpgrade(GameTestHelper helper, Item upgrade,
                                      int expectedProcessTime, int expectedConsumption) {
        TileEntityMachineExposureChamber chamber = place(helper, new BlockPos(2, 1, 2));
        chamber.inventory.setStackInSlot(6, new ItemStack(upgrade));
        TileEntityMachineExposureChamber.serverTick(helper.getLevel(), chamber.getBlockPos(),
                chamber.getBlockState(), chamber);
        helper.assertTrue(chamber.processTime == expectedProcessTime && chamber.consumption == expectedConsumption,
                "Exposure upgrade formula mismatch for " + upgrade + ": "
                        + chamber.processTime + "/" + chamber.consumption);
    }

    private static TileEntityMachineExposureChamber place(GameTestHelper helper, BlockPos relativePos) {
        MachineExposureChamber block = ModMachineBlocks.machine_exposure_chamber.get();
        FoldedTestSupport.assemble(helper, block, relativePos);
        return GameTestHelpers.blockEntity(helper, relativePos, TileEntityMachineExposureChamber.class);
    }
}
