package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.block.IBlowable;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.machine.FanPhysics;
import com.hbm.blocks.machine.MachineFan;
import com.hbm.blocks.machine.MachineIntake;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Feature-owned contracts for the air intake, six-way fan, force math, and IBlowable callback. */
public final class IntakeFanGameTests {

    private IntakeFanGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function INTAKE_LAYOUT =
            TESTS.function("intake_layout_ports", 100, 8, () -> IntakeFanGameTests::intakeLayoutAndPorts);
    public static final GameTests.Function INTAKE_POWER =
            TESTS.function("intake_power_air", 40, () -> IntakeFanGameTests::intakePowerAndAir);
    public static final GameTests.Function FAN_FORCE =
            TESTS.function("fan_force_modes", 40, () -> IntakeFanGameTests::fanForceAndModes);
    public static final GameTests.Function FAN_OBSTRUCTION =
            TESTS.function("fan_obstruction_blowable", 40, 8, () -> IntakeFanGameTests::fanObstructionAndBlowable);

    /** Golem assigns this test-only block a valid ResourceKey before constructing it. */
    private static final DeferredRegister.Blocks TEST_BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    private static final RegistryObject<BlowableBlock> TEST_BLOWABLE = TEST_BLOCKS.registerBlock(
            "test_fan_blowable", BlowableBlock::new, properties -> properties.noOcclusion());

    public static void init() {}

    public static void intakeLayoutAndPorts(GameTestHelper helper) {
        MachineIntake block = ModMachineBlocks.machine_intake.get();
        TileEntityMachineIntake intake = placeIntake(helper, block, new BlockPos(3, 1, 3));
        DirPos[] ports = intake.getConPos();
        BlockPos core = intake.getBlockPos();
        helper.assertTrue(ports.length == 8, "Intake must expose eight anchors");
        // 1.7 TilePortShapes.solderer: core, +rot, -dir and -dir+rot, each open on its two outer sides
        assertPort(helper, ports[0], core.north(), ForgeDirection.NORTH);
        assertPort(helper, ports[1], core.west(), ForgeDirection.WEST);
        assertPort(helper, ports[2], core.east().north(), ForgeDirection.NORTH);
        assertPort(helper, ports[3], core.east(2), ForgeDirection.EAST);
        assertPort(helper, ports[4], core.south(2), ForgeDirection.SOUTH);
        assertPort(helper, ports[5], core.west().south(), ForgeDirection.WEST);
        assertPort(helper, ports[6], core.east().south(2), ForgeDirection.SOUTH);
        assertPort(helper, ports[7], core.east(2).south(), ForgeDirection.EAST);

        helper.assertTrue(block.canConnect(helper.getLevel(), core, ForgeDirection.NORTH),
                "Intake must expose its north energy anchor");
        helper.assertTrue(block.canConnect(Fluids.AIR, helper.getLevel(), core.getX(), core.getY(), core.getZ(),
                        ForgeDirection.NORTH), "Intake must accept AIR at its north anchor");
        helper.assertFalse(block.canConnect(Fluids.OXYGEN, helper.getLevel(), core.getX(), core.getY(), core.getZ(),
                        ForgeDirection.NORTH), "Intake must reject non-AIR fluids");
        helper.assertFalse(block.canConnect(Fluids.AIR, helper.getLevel(), core.getX(), core.getY(), core.getZ(),
                        ForgeDirection.UP), "Intake must reject vertical AIR connections");
        helper.assertTrue(block.getDimensions()[2] == 1 && block.getDimensions()[4] == 1
                        && block.getOffset() == 0, "Intake footprint or offset changed");
        helper.succeed();
    }

    public static void intakePowerAndAir(GameTestHelper helper) {
        TileEntityMachineIntake intake = placeIntake(helper, ModMachineBlocks.machine_intake.get(),
                new BlockPos(3, 1, 3));
        intake.power = 200;
        TileEntityMachineIntake.serverTick(helper.getLevel(), intake.getBlockPos(), intake.getBlockState(), intake);
        helper.assertTrue(intake.power == 100 && intake.compair.getFill() == 1_000,
                "Intake must consume exactly 100 HE and fill 1,000 mB AIR per tick");
        helper.assertTrue(intake.isMotorRunning(), "Intake motor state must remain on at the 100 HE threshold");

        TileEntityMachineIntake.serverTick(helper.getLevel(), intake.getBlockPos(), intake.getBlockState(), intake);
        helper.assertTrue(intake.power == 0 && !intake.isMotorRunning(),
                "Intake motor must stop below the 100 HE threshold");
        helper.succeed();
    }

    public static void fanForceAndModes(GameTestHelper helper) {
        MachineFan block = ModMachineBlocks.fan.get();
        BlockPos relative = new BlockPos(3, 1, 3);
        BlockPos pos = helper.absolutePos(relative);
        helper.setBlock(relative.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.setBlock(relative, block.defaultBlockState().setValue(MachineFan.FACING, Direction.NORTH));
        MachineFan.TileEntityFan fan = (MachineFan.TileEntityFan) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(fan != null, "Fan BlockEntity was not created");

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        helper.assertTrue(block.onScrew(helper.getLevel(), player, pos, Direction.NORTH,
                        Vec3.atCenterOf(pos), InteractionHand.MAIN_HAND, ToolType.SCREWDRIVER),
                "Fan screwdriver action was rejected");
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(MachineFan.FACING) == Direction.SOUTH,
                "Fan screwdriver must reverse all six-way facing");
        helper.assertTrue(block.onScrew(helper.getLevel(), player, pos, Direction.NORTH,
                        Vec3.atCenterOf(pos), InteractionHand.MAIN_HAND, ToolType.HAND_DRILL),
                "Fan hand-drill action was rejected");
        helper.assertFalse(fan.falloff, "Hand drill must toggle falloff off");
        helper.assertTrue(block.onScrew(helper.getLevel(), player, pos, Direction.NORTH,
                        Vec3.atCenterOf(pos), InteractionHand.MAIN_HAND, ToolType.DEFUSER),
                "Fan defuser action was rejected");
        helper.assertTrue(fan.suck, "Defuser must toggle suction on");
        helper.assertTrue(MachineFan.FACING.getPossibleValues().size() == 6,
                "Fan must retain all six facing directions");

        helper.getLevel().setBlock(pos, helper.getLevel().getBlockState(pos)
                .setValue(MachineFan.FACING, Direction.NORTH), 3);
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() - 0.5D, new ItemStack(Items.STICK));
        entity.setNoGravity(true);
        entity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(entity);
        MachineFan.TileEntityFan.tick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), fan);
        helper.assertTrue(Math.abs(entity.getDeltaMovement().z() - 0.1D) < 0.000001D,
                "Fan suction must reverse the constant 0.1 push");

        helper.assertTrue(Math.abs(FanPhysics.coefficient(1D, true, false) - 0.1425D) < 0.000001D,
                "Fan falloff coefficient changed");
        helper.assertTrue(Math.abs(FanPhysics.coefficient(1D, true, true) + 0.1425D) < 0.000001D,
                "Fan suction coefficient changed sign");
        helper.succeed();
    }

    public static void fanObstructionAndBlowable(GameTestHelper helper) {
        MachineFan block = ModMachineBlocks.fan.get();
        BlockPos relative = new BlockPos(3, 1, 3);
        BlockPos pos = helper.absolutePos(relative);
        helper.setBlock(relative.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.setBlock(relative, block.defaultBlockState().setValue(MachineFan.FACING, Direction.NORTH));
        MachineFan.TileEntityFan fan = (MachineFan.TileEntityFan) helper.getLevel().getBlockEntity(pos);

        BlowableBlock.reset();
        BlowableBlock blowable = TEST_BLOWABLE.get();
        BlockPos obstruction = pos.north();
        helper.getLevel().setBlock(obstruction, blowable.defaultBlockState(), 3);
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() - 1.5D, new ItemStack(Items.STICK));
        entity.setNoGravity(true);
        entity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(entity);

        MachineFan.TileEntityFan.tick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), fan);
        helper.assertTrue(BlowableBlock.calls == 1 && BlowableBlock.distance == 1
                        && BlowableBlock.direction == ForgeDirection.NORTH,
                "Fan must call the first IBlowable obstruction once on the server");
        helper.assertTrue(entity.getDeltaMovement().lengthSqr() == 0D,
                "Fan must not affect entities beyond an obstruction");
        helper.assertTrue(fan.spin == 30F, "Powered fan must spin even when its stream is obstructed");
        helper.succeed();
    }

    private static TileEntityMachineIntake placeIntake(GameTestHelper helper, MachineIntake block, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), pos, Direction.NORTH);
        BlockPos core = MultiblockSurface.coreOfAny(helper.getLevel(), pos);
        TileEntityMachineIntake intake = core == null ? null
                : (TileEntityMachineIntake) helper.getLevel().getBlockEntity(core);
        helper.assertTrue(intake != null, "Intake core BlockEntity was not created");
        return intake;
    }

    private static void assertPort(GameTestHelper helper, DirPos port, BlockPos expected, ForgeDirection direction) {
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Intake port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }

    private static final class BlowableBlock extends CompatBlock implements IBlowable {
        private static int calls;
        private static int distance;
        private static ForgeDirection direction;

        private BlowableBlock(Properties properties) {
            super(properties);
            reset();
        }

        private static void reset() {
            calls = 0;
            distance = 0;
            direction = ForgeDirection.UNKNOWN;
        }

        @Override
        public void applyFan(Level level, BlockPos pos,
                             ForgeDirection direction, int distance) {
            calls++;
            BlowableBlock.distance = distance;
            BlowableBlock.direction = direction;
        }
    }
}
