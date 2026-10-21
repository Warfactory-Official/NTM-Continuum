package com.hbm.items.armor;

import com.hbm.api.environment.Environment;
import com.hbm.handler.JetpackHandler;
import com.hbm.items.gear.ArmorFSB;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Direct-wear Murk wings and limp-wing glider, preserving their separate old flight rules. */
public class WingsMurk extends JetpackBase {

    public final boolean murk;

    public WingsMurk(boolean murk, Properties properties) {
        super(properties);
        this.murk = murk;
    }

    @Override
    protected void tickFlight(Level level, Player player, ItemStack stack) {
        if (player.onGround()) return;
        JetpackHandler.resetFlightTime(player);
        player.fallDistance = 0;
        Vec3 motion = player.getDeltaMovement();

        if (!murk) {
            if (motion.y < -0.4D) motion = new Vec3(motion.x, -0.4D, motion.z);
            if (player.isShiftKeyDown() && motion.y < -0.08D) {
                double amount = motion.y * -0.2D;
                motion = motion.add(0, amount, 0).add(player.getViewVector(1.0F).scale(amount));
            }
            player.setDeltaMovement(motion);
            return;
        }

        if (JetpackHandler.isJetpackActive(player)) {
            if (player.isShiftKeyDown()) {
                double y = motion.y;
                if (y < -1D) y += 0.4D;
                else if (y < -0.1D) y += 0.2D;
                else if (y < 0D) y = 0D;
                else if (y > 1D) y -= 0.4D;
                else if (y > 0.1D) y -= 0.2D;
                else if (y > 0D) y = 0D;
                motion = new Vec3(motion.x, y, motion.z);
            } else if (motion.y < 0.6D) {
                motion = motion.add(0, 0.2D, 0);
            } else {
                motion = new Vec3(motion.x, 0.8D, motion.z);
            }
        } else if (JetpackHandler.isBackpackEnabled(player) && !player.isShiftKeyDown()) {
            double y = motion.y;
            if (y < -1D) y += 0.4D;
            else if (y < -0.1D) y += 0.2D;
            else if (y < 0D) y = 0D;
            motion = new Vec3(motion.x, y, motion.z);
        } else if (JetpackHandler.isBackpackEnabled(player)) {
            // hbm-space: sneaking with the wings spread in low gravity replaces the planet's pull with Earth's-like
            // glide, at least 0.2 (its gravity units, Environment.EARTH_GRAVITY = 1.6) unless in free fall. Above 1.5
            // hbm-space returned from the whole tick, which also dropped 1.7's steering below on Earth; here the
            // branch only does nothing there, so plain HBM keeps 1.7 (com.hbm.api.environment.Environment.gravity).
            float gravity = (float) (Environment.gravity(player)
                    * Environment.EARTH_GRAVITY);
            if (gravity <= 1.5F) {
                if (gravity != 0 && gravity < 0.2F) gravity = 0.2F;
                double y = motion.y;
                y /= 0.98F;
                y += gravity / 20F;
                y -= Environment.EARTH_GRAVITY / 20F;
                y *= 0.98F;
                motion = new Vec3(motion.x, y, motion.z);
            }
        }

        if (JetpackHandler.isBackpackEnabled(player)) {
            Vec3 look = player.getViewVector(1.0F);
            Vec3 horizontal = new Vec3(look.x, 0, look.z);
            if (horizontal.lengthSqr() > 0D) horizontal = horizontal.normalize();
            double modifier = player.isSprinting() ? 1D : 0.25D;
            float forward = JetpackHandler.getForwardInput(player);
            float strafe = JetpackHandler.getStrafeInput(player);
            if (forward != 0F) motion = motion.add(horizontal.scale(0.35D * forward * modifier));
            if (strafe != 0F) motion = motion.add(new Vec3(horizontal.z, 0, -horizontal.x)
                    .scale(0.15D * strafe * modifier));
        }
        player.setDeltaMovement(motion);
    }
}
