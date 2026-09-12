package com.hbm.blocks.generic;

import com.hbm.tileentity.deco.TileEntityDeco;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A {@link BlockDeco} whose visual is a 3D OBJ model drawn by {@link com.hbm.client.render.DecoObjRenderer}
 * (boxcar / boat / the satellite dishes). Renders {@link RenderShape#INVISIBLE} and carries a
 * {@link TileEntityDeco} so the block-entity renderer runs — the same invisible-block + BER pattern the
 * machines and the Fat Man use. Collision/facing come from {@link BlockDeco} (these all use {@code FULL}).
 */
public class BlockDecoObj extends BlockDeco implements TickingBlock {

    public BlockDecoObj(Properties properties, Shape shape) {
        super(properties, shape);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDeco(pos, state);
    }
}
