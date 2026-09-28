package com.hbm.client.anim;

import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ToolAnimation;
import com.hbm.items.tool.ItemBoltgun;
import com.hbm.items.tool.ItemChainsaw;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.packet.ToolAnimationPayload;
import com.hbm.util.GameTime;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Random;

/**
 * The client half of 1.7 {@code HbmAnimationPacket.handleItem} and the {@code getAnimation} tables of the
 * {@code IAnimatedItem} tools: chainsaw ({@code ItemChainsaw}), crucible ({@code ItemCrucible}) and boltgun
 * ({@code ItemBoltgun}). Each table is transcribed from its item; the renderers read the buses
 * ({@code SWING_ROT}/{@code SWING_TRANS}, {@code GUARD_ROT}, {@code RECOIL}).
 */
public final class ToolAnimations {

    /** 1.7 {@code Item.itemRand}, which the crucible drew its swing offset and pitch from. */
    private static final Random ITEM_RAND = new Random();

    private ToolAnimations() {}

    public static void handle(ToolAnimationPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        // 1.7 player.getHeldItem(): the main hand only
        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(payload.itemId())) return;
        if (payload.animation() < 0 || payload.animation() >= ToolAnimation.VALUES.length) return;
        BusAnimation animation = get(stack, ToolAnimation.VALUES[payload.animation()]);
        if (animation != null) HbmAnimations.start(stack.getItem(), animation, InteractionHand.MAIN_HAND, 0);
    }

    static @Nullable BusAnimation get(ItemStack stack, ToolAnimation type) {
        if (stack.getItem() instanceof ItemChainsaw) return chainsaw();
        if (stack.getItem() instanceof ItemCrucible) return crucible(type);
        if (stack.getItem() instanceof ItemBoltgun) return boltgun();
        return null;
    }

    /** 1.7 {@code ItemChainsaw.getAnimation}: a fresh swing, or one continued from wherever the last swing is. */
    private static @Nullable BusAnimation chainsaw() {
        int forward = 150;
        int sideways = 100;
        int retire = 200;

        HbmAnimations.Animation current = HbmAnimations.getRelevantAnim(0);
        if (current == null) {
            return new BusAnimation()
                    .addBus("SWING_ROT", new BusAnimationSequence()
                            .addPos(0, 0, 90, forward)
                            .addPos(45, 0, 90, sideways)
                            .addPos(0, 0, 0, retire))
                    .addBus("SWING_TRANS", new BusAnimationSequence()
                            .addPos(0, 0, 3, forward)
                            .addPos(2, 0, 2, sideways)
                            .addPos(0, 0, 0, retire));
        }

        double[] rot = HbmAnimations.getRelevantTransformation("SWING_ROT");
        double[] trans = HbmAnimations.getRelevantTransformation("SWING_TRANS");

        if (GameTime.now() - current.startMillis < 50) return null;

        return new BusAnimation()
                .addBus("SWING_ROT", new BusAnimationSequence()
                        .addPos(rot[0], rot[1], rot[2], 0)
                        .addPos(0, 0, 90, forward)
                        .addPos(45, 0, 90, sideways)
                        .addPos(0, 0, 0, retire))
                .addBus("SWING_TRANS", new BusAnimationSequence()
                        .addPos(trans[0], trans[1], trans[2], 0)
                        .addPos(0, 0, 3, forward)
                        .addPos(2, 0, 2, sideways)
                        .addPos(0, 0, 0, retire));
    }

    /** 1.7 {@code ItemCrucible.getAnimation}: the deploy (guards folded 800 ms, then open) and a random swing. */
    private static @Nullable BusAnimation crucible(ToolAnimation type) {
        if (type == ToolAnimation.EQUIP) {
            return new BusAnimation()
                    .addBus("GUARD_ROT", new BusAnimationSequence()
                            .addPos(90, 0, 1, 0)
                            .addPos(90, 0, 1, 800)
                            .addPos(0, 0, 1, 50));
        }

        if (type == ToolAnimation.SWING && HbmAnimations.getRelevantTransformation("SWING_ROT")[0] == 0) {
            int offset = ITEM_RAND.nextInt(80) - 20;
            // 1.7 playSwing: client-side, not positioned, pitch 0.8..1.0
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    ModAbilityToolItems.CRUCIBLE_SWING.get(), 0.8F + ITEM_RAND.nextFloat() * 0.2F));
            return new BusAnimation()
                    .addBus("SWING_ROT", new BusAnimationSequence()
                            .addPos(90 - offset, 90 - offset, 35, 75)
                            .addPos(90 + offset, 90 - offset, -45, 150)
                            .addPos(0, 0, 0, 500))
                    .addBus("SWING_TRANS", new BusAnimationSequence()
                            .addPos(-3, 0, 0, 75)
                            .addPos(8, 0, 0, 150)
                            .addPos(0, 0, 0, 500));
        }
        return null;
    }

    /** 1.7 {@code ItemBoltgun.getAnimation}: the barrel's recoil, whatever the cue. */
    private static BusAnimation boltgun() {
        return new BusAnimation()
                .addBus("RECOIL", new BusAnimationSequence()
                        .addPos(1, 0, 1, 50)
                        .addPos(0, 0, 1, 100));
    }
}
