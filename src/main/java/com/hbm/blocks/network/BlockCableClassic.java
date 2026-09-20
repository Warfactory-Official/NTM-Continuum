package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.TileEntityCableClassic;

/**
 * {@code red_cable_classic}: the red cable with its old flat look. 1.7 drew it with {@code RenderCableClassic}
 * (a 5.5..10.5 core and arms off one atlas tile); here that is a multipart blockstate over the same six
 * connection flags {@link BlockCable} keeps, so the block model does the drawing and no renderer is needed.
 */
public class BlockCableClassic extends BlockCable {

    public BlockCableClassic(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCableClassic(pos, state);
    }
}
