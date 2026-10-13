package com.hbm.items.weapon.sedna.impl;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Stinger's server-side lock-on state and target selection. */
public class ItemGunStinger extends ItemGunBaseNT {
    public static final String KEY_LOCKINGON = "lockingon";
    public static final String KEY_LOCKONPROGRESS = "lockonprogress";

    public ItemGunStinger(Properties properties, WeaponQuality quality, GunConfig... configs) {
        super(properties, quality, configs);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level,
            Entity entity, EquipmentSlot slot, boolean held) {
        super.compatInventoryTick(stack, level, entity, slot, held);
        if (!(entity instanceof Player player)) return;
        if (!held) {
            setIsLockingOn(stack, false);
            return;
        }
        if (!getIsLockingOn(stack) || !getIsAiming(stack)
                || getPrimaryMagazine(stack, 0) == null
                || getPrimaryMagazine(stack, 0).getAmount(stack, player.getInventory()) <= 0) {
            resetLockon(stack);
            return;
        }
        String previous = getLockonTarget(stack);
        Entity target = getLockonTarget(player, 150D, 10D);
        if (target == null) {
            if (!getIsLockedOn(stack)) resetLockon(stack);
            return;
        }
        if (!getIsLockedOn(stack) && !target.getUUID().toString().equals(previous)) {
            resetLockon(stack);
            setLockonTarget(stack, target);
        }
        setLockonProgress(stack, Math.min(60, getLockonProgress(stack) + 1));
        if (getLockonProgress(stack) >= 60 && !getIsLockedOn(stack)) {
            // 1.7: playSoundAtEntity(player, TECH_BLEEP, 1, 1) once, when the lock is acquired
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS, 1F, 1F);
            setIsLockedOn(stack, true);
        }
    }

    /** 1.7 returned the closest entity's network id (or -1); the port returns the entity itself (or null). */
    public static @org.jspecify.annotations.Nullable Entity getLockonTarget(Player player, double distance, double angleThreshold) {
        if (player == null) return null;
        Vec3 origin = player.getEyePosition();
        Vec3 forward = player.getViewVector(1F).normalize();
        AABB bounds = player.getBoundingBox().expandTowards(forward.scale(distance)).inflate(10D);
        Entity closest = null;
        double closestAngle = angleThreshold;
        for (Entity entity : player.level().getEntities(player, bounds,
                candidate -> candidate.isPickable() && candidate.getBbHeight() >= 0.5F)) {
            Vec3 delta = new Vec3(entity.getX() - origin.x,
                    entity.getY() + entity.getBbHeight() / 2D - origin.y, entity.getZ() - origin.z);
            double angle = Math.toDegrees(Math.acos(Math.max(-1D, Math.min(1D,
                    forward.dot(delta.normalize())))));
            if (angle < closestAngle) {
                closestAngle = angle;
                closest = entity;
            }
        }
        return closest;
    }

    public static void resetLockon(ItemStack stack) {
        setLockonProgress(stack, 0);
        setIsLockedOn(stack, false);
    }

    public static boolean getIsLockingOn(ItemStack stack) { return getValueBool(stack, KEY_LOCKINGON); }
    public static void setIsLockingOn(ItemStack stack, boolean value) { setValueBool(stack, KEY_LOCKINGON, value); }
    public static int getLockonProgress(ItemStack stack) { return getValueInt(stack, KEY_LOCKONPROGRESS); }
    public static void setLockonProgress(ItemStack stack, int value) { setValueInt(stack, KEY_LOCKONPROGRESS, value); }
}
