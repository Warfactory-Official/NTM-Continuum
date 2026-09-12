package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.RBMKColumnType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.*;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class RBMKConsoleRenderer extends GemBER<BlockEntityRBMKConsole, RBMKConsoleRenderer.State> {
    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_console.obj"));
    private static final Identifier TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/machines/rbmk_console.png");
    private final Font font;
    public RBMKConsoleRenderer() { font = net.minecraft.client.Minecraft.getInstance().font; }
    @Override protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(MODEL.all(), Surface.cutout(TEXTURE.toString()))
                .translate(.5F, 0F, .5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(.5F, 0F, 0F);
    }
    @Override public State createRenderState() { return new State(); }
    /** Folded cells own block entities of the same type; only the core draws the desk. */
    @Override public boolean shouldRender(BlockEntityRBMKConsole be, Vec3 camera) {
        return !(be.getBlockState().getBlock() instanceof BlockMultiblockCore core && core.isCellOf(be.getBlockState()))
                && super.shouldRender(be, camera);
    }
    @Override public void extract(BlockEntityRBMKConsole be, State state, float partial, Vec3 camera) {
        state.yaw = yaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));
        state.columns = be.columns.clone();
        for (int i = 0; i < 6; i++) {
            String display = be.screens[i].display;
            if (display == null || display.isEmpty()) { state.text[i] = null; continue; }
            String[] parts = display.split("=", 2);
            state.text[i] = (parts.length == 2 ? Component.translatable(parts[0], parts[1]) : Component.literal(display)).getVisualOrderText();
        }
    }
    /**
     * The column grid is one quad and one diamond per reactor column, built from a snapshot that
     * changes every tick, and the six screens are glyph quads; neither has a mesh to be declared as.
     */
    @Override protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose(); pose.translate(.5D, 0D, .5D); pose.mulPose(Axis.YP.rotationDegrees(state.yaw)); pose.translate(.5D, 0D, 0D);
        if (state.columns != null) sink.submitCustomGeometry(pose, CompatRenderTypes.entitySolid(Ids.of(Constants.MOD_ID, "textures/models/misc/white.png")),
                (last, buffer) -> grid(last, buffer, state.columns, state.lightCoords));
        pose.translate(-.42D, 3.5D, 1.75D);
        for (int i = 0; i < 6; i++) {
            pose.pushPose();
            if ((i & 1) == 1) pose.translate(0D, 0D, -3.5D);
            pose.translate(0D, -.75D * (i >> 1), 0D);
            if (state.text[i] != null) {
                int width = font.width(state.text[i]); float scale = Math.min(.03F, .8F / Math.max(width, 1));
                pose.scale(scale, -scale, scale); pose.mulPose(Axis.YP.rotationDegrees(90F));
                sink.submitText(pose, font, -width / 2F, -font.lineHeight / 2F, state.text[i], false,
                        Font.DisplayMode.POLYGON_OFFSET, 0xF000F0, 0xFF00FF00, 0);
            }
            pose.popPose();
        }
        pose.popPose();
    }
    private static void grid(PoseStack.Pose pose, VertexConsumer buffer, RBMKColumn[] columns, int light) {
        for (int i = 0; i < columns.length; i++) {
            RBMKColumn column = columns[i]; if (column == null) continue;
            float x = -.3725F, y = -(float) Math.floor(i / 15F) * .125F + 3.625F, z = -(i % 15) * .125F + .875F;
            float r = 1F, g = 1F, b = 1F;
            if (column.type == RBMKColumnType.CONTROL) {
                short color = ((RBMKColumn.ControlColumn) column).color;
                if (color >= 0) { int value = new int[]{0xFFFF0000, 0xFFFFFF00, 0xFF008000, 0xFF0000FF, 0xFF800080}[Math.min(color, (short) 4)]; r = (value >> 16 & 255) / 255F; g = (value >> 8 & 255) / 255F; b = (value & 255) / 255F; }
                else { double heat = column.maxHeat == 0D ? 0D : column.heat / column.maxHeat, base = .65D + (i % 2) * .05D; r = (float) (base + (1D - base) * heat); g = b = (float) base; }
            }
            if (column.indicator > 0) { r = g = 1F; b = 0F; }
            quad(pose, buffer, x, y, z, .046875F, color(r, g, b), light);
            if (column.type == RBMKColumnType.FUEL || column.type == RBMKColumnType.FUEL_SIM) {
                float enrichment = (float) ((RBMKColumn.FuelColumn) column).enrichment; diamond(pose, buffer, x + .01F, y, z, 0F, .25F + enrichment * .75F, 0F, light);
            } else if (column.type == RBMKColumnType.CONTROL || column.type == RBMKColumnType.CONTROL_AUTO) {
                float level = (float) ((RBMKColumn.ControlColumn) column).level;
                diamond(pose, buffer, x + .01F, y, z, level, column.type == RBMKColumnType.CONTROL ? level : 0F, column.type == RBMKColumnType.CONTROL ? 0F : level, light);
            }
        }
    }
    private static int color(float r, float g, float b) { return 0xFF000000 | ((int) (Math.max(0, Math.min(1, r)) * 255) << 16) | ((int) (Math.max(0, Math.min(1, g)) * 255) << 8) | (int) (Math.max(0, Math.min(1, b)) * 255); }
    private static void quad(PoseStack.Pose p, VertexConsumer b, float x, float y, float z, float w, int c, int light) { Vertexes.vertex(b,p,x,y+w,z-w,c,0,0,OverlayTexture.NO_OVERLAY,light,0,1,0); Vertexes.vertex(b,p,x,y+w,z+w,c,0,0,OverlayTexture.NO_OVERLAY,light,0,1,0); Vertexes.vertex(b,p,x,y-w,z+w,c,0,0,OverlayTexture.NO_OVERLAY,light,0,1,0); Vertexes.vertex(b,p,x,y-w,z-w,c,0,0,OverlayTexture.NO_OVERLAY,light,0,1,0); }
    private static void diamond(PoseStack.Pose p, VertexConsumer b, float x, float y, float z, float r, float g, float bl, int light) { float w=.03125F,e=.022097F; int c=color(r,g,bl); float[][] v={{x,y+w,z},{x,y+e,z+e},{x,y,z+w},{x,y-e,z+e},{x,y+e,z-e},{x,y+w,z},{x,y-e,z-e},{x,y,z-w},{x,y+w,z},{x,y-e,z+e},{x,y-w,z},{x,y-e,z-e}}; for(float[] q:v) Vertexes.vertex(b,p,q[0],q[1],q[2],c,0,0,OverlayTexture.NO_OVERLAY,light,0,1,0); }
    private static float yaw(net.minecraft.core.Direction direction) { return switch (direction) { case NORTH -> 90F; case WEST -> 180F; case SOUTH -> 270F; default -> 0F; }; }
    public static final class State extends CompatRenderState { float yaw; RBMKColumn[] columns; final net.minecraft.util.FormattedCharSequence[] text = new net.minecraft.util.FormattedCharSequence[6]; }
}
