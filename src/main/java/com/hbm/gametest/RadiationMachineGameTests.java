package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineRadGen;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.ModItems;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineMiniRTG;
import com.hbm.tileentity.machine.TileEntityMachineRadGen;
import com.hbm.tileentity.machine.TileEntityRadSensor;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Focused contracts for the RTGs, radiation sensor, and supported RadGen fuels. */
public final class RadiationMachineGameTests {

    private RadiationMachineGameTests() {}

    public static void miniRtgGeneration(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.machine_minirtg.get().defaultBlockState());
        TileEntityMachineMiniRTG generator = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                TileEntityMachineMiniRTG.class);

        tick(helper, pos, generator);
        helper.assertTrue(generator.power == 70, "Mini RTG must generate 70 HE/t, got " + generator.power);

        generator.power = generator.getMaxPower() - 1;
        tick(helper, pos, generator);
        helper.assertTrue(generator.power == generator.getMaxPower(), "Mini RTG must clamp at its 10,000 HE buffer");

        helper.setBlock(pos, ModMachineBlocks.rtg_polonium.get().defaultBlockState());
        generator = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, TileEntityMachineMiniRTG.class);
        tick(helper, pos, generator);
        helper.assertTrue(generator.power == 2_500, "Polonium RTG must generate 2,500 HE/t, got " + generator.power);
        helper.succeed();
    }

    public static void radSensorLadders(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.radsensor.get().defaultBlockState());
        TileEntityRadSensor sensor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                TileEntityRadSensor.class);

        sensor.chunkRads = 0.5D;
        helper.assertTrue(sensor.getRestoneOutput() == 2, "Sensor must output 2 at 0.5 RAD/s");
        sensor.chunkRads = 4_096D;
        helper.assertTrue(sensor.getRestoneOutput() == 15, "Sensor must output 15 at 4,096 RAD/s");
        sensor.recievedDose = 8D;
        helper.assertTrue(sensor.getComparatorOutput() == 2, "Sensor comparator must output 2 at 8 RAD");
        sensor.recievedDose = 65_536D;
        helper.assertTrue(sensor.getComparatorOutput() == 15, "Sensor comparator must output 15 at 65,536 RAD");
        helper.succeed();
    }

    public static void radGenLayoutPortsAndFuel(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineRadGen block = ModMachineBlocks.machine_radgen.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, BlockDummyable.offset + 2));
        TileEntityMachineRadGen generator = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core,
                TileEntityMachineRadGen.class);
        BlockPos absoluteCore = helper.absolutePos(core);
        block.formStructure(helper.getLevel(), absoluteCore, ForgeDirection.NORTH);

        assertMeta(helper, core.offset(0, 0, 3), 9);
        assertMeta(helper, core.offset(1, 0, 0), 11);
        assertMeta(helper, core.offset(-1, 0, 0), 10);
        DirPos port = generator.getConPos()[0];
        helper.assertTrue(port.getPos().equals(absoluteCore.offset(0, 0, 4))
                        && port.getDir() == ForgeDirection.SOUTH,
                "RadGen rear port must be four blocks behind the core and face outward");
        helper.assertTrue(block.canConnect(helper.getLevel(), helper.absolutePos(core.offset(0, 0, 3)),
                        ForgeDirection.SOUTH), "RadGen rear dummy must expose its cable port");

        generator.inventory.setStackInSlot(0, new ItemStack(ModItems.scrap_nuclear.get()));
        TileEntityMachineRadGen.serverTick(helper.getLevel(), absoluteCore, generator.getBlockState(), generator);
        helper.assertTrue(generator.processing[0].is(ModItems.scrap_nuclear.get())
                        && generator.maxProgress[0] == 6_000 && generator.production[0] == 50
                        && generator.power == 50,
                "RadGen must start scrap nuclear fuel at 50 HE/t for 6,000 ticks");
        helper.succeed();
    }

    public static void radGenMenu(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.machine_radgen.get().defaultBlockState()
                .setValue(BlockDummyable.META, BlockDummyable.offset + 2));
        TileEntityMachineRadGen generator = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                TileEntityMachineRadGen.class);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerMachineRadGen(0, player.getInventory(), generator);
        helper.assertTrue(menu.slots.size() == 60, "RadGen menu must have 24 machine slots + 36 player slots");
        helper.assertTrue(menu.slots.get(0).x == 8 && menu.slots.get(0).y == 17,
                "RadGen first fuel slot is misplaced");
        helper.assertTrue(menu.slots.get(12).x == 116 && menu.slots.get(12).y == 17,
                "RadGen first output slot is misplaced");
        helper.assertFalse(menu.slots.get(12).mayPlace(new ItemStack(ModItems.scrap_nuclear.get())),
                "RadGen output slots must be take-only");
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, BlockPos pos, TileEntityMachineMiniRTG generator) {
        TileEntityMachineMiniRTG.serverTick(helper.getLevel(), helper.absolutePos(pos),
                generator.getBlockState(), generator);
    }

    private static void assertMeta(GameTestHelper helper, BlockPos pos, int expected) {
        int actual = helper.getLevel().getBlockState(helper.absolutePos(pos)).getValue(BlockDummyable.META);
        helper.assertTrue(actual == expected, "RadGen structure meta at " + pos + " was " + actual
                + ", expected " + expected);
    }
}
