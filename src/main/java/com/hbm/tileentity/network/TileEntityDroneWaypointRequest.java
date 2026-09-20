package com.hbm.tileentity.network;

import dev.golem.core.data.Tags;

import com.hbm.tileentity.network.RequestNetwork.PathNode;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.network.DroneWaypointBase;
import com.hbm.tileentity.ModDroneBlockEntities;

/**
 * 1.7 {@code TileEntityDroneWaypointRequest}: the request network's relay. It carries nothing, and every dock,
 * provider and requester links through one. Its node stands off the face it is mounted on by {@link #height};
 * 1.7 declares the stand-off setter but nothing calls it, so the height stays 5.
 */
public class TileEntityDroneWaypointRequest extends TileEntityRequestNetwork {

    public int height = 5;

    public TileEntityDroneWaypointRequest(BlockPos pos, BlockState state) {
        super(ModDroneBlockEntities.DRONE_WAYPOINT_REQUEST.get(), pos, state, 0);
    }

    @Override
    public String getDefaultName() {
        return "container.droneWaypointRequest";
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DroneWaypointBase.FACING) ? state.getValue(DroneWaypointBase.FACING) : Direction.UP;
    }

    @Override
    public BlockPos nodePos() {
        return worldPosition.relative(facing(), height);
    }

    public void addHeight(int delta) {
        height = Mth.clamp(height + delta, 1, 15);
        setChanged();
    }

    @Override
    protected PathNode createNode(BlockPos pos) {
        return new PathNode(pos, reachable);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        height = input.getIntOr("height", 5);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("height", height);
    }

    /** No GUI: the block never opens one. */
    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }
}
