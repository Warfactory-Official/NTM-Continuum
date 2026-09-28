package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDisplay;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.RBMKColumnType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderRBMKDisplay}: the 7x7 column map, one flat quad per column (heat-tinted grey, dyed for
 * control rods, yellow while the crane marks it) with the fuel/control diamond over it, drawn untextured at
 * full brightness and stretched by 8/7 so the grid fills the panel face.
 */
public final class RBMKDisplayRenderer extends GemBER<BlockEntityRBMKDisplay, RBMKDisplayRenderer.State> {
    private static final net.minecraft.resources.Identifier WHITE = Ids.of(Constants.MOD_ID, "textures/models/misc/white.png");

    @Override protected void setup(Gem.Model<State> declaration) { declaration.offScreen(); }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(BlockEntityRBMKDisplay be, State state, float partial, Vec3 camera) {
        state.yaw = RBMKPanelRenderers.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING));
        state.columns = be.columns.clone();
    }

    @Override protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.columns == null) return;
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.translate(0D, .5D, 0D);
        pose.scale(1F, 8F / 7F, 8F / 7F);
        pose.translate(0D, -.5D, 0D);
        final RBMKColumn[] columns = state.columns;
        sink.submitCustomGeometry(pose, CompatRenderTypes.entitySolid(WHITE), (last, buffer) -> grid(last, buffer, columns));
        pose.popPose();
    }

    private static void grid(PoseStack.Pose pose, VertexConsumer buffer, RBMKColumn[] columns) {
        for (int i = 0; i < columns.length; i++) {
            RBMKColumn col = columns[i];
            if (col == null) continue;
            float kx = .28125F, ky = -(i / 7) * .125F + .875F, kz = -(i % 7) * .125F + .125F * 3;
            int color;
            short dye = col instanceof RBMKColumn.ControlColumn control ? control.color : -1;
            if (dye >= 0) {
                color = switch (dye) { case 0 -> 0xFF0000; case 1 -> 0xFFFF00; case 2 -> 0x008000; case 3 -> 0x0000FF; case 4 -> 0x8000FF; default -> 0xFFFFFF; };
                color |= 0xFF000000;
            } else {
                double heat = col.maxHeat == 0D ? 0D : col.heat / col.maxHeat;
                double base = .65D + (i % 2) * .05D;
                color = rgb((float) (base + (1 - base) * heat), (float) base, (float) base);
            }
            if (col.indicator > 0) color = rgb(1F, 1F, 0F);
            float w = .0625F * .75F;
            vertex(buffer, pose, kx, ky + w, kz - w, color);
            vertex(buffer, pose, kx, ky + w, kz + w, color);
            vertex(buffer, pose, kx, ky - w, kz + w, color);
            vertex(buffer, pose, kx, ky - w, kz - w, color);
            if (col.type == RBMKColumnType.FUEL || col.type == RBMKColumnType.FUEL_SIM) {
                float enrichment = (float) ((RBMKColumn.FuelColumn) col).enrichment;
                dot(buffer, pose, kx + .01F, ky, kz, rgb(0F, .25F + enrichment * .75F, 0F));
            } else if (col.type == RBMKColumnType.CONTROL) {
                float level = (float) ((RBMKColumn.ControlColumn) col).level;
                dot(buffer, pose, kx + .01F, ky, kz, rgb(level, level, 0F));
            } else if (col.type == RBMKColumnType.CONTROL_AUTO) {
                float level = (float) ((RBMKColumn.ControlColumn) col).level;
                dot(buffer, pose, kx + .01F, ky, kz, rgb(level, 0F, level));
            }
        }
    }

    private static void dot(VertexConsumer b, PoseStack.Pose p, float x, float y, float z, int c) {
        float w = .03125F, e = .022097F;
        float[][] v = {{x, y + w, z}, {x, y + e, z + e}, {x, y, z + w}, {x, y - e, z + e},
                {x, y + e, z - e}, {x, y + w, z}, {x, y - e, z - e}, {x, y, z - w},
                {x, y + w, z}, {x, y - e, z + e}, {x, y - w, z}, {x, y - e, z - e}};
        for (float[] q : v) vertex(b, p, q[0], q[1], q[2], c);
    }

    private static void vertex(VertexConsumer b, PoseStack.Pose p, float x, float y, float z, int c) {
        Vertexes.vertex(b, p, x, y, z, c, 0F, 0F, OverlayTexture.NO_OVERLAY, 0xF000F0, 1F, 0F, 0F);
    }

    private static int rgb(float r, float g, float b) {
        return 0xFF000000 | (int) (Math.max(0, Math.min(1, r)) * 255) << 16 | (int) (Math.max(0, Math.min(1, g)) * 255) << 8
                | (int) (Math.max(0, Math.min(1, b)) * 255);
    }

    public static final class State extends CompatRenderState {
        RBMKColumn[] columns;
        float yaw;
    }
}
