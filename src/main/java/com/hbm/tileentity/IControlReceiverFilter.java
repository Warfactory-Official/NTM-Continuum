package com.hbm.tileentity;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.interfaces.ICopiable;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.golem.core.data.Tags;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code tileentity/IControlReceiverFilter}: a tile with ghost filter slots that the GUI writes through a
 * control packet ({@code slot} + {@code stack}) and the settings-copy tool reads back as {@code items}.
 *
 * <p>The one port of the interface, shared by the conveyor cranes, the pneumatic tube/exporter/storage and
 * the drone requester.
 */
public interface IControlReceiverFilter extends IControlReceiver, ICopiable {

    /** Advance the match mode of the filter in slot i. */
    void nextMode(int slot);

    /** The start (inclusive) and end (exclusive) of the filter slots. */
    int[] getFilterSlots();

    @Override
    default void receiveControl(CompoundTag data) {
        if (data.contains("slot")) setFilterContents(data);
    }

    /** Expects the implementor to be a block entity and a container; writes one filter from a {slot, stack} payload. */
    default void setFilterContents(CompoundTag nbt) {
        BlockEntity tile = (BlockEntity) this;
        Container inv = (Container) this;
        int slot = Tags.getIntOr(nbt, "slot", 0);
        // a crafted payload must not write a ghost copy into a real slot
        if (slot < getFilterSlots()[0] || slot >= getFilterSlots()[1]) return;
        ItemStack item = readFilterStack(nbt, tile.getLevel());
        if (!item.isEmpty()) item.setCount(1);
        inv.setItem(slot, item);
        nextMode(slot);
        tile.setChanged();
    }

    @Override
    default @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        Container inv = (Container) this;
        CompoundTag nbt = new CompoundTag();
        ListTag tags = new ListTag();
        int count = 0;
        for (int i = getFilterSlots()[0]; i < getFilterSlots()[1]; i++) {
            if (!inv.getItem(i).isEmpty()) {
                CompoundTag slotNbt = new CompoundTag();
                slotNbt.putByte("slot", (byte) count);
                writeStack(slotNbt, inv.getItem(i), level);
                tags.add(slotNbt);
            }
            count++;
        }
        nbt.put("items", tags);
        return nbt;
    }

    /**
     * 1.7 {@code IControlReceiverFilter.pasteSettings}: the interface default additionally requires
     * {@code index < listSize} on every slot, which the crane base's copy of the loop does not. The term
     * is hoisted out of the loop, so the two share one helper.
     */
    @Override
    default void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (index < Tags.getListOrEmpty(nbt, "items").size()) pasteFilter(this, (Container) this, nbt, index, level);
    }

    /** 1.7 {@code TileEntityCraneBase.pasteSettings}, shared with the crane base that layers orientation on top. */
    static void pasteFilter(IControlReceiverFilter filter, Container inv, CompoundTag nbt, int index, Level level) {
        ListTag items = Tags.getListOrEmpty(nbt, "items");
        int listSize = items.size();
        if (listSize == 0) return;

        int count = 0;
        for (int i = filter.getFilterSlots()[0]; i < filter.getFilterSlots()[1]; i++) {
            if (i < listSize) {
                CompoundTag slotNbt = Tags.getCompoundOrEmpty(items, count);
                byte slot = Tags.getByteOr(slotNbt, "slot", (byte) 0);
                ItemStack loaded = readFilterStack(slotNbt, level);
                // whether the filter info came from a router. 1.7 spells the upper bound `index * + 5`, i.e.
                // `index * 5`, so this can never be true; kept as written because it is what upstream ships.
                boolean router = nbt.contains("modes") && slot > index * 5 && slot < index * 5;
                if (!loaded.isEmpty() && (slot < filter.getFilterSlots()[1] || router)) {
                    inv.setItem(slot + filter.getFilterSlots()[0], loaded);
                    filter.nextMode(slot);
                    inv.setChanged();
                }
            }
            count++;
        }
    }

    @Override
    default String @Nullable [] infoForDisplay(Level level, BlockPos pos) {
        return new String[]{"copytool.filter"};
    }

    /** Stores {@code stack} under {@code "stack"} with the level's registries, the 1.7 {@code writeToNBT} of a filter. */
    static void writeStack(CompoundTag nbt, ItemStack stack, @Nullable Level level) {
        if (stack.isEmpty()) return;
        DataResult<Tag> encoded = ItemStacks.optionalCodec()
                .encodeStart(ops(level), stack);
        encoded.result().ifPresent(tag -> nbt.put("stack", tag));
    }

    static ItemStack readFilterStack(CompoundTag nbt, @Nullable Level level) {
        Tag tag = nbt.get("stack");
        if (tag == null) return ItemStack.EMPTY;
        return ItemStacks.optionalCodec().parse(ops(level), tag).result().orElse(ItemStack.EMPTY);
    }

    private static DynamicOps<Tag> ops(@Nullable Level level) {
        HolderLookup.Provider registries = level == null ? null : level.registryAccess();
        return registries == null ? NbtOps.INSTANCE : RegistryOps.create(NbtOps.INSTANCE, registries);
    }
}
