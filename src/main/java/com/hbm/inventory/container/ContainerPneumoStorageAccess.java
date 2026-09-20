package com.hbm.inventory.container;

import com.hbm.packet.PneumoAccessSyncPayload.Delta;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import com.hbm.inventory.ModPneumaticMenus;
import com.hbm.inventory.StackCache;
import com.hbm.packet.PneumoAccessInputPayload;
import com.hbm.packet.PneumoAccessSyncPayload;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageAccess;

/**
 * 1.7 {@code ContainerPneumoStorageAccess}: a grid of virtual slots over the terminal's {@link StackCache}, listed,
 * searched and sorted on the client and clicked through {@link PneumoAccessInputPayload}.
 *
 * <p>The grid slots exist only to be drawn: the server leaves them empty and the client fills them from the index it
 * is sent, so no vanilla slot traffic ever describes them.
 */
public class ContainerPneumoStorageAccess extends PneumaticMenuBase<TileEntityPneumoStorageAccess> {

    public static final int GRID_COLUMNS = 8;
    public static final int GRID_ROWS = 6;
    public static final int GRID_SIZE = GRID_ROWS * GRID_COLUMNS;
    /** 1.7 DELTAS_PER_MSG; a clientbound payload holds at most this many. */
    public static final int DELTAS_PER_MESSAGE = 6 * 8;
    public static final int NO_ENTRY = -1;

    public static final Comparator<Entry> SORT_BY_STACK_SIZE = (a, b) -> {
        int amount = Long.compare(b.amount, a.amount);
        return amount != 0 ? amount : compareType(a, b);
    };
    public static final Comparator<Entry> SORT_BY_ID = (a, b) -> {
        int type = compareType(a, b);
        return type != 0 ? type : Long.compare(b.amount, a.amount);
    };
    public static final Comparator<Entry> SORT_BY_INTERNAL = (a, b) -> {
        int name = a.display.getItem().getDescriptionId().compareToIgnoreCase(b.display.getItem().getDescriptionId());
        return name != 0 ? name : SORT_BY_ID.compare(a, b);
    };
    public static final Comparator<Entry> SORT_BY_LOCALIZED = (a, b) -> {
        int name = a.display.getHoverName().getString().compareToIgnoreCase(b.display.getHoverName().getString());
        return name != 0 ? name : SORT_BY_ID.compare(a, b);
    };

    /** 1.7 canInteractWith: 15 blocks, against vanilla's 8. */
    private static final double REACH_SQ = 15.0D * 15.0D;

    private final Player player;
    /** Both sides: every type the client has been told about, in the order it was told. */
    private final Int2ObjectLinkedOpenHashMap<Entry> entries = new Int2ObjectLinkedOpenHashMap<>();
    /** Server: the same entries, to find the id of a cache slot again. */
    private final Object2ObjectOpenCustomHashMap<ItemStack, Entry> byStack =
            new Object2ObjectOpenCustomHashMap<>(StackCache.TYPE_AND_COMPONENTS);
    private final List<Delta> outbox = new ArrayList<>();

    private int nextEntryId;
    private int listingStart;
    private int listingSize;
    private Comparator<Entry> sorter = SORT_BY_STACK_SIZE;
    private Predicate<ItemStack> filter = stack -> true;

    public ContainerPneumoStorageAccess(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityPneumoStorageAccess.class));
    }

    public ContainerPneumoStorageAccess(int containerId, Inventory playerInventory,
                                        @Nullable TileEntityPneumoStorageAccess access) {
        super(ModPneumaticMenus.PNEUMATIC_STORAGE_ACCESS.get(), containerId, access, 0);
        this.player = playerInventory.player;

        SimpleContainer grid = new SimpleContainer(GRID_SIZE);

        int hOffset = 34;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                addSlot(new SlotPneumo(grid, col + row * GRID_COLUMNS, 8 + col * 18 + hOffset, 17 + row * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8 + hOffset, 169);
    }

    /**
     * 1.7 sorts on the item id, then the metadata, then whether there is NBT; the metadata and NBT are both item
     * data here, which the data hash separates.
     */
    private static int compareType(Entry a, Entry b) {
        int id = Integer.compare(BuiltInRegistries.ITEM.getId(a.display.getItem()),
                BuiltInRegistries.ITEM.getId(b.display.getItem()));
        if (id != 0) return id;
        return Integer.compare(ItemStacks.hashItemAndData(a.display), ItemStacks.hashItemAndData(b.display));
    }

    @Override
    public boolean stillValid(Player player) {
        return tile != null && !tile.isRemoved()
                && tile.getBlockPos().distToCenterSqr(player.position()) <= REACH_SQ;
    }

    /**
     * 1.7 runs this out of {@code slotClick}: an inventory slot deposits into the network, and a grid slot, which the
     * server never fills, moves nothing.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < GRID_SIZE || tile == null) return ItemStack.EMPTY;

        StackCache cache = tile.cache;
        if (cache == null || cache.hasExpired) return ItemStack.EMPTY;

        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;

        int leftover = (int) cache.addItemsAndReturnQuantity(stack, stack.getCount());
        int deposited = stack.getCount() - leftover;
        if (deposited <= 0) return ItemStack.EMPTY;

        slot.remove(deposited);
        slot.setChanged();
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        syncCache();
    }

    /** A full resync blanks the grid, which no server state can restore; the client's own index can. */
    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        super.initializeContents(stateId, items, carried);
        rebuildIndex();
    }

    /** Server: what the cache holds against what this client has been told. 1.7 {@code checkAndSyncCache}. */
    private void syncCache() {
        if (tile == null || !(player instanceof ServerPlayer serverPlayer)) return;
        StackCache cache = tile.cache;
        if (cache == null || cache.hasExpired) return;

        for (StackCache.CacheSlot slot : cache.cacheSlots.values()) {
            // 1.7 skips the null identity: the cache of slots holding nothing
            if (slot.displayStack.isEmpty()) continue;

            Entry entry = byStack.get(slot.displayStack);

            if (entry == null) {
                entry = new Entry(nextEntryId++, slot.displayStack.copy(), slot.stacksize);
                byStack.put(entry.display, entry);
                entries.put(entry.id, entry);
                outbox.add(new Delta(entry.id, entry.display, entry.amount));
            } else if (entry.amount != slot.stacksize) {
                entry.amount = slot.stacksize;
                outbox.add(new Delta(entry.id, null, entry.amount));
            }
        }

        for (int from = 0; from < outbox.size(); from += DELTAS_PER_MESSAGE) {
            int to = Math.min(from + DELTAS_PER_MESSAGE, outbox.size());
            GolemNetwork.sendToPlayer(serverPlayer, PneumoAccessSyncPayload.CODEC,
                    PneumoAccessSyncPayload.of(serverPlayer.level(), containerId, List.copyOf(outbox.subList(from, to))));
        }

        outbox.clear();
    }

    /** Client: 1.7 {@code acceptData}'s client arm. */
    public void applyDeltas(List<Delta> deltas) {
        for (Delta delta : deltas) {
            if (delta.display() != null) {
                entries.put(delta.id(), new Entry(delta.id(), delta.display(), delta.amount()));
            } else {
                Entry entry = entries.get(delta.id());
                if (entry != null) entry.amount = delta.amount();
            }
        }

        rebuildIndex();
    }

    /** Server: 1.7 {@code acceptData}'s server arm, reached from the terminal's own click packet. */
    public void handleClick(ClickOp op, int entryId) {
        if (tile == null) return;
        StackCache cache = tile.cache;
        if (cache == null || cache.hasExpired) return;

        Entry entry = entries.get(entryId);
        ItemStack carried = getCarried();
        boolean sameType = entry != null && !carried.isEmpty()
                && StackCache.TYPE_AND_COMPONENTS.equals(entry.display, carried);

        if (op == ClickOp.SHIFT_CLICK) {
            if (entry == null) return;
            ItemStack stack = entry.display.copy();
            int originalStacksize = (int) Math.min(stack.getMaxStackSize(), entry.amount);
            ItemStack toGive = stack.copyWithCount(originalStacksize);
            player.getInventory().add(toGive);
            int remainder = toGive.isEmpty() ? 0 : toGive.getCount();
            int itemsUsed = originalStacksize - remainder;
            cache.consumeItemsAndReturnQuantity(stack, itemsUsed);
            broadcastChanges();
            return;
        }

        // left click / right click
        // if we drop an item onto a different stack, or no stack at all, deposit
        if (!carried.isEmpty() && !sameType) {
            int toDeposit = op == ClickOp.LEFT_CLICK ? carried.getCount() : 1;
            carried.shrink(toDeposit);
            int leftover = (int) cache.addItemsAndReturnQuantity(carried, toDeposit);
            carried.grow(leftover);
            setCarried(ItemStack.EMPTY);
            if (!carried.isEmpty()) player.getInventory().placeItemBackInInventory(carried);
            broadcastChanges();
            return;
        }

        // if our hand is empty or we have the same type, withdraw
        if (entry != null) {
            ItemStack stack = entry.display.copy();
            int alreadyHeld = carried.isEmpty() ? 0 : carried.getCount();
            int capacity = stack.getMaxStackSize() - alreadyHeld;
            if (op == ClickOp.RIGHT_CLICK && capacity > 1) capacity = 1;
            int toGrab = (int) Math.min(capacity, entry.amount);
            int grabbed = (int) cache.consumeItemsAndReturnQuantity(stack, toGrab);
            setCarried(stack.copyWithCount(alreadyHeld + grabbed));
            broadcastChanges();
        }
    }

    public int entryAt(int slotIndex) {
        return slotIndex >= 0 && slotIndex < GRID_SIZE ? ((SlotPneumo) slots.get(slotIndex)).entryId : NO_ENTRY;
    }

    public int listingStart() {
        return listingStart;
    }

    /** 1.7 getStackCount. */
    public int listingSize() {
        return listingSize;
    }

    public void setListingStart(int listingStart) {
        this.listingStart = listingStart;
        rebuildIndex();
    }

    public void setSorter(Comparator<Entry> sorter) {
        this.listingStart = 0;
        this.sorter = sorter;
        rebuildIndex();
    }

    /**
     * @param filter what a listed type must satisfy to be drawn; the tooltip-aware form is the screen's, since only
     *               it can resolve a tooltip
     */
    public void setFilter(Predicate<ItemStack> filter) {
        this.listingStart = 0;
        this.filter = filter;
        rebuildIndex();
    }

    /** Client: 1.7 {@code rebuildClientIndex}, filling the grid from the page the listing is scrolled to. */
    public void rebuildIndex() {
        List<Entry> listed = new ArrayList<>(entries.size());
        for (Entry entry : entries.values()) {
            if (entry.amount > 0 && filter.test(entry.display)) listed.add(entry);
        }

        listingSize = listed.size();
        listed.sort(sorter);

        int offset = listingStart * GRID_COLUMNS;

        for (int i = 0; i < GRID_SIZE; i++) {
            SlotPneumo slot = (SlotPneumo) slots.get(i);
            int index = offset + i;

            if (index < listed.size()) {
                Entry entry = listed.get(index);
                slot.set(entry.display.copy());
                slot.amount = entry.amount;
                slot.entryId = entry.id;
            } else {
                slot.set(ItemStack.EMPTY);
                slot.amount = 0;
                slot.entryId = NO_ENTRY;
            }
        }
    }

    public enum ClickOp {
        LEFT_CLICK,
        RIGHT_CLICK,
        SHIFT_CLICK
    }

    /** One type the terminal can reach, and how much of it. 1.7 {@code CacheSlotDummy}. */
    public static final class Entry {

        public final int id;
        /** Count 1; the amount is a long the stack cannot hold. */
        public final ItemStack display;
        public long amount;

        Entry(int id, ItemStack display, long amount) {
            this.id = id;
            this.display = display;
            this.amount = amount;
        }
    }

    /**
     * 1.7 {@code SlotPneumo}: shows a type and its total, and refuses everything a player could put in it - the
     * terminal's clicks reach the network, not the slot.
     */
    public static class SlotPneumo extends Slot {

        public long amount;
        public int entryId = NO_ENTRY;

        public SlotPneumo(Container grid, int index, int x, int y) {
            super(grid, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
