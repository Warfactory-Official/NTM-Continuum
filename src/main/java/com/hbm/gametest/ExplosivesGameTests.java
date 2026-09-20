package com.hbm.gametest;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.blocks.bomb.Landmine;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.tileentity.bomb.TileEntityCharge;
import com.hbm.tileentity.bomb.TileEntityLandmine;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Contracts for the conventional-explosives family ({@code com.hbm.explosives}): one landmine and one
 * timed charge. Registered through {@link HbmGameTestFunctions}.
 */
public final class ExplosivesGameTests {

    private ExplosivesGameTests() {}

    /**
     * An AP mine placed next to a player is not primed (the 1.7 self-protection rule); the defuser lifts
     * it without setting it off and leaves the block gone.
     */
    public static void landminePrimingAndDefuse(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(relative, ModExplosivesBlocks.mine_ap.get().defaultBlockState());
        BlockPos pos = helper.absolutePos(relative);
        TileEntityLandmine mine = GameTestHelpers.blockEntity(helper, relative, TileEntityLandmine.class);

        Landmine block = ModExplosivesBlocks.mine_ap.get();
        helper.assertTrue(block.range == 1.5D && block.height == 1D && block.type == Landmine.Type.AP,
                "AP mine trigger box must be the 1.7 1.5 x 1 box");

        // A mock player is not in the level's entity lists; a pig two blocks away is, and it is a LivingEntity.
        net.minecraft.world.entity.LivingEntity pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, relative.east(2));
        TileEntityLandmine.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), mine);
        helper.assertTrue(!mine.isPrimed(), "a mine must not prime while something stands in twice its trigger radius");
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(block), "an unprimed mine must not detonate");
        pig.discard();
        TileEntityLandmine.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), mine);
        helper.assertTrue(mine.isPrimed(), "a mine must prime once its doubled trigger radius is clear");

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);

        helper.assertTrue(block.onScrew(helper.getLevel(), player, pos, Direction.UP, Vec3.atCenterOf(pos),
                InteractionHand.MAIN_HAND, ToolType.DEFUSER), "the defuser must lift a mine");
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "the defused mine must be gone");
        helper.assertTrue(!Landmine.safeMode, "safeMode must be lowered again after the defuser");
        helper.succeed();
    }

    /**
     * The C-4 demolition charge: clicked on the floor it faces up, right-clicks cycle the 1.7 timer
     * table, sneak-click arms it, the defuser disarms it and then removes it as its own item.
     */
    public static void chargeTimerAndDefuse(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 6);
        helper.setBlock(relative.below(), Blocks.STONE.defaultBlockState());
        BlockState placed = ModExplosivesBlocks.charge_c4.get().defaultBlockState().setValue(BlockChargeBase.FACING, Direction.UP);
        helper.setBlock(relative, placed);
        BlockPos pos = helper.absolutePos(relative);
        TileEntityCharge charge = GameTestHelpers.blockEntity(helper, relative, TileEntityCharge.class);
        helper.assertTrue(helper.getLevel().getBlockState(pos).canSurvive(helper.getLevel(), pos),
                "a charge on a stone floor must be supported");

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        int[] table = { 100, 200, 300, 600, 1200, 3600, 6000, 0 };
        for (int expected : table) {
            click(helper, pos, player);
            helper.assertTrue(charge.timer == expected, "timer table expected " + expected + " but was " + charge.timer);
        }
        click(helper, pos, player);
        helper.assertTrue(charge.timer == 100 && !charge.started, "one more click must wrap to 5 s, still unarmed");

        player.setShiftKeyDown(true);
        click(helper, pos, player);
        player.setShiftKeyDown(false);
        helper.assertTrue(charge.started, "sneak-click must arm a charge with a timer");
        helper.assertTrue("00".equals(charge.getMinutes()) && "05".equals(charge.getSeconds()), "5 s must read 00:05");

        helper.assertTrue(ModExplosivesBlocks.charge_c4.get().onScrew(helper.getLevel(), player, pos, Direction.UP,
                Vec3.atCenterOf(pos), InteractionHand.MAIN_HAND, ToolType.DEFUSER), "the defuser must act on an armed charge");
        helper.assertTrue(!charge.started && helper.getLevel().getBlockState(pos).is(ModExplosivesBlocks.charge_c4.get()),
                "the first defuser use must only disarm");
        helper.assertTrue(ModExplosivesBlocks.charge_c4.get().onScrew(helper.getLevel(), player, pos, Direction.UP,
                Vec3.atCenterOf(pos), InteractionHand.MAIN_HAND, ToolType.DEFUSER), "the defuser must remove a disarmed charge");
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir() && !BlockChargeBase.safe,
                "the second defuser use must remove the charge without detonating it");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(2)).isEmpty(),
                "removing a charge with the defuser must drop the charge item");
        helper.succeed();
    }

    /** The empty-hand block interaction, the path a player walks (AGENTS.md section 22). */
    private static void click(GameTestHelper helper, BlockPos pos, Player player) {
        BlockState state = helper.getLevel().getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        dev.golem.core.world.Compat.useBlockWithoutItem(state, helper.getLevel(), player, hit);
    }
}
