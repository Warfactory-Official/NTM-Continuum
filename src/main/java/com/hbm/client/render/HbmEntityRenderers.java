package com.hbm.client.render;

import com.hbm.entity.ModEntities;
import com.hbm.entity.ModRBMKEntities;
import com.hbm.entity.missile.ModMissileEntities;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.gem.GemEntityRenderer;
import java.util.function.Function;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import com.hbm.client.render.projectile.BeamProjectileRenderer;
import com.hbm.client.render.projectile.BulletProjectileRenderer;
import com.hbm.client.render.projectile.ChemicalProjectileRenderer;
import com.hbm.client.render.projectile.CoinProjectileRenderer;
import com.hbm.client.render.projectile.RenderBeamBomb;

/**
 * Cross-loader registration of HBM's entity renderers. Common defines the wiring; each loader passes a
 * {@link Sink} that performs the actual registration (NeoForge via
 * {@code EntityRenderersEvent.RegisterRenderers#registerEntityRenderer}, Fabric via
 * {@code EntityRendererRegistry.register}). Client-only.
 *
 * <p>The nuke explosion entity is invisible, so it uses vanilla's {@code NoopRenderer} — every entity
 * type still needs <em>a</em> renderer registered or the client errors when one spawns.
 */
public final class HbmEntityRenderers {

    private HbmEntityRenderers() {}

    /** Loader-provided registration callback. */
    public interface Sink {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    public static void registerAll(Sink sink) {
        sink.register(ModEntities.NUKE_EXPLOSION_MK5.get(), NoopRenderer::new);
        sink.register(ModEntities.ENTITY_EMP.get(), NoopRenderer::new);
        sink.register(ModEntities.FALLOUT_RAIN.get(), NoopRenderer::new);
        sink.register(ModEntities.FIRE_LINGERING.get(), NoopRenderer::new);
        sink.register(ModEntities.MIST.get(), NoopRenderer::new);
        regGem(sink, ModEntities.NUKE_TOREX.get(), TorexRenderer::new);
        regGem(sink, ModEntities.ZIRNOX_DEBRIS.get(), ZirnoxDebrisRenderer::new);
        regGem(sink, ModEntities.SAWBLADE.get(), SawbladeRenderer::new);
        // The per-ammo LegoClient styles are resolved by the client-owned ID registry.
        regGem(sink, ModEntities.BULLET_MK4.get(), BulletProjectileRenderer::new);
        regGem(sink, ModEntities.BULLET_MK4_CL.get(), BulletProjectileRenderer::new);
        regGem(sink, ModEntities.CHEMICAL.get(), ChemicalProjectileRenderer::new);
        sink.register(ModEntities.BEAM_BOMB.get(), RenderBeamBomb::new);
        regGem(sink, ModEntities.BEAM_MK4.get(), BeamProjectileRenderer::new);
        regGem(sink, com.hbm.entity.NtmToolEntityCatalog.COIN.get(), CoinProjectileRenderer::new);
        regGem(sink, ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
        regGem(sink, ModEntities.VORTEX.get(), BlackHoleRenderer::new);
        regGem(sink, ModEntities.RAGING_VORTEX.get(), BlackHoleRenderer::new);
        sink.register(ModEntities.RUBBLE.get(), HbmEntityRenderers::rubbleRenderer);
        // SHRAPNEL: bound with the ntm-next port's RenderShrapnel below.
        regGem(sink, ModEntities.HIMARS_ROCKET.get(), RenderArtilleryRocket::new);
        regGem(sink, ModEntities.ARTILLERY_SHELL.get(), RenderArtilleryShell::new);
        regGem(sink, ModRBMKEntities.RBMK_DEBRIS.get(), RBMKDebrisRenderer::new);
        regGem(sink, ModRBMKEntities.SPEAR.get(), RenderSpear::new);

        // Tier 0: both bodies use the old missileTaint OBJ, at the old 2x entity scale.
        missile(sink, ModMissileEntities.MISSILE_MICRO.get(), "missile_taint.obj", "missile_micro.png", 2.0F);
        missile(sink, ModMissileEntities.MISSILE_TAINT.get(), "missile_taint.obj", "missile_micro_taint.png", 2.0F);

        // The missiles. Model, texture and scale per airframe, exactly as upstream's 24 one-off
        // renderer classes: V2 and strong at 1.0, huge at 0.8, atlas at 1.5, stealth at 2.0.
        missile(sink, ModMissileEntities.MISSILE_GENERIC.get(), "missile_v2.obj", "missile_v2.png", 1.0F);
        missile(sink, ModMissileEntities.MISSILE_DECOY.get(), "missile_v2.obj", "missile_v2_decoy.png", 1.0F);
        missile(sink, ModMissileEntities.MISSILE_INCENDIARY.get(), "missile_v2.obj", "missile_v2_inc.png", 1.0F);
        missile(sink, ModMissileEntities.MISSILE_BUSTER.get(), "missile_v2.obj", "missile_v2_bu.png", 1.0F);
        missile(sink, ModMissileEntities.MISSILE_STRONG.get(), "missile_strong.obj", "missile_strong.png", 1.0F);
        missile(sink, ModMissileEntities.MISSILE_INCENDIARY_STRONG.get(), "missile_strong.obj", "missile_strong_inc.png", 1.0F);
        missile(sink, ModMissileEntities.MISSILE_BUSTER_STRONG.get(), "missile_strong.obj", "missile_strong_bu.png", 1.0F);
        missile(sink, ModMissileEntities.MISSILE_EMP_STRONG.get(), "missile_strong.obj", "missile_strong_emp.png", 1.0F);
        missile(sink, ModMissileEntities.MISSILE_BURST.get(), "missile_huge.obj", "missile_huge.png", 0.8F);
        missile(sink, ModMissileEntities.MISSILE_INFERNO.get(), "missile_huge.obj", "missile_huge_inc.png", 0.8F);
        missile(sink, ModMissileEntities.MISSILE_NUCLEAR.get(), "missile_atlas.obj", "missile_atlas_nuclear.png", 1.5F);
        missile(sink, ModMissileEntities.MISSILE_MIRV.get(), "missile_atlas.obj", "missile_atlas_thermo.png", 1.5F);
        missile(sink, ModMissileEntities.MISSILE_DOOMSDAY.get(), "missile_atlas.obj", "missile_atlas_doomsday.png", 1.5F);
        missile(sink, ModMissileEntities.MISSILE_DOOMSDAY_RUSTED.get(), "missile_atlas.obj", "missile_atlas_doomsday_weathered.png", 1.5F);
        // Upstream's file is camelCase (missileN2.png); modern Identifiers reject uppercase, so the copy
        // in this tree is renamed to missile_n2.png.
        missile(sink, ModMissileEntities.MISSILE_N2.get(), "missile_atlas.obj", "missile_n2.png", 1.5F);
        missile(sink, ModMissileEntities.MISSILE_STEALTH.get(), "missile_stealth.obj", "missile_stealth.png", 2.0F);
        missile(sink, ModMissileEntities.MISSILE_ANTI_BALLISTIC.get(), "missile_abm.obj", "missile_abm.png", 1.0F);

        com.hbm.client.render.entity.NtmEntityRenderers.registerAll(sink);
    }

    /**
     * Every {@code hbm:} entity type must have a renderer bound: on 26.1 the dispatcher dereferences a missing
     * one and takes the render thread down the frame the entity enters view, and below it the entity is silently
     * invisible. Loaders call this after {@link #registerAll} with the set of types they saw registered. Missing
     * types fail loudly in a development run and are logged otherwise.
     */
    public static void verifyCoverage(java.util.Set<EntityType<?>> bound) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (EntityType<?> type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            String id = EntityType.getKey(type).toString();
            if (id.startsWith(com.hbm.Constants.MOD_ID + ":") && !bound.contains(type)) missing.add(id);
        }
        if (missing.isEmpty()) {
            com.hbm.Constants.LOG.info("Entity renderers: every hbm entity type has a renderer ({} bound)", bound.size());
            return;
        }
        String message = missing.size() + " hbm entity types have no renderer: " + String.join(", ", missing);
        if (dev.golem.core.platform.Platform.isDevelopment()) throw new IllegalStateException(message);
        com.hbm.Constants.LOG.error(message);
    }

    /** A sink that records what it binds, for {@link #verifyCoverage}. */
    public static Sink recording(Sink downstream, java.util.Set<EntityType<?>> bound) {
        return new Sink() {
            @Override
            public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                bound.add(type);
                downstream.register(type, provider);
            }
        };
    }

    private static <T extends Entity & com.hbm.entity.missile.IMissilePitch> void missile(Sink sink, EntityType<T> type, String model, String texture, float scale) {
        regGem(sink, type, context -> new MissileRenderer<>(context, model, texture, scale));
    }

    /**
     * Registers a renderer that declares its model, and builds that declaration at creation.
     *
     * <p>A part naming a mesh group the model does not have throws, and this is the moment where that
     * is a startup failure naming the renderer rather than an entity that quietly draws nothing.
     */
    public static <T extends Entity, S extends CompatEntityRenderState, R extends GemEntityRenderer<T, S>>
            void regGem(Sink sink, EntityType<? extends T> type, Function<EntityRendererProvider.Context, R> provider) {
        sink.register(type, context -> {
            R renderer = provider.apply(context);
            renderer.gem();
            return renderer;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static net.minecraft.client.renderer.entity.EntityRenderer rubbleRenderer(
            EntityRendererProvider.Context context) {
        return new FallingBlockRenderer(context);
    }
}
