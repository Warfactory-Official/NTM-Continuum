package com.hbm.items.special;

import com.hbm.items.LegacyMeta;
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
 * The set is the 1.7 metadata: {@code clay_tablet} (0, the red-room tablet) and {@code clay_tablet_1} (1, the
 * dungeon one), two items, as in ntm-next.
 */
public class ItemClayTablet extends CompatItem implements LegacyMeta {

    private final int set;

    public ItemClayTablet(Properties properties, int set) {
        super(properties);
        this.set = set;
    }

    /** The tablet's set: 0 red room, 1 dungeon (1.7 metadata). */
    public int set() {
        return set;
    }

    @Override
    public int legacyMeta() {
        return set;
    }

    /** The tablet of 1.7 metadata {@code set}. */
    public static ItemStack of(int set) {
        return new ItemStack(set == 1 ? ModUtilityToolItems.clay_tablet_1.get() : ModUtilityToolItems.clay_tablet.get());
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
