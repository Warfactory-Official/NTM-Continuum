package com.hbm.client.render.item;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.tool.ItemToolAbilityFueled;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.7 {@code ItemRenderChainsaw}: the saw body plus twenty teeth that run round the bar while the saw has fuel
 * and sit still, a sixteenth apart, when it does not. The inventory icon is the model too.
 *
 * <p>The first-person swing is the {@code SWING_ROT}/{@code SWING_TRANS} buses, read here exactly as 1.7 read
 * them. The server starts them from every swing ({@code ItemChainsaw.compatOnEntitySwing} -> {@code ToolAnimations}),
 * and this renderer clears the vanilla arm swing every frame ({@code isSwingInProgress = false} in 1.7) so only the
 * saw's own swing plays.
 */
final class ItemRenderChainsaw implements CustomItemRenderer {

    private static final Identifier TEXTURE = tex("textures/models/weapons/chainsaw.png");

    private final WavefrontObj chainsaw = obj("models/weapons/chainsaw.obj");

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return true;
    }

    @Override
    public boolean animated(ItemStack stack, Type type) {
        return type == Type.EQUIPPED_FIRST_PERSON || running(stack);
    }

    private static boolean running(ItemStack stack) {
        return stack.getItem() instanceof ItemToolAbilityFueled fueled && fueled.canOperate(stack);
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        pose.pushPose();
        switch (type) {
            case EQUIPPED_FIRST_PERSON -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) player.swinging = false;

                float s0 = 0.35F;
                pose.translate(0.5D, 0.25D, -0.25D);
                pose.mulPose(Axis.ZP.rotationDegrees(45F));
                pose.mulPose(Axis.YP.rotationDegrees(80F));
                pose.scale(s0, s0, s0);
                if (owner == null || !owner.isBlocking()) {
                    double[] sRot = HbmAnimations.getRelevantTransformation("SWING_ROT");
                    double[] sTrans = HbmAnimations.getRelevantTransformation("SWING_TRANS");
                    pose.translate(sTrans[0], sTrans[1], sTrans[2]);
                    pose.mulPose(Axis.ZP.rotationDegrees((float) sRot[2]));
                    pose.mulPose(Axis.YP.rotationDegrees((float) sRot[1]));
                    pose.mulPose(Axis.XP.rotationDegrees((float) sRot[0]));
                }
            }
            case EQUIPPED -> {
                float scale = -0.375F;
                pose.scale(scale, scale, scale);
                pose.mulPose(Axis.YP.rotationDegrees(85F));
                pose.mulPose(Axis.XP.rotationDegrees(135F));
                pose.translate(-0.125D, -2.0D, 1.75D);
            }
            case ENTITY -> pose.scale(0.5F, 0.5F, 0.5F);
            case INVENTORY -> {
                float s = 4F;
                pose.translate(8D, 10D, 0D);
                pose.mulPose(Axis.YP.rotationDegrees(-90F));
                pose.mulPose(Axis.XP.rotationDegrees(-135F));
                pose.scale(s, s, -s);
            }
        }

        part(pose, sink, chainsaw, culled(TEXTURE), "Saw", light, overlay);

        boolean running = running(stack);
        for (int i = 0; i < 20; i++) {
            double run = running ? System.currentTimeMillis() % 100D * 0.25D / 100D : 0.0625D;
            double forward = i * 0.25 + run - 2.0625;

            pose.pushPose();
            pose.translate(0D, 0D, 1.9375D);
            pose.translate(0D, 0.375D, 0.5625D);
            double angle = Mth.clamp(forward, 0, 0.25 * Math.PI);
            pose.mulPose(Axis.XP.rotationDegrees((float) (angle * 180D / (Math.PI * 0.25))));
            pose.translate(0D, -0.375D, -0.5625D);
            if (forward < 0) pose.translate(0D, 0D, forward);
            if (forward > Math.PI * 0.25) pose.translate(0D, 0D, forward - Math.PI * 0.25);
            part(pose, sink, chainsaw, culled(TEXTURE), "Tooth", light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }
}
