package com.hbm.blocks.machine;

import com.hbm.interfaces.IBomb;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.ModDoorBlocks;
import com.hbm.tileentity.machine.TileEntityBlastDoor;
import com.hbm.tileentity.machine.TileEntityBlastDoorDummy;
import com.hbm.tileentity.machine.TileEntityLockableBase;

/**
 * Port of 1.7.10 {@code DummyBlockBlast}: the invisible leaf blocks of the old blast door. Breaking
 * one breaks the door (unless the door itself is retracting it), and clicking or detonating one acts
 * on the door it points at.
 */
public class DummyBlockBlast extends CompatBlock implements TickingBlock, IBomb {

    public static boolean safeBreak = false;

    public DummyBlockBlast(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBlastDoorDummy(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** 1.7 {@code getItem}: pick-block on a leaf gives the blast door (the cell drops nothing). */
    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModDoorBlocks.BLAST_DOOR.get());
    }

    private static @Nullable TileEntityBlastDoor doorOf(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TileEntityBlastDoorDummy dummy
                && level.getBlockEntity(dummy.target) instanceof TileEntityBlastDoor door) return door;
        return null;
    }

    // Breaking a leaf block breaks its door: that runs from the dummy block entity's removal hook
    // (TileEntityBlastDoorDummy.preRemoveSideEffects), which is the one place both eras still have
    // the block entity in hand; onBlockRemoved runs after it is gone on 26.1.

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else if (TileEntityLockableBase.LOCK_TOOLS.test(player.getMainHandItem())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        } else if (!player.isShiftKeyDown()) {
            TileEntityBlastDoor door = doorOf(level, pos);
            if (door != null && door.canAccess(player)) door.tryToggle();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!level.isClientSide()) {
            TileEntityBlastDoor door = doorOf(level, pos);
            if (door != null && !door.isLocked()) {
                door.tryToggle();
                return BombReturnCode.TRIGGERED;
            }
            return BombReturnCode.ERROR_INCOMPATIBLE;
        }
        return BombReturnCode.UNDEFINED;
    }
}
