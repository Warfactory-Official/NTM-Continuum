package com.hbm.client.gun;

import com.hbm.client.HbmKeybindsClient;
import com.hbm.client.particle.GluonParticles;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.weapon.sedna.impl.ItemGunEgon;
import com.hbm.sound.ModGunSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.NoiseDisplacedRenderType;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.world.TimeOfDay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
 * The client half of 1.12.2's gluon gun: {@code ItemGunEgon.updateClient}, the beams and burn trails
 * {@code ModEventHandlerClient} drew, {@code GunEgonSoundHandler} and the HUD dot.
 * <p>port: every beam is drawn in Core's late world pass. 1.12 drew another player's beam before translucent terrain,
 * so seen through water it was tinted; here it is hidden.
 */
public final class GluonGunClient {

    static final Identifier NOISE = Ids.of("hbm", "textures/misc/noise_2.png");
    static final Identifier BEAM = Ids.of("hbm", "textures/misc/gluonbeam.png");
    static final Identifier LIGHTNING = Ids.of("hbm", "textures/models/bfg/additivebeam.png");
    static final Identifier HUD = Ids.of("hbm", "textures/misc/gluontau_hud.png");
    public static final Identifier MUZZLE_SMOKE = Ids.of("hbm", "textures/misc/gluon_muzzle_smoke.png");
    public static final Identifier MUZZLE_GLOW = Ids.of("hbm", "textures/misc/gluon_muzzle_glow.png");

    /** 1.12 {@code ItemGunEgon.activeTicks}: the local beam's ramp, 0 to 5, frozen while the gun is not held. */
    public static int activeTicks;
    /** The gluon's share of 1.12's {@code firstPersonAuxParticles}, drawn by the gun's own renderer. */
    public static final List<MuzzleSmoke> MUZZLE = new ArrayList<>();
    private static final Map<Player, BeamSounds> SOUNDS = new HashMap<>();
    private static @Nullable ClientLevel lastLevel;
    private static int nextSlot;

    /** For the harness: beams drawn since start. */
    public static int beamsDrawn;

    private GluonGunClient() {}

    public static void register() {
        ClientTicks.onEndTick(GluonGunClient::tick);
        WorldEffects.onBegin(GluonGunClient::render);
        GluonParticles.register();
    }

    private static boolean holdsEgon(Player player) {
        return player.getMainHandItem().getItem() instanceof ItemGunEgon;
    }

    /** 1.12 {@code m1 && countInventoryItem(beltType) >= 2}: the local trigger is down and two rounds are carried. */
    public static boolean localFiring(Player player) {
        ItemStack held = player.getMainHandItem();
        return held.getItem() instanceof ItemGunEgon gun && HbmKeybindsClient.isHeld(EnumKeybind.GUN_PRIMARY)
                && gun.hasBeamAmmo(held, player);
    }

    /** Whether {@code player}'s beam is on: the local player's own state, the server's flag for anyone else. */
    public static boolean firing(Player player) {
        return player == Minecraft.getInstance().player ? localFiring(player) : ItemGunEgon.isFiring(player);
    }

    /** {@code getFirstPersonAnimFade}: how far the held gun has kicked into its firing pose, 0 to 1. */
    public static float firstPersonFade(Player player, float partial) {
        if (!holdsEgon(player)) return 0F;
        return Mth.clamp((localFiring(player) ? activeTicks + partial : activeTicks - partial) / 5F, 0F, 1F);
    }

    /** {@code ItemRenderGunEgon.getOffset}: the slow sway of the firing gun. */
    public static float[] getOffset(float time) {
        float sinval = Mth.sin(time * 0.15F) + Mth.sin(time * 0.25F - 10) + Mth.sin(time * 0.1F + 10);
        sinval /= 3;
        float sinval2 = Mth.sin(time * 0.1F) + Mth.sin(time * 0.05F + 20) + Mth.sin(time * 0.13F + 20);
        sinval /= 3;
        return new float[] {ItemGunEgon.remap(smoothstep(sinval, -1, 1), 0, 1, -2, 1.5F),
                ItemGunEgon.remap(sinval2, -1, 1, -0.03F, 0.05F)};
    }

    /** {@code ItemRenderGunEgon.getJitter}: the fast shake of the firing gun. */
    public static float[] getJitter(float time) {
        float sinval = Mth.sin(time * 0.8F) + Mth.sin(time * 0.6F - 10) + Mth.sin(time * 0.9F + 10);
        sinval /= 3;
        float sinval2 = Mth.sin(time * 0.3F) + Mth.sin(time * 0.2F + 20) + Mth.sin(time * 0.1F + 20);
        sinval /= 3;
        return new float[] {ItemGunEgon.remap(sinval, -1, 1, -3, 3), ItemGunEgon.remap(sinval2, -1, 1, -1F, 1F)};
    }

    private static float smoothstep(float t, float edge0, float edge1) {
        t = Mth.clamp((t - edge0) / (edge1 - edge0), 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    /** The day clock plus {@code partial}, as 1.12's {@code world.getWorldTime() + partialTicks}. */
    static float time(Level level, float partial) {
        return TimeOfDay.get(level) + partial;
    }

    /** The beam's direction this frame: the look vector turned by {@link ItemGunEgon#getBeamDirectionOffset}. */
    static Vec3 beamLook(Player player, float time, float partial) {
        float[] angles = ItemGunEgon.getBeamDirectionOffset(time);
        return ItemGunEgon.changeByAngle(player.getViewVector(partial), angles[0], angles[1]);
    }

    /** Where the flare sits: 0.1 off the face the beam hit, or 50 blocks out when it hit nothing. */
    public static Vec3 flarePosition(Level level, Player player, float partial) {
        Vec3 look = beamLook(player, time(level, partial), partial);
        ItemGunEgon.Hit hit = ItemGunEgon.trace(level, player, look, partial);
        if (hit != null) {
            Direction n = hit.side();
            return hit.location().add(n.getStepX() * 0.1F, n.getStepY() * 0.1F, n.getStepZ() * 0.1F);
        }
        return player.getEyePosition(partial).add(look.scale(ItemGunEgon.RANGE));
    }

    /**
     * 1.12's {@code currentFOV}, the field of view of the last projection set up in a frame, which was the hand's:
     * 70, or 60 with the eyes under water.
     */
    private static float handFov(Player player) {
        return player.isEyeInFluid(FluidTags.WATER) ? 60F : 70F;
    }

    // Ticking

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            MUZZLE.clear();
            SOUNDS.values().forEach(BeamSounds::end);
            SOUNDS.clear();
            lastLevel = level;
        }
        MUZZLE.removeIf(MuzzleSmoke::tick);
        LocalPlayer local = mc.player;
        if (level == null || local == null) return;
        if (holdsEgon(local)) updateLocal(level, local);

        for (Player player : level.players()) {
            if (holdsEgon(player) && !SOUNDS.containsKey(player) && firing(player)) {
                SOUNDS.put(player, new BeamSounds(player));
            }
        }
        Iterator<BeamSounds> sounds = SOUNDS.values().iterator();
        while (sounds.hasNext()) {
            BeamSounds handler = sounds.next();
            handler.update(level);
            if (handler.ticks == -1) sounds.remove();
        }
    }

    /** {@code ItemGunEgon.updateClient} for the main hand of the local player. */
    private static void updateLocal(ClientLevel level, LocalPlayer player) {
        if (!localFiring(player)) {
            activeTicks = Math.max(activeTicks - 1, 0);
            GluonParticles.endTrail(player);
            return;
        }
        activeTicks = Math.min(activeTicks + 1, 5);
        long worldTime = TimeOfDay.get(level);
        Vec3 look = beamLook(player, worldTime + 1, 1F);
        ItemGunEgon.Hit hit = ItemGunEgon.trace(level, player, look, 1F);
        if (hit != null) {
            Direction n = hit.side();
            Vec3 pos = hit.location().add(n.getStepX() * 0.1F, n.getStepY() * 0.1F, n.getStepZ() * 0.1F);
            GluonParticles.flare(level, pos.x, pos.y, pos.z, player);
        } else {
            Vec3 pos = player.getEyePosition(1F).add(look.scale(ItemGunEgon.RANGE));
            GluonParticles.flare(level, pos.x, pos.y, pos.z, player);
        }

        RandomSource rand = level.getRandom();
        float partial = Frames.partialTick();
        float[] offset = getOffset(worldTime + partial);
        float fovDiff = (handFov(player) - 70) * 0.0002F;
        Vec3 start = muzzle(player, partial, -0.18 + offset[0] * 0.075F - fovDiff, -0.2 + offset[1] * 0.1F,
                0.5 - fovDiff * 30).add(player.getEyePosition(partial));
        for (int i = 0; i < 2; i++) {
            GluonParticles.spark(rand, start.x + (rand.nextFloat() - 0.5) * 0.05, start.y + (rand.nextFloat() - 0.5) * 0.05,
                    start.z + (rand.nextFloat() - 0.5) * 0.05);
        }

        if (worldTime % 2 == 0) {
            MUZZLE.add(new MuzzleSmoke(rand, 0, 0, 4.1, 0, MUZZLE_SMOKE, 10, 50, 9)
                    .color(0.2F, 0.4F + rand.nextFloat() * 0.5F, 1F, 3F).lifetime(10));
        }
        if (worldTime % 4 == 0) {
            MUZZLE.add(new MuzzleSmoke(rand, 0, 0, 4, 1, MUZZLE_GLOW, 30, 50, -1)
                    .color(0.2F, 0.4F + rand.nextFloat() * 0.5F, 1F, 2F).lifetime(16));
        }
        if (activeTicks < 3) {
            for (int i = 0; i < 3; i++) {
                MUZZLE.add(new MuzzleSmoke(rand, 0, 0, 4.1, 0, MUZZLE_SMOKE, 10, 50, 25).color(0.2F, 0.4F, 1F, 3F).lifetime(7));
            }
            // 1.12 also added a flare-textured smoke on the first tick, which its renderer never drew
        }
    }

    /** A point in the shooter's view frame: turned by pitch, then yaw, as 1.12's {@code rotatePitch}/{@code rotateYaw}. */
    private static Vec3 muzzle(Player player, float partial, double x, double y, double z) {
        float pitch = player.xRotO + (player.getXRot() - player.xRotO) * partial;
        float yaw = player.yRotO + (player.getYRot() - player.yRotO) * partial;
        return new Vec3(x, y, z).xRot((float) Math.toRadians(-pitch)).yRot((float) Math.toRadians(-yaw));
    }

    // Drawing

    private static void render(CameraInfo camera) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer local = mc.player;
        if (level == null || local == null) return;
        float partial = Frames.partialTick();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        nextSlot = 0;

        if (holdsEgon(local) && activeTicks > 0 && firstPerson) {
            float time = time(level, partial);
            Vec3 look = beamLook(local, time, partial);
            Vec3 eyes = local.getEyePosition(partial);
            Vec3 hitPos = hitPosition(level, local, look, partial, eyes);
            float[] offset = getOffset(time);
            float fovDiff = (handFov(local) - 70) * 0.0002F;
            Vec3 start = muzzle(local, partial, -0.18 + offset[0] * 0.075F - fovDiff, -0.2 + offset[1] * 0.1F,
                    0.35 - fovDiff * 30);
            submitBeam(start.add(eyes).subtract(camera.pos), hitPos.subtract(eyes).subtract(start), 0.4F);
        }

        for (Player player : level.players()) {
            if (!holdsEgon(player)) {
                GluonParticles.endTrail(player);
                continue;
            }
            boolean firing = firing(player);
            if (firing && (player != local || !firstPerson) && nextSlot < NoiseDisplacedRenderType.SLOTS) {
                Vec3 look = beamLook(player, time(level, partial), partial);
                Vec3 eyes = player.getEyePosition(partial);
                Vec3 hitPos = hitPosition(level, player, look, partial, eyes);
                Vec3 start = muzzle(player, partial, -0.18, -0.1, 0.35);
                submitBeam(start.add(eyes).subtract(camera.pos), hitPos.subtract(eyes), 0.4F);
            }
            if (!firing) {
                GluonParticles.endTrail(player);
                continue;
            }
            Vec3 look = beamLook(player, time(level, partial), partial);
            ItemGunEgon.Hit hit = ItemGunEgon.trace(level, player, look, partial);
            if (hit != null && hit.entity() == null) {
                Direction n = hit.side();
                Vec3 normal = new Vec3(n.getStepX(), n.getStepY(), n.getStepZ());
                GluonParticles.trail(player, hit.location().add(normal.scale(0.02)), normal);
            } else {
                GluonParticles.endTrail(player);
            }
        }
    }

    /** The beam's far end: 0.1 short of what it hit, or 50 blocks out. */
    private static Vec3 hitPosition(Level level, Player player, Vec3 look, float partial, Vec3 eyes) {
        ItemGunEgon.Hit hit = ItemGunEgon.trace(level, player, look, partial);
        return hit == null ? eyes.add(look.scale(ItemGunEgon.RANGE)) : hit.location().add(look.scale(-0.1));
    }

    /**
     * {@code BeamPronter.gluonBeam(0, delta, size)} with its origin at {@code start} (camera-relative): the beam's
     * frame is turned so its x axis runs back along {@code delta}'s reverse, as 1.12's Euler angles of
     * {@code pos1 - pos2} did, then the spiral (16 segments a block) and the ribbon (8 a block) are drawn in it.
     */
    private static void submitBeam(Vec3 start, Vec3 delta, float size) {
        Vec3 diff = delta.scale(-1);
        float len = (float) diff.length();
        int spiralSegments = (int) Math.ceil(len * 16);
        int beamSegments = (int) Math.ceil(len * 8);
        if (spiralSegments <= 0) return;
        float yaw = (float) Math.toDegrees(Math.atan2(diff.x, diff.z));
        float pitch = (float) Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));
        PoseStack pose = new PoseStack();
        pose.translate(start.x, start.y, start.z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw + 90));
        pose.mulPose(Axis.ZP.rotationDegrees(-pitch));

        Matrix4f beamToRender = new Matrix4f(pose.last().pose());
        // the camera sits at the render origin; 1.12 read it from the inverse model-view
        Vector3f camera = new Matrix4f(beamToRender).invert().transformPosition(new Vector3f());
        Vector3f ax = beamToRender.transformDirection(new Vector3f(1, 0, 0));
        Vector3f ay = beamToRender.transformDirection(new Vector3f(0, 1, 0));
        Vector3f az = beamToRender.transformDirection(new Vector3f(0, 0, 1));
        float time = (System.currentTimeMillis() % 10000000) / 1000F;
        Matrix4f params = new Matrix4f(
                ax.x, ax.y, ax.z, time,
                ay.x, ay.y, ay.z, len,
                az.x, az.y, az.z, 1F / (spiralSegments + 1),
                camera.x, camera.y, camera.z, size);
        int slot = nextSlot++;
        NoiseDisplacedRenderType.params(slot, params);

        float halfWidth = size * 0.025F;
        WorldEffects.submit(pose, NoiseDisplacedRenderType.textured("hbm:gluon_spiral", NOISE, LIGHTNING, slot), 2,
                (p, buffer) -> {
                    for (int i = 0; i < spiralSegments; i++) {
                        float u0 = i / (float) spiralSegments, u1 = (i + 1) / (float) spiralSegments;
                        NoiseDisplacedRenderType.vertex(buffer, p, u0 * len, 0, -halfWidth, u0, 0.45F, -1);
                        NoiseDisplacedRenderType.vertex(buffer, p, u0 * len, 0, halfWidth, u0, 0.55F, -1);
                        NoiseDisplacedRenderType.vertex(buffer, p, u1 * len, 0, halfWidth, u1, 0.55F, -1);
                        NoiseDisplacedRenderType.vertex(buffer, p, u1 * len, 0, -halfWidth, u1, 0.45F, -1);
                    }
                });
        // the ribbon's width runs across the line from the camera, (0, cam.z, -cam.y) normalised
        Vec3 across = new Vec3(0, camera.z, -camera.y).normalize();
        float vy = (float) across.y, vz = (float) across.z;
        WorldEffects.submit(pose, NoiseDisplacedRenderType.textured("hbm:gluon_beam", NOISE, BEAM, slot), 2,
                (p, buffer) -> {
                    for (int i = 0; i < beamSegments; i++) {
                        float u0 = i / (float) beamSegments, u1 = (i + 1) / (float) beamSegments;
                        NoiseDisplacedRenderType.vertex(buffer, p, u0 * len, -vy, -vz, u0, 0F, -1);
                        NoiseDisplacedRenderType.vertex(buffer, p, u0 * len, vy, vz, u0, 1F, -1);
                        NoiseDisplacedRenderType.vertex(buffer, p, u1 * len, vy, vz, u1, 1F, -1);
                        NoiseDisplacedRenderType.vertex(buffer, p, u1 * len, -vy, -vz, u1, 0F, -1);
                    }
                });
        beamsDrawn++;
    }

    /**
     * 1.12 {@code ItemGunEgon.renderHud}: a 4 by 4 dot of {@code gluontau_hud} on the crosshair, tinted (0.9, 0.9, 0)
     * and added to the screen.
     */
    public static void renderCrosshair(GuiGraphicsExtractor graphics) {
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2;
        GuiDraw.blitUv(graphics, HUD, x - 2, y - 2, 4, 4, 0F, 0F, 1F, 1F, 32, 32, true, 0xFFE5E500);
    }

    /**
     * 1.12.2 {@code ParticleGluonMuzzleSmoke}: a quad in the held gun's own space at the muzzle, turning, growing and
     * fading over its life; type 1 drifts back along the barrel.
     */
    public static final class MuzzleSmoke {
        public final Identifier texture;
        final int type;
        double posX, posY, posZ, prevPosX, prevPosY, prevPosZ;
        final float scale;
        final float rotationOverLife;
        final float scaleOverLife;
        final float angle;
        float r = 1F, g = 1F, b = 1F, alpha = 1F;
        int age;
        int maxAge;

        MuzzleSmoke(RandomSource rand, double x, double y, double z, int type, Identifier texture, float scale, float rot,
                    float scaleOverLife) {
            posX = prevPosX = x;
            posY = prevPosY = y;
            posZ = prevPosZ = z;
            this.scale = scale;
            this.rotationOverLife = rot;
            this.angle = rand.nextFloat() * 360;
            this.scaleOverLife = scaleOverLife;
            this.texture = texture;
            this.type = type;
        }

        MuzzleSmoke color(float r, float g, float b, float alpha) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.alpha = alpha;
            return this;
        }

        MuzzleSmoke lifetime(int lifetime) {
            this.maxAge = lifetime;
            return this;
        }

        boolean tick() {
            age++;
            if (type == 1) {
                prevPosZ = posZ;
                posZ -= 0.075F;
            }
            return age >= maxAge;
        }

        /**
         * The quad's corners and colour as 1.12's {@code renderParticle} wrote them: {@code consumer} receives
         * x, y, z, u, v and the ARGB colour, once per stacked copy (an alpha above 1 drew that many).
         */
        public void emit(float partial, QuadSink consumer) {
            float timeScale = (age + partial) / (float) maxAge;
            float workingAlpha = Mth.clamp(1 - ItemGunEgon.remap(Mth.clamp(timeScale, 0, 1), 0.6F, 1F, 0.6F, 1F), 0, 1) * alpha;
            workingAlpha *= Mth.clamp(ItemGunEgon.remap(Mth.clamp(timeScale, 0, 0.2F), 0F, 0.2F, 0F, 1F), 0, 1);
            float f4 = 0.1F * (scale + timeScale * 5F * scaleOverLife);
            float x = (float) (prevPosX + (posX - prevPosX) * partial);
            float y = (float) (prevPosY + (posY - prevPosY) * partial);
            float z = (float) (prevPosZ + (posZ - prevPosZ) * partial);
            float rad = (float) Math.toRadians(angle + timeScale * rotationOverLife);
            float cos = Mth.cos(rad), sin = Mth.sin(rad);
            float[][] corners = {{0.5F, 0.5F}, {-0.5F, 0.5F}, {-0.5F, -0.5F}, {0.5F, -0.5F}};
            float[][] uv = {{1F, 1F}, {1F, 0F}, {0F, 0F}, {0F, 1F}};
            float a = workingAlpha;
            for (int i = 0; i < workingAlpha; i++) {
                int color = (Mth.clamp((int) (Math.min(a, 1F) * 255F), 0, 255) << 24) | ((int) (r * 255F) << 16)
                        | ((int) (g * 255F) << 8) | (int) (b * 255F);
                for (int c = 0; c < 4; c++) {
                    float cx = corners[c][0] * f4, cy = corners[c][1] * f4;
                    // Vec3.rotateAroundZ: x' = x cos + y sin, y' = y cos - x sin
                    consumer.vertex(x + cx * cos + cy * sin, y + cy * cos - cx * sin, z, uv[c][0], uv[c][1], color);
                }
                a -= 1F;
            }
        }

        @FunctionalInterface
        public interface QuadSink {
            void vertex(float x, float y, float z, float u, float v, int color);
        }
    }

    /** 1.12.2 {@code GunEgonSoundHandler}: start, then the loop from the eighth tick, a hit every other tick, end. */
    private static final class BeamSounds {
        int ticks = -1;
        final Player player;
        final Loop loop;

        BeamSounds(Player player) {
            this.player = player;
            this.loop = new Loop(ModGunSounds.GLUON_LOOP.get(), player);
        }

        void update(ClientLevel level) {
            boolean firing = holdsEgon(player) && firing(player);
            if (ticks < 0 && firing) ticks = 0;
            if (ticks < 0) return;
            if (!firing || !player.isAlive()) {
                loop.done();
                ticks = -1;
                record(ModGunSounds.GLUON_END.get());
                return;
            }
            if (ticks == 0) {
                record(ModGunSounds.GLUON_START.get());
            } else if (ticks == 8) {
                Minecraft.getInstance().getSoundManager().play(loop);
            }
            Vec3 look = beamLook(player, TimeOfDay.get(level) + 1, 1F);
            ItemGunEgon.Hit hit = ItemGunEgon.trace(level, player, look, 1F);
            if (hit != null && hit.entity() instanceof LivingEntity && TimeOfDay.get(level) % 2 == 0) {
                record(ModGunSounds.GLUON_HIT.get());
            }
            ticks++;
        }

        void end() {
            loop.done();
        }

        /** {@code PositionedSoundRecord.getRecordSoundRecord}: the records channel, volume 4, at the player's feet. */
        private void record(SoundEvent sound) {
            Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.RECORDS, 4F, 1F,
                    SoundInstance.createUnseededRandom(), player.getX(), player.getY(), player.getZ()));
        }
    }

    /** 1.12.2 {@code SoundLoopGunEgonFire}: repeats with no delay and follows the player's feet. */
    private static final class Loop extends AbstractTickableSoundInstance {
        private final Player player;

        Loop(SoundEvent sound, Player player) {
            super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        @Override
        public void tick() {
            x = player.getX();
            y = player.getY();
            z = player.getZ();
        }

        void done() {
            stop();
        }
    }
}
