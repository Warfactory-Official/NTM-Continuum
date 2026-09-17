package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.tileentity.machine.TileEntityZirnoxDestroyed;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Poses;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Draws the old RBMK flame quads directly, preserving their additive and vertical billboard appearance. */
public class ZirnoxDestroyedRenderer extends MachineObjRenderer<TileEntityZirnoxDestroyed> {
    private static final Identifier FLAME_TEXTURE = Ids.of(Constants.MOD_ID, "textures/particle/rbmk_fire.png");
    /**
     * Vertical centre of the flame quads. The 1.12.2 renderer put them at 1.75; raised one block so the
     * fire sits on top of the wreck's upper shell instead of inside it.
     */
    private static final float FLAME_Y = 2.75F;

    public ZirnoxDestroyedRenderer() {
        super(90F, 1F, new ModelDef(
                Ids.of(Constants.MOD_ID, "models/zirnox_destroyed.obj"),
                Ids.of(Constants.MOD_ID, "textures/models/machines/zirnox_destroyed.png")));
        doubleSided();
    }

    @Override
    public void extract(TileEntityZirnoxDestroyed wreck, MachineRenderState state, float partialTicks,
                                   net.minecraft.world.phys.Vec3 cameraPosition) {
        state.burningWreck = wreck.isOnFire();
        state.wreckGameTime = wreck.getLevel() == null ? 0L : wreck.getLevel().getGameTime();
    }

    @Override
    protected void submitUndeclared(MachineRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        if (!state.burningWreck) return;

        SubmitSink flameCollector = collector.order(1);
        long latestEmission = state.wreckGameTime / 50L;
        flameCollector.submitCustomGeometry(poseStack, CompatRenderTypes.energySwirl(FLAME_TEXTURE, 0F, 0F), (pose, buffer) -> {
            Poses.rotate(pose, Axis.YP.rotationDegrees(-camera.yRot));
            for (int i = 0; i < 2; i++) {
                long emission = latestEmission - i;
                int age = (int) (state.wreckGameTime - emission * 50L);
                if (age < 0 || age >= 90) continue;

                long seed = emission * 31L + state.blockPos.asLong();
                float x = 0.25F + unit(seed) * 0.5F;
                float z = 0.25F + unit(seed * 17L) * 0.5F;
                float scale = 1.0F + unit(seed * 37L);
                float alpha = Math.min(Mth.clamp(age / 20.0F, 0.0F, 1.0F),
                        Mth.clamp((90 - age) / 20.0F, 0.0F, 1.0F)) * 0.5F;
                int frame = age * 5 % 14;
                float u0 = (frame % 5) / 14.0F;
                float u1 = u0 + 1.0F / 14.0F;

                // This is the original -1 horizontal offset and 2:1 vertical stretch, raised by FLAME_Y.
                vertex(buffer, pose, x - scale - 1F, FLAME_Y - scale * 2F, z, u1, 1F, alpha);
                vertex(buffer, pose, x - scale - 1F, FLAME_Y + scale * 2F, z, u1, 0F, alpha);
                vertex(buffer, pose, x + scale - 1F, FLAME_Y + scale * 2F, z, u0, 0F, alpha);
                vertex(buffer, pose, x + scale - 1F, FLAME_Y - scale * 2F, z, u0, 1F, alpha);
            }
        });
    }

    private static float unit(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return (float) (value & 0xFFFFFFL) / 0x1000000L;
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v, float alpha) {
        // energySwirl is additive; its blend ignores alpha, so fade is carried by RGB.
        Vertexes.vertex(buffer, pose, x, y, z, alpha, alpha, alpha, 1F, u, v,
                OverlayTexture.NO_OVERLAY, 0x00F000F0, 0F, 1F, 0F);
    }
}
