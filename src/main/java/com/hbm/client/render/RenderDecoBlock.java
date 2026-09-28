package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.PinkCloudBroadcaster;
import com.hbm.blocks.machine.Radiobox;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderDecoBlock} for its three box-model blocks: the corrupted broadcaster ({@code ModelBroadcaster}
 * in its skin), the FM radio (the same model in the receiver skin) and the pest control box ({@code ModelRadio},
 * pushed one block back against its wall, its lever thrown to 160 degrees while on and 20 while off). The frame
 * is 1.7's: {@code T(0.5, 1.5, 0.5) Rz(180)}, then the facing, then the 1/16-scaled boxes.
 *
 * <p>The Techne models set {@code mirror} after {@code addBox}, so their boxes were never mirrored; they are
 * built unmirrored here too.
 */
public final class RenderDecoBlock<T extends BlockEntity> extends GemBER<T, RenderDecoBlock.State> {

    public enum Kind { BROADCASTER, RADIOREC, RADIOBOX }

    public static final class State extends CompatRenderState {
        public float yaw;
        public boolean on;
    }

    private static final Identifier BROADCASTER_TEX = Ids.of(Constants.MOD_ID, "textures/models/modelbroadcaster.png");
    private static final Identifier RECEIVER_TEX = Ids.of(Constants.MOD_ID, "textures/models/modelradioreceiver.png");
    private static final Identifier RADIO_TEX = Ids.of(Constants.MOD_ID, "textures/models/modelradio.png");

    private static final ModelPart BROADCASTER = broadcaster();
    private static final ModelPart RADIO_BODY = radioBody();
    private static final ModelPart LEVER_OFF = lever(20);
    private static final ModelPart LEVER_ON = lever(160);

    private final Kind kind;

    public RenderDecoBlock(Kind kind) {
        this.kind = kind;
    }

    private static ModelPart broadcaster() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("shape1", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 14, 10, 8),
                PartPose.offset(-7F, 14F, -4F));
        mesh.getRoot().addOrReplaceChild("shape2", CubeListBuilder.create().texOffs(4, 21).addBox(0F, 0F, 0F, 2, 3, 2),
                PartPose.offset(-5F, 11F, -1F));
        mesh.getRoot().addOrReplaceChild("shape3", CubeListBuilder.create().texOffs(0, 18).addBox(0F, 0F, 0F, 1, 11, 1),
                PartPose.offset(-4.5F, 0F, -0.5F));
        mesh.getRoot().addOrReplaceChild("shape4", CubeListBuilder.create().texOffs(4, 18).addBox(0F, 0F, 0F, 3, 2, 1),
                PartPose.offset(2F, 12F, -0.5F));
        return ModelParts.bake(LayerDefinition.create(mesh, 64, 32));
    }

    private static ModelPart radioBody() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("box", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 8, 14, 4),
                PartPose.offset(-4F, 9F, -12F));
        mesh.getRoot().addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 18).addBox(0F, 0F, 0F, 7, 13, 1),
                PartPose.offset(-3.5F, 9.5F, -12.5F));
        return ModelParts.bake(LayerDefinition.create(mesh, 32, 32));
    }

    private static ModelPart lever(int rotation) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("lever", CubeListBuilder.create().texOffs(16, 18).addBox(0F, -1F, -1F, 2, 8, 2),
                PartPose.offsetAndRotation(4F, 16F, -10F, -(float) (rotation / 180F * Math.PI), 0F, 0F));
        return ModelParts.bake(LayerDefinition.create(mesh, 32, 32));
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.viewDistance(256);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T te, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = te.getBlockState();
        Direction facing = bs.getValue(PinkCloudBroadcaster.FACING);
        if (kind == Kind.RADIOBOX) {
            // 1.7 metadata 4/8 west 270, 2/6 north 0, 5/9 east 90, 3/7 south 180
            state.yaw = switch (facing) {
                case WEST -> 270F;
                case EAST -> 90F;
                case SOUTH -> 180F;
                default -> 0F;
            };
            state.on = bs.getValue(Radiobox.ON);
        } else {
            // 1.7 metadata 4 west 90, 2 north 180, 5 east 270, 3 south 0
            state.yaw = switch (facing) {
                case WEST -> 90F;
                case NORTH -> 180F;
                case EAST -> 270F;
                default -> 0F;
            };
        }
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0.5F, 1.5F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        int light = state.lightCoords;
        switch (kind) {
            case BROADCASTER -> ModelParts.submit(sink, pose, CompatRenderTypes.entityCutoutCull(BROADCASTER_TEX), BROADCASTER,
                    light, OverlayTexture.NO_OVERLAY);
            case RADIOREC -> ModelParts.submit(sink, pose, CompatRenderTypes.entityCutoutCull(RECEIVER_TEX), BROADCASTER,
                    light, OverlayTexture.NO_OVERLAY);
            case RADIOBOX -> {
                pose.translate(0F, 0F, 1F);
                ModelParts.submit(sink, pose, CompatRenderTypes.entityCutoutCull(RADIO_TEX), RADIO_BODY, light,
                        OverlayTexture.NO_OVERLAY);
                ModelParts.submit(sink, pose, CompatRenderTypes.entityCutoutCull(RADIO_TEX), state.on ? LEVER_ON : LEVER_OFF,
                        light, OverlayTexture.NO_OVERLAY);
            }
        }
        pose.popPose();
    }
}
