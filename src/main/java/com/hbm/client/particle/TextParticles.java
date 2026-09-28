package com.hbm.client.particle;

import com.hbm.Constants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.WorldEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ParticleText} (the {@code debug} aux effect): a line of text with a drop shadow that faces the player,
 * rises 0.01 blocks a tick for 100 ticks without colliding, fullbright. Its size is {@code particleScale * 0.01}
 * blocks per pixel, where {@code particleScale} is EntityFX's random {@code (0.5..1) * 2} times the sender's
 * {@code scale}. The power net tool labels every link of a net with the net's id this way.
 *
 * <p>Drawn in Core's late world-effects pass, depth-tested, like {@link SparkParticles}.
 */
public final class TextParticles {

    private static final List<Text> ACTIVE = new ArrayList<>();
    private static boolean initialized;
    private static ClientLevel lastLevel;

    private TextParticles() {}

    public static void register() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        ClientTicks.onEndTick(TextParticles::tick);
        WorldEffects.onBegin(camera -> {
            if (Minecraft.getInstance().level == null || ACTIVE.isEmpty()) return;
            float partial = Frames.partialTick();
            for (Text text : List.copyOf(ACTIVE)) text.submit(camera, partial);
        });
        ClientResourceReloads.register(Constants.MOD_ID, "text_particles", ACTIVE::clear);
    }

    public static void spawn(ClientLevel level, double x, double y, double z, int color, String text, float scale) {
        RandomSource rand = level.getRandom();
        float particleScale = (rand.nextFloat() * 0.5F + 0.5F) * 2.0F * scale;
        ACTIVE.add(new Text(x, y, z, color, text, particleScale));
    }

    /** Live texts, for the harness. */
    public static List<String> active() {
        List<String> out = new ArrayList<>();
        for (Text text : ACTIVE) out.add(text.text);
        return out;
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            ACTIVE.clear();
            lastLevel = level;
        }
        if (level == null) return;
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Text text = ACTIVE.get(i);
            text.prevY = text.y;
            // EntityFX.onUpdate: age, then move by the motion (noClip), then 0.98 friction on it
            if (text.age++ >= 100) {
                ACTIVE.remove(i);
                continue;
            }
            text.y += text.motionY;
            text.motionY *= 0.98D;
        }
    }

    private static final class Text {
        final double x, z;
        double y, prevY;
        double motionY = 0.01D;
        final int color;
        final String text;
        final float scale;
        int age;

        Text(double x, double y, double z, int color, String text, float scale) {
            this.x = x;
            this.y = this.prevY = y;
            this.z = z;
            this.color = color;
            this.text = text;
            this.scale = scale;
        }

        void submit(CameraInfo camera, float partial) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;
            Font font = Minecraft.getInstance().font;
            double py = prevY + (y - prevY) * partial;
            PoseStack pose = new PoseStack();
            pose.translate(x - camera.pos.x, py - camera.pos.y, z - camera.pos.z);
            // 1.7: rotationYaw = -player yaw, rotationPitch = player pitch, then scale(-1, -1, 1)
            pose.mulPose(Axis.YP.rotationDegrees(-player.getViewYRot(partial)));
            pose.mulPose(Axis.XP.rotationDegrees(player.getViewXRot(partial)));
            pose.scale(-1F, -1F, 1F);
            pose.scale(scale * 0.01F, scale * 0.01F, scale * 0.01F);
            WorldEffects.submitText(pose, 20, font, text, -(int) (font.width(text) * 0.5F), -(int) (font.lineHeight * 0.5F),
                    0xFF000000 | color, true, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
    }
}
