package com.hbm.client.render.projectile;

import com.hbm.Constants;
import com.hbm.client.render.BalefireGlint;
import com.hbm.client.render.BeamPronter;
import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.client.Colors;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Geometry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Client-owned source {@code GunFactoryClient} tracer and beam lookup tables. */
public final class TracerRegistry {
    private static final Identifier WHITE = Ids.of("hbm", "textures/models/misc/white.png");
    private static final Identifier FLARE = Ids.of("hbm", "textures/particle/flare.png");
    private static final Identifier FRESNEL = Ids.of("hbm", "textures/models/bfg/fresnel_ms.png");
    private static final Map<Identifier, BulletTracer> BULLETS = new HashMap<>();
    private static final Map<Identifier, BeamTracer> BEAMS = new HashMap<>();
    private static final Map<Identifier, WavefrontObj> MODELS = new HashMap<>();
    private static final Set<Identifier> MISSING_REPORTED = new HashSet<>();
    /** Explicit source omissions and styles whose source assets are not present in this port. */
    private static final Set<Identifier> SOURCE_UNRENDERED = Set.of(
            id("ammo_debug_shot"), id("dgk_normal"), id("fext_water"), id("fext_foam"), id("fext_sand"),
            id("flame_nograv"), id("flame_nograv_bf"), id("r556_inc_sp"), id("r556_inc_fmj"),
            id("r556_inc_jhp"), id("r556_inc_ap"),
            id("flame_diesel"), id("flame_gas"),
            id("flame_napalm"), id("flame_balefire"), id("flame_topaz_diesel"),
            id("flame_topaz_gas"), id("flame_topaz_napalm"), id("flame_topaz_balefire"),
            id("flame_daybreaker_diesel"), id("flame_daybreaker_gas"), id("flame_daybreaker_napalm"),
            id("flame_daybreaker_balefire"), id("rocket_he"),
            id("rocket_heat"), id("rocket_demo"), id("rocket_inc"), id("rocket_phosphorus"));
    private static boolean initialized;

    private TracerRegistry() {}

    private static Identifier id(String path) { return Ids.of("hbm", path); }

    public interface BulletTracer {
        void draw(BulletProjectileRenderer.State state, PoseStack pose, SubmitSink sink);
        default void extract(EntityBulletBaseMK4 bullet, BulletProjectileRenderer.State state, float partialTicks) {}
    }

    public interface BeamTracer {
        void draw(BeamProjectileRenderer.State state, PoseStack pose, SubmitSink sink);
    }

    public static BulletTracer bullet(Identifier id) {
        if (id == null) return null;
        BulletTracer tracer = BULLETS.get(id);
        if (tracer == null) reportMissing("bullet", id);
        return tracer;
    }
    public static BeamTracer beam(Identifier id) {
        if (id == null) return null;
        BeamTracer tracer = BEAMS.get(id);
        if (tracer == null) reportMissing("beam", id);
        return tracer;
    }

    private static void reportMissing(String kind, Identifier id) {
        if (MISSING_REPORTED.add(id)) {
            if (SOURCE_UNRENDERED.contains(id)) {
                Constants.LOG.warn("No usable {} tracer is available for {} (explicitly unavailable source style)", kind, id);
            } else {
                Constants.LOG.error("Missing 1.7 {} tracer for {}", kind, id);
            }
        }
    }

    /** Public seam for factories that are initialized after client setup. */
    public static void setBulletRenderer(Identifier id, BulletTracer renderer) { if (id != null) BULLETS.put(id, renderer); }
    public static void setBeamRenderer(Identifier id, BeamTracer renderer) { if (id != null) BEAMS.put(id, renderer); }

    public static void init() {
        if (initialized) return;
        initialized = true;

        BulletTracer standard = ribbon(0xFFBF00, 0xFFFFFF, false);
        BulletTracer flechette = ribbon(0x8C8C8C, 0xCACACA, false);
        BulletTracer ap = ribbon(0xFF6A00, 0xFFE28D, false);
        BulletTracer express = ribbon(0x9E082E, 0xFF8A79, false);
        BulletTracer du = ribbon(0x5CCD41, 0xE9FF8D, false);
        BulletTracer he = ribbon(0xD8CA00, 0xFFF19D, true);
        BulletTracer smoke = ribbon(0x42A8DD, 0xFFFFFF, true);
        BulletTracer black = ribbon(0x000000, 0x7F006E, true);
        BulletTracer legendary = ribbon(0x7F006E, 0xFF7FED, true);

        // Keep these groups explicit. The old GunFactoryClient assigns the renderer per config;
        // overlapping colour groups silently changed material and geometry in the previous table.
        register(standard, "ammo_debug", "ammo_debug_shot", "stone", "stone_ap", "stone_iron", "stone_shot",
                "m357_bp", "m357_sp", "m357_fmj", "m357_jhp", "m44_bp", "m44_sp", "m44_fmj", "m44_jhp",
                "p22_sp", "p22_fmj", "p22_jhp", "p9_sp", "p9_fmj", "p9_jhp", "p45_sp", "p45_fmj", "p45_jhp",
                "r556_sp", "r556_fmj", "r556_jhp", "r762_sp", "r762_fmj", "r762_jhp", "bmg50_sp", "bmg50_fmj",
                "bmg50_jhp", "g12_bp", "g12_bp_magnum", "g12_bp_slug", "g12", "g12_slug", "g12_magnum",
                "g12_sub", "g12_sub_slug", "g12_sub_magnum", "g10", "g10_shrapnel", "g10_slug");
        register(flechette, "g12_flechette", "g12_sub_flechette");
        register(ap, "m357_ap", "m44_ap", "p9_ap", "p22_ap", "r556_ap", "r762_ap", "bmg50_ap", "p45_ap",
                "b75", "b75_inc", "coil_tungsten", "coil_ferrouranium", "g12_phosphorus",
                "g12_sub_phosphorus", "g10_du");
        register(express, "m357_express", "m44_express", "g12_explosive", "g12_sub_explosive", "b75_exp");
        register(du, "p45_du", "r762_du", "bmg50_du");
        register(he, "g10_explosive", "bmg50_he", "r762_he");
        register(streak(), "g10_jade");
        register(smoke, "bmg50_sm");
        register(black, "bmg50_black");
        register(legendary, "m44_equestrian_pip", "m44_equestrian_mn7", "g12_equestrian_bj", "g12_equestrian_tkr",
                "bmg50_equestrian");
        // 1.7 LegoClient.RENDER_BIG_NUKE (GunFactoryClient: folly_nuke): scale 0.5, rotate 90 on Z, rocket_mirv skin.
        register(model("models/projectiles/projectiles.obj", "textures/models/projectiles/rocket_mirv.png", "MissileMIRV", 0.5F,
                pose -> pose.mulPose(Axis.ZP.rotationDegrees(90F))), "folly_nuke");
        register(model("models/projectiles/projectiles.obj", "textures/models/projectiles/grenade.png", "Grenade", 0.25F,
                pose -> pose.mulPose(Axis.ZP.rotationDegrees(90F))), "nuke_tots", "g40_he", "g40_heat", "g40_demo",
                "g40_inc", "g40_phosphorus", "shell_normal", "shell_explosive", "shell_ap", "shell_du", "shell_w9");
        register(model("models/weapons/fatman.obj", "textures/models/weapons/fatman_mininuke.png", "MiniNuke", 0.125F,
                pose -> { pose.mulPose(Axis.YN.rotationDegrees(90F)); pose.translate(0F, -1F, 1F); }),
                "nuke_standard", "nuke_demo", "nuke_high");
        register(model("models/weapons/fatman.obj", "textures/models/weapons/fatman_submunition.png", "MiniNuke", 0.0625F,
                pose -> { pose.mulPose(Axis.YN.rotationDegrees(90F)); pose.translate(0F, -1F, 1F); }), "cluster_submunition");
        register(rocketModel("models/weapons/panzerschreck.obj", "textures/models/weapons/panzerschreck.png", "Rocket", 0.125F,
                pose -> { pose.mulPose(Axis.YN.rotationDegrees(90F)); pose.translate(0F, 0F, 3.5F); }), "nuke_hive");
        // 1.7 LegoClient.RENDER_NUKE_BALEFIRE: the shell through ItemRenderFatMan.renderBalefire, glint included
        register(balefireNuke(), "nuke_balefire");
        register(model("models/weapons/charge_thrower.obj", "textures/models/weapons/charge_thrower_mortar.png", "Mortar", 0.125F,
                pose -> { pose.mulPose(Axis.YN.rotationDegrees(90F)); pose.mulPose(Axis.ZP.rotationDegrees(180F)); pose.translate(0F, 0F, -6F); }),
                "ct_mortar", "ct_mortar_charge");
        register(model("models/weapons/charge_thrower.obj", "textures/models/weapons/charge_thrower_hook.png", "Hook", 0.125F,
                pose -> { pose.mulPose(Axis.YN.rotationDegrees(90F)); pose.mulPose(Axis.ZP.rotationDegrees(180F)); pose.translate(0F, 0F, -6F); }), "ct_hook");
        register(model("models/weapons/panzerschreck.obj", "textures/models/weapons/panzerschreck.png", "Rocket", 0.125F,
                pose -> { pose.mulPose(Axis.YN.rotationDegrees(90F)); pose.translate(0F, 0F, 3.5F); }), "rocket_rpzb_he",
                "rocket_rpzb_heat", "rocket_rpzb_demo", "rocket_rpzb_inc", "rocket_rpzb_phosphorus",
                "rocket_ncrpa_he", "rocket_ncrpa_heat", "rocket_ncrpa_demo", "rocket_ncrpa_inc", "rocket_ncrpa_phosphorus",
                "rocket_ncrpa_steer_he", "rocket_ncrpa_steer_heat", "rocket_ncrpa_steer_demo", "rocket_ncrpa_steer_inc",
                "rocket_ncrpa_steer_phosphorus");
        register(rocketModel("models/weapons/missile_launcher.obj", "textures/models/weapons/missile_launcher.png", "Missile", 0.25F,
                pose -> { pose.mulPose(Axis.YN.rotationDegrees(90F)); pose.translate(0F, -1F, -4.5F); }), "rocket_ml_he",
                "rocket_ml_heat", "rocket_ml_demo", "rocket_ml_inc", "rocket_ml_phosphorus");
        register(rocketModel("models/projectiles/projectiles.obj", "textures/models/projectiles/rocket.png", "Rocket", 1F,
                pose -> pose.mulPose(Axis.ZP.rotationDegrees(90F))), "rocket_qd_he", "rocket_qd_heat",
                "rocket_qd_demo", "rocket_qd_inc", "rocket_qd_phosphorus");
        register(model("models/projectiles/deb_graphite.obj", "textures/entity/rbmk_debris/graphite.png",
                "Cube_Cube.001", 2F, pose -> { }), "pile_debris");
        BulletTracer flare = flare(1F, 0.5F, 0.5F);
        register(flare, "g26_flare");
        register(flare(0.5F, 0.5F, 1F), "g26_flare_supply");
        register(flare(0.5F, 1F, 0.5F), "g26_flare_weapon");

        BeamTracer cyan = laser(0x15, 0x15, 0x80);
        BeamTracer purple = laser(0x60, 0x15, 0x80);
        BeamTracer red = laser(0x80, 0x15, 0x15);
        BeamTracer emerald = laser(0x15, 0x80, 0x15);
        BeamTracer ni4ni = ni4niBolt();
        registerBeam(cyan, "g12_shredder", "g12_shredder_slug", "g12_shredder_flechette", "g12_shredder_magnum",
                "g12_shredder_explosive", "g12_shredder_phosphorus");
        registerBeam(purple, "energy_lacunae", "energy_lacunae_overcharge", "energy_lacunae_ir");
        registerBeam(red, "energy_las", "energy_las_overcharge", "energy_las_ir");
        registerBeam(emerald, "energy_emerald", "energy_emerald_overcharge", "energy_emerald_ir");
        registerBeam(ni4ni, "ni4ni_arc");
        registerBeam(tesla(false), "energy_tesla", "energy_tesla_overcharge", "energy_tesla_ir");
        // 1.7 GunFactoryClient: setRendererBulkBeam(RENDER_LIGHTNING_SUB, energy_tesla_ir_sub, TileEntityBatterySocket.discharge)
        registerBeam(tesla(true), "energy_tesla_ir_sub", "battery_socket_discharge");
        registerBeam(endpointMaterial(0xE3D692, 0xFFFFFF), "p35800");
        registerBeam(endpointMaterial(0x4C3093, 0x000000), "p35800_bl");
        registerBeam(tau(false), "tau_uranium");
        registerBeam(tau(true), "tau_uranium_charge");
        registerBeam(folly(), "folly_sm");
        audit();
    }

    /** Compares every registered config with the source client table before a renderer can fail silently. */
    public static void audit() {
        int rendered = 0;
        int intentional = 0;
        int foreign = 0;
        List<Identifier> missing = new ArrayList<>();
        for (BulletConfig config :
                BulletConfigs.all()) {
            Identifier id = config.id;
            if (id == null) {
                Constants.LOG.error("Bullet config has no registry id and cannot have a tracer: {}", config);
                continue;
            }
            boolean beam = config.pType == BulletConfig.ProjectileType.BEAM;
            boolean mapped = beam ? BEAMS.containsKey(id) : BULLETS.containsKey(id);
            if (mapped) {
                rendered++;
            } else if (SOURCE_UNRENDERED.contains(id)) {
                intentional++;
                reportMissing(beam ? "beam" : "bullet", id);
            } else if (!id.getNamespace().equals(Constants.MOD_ID)) {
                // An addon's round: the audit pins HBM's own client table against 1.7's GunFactoryClient, an addon's
                // tracers are its own business and may be set after this runs (setBulletRenderer/setBeamRenderer).
                foreign++;
            } else {
                reportMissing(beam ? "beam" : "bullet", id);
                missing.add(id);
            }
        }
        Constants.LOG.info("Projectile tracer audit: {} mapped, {} source-unrendered, {} addon round(s) not audited",
                rendered, intentional, foreign);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing 1.7 projectile tracers: " + missing);
        }
    }

    private static void register(BulletTracer tracer, String... ids) {
        for (String id : ids) setBulletRenderer(Ids.of("hbm", id), tracer);
    }

    private static void registerBeam(BeamTracer tracer, String... ids) {
        for (String id : ids) setBeamRenderer(Ids.of("hbm", id), tracer);
    }

    /** One tracer for several rounds, any namespace (an addon's {@code BulletConfigs.register(Identifier, …)} ids). */
    public static void register(BulletTracer tracer, Identifier... ids) {
        for (Identifier id : ids) setBulletRenderer(id, tracer);
    }

    /** One beam for several rounds, any namespace. */
    public static void registerBeam(BeamTracer tracer, Identifier... ids) {
        for (Identifier id : ids) setBeamRenderer(id, tracer);
    }

    private static BulletTracer model(String modelId, String textureId, String part, float scale,
            Consumer<PoseStack> transform) {
        return model(Ids.of("hbm", modelId), Ids.of("hbm", textureId), part, scale, transform);
    }

    /**
     * 1.7's model tracer ({@code GunFactoryClient} {@code RENDER_*} model styles): one OBJ part, scaled by {@code scale}
     * after {@code transform}, textured cutout, lit by the projectile. Public with full ids so an addon's rounds can
     * use their own models.
     */
    public static BulletTracer model(Identifier model, Identifier texture, String part, float scale,
            Consumer<PoseStack> transform) {
        return (state, pose, sink) -> {
            pose.pushPose();
            pose.scale(scale, scale, scale);
            transform.accept(pose);
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(texture),
                    (p, buffer) -> model(model).renderPart(part, p, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
            pose.popPose();
        };
    }

    /**
     * 1.7 {@code RENDER_NUKE_BALEFIRE}: scale 0.125, -90 about Y, (0, -1, 1), then the balefire mini-nuke and its glint
     * drawn over the same triangles ({@link com.hbm.client.render.BalefireGlint}).
     */
    private static BulletTracer balefireNuke() {
        Identifier modelId = Ids.of("hbm", "models/weapons/fatman.obj");
        Identifier texture = Ids.of("hbm", "textures/models/weapons/fatman_balefire.png");
        return (state, pose, sink) -> {
            Geometry nuke = model(modelId).part("MiniNuke");
            pose.pushPose();
            pose.scale(0.125F, 0.125F, 0.125F);
            pose.mulPose(Axis.YN.rotationDegrees(90F));
            pose.translate(0F, -1F, 1F);
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(texture),
                    (p, buffer) -> nuke.draw(p, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1));
            BalefireGlint.fatMan(pose, sink.order(1), nuke);
            pose.popPose();
        };
    }

    /** The original rocket renderers draw the authored body and the fullbright exhaust ribbon. */
    private static BulletTracer rocketModel(String modelId, String textureId, String part, float scale,
                                            Consumer<PoseStack> transform) {
        BulletTracer body = model(modelId, textureId, part, scale, transform);
        BulletTracer exhaust = ribbon(0x808080, 0xFFF2A7, true);
        return (state, pose, sink) -> {
            body.draw(state, pose, sink);
            pose.pushPose();
            pose.translate(0.375F, 0F, 0F);
            double oldLength = state.length;
            state.length = oldLength * 2D;
            exhaust.draw(state, pose, sink);
            state.length = oldLength;
            pose.popPose();
        };
    }

    private static WavefrontObj model(Identifier id) {
        return MODELS.computeIfAbsent(id, WavefrontObj::load);
    }

    /** 1.7 {@code LegoClient.renderFlare}: two additive sprites drawn with fog off. */
    public static BulletTracer flare(float red, float green, float blue) {
        return (state, pose, sink) -> {
            if (state.age < 2F || state.cameraOrientation == null) return;
            float scale = (float) (Math.min(5D, (state.age - 2F) * 0.5D) * (0.8D + Math.random() * 0.4D));
            Quaternionf camera = state.cameraOrientation;
            pose.pushPose();
            pose.mulPose(camera);
            sink.submitCustomGeometry(pose, CompatRenderTypes.flare(FLARE), (p, buffer) -> {
                flareQuad(p, buffer, scale, red, green, blue, 0.5F);
                flareQuad(p, buffer, scale * 0.5F, 1F, 1F, 1F, 0.75F);
            });
            pose.popPose();
        };
    }

    private static void flareQuad(PoseStack.Pose pose, VertexConsumer buffer,
            float size, float red, float green, float blue, float alpha) {
        int color = Colors.colorFromFloat(alpha, red, green, blue);
        Vertexes.vertex(buffer, pose, -size, -size, 0F, color, 1F, 1F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 0F, 1F);
        Vertexes.vertex(buffer, pose, -size, size, 0F, color, 1F, 0F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 0F, 1F);
        Vertexes.vertex(buffer, pose, size, size, 0F, color, 0F, 0F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 0F, 1F);
        Vertexes.vertex(buffer, pose, size, -size, 0F, color, 0F, 1F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 0F, 1F);
    }

    /**
     * 1.12.2 {@code RenderBulletMk2.renderTracer} ({@code STYLE_TRACER}): a 10-block streak of {@code fresnel_ms} ahead
     * of the round along its flight, 0.1 wide across the line from the eye, tinted (1, 0.7, 0.5), full-bright and
     * added twice. Drawn unturned, in world axes, from the entity's origin.
     */
    public static BulletTracer streak() {
        return new BulletTracer() {
            @Override
            public void extract(EntityBulletBaseMK4 bullet, BulletProjectileRenderer.State state, float partialTicks) {
                Vec3 direction = bullet.getDeltaMovement().normalize();
                Vec3 side = CameraInfo.current().pos.subtract(bullet.getEyePosition(partialTicks)).cross(direction)
                        .normalize().scale(0.05);
                state.sideX = (float) side.x;
                state.sideY = (float) side.y;
                state.sideZ = (float) side.z;
                state.alongX = (float) direction.x * 10F;
                state.alongY = (float) direction.y * 10F;
                state.alongZ = (float) direction.z * 10F;
            }

            @Override
            public void draw(BulletProjectileRenderer.State state, PoseStack pose, SubmitSink sink) {
                int color = Colors.colorFromFloat(1F, 1F, 0.7F, 0.5F);
                float sx = state.sideX, sy = state.sideY, sz = state.sideZ;
                float ax = state.alongX, ay = state.alongY, az = state.alongZ;
                sink.submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(FRESNEL), (p, buffer) -> {
                    for (int i = 0; i < 2; i++) {
                        Vertexes.vertex(buffer, p, sx, sy, sz, color, 1F, 1F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
                        Vertexes.vertex(buffer, p, sx + ax, sy + ay, sz + az, color, 1F, 0F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
                        Vertexes.vertex(buffer, p, -sx + ax, -sy + ay, -sz + az, color, 0F, 0F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
                        Vertexes.vertex(buffer, p, -sx, -sy, -sz, color, 0F, 1F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
                    }
                });
            }
        };
    }

    public static BulletTracer ribbon(int dark, int light, boolean fullbright) {
        return (state, pose, sink) -> {
            if (state.length <= 0D) return;
            float end = (float) state.length;
            int packed = fullbright ? 0xF000F0 : state.lightCoords;
            sink.submitCustomGeometry(pose, fullbright ? CompatRenderTypes.entityTranslucentEmissive(WHITE)
                    : CompatRenderTypes.entityTranslucent(WHITE), (p, buffer) -> {
                // LegoClient.renderBulletStandard is authored along +X. The entity renderer's
                // yaw/pitch transform supplies the flight direction; +Y made every tracer crosswise.
                ribbonVertex(buffer, p, end, 0.0078125F, -0.0078125F, 0F, 1F, dark, packed);
                ribbonVertex(buffer, p, end, 0.0078125F, 0.0078125F, 0F, 0F, dark, packed);
                ribbonVertex(buffer, p, 0F, 0.03125F, 0.03125F, 1F, 0F, light, packed);
                ribbonVertex(buffer, p, 0F, 0.03125F, -0.03125F, 1F, 1F, light, packed);
                ribbonVertex(buffer, p, end, -0.0078125F, -0.0078125F, 0F, 1F, dark, packed);
                ribbonVertex(buffer, p, end, -0.0078125F, 0.0078125F, 0F, 0F, dark, packed);
                ribbonVertex(buffer, p, 0F, -0.03125F, 0.03125F, 1F, 0F, light, packed);
                ribbonVertex(buffer, p, 0F, -0.03125F, -0.03125F, 1F, 1F, light, packed);
                ribbonVertex(buffer, p, end, -0.0078125F, 0.0078125F, 0F, 0F, dark, packed);
                ribbonVertex(buffer, p, end, 0.0078125F, 0.0078125F, 0F, 0F, dark, packed);
                ribbonVertex(buffer, p, 0F, 0.03125F, 0.03125F, 1F, 0F, light, packed);
                ribbonVertex(buffer, p, 0F, -0.03125F, 0.03125F, 1F, 1F, light, packed);
                ribbonVertex(buffer, p, end, -0.0078125F, -0.0078125F, 0F, 1F, dark, packed);
                ribbonVertex(buffer, p, end, 0.0078125F, -0.0078125F, 0F, 1F, dark, packed);
                ribbonVertex(buffer, p, 0F, 0.03125F, -0.03125F, 1F, 0F, light, packed);
                ribbonVertex(buffer, p, 0F, -0.03125F, -0.03125F, 1F, 1F, light, packed);
                // The source pass also closes both ends of the prism.
                ribbonVertex(buffer, p, end, 0.0078125F, 0.0078125F, 0F, 0F, dark, packed);
                ribbonVertex(buffer, p, end, 0.0078125F, -0.0078125F, 0F, 1F, dark, packed);
                ribbonVertex(buffer, p, end, -0.0078125F, -0.0078125F, 1F, 1F, dark, packed);
                ribbonVertex(buffer, p, end, -0.0078125F, 0.0078125F, 1F, 0F, dark, packed);
                ribbonVertex(buffer, p, 0F, 0.03125F, 0.03125F, 0F, 0F, light, packed);
                ribbonVertex(buffer, p, 0F, 0.03125F, -0.03125F, 0F, 1F, light, packed);
                ribbonVertex(buffer, p, 0F, -0.03125F, -0.03125F, 1F, 1F, light, packed);
                ribbonVertex(buffer, p, 0F, -0.03125F, 0.03125F, 1F, 0F, light, packed);
            });
        };
    }

    private static void ribbonVertex(VertexConsumer buffer, PoseStack.Pose pose,
            float x, float y, float z, float u, float v, int color, int light) {
        Vertexes.vertex(buffer, pose, x, y, z, ((color >> 16) & 255) / 255F,
                ((color >> 8) & 255) / 255F, (color & 255) / 255F, 1F, u, v,
                OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
    }

    private static BeamTracer tesla(boolean infraredSub) {
        return (state, pose, sink) -> {
            if (state.length <= 0D) return;
            pose.pushPose();
            float age = fade(state);
            float radialScale = infraredSub ? age / 2F + 0.15F : age / 2F + 0.5F;
            int inner = rgb((int) (0x20 * age), (int) (0x20 * age), (int) (0x40 * age));
            int outer = rgb((int) (0x40 * age), (int) (0x40 * age), (int) (0x80 * age));
            pose.scale(radialScale, 1F, radialScale);
            BeamPronter.prontBeam(pose, sink, state.direction, BeamPronter.EnumWaveType.RANDOM,
                    BeamPronter.EnumBeamType.SOLID, (int) (state.age / 3F),
                    Math.max(1, (int) (state.length / 2D + 1D)), 0.075F, inner, inner, 4, 0.25F);
            BeamPronter.prontBeam(pose, sink, state.direction, BeamPronter.EnumWaveType.RANDOM,
                    BeamPronter.EnumBeamType.SOLID, (int) state.age,
                    Math.max(1, (int) (state.length / 2D + 1D)), 0.525F, outer, outer, 2, 0.0625F);
            BeamPronter.prontBeam(pose, sink, state.direction, BeamPronter.EnumWaveType.RANDOM,
                    BeamPronter.EnumBeamType.SOLID, (int) (state.age / 2F),
                    Math.max(1, (int) (state.length / 2D + 1D)), 0.525F, outer, outer, 2, 0.0625F);
            pose.popPose();
        };
    }

    private static BeamTracer tau(boolean charged) {
        return (state, pose, sink) -> {
            if (state.length <= 0D) return;
            float age = fade(state);
            int color = charged ? rgb((int) (0x60 * age), (int) (0x50 * age), (int) (0x30 * age))
                    : rgb((int) (0x30 * age), (int) (0x25 * age), (int) (0x10 * age));
            pose.pushPose();
            pose.scale(age / 2F + 0.5F, 1F, age / 2F + 0.5F);
            BeamPronter.prontBeam(pose, sink, state.direction, BeamPronter.EnumWaveType.RANDOM,
                    BeamPronter.EnumBeamType.SOLID, (int) ((state.age + 1F) / 2F),
                    Math.max(1, (int) (state.length / 2D + 1D)), 0.3F, color, color, 2, 0.0625F);
            BeamPronter.prontBeamGradient(pose, sink, state.direction,
                    charged ? 0xFFF0A0 : 0xFFBF00, 0xFFFFFF, 0.03125F);
            pose.popPose();
        };
    }

    private static BeamTracer endpointMaterial(int dark, int light) {
        return (state, pose, sink) -> {
            if (state.length <= 0D) return;
            pose.pushPose();
            pose.scale(fade(state) * 5F, 1F, fade(state) * 5F);
            BeamPronter.prontBulletGradient(pose, sink, state.direction, dark, light,
                    0.03125F, 0.0078125F);
            pose.popPose();
        };
    }

    /** Exact source {@code LegoClient.RENDER_NI4NI_BOLT}: endpoint-first tapered cyan/white bolt. */
    private static BeamTracer ni4niBolt() {
        return (state, pose, sink) -> {
            if (state.length <= 0D || state.direction.lengthSqr() <= 1.0E-12D) return;
            float age = fade(state);
            pose.pushPose();
            // LegoClient.bolt owns the orientation because ni4ni_arc disables the generic beam
            // rotation pass. It draws from the measured endpoint back toward the entity origin.
            pose.mulPose(Axis.YP.rotationDegrees(180F - state.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(-state.pitch - 90F));
            pose.scale(age * 5F, 1F, age * 5F);
            pose.translate(0F, state.length, 0F);
            pose.mulPose(Axis.ZN.rotationDegrees(90F));
            BeamPronter.prontBulletStandard(pose, sink, state.length, 0xAAD2E5, 0xFFFFFF);
            pose.popPose();
        };
    }

    public static BeamTracer laser(int r, int g, int b) {
        return (state, pose, sink) -> {
            if (state.length <= 0D) return;
            float age = fade(state);
            int color = rgb((int) (r * age), (int) (g * age), (int) (b * age));
            pose.pushPose();
            pose.scale(age / 2F + 0.5F, 1F, age / 2F + 0.5F);
            BeamPronter.prontBeam(pose, sink, state.direction,
                    BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, (int) state.age,
                    Math.max(1, (int) (state.length / 2D + 1D)), 0F, color, color, 8, 0.0625F);
            pose.popPose();
        };
    }

    private static BeamTracer folly() {
        return (state, pose, sink) -> {
            if (state.length <= 0D) return;
            double age = fade(state);
            pose.pushPose();
            if (state.cameraOrientation != null) pose.mulPose(state.cameraOrientation);
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityTranslucentEmissive(FLARE), (p, buffer) -> {
                flareQuad(p, buffer, (float) ((1D - age) * 7.5D + 1.5D), 1F, 1F, 1F, 0.5F * (float) age);
                flareQuad(p, buffer, (float) ((1D - age) * 3.75D + 0.75D), 1F, 1F, 1F, 0.75F * (float) age);
            });
            pose.popPose();
            pose.pushPose();
            pose.scale((float) ((1D - age) * 25D + 2.5D), 1F, (float) ((1D - age) * 25D + 2.5D));
            int shade = (int) (0x20 * age);
            int color = (shade << 16) | (shade << 8) | shade;
            BeamPronter.prontBeam(pose, sink, state.direction,
                    BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, (int) state.age / 3,
                    Math.max(1, (int) (state.length / 2D + 1D)), 0F, color, color, 8, 0.0625F);
            pose.popPose();
        };
    }

    private static float fade(BeamProjectileRenderer.State state) {
        return Math.max(0F, Math.min(1F,
                1F - (state.age - 2F) / Math.max(1F, state.config.expires)));
    }

    private static int rgb(int r, int g, int b) { return (r << 16) | (g << 8) | b; }
}
