package com.hbm.entity.item;

import dev.golem.core.data.Tags;

import com.hbm.items.ModEntityItems;
import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.tool.ItemDrone;
import com.hbm.items.tool.ItemDrone.EnumDroneType;
import com.hbm.util.ChunkShapeHelper;
import com.hbm.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code entity/item/EntityDeliveryDrone}: the patrol drone. Carries an 18-slot hold and an optional fluid
 * payload between network nodes, and may keep its flight path chunk-loaded.
 */
public class EntityDeliveryDrone extends EntityDroneBase implements Container {

    public static final int SLOT_COUNT = 18;
    // 1.7 dataWatcher id 11
    private static final EntityDataAccessor<Boolean> EXPRESS =
            SynchedEntityData.defineId(EntityDeliveryDrone.class, EntityDataSerializers.BOOLEAN);

    private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    public @Nullable FluidStack fluid;

    private boolean chunkLoading;
    private final LongOpenHashSet forcedChunks = new LongOpenHashSet();

    public EntityDeliveryDrone(EntityType<? extends EntityDeliveryDrone> type, Level level) {
        super(type, level);
    }

    public EntityDeliveryDrone(Level level) {
        super(ModNtmEntities.DELIVERY_DRONE.get(), level);
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(EXPRESS, false);
    }

    public void setExpress(boolean express) {
        entityData.set(EXPRESS, express);
    }

    public boolean isExpress() {
        return entityData.get(EXPRESS);
    }

    public void setChunkLoading(boolean chunkLoading) {
        this.chunkLoading = chunkLoading;
    }

    @Override
    public double getSpeed() {
        return isExpress() ? 0.375 * 3 : 0.375;
    }

    /** 1.7 hitByEntity: punching a patrol drone spills its hold and returns the matching drone item. */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (isRemoved() || !(attacker instanceof Player) || !(level() instanceof ServerLevel server)) {
            return false;
        }

        discard();
        for (ItemStack stack : this.slots) if (!stack.isEmpty()) spawnAtLocation(server, stack);
        spawnAtLocation(server, ModEntityItems.drone.stack(droneType()));
        return false;
    }

    /** 1.7 encoded express and chunkloading as the low two bits of the drone item's metadata. */
    private EnumDroneType droneType() {
        if (isExpress()) {
            return this.chunkLoading ? EnumDroneType.PATROL_EXPRESS_CHUNKLOADING : EnumDroneType.PATROL_EXPRESS;
        }
        return this.chunkLoading ? EnumDroneType.PATROL_CHUNKLOADING : EnumDroneType.PATROL;
    }

    /**
     * 1.7 forced the pos -> pos+motion segment with an 8-block pad via a ForgeChunkManager ENTITY ticket;
     * here the same sliding claim rides per-entity anchor tickets. 8 was the lowest pad that kept the
     * waypoint path loaded - a narrower claim stalls the drone at the border, the chunk ahead never ticked.
     */
    @Override
    protected void loadNeighboringChunks() {
        if (!this.chunkLoading || !(level() instanceof ServerLevel server)) return;

        LongOpenHashSet wanted = ChunkShapeHelper.getChunksAlongLineSegment(
                Mth.floor(getX()), Mth.floor(getZ()),
                Mth.floor(getX() + getDeltaMovement().x()), Mth.floor(getZ() + getDeltaMovement().z()), 8);

        LongIterator stale = this.forcedChunks.iterator();
        while (stale.hasNext()) {
            long key = stale.nextLong();
            if (!wanted.contains(key)) {
                ChunkUtil.releaseAnchor(server, key);
                stale.remove();
            }
        }
        LongIterator added = wanted.iterator();
        while (added.hasNext()) {
            long key = added.nextLong();
            if (this.forcedChunks.add(key)) ChunkUtil.addAnchor(server, key);
        }
    }

    public void clearChunkLoader() {
        if (this.forcedChunks.isEmpty() || !(level() instanceof ServerLevel server)) return;
        LongIterator it = this.forcedChunks.iterator();
        while (it.hasNext()) ChunkUtil.releaseAnchor(server, it.nextLong());
        this.forcedChunks.clear();
    }

    // 26.2: forced chunks outlive entities where 1.7 tickets died with theirs
    @Override
    public void remove(RemovalReason reason) {
        clearChunkLoader();
        super.remove(reason);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.slots.clear();
        dev.golem.core.data.Inventories.load(input, this.slots);
        // the carried fluid: registry name + amount (never the numeric fluid id, History section 19ej-ids)
        this.fluid = input.child("fluid").map(tag -> new FluidStack(Fluids.read(tag, "type"),
                tag.getIntOr("amount", 0), tag.getIntOr("pressure", 0))).orElse(null);
        setExpress(input.getBooleanOr("express", false));
        this.chunkLoading = input.getBooleanOr("chunkLoading", false);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        dev.golem.core.data.Inventories.save(output, this.slots, true);
        if (this.fluid != null) {
            DataOut tag = output.child("fluid");
            Fluids.write(tag, "type", this.fluid.type);
            tag.putInt("amount", this.fluid.fill);
            tag.putInt("pressure", this.fluid.pressure);
        }
        output.putBoolean("express", isExpress());
        output.putBoolean("chunkLoading", this.chunkLoading);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public int getMaxStackSize() {
        return 64; // 1.7 getInventoryStackLimit
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.slots) if (!stack.isEmpty()) return false;
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
        dev.golem.core.world.ItemStacks.limitSize(stack, getMaxStackSize());
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return false; // 1.7 isUseableByPlayer: the hold has no GUI, only the network touches it
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false; // 1.7 isItemValidForSlot
    }

    @Override
    public void clearContent() {
        this.slots.clear();
    }
}
