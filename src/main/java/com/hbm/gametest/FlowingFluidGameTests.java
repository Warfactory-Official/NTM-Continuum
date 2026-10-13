package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.fluid.ModWorldFluids;
import com.hbm.items.ModFluidNetItems;
import dev.golem.core.fluid.FluidBlock;
import dev.golem.core.fluid.WorldFluids;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * The 1.7 world fluids flow as 1.7's Forge fluids did: a classic fluid of {@code q} quanta
 * on flat ground fills the diamond of Manhattan radius {@code q - 1} around its source, each ring one quantum
 * lower, and stops; it never makes a second source; its flowing blocks are lower than the source; and everything
 * drains away when the source is taken. Corium is a finite fluid and keeps its volume. None of them can be targeted
 * or broken, and the physics tags are 1.7's materials. Bodies live here, catalogue rows in
 * {@link HbmGameTestFunctions}.
 */
public final class FlowingFluidGameTests {

    private FlowingFluidGameTests() {}

    /** Floor half-width: far enough out that no flowing block's four-block hole search reaches past the floor. */
    private static final int FLOOR = 12;
    private static final BlockPos SOURCE = new BlockPos(0, 2, 0);

    // Reach, levels, decay

    public static void mudReach(GameTestHelper helper) {
        reach(helper, ModHazardBlocks.mud_block.get(), 4);
    }

    public static void toxicReach(GameTestHelper helper) {
        reach(helper, ModFluidNetBlocks.toxic_block.get(), 4);
    }

    public static void schrabidicReach(GameTestHelper helper) {
        reach(helper, ModFluidNetBlocks.schrabidic_block.get(), 4);
    }

    public static void sulfuricReach(GameTestHelper helper) {
        reach(helper, ModFluidNetBlocks.sulfuric_acid_block.get(), 8);
    }

    /**
     * A source of {@code fluid} on an obsidian floor: once settled, every cell at Manhattan distance {@code d < q}
     * holds {@code q - d} quanta and the next ring is empty; exactly one source; flowing heights below the source's.
     * Then the source is removed and everything must drain away (1.7 never makes a source, so nothing is left).
     */
    private static void reach(GameTestHelper helper, Block fluid, int quanta) {
        ServerLevel level = helper.getLevel();
        floor(helper, Blocks.OBSIDIAN);
        helper.setBlock(SOURCE, fluid.defaultBlockState());

        FluidBlock block = (FluidBlock) fluid;
        boolean[] drained = {false};
        poll(helper, 1, 360, () -> {
            if (!drained[0]) {
                List<String> wrong = shape(helper, block, quanta);
                if (!wrong.isEmpty()) return false;
                assertShape(helper, block, quanta);
                level.setBlock(helper.absolutePos(SOURCE), Blocks.AIR.defaultBlockState(), 3);
                drained[0] = true;
                return false;
            }
            return count(helper, block, quanta + 1) == 0;
        }, () -> drained[0]
                ? "the flowing " + name(fluid) + " must drain away once its source is gone; " + count(helper, block, quanta + 1) + " block(s) left"
                : "the " + name(fluid) + " never settled into its 1.7 diamond: " + String.join(" | ", shape(helper, block, quanta)),
                () -> {
                    clear(helper, quanta + 2);
                    helper.succeed();
                });
    }

    /** Every mismatch between the plot and the settled 1.7 diamond of {@code quanta}. */
    private static List<String> shape(GameTestHelper helper, FluidBlock block, int quanta) {
        List<String> wrong = new ArrayList<>();
        int sources = 0;
        for (int dx = -quanta; dx <= quanta; dx++) {
            for (int dz = -quanta; dz <= quanta; dz++) {
                int d = Math.abs(dx) + Math.abs(dz);
                BlockPos rel = SOURCE.offset(dx, 0, dz);
                BlockState state = helper.getLevel().getBlockState(helper.absolutePos(rel));
                if (state.is(block) && state.getValue(FluidBlock.LEVEL) == 0) sources++;
                if (d < quanta) {
                    if (!state.is(block) || block.quantaOf(state) != quanta - d) {
                        wrong.add(rel.getX() + "," + rel.getZ() + " wants " + (quanta - d) + " quanta, has " + state);
                    }
                } else if (d == quanta && !state.isAir()) {
                    wrong.add(rel.getX() + "," + rel.getZ() + " is past the reach but has " + state);
                }
            }
        }
        if (sources != 1) wrong.add(sources + " sources instead of 1");
        return wrong;
    }

    /** The settled diamond's heights, shapes and the source itself. */
    private static void assertShape(GameTestHelper helper, FluidBlock block, int quanta) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(SOURCE);
        BlockState sourceState = level.getBlockState(source);
        FluidState sourceFluid = sourceState.getFluidState();
        helper.assertTrue(sourceFluid.isSource(), "the placed block must be the fluid's source: " + sourceFluid);
        helper.assertTrue(sourceState.getShape(level, source).isEmpty() && sourceState.getCollisionShape(level, source, CollisionContext.empty()).isEmpty(),
                name(block) + " must have no outline or collision shape, so no tool can target or break it");
        helper.assertTrue(sourceState.getRenderShape() == RenderShape.INVISIBLE, name(block) + " is drawn by the fluid renderer only");
        float previous = sourceFluid.getHeight(level, source);
        for (int d = 1; d < quanta; d++) {
            BlockPos at = helper.absolutePos(SOURCE.east(d));
            FluidState fluid = level.getFluidState(at);
            float height = fluid.getHeight(level, at);
            helper.assertTrue(!fluid.isSource() && height < previous && height < 1.0F,
                    name(block) + " " + d + " out must be flowing and lower than the cell before it ("
                            + previous + "), is " + fluid + " at " + height);
            previous = height;
        }
    }

    // Corium (BlockFluidFinite)

    /**
     * 1.7 {@code BlockFluidFinite} on bedrock: a full corium block (5 quanta) levels out without gaining or losing a
     * quantum. The flow updates are run by hand within one tick (seeded so the corium's one-in-ten solidification
     * does not fire), so the scheduled ticks and their random solidification cannot interfere.
     */
    public static void coriumConservesVolume(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper, Blocks.BEDROCK);
        FluidBlock corium = (FluidBlock) ModFluidNetBlocks.corium_block.get();
        helper.setBlock(SOURCE, corium.defaultBlockState());
        helper.assertTrue(corium.quantaOf(level.getBlockState(helper.absolutePos(SOURCE))) == 5,
                "a placed corium block is full: 1.7 onBlockPlaced gives quantaPerBlock - 1 (5 quanta)");
        for (int round = 0; round < 6; round++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos at = helper.absolutePos(SOURCE.offset(dx, 0, dz));
                    BlockState state = level.getBlockState(at);
                    if (state.is(corium)) corium.flowTick(level, at, state, noSolidify());
                }
            }
        }
        int total = 0;
        int cells = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockState state = level.getBlockState(helper.absolutePos(SOURCE.offset(dx, 0, dz)));
                if (state.is(corium)) {
                    total += corium.quantaOf(state);
                    cells++;
                }
            }
        }
        clear(helper, 4);
        helper.assertTrue(total == 5, "corium must keep its 5 quanta while it spreads (1.7 BlockFluidFinite), has " + total);
        helper.assertTrue(cells == 5, "5 quanta on flat ground settle into the source and its four neighbours, one each; "
                + cells + " cells hold corium");
        helper.succeed();
    }

    /** A random whose first {@code nextInt(10)} is not 0, so {@code BlockCorium.afterFlow} never solidifies. */
    private static RandomSource noSolidify() {
        for (long seed = 1; ; seed++) {
            if (RandomSource.create(seed).nextInt(10) != 0) return RandomSource.create(seed);
        }
    }

    // Acid, lava

    /** 1.7 AcidBlock: after its first flow (5 ticks) every non-acid neighbour, its floor included, is air. */
    public static void acidDissolvesNeighbours(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        for (Direction dir : Direction.Plane.HORIZONTAL) helper.setBlock(SOURCE.relative(dir), Blocks.STONE.defaultBlockState());
        helper.setBlock(SOURCE.above(), Blocks.STONE.defaultBlockState());
        helper.setBlock(SOURCE, ModFluidNetBlocks.acid_block.get().defaultBlockState());
        helper.runAfterDelay(7, () -> {
            List<String> left = new ArrayList<>();
            for (Direction dir : Direction.values()) {
                BlockState state = helper.getLevel().getBlockState(helper.absolutePos(SOURCE.relative(dir)));
                if (state.is(Blocks.STONE)) left.add(dir.getName());
            }
            clear(helper, 6);
            helper.assertTrue(left.isEmpty(), "acid must dissolve every neighbour on its first flow; stone left " + left);
            helper.succeed();
        });
    }

    /**
     * 1.7 VolcanicBlock on obsidian: the lava runs at most three blocks out (quanta 4), never makes a second source,
     * and its thin edges set into basalt.
     */
    public static void volcanicLavaFlowsAndSets(GameTestHelper helper) {
        floor(helper, Blocks.OBSIDIAN);
        Block lava = ModFluidNetBlocks.volcanic_lava_block.get();
        helper.setBlock(SOURCE, lava.defaultBlockState());
        poll(helper, 1, 580, () -> {
            int basalt = 0;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockState state = helper.getLevel().getBlockState(helper.absolutePos(SOURCE.offset(dx, 0, dz)));
                    int d = Math.abs(dx) + Math.abs(dz);
                    helper.assertTrue(d < 4 || !state.is(lava), "volcanic lava (quanta 4) reached " + d + " blocks out");
                    helper.assertTrue(d == 0 || !(state.is(lava) && state.getValue(FluidBlock.LEVEL) == 0),
                            "volcanic lava made a source " + d + " blocks out");
                    if (state.is(ModBlocks.basalt.get())) basalt++;
                }
            }
            return basalt > 0;
        }, () -> "volcanic lava never set into basalt at its edges", () -> {
            clear(helper, 5);
            helper.succeed();
        });
    }

    // Registration

    /** 1.7 materials as fluid tags, the buckets, the fluid ids, and the source as the default state. */
    public static void registration(GameTestHelper helper) {
        check(helper, ModWorldFluids.SULFURIC_ACID, true, false);
        check(helper, ModWorldFluids.VOLCANIC_LAVA, false, true);
        check(helper, ModWorldFluids.RAD_LAVA, false, true);
        for (WorldFluids.Handle none : List.of(ModWorldFluids.MUD, ModWorldFluids.ACID, ModWorldFluids.TOXIC,
                ModWorldFluids.SCHRABIDIC, ModWorldFluids.CORIUM)) {
            check(helper, none, false, false);
        }
        bucket(helper, ModWorldFluids.MUD, ModFluidNetItems.bucket_mud.get());
        bucket(helper, ModWorldFluids.ACID, ModFluidNetItems.bucket_acid.get());
        bucket(helper, ModWorldFluids.TOXIC, ModFluidNetItems.bucket_toxic.get());
        bucket(helper, ModWorldFluids.SCHRABIDIC, ModFluidNetItems.bucket_schrabidic_acid.get());
        bucket(helper, ModWorldFluids.SULFURIC_ACID, ModFluidNetItems.bucket_sulfuric_acid.get());
        helper.succeed();
    }

    private static void check(GameTestHelper helper, WorldFluids.Handle fluid, boolean water, boolean lava) {
        FluidState source = fluid.source().defaultFluidState();
        String id = fluid.namespace() + ":" + fluid.spec().path();
        helper.assertTrue(source.is(FluidTags.WATER) == water && source.is(FluidTags.LAVA) == lava,
                id + " must be in minecraft:water " + water + " / minecraft:lava " + lava + " (its 1.7 material)");
        helper.assertTrue(fluid.block().defaultBlockState().getFluidState().isSource(),
                id + ": the block's default state must be the source, so worldgen and old saves place sources");
        helper.assertTrue(fluid.source().defaultFluidState().createLegacyBlock().is(fluid.block()),
                id + ": the fluid's block must be its 1.7 block");
    }

    private static void bucket(GameTestHelper helper, WorldFluids.Handle fluid, Item bucket) {
        helper.assertTrue(fluid.source().getBucket() == bucket && fluid.flowing().getBucket() == bucket,
                fluid.spec().path() + " must be picked up into " + bucket);
    }

    // Support

    private static void floor(GameTestHelper helper, Block block) {
        for (int dx = -FLOOR; dx <= FLOOR; dx++) {
            for (int dz = -FLOOR; dz <= FLOOR; dz++) {
                helper.setBlock(SOURCE.offset(dx, -1, dz), block.defaultBlockState());
                for (int dy = 0; dy <= 2; dy++) helper.setBlock(SOURCE.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static int count(GameTestHelper helper, Block block, int radius) {
        int found = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (helper.getLevel().getBlockState(helper.absolutePos(SOURCE.offset(dx, 0, dz))).is(block)) found++;
            }
        }
        return found;
    }

    /** Removes everything this test placed, fluids first, so nothing flows into a neighbouring plot. */
    private static void clear(GameTestHelper helper, int radius) {
        for (int dy = 1; dy >= -1; dy--) {
            for (int dx = -FLOOR; dx <= FLOOR; dx++) {
                for (int dz = -FLOOR; dz <= FLOOR; dz++) {
                    helper.getLevel().setBlock(helper.absolutePos(SOURCE.offset(dx, dy, dz)), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static void poll(GameTestHelper helper, int from, int deadline, BooleanSupplier condition,
                             Supplier<String> stuck, Runnable then) {
        helper.runAfterDelay(from, () -> pollStep(helper, deadline - from, condition, stuck, then));
    }

    private static void pollStep(GameTestHelper helper, int ticksLeft, BooleanSupplier condition,
                                 Supplier<String> stuck, Runnable then) {
        if (condition.getAsBoolean()) {
            then.run();
            return;
        }
        if (ticksLeft <= 0) {
            String message = stuck.get();
            clear(helper, FLOOR);
            helper.fail(message);
            return;
        }
        helper.runAfterDelay(1, () -> pollStep(helper, ticksLeft - 1, condition, stuck, then));
    }
}
