package com.hbm.gametest;

import com.hbm.blocks.machine.MachineIndustrialTurbine;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** GameTests for the industrial turbine's structure, ports, stages, and 20% throughput rule. */
public final class IndustrialTurbineGameTests {

    private IndustrialTurbineGameTests() {}

    public static void layout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineIndustrialTurbine block = ModMachineBlocks.machine_industrial_turbine.get();
        FoldedTestSupport.assemble(helper, block, core);
        TileEntityMachineIndustrialTurbine turbine = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, core, TileEntityMachineIndustrialTurbine.class);
        helper.assertTrue(turbine != null, "Industrial turbine core BlockEntity was not created");

        FoldedTestSupport.assertCell(helper, core, core.offset(1, 0, -3));
        FoldedTestSupport.assertPort(helper, core, core.offset(1, 0, -3));
        FoldedTestSupport.assertPort(helper, core, core.offset(-1, 0, -3));
        FoldedTestSupport.assertPort(helper, core, core.offset(1, 0, 1));
        FoldedTestSupport.assertPort(helper, core, core.offset(-1, 0, 1));
        FoldedTestSupport.assertPort(helper, core, core.offset(0, 2, -3));
        FoldedTestSupport.assertPort(helper, core, core.offset(0, 2, 1));
        FoldedTestSupport.assertPort(helper, core, core.offset(0, 1, 3));
        helper.succeed();
    }

    public static void ports(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        TileEntityMachineIndustrialTurbine turbine = place(helper, core);
        DirPos[] fluids = turbine.getConPos();
        helper.assertTrue(fluids.length == 6, "Industrial turbine must expose six fluid ports");
        assertPort(helper, fluids[0], 5, 1, 0, ForgeDirection.EAST);
        assertPort(helper, fluids[1], 1, 1, 0, ForgeDirection.WEST);
        assertPort(helper, fluids[2], 5, 1, 4, ForgeDirection.EAST);
        assertPort(helper, fluids[3], 1, 1, 4, ForgeDirection.WEST);
        assertPort(helper, fluids[4], 3, 4, 0, ForgeDirection.UP);
        assertPort(helper, fluids[5], 3, 4, 4, ForgeDirection.UP);
        DirPos power = turbine.getPowerPos()[0];
        assertPort(helper, power, 3, 2, 7, ForgeDirection.SOUTH);
        helper.succeed();
    }

    public static void conversion(GameTestHelper helper) {
        Fluids.init();
        BlockPos core = new BlockPos(3, 1, 3);
        TileEntityMachineIndustrialTurbine turbine = place(helper, core);
        turbine.tanks[0].setFill(1_000);
        TileEntityMachineIndustrialTurbine.serverTick(helper.getLevel(), helper.absolutePos(core),
                turbine.getBlockState(), turbine);
        helper.assertTrue(turbine.tanks[0].getFill() == 800, "Industrial turbine must consume 20% of available steam");
        helper.assertTrue(turbine.tanks[1].getTankType() == Fluids.SPENTSTEAM && turbine.tanks[1].getFill() == 2,
                "Industrial turbine produced the wrong spent-steam amount");
        helper.assertTrue(turbine.powerBuffer == 400 && turbine.lastPowerTarget == 300_000,
                "Industrial turbine produced the wrong HE target or flywheel output");
        helper.assertTrue(turbine.operational, "Industrial turbine must be operational when an operation completes");

        turbine.onLeverPull();
        helper.assertTrue(turbine.tanks[0].getTankType() == Fluids.HOTSTEAM
                        && turbine.tanks[0].getMaxFill() == 75_000,
                "Industrial turbine lever did not select HOTSTEAM");
        turbine.onLeverPull();
        turbine.onLeverPull();
        turbine.onLeverPull();
        helper.assertTrue(turbine.tanks[0].getTankType() == Fluids.STEAM
                        && turbine.tanks[0].getMaxFill() == 750_000
                        && turbine.tanks[1].getMaxFill() == 3_000_000,
                "Industrial turbine lever did not cycle back to the base stage and capacity");
        helper.succeed();
    }

    private static TileEntityMachineIndustrialTurbine place(GameTestHelper helper, BlockPos pos) {
        MachineIndustrialTurbine block = ModMachineBlocks.machine_industrial_turbine.get();
        FoldedTestSupport.assemble(helper, block, pos);
        TileEntityMachineIndustrialTurbine turbine = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, pos, TileEntityMachineIndustrialTurbine.class);
        helper.assertTrue(turbine != null, "Industrial turbine core BlockEntity was not created");
        return turbine;
    }

    private static void assertPort(GameTestHelper helper, DirPos port, int x, int y, int z, ForgeDirection direction) {
        BlockPos expected = helper.absolutePos(new BlockPos(x, y, z));
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Industrial turbine port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }
}
