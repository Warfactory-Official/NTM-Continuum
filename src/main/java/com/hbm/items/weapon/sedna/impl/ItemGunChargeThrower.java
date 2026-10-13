package com.hbm.items.weapon.sedna.impl;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.JetpackHandler;
import com.hbm.items.weapon.sedna.GunConfig;
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
            GunConfig... configs) {
        super(properties, quality, configs);
    }

    /**
     * The last hook's UUID, or null. 1.7 kept the hook's network entity id; the port keeps its UUID so no numeric
     * id is persisted in the stack. An old int value reads as no hook.
     */
    public static java.util.@org.jspecify.annotations.Nullable UUID getLastHook(ItemStack stack) {
        String id = getValueString(stack, KEY_LASTHOOK);
        if (id.isEmpty()) return null;
        try {
            return java.util.UUID.fromString(id);
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }
    public static void setLastHook(ItemStack stack, Entity hook) { setValueString(stack, KEY_LASTHOOK, hook.getUUID().toString()); }
    /** 1.7 setLastHook(stack, -1). */
    public static void clearLastHook(ItemStack stack) { setValueString(stack, KEY_LASTHOOK, ""); }

    /** The source rope step, driven by physical client input and using the synchronized hook entity. */
    public static void clientGrappleTick(Player player, @org.jspecify.annotations.Nullable Entity entity,
            boolean primary, boolean secondary) {
        if (player == null) return;
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
        if (getState(stack, 0) == GunState.RELOADING) clearLastHook(stack);
        if (slot != EquipmentSlot.MAINHAND || !(entity instanceof Player player)) {
            clearLastHook(stack);
            return;
        }

        java.util.UUID hookId = getLastHook(stack);
        Entity hook = hookId == null ? null : level.getEntity(hookId);
        if (!(hook instanceof EntityBulletBaseMK4 bullet) || !bullet.isAlive()
                || bullet.config != XFactoryTool.ct_hook || bullet.velocity >= 0.01D) return;
        if (getPrimary(stack, 0)
                && hook.position().subtract(player.getX(), player.getY() + player.getEyeHeight(), player.getZ()).length() < 2D) {
            hook.discard();
        }
    }
}
