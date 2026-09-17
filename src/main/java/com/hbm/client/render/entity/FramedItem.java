package com.hbm.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 1.7's framed {@code EntityItem} route - {@code RenderItem.doRender} under {@code renderInFrame} - shared by the
 * renderers that draw a stack riding a belt or a cart.
 *
 * <p>1.7 took one of two arms for a framed stack: a 3D block was drawn as the block, anything else as its flat
 * sprite. Each arm carries its own scale chain inside {@code RenderItem} rather than in the display, so the stack is
 * resolved for {@link ItemDisplayContext#NONE} - the neutral display - and the frame factor is restated here.
 * ntm-next's third arm, a bespoke special-renderer item such as a gun, is folded into the sprite arm: the port's
 * item renderers answer {@code NONE} with a mesh already, so the frame scale is the only difference.
 *
 * <p>The mini-stack copies 1.7 jittered about the pose for larger counts are not drawn; one copy stands for the
 * stack.
 */
public final class FramedItem {

    static final float FRAMED_SPRITE_SCALE = 0.5128205F;
    // 1.7: doRender:69 - f2 at age 0, hoverStart 0
    static final float FRAMED_BOB = 0.1F;
    // 1.7: doRender:118 f9, divided back out
    private static final float F9 = 0.25F;
    private static final float SPRITE_DEPTH = 0.0625F;
    private static final float SPRITE_ANCHOR_Y = 0.25F;
    private static final float SPRITE_ANCHOR_Z = -SPRITE_DEPTH / 2.0F;

    private FramedItem() {
    }

    /** Resolves {@code out} for {@code stack} at the neutral display and answers whether it takes the block arm. */
    public static boolean resolve(CompatItemRenderState out, ItemStack stack, @Nullable Level level) {
        out.extract(level, stack, ItemDisplayContext.NONE);
        return out.isGui3d();
    }

    /** 1.7 RenderItem.doRender:106-123 - the framed 3D-block arm's own chain, past the caller's ops. */
    public static void framedBlockPrefix(PoseStack poseStack) {
        poseStack.translate(0.0, FRAMED_BOB, 0.0);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        poseStack.translate(0.0, 0.05F, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        // f9 = 0.25; renderItemIn3d admits no 0.5
        poseStack.scale(F9, F9, F9);
    }

    /** 1.7 RenderItem.doRender:158-169 - the framed flat-icon arm's own chain, past the caller's ops. */
    public static void framedSpritePrefix(PoseStack poseStack) {
        poseStack.translate(0.0, FRAMED_BOB, 0.0);
        poseStack.scale(FRAMED_SPRITE_SCALE, FRAMED_SPRITE_SCALE, FRAMED_SPRITE_SCALE);
        poseStack.translate(0.0, -0.05, 0.0);
    }

    /**
     * 1.7 RenderItem.renderDroppedItem:282, :314 - the fancy framed turn, and the sheet anchored at -f8 by its
     * FRONT face where NONE centres it.
     */
    public static void framedSpriteAnchor(PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, SPRITE_ANCHOR_Y, SPRITE_ANCHOR_Z);
    }

    /** Draws a resolved stack once at the current pose. */
    public static void draw(PoseStack poseStack, CompatItemRenderState item, SubmitSink sink, int light) {
        sink.submitItem(poseStack, item, light, OverlayTexture.NO_OVERLAY, 0);
    }
}
