package com.hbm.blocks.generic;

import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import dev.golem.core.world.CompatBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Nether phosphorus ore ({@code ore_nether_fire}) — the original-hbm (1.7.10) {@code BlockOre} case
 * {@code rand.nextInt(10) == 0 ? ingot_phosphorus : powder_fire}. 1.7 {@code getDrops} asks
 * {@code getItemDropped} once <i>per item</i>, so with Fortune each of the multiplied items is its own 1-in-10
 * phosphorus roll; a loot table can only pick one item for the whole stack, which is why this is Java.
 * Silk touch gives the ore (default {@code canSilkHarvest} on a plain cube).
 */
public class BlockNetherFireOre extends CompatBlock {

    public BlockNetherFireOre(Properties properties) {
        super(properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (OreDrops.silkTouch(params)) return List.of(new ItemStack(this));
        RandomSource rand = params.getLevel().getRandom();
        int count = OreDrops.fortuneMultiplier(OreDrops.fortune(params), rand);
        return OreDrops.roll(count, () -> new ItemStack(rand.nextInt(10) == 0
                ? ModItems.ingot_phosphorus.get() : ModFuelItems.powder_fire.get()));
    }
}
