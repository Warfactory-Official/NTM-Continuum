package com.hbm.hazard.type;

import com.hbm.api.hazard.IHazardType;
import java.util.List;

import com.hbm.config.RadiationConfig;
import com.hbm.api.hazard.IHazardModifier;
import com.hbm.util.ArmorRegistry;
import com.hbm.api.hazard.HazardClass;
import dev.golem.core.world.Effects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeBlinding implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {

        if (RadiationConfig.disableBlinding)
            return;

        if (!ArmorRegistry.hasProtection(target, EquipmentSlot.HEAD, HazardClass.LIGHT)) {
            Effects.add(target, MobEffects.BLINDNESS, (int) level * hazardRate, 0);
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        list.add(Component.literal("[").append(Component.translatable("trait.blinding")).append(Component.literal("]")).withStyle(ChatFormatting.DARK_AQUA));
    }
}
