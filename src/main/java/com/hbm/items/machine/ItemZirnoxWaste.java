package com.hbm.items.machine;

import dev.golem.core.data.ModelData;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

/** One of the historical spent-fuel material identities; heat is mutable stack state, not another ID. */
public class ItemZirnoxWaste extends CompatItem {

    public static final int HOT_TINT = 0xFFBFA5;

    public ItemZirnoxWaste(Properties properties) {
        super(properties);
    }

    public static boolean isHot(ItemStack stack) {
        return ModDataComponents.wasteHot(stack);
    }

    public static ItemStack makeHot(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        setHot(stack, true);
        return stack;
    }

    public static void setHot(ItemStack stack, boolean hot) {
        if (hot) {
            ModDataComponents.wasteHot(stack, true);
            ModelData.setColor(stack, HOT_TINT);
        } else {
            ModDataComponents.removeWasteHot(stack);
            ModDataComponents.removeCustomModelData(stack);
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (isHot(stack)) {
            tooltip.add(Component.translatable("item.hbm.nuclear_waste.cool").withStyle(ChatFormatting.GOLD));
        }
    }
}
