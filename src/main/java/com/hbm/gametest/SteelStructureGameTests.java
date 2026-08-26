package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.blocks.generic.BlockGrate;
import com.hbm.blocks.generic.BlockScaffold;
import com.hbm.items.ModDataComponents;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Placement, shape, and registration contracts for the steel grate/scaffold wave. */
public final class SteelStructureGameTests {

    private SteelStructureGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function REGISTRY_AND_GRATE =
            TESTS.function("steel_registry_and_grate", 40, () -> SteelStructureGameTests::registryAndGrate);
    public static final GameTests.Function SCAFFOLD_PLACEMENT =
            TESTS.function("steel_scaffold_placement", 40, () -> SteelStructureGameTests::scaffoldPlacement);

    public static void init() {}

    public static void registryAndGrate(GameTestHelper helper) {
        helper.assertTrue(Ids.of(Constants.MOD_ID, "steel_grate")
                        .equals(BuiltInRegistries.BLOCK.getKey(ModSteelBlocks.steel_grate.get())),
                "steel_grate registry id changed");
        helper.assertTrue(Ids.of(Constants.MOD_ID, "steel_scaffold")
                        .equals(BuiltInRegistries.BLOCK.getKey(ModSteelBlocks.steel_scaffold.get())),
                "steel_scaffold registry id changed");

        BlockGrate grate = ModSteelBlocks.steel_grate.get();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int height = 0; height <= 9; height++) {
            var state = grate.defaultBlockState().setValue(BlockGrate.HEIGHT, height);
            var shape = grate.getShape(state, helper.getLevel(), pos, CollisionContext.empty());
            double expectedMin = height == 9 ? -0.125D : height * 0.125D;
            helper.assertTrue(Math.abs(shape.min(Direction.Axis.Y) - expectedMin) < 0.0001D,
                    "grate height " + height + " minY is " + shape.min(Direction.Axis.Y));
            helper.assertTrue(Math.abs(shape.max(Direction.Axis.Y) - (expectedMin + 0.125D)) < 0.0001D,
                    "grate height " + height + " maxY is " + shape.max(Direction.Axis.Y));
        }

        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "steel_grate")));
        helper.assertTrue(grate.getStateForPlacement(context(helper, player, stack, Direction.DOWN, 0.5D)).getValue(BlockGrate.HEIGHT) == 7,
                "downward grate placement must use height 7");
        helper.assertTrue(grate.getStateForPlacement(context(helper, player, stack, Direction.UP, 0.5D)).getValue(BlockGrate.HEIGHT) == 0,
                "upward grate placement must use height 0");
        helper.assertTrue(grate.getStateForPlacement(context(helper, player, stack, Direction.NORTH, 0.25D)).getValue(BlockGrate.HEIGHT) == 2,
                "side grate placement must quantize hitY into eight steps");
        helper.succeed();
    }

    public static void scaffoldPlacement(GameTestHelper helper) {
        BlockScaffold scaffold = ModSteelBlocks.steel_scaffold.get();
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "steel_scaffold")));
        ModDataComponents.ITEM_VARIANT.set(stack, 2);

        player.setYRot(0F);
        var horizontalNs = scaffold.getStateForPlacement(context(helper, player, stack, Direction.UP, 0.5D));
        helper.assertTrue(horizontalNs.getValue(BlockScaffold.ORIENT) == BlockScaffold.Orientation.HORIZONTAL_NS,
                "yaw 0 scaffold must be horizontal north/south");
        helper.assertTrue(horizontalNs.getValue(BlockScaffold.VARIANT) == BlockScaffold.Variant.WHITE,
                "scaffold item component must select the white variant");

        player.setYRot(90F);
        var horizontalEw = scaffold.getStateForPlacement(context(helper, player, stack, Direction.UP, 0.5D));
        helper.assertTrue(horizontalEw.getValue(BlockScaffold.ORIENT) == BlockScaffold.Orientation.HORIZONTAL_EW,
                "yaw 90 scaffold must be horizontal east/west");

        var verticalNs = scaffold.getStateForPlacement(context(helper, player, stack, Direction.NORTH, 0.5D));
        helper.assertTrue(verticalNs.getValue(BlockScaffold.ORIENT) == BlockScaffold.Orientation.VERTICAL_NS,
                "north/south side placement must be vertical north/south");
        var verticalEw = scaffold.getStateForPlacement(context(helper, player, stack, Direction.EAST, 0.5D));
        helper.assertTrue(verticalEw.getValue(BlockScaffold.ORIENT) == BlockScaffold.Orientation.VERTICAL_EW,
                "east/west side placement must be vertical east/west");

        helper.assertTrue(scaffold.getShape(horizontalNs, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)),
                        CollisionContext.empty()).max(Direction.Axis.Y) == 1.0D,
                "horizontal scaffold collision must occupy the full block height");
        helper.succeed();
    }

    private static BlockPlaceContext context(GameTestHelper helper, Player player, ItemStack stack,
                                              Direction face, double localY) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockHitResult hit = new BlockHitResult(
                new Vec3(pos.getX() + 0.5D, pos.getY() + localY, pos.getZ() + 0.5D),
                face, pos, false);
        return new BlockPlaceContext(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
    }
}
