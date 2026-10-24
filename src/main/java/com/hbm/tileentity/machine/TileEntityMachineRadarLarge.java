package com.hbm.tileentity.machine;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import com.hbm.tileentity.IConfigurableMachine;

/**
 * 1.7 {@code TileEntityMachineRadarLarge}: the same radar with three times the range, fed and read through
 * the four edge-centre cells of its 3x3 footprint, so its connection points sit two blocks out.
 */
public class TileEntityMachineRadarLarge extends TileEntityMachineRadarNT {

    public static int radarLargeRange = 3_000;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "radar_large"; }

        @Override public void readIfPresent(JsonObject obj) {
            radarLargeRange = IConfigurableMachine.grab(obj, "I:radarLargeRange", radarLargeRange);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:radarLargeRange").value(radarLargeRange);
        }
    };

    public TileEntityMachineRadarLarge(BlockPos pos, BlockState state) {
        super(ModRadarUtilityBlockEntities.RADAR_LARGE.get(), pos, state);
    }

    @Override
    public int getRange() {
        return radarLargeRange;
    }

    @Override
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.flare(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    /** Only the proxy cells carry power; the core itself is buried in the middle of the footprint. */
    @Override
    public boolean canConnect(ForgeDirection dir) {
        return true;
    }

    /** The emitting cells are one block in from the connection points: notify them, then the points. */
    @Override
    protected void updateRedstone(Level level) {
        for (PortDef port : getPorts()) for (DirPos pos : port.portConnections) {
            BlockPos cell = worldPosition.offset(pos.getDir().offsetX, 0, pos.getDir().offsetZ);
            level.updateNeighborsAt(cell, getBlockState().getBlock());
            level.updateNeighborsAt(pos.getPos(), getBlockState().getBlock());
        }
    }
}
