package com.hbm.tileentity.machine;

import com.hbm.api.entity.RadarEntry;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code TileEntityMachineRadarScreen}: holds nothing of its own. A radar with a linker to it in slot 8
 * writes its blips, position and range in every tick and marks it linked; the screen publishes that and then
 * forgets it, so a screen whose radar goes away falls back to static on the next tick.
 */
public class TileEntityMachineRadarScreen extends TileEntityLoadedBase {

    public List<RadarEntry> entries = new ArrayList<>();
    public int refX;
    public int refY;
    public int refZ;
    public int range;
    public boolean linked;

    public TileEntityMachineRadarScreen(BlockPos pos, BlockState state) {
        super(ModRadarUtilityBlockEntities.RADAR_SCREEN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRadarScreen screen) {
        screen.networkPackNT();
        screen.entries.clear();
        screen.linked = false;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(linked);
        buf.writeInt(refX);
        buf.writeInt(refY);
        buf.writeInt(refZ);
        buf.writeInt(range);
        buf.writeInt(entries.size());
        for (RadarEntry entry : entries) entry.toBytes(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        linked = buf.readBoolean();
        refX = buf.readInt();
        refY = buf.readInt();
        refZ = buf.readInt();
        range = buf.readInt();
        int count = buf.readInt();
        this.entries.clear();
        for (int i = 0; i < count; i++) {
            RadarEntry entry = new RadarEntry();
            entry.fromBytes(buf);
            this.entries.add(entry);
        }
    }
}
