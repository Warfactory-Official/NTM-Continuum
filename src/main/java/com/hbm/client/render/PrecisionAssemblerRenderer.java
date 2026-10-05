package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Animated OBJ renderer for PrecAss, porting RenderPrecAss's ring and four striker arms. */
public final class PrecisionAssemblerRenderer extends GemBER<TileEntityMachinePrecAss, PrecisionAssemblerRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/assembly_machine.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/precass.png");

    /**
     * Four copies of the assembly machine's arm on one ring, a quarter turn apart. Upstream turns the
     * ring pose by another -90 after each arm; declared, that is each arm under its own fixed quarter.
     */
    @Override
    protected void setup(Gem.Model<PrecisionAssemblerRenderState> declaration) {
        Gem.Part<PrecisionAssemblerRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> 90F + state.yaw);

        machine.child(model.part("Base"), surface);
        machine.child(model.part("Frame"), surface)
                .visibleIf(state -> state.showFrame);

        Gem.Part<PrecisionAssemblerRenderState> ring = machine.joint()
                .angle(Gem.Axes.Y, state -> (float) state.ring);
        ring.child(model.part("Ring"), surface);
        ring.child(model.part("Ring2"), surface);
        for (int i = 0; i < 4; i++) {
            final int striker = i;
            arm(ring.joint().rotate(Gem.Axes.Y, -90F * i), state -> (float) state.strikers[striker]);
        }
    }

    private void arm(Gem.Part<PrecisionAssemblerRenderState> mount,
                     Gem.FloatOf<PrecisionAssemblerRenderState> striker) {
        Gem.Part<PrecisionAssemblerRenderState> lower = mount.joint()
                .translate(0F, 1.625F, 0.9375F)
                .angle(Gem.Axes.X, state -> (float) state.arm[0])
                .translate(0F, -1.625F, -0.9375F);
        lower.child(model.part("ArmLower1"), surface);

        Gem.Part<PrecisionAssemblerRenderState> upper = lower.joint()
                .translate(0F, 2.375F, 0.9375F)
                .angle(Gem.Axes.X, state -> (float) state.arm[1])
                .translate(0F, -2.375F, -0.9375F);
        upper.child(model.part("ArmUpper1"), surface);

        Gem.Part<PrecisionAssemblerRenderState> head = upper.joint()
                .translate(0F, 2.375F, 0.4375F)
                .angle(Gem.Axes.X, state -> (float) state.arm[2])
                .translate(0F, -2.375F, -0.4375F);
        head.child(model.part("Head1"), surface);
        head.child(model.part("Spike1"), surface)
                .slide(Gem.Axes.Y, striker);
    }

    @Override public PrecisionAssemblerRenderState createRenderState() { return new PrecisionAssemblerRenderState(); }

    @Override
    public void extract(TileEntityMachinePrecAss be, PrecisionAssemblerRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.showFrame = be.frame;
        state.ring = be.prevRing + (be.ring - be.prevRing) * partialTicks;
        for (int i = 0; i < 3; i++) state.arm[i] = be.prevArmAngles[i]
                + (be.armAngles[i] - be.prevArmAngles[i]) * partialTicks;
        for (int i = 0; i < 4; i++) state.strikers[i] = be.prevStrikers[i]
                + (be.strikers[i] - be.prevStrikers[i]) * partialTicks;

        state.showItem = false;
        GenericRecipe recipe = PrecAssRecipes.INSTANCE.getRecipe(be.assemblerModule.recipe);
        if (recipe != null && be.getLevel() != null) {
            // Measured where the machine is seen: on a moving sub-level (Create Aeronautics) its block sits far from it.
            double distanceSq = dev.golem.core.world.SubLevels.distanceSq(be.getLevel(),
                    be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 1.0, be.getBlockPos().getZ() + 0.5,
                    cameraPosition.x, cameraPosition.y, cameraPosition.z);
            if (distanceSq < 35 * 35) {
                ItemStack icon = recipe.getIcon();
                if (!icon.isEmpty()) {
                    ItemStack one = icon.copy();
                    one.setCount(1);
                    state.outputItem.extract(be.getLevel(), one, ItemDisplayContext.FIXED);
                    state.itemIsBlock = one.getItem() instanceof BlockItem;
                    state.itemIs3d = one.getItem() instanceof BlockItem;
                    state.showItem = !state.outputItem.isEmpty();
                }
            }
        }
        if (!state.showItem) state.outputItem.clear();
    }

    /** The floating product is a recipe-chosen item model, which is not a mesh either backend holds. */
    @Override
    protected void submitUndeclared(PrecisionAssemblerRenderState state, PoseStack pose, SubmitSink collector, CameraInfo camera) {
        if (!state.showItem) return;
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(90F + state.yaw));
        pose.translate(0F, 1.0625F, 0F);
        if (state.itemIsBlock) {
            if (state.itemIs3d) pose.translate(0F, -0.0625F, 0F);
            else {
                pose.translate(0F, -0.125F, 0F);
                pose.scale(0.5F, 0.5F, 0.5F);
            }
        } else {
            pose.mulPose(Axis.XP.rotationDegrees(-90F));
            pose.translate(0F, -0.25F, 0F);
        }
        pose.scale(1.25F, 1.25F, 1.25F);
        collector.submitItem(pose, state.outputItem, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
