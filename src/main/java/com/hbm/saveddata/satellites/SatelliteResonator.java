package com.hbm.saveddata.satellites;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

public class SatelliteResonator extends Satellite {

    public static final MapCodec<SatelliteResonator> CODEC = MapCodec.unit(SatelliteResonator::new);

    public SatelliteResonator() {
        super(Interfaces.SAT_COORD, Set.of(), Set.of(CoordActions.HAS_Y));
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.RESONATOR;
    }

    @Override
    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        serverPlayer.stopRiding();
        serverPlayer.connection.teleport(x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot());
        // 1.7: the second play reads the position AFTER the move, so it lands at the destination.
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
