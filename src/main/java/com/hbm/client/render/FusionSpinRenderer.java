package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiFunction;

/**
 * A fusion machine declared as one static body plus one part that spins about the machine's own axis —
 * the klystron's fan and the MHD turbine's coils, which upstream gives near-identical renderers.
 *
 * <p>Two details matter and are easy to get wrong. The spin is about <b>X after the facing rotation</b>,
 * around a pivot part-way up the model, so the part turns on the machine's axis rather than about the
 * world origin. And {@code offsetX} is applied <b>after</b> the facing rotation: the klystron's model is
 * authored one block off its core cell, and without that shift the whole machine draws a block away
 * from where it actually is — which also makes its ports look like they are in the wrong place.
 */
public class FusionSpinRenderer<T extends BlockEntity> extends GemBER<T, FusionSpinRenderState> {

    private final WavefrontObj model;
    private final Surface surface;
    private final String bodyPart;
    private final String spinPart;
    private final float pivotY;
    private final float offsetX;
    private final float baseYaw;
    /** (blockEntity, partialTicks) -> the interpolated angle of the spinning part. */
    private final BiFunction<T, Float, Float> angle;
    /**
     * Wraps the angle before drawing, or 0 for a freely spinning part. The MHD turbine's coils are a
     * partial arc that upstream deliberately confines to a 15-degree wobble; let them spin the whole
     * way round and they swing out of their housing and present their unpainted inner faces.
     */
    private final float angleModulus;

    public FusionSpinRenderer(String obj, String texture, String bodyPart, String spinPart,
                              float pivotY, float offsetX, float baseYaw, BiFunction<T, Float, Float> angle) {
        this(obj, texture, bodyPart, spinPart, pivotY, offsetX, baseYaw, 0F, angle);
    }

    public FusionSpinRenderer(String obj, String texture, String bodyPart, String spinPart,
                              float pivotY, float offsetX, float baseYaw, float angleModulus,
                              BiFunction<T, Float, Float> angle) {
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/" + obj));
        this.surface = Surface.cutout(Constants.MOD_ID + ":textures/models/" + texture);
        this.bodyPart = bodyPart;
        this.spinPart = spinPart;
        this.pivotY = pivotY;
        this.offsetX = offsetX;
        this.baseYaw = baseYaw;
        this.angle = angle;
        this.angleModulus = angleModulus;
    }

    @Override
    protected void setup(Gem.Model<FusionSpinRenderState> declaration) {
        Gem.Part<FusionSpinRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> baseYaw + state.yaw);
        if (offsetX != 0F) machine.translate(offsetX, 0F, 0F);

        machine.child(model.part(bodyPart), surface);
        machine.child(model.part(spinPart), surface)
                .spin(Gem.Axes.X, 0F, pivotY, 0F, state -> state.spin);
    }

    @Override
    public FusionSpinRenderState createRenderState() {
        return new FusionSpinRenderState();
    }

    @Override
    public void extract(T blockEntity, FusionSpinRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (ForgeDirection.getOrientation(meta - BlockDummyable.offset)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        float spin = angle.apply(blockEntity, partialTicks);
        state.spin = angleModulus > 0F ? spin % angleModulus : spin;
    }
}
