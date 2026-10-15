package com.hbm.client.crucible;

import com.hbm.packet.SpecialDeathPayload;
import com.hbm.physics.Box;
import com.hbm.physics.CollisionSource;
import com.hbm.physics.Mat3;
import com.hbm.physics.V3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import dev.golem.core.render.Bloom;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.EntityMeshes;
import dev.golem.core.render.Poses;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * The client half of 1.12.2 {@code PacketSpecialDeath} effect 3 and everything it spawns: the victim vanishes and
 * its model, cut by the plane, falls apart as rigid bodies ({@code ParticleSlicedMob}) with capped cuts; five blood
 * streaks ({@code ParticleBlood}, which leave {@code blood_dec1} decals where they land) and, for the crucible, eight
 * spark bursts ({@code HbmEffectNT.Spark}, twelve {@code ParticleSpark} each) leave from the cut face.
 *
 * <p>Each kind is a client list ticked once per client tick and drawn in Core's world-effects pass, the way the other
 * custom-quad particles here are ({@code SparkParticles}): none of them is a camera-facing quad.
 */
public final class SlicedMobs {

    private static final Identifier GORE_GENERIC = Ids.of("hbm", "textures/misc/gore_generic.png");
    private static final Identifier CRUCIBLE_CAP = Ids.of("hbm", "textures/misc/crucible_cap.png");
    private static final Identifier BLOOD = Ids.of("hbm", "textures/misc/blood0.png");
    private static final Identifier BLOOD_DECAL = Ids.of("hbm", "textures/misc/blood_dec1.png");
    private static final Identifier SPARK = Ids.of("hbm", "textures/models/bfg/particle.png");

    static final List<Sliced> SLICED = new ArrayList<>();
    static final List<Blood> BLOOD_STREAKS = new ArrayList<>();
    static final List<Spark> SPARKS = new ArrayList<>();
    static final List<Decal> DECALS = new ArrayList<>();
    private static ClientLevel lastLevel;

    /** For the harness: halves made since start. */
    public static int slicedSpawned;
    public static int bloodSpawned;
    public static int sparksSpawned;
    public static int decalsSpawned;
    /** Cubes in the last captured victim model (0: none captured). */
    public static int lastMeshCubes;

    private SlicedMobs() {}

    static void register() {
        ClientTicks.onEndTick(SlicedMobs::tick);
        WorldEffects.onBegin(SlicedMobs::render);
    }

    /** Live counts, for the harness. */
    public static int[] live() {
        return new int[]{SLICED.size(), BLOOD_STREAKS.size(), SPARKS.size(), DECALS.size()};
    }

    // Packet

    public static void handle(SpecialDeathPayload payload) {
        if (payload.effectId() != SpecialDeathPayload.SLICED || payload.auxData().length < 5) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Entity ent = level.getEntity(payload.entityId());
        if (!(ent instanceof LivingEntity living)) return;
        float[] data = payload.auxData();
        int id = Float.floatToIntBits(data[4]);
        Identifier capTex = id == 0 ? GORE_GENERIC : CRUCIBLE_CAP;
        boolean capBloom = id == 1;

        EntityMeshes.Mesh mesh = EntityMeshes.capture(living, Frames.partialTick());
        // ent.setDead(): the client entity goes now; the server's removal follows
        ent.discard();
        if (mesh == null) return;
        lastMeshCubes = mesh.cubes().size();

        double ex = ent.getX(), ey = ent.getY(), ez = ent.getZ();
        MobCutter.Result result = MobCutter.cut(mesh.cubes(), data, ex, ey, ez, collisions(level));
        List<MobCutter.Triangle> capTris = result.capTriangles();
        RandomSource rand = level.getRandom();
        if (!capTris.isEmpty()) {
            int bloodCount = 5;
            int cCount = id == 1 ? 8 : 0;
            for (int i = 0; i < bloodCount + cCount; i++) {
                MobCutter.Triangle t = capTris.get(rand.nextInt(capTris.size()));
                float rand1 = rand.nextFloat();
                float rand2 = rand.nextFloat();
                if (rand2 < rand1) {
                    float tmp = rand2;
                    rand2 = rand1;
                    rand1 = tmp;
                }
                V3 pos = t.p1().pos().scale(rand1).add(t.p2().pos().scale(rand2 - rand1))
                        .add(t.p3().pos().scale(1 - rand2)).add(ex, ey, ez);
                if (i < bloodCount) {
                    Blood blood = new Blood(pos.x(), pos.y(), pos.z(), 1, 0.4F + rand.nextFloat() * 0.4F,
                            18 + rand.nextInt(10), 0.05F);
                    Vec3 look = mc.player != null ? mc.player.getViewVector(1F) : new Vec3(0, 0, 1);
                    V3 direction = new V3(look.x, look.y, look.z).crossProduct(new V3(data[0], data[1], data[2]))
                            .normalize().scale(-0.6F);
                    V3 randMotion = new V3(rand.nextDouble() * 2 - 1, rand.nextDouble() * 2 - 1, rand.nextDouble() * 2 - 1)
                            .scale(0.2F);
                    direction = direction.add(randMotion);
                    blood.motion((float) direction.x(), (float) direction.y(), (float) direction.z());
                    blood.color(0.5F, 0.1F, 0.1F, 1F);
                    blood.tick(level);
                    BLOOD_STREAKS.add(blood);
                    bloodSpawned++;
                } else if (capTris.size() >= 3) {
                    // 1.12 read capTris.get(2) unchecked; with fewer than three cap triangles that threw and lost the
                    // whole effect, halves included. Here the sparks are just skipped.
                    MobCutter.Triangle c0 = capTris.get(0);
                    MobCutter.Triangle c2 = capTris.get(2);
                    V3 direction = c0.p2().pos().subtract(c0.p1().pos())
                            .crossProduct(c2.p2().pos().subtract(c0.p1().pos())).normalize().scale(i % 2 == 0 ? 0.4 : -0.4);
                    sparkBurst(rand, pos, direction);
                }
            }
        }
        for (MobCutter.Piece piece : result.pieces()) {
            SLICED.add(new Sliced(piece, mesh.texture(), capTex, capBloom, 80 + rand.nextInt(20)));
            slicedSpawned++;
        }
    }

    /** {@code HbmEffectNT.Spark} with the cut's tag: 12 sparks in a 60 degree cone. */
    private static void sparkBurst(RandomSource rand, V3 pos, V3 dir) {
        float width = 0.02F;
        float length = 0.8F;
        float randLength = 1.3F - length;
        float gravity = 0.1F;
        int lifetime = 20;
        int randLifeTime = lifetime;
        float velocityRand = 0.3F;
        float angle = 60F;
        float randAngle = 0;
        for (int i = 0; i < 12; i++) {
            V3 up = new V3(0, 1, 0);
            up = up.rotatePitch((float) Math.toRadians(rand.nextFloat() * (angle + rand.nextFloat() * randAngle)));
            up = up.rotateYaw((float) Math.toRadians(rand.nextFloat() * 360));
            double yaw = Math.toDegrees(Math.atan2(dir.x(), dir.z()));
            double pitch = Math.toDegrees(Math.atan2(dir.y(), Math.sqrt(dir.x() * dir.x() + dir.z() * dir.z()))) - 90;
            V3 newDirection = up.rotatePitch((float) Math.toRadians(pitch - 90)).rotateYaw((float) Math.toRadians(yaw));
            newDirection = newDirection.scale((float) dir.length() + rand.nextFloat() * velocityRand);
            Spark spark = new Spark(pos.x(), pos.y(), pos.z(), length + rand.nextFloat() * randLength, width,
                    lifetime + rand.nextInt(randLifeTime), gravity);
            spark.color(0.8F, 0.6F, 0.5F, 1.5F);
            spark.motion((float) newDirection.x(), (float) newDirection.y(), (float) newDirection.z());
            SPARKS.add(spark);
            sparksSpawned++;
        }
    }

    private static CollisionSource collisions(ClientLevel level) {
        return area -> {
            List<Box> boxes = new ArrayList<>();
            for (VoxelShape shape : level.getBlockCollisions(null,
                    new AABB(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ()))) {
                for (AABB box : shape.toAabbs()) boxes.add(new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
            }
            return boxes;
        };
    }

    // Ticking

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            SLICED.clear();
            BLOOD_STREAKS.clear();
            SPARKS.clear();
            DECALS.clear();
            lastLevel = level;
        }
        if (level == null) return;
        SLICED.removeIf(s -> !s.tick());
        for (int i = BLOOD_STREAKS.size() - 1; i >= 0; i--) {
            if (!BLOOD_STREAKS.get(i).tick(level)) BLOOD_STREAKS.remove(i);
        }
        for (int i = SPARKS.size() - 1; i >= 0; i--) {
            if (!SPARKS.get(i).tick(level)) SPARKS.remove(i);
        }
        DECALS.removeIf(d -> ++d.age > d.maxAge);
    }

    /** {@code ParticleSlicedMob}: a rigid body stepped every tick for 80..99 ticks. */
    static final class Sliced {
        final MobCutter.Piece piece;
        final Identifier texture;
        final Identifier capTex;
        final boolean capBloom;
        final int maxAge;
        int age;

        Sliced(MobCutter.Piece piece, Identifier texture, Identifier capTex, boolean capBloom, int maxAge) {
            this.piece = piece;
            this.texture = texture;
            this.capTex = capTex;
            this.capBloom = capBloom;
            this.maxAge = maxAge;
        }

        boolean tick() {
            piece.body().minecraftTimestep();
            age++;
            return age < maxAge;
        }
    }

    /** Old base {@code Particle.move} for a 0.2 particle box: y, then x, then z against block shapes. */
    abstract static class Moving {
        double posX, posY, posZ, prevPosX, prevPosY, prevPosZ;
        double motionX, motionY, motionZ;
        float red = 1, green = 1, blue = 1, alpha = 1;
        int age;
        int maxAge;
        boolean canCollide = true;
        boolean alive = true;

        Moving(double x, double y, double z) {
            posX = prevPosX = x;
            posY = prevPosY = y;
            posZ = prevPosZ = z;
        }

        AABB box() {
            return new AABB(posX - 0.1, posY, posZ - 0.1, posX + 0.1, posY + 0.2, posZ + 0.1);
        }

        /** Returns the {@code y} actually moved; sets the position from the box as {@code resetPositionToBB} did. */
        double[] moveBox(ClientLevel level, double x, double y, double z) {
            AABB box = box();
            if (canCollide) {
                Iterable<VoxelShape> shapes = level.getBlockCollisions(null, box.expandTowards(x, y, z));
                List<VoxelShape> list = new ArrayList<>();
                shapes.forEach(list::add);
                y = Shapes.collide(Direction.Axis.Y, box, list, y);
                box = box.move(0, y, 0);
                x = Shapes.collide(Direction.Axis.X, box, list, x);
                box = box.move(x, 0, 0);
                z = Shapes.collide(Direction.Axis.Z, box, list, z);
                box = box.move(0, 0, z);
            } else {
                box = box.move(x, y, z);
            }
            posX = (box.minX + box.maxX) / 2.0;
            posY = box.minY;
            posZ = (box.minZ + box.maxZ) / 2.0;
            return new double[]{x, y, z};
        }
    }

    /** {@code ParticleBlood}: a streak along its first direction, growing and fading; a block hit leaves a decal. */
    static final class Blood extends Moving {
        final float stretch;
        final float scale;
        final float gravity;
        V3 originalAxis = V3.ZERO;

        Blood(double x, double y, double z, float stretch, float scale, int lifetime, float gravity) {
            super(x, y, z);
            this.stretch = stretch;
            this.scale = scale;
            this.maxAge = lifetime;
            this.gravity = gravity;
        }

        void motion(float mX, float mY, float mZ) {
            motionX = mX;
            motionY = mY - gravity;
            motionZ = mZ;
            posX += mX;
            posY += mY - gravity;
            posZ += mZ;
            originalAxis = new V3(mX, mY, mZ).normalize();
        }

        void color(float r, float g, float b, float a) {
            red = r; green = g; blue = b; alpha = a;
        }

        boolean tick(ClientLevel level) {
            age++;
            if (age > maxAge) return false;
            Vec3 from = new Vec3(posX, posY, posZ);
            Vec3 to = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
            // 1.12 rayTraceBlocks: block collision shapes, no fluids (the viewer stands in for the null entity).
            // 1.21+ CollisionContext.of(null) throws; the viewer is briefly null while disconnecting or respawning,
            // and a droplet then has nothing to be seen by.
            Entity viewer = Minecraft.getInstance().player;
            if (viewer == null) return false;
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    viewer));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 direction = to.subtract(from).normalize();
                Vec3 at = hit.getLocation();
                Decal decal = Decal.create(level, new V3(direction.x, direction.y, direction.z), 1, at);
                if (decal != null) {
                    DECALS.add(decal);
                    decalsSpawned++;
                }
                return false;
            }
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            double[] moved = moveBox(level, motionX, motionY, motionZ);
            if (moved[0] != motionX) motionX = 0;
            if (moved[2] != motionZ) motionZ = 0;
            final float airResistance = 0.9F;
            motionX *= airResistance;
            motionY = motionY * airResistance - gravity;
            motionZ *= airResistance;
            return true;
        }
    }

    /** 1.12 {@code ParticleSpark}: additive streaks of {@code bfg/particle}, bouncing, gone when nearly still. */
    static final class Spark extends Moving {
        final float stretch;
        final float scale;
        final float gravity;
        final Random rand = new Random();

        Spark(double x, double y, double z, float stretch, float scale, int lifetime, float gravity) {
            super(x, y, z);
            this.stretch = stretch;
            this.scale = scale;
            this.maxAge = lifetime;
            this.gravity = gravity;
        }

        void color(float r, float g, float b, float a) {
            red = r; green = g; blue = b; alpha = a;
        }

        void motion(float mX, float mY, float mZ) {
            motionX = mX;
            motionY = mY - gravity;
            motionZ = mZ;
            posX += mX;
            posY += mY - gravity;
            posZ += mZ;
        }

        boolean tick(ClientLevel level) {
            age++;
            if (age >= maxAge || new V3(motionX, motionY, motionZ).lengthSquared() < 0.001) return false;
            canCollide = age >= 4;
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            double d0 = motionY;
            double origX = motionX;
            double origZ = motionZ;
            double[] moved = moveBox(level, motionX, motionY, motionZ);
            if (d0 != moved[1]) motionY = -motionY * 0.75 * (rand.nextFloat() * 0.8 + 0.25);
            if (origX != moved[0]) motionX = -motionX * 0.75 * (rand.nextFloat() * 1.12 + 0.25);
            // 1.12 really wrote motionY here
            if (origZ != moved[2]) motionZ = -motionY * 0.75 * (rand.nextFloat() * 1.12 + 0.25);
            final float airResistance = 0.95F;
            motionX *= airResistance;
            motionY = motionY * airResistance - gravity;
            motionZ *= airResistance;
            return true;
        }
    }

    /**
     * {@code ParticleDecal} of {@code BakedModelUtil.generateDecalMesh}: the faces of the blocks around the hit that face
     * the incoming streak, clipped to a box 2 wide and 6 deep aligned with it (random roll), textured by projection.
     * 1.12 triangulated each block's baked model; here each block's outline shape stands in for it, faces against a
     * full neighbour dropped as the model renderer's side culling did.
     */
    static final class Decal {
        final double x, y, z;
        final List<float[]> triangles; // per triangle 3 x (x, y, z, u, v) relative to (x, y, z), then nx, ny, nz
        final int maxAge = 120;
        int age;

        Decal(double x, double y, double z, List<float[]> triangles) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.triangles = triangles;
        }

        static Decal create(ClientLevel level, V3 normal, float scale, Vec3 hit) {
            double yaw = Math.toDegrees(Math.atan2(normal.x(), normal.z()));
            double pitch = Math.toDegrees(Math.atan2(normal.y(), Math.sqrt(normal.x() * normal.x() + normal.z() * normal.z()))) - 90;
            float roll = level.getRandom().nextFloat() * 2F * (float) Math.PI;
            Mat3 rot = eulerToMat((float) Math.toRadians(yaw), (float) Math.toRadians(pitch + 90), roll);
            V3 c1 = new V3(rot.m00, rot.m01, rot.m02);
            V3 c2 = new V3(rot.m10, rot.m11, rot.m12);
            V3 c3 = new V3(rot.m20, rot.m21, rot.m22);
            float[][] planes = {
                    {(float) c1.x(), (float) c1.y(), (float) c1.z(), scale}, {(float) -c1.x(), (float) -c1.y(), (float) -c1.z(), scale},
                    {(float) c2.x(), (float) c2.y(), (float) c2.z(), scale}, {(float) -c2.x(), (float) -c2.y(), (float) -c2.z(), scale},
                    {(float) c3.x(), (float) c3.y(), (float) c3.z(), scale * 3}, {(float) -c3.x(), (float) -c3.y(), (float) -c3.z(), scale * 3}};
            double ext = Math.abs(c1.x()) * scale + Math.abs(c2.x()) * scale + Math.abs(c3.x()) * scale * 3;
            double eyt = Math.abs(c1.y()) * scale + Math.abs(c2.y()) * scale + Math.abs(c3.y()) * scale * 3;
            double ezt = Math.abs(c1.z()) * scale + Math.abs(c2.z()) * scale + Math.abs(c3.z()) * scale * 3;
            List<float[]> out = new ArrayList<>();
            for (int i = Mth.floor(hit.x - ext); i <= Mth.ceil(hit.x + ext); i++) {
                for (int j = Mth.floor(hit.y - eyt); j <= Mth.ceil(hit.y + eyt); j++) {
                    for (int k = Mth.floor(hit.z - ezt); k <= Mth.ceil(hit.z + ezt); k++) {
                        BlockPos pos = new BlockPos(i, j, k);
                        var state = level.getBlockState(pos);
                        if (state.isAir()) continue;
                        VoxelShape shape = state.getShape(level, pos);
                        for (AABB box : shape.toAabbs()) {
                            for (Direction face : Direction.values()) {
                                if (isFullFaceHidden(level, pos, box, face)) continue;
                                for (float[][] tri : faceTriangles(box.move(pos), face, hit)) {
                                    V3 a = new V3(tri[0][0], tri[0][1], tri[0][2]);
                                    V3 tn = new V3(face.getStepX(), face.getStepY(), face.getStepZ());
                                    if (tn.dotProduct(normal) > -0.2) continue;
                                    List<float[]> poly = new ArrayList<>(List.of(tri));
                                    for (float[] plane : planes) poly = clip(poly, plane);
                                    for (int t = 1; t + 1 < poly.size(); t++) {
                                        float[] tr = new float[3 * 5 + 3];
                                        float[][] corner = {poly.get(0), poly.get(t), poly.get(t + 1)};
                                        for (int c = 0; c < 3; c++) {
                                            float px = corner[c][0], py = corner[c][1], pz = corner[c][2];
                                            float u = (rot.m00 * px + rot.m01 * py + rot.m02 * pz) * 0.5F / scale + 0.5F;
                                            float v = (rot.m10 * px + rot.m11 * py + rot.m12 * pz) * 0.5F / scale + 0.5F;
                                            tr[c * 5] = px; tr[c * 5 + 1] = py; tr[c * 5 + 2] = pz;
                                            tr[c * 5 + 3] = u; tr[c * 5 + 4] = v;
                                        }
                                        tr[15] = (float) tn.x(); tr[16] = (float) tn.y(); tr[17] = (float) tn.z();
                                        out.add(tr);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (out.isEmpty()) return null;
            // ParticleDecal sat 0.001 back along the streak's direction
            return new Decal(hit.x - normal.x() * 0.001, hit.y - normal.y() * 0.001, hit.z - normal.z() * 0.001, out);
        }

        private static boolean isFullFaceHidden(ClientLevel level, BlockPos pos, AABB box, Direction face) {
            boolean onBoundary = switch (face) {
                case DOWN -> box.minY <= 0; case UP -> box.maxY >= 1;
                case NORTH -> box.minZ <= 0; case SOUTH -> box.maxZ >= 1;
                case WEST -> box.minX <= 0; case EAST -> box.maxX >= 1;
            };
            if (!onBoundary) return false;
            BlockPos next = pos.relative(face);
            return level.getBlockState(next).isCollisionShapeFullBlock(level, next);
        }

        /** A box face as two triangles, relative to the hit point. */
        private static List<float[][]> faceTriangles(AABB b, Direction face, Vec3 o) {
            float x0 = (float) (b.minX - o.x), x1 = (float) (b.maxX - o.x);
            float y0 = (float) (b.minY - o.y), y1 = (float) (b.maxY - o.y);
            float z0 = (float) (b.minZ - o.z), z1 = (float) (b.maxZ - o.z);
            float[][] q = switch (face) {
                case DOWN -> new float[][]{{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}};
                case UP -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
                case NORTH -> new float[][]{{x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}};
                case SOUTH -> new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
                case WEST -> new float[][]{{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}};
                case EAST -> new float[][]{{x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}};
            };
            return List.of(new float[][]{q[0], q[1], q[2]}, new float[][]{q[2], q[3], q[0]});
        }

        /** Keeps the part of {@code poly} with {@code n . p + d > 0}. */
        private static List<float[]> clip(List<float[]> poly, float[] plane) {
            List<float[]> out = new ArrayList<>(poly.size() + 2);
            for (int i = 0; i < poly.size(); i++) {
                float[] a = poly.get(i);
                float[] b = poly.get((i + 1) % poly.size());
                float da = a[0] * plane[0] + a[1] * plane[1] + a[2] * plane[2] + plane[3];
                float db = b[0] * plane[0] + b[1] * plane[1] + b[2] * plane[2] + plane[3];
                if (da > 0) out.add(a);
                if ((da > 0) != (db > 0)) {
                    float s = da / (da - db);
                    out.add(new float[]{a[0] + (b[0] - a[0]) * s, a[1] + (b[1] - a[1]) * s, a[2] + (b[2] - a[2]) * s});
                }
            }
            return out;
        }

        /** {@code BakedModelUtil.eulerToMat}: {@code rotZ(roll) * rotX(pitch) * rotY(-yaw)}. */
        private static Mat3 eulerToMat(float yaw, float pitch, float roll) {
            float sy = (float) Math.sin(-yaw), cy = (float) Math.cos(-yaw);
            float sp = (float) Math.sin(pitch), cp = (float) Math.cos(pitch);
            float sr = (float) Math.sin(roll), cr = (float) Math.cos(roll);
            Mat3 mR = new Mat3(cr, -sr, 0, sr, cr, 0, 0, 0, 1);
            mR.mul(new Mat3(1, 0, 0, 0, cp, -sp, 0, sp, cp));
            mR.mul(new Mat3(cy, 0, sy, 0, 1, 0, -sy, 0, cy));
            return mR;
        }
    }

    // Drawing

    private static void render(CameraInfo camera) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        float partial = Frames.partialTick();
        Vec3 cam = camera.pos;
        int overlay = OverlayTexture.NO_OVERLAY;
        for (Sliced s : SLICED) {
            Matrix4f m = s.piece.body().renderMatrix(cam.x, cam.y, cam.z, partial);
            V3 c = s.piece.body().globalCentroid;
            int light = Renderers.lightAt(level, BlockPos.containing(c.x(), c.y(), c.z()));
            PoseStack pose = new PoseStack();
            Poses.mulPose(pose, m);
            WorldEffects.submit(pose, CompatRenderTypes.entityCutoutCull(s.texture), 0, (p, buffer) -> {
                for (MobCutter.CutModelData d : s.piece.parts()) tessellate(p, buffer, d.data, false, light, overlay);
            });
            int capLight = s.capBloom ? 0xF000F0 : light;
            WorldEffects.submit(pose, CompatRenderTypes.flatCutoutDecal(s.capTex), 1, (p, buffer) -> {
                for (MobCutter.CutModelData d : s.piece.parts()) {
                    if (d.cap != null) tessellate(p, buffer, d.cap, d.flip, capLight, overlay);
                }
            });
            if (s.capBloom) {
                WorldEffects.submit(pose, Bloom.of(s.capTex), 2, (p, buffer) -> {
                    for (MobCutter.CutModelData d : s.piece.parts()) {
                        if (d.cap != null) tessellate(p, buffer, d.cap, d.flip, 0xF000F0, overlay);
                    }
                });
            }
        }
        for (Decal d : DECALS) {
            PoseStack pose = new PoseStack();
            pose.translate(d.x - cam.x, d.y - cam.y, d.z - cam.z);
            int light = Renderers.lightAt(level, BlockPos.containing(d.x, d.y, d.z));
            float fade = (d.age - d.maxAge + 30 + partial) / 30F;
            float a = Mth.clamp(1 - fade, 0, 1);
            // ParticleDecal: depthMask(false), polygon offset -5 (here a 0.002 lift off the face), colour 0.5, 0.1, 0.1
            WorldEffects.submit(pose, CompatRenderTypes.flatTranslucent(BLOOD_DECAL), 3, (p, buffer) -> {
                for (float[] t : d.triangles) {
                    float ox = t[15] * 0.002F, oy = t[16] * 0.002F, oz = t[17] * 0.002F;
                    for (int c = 0; c < 4; c++) {
                        int k = Math.min(c, 2) * 5;
                        Vertexes.vertex(buffer, p, t[k] + ox, t[k + 1] + oy, t[k + 2] + oz, 0.5F, 0.1F, 0.1F, a,
                                t[k + 3], t[k + 4], overlay, light, 0, 1, 0);
                    }
                }
            });
        }
        for (Blood b : BLOOD_STREAKS) renderBlood(level, b, cam, partial);
        for (Spark s : SPARKS) renderSpark(s, cam, partial);
    }

    /** {@code VertexData.tessellate(buf, flip, normal)}: triangles as degenerate quads, face normals. */
    private static void tessellate(PoseStack.Pose pose, VertexConsumer buffer, MobCutter.VertexData data, boolean flip,
                                   int light, int overlay) {
        for (int i = 0; i < data.positionIndices.length; i += 3) {
            V3 a = data.positions[data.positionIndices[i]];
            V3 b = data.positions[data.positionIndices[i + 1]];
            V3 c = data.positions[data.positionIndices[i + 2]];
            int tOB = 1;
            int tOC = 2;
            if (flip) {
                V3 tmp = b;
                b = c;
                c = tmp;
                tOB = 2;
                tOC = 1;
            }
            V3 n = b.subtract(a).crossProduct(c.subtract(a)).normalize();
            float nx = (float) n.x(), ny = (float) n.y(), nz = (float) n.z();
            float[] tc = data.texCoords;
            Vertexes.vertex(buffer, pose, (float) a.x(), (float) a.y(), (float) a.z(), 0xFFFFFFFF, tc[i * 2], tc[i * 2 + 1],
                    overlay, light, nx, ny, nz);
            Vertexes.vertex(buffer, pose, (float) b.x(), (float) b.y(), (float) b.z(), 0xFFFFFFFF, tc[(i + tOB) * 2],
                    tc[(i + tOB) * 2 + 1], overlay, light, nx, ny, nz);
            Vertexes.vertex(buffer, pose, (float) c.x(), (float) c.y(), (float) c.z(), 0xFFFFFFFF, tc[(i + tOC) * 2],
                    tc[(i + tOC) * 2 + 1], overlay, light, nx, ny, nz);
            Vertexes.vertex(buffer, pose, (float) c.x(), (float) c.y(), (float) c.z(), 0xFFFFFFFF, tc[(i + tOC) * 2],
                    tc[(i + tOC) * 2 + 1], overlay, light, nx, ny, nz);
        }
    }

    /**
     * {@code ParticleBlood.renderParticle}: a streak across the view of {@code blood0}, growing by
     * {@code fade^2 * 1.5 + 1} and fading by {@code 1 - fade^2}, lit by the light at its position.
     */
    private static void renderBlood(ClientLevel level, Blood b, Vec3 cam, float partial) {
        float fade = (b.age + partial) / (float) b.maxAge;
        float f5 = (float) (b.prevPosX + (b.posX - b.prevPosX) * partial - cam.x);
        float f6 = (float) (b.prevPosY + (b.posY - b.prevPosY) * partial - cam.y);
        float f7 = (float) (b.prevPosZ + (b.posZ - b.prevPosZ) * partial - cam.z);
        V3 axis = b.originalAxis;
        // 1.12 measured from the viewer's feet and subtracted the eye height ("Is it right to subtract the eye height
        // here? No idea!"): the vector from the eye, which is what this camera-relative position already is
        V3 toPlayer = new V3(f5, f6, f7);
        V3 p1 = axis.crossProduct(toPlayer).normalize().scale(0.5 * b.scale * (fade * fade * 1.5F + 1));
        V3 p2 = p1.scale(-1);
        p1 = p1.add(f5, f6, f7);
        p2 = p2.add(f5, f6, f7);
        axis = axis.scale(b.stretch * (fade * fade * 1.5F + 1));
        int light = Renderers.lightAt(level, BlockPos.containing(b.posX, b.posY, b.posZ));
        float a = Mth.clamp(b.alpha * (1 - fade * fade), 0, 1);
        float r = b.red, g = b.green, bl = b.blue;
        V3 q1 = p1, q2 = p2, ax = axis;
        // lighting off, depthMask(false), cull off, SRC_ALPHA / ONE_MINUS_SRC_ALPHA, the particle's own light
        WorldEffects.submit(new PoseStack(), CompatRenderTypes.flatTranslucent(BLOOD), 4, (p, buffer) -> {
            int overlay = OverlayTexture.NO_OVERLAY;
            Vertexes.vertex(buffer, p, (float) q2.x(), (float) q2.y(), (float) q2.z(), r, g, bl, a, 1, 0, overlay, light, 0, 1, 0);
            Vertexes.vertex(buffer, p, (float) q1.x(), (float) q1.y(), (float) q1.z(), r, g, bl, a, 1, 1, overlay, light, 0, 1, 0);
            Vertexes.vertex(buffer, p, (float) (q1.x() + ax.x()), (float) (q1.y() + ax.y()), (float) (q1.z() + ax.z()), r, g, bl, a,
                    0, 1, overlay, light, 0, 1, 0);
            Vertexes.vertex(buffer, p, (float) (q2.x() + ax.x()), (float) (q2.y() + ax.y()), (float) (q2.z() + ax.z()), r, g, bl, a,
                    0, 0, overlay, light, 0, 1, 0);
        });
    }

    /** {@code ParticleSpark.renderParticle}: one quad per unit of alpha (1.5: two), additive, fullbright. */
    private static void renderSpark(Spark s, Vec3 cam, float partial) {
        float f5 = (float) (s.prevPosX + (s.posX - s.prevPosX) * partial - cam.x);
        float f6 = (float) (s.prevPosY + (s.posY - s.prevPosY) * partial - cam.y);
        float f7 = (float) (s.prevPosZ + (s.posZ - s.prevPosZ) * partial - cam.z);
        float mX = (float) (s.posX + s.motionX * partial - cam.x);
        float mY = (float) (s.posY + s.motionY * partial - cam.y);
        float mZ = (float) (s.posZ + s.motionZ * partial - cam.z);
        V3 axis = new V3(mX, mY, mZ).subtract(f5, f6, f7);
        // 1.12: relative to the viewer's feet (interpPos), with no eye height taken off
        double eye = Minecraft.getInstance().getCameraEntity() == null ? 0 : Minecraft.getInstance().getCameraEntity().getEyeHeight();
        V3 toPlayer = new V3(mX, mY + eye, mZ);
        V3 p1 = axis.crossProduct(toPlayer).normalize().scale(0.5 * s.scale);
        V3 p2 = p1.scale(-1).add(f5, f6, f7);
        p1 = p1.add(f5, f6, f7);
        V3 ax = axis.scale(s.stretch);
        V3 q1 = p1, q2 = p2;
        float r = s.red, g = s.green, b = s.blue, alpha = s.alpha;
        WorldEffects.submit(new PoseStack(), CompatRenderTypes.plasmaGlow(SPARK), 5, (p, buffer) -> {
            float left = alpha;
            int overlay = OverlayTexture.NO_OVERLAY;
            int light = 0xF000F0;
            while (left > 0) {
                float a = Mth.clamp(left, 0, 1);
                Vertexes.vertex(buffer, p, (float) q2.x(), (float) q2.y(), (float) q2.z(), r, g, b, a, 1, 0, overlay, light, 0, 1, 0);
                Vertexes.vertex(buffer, p, (float) q1.x(), (float) q1.y(), (float) q1.z(), r, g, b, a, 1, 1, overlay, light, 0, 1, 0);
                Vertexes.vertex(buffer, p, (float) (q1.x() + ax.x()), (float) (q1.y() + ax.y()), (float) (q1.z() + ax.z()), r, g, b, a,
                        0, 1, overlay, light, 0, 1, 0);
                Vertexes.vertex(buffer, p, (float) (q2.x() + ax.x()), (float) (q2.y() + ax.y()), (float) (q2.z() + ax.z()), r, g, b, a,
                        0, 0, overlay, light, 0, 1, 0);
                left -= 1;
            }
        });
    }
}
