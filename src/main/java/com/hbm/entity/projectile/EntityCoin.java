package com.hbm.entity.projectile;

import com.hbm.entity.NtmToolEntityCatalog;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityCoin extends EntityThrowableNT {
    public EntityCoin(EntityType<? extends EntityCoin> type, Level level) {
        super(type, level);
        setBoundingBox(makeBoundingBox(position()));
    }

    public EntityCoin(Level level) { this(NtmToolEntityCatalog.COIN.get(), level); }

    public void setThrower(LivingEntity thrower) { this.thrower = thrower; }
    public LivingEntity getThrower() { return thrower; }

    protected AABB makeBoundingBox(Vec3 position) {
        // The source yOffset is 0.5, so setPosition's one-block box is shifted down by half a block.
        return new AABB(position.x - 0.5D, position.y - 0.5D, position.z - 0.5D,
                position.x + 0.5D, position.y + 0.5D, position.z + 0.5D);
    }

    @Override protected void onImpact(HitResult hit) {
        if (hit instanceof BlockHitResult) discard();
    }

    @Override protected float getAirDrag() { return 1F; }
    @Override public double getGravityVelocity() { return 0.02D; }
    @Override public boolean isPickable() { return true; }
}
