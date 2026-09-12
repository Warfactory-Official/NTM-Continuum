package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.smoke.SmokeChainRegistration;
import com.hbm.tileentity.machine.TileEntityAshpit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The 3x3 ashpit shell; all cells expose the core's output inventory to hoppers. */
public final class MachineAshpit extends BlockDummyable implements WorldlyContainerHolder {

    public MachineAshpit(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAshpit(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        // The old standardOpenBehavior deliberately made shift-right-click a no-op.
        return player.isShiftKeyDown() ? InteractionResult.SUCCESS : super.onUseWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityAshpit ashpit ? ashpit : null;
    }

}
