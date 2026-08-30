package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renderer for the firebox — a port of the 1.12.2 {@code RenderFirebox} TESR. Draws the {@code Main}
 * body + the {@code Door} (swings open on its hinge while a player has the GUI open), and the fire
 * interior: {@code InnerBurning} fullbright while lit, {@code InnerEmpty} otherwise.
 */
public class FireboxRenderer<T extends TileEntityFireboxBase> implements CompatBER<T, FireboxRenderState> {

    private final WavefrontObj model;
    private final RenderType baseType;
    private final boolean translateDoor;

    public FireboxRenderer() {
        this("models/machines/firebox.obj", "textures/models/machines/firebox.png", "InnerEmpty", false);
    }

    public FireboxRenderer(String modelPath, String texturePath, String emptyPart) {
        this(modelPath, texturePath, emptyPart, false);
    }

    public FireboxRenderer(String modelPath, String texturePath, String emptyPart, boolean translateDoor) {
        Identifier obj = dev.golem.core.Ids.of(Constants.MOD_ID, modelPath);
        Identifier tex = dev.golem.core.Ids.of(Constants.MOD_ID, texturePath);
        this.model = WavefrontObj.load(obj);
        this.baseType = CompatRenderTypes.entityCutout(tex);
        this.emptyPart = emptyPart;
        this.translateDoor = translateDoor;
    }

    private final String emptyPart;

    @Override public FireboxRenderState createRenderState() { return new FireboxRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(T be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(T be, FireboxRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 3;
        state.yaw = 90F + switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH
        };
        state.burning = be.wasOn;
        state.doorAngle = be.prevDoorAngle + (be.doorAngle - be.prevDoorAngle) * partialTicks;
    }

    @Override
    public void submit(FireboxRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        part(col, ps, "Main", light);

        ps.pushPose();
        if (translateDoor) {
            // The heating oven slides its door along Z; it does not use the firebox hinge.
            ps.translate(0F, 0F, s.doorAngle * 0.75F / 135F);
        } else {
            // The firebox door swings about the legacy hinge at (1.375, 0.375).
            ps.translate(1.375F, 0F, 0.375F);
            ps.mulPose(Axis.YN.rotationDegrees(s.doorAngle));
            ps.translate(-1.375F, 0F, -0.375F);
        }
        part(col, ps, "Door", light);
        ps.popPose();

        if (s.burning) {
            part(col, ps, "InnerBurning", FoundryRenderHelper.FULLBRIGHT);
        } else {
            part(col, ps, emptyPart, light);
        }

        ps.popPose();
    }

    private void part(SubmitSink col, PoseStack ps, String name, int light) {
        col.submitCustomGeometry(ps, baseType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
