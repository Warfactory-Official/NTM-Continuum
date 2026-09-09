package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** BJ suit model, including the common body/limb passes; its Jetpack group is owned by JetpackRenderLayer. */
public final class BJArmorRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/bj.obj");
    private WavefrontObj model;
    private final RenderType helmet = type("bj_eyepatch");
    private final RenderType chest = type("bj_chest");
    private final RenderType arm = type("bj_arm");
    private final RenderType leg = type("bj_leg");

    @Override
    public void submit(ArmorSlot slot, PoseStack pose, SubmitSink sink, int light, int overlay, HumanoidPose parts) {
        WavefrontObj model = model();
        switch (slot) {
            case HELMET -> submitPart(sink, pose, helmet, model, "Head", parts.head(), Pivot.HEAD, parts.sneaking, light, overlay);
            case CHESTPLATE -> {
                submitPart(sink, pose, chest, model, "Body", parts.body(), Pivot.BODY, parts.sneaking, light, overlay);
                submitPart(sink, pose, arm, model, "LeftArm", parts.leftArm(), Pivot.LEFT_ARM, parts.sneaking, light, overlay);
                submitPart(sink, pose, arm, model, "RightArm", parts.rightArm(), Pivot.RIGHT_ARM, parts.sneaking, light, overlay);
            }
            case LEGGINGS -> {
                submitPart(sink, pose, leg, model, "LeftLeg", parts.leftLeg(), Pivot.LEFT_LEG, parts.sneaking, light, overlay);
                submitPart(sink, pose, leg, model, "RightLeg", parts.rightLeg(), Pivot.RIGHT_LEG, parts.sneaking, light, overlay);
            }
            case BOOTS -> {
                submitPart(sink, pose, leg, model, "LeftFoot", parts.leftLeg(), Pivot.LEFT_LEG, parts.sneaking, light, overlay);
                submitPart(sink, pose, leg, model, "RightFoot", parts.rightLeg(), Pivot.RIGHT_LEG, parts.sneaking, light, overlay);
            }
        }
    }

    private static RenderType type(String name) {
        return CompatRenderTypes.entityCutout(Ids.of("hbm", "textures/armor/" + name + ".png"));
    }

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(OBJ);
        return model;
    }
}
