package com.hbm.entity.mob;

import dev.golem.core.data.CompatPathfinderMob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for "digging" entities, removes things such as fall behavior, enables noClip and implements some basic
 * movement code. 1.7 centred the eye height ({@code height * 0.5}); here each type states it at registration.
 */
public abstract class EntityBurrowingBase extends CompatPathfinderMob {

    protected float airDrag;
    protected float airDragY;
    protected float groundDrag;
    protected float groundDragY;

    protected EntityBurrowingBase(EntityType<? extends EntityBurrowingBase> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.airDrag = 0.995F;
        this.airDragY = 0.997F;
        this.groundDrag = 0.98F;
        this.groundDragY = 0.995F;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableToBase(source) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)) {
            return false;
        }

        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    /** Drag is applied depending on whether it is underground or airborne. */
    @Override
    public void travel(Vec3 input) {
        float drag = this.groundDrag;
        float dragY = this.groundDragY;

        if (!isEntityInsideOpaqueBlock() && !isInWater() && !isInLava()) {
            drag = this.airDrag;
            dragY = this.airDragY;
        }

        moveRelative(0.02F, input);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(drag, dragY, drag));
    }

    /** Sorry, can't use ladders. */
    @Override
    public boolean onClimbable() {
        return false;
    }

    /** If the mob supports the movement behavior through air. */
    public boolean canFly() {
        return false;
    }

    /** Whether the entity can freely dig up and down or if gravity should be applied instead. */
    protected boolean canSupportMovement() {
        return isEntityInsideOpaqueBlock() || canFly();
    }

    /**
     * 1.7 {@code Entity.isEntityInsideOpaqueBlock}: eight points around the eye, any normal cube. Vanilla's
     * {@code isInWall} answers false for a no-clip entity, so it cannot stand in.
     */
    public boolean isEntityInsideOpaqueBlock() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 8; i++) {
            double x = ((i & 1) - 0.5D) * getBbWidth() * 0.8D;
            double y = (((i >> 1) & 1) - 0.5D) * 0.1D;
            double z = (((i >> 2) & 1) - 0.5D) * getBbWidth() * 0.8D;
            pos.set(Mth.floor(getX() + x), Mth.floor(getEyeY() + y), Mth.floor(getZ() + z));

            if (level().getBlockState(pos).isRedstoneConductor(level(), pos)) return true;
        }

        return false;
    }
}
