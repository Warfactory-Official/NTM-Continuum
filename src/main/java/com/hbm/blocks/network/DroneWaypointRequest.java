package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.TileEntityDroneWaypointRequest;

/** 1.7 {@code DroneWaypointRequest}: the request network's relay; no interaction at all. */
public class DroneWaypointRequest extends DroneWaypointBase {

    public DroneWaypointRequest(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDroneWaypointRequest(pos, state);
    }
}
