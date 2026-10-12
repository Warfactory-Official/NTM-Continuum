package com.hbm.particle;

import java.util.HashMap;

/** Source-shaped definition for a spent casing's mesh, tint, bounce and sound. */
public class SpentCasing implements Cloneable {
    public static final int COLOR_CASE_BRASS = 0xEBC35E;
    public static final int COLOR_CASE_EQUESTRIAN = 0x957BA0;
    public static final int COLOR_CASE_12GA = 0x757575;
    public static final int COLOR_CASE_4GA = 0xD8D8D8;
    public static final int COLOR_CASE_44 = 0x3E3E3E;
    public static final int COLOR_CASE_16INCH = 0xD89128;
    public static final int COLOR_CASE_16INCH_PHOS = 0xC8C8C8;
    public static final int COLOR_CASE_16INCH_NUKE = 0x495443;
    public static final int COLOR_CASE_40MM = 0x515151;

    public static final String PLINK_SHELL = "weapon.casing.shell";
    public static final String PLINK_SMALL = "weapon.casing.small";
    public static final String PLINK_MEDIUM = "weapon.casing.medium";
    public static final String PLINK_LARGE = "weapon.casing.large";
    public static final HashMap<String, SpentCasing> casingMap = new HashMap<>();

    private String registryName = "CHANGEME";
    private float scaleX = 1F, scaleY = 1F, scaleZ = 1F;
    private int[] colors;
    private final CasingType type;
    private String bounceSound;
    private float bounceYaw = 1F, bouncePitch = 1F;
    private int maxAge = 240;
    private float smokeChance;
    private double smokeLift;
    private int smokeDuration;
    private int smokeNodeLife;

    public SpentCasing(CasingType type) {
        this.type = type;
        setSound(type == CasingType.SHOTGUN ? PLINK_SHELL : PLINK_SMALL);
    }

    public static SpentCasing fromName(String name) { return casingMap.get(name); }

    public SpentCasing register(String name) {
        registryName = name;
        SpentCasing previous = casingMap.putIfAbsent(name, this);
        if (previous != null && previous != this) {
            throw new IllegalStateException("Duplicate casing id: " + name);
        }
        return this;
    }

    public SpentCasing setScale(float scale) { return setScale(scale, scale, scale); }

    public SpentCasing setScale(float x, float y, float z) {
        scaleX = x;
        scaleY = y;
        scaleZ = z;
        if (x * y * z >= 3 && type != CasingType.SHOTGUN) setSound(PLINK_MEDIUM);
        if (x * y * z >= 100 && type != CasingType.SHOTGUN) setSound(PLINK_LARGE);
        return this;
    }

    public SpentCasing setColor(int... color) { colors = color; return this; }
    public SpentCasing setupSmoke(float chance, double lift, int duration, int nodeLife) {
        smokeChance = chance;
        smokeLift = lift;
        smokeDuration = duration;
        smokeNodeLife = nodeLife;
        return this;
    }
    public SpentCasing setBounceMotion(float yaw, float pitch) { bounceYaw = yaw; bouncePitch = pitch; return this; }
    public String getName() { return registryName; }
    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getScaleZ() { return scaleZ; }
    public int[] getColors() { return colors; }
    public CasingType getType() { return type; }
    public String getSound() { return bounceSound; }
    /** The bounce sound's id: a bare path is HBM's ({@code "casing.small"}), an addon names its own as {@code "ns:path"}. */
    public SpentCasing setSound(String sound) { bounceSound = sound; return this; }
    public float getBounceYaw() { return bounceYaw; }
    public float getBouncePitch() { return bouncePitch; }
    public int getMaxAge() { return maxAge; }
    public SpentCasing setMaxAge(int age) { maxAge = age; return this; }
    public float getSmokeChance() { return smokeChance; }
    public double getSmokeLift() { return smokeLift; }
    public int getSmokeDuration() { return smokeDuration; }
    public int getSmokeNodeLife() { return smokeNodeLife; }

    @Override public SpentCasing clone() {
        try {
            return (SpentCasing) super.clone();
        } catch (CloneNotSupportedException exception) {
            return new SpentCasing(type);
        }
    }

    public enum CasingType {
        STRAIGHT("Straight"), BOTTLENECK("Bottleneck"), SHOTGUN("Shotgun", "ShotgunCase");

        public final String[] partNames;
        CasingType(String... partNames) { this.partNames = partNames; }
    }
}
