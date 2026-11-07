package com.hbm.client.particle;

import com.hbm.Constants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ParticleRBMKFlame}: an additive fire quad that turns only about Y, twice as tall as it is wide, so it
 * cannot be a vanilla square particle. 1.7 draws it one block to the camera's right of its position and picks frames
 * with {@code age * 5 % 14 % 5}, so only the sheet's first five of 14 frames ever show.
 */
public final class RBMKFlameParticles {

    private static final Identifier TEXTURE = Ids.of(Constants.MOD_ID, "textures/particle/rbmk_fire.png");
    private static final List<Flame> ACTIVE = new ArrayList<>();
    private static boolean initialized;
    private static ClientLevel lastLevel;

    private RBMKFlameParticles() {}

    public static void register() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        ClientTicks.onEndTick(RBMKFlameParticles::tick);
        WorldEffects.onBegin(camera -> {
            if (Minecraft.getInstance().level == null) return;
            for (Flame flame : List.copyOf(ACTIVE)) flame.submit(camera);
        });
        ClientResourceReloads.register(Constants.MOD_ID, "rbmk_flame", ACTIVE::clear);
    }

    public static void spawn(Level level, double x, double y, double z, int maxAge) {
        if (!(level instanceof ClientLevel client)) return;
        ACTIVE.add(new Flame(x, y, z, maxAge, client.getRandom().nextFloat() + 1F));
    }

    /** Live flames, for the harness. */
    public static int active() {
        return ACTIVE.size();
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            ACTIVE.clear();
            lastLevel = level;
        }
        if (level == null) return;
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            if (ACTIVE.get(i).age++ >= ACTIVE.get(i).maxAge) ACTIVE.remove(i);
        }
    }

    private static final class Flame {
        private final double x;
        private final double y;
        private final double z;
        private final int maxAge;
        private final float scale;
        private int age;

        private Flame(double x, double y, double z, int maxAge, float scale) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.maxAge = maxAge;
            this.scale = scale;
        }

        private void submit(CameraInfo camera) {
            int shown = Math.min(age, maxAge);
            float alpha = 1F;
            if (shown < 20) alpha = shown / 20F;
            if (shown > maxAge - 20) alpha = (maxAge - shown) / 20F;
            float faded = alpha * 0.5F;
            float u0 = shown * 5 % 14 % 5 / 14F;
            float u1 = u0 + 1F / 14F;

            PoseStack pose = new PoseStack();
            pose.translate(x - camera.pos.x, y - camera.pos.y, z - camera.pos.z);
            pose.mulPose(Axis.YP.rotationDegrees(-camera.yRot));
            WorldEffects.submit(pose, CompatRenderTypes.energySwirl(TEXTURE, 0F, 0F), 1, (drawPose, buffer) -> {
                vertex(buffer, drawPose, -scale - 1F, -scale * 2F, u1, 1F, faded);
                vertex(buffer, drawPose, -scale - 1F, scale * 2F, u1, 0F, faded);
                vertex(buffer, drawPose, scale - 1F, scale * 2F, u0, 0F, faded);
                vertex(buffer, drawPose, scale - 1F, -scale * 2F, u0, 1F, faded);
            });
        }

        /** The sheet is opaque on black and 1.7 blends SRC_ALPHA, ONE, so the fade goes on RGB under energySwirl. */
        private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, float alpha) {
            Vertexes.vertex(buffer, pose, x, y, 0F, alpha, alpha, alpha, 1F, u, v,
                    OverlayTexture.NO_OVERLAY, 0x00F000F0, 0F, 1F, 0F);
        }
    }
}
