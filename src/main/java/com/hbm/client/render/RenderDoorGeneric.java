package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.DoorDecl;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.hbm.tileentity.MachineCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Port of 1.7.10 {@code RenderDoorGeneric} plus the eleven {@code render/tileentity/door/*} routines it
 * dispatched to through {@code DoorDecl.getSEDNARenderer()}. Every transform, pivot and clip plane is
 * the original's; the GL clip planes are CPU clipping ({@link ClippedObjPart}) and the texture binds are
 * render types keyed on the same identifiers.
 *
 * <p>The generic path (silo hatches) plays {@code DoorDecl.getTranslation/getOrigin/getRotation} per OBJ
 * part off the wall clock, and the transition seal plays its converted Collada scene through
 * {@link DoorColladaModel}, exactly where the original branched on {@code getAnimatedModel()}.
 */
public class RenderDoorGeneric implements CompatBER<TileEntityDoorGeneric, DoorRenderState> {

    private static final float[] tran = new float[3];
    private static final float[] orig = new float[3];
    private static final float[] rot = new float[3];

    private final Map<Identifier, WavefrontObj> models = new HashMap<>();
    private final Map<String, ClippedObjPart> clipped = new HashMap<>();
    private @Nullable DoorColladaModel seal;

    private WavefrontObj model(Identifier id) {
        return models.computeIfAbsent(id, WavefrontObj::load);
    }

    private ClippedObjPart clippedPart(Identifier id, String part) {
        return clipped.computeIfAbsent(id + "#" + part, key -> new ClippedObjPart(model(id), part));
    }

    private static RenderType cull(Identifier texture) {
        return CompatRenderTypes.entityCutoutCull(texture);
    }

    private static RenderType noCull(Identifier texture) {
        return CompatRenderTypes.entityCutoutNoCull(texture);
    }

    @Override
    public DoorRenderState createRenderState() {
        return new DoorRenderState();
    }

    @Override public int viewDistance() { return 256; }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityDoorGeneric blockEntity, Vec3 cameraPosition) { return true; }

    @Override
    public void extract(TileEntityDoorGeneric te, DoorRenderState state, float partialTicks, Vec3 cameraPosition) {
        DoorDecl door = te.getDoorType();
        state.decl = door;
        state.state = te.state;
        state.skinIndex = te.getSkinIndex();
        state.facing = MachineCore.facingOrdinal(te.getBlockState(), 2);
        state.animStartTime = te.animStartTime;
        state.now = System.currentTimeMillis();
        state.animation = te.currentAnimation;
        if (door != null) {
            long ms = state.now - te.animStartTime;
            state.openTicks = Mth.clamp(te.state == 2 || te.state == 0 ? door.timeToOpen() * 50 - ms : ms, 0, door.timeToOpen() * 50) * 0.02F;
        }
    }

    @Override
    public void submit(DoorRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        DoorDecl door = state.decl;
        if (door == null) return;
        int light = state.lightCoords;
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);

        switch (state.facing) {
            case 2 -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case 4 -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case 3 -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        switch (door.getRenderer()) {
            case VAULT -> renderVaultDoor(state, poseStack, sink, light, overlay);
            case FIRE -> renderFireDoor(state, poseStack, sink, light, overlay);
            case SLIDING_BLAST -> renderSlidingBlastDoor(state, poseStack, sink, light, overlay);
            case SEAL -> renderSealDoor(state, poseStack, sink, light, overlay);
            case SECURE -> renderSecureDoor(state, poseStack, sink, light, overlay);
            case AIRLOCK -> renderAirlockDoor(state, poseStack, sink, light, overlay);
            case SLIDING -> renderSlidingDoor(state, poseStack, sink, light, overlay);
            case CONTAINMENT -> renderContainmentDoor(state, poseStack, sink, light, overlay);
            case WATER -> renderWaterDoor(state, poseStack, sink, light, overlay);
            case VEHICLE -> renderVehicleDoor(state, poseStack, sink, light, overlay);
            case CARGO -> renderCargoDoor(state, poseStack, sink, light, overlay);
            default -> renderGeneric(state, door, poseStack, sink, light, overlay);
        }

        poseStack.popPose();
    }

    // --- the original RenderDoorGeneric body: animated Collada scene, or OBJ parts moved per DoorDecl ---

    private void renderGeneric(DoorRenderState state, DoorDecl door, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        float[] offset = door.getOffsetTransform();
        poseStack.translate(offset[0], offset[1], offset[2]);
        double[][] clip = door.getClippingPlanes();

        if (door.hasAnimatedModel()) {
            if (seal == null) seal = DoorColladaModel.load(DoorDecl.TRANSITION_SEAL_OBJ, DoorDecl.TRANSITION_SEAL_ANIM,
                    door.getTextureForPart(state.skinIndex, ""));
            long time = state.now;
            long startTime = state.state > 1 ? state.animStartTime : time;
            boolean reverse = state.state == 1 || state.state == 2;
            seal.submit(poseStack, sink, light, time - startTime, reverse);
            return;
        }

        Identifier modelId = door.getModel();
        if (modelId == null) return;
        WavefrontObj model = model(modelId);
        float openTicks = state.openTicks;

        for (String partName : model.partNames()) {
            if (!door.doesRender(partName, false))
                continue;

            poseStack.pushPose();
            {
                Identifier texture = door.getTextureForPart(state.skinIndex, partName);
                doPartTransform(door, partName, openTicks, false, poseStack);
                drawPart(model, modelId, partName, texture, clip, poseStack, sink, light, overlay);

                for (String innerPartName : door.getChildren(partName)) {
                    if (!door.doesRender(innerPartName, true))
                        continue;

                    poseStack.pushPose();
                    {
                        Identifier innerTexture = door.getTextureForPart(state.skinIndex, innerPartName);
                        doPartTransform(door, innerPartName, openTicks, true, poseStack);
                        drawPart(model, modelId, innerPartName, innerTexture, clip, poseStack, sink, light, overlay);
                    }
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
    }

    private void drawPart(WavefrontObj model, Identifier modelId, String part, @Nullable Identifier texture, double[][] clip,
                          PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        if (texture == null) return;
        if (clip.length == 0) {
            sink.submitCustomGeometry(poseStack, cull(texture), (pose, buffer) -> model.renderPart(part, pose, buffer, light, overlay));
        } else {
            ClippedObjPart clippedPart = clippedPart(modelId, part);
            sink.submitCustomGeometry(poseStack, cull(texture), (pose, buffer) -> clippedPart.draw(pose, buffer, light, overlay, clip, 0, 0, 0));
        }
    }

    public void doPartTransform(DoorDecl door, String name, float openTicks, boolean child, PoseStack poseStack) {
        door.getTranslation(name, openTicks, child, tran);
        door.getOrigin(name, orig);
        door.getRotation(name, openTicks, rot);
        poseStack.translate(orig[0], orig[1], orig[2]);
        if (rot[0] != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rot[0]));
        if (rot[1] != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rot[1]));
        if (rot[2] != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rot[2]));
        poseStack.translate(-orig[0] + tran[0], -orig[1] + tran[1], -orig[2] + tran[2]);
    }

    private void part(WavefrontObj model, String part, RenderType type, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) -> model.renderPart(part, pose, buffer, light, overlay));
    }

    private void clippedPart(Identifier modelId, String part, RenderType type, double[][] planes, double ox, double oy, double oz,
                             PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        ClippedObjPart clippedPart = clippedPart(modelId, part);
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) -> clippedPart.draw(pose, buffer, light, overlay, planes, ox, oy, oz));
    }

    // --- RenderVaultDoor ---

    private void renderVaultDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        Identifier doorTex = DoorDecl.PHEO_VAULT_DOOR_3;
        Identifier labelTex = DoorDecl.PHEO_LABEL_101;

        switch (state.skinIndex) {
            case 1 -> labelTex = DoorDecl.PHEO_LABEL_87;
            case 2 -> labelTex = DoorDecl.PHEO_LABEL_106;
            case 3 -> { doorTex = DoorDecl.PHEO_VAULT_DOOR_4; labelTex = DoorDecl.PHEO_LABEL_81; }
            case 4 -> { doorTex = DoorDecl.PHEO_VAULT_DOOR_4; labelTex = DoorDecl.PHEO_LABEL_111; }
            case 5 -> { doorTex = DoorDecl.PHEO_VAULT_DOOR_S; labelTex = DoorDecl.PHEO_LABEL_2; }
            case 6 -> { doorTex = DoorDecl.PHEO_VAULT_DOOR_S; labelTex = DoorDecl.PHEO_LABEL_99; }
            default -> { }
        }

        double pull = 0;
        double slide = 0;

        if (state.state == TileEntityDoorGeneric.STATE_OPEN) {
            pull = 1;
            slide = 1;
        }

        if (state.animation != null) {
            pull = state.bus("PULL")[2];
            slide = state.bus("SLIDE")[0];
        }

        double diameter = 4.25D;
        double circumference = diameter * Math.PI;
        slide *= 5D;
        double roll = 360D * slide / circumference;

        WavefrontObj model = model(DoorDecl.PHEO_VAULT_DOOR_OBJ);
        part(model, "Frame", cull(doorTex), poseStack, sink, light, overlay);
        poseStack.translate(-pull, 0, 0);
        poseStack.translate(0, 0, slide);
        poseStack.translate(0, 2.5, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) roll));
        poseStack.translate(0, -2.5, 0);
        part(model, "Door", cull(doorTex), poseStack, sink, light, overlay);
        part(model, "Label", cull(labelTex), poseStack, sink, light, overlay);
    }

    // --- RenderFireDoor ---

    private void renderFireDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        RenderType type = cull(DoorDecl.FIRE_DOOR.getSkinFromIndex(state.skinIndex));

        double maxRaise = 2.75;
        double raise = 0;
        if (state.state == TileEntityDoorGeneric.STATE_OPEN) raise = maxRaise;

        if (state.animation != null) {
            raise = state.bus("DOOR")[1] * maxRaise;
        }

        WavefrontObj model = model(DoorDecl.PHEO_FIRE_DOOR_OBJ);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(-0.5, 0, 0);
        part(model, "Frame", type, poseStack, sink, light, overlay);
        poseStack.translate(0, Mth.clamp(raise, 0, maxRaise), 0);
        part(model, "Door", type, poseStack, sink, light, overlay);
    }

    // --- RenderSlidingBlastDoor ---

    private void renderSlidingBlastDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        RenderType type = noCull(DoorDecl.SLIDE_DOOR.getSkinFromIndex(state.skinIndex));

        double maxOpen = 2.125;
        double open = 0;
        double lock = 0;
        if (state.state == TileEntityDoorGeneric.STATE_OPEN) {
            open = maxOpen;
            lock = 90;
        }

        if (state.animation != null) {
            open = state.bus("DOOR")[1] * maxOpen;
            lock = state.bus("LOCK")[0] * 90;
        }

        Identifier obj = DoorDecl.PHEO_BLAST_DOOR_OBJ;
        WavefrontObj model = model(obj);
        part(model, "Frame", type, poseStack, sink, light, overlay);

        double[][] planes = { { 0.0, 0.0, 1, 2.5 }, { 0.0, 0.0, -1, 2.5 } };
        double slide = Mth.clamp(open, 0, maxOpen);

        poseStack.pushPose();
        poseStack.translate(0, 0, slide);
        clippedPart(obj, "LeftDoor", type, planes, 0, 0, slide, poseStack, sink, light, overlay);
        poseStack.pushPose();
        poseStack.translate(0, 1.8125, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 + lock)));
        poseStack.translate(0, -1.8125, 0);
        // The lock rides the leaf: the clip planes are the frame's, so they follow the leaf's slide only
        // (the lock's pivot rotation happens well inside the frame, as it did under GL).
        part(model, "RightLock", type, poseStack, sink, light, overlay);
        poseStack.popPose();
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0, -slide);
        clippedPart(obj, "RightDoor", type, planes, 0, 0, -slide, poseStack, sink, light, overlay);
        poseStack.pushPose();
        poseStack.translate(0, 1.8125, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 + lock)));
        poseStack.translate(0, -1.8125, 0);
        part(model, "LeftLock", type, poseStack, sink, light, overlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    // --- RenderSealDoor ---

    private void renderSealDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        RenderType type = cull(DoorDecl.SLIDING_SEAL_DOOR.getSkinFromIndex(state.skinIndex));

        double maxRaise = 1;
        double raise = 0;
        if (state.state == TileEntityDoorGeneric.STATE_OPEN) raise = maxRaise;

        if (state.animation != null) {
            raise = state.bus("DOOR")[1] * maxRaise;
        }

        Identifier obj = DoorDecl.PHEO_SEAL_DOOR_OBJ;
        WavefrontObj model = model(obj);
        poseStack.translate(0.5, 0, 0);
        part(model, "Frame", type, poseStack, sink, light, overlay);

        double[][] planes = { { 0, 0, -1, 0.5001 } };
        double slide = DoorDecl.smoothstep(Mth.clamp(raise, 0, maxRaise), 0, 1) * 0.9;
        poseStack.translate(0, 0, slide);
        clippedPart(obj, "Door", type, planes, 0, 0, slide, poseStack, sink, light, overlay);
    }

    // --- RenderSecureDoor ---

    private void renderSecureDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        RenderType type = cull(DoorDecl.SECURE_ACCESS_DOOR.getSkinFromIndex(state.skinIndex));

        double maxRaise = 3.5;
        double raise = 0;
        if (state.state == TileEntityDoorGeneric.STATE_OPEN) raise = maxRaise;

        if (state.animation != null) {
            raise = state.bus("DOOR")[1] * maxRaise;
        }

        WavefrontObj model = model(DoorDecl.PHEO_SECURE_DOOR_OBJ);
        poseStack.translate(0, 1, 0);
        part(model, "Frame", type, poseStack, sink, light, overlay);
        poseStack.translate(0, Mth.clamp(raise, 0, maxRaise), 0);
        part(model, "Door", type, poseStack, sink, light, overlay);
    }

    // --- RenderAirlockDoor ---

    private void renderAirlockDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        RenderType type = noCull(DoorDecl.ROUND_AIRLOCK_DOOR.getSkinFromIndex(state.skinIndex));

        double maxOpen = 1.5;
        double open = 0;
        if (state.state == TileEntityDoorGeneric.STATE_OPEN) open = maxOpen;

        if (state.animation != null) {
            open = state.bus("DOOR")[1] * maxOpen;
        }

        Identifier obj = DoorDecl.PHEO_AIRLOCK_DOOR_OBJ;
        WavefrontObj model = model(obj);
        poseStack.translate(0, 0, 0.5);
        part(model, "Frame", type, poseStack, sink, light, overlay);

        double[][] planes = { { 0.0, 0.0, 1, 1.999 }, { 0.0, 0.0, -1, 1.999 } };
        double slide = Mth.clamp(open, 0, maxOpen);

        poseStack.pushPose();
        poseStack.translate(0, 0, slide);
        clippedPart(obj, "Left", type, planes, 0, 0, slide, poseStack, sink, light, overlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0, -slide);
        clippedPart(obj, "Right", type, planes, 0, 0, -slide, poseStack, sink, light, overlay);
        poseStack.popPose();
    }

    // --- RenderSlidingDoor (QE sliding) ---

    private void renderSlidingDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        RenderType type = noCull(DoorDecl.QE_SLIDING.getSkinFromIndex(state.skinIndex));

        double maxOpen = 0.95;
        double open = 0;
        if (state.state == TileEntityDoorGeneric.STATE_OPEN) open = maxOpen;

        if (state.animation != null) {
            open = state.bus("DOOR")[1] * maxOpen;
        }

        WavefrontObj model = model(DoorDecl.PHEO_SLIDING_DOOR_OBJ);
        poseStack.translate(0.53125, 0.001, 0.5);
        part(model, "Frame", type, poseStack, sink, light, overlay);

        poseStack.pushPose();
        poseStack.translate(0, 0, Mth.clamp(open, 0, maxOpen));
        part(model, "Left", type, poseStack, sink, light, overlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0, -Mth.clamp(open, 0, maxOpen));
        part(model, "Right", type, poseStack, sink, light, overlay);
        poseStack.popPose();
    }

    // --- RenderContainmentDoor ---

    private void renderContainmentDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        RenderType type = cull(DoorDecl.QE_CONTAINMENT.getSkinFromIndex(state.skinIndex));

        double maxRaise = 2.25;
        double raise = 0;
        if (state.state == TileEntityDoorGeneric.STATE_OPEN) raise = maxRaise;

        if (state.animation != null) {
            raise = state.bus("DOOR")[1] * maxRaise;
        }

        Identifier obj = DoorDecl.PHEO_CONTAINMENT_DOOR_OBJ;
        WavefrontObj model = model(obj);
        poseStack.translate(0.25, 0.0, 0.0);
        part(model, "Frame", type, poseStack, sink, light, overlay);

        double[][] planes = { { 0, -1, 0, 3 } };
        double lift = Mth.clamp(raise, 0, maxRaise);
        poseStack.translate(0, lift, 0);
        clippedPart(obj, "Door", type, planes, 0, lift, 0, poseStack, sink, light, overlay);
    }

    // --- RenderWaterDoor ---

    private void renderWaterDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        RenderType type = cull(DoorDecl.WATER_DOOR.getSkinFromIndex(state.skinIndex));

        double maxRot = 120;
        double rotation = 0;
        double bolt = 0;
        if (state.state == TileEntityDoorGeneric.STATE_OPEN) {
            rotation = maxRot;
            bolt = 1D;
        }

        if (state.animation != null) {
            rotation = state.bus("DOOR")[1] * maxRot;
            bolt = state.bus("BOLT")[2];
        }

        WavefrontObj model = model(DoorDecl.PHEO_WATER_DOOR_OBJ);
        poseStack.translate(0.375, 0.0, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        part(model, "Frame", type, poseStack, sink, light, overlay);

        poseStack.translate(-1.1875, 0, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) -rotation));
        poseStack.translate(1.1875, 0, 0);
        part(model, "Door_Cube.003", type, poseStack, sink, light, overlay); // ah fuck it

        poseStack.pushPose();
        poseStack.translate(-0.4 * bolt, 0, 0);
        part(model, "Bolts", type, poseStack, sink, light, overlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.40625F, 2.28125, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (bolt * 360)));
        poseStack.translate(-0.40625F, -2.28125, 0);
        part(model, "Top", type, poseStack, sink, light, overlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.40625F, 0.71875, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (bolt * 360)));
        poseStack.translate(-0.40625F, -0.71875, 0);
        part(model, "Bottom", type, poseStack, sink, light, overlay);
        poseStack.popPose();
    }

    // --- RenderVehicleDoor ---

    private void renderVehicleDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        RenderType type = noCull(DoorDecl.LARGE_VEHICLE_DOOR.getSkinFromIndex(state.skinIndex));

        double maxOpen = 3;
        double open = 0;
        if (state.state == TileEntityDoorGeneric.STATE_OPEN) open = maxOpen;

        if (state.animation != null) {
            open = state.bus("DOOR")[1] * maxOpen;
        }

        Identifier obj = DoorDecl.PHEO_VEHICLE_DOOR_OBJ;
        WavefrontObj model = model(obj);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        part(model, "Frame", type, poseStack, sink, light, overlay);

        double[][] planes = { { 1, 0, 0, 3.4375 }, { -1, 0, 0, 3.4375 } };
        double slide = Mth.clamp(open, 0, maxOpen);

        poseStack.pushPose();
        poseStack.translate(-slide, 0, 0);
        clippedPart(obj, "Left", type, planes, -slide, 0, 0, poseStack, sink, light, overlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(slide, 0, 0);
        clippedPart(obj, "Right", type, planes, slide, 0, 0, poseStack, sink, light, overlay);
        poseStack.popPose();
    }

    // --- RenderCargoDoor ---

    private void renderCargoDoor(DoorRenderState state, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        RenderType type = cull(DoorDecl.CARGO_DOOR.getSkinFromIndex(state.skinIndex));

        double botMove = 0;
        double topMove = 0;

        if (state.state == TileEntityDoorGeneric.STATE_OPEN) {
            botMove = 2.0;
            topMove = 1.0;
        }

        if (state.animation != null) {
            double botProgress = Mth.clamp(state.bus("BOT")[1], 0, 1);
            double topProgress = Mth.clamp(state.bus("TOP")[1], 0, 1);
            botMove = botProgress * 2.0;
            topMove = topProgress * 1.0;
        }

        WavefrontObj model = model(DoorDecl.PHEO_CARGO_DOOR_OBJ);
        poseStack.pushPose();
        poseStack.translate(0, 0, 0);

        part(model, "Frame", type, poseStack, sink, light, overlay);

        poseStack.pushPose();
        poseStack.translate(0, topMove, 0);
        part(model, "DoorTop", type, poseStack, sink, light, overlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, botMove, 0);
        part(model, "DoorBot", type, poseStack, sink, light, overlay);
        poseStack.popPose();

        poseStack.popPose();
    }
}
