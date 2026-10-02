package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.storage.BlockMassStorage;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderMassStorage}: the unit's front panel — the stored type drawn as a GUI icon, its
 * abbreviated count in green, and a red-to-green fill bar — laid flat on the block's front face in the
 * face's own 16-pixel space, pushed 0.005 out of it against z-fighting. Everything is full-bright (1.7
 * sets the lightmap to 240/240).
 */
public final class RenderMassStorage implements CompatBER<TileEntityMassStorage, RenderMassStorage.State> {

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int TEXT_COLOR = 0xFF00FF00;
    /** 1.7 draws the bar with texturing and lighting off; a white sprite on a flat, unlit sheet is that. */
    private static final Identifier WHITE =
            Ids.of(Constants.MOD_ID, "textures/models/misc/white.png");

    private final Font font = Minecraft.getInstance().font;

    @Override
    public State createRenderState() {
        return new State();
    }

    /** 1.7 syncs the panel over {@code networkPackNT(32)}, its TE render distance. */
    @Override
    public int viewDistance() {
        return 32;
    }

    @Override
    public void extract(TileEntityMassStorage storage, State state, float partialTicks, Vec3 cameraPosition) {
        ItemStack type = storage.getFilterType();
        if (type.isEmpty()) {
            state.visible = false;
            state.item.clear();
            return;
        }
        state.visible = true;
        state.item.extract(storage.getLevel(), type, ItemDisplayContext.GUI);
        state.facing = storage.getBlockState().hasProperty(BlockMassStorage.FACING)
                ? storage.getBlockState().getValue(BlockMassStorage.FACING) : Direction.NORTH;
        state.fraction = (double) storage.getStockpile() / (double) storage.getCapacity();
        state.count = getTextForCount(storage.getStockpile(),
                Minecraft.getInstance().options.forceUnicodeFont().get());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (!state.visible) return;

        pose.pushPose();
        // align item (and flip)
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.mulPose(Axis.YP.rotationDegrees(panelAngle(state.facing)));
        pose.translate(-0.5F, -0.5F, -0.5F);

        pose.translate(0F, 0F, -0.005F); // offset to prevent z-fighting
        pose.scale(1F / 16F, 1F / 16F, -0.0001F); // scale to block size

        pose.pushPose();
        pose.translate(4F, 2.5F, 0F); // adjust to centered location
        pose.scale(8F / 16F, 8F / 16F, 1F); // scale to 8 pixels across
        // 1.7's renderItemIntoGUI at (0, 0): the icon's 16-pixel box centred on (8, 8), Y running down.
        pose.translate(8F, 8F, 0F);
        pose.scale(16F, -16F, 16F);
        flattenedNormals(pose, panelAngle(state.facing));
        sink.submitItem(pose, state.item, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        int textX = 32 - font.width(state.count) / 2;
        int textY = 44;
        pose.pushPose();
        pose.scale(4F / 16F, 4F / 16F, 4F / 16F);
        // 1.7 draws its own shadow one pixel down-right at a quarter brightness, then the text; vanilla's
        // shadowed text is that same pair.
        sink.submitText(pose, font, textX, textY, FormattedCharSequence.forward(state.count, Style.EMPTY),
                true, Font.DisplayMode.NORMAL, FULL_BRIGHT, TEXT_COLOR, 0);
        pose.popPose();

        float fraction = (float) state.fraction;
        int barColor = 0xFF000000 | (Math.round((1F - fraction) * 255F) << 16) | (Math.round(fraction * 255F) << 8);
        float minX = 2F;
        float maxX = (float) (2D + state.fraction * 12D);
        float minY = 13.5F;
        float maxY = 14F;
        sink.submitCustomGeometry(pose, CompatRenderTypes.flatCutout(WHITE), (p, buffer) -> {
            bar(buffer, p, minX, maxY, barColor);
            bar(buffer, p, maxX, maxY, barColor);
            bar(buffer, p, maxX, minY, barColor);
            bar(buffer, p, minX, minY, barColor);
        });

        pose.popPose();
    }

    private static void bar(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int color) {
        Vertexes.vertex(buffer, pose, x, y, 0F, color, 0.5F, 0.5F, OverlayTexture.NO_OVERLAY, FULL_BRIGHT,
                0F, 1F, 0F);
    }

    /**
     * The icon's normal matrix, set outright. The panel flattens the icon with a mirrored, wildly
     * non-uniform scale (z * -0.0001, then the GUI's y * -1); the exact inverse-transpose of that squashes
     * every visible face's normal onto the panel's outward axis, which is what 26.1 computes and
     * normalises. 1.20.1's {@code PoseStack.scale} takes a cube root of the negative scale product and
     * never normalises, so the same scale lights the icon from a garbage direction (a visibly dark icon).
     * Writing the squashed matrix directly — the face's rotation, the in-plane axes reduced 10,000:1 —
     * gives every node the same outward-facing normals: normalised on 1.21+, clamped to the unit axis by
     * 1.20.1's byte packing.
     */
    private static void flattenedNormals(PoseStack pose, float panelAngle) {
        pose.last().normal().set(new org.joml.Matrix3f()
                .rotateZ((float) Math.PI)
                .rotateY((float) Math.toRadians(panelAngle))
                .scale(0.01F, -0.01F, -10000F));
    }

    /** 1.7's {@code switch(meta / 4)}: north (0) unrotated, south 180, west -90, east 90. */
    private static float panelAngle(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180F;
            case WEST -> -90F;
            case EAST -> 90F;
            default -> 0F;
        };
    }

    /** 1.7 {@code getTextForCount}, default-locale {@code String.format} and all. */
    static String getTextForCount(int stackSize, boolean isUnicode) {
        if (stackSize >= 100000000 || (stackSize >= 1000000 && isUnicode)) return String.format("%.0fM", stackSize / 1000000f);
        if (stackSize >= 1000000) return String.format("%.1fM", stackSize / 1000000f);
        if (stackSize >= 100000 || (stackSize >= 10000 && isUnicode)) return String.format("%.0fK", stackSize / 1000f);
        if (stackSize >= 10000) return String.format("%.1fK", stackSize / 1000f);
        return String.valueOf(stackSize);
    }

    public static final class State extends CompatRenderState {
        final CompatItemRenderState item = new CompatItemRenderState();
        boolean visible;
        Direction facing = Direction.NORTH;
        double fraction;
        String count = "0";
    }
}
