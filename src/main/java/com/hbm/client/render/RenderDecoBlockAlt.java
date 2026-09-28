package com.hbm.client.render;

import com.hbm.blocks.generic.DecoBlockAlt;
import com.hbm.items.ModItems;
import com.hbm.tileentity.deco.TileEntityDecoBlockAltF;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.ModelParts;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderDecoBlockAlt} for {@code statue_elb_f} ({@code TileEntityDecoBlockAltF}): the Techne
 * {@code ModelStatue}, a watch held "in frame" in front of it, and the {@code ModelGun} at its side.
 *
 * <p>The GL sequence is replayed literally: up 1.5 blocks and flipped about Z (Techne models are authored upside
 * down), turned by the facing (metadata 4/2/5/3 give 90/180/270/0), then the watch at (0, -2/16, 2/16 + 1/48)
 * flipped back, and the gun half-size further along. 1.7's {@code RenderItem.renderInFrame} scaled the item by
 * 0.5128205 and dropped it 0.05; the modern fixed (item frame) display context stands in for the flat framed icon.
 * {@code setTextureSize(64, 32)} on some gun boxes came after {@code addBox} and changed nothing: all UVs are 64x64.
 */
public final class RenderDecoBlockAlt extends GemBER<TileEntityDecoBlockAltF, RenderDecoBlockAlt.State> {

    public static final class State extends CompatRenderState {
        public float yaw;
        public final CompatItemRenderState watch = new CompatItemRenderState();
    }

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/models/modelstatue.png");
    private static final Identifier GUN_TEXTURE = Ids.of("hbm", "textures/models/modelgun.png");

    private static final ModelPart STATUE = statue();
    private static final ModelPart GUN = gun();

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(TileEntityDecoBlockAltF be, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public void extract(TileEntityDecoBlockAltF be, State state, float partialTicks, Vec3 cameraPosition) {
        Direction facing = be.getBlockState().getValue(DecoBlockAlt.FACING);
        state.yaw = switch (facing) {
            case WEST -> 90F;   // metadata 4
            case NORTH -> 180F; // metadata 2
            case EAST -> 270F;  // metadata 5
            default -> 0F;      // metadata 3
        };
        state.watch.extract(be.getLevel(), new ItemStack(ModItems.watch.get()), ItemDisplayContext.FIXED);
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        pose.pushPose();
        pose.translate(0.5F, 1.5F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));

        ModelParts.submit(sink, pose, CompatRenderTypes.entityCutoutNoCull(TEXTURE), STATUE, light,
                OverlayTexture.NO_OVERLAY);
        float g = 0.0625F;
        float q = g * 2 + 0.0625F / 3;
        pose.translate(0.0F, -2 * g, q);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.pushPose();
        pose.translate(0F, -0.05F, 0F);
        pose.scale(0.5128205F, 0.5128205F, 0.5128205F);
        sink.submitItem(pose, state.watch, light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        pose.translate(0.0F, 2 * g, -q);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.translate(-g * 20, g * 4, g * 11);
        pose.mulPose(Axis.ZP.rotationDegrees(-20F));
        ModelParts.submit(sink, pose, CompatRenderTypes.entityCutoutNoCull(GUN_TEXTURE), GUN, light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static ModelPart statue() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "shape1", 0, 0, 0F, 0F, 0F, 16, 8, 16, -8F, 16F, -8F, 0F, 0F);
        box(root, "shape2", 0, 24, 0F, 0F, 0F, 4, 12, 4, -4F, 4F, -2F, 0F, 0F);
        box(root, "shape3", 16, 24, 0F, 0F, 0F, 4, 12, 4, 0F, 4F, -2F, 0F, 0F);
        box(root, "shape4", 32, 40, 0F, 0F, 0F, 8, 12, 4, -4F, -8F, -2F, 0F, 0F);
        box(root, "shape6", 0, 40, 0F, 0F, -2F, 4, 8, 4, 4F, -8F, 0F, 0.5235988F, 0F);
        box(root, "shape7", 16, 40, -4F, 0F, -2F, 4, 8, 4, -4F, -8F, 0F, -0.0872665F, 0F);
        box(root, "shape8", 0, 52, -2F, 0F, -2F, 4, 8, 4, 6F, -2F, 3F, 1.22173F, 0F);
        box(root, "shape9", 16, 52, 0F, 0F, -2F, 4, 8, 4, -8F, -1F, -0.5F, 0.2617994F, 0F);
        box(root, "shape10", 32, 24, -4F, -8F, -4F, 8, 8, 8, 0F, -8F, 0F, -0.1745329F, 0F);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    private static ModelPart gun() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "shape1", 0, 0, 0F, 0F, 0F, 6, 12, 4, 0F, -4F, -1F, 0F, 0F);
        box(root, "shape2", 52, 0, 0F, 0F, 0F, 3, 3, 3, 4F, -7F, -0.5F, 0F, 0F);
        box(root, "shape3", 28, 58, 0F, 0F, 0F, 15, 3, 3, -15F, -7F, -0.5F, 0F, 0F);
        box(root, "shape4", 0, 61, 0F, 0F, 0F, 1, 2, 1, 2F, -3F, 0.5F, 0F, 0.715585F);
        box(root, "shape5", 0, 57, 0F, 0F, 0F, 2, 2, 1, -13.5F, -8F, 0.5F, 0F, 0.6108652F);
        box(root, "shape6", 52, 7, 0F, 0F, 0F, 4, 3, 2, 0F, -6.5F, 0F, 0F, 0F);
        box(root, "shape7", 46, 49, 0F, 0F, 0F, 6, 5, 3, -15F, -3F, -0.5F, 0F, 0F);
        box(root, "shape8", 22, 0, 0F, 0F, 0F, 12, 1, 2, -15F, -4F, 0F, 0F, 0F);
        box(root, "shape9", 52, 13, 0F, 0F, 0F, 3, 3, 3, -3F, -4F, -0.5F, 0F, 0F);
        box(root, "shape10", 11, 60, 0F, 0F, 0F, 6, 2, 2, -9F, -3F, 0F, 0F, 0F);
        box(root, "shape11", 35, 50, 0F, 0F, 0F, 2, 4, 3, -9F, -1F, -0.5F, 0F, 0F);
        box(root, "shape12", 12, 57, 0F, 0F, 0F, 7, 1, 1, -7F, 2F, 0.5F, 0F, 0F);
        box(root, "shape13", 0, 51, 0F, 0F, 0F, 4, 1, 4, 0F, -5F, -1F, 0F, 0F);
        box(root, "shape14", 0, 43, 0F, 0F, 0F, 3, 5, 2, 7F, -7F, 0F, 0F, 0.7853982F);
        box(root, "shape15", 0, 38, 0F, 0F, 0F, 3, 1, 3, -9F, 3F, -0.5F, 0F, -2.792527F);
        box(root, "shape16", 0, 17, 0F, 0F, 0F, 2, 3, 1, -1F, -2F, 0.5F, 0F, 0.2617994F);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    private static void box(PartDefinition root, String name, int u, int v, float x, float y, float z,
                            int w, int h, int d, float px, float py, float pz, float xRot, float zRot) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d),
                PartPose.offsetAndRotation(px, py, pz, xRot, 0F, zRot));
    }
}
