package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.inventory.container.ContainerMachineBattery;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Energy Storage Block — the first machine ported onto the HBM BlockEntity tower
 * ({@link TileEntityMachineBase}). It stores power and, per its configurable I/O mode, draws from /
 * pushes to / buffers the adjacent power network.
 *
 * <p>Behaviour ported from 1.12.2: two mode slots — {@link #redLow} (used while <b>not</b>
 * redstone-powered) and {@link #redHigh} (used while powered) — each one of input / buffer / output /
 * none, plus a connection {@link #priority}. The three are toggled by the GUI buttons (via the vanilla
 * {@code clickMenuButton} path) and synced to the screen on the BufPacket. (Battery <em>item</em>
 * charge/discharge in the slots is still deferred — no battery items are ported yet.)
 *
 * <p>Energy reaches the GUI via the BufPacket (a full {@code long}, not a 16-bit {@code ContainerData}).
 */
public class TileEntityMachineBattery extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, IEnergyProviderMK2, IEnergyConductorMK2 {

    public static final long MAX_POWER = 1_000_000L;
    public long power = 0L;
    /** Net power throughput in HE/s, shown on the GUI energy bar; synced to clients. */
    public long delta = 0L;

    /** I/O modes. */
    public static final int MODE_INPUT = 0, MODE_BUFFER = 1, MODE_OUTPUT = 2, MODE_NONE = 3;
    /** Mode while NOT receiving a redstone signal (default: input). */
    public short redLow = MODE_INPUT;
    /** Mode while receiving a redstone signal (default: output). */
    public short redHigh = MODE_OUTPUT;
    /** Network connection priority (LOW / NORMAL / HIGH). */
    public ConnectionPriority priority = ConnectionPriority.LOW;

    private boolean indirectlyPowered = false;
    private Nodespace.PowerNode node;

    private long lastPowerForDelta = 0L;
    private int deltaTimer = 0;

    public TileEntityMachineBattery(BlockPos pos, BlockState state) {
        // 4 slots like the original: 0/2 = charge (top), 1/3 = discharge (bottom). The charge/discharge
        // item logic arrives with the battery-item port; for now they are plain slots.
        super(ModBlockEntities.MACHINE_BATTERY.get(), pos, state, 4);
    }

    @Override
    public String getDefaultName() {
        return "container.machine_battery";
    }

    /** The mode in effect right now, picked by the redstone state. */
    public short getRelevantMode() {
        return indirectlyPowered ? redHigh : redLow;
    }

    public void cycleRedLow() { redLow = (short) ((redLow + 1) % 4); setChanged(); }
    public void cycleRedHigh() { redHigh = (short) ((redHigh + 1) % 4); setChanged(); }
    public void cyclePriority() {
        priority = switch (priority) {
            case LOW -> ConnectionPriority.NORMAL;
            case NORMAL -> ConnectionPriority.HIGH;
            default -> ConnectionPriority.LOW;
        };
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineBattery battery) {
        battery.indirectlyPowered = level.hasNeighborSignal(pos);
        int mode = battery.getRelevantMode();

        if (mode == MODE_BUFFER) {
            // Acts like a cable: owns a node and feeds power into its own network (provider + receiver).
            if (battery.node == null || battery.node.expired) {
                battery.node = UniNodespace.getNode(level, pos, Nodespace.THE_POWER_PROVIDER);
                if (battery.node == null || battery.node.expired) {
                    battery.node = battery.createNode();
                    UniNodespace.createNode(level, battery.node);
                }
            }
            battery.tryProvide(level, pos, ForgeDirection.UNKNOWN);
            if (battery.node != null && battery.node.hasValidNet()) battery.node.net.addReceiver(battery);
        } else {
            if (battery.node != null) {
                UniNodespace.destroyNode(level, pos, Nodespace.THE_POWER_PROVIDER);
                battery.node = null;
            }
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                BlockPos np = pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ);
                Nodespace.PowerNode dirNode = UniNodespace.getNode(level, np, Nodespace.THE_POWER_PROVIDER);

                if (mode == MODE_OUTPUT) battery.tryProvide(level, np, dir);
                else if (dirNode != null && dirNode.hasValidNet()) dirNode.net.removeProvider(battery);

                if (mode == MODE_INPUT) {
                    if (dirNode != null && dirNode.hasValidNet()) dirNode.net.addReceiver(battery);
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeReceiver(battery);
                }
            }
        }

        // Net throughput over the last second (HE/s), for the GUI bar tooltip.
        if (++battery.deltaTimer >= 20) {
            battery.delta = battery.power - battery.lastPowerForDelta;
            battery.lastPowerForDelta = battery.power;
            battery.deltaTimer = 0;
        }
        battery.setChanged();
        battery.networkPackNT(); // broadcast power/mode/priority to tracking clients (deduped when unchanged)
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide() && node != null) {
            UniNodespace.destroyNode(level, getBlockPos(), Nodespace.THE_POWER_PROVIDER);
        }
    }

    // --- IEnergyHandler / receiver / provider (isLoaded() inherited from the tower) ---

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return true;
    }

    @Override
    public ConnectionPriority getPriority() {
        return priority;
    }

    @Override
    public long getReceiverSpeed() {
        int mode = getRelevantMode();
        return (mode == MODE_INPUT || mode == MODE_BUFFER) ? MAX_POWER / 20L : 0L;
    }

    @Override
    public long getProviderSpeed() {
        int mode = getRelevantMode();
        return (mode == MODE_OUTPUT || mode == MODE_BUFFER) ? MAX_POWER / 20L : 0L;
    }

    // --- per-tick sync ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(delta);
        buf.writeShort(redLow);
        buf.writeShort(redHigh);
        buf.writeByte(priority.ordinal());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        delta = buf.readLong();
        redLow = buf.readShort();
        redHigh = buf.readShort();
        priority = ConnectionPriority.VALUES[buf.readByte()];
    }

    // --- persistence (inventory + muffled + name handled by the base) ---

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putShort("redLow", redLow);
        output.putShort("redHigh", redHigh);
        output.putByte("priority", (byte) priority.ordinal());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        redLow = (short) input.getShortOr("redLow", (short) MODE_INPUT);
        redHigh = (short) input.getShortOr("redHigh", (short) MODE_OUTPUT);
        priority = ConnectionPriority.VALUES[input.getByteOr("priority", (byte) ConnectionPriority.LOW.ordinal())];
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_battery");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineBattery(containerId, playerInventory, this);
    }
}
