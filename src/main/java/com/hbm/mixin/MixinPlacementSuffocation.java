package com.hbm.mixin;

import com.hbm.blocks.multiblock.PlacementEscape;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla has no entity-aware wall predicate; loader damage hooks and widened access cannot change isInWall.
 */
@Mixin(Entity.class)
public abstract class MixinPlacementSuffocation {
    @ModifyReturnValue(method = "isInWall", at = @At("RETURN"))
    private boolean hbm$placementSuffocation(boolean inWall) {
        if (!inWall || !((Object) this instanceof PlacementEscape escape) || !escape.hbm$hasPlacementEscape())
            return inWall;
        var entity = (Entity) (Object) this;
        float width = entity.getBbWidth() * 0.8F;
        var eye = AABB.ofSize(entity.getEyePosition(), width, 1.0E-6D, width);
        for (var pos : BlockPos.betweenClosed(Mth.floor(eye.minX), Mth.floor(eye.minY), Mth.floor(eye.minZ),
                Mth.floor(eye.maxX), Mth.floor(eye.maxY), Mth.floor(eye.maxZ))) {
            var state = entity.level().getBlockState(pos);
            if (state.isAir() || !state.isSuffocating(entity.level(), pos)
                    || escape.hbm$ignoresPlacementCollision(entity.level(), pos)) continue;
            if (Shapes.joinIsNotEmpty(
                    state.getCollisionShape(entity.level(), pos).move(pos.getX(), pos.getY(), pos.getZ()),
                    Shapes.create(eye), BooleanOp.AND)) return true;
        }
        return false;
    }
}
