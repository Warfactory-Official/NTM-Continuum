package com.hbm.handler;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import com.hbm.items.armor.ItemModDefuser;
import com.hbm.items.tool.ItemDropDetonator;

/**
 * The per-entity server tick the loaders forward for this family: the two dropped detonators
 * (1.7 {@code ItemDrop.onEntityItemUpdate}), obsidian-skinned dropped armor, and the creepers the golden
 * wire cutter defused (its module update itself runs from {@code ArmorModEvents}).
 */
public final class ExplosivesEntityHooks {
    private ExplosivesEntityHooks() {}

    public static void tick(Entity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof ItemEntity item) {
            ItemDropDetonator.tickDropped(item);
            com.hbm.items.weapon.WeaponizedCell.tickDropped(item); // 1.7 WeaponizedCell.onEntityItemUpdate
            com.hbm.items.armor.ItemModObsidian.protectDropped(item);
            com.hbm.items.armor.ArmorHat.tickDropped(item); // 1.7 ArmorHat.onEntityItemUpdate: setDead
            com.hbm.items.special.ItemDrop.tickDropped(item); // 1.7 ItemDrop.onEntityItemUpdate: beta setDead
            com.hbm.items.special.ItemRag.tickDropped(item); // 1.7 ItemRag.onEntityItemUpdate: cloth in water -> damp
            return;
        }
        if (entity instanceof Creeper creeper) {
            ItemModDefuser.tickDefused(creeper);
        }
        // The wire cutter's own modUpdate now runs from ArmorModEvents with every other armor module.
    }
}
