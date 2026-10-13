package com.hbm.entity.train;

import dev.golem.core.data.Inventories;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

public abstract class EntityRailCarCargo extends EntityRailCarBase implements Container, MenuProvider {

    // 1.7 dataWatcher id 10
    private static final EntityDataAccessor<Integer> OCCUPIED =
            SynchedEntityData.defineId(EntityRailCarCargo.class, EntityDataSerializers.INT);

    private NonNullList<ItemStack> slots;

    public EntityRailCarCargo(EntityType<? extends EntityRailCarCargo> type, Level level) {
        super(type, level);
        this.slots = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(OCCUPIED, 0);
    }

    public int countOccupiedSlots() {
        int occupied = 0;
        for(ItemStack stack : this.slots) if(!stack.isEmpty()) occupied++;
        return occupied;
    }

    /** The count the client draws from; server-authoritative. */
    public int getOccupiedSlots() {
        return this.entityData.get(OCCUPIED);
    }

    @Override
    public void tick() {
        super.tick();
        if(!this.level().isClientSide()) this.entityData.set(OCCUPIED, this.countOccupiedSlots());
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public int getMaxStackSize() {
        return 64; // 1.7 getInventoryStackLimit
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack stack : this.slots) if(!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.slots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.slots, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.slots, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.slots.set(slot, stack);
        ItemStacks.limitSize(stack, this.getMaxStackSize()); // 26.1 Container.getMaxStackSize(ItemStack)
    }

    @Override
    public void setChanged() { }

    @Override
    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.slots.clear();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        Inventories.save(output, this.slots, true);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.slots = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        Inventories.load(input, this.slots);
        this.entityData.set(OCCUPIED, this.countOccupiedSlots());
    }
}
