package com.hbm.client.render.armor;

import com.hbm.items.gear.ArmorFSB;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CompatHumanoidLayer;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Worn;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

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
    public void submit(Worn worn, PoseStack pose, SubmitSink sink, int light, HumanoidPose parts,
            CameraInfo camera) {
        if (parts.invisible) return;

        boolean crouching = parts.sneaking
                && !com.hbm.client.jetpack.JetpackClientVisuals.freezesPlayerPose(parts.player());
        HumanoidPose armorPose = crouching == parts.sneaking ? parts : new HumanoidPose(parts.model(), false,
                parts.baby, parts.invisible, parts.player());
        pose.pushPose();
        // ModelArmorBase applies the same global offset after copying the live crouched bones.
        if (crouching && !parts.baby) pose.translate(0F, 0.2F, 0F);
        for (ArmorSlot slot : SLOTS) {
            ItemStack stack = worn.get(slot);
            if (!(stack.getItem() instanceof ArmorFSB piece)) continue;

            String modelName = piece.armorModel();
            if (modelName == null) {
                String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                if (path.startsWith("bj_")) modelName = "bj";
                else if (path.startsWith("dns_")) modelName = "dns";
            }
            ArmorRenderer renderer = ArmorRenderers.get(modelName);
            if (renderer == null) continue;

            renderer.submit(slot, pose, sink, light, OverlayTexture.NO_OVERLAY, armorPose);
        }
        pose.popPose();
    }
}
