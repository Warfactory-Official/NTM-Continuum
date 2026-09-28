package com.hbm.saveddata.satellites;

import com.hbm.advancement.HbmCriteria;
import com.hbm.items.ModSatelliteItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code SatelliteRelay}. */
public class SatelliteRelay extends Satellite {

    public static final MapCodec<SatelliteRelay> CODEC = MapCodec.unit(SatelliteRelay::new);

    public SatelliteRelay() {}

    @Override
    public SatelliteType type() {
        return SatelliteType.RELAY;
    }

    @Override public String getType() { return "RX/TX"; }

    /**
     * 1.7 overrides onOrbit WITHOUT calling super: the relay neither takes the launch site as its target nor
     * announces itself on {@code SAT_LINK}. It only awards achFOEQ to every player in the world.
     */
    @Override
    public void onOrbit(ServerLevel level, double x, double y, double z) {
        ItemStack foeq = new ItemStack(ModSatelliteItems.sat_foeq.get());
        for (ServerPlayer player : level.players()) HbmCriteria.orbit(player, foeq);
    }
}
