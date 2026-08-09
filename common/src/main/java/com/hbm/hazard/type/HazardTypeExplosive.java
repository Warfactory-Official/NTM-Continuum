package com.hbm.hazard.type;

import java.util.List;

import com.hbm.config.RadiationConfig;
import com.hbm.hazard.modifier.IHazardModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HazardTypeExplosive implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {

        if (RadiationConfig.disableExplosive)
            return;

        if (target.isOnFire() && stack.getCount() > 0) {
            stack.setCount(0);
            target.level().explode(null, target.getX(), target.getY() + target.getEyeHeight(), target.getZ(),
                    (float) level, false, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {

        if (RadiationConfig.disableExplosive)
            return;

        if (item.isOnFire()) {
            item.discard();
            item.level().explode(null, item.getX(), item.getY() + item.getBbHeight() * 0.5, item.getZ(),
                    (float) level, false, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        list.add(Component.literal("[").append(Component.translatable("trait.explosive")).append(Component.literal("]")).withStyle(ChatFormatting.RED));
    }
}
