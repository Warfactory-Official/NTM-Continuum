package com.hbm.client.render.item;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.7 {@code ItemRenderBoltgun}: gun body and barrel, the barrel kicked back by the {@code RECOIL} bus in first person
 * (the server starts it on a bolted hit or a bolted block, {@code ItemBoltgun} -> {@code ToolAnimations.boltgun}).
 * While the barrel is out the vanilla arm swing is suppressed, as 1.7 cleared {@code isSwingInProgress}.
 */
final class ItemRenderBoltgun implements CustomItemRenderer {

    private static final Identifier TEXTURE = tex("textures/models/weapons/boltgun.png");

    private final WavefrontObj boltgun = obj("models/weapons/boltgun.obj");

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return true;
    }

    @Override
    public boolean animated(ItemStack stack, Type type) {
        return type == Type.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        pose.pushPose();
        switch (type) {
            case EQUIPPED_FIRST_PERSON -> {
                float s0 = 0.15F;
                pose.translate(0.5D, 0.35D, -0.25D);
                pose.mulPose(Axis.ZP.rotationDegrees(15F));
                pose.mulPose(Axis.YP.rotationDegrees(80F));
                pose.scale(s0, s0, s0);

                pose.pushPose();
                double[] anim = HbmAnimations.getRelevantTransformation("RECOIL");
                pose.translate(0D, 0D, -anim[0]);
                if (anim[0] != 0) {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null) player.swinging = false;
                }
                part(pose, sink, boltgun, culled(TEXTURE), "Barrel", light, overlay);
                pose.popPose();
            }
            case EQUIPPED -> {
                float scale = 0.25F;
                pose.scale(scale, scale, scale);
                pose.mulPose(Axis.YP.rotationDegrees(10F));
                pose.mulPose(Axis.ZP.rotationDegrees(10F));
                pose.mulPose(Axis.XP.rotationDegrees(10F));
                pose.translate(1.5D, -0.25D, 1D);
            }
            case ENTITY -> pose.scale(0.1F, 0.1F, 0.1F);
            case INVENTORY -> {
                float s = 1.75F;
                pose.translate(7D, 10D, 0D);
                pose.mulPose(Axis.YP.rotationDegrees(-90F));
                pose.mulPose(Axis.XP.rotationDegrees(-135F));
                pose.scale(s, s, -s);
            }
        }

        part(pose, sink, boltgun, culled(TEXTURE), "Gun", light, overlay);
        if (type != Type.EQUIPPED_FIRST_PERSON) part(pose, sink, boltgun, culled(TEXTURE), "Barrel", light, overlay);
        pose.popPose();
    }
}
