package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModBathwater}: poisons (Poison III) or, for the horse-scented mk2, withers (Wither V)
 * whoever hits the wearer. The tooltip colour flips every half second, as upstream's did.
 */
public class ItemModBathwater extends ItemArmorMod {

    private final boolean mk2;

    public ItemModBathwater(boolean mk2, Item.Properties properties) {
        super(ArmorModHandler.extra, true, true, true, true, properties);
        this.mk2 = mk2;
    }

    private ChatFormatting colour() {
        boolean first = System.currentTimeMillis() % 1000 < 500;
        if (mk2) return first ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        return first ? ChatFormatting.BLUE : ChatFormatting.LIGHT_PURPLE;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(colour(), "Inflicts poison on the attacker"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(colour(), stack, " (Poisons attackers)"));
    }

    @Override
    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {
        if (event.entity().level().isClientSide()) return;
        // 1.7 tested for an EntityDamageSource; the modern equivalent is a source with a causing entity.
        if (event.source().getEntity() instanceof LivingEntity attacker) {
            if (mk2) Effects.add(attacker, MobEffects.WITHER, 200, 4);
            else Effects.add(attacker, MobEffects.POISON, 200, 2);
        }
    }
}
