package com.hbm.blocks.generic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.function.Supplier;

/**
 * Depth rock carrying an ore — the 1.12.2 {@code BlockDepthOre}. Identical to {@link BlockDepth}
 * (unbreakable, explosion resistance 10) except that it yields a material instead of itself, using
 * the old {@code OreEnum} quantity/drop function pair (see {@link OreDrops}).
 *
 * <p>Upstream grants <b>no experience</b>: {@code BlockDepth} extends plain {@code Block}, not
 * {@code BlockOre}, so it never overrode {@code getExpDrop}. Preserved.
 *
 * <p>{@code ore_depth_borax} passes a {@code null} OreEnum upstream, which falls through to the
 * vanilla "drop the block item" path — here that is expressed by giving it {@link #dropSelf}.
 */
public class BlockDepthOre extends BlockDepth {

    private final OreDrops.Quantity quantity;
    private final Supplier<ItemStack> drop;
    private final boolean dropSelf;

    public BlockDepthOre(Properties properties, OreDrops.Quantity quantity, Supplier<ItemStack> drop) {
        super(properties);
        this.quantity = quantity;
        this.drop = drop;
        this.dropSelf = false;
    }

    /** The upstream {@code oreEnum == null} case: one of the block itself, no fortune scaling. */
    public BlockDepthOre(Properties properties) {
        super(properties);
        this.quantity = OreDrops::const1;
        this.drop = null;
        this.dropSelf = true;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return OreDrops.roll(params, quantity, dropSelf ? () -> new ItemStack(this) : drop);
    }
}
