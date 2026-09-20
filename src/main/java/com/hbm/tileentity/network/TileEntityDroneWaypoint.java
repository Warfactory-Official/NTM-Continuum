package com.hbm.tileentity.network;

import dev.golem.core.data.Tags;

import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.network.DroneWaypointBase;
import com.hbm.client.DroneLineClient;
import com.hbm.tileentity.ModDroneBlockEntities;

/**
 * 1.7 {@code TileEntityDroneWaypoint}: a transport-drone relay. Drones arriving at its stand-off point (its
 * facing times {@link #height}) are sent on to the next link.
 */
public class TileEntityDroneWaypoint extends TileEntityLoadedBase implements IDroneLinkable {

    public int height = 5;
    public @Nullable BlockPos next;

    public TileEntityDroneWaypoint(BlockPos pos, BlockState state) {
        super(ModDroneBlockEntities.DRONE_WAYPOINT.get(), pos, state);
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DroneWaypointBase.FACING) ? state.getValue(DroneWaypointBase.FACING) : Direction.UP;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos point = getPoint();

        if (next != null) {
            for (EntityDeliveryDrone drone : server.getEntitiesOfClass(EntityDeliveryDrone.class, new AABB(point))) {
                if (drone.getDeltaMovement().length() >= 0.05) continue;
                drone.setTarget(next.getX() + 0.5, next.getY(), next.getZ() + 0.5);
            }
        }

        networkPackNT();
    }

    /** 1.7: a redstone dust marks the hover point and a blue line runs to the next link, every other tick. */
    public void clientTick() {
        if (next == null || level == null || level.getGameTime() % 2 != 0) return;

        BlockPos point = getPoint();
        double x = point.getX() + 0.5, y = point.getY() + 0.5, z = point.getZ() + 0.5;
        level.addParticle(DustParticleOptions.REDSTONE, x, y, z, 0, 0, 0);
        DroneLineClient.show(level, x, y, z, next.getX() - point.getX(), next.getY() - point.getY(),
                next.getZ() - point.getZ(), 0x0000ff);
    }

    @Override
    public BlockPos getPoint() {
        return worldPosition.relative(facing(), height);
    }

    @Override
    public void setNextTarget(BlockPos target) {
        this.next = target.immutable();
        setChanged();
    }

    public void addHeight(int delta) {
        height = Mth.clamp(height + delta, 1, 15);
        setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(height);
        buf.writeBoolean(next != null);
        if (next != null) buf.writeBlockPos(next);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        height = buf.readInt();
        next = buf.readBoolean() ? buf.readBlockPos() : null;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        height = input.getIntOr("height", 5);
        next = input.read("next", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("height", height);
        if (next != null) output.store("next", BlockPos.CODEC, next);
    }
}
