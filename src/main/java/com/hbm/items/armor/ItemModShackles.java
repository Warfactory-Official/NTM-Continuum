package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModDataComponents;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModShackles}: infinite revives below 1000 RAD, each one costing {@code n^2} RAD where
 * {@code n} is the revive count. The count is kept in the item's damage value, which is why upstream's
 * shackles were an undamageable item whose damage still climbed - the port stores it in a custom-data
 * counter because a modern stack refuses damage on an item without durability.
 */
public class ItemModShackles extends ItemArmorMod {

    public ItemModShackles(Item.Properties properties) {
        super(ArmorModHandler.extra, false, false, true, false, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.RED, "You will speak when I ask you to."));
        tooltip.add(coloured(ChatFormatting.RED, "You will eat when I tell you to."));
        tooltip.add(Component.literal("You will die when I allow you to.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.empty());
        tooltip.add(coloured(ChatFormatting.GOLD, "∞ revives left"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.GOLD, stack, " (∞ revives left)"));
    }

    private static final String USES = "shackle_revives";

    /** The number of revives so far - upstream's item damage. */
    public static int revives(ItemStack stack) {
        CompoundTag data = ModDataComponents.PERSISTENT_DATA.get(stack).orElse(null);
        return data == null ? 0 : Tags.getIntOr(data, USES, 0);
    }

    public static void setRevives(ItemStack stack, int count) {
        ModDataComponents.PERSISTENT_DATA.update(stack, new CompoundTag(), tag -> {
            CompoundTag copy = tag.copy();
            copy.putInt(USES, count);
            return copy;
        });
    }
}
