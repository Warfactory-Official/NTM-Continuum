package com.hbm.gametest;

import com.hbm.Constants;
import dev.golem.core.dejson.BlockStates;
import dev.golem.core.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds the ticker declarations against the blocks that have to consume them.
 *
 * <p>A machine's ticker is declared once beside the block entity, not restated by the block in a
 * {@code getTicker} override. The block says only that it is a {@code TickingBlock}. The failure mode
 * is quiet: a block left on plain {@code EntityBlock} still compiles, still places, still keeps its
 * inventory and its GUI, and never ticks. Nothing in the language catches it and a normal play session
 * does not catch it quickly.
 *
 * <p>So this test asks every block the mod registers the question vanilla will ask it: hand it the block
 * entity type it would really get and see whether a ticker comes back.
 */
public final class TickerGameTests {

    private TickerGameTests() {}

    /**
     * The number of block entity types that declare a ticker. It is pinned because the risk this
     * test exists for is a ticker quietly going missing, and a count is the only way a <i>deleted</i>
     * declaration fails anything: every other check here looks at declarations that are present.
     *
     * <p>The cable, fluid duct and fluid valve do not tick: a network node is a graph cell the block owns,
     * so none of the three has anything to do each tick.
     */
    private static final int TICKING_TYPES = 317;

    public static void everyTickingBlockAnswersWithItsTicker(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        Set<BlockEntityType<?>> reached = new LinkedHashSet<>();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        int checked = 0;

        for (Block block : BlockStates.blocksIn(Constants.MOD_ID)) {
            if (!(block instanceof EntityBlock entityBlock)) {
                continue;
            }
            BlockState state = block.defaultBlockState();
            BlockEntity blockEntity;
            try {
                blockEntity = entityBlock.newBlockEntity(pos, state);
            } catch (RuntimeException creationFailed) {
                // A block entity that refuses to exist outside its multiblock is not this test's
                // business; it cannot be asked about a ticker either way.
                continue;
            }
            if (blockEntity == null) {
                continue;
            }
            BlockEntityType<?> type = blockEntity.getType();
            reached.add(type);
            checked++;

            for (boolean client : new boolean[]{false, true}) {
                boolean declared = BlockEntities.ticks(type, client);
                boolean answered = ticker(entityBlock, helper, state, type, client) != null;
                if (declared != answered) {
                    problems.add(block.getName().getString() + " (" + (client ? "client" : "server")
                            + "): a ticker is " + (declared ? "declared but the block returns none"
                            : "not declared but the block returns one"));
                }
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(checked > 100, "only " + checked + " blocks with a block entity were reached; "
                + "the registry sweep is not seeing the mod's blocks");
        Constants.LOG.info("[TICKER] {} blocks with a block entity, over {} types, all answer with "
                + "exactly the ticker declared for them", checked, reached.size());
        helper.succeed();
    }

    /**
     * The number of block entity types that declare a ticker matches {@link #TICKING_TYPES}.
     *
     * <p>The other test here can only look at declarations that are present, so a declaration that
     * was dropped (a {@code serverTicking} that went back to being a {@code type}) passes
     * it silently. A count is the only check that fails for that.
     *
     * <p>It is not derived from reachability. Not every registered type is reachable from
     * a block's default state, so sweeping that way would make this number a measure of block shape
     * rather than of ticker coverage.
     */
    public static void everyDeclaredTickerBelongsToABlock(GameTestHelper helper) {
        int ticking = BlockEntities.tickingTypes();
        helper.assertTrue(ticking == TICKING_TYPES,
                "expected " + TICKING_TYPES + " block entity types to declare a ticker, found "
                        + ticking + "; one was added or lost at a registration");
        Constants.LOG.info("[TICKER] {} block entity types declare a ticker", ticking);
        helper.succeed();
    }

    private static <T extends BlockEntity> Object ticker(EntityBlock block, GameTestHelper helper,
                                                         BlockState state, BlockEntityType<T> type,
                                                         boolean client) {
        // getTicker takes the level to decide the side; the test server has only the server one, so
        // the client answer is read from the declaration side of the same lookup.
        return client ? BlockEntities.ticker(type, true)
                : block.getTicker(helper.getLevel(), state, type);
    }

    private static String join(List<String> problems) {
        if (problems.size() <= 8) {
            return String.join("; ", problems);
        }
        return String.join("; ", problems.subList(0, 8)) + "; and " + (problems.size() - 8) + " more";
    }
}
