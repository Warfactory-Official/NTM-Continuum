package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Everything the submit step needs about one turret, sampled during extract. A turret's own renderer
 * contributes only an {@link Emitter}, which turns this into posed parts.
 */
public final class TurretState<T extends TileEntityTurretBaseNT> {

    public final TurretFrame frame = new TurretFrame();

    /** Where a beam-firing turret's effect starts, filled by its emitter alongside the frame. */
    public final org.joml.Matrix4f effect = new org.joml.Matrix4f();

    private final Emitter<T> emitter;

    public double offsetX;
    public double offsetZ;
    public float yaw;
    public float pitch;
    public float partialTicks;
    public int connectorMask;
    public long gameTime;
    public boolean beam;
    public double beamLength;
    public int beamSegments;

    private @Nullable T turret;

    public TurretState(Emitter<T> emitter) {
        this.emitter = emitter;
    }

    public void sample(T turret, float partialTicks) {
        this.turret = turret;
        this.partialTicks = partialTicks;
        Vec3 offset = turret.getHorizontalOffset();
        offsetX = offset.x;
        offsetZ = offset.z;
        yaw = (float) -Math.toDegrees(Mth.lerp(partialTicks, turret.lastRotationYaw, turret.rotationYaw));
        pitch = (float) Math.toDegrees(Mth.lerp(partialTicks, turret.lastRotationPitch, turret.rotationPitch));
        connectorMask = turret.connectorMask;
        gameTime = turret.getLevel() != null ? turret.getLevel().getGameTime() : 0L;
    }

    public TurretFrame emit() {
        frame.rewind();
        effect.identity();
        beam = false;
        beamLength = 0D;
        beamSegments = 0;
        T sampled = turret;
        if (sampled != null) emitter.emit(sampled, this, frame);
        return frame;
    }

    public interface Emitter<T extends TileEntityTurretBaseNT> {
        void emit(T turret, TurretState<T> state, TurretFrame frame);
    }
}
