package com.hbm.client.anim;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The client's running animations, and where a renderer asks what a bus is doing right now.
 *
 * <p>Ported from 1.12.2 {@code HbmAnimationsSedna}, including the reason it is shaped this way. Flan's
 * Mod and its descendants keep a single animation timer per client, so reloading and then switching
 * weapons replays the reload on the <em>new</em> weapon. Upstream's fix is one timer per hotbar slot —
 * nine slots × eight parallel rails — and a key check: if the item in the slot is not the item the
 * animation was started for, the animation simply does not apply. You can still confuse it by moving a
 * weapon between slots mid-animation, which cancels it rather than misplaying it.
 *
 * <p>Client-only, and single-threaded by construction (render thread).
 */
public final class HbmAnimations {

    /** [hotbar slot][rail]. Eight rails so a gun's several configs can animate independently. */
    private static final Animation[][] HOTBAR = new Animation[9][8];

    /** What a bus looks like when nothing is playing: no movement, no rotation, unit scale, XYZ order. */
    private static final double[] IDENTITY = {
            0, 0, 0,
            0, 0, 0,
            1, 1, 1,
            0, 0, 0,
            0, 1, 2,
    };

    private HbmAnimations() {}

    public static class Animation {
        /** The item this animation belongs to; a mismatch cancels it. */
        public final Item key;
        public final long startMillis;
        public final BusAnimation animation;
        /** Stay on the last frame instead of ending — for poses that persist until replaced. */
        public final boolean holdLastFrame;

        public Animation(Item key, long startMillis, BusAnimation animation, boolean holdLastFrame) {
            this.key = key;
            this.startMillis = startMillis;
            this.animation = animation;
            this.holdLastFrame = holdLastFrame;
        }
    }

    /** Starts {@code animation} on the held slot's given rail, replacing whatever was playing there. */
    public static void start(Item item, BusAnimation animation, int rail) {
        Player player = Minecraft.getInstance().player;
        if (player == null || animation == null) return;
        int slot = slotOf(player);
        if (rail < 0 || rail >= HOTBAR[slot].length) return;
        HOTBAR[slot][rail] = new Animation(item, System.currentTimeMillis(), animation, false);
    }

    /** The animation currently applying to the held item on {@code rail}, or null. */
    public static Animation getRelevantAnim(int rail) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return null;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return null;

        int slot = slotOf(player);
        if (rail < 0 || rail >= HOTBAR[slot].length) return null;

        Animation animation = HOTBAR[slot][rail];
        if (animation == null) return null;
        // The slot's animation belongs to a different item — the player swapped. Do not replay it.
        return animation.key == stack.getItem() ? animation : null;
    }

    public static double[] getRelevantTransformation(String bus) {
        return getRelevantTransformation(bus, 0);
    }

    /**
     * The 15-slot transform for {@code bus} right now, or the identity when nothing is playing.
     *
     * <p>Never returns null, so a renderer can read every bus unconditionally — a model with a bus the
     * current animation does not mention simply stays at rest.
     */
    public static double[] getRelevantTransformation(String bus, int rail) {
        Animation animation = getRelevantAnim(rail);
        if (animation != null) {
            int millis = (int) (System.currentTimeMillis() - animation.startMillis);
            double[] transform = animation.animation.getTimedTransformation(bus, millis);
            if (transform != null) return transform;
        }
        return IDENTITY.clone();
    }

    /** Clears every rail — on world change, so a stale animation cannot leak into a new session. */
    public static void clear() {
        for (Animation[] rails : HOTBAR) java.util.Arrays.fill(rails, null);
    }

    /** The player's selected hotbar slot, folded into range for non-vanilla hotbar sizes. */
    private static int slotOf(Player player) {
        int slot = dev.golem.core.world.Compat.selectedSlot(player);
        return slot >= 0 && slot < 9 ? slot : Math.abs(slot) % 9;
    }
}
