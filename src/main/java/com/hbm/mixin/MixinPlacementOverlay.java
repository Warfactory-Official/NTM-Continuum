package com.hbm.mixin;

import com.hbm.blocks.multiblock.MultiblockPlacementEscape;
import com.hbm.blocks.multiblock.PlacementEscape;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla and loader hooks cannot skip one overlay cell and test the rest; widening cannot add that predicate.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class MixinPlacementOverlay {
    @Redirect(
            //? if fabric {
            /*method = "getViewBlockingState",
            *///?} else {
            method = "getOverlayBlock",
            //?}
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "isViewBlocking(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean hbm$placementOverlay(BlockState state, BlockGetter view, BlockPos pos, Player player) {
        return state.isViewBlocking(view, pos) && !(view instanceof Level level
                && ((PlacementEscape) player).hbm$ignoresPlacementCollision(level, pos)
                && MultiblockPlacementEscape.isCurrentState(level, pos, state));
    }
}
