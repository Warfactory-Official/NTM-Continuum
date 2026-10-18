package com.hbm.client.render;

import com.hbm.client.ClientEffects;
import com.hbm.client.render.turret.TurretResources;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The rocket in flight: the launcher's own rocket meshes, flown nose-first, wearing whichever skin the
 * loaded pod carries.
 *
 * <p>Both the mesh and the skin come off the rocket type, so the model declares one slot and lets the
 * state fill it. That is one part that moves between buckets rather than a part per type with all but
 * one of them hidden.
 */
public class RenderArtilleryRocket
        extends GemEntityRenderer<EntityArtilleryRocket, RenderArtilleryRocket.State> {

    /** Indexed by {@link HIMARSRocketType#modelType}: 0 is the sixfold pod, 1 the single rocket. */
    private static final Geometry[] MESHES = {
            TurretResources.HIMARS.part("RocketStandard"),
            TurretResources.HIMARS.part("RocketSingle"),
    };

    private static final Map<HIMARSRocketType, Surface> SKINS = skins();

    private static Map<HIMARSRocketType, Surface> skins() {
        Map<HIMARSRocketType, Surface> built = new EnumMap<>(HIMARSRocketType.class);
        for (HIMARSRocketType type : HIMARSRocketType.values()) {
            built.put(type, Surface.cutout(type.texture.toString()));
        }
        return built;
    }

    private static Geometry meshOf(HIMARSRocketType type) {
        return MESHES[Math.floorMod(type.modelType, MESHES.length)];
    }

    public RenderArtilleryRocket(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.part(meshOf(State.DEFAULT), SKINS.get(State.DEFAULT))
                .mesh(state -> meshOf(state.rocket))
                .skin(state -> SKINS.get(state.rocket))
                .angle(Gem.Axes.Y, state -> state.yaw - 90F)
                .angle(Gem.Axes.Z, state -> state.pitch - 90F)
                .rotate(Gem.Axes.Y, 90F)
                .rotate(Gem.Axes.X, 90F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityArtilleryRocket rocket, State state, float partialTicks) {
        state.yaw = Mth.rotLerp(partialTicks, rocket.yRotO, rocket.getYRot());
        state.pitch = Mth.lerp(partialTicks, rocket.xRotO, rocket.getXRot());
        state.rocket = rocket.getRocket();
        trail(rocket, partialTicks);
    }

    /** 1.7 {@code onRenderTick}: a kerosene contrail puff every four blocks of the path drawn since the last frame. */
    private static void trail(EntityArtilleryRocket rocket, float interp) {
        double x = Mth.lerp(interp, rocket.xOld, rocket.getX());
        double y = Mth.lerp(interp, rocket.yOld, rocket.getY());
        double z = Mth.lerp(interp, rocket.zOld, rocket.getZ());

        if (rocket.lastRenderX == 0) rocket.lastRenderX = x;
        if (rocket.lastRenderY == 0) rocket.lastRenderY = y;
        if (rocket.lastRenderZ == 0) rocket.lastRenderZ = z;

        // 1.7 measures from the last frame to the entity's tick position, not to the interpolated one
        Vec3 delta = new Vec3(rocket.lastRenderX - rocket.getX(), rocket.lastRenderY - rocket.getY(),
                rocket.lastRenderZ - rocket.getZ());
        rocket.trailPath += delta.length();
        delta = delta.normalize();

        for (; rocket.trailPath >= 4D; rocket.trailPath -= 4D) {
            ClientEffects.spawnContrail(rocket.level(), x - delta.x * rocket.trailPath, y - delta.y * rocket.trailPath,
                    z - delta.z * rocket.trailPath, ClientEffects.Contrail.KEROSENE);
        }

        rocket.lastRenderX = x;
        rocket.lastRenderY = y;
        rocket.lastRenderZ = z;
    }

    public static final class State extends CompatEntityRenderState {

        /** The mesh and skin choosers must always answer, so the state starts on a real type. */
        static final HIMARSRocketType DEFAULT = HIMARSRocketType.values()[0];

        public float yaw;
        public float pitch;
        public HIMARSRocketType rocket = DEFAULT;
    }
}
