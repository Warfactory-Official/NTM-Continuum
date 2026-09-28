package com.hbm.gametest;

import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.machine.BlockICF;
import com.hbm.blocks.machine.MachineICFController;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModIcfItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.items.machine.ItemICFPellet.EnumICFFuel;
import com.hbm.tileentity.machine.TileEntityBlockICF;
import com.hbm.tileentity.machine.TileEntityICF;
import com.hbm.tileentity.machine.TileEntityICFController;
import com.hbm.tileentity.machine.TileEntityICFPress;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The ICF without a client: the pellet maker's fluid and muon paths, one reactor tick against a hand-computed 1.7
 * result, and the laser array scan's part counts and capacity.
 */
public final class IcfGameTests {

    private IcfGameTests() {}

    /** Deuterium and tritium from the tanks make a D/T pellet and cost 1000 mB each; a muon catalyses the next. */
    public static void pressFillsPellets(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModIcfBlocks.machine_icf_press.get().defaultBlockState());
        TileEntityICFPress press = GameTestHelpers.blockEntity(helper, pos, TileEntityICFPress.class);
        helper.assertTrue(press != null, "no pellet maker block entity");

        press.tanks[0].setFill(2_000);
        press.tanks[1].setFill(2_000);
        press.inventory.setStackInSlot(0, new ItemStack(ModItems.icf_pellet_empty.get(), 2));
        BlockPos abs = helper.absolutePos(pos);
        TileEntityICFPress.serverTick(helper.getLevel(), abs, press.getBlockState(), press);

        ItemStack pellet = press.inventory.getStackInSlot(1);
        helper.assertTrue(pellet.getItem() == ModIcfItems.icf_pellet.get(), "no pellet came out: " + pellet);
        helper.assertTrue(ItemICFPellet.getType(pellet, true) == EnumICFFuel.DEUTERIUM
                && ItemICFPellet.getType(pellet, false) == EnumICFFuel.TRITIUM, "wrong fuels in the pellet");
        helper.assertTrue(!ModIcfItems.PELLET_MUON.getOr(pellet, false), "an uncatalysed pellet claims a muon");
        helper.assertTrue(press.tanks[0].getFill() == 1_000 && press.tanks[1].getFill() == 1_000,
                "the press used " + press.tanks[0].getFill() + "/" + press.tanks[1].getFill() + " mB left, expected 1000 each");
        // D x T: 10M * 1.0 * 1.05 = 10.5M TU; muon-catalysed it is a quarter
        helper.assertTrue(ItemICFPellet.getFusingDifficulty(pellet) == 10_500_000L,
                "fusing difficulty " + ItemICFPellet.getFusingDifficulty(pellet));

        press.inventory.setStackInSlot(1, ItemStack.EMPTY);
        press.inventory.setStackInSlot(2, new ItemStack(ModItems.particle_muon.get()));
        TileEntityICFPress.serverTick(helper.getLevel(), abs, press.getBlockState(), press);
        ItemStack catalysed = press.inventory.getStackInSlot(1);
        helper.assertTrue(ModIcfItems.PELLET_MUON.getOr(catalysed, false), "the muon did not catalyse the pellet");
        helper.assertTrue(ItemICFPellet.getFusingDifficulty(catalysed) == 2_625_000L,
                "catalysed difficulty " + ItemICFPellet.getFusingDifficulty(catalysed));
        helper.assertTrue(press.muon == TileEntityICFPress.maxMuon - 1, "muon charge " + press.muon);
        helper.assertTrue(press.inventory.getStackInSlot(3).getItem() == ModItems.particle_empty.get(),
                "the muon's capsule was not returned");
        helper.succeed();
    }

    /**
     * One tick at 15 MTU into a D/T pellet over a full sodium tank, by 1.7's formulas: heat 28,125,000;
     * 52,734 cooling cycles of 400 TU (sodium ICF efficiency 3, a quarter per tick); 7,031,400 left,
     * times 0.999 truncated = 7,024,368; one mB of stellar flux.
     */
    public static void reactorTick(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModIcfBlocks.icf.get().defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH));
        TileEntityICF icf = GameTestHelpers.blockEntity(helper, pos, TileEntityICF.class);
        helper.assertTrue(icf != null, "no ICF block entity");

        icf.tanks[0].setFill(512_000);
        icf.inventory.setStackInSlot(0, ItemICFPellet.setup(EnumICFFuel.DEUTERIUM, EnumICFFuel.TRITIUM, false));
        icf.laser = 15_000_000L;
        icf.maxLaser = 15_000_000L;
        icf.tick(helper.getLevel(), helper.absolutePos(pos));

        helper.assertTrue(icf.inventory.getStackInSlot(0).isEmpty(), "the pellet did not move into the chamber");
        ItemStack pellet = icf.inventory.getStackInSlot(5);
        helper.assertTrue(ItemICFPellet.getDepletion(pellet) == 15_000_000L, "depletion " + ItemICFPellet.getDepletion(pellet));
        helper.assertTrue(icf.heatup == 28_125_000L, "heatup " + icf.heatup);
        helper.assertTrue(icf.consumption == 52_734 && icf.output == 52_734, "cycles " + icf.consumption + "/" + icf.output);
        helper.assertTrue(icf.tanks[0].getFill() == 512_000 - 52_734, "sodium " + icf.tanks[0].getFill());
        helper.assertTrue(icf.tanks[1].getTankType() == Fluids.SODIUM_HOT && icf.tanks[1].getFill() == 52_734,
                "hot sodium " + icf.tanks[1].getFill());
        helper.assertTrue(icf.heat == 7_024_368L, "heat " + icf.heat);
        helper.assertTrue(icf.tanks[2].getFill() == 1, "stellar flux " + icf.tanks[2].getFill());
        helper.assertTrue(icf.laser == 0 && icf.maxLaser == 0, "the laser input was not consumed");

        // below the pellet's 10.5 MTU the laser only heats the chamber by a quarter
        icf.heat = 0;
        icf.tanks[0].setFill(0);
        icf.laser = 10_000_000L;
        icf.tick(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(icf.heatup == 0 && icf.heat == (long) (2_500_000L * 0.999D), "sub-threshold heat " + icf.heat);
        helper.succeed();
    }

    /**
     * A controller facing east with two cells in line behind it, two flash tubes on the first cell, four capacitors
     * on the tubes and four turbochargers on the capacitors, closed in by casing and one port: 2.5M * sqrt 4 +
     * 5M * sqrt 4 = 15 MHE.
     */
    public static void laserArrayScan(GameTestHelper helper) {
        BlockPos c = new BlockPos(5, 2, 3); // controller; the array runs -X behind it
        Block casing = ModIcfBlocks.icf_laser_component_casing.get();
        for (int x = -4; x <= 0; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -3; z <= 3; z++) helper.setBlock(c.offset(x, y, z), casing.defaultBlockState());
            }
        }
        set(helper, c, -1, 0, ModIcfBlocks.icf_laser_component_cell.get());
        set(helper, c, -2, 0, ModIcfBlocks.icf_laser_component_cell.get());
        set(helper, c, -1, 1, ModIcfBlocks.icf_laser_component_emitter.get());
        set(helper, c, -1, -1, ModIcfBlocks.icf_laser_component_emitter.get());
        set(helper, c, -2, 1, ModIcfBlocks.icf_laser_component_capacitor.get());
        set(helper, c, -2, -1, ModIcfBlocks.icf_laser_component_capacitor.get());
        set(helper, c, -1, 2, ModIcfBlocks.icf_laser_component_capacitor.get());
        set(helper, c, -1, -2, ModIcfBlocks.icf_laser_component_capacitor.get());
        set(helper, c, -2, 2, ModIcfBlocks.icf_laser_component_turbo.get());
        set(helper, c, -2, -2, ModIcfBlocks.icf_laser_component_turbo.get());
        set(helper, c, -3, 1, ModIcfBlocks.icf_laser_component_turbo.get());
        set(helper, c, -3, -1, ModIcfBlocks.icf_laser_component_turbo.get());
        helper.setBlock(c.offset(-2, 1, 0), ModIcfBlocks.icf_laser_component_port.get().defaultBlockState());
        helper.setBlock(c, ModIcfBlocks.icf_controller.get().defaultBlockState()
                .setValue(MachineICFController.FACING, Direction.EAST));

        BlockPos abs = helper.absolutePos(c);
        ModIcfBlocks.icf_controller.get().assemble(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), null);
        TileEntityICFController controller = GameTestHelpers.blockEntity(helper, c, TileEntityICFController.class);
        helper.assertTrue(controller != null && controller.assembled, "the array did not assemble");
        helper.assertTrue(controller.cellCount == 2 && controller.emitterCount == 2 && controller.capacitorCount == 4
                        && controller.turbochargerCount == 4, "counts " + controller.cellCount + "/" + controller.emitterCount
                        + "/" + controller.capacitorCount + "/" + controller.turbochargerCount);
        helper.assertTrue(controller.getMaxPower() == 15_000_000L, "max power " + controller.getMaxPower());

        BlockPos port = c.offset(-2, 1, 0);
        helper.assertTrue(helper.getBlockState(port).getBlock() == ModIcfBlocks.icf_block.get()
                && helper.getBlockState(port).getValue(BlockICF.PORT), "the port did not become a port cell");
        TileEntityBlockICF cell = GameTestHelpers.blockEntity(helper, port, TileEntityBlockICF.class);
        helper.assertTrue(cell != null && cell.getMaxPower() == 15_000_000L, "the port cell does not relay the buffer");
        helper.assertTrue(cell.block == ModIcfBlocks.icf_laser_component_port.get(), "the port cell forgot its part");

        // breaking a cell restores its part and disassembles the controller
        helper.getLevel().removeBlock(helper.absolutePos(port), false);
        helper.assertTrue(helper.getBlockState(port).getBlock() == ModIcfBlocks.icf_laser_component_port.get(),
                "the broken cell did not revert to its part");
        helper.assertTrue(!controller.assembled, "the controller stayed assembled");
        helper.succeed();
    }

    private static void set(GameTestHelper helper, BlockPos c, int dx, int dz, Block block) {
        helper.setBlock(c.offset(dx, 0, dz), block.defaultBlockState());
    }
}
