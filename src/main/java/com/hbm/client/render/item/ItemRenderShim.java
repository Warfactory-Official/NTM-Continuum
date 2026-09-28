package com.hbm.client.render.item;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.7 {@code ItemRenderShim}: the shimmer sledge and axe and the three road signs as OBJs in hand and on the
 * ground; the inventory keeps the sprite. In first person the signs get an extra quarter turn before the
 * shared pose, which is why the case falls through.
 */
final class ItemRenderShim implements CustomItemRenderer {

    private static final Identifier SLEDGE_TEX = tex("textures/models/shimmer_sledge.png");
    private static final Identifier AXE_TEX = tex("textures/models/shimmer_axe.png");
    private static final Identifier STOPSIGN_TEX = tex("textures/models/weapons/stopsign.png");
    private static final Identifier SOPSIGN_TEX = tex("textures/models/weapons/sopsign.png");
    private static final Identifier CHERNOBYLSIGN_TEX = tex("textures/models/weapons/chernobylsign.png");

    private final WavefrontObj sledge = obj("models/shimmer_sledge.obj");
    private final WavefrontObj axe = obj("models/shimmer_axe.obj");
    private final WavefrontObj stopsign = obj("models/weapons/stopsign.obj");

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return type != Type.INVENTORY;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        String id = id(stack);
        boolean sign = id.equals("stopsign") || id.equals("sopsign") || id.equals("chernobylsign");
        boolean shimmer = id.equals("shimmer_sledge") || id.equals("shimmer_axe");
        pose.pushPose();
        if (type == Type.EQUIPPED_FIRST_PERSON && sign) {
            pose.mulPose(Axis.YP.rotationDegrees(180F));
            pose.mulPose(Axis.ZP.rotationDegrees(-90F));
            pose.translate(-1.0F, -1.5F, 0.0F);
        }
        if (shimmer) {
            pose.mulPose(Axis.ZP.rotationDegrees(-135.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(180F));
            pose.scale(1.5F, 1.5F, 1.5F);
            pose.translate(0.45F, -0.3F, 0.0F);
        }
        if (sign) {
            pose.mulPose(Axis.ZP.rotationDegrees(45.0F));
            pose.scale(0.35F, 0.35F, 0.35F);
            pose.translate(2.0F, -2.0F, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(90F));
        }
        switch (id) {
            case "shimmer_sledge" -> all(pose, sink, sledge, culled(SLEDGE_TEX), light, overlay);
            case "shimmer_axe" -> all(pose, sink, axe, culled(AXE_TEX), light, overlay);
            case "stopsign" -> all(pose, sink, stopsign, culled(STOPSIGN_TEX), light, overlay);
            case "sopsign" -> all(pose, sink, stopsign, culled(SOPSIGN_TEX), light, overlay);
            case "chernobylsign" -> all(pose, sink, stopsign, culled(CHERNOBYLSIGN_TEX), light, overlay);
            default -> { }
        }
        pose.popPose();
    }
}
