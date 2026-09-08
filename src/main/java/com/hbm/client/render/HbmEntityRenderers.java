package com.hbm.client.render;

import com.hbm.entity.ModEntities;
import com.hbm.entity.ModRBMKEntities;
import com.hbm.entity.missile.ModMissileEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

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
        sink.register(ModEntities.NUKE_TOREX.get(), TorexRenderer::new);
        sink.register(ModEntities.ZIRNOX_DEBRIS.get(), ZirnoxDebrisRenderer::new);
        sink.register(ModEntities.SAWBLADE.get(), SawbladeRenderer::new);
        // Bullets are invisible for now: upstream draws them through ~30 per-ammo tracer styles in
        // LegoClient, which is a follow-up. A renderer must still be registered or the client throws
        // the moment one spawns.
        sink.register(ModEntities.BULLET_MK4.get(), NoopRenderer::new);
        sink.register(ModRBMKEntities.RBMK_DEBRIS.get(), RBMKDebrisRenderer::new);
        sink.register(ModRBMKEntities.SPEAR.get(), RenderSpear::new);

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
    }

    private static <T extends Entity & com.hbm.entity.missile.IMissilePitch> void missile(Sink sink, EntityType<T> type, String model, String texture, float scale) {
        sink.register(type, context -> new MissileRenderer<>(context, model, texture, scale));
    }
}
