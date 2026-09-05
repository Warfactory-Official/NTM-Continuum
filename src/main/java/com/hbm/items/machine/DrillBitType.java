package com.hbm.items.machine;

/** Exact data table from the legacy EnumDrillType used by the excavator. */
public enum DrillBitType {
    STEEL(1.0D, 1, 0, false, false, "Steel Drillbit"),
    STEEL_DIAMOND(1.0D, 1, 2, false, true, "Steel Drillbit (Diamond-Tipped)"),
    HSS(1.2D, 2, 0, true, false, "High-Speed Steel Drillbit"),
    HSS_DIAMOND(1.2D, 2, 3, true, true, "High-Speed Steel Drillbit (Diamond-Tipped)"),
    DESH(1.5D, 3, 1, true, true, "Desh Drillbit"),
    DESH_DIAMOND(1.5D, 3, 4, true, true, "Desh Drillbit (Diamond-Tipped)"),
    TCALLOY(2.0D, 4, 1, true, true, "Technetium Steel Drillbit"),
    TCALLOY_DIAMOND(2.0D, 4, 4, true, true, "Technetium Steel Drillbit (Diamond-Tipped)"),
    FERRO(2.5D, 5, 1, true, true, "Ferrouranium Drillbit"),
    FERRO_DIAMOND(2.5D, 5, 4, true, true, "Ferrouranium Drillbit (Diamond-Tipped)");

    public static final DrillBitType[] VALUES = values();

    public final double speed;
    public final int tier;
    public final int fortune;
    public final boolean vein;
    public final boolean silk;
    public final String displayName;

    DrillBitType(double speed, int tier, int fortune, boolean vein, boolean silk, String displayName) {
        this.speed = speed;
        this.tier = tier;
        this.fortune = fortune;
        this.vein = vein;
        this.silk = silk;
        this.displayName = displayName;
    }
}
