package com.hbm.client.anim;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.packet.GunAnimationPayload;
import com.hbm.util.GameTime;
import dev.golem.core.render.anim.BusAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Turns an animation cue from the server into a running animation, and stamps the shot time.
 *
 * <p>Ported from the handler half of 1.12.2's {@code GunAnimationPacketSedna}. The server is the only
 * thing that knows when a gun actually fired or finished a reload, so it says so and the client plays
 * the matching animation — which is also why muzzle flash and recoil key off this rather than off the
 * item's synced state, which arrives a tick or more later.
 *
 * <p>A cue the gun has no animation for is silently ignored, exactly as upstream does.
 */
public final class GunAnimationHandler {

    private GunAnimationHandler() {}

    public static void handle(GunAnimationPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        net.minecraft.world.InteractionHand hand = payload.interactionHand();
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
                java.util.function.BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> recoil =
                        receivers[receiverIndex].getRecoil(stack);
                if (recoil != null) recoil.accept(stack,
                        new ItemGunBaseNT.LambdaContext(config, minecraft.player,
                                minecraft.player.getInventory(), hand, index));
            }
        }

        // The config's function is the one a weapon mod substitutes, so it is asked first; a mod that
        // has no opinion on this cue answers null and the gun's own table supplies the animation.
        java.util.function.BiFunction<ItemStack, GunAnimation, BusAnimation> configured =
                config.getAnimation(stack);
        BusAnimation animation = configured == null ? null : configured.apply(stack, cue);
        if (animation == null) animation = GunAnimations.get(stack, cue, index);
        if (animation != null) HbmAnimations.start(stack.getItem(), animation, hand, index);
    }
}
