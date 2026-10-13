package com.hbm.tileentity.bomb;

import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Shared inventory, persistence, and tracking sync for the old component-built bomb blocks. */
public abstract class TileEntityNukeBomb extends TileEntityMachineBase implements MenuProvider {

    /** The player who placed the bomb, used when redstone supplies no detonator entity. */
    public @Nullable UUID placerID;

    protected TileEntityNukeBomb(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public abstract boolean isReady();

    public void clearSlots() {
        clearContent();
    }

    @Override
    protected void onSlotContentsChanged(int slot, ItemStack stack) {
        super.onSlotContentsChanged(slot, stack);
        // Menus receive ordinary slot changes, while this also keeps observers and already-open clients in sync.
        networkPackNT();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        if (placerID != null) output.store("placer", UUIDUtil.CODEC, placerID);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        placerID = input.read("placer", UUIDUtil.CODEC).orElse(null);
    }

    /** The BufPacket replacement carries component identities/counts for live GUI updates. */
    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeVarInt(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            buffer.writeBoolean(!stack.isEmpty());
            if (!stack.isEmpty()) {
                buffer.writeVarInt(BuiltInRegistries.ITEM.getId(stack.getItem()));
                buffer.writeVarInt(stack.getCount());
            }
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        int slots = buffer.readVarInt();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = ItemStack.EMPTY;
            if (buffer.readBoolean()) {
                int id = buffer.readVarInt();
                int count = buffer.readVarInt();
                if (i < inventory.getSlots() && id >= 0) {
                    stack = new ItemStack(BuiltInRegistries.ITEM.byId(id), count);
                }
            }
            if (i < inventory.getSlots()) inventory.setStackInSlot(i, stack);
        }
    }

    public abstract String getDefaultName();

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getDefaultName());
    }

    @Override
    public abstract AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player);
}
