package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.machine.AutosawFeature;
import com.hbm.blocks.machine.MachineAutosaw;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.AutosawMath;
import com.hbm.tileentity.machine.TileEntityMachineAutosaw;
import dev.golem.core.gametest.GameTests;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Focused contracts for the autosaw's geometry, harvesting, damage, tooling, and sync state. */
public final class AutosawGameTests {

    private AutosawGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function GEOMETRY =
            TESTS.function("autosaw_geometry", 20, () -> AutosawGameTests::geometry);
    public static final GameTests.Function FUEL_AND_TOOL =
            TESTS.function("autosaw_fuel_and_tool", 20, () -> AutosawGameTests::fuelAndTool);
    public static final GameTests.Function CROPS =
            TESTS.function("autosaw_crops", 40, () -> AutosawGameTests::crops);
    public static final GameTests.Function TREE =
            TESTS.function("autosaw_tree", 40, () -> AutosawGameTests::tree);
    public static final GameTests.Function BLADE =
            TESTS.function("autosaw_blade", 40, () -> AutosawGameTests::blade);
    public static final GameTests.Function SYNC =
            TESTS.function("autosaw_sync", 20, () -> AutosawGameTests::sync);

    public static void init() {}

    public static void geometry(GameTestHelper helper) {
        helper.assertFalse(AutosawMath.isInCutWindow(2, 0, 90F), "inner radius must be excluded");
        helper.assertTrue(AutosawMath.isInCutWindow(3, 0, 90F), "radius three must be in the annulus");
        helper.assertFalse(AutosawMath.isInCutWindow(3, 1, 90F), "cut window must remain five degrees");
        helper.assertFalse(AutosawMath.isInCutWindow(0, 10, 90F), "outer radius must be nine");
        helper.succeed();
    }

    public static void fuelAndTool(GameTestHelper helper) {
        // 1.7 TileEntityMachineAutosaw.acceptedFuels: wood oil, ethanol, fish oil, heavy oil and coal creosote
        helper.assertTrue(TileEntityMachineAutosaw.acceptedFuels.size() == 5
                        && TileEntityMachineAutosaw.acceptedFuels.contains(Fluids.COALCREOSOTE)
                        && TileEntityMachineAutosaw.acceptedFuels.contains(Fluids.WOODOIL)
                        && TileEntityMachineAutosaw.acceptedFuels.contains(Fluids.ETHANOL)
                        && TileEntityMachineAutosaw.acceptedFuels.contains(Fluids.FISHOIL)
                        && TileEntityMachineAutosaw.acceptedFuels.contains(Fluids.HEAVYOIL),
                "autosaw fuel set must contain exactly 1.7's five fuels");

        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        MachineAutosaw block = AutosawFeature.machine_autosaw.get();
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        TileEntityMachineAutosaw saw = (TileEntityMachineAutosaw) helper.getLevel().getBlockEntity(pos);
        saw.tank.setFill(2);
        saw.isOn = true;
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        helper.assertTrue(block.onScrew(helper.getLevel(), player, pos, Direction.UP,
                        Vec3.atCenterOf(pos), net.minecraft.world.InteractionHand.MAIN_HAND,
                        ToolType.SCREWDRIVER), "screwdriver must suspend the autosaw");
        helper.assertTrue(saw.isSuspended, "screwdriver did not suspend the autosaw");
        helper.succeed();
    }

    public static void crops(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(8, 1, 8));
        TileEntityMachineAutosaw saw = place(helper, machinePos);
        BlockPos cropPos = machinePos.offset(0, 1, -3);
        helper.getLevel().setBlock(cropPos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
        helper.getLevel().setBlock(cropPos,
                Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7), 3);
        saw.rotationYaw = 0F;
        saw.isOn = true;
        saw.tank.setFill(2);
        TileEntityMachineAutosaw.serverTick(helper.getLevel(), machinePos,
                helper.getLevel().getBlockState(machinePos), saw);
        helper.assertTrue(helper.getLevel().getBlockState(cropPos).is(Blocks.WHEAT)
                        && helper.getLevel().getBlockState(cropPos).getValue(BlockStateProperties.AGE_7) == 0,
                "mature wheat must be harvested and replanted");

        BlockPos immatureMachinePos = helper.absolutePos(new BlockPos(20, 1, 8));
        TileEntityMachineAutosaw immatureSaw = place(helper, immatureMachinePos);
        BlockPos immature = immatureMachinePos.offset(0, 1, -3);
        helper.getLevel().setBlock(immature.below(), Blocks.FARMLAND.defaultBlockState(), 3);
        helper.getLevel().setBlock(immature, Blocks.WHEAT.defaultBlockState(), 3);
        immatureSaw.rotationYaw = 0F;
        immatureSaw.isOn = true;
        immatureSaw.tank.setFill(2);
        TileEntityMachineAutosaw.serverTick(helper.getLevel(), immatureMachinePos,
                helper.getLevel().getBlockState(immatureMachinePos), immatureSaw);
        helper.assertTrue(helper.getLevel().getBlockState(immature).getValue(BlockStateProperties.AGE_7) == 0,
                "immature wheat must be ignored");
        helper.succeed();
    }

    public static void tree(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(8, 1, 8));
        TileEntityMachineAutosaw saw = place(helper, machinePos);
        BlockPos trunk = machinePos.offset(0, 1, -3);
        helper.getLevel().setBlock(trunk.below(), Blocks.DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(trunk, Blocks.OAK_LOG.defaultBlockState(), 3);
        helper.getLevel().setBlock(trunk.above(), Blocks.OAK_LOG.defaultBlockState(), 3);
        helper.getLevel().setBlock(trunk.above(2), Blocks.OAK_LEAVES.defaultBlockState(), 3);
        BlockPos edgeTrunk = trunk.offset(1, 0, 1);
        // Keep this from becoming a second Step-A trunk root. It is a diagonal branch of the hit
        // trunk, and the old scanner only seeds same-height columns that can support a sapling.
        helper.getLevel().setBlock(edgeTrunk.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(edgeTrunk, Blocks.OAK_LOG.defaultBlockState(), 3);
        BlockPos separateTrunk = trunk.offset(4, 0, 0);
        helper.getLevel().setBlock(separateTrunk.below(), Blocks.DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(separateTrunk, Blocks.OAK_LOG.defaultBlockState(), 3);
        saw.rotationYaw = 0F;
        saw.isOn = true;
        saw.tank.setFill(2);
        TileEntityMachineAutosaw.serverTick(helper.getLevel(), machinePos,
                helper.getLevel().getBlockState(machinePos), saw);
        helper.assertTrue(helper.getLevel().getBlockState(trunk).is(Blocks.OAK_SAPLING),
                "the owned trunk base must be replanted");
        helper.assertTrue(helper.getLevel().getBlockState(edgeTrunk).isAir(),
                "18-connected edge logs must belong to the hit tree");
        helper.assertTrue(helper.getLevel().getBlockState(trunk.above()).isAir()
                        && helper.getLevel().getBlockState(trunk.above(2)).isAir(),
                "the owned tree must be felled through leaves");
        helper.assertTrue(helper.getLevel().getBlockState(separateTrunk).is(Blocks.OAK_LOG),
                "a separate trunk must remain outside the hit-tree ownership");
        helper.succeed();
    }

    public static void blade(GameTestHelper helper) {
        BlockPos machinePos = helper.absolutePos(new BlockPos(8, 1, 8));
        TileEntityMachineAutosaw saw = place(helper, machinePos);
        LivingEntity victim = helper.spawn(EntityType.PIG, machinePos);
        victim.setPos(machinePos.getX() + 0.5D, machinePos.getY() + 1.5D,
                machinePos.getZ() - 2.9D);
        saw.rotationYaw = 0F;
        saw.isOn = true;
        saw.tank.setFill(2);
        TileEntityMachineAutosaw.serverTick(helper.getLevel(), machinePos,
                helper.getLevel().getBlockState(machinePos), saw);
        helper.assertFalse(victim.isAlive(), "the moving blade AABB must damage living entities");
        helper.succeed();
    }

    public static void sync(GameTestHelper helper) {
        BlockPos firstPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos secondPos = helper.absolutePos(new BlockPos(5, 1, 2));
        TileEntityMachineAutosaw first = place(helper, firstPos);
        TileEntityMachineAutosaw second = place(helper, secondPos);
        first.isOn = true;
        first.isSuspended = true;
        first.rotationYaw = 123.5F;
        first.rotationPitch = 41.5F;
        first.tank.setTankType(Fluids.ETHANOL);
        first.tank.setFill(77);

        var buf = dev.golem.core.net.GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        first.serialize(buf);
        second.deserialize(buf);
        buf.release();

        helper.assertTrue(second.isOn && second.isSuspended && second.syncYaw == 123.5F
                        && second.syncPitch == 41.5F && second.tank.getTankType() == Fluids.ETHANOL
                        && second.tank.getFill() == 77,
                "autosaw network state did not round-trip");
        helper.succeed();
    }

    private static TileEntityMachineAutosaw place(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlock(pos, AutosawFeature.machine_autosaw.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileEntityMachineAutosaw,
                "autosaw block entity was not created");
        return (TileEntityMachineAutosaw) helper.getLevel().getBlockEntity(pos);
    }
}
