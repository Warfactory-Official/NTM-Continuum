package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.CompatArmorItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModServos}: on a chestplate Haste and +50%/+150% attack damage; on leggings +25%/+50%
 * speed and Jump Boost. The damage and speed bonuses are legacy operation 2 (multiply total).
 */
public class ItemModServos extends ItemArmorMod {

    private final boolean desh;

    public ItemModServos(boolean desh, Item.Properties properties) {
        super(ArmorModHandler.servos, false, true, true, false, properties);
        this.desh = desh;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (!desh) {
            tooltip.add(coloured(ChatFormatting.DARK_PURPLE, "Chestplate: Haste I / Damage +50%"));
            tooltip.add(coloured(ChatFormatting.DARK_PURPLE, "Leggings: Speed +25% / Jump II"));
        } else {
            tooltip.add(coloured(ChatFormatting.DARK_PURPLE, "Chestplate: Haste III / Damage +150%"));
            tooltip.add(coloured(ChatFormatting.DARK_PURPLE, "Leggings: Speed +50% / Jump III"));
        }
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        ArmorSlot slot = slotOf(armor);
        if (slot == ArmorSlot.CHESTPLATE) {
            tooltip.add(modLine(ChatFormatting.DARK_PURPLE, stack, desh ? " (Haste III / Damage +150%)" : " (Haste I / Damage +50%)"));
        }
        if (slot == ArmorSlot.LEGGINGS) {
            tooltip.add(modLine(ChatFormatting.DARK_PURPLE, stack, desh ? " (Speed +50% / Jump III)" : " (Speed +25% / Jump II)"));
        }
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        ArmorSlot slot = slotOf(armor);
        if (slot == ArmorSlot.CHESTPLATE) Effects.add(entity, Effects.haste(), 60, desh ? 2 : 0);
        if (slot == ArmorSlot.LEGGINGS) Effects.add(entity, Effects.jumpBoost(), 60, desh ? 2 : 1);
    }

    @Override
    public void collectModifiers(ItemStack armor, ArmorSlot slot, List<Modifier> modifiers) {
        if (slot == ArmorSlot.CHESTPLATE)
            modifiers.add(new Modifier(Modifier.ATTACK_DAMAGE, desh ? 1.5D : 0.5D, Attributes.Op.MULTIPLY_TOTAL));
        if (slot == ArmorSlot.LEGGINGS)
            modifiers.add(new Modifier(Modifier.MOVEMENT_SPEED, desh ? 0.5D : 0.25D, Attributes.Op.MULTIPLY_TOTAL));
    }

    private static ArmorSlot slotOf(ItemStack armor) {
        return armor.getItem() instanceof CompatArmorItem item ? item.armorSlot() : null;
    }
}
