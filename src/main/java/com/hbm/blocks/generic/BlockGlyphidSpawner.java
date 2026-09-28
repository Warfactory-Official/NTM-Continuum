package com.hbm.blocks.generic;

import com.hbm.items.ModItems;
import com.hbm.items.block.ItemBlockDecoVariant;
import com.hbm.tileentity.deco.TileEntityGlyphidSpawner;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * 1.7 {@code BlockGlyphidSpawner}: the hive's egg cluster. Its block entity spawns a glyphid swarm on its first
 * tick and then every {@code MobConfig.swarmCooldown}. Metadata 0/1/2 ({@link BlockGlyphid#TYPE}) makes the swarm
 * normal, infested or radioactive. Broken, it drops one to three glyphid eggs (1.7 {@code 1 + nextInt(3) +
 * fortune}; the port's block drop hook has no fortune level, so fortune adds nothing here).
 */
public class BlockGlyphidSpawner extends CompatBlock implements TickingBlock {

    private static final Random RAND = new Random();

    public BlockGlyphidSpawner(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockGlyphid.TYPE, BlockGlyphid.HiveType.NORMAL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockGlyphid.TYPE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityGlyphidSpawner(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockGlyphid.TYPE,
                BlockGlyphid.HiveType.of(BlockGlyphid.variantOf(context.getItemInHand())));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(ModItems.egg_glyphid.get(), 1 + RAND.nextInt(3)));
    }

    private ItemStack stackFor(BlockState state) {
        return asItem() instanceof ItemBlockDecoVariant item
                ? item.of(state.getValue(BlockGlyphid.TYPE).ordinal()) : new ItemStack(this);
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return stackFor(state);
    }

    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return stackFor(state);
    }
}
