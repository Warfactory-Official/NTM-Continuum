package com.hbm.blocks.generic;

import com.hbm.tileentity.deco.TileEntityReeds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code BlockReeds} ({@code plant_reeds}): a reed that stands on water and drops a stick. The old
 * {@code RenderReeds} drew crossed squares down through the whole water column below the block, measured every
 * frame. The block model draws the top segment; {@link TileEntityReeds} carries
 * {@link com.hbm.client.render.RenderReeds}, which draws the rest of the column at any depth (a block model can
 * reach one block below its cell at most, which is why the earlier depth-1..3 blockstate stopped short).
 */
public class BlockReeds extends BlockTerrainPlant implements EntityBlock {

    public BlockReeds(Properties properties) {
        super(properties);
    }

    private static boolean isWater(BlockState state) {
        return state.is(Blocks.WATER);
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        return isWater(level.getBlockState(pos.below()));
    }

    /** 1.7 {@code checkAndDropBlock} only sets air. */
    @Override
    protected boolean dropsWhenUnsupported(BlockState state) {
        return false;
    }

    /**
     * 1.7 {@code RenderReeds}' {@code depth}: walking down from the block below, {@code depth = y - i} is set
     * before the water test, so it counts every water block plus one (the reed itself). The first non-water
     * block ends the walk; the old loop also stopped above y = 0, here the level's floor.
     */
    public static int columnDepth(BlockGetter level, BlockPos pos) {
        int depth = 0;
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int y = pos.getY() - 1; y > level.getMinY(); y--) {
            depth = pos.getY() - y;
            if (!isWater(level.getBlockState(cursor.setY(y)))) break;
        }
        return Math.max(1, depth);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!isWater(level.getBlockState(pos.below()))) return null;
        return defaultBlockState();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityReeds(pos, state);
    }

    /** 1.7 {@code getItemDropped -> Items.stick}. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Items.STICK));
    }
}
