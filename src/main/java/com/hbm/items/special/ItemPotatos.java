package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemBattery;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ItemPotatos} (PotatOS): a battery that talks. While it holds any charge and is the held item, a
 * countdown (the stack's {@code timer}) runs out every 200-299 ticks and plays {@code potatos.random}, pitched up
 * with the remaining charge (0.5 empty .. 1.0 full). The timer only counts while a charge remains.
 */
public class ItemPotatos extends ItemBattery {

    public ItemPotatos(Properties properties, long maxCharge, long chargeRate, long dischargeRate) {
        super(properties, maxCharge, chargeRate, dischargeRate);
    }

    /** The countdown rewrites the stack every tick; that is not a new item for the held-item animation. */
    @Override
    public boolean compatIsSameHeldItem(ItemStack shown, ItemStack held) {
        return shown.getItem() == held.getItem();
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot,
                                       boolean held) {
        if (getCharge(stack) == 0) return;

        int timer = ModDataComponents.POTATOS_TIMER.getOr(stack, 0);
        if (timer > 0) {
            ModDataComponents.POTATOS_TIMER.set(stack, timer - 1);
        } else if (owner instanceof Player player && player.getMainHandItem() == stack) {
            float pitch = (float) getCharge(stack) / (float) getMaxCharge(stack) * 0.5F + 0.5F;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POTATO_RANDOM.get(),
                    SoundSource.PLAYERS, 1.0F, pitch);
            ModDataComponents.POTATOS_TIMER.set(stack, 200 + level.getRandom().nextInt(100));
        }
    }
}
