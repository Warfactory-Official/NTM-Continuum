package com.hbm.gametest;

import com.hbm.blocks.machine.MachineLargeTurbine;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

/** GameTests for the legacy four-long turbine's layout, ports, conversion, and menu. */
public final class LargeTurbineGameTests {

    private LargeTurbineGameTests() {}


    public static void layout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineLargeTurbine block = ModMachineBlocks.machine_large_turbine.get();
        helper.setBlock(core, block.defaultBlockState());
        TileEntityMachineLargeTurbine turbine = placeCore(helper, core);
        block.fillSpace(helper.getLevel(), helper.absolutePos(core), Direction.NORTH);

        helper.assertTrue(turbine != null, "Large turbine core BlockEntity was not created");
        FoldedTestSupport.assertPort(helper, core, core.offset(0, 0, 3));
        FoldedTestSupport.assertPort(helper, core, core.offset(0, 0, -1));
        FoldedTestSupport.assertPort(helper, core, core.offset(1, 0, 0));
        FoldedTestSupport.assertPort(helper, core, core.offset(-1, 0, 0));
        helper.succeed();
    }

    public static void ports(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineLargeTurbine block = ModMachineBlocks.machine_large_turbine.get();
        TileEntityMachineLargeTurbine turbine = place(helper, core);
        DirPos[] fluids = turbine.getConPos();
        helper.assertTrue(fluids.length == 3, "Large turbine must expose three fluid ports");
        assertPort(helper, fluids[0], 5, 1, 3, ForgeDirection.EAST);
        assertPort(helper, fluids[1], 1, 1, 3, ForgeDirection.WEST);
        assertPort(helper, fluids[2], 3, 1, 1, ForgeDirection.NORTH);
        assertPort(helper, turbine.getPowerPos()[0], 3, 1, 7, ForgeDirection.SOUTH);

        BlockPos eastPortCell = helper.absolutePos(core.offset(1, 0, 0));
        helper.assertFalse(block.canConnect(helper.getLevel(), eastPortCell, ForgeDirection.EAST),
                "Large turbine fluid cells must not expose false cable ports");
        helper.assertFalse(block.canConnect(helper.getLevel(), helper.absolutePos(core), ForgeDirection.EAST),
                "Large turbine core must not expose a false cable port");
        helper.assertTrue(block.canConnect(Fluids.STEAM, helper.getLevel(), eastPortCell.getX(),
                        eastPortCell.getY(), eastPortCell.getZ(), ForgeDirection.EAST),
                "Large turbine must accept a valid cooling fluid at its fluid port");
        helper.assertFalse(block.canConnect(Fluids.WATER, helper.getLevel(), eastPortCell.getX(),
                        eastPortCell.getY(), eastPortCell.getZ(), ForgeDirection.EAST),
                "Large turbine must reject non-turbine fluids at its fluid port");
        BlockPos powerPortCell = helper.absolutePos(core.offset(0, 0, 3));
        helper.assertTrue(block.canConnect(helper.getLevel(), powerPortCell, ForgeDirection.SOUTH),
                "Large turbine must expose its rear power port");
        helper.succeed();
    }

    public static void conversion(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineLargeTurbine turbine = place(helper, new BlockPos(3, 1, 3));
        turbine.tanks[0].setFill(1_000);
        TileEntityMachineLargeTurbine.serverTick(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)),
                turbine.getBlockState(), turbine);

        helper.assertTrue(turbine.tanks[0].getFill() == 800,
                "Large turbine must enforce the 20% minimum-throughput rule");
        helper.assertTrue(turbine.tanks[1].getTankType() == Fluids.SPENTSTEAM && turbine.tanks[1].getFill() == 2,
                "Large turbine produced the wrong spent-steam amount");
        helper.assertTrue(turbine.power == 400 && turbine.shouldTurn,
                "Large turbine produced the wrong HE or did not start its rotor");
        helper.succeed();
    }

    public static void menu(GameTestHelper helper) {
        TileEntityMachineLargeTurbine turbine = place(helper, new BlockPos(3, 1, 3));
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerMachineLargeTurbine(0, player.getInventory(), turbine);
        helper.assertTrue(menu.slots.size() == 43, "Large turbine menu must have 7 machine slots + player inventory");
        helper.assertTrue(menu.slots.get(0).x == 8 && menu.slots.get(0).y == 17,
                "Large turbine identifier slot is misplaced");
        helper.assertTrue(menu.slots.get(6).x == 152 && menu.slots.get(6).y == 53,
                "Large turbine output canister slot is misplaced");
        helper.assertFalse(menu.slots.get(1).mayPlace(com.hbm.items.machine.ItemFluidId.make(Fluids.STEAM)),
                "Large turbine identifier output must be take-only");
        helper.succeed();
    }

    private static TileEntityMachineLargeTurbine place(GameTestHelper helper, BlockPos pos) {
        MachineLargeTurbine block = ModMachineBlocks.machine_large_turbine.get();
        TileEntityMachineLargeTurbine turbine = placeCore(helper, pos);
        block.fillSpace(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH);
        return turbine;
    }

    private static TileEntityMachineLargeTurbine placeCore(GameTestHelper helper, BlockPos pos) {
        MachineLargeTurbine block = ModMachineBlocks.machine_large_turbine.get();
        helper.setBlock(pos, block.defaultBlockState());
        return dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                TileEntityMachineLargeTurbine.class);
    }

    private static void assertPort(GameTestHelper helper, DirPos port, int x, int y, int z,
                                   ForgeDirection direction) {
        BlockPos expected = helper.absolutePos(new BlockPos(x, y, z));
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Large turbine port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }
}
