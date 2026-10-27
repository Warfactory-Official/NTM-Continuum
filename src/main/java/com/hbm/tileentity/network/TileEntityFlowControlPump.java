package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModFluidNetBlockEntities;
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

/**
 * 1.7 {@code FlowControlPump.TileEntityFlowControlPump}: a 1x3 throttle. It takes from the net on its intake side
 * into a buffer of {@link #bufferSize} mB and hands that buffer to the net on its outlet side, so at most
 * {@code bufferSize} mB pass per tick. The two nets stay apart. Redstone on any of its three blocks stops it.
 */
public class TileEntityFlowControlPump extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2, IControlReceiver {

    public static final float ROTOR_ACCELERATION = 1F;

    public int bufferSize = 100;
    public final FluidTankNTM[] tank;
    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    public boolean redstone = false;

    public boolean topRail = false;
    public boolean bottomRail = true;
    public boolean isWorking = false;

    public float rotor;
    public float prevRotor;
    public float rotorSpeed;

    protected PortDef cachedOutPort;
    protected PortDef cachedInPort;

    public TileEntityFlowControlPump(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.FLOW_CONTROL_PUMP.get(), pos, state);
        this.tank = new FluidTankNTM[]{new FluidTankNTM(Fluids.NONE, bufferSize)};
    }

    /** The outlet side: the facing turned about DOWN. */
    public ForgeDirection outSide() {
        return ForgeDirection.getOrientation(MachineCore.facingOrdinal(getBlockState(), 2)).getRotation(ForgeDirection.DOWN);
    }

    public PortDef getOutPort() {
        if (cachedOutPort == null) {
            ForgeDirection dir = outSide();
            cachedOutPort = PortDef.make(worldPosition.getX() + dir.offsetX, worldPosition.getY(), worldPosition.getZ() + dir.offsetZ, dir);
        }
        return cachedOutPort;
    }

    public PortDef getInPort() {
        if (cachedInPort == null) {
            ForgeDirection dir = outSide().getOpposite();
            cachedInPort = PortDef.make(worldPosition.getX() + dir.offsetX, worldPosition.getY(), worldPosition.getZ() + dir.offsetZ, dir);
        }
        return cachedInPort;
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        ForgeDirection rot = outSide();
        return dir == rot || dir == rot.getOpposite();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFlowControlPump pump) {
        pump.setupFluidOutPorts(pump.getSendingTanks(), pump.getOutPort());
        pump.setupFluidInPorts(pump.getReceivingTanks(), pump.getInPort());
        pump.updatePortFIFO();

        // if the capacity were changed directly, any excess buffered fluid would be destroyed
        // when running a closed loop or handling hard to get fluids, that's quite bad
        if (pump.bufferMismatch()) {
            int nextBuffer = Math.max(pump.tank[0].getFill(), pump.bufferSize);
            pump.tank[0].changeTankSize(nextBuffer);
        }

        ForgeDirection dir = pump.outSide();
        pump.redstone = level.hasNeighborSignal(pos)
                || level.hasNeighborSignal(pos.offset(dir.offsetX, 0, dir.offsetZ))
                || level.hasNeighborSignal(pos.offset(-dir.offsetX, 0, -dir.offsetZ));

        pump.networkPackNT();
        pump.isWorking = false;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFlowControlPump pump) {
        if (level.getGameTime() % 20 == 0) {
            pump.topRail = !level.getBlockState(pos.above()).isAir();
            pump.bottomRail = !level.getBlockState(pos.below()).isAir();
        }

        if (pump.isWorking) pump.rotorSpeed += ROTOR_ACCELERATION;
        else pump.rotorSpeed -= ROTOR_ACCELERATION;
        pump.rotorSpeed = Mth.clamp(pump.rotorSpeed, 0F, 30);

        pump.prevRotor = pump.rotor;
        pump.rotor += pump.rotorSpeed;
        if (pump.rotor >= 360F) {
            pump.rotor -= 360F;
            pump.prevRotor -= 360F;
        }
    }

    @Override
    public void useUpFluid(FluidType type, int pressure, long amount) {
        if (amount > 0 && tank[0].provides(type) && tank[0].getPressure() == pressure) {
            int toRem = (int) Math.min(amount, tank[0].getFill());
            tank[0].setFill(tank[0].getFill() - toRem);
            this.isWorking = true;
        }
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
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 128;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "capacity")) this.bufferSize = Mth.clamp(Tags.getIntOr(data, "capacity", bufferSize), 0, 10_000);
        if (Tags.contains(data, "pressure")) this.tank[0].withPressure(Mth.clamp(Tags.getByteOr(data, "pressure", (byte) 0), 0, 5));
        if (Tags.contains(data, "priority")) priority = priorityOf(Tags.getByteOr(data, "priority", (byte) 0));
        setChanged();
    }

    /** 1.7 {@code EnumUtil.grabEnumSafely}: an index out of range wraps. */
    private static ConnectionPriority priorityOf(int index) {
        return ConnectionPriority.VALUES[Math.floorMod(index, ConnectionPriority.VALUES.length)];
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank[0].serialize(buf);
        buf.writeByte((byte) priority.ordinal());
        buf.writeInt(bufferSize);
        buf.writeBoolean(redstone);
        buf.writeBoolean(isWorking);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank[0].deserialize(buf);
        priority = priorityOf(buf.readByte());
        bufferSize = buf.readInt();
        redstone = buf.readBoolean();
        isWorking = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tank[0].writeToNBT(output, "t");
        SavedIds.putEnum(output, "p", priority);
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
