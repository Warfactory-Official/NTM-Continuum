package com.hbm.blocks.network;

import com.hbm.interfaces.ILookOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.tileentity.network.TileEntityDroneWaypoint;

/**
 * 1.7 {@code DroneWaypoint}: the transport network's relay. A right-click raises the stand-off height, a sneaking
 * one lowers it; the linker's own use wins over both.
 */
public class DroneWaypoint extends DroneWaypointBase implements ILookOverlay {

    public DroneWaypoint(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDroneWaypoint(pos, state);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (DroneCrate.holdsLinker(player)) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityDroneWaypoint tile) {
            tile.addHeight(player.isShiftKeyDown() ? -1 : 1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityDroneWaypoint tile)) return null;
        List<String> text = new ArrayList<>();
        text.add("Waypoint distance: " + tile.height);
        if (tile.next != null) {
            text.add("Next waypoint: " + tile.next.getX() + " / " + tile.next.getY() + " / " + tile.next.getZ());
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
