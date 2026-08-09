package com.hbm.hazard.type;

import java.util.List;

import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.ContaminationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeDigamma implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {
        ContaminationUtil.applyDigammaData(target, (level * stack.getCount() / 20D) * hazardRate);
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        level = IHazardModifier.evalAllModifiers(stack, player, level, modifiers);

        final double displayLevel = Math.round(level * 10000D) / 10D;
        list.add(Component.literal("[").append(Component.translatable("trait.digamma")).append(Component.literal("]")).withStyle(ChatFormatting.RED));
        list.add(Component.literal(displayLevel + "mDRX/s").withStyle(ChatFormatting.DARK_RED));

        if (stack.getCount() > 1) {
            final double stackLevel = displayLevel * stack.getCount();
            list.add(Component.literal("Stack: " + stackLevel + "mDRX/s").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
