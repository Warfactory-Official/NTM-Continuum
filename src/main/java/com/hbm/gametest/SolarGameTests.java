package com.hbm.gametest;

import com.hbm.blocks.machine.solar.MachineSolarBoiler;
import com.hbm.blocks.machine.solar.ModSolarBlocks;
import com.hbm.blocks.machine.solar.SolarMirror;
import com.hbm.items.solar.ModSolarItems;
import com.hbm.items.solar.SolarDataComponents;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.solar.TileEntitySolarBoiler;
import com.hbm.tileentity.machine.solar.TileEntitySolarMirror;
import dev.golem.core.gametest.GameTests;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.context.UseOnContext;

/** Focused contracts for the solar boiler, heliostat mirror, and mirror adjustment tool. */
public final class SolarGameTests {

    private SolarGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(com.hbm.Constants.MOD_ID);
    public static final GameTests.Function LINK = TESTS.function(
            "solar_tool_link", 40, 30, () -> SolarGameTests::toolLinkRangeAndRejection);
    public static final GameTests.Function DAYLIGHT = TESTS.function(
            "solar_daylight", 40, 8, () -> SolarGameTests::daylightAndSkyGate);
    public static final GameTests.Function HEAT = TESTS.function(
            "solar_heat", 40, 12, () -> SolarGameTests::multiMirrorHeatAccumulationAndDecay);
    public static final GameTests.Function CONVERSION = TESTS.function(
            "solar_conversion", 40, 12, () -> SolarGameTests::conversionPortsAndPersistence);

    public static void init() {}

    public static void toolLinkRangeAndRejection(GameTestHelper helper) {
        BlockPos coreRelative = new BlockPos(3, 1, 3);
        TileEntitySolarBoiler boiler = placeBoiler(helper, coreRelative);
        BlockPos core = helper.absolutePos(coreRelative);

        ItemStack tool = new ItemStack(ModSolarItems.mirror_tool.get());
        useOn(helper, tool, coreRelative, false);
        helper.assertFalse(SolarDataComponents.MIRROR_TARGET.has(tool),
                "Ordinary right-click on the boiler must not overwrite the alignment target");
        useOn(helper, tool, coreRelative, true);
        helper.assertTrue(SolarDataComponents.MIRROR_TARGET.get(tool).orElse(BlockPos.ZERO).equals(core.above()),
                "Tool must record one block above the boiler core");
        helper.assertTrue(SolarDataComponents.MIRROR_TARGET.get(tool.copy()).orElse(BlockPos.ZERO).equals(core.above()),
                "Mirror target component must persist when the tool stack is copied");

        BlockPos insideRelative = new BlockPos(27, 2, 3);
        helper.setBlock(insideRelative, ModSolarBlocks.solar_mirror.get().defaultBlockState());
        TileEntitySolarMirror inside = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, insideRelative, TileEntitySolarMirror.class);
        useOn(helper, tool, insideRelative, true);
        helper.assertTrue(inside.getTarget().equals(BlockPos.ZERO),
                "Shift-right-click on a mirror must not turn it");
        useOn(helper, tool, insideRelative, false);
        helper.assertTrue(inside.getTarget().equals(core.above()),
                "A target at distance 24 must be accepted");

        BlockPos boundaryRelative = new BlockPos(28, 2, 3);
        helper.setBlock(boundaryRelative, ModSolarBlocks.solar_mirror.get().defaultBlockState());
        TileEntitySolarMirror boundary = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, boundaryRelative, TileEntitySolarMirror.class);
        useOn(helper, tool, boundaryRelative, false);
        helper.assertTrue(boundary.getTarget().equals(BlockPos.ZERO),
                "The old strict distance check must reject exactly 25 blocks");
        helper.assertTrue(boiler.getBlockPos().equals(core), "Boiler core moved during tool test");
        helper.succeed();
    }

    public static void daylightAndSkyGate(GameTestHelper helper) {
        int clearDay = TileEntitySolarMirror.calculateSun(15, 0);
        int night = TileEntitySolarMirror.calculateSun(15, 11);
        helper.assertTrue(clearDay == 4, "Clear-day solar calculation changed");
        helper.assertTrue(night == -7, "Night solar calculation changed");
        helper.assertTrue(TileEntitySolarMirror.isSolarActive(clearDay, true),
                "Positive clear-day heat must pass the sunlight gate");
        helper.assertFalse(TileEntitySolarMirror.isSolarActive(night, true),
                "Night heat must fail the sunlight gate");
        helper.assertFalse(TileEntitySolarMirror.isSolarActive(clearDay, false),
                "A blocked sky must fail the sunlight gate");

        BlockPos relative = new BlockPos(3, 1, 3);
        SolarMirror block = ModSolarBlocks.solar_mirror.get();
        helper.setBlock(relative, block.defaultBlockState());
        TileEntitySolarMirror mirror = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntitySolarMirror.class);
        // GameTest placement is relative, but production ticks and Level writes are absolute. Use the
        // placed block entity as the single source of truth for the mirror and its queried sky cell.
        BlockPos pos = mirror.getBlockPos();
        mirror.setTarget(pos.getX(), pos.getY() + 1, pos.getZ());

        TileEntitySolarMirror.serverTick(helper.getLevel(), pos, mirror.getBlockState(), mirror);
        int sun = TileEntitySolarMirror.getSun(helper.getLevel(), pos);
        boolean expected = TileEntitySolarMirror.isSolarActive(sun, helper.getLevel().canSeeSky(pos.above()));
        helper.assertTrue(mirror.isOn == expected,
                "Mirror active state must use sky light minus sky darkening minus 11");

        BlockPos roof = pos.above();
        helper.getLevel().setBlockAndUpdate(roof, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        helper.assertTrue(helper.getLevel().getBlockState(roof).is(net.minecraft.world.level.block.Blocks.STONE),
                "The solar fixture roof was not placed at the mirror's absolute position");
        TileEntitySolarMirror.serverTick(helper.getLevel(), pos, mirror.getBlockState(), mirror);
        helper.assertFalse(mirror.isOn, "A block directly above the mirror must disable the mirror");

        helper.getLevel().removeBlock(roof, false);
        TileEntitySolarMirror.serverTick(helper.getLevel(), pos, mirror.getBlockState(), mirror);
        helper.assertTrue(mirror.isOn == TileEntitySolarMirror.isSolarActive(
                        TileEntitySolarMirror.getSun(helper.getLevel(), pos),
                        helper.getLevel().canSeeSky(pos.above())),
                "The live server gate must match the current public skylight APIs");
        helper.succeed();
    }

    public static void multiMirrorHeatAccumulationAndDecay(GameTestHelper helper) {
        BlockPos coreRelative = new BlockPos(3, 1, 3);
        TileEntitySolarBoiler boiler = placeBoiler(helper, coreRelative);
        BlockPos core = helper.absolutePos(coreRelative);

        BlockPos firstRelative = new BlockPos(7, 1, 3);
        BlockPos secondRelative = new BlockPos(8, 1, 3);
        SolarMirror mirrorBlock = ModSolarBlocks.solar_mirror.get();
        helper.setBlock(firstRelative, mirrorBlock.defaultBlockState());
        helper.setBlock(secondRelative, mirrorBlock.defaultBlockState());
        TileEntitySolarMirror first = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, firstRelative, TileEntitySolarMirror.class);
        TileEntitySolarMirror second = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, secondRelative, TileEntitySolarMirror.class);
        first.setTarget(core.getX(), core.getY() + 1, core.getZ());
        second.setTarget(core.getX(), core.getY() + 1, core.getZ());

        TileEntitySolarMirror.serverTick(helper.getLevel(), first.getBlockPos(), first.getBlockState(), first);
        TileEntitySolarMirror.serverTick(helper.getLevel(), second.getBlockPos(), second.getBlockState(), second);
        int firstSun = TileEntitySolarMirror.getSun(helper.getLevel(), first.getBlockPos());
        int secondSun = TileEntitySolarMirror.getSun(helper.getLevel(), second.getBlockPos());
        int expected = (firstSun > 0 ? firstSun : 0) + (secondSun > 0 ? secondSun : 0);
        helper.assertTrue(boiler.heatInput == expected,
                "Multiple active mirrors must accumulate their exact heat contributions");
        helper.assertTrue(TileEntitySolarBoiler.maxHeat == 320_000,
                "Solar boiler heat cap changed");
        helper.assertTrue(TileEntitySolarBoiler.diffusion == 0.1D,
                "Solar boiler diffusion constant changed");

        boiler.heat = 0;
        boiler.heatInput = 10;
        TileEntitySolarBoiler.serverTick(helper.getLevel(), core, boiler.getBlockState(), boiler);
        helper.assertTrue(boiler.heat == 9,
                "Solar boiler heat must retain old compound-assignment decay truncation");
        helper.succeed();
    }

    public static void conversionPortsAndPersistence(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        MachineSolarBoiler block = ModSolarBlocks.machine_solar_boiler.get();
        TileEntitySolarBoiler boiler = placeBoiler(helper, relative);
        BlockPos core = helper.absolutePos(relative);

        boiler.tanks[0].setFill(1);
        boiler.heat = 50;
        boiler.heatInput = 0;
        TileEntitySolarBoiler.serverTick(helper.getLevel(), core, boiler.getBlockState(), boiler);
        helper.assertTrue(boiler.tanks[0].getFill() == 0 && boiler.tanks[1].getFill() == 100 && boiler.heat == 0,
                "One water operation must consume 50 TU and produce 100 steam");
        FoldedTestSupport.assertPort(helper, relative, relative.above(2));

        DirPos[] ports = boiler.getConPos();
        helper.assertTrue(ports.length == 2
                        && ports[0].getPos().equals(core.above(3)) && ports[0].getDir() == ForgeDirection.UP
                        && ports[1].getPos().equals(core.below()) && ports[1].getDir() == ForgeDirection.DOWN,
                "Solar boiler ports must remain top and bottom only");
        for (DirPos port : ports) {
            BlockPos cell = port.getPos().offset(-port.getDir().offsetX, -port.getDir().offsetY,
                    -port.getDir().offsetZ);
            helper.assertTrue(block.canConnect(com.hbm.inventory.fluid.Fluids.WATER, helper.getLevel(),
                            cell.getX(), cell.getY(), cell.getZ(), port.getDir()),
                    "Declared solar port is not exposed at " + port.getPos());
        }
        helper.assertFalse(block.canConnect(com.hbm.inventory.fluid.Fluids.WATER, helper.getLevel(),
                        core.getX(), core.getY(), core.getZ(), ForgeDirection.UP),
                "Solar boiler must not expose a middle/top-interior port");

        // serialize/deserialize is the cross-target packet contract; disk persistence uses the same fields
        // through TileEntityLoadedBase's DataIn/DataOut bridge.
        boiler.heatInput = 1234;
        TileEntitySolarBoiler.serverTick(helper.getLevel(), core, boiler.getBlockState(), boiler);
        var boilerBuffer = dev.golem.core.net.GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        boiler.serialize(boilerBuffer);
        boilerBuffer.readerIndex(0);
        TileEntitySolarBoiler boilerCopy = new TileEntitySolarBoiler(core.east(), boiler.getBlockState());
        boilerCopy.deserialize(boilerBuffer);
        helper.assertTrue(boilerCopy.heat == 1234 && boilerCopy.heatInput == 1234,
                "Solar boiler heat sync did not preserve the pre-decay state");
        boilerBuffer.release();

        BlockPos mirrorRelative = new BlockPos(8, 1, 3);
        SolarMirror mirrorBlock = ModSolarBlocks.solar_mirror.get();
        helper.setBlock(mirrorRelative, mirrorBlock.defaultBlockState());
        TileEntitySolarMirror mirror = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, mirrorRelative, TileEntitySolarMirror.class);
        mirror.setTarget(111, 42, -7);
        mirror.isOn = true;
        var mirrorBuffer = dev.golem.core.net.GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        mirror.serialize(mirrorBuffer);
        mirrorBuffer.readerIndex(0);
        TileEntitySolarMirror mirrorCopy = new TileEntitySolarMirror(core.west(), mirror.getBlockState());
        mirrorCopy.deserialize(mirrorBuffer);
        helper.assertTrue(mirrorCopy.getTarget().equals(new BlockPos(111, 42, -7)) && mirrorCopy.isOn,
                "Mirror target and active state did not survive serialization");
        mirrorBuffer.release();
        helper.succeed();
    }

    private static TileEntitySolarBoiler placeBoiler(GameTestHelper helper, BlockPos relative) {
        MachineSolarBoiler block = ModSolarBlocks.machine_solar_boiler.get();
        BlockPos core = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);
        TileEntitySolarBoiler boiler = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntitySolarBoiler.class);
        helper.assertTrue(boiler != null, "Solar boiler core BlockEntity was not created");
        return boiler;
    }

    private static void useOn(GameTestHelper helper, ItemStack stack, BlockPos relative, boolean shifting) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(shifting);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D),
                Direction.UP, pos, false);
        stack.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
    }
}
