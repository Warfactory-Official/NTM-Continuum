package com.hbm.blocks.generic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import dev.golem.core.world.CompatEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Helpers for blocks whose drops are computed in Java ({@code getDrops} overrides) rather than declared
 * as a loot table, following 1.7 {@code Block.getDrops}: the count comes from
 * {@code quantityDroppedWithBonus(fortune, rand)} and each of those items is a separate
 * {@code getItemDropped} roll.
 *
 * <p>1.7's ore curve ({@code BlockOre}, {@code BlockDepthOre}, {@code BlockSellafieldOre}) rolls the base
 * quantity and then <i>multiplies</i> it by {@code max(0, rand(fortune + 2) - 1) + 1}: the same formula
 * as vanilla's {@code ore_drops}. A flat {@code base + rand + fortune} is 1.12.2's {@code OreEnumUtil}, not 1.7.
 */
public final class OreDrops {

    private OreDrops() {}

    /** 1.7 {@code quantityDroppedWithBonus}'s multiplier: {@code max(rand(fortune + 2) - 1, 0) + 1}. */
    public static int fortuneMultiplier(int fortune, RandomSource rand) {
        return Math.max(rand.nextInt(Math.max(fortune, 0) + 2) - 1, 0) + 1;
    }

    /**
     * {@code count} separate rolls of {@code drop}, as 1.7's {@code getDrops} loop made them. Empty stacks
     * are left out.
     */
    public static List<ItemStack> roll(int count, Supplier<ItemStack> drop) {
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
