package com.hbm.tileentity.network.pneumatic;

import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.inventory.StackCache.CacheSlot;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.BobMathUtil;
import dev.golem.core.Ids;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import com.hbm.blocks.network.PneumoStorageExporter;
import com.hbm.inventory.container.ContainerPneumoStorageExporter;
import com.hbm.tileentity.ModPneumaticBlockEntities;

/**
 * 1.7 {@code TileEntityPneumoStorageExporter}: nine request filters (their stack size is the requested amount)
 * pulling from the network into nine take-only output slots. 1.7's RoR filters were (item id, meta, amount)
 * triples; item ids are registry names here and the meta axis is gone, so a RoR filter is one stack whose
 * count is the amount.
 */
public class TileEntityPneumoStorageExporter extends TileEntityPneumaticMachineBase
        implements IRORInteractive, IControlReceiverFilter, WorldlyContainer {

    /** If requests should be pulled repeatedly every tick. */
    public boolean continuousRequest = false;
    /** If RoR configuration has taken place, ignore manually defined filters entirely. */
    public boolean rorConfiguredMode = false;
    /** What strategy to use for handling request filters. */
    public int requestMode = 0;
    /** RoR controlled filters, count = amount. */
    public ItemStack[] rorFilters = new ItemStack[9];
    /** Delay for non-forced (i.e. continuous request) grabs, if not successful. */
    public int[] slotDelay = new int[9];
    public static final int SLOT_DELAY = 10;

    /** Each slot individually tries to pull as much as it can of the configured item. */
    public static final int MODE_AS_MUCH_AS_POSSIBLE = 0;
    /** Each slot individually tries to pull the exact quantity configured. */
    public static final int MODE_FULL_STACK = 1;
    /** All request slots try to pull the desired quantities simultaneously. */
    public static final int MODE_FULL_REQUEST = 2;

    public static final int SLOT_COUNT = 18;
    private static final int[] SLOT_ACCESS = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    public boolean lastRedstone = false;

    public TileEntityPneumoStorageExporter(BlockPos pos, BlockState state) {
        super(ModPneumaticBlockEntities.PNEUMATIC_STORAGE_EXPORTER.get(), pos, state, SLOT_COUNT);
        Arrays.fill(rorFilters, ItemStack.EMPTY);
    }

    private static ItemStack itemFromId(String id) {
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(id));
            return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public String getDefaultName() {
        return "container.pneumoStorageExporter";
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOT_ACCESS;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return SLOT_ACCESS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 9;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (!(level instanceof ServerLevel sl)) return;

        for (int i = 0; i < 9; i++) {
            if (slotDelay[i] > 0) slotDelay[i]--;
        }

        boolean redstone = sl.hasNeighborSignal(worldPosition);

        if (continuousRequest) {
            this.doRequest(false);
        } else {
            if (redstone && !lastRedstone) this.doRequest(true);
        }

        this.lastRedstone = redstone;

        this.syncState();
    }

    public void doRequest(boolean force) {

        if (this.requestMode != MODE_FULL_REQUEST) {
            // handle all filters individually
            for (int i = 0; i < 9; i++) if (!requestSlot(i, force)) this.slotDelay[i] = SLOT_DELAY;
        } else {

            if (this.cache == null || this.cache.hasExpired) return;

            // check filter delay if forced pulls aren't active
            if (!force) for (int i = 0; i < 9; i++) {
                if (!this.getFilter(i).isEmpty() && slotDelay[i] > 0) return;
            }

            // check if filter demands are met and space is free
            for (int i = 0; i < 9; i++) {
                ItemStack filter = this.getFilter(i);
                if (filter.isEmpty()) continue;

                int requestSize = filter.getCount();
                int existingSize = 0;
                ItemStack existingStack = inventory.getStackInSlot(i + 9);

                if (!existingStack.isEmpty()) {
                    if (ItemStacks.sameItemAndData(existingStack, filter)) {
                        existingSize = existingStack.getCount();
                    } else {
                        this.slotDelay[i] = SLOT_DELAY;
                        return;
                    }
                }

                int capacityLeft = filter.getMaxStackSize() - existingSize;

                if (capacityLeft < requestSize || getAvailability(filter) < requestSize) {
                    this.slotDelay[i] = SLOT_DELAY;
                    return;
                }
            }

            // everything is good, pull items. continues should not happen, but are in place nonetheless
            for (int i = 0; i < 9; i++) {
                ItemStack filter = this.getFilter(i);
                if (filter.isEmpty()) continue;

                int requestSize = filter.getCount();
                ItemStack existingStack = inventory.getStackInSlot(i + 9);
                int existingSize = existingStack.isEmpty() ? 0 : existingStack.getCount();

                ItemStack newStack = filter.copyWithCount(1);

                CacheSlot cacheSlot = this.cache.getSlotFromStack(newStack);
                if (cacheSlot == null) continue; // safeguard

                int pulled = (int) this.cache.consumeItemsAndReturnQuantity(newStack, requestSize);
                inventory.setStackInSlot(i + 9, newStack.copyWithCount(existingSize + pulled));
            }

            this.setChanged();
        }
    }

    /** Returns false if the slot delay should be reset (unsuccessful) or true if not (successful or delay still active). */
    public boolean requestSlot(int slot, boolean force) {

        if (!force && slotDelay[slot] > 0) return true;
        if (this.cache == null || this.cache.hasExpired) return false;

        ItemStack filter = this.getFilter(slot);
        if (filter.isEmpty()) return false;

        int requestSize = filter.getCount();
        int existingSize = 0;
        ItemStack existingStack = inventory.getStackInSlot(slot + 9);

        if (!existingStack.isEmpty()) {
            if (ItemStacks.sameItemAndData(existingStack, filter)) {
                existingSize = existingStack.getCount();
            } else {
                return false;
            }
        }

        ItemStack newStack = filter.copyWithCount(1);
        int capacityLeft = newStack.getMaxStackSize() - existingSize;

        // any non-AMAP mode will fail if it can't insert that much before we even check availability from the pneumo sys
        if (capacityLeft < requestSize && this.requestMode != MODE_AS_MUCH_AS_POSSIBLE) return false;

        CacheSlot cacheSlot = this.cache.getSlotFromStack(newStack);
        if (cacheSlot == null) return false;

        // any non-AMAP mode will fail if the system doesn't have the requested amount
        if (cacheSlot.stacksize < requestSize && this.requestMode != MODE_AS_MUCH_AS_POSSIBLE) return false;
        if (cacheSlot.stacksize <= 0) return false;

        int toPull = BobMathUtil.min(requestSize, (int) Math.min(cacheSlot.stacksize, Integer.MAX_VALUE), capacityLeft);

        int pulled = (int) this.cache.consumeItemsAndReturnQuantity(newStack, toPull);
        inventory.setStackInSlot(slot + 9, newStack.copyWithCount(existingSize + pulled));
        this.setChanged();

        return true;
    }

    /** The requested item and amount (as its count) for the given filter, or EMPTY if none. */
    public ItemStack getFilter(int slot) {
        if (rorConfiguredMode) return rorFilters[slot];
        return inventory.getStackInSlot(slot);
    }

    public long getAvailability(ItemStack stack) {
        if (this.cache == null || this.cache.hasExpired) return 0;
        if (stack.isEmpty()) return 0;
        CacheSlot slot = this.cache.getSlotFromStack(stack.copyWithCount(1));
        if (slot == null) return 0;
        return slot.stacksize;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(continuousRequest);
        buf.writeBoolean(rorConfiguredMode);
        buf.writeByte(requestMode);
        for (int i = 0; i < 9; i++) ItemStacks.writeOptional(buf, rorFilters[i]);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.continuousRequest = buf.readBoolean();
        this.rorConfiguredMode = buf.readBoolean();
        this.requestMode = buf.readByte();
        for (int i = 0; i < 9; i++) rorFilters[i] = ItemStacks.readOptional(buf);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);

        this.continuousRequest = input.getBooleanOr("continuousRequest", false);
        this.rorConfiguredMode = input.getBooleanOr("rorConfiguredMode", false);
        this.requestMode = input.getByteOr("requestMode", (byte) 0);
        for (int i = 0; i < 9; i++) {
            rorFilters[i] = input.read("rorFilter_" + i, ItemStacks.optionalCodec()).orElse(ItemStack.EMPTY);
        }

        this.lastRedstone = input.getBooleanOr("lastRedstone", false);
        int[] delays = input.getIntArrayOr("slotDelay", null);
        if (delays != null && delays.length == 9) this.slotDelay = delays;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);

        output.putBoolean("continuousRequest", continuousRequest);
        output.putBoolean("rorConfiguredMode", rorConfiguredMode);
        output.putByte("requestMode", (byte) requestMode);

        for (int i = 0; i < 9; i++) {
            if (!rorFilters[i].isEmpty()) output.store("rorFilter_" + i, ItemStacks.optionalCodec(), rorFilters[i]);
        }

        output.putBoolean("lastRedstone", lastRedstone);
        output.putIntArray("slotDelay", slotDelay);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerPneumoStorageExporter(containerId, playerInventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "continuous")) {
            this.continuousRequest = !this.continuousRequest;
        }
        if (Tags.contains(data, "request")) {
            this.requestMode++;
            if (this.requestMode >= 3) this.requestMode = 0;
        }
        if (Tags.contains(data, "ror")) {
            this.rorConfiguredMode = !this.rorConfiguredMode;
        }
        if (Tags.contains(data, "slot")) {
            setFilterContents(data);
        }
        this.setChanged();
    }

    @Override
    public int[] getFilterSlots() {
        return new int[]{0, 9};
    }

    @Override
    public void nextMode(int i) {
    }

    /** 1.7 overrides this to keep the requested COUNT, which the shared implementation trims to one. */
    @Override
    public void setFilterContents(CompoundTag nbt) {
        int slot = Tags.getIntOr(nbt, "slot", 0);
        if (slot < getFilterSlots()[0] || slot >= getFilterSlots()[1]) return;
        ItemStack item = IControlReceiverFilter.readFilterStack(nbt, level);
        inventory.setStackInSlot(slot, item);
        this.setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_FUNCTION + "setfilter" + NAME_SEPARATOR + "slot" + PARAM_SEPARATOR + "itemid" + PARAM_SEPARATOR + "amount",
                PREFIX_FUNCTION + "setcontinuous" + NAME_SEPARATOR + "on/off",
                PREFIX_FUNCTION + "request",
                PREFIX_FUNCTION + "requestslot" + NAME_SEPARATOR + "slot",
                PREFIX_FUNCTION + "checkavailability" + NAME_SEPARATOR + "itemid" + PARAM_SEPARATOR + "returnchannel",
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {

        if ((PREFIX_FUNCTION + "setfilter").equals(name) && params.length == 3) {
            int slot = IRORInteractive.parseInt(params[0], 1, 9) - 1;
            ItemStack item = itemFromId(params[1]);
            int amount = IRORInteractive.parseInt(params[2], 1, 64);

            this.rorFilters[slot] = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(amount);
            this.setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "setcontinuous").equals(name) && params.length == 1) {
            if ("on".equals(params[0])) this.continuousRequest = true;
            if ("off".equals(params[0])) this.continuousRequest = false;
            this.setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "request").equals(name)) {
            this.doRequest(true);
            return null;
        }

        if ((PREFIX_FUNCTION + "requestslot").equals(name) && params.length == 1) {
            int slot = IRORInteractive.parseInt(params[0], 1, 9) - 1;
            if (!this.requestSlot(slot, true)) this.slotDelay[slot] = SLOT_DELAY;
            return null;
        }

        if ((PREFIX_FUNCTION + "checkavailability").equals(name) && params.length == 2) {
            ItemStack item = itemFromId(params[0]);
            String ret = params[1];
            long availability = this.getAvailability(item);
            RTTYSystem.broadcast(level, ret, availability + "");
            return null;
        }

        return null;
    }

    /** 1.7 {@code PneumoStorageExporter.breakBlock}: only slots 9..17 (the buffer) drop, the nine filters do not. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        for (int i = 9; i < SLOT_COUNT; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }
}
