package com.hbm.entity.grenade;

import com.hbm.items.ModEntityItems;
import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import com.hbm.items.weapon.ItemGenericGrenade;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * 1.7 EntityGrenadeBouncyGeneric: the thrown form of every ItemGenericGrenade, which supplies its fuse, bounciness and detonation.
 */
public class EntityGrenadeBouncyGeneric extends EntityGrenadeBouncyBase implements IGenericGrenade {

    // 1.7: dataWatcher 12 - item id; 26.2 syncs the item
    private static final EntityDataAccessor<ItemStack> GRENADE =
            SynchedEntityData.defineId(EntityGrenadeBouncyGeneric.class, EntityDataSerializers.ITEM_STACK);

    public EntityGrenadeBouncyGeneric(EntityType<? extends EntityGrenadeBouncyGeneric> type, Level level) {
        super(type, level);
    }

    public EntityGrenadeBouncyGeneric(Level level) {
        this(ModNtmEntities.GRENADE_BOUNCY_GENERIC.get(), level);
    }

    public EntityGrenadeBouncyGeneric(Level level, LivingEntity thrower) {
        this(level);
        initThrower(thrower);
    }

    public EntityGrenadeBouncyGeneric(Level level, double x, double y, double z) {
        this(level);
        setPos(x, y, z);
    }

    public EntityGrenadeBouncyGeneric setType(ItemGenericGrenade grenade) {
        entityData.set(GRENADE, new ItemStack(grenade));
        return this;
    }

    @Override
    public ItemGenericGrenade getGrenade() {
        return entityData.get(GRENADE).getItem() instanceof ItemGenericGrenade gren
                ? gren
                : ModEntityItems.stick_dynamite.get();
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(GRENADE, ItemStack.EMPTY);
    }

    @Override
    public void explode() {
        getGrenade().explode(this, getThrower(), level(), getX(), getY(), getZ());
        discard();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.store("grenade", dev.golem.core.world.ItemStacks.optionalCodec(), entityData.get(GRENADE));
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        input.read("grenade", dev.golem.core.world.ItemStacks.optionalCodec()).ifPresent(stack -> entityData.set(GRENADE, stack));
    }

    @Override
    protected int getMaxTimer() {
        return getGrenade().getMaxTimer();
    }

    @Override
    protected double getBounceMod() {
        return getGrenade().getBounceMod();
    }
}
