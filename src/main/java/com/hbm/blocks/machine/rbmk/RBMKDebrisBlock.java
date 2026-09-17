package com.hbm.blocks.machine.rbmk;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDebris;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Keeps wreck gameplay in the block while a shared BER draws its exact OBJ mesh. */
public class RBMKDebrisBlock extends CompatBlock implements TickingBlock {
    public RBMKDebrisBlock(Properties properties) { super(properties); }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKDebris(pos, state);
    }
}
