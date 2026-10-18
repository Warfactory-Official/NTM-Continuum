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

/** 1.7 {@code ItemSoyuz}: the satellite launch vehicle, one item per paint ({@code missile_soyuz_0/1/2}). */
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
        SoyuzSkinType type = typeOf(stack);
        if (type == null) return;
        Component skin = switch (type) {
            case NORMAL -> Component.translatable("item.missile_soyuz.orig.desc").withStyle(ChatFormatting.GOLD);
            case LUNAR -> Component.translatable("item.missile_soyuz.luna.desc").withStyle(ChatFormatting.BLUE);
            case POST_WAR -> Component.translatable("item.missile_soyuz.war.desc").withStyle(ChatFormatting.GREEN);
        };
        tooltip.add(Component.translatable("item.missile_soyuz.desc").append(" ").append(skin));
    }
}
