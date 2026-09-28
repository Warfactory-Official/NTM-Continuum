package com.hbm.items.special;

import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.tool.UtilityToolScreens;
import dev.golem.core.world.CompatItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemClayTablet}: the first read draws a seed ({@code tabletSeed}) from the reader's random, and the
 * tablet then always shows the same pedestal recipe of its set, partly worn away ({@code GUIScreenClayTablet}).
 * The set is the 1.7 metadata, {@code item_variant} here (0 for the red-room tablet, 1 for the dungeon one).
 */
public class ItemClayTablet extends CompatItem {

    public ItemClayTablet(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide() && !ModUtilityToolItems.TABLET_SEED.has(stack)) {
            ModUtilityToolItems.TABLET_SEED.set(stack, player.getRandom().nextLong());
        }
        if (world.isClientSide()) UtilityToolScreens.open(UtilityToolScreens.Kind.CLAY_TABLET, stack);
        return InteractionResult.SUCCESS;
    }
}
