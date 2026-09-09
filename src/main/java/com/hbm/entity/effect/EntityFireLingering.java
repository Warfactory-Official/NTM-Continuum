package com.hbm.entity.effect;

import com.hbm.capability.HbmLivingProps;
import com.hbm.client.FireLingeringClientEffects;
import com.hbm.client.particle.FlameCreator;
import com.hbm.entity.ModEntities;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ground fire that stays put: an invisible box that re-ignites everything standing in its column every
 * tick, and raises its own flames on each client rather than through particle packets.
 *
 * <p>1.12.2 overrides {@code onEntityUpdate} without calling super, so none of the base per-tick block
 * — fire ticks, air, void, fluid push — runs. That is reproduced here by not calling {@code super.tick()},
 * which is load-bearing: a lingering fire must not drown or suffocate.
 *
 * <p>Upstream's black-fire type is absent, since nothing that fires it is ported.
 */
public class EntityFireLingering extends CompatEntity {

    public static final int TYPE_DIESEL = 0;
    public static final int TYPE_BALEFIRE = 1;
    public static final int TYPE_PHOSPHORUS = 2;
    public static final int TYPE_OXY = 3;

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
            SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.FLOAT);

    public int maxAge = 150;

    public EntityFireLingering(EntityType<? extends EntityFireLingering> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EntityFireLingering(Level level) {
        this(ModEntities.FIRE_LINGERING.get(), level);
    }

    public EntityFireLingering setArea(float width, float height) {
        this.entityData.set(WIDTH, width);
        this.entityData.set(HEIGHT, height);
        return this;
    }

    public EntityFireLingering setDuration(int duration) {
        this.maxAge = duration;
        return this;
    }

    public EntityFireLingering setFireType(int type) {
        this.entityData.set(TYPE, type);
        return this;
    }

    public int getFireType() {
        return this.entityData.get(TYPE);
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(TYPE, 0);
        data.define(WIDTH, 0F);
        data.define(HEIGHT, 0F);
    }

    @Override
    public void tick() {
        this.tickCount++;
        if (this.tickCount >= maxAge) {
            this.discard();
            return;
        }
        float width = this.entityData.get(WIDTH);
        float height = this.entityData.get(HEIGHT);

        if (this.level() instanceof ServerLevel) burn(width, height);
        else raiseFlames(width, height);
    }

    private void burn(float width, float height) {
        List<Entity> affected = this.level().getEntities(this,
                new AABB(getX() - width / 2, getY(), getZ() - width / 2,
                        getX() + width / 2, getY() + height, getZ() + width / 2));

        for (Entity e : affected) {
            if (!(e instanceof LivingEntity living)) {
                Effects.ignite(e, 4);
                continue;
            }
            switch (getFireType()) {
                case TYPE_DIESEL -> HbmLivingProps.setFireAtLeast(living, 60);
                case TYPE_PHOSPHORUS -> HbmLivingProps.setFireAtLeast(living, 300);
                case TYPE_BALEFIRE -> HbmLivingProps.setBalefireAtLeast(living, 100);
                default -> { }
            }
        }
    }

    /** Each flame is dropped onto whatever the column hits, so fire on a slope sits on the ground. */
    private void raiseFlames(float width, float height) {
        for (int i = 0; i < (width >= 5 ? 2 : 1); i++) {
            double x = getX() - width / 2 + this.random.nextDouble() * width;
            double z = getZ() - width / 2 + this.random.nextDouble() * width;

            Vec3 up = new Vec3(x, getY() + height, z);
            Vec3 down = new Vec3(x, getY() - height, z);
            BlockHitResult hit = this.level().clip(new ClipContext(up, down, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK) down = hit.getLocation();

            int meta;
            switch (getFireType()) {
                case TYPE_DIESEL, TYPE_PHOSPHORUS -> meta = FlameCreator.META_NORMAL;
                case TYPE_BALEFIRE -> meta = FlameCreator.META_BALEFIRE;
                default -> { continue; }
            }
            FireLingeringClientEffects.flame(this.level(), x, down.y, z, meta);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void saveData(DataOut output) {}

    @Override
    protected void loadData(DataIn input) {
        this.discard();
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}
