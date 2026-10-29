package com.hbm.mixin;

import com.hbm.blocks.multiblock.MultiblockPlacementEscape;
import com.hbm.blocks.multiblock.PlacementEscape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cached vanilla shapes omit CollisionContext; loader hooks and widening cannot filter their result.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinPlacementCollision {
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void hbm$placementCollision(BlockGetter view, BlockPos pos, CollisionContext context,
                                        CallbackInfoReturnable<VoxelShape> result) {
        if (context instanceof EntityCollisionContext entity && entity.getEntity() instanceof PlacementEscape escape
                && view instanceof Level level && escape.hbm$ignoresPlacementCollision(level, pos)
                && MultiblockPlacementEscape.isCurrentState(level, pos, (BlockState) (Object) this))
            result.setReturnValue(Shapes.empty());
    }
}
