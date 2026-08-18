package com.hbm.client.render.armor;

import com.hbm.items.gear.ArmorFSB;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CompatHumanoidLayer;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Worn;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.item.ItemStack;

/**
 * Draws HBM's modelled armour on anything humanoid — players, mannequins, armour stands, zombies.
 *
 * <p>This is the whole of the port's worn-armour rendering: one layer, attached by Golem Core to every
 * humanoid renderer, dispatching each occupied slot to the suit's {@link ArmorRenderer}. A suit with no
 * declared model falls through and is drawn by vanilla's ordinary equipment-texture layer instead, which
 * is exactly what the hazmat suits want.
 */
public class ArmorRenderLayer extends CompatHumanoidLayer {

    @SuppressWarnings("rawtypes")
    public ArmorRenderLayer(RenderLayerParent parent) {
        super(parent);
    }

    private static final ArmorSlot[] SLOTS = ArmorSlot.values();

    @Override
    public void submit(Worn worn, PoseStack pose, SubmitSink sink, int light, HumanoidPose parts) {
        if (parts.invisible) return;

        for (ArmorSlot slot : SLOTS) {
            ItemStack stack = worn.get(slot);
            if (!(stack.getItem() instanceof ArmorFSB piece)) continue;

            ArmorRenderer renderer = ArmorRenderers.get(piece.armorModel());
            if (renderer == null) continue;

            renderer.submit(slot, pose, sink, light, OverlayTexture.NO_OVERLAY, parts);
        }
    }
}
