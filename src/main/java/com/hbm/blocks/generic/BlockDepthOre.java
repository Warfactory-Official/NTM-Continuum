package com.hbm.blocks.generic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.function.Supplier;

/**
 * Depth rock carrying an ore — original-hbm (1.7.10) {@code BlockDepthOre}. Identical to {@link BlockDepth}
 * (unbreakable except by a depth-rock tool, explosion resistance 10) except for what it drops.
 *
 * <p>1.7 {@code quantityDropped} is {@code base + rand(spread)} (cinnabar 2-4, zirconium and neodymium 2-3,
 * everything else 1), and {@code quantityDroppedWithBonus} multiplies it by
 * {@link OreDrops#fortuneMultiplier} for <i>every</i> depth ore and cluster, capping alexandrite at 2.
 * {@code BlockDepth} is a plain opaque cube without a tile entity, so 1.7's default {@code canSilkHarvest}
 * holds and silk touch gives the block. ({@code ore_depth_borax} drops {@code powder_borax} in 1.7; the
 * 1.12.2 "null OreEnum, drops itself" path is not 1.7.)
 *
 * <p>No experience: {@code BlockDepth} extends plain {@code Block}, not {@code BlockOre}.
 */
public class BlockDepthOre extends BlockDepth {

    private final int base;
    private final int spread;
    private final int cap;
    private final boolean fortune;
    private final Supplier<ItemStack> drop;

    /**
     * @param base   the guaranteed count
     * @param spread {@code rand.nextInt(spread)} is added when positive
     * @param cap    the 1.7 alexandrite clamp, or {@code Integer.MAX_VALUE}
     */
    public BlockDepthOre(Properties properties, int base, int spread, int cap, boolean fortune, Supplier<ItemStack> drop) {
        super(properties);
        this.base = base;
        this.spread = spread;
        this.cap = cap;
        this.fortune = fortune;
        this.drop = drop;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (OreDrops.silkTouch(params)) return List.of(new ItemStack(this));
        RandomSource rand = params.getLevel().getRandom();
        int quantity = base + (spread > 0 ? rand.nextInt(spread) : 0);
        if (fortune) quantity *= OreDrops.fortuneMultiplier(OreDrops.fortune(params), rand);
        return OreDrops.roll(Math.min(quantity, cap), drop);
    }
}
