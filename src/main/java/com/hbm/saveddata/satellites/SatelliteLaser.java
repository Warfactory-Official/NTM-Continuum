package com.hbm.saveddata.satellites;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.logic.EntityDeathBlast;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.util.GameTime;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;

public class SatelliteLaser extends Satellite {

    public static final MapCodec<SatelliteLaser> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("lastOp", 0L).forGetter(sat -> sat.lastOp)
    ).apply(i, SatelliteLaser::new));

    /**
     * 1.7 stamped this off {@code System.currentTimeMillis()}; here it is {@link GameTime#millis(net.minecraft.world.level.Level)},
     * so the window survives {@code /tick freeze} and both ends share one clock domain.
     */
    public long lastOp;

    public SatelliteLaser() {
        this(0L);
    }

    private SatelliteLaser(long lastOp) {
        super(Interfaces.SAT_PANEL, Set.of(InterfaceActions.HAS_MAP, InterfaceActions.SHOW_COORDS,
                InterfaceActions.CAN_CLICK), Set.of());
        this.lastOp = lastOp;
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.LASER;
    }

    @Override
    public void onClick(ServerLevel level, int x, int z) {
        long now = GameTime.millis(level);
        if (lastOp + 10000 < now) {
            lastOp = now;
            SatelliteSavedData.get(level).setDirty();

            EntityDeathBlast blast = new EntityDeathBlast(ModNtmEntities.LASER_BLAST.get(), level);
            blast.anchorAndLoad(level, x >> 4, z >> 4);

            blast.setPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z); // 1.7 getHeightValue: first light-blocking block from the top
            level.addFreshEntity(blast);
        }
    }
}
