package com.hbm.tileentity.network.pneumatic;

import com.hbm.tileentity.IControlReceiverFilter;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.network.PneumoStorageMono;
import com.hbm.inventory.container.ContainerPneumoStorageMono;
import com.hbm.tileentity.ModPneumaticBlockEntities;

/**
 * 1.7 {@code TileEntityPneumoStorageMono}: three typed cells of 100,000 each. The three slots hold the type markers
 * (ghost stacks); the counts live in {@link #amounts}. Both travel with the dropped block.
 */
public class TileEntityPneumoStorageMono extends TileEntityPneumaticStorageBase implements IControlReceiverFilter {

    public static final int SLOT_COUNT = 3;
    public static final int CAPACITY = 100_000;

    public int[] amounts;

    public TileEntityPneumoStorageMono(BlockPos pos, BlockState state) {
        super(ModPneumaticBlockEntities.PNEUMATIC_STORAGE_MONO.get(), pos, state, SLOT_COUNT);
        this.amounts = new int[this.monitors.length];
    }

    @Override
    public String getDefaultName() {
        return "container.pneumoStorageMono";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerPneumoStorageMono(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (int amount : amounts) buf.writeInt(amount);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (int i = 0; i < amounts.length; i++) amounts[i] = buf.readInt();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        int[] loaded = input.getIntArrayOr("amounts", null);
        if (loaded != null && loaded.length == SLOT_COUNT) this.amounts = loaded;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putIntArray("amounts", amounts);
    }

    // --- 1.7 PneumoStorageMono.removedByPlayer: slots and amounts ride the dropped item ---

    @Override
    protected NonNullList<ItemStack> carriedItems() {
        return inventory.getStacks();
    }

    @Override
    protected CompoundTag collectCarriedData() {
        CompoundTag data = new CompoundTag();
        data.putIntArray("amounts", amounts.clone());
        return data;
    }

    @Override
    protected void applyCarriedData(CompoundTag data) {
        int[] loaded = Tags.getIntArrayOr(data, "amounts", null);
        if (loaded != null && loaded.length == SLOT_COUNT) this.amounts = loaded.clone();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // the type markers travel with the item; spilling them would drop ghosts
    }

    @Override
    protected void removeSavedComponents(DataOut output) {
        super.removeSavedComponents(output);
        output.discard("inventory");
        output.discard("amounts");
        output.discard("customName");
    }

    /** 1.7: a cell holding items keeps its type marker, whether a click, a filter packet or a paste writes it. */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (level != null && !level.isClientSide() && amounts[slot] > 0) return;
        super.setItem(slot, stack);
    }

    @Override
    public long getAmountAt(int index) {
        return amounts[index];
    }

    @Override
    public boolean allowTypeSetting() {
        return false;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        super.receiveControl(data);
        if (Tags.contains(data, "slot")) {
            setFilterContents(data);
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return super.hasPermission(player);
    }

    @Override
    public long useUpItem(int index, long amount) {
        if (amounts[index] <= 0) return amount;
        int toRemove = (int) Math.min(amount, amounts[index]);
        amounts[index] -= toRemove;
        setChanged();
        return amount - toRemove;
    }

    @Override
    public long addItem(int index, long amount) {
        int capacity = CAPACITY - amounts[index];
        if (capacity <= 0) return amount;
        int toAdd = (int) Math.min(amount, capacity);
        amounts[index] += toAdd;
        setChanged();
        return amount - toAdd;
    }

    @Override
    public long setupType(int index, ItemStack zeroStack, long amount) {
        return amount;
    }

    @Override
    public void nextMode(int i) {
    }

    @Override
    public int[] getFilterSlots() {
        return new int[]{0, SLOT_COUNT};
    }
}
