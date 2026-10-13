package com.hbm.client.render.weapon;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

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
    public InteractionHand hand = InteractionHand.MAIN_HAND;
    /** Source shot clock and random roll, copied before a deferred submit callback runs. */
    public long[] lastShot = new long[0];
    public float shotRand;
    public @Nullable LivingEntity owner;
    /**
     * Drawn by the weapon table's preview: 1.7's {@code renderModTable(stack, index)}, which defaults to
     * {@code renderOther(stack, INVENTORY)} (so {@link #context} is {@code GUI}) but is not {@code renderInv}.
     * Declarations whose GUI draw is a 1.7 {@code renderInv} override, or whose 1.7 class overrides
     * {@code renderModTable}, gate on this.
     */
    public boolean modTable;
    /** The configuration the table shows, 1.7's {@code container.index}; meaningful only with {@link #modTable}. */
    public int tableIndex;

    private final Map<String, double[]> buses = new HashMap<>();

    public double[] bus(String name) {
        if (!animated) return REST;
        return buses.computeIfAbsent(name, key -> HbmAnimations.getRelevantTransformation(name, hand, 0));
    }

    public double[] bus(String name, int rail) {
        if (!animated) return REST;
        return buses.computeIfAbsent(name + "#" + rail,
                key -> HbmAnimations.getRelevantTransformation(name, hand, rail));
    }

    void begin(ItemStack stack, boolean animated, int overlay, ItemDisplayContext context,
            @Nullable LivingEntity owner, InteractionHand hand) {
        this.stack = stack;
        this.animated = animated;
        this.overlay = overlay;
        this.context = context;
        this.owner = owner;
        this.hand = hand;
        this.modTable = false;
        this.tableIndex = 0;
        if (stack.getItem() instanceof ItemGunBaseNT gun) {
            this.lastShot = gun.lastShotByHand[hand.ordinal()].clone();
            this.shotRand = (float) gun.shotRandByHand[hand.ordinal()];
        } else {
            this.lastShot = new long[0];
            this.shotRand = 0F;
        }
        buses.clear();
    }
}
