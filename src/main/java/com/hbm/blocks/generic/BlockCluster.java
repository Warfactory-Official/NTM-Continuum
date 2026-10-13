package com.hbm.blocks.generic;

import dev.golem.core.world.CompatDropExperienceBlock;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.function.Supplier;

/**
 * Ore cluster, from 1.7 {@code BlockCluster}. A rich, blast-resistant (35) ore that yields
 * exactly <b>one</b> crystal: {@code getItemDropped} names the crystal and the default
 * {@code quantityDropped} of 1 is never raised by Fortune (plain {@code Block}). It is an ordinary opaque
 * cube without a tile entity, so 1.7's default {@code canSilkHarvest} lets Silk Touch return the block.
 * (1.12.2's {@code harvestBlock} override, which ignored silk touch, is not 1.7.)
 *
 * <p>Experience: none. 1.7 {@code BlockCluster} is a plain {@code Block} without {@code getExpDrop}. (1.12.2's
 * {@code BlockNTMOre} paid the clobbered static {@code xp} of 2.)
 *
 * <p><b>Deferred:</b> {@code IDrillInteraction} (mining drills of rating &ge; 30 extract the crystal
 * without breaking the block). No such drill is ported.
 */
public class BlockCluster extends CompatDropExperienceBlock {

    private final Supplier<ItemStack> drop;

    public BlockCluster(Properties properties, Supplier<ItemStack> drop) {
        super(ConstantInt.of(0), properties); // 1.7 plain Block: no experience
        this.drop = drop;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (OreDrops.silkTouch(params)) return List.of(new ItemStack(this));
        ItemStack stack = drop.get();
        return stack.isEmpty() ? List.of() : List.of(stack);
    }
}
