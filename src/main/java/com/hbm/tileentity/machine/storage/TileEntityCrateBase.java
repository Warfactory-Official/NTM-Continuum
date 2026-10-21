package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.NTMItemHandler;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.Entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Random;

/**
 * 1.7 {@code TileEntityCrateBase}: a slot inventory on a pin lock, with a custom name and the spider easter
 * egg. Base of the tungsten crate, the safe and the filing cabinet.
 *
 * <p>The contents, the anvil name and - while locked - the pins travel with the dropped item, as 1.7's
 * {@code BlockStorageCrate.removedByPlayer} wrote them into the stack NBT under the default
 * {@code CRATE_KEEP_CONTENTS}: the inventory as the {@code minecraft:container} component, the lock and the
 * spider flag as custom data. Automation reaches every slot unless the crate is locked.
 */
public abstract class TileEntityCrateBase extends TileEntityLockableBase implements WorldlyContainer, MenuProvider {

    public final NTMItemHandler inventory;
    public @Nullable String customName;
    public boolean hasSpiders = false;

    protected TileEntityCrateBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int count) {
        super(type, pos, state);
        this.inventory = new NTMItemHandler(count) {
            @Override
            protected void onContentsChanged(int slot) {
                TileEntityCrateBase.this.setChanged();
            }
        };
    }

    /** The untranslated default name, 1.7's {@code getInventoryName} without a custom name. */
    public abstract String getDefaultName();

    public boolean hasCustomName() {
        return customName != null && !customName.isEmpty();
    }

    public void setCustomName(String name) {
        this.customName = name;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(customName) : Component.translatable(getDefaultName());
    }

    // --- 1.7 openInventory / closeInventory: the crate lid sounds ---

    public void openInventory() {
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    ModSounds.CRATE_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public void closeInventory() {
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    ModSounds.CRATE_CLOSE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    // --- persistence ---

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        inventory.save(output.child("inventory"));
        output.putBoolean("spiders", hasSpiders);
        if (customName != null) output.putString("name", customName);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        input.child("inventory").ifPresent(inventory::load);
        hasSpiders = input.getBooleanOr("spiders", false);
        String name = input.getStringOr("name", input.getStringOr("customName", ""));
        customName = name.isEmpty() ? null : name;
    }

    // --- the item form (1.7 removedByPlayer / onBlockPlacedBy) ---

    @Override
    protected NonNullList<ItemStack> carriedItems() {
        return inventory.getStacks();
    }

    @Override
    protected @Nullable CompoundTag collectCarriedData() {
        CompoundTag tag = new CompoundTag();
        if (isLocked()) {
            tag.putInt("lock", getPins());
            tag.putDouble("lockMod", getMod());
        }
        // Saves memory and ensures consistency between crafted crates and mined ones
        if (hasSpiders) tag.putBoolean("spiders", true);
        return tag.isEmpty() ? null : tag;
    }

    @Override
    protected void applyCarriedData(CompoundTag data) {
        if (Tags.contains(data, "lock")) {
            setPins(Tags.getIntOr(data, "lock", 0));
            setMod(Tags.getDoubleOr(data, "lockMod", 0.1D));
            lock();
        }
        hasSpiders = Tags.getBooleanOr(data, "spiders", false);
    }

    @Override
    protected @Nullable Component collectCarriedName() {
        return hasCustomName() ? Component.literal(customName) : null;
    }

    @Override
    protected void applyCarriedName(Component name) {
        customName = name.getString();
    }

    @Override
    protected void removeSavedComponents(DataOut output) {
        super.removeSavedComponents(output);
        output.discard("inventory");
        output.discard("name");
    }

    /** The contents ride on the dropped item; breaking the block must not spill them as well. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    // --- 1.7 spawnSpiders: three cave spiders around the opener, once ---

    public static void spawnSpiders(Player player, Level level, TileEntityCrateBase crate) {
        if (!crate.hasSpiders || !(level instanceof ServerLevel server)) return;
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            var spider = Entities.createTriggered(EntityType.CAVE_SPIDER, server);
            if (spider == null) continue;
            BlockPos p = crate.getBlockPos();
            spider.snapTo(p.getX() + random.nextGaussian() * 2, p.getY() + 1, p.getZ() + random.nextGaussian() * 2,
                    random.nextFloat(), 0);
            spider.setTarget(player);
            level.addFreshEntity(spider);
        }
        crate.hasSpiders = false;
        crate.setChanged();
    }

    // --- Container ---

    @Override
    public int getContainerSize() {
        return inventory.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < inventory.getSlots(); i++) if (!inventory.getStackInSlot(i).isEmpty()) return false;
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

    /** 1.7 {@code isUseableByPlayer}: the same tile, within eight blocks. */
    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < inventory.getSlots(); i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
    }

    // --- sided access ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack) && !isLocked();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isLocked();
    }
}
