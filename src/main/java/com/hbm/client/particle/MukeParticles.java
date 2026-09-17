package com.hbm.client.particle;

import com.hbm.Constants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import dev.golem.core.data.Tags;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
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
    private static final List<Detonation> ACTIVE = new ArrayList<>();
    private static boolean initialized;
    private static ClientLevel lastLevel;

    private MukeParticles() {}

    public static void register() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        ClientTicks.onEndTick(MukeParticles::tick);
        WorldEffects.onBegin(camera -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            float partial = Frames.partialTick();
            for (Detonation detonation : List.copyOf(ACTIVE)) detonation.submit(camera, partial);
        });
        ClientResourceReloads.register(Constants.MOD_ID, "muke_particles", MukeParticles::clear);
    }

    public static void spawnMuke(Level level, double x, double y, double z, CompoundTag data) {
        spawn(level, x, y, z, false, Tags.getBooleanOr(data, "balefire", false));
    }

    public static void spawnTinyTot(Level level, double x, double y, double z, CompoundTag data) {
        spawn(level, x, y, z, true, Tags.getBooleanOr(data, "balefire", false));
    }

    private static void spawn(Level level, double x, double y, double z, boolean tiny, boolean balefire) {
        if (!(level instanceof ClientLevel client)) return;
        ACTIVE.add(new Detonation(client, x, y, z, tiny, balefire));
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

        private void submit(CameraInfo camera, float partial) {
            PoseStack pose = new PoseStack();
            pose.translate(x - camera.pos.x, y - camera.pos.y, z - camera.pos.z);
            // The captured world model-view already applies camera orientation.  Supplying camera
            // rotated billboard axes here would rotate the effect a second time as the player turns.
            Vector3f right = new Vector3f(1F, 0F, 0F);
            Vector3f up = new Vector3f(0F, 1F, 0F);

            if (age < WAVE_LIFETIME) {
                float waveAge = age + partial;
                float alpha = 1F - waveAge / WAVE_LIFETIME;
                float scale = (float) (1D - Math.exp(waveAge * -0.125D)) * 45F;
                WorldEffects.submit(pose, CompatRenderTypes.plasmaGlow(SHOCKWAVE), 0,
                        (drawPose, buffer) -> wave(buffer, drawPose, scale, alpha));
            }
            if (!tiny && age < FLASH_LIFETIME) {
                submitFlash(pose, right, up, partial);
            }
            if (!clouds.isEmpty()) submitClouds(pose, right, up);
        }

        private void submitFlash(PoseStack pose, Vector3f right, Vector3f up, float partial) {
            float flashAge = age + partial;
            float alpha = (1F - flashAge / FLASH_LIFETIME) * 0.5F;
            float scale = flashAge * 3F + 1F;
            WorldEffects.submit(pose, CompatRenderTypes.plasmaGlow(FLARE), 1, (drawPose, buffer) -> {
                Random random = new Random();
                for (int i = 0; i < 24; i++) {
                    random.setSeed(i * 31L + 1L);
                    float cx = (float) (random.nextDouble() * 15D - 7.5D);
                    float cy = (float) (random.nextDouble() * 7.5D - 3.75D);
                    float cz = (float) (random.nextDouble() * 15D - 7.5D);
                    quad(buffer, drawPose, cx, cy, cz, right, up, scale, 0F, 1F, 0F, 1F,
                            1F, 0.9F, 0.75F, alpha);
                }
            });
        }

        private void submitClouds(PoseStack pose, Vector3f right, Vector3f up) {
            Identifier texture = !tiny && balefire ? EXPLOSION_BF : EXPLOSION;
                WorldEffects.submit(pose, CompatRenderTypes.entityTranslucentEmissive(texture), 2,
                    (drawPose, buffer) -> {
                        for (Cloud cloud : List.copyOf(clouds))
                            cloud.render(buffer, drawPose, right, up, x, y, z);
                    });
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

        private void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                Vector3f right, Vector3f up, double ox, double oy, double oz) {
            int frame = Math.min(24, age * 25 / lifetime);
            float u0 = (frame % 5) / 5F;
            float u1 = u0 + 0.2F;
            float v0 = (frame / 5) / 5F;
            float v1 = v0 + 0.2F;
            float partial = Frames.partialTick();
            float cx = (float) (xo + (x - xo) * partial - ox);
            float cy = (float) (yo + (y - yo) * partial - oy);
            float cz = (float) (zo + (z - zo) * partial - oz);
            quad(buffer, pose, cx, cy, cz, right, up, 3F, u0, u1, v0, v1, 1F, 1F, 1F, 1F);
        }
    }

    private static void wave(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
            float scale, float alpha) {
        Vertexes.vertex(buffer, pose, -scale, -0.25F, -scale, 1F, 1F, 1F, alpha,
                1F, 1F, OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
        Vertexes.vertex(buffer, pose, -scale, -0.25F, scale, 1F, 1F, 1F, alpha,
                1F, 0F, OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
        Vertexes.vertex(buffer, pose, scale, -0.25F, scale, 1F, 1F, 1F, alpha,
                0F, 0F, OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
        Vertexes.vertex(buffer, pose, scale, -0.25F, -scale, 1F, 1F, 1F, alpha,
                0F, 1F, OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
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
