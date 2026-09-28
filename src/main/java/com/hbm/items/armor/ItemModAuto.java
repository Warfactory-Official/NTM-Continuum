package com.hbm.items.armor;

import com.hbm.capability.HbmLivingProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.potion.ModEffects;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 1.7 {@code ItemModAuto} (5-HTP autoinjector): spends itself to purge 5 digamma once it reaches 5. */
public class ItemModAuto extends ItemArmorMod {

    public ItemModAuto(Item.Properties properties) {
        super(ArmorModHandler.extra, false, true, false, false, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.BLUE, "Imported from Japsterdam."));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.BLUE, stack, ""));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide()) return;
        if (HbmLivingProps.getDigamma(entity) >= 5F) {
            ArmorModHandler.removeMod(armor, ArmorModHandler.extra);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.SYRINGE.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            HbmLivingProps.setDigamma(entity, HbmLivingProps.getDigamma(entity) - 5F);
            entity.addEffect(Effects.instanceRegistered(ModEffects.STABILITY.get(), 60 * 20, 0));
            entity.heal(20F);
        }
    }
}
