package com.hbm.handler;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import com.hbm.items.armor.ItemModDefuser;
import com.hbm.items.tool.ItemDropDetonator;

/**
 * The per-entity server tick the loaders forward for this family: the two dropped detonators
 * (1.7 {@code ItemDrop.onEntityItemUpdate}), the golden wire cutter's armor-module update (1.7
 * {@code ItemArmorMod.modUpdate}, which no other module in this port uses yet) and the defused
 * creepers it leaves behind.
 */
public final class ExplosivesEntityHooks {
    private ExplosivesEntityHooks() {}

    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    public static void tick(Entity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof ItemEntity item) {
            ItemDropDetonator.tickDropped(item);
            return;
        }
        if (entity instanceof Creeper creeper) {
            ItemModDefuser.tickDefused(creeper);
            return;
        }
        if (entity instanceof LivingEntity living && living.level().getGameTime() % 20 == 0) {
            for (EquipmentSlot slot : ARMOR) {
                ItemStack armor = living.getItemBySlot(slot);
                if (armor.isEmpty() || !ArmorModHandler.hasMods(armor)) continue;
                ItemStack mod = ArmorModHandler.pryMod(armor, ArmorModHandler.extra);
                if (mod.getItem() instanceof ItemModDefuser defuser) defuser.modUpdate(living, armor);
            }
        }
    }
}
