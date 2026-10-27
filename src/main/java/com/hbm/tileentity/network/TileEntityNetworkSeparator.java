package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.lib.ForgeDirection;
import com.hbm.module.portmanager.ModulePortManPower;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModEnergyNetBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.uninos.PortNet;
import com.hbm.util.SavedIds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockNetworkSeparator.TileEntityNetworkSeparator}: takes power from the net on its intake end and gives
 * it to the net on its outlet end, never more than {@link #configuredLimit} per tick and never more than the outlet
 * net asks for. It logs only what it gives off. Redstone on any of its three lower blocks cuts it off.
 */
public class TileEntityNetworkSeparator extends TileEntityLoadedBase implements IEnergyProviderMK2, IEnergyReceiverMK2,
        IControlReceiver, IRORValueProvider, IRORInteractive, DiodeControl {

    public static final long MAX_LIMIT = 10_000_000_000L;

    /** Inter-tick tracker for transferred power. */
    protected long deltaTracker = 0;
    /** Final power per tick over the past second. */
    public long[] deltaTick = new long[20];
    /** Sum of the whole previous log. */
    public long deltaSecond = 0;

    private long power;
    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    public boolean redstone = false;
    public long configuredLimit = 1_000;
    public long actualLimit = configuredLimit;

    protected final ModulePortManPower inPort;
    protected final ModulePortManPower outPort;
    protected PortDef[] cachedOutPort;
    protected PortDef[] cachedInPort;

    public TileEntityNetworkSeparator(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.NETWORK_SEPARATOR.get(), pos, state);
        inPort = new ModulePortManPower(this).onlyInput();
        outPort = new ModulePortManPower(this).onlyOutput();
    }

    /** The outlet side, or the intake side when {@code opposite}: the facing turned about DOWN. */
    public ForgeDirection getDir(boolean opposite) {
        ForgeDirection dir = ForgeDirection.getOrientation(MachineCore.facingOrdinal(getBlockState(), 2)).getRotation(ForgeDirection.DOWN);
        return opposite ? dir.getOpposite() : dir;
    }

    public PortDef[] getOutPort() {
        if (cachedOutPort == null) cachedOutPort = new PortDef[]{portAt(getDir(false))};
        return cachedOutPort;
    }

    public PortDef[] getInPort() {
        if (cachedInPort == null) cachedInPort = new PortDef[]{portAt(getDir(true))};
        return cachedInPort;
    }

    private PortDef portAt(ForgeDirection dir) {
        return PortDef.make(worldPosition.getX() + dir.offsetX, worldPosition.getY(), worldPosition.getZ() + dir.offsetZ, dir);
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        ForgeDirection rot = getDir(false);
        return dir == rot || dir == rot.getOpposite();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityNetworkSeparator sep) {
        sep.inPort.update(sep.getInPort());
        sep.outPort.update(sep.getOutPort());

        sep.actualLimit = sep.configuredLimit;
        PortNet<?> net = sep.outPort.powerPorts[0].getNetwork();
        if (net != null) sep.surveyOutgoingNetwork(net);
        sep.actualLimit = Math.max(sep.actualLimit, sep.power);

        long total = sep.deltaTick[0];
        for (int i = 1; i < sep.deltaTick.length; i++) {
            total += sep.deltaTick[i];
            sep.deltaTick[i - 1] = sep.deltaTick[i];
        }
        sep.deltaTick[19] = sep.deltaTracker;
        sep.deltaTracker = 0;
        sep.deltaSecond = total;

        ForgeDirection dir = sep.getDir(false);
        sep.redstone = level.hasNeighborSignal(pos)
                || level.hasNeighborSignal(pos.offset(dir.offsetX, 0, dir.offsetZ))
                || level.hasNeighborSignal(pos.offset(-dir.offsetX, 0, -dir.offsetZ));

        sep.networkPackNT();
    }

    @Override
    public void usePower(long power) {
        this.setPower(this.getPower() - power);
        deltaTracker += power;
    }

    /** What the outlet net's receivers took last round; a separator downstream counts with its own limit. */
    protected void surveyOutgoingNetwork(PortNet<?> net) {
        long demand = 0;
        for (Object entry : net.receiverEntries) {
            if (entry instanceof TileEntityNetworkSeparator separator) {
                demand += separator.actualLimit;
            } else if (entry instanceof IEnergyHandlerMK2 rec) {
                demand += Math.min(rec.getMaxPower() - rec.getPower(), rec.getReceiverSpeed());
            }
        }

        //demand *= 1.25; // a small margin of error

        if (demand < this.actualLimit) this.actualLimit = demand;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        inPort.destroy();
        outPort.destroy();
    }

    @Override public long getReceiverSpeed() { return redstone || bufferMismatch() ? 0 : this.getMaxPower(); }
    @Override public long getProviderSpeed() { return redstone ? 0 : this.getMaxPower(); }
    @Override public long getMaxPower() { return actualLimit; }
    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public ConnectionPriority getPriority() { return this.priority; }
    @Override public long getConfiguredLimit() { return configuredLimit; }

    public boolean bufferMismatch() {
        return this.configuredLimit < this.getMaxPower();
    }

    /** 1.7 {@code EnumUtil.grabEnumSafely}: an index out of range wraps. */
    private static ConnectionPriority priorityOf(int index) {
        return ConnectionPriority.VALUES[Math.floorMod(index, ConnectionPriority.VALUES.length)];
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.power = input.getLongOr("power", 0L);
        this.configuredLimit = input.getLongOr("configuredLimit", 1_000L);
        this.actualLimit = input.getLongOr("actualLimit", configuredLimit);
        this.priority = SavedIds.getEnum(input, "p", ConnectionPriority.NORMAL);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putLong("configuredLimit", configuredLimit);
        output.putLong("actualLimit", actualLimit);
        SavedIds.putEnum(output, "p", priority);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte((byte) priority.ordinal());
        buf.writeBoolean(redstone);
        buf.writeLong(configuredLimit);
        buf.writeLong(deltaTick[19]);
        buf.writeLong(deltaSecond);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        priority = priorityOf(buf.readByte());
        redstone = buf.readBoolean();
        configuredLimit = buf.readLong();
        deltaTick[19] = buf.readLong();
        deltaSecond = buf.readLong();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 128;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "limit")) this.configuredLimit = Tags.getLongOr(data, "limit", configuredLimit);
        if (Tags.contains(data, "priority")) this.priority = priorityOf(Tags.getByteOr(data, "priority", (byte) 0));
        if (configuredLimit < 0) configuredLimit = 0;
        if (configuredLimit > MAX_LIMIT) configuredLimit = MAX_LIMIT;
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "deltatick",
                PREFIX_VALUE + "deltasecond",
                PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)",
                PREFIX_FUNCTION + "setlimit" + NAME_SEPARATOR + "limit",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "deltatick").equals(name)) return "" + deltaTick[19];
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return "" + deltaSecond;
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            // 0-2 is LOW..HIGH
            this.priority = priorityOf(IRORInteractive.parseInt(params[0], 0, 2) + 1);
            this.markChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setlimit").equals(name) && params.length > 0) {
            this.configuredLimit = IRORInteractive.parseLong(params[0], 0, MAX_LIMIT);
            this.markChanged();
            return null;
        }
        return null;
    }
}
