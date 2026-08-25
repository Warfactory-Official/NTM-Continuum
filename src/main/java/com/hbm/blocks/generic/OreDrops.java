package com.hbm.blocks.generic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import dev.golem.core.world.CompatEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The 1.12.2 {@code OreEnumUtil} drop model, ported verbatim: a <b>quantity function</b>
 * {@code (fortune, rand) -> count} plus a <b>drop function</b> {@code rand -> ItemStack}, rolled
 * independently {@code count} times.
 *
 * <p><b>Why this is Java and not a loot table.</b> The old quantity functions add the fortune level
 * <i>flatly</i> ({@code 1 + rand.nextInt(2) + fortune}), and vanilla's loot functions cannot express
 * that: {@code uniform_bonus_count} adds {@code rand(level*mult + 1)} and {@code ore_drops} multiplies
 * by {@code max(1, rand(level + 2))}. Wave 2b accepted {@code uniform_bonus_count} as an idiomatic
 * approximation for the 39 ores it generated; the blocks added for the ore-worldgen wave use this
 * class instead so their yields are exact. (Retrofitting the Wave-2b ores onto it is a separate,
 * behaviour-changing job — see IMPLEMENTATION.md §20.)
 *
 * <p>A block using this overrides {@code getDrops} and returns {@link #roll}, which bypasses the loot
 * table entirely. Such blocks still need a loot-table JSON only if something else references it; the
 * block itself never consults one.
 */
public final class OreDrops {

    private OreDrops() {}

    /** {@code (state, fortune, rand) -> count} from the old {@code OreEnumUtil}. */
    @FunctionalInterface
    public interface Quantity {
        int apply(int fortune, RandomSource rand);
    }

    // --- the old quantity functions, verbatim ---------------------------------------------------

    public static int base2Rand3Fortune(int fortune, RandomSource rand) { return 2 + rand.nextInt(3) + fortune; }
    public static int base2Rand2Fortune(int fortune, RandomSource rand) { return 2 + rand.nextInt(2) + fortune; }
    public static int base1Rand2Fortune(int fortune, RandomSource rand) { return 1 + rand.nextInt(2) + fortune; }
    /** Fortune deliberately ignored upstream. */
    public static int base1Rand3(int fortune, RandomSource rand) { return 1 + rand.nextInt(3); }
    public static int const1(int fortune, RandomSource rand) { return 1; }
    /** {@code 1 + (fortune <= 0 ? 0 : rand.nextInt(fortune))} — note {@code nextInt(fortune)}, not {@code fortune+1}. */
    public static int vanillaFortune(int fortune, RandomSource rand) {
        return 1 + (fortune <= 0 ? 0 : rand.nextInt(fortune));
    }
    /** {@code 4 + rand.nextInt(6)} — fortune ignored. */
    public static int cobaltAmount(int fortune, RandomSource rand) { return 4 + rand.nextInt(6); }
    /** {@code 5 + rand.nextInt(8)} — fortune ignored. */
    public static int cobaltNetherAmount(int fortune, RandomSource rand) { return 5 + rand.nextInt(8); }
    /** {@code min(1 + rand.nextInt(2) + fortune, 2)} — capped at 2 however high fortune goes. */
    public static int alexandriteAmount(int fortune, RandomSource rand) {
        return Math.min(1 + rand.nextInt(2) + fortune, 2);
    }

    // --- rolling ---------------------------------------------------------------------------------

    /**
     * Roll {@code quantity(fortune, rand)} independent stacks of {@code drop}. Empty stacks are
     * dropped from the result, matching the old {@code getDrops} loop.
     */
    public static List<ItemStack> roll(LootParams.Builder params, Quantity quantity, Supplier<ItemStack> drop) {
        RandomSource rand = params.getLevel().getRandom();
        int count = quantity.apply(fortune(params), rand);
        List<ItemStack> drops = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            ItemStack stack = drop.get();
            if (!stack.isEmpty()) drops.add(stack);
        }
        return drops;
    }

    /** The Fortune level on the harvesting tool, or 0 when there is no tool in the loot context. */
    public static int fortune(LootParams.Builder params) {
        return CompatEnchantments.fortune(params);
    }

    /** Whether the harvesting tool carries Silk Touch. */
    public static boolean silkTouch(LootParams.Builder params) {
        return CompatEnchantments.silkTouch(params);
    }
}
