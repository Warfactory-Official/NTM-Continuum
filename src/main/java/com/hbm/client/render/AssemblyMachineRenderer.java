package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Animated renderer for the assembly machine — the worked example of porting a 1.12.2 OBJ TESR to the
 * 26.1 render-state pipeline. It replays the original renderer's transforms: a base + (conditional)
 * frame, a spinning ring, and under that ring two three-joint robotic arms (lower/upper arm, head,
 * striker). Animation state lives on the {@link TileEntityMachineAssemblyMachine} (advanced by its
 * client ticker); here it is read and interpolated by partial-ticks.
 *
 * <p>Each part is a separate {@code submitCustomGeometry} call — that snapshots the current pose, so
 * the accumulated joint transforms apply per part.
 */
public class AssemblyMachineRenderer implements CompatBER<TileEntityMachineAssemblyMachine, AssemblyRenderState> {

    private static final float BASE_YAW = 90F;
    private static final int FRAME_HEIGHT = 3;

    private final WavefrontObj model;
    private final RenderType renderType;

    public AssemblyMachineRenderer() {
        Identifier ns = dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/assembly_machine.obj");
        Identifier tex = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/assembly_machine.png");
        this.model = WavefrontObj.load(ns);
        this.renderType = CompatRenderTypes.entityCutout(tex);
    }

    @Override
    public AssemblyRenderState createRenderState() {
        return new AssemblyRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntityMachineAssemblyMachine blockEntity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extract(TileEntityMachineAssemblyMachine be, AssemblyRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH
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
            double dx = cameraPosition.x - (be.getBlockPos().getX() + 0.5);
            double dy = cameraPosition.y - (be.getBlockPos().getY() + 1.0);
            double dz = cameraPosition.z - (be.getBlockPos().getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz < 35 * 35) {
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

    @Override
    public void submit(AssemblyRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));

        part(col, ps, "Base", light);
        if (s.showFrame) part(col, ps, "Frame", light);

        // Ring (and the arms beneath it) spin together.
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) s.ring));
        part(col, ps, "Ring", light);

        // Arm 1.
        ps.pushPose();
        ps.translate(0F, 1.625F, 0.9375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) s.arm1[0]));
        ps.translate(0F, -1.625F, -0.9375F);
        part(col, ps, "ArmLower1", light);
        ps.translate(0F, 2.375F, 0.9375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) s.arm1[1]));
        ps.translate(0F, -2.375F, -0.9375F);
        part(col, ps, "ArmUpper1", light);
        ps.translate(0F, 2.375F, 0.4375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) s.arm1[2]));
        ps.translate(0F, -2.375F, -0.4375F);
        part(col, ps, "Head1", light);
        ps.translate(0F, (float) s.arm1[3], 0F);
        part(col, ps, "Spike1", light);
        ps.popPose();

        // Arm 2 (mirrored across Z; rotations negated).
        ps.pushPose();
        ps.translate(0F, 1.625F, -0.9375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm2[0]));
        ps.translate(0F, -1.625F, 0.9375F);
        part(col, ps, "ArmLower2", light);
        ps.translate(0F, 2.375F, -0.9375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm2[1]));
        ps.translate(0F, -2.375F, 0.9375F);
        part(col, ps, "ArmUpper2", light);
        ps.translate(0F, 2.375F, -0.4375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm2[2]));
        ps.translate(0F, -2.375F, 0.4375F);
        part(col, ps, "Head2", light);
        ps.translate(0F, (float) s.arm2[3], 0F);
        part(col, ps, "Spike2", light);
        ps.popPose();

        ps.popPose(); // ring

        // Floating product: the recipe output hovering above the machine centre (1.12.2 transforms).
        if (s.showItem) {
            ps.pushPose();
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
            col.submitItem(ps, s.outputItem, light, OverlayTexture.NO_OVERLAY, 0);
            ps.popPose();
        }

        ps.popPose(); // machine
    }

    private void part(SubmitSink col, PoseStack ps, String name, int light) {
        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
