package com.hbm.items.weapon;

import dev.golem.core.data.Tags;

import com.hbm.items.ModDataComponents;
import com.hbm.items.ModGunItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** The reusable B92 cell. It siphons charge from a carried B92 and becomes a bomb at 25 charges. */
public final class GunB92Cell extends CompatItem {

    public GunB92Cell(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level,
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
                return;
            }
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.gun_b92_ammo.desc.0"));
        tooltip.add(Component.translatable("item.hbm.gun_b92_ammo.desc.1"));
        tooltip.add(Component.translatable("item.hbm.gun_b92_ammo.desc.2"));
        tooltip.add(Component.translatable("item.hbm.gun_b92_ammo.desc.3"));
        tooltip.add(Component.translatable("item.hbm.gun_b92_ammo.charges", getPower(stack)));
    }

    /**
     * The cell's charge, {@code hbm:b92_energy} (ntm-next's component). 1.7 marked the full cell (25) with metadata 1
     * as well; the port matches the full charge itself instead of a mirrored number.
     */
    public static int getPower(ItemStack stack) {
        return ModDataComponents.B92_ENERGY.getOr(stack, 0);
    }

    public static void setPower(ItemStack stack, int value) {
        ModDataComponents.B92_ENERGY.set(stack, value);
    }

    public static ItemStack getFullCell() {
        ItemStack stack = new ItemStack(ModGunItems.gun_b92_ammo.get());
        setPower(stack, 25);
        return stack;
    }

    /** Shared test/gameplay seam for the source cell tick without spelling the era-specific Item API. */
    public void tickCell(ItemStack stack, ServerLevel level, Player player) {
        compatInventoryTick(stack, level, player, EquipmentSlot.MAINHAND, true);
    }
}
