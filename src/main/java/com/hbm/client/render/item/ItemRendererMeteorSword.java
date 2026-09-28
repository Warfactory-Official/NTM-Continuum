package com.hbm.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.7 {@code ItemRendererMeteorSword}: every tier past the plain sword is the one sword sprite with a glint in the
 * tier's colour, always on, in every slot but a dropped or framed item ({@code ENTITY}, which 1.7 left to vanilla).
 *
 * <p>Both 1.7 glints are reproduced pass for pass over HBM's own {@code misc/glint.png}. The colour was
 * {@code glColor4f(r * 0.36, g * 0.36, b * 0.36)} over that sheet, so each tier has the sheet pre-multiplied by it
 * ({@code misc/glint_meteorite_<tier>.png}); {@code GlintRenderType} says why a glint's colour has to be its
 * texture. The inventory glint is {@code renderGlintFlat}: two quads over the slot, blended {@code DST_ALPHA, ONE},
 * UVs scrolled on the CPU with periods of 3000 and 4873 ms. The held glint is {@code renderGlint3D}: two passes over
 * the sprite's faces, blended {@code SRC_COLOR, ONE}, through the texture matrices {@code S(1/8) T(t, 0) R(-50)} and
 * {@code S(1/8) T(-t', 0) R(10)}. Both test depth {@code EQUAL}, so they lie only on the sprite; the 1.7 3D pass
 * also drew a 255-slice edge that almost never matched a depth, which is left out.
 */
final class ItemRendererMeteorSword implements CustomItemRenderer {

    /** The front and back faces of an {@code item/generated} sprite. */
    private static final float FRONT = 8.5F / 16F;
    private static final float BACK = 7.5F / 16F;

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return type != Type.ENTITY && tint(stack) != null;
    }

    @Override
    public boolean animated(ItemStack stack, Type type) {
        return true;
    }

    /** {@code meteorite_sword_<tier>} to the tier's sheet; the untiered sword had no renderer in 1.7. */
    private static @Nullable Identifier tint(ItemStack stack) {
        String id = id(stack);
        if (!id.startsWith("meteorite_sword_")) return null;
        return tex("textures/misc/glint_meteorite_" + id.substring("meteorite_sword_".length()) + ".png");
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        Identifier sheet = tint(stack);
        if (sheet == null) return;
        // the sword itself, then the glint on it (see Core ItemSprites for why the sprite is drawn here)
        dev.golem.core.render.ItemSprites.submit(pose, sink, tex("textures/items/meteorite_sword.png"), light, overlay);
        long now = System.currentTimeMillis();
        // after the sprite: an EQUAL depth test needs the surface already drawn
        SubmitSink after = sink.order(1);
        if (type == Type.INVENTORY) {
            after.submitCustomGeometry(pose, CompatRenderTypes.glint(sheet, true), (p, buffer) -> {
                for (int j = 0; j < 2; j++) flatPass(p, buffer, now, j);
            });
        } else {
            after.submitCustomGeometry(pose, CompatRenderTypes.glint(sheet, false), (p, buffer) -> {
                float offset = (float) (now % 3000L) / 3000.0F * 8.0F;
                pass3D(p, buffer, texture(offset, -50F));
                offset = (float) (now % 4873L) / 4873.0F * 8.0F;
                pass3D(p, buffer, texture(-offset, 10F));
            });
        }
    }

    /**
     * One {@code renderGlintFlat} quad. 1.7 drew it in GUI pixels, y down, over the sixteen-pixel slot; the sprite's
     * face here spans the unit square, y up, so pixel {@code (x, y)} is {@code (x / 16, 1 - y / 16)}.
     */
    private static void flatPass(PoseStack.Pose pose, VertexConsumer buffer, long now, int j) {
        float scaleU = 0.00390625F;
        float scaleV = 0.00390625F;
        float anim = (float) (now % (long) (3000 + j * 1873)) / (3000.0F + (float) (j * 1873)) * 256.0F;
        float offsetV = 0.0F;
        float sizeMultU = j == 1 ? -1.0F : 4.0F;
        int sizeX = 16;
        int sizeY = 16;
        flat(pose, buffer, 0, sizeY, (anim + sizeY * sizeMultU) * scaleU, (offsetV + sizeY) * scaleV);
        flat(pose, buffer, sizeX, sizeY, (anim + sizeX + sizeY * sizeMultU) * scaleU, (offsetV + sizeY) * scaleV);
        flat(pose, buffer, sizeX, 0, (anim + sizeX) * scaleU, (offsetV + 0.0F) * scaleV);
        flat(pose, buffer, 0, 0, (anim + 0.0F) * scaleU, (offsetV + 0.0F) * scaleV);
    }

    private static void flat(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float u, float v) {
        Vertexes.positionTex(buffer, pose, x / 16F, 1F - y / 16F, FRONT, u, v);
    }

    /** 1.7's texture matrix {@code glScalef(1/8) glTranslatef(offset, 0, 0) glRotatef(angle, 0, 0, 1)}. */
    private static Matrix3f texture(float offset, float angle) {
        float scale = 0.125F;
        // column-major: the third column carries the 2D translation of a homogeneous (u, v, 1)
        Matrix3f translate = new Matrix3f(1F, 0F, 0F, 0F, 1F, 0F, offset, 0F, 1F);
        return new Matrix3f().scaling(scale, scale, 1F).mul(translate).rotateZ((float) Math.toRadians(angle));
    }

    /** {@code renderItemIn2D(0, 0, 1, 1)}'s two faces: {@code u = x}, {@code v = 1 - y}, before the matrix. */
    private static void pass3D(PoseStack.Pose pose, VertexConsumer buffer, Matrix3f texture) {
        // corner for corner in ItemSprites' order, so the EQUAL test compares identical triangles
        corner(pose, buffer, texture, 0F, 0F, FRONT);
        corner(pose, buffer, texture, 1F, 0F, FRONT);
        corner(pose, buffer, texture, 1F, 1F, FRONT);
        corner(pose, buffer, texture, 0F, 1F, FRONT);
        corner(pose, buffer, texture, 0F, 1F, BACK);
        corner(pose, buffer, texture, 1F, 1F, BACK);
        corner(pose, buffer, texture, 1F, 0F, BACK);
        corner(pose, buffer, texture, 0F, 0F, BACK);
    }

    private static void corner(PoseStack.Pose pose, VertexConsumer buffer, Matrix3f texture,
                               float x, float y, float z) {
        Vector3f uv = texture.transform(new Vector3f(x, 1F - y, 1F));
        Vertexes.positionTex(buffer, pose, x, y, z, uv.x, uv.y);
    }
}
