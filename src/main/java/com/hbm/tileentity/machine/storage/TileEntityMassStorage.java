package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerMassStorage;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.TileEntityMachineBase;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * The legacy three-slot mass-storage stockpile. The block supplies the variant and registry suppliers;
 * this class contains all four capacities' shared behavior.
 */
public class TileEntityMassStorage extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

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
                                 Supplier<? extends net.minecraft.world.level.block.entity.BlockEntityType<?>> blockEntityType,
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

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && (getFilterType().isEmpty()
                || ItemStacks.sameItemAndData(stack, getFilterType()))
                || slot == FILTER_SLOT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{INPUT_SLOT, OUTPUT_SLOT};
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{INPUT_SLOT, OUTPUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == INPUT_SLOT && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getDefaultName());
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
        CompoundTag data = new CompoundTag();
        data.putInt("stack", stockpile);
        data.putBoolean("output", output);
        data.putInt("capacity", capacity);
        data.putByte("redstone", (byte) redstone);
        return data;
    }

    @Override
    protected void applyCarriedData(CompoundTag data) {
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
}
