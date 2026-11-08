package com.hbm.tileentity.deco;

import com.hbm.blocks.generic.BlockBobble;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import com.hbm.world.gen.nbt.INBTTileEntityTransformable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 {@code BlockBobble.TileEntityBobble}: holds the figure type; never ticks. */
public class TileEntityBobble extends TileEntityTrinket implements INBTTileEntityTransformable {

    public TileEntityBobble(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.BOBBLE.get(), pos, state);
    }

    @Override
    protected Enum<?>[] typeValues() {
        return BlockBobble.BobbleType.VALUES;
    }

    public BlockBobble.BobbleType type() {
        return BlockBobble.BobbleType.of(typeOrdinal());
    }

    /** 1.7: a structure's bobblehead is any figure but NONE, whatever its template saved. */
    @Override
    public void transformTE(LevelAccessor level, int coordBaseMode) {
        type = level.getRandom().nextInt(typeCount() - 1) + 1;
    }
}
