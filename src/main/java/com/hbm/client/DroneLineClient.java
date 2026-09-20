package com.hbm.client;

import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.Compat;
import net.minecraft.world.level.Level;

import java.util.function.BooleanSupplier;
import com.hbm.packet.DroneLinePayload;

/**
 * The client half of 1.7's {@code debugdrone} effect. 1.7 drew a {@code ParticleDebugLine} between a node and
 * its link, but only while the player held one of the drone family's items; here the line is a run of dust
 * particles along the same segment, under the same held-item gate, which the client registrations supply.
 * Server-safe: nothing here names a client class.
 */
public final class DroneLineClient {

    /** Whether the local player is holding a drone-family item; installed by the client registrations. */
    public static BooleanSupplier holdsDroneItem = () -> false;

    private static final double STEP = 0.5D;

    private DroneLineClient() {}

    public static void show(Level level, double x, double y, double z, double dx, double dy, double dz, int color) {
        if (!level.isClientSide() || !holdsDroneItem.getAsBoolean()) return;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) (length / STEP));
        var dust = Compat.dust(color, 0.5F);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            level.addParticle(dust, x + dx * t, y + dy * t, z + dz * t, 0, 0, 0);
        }
    }

    public static void handle(DroneLinePayload payload, GolemNetwork.ClientContext context) {
        Level level = context.level();
        if (level == null) return;
        show(level, payload.x(), payload.y(), payload.z(), payload.dx(), payload.dy(), payload.dz(), payload.color());
    }
}
