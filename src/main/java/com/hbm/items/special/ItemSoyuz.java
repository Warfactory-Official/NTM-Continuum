package com.hbm.items.special;

import com.hbm.items.ItemEnumMulti;
import com.hbm.items.ItemEnums.SoyuzSkinType;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** The satellite launch vehicle, as the item the launcher consumes; one item per paint
 * ({@code missile_soyuz_0/1/2}). */
public class ItemSoyuz extends ItemEnumMulti {

    public ItemSoyuz(Properties properties, SoyuzSkinType type) {
        super(SoyuzSkinType.class, type, properties);
    }

    public @Nullable SoyuzSkinType typeOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemSoyuz)) return null;
        return (SoyuzSkinType) getType(stack);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.soyuz").withStyle(ChatFormatting.GRAY));
    }
}
