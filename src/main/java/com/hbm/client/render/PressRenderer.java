package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * A press: a static body, a stamp head that descends with the machine's progress, and the ingredient
 * lying on the anvil between them.
 *
 * <p>Whether the head turns with the body is per-machine: the e-press repeats the body's facing switch
 * on its head, the press does not.
 *
 * <p>The ingredient is the one thing here that is not a resident mesh — it is whatever the input slot
 * holds, baked by the item pipeline — so it is drawn in {@link #submitUndeclared}. Its placement is
 * 1.12.2's rather than 1.7's: 1.7 offsets it by {@code 0.0625 * 165/100} to clear the depth its own
 * extruded-sprite renderer added, which the modern item pipeline does not add, and 1.12.2 re-derived the
 * chain without it. The two differ by 1.65 pixels off the anvil's centre.
 */
public class PressRenderer<T extends BlockEntity> extends GemBER<T, PressRenderState> {

    private final WavefrontObj body;
    private final WavefrontObj head;
    private final Surface bodySurface;
    private final Surface headSurface;
    private final float baseYaw;
    private final boolean applyFacing;
    private final float headBaseOffset;
    private final float headScaleXZ;
    private final boolean headTurnsWithBody;
    private final int maxProgress;
    private final ToIntFunction<T> progress;
    private final ToIntFunction<T> lastProgress;
    /** Where the anvil is along the machine's own X, measured from the block centre. */
    private final float itemOffsetX;
    /** The ingredient's own yaw within the machine's frame, before it is laid flat. */
    private final float itemYaw;
    private final Function<T, ItemStack> pressed;

    public PressRenderer(Identifier bodyObj, Identifier bodyTex, Identifier headObj, Identifier headTex,
                         float baseYaw, boolean applyFacing, float headBaseOffset, float headScaleXZ,
                         boolean headTurnsWithBody, int maxProgress, ToIntFunction<T> progress,
                         ToIntFunction<T> lastProgress, float itemOffsetX, float itemYaw,
                         Function<T, ItemStack> pressed) {
        this.body = WavefrontObj.load(bodyObj);
        this.head = WavefrontObj.load(headObj);
        this.bodySurface = Surface.cutout(bodyTex.toString());
        this.headSurface = Surface.cutout(headTex.toString());
        this.baseYaw = baseYaw;
        this.applyFacing = applyFacing;
        this.headBaseOffset = headBaseOffset;
        this.headScaleXZ = headScaleXZ;
        this.headTurnsWithBody = headTurnsWithBody;
        this.maxProgress = maxProgress;
        this.progress = progress;
        this.lastProgress = lastProgress;
        this.itemOffsetX = itemOffsetX;
        this.itemYaw = itemYaw;
        this.pressed = pressed;
    }

    @Override
    protected void setup(Gem.Model<PressRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(body.all(), bodySurface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> baseYaw + state.yaw);

        Gem.Part<PressRenderState> stamp = declaration.part(head.all(), headSurface)
                .translate(0.5F, headBaseOffset, 0.5F)
                .slide(Gem.Axes.Y, state -> -state.progress * 0.875F / maxProgress);
        if (headTurnsWithBody) stamp.angle(Gem.Axes.Y, state -> baseYaw + state.yaw);
        if (headScaleXZ != 1.0F) stamp.scale(headScaleXZ, 1.0F, headScaleXZ);
    }

    @Override
    public PressRenderState createRenderState() {
        return new PressRenderState();
    }

    @Override
    public void extract(T be, PressRenderState state, float partialTicks, Vec3 cameraPosition) {
        if (applyFacing) {
            BlockState bs = be.getBlockState();
            int meta = MachineCore.facingOrdinal(bs, 2);
            state.yaw = switch (meta) {
                case 4 -> 90F;
                case 3 -> 180F;
                case 5 -> 270F;
                default -> 0F;
            };
        } else {
            state.yaw = 0F;
        }
        int last = lastProgress.applyAsInt(be);
        state.progress = last + (progress.applyAsInt(be) - last) * partialTicks;

        state.showItem = false;
        ItemStack stack = pressed.apply(be);
        if (stack != null && !stack.isEmpty()) {
            ItemStack one = stack.copy();
            one.setCount(1);
            state.pressed.extract(be.getLevel(), one, ItemDisplayContext.FIXED);
            state.itemIs3d = state.pressed.isGui3d();
            state.showItem = !state.pressed.isEmpty();
        }
        if (!state.showItem) state.pressed.clear();
    }

    /**
     * The ingredient on the anvil: a slot-chosen item model with no mesh of its own. A flat sprite is
     * laid face up where 1.12.2 puts it; a block model stays upright and is lifted to stand on the
     * anvil rather than sink half into it, which is why 1.12.2 declined to draw one at all.
     */
    @Override
    protected void submitUndeclared(PressRenderState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (!state.showItem) return;
        pose.pushPose();
        pose.translate(0.5F, 1F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(baseYaw + state.yaw));
        pose.translate(itemOffsetX, 0F, 0F);
        if (state.itemIs3d) {
            pose.translate(0F, 0.125F, 0F);
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(itemYaw));
            pose.mulPose(Axis.XP.rotationDegrees(-90F));
            pose.mulPose(Axis.YP.rotationDegrees(180F));
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        sink.submitItem(pose, state.pressed, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
