package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** Puts a player wherever they ask, which is the whole of what it does. */
public class SatelliteResonator extends Satellite {

    public SatelliteResonator() {
        this.coordAcs.add(CoordActions.HAS_Y);
        this.satIface = Interfaces.SAT_COORD;
    }

    @Override public String getType() { return "XEN_RELAY"; }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable(ItemSatellite.nameKey(EnumSatType.XENIUM_RESONATOR))};
    }

    @Override
    public void onCoordAction(Level level, ServerPlayer player, int x, int y, int z) {
        if (!(level instanceof ServerLevel server)) return;

        server.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.stopRiding();
        server.getChunk(x >> 4, z >> 4);
        int target = y < server.getMinY() ? server.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) : y;
        player.teleportTo(server, x + 0.5D, target, z + 0.5D, java.util.Set.of(),
                player.getYRot(), player.getXRot(), true);
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public float[] getColor() {
        return new float[]{1.0F, 0.646F, 0.181F};
    }
}
