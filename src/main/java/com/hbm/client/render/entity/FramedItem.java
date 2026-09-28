package com.hbm.client.render.entity;

import com.hbm.client.render.weapon.WeaponRenderers;
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
 *
 * <p>An item with a Forge {@code IItemRenderer} for {@code ENTITY} never reached either arm: Forge's
 * {@code ForgeHooksClient.renderEntityItem} took it first, and with {@code renderInFrame} ignored (its non-3D
 * branch has no frame case) it drew the renderer's own {@code ENTITY} chain at {@code scale(0.5)}. For a Sedna
 * gun that chain is {@code setupEntity}'s {@code scale(0.125)}, so its model units are 1/16 block. Folding it
 * into the sprite arm, as this class once did, drew the gun at {@code NONE} - its raw model, with no
 * {@code setupEntity} at all - eight times too large on the pedestal and the skeleton holder. The
 * {@link Arm#CUSTOM} arm resolves the stack at {@link ItemDisplayContext#GROUND} instead, whose frame is exactly
 * that Forge chain (see {@code Pose17.Klass.ITEM_RENDER_WEAPON}), and only restates the placement.
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
    // Pose17 ITEM_RENDER_WEAPON GROUND: translate(0.5, 0.25, 0.5) after vanilla's translate(-0.5, -0.5, -0.5)
    static final float GROUND_FRAME_DROP = 0.25F;

    private FramedItem() {
    }

    /** Which of 1.7's routes a framed stack takes. */
    public enum Arm {
        /** {@code RenderItem.doRender}'s 3D-block arm. */
        BLOCK,
        /** {@code RenderItem.doRender}'s flat-icon arm. */
        SPRITE,
        /** {@code ForgeHooksClient.renderEntityItem}: an {@code IItemRenderer} item such as a Sedna gun. */
        CUSTOM;

        public boolean block() {
            return this == BLOCK;
        }
    }

    /** Resolves {@code out} for {@code stack} at the neutral display and answers whether it takes the block arm. */
    public static boolean resolve(CompatItemRenderState out, ItemStack stack, @Nullable Level level) {
        return resolveArm(out, stack, level) == Arm.BLOCK;
    }

    /**
     * Resolves {@code out} for {@code stack} and answers its arm: a weapon at {@link ItemDisplayContext#GROUND}
     * (Forge's {@code ENTITY} route, see the class comment), anything else at the neutral display.
     */
    public static Arm resolveArm(CompatItemRenderState out, ItemStack stack, @Nullable Level level) {
        // G-C: any 1.7 IItemRenderer that handled ENTITY took Forge's route, not only the Sedna guns
        if (WeaponRenderers.handles(stack) || EntityItemRenderers.handles(stack)) {
            out.extract(level, stack, ItemDisplayContext.GROUND);
            return Arm.CUSTOM;
        }
        out.extract(level, stack, ItemDisplayContext.NONE);
        return out.isGui3d() ? Arm.BLOCK : Arm.SPRITE;
    }

    /**
     * The chain past the caller's own ops for a stack that is not a 3D block: the flat-icon arm, or for an
     * {@link Arm#CUSTOM} stack Forge's route.
     */
    public static void framedFlatArm(PoseStack poseStack, Arm arm) {
        if (arm == Arm.CUSTOM) {
            framedCustomPrefix(poseStack);
        } else {
            framedSpritePrefix(poseStack);
            framedSpriteAnchor(poseStack);
        }
    }

    /**
     * 1.7 RenderItem.doRender's {@code translate(0, f2, 0)} and ForgeHooksClient.renderEntityItem's
     * {@code rotate(f3, Y)} (0 at age 0, hoverStart 0) ahead of a {@link ItemDisplayContext#GROUND} stack. The
     * GROUND frame carries Forge's {@code scale(0.5)} and the renderer's {@code setupEntity}, but rides on
     * vanilla's display origin: {@code translate(-0.5)} after {@code Pose17}'s {@code translate(0.5, 0.25, 0.5)}
     * leaves it {@link #GROUND_FRAME_DROP} below the caller's origin, which is put back here.
     */
    public static void framedCustomPrefix(PoseStack poseStack) {
        poseStack.translate(0.0, FRAMED_BOB + GROUND_FRAME_DROP, 0.0);
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
