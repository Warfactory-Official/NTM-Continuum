package com.hbm.client.gun;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.client.CameraHooks;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The client half of 1.12.2's jade (occult) shotgun ({@code ItemGunJShotty}): its animation table (the recoil bus and
 * which Collada reload plays), the camera kick and muzzle smoke of a shot ({@code startAnim(CYCLE)}), the
 * {@code RecoilHandler} pitch it feeds, and its crosshair.
 */
public final class JadeShotgunClient {

    static final Identifier CROSSHAIR = Ids.of("hbm", "textures/misc/shotgun_crosshair.png");
    static final Identifier SMOKE = Ids.of("hbm", "textures/misc/smo0_blur4.png");

    /** The Collada reloads: one shell over 1.5 seconds, two over 3. */
    public static final String RELOAD_ONE = "JS_RELOAD0";
    public static final String RELOAD_TWO = "JS_RELOAD1";
    public static final String RECOIL = "JS_RECOIL";

    static final List<Smoke> SMOKES = new ArrayList<>();
    private static @Nullable ClientLevel lastLevel;

    /** 1.12.2 {@code RecoilHandler}: the view's upward kick, decaying in wall-clock time. */
    private static float verticalVelocity;
    private static float verticalRecoil;
    private static long lastRenderTime;

    /** For the harness: shots seen and smoke quads submitted since start. */
    public static int shotsSeen;
    public static int smokeDraws;
    /** The game time of the local player's last shot: the muzzle lights for that tick. */
    public static long lastShotTime = Long.MIN_VALUE;

    private JadeShotgunClient() {}

    /** Smoke puffs alive, for the harness. */
    public static int smokes() {
        return SMOKES.size();
    }

    public static void register() {
        ClientTicks.onEndTick(JadeShotgunClient::tick);
        WorldEffects.onBegin(JadeShotgunClient::render);
        CameraHooks.PITCH.register(JadeShotgunClient::pitchOffset);
    }

    /**
     * The animation for {@code cue}: the reload is chosen by the shells still loaded when it starts (1.12's
     * {@code startAnim}: one loaded plays the one-shell reload, none the two-shell one), a shot plays the recoil.
     */
    public static @Nullable BusAnimation animation(ItemStack stack, GunAnimation cue) {
        if (cue == GunAnimation.RELOAD) {
            int loaded = loaded(stack);
            if (loaded == 1) return hold(RELOAD_ONE, 1500);
            if (loaded == 0) return hold(RELOAD_TWO, 3000);
            return null;
        }
        if (cue == GunAnimation.CYCLE) {
            onShot();
            return new BusAnimation().addBus(RECOIL, new BusAnimationSequence()
                    .addPos(0.5, 0, 0, 50).addPos(0.4, 0, 0, 150).addPos(0, 0, 0, 250));
        }
        return null;
    }

    /** A bus that only marks which Collada animation runs and for how long. */
    private static BusAnimation hold(String bus, int millis) {
        return new BusAnimation().addBus(bus, new BusAnimationSequence().hold(millis));
    }

    /** The shells in the gun, as the client last saw them. */
    public static int loaded(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return 0;
        return gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
    }

    /** {@code startAnim(CYCLE)}: the camera kick in first person and one smoke puff at the muzzle. */
    private static void onShot() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        shotsSeen++;
        lastShotTime = mc.level.getGameTime();
        if (mc.options.getCameraType().isFirstPerson()) verticalVelocity = 10F;
        float partial = Frames.partialTick();
        Vec3 start = new Vec3(-0.28, -0.1, 2).xRot(-(float) Math.toRadians(player.getXRot()))
                .yRot(-(float) Math.toRadians(player.getYRot())).add(player.getEyePosition(partial));
        Vec3 look = randVecInCone(player.getViewVector(partial), 20, mc.level.getRandom()).scale(0.1F);
        Smoke smoke = new Smoke(mc.level.getRandom(), start.x, start.y, start.z, 0.05F, 4, 0.15F, 5);
        smoke.gravity = -0.01F;
        smoke.motionX = (float) look.x;
        smoke.motionY = (float) look.y;
        smoke.motionZ = (float) look.z;
        smoke.opacity = 0.5F;
        SMOKES.add(smoke);
    }

    /**
     * 1.12.2 {@code BobMathUtil.randVecInCone}: up, turned about x by a random angle up to twice {@code angle}
     * (a random fraction of {@code angle + random * angle}), about y by a random turn, then onto {@code direction}.
     */
    static Vec3 randVecInCone(Vec3 direction, float angle, RandomSource rand) {
        Vec3 up = new Vec3(0, 1, 0);
        up = rotateAroundX(up, (float) Math.toRadians(rand.nextFloat() * (angle + rand.nextFloat() * angle)));
        up = rotateAroundY(up, (float) Math.toRadians(rand.nextFloat() * 360));
        double yaw = Math.toDegrees(Math.atan2(direction.x, direction.z));
        double pitch = Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
        up = rotateAroundX(up, (float) Math.toRadians(pitch - 90));
        return rotateAroundY(up, (float) Math.toRadians(yaw));
    }

    /** The amlfrom1710 {@code Vec3.rotateAroundX}: y' = y cos + z sin, z' = z cos - y sin. */
    private static Vec3 rotateAroundX(Vec3 v, float angle) {
        float c = Mth.cos(angle), s = Mth.sin(angle);
        return new Vec3(v.x, v.y * c + v.z * s, v.z * c - v.y * s);
    }

    /** The amlfrom1710 {@code Vec3.rotateAroundY}: x' = x cos + z sin, z' = z cos - x sin. */
    private static Vec3 rotateAroundY(Vec3 v, float angle) {
        float c = Mth.cos(angle), s = Mth.sin(angle);
        return new Vec3(v.x * c + v.z * s, v.y, v.z * c - v.x * s);
    }

    /** {@code RecoilHandler.modifiyCamera}: called once per camera setup, raises the view by the decaying kick. */
    private static float pitchOffset(Entity entity) {
        long currentTime = System.currentTimeMillis();
        float scale = (currentTime - lastRenderTime) / 1000F;
        float settle = 20F * Mth.clamp(verticalRecoil / 4, 0, 200);
        verticalRecoil = Math.max(0, verticalRecoil - scale * settle + verticalVelocity);
        verticalVelocity *= 0.35F * scale;
        lastRenderTime = currentTime;
        return -verticalRecoil;
    }

    /**
     * 1.12.2 {@code ItemGunJShotty.renderHUD}: the 40 by 40 {@code shotgun_crosshair}, tinted (0.7, 1, 0.5) and added
     * to the screen, in place of the vanilla crosshair.
     */
    public static void renderCrosshair(GuiGraphicsExtractor graphics) {
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2;
        GuiDraw.blitUv(graphics, CROSSHAIR, x - 20, y - 20, 40, 40, 0F, 0F, 1F, 1F, 256, 256, true, 0xFFB2FF7F);
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            SMOKES.clear();
            lastLevel = level;
        }
        if (level == null) return;
        SMOKES.removeIf(smoke -> smoke.tick(level));
    }

    private static void render(CameraInfo camera) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || SMOKES.isEmpty()) return;
        float partial = Frames.partialTick();
        Vector3f right = camera.orientation.transform(new Vector3f(1F, 0F, 0F));
        Vector3f up = camera.orientation.transform(new Vector3f(0F, 1F, 0F));
        Vec3 view = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getViewVector(partial) : Vec3.ZERO;
        for (Smoke smoke : SMOKES) smoke.render(level, camera.pos, right, up, view, partial);
    }

    /** 1.12.2 {@code ParticleSmokeAnim}: a 9 by 9 sheet of smoke frames, growing, rising and fading. */
    static final class Smoke {
        double posX, posY, posZ, prevPosX, prevPosY, prevPosZ;
        double motionX, motionY, motionZ;
        AABB box;
        boolean onGround;
        final float speed;
        final int offset;
        float scale, prevScale, scaleOverLife;
        float angle, prevAngle;
        float gravity = 0.04F;
        float opacity = 1F;
        int age;
        final int maxAge;

        Smoke(RandomSource rand, double x, double y, double z, float speed, float scale, float scaleOverLife, int lifetime) {
            posX = prevPosX = x;
            posY = prevPosY = y;
            posZ = prevPosZ = z;
            box = new AABB(x - 0.1, y, z - 0.1, x + 0.1, y + 0.2, z + 0.1);
            // 1.12's Particle constructor jittered the motion, which ParticleSmokeAnim's caller then replaced
            this.scale = prevScale = scale;
            angle = prevAngle = (float) (rand.nextFloat() * Math.PI * 2);
            this.speed = speed;
            maxAge = lifetime * 2;
            offset = rand.nextInt(81);
            this.scaleOverLife = scaleOverLife;
        }

        /** vanilla 1.12 {@code Particle.onUpdate} (gravity 0), then the smoke's own drag, lift and growth. */
        boolean tick(ClientLevel level) {
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            boolean expired = age++ >= maxAge;
            Vec3 wanted = new Vec3(motionX, motionY, motionZ);
            Vec3 allowed = Entity.collideBoundingBox(null, wanted, box, level, List.of());
            box = box.move(allowed);
            posX = (box.minX + box.maxX) / 2.0;
            posY = box.minY;
            posZ = (box.minZ + box.maxZ) / 2.0;
            onGround = wanted.y != allowed.y && wanted.y < 0;
            if (wanted.x != allowed.x) motionX = 0;
            if (wanted.z != allowed.z) motionZ = 0;
            motionX *= 0.98F;
            motionY *= 0.98F;
            motionZ *= 0.98F;
            if (onGround) {
                motionX *= 0.7F;
                motionZ *= 0.7F;
            }
            motionX *= 0.9F;
            motionY *= 0.9F;
            motionZ *= 0.9F;
            motionY -= gravity;
            prevScale = scale;
            scale += scaleOverLife;
            scaleOverLife *= 0.95F;
            return expired;
        }

        void render(ClientLevel level, Vec3 cam, Vector3f right, Vector3f up, Vec3 view, float partial) {
            float fade = Mth.clamp((age + partial) / maxAge, 0F, 1F);
            fade = (float) (1 - Math.log10(fade * 9 + 1));
            float alpha = fade * 0.3F * opacity;
            int index = (int) ((age + partial) * 25F * speed + offset) % 81;
            float size = 1 / 9F;
            float u = (index % 9) * size;
            float v = (index / 9) * size;
            float f4 = 0.1F * (prevScale + (scale - prevScale) * partial);
            float x = (float) (prevPosX + (posX - prevPosX) * partial - cam.x);
            float y = (float) (prevPosY + (posY - prevPosY) * partial - cam.y);
            float z = (float) (prevPosZ + (posZ - prevPosZ) * partial - cam.z);
            int light = Renderers.lightAt(level, BlockPos.containing(posX, posY, posZ));

            float f8 = angle + (angle - prevAngle) * partial;
            float f9 = Mth.cos(f8 * 0.5F);
            Vector3f axis = new Vector3f((float) view.x, (float) view.y, (float) view.z).mul(Mth.sin(f8 * 0.5F));
            Vector3f[] corners = {
                    new Vector3f(right).mul(-f4).sub(new Vector3f(up).mul(f4)),
                    new Vector3f(right).mul(-f4).add(new Vector3f(up).mul(f4)),
                    new Vector3f(right).mul(f4).add(new Vector3f(up).mul(f4)),
                    new Vector3f(right).mul(f4).sub(new Vector3f(up).mul(f4))};
            if (angle != 0F) {
                for (Vector3f c : corners) {
                    Vector3f a = new Vector3f(axis).mul(2F * c.dot(axis));
                    Vector3f b = new Vector3f(c).mul(f9 * f9 - axis.dot(axis));
                    Vector3f d = new Vector3f(axis).cross(c).mul(2F * f9);
                    c.set(a.add(b).add(d));
                }
            }
            float[][] uv = {{u + size, v + size}, {u + size, v}, {u, v}, {u, v + size}};
            int color = (Mth.clamp((int) (alpha * 255F), 0, 255) << 24) | 0xFFFFFF;
            smokeDraws++;
            WorldEffects.submit(new PoseStack(), CompatRenderTypes.flatTranslucent(SMOKE), 1, (p, buffer) -> {
                for (int i = 0; i < 4; i++) {
                    Vertexes.vertex(buffer, p, x + corners[i].x, y + corners[i].y, z + corners[i].z, color, uv[i][0], uv[i][1],
                            OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
                }
            });
        }
    }
}
