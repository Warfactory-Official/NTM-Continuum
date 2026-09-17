package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType;
import com.hbm.entity.projectile.EntityZirnoxDebris;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;

/** Renders the six physical debris variants ejected by a ZIRNOX meltdown. */
public class ZirnoxDebrisRenderer extends dev.golem.core.render.gem.GemEntityRenderer<EntityZirnoxDebris, ZirnoxDebrisRenderState> {

    private static final float DIAGONAL_AXIS = 0.57735026F;
    private final Geometry[] meshes = new Geometry[DebrisType.values().length];
    private final Surface[] skins = new Surface[DebrisType.values().length];

    public ZirnoxDebrisRenderer(EntityRendererProvider.Context context) {
        super(context);
        register(DebrisType.BLANK, "zirnox/deb_blank.obj", "models/machines/zirnox.png");
        register(DebrisType.ELEMENT, "zirnox/deb_element.obj", "models/zirnox/zirnox_deb_element.png");
        register(DebrisType.SHRAPNEL, "zirnox/deb_shrapnel.obj", "models/machines/zirnox.png");
        register(DebrisType.GRAPHITE, "projectiles/deb_graphite.obj", "blocks/block_graphite.png");
        register(DebrisType.CONCRETE, "zirnox/deb_concrete.obj", "models/machines/zirnox_destroyed.png");
        register(DebrisType.EXCHANGER, "zirnox/deb_exchanger.obj", "models/machines/zirnox.png");
    }

    private void register(DebrisType type, String model, String texture) {
        meshes[type.ordinal()] = WavefrontObj.load(
                dev.golem.core.Ids.of(Constants.MOD_ID, "models/" + model)).all(true);
        skins[type.ordinal()] = Surface.cutout(Constants.MOD_ID + ":textures/" + texture);
    }

    /** One debris-shaped slot holding whichever of the six pieces this one is, tumbling about a diagonal. */
    @Override
    protected void setup(Gem.Model<ZirnoxDebrisRenderState> declaration) {
        declaration.part(meshes[0], skins[0])
                .mesh(state -> meshes[state.type == null ? 0 : state.type.ordinal()])
                .skin(state -> skins[state.type == null ? 0 : state.type.ordinal()])
                .visibleIf(state -> state.type != null)
                .translate(0.0F, 0.125F, 0.0F)
                .angle(Gem.Axes.Y, state -> Math.floorMod(state.entityId, 360))
                .pose(state -> state.tumble);
    }

    @Override
    public ZirnoxDebrisRenderState createRenderState() {
        return new ZirnoxDebrisRenderState();
    }

    @Override
    public void extract(EntityZirnoxDebris entity, ZirnoxDebrisRenderState state, float partialTicks) {
        state.type = entity.getDebrisType();
        state.entityId = entity.getId();
        state.rotation = entity.lastRot + (entity.rot - entity.lastRot) * partialTicks;
        state.tumble.rotation(new Quaternionf().rotationAxis((float) Math.toRadians(state.rotation),
                DIAGONAL_AXIS, DIAGONAL_AXIS, DIAGONAL_AXIS));
    }

}
