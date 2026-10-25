package com.hbm.client.render.entity;

import com.hbm.client.render.BalefireGlint;
import com.hbm.client.render.BeamPronter;
import com.hbm.client.render.BeamPronter.EnumBeamType;
import com.hbm.client.render.BeamPronter.EnumWaveType;
import com.hbm.entity.mob.siege.EntitySiegeCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.Random;

/**
 * The siege saucer in its tier's skin, coils lit green to red by health, coil arcs and, while beaming, a glint over
 * the hull and three beams to the lock-on. The glint sheet is {@code glintBF.png} pre-multiplied by 1.7's
 * {@code (0.5, 1, 1) * 0.76}.
 */
public class RenderSiegeCraft extends CompatEntityRenderer<EntitySiegeCraft, RenderSiegeCraft.State> {

    private static final Identifier WHITE = Ids.of("hbm", "textures/models/misc/white.png");
    private static final Identifier GLINT = Ids.of("hbm", "textures/misc/glint_siege_craft.png");

    public RenderSiegeCraft(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public boolean culled(EntitySiegeCraft entity) {
        return false; // 1.7 ignoreFrustumCheck
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntitySiegeCraft entity, State state, float partialTicks) {
        state.spin = (entity.tickCount + partialTicks) * 5 % 360D;
        state.alive = entity.isAlive();
        state.tilt = entity.deathTime + partialTicks;
        state.health = entity.getHealth() / entity.getMaxHealth();
        state.beam = entity.getBeam();
        state.lockon = entity.getLockon().subtract(entity.getX(), entity.getY(), entity.getZ());
        state.age = entity.tickCount;
        state.skin = Ids.of("hbm", "textures/entity/siege_craft_" + entity.getTier().name + ".png");
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;

        ps.pushPose();
        ps.translate(0D, 0.5D, 0D);
        ps.pushPose();

        ps.mulPose(Axis.YP.rotationDegrees((float) state.spin));

        if (!state.alive) {
            // glRotatef(tilt * 5, 1, 0, 1) normalises the axis
            ps.mulPose(new Quaternionf().rotateAxis(state.tilt * 5 * Mth.DEG_TO_RAD, 0.70710677F, 0F, 0.70710677F));
        }

        sink.submitCustomGeometry(ps, CompatRenderTypes.entityCutoutNoCull(state.skin),
                (pose, buffer) -> NtmEntityModels.SIEGE_UFO.renderPart("UFO", pose, buffer, light, OverlayTexture.NO_OVERLAY));

        int coil = 0xFF000000 | ((int) (255 * Mth.clamp(1F - state.health, 0F, 1F)) << 16)
                | ((int) (255 * Mth.clamp(state.health, 0F, 1F)) << 8);
        sink.submitCustomGeometry(ps, CompatRenderTypes.flatCutout(WHITE),
                (pose, buffer) -> NtmEntityModels.SIEGE_UFO.renderPart("Coils", pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, coil));

        if (state.beam) {
            BalefireGlint.submit(ps, sink.order(1), NtmEntityModels.SIEGE_UFO.part("UFO"), GLINT, 2, 1F, 5F);
        }

        Random rand = new Random(state.age / 4);
        int start = (int) (System.currentTimeMillis() % 1000) / 50;

        ps.pushPose();
        for (int i = 0; i < 8; i++) {
            ps.mulPose(Axis.YP.rotationDegrees(45F));
            if (rand.nextInt(5) == 0 || state.beam) {
                ps.pushPose();
                ps.translate(4D, 0D, 0D);
                BeamPronter.prontBeam(ps, sink, new Vec3(-1.125, 0, 2.875), EnumWaveType.RANDOM, EnumBeamType.LINE,
                        start, 15, 0.125F, 0x80d0ff, 0xffffff, 1, 0F);
                ps.popPose();
            }
        }
        ps.popPose();
        ps.popPose();

        if (state.beam) {
            Vec3 delta = state.lockon;
            double length = delta.length();
            double scale = 0.1D;
            BeamPronter.prontBeam(ps, sink, delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, state.age / 6,
                    (int) (length / 2 + 1), (float) scale * 1F, 0x101020, 0x101020, 4, 0.25F);
            BeamPronter.prontBeam(ps, sink, delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, state.age / 2,
                    (int) (length / 2 + 1), (float) scale * 7F, 0x202060, 0x202060, 2, 0.0625F);
            BeamPronter.prontBeam(ps, sink, delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, state.age / 4,
                    (int) (length / 2 + 1), (float) scale * 7F, 0x202060, 0x202060, 2, 0.0625F);
        }

        ps.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        double spin;
        boolean alive;
        float tilt;
        float health;
        boolean beam;
        Vec3 lockon = Vec3.ZERO;
        int age;
        Identifier skin;
    }
}
