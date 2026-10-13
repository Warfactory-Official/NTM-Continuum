package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.SubLevels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Animated renderer for the assembly machine, the worked example of porting a 1.12.2 OBJ TESR to the
 * 26.1 render-state pipeline. It replays the original renderer's transforms: a base + (conditional)
 * frame, a spinning ring, and under that ring two three-joint robotic arms (lower/upper arm, head,
 * striker). Animation state lives on the {@link TileEntityMachineAssemblyMachine} (advanced by its
 * client ticker); here it is read and interpolated by partial-ticks.
 *
 * <p>Each part is a separate {@code submitCustomGeometry} call, which snapshots the current pose, so
 * the accumulated joint transforms apply per part.
 */
public class AssemblyMachineRenderer extends GemBER<TileEntityMachineAssemblyMachine, AssemblyRenderState> {

    private static final float BASE_YAW = 90F;
    private static final int FRAME_HEIGHT = 3;

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/assembly_machine.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/assembly_machine.png");

    /**
     * The ring carries both arms, and each arm is a chain of three hinges with a striker sliding on the
     * end. Upstream walks one pose stack down it without pushing, which is the same thing nested.
     * The second arm is the first mirrored across Z, with its rotations negated.
     */
    @Override
    protected void setup(Gem.Model<AssemblyRenderState> declaration) {
        Gem.Part<AssemblyRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw);

        machine.child(model.part("Base"), surface);
        machine.child(model.part("Frame"), surface)
                .visibleIf(state -> state.showFrame);

        Gem.Part<AssemblyRenderState> ring = machine.joint()
                .angle(Gem.Axes.Y, state -> (float) state.ring);
        ring.child(model.part("Ring"), surface);
        arm(ring, "1", 1F, state -> state.arm1);
        arm(ring, "2", -1F, state -> state.arm2);
    }

    private void arm(Gem.Part<AssemblyRenderState> ring, String suffix, float side,
                     Function<AssemblyRenderState, double[]> angles) {
        Gem.Part<AssemblyRenderState> lower = ring.joint()
                .translate(0F, 1.625F, side * 0.9375F)
                .angle(Gem.Axes.X, state -> (float) (side * angles.apply(state)[0]))
                .translate(0F, -1.625F, side * -0.9375F);
        lower.child(model.part("ArmLower" + suffix), surface);

        Gem.Part<AssemblyRenderState> upper = lower.joint()
                .translate(0F, 2.375F, side * 0.9375F)
                .angle(Gem.Axes.X, state -> (float) (side * angles.apply(state)[1]))
                .translate(0F, -2.375F, side * -0.9375F);
        upper.child(model.part("ArmUpper" + suffix), surface);

        Gem.Part<AssemblyRenderState> head = upper.joint()
                .translate(0F, 2.375F, side * 0.4375F)
                .angle(Gem.Axes.X, state -> (float) (side * angles.apply(state)[2]))
                .translate(0F, -2.375F, side * -0.4375F);
        head.child(model.part("Head" + suffix), surface);
        head.child(model.part("Spike" + suffix), surface)
                .slide(Gem.Axes.Y, state -> (float) angles.apply(state)[3]);
    }

    @Override
    public AssemblyRenderState createRenderState() {
        return new AssemblyRenderState();
    }

    @Override
    public void extract(TileEntityMachineAssemblyMachine be, AssemblyRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.showFrame = be.getLevel() != null
                && !be.getLevel().getBlockState(be.getBlockPos().above(FRAME_HEIGHT)).isAir();
        state.ring = be.prevRing + (be.ring - be.prevRing) * partialTicks;
        state.arm1 = be.arms[0].getPositions(partialTicks);
        state.arm2 = be.arms[1].getPositions(partialTicks);

        // Floating product display: the selected recipe's output item, shown within 35 blocks (1.12.2).
        state.showItem = false;
        GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe(be.assemblerModule.recipe);
        if (recipe != null && be.getLevel() != null) {
            // Measured where the machine is seen: on a moving sub-level (Create Aeronautics) its block sits far from it.
            double distanceSq = SubLevels.distanceSq(be.getLevel(),
                    be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 1.0, be.getBlockPos().getZ() + 0.5,
                    cameraPosition.x, cameraPosition.y, cameraPosition.z);
            if (distanceSq < 35 * 35) {
                ItemStack icon = recipe.getIcon();
                if (!icon.isEmpty()) {
                    ItemStack one = icon.copy();
                    one.setCount(1);
                    state.outputItem.extract(be.getLevel(), one, ItemDisplayContext.FIXED);
                    state.itemIs3d = one.getItem() instanceof BlockItem;
                    state.showItem = !state.outputItem.isEmpty();
                }
            }
        }
        if (!state.showItem) state.outputItem.clear();
    }

    /** The floating product is a recipe-chosen item model, so it has no mesh either backend could hold. */
    @Override
    protected void submitUndeclared(AssemblyRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        if (!s.showItem) return;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));
        ps.translate(0F, 1.0625F, 0F);
        if (s.itemIs3d) {
            ps.translate(0F, 0.125F, 0F);
            ps.scale(1.25F, 1.25F, 1.25F);
        } else {
            ps.translate(0F, 0.015F, 0F);
            ps.mulPose(Axis.XP.rotationDegrees(-90F));
            ps.mulPose(Axis.ZP.rotationDegrees(-90F));
            ps.scale(0.85F, 0.85F, 0.85F);
        }
        col.submitItem(ps, s.outputItem, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }
}
