package com.hbm.entity;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import com.hbm.handler.EnumCraneKey;
import net.minecraft.util.Mth;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Per-living-entity HBM state (radiation, digamma, lung disease, timers), ported from 1.12.2's
 * {@code HbmLivingCapability.EntityHbmProps}. There is no Forge capability system in common code:
 * every {@link net.minecraft.world.entity.LivingEntity} carries one of these via the
 * {@code MixinLivingEntity} duck field ({@link IHbmLivingDuck}), so the old DUMMY
 * fallback no longer exists. Use the {@link com.hbm.capability.HbmLivingProps} static facade for all
 * gameplay access (it carries the clamps/side-effects); this class is dumb storage + serialization.
 *
 * <p>NBT lives under the {@code HbmLivingProps} child of the entity tag (v1 format, doubles). The
 * network payload is the old versioned ByteBuf format (v1).
 */
public class EntityHbmProps {

    public static final int MAX_ASBESTOS = 60 * 60 * 20;
    public static final int MAX_BLACKLUNG = 60 * 60 * 20;

    private double rads = 0D;
    private double neutrons = 0D;
    private double envRads = 0D;
    private double radBuf = 0D;
    private double digamma = 0D;
    private int asbestos = 0;
    private int blacklung = 0;
    private int bombTimer = 0;
    private int contagion = 0;
    private int oil = 0;
    public int phosphorus = 0;
    public int fire = 0;
    public int balefire = 0;
    private final List<ContaminationEffect> contamination = new ArrayList<>();
    /** Live crane input only; deliberately excluded from persistence and entity sync. */
    private final boolean[] craneKeysPressed = new boolean[EnumCraneKey.VALUES.length];

    public double getRads() { return rads; }
    /** Clamped to [0, 2500] exactly like the old capability setter. */
    public void setRads(double rads) { this.rads = Mth.clamp(rads, 0D, 2500D); }

    public double getNeutrons() { return neutrons; }
    public void setNeutrons(double neutrons) { this.neutrons = Math.max(neutrons, 0D); }

    public double getRadsEnv() { return envRads; }
    public void setRadsEnv(double rads) { this.envRads = rads; }

    public double getRadBuf() { return radBuf; }
    public void setRadBuf(double buf) { this.radBuf = buf; }

    public double getDigamma() { return digamma; }
    public void setDigamma(double dig) { this.digamma = dig; }

    public int getAsbestos() { return asbestos; }
    public void setAsbestos(int asbestos) { this.asbestos = asbestos; }

    public int getBlacklung() { return blacklung; }
    public void setBlacklung(int blacklung) { this.blacklung = blacklung; }

    public int getBombTimer() { return bombTimer; }
    public void setBombTimer(int bombTimer) { this.bombTimer = bombTimer; }

    public int getContagion() { return contagion; }
    public void setContagion(int cont) { this.contagion = cont; }

    public int getOil() { return oil; }
    public void setOil(int time) { this.oil = time; }

    public int getPhosphorus() { return phosphorus; }
    public void setPhosphorus(int time) { this.phosphorus = time; }

    public int getFire() { return fire; }
    public void setFire(int time) { this.fire = time; }

    public int getBalefire() { return balefire; }
    public void setBalefire(int time) { this.balefire = time; }

    public List<ContaminationEffect> getContaminationEffectList() { return contamination; }

    public boolean isCraneKeyPressed(EnumCraneKey key) { return craneKeysPressed[key.ordinal()]; }

    public void setCraneKeyPressed(EnumCraneKey key, boolean pressed) { craneKeysPressed[key.ordinal()] = pressed; }

    // ---- persistence (entity NBT child "HbmLivingProps") ----

    public void save(DataOut out) {
        out.putString("fmt", "v1");
        out.putDouble("rads", rads);
        out.putDouble("neutrons", neutrons);
        out.putDouble("envRads", envRads);
        out.putDouble("radBuf", radBuf);
        out.putDouble("digamma", digamma);
        out.putInt("asbestos", asbestos);
        out.putInt("blacklung", blacklung);
        out.putInt("bombtimer", bombTimer);
        out.putInt("contagion", contagion);
        out.putInt("oil", oil);
        out.putInt("fire", fire);
        out.putInt("phosphorus", phosphorus);
        out.putInt("balefire", balefire);
        DataOut.ChildList list = out.childrenList("cont");
        for (ContaminationEffect effect : contamination) effect.save(list.addChild());
    }

    public void load(DataIn in) {
        this.rads = in.getDoubleOr("rads", 0D);
        this.neutrons = in.getDoubleOr("neutrons", 0D);
        this.envRads = in.getDoubleOr("envRads", 0D);
        this.radBuf = in.getDoubleOr("radBuf", 0D);
        this.digamma = in.getDoubleOr("digamma", 0D);
        this.asbestos = in.getIntOr("asbestos", 0);
        this.blacklung = in.getIntOr("blacklung", 0);
        this.bombTimer = in.getIntOr("bombtimer", 0);
        this.contagion = in.getIntOr("contagion", 0);
        this.oil = in.getIntOr("oil", 0);
        this.fire = in.getIntOr("fire", 0);
        this.phosphorus = in.getIntOr("phosphorus", 0);
        this.balefire = in.getIntOr("balefire", 0);
        contamination.clear();
        for (DataIn child : in.childrenListOrEmpty("cont")) {
            contamination.add(ContaminationEffect.load(child));
        }
    }

    // ---- network payload (old HbmPlayerSyncPacket v1 format) ----

    public void serialize(ByteBuf buf) {
        buf.writeByte(1);
        buf.writeDouble(rads);
        buf.writeDouble(neutrons);
        buf.writeDouble(envRads);
        buf.writeDouble(radBuf);
        buf.writeDouble(digamma);
        buf.writeInt(asbestos);
        buf.writeInt(blacklung);
        buf.writeInt(bombTimer);
        buf.writeInt(contagion);
        buf.writeInt(oil);
        buf.writeInt(phosphorus);
        buf.writeInt(fire);
        buf.writeInt(balefire);
        buf.writeInt(contamination.size());
        for (ContaminationEffect e : contamination) e.writeTo(buf);
    }

    public void deserialize(ByteBuf buf) {
        if (buf.readByte() != 1) return;
        this.rads = buf.readDouble();
        this.neutrons = buf.readDouble();
        this.envRads = buf.readDouble();
        this.radBuf = buf.readDouble();
        this.digamma = buf.readDouble();
        this.asbestos = buf.readInt();
        this.blacklung = buf.readInt();
        this.bombTimer = buf.readInt();
        this.contagion = buf.readInt();
        this.oil = buf.readInt();
        this.phosphorus = buf.readInt();
        this.fire = buf.readInt();
        this.balefire = buf.readInt();
        contamination.clear();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) contamination.add(ContaminationEffect.readFrom(buf));
    }

    /**
     * A timed radiation dose (e.g. from standing in a blast): applies {@code maxRad * time/maxTime}
     * RAD per tick, linearly decaying to zero. Ported 1:1 from {@code HbmLivingProps.ContaminationEffect}.
     */
    public static class ContaminationEffect {

        public double maxRad;
        public int maxTime;
        public int time;
        public boolean ignoreArmor;

        public ContaminationEffect(double rad, int time, boolean ignoreArmor) {
            this.maxRad = rad;
            this.maxTime = this.time = time;
            this.ignoreArmor = ignoreArmor;
        }

        public double getRad() {
            return maxRad * ((double) time / (double) maxTime);
        }

        public void save(DataOut out) {
            out.putDouble("maxRad", maxRad);
            out.putInt("maxTime", maxTime);
            out.putInt("time", time);
            out.putBoolean("ignoreArmor", ignoreArmor);
        }

        public static ContaminationEffect load(DataIn in) {
            ContaminationEffect effect = new ContaminationEffect(
                    in.getDoubleOr("maxRad", 0D), in.getIntOr("maxTime", 1), in.getBooleanOr("ignoreArmor", false));
            effect.time = in.getIntOr("time", 0);
            return effect;
        }

        public void writeTo(ByteBuf buf) {
            buf.writeDouble(maxRad);
            buf.writeInt(maxTime);
            buf.writeInt(time);
            buf.writeBoolean(ignoreArmor);
        }

        public static ContaminationEffect readFrom(ByteBuf buf) {
            double maxRad = buf.readDouble();
            int maxTime = buf.readInt();
            int time = buf.readInt();
            boolean ignoreArmor = buf.readBoolean();
            ContaminationEffect effect = new ContaminationEffect(maxRad, maxTime, ignoreArmor);
            effect.time = time;
            return effect;
        }
    }
}
