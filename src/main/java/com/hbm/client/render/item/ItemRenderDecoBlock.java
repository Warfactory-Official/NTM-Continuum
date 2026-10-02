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
 * 1.7 {@code ItemRenderDecoBlock}, registered for {@code steel_roof} only. It declined {@code INVENTORY}, so the slot
 * shows the block's flat {@code steel_roof} icon; in hand and on the ground it drew {@code ModelSteelRoof} at the
 * ModelBase scale {@code 0.0625}. The port's {@code steel_roof.obj} is that model in its ModelBase pixel space (the
 * placed roof draws it the same way, see {@code DecoObjRenderer}).
 */
final class ItemRenderDecoBlock implements CustomItemRenderer {

    private static final Identifier TEXTURE = tex("textures/models/deco/steelroof.png");

    private final WavefrontObj roof = obj("models/deco/steel_roof.obj");

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return type != Type.INVENTORY;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        pose.pushPose();
        switch (type) {
            case ENTITY -> {
                pose.scale(0.5F, 0.5F, 0.5F);
                pose.mulPose(Axis.XP.rotationDegrees(180F));
                pose.translate(0D, -1D, 0D);
            }
            case EQUIPPED -> {
                pose.scale(0.5F, 0.5F, 0.5F);
                pose.translate(0.8D, -0.3D, 0.2D);
            }
            case EQUIPPED_FIRST_PERSON -> {
                pose.mulPose(Axis.ZP.rotationDegrees(-135F));
                pose.translate(-0.6D, -0.6D, -0.1D);
                pose.scale(0.5F, 0.5F, 0.5F);
            }
            default -> { }
        }
        float s = 0.0625F;
        pose.scale(s, s, s);
        all(pose, sink, roof, culled(TEXTURE), light, overlay);
        pose.popPose();
    }
}
