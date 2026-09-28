package com.hbm.blocks.generic;

import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.items.ModItems;
import com.hbm.sound.ModDoorSounds;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.7 {@code BlockForgottenLock}: a forgotten brick with a lock in it. Its three metadata looks are flattened to
 * {@code brick_forgotten_lock}, {@code _bw} and {@code _nullstone} (1.7's meta 3, "the block that kills you", was
 * never listed and has no look of its own, so it is not registered).
 *
 * <p>The red key, or a cracked red key (which is used up), opens it from any side face: a 15-deep, 5x5 corridor
 * lined with forgotten bricks is cut into the wall behind the lock, capped at its far end, and the lock clicks
 * open. 1.7's comment calls the lock a red herring; the corridor leads nowhere, and that is kept.
 */
public class BlockForgottenLock extends CompatBlock {

    public BlockForgottenLock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        boolean cracked = stack.is(ModItems.key_red_cracked.get());
        Direction side = hit.getDirection();
        if ((!stack.is(ModItems.key_red.get()) && !cracked) || side.getAxis() == Direction.Axis.Y) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (cracked) stack.shrink(1);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        generate(level, pos, side);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModDoorSounds.LOCK_OPEN.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** 1.7 {@code generate}: the corridor runs from the lock away from the clicked face. */
    public static void generate(Level level, BlockPos pos, Direction dir) {
        // 1.7 dir.getRotation(UP); the width is symmetric, so only the axis matters
        Direction rot = dir.getCounterClockWise();
        int len = 15;
        for (int w = -2; w <= 2; w++) for (int h = -2; h <= 2; h++) for (int d = 0; d < len; d++) {
            boolean wall = w == -2 || w == 2 || h == -2 || h == 2 || d == len - 1;
            BlockPos at = new BlockPos(pos.getX() - dir.getStepX() * d + rot.getStepX() * w, pos.getY() + h,
                    pos.getZ() - dir.getStepZ() * d + rot.getStepZ() * w);
            level.setBlock(at, wall ? ModDungeonBlocks.brick_forgotten.get().defaultBlockState()
                    : Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
