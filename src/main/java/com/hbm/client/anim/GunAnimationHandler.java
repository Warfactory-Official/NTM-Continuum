package com.hbm.client.anim;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.packet.GunAnimationPayload;
import net.minecraft.client.Minecraft;
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

        GunAnimation cue = GunAnimation.byOrdinal(payload.animation());
        int index = payload.configIndex();

        // The shot timestamp drives muzzle flash and smoke, and must be stamped even when the gun has
        // no animation table at all.
        if (cue == GunAnimation.CYCLE && index >= 0 && index < gun.lastShot.length) {
            gun.lastShot[index] = System.currentTimeMillis();
        }

        BusAnimation animation = GunAnimations.get(stack.getItem(), cue);
        if (animation != null) HbmAnimations.start(stack.getItem(), animation, index);
    }
}
