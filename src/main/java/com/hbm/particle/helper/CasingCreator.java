package com.hbm.particle.helper;

import com.hbm.packet.CasingEjectPayload;
import com.hbm.packet.GunCasingEjectPayload;
import com.hbm.packet.HbmPackets;
import com.hbm.particle.SpentCasing;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Source-shaped server casing emitter; payload construction stays on the network-owned codec types. */
public final class CasingCreator {
    private static volatile Sender sender = CasingCreator::sendNetwork;

    private CasingCreator() {}

    @FunctionalInterface
    public interface Sender {
        void send(Level level, Event event);
    }

    /**
     * Resolves a casing without imposing a bullet type on a heterogeneous magazine.
     *
     * <p>The old receiver signature is necessarily generic, but engine/drill magazines return a
     * {@code FluidType}. Calling that signature through {@code IMagazine<BulletConfig>} inserts a
     * compiler cast at the call site. Keep the value wildcarded until the runtime type check.
     */
    public static SpentCasing getCasing(IMagazine<?> magazine, ItemStack stack, Container inventory) {
        if (magazine == null) return null;
        Object loaded = magazine.getType(stack, inventory);
        return loaded instanceof BulletConfig bullet ? bullet.casing : null;
    }

    /** Exact source CasingEjectPayload fields, plus the gun discriminator and legacy-mode bit. */
    public record Event(double x, double y, double z, double mX, double mY, double mZ, float yaw, float pitch,
            float mPitch, float mYaw, String casing, boolean smoking, int smokeLife, double smokeLift, int nodeLife,
            boolean gun, boolean legacy) {}

    /** Network registration seam; the network owner maps this event to its exact payload type and fields. */
    public static void setSender(Sender value) { sender = value == null ? CasingCreator::sendNetwork : value; }

    private static void sendNetwork(Level level, Event event) {
        if (!(level instanceof ServerLevel server)) return;
        if (event.gun()) {
            HbmPackets.sendGunCasing(server, new GunCasingEjectPayload(event.x(), event.y(), event.z(), event.mX(),
                    event.mY(), event.mZ(), event.yaw(), event.pitch(), event.mPitch(), event.mYaw(), event.casing(),
                    event.smoking(), event.smokeLife(), event.smokeLift(), event.nodeLife(), event.legacy()));
        } else {
            HbmPackets.sendCasing(server, new CasingEjectPayload(event.x(), event.y(), event.z(), event.mX(),
                    event.mY(), event.mZ(), event.yaw(), event.pitch(), event.mPitch(), event.mYaw(), event.casing(),
                    event.smoking(), event.smokeLife(), event.smokeLift(), event.nodeLife()));
        }
    }

    public static void composeEffect(Level level, LivingEntity player, double frontOffset, double heightOffset,
            double sideOffset, double frontMotion, double heightMotion, double sideMotion, double motionVariance,
            String casing) {
        composeEffect(level, player, frontOffset, heightOffset, sideOffset, frontMotion, heightMotion, sideMotion,
                motionVariance, 5F, 10F, casing, false, 0, 0D, 0);
    }

    public static void composeEffect(Level level, double x, double y, double z, float yaw, float pitch,
            double frontMotion, double heightMotion, double sideMotion, double motionVariance,
            float multPitch, float multYaw, String casing, boolean smoking, int smokeLife, double smokeLift,
            int nodeLife) {
        if (!(level instanceof ServerLevel)) return;
        Vec3 motion = new Vec3(sideMotion, heightMotion, frontMotion)
                .xRot(-pitch / 180F * (float) Math.PI)
                .yRot(-yaw / 180F * (float) Math.PI);
        sender.send(level, new Event(x, y, z,
                motion.x + level.getRandom().nextGaussian() * motionVariance,
                motion.y + level.getRandom().nextGaussian() * motionVariance,
                motion.z + level.getRandom().nextGaussian() * motionVariance,
                yaw, pitch, multPitch, multYaw, casing, smoking, smokeLife, smokeLift, nodeLife, false, false));
    }

    /** Eject a registered source casing using the smoke profile attached to that casing. */
    public static void composeEffect(Level level, double x, double y, double z, float yaw, float pitch,
            double frontMotion, double heightMotion, double sideMotion, double motionVariance,
            float multPitch, float multYaw, SpentCasing casing) {
        if (casing == null || casing.getName() == null) return;
        composeEffect(level, x, y, z, yaw, pitch, frontMotion, heightMotion, sideMotion, motionVariance,
                multPitch, multYaw, casing.getName(), casing.getSmokeChance() > 0F,
                casing.getSmokeDuration(), casing.getSmokeLift(), casing.getSmokeNodeLife());
    }

    public static void composeEffect(Level level, LivingEntity player, double frontOffset, double heightOffset,
            double sideOffset, double frontMotion, double heightMotion, double sideMotion, double motionVariance,
            float multPitch, float multYaw, String casing) {
        composeEffect(level, player, frontOffset, heightOffset, sideOffset, frontMotion, heightMotion, sideMotion,
                motionVariance, multPitch, multYaw, casing, false, 0, 0D, 0);
    }

    /** Player-relative form using the registered casing's source smoke profile. */
    public static void composeEffect(Level level, LivingEntity player, double frontOffset, double heightOffset,
            double sideOffset, double frontMotion, double heightMotion, double sideMotion, double motionVariance,
            float multPitch, float multYaw, SpentCasing casing) {
        if (casing == null || casing.getName() == null) return;
        composeEffect(level, player, frontOffset, heightOffset, sideOffset, frontMotion, heightMotion, sideMotion,
                motionVariance, multPitch, multYaw, casing.getName(), casing.getSmokeChance() > 0F,
                casing.getSmokeDuration(), casing.getSmokeLift(), casing.getSmokeNodeLife());
    }

    public static void composeEffect(Level level, LivingEntity player, double frontOffset, double heightOffset,
            double sideOffset, double frontMotion, double heightMotion, double sideMotion, double motionVariance,
            float multPitch, float multYaw, String casing, boolean smoking, int smokeLife, double smokeLift,
            int nodeLife) {
        composeEffect(level, player, frontOffset, heightOffset, sideOffset, frontMotion, heightMotion, sideMotion,
                motionVariance, multPitch, multYaw, casing, smoking, smokeLife, smokeLift, nodeLife, frontOffset);
    }

    public static void composeEffect(Level level, LivingEntity player, double frontOffset, double heightOffset,
            double sideOffset, double frontMotion, double heightMotion, double sideMotion, double motionVariance,
            float multPitch, float multYaw, String casing, boolean smoking, int smokeLife, double smokeLift,
            int nodeLife, double legacyFrontOffset) {
        if (!(level instanceof ServerLevel)) return;
        if (player.isShiftKeyDown()) heightOffset -= 0.075F;
        Vec3 offset = new Vec3(sideOffset, heightOffset, frontOffset)
                .xRot(-player.getXRot() / 180F * (float) Math.PI)
                .yRot(-player.getYRot() / 180F * (float) Math.PI);
        double x = player.getX() + offset.x;
        double y = player.getY() + player.getEyeHeight() + offset.y;
        double z = player.getZ() + offset.z;
        Vec3 motion = new Vec3(sideMotion, heightMotion, frontMotion)
                .xRot(-player.getXRot() / 180F * (float) Math.PI)
                .yRot(-player.getYRot() / 180F * (float) Math.PI);
        double mX = player.getDeltaMovement().x + motion.x + player.getRandom().nextGaussian() * motionVariance;
        double mY = player.getDeltaMovement().y + motion.y + player.getRandom().nextGaussian() * motionVariance;
        double mZ = player.getDeltaMovement().z + motion.z + player.getRandom().nextGaussian() * motionVariance;
        if (player instanceof Player p && p.getAbilities().flying) mY -= 0.04D;
        if (legacyFrontOffset == frontOffset) {
            sender.send(level, new Event(x, y, z, mX, mY, mZ, player.getYRot(), player.getXRot(), multPitch, multYaw,
                    casing, smoking, smokeLife, smokeLift, nodeLife, false, false));
            return;
        }
        sender.send(level, new Event(x, y, z, mX, mY, mZ, player.getYRot(), player.getXRot(), multPitch, multYaw,
                casing, smoking, smokeLife, smokeLift, nodeLife, true, false));
        Vec3 legacyOffset = new Vec3(sideOffset, heightOffset, legacyFrontOffset)
                .xRot(-player.getXRot() / 180F * (float) Math.PI)
                .yRot(-player.getYRot() / 180F * (float) Math.PI);
        sender.send(level, new Event(player.getX() + legacyOffset.x, player.getY() + player.getEyeHeight() + legacyOffset.y,
                player.getZ() + legacyOffset.z, mX, mY, mZ, player.getYRot(), player.getXRot(), multPitch, multYaw,
                casing, smoking, smokeLife, smokeLift, nodeLife, true, true));
    }
}
