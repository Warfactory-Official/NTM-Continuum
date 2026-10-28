package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemSatellite.EnumSatType;
import net.minecraft.network.chat.Component;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionSavedData.PollutionData;
import com.hbm.handler.pollution.PollutionType;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 1.7 {@code SatelliteMapper}, the spy satellite. */
public class SatelliteMapper extends Satellite {

    public static final MapCodec<SatelliteMapper> CODEC = MapCodec.unit(SatelliteMapper::new);

    public static final String CMD_TARGET_LOADED = "targetloaded";
    public static final String CMD_GETSMOG = "getsmog";
    public static final String CMD_SPOT_PLAYER = "spotplayers";
    public static final String CMD_FIND_PLAYER = "findplayer";

    public static final int SPOT_PLAYER_MAX_RANGE = 250;

    public SatelliteMapper() {}

    @Override
    public SatelliteType type() {
        return SatelliteType.MAPPER;
    }

    @Override public String getType() { return "NOT_A_SPY_SATELLITE_:)"; }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[] { nameOf(EnumSatType.SPY) };
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_TARGET_LOADED)) {
            // 1.7 chunkProvider.chunkExists: whether the chunk is currently loaded
            this.tx = ("" + (level instanceof ServerLevel server
                    && server.getChunkSource().hasChunk(targetX >> 4, targetZ >> 4))).toUpperCase(Locale.US);
            return;
        }

        if (cmd[0].equals(CMD_GETSMOG)) {
            PollutionData data = PollutionHandler.getPollutionData(level, new BlockPos(this.targetX, 255, this.targetZ));
            if (data != null) {
                float soot = data.pollution[PollutionType.SOOT.ordinal()];
                this.tx = "" + (int) Math.ceil(soot);
            }
            return;
        }

        if (cmd[0].equals(CMD_SPOT_PLAYER)) {
            List<String> names = new ArrayList<>();

            for (Player player : level.players()) {
                int x = (int) Math.floor(player.getX());
                int z = (int) Math.floor(player.getZ());

                double dX = x - targetX;
                double dZ = z - targetZ;

                if (dX * dX + dZ * dZ <= SPOT_PLAYER_MAX_RANGE * SPOT_PLAYER_MAX_RANGE) {
                    int height = Heights.heightValue(level, x, z);
                    if (height < player.getY() + 2) names.add(player.getName().getString());
                }
            }

            if (names.isEmpty()) {
                this.tx = "NONE";
                return;
            }

            this.tx = String.join(";", names);
            return;
        }

        if (cmd[0].equals(CMD_FIND_PLAYER) && cmd.length == 2) {
            String lookingFor = cmd[1].toLowerCase(Locale.US);

            for (Player player : level.players()) {
                int x = (int) Math.floor(player.getX());
                int z = (int) Math.floor(player.getZ());

                double dX = x - targetX;
                double dZ = z - targetZ;

                if (dX * dX + dZ * dZ <= SPOT_PLAYER_MAX_RANGE * SPOT_PLAYER_MAX_RANGE
                        && player.getName().getString().toLowerCase(Locale.US).equals(lookingFor)) {
                    int height = Heights.heightValue(level, x, z);
                    if (height < player.getY() + 2) {
                        this.tx = x + ";" + z;
                        return;
                    }
                }
            }

            this.tx = "NOT FOUND";
        }
    }
}
