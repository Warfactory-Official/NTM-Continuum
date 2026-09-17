package com.hbm.entity.cart;

import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatMinecart;
import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * 1.7 {@code entity/cart/EntityMinecartNTM}: the shared base for NTM's cart roster. Carries the chassis
 * material ({@link EnumCartBase}) as synched data and drops the matching assembled item when broken.
 */
public abstract class EntityMinecartNTM extends CompatMinecart implements ICartChassis {

    // 1.7 dataWatcher id 23
    private static final EntityDataAccessor<Integer> BASE =
            SynchedEntityData.defineId(EntityMinecartNTM.class, EntityDataSerializers.INT);

    protected EntityMinecartNTM(EntityType<? extends EntityMinecartNTM> type, Level level) {
        super(type, level);
    }

    protected EntityMinecartNTM(EntityType<? extends EntityMinecartNTM> type, Level level,
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

    // VehicleEntity's plain-Item drop; the real drop carries components, so destroy() overrides it below
    @Override
    protected Item getDropItem() {
        return getCartItem().getItem();
    }

    @Override
    public boolean isPickable() {
        return true; // 1.7 canBeCollidedWith
    }

    @Override
    public ItemStack getPickResult() {
        return getCartItem();
    }

    /**
     * 1.7 killMinecart: drops the assembled cart item, carrying any custom name across.
     */
    @Override
    public void destroy(ServerLevel level, DamageSource source) {
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
        output.putInt("base", entityData.get(BASE));
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        entityData.set(BASE, input.getIntOr("base", 0));
    }
}
