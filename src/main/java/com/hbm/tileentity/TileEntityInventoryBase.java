package com.hbm.tileentity;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.container.Containers;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Adds a slot inventory to {@link TileEntityLoadedBase}. Backed by an {@link NTMItemHandler} (HBM's
 * vanilla-only replacement for Forge's {@code ItemStackHandler}) and exposes it as a vanilla
 * {@link Container} so menus/slots can bind to it directly.
 *
 * <p>The handler's {@link NTMItemHandler#onContentsChanged(int)} is wired to {@link #setChanged()},
 * matching the old base which called {@code markDirty} on content change.
 */
public abstract class TileEntityInventoryBase extends TileEntityLoadedBase implements Container {

    public final NTMItemHandler inventory;

    public TileEntityInventoryBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.inventory = createInventory(slotCount);
    }

    /** Builds the backing handler. Override to customise per-slot limits / validity. */
    protected NTMItemHandler createInventory(int slotCount) {
        return new NTMItemHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                TileEntityInventoryBase.this.setChanged();
                TileEntityInventoryBase.this.onSlotContentsChanged(slot, getStackInSlot(slot));
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return TileEntityInventoryBase.this.isItemValidForSlot(slot, stack);
            }
        };
    }

    /**
     * Inclusive {@code {first, last}} upgrade-slot range, or null when the machine takes no upgrades.
     * Machines override this; inserting an upgrade into the range plays the 1.12.2 "plug" sound
     * (each old TE hooked its inventory's {@code setStackInSlot} for this).
     */
    protected int[] getUpgradeSlotRange() {
        return null;
    }

    /** Post-change hook (after {@link #setChanged()}); default plays the upgrade plug sound. */
    protected void onSlotContentsChanged(int slot, ItemStack stack) {
        int[] range = getUpgradeSlotRange();
        if (range != null && slot >= range[0] && slot <= range[1]
                && Library.isMachineUpgrade(stack)
                && this.level != null && !this.level.isClientSide()) {
            this.level.playSound(null, this.worldPosition, ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** Override to gate which items may live in a slot (mirrors the old {@code isItemValidForSlot}). */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    // Persistence

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

    // Container (delegates to the handler)

    @Override
    public int getContainerSize() {
        return inventory.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(inventory.getStacks(), slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventory.getStacks(), slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
    }

    /**
     * Upstream's {@code isUseableByPlayer}. The radius comes from the block rather than being a flat
     * constant, so a multiblock's is measured against its footprint and not just its core cell, see
     * {@link com.hbm.inventory.container.Containers#inRangeOfMultiblock}.
     */
    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) return false;
        if (this.level.getBlockState(this.worldPosition).getBlock()
                instanceof BlockMultiblockCore) {
            return Containers.inRangeOfMultiblock(this.level, this.worldPosition, player);
        }
        // A single-cell machine uses 1.7's isUseableByPlayer: the plain 64-block-squared radius. The multiblock
        // check answers false for a plain block, which closes the menu on the tick after it opens.
        return player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
