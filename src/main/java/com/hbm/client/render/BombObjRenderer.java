package com.hbm.client.render;

import com.hbm.blocks.bomb.NukeMan;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a HBM bomb's static OBJ model, replacing the 1.12.2 {@code StaticTesrBakedModels} entry. Like
 * {@link MachineObjRenderer} but keyed on the bomb block's {@link NukeMan#FACING} property (2–5) with
 * the Fat Man's own yaw map (the old {@code yawMap}: 2→270°, 3→90°, 4→0°, 5→180°).
 */
public class BombObjRenderer<T extends BlockEntity> extends GemBER<T, MachineRenderState> {

    private final WavefrontObj model;
    private final Surface surface;
    private final float scale;
    private final IntegerProperty facingProperty;
    private final float preTranslateX;
    private final float preTranslateY;
    private final float preTranslateZ;
    private final float[] yawsByFacing;
    private final boolean doubleSided;

    public BombObjRenderer(Identifier obj, Identifier texture, float scale) {
        this(obj, texture, scale, false, NukeMan.FACING, 0F, 0F, 0F, 270F, 90F, 0F, 180F);
    }

    /**
     * @param yawsByFacing values for legacy facing 2, 3, 4, and 5 respectively
     */
    public BombObjRenderer(Identifier obj, Identifier texture, float scale, IntegerProperty facingProperty,
                           float preTranslateX, float preTranslateY, float preTranslateZ, float... yawsByFacing) {
        this(obj, texture, scale, true, facingProperty, preTranslateX, preTranslateY, preTranslateZ, yawsByFacing);
    }

    private BombObjRenderer(Identifier obj, Identifier texture, float scale, boolean doubleSided,
                           IntegerProperty facingProperty, float preTranslateX, float preTranslateY,
                           float preTranslateZ, float... yawsByFacing) {
        this.model = WavefrontObj.load(obj);
        // Reversed faces need culling: otherwise both coplanar normals compete in the depth buffer.
        // Fat Man is the one legacy spec that did not request reversed faces at all.
        this.surface = Surface.cutoutCull(texture.toString());
        this.doubleSided = doubleSided;
        this.scale = scale;
        this.facingProperty = facingProperty;
        this.preTranslateX = preTranslateX;
        this.preTranslateY = preTranslateY;
        this.preTranslateZ = preTranslateZ;
        if (yawsByFacing.length != 4) throw new IllegalArgumentException("A bomb needs four horizontal yaws");
        this.yawsByFacing = yawsByFacing.clone();
    }

    @Override
    protected void setup(Gem.Model<MachineRenderState> declaration) {
        Gem.Part<MachineRenderState> bomb = declaration.offScreen().viewDistance(256)
                .part(model.all(doubleSided), surface)
                .translate(0.5F, 0.0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        if (preTranslateX != 0F || preTranslateY != 0F || preTranslateZ != 0F) {
            bomb.translate(preTranslateX, preTranslateY, preTranslateZ);
        }
        if (scale != 1.0F) bomb.scale(scale);
    }

    @Override
    public MachineRenderState createRenderState() {
        return new MachineRenderState();
    }

    @Override
    public void extract(T blockEntity, MachineRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        int facing = bs.hasProperty(facingProperty) ? bs.getValue(facingProperty) : 2;
        state.yaw = facing >= 2 && facing <= 5 ? yawsByFacing[facing - 2] : yawsByFacing[0];
    }

}
