package com.hbm.gametest;

import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.entity.missile.EntityMissileTier0;
import com.hbm.items.ModMissileItems;
import com.hbm.items.ModWandItems;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The creative wands' server logic and missile_test's launch-table entry. */
public final class WandGameTests {

    private WandGameTests() {}

    private static void click(GameTestHelper helper, Player player, ItemStack stack, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        stack.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
    }

    /** 1.7 ItemWand: crouch-click saves the block, two plain clicks fill the inclusive box with it. */
    public static void constructionWandFillsBox(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.GOLD_BLOCK.defaultBlockState());
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.CREATIVE);
        ItemStack wand = new ItemStack(ModWandItems.wand.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);

        player.setShiftKeyDown(true);
        click(helper, player, wand, new BlockPos(4, 2, 4));
        player.setShiftKeyDown(false);
        click(helper, player, wand, new BlockPos(1, 1, 1));
        click(helper, player, wand, new BlockPos(2, 2, 3));

        for (int x = 1; x <= 2; x++) for (int y = 1; y <= 2; y++) for (int z = 1; z <= 3; z++) {
            helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(x, y, z));
        }
        helper.assertBlockNotPresent(Blocks.GOLD_BLOCK, new BlockPos(3, 1, 1));
        helper.succeed();
    }

    /** missile_test flies as EntityMissileTest, which names missile_test as its item. */
    public static void testMissileIsLaunchable(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();
        TileEntityLaunchPadBase.MissileFactory factory = TileEntityLaunchPadBase.missiles.get(ModMissileItems.missile_test.get());
        helper.assertTrue(factory != null, "missile_test has no launch-table entry");
        EntityMissileBaseNT missile = factory.create(helper.getLevel(), 0F, 0F, 0F, 0, 0);
        helper.assertTrue(missile instanceof EntityMissileTier0.EntityMissileTest, "missile_test spawns " + missile);
        helper.assertTrue(missile.getMissileItemForInfo().is(ModMissileItems.missile_test.get()),
                "EntityMissileTest does not name missile_test as its item");
        missile.discard();
        helper.succeed();
    }
}
