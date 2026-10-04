package com.hbm.entity.cart;

import dev.golem.core.data.Tags;

import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatMinecartContainer;
import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * 1.7 {@code entity/cart/EntityMinecartContainerBase}: the inventory-bearing half of the cart roster.
 *
 * <p>Java has no multiple inheritance and 26.2 hangs the container behaviour off
 * {@link AbstractMinecartContainer}, so this cannot extend {@link EntityMinecartNTM} the way 1.7 did.
 * The chassis accessor is therefore restated here; both halves answer {@link ICartChassis}.
 */
public abstract class EntityMinecartContainerBase extends CompatMinecartContainer implements ICartChassis {

    // 1.7 dataWatcher id 23, same as EntityMinecartNTM
    private static final EntityDataAccessor<Integer> BASE =
            SynchedEntityData.defineId(EntityMinecartContainerBase.class, EntityDataSerializers.INT);

    protected EntityMinecartContainerBase(EntityType<? extends EntityMinecartContainerBase> type, Level level) {
        super(type, level);
        // 26.2: AbstractMinecartContainer hardcodes 36 slots; this re-sizes to getContainerSize().
        clearItemStacks();
    }

    protected EntityMinecartContainerBase(EntityType<? extends EntityMinecartContainerBase> type, Level level,
                                          double x, double y, double z, EnumCartBase base) {
        this(type, level);
        dev.golem.core.world.Compat.setInitialPos(this, x, y, z);
        setBase(base);
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(BASE, 0);
    }

    public void setBase(EnumCartBase type) {
        entityData.set(BASE, type.ordinal());
    }

    @Override
    public EnumCartBase getBase() {
        return EnumCartBase.values()[entityData.get(BASE)];
    }

    @Override
    public int getMaxStackSize() {
        return 64; // 1.7 getInventoryStackLimit
    }

    @Override
    protected Item getDropItem() {
        return getCartItem().getItem();
    }

    @Override
    public ItemStack getPickResult() {
        return getCartItem();
    }

    /**
     * 1.7 killMinecart: exactly one assembled cart item. The contents are neither packed nor spilled - 1.7's
     * container base never overrides killMinecart, so they vanish. {@code clearContent} heads off the spill
     * {@code AbstractMinecartContainer#remove} runs on a destroying removal.
     */
    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        clearContent();
        dev.golem.core.world.Compat.kill(this, level);

        if (Compat.entityDrops(level)) {
            ItemStack stack = getCartItem();
            if (hasCustomName()) dev.golem.core.world.ItemStacks.setCustomName(stack, getCustomName());
            spawnAtLocation(level, stack);
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        com.hbm.util.SavedIds.putEnum(output, "base", getBase()); // by name; the synched int is session-only
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        setBase(com.hbm.util.SavedIds.getEnum(input, "base", EnumCartBase.values()[0]));
    }
}
