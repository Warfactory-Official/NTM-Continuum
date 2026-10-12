package com.hbm.recipes.data;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.ReactorDataMaps.BatteryPack;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code ItemBatteryPack.EnumBatteryPack}, with its three constructors. */
public final class BatteryPackSource extends DataMapSource<BatteryPack> {

    public BatteryPackSource() {
        super(ReactorDataMaps.BATTERY_PACK, false);
    }

    /** 1.7 {@code EnumBatteryPack(tex, dischargeRate, capacitor)}. */
    private void pack(String tex, long dischargeRate, boolean capacitor) {
        pack(tex, capacitor ? (dischargeRate * 20 * 30) : (dischargeRate * 20 * 60 * 15),
                capacitor ? dischargeRate : dischargeRate * 10, dischargeRate, capacitor);
    }

    /** 1.7 {@code EnumBatteryPack(tex, dischargeRate, duration)}. */
    private void pack(String tex, long dischargeRate, long duration) {
        pack(tex, dischargeRate * duration, dischargeRate * 10, dischargeRate, false);
    }

    /** 1.7 {@code EnumBatteryPack(tex, capacity, chargeRate, dischargeRate)}; {@code isCapacitor()} is the entries after {@code BATTERY_QUANTUM}. */
    private void pack(String tex, long capacity, long chargeRate, long dischargeRate, boolean capacitor) {
        put("hbm:" + tex, new BatteryPack(capacity, chargeRate, dischargeRate, capacitor,
                "hbm:textures/models/machines/" + tex + ".png"));
    }

    @Override
    protected void values() {
        pack("battery_redstone", 100L, false);
        pack("battery_lead", 1_000L, false);
        pack("battery_lithium", 10_000L, false);
        pack("battery_sodium", 50_000L, false);
        pack("battery_schrabidium", 250_000L, false);
        pack("battery_quantum", 1_000_000L, 20 * 60 * 60);

        pack("capacitor_copper", 1_000L, true);
        pack("capacitor_gold", 10_000L, true);
        pack("capacitor_niobium", 100_000L, true);
        pack("capacitor_tantalum", 500_000L, true);
        pack("capacitor_bismuth", 2_500_000L, true);
        pack("capacitor_spark", 10_000_000L, true);
    }
}
