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

/**
 * Dropped radioactive material contaminates the ground around it (chunk radiation + waste transform).
 * Dropped-item ticking is deferred with the fallout/waste systems — the tooltip and registration are
 * kept so the data carries through.
 */
public class HazardTypeContaminating implements IHazardType {

    private static final int MAX_RADIUS = 500;

    private static int computeRadius(double level) {
        return (int) Math.min(Math.sqrt(level) + 0.5D, MAX_RADIUS);
    }

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
    }

    @Override
    public void updateEntity(ItemEntity item, double level) {
        // TODO(fallout): ground contamination (incrementRad + ExplosionNukeGeneric.waste) once
        // dropped-item hazard ticking + the waste transform land. Config-gated off by default anyway.
    }

    @Override
    public void addHazardInformation(Player player, List<Component> list, double level, ItemStack stack, List<IHazardModifier> modifiers) {
        if (!RadiationConfig.enableContaminationOnGround) return;
        int radius = computeRadius(level);
        if (radius > 1) {
            list.add(Component.literal("[").append(Component.translatable("trait.contaminating")).append(Component.literal("]")).withStyle(ChatFormatting.DARK_GREEN));
            list.add(Component.literal(" ").append(Component.translatable("trait.contaminating.radius", radius)).withStyle(ChatFormatting.GREEN));
        }
    }
}
