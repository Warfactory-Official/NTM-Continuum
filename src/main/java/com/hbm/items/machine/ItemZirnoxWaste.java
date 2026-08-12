package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;

/** One of the historical spent-fuel material identities; heat is mutable stack state, not another ID. */
public class ItemZirnoxWaste extends Item {

    public static final int HOT_TINT = 0xFFBFA5;

    public ItemZirnoxWaste(Properties properties) {
        super(properties);
    }

    public static boolean isHot(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FUEL_WASTE_HOT.get(), false);
    }

    public static ItemStack makeHot(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        setHot(stack, true);
        return stack;
    }

    public static void setHot(ItemStack stack, boolean hot) {
        if (hot) {
            stack.set(ModDataComponents.FUEL_WASTE_HOT.get(), true);
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(HOT_TINT)));
        } else {
            stack.remove(ModDataComponents.FUEL_WASTE_HOT.get());
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        if (isHot(stack)) {
            adder.accept(Component.translatable("item.hbm.nuclear_waste.cool").withStyle(ChatFormatting.GOLD));
        }
    }
}
