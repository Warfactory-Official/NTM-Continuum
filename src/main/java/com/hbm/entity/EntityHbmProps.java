package com.hbm.entity;

import dev.golem.core.data.Tags;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import com.hbm.handler.EnumCraneKey;
import net.minecraft.util.Mth;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Per-living-entity HBM state (radiation, digamma, lung disease, timers), ported from 1.12.2's
 * {@code HbmLivingCapability.EntityHbmProps}. Every {@link net.minecraft.world.entity.LivingEntity} carries
 * one of these as Golem Core entity data ({@link com.hbm.capability.HbmLivingProps#DATA}: a NeoForge/Fabric data
 * attachment or a Forge capability), so there is no DUMMY fallback. Use the
 * {@link com.hbm.capability.HbmLivingProps} static facade for all gameplay access (it carries the
 * clamps/side-effects); this class is dumb storage + serialization.
 *
 * <p>Saved as the {@code hbm:living_props} attachment (v1 field layout, doubles). Older saves hold the same
 * layout as a {@code HbmLivingProps} child of the entity tag, read once on load. The network payload
 * is a versioned ByteBuf format (v1).
 */
public class EntityHbmProps {

    public static final int MAX_ASBESTOS = 60 * 60 * 20;
    /** 1.7 {@code HbmLivingProps.maxBlacklung}: two hours of exposure (1.12.2 halved it; 1.7 is the spec). */
    public static final int MAX_BLACKLUNG = 2 * 60 * 60 * 20;

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
    /** 1.7 HbmLivingProps.blackFire (saved as "blackfire"): the 358.00 BL black flame. */
    public int blackFire = 0;
    /** 1.7 HbmPlayerProps.grenadeDeployment: the universal grenade's pin state, kept out of every persistence path. */
    public int grenadeDeployment = 0;
    private final List<ContaminationEffect> contamination = new ArrayList<>();
    /** Live crane input only; excluded from persistence and entity sync. */
    private final boolean[] craneKeysPressed = new boolean[EnumCraneKey.VALUES.length];
    /** Persistent general backpack/HUD toggles formerly carried by HbmCapability. */
    private boolean enableBackpack = true;
    private boolean enableHUD = true;
    /** 1.7 HbmPlayerProps shield: absorbed before armor modules; raised by ItemModShield. Synced for the HUD bar (G-C). */
    public float shield = 0;
    public float maxShield = 0;
    /** Tick of the last hurt event, for the shield's 60-tick regeneration delay. Not persisted, as in 1.7. */
    public int lastDamage = 0;
    /** 1.7 HbmPlayerProps.reputation: the dungeon karma, -25..25 (old lanterns repaired / destroyed). Persisted, synced, kept on death. */
    public int reputation = 0;
    /** 1.7 HbmPlayerProps.enableMagnet (G-C): whether magnet armour modules pull items. Persisted, synced, kept on death. */
    public boolean enableMagnet = true;
    /** 1.7 HbmPlayerProps.maskManTimer: seconds the mask man's conditions held. Persisted, reset on death (1.7 did not copy it). */
    public int maskManTimer = 0;
    /** 1.7 player PERSISTED_NBT_TAG "fbiMark": the world time until which BossSpawnHandler's raid skips the player. Kept on death. */
    public long fbiMark = 0;
    /** 1.7 player PERSISTED_NBT_TAG "radMark": set by a reactor meltdown nearby, consumed by the radiation beasts. Kept on death. */
    public boolean radMark = false;

    /**
     * 1.7 {@code ModEventHandler.onPlayerClone}: the respawned (or End-returned) player gets the old one's
     * HbmPlayerProps ({@code serialize}/{@code deserialize}: shield, max shield, backpack, HUD, reputation) and its
     * PERSISTED_NBT_TAG (fbiMark, radMark); everything HbmLivingProps held starts over.
     */
    public void copyPlayerPropsFrom(EntityHbmProps old) {
        this.shield = old.shield;
        this.maxShield = old.maxShield;
        this.enableBackpack = old.enableBackpack;
        this.enableHUD = old.enableHUD;
        this.reputation = old.reputation;
        this.enableMagnet = old.enableMagnet;
        this.fbiMark = old.fbiMark;
        this.radMark = old.radMark;
    }

    public double getRads() { return rads; }
    /** Clamped to [0, 2500]; 2500 is 1.7's cap ({@code incrementRadiation} and the sickness handler). */
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

    public int getBlackFire() { return blackFire; }
    public void setBlackFire(int time) { this.blackFire = time; }

    public List<ContaminationEffect> getContaminationEffectList() { return contamination; }

    public boolean isCraneKeyPressed(EnumCraneKey key) { return craneKeysPressed[key.ordinal()]; }

    public void setCraneKeyPressed(EnumCraneKey key, boolean pressed) { craneKeysPressed[key.ordinal()] = pressed; }

    public boolean getEnableBackpack() { return enableBackpack; }
    public void setEnableBackpack(boolean enabled) { enableBackpack = enabled; }
    public boolean getEnableHUD() { return enableHUD; }
    public void setEnableHUD(boolean enabled) { enableHUD = enabled; }

    // Persistence (the hbm:living_props attachment)

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
        out.putInt("blackfire", blackFire);
        out.putBoolean("enableBackpack", enableBackpack);
        out.putBoolean("enableHUD", enableHUD);
        out.putFloat("shield", shield);
        out.putFloat("maxShield", maxShield);
        out.putInt("reputation", reputation);
        out.putBoolean("enableMagnet", enableMagnet);
        out.putInt("maskManTimer", maskManTimer);
        out.putLong("fbiMark", fbiMark);
        out.putBoolean("radMark", radMark);
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
        this.blackFire = in.getIntOr("blackfire", 0);
        this.enableBackpack = in.getBooleanOr("enableBackpack", true);
        this.enableHUD = in.getBooleanOr("enableHUD", true);
        this.shield = in.getFloatOr("shield", 0F);
        this.maxShield = in.getFloatOr("maxShield", 0F);
        this.reputation = in.getIntOr("reputation", 0);
        this.enableMagnet = in.getBooleanOr("enableMagnet", true);
        this.maskManTimer = in.getIntOr("maskManTimer", 0);
        this.fbiMark = in.getLongOr("fbiMark", 0L);
        this.radMark = in.getBooleanOr("radMark", false);
        contamination.clear();
        for (DataIn child : in.childrenListOrEmpty("cont")) {
            contamination.add(ContaminationEffect.load(child));
        }
    }

    // Network payload (old HbmPlayerSyncPacket v1 format)

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
        buf.writeBoolean(enableBackpack);
        buf.writeBoolean(enableHUD);
        buf.writeInt(contamination.size());
        for (ContaminationEffect e : contamination) e.writeTo(buf);
        buf.writeInt(reputation); // 1.7 HbmPlayerProps.serialize carried it to the client
        // G-C: 1.7 HbmPlayerProps.serialize also carried the shield (for the HUD bar) and the magnet toggle
        buf.writeFloat(shield);
        buf.writeFloat(maxShield);
        buf.writeBoolean(enableMagnet);
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
        if (buf.readableBytes() >= 2) {
            this.enableBackpack = buf.readBoolean();
            this.enableHUD = buf.readBoolean();
        }
        contamination.clear();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) contamination.add(ContaminationEffect.readFrom(buf));
        if (buf.readableBytes() >= 4) this.reputation = buf.readInt();
        if (buf.readableBytes() >= 9) {
            this.shield = buf.readFloat();
            this.maxShield = buf.readFloat();
            this.enableMagnet = buf.readBoolean();
        }
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
