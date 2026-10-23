package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerMassStorage;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.items.ModFluidNetItems;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * The legacy three-slot mass-storage stockpile. The block supplies the variant and registry suppliers;
 * this class contains all four capacities' shared behavior.
 */
public class TileEntityMassStorage extends TileEntityCrateBase implements IRORValueProvider, IRORInteractive {

    public static final int INPUT_SLOT = 0;
    public static final int FILTER_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    public final MassStorageType storageType;
    private final Supplier<MenuType<ContainerMassStorage>> menuType;
    private int capacity;
    private int stockpile;
    public boolean output;
    public int redstone;

    public TileEntityMassStorage(MassStorageType storageType,
                                 Supplier<? extends BlockEntityType<?>> blockEntityType,
                                 Supplier<MenuType<ContainerMassStorage>> menuType,
                                 BlockPos pos, BlockState state) {
        super(blockEntityType.get(), pos, state, 3);
        this.storageType = storageType;
        this.capacity = storageType.capacity;
        this.menuType = menuType;
    }

    /** Registered game instances use the lazy suppliers wired by the central rows. */
    public TileEntityMassStorage(MassStorageType storageType, BlockPos pos, BlockState state) {
        this(storageType, storageType::blockEntityType, storageType::menuType, pos, state);
    }

    @Override
    public String getDefaultName() {
        return storageType.defaultName;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMassStorage storage) {
        int newRedstone = storage.getComparatorPower();
        if (newRedstone != storage.redstone) {
            storage.redstone = newRedstone;
            storage.setChanged();
        }

        ItemStack type = storage.getFilterType();
        ItemStack input = storage.inventory.getStackInSlot(INPUT_SLOT);
        if (input.is(ModFluidNetItems.fluid_barrel_infinite.get())) storage.stockpile = storage.capacity;
        if (type.isEmpty()) storage.stockpile = 0;

        if (!type.isEmpty() && storage.stockpile < storage.capacity
                && !input.isEmpty() && ItemStacks.sameItemAndData(input, type)) {
            int amount = Math.min(storage.capacity - storage.stockpile, input.getCount());
            storage.inventory.setStackInSlot(INPUT_SLOT, ItemStacks.copyWithCount(input, input.getCount() - amount));
            storage.stockpile += amount;
            storage.setChanged();
        }

        if (storage.output && !type.isEmpty()) {
            storage.pushOutput(Math.min(storage.stockpile, type.getMaxStackSize()));
        }

        storage.networkPackNT();
    }

    private int pushOutput(int requested) {
        if (requested <= 0) return 0;
        ItemStack type = getFilterType();
        ItemStack existing = inventory.getStackInSlot(OUTPUT_SLOT);
        if (!existing.isEmpty() && !ItemStacks.sameItemAndData(existing, type)) return 0;

        int room = existing.isEmpty() ? type.getMaxStackSize()
                : type.getMaxStackSize() - existing.getCount();
        int amount = Math.min(requested, Math.max(0, room));
        if (amount <= 0) return 0;

        if (existing.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, ItemStacks.copyWithCount(type, amount));
        } else {
            inventory.setStackInSlot(OUTPUT_SLOT, ItemStacks.copyWithCount(existing, existing.getCount() + amount));
        }
        stockpile -= amount;
        setChanged();
        return amount;
    }

    public int getCapacity() {
        return capacity;
    }

    public ItemStack getFilterType() {
        ItemStack type = inventory.getStackInSlot(FILTER_SLOT);
        return type.isEmpty() ? ItemStack.EMPTY : ItemStacks.copyWithCount(type, 1);
    }

    public int getStockpile() {
        return stockpile;
    }

    public void setStockpile(int stockpile) {
        this.stockpile = Math.max(0, Math.min(stockpile, capacity));
        setChanged();
    }

    public void setFilter(ItemStack stack) {
        if (stockpile > 0) return;
        inventory.setStackInSlot(FILTER_SLOT, stack.isEmpty() ? ItemStack.EMPTY : ItemStacks.copyWithCount(stack, 1));
        setChanged();
        networkPackNT();
    }

    public void provide(boolean stack) {
        ItemStack type = getFilterType();
        if (type.isEmpty() || stockpile <= 0) return;
        pushOutput(Math.min(stockpile, stack ? type.getMaxStackSize() : 1));
        networkPackNT();
    }

    public void toggleOutput() {
        output = !output;
        setChanged();
        networkPackNT();
    }

    public int getComparatorPower() {
        return capacity <= 0 ? 0 : Math.max(0, Math.min(15, stockpile * 15 / capacity));
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == INPUT_SLOT || slot == FILTER_SLOT;
    }

    private boolean matchesFilter(ItemStack stack) {
        ItemStack type = getFilterType();
        return type.isEmpty() || ItemStacks.sameItemAndData(stack, type);
    }

    /** 1.7 {@code canInsert}: the stockpile takes only the filtered type. */
    public boolean canInsert(ItemStack stack) {
        ItemStack type = getFilterType();
        return !type.isEmpty() && stockpile < capacity && !stack.isEmpty() && ItemStacks.sameItemAndData(stack, type);
    }

    /** 1.7 {@code quickInsert}: shift-click moves a whole stack straight into the stockpile if all of it fits. */
    public boolean quickInsert(ItemStack stack) {
        if (!canInsert(stack) || capacity - stockpile < stack.getCount()) return false;
        stockpile += stack.getCount();
        stack.setCount(0);
        setChanged();
        return true;
    }

    /** 1.7: the stockpile plus matching input and output stacks, as AE2 storage buses see it. */
    public int getTotalStockpile() {
        ItemStack type = getFilterType();
        if (type.isEmpty()) return 0;
        int result = stockpile;
        ItemStack in = inventory.getStackInSlot(INPUT_SLOT);
        if (ItemStacks.sameItemAndData(type, in)) result += in.getCount();
        ItemStack out = inventory.getStackInSlot(OUTPUT_SLOT);
        if (ItemStacks.sameItemAndData(type, out)) result += out.getCount();
        return result;
    }

    /** Returns the remainder that did not fit; with {@code actually} false only predicts it. */
    public int increaseTotalStockpile(int amount, boolean actually) {
        return changeTotalStockpile(amount, actually, +1);
    }

    /** Returns the remainder that could not be taken; with {@code actually} false only predicts it. */
    public int decreaseTotalStockpile(int amount, boolean actually) {
        return changeTotalStockpile(amount, actually, -1);
    }

    private int changeTotalStockpile(int amount, boolean actually, int sign) {
        ItemStack type = getFilterType();
        if (type.isEmpty()) return amount;

        int stockpileAvail = sign > 0 ? capacity - stockpile : stockpile;
        if (amount > 0 && stockpileAvail > 0) {
            int deposit = Math.min(amount, stockpileAvail);
            if (actually) stockpile += sign * deposit;
            amount -= deposit;
        }
        amount = changeSlot(INPUT_SLOT, type, amount, actually, sign);
        amount = changeSlot(OUTPUT_SLOT, type, amount, actually, sign);
        if (actually) setChanged();
        return amount;
    }

    private int changeSlot(int slot, ItemStack type, int amount, boolean actually, int sign) {
        ItemStack stack = inventory.getStackInSlot(slot);
        int avail = 0;
        if (ItemStacks.sameItemAndData(type, stack)) {
            avail = sign > 0 ? stack.getMaxStackSize() - stack.getCount() : stack.getCount();
        } else if (stack.isEmpty()) {
            avail = sign > 0 ? type.getMaxStackSize() : 0;
        }
        if (amount <= 0 || avail <= 0) return amount;
        int moved = Math.min(amount, avail);
        if (actually) {
            int count = (stack.isEmpty() ? 0 : stack.getCount()) + sign * moved;
            inventory.setStackInSlot(slot, count == 0 ? ItemStack.EMPTY : ItemStacks.copyWithCount(type, count));
        }
        return amount - moved;
    }

    /** 1.7 {@code quickExtract}: one full stack of the filtered type, or empty when output is off or the stockpile is short. */
    public ItemStack quickExtract() {
        ItemStack type = getFilterType();
        if (!output || type.isEmpty() || stockpile < type.getMaxStackSize()) return ItemStack.EMPTY;
        stockpile -= type.getMaxStackSize();
        setChanged();
        return ItemStacks.copyWithCount(type, type.getMaxStackSize());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{INPUT_SLOT, OUTPUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isLocked() && slot == INPUT_SLOT && matchesFilter(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isLocked() && slot == OUTPUT_SLOT;
    }

    public void openInventory(Player player) {
        if (level != null && !level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.STORAGE_OPEN.get(), SoundSource.BLOCKS, 1F, 1F);
        }
    }

    public void closeInventory(Player player) {
        if (level != null && !level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.STORAGE_CLOSE.get(), SoundSource.BLOCKS, 1F, 1F);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMassStorage(containerId, playerInventory, this, menuType);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(stockpile);
        buf.writeBoolean(output);
        ItemStacks.writeOptional(buf, inventory.getStackInSlot(FILTER_SLOT));
        buf.writeInt(capacity);
        buf.writeByte(redstone);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        stockpile = Math.max(0, buf.readInt());
        output = buf.readBoolean();
        inventory.setStackInSlot(FILTER_SLOT, ItemStacks.copyWithCount(ItemStacks.readOptional(buf), 1));
        int syncedCapacity = buf.readInt();
        capacity = syncedCapacity > 0 ? syncedCapacity : storageType.capacity;
        redstone = buf.readByte();
        if (capacity <= 0) capacity = storageType.capacity;
        stockpile = Math.min(stockpile, capacity);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("stack", stockpile);
        output.putBoolean("output", this.output);
        output.putInt("capacity", capacity);
        output.putByte("redstone", (byte) redstone);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        stockpile = Math.max(0, input.getIntOr("stack", 0));
        output = input.getBooleanOr("output", false);
        capacity = input.getIntOr("capacity", storageType.capacity);
        if (capacity <= 0) capacity = storageType.capacity;
        redstone = input.getByteOr("redstone", (byte) 0);
        stockpile = Math.min(stockpile, capacity);
    }

    @Override
    protected NonNullList<ItemStack> carriedItems() {
        return inventory.getStacks();
    }

    @Override
    protected CompoundTag collectCarriedData() {
        CompoundTag lock = super.collectCarriedData();
        CompoundTag data = lock == null ? new CompoundTag() : lock;
        data.putInt("stack", stockpile);
        data.putBoolean("output", output);
        data.putInt("capacity", capacity);
        data.putByte("redstone", (byte) redstone);
        return data;
    }

    @Override
    protected void applyCarriedData(CompoundTag data) {
        super.applyCarriedData(data);
        loadItemState(data);
    }

    @Override
    protected void removeSavedComponents(DataOut output) {
        super.removeSavedComponents(output);
        output.discard("inventory");
        output.discard("stack");
        output.discard("output");
        output.discard("capacity");
        output.discard("redstone");
        output.discard("customName");
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // The contents and stockpile travel with the block item; do not spill a second copy.
    }

    private void loadItemState(CompoundTag data) {
        stockpile = Math.max(0, Math.min(Tags.getIntOr(data, "stack", 0), capacity));
        output = Tags.getBooleanOr(data, "output", false);
        int carriedCapacity = Tags.getIntOr(data, "capacity", 0);
        if (carriedCapacity > 0) capacity = carriedCapacity;
        redstone = Tags.getByteOr(data, "redstone", (byte) 0);
        stockpile = Math.min(stockpile, capacity);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "type", PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent",
                PREFIX_FUNCTION + "toggleoutput"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + stockpile;
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + (stockpile * 100 / capacity);
        if ((PREFIX_VALUE + "type").equals(name)) {
            ItemStack type = inventory.getStackInSlot(FILTER_SLOT);
            return type.isEmpty() ? "None" : type.getHoverName().getString();
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "toggleoutput").equals(name)) {
            output = !output;
            setChanged();
        }
        return null;
    }
}
