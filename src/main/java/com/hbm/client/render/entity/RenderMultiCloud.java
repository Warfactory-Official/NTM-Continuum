package com.hbm.client.render.entity;

import com.hbm.lib.Library;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import java.util.Random;
import java.util.function.ToIntFunction;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 1.7 {@code MultiCloudRenderer}: five camera-facing billboards per gas puff, drawn from an eight-frame sprite strip
 * picked by the entity's age. The four gas clouds share no supertype in the port, so age and lifetime arrive as
 * accessors rather than through a common interface.
 */
public final class RenderMultiCloud<T extends Entity> extends CompatEntityRenderer<T, RenderMultiCloud.State> {

    private final RenderType[] stages;
    private final ToIntFunction<T> age;
    private final ToIntFunction<T> maxAge;

    public RenderMultiCloud(EntityRendererProvider.Context context, String sprite, ToIntFunction<T> age,
                            ToIntFunction<T> maxAge) {
        super(context);
        this.stages = new RenderType[8];
        for (int i = 0; i < 8; i++) {
            Identifier texture = Library.id("textures/items/" + sprite + (i + 1) + ".png");
            this.stages[i] = CompatRenderTypes.entityCutout(texture);
        }
        this.age = age;
        this.maxAge = maxAge;
        this.shadowRadius = 0F;
    }

    private static int stage(State state) {
        int eighth = state.maxAge / 8;
        if (eighth <= 0) return 0;
        return Mth.clamp(state.age / eighth, 0, 7);
    }

    private static void drawCloud(VertexConsumer buffer, PoseStack.Pose pose, State state, Vector3f right,
                                  Vector3f up) {
        Random shade = state.shade;
        Random spread = state.spread;
        shade.setSeed(state.hash);
        spread.setSeed(100);
        for (int i = 0; i < 5; i++) {
            float brightness = 1F - shade.nextInt(10) * 0.05F;
            float x = (float) ((spread.nextGaussian() - 1D) * 0.15D);
            float y = (float) ((spread.nextGaussian() - 1D) * 0.15D);
            float z = (float) ((spread.nextGaussian() - 1D) * 0.15D);
            float scale = (float) ((spread.nextDouble() * 0.5D + 0.25D) * 3.75D);
            // 1.7 rotates before offsetting, so the bias rides the billboard's up, not world Y
            float bias = scale * 0.25F;
            int b = (int) (brightness * 255F);
            int color = 0xFF000000 | (b << 16) | (b << 8) | b;
            quad(buffer, pose, x * 3.75F + up.x * bias, y * 3.75F + up.y * bias, z * 3.75F + up.z * bias,
                    scale * 0.5F, right, up, color, state.light);
        }
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float half,
                             Vector3f right, Vector3f up, int color, int light) {
        float rx = right.x * half, ry = right.y * half, rz = right.z * half;
        float ux = up.x * half, uy = up.y * half, uz = up.z * half;
        vertex(buffer, pose, x - rx - ux, y - ry - uy, z - rz - uz, 0F, 1F, color, light);
        vertex(buffer, pose, x + rx - ux, y + ry - uy, z + rz - uz, 1F, 1F, color, light);
        vertex(buffer, pose, x + rx + ux, y + ry + uy, z + rz + uz, 1F, 0F, color, light);
        vertex(buffer, pose, x - rx + ux, y - ry + uy, z - rz + uz, 0F, 0F, color, light);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int color, int light) {
        Vertexes.vertex(buffer, pose, x, y, z, color, u, v, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
    }

    @Override
    public boolean culled(T entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T entity, State state, float partialTicks) {
        state.age = age.applyAsInt(entity);
        state.maxAge = maxAge.applyAsInt(entity);
        state.hash = entity.hashCode();
        state.light = state.lightCoords;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.maxAge <= 0) return;
        Quaternionf rotation = camera.orientation;
        rotation.transform(1F, 0F, 0F, state.right);
        rotation.transform(0F, 1F, 0F, state.up);
        sink.submitCustomGeometry(pose, stages[stage(state)], state);
    }

    /**
     * The state is the draw callback: it already lives exactly as long as the deferred draw needs, so it
     * carries the billboard axes and the two seeded generators instead of allocating them per frame.
     */
    public static final class State extends CompatEntityRenderState implements SubmitSink.CustomGeometry {
        final Vector3f right = new Vector3f();
        final Vector3f up = new Vector3f();
        final Random shade = new Random();
        final Random spread = new Random();
        int age;
        int maxAge;
        int hash;
        int light;

        @Override
        public void render(PoseStack.Pose pose, VertexConsumer buffer) {
            drawCloud(buffer, pose, this, right, up);
        }
    }
}
