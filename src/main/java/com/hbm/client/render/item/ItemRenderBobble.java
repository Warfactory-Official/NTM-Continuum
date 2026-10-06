package com.hbm.client.render.item;

import com.hbm.client.render.RenderBobble;
import com.hbm.items.ModDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ItemRenderLibrary}'s bobblehead entry, an {@code ItemRenderBase}: inventory {@code T(0, -3.5, 0)
 * S(10)}, then for every slot {@code S(0.5)} and the block renderer's own {@code renderBobble} for the stack's
 * type - so the item carries everything the placed figure does, engraved name, props and glow skins included.
 * The frame around it is {@code ItemRenderBase}'s, supplied by the item's {@code machine} frame.
 */
final class ItemRenderBobble implements CustomItemRenderer {

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return true;
    }

    /** The head bobs on the wall clock, and MELLOW/PU238 glow on it. */
    @Override
    public boolean animated(ItemStack stack, Type type) {
        return true;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        pose.pushPose();
        if (type == Type.INVENTORY) {
            pose.translate(0D, -3.5D, 0D);
            pose.scale(10F, 10F, 10F);
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        RenderBobble.renderItem(pose, sink, com.hbm.items.LegacyMeta.of(stack), light);
        pose.popPose();
    }
}
