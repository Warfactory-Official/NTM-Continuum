package com.hbm.items.tool;

import com.hbm.api.item.IGasMask;
import com.hbm.handler.ArmorModHandler;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemFilter}: a gas mask filter. Right-clicking one screws it into the worn helmet, or into the helmet
 * module of the worn helmet, swapping out a filter that was already there.
 */
public class ItemFilter extends CompatItem {

    public ItemFilter(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack filter = player.getItemInHand(hand);
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!(helmet.getItem() instanceof IGasMask) && ArmorModHandler.hasMods(helmet)) {
            ItemStack mask = ArmorModHandler.pryMod(helmet, ArmorModHandler.helmet_only);
            if (!mask.isEmpty()) {
                InteractionResult result = installOn(level, player, hand, mask, filter);
                if (!level.isClientSide()) ArmorModHandler.applyMod(helmet, mask);
                return result;
            }
        }
        return installOn(level, player, hand, helmet, filter);
    }

    private static InteractionResult installOn(Level level, Player player, InteractionHand hand, ItemStack helmet,
                                               ItemStack filter) {
        if (!(helmet.getItem() instanceof IGasMask mask) || !mask.isFilterApplicable(helmet, filter)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack current = mask.getFilter(helmet);
        mask.installFilter(helmet, filter);
        // 1.7: the held filter is used up, or becomes the filter that was replaced
        player.setItemInHand(hand, current.isEmpty() ? ItemStack.EMPTY : current);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GASMASK_SCREW.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
