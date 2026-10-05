package com.hbm.items.machine;

/** Immutable fuel specification copied from the 1.12.2 ZIRNOX rod enum. */
public enum ZirnoxFuel {
    NATURAL_URANIUM("natural_uranium_fuel", 250_000, 30, false, 0.35F, 172.5F, "natural_uranium"),
    URANIUM("uranium_fuel", 200_000, 50, false, 0.5F, 150F, "uranium"),
    TH232("th232_fuel", 20_000, 0, true, 0.1F, 1.75F, null),
    THORIUM("thorium_fuel", 200_000, 40, false, 1.75F, 112.5F, "thorium"),
    MOX("mox_fuel", 165_000, 75, false, 2.5F, 150F, "mox"),
    PLUTONIUM("plutonium_fuel", 175_000, 65, false, 4.25F, 187.5F, "plutonium"),
    U233("u233_fuel", 150_000, 100, false, 5F, 150F, "u233"),
    U235("u235_fuel", 165_000, 85, false, 1F, 165F, "u235"),
    // 1.7 names the LES rod's waste waste_schrabidium ("Depleted Schrabidium Fuel"); there is no waste_les
    LES("les_fuel", 150_000, 150, false, 5.85F, 225F, "schrabidium"),
    LITHIUM("lithium_fuel", 20_000, 0, true, 0F, 0.001F, null),
    ZFB_MOX("zfb_mox_fuel", 50_000, 35, false, 2.5F, 75F, "zfb_mox");

    public final String id;
    public final int maxLife;
    public final int heat;
    public final boolean breeding;
    public final float baseRadiation;
    public final float depletedRadiation;
    /** The matching waste item suffix (1.7 item id {@code waste_<suffix>}), or {@code null} for breeding rods. */
    public final String wasteId;

    /** 1.7 {@code setTextureName}: natural uranium, U233 and U235 waste share {@code waste_uranium}. */
    public String wasteTexture() {
        return switch (this) {
            case NATURAL_URANIUM, U233, U235 -> "waste_uranium";
            default -> "waste_" + wasteId;
        };
    }

    ZirnoxFuel(String id, int maxLife, int heat, boolean breeding, float baseRadiation, float depletedRadiation,
               String wasteId) {
        this.id = id;
        this.maxLife = maxLife;
        this.heat = heat;
        this.breeding = breeding;
        this.baseRadiation = baseRadiation;
        this.depletedRadiation = depletedRadiation;
        this.wasteId = wasteId;
    }
}
