package com.hbm.client.particle;

import com.hbm.client.gun.GluonGunClient;
import com.hbm.lib.ModDamageSources;
import com.hbm.packet.SpecialDeathPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.EntityMeshes;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.world.LivingHurtAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The gluon gun's world particles, 1.12.2 {@code particle.gluon} and {@code particle.tau.ParticleTauParticle}: the
 * flare where the beam lands, the sparks off the muzzle, the burn trail it drags across blocks, and the kill effect
 * of {@code PacketSpecialDeath} effect 0, the victim's model cubes spinning apart with glow motes inside them.
 *
 * <p>All of them were layer-3 particles that drew themselves; here each kind is a client list ticked once per client
 * tick and drawn in Core's world-effects pass, as {@code SlicedMobs} does with the crucible's pieces.
 */
public final class GluonParticles {

    static final Identifier FLARE = Ids.of("hbm", "textures/misc/flare2.png");
    static final Identifier FRESNEL = Ids.of("hbm", "textures/models/bfg/fresnel_ms.png");
    static final Identifier BURN = Ids.of("hbm", "textures/misc/gluon_burn.png");
    static final Identifier BEAM_GENERIC = Ids.of("hbm", "textures/misc/beam_generic.png");

    private static final int FULLBRIGHT = 0xF000F0;

    static final List<Flare> FLARES = new ArrayList<>();
    static final List<Spark> SPARKS = new ArrayList<>();
    static final List<Trail> TRAILS = new ArrayList<>();
    /** 1.12 {@code ItemGunEgon.activeTrailParticles}: the trail each firing player is still extending. */
    static final Map<Player, Trail> ACTIVE_TRAILS = new HashMap<>();
    static final List<Cube> CUBES = new ArrayList<>();
    static final List<Mote> MOTES = new ArrayList<>();
    private static @Nullable ClientLevel lastLevel;

    /** For the harness: flares, sparks, trail nodes and disintegration cubes made since start. */
    public static int flaresSpawned;
    public static int sparksSpawned;
    public static int trailNodesAdded;
    public static int cubesSpawned;
    public static int hitsShown;

    private GluonParticles() {}

    public static void register() {
        ClientTicks.onEndTick(GluonParticles::tick);
        WorldEffects.onBegin(GluonParticles::render);
    }

    /** Live counts, for the harness: flares, sparks, trails, cubes, motes. */
    public static int[] live() {
        return new int[] {FLARES.size(), SPARKS.size(), TRAILS.size(), CUBES.size(), MOTES.size()};
    }

    // Spawning

    /** {@code new ParticleGluonFlare(world, x, y, z, player)}: placed at that player's beam end every frame. */
    public static void flare(Level level, double x, double y, double z, @Nullable Player player) {
        FLARES.add(new Flare(level.getRandom(), x, y, z, player));
        flaresSpawned++;
    }

    /** One muzzle spark as {@code ItemGunEgon.updateClient} made them. */
    public static void spark(RandomSource rand, double x, double y, double z) {
        Spark p = new Spark(rand, x, y, z, 0.05F, 0.02F, 1, 3, 0F);
        p.motion((rand.nextFloat() - 0.5F) * 0.04F, (rand.nextFloat() - 0.5F) * 0.04F, (rand.nextFloat() - 0.5F) * 0.04F);
        p.maxAge = 6 + rand.nextInt(4);
        p.color(0.2F, 0.4F + rand.nextFloat() * 0.5F, 1F, 2F);
        SPARKS.add(p);
        sparksSpawned++;
    }

    /**
     * {@code ParticleGluonBurnTrail.tryAddNewPosition} on {@code player}'s active trail, starting one if there is none;
     * a jump of more than 3 blocks ends it.
     */
    public static void trail(Player player, Vec3 pos, Vec3 normal) {
        Trail trail = ACTIVE_TRAILS.get(player);
        if (trail == null) {
            trail = new Trail(player, 0.4F);
            TRAILS.add(trail);
            ACTIVE_TRAILS.put(player, trail);
        }
        if (!trail.tryAddNewPosition(pos, normal)) ACTIVE_TRAILS.remove(player);
    }

    public static void endTrail(Player player) {
        ACTIVE_TRAILS.remove(player);
    }

    // The packet

    /**
     * 1.12.2 {@code PacketSpecialDeath} effects 1 and 0. A hit flashes the victim red for two ticks and plays the hurt
     * sound it would make for radiation; a kill removes it and breaks its model into spinning cubes.
     */
    public static void handle(SpecialDeathPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(payload.entityId());
        if (!(entity instanceof LivingEntity living)) return;
        if (payload.effectId() == SpecialDeathPayload.GLUON_HIT) {
            living.hurtTime = 2;
            SoundEvent sound = ((LivingHurtAccess) living).golem$hurtSound(ModDamageSources.radiation(level));
            if (sound != null) {
                level.playLocalSound(living.getX(), living.getY(), living.getZ(), sound, SoundSource.MASTER, 1F, 1F, false);
            }
            hitsShown++;
        } else if (payload.effectId() == SpecialDeathPayload.GLUON) {
            EntityMeshes.Mesh mesh = EntityMeshes.capture(living, Frames.partialTick());
            living.discard();
            if (mesh != null) disintegrate(level, living, mesh);
        }
    }

    /**
     * {@code DisintegrationParticleHandler.spawnGluonDisintegrateParticles}: one {@link Cube} per model cube, at the
     * cube's centre, moving towards the middle of the body at 0.25 plus up to 0.5 of random push.
     */
    private static void disintegrate(ClientLevel level, LivingEntity living, EntityMeshes.Mesh mesh) {
        RandomSource rand = level.getRandom();
        double x = living.getX(), y = living.getY(), z = living.getZ();
        Vec3 middle = new Vec3(x, y + living.getEyeHeight() / 2, z);
        for (float[] data : mesh.cubes()) {
            Cube cube = new Cube(level, mesh.texture(), data, x, y, z);
            Vec3 motion = middle.subtract(cube.posX, cube.posY, cube.posZ).normalize().scale(0.25)
                    .add(rand.nextFloat() - 0.5F, rand.nextFloat() - 0.5F, rand.nextFloat() - 0.5F);
            cube.motionX = motion.x;
            cube.motionY = motion.y;
            cube.motionZ = motion.z;
            CUBES.add(cube);
            cubesSpawned++;
        }
    }

    // Ticking

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            FLARES.clear();
            SPARKS.clear();
            TRAILS.clear();
            ACTIVE_TRAILS.clear();
            CUBES.clear();
            MOTES.clear();
            lastLevel = level;
        }
        if (level == null) return;
        FLARES.removeIf(Flare::tick);
        SPARKS.removeIf(Spark::tick);
        TRAILS.removeIf(Trail::tick);
        CUBES.removeIf(Cube::tick);
        MOTES.removeIf(Mote::tick);
        ACTIVE_TRAILS.keySet().removeIf(player -> player.isRemoved() || player.level() != level);
    }

    // Drawing

    private static void render(CameraInfo camera) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        float partial = Frames.partialTick();
        Vec3 cam = camera.pos;
        Vector3f right = camera.orientation.transform(new Vector3f(1F, 0F, 0F));
        Vector3f up = camera.orientation.transform(new Vector3f(0F, 1F, 0F));
        for (Trail t : TRAILS) t.render(level, cam, partial);
        for (Spark s : SPARKS) s.render(cam, right, up, partial);
        for (Mote m : MOTES) m.render(cam, right, up, partial);
        for (Cube c : CUBES) c.render(cam, right, up, partial);
        for (Flare f : FLARES) f.render(level, cam, right, up, partial);
    }

    /** One camera-facing quad of 1.12's particle billboard, corners {@code -R-U, -R+U, R+U, R-U}, uv (1,1)(1,0)(0,0)(0,1). */
    static void billboard(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, Vector3f right, Vector3f up,
                          float size, float r, float g, float b, float a) {
        int color = argb(r, g, b, a);
        corner(buffer, pose, x, y, z, right, up, -size, -size, 1F, 1F, color);
        corner(buffer, pose, x, y, z, right, up, -size, size, 1F, 0F, color);
        corner(buffer, pose, x, y, z, right, up, size, size, 0F, 0F, color);
        corner(buffer, pose, x, y, z, right, up, size, -size, 0F, 1F, color);
    }

    private static void corner(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, Vector3f right,
                               Vector3f up, float r, float u, float tu, float tv, int color) {
        Vertexes.vertex(buffer, pose, x + right.x * r + up.x * u, y + right.y * r + up.y * u, z + right.z * r + up.z * u,
                color, tu, tv, OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
    }

    static int argb(float r, float g, float b, float a) {
        return (Mth.clamp((int) (a * 255F), 0, 255) << 24) | (Mth.clamp((int) (r * 255F), 0, 255) << 16)
                | (Mth.clamp((int) (g * 255F), 0, 255) << 8) | Mth.clamp((int) (b * 255F), 0, 255);
    }

    static float remap01Clamp(float num, float min, float max) {
        return Mth.clamp((num - min) / (max - min), 0F, 1F);
    }

    /** {@code clamp(1 - remap(clamp(t, 0, 1), 0.6, 1, 0.6, 1), 0, 1)}: the remap maps the range onto itself, so 1 - t. */
    static float shrink(float timeScale) {
        float t = Mth.clamp(timeScale, 0F, 1F);
        return Mth.clamp(1F - ((t - 0.6F) / 0.4F * 0.4F + 0.6F), 0F, 1F);
    }

    /** 1.12.2 {@code ParticleGluonFlare}. */
    static final class Flare {
        double posX, posY, posZ;
        final int maxAge;
        int age;
        float angle, prevAngle;
        final float rot;
        final @Nullable Player player;
        /** What {@code LensVisibilityHandler} last reported, and whether a query was started. */
        float visibility;
        boolean queried;
        float pendingVisibility;

        Flare(RandomSource rand, double x, double y, double z, @Nullable Player player) {
            posX = x;
            posY = y;
            posZ = z;
            maxAge = 3 + rand.nextInt(3);
            rot = rand.nextFloat() * 0.1F + 0.1F;
            angle = rand.nextFloat() * 6F;
            prevAngle = angle;
            this.player = player;
        }

        boolean tick() {
            age++;
            prevAngle = angle;
            angle += rot;
            return age >= maxAge;
        }

        void render(ClientLevel level, Vec3 cam, Vector3f right, Vector3f up, float partial) {
            Vec3 pos = player != null ? GluonGunClient.flarePosition(level, player, partial) : new Vec3(posX, posY, posZ);
            // 1.12 occlusion-queried a 0.05 sphere (0.1 once a result came back) and read the answer a frame or more
            // later, so a new flare starts invisible.
            // port: the queried fraction is the share of points on that sphere a ray from the camera reaches.
            if (queried) visibility = pendingVisibility;
            pendingVisibility = sphereVisibility(level, cam, pos, queried ? 0.1D : 0.05D);
            queried = true;

            float vis = visibility * visibility;
            float ageN = (age + partial) / (float) maxAge;
            float scale = Mth.clamp(ageN * 2, 0F, 1F) * Mth.clamp(2 - ageN * 2 + 0.1F, 0F, 1F);
            float f4 = 0.1F * 7F * vis * scale;
            if (f4 <= 0F) return;
            float x = (float) (pos.x - cam.x), y = (float) (pos.y - cam.y), z = (float) (pos.z - cam.z);

            Vec3 view = Minecraft.getInstance().player != null
                    ? Minecraft.getInstance().player.getViewVector(partial) : Vec3.ZERO;
            float f8 = angle + (angle - prevAngle) * partial;
            float f9 = Mth.cos(f8 * 0.5F);
            Vector3f axis = new Vector3f((float) view.x, (float) view.y, (float) view.z).mul(Mth.sin(f8 * 0.5F));
            Vector3f[] corners = {
                    new Vector3f(right).mul(-f4).sub(new Vector3f(up).mul(f4)),
                    new Vector3f(right).mul(-f4).add(new Vector3f(up).mul(f4)),
                    new Vector3f(right).mul(f4).add(new Vector3f(up).mul(f4)),
                    new Vector3f(right).mul(f4).sub(new Vector3f(up).mul(f4))};
            for (Vector3f c : corners) {
                Vector3f a = new Vector3f(axis).mul(2F * c.dot(axis));
                Vector3f b = new Vector3f(c).mul(f9 * f9 - axis.dot(axis));
                Vector3f d = new Vector3f(axis).cross(c).mul(2F * f9);
                c.set(a.add(b).add(d));
            }
            float[][] uv = {{1F, 1F}, {1F, 0F}, {0F, 0F}, {0F, 1F}};
            int inner = argb(0.5F, 0.7F, 1F, vis * vis * 0.8F);
            PoseStack pose = new PoseStack();
            WorldEffects.submit(pose, CompatRenderTypes.plasmaGlowSeeThrough(FLARE), 2, (p, buffer) -> {
                for (int i = 0; i < 4; i++) {
                    Vertexes.vertex(buffer, p, x + corners[i].x, y + corners[i].y, z + corners[i].z, inner, uv[i][0], uv[i][1],
                            OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
                }
            });
            WorldEffects.submit(pose, CompatRenderTypes.plasmaGlowSeeThrough(FRESNEL), 2, (p, buffer) ->
                    billboard(buffer, p, x, y, z, right, up, f4 * 5F, 0.5F, 0.7F, 1F, vis * 0.2F));
        }
    }

    private static final double[][] SPHERE = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
            {0.577, 0.577, 0.577}, {0.577, 0.577, -0.577}, {0.577, -0.577, 0.577}, {0.577, -0.577, -0.577},
            {-0.577, 0.577, 0.577}, {-0.577, 0.577, -0.577}, {-0.577, -0.577, 0.577}, {-0.577, -0.577, -0.577}};

    static float sphereVisibility(Level level, Vec3 cam, Vec3 center, double radius) {
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        int seen = 0;
        for (double[] d : SPHERE) {
            Vec3 point = center.add(d[0] * radius, d[1] * radius, d[2] * radius);
            if (level.clip(new ClipContext(cam, point, ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, viewer)).getType()
                    == HitResult.Type.MISS) seen++;
        }
        return seen / (float) SPHERE.length;
    }

    /** 1.12.2 {@code ParticleTauParticle}. */
    static final class Spark {
        double posX, posY, posZ, prevPosX, prevPosY, prevPosZ;
        double motionX, motionY, motionZ;
        float mX, mY, mZ;
        final float scale;
        final float randomness;
        final int timeUntilNextRand;
        final int timeUntilChangeRand;
        int timeUntilChange;
        final float gravity;
        float r = 1F, g = 1F, b = 1F, alpha = 1F;
        int age;
        int maxAge;
        private final RandomSource rand;

        Spark(RandomSource rand, double x, double y, double z, float scale, float randomness, int timeUntilNextRand,
              int timeUntilChangeRand, float gravity) {
            this.rand = rand;
            posX = prevPosX = x;
            posY = prevPosY = y;
            posZ = prevPosZ = z;
            this.scale = scale;
            motionX = (rand.nextFloat() - 0.5) * randomness;
            motionY = (rand.nextFloat() - 0.5) * randomness;
            motionZ = (rand.nextFloat() - 0.5) * randomness;
            this.timeUntilChangeRand = timeUntilChangeRand;
            timeUntilChange = rand.nextInt(timeUntilChangeRand) + timeUntilNextRand;
            this.gravity = gravity;
            this.randomness = randomness;
            this.timeUntilNextRand = timeUntilNextRand;
        }

        void motion(float mX, float mY, float mZ) {
            // 1.12 added mX three times and never mY or mZ
            motionX += mX;
            motionX += mX;
            motionX += mX;
            this.mX = mX;
            this.mY = mY;
            this.mZ = mZ;
        }

        void color(float r, float g, float b, float alpha) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.alpha = alpha;
        }

        boolean tick() {
            age++;
            timeUntilChange--;
            boolean expired = age >= maxAge;
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            mY -= gravity;
            mX *= 0.90F;
            mY *= 0.90F;
            mZ *= 0.90F;
            if (timeUntilChange == 0) {
                timeUntilChange = rand.nextInt(timeUntilChangeRand) + timeUntilNextRand;
                motionX = (rand.nextFloat() - 0.5) * randomness + mX;
                motionY = (rand.nextFloat() - 0.5) * randomness + mY;
                motionZ = (rand.nextFloat() - 0.5) * randomness + mZ;
            }
            return expired;
        }

        void render(Vec3 cam, Vector3f right, Vector3f up, float partial) {
            float workingAlpha = shrink((age + partial) / (float) maxAge) * alpha;
            float f4 = 0.1F * scale;
            float x = (float) (prevPosX + (posX - prevPosX) * partial - cam.x);
            float y = (float) (prevPosY + (posY - prevPosY) * partial - cam.y);
            float z = (float) (prevPosZ + (posZ - prevPosZ) * partial - cam.z);
            WorldEffects.submit(new PoseStack(), CompatRenderTypes.plasmaGlow(FRESNEL), 1, (p, buffer) -> {
                float a = workingAlpha;
                // an alpha above 1 was drawn as that many stacked quads
                for (int i = 0; i < workingAlpha; i++) {
                    billboard(buffer, p, x, y, z, right, up, f4, r, g, b, Math.min(a, 1F));
                    a -= 1F;
                }
            });
        }
    }

    /** 1.12.2 {@code ParticleGluonBurnTrail}: a ribbon of nodes on block faces, each living 60 ticks. */
    static final class Trail {
        final Player owner;
        final float scale;
        final List<Node> nodes = new ArrayList<>();

        Trail(Player owner, float scale) {
            this.owner = owner;
            this.scale = scale;
        }

        boolean tryAddNewPosition(Vec3 pos, Vec3 normal) {
            if (nodes.isEmpty()) nodes.add(new Node(pos, normal, null, 0));
            float dist = (float) pos.subtract(nodes.get(nodes.size() - 1).pos).length();
            if (dist < 0.1) return true;
            if (dist > 3) return false;
            float uScale = 0.25F;
            if (nodes.size() == 1) {
                Vec3 tan = normal.cross(nodes.get(0).pos.subtract(pos)).normalize();
                nodes.add(new Node(pos, normal, tan, dist * uScale));
                nodes.get(0).tangent = tan;
            } else {
                Node prev = nodes.get(nodes.size() - 1);
                if (!prev.normal.equals(normal) && dist < 0.2F) {
                    // 1.12: "Attempt to stop it from going through blocks in some cases."
                    Node prevPrev = nodes.get(nodes.size() - 2);
                    Vec3 dir = prev.pos.subtract(prevPrev.pos);
                    Vec3 diff = prev.pos.subtract(pos);
                    float distToPlane = (float) (diff.dot(normal) / dir.dot(normal));
                    Vec3 interPos = prev.pos.subtract(dir.scale(distToPlane));
                    Vec3 interNormal = prev.normal.add(normal).normalize();
                    Vec3 interTan = interNormal.cross(normal).scale(-1).normalize();
                    nodes.add(new Node(interPos, interNormal, interTan, prev.uCoord - distToPlane * uScale));
                    Vec3 tan = normal.cross(interPos.subtract(pos)).normalize();
                    nodes.add(new Node(pos, normal, tan,
                            (prev.uCoord - distToPlane * uScale) + (float) pos.subtract(interPos).length() * uScale));
                } else {
                    Vec3 tan = normal.cross(prev.pos.subtract(pos)).normalize();
                    nodes.add(new Node(pos, normal, tan, prev.uCoord + dist * uScale));
                }
            }
            trailNodesAdded++;
            return true;
        }

        boolean tick() {
            Iterator<Node> itr = nodes.iterator();
            while (itr.hasNext()) {
                Node n = itr.next();
                n.age++;
                if (n.age > 60) itr.remove();
            }
            return nodes.isEmpty() && ACTIVE_TRAILS.get(owner) != this;
        }

        void render(ClientLevel level, Vec3 cam, float partial) {
            if (nodes.size() < 2) return;
            // 1.12 lit the burn with the light at the particle's own position, which it never moved from (0, 0, 0)
            int light = Renderers.lightAt(level, BlockPos.ZERO);
            PoseStack pose = new PoseStack();
            WorldEffects.submit(pose, CompatRenderTypes.flatTranslucent(BURN), 0, (p, buffer) -> {
                for (int i = 0; i < nodes.size() - 1; i++) {
                    Node prev = nodes.get(i);
                    Node next = nodes.get(i + 1);
                    float prevFade = 1 - remap01Clamp(prev.age + partial, 50, 60);
                    float nextFade = 1 - remap01Clamp(next.age + partial, 50, 60);
                    segment(buffer, p, cam, prev, next, scale, scale, 0.1F, 0.1F, 0.1F, 0.8F * prevFade, 0.8F * nextFade, light);
                }
            });
            int idx = 0;
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i).age <= 15) {
                    idx = i;
                    break;
                }
            }
            int first = idx;
            WorldEffects.submit(pose, CompatRenderTypes.plasmaGlow(BEAM_GENERIC), 1, (p, buffer) -> {
                for (int i = first; i < nodes.size() - 1; i++) {
                    Node prev = nodes.get(i);
                    Node next = nodes.get(i + 1);
                    float prevFade = (1 - remap01Clamp(prev.age + partial, 2, 12)) * 0.75F;
                    float nextFade = (1 - remap01Clamp(next.age + partial, 2, 12)) * 0.75F;
                    for (int j = 0; j < 2; j++) {
                        segment(buffer, p, cam, prev, next, scale * prevFade, scale * nextFade, 0.4F, 0.6F, 1F, 1F, 1F,
                                FULLBRIGHT);
                    }
                }
            });
        }

        /** The four triangles 1.12 drew between two nodes, fanned about their midpoint, as degenerate quads. */
        private static void segment(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cam, Node prev, Node next,
                                    float prevWidth, float nextWidth, float r, float g, float b, float prevAlpha,
                                    float nextAlpha, int light) {
            Vec3 prevPos1 = prev.pos.add(prev.tangent.scale(prevWidth)).subtract(cam);
            Vec3 prevPos2 = prev.pos.add(prev.tangent.scale(-prevWidth)).subtract(cam);
            Vec3 mid = prev.pos.add(next.pos.subtract(prev.pos).scale(0.5F)).subtract(cam);
            Vec3 nextPos1 = next.pos.add(next.tangent.scale(nextWidth)).subtract(cam);
            Vec3 nextPos2 = next.pos.add(next.tangent.scale(-nextWidth)).subtract(cam);
            float prevU = prev.uCoord;
            float nextU = next.uCoord;
            float midU = prevU + (nextU - prevU) * 0.5F;
            float midAlpha = prevAlpha + (nextAlpha - prevAlpha) * 0.5F;
            int cPrev = argb(r, g, b, prevAlpha), cNext = argb(r, g, b, nextAlpha), cMid = argb(r, g, b, midAlpha);
            triangle(buffer, pose, prevPos1, prevU, 0, cPrev, prevPos2, prevU, 1, cPrev, mid, midU, 0.5F, cMid, light);
            triangle(buffer, pose, prevPos1, prevU, 0, cPrev, mid, midU, 0.5F, cMid, nextPos1, nextU, 0, cNext, light);
            triangle(buffer, pose, prevPos2, prevU, 1, cPrev, nextPos2, nextU, 1, cNext, mid, midU, 0.5F, cMid, light);
            triangle(buffer, pose, nextPos2, nextU, 1, cNext, nextPos1, nextU, 0, cNext, mid, midU, 0.5F, cMid, light);
        }

        private static void triangle(VertexConsumer buffer, PoseStack.Pose pose, Vec3 a, float ua, float va, int ca,
                                     Vec3 b, float ub, float vb, int cb, Vec3 c, float uc, float vc, int cc, int light) {
            Vertexes.vertex(buffer, pose, (float) a.x, (float) a.y, (float) a.z, ca, ua, va, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, (float) b.x, (float) b.y, (float) b.z, cb, ub, vb, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, (float) c.x, (float) c.y, (float) c.z, cc, uc, vc, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, (float) c.x, (float) c.y, (float) c.z, cc, uc, vc, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
        }

        private static final class Node {
            int age;
            final Vec3 pos;
            final Vec3 normal;
            Vec3 tangent;
            final float uCoord;

            Node(Vec3 pos, Vec3 normal, @Nullable Vec3 tangent, float u) {
                this.pos = pos;
                this.normal = normal;
                this.tangent = tangent;
                this.uCoord = u;
            }
        }
    }

    /**
     * 1.12.2 {@code ParticleGluonDisintegration}: one cube of the victim's model, flying and tumbling for 90 ticks, a
     * near-black copy of itself (vertex colour 0.1) that fades out over ticks 65 to 67, filled with glow motes and
     * shedding two loose motes into the world each tick until then.
     *
     * <p>port: the cube arrives posed (Core {@code EntityMeshes}), so the tumble turns it about world axes through its
     * centre where 1.12 turned it about the cube's own model axes; the spin rates are random either way.
     */
    static final class Cube {
        final Level level;
        final Identifier texture;
        /** The cube's 24 vertices relative to its centre: x, y, z, u, v. */
        final float[] local;
        final Vec3 corner;
        final Vec3[] edges;
        final int numParticles;
        double posX, posY, posZ, prevPosX, prevPosY, prevPosZ;
        double motionX, motionY, motionZ;
        AABB box;
        float rotX, rotY, rotZ, prevRotX, prevRotY, prevRotZ;
        Vec3 rotation;
        int age;
        final int maxAge = 90;
        final List<Glow> glows = new ArrayList<>();
        final RandomSource rand;

        Cube(Level level, Identifier texture, float[] data, double x, double y, double z) {
            this.level = level;
            this.texture = texture;
            this.rand = level.getRandom();
            int vertices = EntityMeshes.CUBE_VERTICES;
            int stride = EntityMeshes.STRIDE;
            double cx = 0, cy = 0, cz = 0;
            List<Vec3> corners = new ArrayList<>();
            for (int i = 0; i < vertices; i++) {
                Vec3 v = new Vec3(data[i * stride], data[i * stride + 1], data[i * stride + 2]);
                if (corners.stream().noneMatch(c -> c.distanceToSqr(v) < 1.0E-8)) corners.add(v);
            }
            for (Vec3 c : corners) {
                cx += c.x;
                cy += c.y;
                cz += c.z;
            }
            cx /= corners.size();
            cy /= corners.size();
            cz /= corners.size();
            local = data.clone();
            for (int i = 0; i < vertices; i++) {
                local[i * stride] -= (float) cx;
                local[i * stride + 1] -= (float) cy;
                local[i * stride + 2] -= (float) cz;
            }
            Vec3 center = new Vec3(cx, cy, cz);
            corner = corners.get(0).subtract(center);
            edges = edges(corners);
            // 1.12 counted the box's volume in model pixels
            double volume = edges[0].length() * edges[1].length() * edges[2].length() / (0.0625 * 0.0625 * 0.0625);
            numParticles = Math.min(15, (int) Math.cbrt(volume * 0.6) + 1);
            posX = prevPosX = x + cx;
            posY = prevPosY = y + cy;
            posZ = prevPosZ = z + cz;
            box = new AABB(posX - 0.1, posY, posZ - 0.1, posX + 0.1, posY + 0.2, posZ + 0.1);
            rotation = new Vec3((rand.nextFloat() - 0.5) * 50, (rand.nextFloat() - 0.5) * 50, (rand.nextFloat() - 0.5) * 50);
        }

        /** The three edges leaving the first corner: the nearest corner, then the nearest off that line, then off that plane. */
        private static Vec3[] edges(List<Vec3> corners) {
            Vec3 origin = corners.get(0);
            List<Vec3> rest = new ArrayList<>();
            for (int i = 1; i < corners.size(); i++) rest.add(corners.get(i).subtract(origin));
            rest.sort((a, b) -> Double.compare(a.lengthSqr(), b.lengthSqr()));
            Vec3 e1 = Vec3.ZERO, e2 = Vec3.ZERO, e3 = Vec3.ZERO;
            for (Vec3 v : rest) {
                if (e1 == Vec3.ZERO) e1 = v;
                else if (e2 == Vec3.ZERO && v.cross(e1).lengthSqr() > 1.0E-10) e2 = v;
                else if (e2 != Vec3.ZERO && e3 == Vec3.ZERO && Math.abs(v.dot(e1.cross(e2))) > 1.0E-10) e3 = v;
            }
            return new Vec3[] {e1, e2, e3};
        }

        private Vec3 randomPoint(float reach) {
            Vec3 p = corner;
            for (Vec3 e : edges) p = p.add(e.scale(rand.nextFloat() * reach));
            return p;
        }

        boolean tick() {
            age++;
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            prevRotX = rotX;
            prevRotY = rotY;
            prevRotZ = rotZ;
            move(motionX, motionY, motionZ);
            motionX *= 0.9;
            motionY *= 0.9;
            motionZ *= 0.9;
            if (age >= maxAge) return true;
            glows.removeIf(Glow::tick);
            if (age < 65) {
                for (int i = 0; i < numParticles; i++) glows.add(new Glow(randomPoint(1F), 0.9F, rand.nextInt(4) + 2));
                for (int i = 0; i < 2; i++) {
                    MOTES.add(new Mote(rand, posX, posY, posZ, rand.nextFloat() * 0.25F - 0.125F,
                            rand.nextFloat() * 0.25F - 0.125F, rand.nextFloat() * 0.25F - 0.125F));
                }
            } else if (age == 65) {
                for (int i = 0; i < numParticles * 3; i++) glows.add(new Glow(randomPoint(1.3F), 1.2F, rand.nextInt(12) + 8));
            }
            rotX += (float) rotation.x;
            rotY += (float) rotation.y;
            rotZ += (float) rotation.z;
            rotation = rotation.scale(0.95);
            return false;
        }

        /** 1.12 {@code move} with its own bounce: each blocked axis reverses, scaled; z takes the y speed. */
        private void move(double x, double y, double z) {
            double origX = x, origY = y, origZ = z;
            Vec3 allowed = Entity.collideBoundingBox(null, new Vec3(x, y, z), box, level, List.of());
            box = box.move(allowed);
            posX = (box.minX + box.maxX) / 2.0;
            posY = box.minY;
            posZ = (box.minZ + box.maxZ) / 2.0;
            if (origY != allowed.y) motionY = -motionY * 0.75 * (rand.nextFloat() * 0.8 + 0.25);
            if (origX != allowed.x) motionX = -motionX * 0.75 * (rand.nextFloat() * 1.12 + 0.25);
            if (origZ != allowed.z) motionZ = -motionY * 0.75 * (rand.nextFloat() * 1.12 + 0.25);
        }

        void render(Vec3 cam, Vector3f right, Vector3f up, float partial) {
            PoseStack pose = new PoseStack();
            pose.translate(prevPosX + (posX - prevPosX) * partial - cam.x, prevPosY + (posY - prevPosY) * partial - cam.y,
                    prevPosZ + (posZ - prevPosZ) * partial - cam.z);
            pose.mulPose(Axis.XP.rotationDegrees(prevRotX + (rotX - prevRotX) * partial));
            pose.mulPose(Axis.YP.rotationDegrees(prevRotY + (rotY - prevRotY) * partial));
            pose.mulPose(Axis.ZP.rotationDegrees(prevRotZ + (rotZ - prevRotZ) * partial));
            float a = 1 - remap01Clamp(age + partial, 65, 67);
            int color = argb(0.1F, 0.1F, 0.1F, a);
            RenderType type = CompatRenderTypes.entityTranslucentCull(texture);
            int stride = EntityMeshes.STRIDE;
            WorldEffects.submit(pose, type, 0, (p, buffer) -> {
                for (int q = 0; q < EntityMeshes.CUBE_VERTICES; q += 4) {
                    Vec3 n = normal(q);
                    for (int i = q; i < q + 4; i++) {
                        Vertexes.vertex(buffer, p, local[i * stride], local[i * stride + 1], local[i * stride + 2], color,
                                local[i * stride + 3], local[i * stride + 4], OverlayTexture.NO_OVERLAY, FULLBRIGHT,
                                (float) n.x, (float) n.y, (float) n.z);
                    }
                }
            });
            if (glows.isEmpty()) return;
            Matrix4f m = pose.last().pose();
            WorldEffects.submit(new PoseStack(), CompatRenderTypes.plasmaGlow(FRESNEL), 1, (p, buffer) -> {
                for (Glow glow : glows) glow.render(buffer, p, m, right, up, partial);
            });
        }

        private Vec3 normal(int quad) {
            int stride = EntityMeshes.STRIDE;
            Vec3 a = new Vec3(local[quad * stride], local[quad * stride + 1], local[quad * stride + 2]);
            Vec3 b = new Vec3(local[(quad + 1) * stride], local[(quad + 1) * stride + 1], local[(quad + 1) * stride + 2]);
            Vec3 c = new Vec3(local[(quad + 2) * stride], local[(quad + 2) * stride + 1], local[(quad + 2) * stride + 2]);
            Vec3 n = b.subtract(a).cross(c.subtract(a));
            return n.lengthSqr() < 1.0E-12 ? new Vec3(0, 1, 0) : n.normalize();
        }
    }

    /** 1.12.2 {@code ParticleGluonParticle}: a glow mote inside a tumbling cube, drawn facing the camera. */
    static final class Glow {
        final Vec3 pos;
        final float scale;
        final int maxAge;
        int age;

        Glow(Vec3 pos, float scale, int maxAge) {
            this.pos = pos;
            this.scale = scale;
            this.maxAge = maxAge;
        }

        boolean tick() {
            return ++age >= maxAge;
        }

        void render(VertexConsumer buffer, PoseStack.Pose pose, Matrix4f cube, Vector3f right, Vector3f up,
                    float partial) {
            float shrink = shrink((age + partial) / (float) maxAge);
            float alpha = shrink * 0.9F;
            float f4 = 0.1F * (scale + shrink * scale * 4);
            Vector3f at = cube.transformPosition(new Vector3f((float) pos.x, (float) pos.y, (float) pos.z));
            billboard(buffer, pose, at.x, at.y, at.z, right, up, f4, 0.4F, 0.7F, 1F, alpha);
        }
    }

    /** 1.12.2 {@code ParticleGluonParticleSmol}: a loose glow mote drifting off a cube. */
    static final class Mote {
        double posX, posY, posZ, prevPosX, prevPosY, prevPosZ;
        double motionX, motionY, motionZ;
        final int maxAge;
        int age;

        Mote(RandomSource rand, double x, double y, double z, double mX, double mY, double mZ) {
            posX = prevPosX = x;
            posY = prevPosY = y;
            posZ = prevPosZ = z;
            maxAge = 6 + rand.nextInt(5);
            motionX = mX + (rand.nextFloat() - 0.5) * 0.04;
            motionY = mY + (rand.nextFloat() - 0.5) * 0.04;
            motionZ = mZ + (rand.nextFloat() - 0.5) * 0.04;
            // 1.12 also drew a wander delay it never counted down, so a mote never changes course
            rand.nextInt(5);
        }

        boolean tick() {
            age++;
            if (age >= maxAge) return true;
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            motionX *= 0.8;
            motionY *= 0.8;
            motionZ *= 0.8;
            return false;
        }

        void render(Vec3 cam, Vector3f right, Vector3f up, float partial) {
            float shrink = shrink((age + partial) / (float) maxAge);
            float f4 = 0.1F * (0.1F + shrink * 0.1F * 4);
            float x = (float) (prevPosX + (posX - prevPosX) * partial - cam.x);
            float y = (float) (prevPosY + (posY - prevPosY) * partial - cam.y);
            float z = (float) (prevPosZ + (posZ - prevPosZ) * partial - cam.z);
            WorldEffects.submit(new PoseStack(), CompatRenderTypes.plasmaGlow(FRESNEL), 1, (p, buffer) ->
                    billboard(buffer, p, x, y, z, right, up, f4, 0.5F, 0.8F, 1F, shrink));
        }
    }
}
