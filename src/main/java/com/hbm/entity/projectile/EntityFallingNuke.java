package com.hbm.entity.projectile;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatThrowableProjectile;
import com.hbm.explosion.ExplosionNukeCustom;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 entity/projectile/EntityFallingNuke - the custom nuke's "falling" trigger mode: the loaded NukeCustom block pops
 * out as this bomb, free-falls from where the block stood and detonates NukeCustom.explodeCustom with the stored yields
 * the moment its own block position stops being air. 1.7 registered as "entity_falling_bomb", tracking range 1000.
 */
public class EntityFallingNuke extends CompatThrowableProjectile {

    // 1.7: dataWatcher id 20 - block metadata of the spawning NukeCustom block; the spawner writes it before
    // spawnEntityInWorld and RenderFallingNuke switches on it to pick the model
    private static final EntityDataAccessor<Byte> NUKE_META =
            SynchedEntityData.defineId(EntityFallingNuke.class, EntityDataSerializers.BYTE);

    float tnt;
    float nuke;
    float hydro;
    float amat;
    float dirty;
    float schrab;
    float euph;

    public EntityFallingNuke(EntityType<? extends EntityFallingNuke> type, Level level) {
        super(type, level);
        // 1.7: setSize(0.98F, 0.98F); ignoreFrustumCheck is renderer-side in 26.2
        refreshDimensions();
    }

    public EntityFallingNuke(Level level, float tnt, float nuke, float hydro, float amat, float dirty, float schrab, float euph) {
        this(ModNtmEntities.FALLING_BOMB.get(), level);
        this.tnt = tnt;
        this.nuke = nuke;
        this.hydro = hydro;
        this.amat = amat;
        this.dirty = dirty;
        this.schrab = schrab;
        this.euph = euph;
        // 1.7: prevRotationYaw = rotationYaw = 90; prevRotationPitch = rotationPitch = 90
        this.setYRot(90F);
        this.yRotO = 90F;
        this.setXRot(90F);
        this.xRotO = 90F;
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(NUKE_META, (byte) 0);
    }

    /** 1.7: dataWatcher id 20, written by the NukeCustom spawner, read by RenderFallingNuke. */
    public int getNukeMeta() {
        return this.entityData.get(NUKE_META);
    }

    public void setNukeMeta(int meta) {
        this.entityData.set(NUKE_META, (byte) meta);
    }

    // 1.7: setSize(0.98F, 0.98F)
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(0.98F, 0.98F);
    }

    @Override
    public void tick() {
        // 1.7: onUpdate never calls super - no vanilla throwable drag/gravity/impact trace, everything manual below
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        // 1.7: horizontal drag *0.99, gravity 0.05, terminal velocity -1
        setDeltaMovement(motion.x * 0.99D, Math.max(motion.y - 0.05D, -1.0D), motion.z * 0.99D);

        rotation();

        // 1.7: detonates the moment its floor'd block position is not air (its onImpact override was empty -
        // the base's raytrace impact path was bypassed along with super.onUpdate)
        if (!level().getBlockState(BlockPos.containing(getX(), getY(), getZ())).isAir()) {
            if (!level().isClientSide()) {
                ExplosionNukeCustom.explodeCustom(level(), getX(), getY(), getZ(), tnt, nuke, hydro, amat, dirty,
                        schrab, euph);
                discard();
            }
        }
    }

    public void rotation() {
        this.xRotO = getXRot();

        if (getXRot() > -75F) setXRot(getXRot() - 2F);
    }

    // 1.7: isInRangeToRenderDist - squared camera distance against a flat 25000
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tnt = input.getFloatOr("tnt", 0F);
        nuke = input.getFloatOr("nuke", 0F);
        hydro = input.getFloatOr("hydro", 0F);
        amat = input.getFloatOr("amat", 0F);
        dirty = input.getFloatOr("dirty", 0F);
        schrab = input.getFloatOr("schrab", 0F);
        euph = input.getFloatOr("euph", 0F);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putFloat("tnt", tnt);
        output.putFloat("nuke", nuke);
        output.putFloat("hydro", hydro);
        output.putFloat("amat", amat);
        output.putFloat("dirty", dirty);
        output.putFloat("schrab", schrab);
        output.putFloat("euph", euph);
    }
}
