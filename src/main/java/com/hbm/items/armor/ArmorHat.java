package com.hbm.items.armor;

import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.GolemArmorMaterial;
import dev.golem.core.world.Sounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ArmorHat}, the Fabulous Hat ({@code nossy_hat}): 2 points of damage threshold against anything
 * armour can block - a hit of 2 or less is cancelled outright with a loud break sound - and a dropped hat
 * vanishes.
 */
public class ArmorHat extends ArmorModel implements IAttackHandler, IDamageHandler {

    public ArmorHat(GolemArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(material, slot, properties, "nossy_hat");
    }

    /** 1.7 {@code onEntityItemUpdate}: {@code setDead}. Driven by the loaders' item-entity tick. */
    public static void tickDropped(ItemEntity entity) {
        if (entity.getItem().getItem() instanceof ArmorHat) entity.discard();
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("+2 DT").withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void handleDamage(DamageEvents.DamageEvent event, ItemStack stack) {
        if (event.source().is(DamageTypeTags.BYPASSES_ARMOR)) return;
        float amount = event.amount() - 2F;
        event.setAmount(Math.max(amount, 0F));
    }

    @Override
    public void handleAttack(DamageEvents.DamageEvent event, ItemStack armor) {
        if (event.source().is(DamageTypeTags.BYPASSES_ARMOR)) return;
        if (event.amount() <= 2F) {
            LivingEntity e = event.entity();
            e.level().playSound(null, e.getX(), e.getY(), e.getZ(), Sounds.minecraft("entity.item.break"),
                    SoundSource.PLAYERS, 5F, 1.0F + e.getRandom().nextFloat() * 0.5F);
            event.cancel();
        }
    }
}
