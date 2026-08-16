package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.machine.MachineDiesel;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Diesel generator renderer — port of the 1.12.2 {@code RenderDieselGen}: the static {@code Generator}
 * part plus the {@code Engine} part, which jitters slightly while running (the old sine swing). Yaw
 * from the block's {@code FACING} (old meta map: S0/E90/N180/W270 → base yaws below).
 */
public class DieselGenRenderer implements CompatBER<TileEntityMachineDiesel, DieselGenRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public DieselGenRenderer() {
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/dieselgen.obj"));
        this.renderType = CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/dieselgen.png"));
    }

    @Override public DieselGenRenderState createRenderState() { return new DieselGenRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineDiesel be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineDiesel be, DieselGenRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        Direction facing = bs.hasProperty(MachineDiesel.FACING) ? bs.getValue(MachineDiesel.FACING) : Direction.NORTH;
        // Old meta yaw map (meta = EnumFacing ordinal): S(3)=0, E(5)=90, N(2)=180, W(4)=270.
        state.yaw = switch (facing) {
            case EAST -> 90F;
            case NORTH -> 180F;
            case WEST -> 270F;
            default -> 0F; // SOUTH
        };
        state.running = be.wasOn;
    }

    @Override
    public void submit(DieselGenRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderPart("Generator", pose, buffer, light, OverlayTexture.NO_OVERLAY));

        if (s.running) {
            double swingSide = Math.sin(System.currentTimeMillis() / 50D) * 0.005;
            double swingFront = Math.sin(System.currentTimeMillis() / 25D) * 0.005;
            ps.translate((float) swingFront, 0F, (float) swingSide);
        }
        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderPart("Engine", pose, buffer, light, OverlayTexture.NO_OVERLAY));

        ps.popPose();
    }
}
