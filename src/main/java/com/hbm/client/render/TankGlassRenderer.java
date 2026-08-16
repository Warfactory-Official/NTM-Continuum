package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Generic renderer for the liquefactor / solidifier — port of the 1.12.2 {@code RenderLiquefactor} /
 * {@code RenderSolidifier}: a static {@code Main} body, the {@code Fluid} column Y-scaled to the tank
 * fill fraction and vertex-tinted by the fluid colour (fullbright, unlit — the old {@code disableLighting}
 * pass), and a translucent {@code Glass} shell (colour 0.75,1,1 @ ~15% alpha, depth-write off →
 * {@code entityTranslucent}). Facing-independent (the old TESRs applied no rotation).
 */
public class TankGlassRenderer<T extends BlockEntity> implements CompatBER<T, TankGlassRenderState> {

    private static final int FULLBRIGHT = 0xF000F0;

    private final WavefrontObj model;
    private final RenderType baseType;
    private final RenderType glassType;
    private final Function<T, FluidTankNTM> tankGetter;

    public TankGlassRenderer(String objName, String texName, Function<T, FluidTankNTM> tankGetter) {
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/" + objName + ".obj"));
        Identifier tex = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/" + texName + ".png");
        this.baseType = CompatRenderTypes.entityCutout(tex);
        this.glassType = CompatRenderTypes.entityTranslucent(tex, false);
        this.tankGetter = tankGetter;
    }

    @Override public TankGlassRenderState createRenderState() { return new TankGlassRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(T be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(T be, TankGlassRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        // The old renderer applies no facing rotation — but keep a yaw slot for parity if needed.
        state.yaw = 0F;
        FluidTankNTM tank = tankGetter.apply(be);
        state.fillFraction = tank.getMaxFill() <= 0 ? 0F : (float) tank.getFill() / (float) tank.getMaxFill();
        state.fluidColor = tank.getTankType().getColor();
    }

    @Override
    public void submit(TankGlassRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        col.submitCustomGeometry(ps, baseType,
                (pose, buffer) -> model.renderPart("Main", pose, buffer, light, OverlayTexture.NO_OVERLAY));

        // Fluid column, Y-scaled to fill fraction, tinted + fullbright (old disableLighting pass).
        if (s.fillFraction > 0F) {
            int color = 0xFF000000 | (s.fluidColor & 0xFFFFFF);
            ps.pushPose();
            ps.translate(0F, 1F, 0F);
            ps.scale(1F, s.fillFraction, 1F);
            ps.translate(0F, -1F, 0F);
            col.submitCustomGeometry(ps, baseType,
                    (pose, buffer) -> model.renderPart("Fluid", pose, buffer, FULLBRIGHT, OverlayTexture.NO_OVERLAY, color));
            ps.popPose();
        }

        // Translucent glass shell (old 0.75,1,1 @ 15% alpha).
        int glass = 0x26BFFFFF; // alpha ~0.15, rgb 0.75,1,1
        col.submitCustomGeometry(ps, glassType,
                (pose, buffer) -> model.renderPart("Glass", pose, buffer, light, OverlayTexture.NO_OVERLAY, glass));

        ps.popPose();
    }
}
