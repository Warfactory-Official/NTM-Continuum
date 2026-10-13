package com.hbm.client.render.item;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Random;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.7 {@code ItemRenderDetonatorLaser}, an {@code IItemRenderer} of its own that handled every slot, the inventory
 * included. The body is {@code Main} in its skin; then, unlit at lightmap 240/240 with culling off, the {@code Lights} untextured in red, a
 * 32-segment yellow sine strip along the barrel ({@code sin(i * 0.5 - millis / 100) * 0.075}), and three random
 * six-digit red numbers on the display, re-rolled every 500 ms from {@code new Random(millis / 500)} exactly as 1.7
 * seeded it.
 */
final class ItemRenderDetonatorLaser implements CustomItemRenderer {

    private static final Identifier TEXTURE = tex("textures/models/weapons/detonator_laser.png");
    private static final Identifier WHITE = tex("textures/models/misc/white.png");

    private final WavefrontObj model = obj("models/weapons/detonator_laser.obj");

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return true;
    }

    /** The strip and the digits run on the wall clock in every slot. */
    @Override
    public boolean animated(ItemStack stack, Type type) {
        return true;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        pose.pushPose();
        switch (type) {
            case EQUIPPED_FIRST_PERSON -> {
                float s0 = 0.25F;
                pose.scale(s0, s0, s0);
                pose.mulPose(Axis.YP.rotationDegrees(80F));
                pose.mulPose(Axis.XP.rotationDegrees(-20F));
                pose.translate(1.0D, 0.5D, 3.0D);
            }
            case EQUIPPED -> {
                float scale = 0.125F;
                pose.scale(-scale, -scale, -scale);
                pose.mulPose(Axis.YP.rotationDegrees(85F));
                pose.mulPose(Axis.XP.rotationDegrees(145F));
                pose.translate(-0.5D, -1.0D, 6.5D);
            }
            case ENTITY -> {
                float s1 = 0.25F;
                pose.mulPose(Axis.YP.rotationDegrees(-90F));
                pose.scale(s1, s1, s1);
            }
            case INVENTORY -> {
                float s = 3.5F;
                pose.scale(s, s, -s);
                pose.translate(1.5D, 2.75D, 0.0D);
                pose.mulPose(Axis.XP.rotationDegrees(180F));
                pose.mulPose(Axis.YP.rotationDegrees(-90F));
                pose.mulPose(Axis.XP.rotationDegrees(-45F));
            }
        }

        part(pose, sink, model, culled(TEXTURE), "Main", light, overlay);

        // lighting and culling off, lightmap 240/240, texturing off: the lamps in red
        sink.submitCustomGeometry(pose, CompatRenderTypes.flatCutout(WHITE),
                (p, buffer) -> model.renderPart("Lights", p, buffer, FULLBRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFF0000));

        pose.pushPose();
        float px = 0.0625F;
        pose.translate(0.5626D, px * 18D, -px * 14D);
        int sub = 32;
        double width = px * 8;
        double len = width / sub;
        double time = System.currentTimeMillis() / -100D;
        double amplitude = 0.075;
        // The strip lies 0.0001 in front of Main's lens face (x 0.5625): 1.7's depth buffer resolved that, ours do not
        // reliably (26.1 showed a pixel of it, 1.20.1 none), so it is drawn as a depth-biased decal.
        sink.submitCustomGeometry(pose, CompatRenderTypes.flatCutoutDecal(WHITE), (p, buffer) -> {
            for (int i = 0; i < sub; i++) {
                float h0 = (float) (Math.sin(i * 0.5 + time) * amplitude);
                float h1 = (float) (Math.sin((i + 1) * 0.5 + time) * amplitude);
                float z0 = (float) (len * i);
                float z1 = (float) (len * (i + 1));
                beam(buffer, p, -px * 0.25F + h1, z1);
                beam(buffer, p, px * 0.25F + h1, z1);
                beam(buffer, p, px * 0.25F + h0, z0);
                beam(buffer, p, -px * 0.25F + h0, z0);
            }
        });
        pose.popPose();

        // texturing back on, still unlit: the display's numbers
        pose.pushPose();
        Random rand = new Random(System.currentTimeMillis() / 500);
        Font font = Minecraft.getInstance().font;
        float f3 = 0.01F;
        pose.translate(0.5625D, 1.3125D, 0.875D);
        pose.scale(f3, -f3, f3);
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        pose.translate(3D, -2D, 0.2D);
        for (int i = 0; i < 3; i++) {
            String s = (rand.nextInt(900000) + 100000) + "";
            sink.submitText(pose, font, 0F, 0F, Component.literal(s).getVisualOrderText(), false,
                    Font.DisplayMode.NORMAL, FULLBRIGHT, 0xFFFF0000, 0);
            pose.translate(0D, 12.5D, 0D);
        }
        pose.popPose();

        pose.popPose();
    }

    /** One vertex of the strip: opaque 0xFFFF00, untextured, fullbright. */
    private static void beam(VertexConsumer buffer, PoseStack.Pose pose, float y, float z) {
        Vertexes.vertex(buffer, pose, 0F, y, z, 0xFFFFFF00, 0.5F, 0.5F, OverlayTexture.NO_OVERLAY, FULLBRIGHT,
                0F, 1F, 0F); // flatCutout wants an upward normal

    }
}
