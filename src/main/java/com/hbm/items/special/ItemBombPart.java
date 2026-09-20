package com.hbm.items.special;

import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 1.7 {@code ItemFleija} / {@code ItemSolinium}: a bomb component whose tooltip reads
 * {@code item.bomb_part.used_in} followed by the localized name of the bomb it belongs to.
 */
public class ItemBombPart extends CompatItem {

    private final RegistryObject<? extends Block> usedIn;

    public ItemBombPart(Item.Properties properties, RegistryObject<? extends Block> usedIn) {
        super(properties);
        this.usedIn = usedIn;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bomb_part.used_in"));
        tooltip.add(usedIn.get().getName());
    }
}
