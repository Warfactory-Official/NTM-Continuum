package com.hbm.entity.explosive;

import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.Tags;

import com.hbm.blocks.bomb.BlockTNTBase;
import com.hbm.blocks.bomb.ModBombBlocks;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Custom primed explosive carrying the HBM block kind, fuse, and owner. */
public class EntityPrimedExplosive extends CompatEntity {

    private static final EntityDataAccessor<Integer> FUSE =
            SynchedEntityData.defineId(EntityPrimedExplosive.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(EntityPrimedExplosive.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ARMED =
            SynchedEntityData.defineId(EntityPrimedExplosive.class,
                    EntityDataSerializers.BOOLEAN);

    private int fuse = 80;
    private @Nullable UUID ownerId;
    public boolean detonateOnCollision;

    public EntityPrimedExplosive(EntityType<?> type, Level level) {
        super(type, level);
        blocksBuilding = true;
    }

    public EntityPrimedExplosive(Level level, double x, double y, double z, @Nullable LivingEntity owner,
                                 BlockState state) {
        this(ModExplosiveEntities.PRIMED_EXPLOSIVE.get(), level);
        setPos(x, y, z);
        double angle = level.getRandom().nextDouble() * (float) (Math.PI * 2.0D);
        setDeltaMovement(-Math.sin(angle) * 0.02D, 0.2F, -Math.cos(angle) * 0.02D);
        xo = x;
        yo = y;
        zo = z;
        ownerId = owner == null ? null : owner.getUUID();
        setExplosiveState(state);
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(FUSE, 80);
        data.define(KIND, 0);
        data.define(ARMED, false);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public void tick() {
        // Vanilla PrimedTnt skips Entity.tick/baseTick; baseTick would process fluids before movement.
        compatHandlePortal();
        if (!isNoGravity()) setDeltaMovement(getDeltaMovement().add(0D, -0.04D, 0D));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.98D));
        if (onGround()) setDeltaMovement(getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));

        boolean collided = horizontalCollision || verticalCollision;
        int nextFuse = getFuse() - 1;
        setFuse(nextFuse);
        if (nextFuse <= 0 || detonateOnCollision && collided) {
            discard();
            if (!level().isClientSide()) detonate();
        } else {
            compatUpdateInWaterStateAndDoFluidPushing();
            if (level().isClientSide()) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5D, getZ(), 0D, 0D, 0D);
            }
        }
    }

    private void detonate() {
        BlockState state = getExplosiveState();
        if (state.getBlock() instanceof BlockTNTBase explosive) {
            explosive.detonate(level(), getX(), getY(), getZ(), this);
        }
    }

    public int getFuse() {
        return level().isClientSide() ? entityData.get(FUSE) : fuse;
    }

    public void setFuse(int fuse) {
        this.fuse = fuse;
        entityData.set(FUSE, fuse);
    }

    public void setExplosiveState(BlockState state) {
        if (!(state.getBlock() instanceof BlockTNTBase explosive)) return;
        entityData.set(KIND, explosive.kindId());
        entityData.set(ARMED, state.getValue(BlockTNTBase.ARMED));
    }

    public BlockState getExplosiveState() {
        BlockTNTBase block = switch (entityData.get(KIND)) {
            case 1 -> ModBombBlocks.dynamite.get();
            case 2 -> ModBombBlocks.semtex.get();
            case 3 -> ModBombBlocks.c4.get();
            case 4 -> ModExplosivesBlocks.fissure_bomb.get(); // com.hbm.explosives
            // The effect barrels of com.hbm.fluidnet (kinds 20-25).
            case 20 -> ModFluidNetBlocks.red_barrel.get();
            case 21 -> ModFluidNetBlocks.pink_barrel.get();
            case 22 -> ModFluidNetBlocks.yellow_barrel.get();
            case 23 -> ModFluidNetBlocks.vitrified_barrel.get();
            case 24 -> ModFluidNetBlocks.lox_barrel.get();
            case 25 -> ModFluidNetBlocks.taint_barrel.get();
            default -> ModBombBlocks.tnt.get();
        };
        return block.defaultBlockState().setValue(BlockTNTBase.ARMED, entityData.get(ARMED));
    }

    public @Nullable LivingEntity getOwnerEntity() {
        if (ownerId == null) return null;
        if (level() instanceof ServerLevel server) {
            Entity entity = server.getEntity(ownerId);
            return entity instanceof LivingEntity living ? living : null;
        }
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(256D))) {
            if (ownerId.equals(living.getUUID())) return living;
        }
        return null;
    }

    public @Nullable UUID getOwnerId() {
        return ownerId;
    }

    @Override
    protected void saveData(DataOut output) {
        // Intentional old bug repair: 1.12 wrote "fuse" but read "Fuse", failed to restore the synced
        // block state, and omitted owner/collision state. Keep all useful state across saves here.
        output.putInt("fuse", fuse);
        output.putInt("kind", entityData.get(KIND));
        output.putBoolean("armed", entityData.get(ARMED));
        output.putBoolean("detonateOnCollision", detonateOnCollision);
        if (ownerId != null) output.store("owner", UUIDUtil.CODEC, ownerId);
    }

    @Override
    protected void loadData(DataIn input) {
        setFuse(input.getIntOr("fuse", 80));
        entityData.set(KIND, input.getIntOr("kind", 0));
        entityData.set(ARMED, input.getBooleanOr("armed", false));
        detonateOnCollision = input.getBooleanOr("detonateOnCollision", false);
        ownerId = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }
}
