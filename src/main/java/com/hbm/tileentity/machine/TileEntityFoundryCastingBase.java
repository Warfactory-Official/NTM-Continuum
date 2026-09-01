package com.hbm.tileentity.machine;

import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemMold.Mold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Base for the casting blocks (1.12.2 {@code TileEntityFoundryCastingBase}): 2 slots (0 = installed
 * mold, 1 = finished item). Capacity = the installed mold's cost; once filled exactly to capacity with
 * an empty output slot, a 200-tick cooloff runs and the melt solidifies into
 * {@code mold.getOutput(type)}. The inventory is NOT client-replicated per-tick, so the two slots are
 * synced explicitly on the BufPacket (the renderer draws them).
 */
public abstract class TileEntityFoundryCastingBase extends TileEntityFoundryBase implements net.minecraft.world.WorldlyContainer {

    /** 0 = mold input, 1 = item output. */
    public final NTMItemHandler inventory;
    public int cooloff = 100;

    // Client copies of the two slots, synced via the BufPacket for the renderer.
    public ItemStack syncMold = ItemStack.EMPTY;
    public ItemStack syncOutput = ItemStack.EMPTY;

    public TileEntityFoundryCastingBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 2);
    }

    protected TileEntityFoundryCastingBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.inventory = new NTMItemHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                TileEntityFoundryCastingBase.this.setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && stack.getItem() instanceof ItemMold;
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFoundryCastingBase te) {
        if (te.amount > te.getCapacity()) {
            te.amount = te.getCapacity();
        }

        if (te.amount == 0) {
            te.type = null;
        }

        Mold mold = te.getInstalledMold();

        if (mold != null && te.amount == te.getCapacity() && te.getCapacity() > 0 && te.inventory.getStackInSlot(1).isEmpty()) {
            te.cooloff--;

            if (te.cooloff <= 0) {
                te.amount = 0;

                ItemStack out = te.type == null ? ItemStack.EMPTY : mold.getOutput(te.type);
                if (!out.isEmpty()) {
                    te.inventory.setStackInSlot(1, out.copy());
                }

                te.cooloff = 200;
                te.setChanged();
            }
        } else {
            te.cooloff = 200;
        }

        te.syncMold = te.inventory.getStackInSlot(0);
        te.syncOutput = te.inventory.getStackInSlot(1);
        te.networkPackNT();
    }

    /**
     * The mold in slot 0, or null if absent / wrong size for this caster. The BE inventory is NOT
     * replicated to clients, so the client falls back to the BufPacket-synced {@link #syncMold} —
     * without this the client-side capacity is 0 and the melt level computes hundreds of blocks up.
     */
    public @Nullable Mold getInstalledMold() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) stack = syncMold;
        Mold mold = ItemMold.getMold(stack);
        return mold != null && mold.size == this.getMoldSize() ? mold : null;
    }

    /** Capacity = the installed mold's cost (0 with no mold). */
    @Override
    public int getCapacity() {
        Mold mold = this.getInstalledMold();
        return mold == null ? 0 : mold.getCost();
    }

    /**
     * Standard check plus: output slot empty, a mold installed, and the mold has an output for this
     * material (no match → no pouring).
     */
    @Override
    public boolean standardCheck(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) {
        if (!super.standardCheck(world, p, side, stack)) return false;
        if (!inventory.getStackInSlot(1).isEmpty()) return false;
        Mold mold = this.getInstalledMold();
        if (mold == null) return false;
        return !mold.getOutput(stack.material).isEmpty();
    }

    /** 0 = small mold caster, 1 = basin. */
    public abstract int getMoldSize();

    // Destruction drops: the vanilla preRemoveSideEffects default spills a Container's slots (the BE
    // now implements WorldlyContainer), and the FoundryBase override adds the molten content as scraps.

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(cooloff);
        dev.golem.core.world.ItemStacks.writeOptional(buf, inventory.getStackInSlot(0));
        dev.golem.core.world.ItemStacks.writeOptional(buf, inventory.getStackInSlot(1));
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        cooloff = buf.readInt();
        syncMold = dev.golem.core.world.ItemStacks.readOptional(buf);
        syncOutput = dev.golem.core.world.ItemStacks.readOptional(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        inventory.save(output.child("inventory"));
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        input.child("inventory").ifPresent(inventory::load);
    }

    /** Convenience for the block's interactions. */
    public static void giveOrDrop(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        }
    }

    // --- WorldlyContainer: hoppers insert molds into slot 0, pull the finished item from slot 1
    //     (the old ITEM_HANDLER capability wrapper) ---

    @Override public int[] getSlotsForFace(net.minecraft.core.Direction side) { return new int[]{0, 1}; }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot == 0 && stack.getItem() instanceof ItemMold;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot == 1;
    }

    // --- Container (delegates to the handler, like TileEntityInventoryBase) ---

    @Override public int getContainerSize() { return inventory.getSlots(); }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override public ItemStack getItem(int slot) { return inventory.getStackInSlot(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = net.minecraft.world.ContainerHelper.removeItem(inventory.getStacks(), slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return net.minecraft.world.ContainerHelper.takeItem(inventory.getStacks(), slot);
    }

    @Override public void setItem(int slot, ItemStack stack) { inventory.setStackInSlot(slot, stack); }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof ItemMold;
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
}
