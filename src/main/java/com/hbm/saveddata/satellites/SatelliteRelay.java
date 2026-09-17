package com.hbm.saveddata.satellites;

import com.hbm.advancement.HbmCriteria;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class SatelliteRelay extends Satellite {

    public static final MapCodec<SatelliteRelay> CODEC = MapCodec.unit(SatelliteRelay::new);

    public SatelliteRelay() {
        super(Interfaces.NONE, Set.of(), Set.of());
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.RELAY;
    }

    @Override
    public void onOrbit(ServerLevel level, double x, double y, double z) {
        ItemStack chip = new ItemStack(type().item());
        for (ServerPlayer player : level.players()) HbmCriteria.orbit(player, chip);
    }
}
