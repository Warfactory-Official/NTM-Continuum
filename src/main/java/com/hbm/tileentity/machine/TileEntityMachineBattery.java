package com.hbm.tileentity.machine;

import net.minecraft.core.Direction;
import com.hbm.network.power.CableData;
import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.network.power.PowerGraph;
import com.hbm.network.power.PowerGraphProvider;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import dev.golem.core.graph.Domains;
import com.hbm.inventory.container.ContainerMachineBattery;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
public class TileEntityMachineBattery extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, IEnergyProviderMK2, IEnergyConductorMK2, IRORValueProvider, IRORInteractive {

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

    private long lastPowerForDelta = 0L;
    private int deltaTimer = 0;

    /** This block's capacity: {@link #MAX_POWER} for {@code machine_battery}, the tier's own for the others. */
    private final long maxPower;

    public TileEntityMachineBattery(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MACHINE_BATTERY.get(), pos, state, MAX_POWER);
    }

    /**
     * The tiered batteries (potato / lithium / schrabidium / dineutronium) are this same machine with
     * another capacity; 1.7 read it off the block ({@code MachineBattery.maxPower}), the port hands it in.
     */
    protected TileEntityMachineBattery(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                       BlockPos pos, BlockState state, long maxPower) {
        // 4 slots like the original: 0/2 = charge (top), 1/3 = discharge (bottom). The charge/discharge
        // item logic arrives with the battery-item port; for now they are plain slots.
        super(type, pos, state, 4);
        this.maxPower = maxPower;
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

        if (level instanceof ServerLevel server) battery.updateBufferCell(server, state, mode);

        if (mode == MODE_OUTPUT) {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                battery.tryProvide(level, pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ), dir.toDirection());
            }
        }

        // Net throughput over the last second (HE/s), for the GUI bar tooltip.
        if (++battery.deltaTimer >= 20) {
            battery.delta = battery.power - battery.lastPowerForDelta;
            battery.lastPowerForDelta = battery.power;
            battery.deltaTimer = 0;
        }
        battery.setChanged();
        battery.syncState(); // broadcast power/mode/priority to tracking clients (deduped when unchanged)
    }

    /**
     * 1.7: "in buffer mode, becomes a cable block and provides power to itself". The cell is what makes
     * that literal — two cable runs meeting at the battery are one network, so a generator on either
     * side reaches a machine on the other in a single round instead of every watt crossing through
     * storage at a twentieth of the buffer a tick. Its own two roles reach that network through the
     * pseudo-face.
     */
    private void updateBufferCell(ServerLevel level, BlockState state, int mode) {
        LevelNodeGraph<CableData> graph = PowerGraph.get(level);
        long key = getBlockPos().asLong();
        if (mode != MODE_BUFFER) {
            if (graph.containsCell(key)) graph.removeNode(key);
            return;
        }
        if (!graph.containsCell(key)) {
            graph.addNode(key, PowerGraphProvider.INSTANCE.createData(state), Domains.FACES_ALL);
        }
        graph.setSelfEndpoint(key, true);
    }

    // --- IEnergyHandler / receiver / provider (isLoaded() inherited from the tower) ---

    @Override
    public void setRemoved() {
        super.setRemoved();
        PowerGraph.dropCells(this, getBlockPos());
    }

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
        return maxPower;
    }

    @Override
    public boolean canConnect(Direction dir) {
        return true;
    }

    @Override
    public ConnectionPriority getPriority() {
        return priority;
    }

    @Override
    public long getReceiverSpeed() {
        int mode = getRelevantMode();
        return (mode == MODE_INPUT || mode == MODE_BUFFER) ? getMaxPower() / 200L : 0L;
    }

    @Override
    public long getProviderSpeed() {
        int mode = getRelevantMode();
        return (mode == MODE_OUTPUT || mode == MODE_BUFFER) ? getMaxPower() / 600L : 0L;
    }

    @Override
    public boolean bufferedEndpoint() {
        return getRelevantMode() == MODE_BUFFER;
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
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineBattery(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent", PREFIX_VALUE + "delta",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
                PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + ((power * 100L) / getMaxPower());
        if ((PREFIX_VALUE + "delta").equals(name)) return "" + delta;
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int mode = IRORInteractive.parseInt(params[0], 0, 3);
            if (mode != redLow) {
                redLow = (short) mode;
            } else if (params.length > 1) {
                redLow = (short) IRORInteractive.parseInt(params[1], 0, 3);
            }
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setredmode").equals(name) && params.length > 0) {
            int mode = IRORInteractive.parseInt(params[0], 0, 3);
            if (mode != redHigh) {
                redHigh = (short) mode;
            } else if (params.length > 1) {
                redHigh = (short) IRORInteractive.parseInt(params[1], 0, 3);
            }
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            priority = ConnectionPriority.VALUES[IRORInteractive.parseInt(params[0], 0, 2) + 1];
            setChanged();
        }
        return null;
    }
}
