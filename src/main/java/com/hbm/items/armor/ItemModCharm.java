package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModCharm}: the Charm of Protection halves broadcaster damage, the Meteor Charm negates
 * it. Their meteor behaviour is {@link com.hbm.handler.BossSpawnHandler#strike}: worn as a helmet mod (or on a
 * pedestal within 100 blocks), the Meteor Charm cancels a strike and the Charm of Protection diverts it.
 */
public class ItemModCharm extends ItemArmorMod {

    private final boolean meteor;

    public ItemModCharm(boolean meteor, Item.Properties properties) {
        super(ArmorModHandler.helmet_only, true, true, false, false, properties);
        this.meteor = meteor;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.AQUA, "You feel blessed."));
        if (!meteor) {
            tooltip.add(coloured(ChatFormatting.AQUA, "Diverts meteors away from the player."));
            tooltip.add(coloured(ChatFormatting.AQUA, "Meteors no longer destroy blocks."));
            tooltip.add(coloured(ChatFormatting.AQUA, "Halves broadcaster damage"));
        } else {
            tooltip.add(coloured(ChatFormatting.AQUA, "Disables meteorite spawning."));
            tooltip.add(coloured(ChatFormatting.AQUA, "Negates broadcaster damage"));
        }
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.GOLD, stack, ""));
    }

    @Override
    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {
        // 1.7 compared against ModDamageSource.broadcast, whose message id was "broadcast".
        if ("broadcast".equals(event.source().getMsgId())) {
            if (!meteor) event.setAmount(event.amount() * 0.5F);
            else event.setAmount(0F);
        }
    }

    public boolean repelsMeteors() {
        return !meteor;
    }

    public boolean stopsMeteors() {
        return meteor;
    }
}
