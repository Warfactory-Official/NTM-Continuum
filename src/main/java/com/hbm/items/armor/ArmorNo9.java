package com.hbm.items.armor;

import com.hbm.capability.HbmLivingProps;
import com.hbm.items.ModDataComponents;
import dev.golem.core.data.Tags;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.GolemArmorMaterial;
import dev.golem.core.world.Sounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ArmorNo9}, the mining helmet. Never wears out; 0.5 damage threshold against blockable damage (a hit
 * of 0.5 or less is cancelled). While worn, black lung is capped at 90% of the maximum and, from 25% up, recovers
 * one point per tick.
 *
 * <p>The lamp follows the player's HUD toggle ({@code enableHUD}, the head toggle key): the server mirrors it into
 * the stack's {@code isOn} flag, playing the flint-and-steel click (pitch 1.5) when it lights and a fizz (0.5,
 * pitch 2) when it goes out, and the worn model draws the flame fullbright while it is set. The lamp itself is
 * {@code LampLights}.
 */
public class ArmorNo9 extends ArmorModel implements IAttackHandler, IDamageHandler {

    public ArmorNo9(GolemArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(material, slot, properties, "no9");
    }

    /** The 1.7 {@code isOn} tag, kept in the stack's persistent-data compound. */
    public static boolean isOn(ItemStack stack) {
        CompoundTag tag = ModDataComponents.PERSISTENT_DATA.getOr(stack, null);
        return tag != null && Tags.getBooleanOr(tag, "isOn", false);
    }

    private static void setOn(ItemStack stack, boolean on) {
        if (isOn(stack) == on && ModDataComponents.PERSISTENT_DATA.has(stack)) return;
        CompoundTag tag = ModDataComponents.PERSISTENT_DATA.getOr(stack, null);
        tag = tag == null ? new CompoundTag() : tag.copy();
        tag.putBoolean("isOn", on);
        ModDataComponents.PERSISTENT_DATA.set(stack, tag);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("+0.5 DT").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("Lets you breathe coal, neat!").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void handleDamage(DamageEvents.DamageEvent event, ItemStack stack) {
        if (event.source().is(DamageTypeTags.BYPASSES_ARMOR)) return;
        event.setAmount(Math.max(event.amount() - 0.5F, 0F));
    }

    @Override
    public void handleAttack(DamageEvents.DamageEvent event, ItemStack armor) {
        if (event.source().is(DamageTypeTags.BYPASSES_ARMOR)) return;
        if (event.amount() <= 0.5F) event.cancel();
    }

    /** 1.7 {@code onArmorTick}, server half; driven from the player tick while the helmet is worn. */
    public void armorTick(Level world, Player player, ItemStack armor) {
        if (world.isClientSide()) return;

        boolean turnOn = HbmLivingProps.getEnableHUD(player);
        boolean wasOn = isOn(armor);

        if (turnOn && !wasOn) world.playSound(null, player.getX(), player.getY(), player.getZ(),
                Sounds.minecraft("item.flintandsteel.use"), SoundSource.PLAYERS, 1F, 1.5F);
        if (!turnOn && wasOn) world.playSound(null, player.getX(), player.getY(), player.getZ(),
                Sounds.minecraft("block.fire.extinguish"), SoundSource.PLAYERS, 0.5F, 2F);
        setOn(armor, turnOn);

        int max = HbmLivingProps.maxBlacklung;
        if (HbmLivingProps.getBlackLung(player) > max * 0.9) {
            HbmLivingProps.setBlackLung(player, (int) (max * 0.9));
        }
        if (HbmLivingProps.getBlackLung(player) >= max * 0.25) {
            HbmLivingProps.setBlackLung(player, HbmLivingProps.getBlackLung(player) - 1);
        }
    }
}
