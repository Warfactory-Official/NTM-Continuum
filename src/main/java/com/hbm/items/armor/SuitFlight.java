package com.hbm.items.armor;

import com.hbm.api.environment.Environment;
import com.hbm.handler.JetpackHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** DNS glide and exhaust rules, from the Space ArmorDNT armour tick. */
public final class SuitFlight {

    private SuitFlight() {}

    public static Vec3 dnsGlide(Player player, Vec3 motion, boolean backpack, float forward, float strafe,
                                float gravity) {
        double x = motion.x;
        double y = motion.y;
        double z = motion.z;
        if (!player.isShiftKeyDown() && !player.onGround() && backpack) {
            player.fallDistance = 0;
            y = y < -1D ? y + 0.4D : y < -0.1D ? y + 0.2D : y > 0.1D ? y - 0.2D : y < 0D ? 0D : y;
            if (forward != 0F || strafe != 0F) {
                x *= 1.05D;
                z *= 1.05D;
            }
            if (forward != 0F && player.isSprinting()) {
                var look = player.getLookAngle();
                x += look.x * 0.25D * forward;
                z += look.z * 0.25D * forward;
            }
        }
        if (player.isShiftKeyDown() && !player.onGround() && (gravity > 0F || backpack)) y -= 0.1D;
        return new Vec3(x, y, z);
    }

    public static boolean dnsShowsThrust(ServerPlayer player, boolean backpack) {
        return shows(player.isShiftKeyDown(), player.onGround(), backpack,
                JetpackHandler.getForwardInput(player) != 0F || JetpackHandler.getStrafeInput(player) != 0F,
                player.isSprinting(), (float) Environment.gravity(player));
    }

    public static boolean shows(boolean sneaking, boolean onGround, boolean backpack, boolean moving,
                                boolean sprinting, float gravity) {
        if (onGround || !backpack) return false;
        if (sneaking) return gravity == 0F;
        return moving || gravity > 0F || sprinting;
    }
}
