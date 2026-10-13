package com.hbm.client.render.item;

import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.client.render.RenderPlushie;
import com.hbm.items.LegacyMeta;
import com.hbm.items.ModDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code RenderPlushie}'s item, an {@code ItemRenderBase}: inventory {@code T(0, -6, 0) S(6)}; every slot
 * {@code T(0, 0.25, 0)}, the type's own offset and scale, then the block renderer's {@code renderPlushie(type,
 * false)} - Number Nine's helmet, insignia and cigarette included.
 */
final class ItemRenderPlushie implements CustomItemRenderer {

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return true;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        PlushieType[] types = PlushieType.values();
        int ordinal = LegacyMeta.of(stack);
        // 1.7 EnumUtil.grabEnumSafely: abs(index % length)
        PlushieType plushie = types[Math.abs(ordinal % types.length)];

        pose.pushPose();
        if (type == Type.INVENTORY) {
            pose.translate(0D, -6D, 0D);
            pose.scale(6F, 6F, 6F);
        }
        pose.translate(0D, 0.25D, 0D);
        switch (plushie) {
            case YOMI -> pose.scale(1.25F, 1.25F, 1.25F);
            case NUMBERNINE -> {
                pose.translate(0D, 0.25D, 0.25D);
                pose.scale(1.25F, 1.25F, 1.25F);
            }
            case HUNDUN -> {
                pose.translate(0.5D, 0.5D, 0D);
                pose.scale(1.25F, 1.25F, 1.25F);
            }
            case DERG -> pose.scale(1.5F, 1.5F, 1.5F);
            default -> { }
        }
        RenderPlushie.renderItem(pose, sink, plushie, light);
        pose.popPose();
    }
}
