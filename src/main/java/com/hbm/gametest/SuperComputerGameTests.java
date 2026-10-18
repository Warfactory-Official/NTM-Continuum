package com.hbm.gametest;

import com.hbm.blocks.machine.MachineSuperComputer;
import com.hbm.blocks.machine.MachineTapeDrive;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerTapeDrive;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.SuperComputerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemDrive;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.lib.DirPos;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import com.hbm.tileentity.machine.TileEntityMachineSuperComputer;
import com.hbm.tileentity.machine.TileEntityMachineTapeDrive;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** The supercomputer, drives and drive enclosure against 1.7's {@code SuperComputerRecipes} and tiles. */
public final class SuperComputerGameTests {

    private static final int MIN = 60 * 20;
    private static final int FREQ = 4_242;

    private SuperComputerGameTests() {}

    /** Durations and success weights from 1.7's registerTriplet/registerCopy arguments. */
    public static void table(GameTestHelper helper) {
        SuperComputerRecipes.INSTANCE.registerRecipes();
        helper.assertTrue(SuperComputerRecipes.INSTANCE.recipeOrderedList.size() == 18,
                "1.7 registers 18 supercomputer rows, found " + SuperComputerRecipes.INSTANCE.recipeOrderedList.size());
        triplet(helper, "com.flightcalc", 15 * MIN, 3_000, MIN, 95, 50, 25, EnumDriveType.FLASH_FLIGHTSIM);
        triplet(helper, "com.particlecalc", 15 * MIN, 3_000, MIN, 95, 50, 25, EnumDriveType.FLASH_PARTICLESIM);
        triplet(helper, "com.processflight", 30 * MIN, 15 * MIN, 2 * MIN, 99, 95, 90, EnumDriveType.DISK_FLIGHTDATA_PROCESSED);
        triplet(helper, "com.processorbit", 60 * MIN, 30 * MIN, 5 * MIN, 75, 65, 50, EnumDriveType.DISK_ORBITDATA_PROCESSED);

        GenericRecipe copy = SuperComputerRecipes.INSTANCE.getRecipe("com.copyfligthdata");
        helper.assertTrue(copy.duration == 15 * MIN && copy.outputChoices[0].weight() == 75
                        && copy.outputChoices[0].stack().getCount() == 2 && copy.outputChoices[1].stack().getCount() == 2
                        && ItemDrive.typeOf(copy.outputChoices[1].stack()) == EnumDriveType.DISK_BROKEN,
                "copying flight data: 75 for two processed disks, else two broken ones");
        GenericRecipe blueprints = SuperComputerRecipes.INSTANCE.getRecipe("com.blueprints");
        helper.assertTrue(blueprints.power == 50_000 && blueprints.outputChoices[0].weight() == 20, "blueprint booklets succeed at weight 20");
        GenericRecipe klaus = SuperComputerRecipes.INSTANCE.getRecipe("com.klaus");
        helper.assertTrue(klaus.duration == 60 * MIN && klaus.power == 5_000_000 && klaus.inputItem.length == 3
                        && klaus.inputItem[0].stacksize == 64 && klaus.inputFluid[0].fill == 1_000_000
                        && klaus.outputFluid[0].type == Fluids.SLOP && ItemDrive.typeOf(klaus.outputItem[0]) == EnumDriveType.KLAUS,
                "Klaus: 3 × 64 empty disks and 1,000,000 mB water over an hour at 5 MHE/t");
        helper.succeed();
    }

    private static void triplet(GameTestHelper helper, String name, int water, int pfm, int helium, int c0, int c1, int c2, EnumDriveType out) {
        check(helper, name + "_water", water, c0, out, Fluids.WATER, Fluids.SPENTSTEAM);
        check(helper, name + "_pfm", pfm, c1, out, Fluids.PERFLUOROMETHYL_COLD, Fluids.PERFLUOROMETHYL);
        check(helper, name + "_helium", helium, c2, out, Fluids.HELIUM4, null);
    }

    private static void check(GameTestHelper helper, String name, int duration, int chance, EnumDriveType out,
                              Object coolant, Object spent) {
        GenericRecipe recipe = SuperComputerRecipes.INSTANCE.getRecipe(name);
        helper.assertTrue(recipe != null && recipe.duration == duration && recipe.power == 10_000
                        && recipe.inputFluid[0].type == coolant && recipe.inputFluid[0].fill == 16_000
                        && (spent == null ? recipe.outputFluid == null : recipe.outputFluid[0].type == spent)
                        && recipe.outputChoices[0].weight() == chance && recipe.outputChoices[1].weight() == 100 - chance
                        && ItemDrive.typeOf(recipe.outputChoices[0].stack()) == out,
                name + " must take " + duration + " ticks and succeed at " + chance + "/100");
    }

    /** One flight simulation with helium: 1,200 ticks at 10,000 HE/t, 16,000 mB helium, one drive out. */
    public static void processing(GameTestHelper helper) {
        TileEntityMachineSuperComputer computer = place(helper);
        computer.computerModule.recipe = "com.flightcalc_helium";
        tick(helper, computer);
        helper.assertTrue(computer.inputTank.getTankType() == Fluids.HELIUM4 && computer.inputTank.getMaxFill() == 32_000,
                "the coolant tank takes helium at twice the recipe amount");
        helper.assertTrue(computer.maxPower == 1_000_000, "the buffer is the recipe's draw × 100");

        computer.inputTank.setFill(32_000);
        computer.inventory.setStackInSlot(2, ItemDrive.stack(EnumDriveType.FLASH_EMPTY, 1));
        for (int i = 0; i < 1_199; i++) {
            computer.power = 1_000_000;
            tick(helper, computer);
        }
        helper.assertTrue(computer.inventory.getStackInSlot(5).isEmpty(), "no result before tick 1,200");
        computer.power = 1_000_000;
        tick(helper, computer);

        EnumDriveType result = ItemDrive.typeOf(computer.inventory.getStackInSlot(5));
        helper.assertTrue(result == EnumDriveType.FLASH_FLIGHTSIM || result == EnumDriveType.FLASH_BROKEN,
                "a flight simulation yields a sim drive or a broken one, got " + result);
        helper.assertTrue(computer.inputTank.getFill() == 16_000, "one cycle uses 16,000 mB helium");
        helper.assertTrue(computer.power == 1_000_000 - 10_000, "the last tick drew 10,000 HE");
        helper.assertTrue(computer.inventory.getStackInSlot(2).isEmpty(), "the empty flash drive was consumed");
        helper.succeed();
    }

    /** The helium simulation succeeds at weight 25 of 100: 400 cycles land near 100 successes. */
    public static void failureChance(GameTestHelper helper) {
        TileEntityMachineSuperComputer computer = place(helper);
        computer.computerModule.recipe = "com.flightcalc_helium";
        tick(helper, computer);
        int success = 0;
        int broken = 0;
        for (int i = 0; i < 400; i++) {
            computer.inputTank.setFill(16_000);
            computer.inventory.setStackInSlot(2, ItemDrive.stack(EnumDriveType.FLASH_EMPTY, 1));
            computer.inventory.setStackInSlot(5, ItemStack.EMPTY);
            computer.power = 1_000_000;
            computer.computerModule.progress = 0.9999D;
            tick(helper, computer);
            EnumDriveType result = ItemDrive.typeOf(computer.inventory.getStackInSlot(5));
            if (result == EnumDriveType.FLASH_FLIGHTSIM) success++;
            if (result == EnumDriveType.FLASH_BROKEN) broken++;
        }
        helper.assertTrue(success + broken == 400, "every cycle yields one drive, got " + (success + broken));
        helper.assertTrue(success >= 60 && success <= 140, "expected about 100 successes in 400 at 25%, got " + success);
        helper.succeed();
    }

    public static void layout(GameTestHelper helper) {
        TileEntityMachineSuperComputer computer = place(helper);
        MachineSuperComputer block = ModMachineBlocks.machine_supercomputer.get();
        helper.assertTrue(computer.getPorts().length == 5, "1.7 gives the supercomputer five ports");
        for (DirPos connection : PortDef.connections(computer.getPorts())) {
            BlockPos cell = connection.getPos().offset(-connection.getDir().offsetX, 0, -connection.getDir().offsetZ);
            helper.assertTrue(helper.getLevel().getBlockState(cell).getBlock() == block, "port " + connection.getPos() + " has no machine cell behind it");
            helper.assertTrue(block.canConnect(helper.getLevel(), cell, connection.getDir()), "a supercomputer port does not connect");
        }
        BlockPos core = computer.getBlockPos();
        helper.assertTrue(helper.getLevel().getBlockState(core.above(7)).getBlock() == block, "the crown reaches seven blocks up");
        helper.assertTrue(helper.getLevel().getBlockState(core.north(8).above(2)).getBlock() == block, "the cable run is three high at its end");
        helper.succeed();
    }

    /** A drive enclosure behind a connected ground station writes the satellite's data to a matching drive. */
    public static void tapeDrive(GameTestHelper helper) {
        BlockPos linkRel = new BlockPos(3, 1, 3);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_satlink.get(), linkRel);
        TileEntityMachineSatLink link = GameTestHelpers.blockEntity(helper, linkRel, TileEntityMachineSatLink.class);

        Direction away = null;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (helper.getLevel().getBlockState(link.getBlockPos().relative(dir)).isAir()) away = dir;
        }
        BlockPos driveRel = linkRel.relative(away);
        helper.setBlock(driveRel, ModMachineBlocks.machine_tape_drive.get().defaultBlockState().setValue(MachineTapeDrive.FACING, away));
        TileEntityMachineTapeDrive drive = GameTestHelpers.blockEntity(helper, driveRel, TileEntityMachineTapeDrive.class);

        SatelliteRayScan satellite = new SatelliteRayScan();
        satellite.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_FLIGHTDATA);
        SatelliteSavedData.get(helper.getLevel()).put(FREQ, satellite);
        link.freq = FREQ;
        link.connected = true;

        drive.inventory.setStackInSlot(0, new ItemStack(Items.PAPER));
        drive.inventory.setStackInSlot(3, ItemDrive.stack(EnumDriveType.FLASH_EMPTY, 1));
        drive.inventory.setStackInSlot(7, ItemDrive.stack(EnumDriveType.DISK_EMPTY, 1));

        helper.succeedWhen(() -> {
            link.connected = true;
            helper.assertTrue(ItemDrive.typeOf(drive.inventory.getStackInSlot(7)) == EnumDriveType.DISK_FLIGHTDATA,
                    "the empty disk was not written");
            helper.assertTrue(ItemDrive.typeOf(drive.inventory.getStackInSlot(3)) == EnumDriveType.FLASH_EMPTY,
                    "a flash drive is not what the satellite wants");
            helper.assertTrue(!satellite.hasData(helper.getLevel()), "writing the drive consumes the satellite's data");
            SatelliteSavedData.get(helper.getLevel()).sats.remove(FREQ);
        });
    }

    public static void tapeDriveMenu(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModMachineBlocks.machine_tape_drive.get().defaultBlockState());
        TileEntityMachineTapeDrive drive = GameTestHelpers.blockEntity(helper, rel, TileEntityMachineTapeDrive.class);
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerTapeDrive menu = new ContainerTapeDrive(0, player.getInventory(), drive);
        helper.assertTrue(menu.slots.size() == 12 + 36, "twelve drive slots and the player inventory");
        helper.assertTrue(menu.getSlot(11).mayPlace(ItemDrive.stack(EnumDriveType.KLAUS, 1)), "drive slots take drives");
        helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.PAPER)), "drive slots take nothing else");
        helper.assertTrue(menu.getSlot(0).getMaxStackSize() == 1, "one drive per slot");
        helper.assertFalse(drive.canTakeItemThroughFace(0, ItemDrive.stack(EnumDriveType.DISK_EMPTY, 1), Direction.UP),
                "automation leaves empty drives in");
        helper.succeed();
    }

    private static TileEntityMachineSuperComputer place(GameTestHelper helper) {
        BlockPos rel = new BlockPos(4, 1, 12);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_supercomputer.get(), rel);
        return GameTestHelpers.blockEntity(helper, rel, TileEntityMachineSuperComputer.class);
    }

    private static void tick(GameTestHelper helper, TileEntityMachineSuperComputer computer) {
        TileEntityMachineSuperComputer.serverTick(helper.getLevel(), computer.getBlockPos(), computer.getBlockState(), computer);
    }
}
