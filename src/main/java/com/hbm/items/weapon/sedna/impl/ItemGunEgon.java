package com.hbm.items.weapon.sedna.impl;

import com.hbm.items.weapon.MobSlicer;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.lib.ModDamageSources;
import com.hbm.packet.SpecialDeathPayload;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.Compat;
import dev.golem.core.world.TimeOfDay;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 1.12.2 {@code ItemGunEgon}, the gluon gun: while the trigger is held and at least two rounds are in the inventory the
 * beam is on. Every fifth game tick takes two rounds; every tick the beam, wobbling by
 * {@link #getBeamDirectionOffset}, takes up to 2 health straight off the first living thing it meets within 50 blocks.
 *
 * <p>The gun never fires a round through the receiver: its config has no primary press handler, so the trigger only
 * sets the primary flag this class reads.
 */
public class ItemGunEgon extends ItemGunBaseNT {

    public static final String KEY_FIRING = "egonfiring";
    public static final double RANGE = 50D;

    public ItemGunEgon(Properties properties, WeaponQuality quality, GunConfig... configs) {
        super(properties, quality, configs);
    }

    /** The receiver never fires, so its base damage is 0; the beam's own damage instead. */
    @Override
    protected void addDamageInfo(ItemStack stack, Receiver receiver, IMagazine<?> magazine, Container inventory,
                                 List<Component> tooltip) {
        tooltip.add(Component.translatable("gui.weapon.egonDamage"));
    }

    @Override
    public void serverTick(ItemStack stack, ServerLevel level, Entity entity, boolean isHeld, InteractionHand hand) {
        super.serverTick(stack, level, entity, isHeld, hand);
        if (!isHeld || hand != InteractionHand.MAIN_HAND || !(entity instanceof Player player)) return;

        IMagazine<?> mag = getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        if (getPrimary(stack, 0) && mag.getAmount(stack, player.getInventory()) >= 2) {
            setIsFiring(stack, true);
            long time = TimeOfDay.get(level);
            // port: 1.12 counted on the day clock, so a stopped daylight cycle took ammo every tick or never
            if (level.getGameTime() % 5 == 0) mag.useUpAmmo(stack, player.getInventory(), 2);
            float[] angles = getBeamDirectionOffset(time + 1);
            Vec3 look = changeByAngle(player.getViewVector(1F), angles[0], angles[1]);
            Hit hit = trace(level, player, look, 1F);
            if (hit != null && hit.entity() instanceof LivingEntity living) {
                if (living instanceof Player target && target.isCreative()) return;
                burn(level, living);
            }
        } else {
            setIsFiring(stack, false);
        }
    }

    private static void burn(ServerLevel level, LivingEntity living) {
        DamageSource source = ModDamageSources.source(level, ModDamageSources.GLUON);
        float damage = Math.min(living.getHealth(), 2F);
        living.getCombatTracker().recordDamage(source, damage);
        living.setHealth(living.getHealth() - damage);
        // the tracker list never held the entity itself, so a struck player is told separately
        MobSlicer.sendToTrackers(level, living, new SpecialDeathPayload(living.getId(), SpecialDeathPayload.GLUON_HIT, new float[0]));
        if (living.getHealth() > 0) return;
        MobSlicer.sendToTrackers(level, living, new SpecialDeathPayload(living.getId(), SpecialDeathPayload.GLUON, new float[0]));
        // setDead(), onDeath(gluon), then onKillCommand(), which only did something for the dragon
        living.die(source);
        if (living instanceof Player) return;
        if (living instanceof EnderDragon dragon) {
            Compat.kill(dragon, level);
            return;
        }
        living.discard();
    }

    public static void setIsFiring(ItemStack stack, boolean firing) {
        setValueBool(stack, KEY_FIRING, firing);
    }

    public static boolean getIsFiring(ItemStack stack) {
        return getValueBool(stack, KEY_FIRING);
    }

    /** Yaw and pitch, in degrees, by which the beam strays from the look direction at {@code time} (day ticks). */
    public static float[] getBeamDirectionOffset(float time) {
        float sinval = Mth.sin(time * 1.2F) + Mth.sin(time * 0.8F - 10) + Mth.sin(time * 1.0F + 10);
        sinval /= 3;
        float sinval2 = Mth.sin(time * 0.6F) + Mth.sin(time * 0.2F + 20) + Mth.sin(time * 0.1F + 20);
        // 1.12 divides sinval a second time and never sinval2: the yaw spans -1..1 degrees, the pitch -1.5..1.5
        sinval /= 3;
        return new float[] {remap(sinval, -1, 1, -3, 3), remap(sinval2, -1, 1, -0.5F, 0.5F)};
    }

    public static float remap(float num, float min1, float max1, float min2, float max2) {
        return (num - min1) / (max1 - min1) * (max2 - min2) + min2;
    }

    /** 1.12.2 {@code Library.changeByAngle}: (0, 0, 1) turned by the offset, then turned onto {@code oldDir}. */
    public static Vec3 changeByAngle(Vec3 oldDir, float yaw, float pitch) {
        Vec3 dir = new Vec3(0, 0, 1).xRot((float) Math.toRadians(pitch)).yRot((float) Math.toRadians(yaw));
        double angleYaw = Math.toDegrees(Math.atan2(oldDir.x, oldDir.z));
        double anglePitch = Math.toDegrees(Math.atan2(oldDir.y, Math.sqrt(oldDir.x * oldDir.x + oldDir.z * oldDir.z))) - 90;
        return dir.xRot((float) Math.toRadians(anglePitch + 90)).yRot((float) Math.toRadians(angleYaw));
    }

    /** What the beam met: the point, the face it entered by, and the living entity if it was one. */
    public record Hit(Vec3 location, Direction side, @Nullable Entity entity) {}

    /**
     * 1.12.2 {@code Library.rayTraceIncludeEntitiesCustomDirection(player, look, 50, partialTicks)}: blocks by their
     * collision shape, then any alive living entity whose box grown by 0.3 the shortened segment crosses, nearest
     * first; null when nothing was hit.
     */
    public static @Nullable Hit trace(Level level, Player player, Vec3 look, float partialTicks) {
        Vec3 start = player.getEyePosition(partialTicks);
        Vec3 end = start.add(look.scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Hit result = null;
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
            result = new Hit(end, block.getDirection(), null);
        }
        AABB area = new AABB(start, end).inflate(1D);
        for (Entity entity : level.getEntities(player, area, e -> e instanceof LivingEntity living && living.isAlive())) {
            AABB box = entity.getBoundingBox().inflate(0.3D);
            Optional<Vec3> crossing = box.clip(start, end);
            if (crossing.isEmpty()) continue;
            Vec3 at = crossing.get();
            if (result == null || start.distanceToSqr(result.location()) > start.distanceToSqr(at)) {
                result = new Hit(at, side(box, at), entity);
            }
        }
        return result;
    }

    /** The face of {@code box} that {@code at}, a point on its surface, lies on. */
    private static Direction side(AABB box, Vec3 at) {
        double eps = 1.0E-7D;
        if (Math.abs(at.x - box.minX) < eps) return Direction.WEST;
        if (Math.abs(at.x - box.maxX) < eps) return Direction.EAST;
        if (Math.abs(at.y - box.minY) < eps) return Direction.DOWN;
        if (Math.abs(at.y - box.maxY) < eps) return Direction.UP;
        if (Math.abs(at.z - box.minZ) < eps) return Direction.NORTH;
        return Direction.SOUTH;
    }

    /** Whether {@code player} has the two rounds a beam tick needs. */
    public boolean hasBeamAmmo(ItemStack stack, Player player) {
        return getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, player.getInventory()) >= 2;
    }

    /** Whether {@code player}'s beam is on as the server sees it, for players other than the local one. */
    public static boolean isFiring(Player player) {
        ItemStack held = player.getMainHandItem();
        return held.getItem() instanceof ItemGunEgon && getIsFiring(held);
    }
}
