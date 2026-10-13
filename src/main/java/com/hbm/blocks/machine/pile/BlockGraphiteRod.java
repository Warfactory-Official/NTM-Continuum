package com.hbm.blocks.machine.pile;

import com.hbm.items.ModNuclearMiscItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Pile Control Rod (LEGACY): a boron rod; {@code OUT} = withdrawn (neutrons pass), else absorbing. */
@Deprecated
public class BlockGraphiteRod extends BlockGraphiteDrilledBase {

    public BlockGraphiteRod(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggle(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        return toggle(state, level, pos, player, hit);
    }

    /** Click the channel face: toggles this rod and every identically-set rod in line with it. */
    private InteractionResult toggle(BlockState oldState, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Direction side = hit.getDirection();
        if (!isChannelFace(oldState, side)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockState newState = oldState.setValue(OUT, !oldState.getValue(OUT)); // toggle bit #4
        level.setBlock(pos, newState, 3);

        // 1.7: pitch 0.75 when the old meta had no bit 8 (pureMeta == oldMeta & 11), 0.65 otherwise
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, !oldState.getValue(OUT) ? 0.75F : 0.65F);

        for (int i = -1; i <= 1; i += 1) {
            BlockPos ipos = pos.relative(side, i);
            while (level.getBlockState(ipos).getBlock() == this && level.getBlockState(ipos) == oldState) {
                level.setBlock(ipos, newState, 3);
                ipos = ipos.relative(side, i);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemStack getInsertedItem() {
        return new ItemStack(ModNuclearMiscItems.PILE_ROD_BORON.get());
    }
}
