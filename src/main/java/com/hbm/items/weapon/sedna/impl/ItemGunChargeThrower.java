package com.hbm.items.weapon.sedna.impl;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.JetpackHandler;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ItemGunChargeThrower extends ItemGunBaseNT {
    public static final String KEY_LASTHOOK = "lasthook";

    public ItemGunChargeThrower(Properties properties, WeaponQuality quality,
            com.hbm.items.weapon.sedna.GunConfig... configs) {
        super(properties, quality, configs);
    }

    public static int getLastHook(ItemStack stack) { return getValueInt(stack, KEY_LASTHOOK); }
    public static void setLastHook(ItemStack stack, int value) { setValueInt(stack, KEY_LASTHOOK, value); }

    /** The source rope step, driven by physical client input and using the synchronized hook entity. */
    public static void clientGrappleTick(Player player, ItemStack stack, boolean primary, boolean secondary) {
        if (player == null) return;
        Entity entity = player.level().getEntity(getLastHook(stack));
        if (!(entity instanceof EntityBulletBaseMK4 hook) || !hook.isAlive()
                || hook.config != XFactoryTool.ct_hook || hook.velocity >= 0.01D) return;

        Vec3 eye = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        Vec3 vector = hook.position().subtract(eye);
        double line = vector.length();
        if (primary) {
            Vec3 pull = vector.normalize().scale(0.1D);
            player.setDeltaMovement(player.getDeltaMovement().add(pull.x, pull.y + 0.04D, pull.z));
        } else if (!secondary) {
            Vec3 motion = player.getDeltaMovement();
            Vec3 next = new Vec3(player.getX() + motion.x,
                    player.getY() + player.getEyeHeight() + motion.y, player.getZ() + motion.z);
            Vec3 delta = hook.position().subtract(next);
            if (delta.length() > line) {
                Vec3 clamped = delta.normalize().scale(line);
                Vec3 newNext = hook.position().subtract(clamped);
                Vec3 velocity = newNext.subtract(eye);
                if (velocity.length() < 3D) player.setDeltaMovement(velocity);
            }
        } else {
            player.setDeltaMovement(player.getDeltaMovement().scale(0.5D));
        }

        if (player.getDeltaMovement().y > -0.1D) player.fallDistance = 0F;
        JetpackHandler.resetFlightTime(player);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity entity,
            EquipmentSlot slot, boolean held) {
        super.compatInventoryTick(stack, level, entity, slot, held);
        if (getState(stack, 0) == GunState.RELOADING) setLastHook(stack, -1);
        if (slot != EquipmentSlot.MAINHAND || !(entity instanceof Player player)) {
            setLastHook(stack, -1);
            return;
        }

        Entity hook = level.getEntity(getLastHook(stack));
        if (!(hook instanceof EntityBulletBaseMK4 bullet) || !bullet.isAlive()
                || bullet.config != XFactoryTool.ct_hook || bullet.velocity >= 0.01D) return;
        if (getPrimary(stack, 0)
                && hook.position().subtract(player.getX(), player.getY() + player.getEyeHeight(), player.getZ()).length() < 2D) {
            hook.discard();
        }
    }
}
