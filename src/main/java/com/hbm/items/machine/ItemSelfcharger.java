package com.hbm.items.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.lib.Library;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Deprecated fixed-output battery held in hand to offset powered-armour drain. */
@Deprecated
public final class ItemSelfcharger extends CompatItem implements IBatteryItem {
    private final long charge;

    public ItemSelfcharger(Item.Properties properties, long charge) {
        super(properties);
        this.charge = charge;
    }

    @Override public void chargeBattery(ItemStack stack, long amount) {}
    @Override public void setCharge(ItemStack stack, long amount) {}
    @Override public void dischargeBattery(ItemStack stack, long amount) {}
    @Override public long getCharge(ItemStack stack) { return charge; }
    @Override public long getMaxCharge(ItemStack stack) { return charge; }
    @Override public long getChargeRate(ItemStack stack) { return 0; }
    @Override public long getDischargeRate(ItemStack stack) { return charge; }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (charge == Long.MAX_VALUE) {
            tooltip.add(Component.translatable("desc.infinitehe").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.literal(Library.getShortNumber(charge * 20) + "HE/s")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}
