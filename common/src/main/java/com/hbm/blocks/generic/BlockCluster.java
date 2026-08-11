package com.hbm.blocks.generic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.function.Supplier;

/**
 * Ore cluster — the 1.12.2 {@code BlockCluster}. A rich, blast-resistant (35) ore that yields exactly
 * <b>one</b> crystal however it is mined.
 *
 * <p>Two upstream details that look like bugs but are load-bearing, both preserved:
 * <ul>
 *   <li>{@code getItemDropped} returns AIR and {@code harvestBlock} is overridden to spawn a single
 *       {@code getDrop(...)} item directly — so the {@code OreEnum}'s {@code vanillaFortune} quantity
 *       function is <b>never consulted</b> and Fortune does nothing. One crystal, always.</li>
 *   <li>Because that override ignores the silk-touch branch entirely, <b>Silk Touch does not return
 *       the block</b>; it still gives one crystal.</li>
 * </ul>
 *
 * <p>Experience: {@code BlockNTMOre.getExpDrop} pays out when the block drops something other than
 * itself, which is always true here. The amount is the infamous {@code static int xp} field, which
 * every ore constructor overwrote — it settles at 2 by the last registration, so clusters give 2.
 * (Same reasoning as the Wave-2b ore xp decision, IMPLEMENTATION.md §20.)
 *
 * <p><b>Deferred:</b> {@code IDrillInteraction} (mining drills of rating &ge; 30 extract the crystal
 * without breaking the block) — no such drill is ported.
 */
public class BlockCluster extends net.minecraft.world.level.block.DropExperienceBlock {

    private final Supplier<ItemStack> drop;

    public BlockCluster(Properties properties, Supplier<ItemStack> drop) {
        super(net.minecraft.util.valueproviders.ConstantInt.of(2), properties);
        this.drop = drop;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = drop.get();
        return stack.isEmpty() ? List.of() : List.of(stack);
    }
}
