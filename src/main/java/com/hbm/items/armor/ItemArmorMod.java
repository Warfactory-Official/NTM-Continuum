package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.special.ItemCustomLore;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Base contract for an armor module: one ordered slot and the armor pieces it accepts. */
public class ItemArmorMod extends ItemCustomLore {

    public final int type;
    public final boolean helmet;
    public final boolean chestplate;
    public final boolean leggings;
    public final boolean boots;

    public ItemArmorMod(int type, boolean helmet, boolean chestplate, boolean leggings, boolean boots,
                        Item.Properties properties) {
        super(properties);
        this.type = type;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        super.appendCompatHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("armorMod.applicableTo").withStyle(ChatFormatting.DARK_PURPLE));
        if (helmet && chestplate && leggings && boots) {
            tooltip.add(Component.literal("  ").append(Component.translatable("armorMod.all")));
        } else {
            if (helmet) tooltip.add(Component.literal("  ").append(Component.translatable("armorMod.helmets")));
            if (chestplate) tooltip.add(Component.literal("  ").append(Component.translatable("armorMod.chestplates")));
            if (leggings) tooltip.add(Component.literal("  ").append(Component.translatable("armorMod.leggings")));
            if (boots) tooltip.add(Component.literal("  ").append(Component.translatable("armorMod.boots")));
        }
        tooltip.add(Component.translatable("desc.applicableslot").withStyle(ChatFormatting.DARK_PURPLE));
        String key = switch (type) {
            case ArmorModHandler.helmet_only -> "armorMod.type.helmet";
            case ArmorModHandler.plate_only -> "armorMod.type.chestplate";
            case ArmorModHandler.legs_only -> "armorMod.type.leggings";
            case ArmorModHandler.boots_only -> "armorMod.type.boots";
            case ArmorModHandler.servos -> "armorMod.type.servo";
            case ArmorModHandler.cladding -> "armorMod.type.cladding";
            case ArmorModHandler.kevlar -> "armorMod.type.insert";
            case ArmorModHandler.extra -> "armorMod.type.special";
            case ArmorModHandler.battery -> "armorMod.type.battery";
            default -> null;
        };
        if (key != null) tooltip.add(Component.literal("  ").append(Component.translatable(key)));
    }

    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {}

    public void modUpdate(LivingEntity entity, ItemStack armor) {}

    /** Old {@code addDesc}: one installed-module line in an armor piece's shift tooltip. */
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(stack.getHoverName());
    }
}
