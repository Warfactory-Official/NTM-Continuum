package com.hbm.entity.missile;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.interfaces.StoredItems;
import com.hbm.client.ClientEffects;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * 1.7 entity/missile/EntityBobmazon - the parody delivery drone: dropped at y=300 over the customer, free-falls
 * straight down on hand-rolled integration, and once ground is under its nose it throws a particle blast, plays
 * the old-explosion stinger and parachutes its payload stack as an ItemEntity.
 * 1.7's ignoreFrustumCheck/setSize(1,3) are renderer/type-dimension concerns in 26.2 (registration's job).
 */
public class EntityBobmazon extends CompatEntity implements StoredItems {

    // 1.7 dataWatcher id 16 (int, default 0): impact effects only fire while != 1. Never written anywhere in
    // 1.7 - the gate is kept verbatim rather than second-guessed away.
    private static final EntityDataAccessor<Integer> STATUS =
            SynchedEntityData.defineId(EntityBobmazon.class, EntityDataSerializers.INT);

    // 1.7 null until a spawner assigns it; EMPTY is the sentinel here (1.7 NPE'd on saving a payload-less drone).
    public ItemStack payload = ItemStack.EMPTY;

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (visitor.visit(payload) && payload.isEmpty()) payload = ItemStack.EMPTY;
    }

    public EntityBobmazon(EntityType<? extends EntityBobmazon> type, Level level) {
        super(type, level);
    }

    public EntityBobmazon(Level level) {
        this(ModNtmEntities.BOBMAZON_DELIVERY.get(), level);
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(STATUS, 0);
    }

    public int getStatus() {
        return entityData.get(STATUS);
    }

    public void setStatus(int status) {
        entityData.set(STATUS, status);
    }

    @Override
    public void tick() {
        // 1.7 replaces Entity.onUpdate wholesale - no super.tick(), no baseTick side effects

        setDeltaMovement(0, -0.5, 0);

        for (int i = 0; i < 4; i++) {

            // 1.7 (int) casts kept - truncation, not floor
            BlockPos nose = new BlockPos((int) (getX() - 0.5), (int) (getY() + 1), (int) (getZ() - 0.5));
            if (!level().getBlockState(nose).isAir() && !level().isClientSide() && getStatus() != 1) {
                ExplosionLarge.spawnParticles(level(), getX(), getY() + 1, getZ(), 50);

                // 1.7 playSoundEffect "hbm:entity.oldExplosion"; category ambient per 1.7 sounds.json
                level().playSound(null, getX(), getY(), getZ(), ModSounds.OLD_EXPLOSION.get(),
                        SoundSource.AMBIENT, 10.0F, 0.5F + random.nextFloat() * 0.1F);

                if (!payload.isEmpty()) {
                    // 1.7 zeroes X/Z after the ItemEntity ctor rolls its scatter velocity; Y stays
                    ItemEntity pack = new ItemEntity(level(), getX(), getY() + 2, getZ(), payload);
                    pack.setDeltaMovement(0, pack.getDeltaMovement().y, 0);
                    level().addFreshEntity(pack);
                }

                discard();

                break;
            }

            this.setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y,
                    getZ() + getDeltaMovement().z);
        }

        if (level().isClientSide()) {
            // 1.7 effectNT type=exhaust mode=meteor count=1 width=0: one stationary flame puff at the tail
            ClientEffects.spawnRocketFlame(level(), getX(), getY() + 1, getZ(), 1F, 0D, 0D, 0D,
                    300 + random.nextInt(50));
        }
    }

    @Override
    protected void loadData(DataIn input) {
        this.payload = input.read("payload", dev.golem.core.world.ItemStacks.optionalCodec()).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveData(DataOut output) {
        // 1.7 wrote unconditionally and NPE'd on a null payload; EMPTY is simply skipped instead
        if (!payload.isEmpty()) output.store("payload", dev.golem.core.world.ItemStacks.optionalCodec(), payload);
    }

    // 26.2: hurt() final; 1.7 leaves Entity's default (immune)
    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    // 1.7: isInRangeToRenderDist(distance < 500000)
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }
}
