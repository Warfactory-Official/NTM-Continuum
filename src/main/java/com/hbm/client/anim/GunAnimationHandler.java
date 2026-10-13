package com.hbm.client.anim;

import com.hbm.items.armor.ArmorTrenchmaster;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.packet.GunAnimationPayload;
import com.hbm.util.GameTime;
import dev.golem.core.render.anim.BusAnimation;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Turns an animation cue from the server into a running animation, and stamps the shot time.
 *
 * <p>Ported from the handler half of 1.12.2's {@code GunAnimationPacketSedna}. The server is the only
 * thing that knows when a gun actually fired or finished a reload, so it says so and the client plays
 * the matching animation. That is also why muzzle flash and recoil key off this rather than off the
 * item's synced state, which arrives a tick or more later.
 *
 * <p>A cue the gun has no animation for is silently ignored, exactly as upstream does.
 */
public final class GunAnimationHandler {

    private GunAnimationHandler() {}

    public static void handle(GunAnimationPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        InteractionHand hand = payload.interactionHand();
        ItemStack stack = minecraft.player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return;

        String heldItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (payload.itemId() == null || !heldItemId.equals(payload.itemId())) return;

        if (payload.animation() < 0 || payload.animation() >= GunAnimation.VALUES.length) return;

        GunAnimation cue = GunAnimation.byOrdinal(payload.animation());
        HbmAnimations.setBuildingHand(hand);
        int index = payload.configIndex();
        if (!gun.isValidConfigIndex(index)) return;
        GunConfig config = gun.getConfig(stack, index);
        Receiver[] receivers = config.getReceivers(stack);
        if (receivers == null) return;

        // The shot timestamp drives muzzle flash and smoke, and must be stamped even when the gun has
        // no animation table at all.
        if ((cue == GunAnimation.CYCLE || cue == GunAnimation.ALT_CYCLE)
                && index < gun.lastShotByHand[hand.ordinal()].length) {
            // Use the client world clock, not wall time: smoke and muzzle flash share this epoch.
            gun.lastShotByHand[hand.ordinal()][index] = GameTime.millis(minecraft.level);
            if (cue == GunAnimation.CYCLE) {
                gun.shotRandByHand[hand.ordinal()] = minecraft.level.getRandom().nextDouble();
                int receiverIndex = payload.receiver();
                if (receiverIndex < 0 || receiverIndex >= receivers.length) return;
                BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> recoil =
                        receivers[receiverIndex].getRecoil(stack);
                if (recoil != null) recoil.accept(stack,
                        new ItemGunBaseNT.LambdaContext(config, minecraft.player,
                                minecraft.player.getInventory(), hand, index));
            }
        }

        // The config's function is the one a weapon mod substitutes, so it is asked first; a mod that
        // has no opinion on this cue answers null and the gun's own table supplies the animation.
        BiFunction<ItemStack, GunAnimation, BusAnimation> configured =
                config.getAnimation(stack);
        BusAnimation animation = configured == null ? null : configured.apply(stack, cue);
        if (animation == null) animation = GunAnimations.get(stack, cue, index);
        // 1.7 HbmAnimationPacket: a Trenchmaster's reload animations play at double speed. Resource-loaded
        // animations are cached and shared, and setTimeMult scales from the original durations, so a reload
        // without the suit explicitly restores 1x rather than inheriting the last Trenchmaster reload's speed.
        boolean isReloadAnimation = cue == GunAnimation.RELOAD || cue == GunAnimation.RELOAD_CYCLE;
        if (animation != null && isReloadAnimation) animation.setTimeMult(
                ArmorTrenchmaster.isTrenchMaster(minecraft.player) ? 0.5D : 1.0D);
        // 1.7: a sequential reload holds its last frame until the next cycle replaces it; everything else expires.
        if (animation != null) HbmAnimations.start(stack.getItem(), animation, hand, index,
                isReloadAnimation && config.getReloadAnimSequential(stack));
    }
}
