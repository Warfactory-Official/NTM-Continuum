package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModWD40} (VT-40): +4 max health, and four hits in five repair a point of the armor it
 * sits in. While hurt the wearer sheds blue-green dust on the client (a {@code vanillaExt reddust}
 * whose motion fields were really the dust colour).
 */
public class ItemModWD40 extends ItemArmorMod {

    /** Legacy reddust colour (0.01, 0.5, 0.8) as RGB. */
    private static final int DUST = ((int) (0.01F * 255) << 16) | ((int) (0.5F * 255) << 8) | (int) (0.8F * 255);

    public ItemModWD40(Item.Properties properties) {
        super(ArmorModHandler.extra, true, true, true, true, properties);
    }

    private static ChatFormatting colour() {
        return System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.BLUE : ChatFormatting.YELLOW;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(colour(), "Highly reduces damage taken by armor, +2 HP"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(colour(), stack, " (-80% armor wear / +2 HP)"));
    }

    @Override
    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {
        LivingEntity entity = event.entity();
        if (!entity.level().isClientSide() && armor.getDamageValue() > 0 && entity.getRandom().nextInt(5) != 0) {
            armor.setDamageValue(armor.getDamageValue() - 1);
        }
    }

    @Override
    public void collectModifiers(ItemStack armor, ArmorSlot slot, List<Modifier> modifiers) {
        // Upstream says "+2 HP" (one heart) and grants 4 health points, which is the same thing.
        modifiers.add(new Modifier(Modifier.MAX_HEALTH, 4D, Attributes.Op.ADD));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide() && entity.hurtTime > 0) {
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth() * 2;
            double y = entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight();
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth() * 2;
            entity.level().addParticle(Compat.dust(DUST, 1F), x, y, z, 0, 0, 0);
        }
    }
}
