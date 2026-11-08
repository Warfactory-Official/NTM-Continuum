package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Explosions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.7 {@code MachineOilWell.onBlockExploded}: a derrick caught in an explosion with any oil or gas left empties both
 * tanks into a fixed strength 15 blast at its core. An empty derrick, or one broken by hand, does not explode. The blast
 * is told by a ring of planks seven blocks out, which the strength 4 trigger (TNT) cannot reach.
 */
public final class DerrickBlastGameTests {

    private DerrickBlastGameTests() {}

    private static final BlockPos CORE = new BlockPos(8, 1, 8);
    private static final int RING = 24;

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function FULL =
            TESTS.function("derrick_full_tanks_blow_up", 60, 16, () -> helper -> blast(helper, 64_000, 64_000, true));
    public static final GameTests.Function TRACE =
            TESTS.function("derrick_one_mb_of_gas_blows_up", 60, 16, () -> helper -> blast(helper, 0, 1, true));
    public static final GameTests.Function EMPTY =
            TESTS.function("derrick_empty_does_not_blow_up", 60, 16, () -> helper -> blast(helper, 0, 0, false));
    public static final GameTests.Function BROKEN =
            TESTS.function("derrick_broken_by_hand_does_not_blow_up", 60, 16, () -> DerrickBlastGameTests::broken);

    public static void init() {}

    private static TileEntityMachineOilWell derrick(GameTestHelper helper, int oil, int gas) {
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_well.get(), CORE);
        TileEntityMachineOilWell well = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachineOilWell.class);
        well.tanks[0].setFill(oil);
        well.tanks[1].setFill(gas);
        for (int i = 0; i < RING; i++) {
            double angle = Math.PI * 2D * i / RING;
            helper.setBlock(ringCell(i, angle), Blocks.OAK_PLANKS.defaultBlockState());
        }
        return well;
    }

    private static BlockPos ringCell(int i, double angle) {
        return CORE.offset((int) Math.round(Math.cos(angle) * 7D), 1, (int) Math.round(Math.sin(angle) * 7D));
    }

    private static int ringLeft(GameTestHelper helper) {
        int left = 0;
        for (int i = 0; i < RING; i++) {
            if (helper.getBlockState(ringCell(i, Math.PI * 2D * i / RING)).is(Blocks.OAK_PLANKS)) left++;
        }
        return left;
    }

    private static void blast(GameTestHelper helper, int oil, int gas, boolean blows) {
        TileEntityMachineOilWell well = derrick(helper, oil, gas);
        BlockPos core = helper.absolutePos(CORE);
        Explosions.explode(helper.getLevel(), null, core.getX() + 0.5D, core.getY() + 0.5D, core.getZ() + 0.5D, 4F,
                Level.ExplosionInteraction.BLOCK);
        int left = ringLeft(helper);
        if (blows) {
            helper.assertTrue(well.tanks[0].getFill() == 0 && well.tanks[1].getFill() == 0, "the blast empties both tanks");
            // 1.7's 24-ray allocator leaves gaps this far out, so some planks survive even a full blast
            helper.assertTrue(left < RING, "oil " + oil + ", gas " + gas + ": the strength 15 blast left " + left
                    + " of " + RING + " planks seven blocks out");
        } else {
            helper.assertTrue(left == RING, "an empty derrick does not explode, but " + (RING - left) + " planks went");
        }
        helper.succeed();
    }

    private static void broken(GameTestHelper helper) {
        derrick(helper, 64_000, 64_000);
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        player.gameMode.destroyBlock(helper.absolutePos(CORE));
        helper.assertTrue(helper.getBlockState(CORE).isAir(), "the derrick is broken");
        helper.assertTrue(ringLeft(helper) == RING, "1.7 blows the derrick up only from an explosion, not when broken");
        helper.succeed();
    }
}
