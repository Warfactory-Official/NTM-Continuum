package com.hbm.client.jetpack;

import com.hbm.client.render.armor.ArmorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatHumanoidLayer;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Worn;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.item.ItemStack;

/** The layer for direct jetpack items and chestplate-mounted jetpack modules. */
public final class JetpackRenderLayer extends CompatHumanoidLayer {

    @SuppressWarnings("rawtypes")
    public JetpackRenderLayer(RenderLayerParent parent) {
        super(parent);
    }

    @Override
    public void submit(Worn worn, PoseStack pose, SubmitSink sink, int light, HumanoidPose parts,
            CameraInfo camera) {
        if (parts.invisible) return;
        ItemStack chest = worn.chest();
        String kind = JetpackClientVisuals.kindFromStack(chest);
        if (kind == null) kind = JetpackClientVisuals.kindFromStack(
                com.hbm.handler.ArmorModHandler.pryMod(chest, com.hbm.handler.ArmorModHandler.plate_only));
        if (kind == null) return;

        boolean modeledArmor = JetpackClientVisuals.BJ.equals(kind) || "wings".equals(kind);
        if (modeledArmor) {
            pose.pushPose();
            if (parts.sneaking && !parts.baby && !JetpackClientVisuals.freezesPlayerPose(parts.player()))
                pose.translate(0F, 0.2F, 0F);
        }
        switch (kind) {
            case JetpackClientVisuals.GLIDER -> {
                JetpackClientBridge.State state = parts.player() == null
                        ? null : JetpackClientBridge.state(parts.player());
                boolean opening = state == null || state.opening();
                long animTime = state == null ? 0L : state.animTime();
                JetpackClientVisuals.submitGlider(pose, sink, light, parts, false, opening, animTime);
                if (parts.player() != null)
                    JetpackClientVisuals.submitLocalEffects(pose, camera, parts.sneaking, parts.player());
            }
            case JetpackClientVisuals.BJ -> JetpackClientVisuals.submitBJ(pose, sink, light, parts);
            case "wings" -> JetpackClientVisuals.submitWings(pose, sink, light, parts,
                    JetpackClientVisuals.sourceStack(chest));
            case "standard" -> {
                ItemStack source = JetpackClientVisuals.kindFromStack(chest) == null
                        ? com.hbm.handler.ArmorModHandler.pryMod(chest,
                        com.hbm.handler.ArmorModHandler.plate_only) : chest;
                String path = JetpackClientVisuals.path(source);
                String textureKind = path != null && path.endsWith("break") ? "standard_blue"
                        : path != null && path.endsWith("vector") ? "standard_green"
                        : path != null && path.endsWith("boost") ? "standard_boost" : "standard";
                JetpackClientVisuals.submitStandard(pose, sink, light, parts, textureKind);
            }
            default -> {
                // DNS has its own exhaust packet but no physical backpack in the old DNT model.
            }
        }
        if (modeledArmor) pose.popPose();
    }
}
