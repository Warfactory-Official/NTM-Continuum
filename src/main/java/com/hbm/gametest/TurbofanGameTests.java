package com.hbm.gametest;

import com.hbm.blocks.machine.MachineTurbofan;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineTurbofan;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** GameTests for the turbofan's footprint, exhaust ports, burn path, and menu. */
public final class TurbofanGameTests {
    private TurbofanGameTests() {}

    public static void layout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineTurbofan block = ModMachineBlocks.machine_turbofan.get();
        TileEntityMachineTurbofan fan = place(helper, core);
        helper.assertTrue(fan != null, "Turbofan core BlockEntity was not created");
        FoldedTestSupport.assertPort(helper, core, core.offset(-1, 0, -1));
        FoldedTestSupport.assertPort(helper, core, core.offset(0, 0, 1));
        helper.succeed();
    }

    public static void portsAndBurn(GameTestHelper helper) {
        TileEntityMachineTurbofan fan = place(helper, new BlockPos(3, 1, 3));
        MachineTurbofan block = ModMachineBlocks.machine_turbofan.get();
        DirPos[] ports = fan.getConPos();
        helper.assertTrue(ports.length == 4, "Turbofan must expose four ports");
        assertPort(helper, ports[0], 3, 1, 2, ForgeDirection.NORTH);
        assertPort(helper, ports[2], 3, 1, 4, ForgeDirection.SOUTH);
        BlockPos portCell = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.assertTrue(block.canConnect(helper.getLevel(), portCell, ForgeDirection.NORTH),
                "Turbofan must expose a cable/fluid port");

        fan.tank.setTankType(Fluids.KEROSENE);
        fan.tank.setFill(20);
        TileEntityMachineTurbofan.serverTick(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)),
                fan.getBlockState(), fan);
        helper.assertTrue(fan.wasOn && fan.output > 0 && fan.power == fan.output && fan.tank.getFill() == 19,
                "Turbofan burn did not produce the expected one-unit output");
        helper.succeed();
    }

    /**
     * 1.7 {@code TileEntityMachineTurbofan.updateEntity}: the tank burns {@code min(1 + afterburner, fill)} of whatever
     * it holds; only AERO-grade fuel gives a burn value. Diesel (HIGH grade) and water (not combustible), no upgrade:
     * one tick burns 1 mB, {@code wasOn} and {@code consumption = 1}, output {@code 0 * 1 * (1 + 0) = 0}, power stays
     * 0. A redstone signal on a port stops the burn entirely (fill unchanged, not on).
     */
    public static void anyFuelZeroOutput(GameTestHelper helper) {
        TileEntityMachineTurbofan fan = place(helper, new BlockPos(3, 1, 3));
        BlockPos core = helper.absolutePos(new BlockPos(3, 1, 3));
        for (var type : new FluidType[] {Fluids.DIESEL, Fluids.WATER}) {
            fan.tank.setTankType(type);
            fan.tank.setFill(20);
            fan.power = 0;
            TileEntityMachineTurbofan.serverTick(helper.getLevel(), core, fan.getBlockState(), fan);
            helper.assertTrue(fan.wasOn && fan.consumption == 1 && fan.output == 0 && fan.power == 0
                            && fan.tank.getFill() == 19,
                    type.getName() + " must burn 1 mB at zero output (1.7), got on=" + fan.wasOn + " consumption="
                            + fan.consumption + " output=" + fan.output + " power=" + fan.power + " fill=" + fan.tank.getFill());
        }
        // beside the turbofan's own edge cell (3,1,2), which 1.7 checks for a redstone signal
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.REDSTONE_BLOCK);
        fan.tank.setFill(20);
        TileEntityMachineTurbofan.serverTick(helper.getLevel(), core, fan.getBlockState(), fan);
        helper.assertTrue(!fan.wasOn && fan.tank.getFill() == 20, "a powered port stops the burn");
        helper.succeed();
    }

    public static void menu(GameTestHelper helper) {
        TileEntityMachineTurbofan fan = place(helper, new BlockPos(3, 1, 3));
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        var menu = new ContainerMachineTurbofan(0, player.getInventory(), fan);
        helper.assertTrue(menu.slots.size() == 41, "Turbofan menu must have 5 machine slots + player inventory");
        helper.assertTrue(menu.slots.get(2).x == 98 && menu.slots.get(2).y == 71,
                "Turbofan upgrade slot is misplaced");
        helper.succeed();
    }

    private static TileEntityMachineTurbofan place(GameTestHelper helper, BlockPos pos) {
        MachineTurbofan block = ModMachineBlocks.machine_turbofan.get();
        helper.setBlock(pos, block.defaultBlockState());
        TileEntityMachineTurbofan fan = GameTestHelpers.blockEntity(
                helper, pos, TileEntityMachineTurbofan.class);
        block.fillSpace(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH);
        return fan;
    }

    private static void assertPort(GameTestHelper helper, DirPos port, int x, int y, int z, ForgeDirection direction) {
        BlockPos expected = helper.absolutePos(new BlockPos(x, y, z));
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Turbofan port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }
}
