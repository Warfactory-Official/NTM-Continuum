package com.hbm.client.render;

import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.ToIntFunction;

/**
 * A press: a static body and a stamp head that descends with the machine's progress.
 *
 * <p>Whether the head turns with the body is per-machine: the e-press repeats the body's facing switch
 * on its head, the press does not.
 */
public class PressRenderer<T extends BlockEntity> extends GemBER<T, PressRenderState> {

    private final WavefrontObj body;
    private final WavefrontObj head;
    private final Surface bodySurface;
    private final Surface headSurface;
    private final float baseYaw;
    private final boolean applyFacing;
    private final float headBaseOffset;
    private final float headScaleXZ;
    private final boolean headTurnsWithBody;
    private final int maxProgress;
    private final ToIntFunction<T> progress;

    public PressRenderer(Identifier bodyObj, Identifier bodyTex, Identifier headObj, Identifier headTex,
                         float baseYaw, boolean applyFacing, float headBaseOffset, float headScaleXZ,
                         boolean headTurnsWithBody, int maxProgress, ToIntFunction<T> progress) {
        this.body = WavefrontObj.load(bodyObj);
        this.head = WavefrontObj.load(headObj);
        this.bodySurface = Surface.cutout(bodyTex.toString());
        this.headSurface = Surface.cutout(headTex.toString());
        this.baseYaw = baseYaw;
        this.applyFacing = applyFacing;
        this.headBaseOffset = headBaseOffset;
        this.headScaleXZ = headScaleXZ;
        this.headTurnsWithBody = headTurnsWithBody;
        this.maxProgress = maxProgress;
        this.progress = progress;
    }

    @Override
    protected void setup(Gem.Model<PressRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(body.all(), bodySurface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> baseYaw + state.yaw);

        Gem.Part<PressRenderState> stamp = declaration.part(head.all(), headSurface)
                .translate(0.5F, headBaseOffset, 0.5F)
                .slide(Gem.Axes.Y, state -> -state.progress * 0.875F / maxProgress);
        if (headTurnsWithBody) stamp.angle(Gem.Axes.Y, state -> baseYaw + state.yaw);
        if (headScaleXZ != 1.0F) stamp.scale(headScaleXZ, 1.0F, headScaleXZ);
    }

    @Override
    public PressRenderState createRenderState() {
        return new PressRenderState();
    }

    @Override
    public void extract(T be, PressRenderState state, float partialTicks, Vec3 cameraPosition) {
        if (applyFacing) {
            BlockState bs = be.getBlockState();
            int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
            state.yaw = switch (meta - BlockDummyable.offset) {
                case 4 -> 90F;
                case 3 -> 180F;
                case 5 -> 270F;
                default -> 0F;
            };
        } else {
            state.yaw = 0F;
        }
        state.progress = progress.applyAsInt(be);
    }
}
