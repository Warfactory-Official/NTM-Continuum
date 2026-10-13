package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockGasBase;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.items.tool.ItemDosimeter;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * 1.7 {@code ItemDosimeter.onUpdate} click choice and 1.7 {@code BlockGasBase.randomDisplayTick} ash-glasses gas
 * cloud gate and colours, plus the two NTM: Space switches over them (ADDON-API-INVENTORY V-186, V-187).
 */
public final class DosimeterGasGameTests {

    private DosimeterGasGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CLICK_LADDER =
            TESTS.function("dosimeter_click_ladder", 20, () -> DosimeterGasGameTests::clickLadder);
    public static final GameTests.Function CLICK_GATE =
            TESTS.function("dosimeter_click_gate", 20, () -> DosimeterGasGameTests::clickGate);
    public static final GameTests.Function GAS_GLASSES =
            TESTS.function("gas_ash_glasses_gate", 20, () -> DosimeterGasGameTests::glassesGate);
    public static final GameTests.Function GAS_COLOURS =
            TESTS.function("gas_ash_glasses_colours", 20, () -> DosimeterGasGameTests::colours);

    public static void init() {}

    private static final int DRAWS = 100_000;

    /** The sound numbers a dose can choose (1.7's lists; 0 = silent) and the share of silent click ticks. */
    private static void clickLadder(GameTestHelper helper) {
        boolean saved = ItemDosimeter.probabilisticClicks;
        try {
            ItemDosimeter.probabilisticClicks = false;
            expect(helper, 0.3F, Set.of(0, 1), 0.45, 0.55);   // [0, 1]
            expect(helper, 0.7F, Set.of(1, 2), 0.0, 0.0);     // [1, 2]
            expect(helper, 1.5F, Set.of(2), 0.0, 0.0);        // [2]
            expect(helper, 3.0F, Set.of(3), 0.0, 0.0);        // [3] (1.7's "x >= 1 && x >= 2")
            expect(helper, 2.0F, Set.of(3), 0.0, 0.0);
            // background: geiger1 one click tick in 100
            expect(helper, 0.0F, Set.of(0, 1), 0.985, 0.995);
            expect(helper, 1E-6F, Set.of(0, 1), 0.985, 0.995);
        } finally {
            ItemDosimeter.probabilisticClicks = saved;
        }
        helper.succeed();
    }

    /** hbm-space: below a dose of 1 a click tick goes ahead only with probability = dose. */
    private static void clickGate(GameTestHelper helper) {
        boolean saved = ItemDosimeter.probabilisticClicks;
        try {
            ItemDosimeter.probabilisticClicks = true;
            expect(helper, 0.2F, Set.of(0, 1), 0.88, 0.92);   // 1 - 0.2 * 0.5
            expect(helper, 0.7F, Set.of(0, 1, 2), 0.27, 0.33); // 1 - 0.7
            expect(helper, 1.5F, Set.of(2), 0.0, 0.0);        // nextFloat() > 1.5 never
            expect(helper, 0.0F, Set.of(0, 1), 0.985, 0.995); // background clicks are not gated
        } finally {
            ItemDosimeter.probabilisticClicks = saved;
        }
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, float dose, Set<Integer> allowed, double silentMin, double silentMax) {
        RandomSource rand = RandomSource.create(4815162342L);
        RandomSource worldRand = RandomSource.create(1234L);
        Set<Integer> seen = new TreeSet<>();
        int silent = 0;
        for (int i = 0; i < DRAWS; i++) {
            int r = ItemDosimeter.click(dose, rand, worldRand);
            seen.add(r);
            if (r == 0) silent++;
        }
        double share = silent / (double) DRAWS;
        String label = "dose " + dose + " gate " + ItemDosimeter.probabilisticClicks;
        helper.assertTrue(allowed.containsAll(seen) && seen.containsAll(allowed),
                label + ": sounds " + seen + ", 1.7 " + allowed);
        helper.assertTrue(share >= silentMin && share <= silentMax,
                label + ": silent share " + share + ", expected " + silentMin + ".." + silentMax);
    }

    /** No glasses: nothing, no draw. Glasses: every display tick (1.7), or 1 in 9 (hbm-space). */
    private static void glassesGate(GameTestHelper helper) {
        int saved = BlockGasBase.ashGlassesParticleChance;
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        try {
            BlockGasBase.ashGlassesParticleChance = 1;
            RandomSource a = RandomSource.create(7L);
            RandomSource b = RandomSource.create(7L);
            helper.assertTrue(!BlockGasBase.revealsGas(null, a), "no viewer, no cloud");
            helper.assertTrue(!BlockGasBase.revealsGas(player, a), "bare head, no cloud");
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            helper.assertTrue(!BlockGasBase.revealsGas(player, a), "an iron helmet shows no gas");
            helper.assertTrue(a.nextLong() == b.nextLong(), "the gate draws nothing without the glasses");

            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModArmorSetBItems.ashglasses.get()));
            int shown = 0;
            for (int i = 0; i < 1000; i++) if (BlockGasBase.revealsGas(player, a)) shown++;
            helper.assertTrue(shown == 1000, "1.7: the glasses show a cloud on every display tick: " + shown);
            helper.assertTrue(a.nextLong() == b.nextLong(), "1.7 draws nothing for the cloud gate");

            BlockGasBase.ashGlassesParticleChance = 9;
            shown = 0;
            for (int i = 0; i < 90_000; i++) if (BlockGasBase.revealsGas(player, a)) shown++;
            helper.assertTrue(shown > 9_500 && shown < 10_500, "hbm-space: one display tick in 9: " + shown);
        } finally {
            BlockGasBase.ashGlassesParticleChance = saved;
        }
        helper.succeed();
    }

    /** The 1.7 {@code BlockGasBase(r, g, b)} colours of every gas. */
    private static void colours(GameTestHelper helper) {
        colour(helper, ModNuclearBlocks.gas_asbestos.get(), 0.6F, 0.6F, 0.5F);
        colour(helper, ModTerrainBlocks.chlorine_gas.get(), 0.7F, 0.8F, 0.6F);
        colour(helper, ModTerrainBlocks.gas_coal.get(), 0.2F, 0.2F, 0.2F);
        colour(helper, ModOreBlocks.gas_flammable.get(), 0.8F, 0.8F, 0.2F);
        colour(helper, ModOreBlocks.gas_explosive.get(), 0.8F, 0.8F, 0.2F);
        colour(helper, ModNuclearBlocks.gas_meltdown.get(), 0.1F, 0.4F, 0.1F);
        colour(helper, ModNuclearBlocks.gas_monoxide.get(), 0.1F, 0.1F, 0.1F);
        colour(helper, ModNuclearBlocks.gas_radon.get(), 0.1F, 0.8F, 0.1F);
        colour(helper, ModNuclearBlocks.gas_radon_dense.get(), 0.1F, 0.5F, 0.1F);
        colour(helper, ModNuclearBlocks.gas_radon_tomb.get(), 0.1F, 0.3F, 0.1F);
        helper.succeed();
    }

    private static void colour(GameTestHelper helper, Block block, float r, float g, float b) {
        float[] rgb = BlockGasBase.cloudColor(block);
        helper.assertTrue(rgb != null && Arrays.equals(rgb, new float[] {r, g, b}),
                block + " cloud colour " + Arrays.toString(rgb) + ", 1.7 " + r + "/" + g + "/" + b);
    }
}
