package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.tool.ItemGeigerCounter;
import com.hbm.tileentity.machine.TileEntityGeiger;
import com.hbm.util.ContaminationUtil;
import dev.golem.core.gametest.GameTests;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 1.7 {@code ItemGeigerCounter.onUpdate} click choice (ladder geiger1..6, background geiger1 one click tick in 50),
 * NTM: Space's gate over it (ADDON-API-INVENTORY V-188), and 1.7 {@code ContaminationUtil.printGeigerData}'s readout
 * (X-31); the placed counter's 1.7 {@code TileEntityGeiger} / {@code GeigerCounter} (ladder, gate V-183, sample
 * phase, comparator).
 */
public final class GeigerCounterGameTests {

    private GeigerCounterGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CLICK_LADDER =
            TESTS.function("geiger_counter_click_ladder", 20, () -> GeigerCounterGameTests::clickLadder);
    public static final GameTests.Function CLICK_GATE =
            TESTS.function("geiger_counter_click_gate", 20, () -> GeigerCounterGameTests::clickGate);
    public static final GameTests.Function READOUT =
            TESTS.function("geiger_counter_readout", 20, () -> GeigerCounterGameTests::readout);

    public static final GameTests.Function BLOCK_CLICK_LADDER =
            TESTS.function("geiger_block_click_ladder", 20, () -> GeigerCounterGameTests::blockClickLadder);
    public static final GameTests.Function BLOCK_CLICK_GATE =
            TESTS.function("geiger_block_click_gate", 20, () -> GeigerCounterGameTests::blockClickGate);
    public static final GameTests.Function BLOCK_SAMPLE =
            TESTS.function("geiger_block_sample_and_comparator", 40, () -> GeigerCounterGameTests::blockSample);

    public static void init() {}

    private static final int DRAWS = 100_000;

    /** The sound numbers each dose can choose (1.7's lists; 0 = silent) and the share of silent click ticks. */
    private static void clickLadder(GameTestHelper helper) {
        helper.assertTrue(!ItemGeigerCounter.probabilisticClicks, "1.7's geiger counter clicks on every fifth tick");
        boolean saved = ItemGeigerCounter.probabilisticClicks;
        try {
            ItemGeigerCounter.probabilisticClicks = false;
            expect(helper, 0.5F, Set.of(0, 1), 0.66, 0.673);    // [0, 0, 1]
            expect(helper, 3.0F, Set.of(0, 1), 0.495, 0.505);   // [0, 1]
            expect(helper, 5.0F, Set.of(1), 0.0, 0.0);          // [1]: "x > 5" is strict
            expect(helper, 7.0F, Set.of(1, 2), 0.0, 0.0);
            expect(helper, 10.0F, Set.of(2), 0.0, 0.0);         // "x < 10" and "x > 10" both strict
            expect(helper, 12.0F, Set.of(2, 3), 0.0, 0.0);
            expect(helper, 17.0F, Set.of(3, 4), 0.0, 0.0);
            expect(helper, 22.0F, Set.of(4, 5), 0.0, 0.0);
            expect(helper, 25.0F, Set.of(5), 0.0, 0.0);
            expect(helper, 27.0F, Set.of(5, 6), 0.0, 0.0);
            expect(helper, 35.0F, Set.of(6), 0.0, 0.0);
            // background: geiger1 one click tick in 50 (1.12.2 rolled 1 in 100)
            expect(helper, 0.0F, Set.of(0, 1), 0.976, 0.984);
            expect(helper, 1E-5F, Set.of(0, 1), 0.976, 0.984);  // "x > 1E-5" is strict
        } finally {
            ItemGeigerCounter.probabilisticClicks = saved;
        }
        helper.succeed();
    }

    /** hbm-space: below a dose of 1 a click tick goes ahead only with probability = dose. */
    private static void clickGate(GameTestHelper helper) {
        boolean saved = ItemGeigerCounter.probabilisticClicks;
        try {
            ItemGeigerCounter.probabilisticClicks = true;
            expect(helper, 0.5F, Set.of(0, 1), 0.828, 0.839);   // 1 - 0.5 / 3
            expect(helper, 3.0F, Set.of(0, 1), 0.495, 0.505);   // nextFloat() > 3 never
            expect(helper, 27.0F, Set.of(5, 6), 0.0, 0.0);
            expect(helper, 0.0F, Set.of(0, 1), 0.976, 0.984);   // background clicks are not gated
        } finally {
            ItemGeigerCounter.probabilisticClicks = saved;
        }
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, float dose, Set<Integer> allowed, double silentMin, double silentMax) {
        RandomSource rand = RandomSource.create(4815162342L);
        RandomSource worldRand = RandomSource.create(1234L);
        Set<Integer> seen = new TreeSet<>();
        int silent = 0;
        for (int i = 0; i < DRAWS; i++) {
            int r = ItemGeigerCounter.click(dose, rand, worldRand);
            seen.add(r);
            if (r == 0) silent++;
        }
        double share = silent / (double) DRAWS;
        String label = "dose " + dose + " gate " + ItemGeigerCounter.probabilisticClicks;
        helper.assertTrue(allowed.containsAll(seen) && seen.containsAll(allowed),
                label + ": sounds " + seen + ", 1.7 " + allowed);
        helper.assertTrue(share >= silentMin && share <= silentMax,
                label + ": silent share " + share + ", expected " + silentMin + ".." + silentMax);
    }

    /**
     * 1.7 {@code TileEntityGeiger.updateEntity}: the item's ladder, but "ticker > 0" (not 1E-5) and every draw from one
     * random; the background click draws twice ({@code nextInt(50)}, then {@code 1 + nextInt(1)}).
     */
    private static void blockClickLadder(GameTestHelper helper) {
        helper.assertTrue(!TileEntityGeiger.probabilisticClicks, "1.7's placed geiger clicks on every fifth tick");
        boolean saved = TileEntityGeiger.probabilisticClicks;
        try {
            TileEntityGeiger.probabilisticClicks = false;
            expectBlock(helper, 0.5F, Set.of(0, 1), 0.66, 0.673);    // [0, 0, 1]
            expectBlock(helper, 3.0F, Set.of(0, 1), 0.495, 0.505);   // [0, 1]
            expectBlock(helper, 5.0F, Set.of(1), 0.0, 0.0);          // "ticker > 5" is strict
            expectBlock(helper, 7.0F, Set.of(1, 2), 0.0, 0.0);
            expectBlock(helper, 10.0F, Set.of(2), 0.0, 0.0);
            expectBlock(helper, 12.0F, Set.of(2, 3), 0.0, 0.0);
            expectBlock(helper, 17.0F, Set.of(3, 4), 0.0, 0.0);
            expectBlock(helper, 22.0F, Set.of(4, 5), 0.0, 0.0);
            expectBlock(helper, 25.0F, Set.of(5), 0.0, 0.0);
            expectBlock(helper, 27.0F, Set.of(5, 6), 0.0, 0.0);
            expectBlock(helper, 35.0F, Set.of(6), 0.0, 0.0);
            expectBlock(helper, 0.0F, Set.of(0, 1), 0.976, 0.984);   // background geiger1, 1 in 50
            expectBlock(helper, 1E-5F, Set.of(0, 1), 0.66, 0.673);   // any dose above 0 takes the ladder

            // draw order: a background click consumes nextInt(50) and nextInt(1), a ladder click one nextInt
            RandomSource rand = RandomSource.create(4815162342L);
            RandomSource ref = RandomSource.create(4815162342L);
            for (int i = 0; i < 5_000; i++) {
                int r = TileEntityGeiger.click(0.0F, rand);
                int expected = 0;
                if (ref.nextInt(50) == 0) expected = 1 + ref.nextInt(1);
                helper.assertTrue(r == expected, "background draw " + i + ": " + r + ", 1.7 " + expected);
                r = TileEntityGeiger.click(12.0F, rand);
                expected = ref.nextInt(2) == 0 ? 2 : 3;
                helper.assertTrue(r == expected, "ladder draw " + i + ": " + r + ", 1.7 " + expected);
            }
        } finally {
            TileEntityGeiger.probabilisticClicks = saved;
        }
        helper.succeed();
    }

    /** hbm-space's gate on the placed counter: {@code rand.nextFloat() > ticker} skips the click, from the same random. */
    private static void blockClickGate(GameTestHelper helper) {
        boolean saved = TileEntityGeiger.probabilisticClicks;
        try {
            TileEntityGeiger.probabilisticClicks = true;
            expectBlock(helper, 0.5F, Set.of(0, 1), 0.828, 0.839);   // 1 - 0.5 / 3
            expectBlock(helper, 3.0F, Set.of(0, 1), 0.495, 0.505);
            expectBlock(helper, 27.0F, Set.of(5, 6), 0.0, 0.0);
            expectBlock(helper, 0.0F, Set.of(0, 1), 0.976, 0.984);   // background clicks are not gated
            RandomSource rand = RandomSource.create(99L);
            RandomSource ref = RandomSource.create(99L);
            for (int i = 0; i < 5_000; i++) {
                int r = TileEntityGeiger.click(0.5F, rand);
                int expected = ref.nextFloat() > 0.5F ? 0 : (ref.nextInt(3) == 2 ? 1 : 0);
                helper.assertTrue(r == expected, "gated draw " + i + ": " + r + ", hbm-space " + expected);
            }
        } finally {
            TileEntityGeiger.probabilisticClicks = saved;
        }
        helper.succeed();
    }

    private static void expectBlock(GameTestHelper helper, float dose, Set<Integer> allowed, double silentMin, double silentMax) {
        RandomSource rand = RandomSource.create(4815162342L);
        Set<Integer> seen = new TreeSet<>();
        int silent = 0;
        for (int i = 0; i < DRAWS; i++) {
            int r = TileEntityGeiger.click(dose, rand);
            seen.add(r);
            if (r == 0) silent++;
        }
        double share = silent / (double) DRAWS;
        String label = "block dose " + dose + " gate " + TileEntityGeiger.probabilisticClicks;
        helper.assertTrue(allowed.containsAll(seen) && seen.containsAll(allowed),
                label + ": sounds " + seen + ", 1.7 " + allowed);
        helper.assertTrue(share >= silentMin && share <= silentMax,
                label + ": silent share " + share + ", expected " + silentMin + ".." + silentMax);
    }

    /**
     * The placed counter samples the chunk at its block on the tenth tick of its counter (and not before), the ROR
     * {@code rad} value is that sample rounded up, and the comparator is 1.7's {@code min(ceil(rad / 5f), 15)}.
     * The ticks are driven by hand within one server tick, so neighbouring plots cannot change the chunk in between.
     */
    private static void blockSample(GameTestHelper helper) {
        int[][] ladder = {{0, 0}, {1, 1}, {5, 1}, {6, 2}, {50, 10}, {74, 15}, {75, 15}, {1000, 15}};
        for (int[] row : ladder) {
            helper.assertTrue(TileEntityGeiger.comparatorLevel(row[0]) == row[1],
                    "comparator at " + row[0] + " RAD/s: " + TileEntityGeiger.comparatorLevel(row[0]) + ", 1.7 " + row[1]);
        }
        helper.assertTrue(TileEntityGeiger.comparatorLevel(0.01F) == 1, "a trace of radiation already gives 1");
        helper.assertTrue(TileEntityGeiger.comparatorLevel(5.0001F) == 2, "just above 5 RAD/s gives 2");

        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, ModMachineBlocks.geiger.get().defaultBlockState());
        helper.runAfterDelay(1, () -> {
            BlockPos abs = helper.absolutePos(rel);
            var level = helper.getLevel();
            if (!(level.getBlockEntity(abs) instanceof TileEntityGeiger geiger)) {
                helper.fail("no geiger block entity");
                return;
            }
            double before = ChunkRadiationManager.proxy.getRadiation(level, abs);
            try {
                ChunkRadiationManager.proxy.setRadiation(level, abs, 12.3D);
                float live = geiger.check();
                helper.assertTrue(live > 0, "the chunk must read radiation after setting it: " + live);
                geiger.timer = 0;
                geiger.ticker = 0;
                var state = level.getBlockState(abs);
                for (int t = 1; t < 10; t++) {
                    TileEntityGeiger.serverTick(level, abs, state, geiger);
                    helper.assertTrue(geiger.ticker == 0 && geiger.timer == t, "no sample before the tenth tick (" + t + ")");
                }
                TileEntityGeiger.serverTick(level, abs, state, geiger);
                helper.assertTrue(geiger.timer == 0 && geiger.ticker == live,
                        "tenth tick samples the chunk: " + geiger.ticker + ", chunk " + live);
                helper.assertTrue(("" + (int) Math.ceil(live)).equals(geiger.provideRORValue("VAL:rad")),
                        "ROR rad " + geiger.provideRORValue("VAL:rad") + " for " + live);
            } finally {
                ChunkRadiationManager.proxy.setRadiation(level, abs, before);
            }
            helper.succeed();
        });
    }

    /**
     * 1.7's five lines: values truncated to one decimal and printed as Java doubles, the rad buffer (not the
     * armor-reduced dose) as environmental radiation, no "received radiation" line.
     */
    private static void readout(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        HbmLivingProps.setRadiation(player, 250.04);
        HbmLivingProps.setRadBuf(player, 3.27);
        BlockPos at = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        double rads = ((int) (ChunkRadiationManager.proxy.getRadiation(player.level(), at) * 10)) / 10D;

        List<Component> lines = ContaminationUtil.geigerData(player);
        helper.assertTrue(key(lines.get(0)) == null && lines.get(0).getString().startsWith("===== ☢ "), "title " + lines.get(0));
        expectLine(helper, lines, "geiger.chunkRad", " " + ContaminationUtil.getPreffixFromRad(rads) + rads + " RAD/s");
        expectLine(helper, lines, "geiger.envRad", " " + ChatFormatting.GOLD + "3.2 RAD/s");
        expectLine(helper, lines, "geiger.playerRad", " " + ChatFormatting.YELLOW + "250.0 RAD");
        expectLine(helper, lines, "geiger.playerRes", " " + ChatFormatting.WHITE + "0.0% (0.0)");
        helper.assertTrue(lines.stream().noneMatch(l -> "geiger.recievedRad".equals(key(l))), "1.7 has no received-radiation line");
        helper.assertTrue("geiger.playerRes".equals(key(lines.get(lines.size() - 1))), "resistance is the last line");
        helper.assertTrue(!ContaminationUtil.GEIGER_LINES.isEmpty() || lines.size() == 5, "plain HBM prints 1.7's five lines: " + lines.size());

        HbmLivingProps.setRadiation(player, 0);
        HbmLivingProps.setRadBuf(player, 0);
        helper.succeed();
    }

    private static String key(Component line) {
        return line.getContents() instanceof TranslatableContents t ? t.getKey() : null;
    }

    private static void expectLine(GameTestHelper helper, List<Component> lines, String key, String literal) {
        for (Component line : lines) {
            if (!key.equals(key(line))) continue;
            String text = line.getSiblings().isEmpty() ? "" : line.getSiblings().get(0).getString();
            helper.assertTrue(text.equals(literal), key + ": \"" + text + "\", 1.7 \"" + literal + "\"");
            return;
        }
        helper.fail(key + " missing from the readout");
    }
}
