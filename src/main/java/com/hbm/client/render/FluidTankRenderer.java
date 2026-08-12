package com.hbm.client.render;

import dev.golem.core.render.CompatBER;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CameraInfo;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Draws the fluid-tank multiblock from {@code fluidtank.obj}. Unlike the generic
 * {@link MachineObjRenderer}, the cylinder body ({@code Tank} part) is textured <b>per fluid</b>
 * ({@code textures/models/tank/tank_<name>.png}, falling back to {@code tank_none} for an empty tank),
 * matching the 1.12.2 {@code RenderFluidTank}. The {@code Frame} part keeps the fixed
 * {@code machines/tank.png}. Base yaw 180° (the tank's facing→yaw differs from the machines' — see
 * {@link HbmBlockEntityRenderers}).
 */
public class FluidTankRenderer implements CompatBER<TileEntityMachineFluidTank, FluidTankRenderState> {

    private static final Identifier OBJ = Identifier.fromNamespaceAndPath("hbm", "models/fluidtank.obj");
    private static final Identifier FRAME_TEX = Identifier.fromNamespaceAndPath("hbm", "textures/models/machines/tank.png");
    private static final Identifier TANK_NONE = Identifier.fromNamespaceAndPath("hbm", "textures/models/tank/tank_none.png");
    private static final float BASE_YAW = 180F;

    private final WavefrontObj model = WavefrontObj.load(OBJ);
    private final RenderType frameType = RenderTypes.entityCutout(FRAME_TEX);
    private final Map<Identifier, RenderType> bodyTypes = new HashMap<>();
    /** Cache of fluid name → resolved body texture (falling back to tank_none when a fluid has no skin). */
    private static final Map<String, Identifier> BODY_TEX = new HashMap<>();

    /**
     * The body skin for a fluid, falling back to {@code tank_none} when that fluid ships no
     * {@code tank_<name>.png} (many of the ~157 ported fluids — plasmas, smokes, intermediates — have no
     * dedicated tank texture; without this they'd render the missing-texture sprite). Existence is
     * checked once per fluid against the client resource manager and cached.
     */
    private static Identifier bodyTextureFor(String fluidName) {
        return BODY_TEX.computeIfAbsent(fluidName.toLowerCase(Locale.US), name -> {
            Identifier tex = Identifier.fromNamespaceAndPath("hbm", "textures/models/tank/tank_" + name + ".png");
            return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(tex).isPresent() ? tex : TANK_NONE;
        });
    }

    @Override
    public FluidTankRenderState createRenderState() {
        return new FluidTankRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true; // the model spans the 5×3×5 footprint well past the 1-block core
    }

    @Override
    public boolean shouldRender(TileEntityMachineFluidTank be, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extract(TileEntityMachineFluidTank be, FluidTankRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH (2)
        };
        state.bodyTexture = bodyTextureFor(be.tank.getTankType().getName());
    }

    @Override
    public void submit(FluidTankRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        RenderType bodyType = bodyTypes.computeIfAbsent(state.bodyTexture, RenderTypes::entityCutout);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(BASE_YAW + state.yaw));
        collector.submitCustomGeometry(poseStack, frameType,
                (pose, buffer) -> model.renderPart("Frame", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        collector.submitCustomGeometry(poseStack, bodyType,
                (pose, buffer) -> model.renderPart("Tank", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }
}
