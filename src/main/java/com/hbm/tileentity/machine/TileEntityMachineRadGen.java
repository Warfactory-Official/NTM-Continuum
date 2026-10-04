package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.inventory.container.ContainerMachineRadGen;
import com.hbm.items.ModItems;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/** Twelve independent radiation-fuel generators, preserving the legacy channel timings and output slots. */
public class TileEntityMachineRadGen extends TileEntityMachineBase
        implements MenuProvider, IEnergyProviderMK2, WorldlyContainer {

    public static final int CHANNELS = 12;
    public static final long MAX_POWER = 1_000_000L;

    public final int[] progress = new int[CHANNELS];
    public final int[] maxProgress = new int[CHANNELS];
    public final int[] production = new int[CHANNELS];
    public final ItemStack[] processing = new ItemStack[CHANNELS];
    public long power;
    public boolean isOn;

    private record Fuel(int production, int duration, ItemStack output) {}

    private static final Map<Item, Fuel> FUELS = Map.of(
            ModItems.scrap_nuclear.get(), new Fuel(50, 5 * 60 * 20, ItemStack.EMPTY),
            ModItems.gem_rad.get(), new Fuel(25_000, 30 * 60 * 20, new ItemStack(Items.DIAMOND)));

    public TileEntityMachineRadGen(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_RAD_GEN.get(), pos, state, 24);
        for (int i = 0; i < CHANNELS; i++) processing[i] = ItemStack.EMPTY;
    }

    @Override
    public String getDefaultName() {
        return "container.radGen";
    }

    /**
     * 1.7.10's fuel table: every waste class of {@code nuclear_waste_short} (1,500 HE/t, 30 min),
     * {@code _short_tiny} (150, 3 min), {@code _long} (500, 2 h) and {@code _long_tiny} (50, 12 min), each
     * leaving its depleted counterpart of the same class, plus scrap and gem_rad.
     */
    private static Fuel getFuel(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item instanceof com.hbm.items.special.ItemWasteStorage waste) {
            int variant = waste.variant(stack);
            if (item == com.hbm.items.ModStorageDrumItems.nuclear_waste_short.get())
                return wasteFuel(1500, 30 * 60 * 20, com.hbm.items.ModStorageDrumItems.nuclear_waste_short_depleted.get(), variant);
            if (item == com.hbm.items.ModStorageDrumItems.nuclear_waste_short_tiny.get())
                return wasteFuel(150, 3 * 60 * 20, com.hbm.items.ModStorageDrumItems.nuclear_waste_short_depleted_tiny.get(), variant);
            if (item == com.hbm.items.ModStorageDrumItems.nuclear_waste_long.get())
                return wasteFuel(500, 2 * 60 * 60 * 20, com.hbm.items.ModStorageDrumItems.nuclear_waste_long_depleted.get(), variant);
            if (item == com.hbm.items.ModStorageDrumItems.nuclear_waste_long_tiny.get())
                return wasteFuel(50, 12 * 60 * 20, com.hbm.items.ModStorageDrumItems.nuclear_waste_long_depleted_tiny.get(), variant);
            return null;
        }
        return FUELS.get(item);
    }

    private static Fuel wasteFuel(int production, int duration, Item depleted, int variant) {
        return new Fuel(production, duration, com.hbm.items.special.ItemWasteStorage.make(depleted, variant));
    }

    public int getProgressScaled(int channel, int scale) {
        return maxProgress[channel] <= 0 ? 0 : progress[channel] * scale / maxProgress[channel];
    }

    public int getPowerScaled(int scale) {
        return (int) (power * scale / MAX_POWER);
    }

    private boolean canStoreOutput(int channel, Fuel fuel) {
        if (fuel.output().isEmpty()) return true;
        ItemStack existing = inventory.getStackInSlot(channel + CHANNELS);
        if (existing.isEmpty()) return true;
        return ItemStacks.sameItemAndData(existing, fuel.output())
                && existing.getCount() + fuel.output().getCount()
                <= Math.min(existing.getMaxStackSize(), inventory.getSlotLimit(channel + CHANNELS));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRadGen generator) {
        generator.isOn = false;

        for (DirPos connection : generator.getConPos()) {
            generator.tryProvide(level, connection.getPos(), connection.getDir());
        }

        for (int i = 0; i < CHANNELS; i++) {
            if (!generator.processing[i].isEmpty()) continue;
            ItemStack input = generator.inventory.getStackInSlot(i);
            Fuel fuel = getFuel(input);
            if (fuel == null || !generator.canStoreOutput(i, fuel)) continue;

            generator.progress[i] = 0;
            generator.maxProgress[i] = fuel.duration();
            generator.production[i] = fuel.production();
            generator.processing[i] = input.copy();
            generator.processing[i].setCount(1);
            generator.inventory.extractItem(i, 1, false);
        }

        for (int i = 0; i < CHANNELS; i++) {
            if (generator.processing[i].isEmpty()) continue;

            generator.isOn = true;
            generator.power = Math.min(MAX_POWER, generator.power + generator.production[i]);
            generator.progress[i]++;
            if (generator.progress[i] < generator.maxProgress[i]) continue;

            Fuel fuel = getFuel(generator.processing[i]);
            if (fuel != null && !fuel.output().isEmpty()) {
                ItemStack output = generator.inventory.getStackInSlot(i + CHANNELS);
                if (output.isEmpty()) {
                    generator.inventory.setStackInSlot(i + CHANNELS, fuel.output().copy());
                } else {
                    output.grow(fuel.output().getCount());
                    generator.inventory.setStackInSlot(i + CHANNELS, output);
                }
            }
            generator.progress[i] = 0;
            generator.processing[i] = ItemStack.EMPTY;
        }

        generator.setChanged();
        generator.networkPackNT();
    }

    /** The rear port is four blocks behind the core, adjacent to the extra dummy cell. */
    public DirPos[] getConPos() {
        ForgeDirection facing = MachineCore.facing(getBlockState());
        return new DirPos[]{new DirPos(
                worldPosition.getX() - facing.offsetX * 4,
                worldPosition.getY(),
                worldPosition.getZ() - facing.offsetZ * 4,
                facing.getOpposite())};
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= CHANNELS) return false;
        Fuel fuel = getFuel(stack);
        if (fuel == null) return false;
        if (inventory.getStackInSlot(slot).isEmpty()) return true;

        int size = inventory.getStackInSlot(slot).getCount();
        for (int i = 0; i < CHANNELS; i++) {
            ItemStack existing = inventory.getStackInSlot(i);
            if (existing.isEmpty()) return false;
            if (ItemStacks.sameItemAndData(existing, stack) && existing.getCount() < size) return false;
        }
        return true;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        int[] slots = new int[getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return getAccessibleSlotsFromSide(side.ordinal());
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < CHANNELS && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= CHANNELS;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.radGen");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineRadGen(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (int value : progress) buf.writeInt(value);
        for (int value : maxProgress) buf.writeInt(value);
        for (int value : production) buf.writeInt(value);
        buf.writeLong(power);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (int i = 0; i < CHANNELS; i++) progress[i] = buf.readInt();
        for (int i = 0; i < CHANNELS; i++) maxProgress[i] = buf.readInt();
        for (int i = 0; i < CHANNELS; i++) production[i] = buf.readInt();
        power = buf.readLong();
        isOn = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putIntArray("progress", progress);
        output.putIntArray("maxProgress", maxProgress);
        output.putIntArray("production", production);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);

        DataOut.ChildList list = output.childrenList("progressing");
        for (int i = 0; i < CHANNELS; i++) {
            if (processing[i].isEmpty()) continue;
            DataOut child = list.addChild();
            child.putInt("slot", i);
            child.store("stack", ItemStack.CODEC, processing[i]);
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        copyArray(input.getIntArrayOr("progress", new int[CHANNELS]), progress);
        copyArray(input.getIntArrayOr("maxProgress", new int[CHANNELS]), maxProgress);
        copyArray(input.getIntArrayOr("production", new int[CHANNELS]), production);
        power = input.getLongOr("power", 0L);
        isOn = input.getBooleanOr("isOn", false);
        for (int i = 0; i < CHANNELS; i++) processing[i] = ItemStack.EMPTY;
        for (DataIn child : input.childrenListOrEmpty("progressing")) {
            int slot = child.getIntOr("slot", -1);
            if (slot >= 0 && slot < CHANNELS) {
                child.read("stack", ItemStack.CODEC).ifPresent(stack -> processing[slot] = stack);
            }
        }
    }

    private static void copyArray(int[] source, int[] target) {
        if (source.length == target.length) System.arraycopy(source, 0, target, 0, target.length);
    }
}
