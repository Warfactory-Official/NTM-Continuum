package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineSolderingStation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Renderer for the soldering station: the static OBJ (with the asymmetric-footprint offset it needs) plus
 * the current recipe's output item floating above — the 1.12.2 product display, drawn exactly the way the
 * assembly machine renders its floating output (an {@link net.minecraft.client.renderer.item.CompatItemRenderState}
 * built in {@code extractRenderState}, submitted above the model). The output is the synced
 * {@link TileEntityMachineSolderingStation#display}.
 */
public class SolderingStationRenderer extends GemBER<TileEntityMachineSolderingStation, SolderingRenderState> {

    private static final float BASE_YAW = 90F;

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/soldering_station.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/soldering_station.png");

    /** The trailing shift is the asymmetric-footprint offset, as in the generic-renderer registration. */
    @Override
    protected void setup(Gem.Model<SolderingRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(model.all(), surface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw)
                .translate(-0.5F, 0F, 0.5F);
    }

    @Override
    public SolderingRenderState createRenderState() {
        return new SolderingRenderState();
    }

    @Override
    public void extract(TileEntityMachineSolderingStation be, SolderingRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };

        // Floating product display: the current recipe's output, shown within 35 blocks (1.12.2), built the
        // same way as the assembly machine's floating output.
        state.showItem = false;
        ItemStack display = be.display;
        if (display != null && !display.isEmpty() && be.getLevel() != null) {
            double dx = cameraPosition.x - (be.getBlockPos().getX() + 0.5);
            double dy = cameraPosition.y - (be.getBlockPos().getY() + 1.0);
            double dz = cameraPosition.z - (be.getBlockPos().getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz < 35 * 35) {
                ItemStack one = display.copy();
                one.setCount(1);
                state.outputItem.extract(be.getLevel(), one, ItemDisplayContext.FIXED);
                state.itemIs3d = one.getItem() instanceof BlockItem;
                state.showItem = !state.outputItem.isEmpty();
            }
        }
        if (!state.showItem) state.outputItem.clear();
    }

    /**
     * The floating product is a recipe-chosen item model with no mesh of its own; the transforms are
     * the 1.12.2 ones, matching the assembly machine's floating output.
     */
    @Override
    protected void submitUndeclared(SolderingRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        if (!s.showItem) return;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));
        ps.translate(-0.5F, 0F, 0.5F);
        ps.translate(0F, 1.1F, 0F);
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
