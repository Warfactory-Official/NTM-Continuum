package com.hbm.client.particle;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Client lifetime and Core submission path for original-hbm's skeleton and giblet effects. */
public final class DisintegrationParticles {
    private static final Identifier SKELETON_MODEL = Ids.of(Constants.MOD_ID, "models/effect/skeleton.obj");
    private static final Identifier SKELETON_TEXTURE = Ids.of(Constants.MOD_ID, "textures/particle/skeleton.png");
    private static final Identifier SKELETON_TEXTURE_EXT = Ids.of(Constants.MOD_ID, "textures/particle/skoilet.png");
    private static final Identifier SKELETON_BLOOD = Ids.of(Constants.MOD_ID, "textures/particle/skeleton_blood.png");
    private static final Identifier SKELETON_BLOOD_EXT = Ids.of(Constants.MOD_ID, "textures/particle/skoilet_blood.png");
    private static final Identifier MEAT = Ids.of(Constants.MOD_ID, "textures/particle/meat.png");
    private static final Identifier SLIME = Ids.of(Constants.MOD_ID, "textures/particle/slime.png");
    private static final Identifier METAL = Ids.of(Constants.MOD_ID, "textures/particle/metal.png");
    private static WavefrontObj model;
    private static final List<SkeletonPiece> SKELETONS = new ArrayList<>();
    private static final List<Giblet> GIBLETS = new ArrayList<>();
    private static final Map<Integer, Vanish> VANISHED = new HashMap<>();
    private static boolean initialized;

    private DisintegrationParticles() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        dev.golem.core.client.ClientTicks.onEndTick(DisintegrationParticles::tick);
        WorldEffects.onBegin(camera -> submit(camera));
        dev.golem.core.client.ClientResourceReloads.register(Constants.MOD_ID,
                "disintegration_particles", () -> { clear(); model = null; });
    }

    public static void skeleton(ClientLevel level, RandomSource rand, double x, double y, double z,
            int entityId, float brightness, boolean gib) {
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof LivingEntity living)) return;
        vanish(living);
        Bone[] bones = bones(living);
        if (bones == null) return;
        boolean vanillaSkeleton = living.getType() == EntityType.SKELETON;
        for (Bone bone : bones) {
            if (gib && !vanillaSkeleton && rand.nextBoolean()) continue;
            SKELETONS.add(new SkeletonPiece(bone, brightness, gib, vanillaSkeleton, rand));
        }
    }

    public static void giblets(ClientLevel level, RandomSource rand, double x, double y, double z,
            int entityId, int gibType) {
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof LivingEntity living) || living.getType() == EntityType.OCELOT) return;
        vanish(living);
        int count = (int) ((living.getBbWidth() / 0.25F) * 1.5F * (living.getBbHeight() / 0.25F));
        boolean launch = rand.nextInt(15) == 0;
        double mult = launch ? 10D : 1D;
        for (int i = 0; i < count; i++) {
            GIBLETS.add(new Giblet(x, y, z, rand.nextGaussian() * 0.25D * mult, rand.nextDouble() * mult,
                    rand.nextGaussian() * 0.25D * mult, gibType, rand));
        }
    }

    /** Hides the source entity for the same two-second client presentation window as ClientProxy. */
    public static void vanish(LivingEntity entity) {
        int id = entity.getId();
        VANISHED.putIfAbsent(id, new Vanish(entity, entity.isInvisible(), System.currentTimeMillis() + 2_000L));
        entity.setInvisible(true);
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            clear();
            return;
        }
        for (int i = SKELETONS.size() - 1; i >= 0; i--) {
            SKELETONS.get(i).tick(level);
            if (SKELETONS.get(i).expired()) SKELETONS.remove(i);
        }
        for (int i = GIBLETS.size() - 1; i >= 0; i--) {
            GIBLETS.get(i).tick(level);
            if (GIBLETS.get(i).expired()) GIBLETS.remove(i);
        }
        VANISHED.values().removeIf(vanish -> {
            if (vanish.until > System.currentTimeMillis()) return false;
            if (level.getEntity(vanish.entity.getId()) == vanish.entity) vanish.entity.setInvisible(vanish.wasInvisible);
            return true;
        });
    }

    private static void submit(CameraInfo camera) {
        if (SKELETONS.isEmpty() && GIBLETS.isEmpty()) return;
        float partial = Frames.partialTick();
        for (SkeletonPiece piece : List.copyOf(SKELETONS)) piece.submit(camera, partial);
        Vector3f right = camera.orientation.transform(new Vector3f(1F, 0F, 0F));
        Vector3f up = camera.orientation.transform(new Vector3f(0F, 1F, 0F));
        for (Giblet giblet : List.copyOf(GIBLETS)) giblet.submit(camera, partial, right, up);
    }

    private static void clear() {
        SKELETONS.clear();
        GIBLETS.clear();
        for (Vanish vanish : VANISHED.values()) vanish.entity.setInvisible(vanish.wasInvisible);
        VANISHED.clear();
    }

    private static Bone[] bones(LivingEntity entity) {
        float body = entity.yBodyRot;
        float head = entity.yHeadRot;
        float[] arm = rotate(0.375F, 0F, -body);
        float[] leg = rotate(0.125F, 0F, -body);
        if (entity instanceof Player) return biped(entity, body, head, arm, leg);
        EntityType<?> type = entity.getType();
        if (type == EntityType.ZOMBIE || type == EntityType.SKELETON || type == EntityType.ZOMBIFIED_PIGLIN)
            return zombie(entity, body, head, arm, leg);
        if (type == EntityType.VILLAGER || type == EntityType.WITCH)
            return villager(entity, body, head, arm, leg);
        return null;
    }

    private static Bone[] biped(LivingEntity e, float body, float head, float[] arm, float[] leg) {
        return new Bone[] {new Bone(Part.SKULL, -head, e.getXRot(), e.getX(), e.getY() + 1.75, e.getZ()),
                new Bone(Part.TORSO, -body, 0F, e.getX(), e.getY() + 1.125, e.getZ()),
                new Bone(Part.LIMB, -body, 0F, e.getX() + arm[0], e.getY() + 1.125, e.getZ() + arm[1]),
                new Bone(Part.LIMB, -body, 0F, e.getX() - arm[0], e.getY() + 1.125, e.getZ() - arm[1]),
                new Bone(Part.LIMB, -body, 0F, e.getX() + leg[0], e.getY() + 0.375, e.getZ() + leg[1]),
                new Bone(Part.LIMB, -body, 0F, e.getX() - leg[0], e.getY() + 0.375, e.getZ() - leg[1])};
    }

    private static Bone[] zombie(LivingEntity e, float body, float head, float[] arm, float[] leg) {
        float[] forward = rotate(0F, 0.25F, -body);
        return new Bone[] {new Bone(Part.SKULL, -head, e.getXRot(), e.getX(), e.getY() + 1.75, e.getZ()),
                new Bone(Part.TORSO, -body, 0F, e.getX(), e.getY() + 1.125, e.getZ()),
                new Bone(Part.LIMB, -body, -90F, e.getX() + arm[0] + forward[0], e.getY() + 1.375, e.getZ() + arm[1] + forward[1]),
                new Bone(Part.LIMB, -body, -90F, e.getX() - arm[0] + forward[0], e.getY() + 1.375, e.getZ() - arm[1] + forward[1]),
                new Bone(Part.LIMB, -body, 0F, e.getX() + leg[0], e.getY() + 0.375, e.getZ() + leg[1]),
                new Bone(Part.LIMB, -body, 0F, e.getX() - leg[0], e.getY() + 0.375, e.getZ() - leg[1])};
    }

    private static Bone[] villager(LivingEntity e, float body, float head, float[] arm, float[] leg) {
        float[] forward = rotate(0F, 0.25F, -body);
        return new Bone[] {new Bone(Part.VILLAGER_SKULL, -head, e.getXRot(), e.getX(), e.getY() + 1.6875, e.getZ()),
                new Bone(Part.TORSO, -body, 0F, e.getX(), e.getY() + 1, e.getZ()),
                new Bone(Part.LIMB, -body, -45F, e.getX() + arm[0] + forward[0], e.getY() + 1.125, e.getZ() + arm[1] + forward[1]),
                new Bone(Part.LIMB, -body, -45F, e.getX() - arm[0] + forward[0], e.getY() + 1.125, e.getZ() - arm[1] + forward[1]),
                new Bone(Part.LIMB, -body, 0F, e.getX() + leg[0], e.getY() + 0.375, e.getZ() + leg[1]),
                new Bone(Part.LIMB, -body, 0F, e.getX() - leg[0], e.getY() + 0.375, e.getZ() - leg[1])};
    }

    private static float[] rotate(float x, float z, float degrees) {
        double angle = Math.toRadians(degrees);
        return new float[] {(float) (x * Math.cos(angle) - z * Math.sin(angle)),
                (float) (x * Math.sin(angle) + z * Math.cos(angle))};
    }

    private enum Part { SKULL, TORSO, LIMB, VILLAGER_SKULL }
    private record Bone(Part part, float yaw, float pitch, double x, double y, double z) {}
    private record Vanish(LivingEntity entity, boolean wasInvisible, long until) {}

    private static final class SkeletonPiece {
        private double x, y, z, xo, yo, zo, xd, yd, zd;
        private float yaw, pitch, oYaw, oPitch, momentumYaw, momentumPitch;
        private boolean onGround;
        private final Part part;
        private final float brightness;
        private final Identifier texture;
        private int initialDelay;
        private final float gravity;
        private final int lifetime;
        private int age;

        SkeletonPiece(Bone bone, float brightness, boolean gib, boolean vanillaSkeleton, RandomSource rand) {
            this.x = this.xo = bone.x;
            this.y = this.yo = bone.y;
            this.z = this.zo = bone.z;
            this.yaw = this.oYaw = bone.yaw;
            this.pitch = this.oPitch = bone.pitch;
            this.part = bone.part;
            this.brightness = brightness;
            this.texture = gib && !vanillaSkeleton ? (part == Part.VILLAGER_SKULL ? SKELETON_BLOOD_EXT : SKELETON_BLOOD)
                    : (part == Part.VILLAGER_SKULL ? SKELETON_TEXTURE_EXT : SKELETON_TEXTURE);
            this.initialDelay = gib ? -2 : 20;
            this.gravity = gib ? 0.04F : 0.02F;
            this.lifetime = (gib ? 600 : 1200) + rand.nextInt(20);
            this.momentumPitch = rand.nextFloat() * 5F * (rand.nextBoolean() ? 1F : -1F);
            this.momentumYaw = rand.nextFloat() * 5F * (rand.nextBoolean() ? 1F : -1F);
            if (gib) {
                xd = rand.nextGaussian() * 0.25D;
                yd = (rand.nextGaussian() + 1D) * 0.25D;
                zd = rand.nextGaussian() * 0.25D;
            }
        }

        void tick(ClientLevel level) {
            xo = x; yo = y; zo = z; oYaw = yaw; oPitch = pitch;
            if (initialDelay-- > 0) return;
            if (initialDelay == -1) { xd = level.getRandom().nextGaussian() * 0.025D; zd = level.getRandom().nextGaussian() * 0.025D; }
            age++;
            yd -= gravity;
            boolean wasOnGround = onGround;
            onGround = move(level, xd, yd, zd);
            xd *= 0.98D; yd *= 0.98D; zd *= 0.98D;
            if (!onGround) { pitch += momentumPitch; yaw += momentumYaw; }
            else {
                xd = yd = zd = 0D;
                if (!wasOnGround) level.playLocalSound(x, y, z, SoundEvents.SKELETON_HURT,
                        SoundSource.PLAYERS, 0.25F, 0.8F + level.getRandom().nextFloat() * 0.4F, false);
            }
        }

        boolean move(ClientLevel level, double mx, double my, double mz) {
            AABB box = new AABB(x - 0.125, y, z - 0.125, x + 0.125, y + 0.25, z + 0.125);
            List<VoxelShape> collisions = new ArrayList<>();
            level.getBlockCollisions(null, box.expandTowards(mx, my, mz)).forEach(collisions::add);
            Vec3 moved = Entity.collideBoundingBox(null, new Vec3(mx, my, mz), box, level, collisions);
            x += moved.x; y += moved.y; z += moved.z;
            return my < 0D && moved.y != my;
        }

        boolean expired() { return age >= lifetime; }

        void submit(CameraInfo camera, float partial) {
            PoseStack pose = new PoseStack();
            pose.translate(Mth.lerp(partial, xo, x) - camera.pos.x, Mth.lerp(partial, yo, y) - camera.pos.y,
                    Mth.lerp(partial, zo, z) - camera.pos.z);
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, oYaw, yaw)));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, oPitch, pitch)));
            pose.mulPose(Axis.YP.rotationDegrees(-90F));
            int light = dev.golem.core.render.Renderers.lightAt(Minecraft.getInstance().level,
                    BlockPos.containing(x, y, z));
            float alpha = lifetime - (age + partial) < 40F
                    ? Math.max(0F, (lifetime - (age + partial)) / 40F) : 1F;
            int color = dev.golem.core.client.Colors.colorFromFloat(alpha, brightness, brightness, brightness);
            WorldEffects.submit(pose, CompatRenderTypes.entityTranslucent(texture), 0,
                    (drawPose, buffer) -> model().renderPart(part == Part.VILLAGER_SKULL ? "SkullVillager" : part.name().charAt(0) + part.name().substring(1).toLowerCase(),
                            drawPose, buffer, light, OverlayTexture.NO_OVERLAY, color));
        }

        private static WavefrontObj model() {
            if (model == null) model = WavefrontObj.load(SKELETON_MODEL);
            return model;
        }
    }

    private static final class Giblet {
        private double x, y, z, xo, yo, zo, xd, yd, zd;
        private float yaw, pitch, oYaw, oPitch, momentumYaw, momentumPitch;
        private final int type;
        private final int lifetime;
        private int age;
        Giblet(double x, double y, double z, double xd, double yd, double zd, int type, RandomSource rand) {
            this.x = this.xo = x; this.y = this.yo = y; this.z = this.zo = z;
            this.xd = xd; this.yd = yd; this.zd = zd; this.type = type; this.lifetime = 140 + rand.nextInt(20);
            this.momentumYaw = (float) rand.nextGaussian() * 15F;
            this.momentumPitch = (float) rand.nextGaussian() * 15F;
        }
        void tick(ClientLevel level) {
            xo = x; yo = y; zo = z; oYaw = yaw; oPitch = pitch; age++;
            yd -= 0.04D * (type == 2 ? 4D : 2D);
            AABB box = new AABB(x - 0.05, y - 0.05, z - 0.05, x + 0.05, y + 0.05, z + 0.05);
            List<VoxelShape> collisions = new ArrayList<>();
            level.getBlockCollisions(null, box.expandTowards(xd, yd, zd)).forEach(collisions::add);
            Vec3 moved = Entity.collideBoundingBox(null, new Vec3(xd, yd, zd), box, level, collisions);
            boolean onGround = yd < 0D && moved.y != yd;
            x += moved.x; y += moved.y; z += moved.z; xd *= 0.98D; yd *= 0.98D; zd *= 0.98D;
            if (!onGround) {
                yaw += momentumYaw;
                pitch += momentumPitch;
                if (type != 2) level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,
                        type == 1 ? Blocks.MELON.defaultBlockState() : Blocks.REDSTONE_BLOCK.defaultBlockState()), x, y, z, 0, 0, 0);
            }
        }
        boolean expired() { return age >= lifetime; }
        void submit(CameraInfo camera, float partial, Vector3f right, Vector3f up) {
            Identifier texture = type == 2 ? METAL : type == 1 ? SLIME : MEAT;
            float cx = (float) (Mth.lerp(partial, xo, x) - camera.pos.x);
            float cy = (float) (Mth.lerp(partial, yo, y) - camera.pos.y);
            float cz = (float) (Mth.lerp(partial, zo, z) - camera.pos.z);
            int light = dev.golem.core.render.Renderers.lightAt(Minecraft.getInstance().level,
                    BlockPos.containing(x, y, z));
            WorldEffects.submit(new PoseStack(), CompatRenderTypes.entityTranslucent(texture), 0,
                    (pose, buffer) -> {
                        float size = 0.1F;
                        float rx = right.x * size, ry = right.y * size, rz = right.z * size;
                        float ux = up.x * size, uy = up.y * size, uz = up.z * size;
                        Vertexes.vertex(buffer, pose, cx - rx - ux, cy - ry - uy, cz - rz - uz, 1F, 1F, 1F, 1F, 0F, 0F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
                        Vertexes.vertex(buffer, pose, cx - rx + ux, cy - ry + uy, cz - rz + uz, 1F, 1F, 1F, 1F, 0F, 1F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
                        Vertexes.vertex(buffer, pose, cx + rx + ux, cy + ry + uy, cz + rz + uz, 1F, 1F, 1F, 1F, 1F, 1F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
                        Vertexes.vertex(buffer, pose, cx + rx - ux, cy + ry - uy, cz + rz - uz, 1F, 1F, 1F, 1F, 1F, 0F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
                    });
        }
    }
}
