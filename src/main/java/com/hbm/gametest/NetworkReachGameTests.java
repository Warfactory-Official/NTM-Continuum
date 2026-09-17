package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.fluidmk2.IFluidPipeMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.dejson.BlockStates;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Every machine on a network is findable by the conductor laid against it.
 *
 * <p>This is the test the move off UNINOS made necessary. A machine used to announce itself to whatever
 * net was next to it, so one the net could not have found still worked. Discovery is a pull now: a
 * conductor probes the cell on the other side of its face and asks what owns it. A single-block machine
 * owns its own cell and answers for itself, but a multiblock's ports are cells with no block entity at
 * all, and the only thing that can speak for them is the block. A machine that fails this does not throw
 * and does not log — it simply never runs again, which is why a count of them is worth a test.
 *
 * <p>A folded multiblock's cells resolve through the server's core index, and a conductor touching one
 * reaches nothing without the block's own answer.
 */
public final class NetworkReachGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function REACH =
            TESTS.function("network_machines_are_reachable", 40, () -> NetworkReachGameTests::reachable);

    private NetworkReachGameTests() {}

    public static void init() {}

    private static void reachable(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        int power = 0;
        int fluid = 0;

        for (Block block : BlockStates.blocksIn(Constants.MOD_ID)) {
            if (!(block instanceof EntityBlock entityBlock)) continue;
            boolean spread = block instanceof BlockMultiblockCore || block instanceof BlockMultiblockCell;
            if (!spread || portless(block)) continue;

            BlockEntity be;
            try {
                be = entityBlock.newBlockEntity(pos, coreState(block));
            } catch (RuntimeException refusedToExist) {
                continue;
            }
            if (be == null) continue;

            if (be instanceof IEnergyHandlerMK2) {
                power++;
                if (!(block instanceof IEnergyConnectorBlock)) {
                    problems.add(name(block) + " takes power over a footprint no cable can resolve: "
                            + "its block answers no IEnergyConnectorBlock");
                }
            }
            if (be instanceof IFluidUserMK2 && !(be instanceof IFluidPipeMK2)) {
                fluid++;
                if (!(block instanceof IFluidConnectorBlockMK2)) {
                    problems.add(name(block) + " takes fluid over a footprint no duct can resolve: "
                            + "its block answers no IFluidConnectorBlockMK2");
                }
            }
        }

        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.assertTrue(power > 30 && fluid > 30,
                "the registry sweep reached only " + power + " power and " + fluid + " fluid multiblocks, "
                        + "which is too few to be looking at this mod");
        Constants.LOG.info("[REACH] {} power and {} fluid machines spread over a footprint, every one of "
                + "them resolvable from a port cell", power, fluid);
        helper.succeed();
    }

    /**
     * A machine with no port cells at all needs nothing to speak for them. There is one, and it is one
     * on purpose: the creative klystron takes no power and no air, so its {@code formStructure} marks no
     * cells as ports and the question this test asks does not apply to it. It inherits the real
     * klystron's interfaces all the same, which is why it has to be named here rather than detected.
     *
     * <p>A second entry should be argued for, not added. "It has no ports" has to mean the block marks
     * none, not that nobody has tried to plug into it yet.
     */
    private static boolean portless(Block block) {
        return block == ModMachineBlocks.fusion_klystron_creative.get();
    }

    /** A folded core's default state is already a core state; a cell is told apart by {@code CELL}. */
    private static BlockState coreState(Block block) {
        return block.defaultBlockState();
    }

    private static String name(Block block) {
        return block.getName().getString();
    }
}
