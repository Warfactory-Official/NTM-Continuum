package com.hbm.items.weapon.sedna.impl;

import com.hbm.items.ICustomizable;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ItemGunNI4NI extends ItemGunBaseNT implements ICustomizable {
    public static final String KEY_COIN_COUNT = "coincount";
    public static final String KEY_COIN_CHARGE = "coincharge";

    public ItemGunNI4NI(Properties properties, WeaponQuality quality, GunConfig... configs) {
        super(properties, quality, configs);
    }

    public static void resetColors(ItemStack stack) { removeValue(stack, "colors"); }
    public static void setColors(ItemStack stack, int dark, int light, int grip) {
        setValueIntArray(stack, "colors", new int[] {dark, light, grip});
    }
    public static int[] getColors(ItemStack stack) {
        int[] colors = getValueIntArray(stack, "colors");
        return colors.length == 3 ? colors : null;
    }
    public static int getCoinCount(ItemStack stack) { return getValueInt(stack, KEY_COIN_COUNT); }
    public static void setCoinCount(ItemStack stack, int value) { setValueInt(stack, KEY_COIN_COUNT, value); }
    public static int getCoinCharge(ItemStack stack) { return getValueInt(stack, KEY_COIN_CHARGE); }
    public static void setCoinCharge(ItemStack stack, int value) { setValueInt(stack, KEY_COIN_CHARGE, value); }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity entity,
            EquipmentSlot slot, boolean held) {
        super.compatInventoryTick(stack, level, entity, slot, held);
        int maxCoin = 4;
        if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NI4NI_NICKEL)) maxCoin += 2;
        if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NI4NI_DOUBLOONS)) maxCoin += 2;
        if (getCoinCount(stack) >= maxCoin) return;

        int charge = getCoinCharge(stack) + 1;
        if (charge >= 80) {
            charge = 0;
            int count = getCoinCount(stack) + 1;
            setCoinCount(stack, count);
            if (slot == EquipmentSlot.MAINHAND) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.TECH_BOOP.get(),
                        SoundSource.PLAYERS, 1F, 1F + count / (float) maxCoin);
            }
        }
        setCoinCharge(stack, charge);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.item.gunNI4NI.nowDonTGet"));
        tooltip.add(Component.translatable("desc.item.gunNI4NI.i").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable("desc.item.gunNI4NI.fuckingHate").withStyle(ChatFormatting.RED))
                .append(Component.translatable("desc.item.gunNI4NI.thisGame").withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.translatable("desc.item.gunNI4NI.iDidnTDo"));
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void customize(net.minecraft.server.level.ServerPlayer player, ItemStack stack, String... args) {
        if (args.length == 0) {
            resetColors(stack);
            player.sendSystemMessage(Component.translatable("desc.item.gunNI4NI.colorsReset").withStyle(ChatFormatting.GREEN));
            return;
        }
        if (args.length != 3) {
            resetColors(stack);
            player.sendSystemMessage(Component.translatable("desc.item.gunNI4NI.requiresThreeHexadecimal").withStyle(ChatFormatting.RED));
            return;
        }
        try {
            int dark = Integer.parseInt(args[0], 16);
            int light = Integer.parseInt(args[1], 16);
            int grip = Integer.parseInt(args[2], 16);
            if (dark < 0 || dark > 0xFFFFFF || light < 0 || light > 0xFFFFFF || grip < 0 || grip > 0xFFFFFF) {
                player.sendSystemMessage(Component.translatable("desc.item.gunNI4NI.colorsMustRangeFrom").withStyle(ChatFormatting.RED));
                return;
            }
            setColors(stack, dark, light, grip);
            player.sendSystemMessage(Component.translatable("desc.item.gunNI4NI.colorsSet").withStyle(ChatFormatting.GREEN));
        } catch (Throwable ex) {
            player.sendSystemMessage(Component.literal(ex.getLocalizedMessage()).withStyle(ChatFormatting.RED));
        }
    }
}
