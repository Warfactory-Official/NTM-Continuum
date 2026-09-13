package com.hbm.client.jetpack;

//? if <26.1 {

/*import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.RecordingSubmitSink;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/^ Legacy BEWLR/DynamicItemRenderer adapter for the same generated glider model. ^/
public final class JetpackGliderLegacyRenderer {

    private JetpackGliderLegacyRenderer() {}

    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        RecordingSubmitSink sink = new RecordingSubmitSink();
        JetpackGliderItemModel.shared().submit(pose, sink, light, overlay, context);
        sink.flush(buffers, null);
    }
}
*///?}
