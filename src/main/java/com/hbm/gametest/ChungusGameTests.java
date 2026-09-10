package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineChungus;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityChungus;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.InputStream;

/** GameTests for the Leviathan turbine's structure, ports, and full-throughput conversion. */
public final class ChungusGameTests {

    private ChungusGameTests() {}

    private static final int FACING_NORTH = BlockDummyable.offset + 2;
    /** Far enough into the plot that the 14-block-long machine never reaches a negative coordinate. */
    private static final BlockPos CORE = new BlockPos(3, 1, 6);

    public static void structure(GameTestHelper helper) {
        TileEntityChungus chungus = place(helper);

        helper.assertTrue(helper.absolutePos(CORE).equals(chungusBlock().findCore(helper.getLevel(),
                        helper.absolutePos(new BlockPos(3, 1, 3)))),
                "Leviathan turbine dummy did not chain to its core");

        assertMeta(helper, new BlockPos(3, 5, 4), 1);
        assertMeta(helper, new BlockPos(3, 4, 12), 1);
        assertMeta(helper, new BlockPos(3, 3, 16), 1);

        assertMeta(helper, new BlockPos(3, 3, 2), BlockDummyable.extra + ForgeDirection.NORTH.ordinal());
        assertMeta(helper, new BlockPos(3, 1, 16), BlockDummyable.extra + ForgeDirection.SOUTH.ordinal());
        assertMeta(helper, new BlockPos(5, 1, 6), BlockDummyable.extra + ForgeDirection.EAST.ordinal());
        assertMeta(helper, new BlockPos(1, 1, 6), BlockDummyable.extra + ForgeDirection.WEST.ordinal());

        helper.assertTrue(chungus.getFacing() == ForgeDirection.NORTH, "Leviathan turbine lost its facing");
        helper.succeed();
    }

    public static void ports(GameTestHelper helper) {
        TileEntityChungus chungus = place(helper);
        MachineChungus block = chungusBlock();

        DirPos[] fluids = chungus.getConPos();
        helper.assertTrue(fluids.length == 3, "Leviathan turbine must expose three fluid ports");
        assertPort(helper, fluids[0], 3, 3, 1, ForgeDirection.NORTH);
        assertPort(helper, fluids[1], 6, 1, 6, ForgeDirection.EAST);
        assertPort(helper, fluids[2], 0, 1, 6, ForgeDirection.WEST);

        DirPos[] power = chungus.getPowerPos();
        helper.assertTrue(power.length == 1, "Leviathan turbine must expose one power port");
        assertPort(helper, power[0], 3, 1, 17, ForgeDirection.SOUTH);

        helper.assertTrue(block.canConnect(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 16)),
                        ForgeDirection.SOUTH),
                "Leviathan turbine generator does not accept a cable at its power port");
        helper.assertTrue(!block.canConnect(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 15)),
                        ForgeDirection.EAST),
                "Leviathan turbine accepted a cable away from its power port");

        BlockPos intake = helper.absolutePos(new BlockPos(3, 3, 2));
        helper.assertTrue(block.canConnect(Fluids.STEAM, helper.getLevel(), intake.getX(), intake.getY(),
                        intake.getZ(), ForgeDirection.NORTH),
                "Leviathan turbine does not accept steam at its intake port");
        helper.assertTrue(!block.canConnect(Fluids.WATER, helper.getLevel(), intake.getX(), intake.getY(),
                        intake.getZ(), ForgeDirection.NORTH),
                "Leviathan turbine accepted a fluid it cannot cool");
        helper.succeed();
    }

    public static void conversion(GameTestHelper helper) {
        Fluids.init();
        TileEntityChungus chungus = place(helper);
        chungus.tanks[0].setFill(1_000);

        TileEntityChungus.serverTick(helper.getLevel(), helper.absolutePos(CORE), chungus.getBlockState(), chungus);

        helper.assertTrue(chungus.tanks[0].getFill() == 0,
                "Leviathan turbine must consume all available steam in one tick");
        helper.assertTrue(chungus.tanks[1].getTankType() == Fluids.SPENTSTEAM && chungus.tanks[1].getFill() == 10,
                "Leviathan turbine produced the wrong spent-steam amount");
        helper.assertTrue(chungus.powerBuffer == 1_700,
                "Leviathan turbine produced the wrong HE at 85% efficiency, got " + chungus.powerBuffer);
        helper.assertTrue(chungus.operational && chungus.turnTimer == 25,
                "Leviathan turbine did not latch its fan timer while operating");
        helper.assertTrue("1700".equals(chungus.provideRORValue("VAL:output")),
                "Leviathan turbine reported the wrong radio output");
        helper.succeed();
    }

    /** The Leviathan's lever steps through the steam grades without ever resizing its billion-mB tanks. */
    public static void lever(GameTestHelper helper) {
        Fluids.init();
        TileEntityChungus chungus = place(helper);
        helper.assertTrue(!chungus.doesResizeCompressor(), "Leviathan turbine must not resize its tanks");

        chungus.onLeverPull();
        helper.assertTrue(chungus.tanks[0].getTankType() == Fluids.HOTSTEAM
                        && chungus.tanks[1].getTankType() == Fluids.STEAM
                        && chungus.tanks[0].getMaxFill() == TileEntityChungus.inputTankSize,
                "Leviathan turbine lever did not select HOTSTEAM at full capacity");

        chungus.onLeverPull();
        chungus.onLeverPull();
        helper.assertTrue(chungus.tanks[0].getTankType() == Fluids.ULTRAHOTSTEAM,
                "Leviathan turbine lever did not reach ULTRAHOTSTEAM");

        chungus.onLeverPull();
        helper.assertTrue(chungus.tanks[0].getTankType() == Fluids.STEAM
                        && chungus.tanks[1].getTankType() == Fluids.SPENTSTEAM
                        && chungus.tanks[0].getMaxFill() == TileEntityChungus.inputTankSize
                        && chungus.tanks[1].getMaxFill() == TileEntityChungus.outputTankSize,
                "Leviathan turbine lever did not cycle back to steam at full capacity");
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] required = {
                "assets/hbm/models/machines/chungus.obj",
                "assets/hbm/textures/models/machines/chungus.png",
                "assets/hbm/sounds/block/chungusturbine.ogg",
                "assets/hbm/sounds/block/largeturbine.ogg",
                "assets/hbm/blockstates/machine_chungus.json",
                "assets/hbm/models/block/machine_chungus.json",
                "assets/hbm/items/machine_chungus.json",
                "data/hbm/loot_table/blocks/machine_chungus.json",
        };
        for (String path : required) {
            helper.assertTrue(resourceExists(path), "Missing Leviathan turbine asset: " + path);
        }
        helper.succeed();
    }

    private static MachineChungus chungusBlock() {
        return ModMachineBlocks.machine_chungus.get();
    }

    private static TileEntityChungus place(GameTestHelper helper) {
        MachineChungus block = chungusBlock();
        helper.setBlock(CORE, block.defaultBlockState().setValue(BlockDummyable.META, FACING_NORTH));
        TileEntityChungus chungus = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, CORE, TileEntityChungus.class);
        helper.assertTrue(chungus != null, "Leviathan turbine core BlockEntity was not created");
        block.formStructure(helper.getLevel(), helper.absolutePos(CORE), ForgeDirection.NORTH);
        return chungus;
    }

    private static void assertMeta(GameTestHelper helper, BlockPos pos, int expected) {
        int actual = helper.getLevel().getBlockState(helper.absolutePos(pos)).getValue(BlockDummyable.META);
        helper.assertTrue(actual == expected,
                "Leviathan turbine cell at " + pos + " has meta " + actual + ", expected " + expected);
    }

    private static void assertPort(GameTestHelper helper, DirPos port, int x, int y, int z, ForgeDirection direction) {
        BlockPos expected = helper.absolutePos(new BlockPos(x, y, z));
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Leviathan turbine port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }

    private static boolean resourceExists(String path) {
        try (InputStream stream = ChungusGameTests.class.getClassLoader().getResourceAsStream(path)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }
}
