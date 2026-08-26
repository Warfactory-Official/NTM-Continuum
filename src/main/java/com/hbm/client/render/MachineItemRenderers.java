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
        if (MachineItemModels.isEmpty()) return;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        MachineItemModel model = MachineItemModels.get(id, stack);
        RBMKColumnItemModel column = MachineItemModels.rbmkColumn(id);
        RBMKBlockColumnItemModel block = MachineItemModels.blockColumn(id);
        if (model == null && column == null && block == null) return;

        // In a GUI these models must not paint outside their slot; see clipToSlot.
        boolean clipped = context == ItemDisplayContext.GUI && clipToSlot(poseStack);

        RecordingSubmitSink sink = new RecordingSubmitSink();
        if (column != null) column.submit(poseStack, sink, light, overlay);
        else if (block != null) block.submit(poseStack, sink, light, overlay);
        else model.submit(poseStack, sink, light, overlay);
        // No level: these draws happen in inventory/hand space, and the sink only needs one for item
        // sub-draws, which an OBJ machine never makes.
        sink.flush(buffers, null);

        if (clipped) {
            // The scissor only binds while the draw actually reaches GL, and a buffer source defers that
            // to its batch end — which would otherwise happen after the scissor is dropped.
            if (buffers instanceof MultiBufferSource.BufferSource batched) batched.endBatch();
            RenderSystem.disableScissor();
        }
    }

    /^ 
     * Clips drawing to the 16x16 slot this item is being rendered into, returning whether a scissor was
     * actually set.
     *
     * <p>26.1 gives every GUI item a bounding rectangle — {@code GuiItemRenderState.bounds}, which for an
     * ordinary item is exactly its slot — and an item may only exceed it by declaring
     * {@code oversized_in_gui}. Nothing equivalent exists here: a custom item renderer draws wherever its
     * geometry reaches, so a machine model spills over the surrounding inventory frame. Re-imposing the
     * slot reproduces 26.1's default.
     *
     * <p>The rectangle is recovered from the pose rather than passed in, because the callback is never
     * told which slot it is drawing into. By the time vanilla calls a custom renderer it has translated to
     * the slot centre, scaled by 16, applied the model's GUI display transform and then shifted by
     * {@code (-0.5,-0.5,-0.5)} — so the model-space point {@code (0.5,0.5,0.5)} is the slot centre, and
     * transforming it back out yields that centre in GUI pixels.
     ^/
    private static boolean clipToSlot(PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() == null) return false;

        Vector3f centre = new Vector3f(0.5F, 0.5F, 0.5F).mulPosition(poseStack.last().pose());
        double guiScale = minecraft.getWindow().getGuiScale();
        // Framebuffer coordinates: origin bottom-left, so the top edge is measured from the window height.
        // This is the same conversion GuiGraphicsExtractor.applyScissor performs.
        int left = (int) ((centre.x() - HALF_SLOT) * guiScale);
        int bottom = (int) (minecraft.getWindow().getHeight() - (centre.y() + HALF_SLOT) * guiScale);
        int size = (int) (HALF_SLOT * 2 * guiScale);
        if (size <= 0) return false;

        RenderSystem.enableScissor(left, bottom, size, size);
        return true;
    }
}
*///?}
