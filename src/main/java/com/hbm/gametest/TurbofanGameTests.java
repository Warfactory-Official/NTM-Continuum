package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineTurbofan;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** GameTests for the turbofan's footprint, exhaust ports, burn path, and menu. */
public final class TurbofanGameTests {
    private TurbofanGameTests() {}
    private static final int FACING_NORTH = BlockDummyable.offset + 2;

    public static void layout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineTurbofan block = ModMachineBlocks.machine_turbofan.get();
        TileEntityMachineTurbofan fan = place(helper, core);
        helper.assertTrue(fan != null, "Turbofan core BlockEntity was not created");
        assertMeta(helper, core.offset(-1, 0, -1), 10);
        assertMeta(helper, core.offset(0, 0, 1), 9);
        helper.succeed();
    }

    public static void portsAndBurn(GameTestHelper helper) {
        TileEntityMachineTurbofan fan = place(helper, new BlockPos(3, 1, 3));
        MachineTurbofan block = ModMachineBlocks.machine_turbofan.get();
        DirPos[] ports = fan.getConPos();
        helper.assertTrue(ports.length == 4, "Turbofan must expose four ports");
        assertPort(helper, ports[0], 3, 1, 1, ForgeDirection.NORTH);
        assertPort(helper, ports[2], 3, 1, 5, ForgeDirection.SOUTH);
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

    public static void menu(GameTestHelper helper) {
        TileEntityMachineTurbofan fan = place(helper, new BlockPos(3, 1, 3));
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerMachineTurbofan(0, player.getInventory(), fan);
        helper.assertTrue(menu.slots.size() == 41, "Turbofan menu must have 5 machine slots + player inventory");
        helper.assertTrue(menu.slots.get(2).x == 98 && menu.slots.get(2).y == 71,
                "Turbofan upgrade slot is misplaced");
        helper.succeed();
    }

    private static TileEntityMachineTurbofan place(GameTestHelper helper, BlockPos pos) {
        MachineTurbofan block = ModMachineBlocks.machine_turbofan.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(BlockDummyable.META, FACING_NORTH));
        TileEntityMachineTurbofan fan = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, pos, TileEntityMachineTurbofan.class);
        block.formStructure(helper.getLevel(), helper.absolutePos(pos), ForgeDirection.NORTH);
        return fan;
    }

    private static void assertMeta(GameTestHelper helper, BlockPos pos, int expected) {
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).getValue(BlockDummyable.META) == expected,
                "Turbofan extra cell mismatch at " + pos);
    }

    private static void assertPort(GameTestHelper helper, DirPos port, int x, int y, int z, ForgeDirection direction) {
        BlockPos expected = helper.absolutePos(new BlockPos(x, y, z));
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Turbofan port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }
}
