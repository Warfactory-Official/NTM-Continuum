package com.hbm.entity.grenade;

import dev.golem.core.data.Tags;

import com.hbm.items.ModEntityItems;
import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import com.hbm.entity.effect.EntityMist;
import com.hbm.items.weapon.ItemDisperser;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * 1.7 EntityDisperserCanister: a thrown fluid canister that bursts into an {@link EntityMist} cloud on impact.
 */
public class EntityDisperserCanister extends EntityGrenadeBase {

    // 1.7: dataWatcher 12/13 - fluid id and item
    private static final EntityDataAccessor<Integer> FLUID =
            SynchedEntityData.defineId(EntityDisperserCanister.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> TYPE =
            SynchedEntityData.defineId(EntityDisperserCanister.class, EntityDataSerializers.ITEM_STACK);

    public EntityDisperserCanister(EntityType<? extends EntityDisperserCanister> type, Level level) {
        super(type, level);
    }

    public EntityDisperserCanister(Level level) {
        this(ModNtmEntities.DISPERSER_CANISTER.get(), level);
    }

    public EntityDisperserCanister(Level level, LivingEntity thrower) {
        this(level);
        initThrower(thrower);
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(FLUID, 0);
        builder.define(TYPE, ItemStack.EMPTY);
    }

    public FluidType getFluid() {
        FluidType fluid = Fluids.byNetworkId(entityData.get(FLUID));
        return fluid == null ? Fluids.NONE : fluid;
    }

    public EntityDisperserCanister setFluid(FluidType fluid) {
        entityData.set(FLUID, Fluids.networkId(fluid == null ? Fluids.NONE : fluid));
        return this;
    }

    public ItemDisperser getDisperserItem() {
        return entityData.get(TYPE).getItem() instanceof ItemDisperser disperser
                ? disperser
                : ModEntityItems.disperser_canister.get();
    }

    // 26.2: getType() taken by Entity; renamed
    public EntityDisperserCanister setDisperserItem(ItemDisperser item) {
        entityData.set(TYPE, new ItemStack(item));
        return this;
    }

    @Override
    public void explode() {
        if (!level().isClientSide()) {
            EntityMist mist = new EntityMist(level());
            mist.setType(getFluid());
            mist.setPos(getX(), getY(), getZ());
            mist.setArea(10, 5);
            mist.setDuration(80);
            level().addFreshEntity(mist);
            discard();
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        Fluids.write(output, "fluid", getFluid()); // registry name; the synched int is session-only
        output.store("item", dev.golem.core.world.ItemStacks.optionalCodec(), entityData.get(TYPE));
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        setFluid(Fluids.read(input, "fluid"));
        input.read("item", dev.golem.core.world.ItemStacks.optionalCodec()).ifPresent(stack -> entityData.set(TYPE, stack));
    }
}
