package com.hbm.client.particle;

import com.hbm.Constants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import dev.golem.core.client.Colors;
import dev.golem.core.data.Tags;
import dev.golem.core.render.Billboards;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** The source muke/tinytot detonation visuals, submitted through Core's late world-effects pass. */
public final class MukeParticles {
    private static final Identifier SHOCKWAVE = Ids.of(Constants.MOD_ID, "textures/particle/shockwave.png");
    private static final Identifier FLARE = Ids.of(Constants.MOD_ID, "textures/particle/flare.png");
    private static final Identifier EXPLOSION = Ids.of(Constants.MOD_ID, "textures/particle/explosion.png");
    private static final Identifier EXPLOSION_BF = Ids.of(Constants.MOD_ID, "textures/particle/explosion_bf.png");
    private static final int WAVE_LIFETIME = 25;
    private static final int FLASH_LIFETIME = 20;
    private static final int FULLBRIGHT = 0xF000F0;
    private static final int LAYER_FLARE = 0;
    private static final int LAYER_EXPLOSION = 1;
    private static final int LAYER_BALEFIRE = 2;
    private static final int CLOUD_FRAMES = 5;
    private static final List<Detonation> ACTIVE = new ArrayList<>();
    private static final Pool POOL = new Pool();
    private static final Surface SHOCKWAVE_SKIN = Surface.glow(SHOCKWAVE.toString());
    private static final Geometry SHOCKWAVE_SHEET = Quads.named("hbm_muke_shockwave")
            .normal(0F, 1F, 0F)
            .vertex(-1F, -0.25F, -1F, 1F, 1F)
            .vertex(-1F, -0.25F, 1F, 1F, 0F)
            .vertex(1F, -0.25F, 1F, 0F, 0F)
            .vertex(1F, -0.25F, -1F, 0F, 1F)
            .build();
    private static Gem.Model<Detonation> shockwave;
    private static boolean initialized;
    private static ClientLevel lastLevel;

    private MukeParticles() {}

    /**
     * The billboards of every live detonation, as one pool.
     *
     * <p>One pool rather than one per detonation because a visual is a bucket of GPU instances and
     * the UBO backend only instances a single billboard visual per level; a nuke is also exactly the
     * moment not to be allocating.
     */
    static final class Pool {
        private Pool() {}
    }

    public static void register() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        ClientTicks.onEndTick(MukeParticles::tick);
        Billboards.registerPool(Pool.class, List.of(
                        new Billboards.Layer(FLARE.toString(), Billboards.Mode.SOURCE_ALPHA_ADDITIVE),
                        new Billboards.Layer(EXPLOSION.toString(), Billboards.Mode.EMISSIVE_TRANSLUCENT,
                                Billboards.Align.Y_LOCKED),
                        new Billboards.Layer(EXPLOSION_BF.toString(), Billboards.Mode.EMISSIVE_TRANSLUCENT,
                                Billboards.Align.Y_LOCKED)),
                (pool, frame, layers) -> {
                    for (Detonation detonation : List.copyOf(ACTIVE)) detonation.writeRows(frame, layers);
                },
                pool -> ACTIVE.isEmpty());
        WorldEffects.onBegin(camera -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            float partial = Frames.partialTick();
            boolean instanced = Billboards.owns(POOL);
            for (Detonation detonation : List.copyOf(ACTIVE)) detonation.submit(camera, partial, instanced);
        });
        ClientResourceReloads.register(Constants.MOD_ID, "muke_particles", MukeParticles::clear);
    }

    public static void spawnMuke(Level level, double x, double y, double z, CompoundTag data) {
        spawn(level, x, y, z, false, Tags.getBooleanOr(data, "balefire", false));
    }

    public static void spawnTinyTot(Level level, double x, double y, double z, CompoundTag data) {
        spawn(level, x, y, z, true, Tags.getBooleanOr(data, "balefire", false));
    }

    /**
     * original-hbm (1.7.10) {@code ClientProxy} type {@code ufo}: one lone {@code ParticleMukeCloud} moving
     * {@code gaussian * motion} sideways. The UFO's tractor beam sends no {@code motion}, so its two clouds a tick
     * stand still at the beam's foot (friction 0.95, 52..62 ticks). No flash, shockwave or camera jolt.
     */
    public static void spawnUfo(Level level, double x, double y, double z, CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        double motion = Tags.getDoubleOr(data, "motion", 0D);
        RandomSource random = client.getRandom();
        ACTIVE.add(new Detonation(client, x, y, z, random.nextGaussian() * motion, 0D, random.nextGaussian() * motion));
        Billboards.add(client, POOL);
    }

    private static void spawn(Level level, double x, double y, double z, boolean tiny, boolean balefire) {
        if (!(level instanceof ClientLevel client)) return;
        ACTIVE.add(new Detonation(client, x, y, z, tiny, balefire));
        Billboards.add(client, POOL);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.animateHurt(0F);
            player.hurtTime = 15;
            player.hurtDuration = 15;
        }
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            clear();
            lastLevel = null;
            return;
        }
        if (lastLevel != level) {
            clear();
            lastLevel = level;
        }
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Detonation detonation = ACTIVE.get(i);
            detonation.tick(level);
            if (detonation.expired()) ACTIVE.remove(i);
        }
    }

    private static void clear() {
        ACTIVE.clear();
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) Billboards.remove(level, POOL);
    }

    private static final class Detonation {
        private final double x;
        private final double y;
        private final double z;
        private final boolean tiny;
        private final boolean balefire;
        private final List<Cloud> clouds = new ArrayList<>();
        private int age;

        private Detonation(ClientLevel level, double x, double y, double z, boolean tiny, boolean balefire) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.tiny = tiny;
            this.balefire = balefire;
            if (tiny) spawnTinyClouds(level);
        }

        /** A single cloud with no wave or flash (1.7 {@code ufo}): starts past both lifetimes, never spawns more. */
        private Detonation(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.tiny = true;
            this.balefire = false;
            this.age = WAVE_LIFETIME;
            clouds.add(new Cloud(level, x, y, z, xd, yd, zd, false));
        }

        private void tick(ClientLevel level) {
            if (age < WAVE_LIFETIME) age++;
            for (int i = clouds.size() - 1; i >= 0; i--) {
                Cloud cloud = clouds.get(i);
                cloud.tick(level);
                if (cloud.expired) clouds.remove(i);
            }
            // ParticleMukeFlash creates the delayed full cloud at particleAge == 15.
            if (!tiny && age == 15) spawnMukeClouds(level);
        }

        private boolean expired() {
            return age >= WAVE_LIFETIME && clouds.isEmpty();
        }

        private float waveScale() {
            return (float) (1D - Math.exp((age + Frames.partialTick()) * -0.125D)) * 45F;
        }

        private float waveAlpha() {
            return 1F - (age + Frames.partialTick()) / WAVE_LIFETIME;
        }

        private void spawnMukeClouds(ClientLevel level) {
            RandomSource random = level.getRandom();
            for (double d = 0D; d <= 1.8D; d += 0.1D) {
                clouds.add(new Cloud(level, x, y, z, random.nextGaussian() * 0.05D,
                        d + random.nextGaussian() * 0.02D, random.nextGaussian() * 0.05D, balefire));
            }
            for (int i = 0; i < 100; i++) {
                clouds.add(new Cloud(level, x, y + 0.5D, z, random.nextGaussian() * 0.5D,
                        random.nextInt(5) == 0 ? 0.02D : 0D, random.nextGaussian() * 0.5D, balefire));
            }
            for (int i = 0; i < 75; i++) {
                double cx = random.nextGaussian() * 0.5D;
                double cz = random.nextGaussian() * 0.5D;
                if (cx * cx + cz * cz > 1.5D) {
                    cx *= 0.5D;
                    cz *= 0.5D;
                }
                double cy = 1.8D + (random.nextDouble() * 3D - 1.5D)
                        * (0.75D - (cx * cx + cz * cz)) * 0.5D;
                clouds.add(new Cloud(level, x, y, z, cx, cy + random.nextGaussian() * 0.02D, cz, balefire));
            }
        }

        private void spawnTinyClouds(ClientLevel level) {
            RandomSource random = level.getRandom();
            for (double d = 0D; d <= 1.6D; d += 0.1D) {
                clouds.add(new Cloud(level, x, y, z, random.nextGaussian() * 0.05D,
                        d + random.nextGaussian() * 0.02D, random.nextGaussian() * 0.05D, false));
            }
            for (int i = 0; i < 50; i++) {
                clouds.add(new Cloud(level, x, y + 0.5D, z, random.nextGaussian() * 0.5D,
                        random.nextInt(5) == 0 ? 0.02D : 0D, random.nextGaussian() * 0.5D, false));
            }
            for (int i = 0; i < 15; i++) {
                double cx = random.nextGaussian() * 0.2D;
                double cz = random.nextGaussian() * 0.2D;
                if (cx * cx + cz * cz > 0.75D) {
                    cx *= 0.5D;
                    cz *= 0.5D;
                }
                double cy = 1.6D + (random.nextDouble() * 2D - 1D)
                        * (0.75D - (cx * cx + cz * cz)) * 0.5D;
                clouds.add(new Cloud(level, x, y, z, cx, cy + random.nextGaussian() * 0.02D, cz, false));
            }
        }

        private void writeRows(Billboards.Frame frame, Billboards.LayerWriter layers) {
            float partial = frame.partialTick();
            if (!tiny && age < FLASH_LIFETIME) writeFlash(layers.layer(LAYER_FLARE), partial);
            if (!clouds.isEmpty()) {
                writeClouds(layers.layer(!tiny && balefire ? LAYER_BALEFIRE : LAYER_EXPLOSION), partial);
            }
        }

        private void writeFlash(Billboards.RowWriter writer, float partial) {
            float flashAge = age + partial;
            float alpha = (1F - flashAge / FLASH_LIFETIME) * 0.5F;
            float scale = flashAge * 3F + 1F;
            Random random = new Random();
            for (int i = 0; i < 24; i++) {
                random.setSeed(i * 31L + 1L);
                writer.row(x + random.nextDouble() * 15D - 7.5D,
                        y + random.nextDouble() * 7.5D - 3.75D,
                        z + random.nextDouble() * 15D - 7.5D,
                        scale, 1F, 0.9F, 0.75F, alpha, FULLBRIGHT);
            }
        }

        private void writeClouds(Billboards.RowWriter writer, float partial) {
            for (Cloud cloud : List.copyOf(clouds)) cloud.write(writer, partial);
        }

        private void submit(CameraInfo camera, float partial, boolean instanced) {
            PoseStack pose = new PoseStack();
            pose.translate(x - camera.pos.x, y - camera.pos.y, z - camera.pos.z);

            if (age < WAVE_LIFETIME) {
                Gem.replay(shockwave(), this, pose, WorldEffects.sink(0), FULLBRIGHT);
            }
            if (instanced) return;

            Vector3f right = camera.orientation.transform(new Vector3f(1F, 0F, 0F));
            Vector3f up = camera.orientation.transform(new Vector3f(0F, 1F, 0F));
            if (!tiny && age < FLASH_LIFETIME) {
                WorldEffects.submit(pose, CompatRenderTypes.plasmaGlow(FLARE), 1,
                        (drawPose, buffer) -> writeFlash(quads(drawPose, buffer, right, up), partial));
            }
            if (!clouds.isEmpty()) {
                Identifier texture = !tiny && balefire ? EXPLOSION_BF : EXPLOSION;
                Vector3f locked = new Vector3f(up.x, 1F, up.z);
                WorldEffects.submit(pose, CompatRenderTypes.entityTranslucentEmissive(texture), 2,
                        (drawPose, buffer) -> writeClouds(quads(drawPose, buffer, right, locked), partial));
            }
        }

        private Billboards.RowWriter quads(PoseStack.Pose pose,
                com.mojang.blaze3d.vertex.VertexConsumer buffer, Vector3f right, Vector3f up) {
            return new Billboards.RowWriter() {
                @Override
                public void row(double rowX, double rowY, double rowZ, float halfSize,
                        float red, float green, float blue, float alpha, int packedLight) {
                    quad(buffer, pose, (float) (rowX - x), (float) (rowY - y), (float) (rowZ - z),
                            right, up, halfSize, 0F, 1F, 0F, 1F, red, green, blue, alpha);
                }

                @Override
                public void sprite(double rowX, double rowY, double rowZ, float halfSize,
                        float red, float green, float blue, float alpha, int packedLight,
                        int column, int row, int columns, int rows) {
                    float u0 = (float) column / columns;
                    float v0 = (float) row / rows;
                    quad(buffer, pose, (float) (rowX - x), (float) (rowY - y), (float) (rowZ - z),
                            right, up, halfSize, u0, u0 + 1F / columns, v0, v0 + 1F / rows,
                            red, green, blue, alpha);
                }
            };
        }
    }

    private static final class Cloud {
        private double x;
        private double y;
        private double z;
        private double xo;
        private double yo;
        private double zo;
        private double xd;
        private double yd;
        private double zd;
        private final float friction;
        private final int lifetime;
        private int age;
        private boolean expired;

        private Cloud(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                boolean balefire) {
            this.x = this.xo = x;
            this.y = this.yo = y;
            this.z = this.zo = z;
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            if (yd > 0D) {
                friction = 0.9F;
                lifetime = yd > 0.1D ? 92 + level.getRandom().nextInt(11) + (int) (yd * 20D)
                        : 72 + level.getRandom().nextInt(11);
            } else if (yd == 0D) {
                friction = 0.95F;
                lifetime = 52 + level.getRandom().nextInt(11);
            } else {
                friction = 0.85F;
                lifetime = 122 + level.getRandom().nextInt(31);
                age = 80;
            }
        }

        private void tick(ClientLevel level) {
            boolean noClip = age <= 2;
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime - 2) {
                expired = true;
                return;
            }
            Vec3 initial = new Vec3(xd, yd, zd);
            Vec3 collided = initial;
            if (!noClip) {
                AABB bounds = new AABB(x - 0.1D, y - 0.1D, z - 0.1D,
                        x + 0.1D, y + 0.1D, z + 0.1D);
                ArrayList<VoxelShape> collisions = new ArrayList<>();
                level.getBlockCollisions(null, bounds.expandTowards(initial.x, initial.y, initial.z))
                        .forEach(collisions::add);
                collided = Entity.collideBoundingBox(null, initial, bounds, level, collisions);
            }
            x += collided.x;
            y += collided.y;
            z += collided.z;
            xd *= friction;
            yd *= friction;
            zd *= friction;
            if (collided.y != initial.y) {
                yd = 0D;
            }
            if (collided.x != initial.x) {
                xd = 0D;
            }
            if (collided.z != initial.z) {
                zd = 0D;
            }
            if (collided.y != initial.y && initial.y < 0D) {
                xd *= 0.7D;
                zd *= 0.7D;
            }
        }

        private void write(Billboards.RowWriter writer, float partial) {
            int frame = Math.min(24, age * 25 / lifetime);
            writer.sprite(xo + (x - xo) * partial, yo + (y - yo) * partial, zo + (z - zo) * partial,
                    3F, 1F, 1F, 1F, 1F, FULLBRIGHT,
                    frame % CLOUD_FRAMES, frame / CLOUD_FRAMES, CLOUD_FRAMES, CLOUD_FRAMES);
        }
    }

    /**
     * The ground ring, whose only animation is its radius: one sheet, stretched. Its Y stays put
     * because the stretch is X and Z alone, which is what keeps it 0.25 under the detonation.
     */
    private static Gem.Model<Detonation> shockwave() {
        if (shockwave != null) return shockwave;
        Gem.Model<Detonation> model = Gem.model();
        model.part(SHOCKWAVE_SHEET, SHOCKWAVE_SKIN)
                .stretch(Gem.Axes.X, Detonation::waveScale)
                .stretch(Gem.Axes.Z, Detonation::waveScale)
                .tint(detonation -> Colors.colorFromFloat(detonation.waveAlpha(), 1F, 1F, 1F))
                .light(detonation -> FULLBRIGHT);
        shockwave = model.seal();
        return shockwave;
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
            float cx, float cy, float cz, Vector3f right, Vector3f up, float size,
            float u0, float u1, float v0, float v1, float red, float green, float blue, float alpha) {
        float rx = right.x * size, ry = right.y * size, rz = right.z * size;
        float ux = up.x * size, uy = up.y * size, uz = up.z * size;
        Vertexes.vertex(buffer, pose, cx - rx - ux, cy - ry - uy, cz - rz - uz, red, green, blue, alpha,
                u1, v1, OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
        Vertexes.vertex(buffer, pose, cx - rx + ux, cy - ry + uy, cz - rz + uz, red, green, blue, alpha,
                u1, v0, OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
        Vertexes.vertex(buffer, pose, cx + rx + ux, cy + ry + uy, cz + rz + uz, red, green, blue, alpha,
                u0, v0, OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
        Vertexes.vertex(buffer, pose, cx + rx - ux, cy + ry - uy, cz + rz - uz, red, green, blue, alpha,
                u0, v1, OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
    }
}
