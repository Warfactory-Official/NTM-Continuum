package com.hbm.mixin;

import com.hbm.blocks.multiblock.MultiblockPlacementEscape;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinPlacementSuffocation {

    // Cached no-context shapes omit the placement owner; loader damage hooks run after this query.
    // Access widening cannot add that context. Only the freshly granted owner's folded cells are exempt.
    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void hbm$grantedCoreSuffocation(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Player player) || !MultiblockPlacementEscape.hasGrant(player)) return;
        Entity entity = (Entity) (Object) this;
        if (entity.noPhysics) {
            cir.setReturnValue(false);
            return;
        }
        float width = entity.getBbWidth() * 0.8F;
        AABB eye = AABB.ofSize(entity.getEyePosition(), width, 1.0E-6D, width);
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(eye.minX, eye.minY, eye.minZ),
                BlockPos.containing(eye.maxX, eye.maxY, eye.maxZ))) {
            var state = entity.level().getBlockState(pos);
            if (state.isAir() || !state.isSuffocating(entity.level(), pos)) continue;
            if ((MultiblockSurface.foldedCore(state) != null || MultiblockSurface.isFoldedCell(state))
                    && MultiblockPlacementEscape.ignores(player, entity.level(), pos)) continue;
            if (Shapes.joinIsNotEmpty(state.getCollisionShape(entity.level(), pos).move(pos.getX(), pos.getY(), pos.getZ()),
                    Shapes.create(eye), BooleanOp.AND)) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }
}
