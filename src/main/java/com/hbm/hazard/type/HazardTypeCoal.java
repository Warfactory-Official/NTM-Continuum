package com.hbm.hazard.type;

import com.hbm.api.hazard.IHazardType;
import java.util.List;

import com.hbm.capability.HbmLivingProps;
import com.hbm.config.RadiationConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.api.hazard.IHazardModifier;
import com.hbm.util.ArmorRegistry;
import com.hbm.api.hazard.HazardClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeCoal implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {

        if (RadiationConfig.disableCoal)
            return;

        if (!ArmorRegistry.hasProtection(target, EquipmentSlot.HEAD, HazardClass.PARTICLE_COARSE)) {
            HbmLivingProps.incrementBlackLung(target, (int) Math.min(level * stack.getCount(), 10) * hazardRate);
        } else {
            if (target.getRandom().nextInt(Math.max(65 - stack.getCount(), 1)) == 0) {
                ArmorUtil.damageGasMaskFilter(target, (int) level * hazardRate);
            }
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        list.add(Component.literal("[").append(Component.translatable("trait.coal")).append(Component.literal("]")).withStyle(ChatFormatting.DARK_GRAY));
    }
}
