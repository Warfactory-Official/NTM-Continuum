package com.hbm.items.special;

import com.hbm.util.ContaminationUtil;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** The current digamma particle: one inventory dose every tick and the original half-life tooltip. */
public final class ItemDigamma extends CompatItem {
    private final int digamma;

    public ItemDigamma(Properties properties, int digamma) {
        super(properties);
        this.digamma = digamma;
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner,
                                       EquipmentSlot slot, boolean held) {
        if (owner instanceof LivingEntity living) {
            ContaminationUtil.applyDigammaData(living, 1.0 / digamma);
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("trait.hlParticle", "1.67*10\u00b3\u2074 a")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("trait.hlPlayer", (digamma / 20D) + "s")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.empty());
        double mdrxPerSecond = ((int) ((1000D / digamma) * 200D)) / 10D;
        tooltip.add(Component.literal("[").append(Component.translatable("trait.digamma")).append("]")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(mdrxPerSecond + "mDRX/s").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("[").append(Component.translatable("trait.drop")).append("]")
                .withStyle(ChatFormatting.RED));
    }
}
