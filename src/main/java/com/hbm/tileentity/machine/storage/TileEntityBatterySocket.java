package com.hbm.tileentity.machine.storage;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.ContainerBatterySocket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * The Battery Socket multiblock's core BlockEntity. Unlike the {@code machine_battery} block (which stores
 * its own {@code long power}), the socket holds <b>no internal energy</b> — its power IS the charge of the
 * {@link IBatteryItem} placed in slot 0. {@link #getPower()}/{@link #setPower(long)}/{@link #getMaxPower()}
 * read and write that item's charge, so charging the socket from the grid literally fills the battery item,
 * which can then be carried away full. Acts as a cable: all 8 connection ports share one network.
 *
 * <p>Slim port of 1.12.2's {@code TileEntityBatterySocket}: the SC-battery discharge (lightning beams +
 * area explosion), OpenComputers, and Redstone-over-Radio hooks are dropped — they depend on the
 * vanillant explosion / bullet-beam subsystems which aren't ported. The core socket behaviour (battery as
 * storage, I/O modes, priority, comparator, the carried-stack sync for rendering) is kept.
 */
public class TileEntityBatterySocket extends TileEntityBatteryBase implements MenuProvider {

    /** Net throughput over the last second (HE/s), for the GUI energy-bar tooltip; synced to clients. */
    public long delta = 0L;
    /** Carried battery stack, synced to clients so the renderer can draw the inserted battery. */
    public ItemStack syncStack = ItemStack.EMPTY;

    private final long[] log = new long[20];

    public TileEntityBatterySocket(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_SOCKET.get(), pos, state, 1);
    }

    @Override
    public String getDefaultName() {
        return "container.batterySocket";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityBatterySocket socket) {
        long prevPower = socket.getPower();
        socket.serverTickBattery(level);

        // Net throughput over the last second (HE/s), averaged like the 1.12.2 socket's rolling log.
        long avg = (socket.getPower() + prevPower) / 2;
        socket.delta = avg - socket.log[0];
        System.arraycopy(socket.log, 1, socket.log, 0, socket.log.length - 1);
        socket.log[socket.log.length - 1] = avg;

        socket.syncStack = socket.inventory.getStackInSlot(0);
    }

    // --- the inserted battery item IS the storage ---

    private ItemStack battery() {
        return inventory.getStackInSlot(0);
    }

    public static long powerFromStack(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IBatteryItem bat ? bat.getCharge(stack) : 0L;
    }

    public static long maxPowerFromStack(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IBatteryItem bat ? bat.getMaxCharge(stack) : 0L;
    }

    @Override
    public long getPower() {
        return powerFromStack(battery());
    }

    @Override
    public long getMaxPower() {
        return maxPowerFromStack(battery());
    }

    @Override
    public void setPower(long power) {
        ItemStack stack = battery();
        if (stack.getItem() instanceof IBatteryItem bat) bat.setCharge(stack, power);
    }

    @Override
    public long getReceiverSpeed() {
        ItemStack stack = battery();
        if (!(stack.getItem() instanceof IBatteryItem bat)) return 0L;
        int mode = getRelevantMode(true);
        return (mode == mode_input || mode == mode_buffer) ? bat.getChargeRate(stack) : 0L;
    }

    @Override
    public long getProviderSpeed() {
        ItemStack stack = battery();
        if (!(stack.getItem() instanceof IBatteryItem bat)) return 0L;
        int mode = getRelevantMode(true);
        return (mode == mode_output || mode == mode_buffer) ? bat.getDischargeRate(stack) : 0L;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof IBatteryItem;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{0};
    }

    // --- multiblock port geometry (ported verbatim from the 1.12.2 socket) ---

    private ForgeDirection facing() {
        return ForgeDirection.getOrientation(getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset);
    }

    @Override
    public BlockPos[] getPortPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = getBlockPos().getX(), y = getBlockPos().getY(), z = getBlockPos().getZ();
        return new BlockPos[]{
                new BlockPos(x, y, z),
                new BlockPos(x - dir.offsetX, y, z - dir.offsetZ),
                new BlockPos(x + rot.offsetX, y, z + rot.offsetZ),
                new BlockPos(x - dir.offsetX + rot.offsetX, y, z - dir.offsetZ + rot.offsetZ)};
    }

    @Override
    public DirPos[] getConPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = getBlockPos().getX(), y = getBlockPos().getY(), z = getBlockPos().getZ();
        return new DirPos[]{
                new DirPos(x + dir.offsetX, y, z + dir.offsetZ, dir),
                new DirPos(x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ, dir),

                new DirPos(x - dir.offsetX * 2, y, z - dir.offsetZ * 2, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),

                new DirPos(x + rot.offsetX * 2, y, z + rot.offsetZ * 2, rot),
                new DirPos(x + rot.offsetX * 2 - dir.offsetX, y, z + rot.offsetZ * 2 - dir.offsetZ, rot),

                new DirPos(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
                new DirPos(x - rot.offsetX - dir.offsetX, y, z - rot.offsetZ - dir.offsetZ, rot.getOpposite())};
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_battery_socket");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerBatterySocket(containerId, playerInventory, this);
    }

    // --- sync (BufPacket) + persistence: add delta + the carried stack on top of the base fields ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(delta);
        dev.golem.core.world.ItemStacks.writeOptional(buf, inventory.getStackInSlot(0));
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        delta = buf.readLong();
        syncStack = dev.golem.core.world.ItemStacks.readOptional(buf);
    }
}
