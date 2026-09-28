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
 * 1.7 {@code ItemRenderRedstoneSword}: the Techne {@code ModelSword} in hand and on the ground, the sprite in the
 * inventory. As with {@code ModelBigSword}, the export's {@code mirror} is set after {@code addBox} and never
 * applied.
 */
final class ItemRenderRedstoneSword implements CustomItemRenderer {

    private static final Identifier TEXTURE = tex("textures/models/modelswordredstone.png");

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
        pose.translate(-0.8F, 0.4F, -0.1F);
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
        root.addOrReplaceChild("grip_bottom", CubeListBuilder.create().texOffs(0, 17).addBox(0F, 0F, 0F, 3, 3, 1),
                PartPose.ZERO);
        root.addOrReplaceChild("grip_handle", CubeListBuilder.create().texOffs(8, 2).addBox(0F, 0F, 0F, 2, 5, 1),
                PartPose.offset(0.5F, -5F, 0F));
        root.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(14, 5).addBox(0F, 0F, 0F, 6, 1, 3),
                PartPose.offset(-1.5F, -6F, -1F));
        root.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 3, 16, 1),
                PartPose.offset(0F, -22F, 0F));
        root.addOrReplaceChild("blade_tip", CubeListBuilder.create().texOffs(8, 0).addBox(0F, 0F, 0F, 2, 1, 1),
                PartPose.offset(0.5F, -23F, 0F));
        root.addOrReplaceChild("shield1", CubeListBuilder.create().texOffs(14, 0).addBox(0F, 0F, 0F, 1, 1, 4),
                PartPose.offset(-2F, -6.5F, -1.5F));
        root.addOrReplaceChild("shield2", CubeListBuilder.create().texOffs(24, 0).addBox(0F, 0F, 0F, 1, 1, 4),
                PartPose.offset(4F, -6.5F, -1.5F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
