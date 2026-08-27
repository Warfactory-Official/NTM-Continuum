package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachinePump;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachinePumpElectric;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Focused contracts for the electric groundwater pump. */
public final class PumpGameTests {

    private PumpGameTests() {}

    private static final int FACING_NORTH = BlockDummyable.offset + 2;

    public static void layout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachinePump pump = ModMachineBlocks.machine_pump_electric.get();
        helper.setBlock(core, pump.defaultBlockState().setValue(BlockDummyable.META, FACING_NORTH));
        pump.formStructure(helper.getLevel(), helper.absolutePos(core), ForgeDirection.NORTH);

        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(core.east())).getValue(BlockDummyable.META) >= 6,
                "Pump east port was not marked extra");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(core.west())).getValue(BlockDummyable.META) >= 6,
                "Pump west port was not marked extra");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(core.north())).getValue(BlockDummyable.META) >= 6,
                "Pump north port was not marked extra");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(core.south())).getValue(BlockDummyable.META) >= 6,
                "Pump south port was not marked extra");
        helper.succeed();
    }

    public static void ports(GameTestHelper helper) {
        TileEntityMachinePumpElectric pump = place(helper, new BlockPos(3, 1, 3));
        BlockPos core = pump.getBlockPos();
        DirPos[] ports = pump.getConPos();

        helper.assertTrue(ports.length == 4, "Pump must expose four ports");
        assertPort(helper, ports[0], core.east(2), ForgeDirection.EAST);
        assertPort(helper, ports[1], core.west(2), ForgeDirection.WEST);
        assertPort(helper, ports[2], core.south(2), ForgeDirection.SOUTH);
        assertPort(helper, ports[3], core.north(2), ForgeDirection.NORTH);
        helper.assertTrue(ModMachineBlocks.machine_pump_electric.get().canConnect(helper.getLevel(), core.east(), ForgeDirection.EAST),
                "Pump must expose its east cable port");
        helper.succeed();
    }

    public static void conversion(GameTestHelper helper) {
        TileEntityMachinePumpElectric pump = place(helper, new BlockPos(3, 1, 3));
        pump.power = TileEntityMachinePumpElectric.POWER_COST;
        pump.groundCheckDelay = 1;
        pump.onGround = true;

        TileEntityMachinePumpElectric.serverTick(helper.getLevel(), pump.getBlockPos(), pump.getBlockState(), pump);

        helper.assertTrue(pump.isOn, "Pump did not operate with power and valid ground");
        helper.assertTrue(pump.power == 0, "Pump did not consume 1,000 HE");
        helper.assertTrue(pump.water.getFill() == TileEntityMachinePumpElectric.ELECTRIC_SPEED,
                "Pump did not produce 10,000 mB");
        helper.succeed();
    }

    public static void recipe(GameTestHelper helper) {
        var recipe = AnvilRecipes.getConstruction().stream()
                .filter(candidate -> candidate.output.get(0).stack.is(ModMachineBlocks.machine_pump_electric.get().asItem()))
                .findFirst().orElse(null);
        helper.assertTrue(recipe != null, "Electric pump construction recipe is missing");
        helper.assertTrue(recipe != null && recipe.input.size() == 5 && recipe.tierLower == 3,
                "Electric pump recipe inputs or tier are wrong");
        helper.succeed();
    }

    private static TileEntityMachinePumpElectric place(GameTestHelper helper, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        MachinePump pump = ModMachineBlocks.machine_pump_electric.get();
        helper.getLevel().setBlock(pos, pump.defaultBlockState().setValue(BlockDummyable.META, FACING_NORTH), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileEntityMachinePumpElectric,
                "Pump core BlockEntity was not created");
        pump.formStructure(helper.getLevel(), pos, ForgeDirection.NORTH);
        return (TileEntityMachinePumpElectric) helper.getLevel().getBlockEntity(pos);
    }

    private static void assertPort(GameTestHelper helper, DirPos port, BlockPos expected, ForgeDirection direction) {
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Pump port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }
}
