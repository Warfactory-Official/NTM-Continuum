package com.hbm.items.special;

import java.util.function.Consumer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemCustomLore extends Item {

    public ItemCustomLore(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        String base = this.getDescriptionId() + ".desc.";
        Language language = Language.getInstance();
        for (int i = 0; language.has(base + i); i++) {
            adder.accept(Component.translatable(base + i));
        }
    }
}
