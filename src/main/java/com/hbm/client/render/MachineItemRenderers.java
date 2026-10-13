package com.hbm.client.render;

//? if <26.1 {

/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.RecordingSubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

 /^ 
 * Draws an OBJ machine item below 26.1, where there is no special-model system and the equivalent hook
 * is a per-loader "render this item yourself" callback (NeoForge's BEWLR, Fabric's DynamicItemRenderer).
 * Both hand over a {@code MultiBufferSource}, so the model is submitted into a {@link RecordingSubmitSink}
 * and flushed straight into it.
 *
 * <p>Vanilla only reaches these callbacks for items whose baked model reports {@code isCustomRenderer()},
 * which is true exactly when the model's parent chain ends at {@code minecraft:builtin/entity}. The build
 * generates such a model for every machine_obj item on these versions, so registering the callback for
 * every HBM item costs nothing and misroutes nothing.
 ^/
public final class MachineItemRenderers {

    private MachineItemRenderers() {}

    /^ Half a slot, in GUI pixels — vanilla renders an inventory item into a 16x16 box centred on it. ^/
    private static final float HALF_SLOT = 8.0F;

    /^ Renders {@code stack} if it is an OBJ machine; does nothing otherwise. ^/
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                              MultiBufferSource buffers, int light, int overlay) {
        // Guns share this hook: it is the one place below 26.1 that is handed the display context, which
        // a weapon needs in order to tell a held body from an inventory icon.
        if (com.hbm.client.render.weapon.GunLegacyModels.render(stack, context, poseStack, buffers, light, overlay)) return;
        // Ported 1.7 IItemRenderers (hbm:custom_item) are context-driven for the same reason.
        if (MachineItemModels.renderCustom(stack, context, poseStack, buffers, light, overlay)) return;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (MachineItemModels.isGlider(id)) {
            com.hbm.client.jetpack.JetpackGliderLegacyRenderer.render(stack, context, poseStack, buffers, light, overlay);
            return;
        }
        if (MachineItemModels.isEmpty()) return;
        MachineItemModel model = MachineItemModels.get(id, stack);
        RBMKColumnItemModel column = MachineItemModels.rbmkColumn(id);
        RBMKBlockColumnItemModel block = MachineItemModels.blockColumn(id);
        if (model == null && column == null && block == null) return;

        // In a GUI these models must not paint outside their slot; see clipToSlot.
        boolean clipped = context == ItemDisplayContext.GUI
                && !MachineItemModels.isOversizedInGui(id) && clipToSlot(poseStack, stack);

        RecordingSubmitSink sink = new RecordingSubmitSink();
        if (column != null) column.submit(poseStack, sink, light, overlay);
        else if (block != null) block.submit(poseStack, sink, light, overlay);
        else model.submit(poseStack, sink, light, overlay, context);
        // No level: these draws happen in inventory/hand space, and the sink only needs one for item
        // sub-draws, which an OBJ machine never makes.
        sink.flush(buffers, null);

        if (clipped) {
            // The scissor only binds while the draw actually reaches GL, and a buffer source defers that
            // to its batch end, which would otherwise happen after the scissor is dropped.
            if (buffers instanceof MultiBufferSource.BufferSource batched) batched.endBatch();
            dev.golem.core.gui.Scissors.pop();
        }
    }

    /^ 
     * Clips drawing to the 16x16 slot this item is being rendered into, returning whether a scissor was
     * actually set.
     *
     * <p>26.1 gives every GUI item a bounding rectangle, {@code GuiItemRenderState.bounds}, which for an
     * ordinary item is its slot, and an item may only exceed it by declaring {@code oversized_in_gui}.
     * A custom item renderer here draws wherever its geometry reaches, so a machine model would spill over
     * the inventory frame. Re-imposing the slot reproduces 26.1's default, and reading the same
     * {@code oversized_in_gui} flag reproduces the opt-out, which the machines framed from 1.7 need: 1.7
     * clipped nothing, and its inventory scales put the larger multiblocks a few pixels outside the slot.
     *
     * <p>The rectangle is recovered from the pose because the callback is never told which slot it draws
     * into. By the time vanilla calls a custom renderer it has translated to the slot centre, scaled by 16,
     * applied the model's GUI display transform and shifted by {@code (-0.5,-0.5,-0.5)}, so the model-space
     * point {@code (0.5,0.5,0.5)} is the slot centre moved by that display transform's translation, which is
     * applied outermost ({@code T . R . S}). The machine frames translate (machine_obj_context_base: -0.707,
     * -1.567 px), so that translation is taken back out; otherwise the clip sits 1.6 px low and cuts the top
     * of tall icons (the trenchmaster and fau antennas) that 26.1, whose bound is the slot itself, keeps.
     ^/
    private static boolean clipToSlot(PoseStack poseStack, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() == null) return false;

        Vector3f centre = new Vector3f(0.5F, 0.5F, 0.5F).mulPosition(poseStack.last().pose());
        // ItemTransform.translation is in blocks; the GUI base pose is scale(16, -16, 16).
        // The shaper's per-item model is a map lookup; only an item whose model carries overrides (a
        // custom_model_data variant may frame differently) pays for getModel's override resolution.
        net.minecraft.client.resources.model.BakedModel model = minecraft.getItemRenderer().getItemModelShaper().getItemModel(stack);
        if (model.getOverrides() != net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY) {
            model = minecraft.getItemRenderer().getModel(stack, minecraft.level, null, 0);
        }
        Vector3f shift = model.getTransforms().getTransform(ItemDisplayContext.GUI).translation;
        centre.add(-shift.x() * 16F, shift.y() * 16F, 0F);
        double guiScale = minecraft.getWindow().getGuiScale();
        // Framebuffer coordinates: origin bottom-left, so the top edge is measured from the window height.
        // This is the same conversion GuiGraphicsExtractor.applyScissor performs.
        int left = (int) ((centre.x() - HALF_SLOT) * guiScale);
        int bottom = (int) (minecraft.getWindow().getHeight() - (centre.y() + HALF_SLOT) * guiScale);
        int size = (int) (HALF_SLOT * 2 * guiScale);
        if (size <= 0) return false;

        // Core Scissors: nested in (and restored to) any clip already active, e.g. the advancement window's
        return dev.golem.core.gui.Scissors.push(left, bottom, size, size);
    }
}
*///?}
