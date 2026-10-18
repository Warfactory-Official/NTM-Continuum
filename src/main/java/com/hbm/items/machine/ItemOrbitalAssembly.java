package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.items.ItemEnumMulti;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7 {@code ItemOrbitalAssembly}: a part sent up to a space laboratory, which its 0G assemblers finish in orbit. */
public class ItemOrbitalAssembly extends ItemEnumMulti implements ISatChip {

    public ItemOrbitalAssembly(Properties properties, EnumOrbitalAssembly type) {
        super(EnumOrbitalAssembly.class, type, properties);
    }

    public enum EnumOrbitalAssembly {
        CRYSTAL_CIRCUIT,
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(Language.getInstance().getOrDefault("satchip.frequency") + ": " + getFreq(stack))
                .withStyle(ChatFormatting.AQUA));
    }
}
