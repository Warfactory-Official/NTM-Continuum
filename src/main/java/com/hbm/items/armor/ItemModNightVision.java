package com.hbm.items.armor;

import com.hbm.capability.HbmLivingProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModDataComponents;
import com.hbm.items.gear.ArmorFSBPowered;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModNightVision}: while a complete, charged electric suit is worn and its HUD is on,
 * keeps Night Vision topped up at 15 s (to hide client lag flicker) and wears the suit one point in 200
 * ticks. The armor remembers that it applied the effect so turning the HUD off removes only its own
 * night vision.
 */
public class ItemModNightVision extends ItemArmorMod {

    private static final String NIGHT_VISION_ACTIVE_NBT_KEY = "ITEM_MOD_NV_ACTIVE";

    public ItemModNightVision(Item.Properties properties) {
        super(ArmorModHandler.helmet_only, true, false, false, false, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.night_vision.description.item").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(Component.translatable("item.night_vision.description.in_armor", stack.getHoverName())
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide() || !(entity instanceof Player player)
                || !(armor.getItem() instanceof ArmorFSBPowered powered) || !ArmorFSBPowered.hasFSBArmor(player)) return;

        if (HbmLivingProps.getData(player).getEnableHUD()) {
            // 15 seconds to make less flickering if the client lags
            Effects.add(player, MobEffects.NIGHT_VISION, 15 * 20, 0);
            if (!hasFlag(armor)) {
                ModDataComponents.PERSISTENT_DATA.update(armor, new CompoundTag(), tag -> {
                    CompoundTag copy = tag.copy();
                    copy.putBoolean(NIGHT_VISION_ACTIVE_NBT_KEY, true); // value does not matter, it's a flag
                    return copy;
                });
            }
            // 1.7 damageItem(1) on a powered suit is setDamage, which drains one hit's worth of power.
            if (player.getRandom().nextInt(200) == 0) powered.dischargeBattery(armor, powered.consumption);
        } else if (hasFlag(armor)) {
            // Only remove night vision the module itself applied.
            Effects.remove(player, MobEffects.NIGHT_VISION);
            ModDataComponents.PERSISTENT_DATA.update(armor, new CompoundTag(), tag -> {
                CompoundTag copy = tag.copy();
                copy.remove(NIGHT_VISION_ACTIVE_NBT_KEY);
                return copy;
            });
        }
    }

    private static boolean hasFlag(ItemStack armor) {
        CompoundTag data = ModDataComponents.PERSISTENT_DATA.get(armor).orElse(null);
        return data != null && data.contains(NIGHT_VISION_ACTIVE_NBT_KEY);
    }
}
