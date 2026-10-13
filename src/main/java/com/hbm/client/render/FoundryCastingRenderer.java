package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityFoundryBasin;
import com.hbm.tileentity.machine.TileEntityFoundryCastingBase;
import com.hbm.tileentity.machine.TileEntityFoundryMold;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
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
public class FoundryCastingRenderer<T extends TileEntityFoundryCastingBase> extends GemBER<T, FoundryCastingRenderer.State> {

    private static final Geometry MELT = Quads.named(Constants.MOD_ID + ":foundry_casting_melt")
            .normal(0F, 1F, 0F)
            .vertex(0.125F, 0F, 0.125F, 0F, 0F).vertex(0.125F, 0F, 0.875F, 0F, 1F)
            .vertex(0.875F, 0F, 0.875F, 1F, 1F).vertex(0.875F, 0F, 0.125F, 1F, 0F)
            .build();

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
    /** material name -> the surface it wears (multi-texture OBJ, resolved from the .mtl). */
    private final Map<String, Surface> materials = new LinkedHashMap<>();
    private final Surface melt = Surface.emissive(
            Constants.MOD_ID + ":textures/models/machines/lava_gray.png");

    /** @param bodyObjPath OBJ body to draw (basin), or null for JSON-modelled blocks (mold caster). */
    public FoundryCastingRenderer(@Nullable String bodyObjPath) {
        if (bodyObjPath != null) {
            this.body = WavefrontObj.load(Ids.of(Constants.MOD_ID, bodyObjPath));
            var mtl = WavefrontObj.loadMtl(Ids.of(Constants.MOD_ID, bodyObjPath.replace(".obj", ".mtl")));
            for (String mat : body.materialNames()) {
                Identifier tex = mtl.get(mat);
                if (tex != null) materials.put(mat, Surface.cutout(tex.toString()));
            }
        } else {
            this.body = null;
        }
    }

    /**
     * The basin's OBJ splits its runs by {@code usemtl} rather than by group, so it is one part per
     * material. The mold caster has no OBJ at all and declares nothing. The vertices are in 0..1 corner
     * space (the forge_marker convention), so nothing needs centring.
     */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        if (body != null) {
            for (var material : materials.entrySet()) {
                declaration.part(body.material(material.getKey()), material.getValue());
            }
        }
        declaration.part(MELT, melt)
                .visibleIf(state -> state.hasMelt)
                .slide(Gem.Axes.Y, state -> state.meltY)
                .tint(state -> 0xFF000000 | (state.meltColor & 0xFFFFFF))
                .light(state -> FoundryRenderHelper.FULLBRIGHT);
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

    /** The mold, the cast output and the melt surface are items and a fill-level quad, not meshes. */
    @Override
    protected void submitUndeclared(State s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;

        if (s.showMold) submitFlatItem(s.moldItem, false, s.moldHeight, ps, col, light);
        if (s.showOutput) submitFlatItem(s.outputItem, s.outputIs3d, s.outHeight, ps, col, light);
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
