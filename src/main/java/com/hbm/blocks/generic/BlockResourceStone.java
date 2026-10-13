package com.hbm.blocks.generic;

import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModIntermediateItems;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * A resource-bearing stone variant, the 1.12.2 {@code BlockResourceStone}, one of the six
 * {@code EnumStoneType} metas of the single {@code stone_resource} block. 26.1 has no metadata, so the
 * set is flattened into six registrations ({@code stone_resource_sulfur} … {@code _bauxite}) per the
 * port's {@code <base>_<variant>} convention.
 *
 * <p>Five of the six drop themselves. <b>Malachite is the exception</b> and yields
 * {@code chunk_ore[MALACHITE] × (3 + fortune + rand.nextInt(fortune + 2))}, a single stack, not a
 * per-item roll, exactly as upstream.
 */
public class BlockResourceStone extends CompatBlock {

    private final boolean malachite;
    private final boolean asbestos;

    public BlockResourceStone(Properties properties, String variant) {
        super(properties);
        this.malachite = "malachite".equals(variant);
        this.asbestos = "asbestos".equals(variant);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // 1.7 harvestBlock takes the silk-touch branch before getDrops, so silk touch keeps the malachite block.
        boolean silk = OreDrops.silkTouch(params);
        if (asbestos && !silk) releaseAsbestos(params);
        if (!malachite || silk) return List.of(new ItemStack(this));
        RandomSource rand = params.getLevel().getRandom();
        int fortune = OreDrops.fortune(params);
        int count = 3 + fortune + rand.nextInt(fortune + 2);
        return List.of(ModIntermediateItems.chunk(EnumChunkType.MALACHITE, count));
    }

    /**
     * 1.7 {@code dropBlockAsItemWithChance}: breaking the asbestos meta leaves an asbestos gas cloud where it
     * stood (that method is skipped by the silk-touch branch of {@code harvestBlock}, and an explosion clears
     * the gas again right after). Here it runs from the drop computation, which is called after the block
     * was removed, so it only fills the space if it is still empty.
     */
    private static void releaseAsbestos(LootParams.Builder params) {
        Vec3 origin = params.getOptionalParameter(
                LootContextParams.ORIGIN);
        if (origin == null) return;
        BlockPos pos = BlockPos.containing(origin);
        if (params.getLevel().getBlockState(pos).isAir()) {
            params.getLevel().setBlock(pos, ModNuclearBlocks.gas_asbestos.get().defaultBlockState(), 3);
        }
    }
}
