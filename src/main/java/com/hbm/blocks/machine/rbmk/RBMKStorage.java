package com.hbm.blocks.machine.rbmk;
import com.hbm.tileentity.ModRBMKBlockEntities; import com.hbm.tileentity.machine.rbmk.*; import com.hbm.blocks.multiblock.MultiblockSurface; import net.minecraft.core.BlockPos; import net.minecraft.world.WorldlyContainer; import net.minecraft.world.WorldlyContainerHolder; import net.minecraft.world.level.LevelAccessor; import org.jspecify.annotations.Nullable; import net.minecraft.world.level.block.entity.BlockEntityType; import net.minecraft.world.level.block.state.BlockState;
public class RBMKStorage extends RBMKBase implements WorldlyContainerHolder { public RBMKStorage(Properties properties) { super(properties); } @Override protected BlockEntityType<? extends BlockEntityRBMKBase> beType() { return ModRBMKBlockEntities.RBMK_STORAGE.get(); } @Override protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) { return new BlockEntityRBMKStorage(pos, state); }
    /** 1.7 proxies the core's inventory on every block of the column. */
    @Override public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof WorldlyContainer column ? column : null;
    }
}
