package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import dev.golem.core.data.Tags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Mines an asteroid and drops the result to whatever collects it. The cargo is named rather than rolled
 * here: the pool it names comes with the item-pool system, which is not ported, so the satellite orbits and
 * answers commands but has nothing on the ground yet to hand its load to.
 */
public class SatelliteMiner extends Satellite {

    public static final String POOL_SAT_MINER = "POOL_SAT_MINER";
    public static final String POOL_SAT_LUNAR = "POOL_SAT_LUNAR";

    private static final Map<Class<? extends SatelliteMiner>, String> CARGO = new HashMap<>();

    static {
        registerCargo(SatelliteMiner.class, POOL_SAT_MINER);
        registerCargo(SatelliteLunarMiner.class, POOL_SAT_LUNAR);
    }

    public long lastOp;

    public SatelliteMiner() {
        this.satIface = Interfaces.NONE;
    }

    @Override public String getType() { return "ASTEROID_MINER"; }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable(ItemSatellite.nameKey(EnumSatType.MINER_ASTRO))};
    }

    @Override
    public void save(CompoundTag tag) {
        super.save(tag);
        tag.putLong("lastOp", lastOp);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lastOp = Tags.getLongOr(tag, "lastOp", 0L);
    }

    public static void registerCargo(Class<? extends SatelliteMiner> type, String cargo) {
        CARGO.put(type, cargo);
    }

    public String getCargo() {
        return CARGO.get(getClass());
    }

    @Override
    public float[] getColor() {
        return new float[]{0.0F, 0.0F, 0.0F};
    }
}
