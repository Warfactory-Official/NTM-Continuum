package com.hbm.mixin;

import com.hbm.blocks.multiblock.MultiblockPlacementEscape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
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

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinPlacementCollision {

    // Cached state queries bypass subclass shapes; loader hooks cannot replace this collision verdict.
    // Access widening cannot change it. Prospective placement states retain vanilla's obstruction veto.
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void hbm$placementEscape(BlockGetter view, BlockPos pos, CollisionContext context,
                                     CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player && view instanceof Level level
                && MultiblockPlacementEscape.ignores(player, level, pos)
                && MultiblockPlacementEscape.isCurrentState(level, pos, (BlockState) (Object) this)) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
