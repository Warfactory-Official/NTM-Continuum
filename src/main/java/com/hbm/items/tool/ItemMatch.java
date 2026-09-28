package com.hbm.items.tool;

import dev.golem.core.world.CompatItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.7 {@code ItemMatch}: a single-use flint and steel. It lights plain fire in the air block in front of the
 * clicked face and is used up (outside creative) whether or not that block was air.
 */
public class ItemMatch extends CompatItem {

    public ItemMatch(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;

        if (level.getBlockState(pos).isAir()) {
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                    level.getRandom().nextFloat() * 0.4F + 0.8F);
            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
        }
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
