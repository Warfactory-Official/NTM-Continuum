package com.hbm.client.render.weapon;

import com.hbm.client.anim.HbmAnimations;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * What a weapon's declaration reads.
 *
 * <p>The buses belong to the <em>held</em> item, so every other draw reads {@link #REST} instead; an
 * icon that read them live would replay whatever the player's hands happen to be doing.
 */
public final class WeaponPose {

    private static final double[] REST = {0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 2};

    public ItemStack stack = ItemStack.EMPTY;
    public boolean animated;
    public int overlay = OverlayTexture.NO_OVERLAY;
    public ItemDisplayContext context = ItemDisplayContext.NONE;

    private final Map<String, double[]> buses = new HashMap<>();

    public double[] bus(String name) {
        if (!animated) return REST;
        return buses.computeIfAbsent(name, HbmAnimations::getRelevantTransformation);
    }

    public double[] bus(String name, int rail) {
        if (!animated) return REST;
        return buses.computeIfAbsent(name + "#" + rail,
                key -> HbmAnimations.getRelevantTransformation(name, rail));
    }

    void begin(ItemStack stack, boolean animated, int overlay, ItemDisplayContext context) {
        this.stack = stack;
        this.animated = animated;
        this.overlay = overlay;
        this.context = context;
        buses.clear();
    }
}
