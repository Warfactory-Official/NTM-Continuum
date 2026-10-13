package com.hbm.tileentity.network;

import com.hbm.util.SavedIds;
import dev.golem.core.data.Tags;

import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.interfaces.ICopiable;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.network.BlockCraneBase;
import com.hbm.util.ConveyorInventories;

/**
 * 1.7 {@code tileentity/network/TileEntityCraneBase}. Both sides live in the block state here, so the tile keeps
 * neither the output override nor the client-side resync 1.7 needed to hold it steady.
 */
public abstract class TileEntityCraneBase extends TileEntityMachineBase implements ICopiable, WorldlyContainer,
        MenuProvider {

    /** 1.7 {@code infoForDisplay} order: the settings tool's index 0 is the filter, 1 the orientation. */
    private static final int PASTE_ORIENTATION = 1;

    protected TileEntityCraneBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state, slotCount);
    }

    public Direction getInputSide() {
        return BlockCraneBase.inputSide(getBlockState());
    }

    public Direction getOutputSide() {
        return BlockCraneBase.outputSide(getBlockState());
    }

    /**
     * 1.7 {@code BlockCraneBase.dropContents}: the slot band {@code {start, end}} (end exclusive) this crane spills.
     * Everything outside it is a ghost filter, which is not an item and must not fall out.
     */
    protected int[] droppedBand() {
        return new int[]{0, getContainerSize()};
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;

        int[] band = droppedBand();
        for (int i = band[0]; i < band[1]; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        clearContent();
    }

    /** 1.7 {@code hasPermission}: within 20 blocks. */
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 400D;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    // --- WorldlyContainer: the 1.7 ISidedInventory triple, defaulting to the base's access array ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] access = getAccessibleSlotsFromSide(side.ordinal());
        return access == null ? ConveyorInventories.range(getContainerSize()) : access;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canExtractItem(slot, stack, side.ordinal());
    }

    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return true;
    }

    // --- ICopiable ---

    @Override
    public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        SavedIds.putEnum(nbt, "inputSide", getInputSide()); // copied settings: by name
        SavedIds.putEnum(nbt, "outputSide", getOutputSide());

        if (this instanceof IControlReceiverFilter filter) {
            ListTag tags = new ListTag();
            int count = 0;
            for (int i = filter.getFilterSlots()[0]; i < filter.getFilterSlots()[1]; i++) {
                if (!getItem(i).isEmpty()) {
                    CompoundTag slotNbt = new CompoundTag();
                    slotNbt.putByte("slot", (byte) count);
                    IControlReceiverFilter.writeStack(slotNbt, getItem(i), level);
                    tags.add(slotNbt);
                }
                count++;
            }
            nbt.put("items", tags);
        }

        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (index == PASTE_ORIENTATION) {
            BlockState state = getBlockState();
            if (nbt.contains("outputSide")) {
                state = state.setValue(BlockCraneBase.OUTPUT, SavedIds.getEnum(nbt, "outputSide", Direction.UP));
            }
            if (nbt.contains("inputSide")) {
                state = state.setValue(BlockCraneBase.INPUT, SavedIds.getEnum(nbt, "inputSide", Direction.DOWN));
            }
            level.setBlockAndUpdate(pos, state);
            return;
        }

        if (this instanceof IControlReceiverFilter filter) {
            IControlReceiverFilter.pasteFilter(filter, this, nbt, index, level);
        }
    }

    @Override
    public String[] infoForDisplay(Level level, BlockPos pos) {
        return new String[]{"copytool.filter", "copytool.orientation"};
    }
}
