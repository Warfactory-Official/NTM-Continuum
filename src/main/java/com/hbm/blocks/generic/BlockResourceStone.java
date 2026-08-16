package com.hbm.blocks.generic;

import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModIntermediateItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * A resource-bearing stone variant — the 1.12.2 {@code BlockResourceStone}, one of the six
 * {@code EnumStoneType} metas of the single {@code stone_resource} block. 26.1 has no metadata, so the
 * set is flattened into six registrations ({@code stone_resource_sulfur} … {@code _bauxite}) per the
 * port's {@code <base>_<variant>} convention.
 *
 * <p>Five of the six simply drop themselves. <b>Malachite is the exception</b> and yields
 * {@code chunk_ore[MALACHITE] × (3 + fortune + rand.nextInt(fortune + 2))} — a single stack, not a
 * per-item roll, exactly as upstream.
 */
public class BlockResourceStone extends dev.golem.core.world.CompatBlock {

    private final boolean malachite;

    public BlockResourceStone(Properties properties, boolean malachite) {
        super(properties);
        this.malachite = malachite;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!malachite) return List.of(new ItemStack(this));
        RandomSource rand = params.getLevel().getRandom();
        int fortune = OreDrops.fortune(params);
        int count = 3 + fortune + rand.nextInt(fortune + 2);
        return List.of(ModIntermediateItems.chunk(EnumChunkType.MALACHITE, count));
    }
}
