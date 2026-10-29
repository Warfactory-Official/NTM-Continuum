package com.hbm.mixin;

import com.hbm.blocks.multiblock.MultiblockPlacementEscape;
import com.hbm.blocks.multiblock.PlacementEscape;
import com.hbm.handler.SpecialPlayers;
import com.hbm.items.armor.ArmorFSB;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Power armour's hard landing: the stand-in for 1.12's {@code LivingFallEvent}.
 *
 * <p><b>Why this targets {@code Player} and not {@code LivingEntity}.</b> {@code Player.causeFallDamage}
 * <b>returns early whenever the player may fly</b>, i.e. in creative, and never calls {@code super}, so a
 * hook on {@code LivingEntity.causeFallDamage} never runs there. That early return is where Forge fires
 * {@code PlayerFlyableFallEvent}, which upstream subscribes to <i>alongside</i> {@code LivingFallEvent}.
 * Injecting at the head of the {@code Player} override covers both branches with one hook.
 *
 * <p>HEAD, because vanilla resets {@code fallDistance} immediately afterwards.
 *
 * <p>The two branches are a descriptor guard: the first parameter widened from {@code float} to
 * {@code double} at 26.1, and a mixin injector's parameters must match its target exactly.
 */
@Mixin(Player.class)
public class MixinPlayer implements PlacementEscape {

    @Unique
    private long @Nullable [] hbm$escapeCores;
    @Unique
    private int hbm$escapeCount;
    @Unique
    private @Nullable ResourceKey<Level> hbm$escapeDimension;

    @Override
    public void hbm$grantPlacementEscape(ResourceKey<Level> dimension, long core) {
        if (hbm$escapeDimension != dimension) {
            hbm$escapeCount = 0;
            hbm$escapeDimension = dimension;
        }
        for (int i = 0; i < hbm$escapeCount; i++) if (hbm$escapeCores[i] == core) return;
        if (hbm$escapeCores == null) hbm$escapeCores = new long[2];
        else if (hbm$escapeCount == hbm$escapeCores.length)
            hbm$escapeCores = Arrays.copyOf(hbm$escapeCores, hbm$escapeCount * 2);
        hbm$escapeCores[hbm$escapeCount++] = core;
    }

    @Override
    public void hbm$refreshPlacementEscape() {
        if (hbm$escapeCount == 0) return;
        var player = (Player) (Object) this;
        if (hbm$escapeDimension != player.level().dimension()) {
            hbm$escapeCount = 0;
            hbm$escapeDimension = null;
            return;
        }
        var feet = BlockPos.containing(player.getX(), player.getY() + 0.1D, player.getZ());
        long feetOwner = MultiblockPlacementEscape.ownerAt(player.level(), feet);
        long headOwner = MultiblockPlacementEscape.ownerAt(player.level(), feet.above());
        long eyeOwner = MultiblockPlacementEscape.ownerAt(player.level(), BlockPos.containing(player.getEyePosition()));
        if (feetOwner == MultiblockPlacementEscape.UNRESOLVED || headOwner == MultiblockPlacementEscape.UNRESOLVED
                || eyeOwner == MultiblockPlacementEscape.UNRESOLVED)
            return;
        int kept = 0;
        for (int i = 0; i < hbm$escapeCount; i++) {
            long core = hbm$escapeCores[i];
            if (core == feetOwner || core == headOwner || core == eyeOwner) hbm$escapeCores[kept++] = core;
        }
        hbm$escapeCount = kept;
        if (kept == 0) hbm$escapeDimension = null;
    }

    @Override
    public boolean hbm$hasPlacementEscape() {
        hbm$refreshPlacementEscape();
        return hbm$escapeCount != 0;
    }

    @Override
    public boolean hbm$ignoresPlacementCollision(Level level, BlockPos pos) {
        if (hbm$escapeCount == 0 || level != ((Player) (Object) this).level()) return false;
        hbm$refreshPlacementEscape();
        if (hbm$escapeCount == 0) return false;
        long owner = MultiblockPlacementEscape.ownerAt(level, pos);
        if (owner == MultiblockPlacementEscape.NO_CORE || owner == MultiblockPlacementEscape.UNRESOLVED) return false;
        for (int i = 0; i < hbm$escapeCount; i++) if (hbm$escapeCores[i] == owner) return true;
        return false;
    }

    // Rung 2 fails: Fabric has no player tick event.
    @Inject(method = "tick", at = @At("HEAD"))
    private void hbm$playerTickStart(CallbackInfo ci) {
        hbm$refreshPlacementEscape();
        SpecialPlayers.tickStart((Player) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void hbm$playerTickEnd(CallbackInfo ci) {
        SpecialPlayers.tickEnd((Player) (Object) this);
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"))
    //? if >=26.1 {
    private void hbm$fall(double fallDistance, float damageModifier,
                          net.minecraft.world.damagesource.DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        ArmorFSB.handleFall((Player) (Object) this, (float) fallDistance);
    }
    //?} else {
    /*private void hbm$fall(float fallDistance, float damageModifier,
            net.minecraft.world.damagesource.DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        ArmorFSB.handleFall((Player) (Object) this, fallDistance);
    }
    *///?}
}
