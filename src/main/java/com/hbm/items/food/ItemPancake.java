package com.hbm.items.food;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.ModArmorItems;
import com.hbm.items.gear.ArmorFSB;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemPancake}: only a wearer of the full lunar (BJ) set can eat it, and eating it fills every worn
 * battery-backed armor piece. Everyone else is told their teeth are too soft.
 */
public class ItemPancake extends ItemModFood {

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
            EquipmentSlot.HEAD};

    public ItemPancake(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (ArmorFSB.hasFSBArmorIgnoreCharge(player)
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModArmorItems.bj_helmet.get())) {
            return compatVanillaUse(level, player, hand);
        }
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.literal("Your teeth are too soft to eat this.")
                    .withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected void onFoodEaten(ItemStack stack, ServerLevel level, Player player) {
        for (EquipmentSlot slot : ARMOR) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof IBatteryItem battery) {
                battery.setCharge(armor, battery.getMaxCharge(armor));
            }
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Can be eaten to recharge lunar cybernetic armor"));
        tooltip.add(Component.literal("Not for people with weak molars"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Half burnt and smells horrible"));
    }
}
