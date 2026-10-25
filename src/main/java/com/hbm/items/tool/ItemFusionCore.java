package com.hbm.items.tool;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.armor.ArmorFSBPowered;
import com.hbm.lib.Library;
import com.hbm.sound.EnergyNetSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemFusionCore}: with a full powered-armor set worn (chestplate an {@link ArmorFSBPowered}) it adds
 * its charge to every worn battery-backed piece, capped at each piece's maximum, and is used up.
 */
public class ItemFusionCore extends CompatItem {

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
            EquipmentSlot.HEAD};

    private final long charge;

    public ItemFusionCore(Properties properties, long charge) {
        super(properties);
        this.charge = charge;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ArmorFSB.hasFSBArmorIgnoreCharge(player)
                && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorFSBPowered) {
            for (EquipmentSlot slot : ARMOR) {
                ItemStack armor = player.getItemBySlot(slot);
                if (armor.getItem() instanceof IBatteryItem battery) {
                    long max = battery.getMaxCharge(armor);
                    battery.setCharge(armor, Math.min(battery.getCharge(armor) + charge, max));
                }
            }
            stack.shrink(1); // 1.7 spends the core even in creative
            level.playSound(null, player.getX(), player.getY(), player.getZ(), EnergyNetSounds.ITEM_BATTERY.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Charges all worn armor pieces by " + Library.getShortNumber(charge) + "HE")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("[Requires full electric set to be worn]"));
    }
}
