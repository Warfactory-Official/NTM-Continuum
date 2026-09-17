package com.hbm.entity.mob;

import com.hbm.items.ModEntityItems;
import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.CompatSquid;
import com.hbm.entity.item.EntityItemBuoyant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

// 1.7: EntityPlasticBag keeps the bag's pop, drop and spawn rules on the current squid.
public class EntityPlasticBag extends CompatSquid {

    public EntityPlasticBag(EntityType<? extends EntityPlasticBag> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Squid.createAttributes();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.45F, 0.45F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        discard();
        spawnAtLocation(level, new ItemStack(ModEntityItems.plastic_bag.get()), 0F);
        return true;
    }

    // 1.7: the bag's drops float because it dies in open water.
    @Override
    public @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemStack stack, float offset) {
        if (stack.isEmpty()) return null;

        EntityItemBuoyant item = new EntityItemBuoyant(level, getX(), getY() + offset, getZ(), stack);
        item.setPickUpDelay(10);
        level.addFreshEntity(item);
        return item;
    }

    // ntm-next overrides getBreedOffspring to spawn a bag; squids only became ageable in 1.21.2, and 1.7's
    // bag never bred, so the port leaves vanilla's squid answer (a breeding squid child) on 26.1 and has no
    // hook below it. Recorded in entity-port-manifest.md.

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return getY() > 45.0D && getY() < 63.0D && random.nextInt(10) == 0
                && super.checkSpawnRules(level, reason);
    }
}
