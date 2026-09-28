package com.hbm.items.tool;

import com.hbm.blocks.bomb.ModBombBlocks;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemBalefireMatch} (balefire and steel): a 256-use flint and steel that places balefire. Unlike the
 * matchstick it runs on both sides and wears by one on every click, even when the target was not air.
 */
public class ItemBalefireMatch extends CompatItem {

    public ItemBalefireMatch(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;

        if (level.getBlockState(pos).isAir()) {
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                    level.getRandom().nextFloat() * 0.4F + 0.8F);
            if (!level.isClientSide()) level.setBlock(pos, ModBombBlocks.balefire.get().defaultBlockState(), 3);
        }
        if (!level.isClientSide() && player != null) ItemStacks.hurtAndBreak(stack, 1, player, context.getHand());
        return InteractionResult.SUCCESS;
    }
}
