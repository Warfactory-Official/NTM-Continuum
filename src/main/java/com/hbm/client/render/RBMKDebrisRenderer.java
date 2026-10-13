package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Tumbling, type-specific RBMK column fragment. */
public final class RBMKDebrisRenderer extends GemEntityRenderer<EntityRBMKDebris, RBMKDebrisRenderer.State> {
    private static final float DIAG = 0.57735026F;
    private static final WavefrontObj[] MODELS = {
            model("deb_blank"), model("deb_element"), model("deb_fuel"), model("deb_rod"), model("deb_graphite"), model("deb_lid")};
    private static final Identifier[] TEXTURES = {
            texture("blank"), texture("element"), texture("fuel"), texture("control"), texture("graphite"), texture("lid")};

    public RBMKDebrisRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = .5F; }

    /** One debris-shaped slot holding whichever of the six pieces this one is, tumbling about a diagonal. */
    @Override protected void setup(Gem.Model<State> declaration) {
        Geometry[] meshes = new Geometry[MODELS.length];
        Surface[] skins = new Surface[MODELS.length];
        for (int i = 0; i < MODELS.length; i++) {
            meshes[i] = MODELS[i].all();
            skins[i] = Surface.cutout(TEXTURES[i].toString());
        }
        declaration.part(meshes[1], skins[1])
                .mesh(state -> meshes[state.type])
                .skin(state -> skins[state.type])
                .translate(0F, .125F, 0F)
                .angle(Gem.Axes.Y, state -> state.spin)
                .pose(state -> state.tumble);
    }

    @Override public State createRenderState() { return new State(); }
    @Override public void extract(EntityRBMKDebris debris, State state, float partial) {
        state.type = switch (debris.getDebrisType()) { case BLANK -> 0; case ELEMENT -> 1; case FUEL -> 2; case ROD -> 3; case GRAPHITE -> 4; case LID -> 5; };
        state.rot = Mth.lerp(partial, debris.lastRot, debris.rot);
        state.spin = Math.floorMod(debris.getId(), 360);
        state.tumble.rotation(new Quaternionf().rotationAxis((float) Math.toRadians(state.rot), DIAG, DIAG, DIAG));
    }
    private static WavefrontObj model(String name) { return WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/projectiles/" + name + ".obj")); }
    private static Identifier texture(String name) { return Ids.of(Constants.MOD_ID, "textures/entity/rbmk_debris/" + name + ".png"); }
    public static final class State extends CompatEntityRenderState {
        int type = 1;
        int spin;
        float rot;
        final Matrix4f tumble = new Matrix4f();
    }
}
