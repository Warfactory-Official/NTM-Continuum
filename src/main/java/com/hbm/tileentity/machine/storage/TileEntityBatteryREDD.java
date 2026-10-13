package com.hbm.tileentity.machine.storage;

import com.hbm.client.sound.FensuSoundHandler;
import dev.golem.core.data.Tags;

import com.hbm.tileentity.MachineCore;
import com.hbm.inventory.container.ContainerBatteryREDD;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.math.BigInteger;

/** Current FEnSU storage: the legacy arbitrary-precision six-port energy bank. */
public class TileEntityBatteryREDD extends TileEntityBatteryBase implements MenuProvider, WorldlyContainer {

    public static final long MAX_POWER = Long.MAX_VALUE / 100L;
    private static final BigInteger TWO = BigInteger.valueOf(2L);

    public BigInteger power = BigInteger.ZERO;
    public BigInteger delta = BigInteger.ZERO;
    public float prevRotation;
    public float rotation;
    public final BigInteger[] log = new BigInteger[20];

    public TileEntityBatteryREDD(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_REDD.get(), pos, state, 2);
        Arrays.fill(log, BigInteger.ZERO);
    }

    @Override
    public String getDefaultName() {
        return "container.batteryREDD";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityBatteryREDD battery) {
        BigInteger previous = battery.power;
        battery.serverTickBattery(level);

        // The old REDD bank feeds slot 0 into its internal store and drains its exposed half into slot 1.
        long toAdd = Library.chargeTEFromItems(battery.inventory, 0, 0L, battery.getMaxPower());
        if (toAdd > 0L) battery.power = battery.power.add(BigInteger.valueOf(toAdd));

        long exposedBefore = battery.getPower();
        long exposedAfter = Library.chargeItemsFromTE(battery.inventory, 1, exposedBefore, battery.getMaxPower());
        long toRemove = exposedBefore - exposedAfter;
        if (toRemove > 0L) battery.power = battery.power.subtract(BigInteger.valueOf(toRemove));

        BigInteger average = battery.power.add(previous).divide(TWO);
        battery.delta = average.subtract(battery.log[0]);
        System.arraycopy(battery.log, 1, battery.log, 0, battery.log.length - 1);
        battery.log[battery.log.length - 1] = average;
        battery.setChanged();
        battery.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityBatteryREDD battery) {
        battery.prevRotation = battery.rotation;
        battery.rotation += battery.getSpeed();
        if (battery.rotation >= 360F) {
            battery.rotation -= 360F;
            battery.prevRotation -= 360F;
        }
        FensuSoundHandler.tickLoop(battery);
    }

    /** Exact old REDD speed curve, including its 15 rad/tick upper bound. */
    public float getSpeed() {
        return (float) Math.min(Math.pow(Math.log(power.doubleValue() * 0.05D + 1D) * 0.05F, 5), 15F);
    }

    @Override
    public long getPower() {
        return power.min(BigInteger.valueOf(MAX_POWER / 2L)).longValue();
    }

    @Override
    public void setPower(long power) {
        // REDD uses transferPower/usePower directly; replacing the BigInteger store would truncate it.
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public long transferPower(long incoming, boolean simulate) {
        if (!simulate && incoming > 0L) power = power.add(BigInteger.valueOf(incoming));
        return 0L;
    }

    @Override
    public void usePower(long amount) {
        if (amount > 0L) power = power.subtract(BigInteger.valueOf(amount));
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return (slot == 0 || slot == 1) && Library.isBattery(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    private ForgeDirection facing() {
        return MachineCore.facing(getBlockState());
    }

    @Override
    public BlockPos[] getPortPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        BlockPos pos = getBlockPos();
        return new BlockPos[]{
                pos.offset(dir.offsetX * 2 + rot.offsetX * 2, 0, dir.offsetZ * 2 + rot.offsetZ * 2),
                pos.offset(dir.offsetX * 2 - rot.offsetX * 2, 0, dir.offsetZ * 2 - rot.offsetZ * 2),
                pos.offset(-dir.offsetX * 2 + rot.offsetX * 2, 0, -dir.offsetZ * 2 + rot.offsetZ * 2),
                pos.offset(-dir.offsetX * 2 - rot.offsetX * 2, 0, -dir.offsetZ * 2 - rot.offsetZ * 2),
                pos.offset(rot.offsetX * 4, 0, rot.offsetZ * 4),
                pos.offset(-rot.offsetX * 4, 0, -rot.offsetZ * 4)
        };
    }

    @Override
    public DirPos[] getConPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        BlockPos pos = getBlockPos();
        return new DirPos[]{
                new DirPos(pos.getX() + dir.offsetX * 3 + rot.offsetX * 2, pos.getY(),
                        pos.getZ() + dir.offsetZ * 3 + rot.offsetZ * 2, dir),
                new DirPos(pos.getX() + dir.offsetX * 3 - rot.offsetX * 2, pos.getY(),
                        pos.getZ() + dir.offsetZ * 3 - rot.offsetZ * 2, dir),
                new DirPos(pos.getX() - dir.offsetX * 3 + rot.offsetX * 2, pos.getY(),
                        pos.getZ() - dir.offsetZ * 3 + rot.offsetZ * 2, dir.getOpposite()),
                new DirPos(pos.getX() - dir.offsetX * 3 - rot.offsetX * 2, pos.getY(),
                        pos.getZ() - dir.offsetZ * 3 - rot.offsetZ * 2, dir.getOpposite()),
                new DirPos(pos.getX() + rot.offsetX * 5, pos.getY(),
                        pos.getZ() + rot.offsetZ * 5, rot),
                new DirPos(pos.getX() - rot.offsetX * 5, pos.getY(),
                        pos.getZ() - rot.offsetZ * 5, rot.getOpposite())
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerBatteryREDD(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        writeBigInteger(buf, power);
        writeBigInteger(buf, delta);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = readBigInteger(buf);
        delta = readBigInteger(buf);
    }

    private static void writeBigInteger(RegistryFriendlyByteBuf buf, BigInteger value) {
        byte[] bytes = value.toByteArray();
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static BigInteger readBigInteger(RegistryFriendlyByteBuf buf) {
        int length = buf.readInt();
        if (length < 1 || length > 4096) return BigInteger.ZERO;
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new BigInteger(bytes);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putString("power", power.toString());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        try {
            power = new BigInteger(input.getStringOr("power", "0"));
        } catch (NumberFormatException exception) {
            power = BigInteger.ZERO;
        }
        if (power.signum() < 0) power = BigInteger.ZERO;
    }
}
