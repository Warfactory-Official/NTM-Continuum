package com.hbm.client.anim;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.packet.GunAnimationPayload;
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
        if (minecraft.player == null) return;

        ItemStack stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return;

        String heldItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (payload.itemId() == null || !heldItemId.equals(payload.itemId())) return;

        if (payload.animation() < 0 || payload.animation() >= GunAnimation.VALUES.length) return;

        GunAnimation cue = GunAnimation.byOrdinal(payload.animation());
        int index = payload.configIndex();
        if (!gun.isValidConfigIndex(index)) return;
        GunConfig config = gun.getConfig(stack, index);
        Receiver[] receivers = config.getReceivers(stack);
        if (receivers == null) return;

        // The shot timestamp drives muzzle flash and smoke, and must be stamped even when the gun has
        // no animation table at all.
        if (cue == GunAnimation.CYCLE && index < gun.lastShot.length) {
            int receiverIndex = payload.receiver();
            if (receiverIndex < 0 || receiverIndex >= receivers.length) return;
            gun.lastShot[index] = System.currentTimeMillis();
            java.util.function.BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> recoil =
                    receivers[receiverIndex].getRecoil(stack);
            if (recoil != null) recoil.accept(stack,
                    new ItemGunBaseNT.LambdaContext(config, minecraft.player,
                            minecraft.player.getInventory(), index));
        }

        BusAnimation animation = GunAnimations.get(stack, cue, index);
        if (animation != null) HbmAnimations.start(stack.getItem(), animation, index);
    }
}
