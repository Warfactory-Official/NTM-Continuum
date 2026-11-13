package com.hbm.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.Poses;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * A 1.7 {@code IItemRenderer}, ported as a body drawn inside one of {@code Pose17}'s frames.
 *
 * <p>1.7 asked the renderer twice: {@code handleRenderType} decided whether it drew a given stack in a given
 * slot at all, and {@code renderItem} drew it. Both halves survive as they were. A slot the renderer declines
 * is drawn from the item's own flat model, which is how the held weapons are sprites in the inventory and
 * models in the hand. The draw receives the pose the engine had set up for that slot, so the body is the
 * renderer's own GL calls transcribed op for op; the slot-to-context mapping is {@link Type#of}.
 *
 * <p>Who calls this differs between eras and is not the renderer's concern: it rides the OBJ machine items' split.
 * 26.1 goes through {@code MachineItemModelRenderer.CustomItem} (an item model, because only an item model is told
 * the display context), older versions through {@code MachineItemModels.renderCustom} from the loader's BEWLR hook.
 */
public interface CustomItemRenderer {

    /** 1.7's {@code ItemRenderType}, the four the item renderers here handled. */
    enum Type {
        INVENTORY, EQUIPPED, EQUIPPED_FIRST_PERSON, ENTITY;

        /**
         * The modern display slot as 1.7 would have reached the renderer. Third person and the head were both
         * {@code EQUIPPED}; a dropped item and an item frame were both {@code ENTITY} (1.7 drew a framed item
         * through {@code RenderItem} like a dropped one).
         */
        public static Type of(ItemDisplayContext context) {
            return switch (context) {
                case GUI -> INVENTORY;
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> EQUIPPED_FIRST_PERSON;
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD -> EQUIPPED;
                default -> ENTITY;
            };
        }
    }

    /** 1.7 {@code handleRenderType}. A declined slot shows the item's flat model instead. */
    boolean handles(ItemStack stack, Type type);

    /** 1.7 {@code renderItem}, in the frame's space. The pose must be left as it was found. */
    void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                @Nullable LivingEntity owner);

    /**
     * {@link #render}, from a caller that knows the vanilla display slot. 1.7 had one first-person type for both
     * hands; a renderer that must tell the left hand from the right (the crucible draws only the main hand) overrides
     * this. Everything else draws the same in both.
     */
    default void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, SubmitSink sink, int light,
                        int overlay, @Nullable LivingEntity owner) {
        render(stack, Type.of(context), pose, sink, light, overlay, owner);
    }

    /** The frame of 1.7's full-3D held weapons, whose left hands are vanilla handheld's (see {@link #renderInFrame}). */
    String FULL3D_FRAME = "weapon_full3d";

    /**
     * {@link #render} in its frame's slot. 1.7 had no off hand: the full-3D frame's left hands are vanilla handheld's,
     * the right hand mirrored with the body turned about the plane {@code z = 1/2}. That fits a sprite; a held body is
     * turned about the principal plane it is most nearly symmetric about instead ({@link HeldMirror}), so the off hand
     * shows the main hand's mirror image.
     */
    static void renderInFrame(CustomItemRenderer renderer, boolean full3d, ItemStack stack, ItemDisplayContext context,
                              PoseStack pose, SubmitSink sink, int light, int overlay, @Nullable LivingEntity owner) {
        Matrix4f turn = full3d ? HeldMirror.of(renderer, stack, context, pose.last(), light, overlay, owner) : null;
        if (turn == null) {
            renderer.render(stack, context, pose, sink, light, overlay, owner);
            return;
        }
        pose.pushPose();
        Poses.mulPose(pose, turn);
        renderer.render(stack, context, pose, sink, light, overlay, owner);
        pose.popPose();
    }

    /**
     * Whether the off hand draws this body as the main hand's exact mirror image; the renderer then draws its left
     * hands unculled. For a body with no mirror plane, which {@link HeldMirror} cannot turn onto itself.
     */
    default boolean mirrorsExactly() {
        return false;
    }

    /** Whether the drawing changes with time, so 26.1 must not serve a cached icon. */
    default boolean animated(ItemStack stack, Type type) {
        return false;
    }
}
