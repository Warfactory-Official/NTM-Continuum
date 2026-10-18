package com.hbm.gametest;

import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.generic.BlockPedestal;
import com.hbm.blocks.generic.BlockRebar;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.world.gen.util.LogicBlockActions;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * The dungeon family without a client: rebar concrete distribution (1.7 {@code TileEntityRebar.transferFluid})
 * against hand-computed numbers, and the pedestal ritual's matching and consumption.
 */
public final class DungeonGameTests {

    private DungeonGameTests() {}

    /**
     * An L-shaped cage: two rebar on the bottom layer, one above. 100 mB into it: the bottom layer takes it all,
     * 50 each (the per-rebar cap is 50 per delivery, and 100 is exactly two of those); the top rebar gets nothing.
     * Then 2000 mB: the bottom layer tops out at 50 more each (capped), 1900 come back. Once both are at 1000 the
     * next delivery goes to the upper rebar.
     */
    public static void rebarFillsLowestLayer(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(2, 1, 1), top = new BlockPos(2, 2, 1);
        for (BlockPos p : new BlockPos[]{a, b, top}) helper.setBlock(p, ModDungeonBlocks.rebar.get().defaultBlockState());
        BlockRebar.TileEntityRebar ra = GameTestHelpers.blockEntity(helper, a, BlockRebar.TileEntityRebar.class);
        BlockRebar.TileEntityRebar rb = GameTestHelpers.blockEntity(helper, b, BlockRebar.TileEntityRebar.class);
        BlockRebar.TileEntityRebar rt = GameTestHelpers.blockEntity(helper, top, BlockRebar.TileEntityRebar.class);
        helper.assertTrue(ra != null && rb != null && rt != null, "rebar block entities missing");

        long left = ra.transferFluid(Fluids.CONCRETE, 0, 100);
        helper.assertTrue(left == 0 && ra.progress == 50 && rb.progress == 50 && rt.progress == 0,
                "first delivery: left " + left + ", progress " + ra.progress + "/" + rb.progress + "/" + rt.progress);
        left = ra.transferFluid(Fluids.CONCRETE, 0, 2000);
        helper.assertTrue(left == 1900 && ra.progress == 100 && rb.progress == 100,
                "capped delivery: left " + left + ", progress " + ra.progress + "/" + rb.progress);
        helper.assertTrue(ra.transferFluid(Fluids.WATER, 0, 500) == 500, "rebar took a fluid other than concrete");

        ra.progress = 1000;
        rb.progress = 1000;
        // the full layer sets on its next tick; the cage is then only the top rebar
        helper.setBlock(a, Blocks.STONE.defaultBlockState());
        helper.setBlock(b, Blocks.STONE.defaultBlockState());
        left = rt.transferFluid(Fluids.CONCRETE, 0, 30);
        helper.assertTrue(left == 0 && rt.progress == 30, "upper rebar: left " + left + ", progress " + rt.progress);
        helper.succeed();
    }

    /** 1.7 X5846 rebar: one concrete port at y + 1 while a duct touches it, so a duct beside it feeds nothing. */
    public static void rebarFedFromAbove(GameTestHelper helper) {
        BlockPos side = new BlockPos(1, 1, 2), top = new BlockPos(5, 1, 2);
        helper.setBlock(side, ModDungeonBlocks.rebar.get().defaultBlockState());
        helper.setBlock(top, ModDungeonBlocks.rebar.get().defaultBlockState());
        concreteFeed(helper, side.east(), side.east(2));
        concreteFeed(helper, top.above(), top.above(2));
        helper.runAfterDelay(10, () -> {
            BlockRebar.TileEntityRebar bySide = GameTestHelpers.blockEntity(helper, side, BlockRebar.TileEntityRebar.class);
            BlockRebar.TileEntityRebar byTop = GameTestHelpers.blockEntity(helper, top, BlockRebar.TileEntityRebar.class);
            helper.assertTrue(bySide.hasConnection && bySide.progress == 0, "a side duct fed the rebar: " + bySide.progress);
            helper.assertTrue(byTop.progress > 0, "the duct above fed nothing");
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(!helper.getBlockState(top).is(ModDungeonBlocks.rebar.get()), "the fed rebar never set");
                helper.assertTrue(bySide.progress == 0, "a side duct fed the rebar: " + bySide.progress);
                helper.succeed();
            });
        });
    }

    private static void concreteFeed(GameTestHelper helper, BlockPos duct, BlockPos barrelAt) {
        helper.setBlock(duct, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState());
        ((TileEntityFluidDuctNT) helper.getLevel().getBlockEntity(helper.absolutePos(duct))).setFluidType(Fluids.CONCRETE);
        helper.setBlock(barrelAt, ModFluidNetBlocks.barrel_steel.get().defaultBlockState());
        TileEntityBarrel barrel = GameTestHelpers.blockEntity(helper, barrelAt, TileEntityBarrel.class);
        barrel.tank.setTankType(Fluids.CONCRETE);
        barrel.tank.setFill(16_000);
        barrel.mode = 2;
    }

    /** Dani's revolver: gold east and west, lead north and south, the revolver in the middle; redstone performs it. */
    public static void pedestalRitual(GameTestHelper helper) {
        BlockPos center = new BlockPos(4, 1, 4);
        int[][] cells = {{0, 0}, {0, -3}, {0, 3}, {-3, 0}, {3, 0}, {-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
        for (int[] c : cells) helper.setBlock(center.offset(c[0], 0, c[1]), ModDungeonBlocks.pedestal.get().defaultBlockState());
        set(helper, center, "hbm:gun_light_revolver");
        set(helper, center.offset(0, 0, -3), "hbm:plate_lead");
        set(helper, center.offset(0, 0, 3), "hbm:plate_lead");
        set(helper, center.offset(-3, 0, 0), "hbm:plate_gold");
        set(helper, center.offset(3, 0, 0), "hbm:plate_gold");

        helper.assertTrue(PedestalRecipes.matches(new PedestalRecipes.Input("#c:plates/lead", 1),
                LogicBlockActions.stack("hbm:plate_lead", 1)), "lead plate does not match #c:plates/lead");
        helper.assertTrue(!PedestalRecipes.matches(new PedestalRecipes.Input("#c:plates/lead", 1),
                LogicBlockActions.stack("hbm:plate_lead", 2)), "two plates matched a count of one");

        helper.setBlock(center.offset(0, 0, 1), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            ItemStack out = GameTestHelpers.blockEntity(helper, center, BlockPedestal.TileEntityPedestal.class).getItem();
            helper.assertTrue(out.is(BuiltInRegistries.ITEM.getValue(Ids.parse("hbm:gun_light_revolver_dani"))),
                    "the ritual made " + out);
            helper.assertTrue(GameTestHelpers.blockEntity(helper, center.offset(3, 0, 0), BlockPedestal.TileEntityPedestal.class)
                    .getItem().isEmpty(), "the east gold plate was not consumed");
            helper.succeed();
        });
    }

    private static void set(GameTestHelper helper, BlockPos pos, String id) {
        GameTestHelpers.blockEntity(helper, pos, BlockPedestal.TileEntityPedestal.class)
                .setItem(LogicBlockActions.stack(id, 1));
    }
}
