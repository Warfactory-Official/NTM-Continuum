package com.hbm.blocks.machine.rbmk;
import com.hbm.tileentity.ModRBMKBlockEntities; import com.hbm.tileentity.machine.rbmk.*; import com.hbm.blocks.multiblock.MultiblockSurface; import net.minecraft.core.BlockPos; import net.minecraft.world.WorldlyContainer; import net.minecraft.world.WorldlyContainerHolder; import net.minecraft.world.level.LevelAccessor; import org.jspecify.annotations.Nullable; import net.minecraft.world.level.block.entity.BlockEntityType; import net.minecraft.world.level.block.state.BlockState;
public class RBMKOutgasser extends RBMKBase implements WorldlyContainerHolder { public RBMKOutgasser(Properties properties) { super(properties); } @Override public int coreMask() { return MASK_DOWN; } @Override protected int topMask() { return MASK_UP; } @Override protected BlockEntityType<? extends BlockEntityRBMKBase> beType() { return ModRBMKBlockEntities.RBMK_OUTGASSER.get(); } @Override protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) { return new BlockEntityRBMKOutgasser(pos, state); }
    /** 1.7 proxies the core's inventory on the lid only. */
    @Override public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(PART) == Part.MIDDLE) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof WorldlyContainer column ? column : null;
    }
}
