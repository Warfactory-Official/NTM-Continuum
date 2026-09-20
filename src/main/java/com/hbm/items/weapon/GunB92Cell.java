package com.hbm.items.weapon;

import dev.golem.core.data.Tags;

import com.hbm.items.ModDataComponents;
import com.hbm.items.ModGunItems;
import dev.golem.core.world.CompatItem;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The reusable B92 cell. It siphons charge from a carried B92 and becomes a bomb at 25 charges. */
public final class GunB92Cell extends CompatItem {

    private static final String ENERGY = "b92_cell_energy";

    public GunB92Cell(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
                                       Entity owner, EquipmentSlot slot, boolean held) {
        if (!(owner instanceof Player player) || getPower(stack) >= 25) return;

        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            ItemStack gun = player.getInventory().getItem(slotIndex);
            if (gun.getItem() != ModGunItems.gun_b92.get()) continue;

            int power = GunB92.getPower(gun);
            if (power > 1) {
                GunB92.setPower(gun, power - 1);
                int cellPower = getPower(stack) + 1;
                setPower(stack, cellPower);
                if (cellPower == 25) ModDataComponents.ITEM_VARIANT.set(stack, 1);
                return;
            }
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, dev.golem.core.world.CompatTooltipContext context,
                                         List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.gun_b92_ammo.desc.0"));
        tooltip.add(Component.translatable("item.hbm.gun_b92_ammo.desc.1"));
        tooltip.add(Component.translatable("item.hbm.gun_b92_ammo.desc.2"));
        tooltip.add(Component.translatable("item.hbm.gun_b92_ammo.desc.3"));
        tooltip.add(Component.translatable("item.hbm.gun_b92_ammo.charges", getPower(stack)));
    }

    public static int getPower(ItemStack stack) {
        CompoundTag tag = ModDataComponents.GUN_STATE.getOr(stack, new CompoundTag());
        return dev.golem.core.data.Tags.getIntOr(tag, ENERGY, 0);
    }

    public static void setPower(ItemStack stack, int value) {
        CompoundTag current = ModDataComponents.GUN_STATE.getOr(stack, new CompoundTag());
        CompoundTag next = current.copy();
        next.putInt(ENERGY, value);
        ModDataComponents.GUN_STATE.set(stack, next);
    }

    public static ItemStack getFullCell() {
        ItemStack stack = new ItemStack(ModGunItems.gun_b92_ammo.get());
        setPower(stack, 25);
        ModDataComponents.ITEM_VARIANT.set(stack, 1);
        return stack;
    }

    /** Shared test/gameplay seam for the source cell tick without spelling the era-specific Item API. */
    public void tickCell(ItemStack stack, net.minecraft.server.level.ServerLevel level, Player player) {
        compatInventoryTick(stack, level, player, EquipmentSlot.MAINHAND, true);
    }
}
