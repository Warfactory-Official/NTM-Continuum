package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

/** The item form of the arbitrary-capacity machine_battery_redd storage block. */
public final class ItemBatteryREDD extends CompatBlockItem {

    public ItemBatteryREDD(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (!ModDataComponents.hasBatteryReddPower(stack)) return;
        BigInteger power = ModDataComponents.batteryReddPower(stack);
        tooltip.add(Component.literal(String.format(Locale.US, "%,d", power) + " HE")
                .withStyle(ChatFormatting.YELLOW));
    }
}
