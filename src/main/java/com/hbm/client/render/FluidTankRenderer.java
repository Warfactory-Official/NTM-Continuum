package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws the fluid-tank multiblock from {@code fluidtank.obj}. Unlike the generic
 * {@link MachineObjRenderer}, the cylinder body ({@code Tank} part) is textured <b>per fluid</b>
 * ({@code textures/models/tank/tank_<name>.png}, falling back to {@code tank_none} for an empty tank),
 * matching the 1.12.2 {@code RenderFluidTank}. The {@code Frame} part keeps the fixed
 * {@code machines/tank.png}. Base yaw 180° (the tank's facing->yaw differs from the machines', see
 * {@link HbmBlockEntityRenderers}).
 */
public class FluidTankRenderer extends GemBER<TileEntityMachineFluidTank, FluidTankRenderState> {

    private static final Identifier OBJ = Ids.of("hbm", "models/fluidtank.obj");
    private static final Identifier FRAME_TEX = Ids.of("hbm", "textures/models/machines/tank.png");
    private static final Identifier TANK_NONE = Ids.of("hbm", "textures/models/tank/tank_none.png");
    private static final float BASE_YAW = 180F;

    private final WavefrontObj model = WavefrontObj.load(OBJ);
    private final WavefrontObj exploded = WavefrontObj.load(Ids.of("hbm", "models/fluidtank_exploded.obj"));
    private final Surface inner = Surface.cutout(Ids.of("hbm", "textures/models/tank/tank_inner.png").toString());
    private final Surface frame = Surface.cutout(FRAME_TEX.toString());
    private final Map<Identifier, Surface> skins = new ConcurrentHashMap<>();
    /** Cache of fluid registry name -> resolved body texture (falling back to tank_none when a fluid has no skin). */
    private static final Map<String, Identifier> BODY_TEX = new ConcurrentHashMap<>();

    /**
     * The body skin for a fluid, falling back to {@code tank_none} when that fluid ships no
     * {@code tank_<name>.png}. Many of the 156 fluids (plasmas, smokes, intermediates) have no dedicated
     * tank texture and would render the missing-texture sprite. Existence is checked once per fluid
     * against the client resource manager and cached.
     */
    private static Identifier bodyTextureFor(FluidType fluid) {
        // in the fluid's own namespace: an addon fluid ships <ns>:textures/models/tank/tank_<key>.png
        return BODY_TEX.computeIfAbsent(fluid.getNamespace() + ":" + fluid.getKey(), name -> {
            Identifier tex = Ids.of(fluid.getNamespace(), "textures/models/tank/tank_" + fluid.getKey() + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(tex).isPresent() ? tex : TANK_NONE;
        });
    }

    /** The model spans the 5x3x5 footprint well past the 1-block core, hence the off-screen pass. */
    @Override
    protected void setup(Gem.Model<FluidTankRenderState> declaration) {
        Gem.Part<FluidTankRenderState> tank = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw);

        tank.child(model.part("Frame"), frame).visibleIf(state -> !state.exploded);
        tank.child(model.part("Tank"), frame).skin(this::skinOf).visibleIf(state -> !state.exploded);
        // 1.7 RenderFluidTank: a tank that exploded is the torn model, its inner wall in tank_inner
        tank.child(exploded.part("Frame"), frame).visibleIf(state -> state.exploded);
        tank.child(exploded.part("TankInner"), inner).visibleIf(state -> state.exploded);
        tank.child(exploded.part("Tank"), frame).skin(this::skinOf).visibleIf(state -> state.exploded);
    }

    private Surface skinOf(FluidTankRenderState state) {
        return skins.computeIfAbsent(state.bodyTexture, id -> Surface.cutout(id.toString()));
    }

    @Override
    public FluidTankRenderState createRenderState() {
        return new FluidTankRenderState();
    }

    @Override
    public void extract(TileEntityMachineFluidTank be, FluidTankRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.bodyTexture = bodyTextureFor(be.tank.getTankType());
        state.exploded = be.hasExploded;
        FluidType type = be.tank.getTankType();
        state.hazard = type == null || type == Fluids.NONE ? null : type.getHazard();
    }

    /**
     * 1.7 {@code RenderFluidTank}: the fluid's NFPA diamond on both ends of the tank, after the tank's own rotation:
     * {@code translate(-0.25, 0.5, -1.501)}, {@code rotate(90, Y)} and {@code translate(0.25, 0.5, 1.501)},
     * {@code rotate(-90, Y)}, each {@code scale(1, 0.375, 0.375)}. Drawn for an exploded tank too, as in 1.7.
     */
    @Override
    protected void submitUndeclared(FluidTankRenderState state, PoseStack poseStack,
                                    SubmitSink sink, CameraInfo camera) {
        if (state.hazard == null) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(BASE_YAW + state.yaw));
        poseStack.pushPose();
        poseStack.translate(-0.25F, 0.5F, -1.501F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        poseStack.scale(1.0F, 0.375F, 0.375F);
        DiamondPronter.pront(poseStack, sink, state.hazard);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.25F, 0.5F, 1.501F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
        poseStack.scale(1.0F, 0.375F, 0.375F);
        DiamondPronter.pront(poseStack, sink, state.hazard);
        poseStack.popPose();
        poseStack.popPose();
    }

}
