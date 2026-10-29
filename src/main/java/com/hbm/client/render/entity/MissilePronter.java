package com.hbm.client.render.entity;

import com.hbm.client.model.WavefrontObj;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 MissilePronter: an assembled custom missile drawn bottom to top, each part raised by the height of the one
 * below it. Meshes and skins are resolved from each part's own declaration on first draw; a part that declares
 * no mesh (the guidance chips, as in 1.7) draws nothing.
 */
public final class MissilePronter {

    private final Map<Identifier, WavefrontObj> meshes = new HashMap<>();
    private final Map<Identifier, RenderType> skins = new HashMap<>();

    public void pront(MissileStruct missile, PoseStack pose, SubmitSink sink, int light) {
        pose.pushPose();

        ItemCustomMissilePart thruster = missile.thruster();
        if (thruster != null && thruster.type == PartType.THRUSTER) {
            draw(thruster, pose, sink, light);
            pose.translate(0F, thruster.height, 0F);
        }

        ItemCustomMissilePart fuselage = missile.fuselage();
        if (fuselage != null && fuselage.type == PartType.FUSELAGE) {
            // 1.7: the fins ride the fuselage's own base, so they draw before it and move nothing
            ItemCustomMissilePart fins = missile.fins();
            if (fins != null && fins.type == PartType.FINS) draw(fins, pose, sink, light);
            draw(fuselage, pose, sink, light);
            pose.translate(0F, fuselage.height, 0F);
        }

        ItemCustomMissilePart warhead = missile.warhead();
        if (warhead != null && warhead.type == PartType.WARHEAD) draw(warhead, pose, sink, light);

        pose.popPose();
    }

    private void draw(ItemCustomMissilePart part, PoseStack pose, SubmitSink sink, int light) {
        Identifier meshId = part.mesh;
        Identifier skinId = part.skin;
        if (meshId == null || skinId == null) return;
        WavefrontObj mesh = meshes.computeIfAbsent(meshId,
                id -> WavefrontObj.load(id, ItemCustomMissilePart.useVertexNormals(id)));
        RenderType skin = skins.computeIfAbsent(skinId, CompatRenderTypes::entityCutoutCull);
        sink.submitCustomGeometry(pose, skin,
                (p, buffer) -> mesh.renderAll(p, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    /**
     * True when a struct has at least one drawable part.
     */
    public static boolean drawable(@Nullable MissileStruct missile) {
        if (missile == null) return false;
        for (ItemCustomMissilePart part : new ItemCustomMissilePart[]{missile.thruster(), missile.fuselage(),
                missile.fins(), missile.warhead()}) {
            if (part != null && part.mesh != null) return true;
        }
        return false;
    }
}
