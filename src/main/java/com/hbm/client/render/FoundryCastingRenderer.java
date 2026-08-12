package com.hbm.client.render;

import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityFoundryBasin;
import com.hbm.tileentity.machine.TileEntityFoundryCastingBase;
import com.hbm.tileentity.machine.TileEntityFoundryMold;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renderer for the casting blocks (1.12.2 {@code RenderFoundry}): the installed mold + the finished
 * item lying flat on the block, and the molten metal surface ({@code lava_gray} tinted by
 * {@code moltenColor}, fullbright) while filled. For the basin it also draws the {@code foundry_basin.obj}
 * body (the mold caster keeps its JSON block model).
 */
public class FoundryCastingRenderer<T extends TileEntityFoundryCastingBase> implements CompatBER<T, FoundryCastingRenderer.State> {

    public static class State extends CompatRenderState {
        public boolean hasMelt;
        public int meltColor = 0xFFFFFF;
        public float meltY;
        public float moldHeight;
        public float outHeight;
        public final CompatItemRenderState moldItem = new CompatItemRenderState();
        public boolean showMold;
        public final CompatItemRenderState outputItem = new CompatItemRenderState();
        public boolean showOutput;
        public boolean outputIs3d;
    }

    private final @Nullable WavefrontObj body;
    /** material name → its render type (multi-texture OBJ, resolved from the .mtl). */
    private final java.util.Map<String, RenderType> materialTypes = new java.util.LinkedHashMap<>();
    private final RenderType meltType;

    /** @param bodyObjPath OBJ body to draw (basin), or null for JSON-modelled blocks (mold caster). */
    public FoundryCastingRenderer(@Nullable String bodyObjPath) {
        if (bodyObjPath != null) {
            this.body = WavefrontObj.load(Identifier.fromNamespaceAndPath(Constants.MOD_ID, bodyObjPath));
            var mtl = WavefrontObj.loadMtl(Identifier.fromNamespaceAndPath(Constants.MOD_ID, bodyObjPath.replace(".obj", ".mtl")));
            for (String mat : body.materialNames()) {
                Identifier tex = mtl.get(mat);
                if (tex != null) materialTypes.put(mat, RenderTypes.entityCutout(tex));
            }
        } else {
            this.body = null;
        }
        // Emissive = uniform fullbright, no per-face shading (the old lighting-off molten quads).
        this.meltType = RenderTypes.entityTranslucentEmissive(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/lava_gray.png"));
    }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRender(T be, Vec3 cameraPosition) { return true; }

    @Override
    public void extract(T be, State state, float partialTicks, Vec3 cameraPosition) {

        state.hasMelt = be.type != null && be.amount > 0;
        if (state.hasMelt) state.meltColor = be.type.moltenColor;

        if (be instanceof TileEntityFoundryMold mold) {
            state.meltY = (float) mold.getMeltLevel();
            state.moldHeight = (float) mold.moldHeight();
            state.outHeight = (float) mold.outHeight();
        } else if (be instanceof TileEntityFoundryBasin basin) {
            state.meltY = (float) basin.getMeltLevel();
            state.moldHeight = (float) basin.moldHeight();
            state.outHeight = (float) basin.outHeight();
        }

        state.showMold = extractItem(be, be.syncMold, state.moldItem);
        state.showOutput = extractItem(be, be.syncOutput, state.outputItem);
        state.outputIs3d = be.syncOutput.getItem() instanceof BlockItem;
    }

    private static boolean extractItem(TileEntityFoundryCastingBase be, ItemStack stack, CompatItemRenderState target) {
        if (stack.isEmpty() || be.getLevel() == null) {
            target.clear();
            return false;
        }
        ItemStack one = stack.copy();
        one.setCount(1);
        target.extract(be.getLevel(), one, ItemDisplayContext.FIXED);
        return !target.isEmpty();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;

        // The OBJ vertices are in 0..1 corner space (forge_marker convention) — no centring needed.
        if (body != null) {
            for (var e : materialTypes.entrySet()) {
                String mat = e.getKey();
                col.submitCustomGeometry(ps, e.getValue(),
                        (pose, buffer) -> body.renderMaterial(mat, pose, buffer, light, OverlayTexture.NO_OVERLAY));
            }
        }

        // items lying flat (the old drawItem: centred, rotated onto the XZ plane, 1.5x scale)
        if (s.showMold) submitFlatItem(s.moldItem, false, s.moldHeight, ps, col, light);
        if (s.showOutput) submitFlatItem(s.outputItem, s.outputIs3d, s.outHeight, ps, col, light);

        // molten surface
        if (s.hasMelt) {
            int color = 0xFF000000 | (s.meltColor & 0xFFFFFF);
            float y = s.meltY;
            col.submitCustomGeometry(ps, meltType,
                    (pose, buffer) -> FoundryRenderHelper.flatQuad(pose, buffer, 0.125F, 0.125F, 0.875F, 0.875F, y, color));
        }
    }

    private static void submitFlatItem(CompatItemRenderState item, boolean is3d, float height,
                                       PoseStack ps, SubmitSink col, int light) {
        ps.pushPose();
        ps.translate(0.5F, height, 0.5F);
        if (!is3d) {
            ps.mulPose(Axis.YP.rotationDegrees(-180F));
            ps.scale(0.5F, 0.5F, 0.5F);
        } else {
            ps.translate(0F, -0.352F, 0F);
        }
        float scale = 24F / 16F;
        ps.scale(scale, scale, scale);
        ps.mulPose(Axis.XP.rotationDegrees(90F));
        col.submitItem(ps, item, light, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }
}
