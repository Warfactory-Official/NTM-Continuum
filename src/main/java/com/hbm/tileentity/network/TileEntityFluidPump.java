package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.SavedIds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.hbm.blocks.network.FluidPump;

/**
 * The 1.7 {@code FluidPump.TileEntityFluidPump}, deprecated: both sides are one passthrough port, so the nets on
 * either side are one net and the pump is a buffer on it. Pumps from older saves keep their settings, which no screen
 * edits.
 */
public class TileEntityFluidPump extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2, IControlReceiver {

    public int bufferSize = 100;
    public final FluidTankNTM[] tank;
    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    public boolean redstone = false;

    public TileEntityFluidPump(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.PUMP.get(), pos, state);
        this.tank = new FluidTankNTM[]{new FluidTankNTM(Fluids.NONE, bufferSize)};
    }

    public ForgeDirection facing() {
        BlockState state = getBlockState();
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return ForgeDirection.NORTH;
        return ForgeDirection.getOrientation(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    /** Intake side: the facing rotated about UP, exactly the 1.7 {@code dir.getRotation(UP)}. */
    public ForgeDirection inSide() {
        return facing().getRotation(ForgeDirection.UP);
    }

    public ForgeDirection outSide() {
        return inSide().getOpposite();
    }

    protected PortDef cachedPort;

    public PortDef getPorts() {
        if (cachedPort == null) cachedPort = PortDef.make(worldPosition, inSide(), outSide());
        return cachedPort;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFluidPump pump) {
        pump.setupFluidOutPorts(pump.getSendingTanks(), pump.getPorts());
        pump.setupFluidInPorts(pump.getReceivingTanks(), pump.getPorts());
        pump.updatePortFIFO();

        // if the capacity were changed directly, any excess buffered fluid would be destroyed
        // when running a closed loop or handling hard to get fluids, that's quite bad
        if (pump.bufferMismatch()) {
            int nextBuffer = Math.max(pump.tank[0].getFill(), pump.bufferSize);
            pump.tank[0].changeTankSize(nextBuffer);
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != pump.redstone) {
            pump.redstone = powered;
            pump.setChanged();
        }
        pump.networkPackNT(); // 1.7 packed every 15 ticks; the port's pack skips unchanged payloads
    }

    public boolean bufferMismatch() {
        return this.bufferSize != this.tank[0].getMaxFill();
    }

    @Override public ConnectionPriority getFluidPriority() { return priority; }
    @Override public FluidTankNTM[] getSendingTanks() { return tank; }
    @Override public FluidTankNTM[] getReceivingTanks() { return tank; }
    @Override public FluidTankNTM[] getAllTanks() { return tank; }

    @Override public long getReceiverSpeed(FluidType type, int pressure) { return redstone || bufferMismatch() ? 0 : 1_000_000_000; }
    @Override public long getProviderSpeed(FluidType type, int pressure) { return redstone ? 0 : 1_000_000_000; }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(getBlockPos().getX() + 0.5D, getBlockPos().getY() + 0.5D, getBlockPos().getZ() + 0.5D) <= 128;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "capacity")) this.bufferSize = Mth.clamp(Tags.getIntOr(data, "capacity", bufferSize), 0, 10_000);
        if (Tags.contains(data, "pressure")) this.tank[0].withPressure(Mth.clamp(Tags.getByteOr(data, "pressure", (byte) 0), 0, 5));
        if (Tags.contains(data, "priority")) {
            ConnectionPriority[] values = ConnectionPriority.values();
            int index = Tags.getByteOr(data, "priority", (byte) 0);
            priority = index >= 0 && index < values.length ? values[index] : ConnectionPriority.NORMAL;
        }
        setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank[0].serialize(buf);
        buf.writeByte((byte) priority.ordinal());
        buf.writeInt(bufferSize);
        buf.writeBoolean(redstone);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank[0].deserialize(buf);
        int p = buf.readByte();
        ConnectionPriority[] values = ConnectionPriority.values();
        priority = p >= 0 && p < values.length ? values[p] : ConnectionPriority.NORMAL;
        bufferSize = buf.readInt();
        redstone = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tank[0].writeToNBT(output, "t");
        SavedIds.putEnum(output, "p", priority); // by name, never an ordinal
        output.putInt("buffer", bufferSize);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tank[0].readFromNBT(input, "t");
        priority = SavedIds.getEnum(input, "p", ConnectionPriority.NORMAL);
        bufferSize = input.getIntOr("buffer", 100);
    }
}
