package com.hbm.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.7 {@code ItemRenderBigSword}: the Techne {@code ModelBigSword} in hand and on the ground, the sprite in the
 * inventory.
 *
 * <p>The Techne export sets {@code setTextureSize(64, 32)} and {@code mirror} on every box, but only after
 * {@code addBox}, and a 1.7 {@code ModelBox} reads both when it is built. So neither ever applied: the UVs are
 * laid out on the model's own 128x32 sheet and no box is mirrored.
 */
final class ItemRenderBigSword implements CustomItemRenderer {

    private static final Identifier TEXTURE = tex("textures/models/modelbigswordtexture.png");

    private final ModelPart model = createModel().bakeRoot();

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return type != Type.INVENTORY;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(-135.0F));
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.translate(0.0F, 0.4F, -0.7F);
        sink.submitCustomGeometry(pose, culled(TEXTURE), (p, buffer) -> {
            PoseStack local = new PoseStack();
            local.last().pose().set(p.pose());
            local.last().normal().set(p.normal());
            model.render(local, buffer, light, overlay);
        });
        pose.popPose();
    }

    private static LayerDefinition createModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("handle_bottom", CubeListBuilder.create().texOffs(1, 1).addBox(0F, 0F, 0F, 1, 3, 3),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.7853982F, 0F, 0F));
        root.addOrReplaceChild("handle_grip", CubeListBuilder.create().texOffs(17, 1).addBox(0F, 0F, 0F, 1, 5, 2),
                PartPose.offset(0F, -4F, -1F));
        root.addOrReplaceChild("handle1", CubeListBuilder.create().texOffs(25, 1).addBox(0F, -1F, 0F, 2, 1, 4),
                PartPose.offsetAndRotation(-0.5F, -3F, 0F, 0.2617994F, 0F, 0F));
        root.addOrReplaceChild("handle2", CubeListBuilder.create().texOffs(41, 1).addBox(0F, -1F, -4F, 2, 1, 4),
                PartPose.offsetAndRotation(-0.5F, -3F, 0F, -0.2617994F, 0F, 0F));
        root.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(57, 1).addBox(0F, 0F, 0F, 3, 18, 1),
                PartPose.offsetAndRotation(0F, -22F, 1.5F, 0F, 1.570796F, 0F));
        root.addOrReplaceChild("blade_tip", CubeListBuilder.create().texOffs(2, 10).addBox(0F, 0F, 0F, 1, 2, 2),
                PartPose.offsetAndRotation(0F, -23.5F, 0F, -0.7853982F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 32);
    }
}
