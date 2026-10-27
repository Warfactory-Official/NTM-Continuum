package com.hbm.client.render;

import com.hbm.api.block.BarrelSkin;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.generic.BlockBarrelDeco;
import com.hbm.blocks.machine.BlockFluidBarrel;
import com.hbm.inventory.fluid.FluidHazard;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * The 1.7 {@code RenderBarrel} + {@code RenderFluidBarrel}: every barrel is the {@code barrel.obj}
 * drum in its block's texture; a storage barrel with a fluid also grows a {@code Connector} on each
 * horizontal side where a duct of that fluid can attach, and the fluid's NFPA diamond ({@link DiamondPronter}) on
 * four sides.
 */
public class BarrelRenderer<T extends BlockEntity> extends GemBER<T, BarrelRenderer.State> {

    public static class State extends CompatRenderState {
        public int texture;
        /** The skin drawn: one of HBM's, or another mod's ({@link com.hbm.api.block.BarrelSkin}). */
        public Surface skin = SURF[0];
        public boolean[] connector = new boolean[4]; // +X, -X, -Z, +Z
        /** The stored fluid's diamond; null for an empty or decorative barrel (1.7: {@code type != Fluids.NONE}). */
        public FluidHazard hazard;
    }

    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of("hbm", "models/blocks/barrel.obj"));
    private static final String[] TEXTURES = {"barrel_plastic", "barrel_corroded", "barrel_steel", "barrel_tcalloy", "barrel_antimatter",
            "barrel_red", "barrel_pink", "barrel_yellow", "barrel_vitrified", "barrel_lox", "barrel_taint", "barrel_iron"};
    private static final Surface[] SURF = new Surface[TEXTURES.length];
    private static final Map<String, Integer> INDEX = new HashMap<>();
    /** Other mods' skins by full texture path ({@link com.hbm.api.block.BarrelSkin}), made at first sight. */
    private static final Map<String, Surface> FOREIGN = new ConcurrentHashMap<>();
    /** Yaw per connector slot, the 1.7 rotations for +X, -X, -Z, +Z. */
    private static final float[] YAW = {0F, 180F, 90F, -90F};

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            SURF[i] = Surface.cutout("hbm:textures/blocks/" + TEXTURES[i] + ".png");
            INDEX.put(TEXTURES[i], i);
        }
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> root = declaration.joint().translate(0.5F, 0F, 0.5F);
        root.child(MODEL.part("Barrel"), SURF[0]).skin(state -> state.skin);
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            root.joint()
                    .visibleIf(state -> state.connector[slot])
                    .translate(0F, 0.5F, 0F)
                    .rotate(Gem.Axes.Y, YAW[i])
                    .translate(0F, -0.5F, 0F)
                    .child(MODEL.part("Connector"), SURF[0]).skin(state -> state.skin);
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T be, State state, float partialTicks, Vec3 cameraPosition) {
        Block block = be.getBlockState().getBlock();
        String texture = block instanceof BlockFluidBarrel barrel ? barrel.texture
                : block instanceof BlockBarrelDeco deco ? deco.barrelTexture() : "barrel_iron";
        state.texture = INDEX.getOrDefault(texture, TEXTURES.length - 1);
        state.skin = block instanceof BarrelSkin foreign
                ? FOREIGN.computeIfAbsent(foreign.barrelSkin(), Surface::cutout) : SURF[state.texture];
        Arrays.fill(state.connector, false);
        state.hazard = null;
        if (be instanceof TileEntityBarrel barrel && be.getLevel() != null) {
            FluidType type = barrel.tank.getTankType();
            if (type != Fluids.NONE) {
                state.hazard = type.clientMetadata().getHazard();
                Level level = be.getLevel();
                BlockPos pos = be.getBlockPos();
                state.connector[0] = Library.canConnectFluid(level, pos.east(), ForgeDirection.EAST, type);
                state.connector[1] = Library.canConnectFluid(level, pos.west(), ForgeDirection.WEST, type);
                state.connector[2] = Library.canConnectFluid(level, pos.north(), ForgeDirection.NORTH, type);
                state.connector[3] = Library.canConnectFluid(level, pos.south(), ForgeDirection.SOUTH, type);
            }
        }
    }

    /**
     * 1.7 {@code RenderFluidBarrel}: from the block centre, four times {@code translate(0.4, 0.30, -0.24)},
     * {@code scale(1, 0.25, 0.25)}, the diamond, then a quarter turn about Y.
     */
    @Override
    protected void submitUndeclared(State state, PoseStack poseStack,
                                    SubmitSink sink, CameraInfo camera) {
        if (state.hazard == null) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        for (int j = 0; j < 4; j++) {
            poseStack.pushPose();
            poseStack.translate(0.4F, 0.30F, -0.24F);
            poseStack.scale(1.0F, 0.25F, 0.25F);
            DiamondPronter.pront(poseStack, sink, state.hazard);
            poseStack.popPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        }
        poseStack.popPose();
    }
}
