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

/**
 * T-51b's worn body — the port's first modelled armour set.
 *
 * <p>The part tree is 1.12.2's {@code ModelArmorT51}: eight named objects in one OBJ, four textures
 * across them, and the boots drawn on the <b>leg</b> bones rather than bones of their own (the vanilla
 * humanoid has no foot bone, which is why upstream's model class copies the leg angles into its
 * {@code leftFoot}/{@code rightFoot} renderers).
 *
 * <p>Textures are per-region, not per-slot: the chestplate draws its torso from {@code t51_chest} and
 * its arms from {@code t51_arm}, and the leggings and boots share {@code t51_leg}.
 */
public class T51ArmorRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/t51.obj");

    private static final Identifier HELMET_TEX = tex("t51_helmet");
    private static final Identifier CHEST_TEX = tex("t51_chest");
    private static final Identifier ARM_TEX = tex("t51_arm");
    private static final Identifier LEG_TEX = tex("t51_leg");

    private static Identifier tex(String name) {
        return Ids.of("hbm", "textures/models/armor/" + name + ".png");
    }

    /** Loaded on first use: resource reloads happen before any entity renders. */
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(OBJ);
        return model;
    }

    @Override
    public void submit(ArmorSlot slot, PoseStack pose, SubmitSink sink, int light, int overlay,
            HumanoidPose parts) {

        WavefrontObj obj = model();

        switch (slot) {
            case HELMET -> submitPart(sink, pose, type(HELMET_TEX), obj, "Helmet", parts.head(), light, overlay);

            case CHESTPLATE -> {
                submitPart(sink, pose, type(CHEST_TEX), obj, "Chest", parts.body(), light, overlay);
                RenderType arm = type(ARM_TEX);
                submitPart(sink, pose, arm, obj, "LeftArm", parts.leftArm(), light, overlay);
                submitPart(sink, pose, arm, obj, "RightArm", parts.rightArm(), light, overlay);
            }

            case LEGGINGS -> {
                RenderType leg = type(LEG_TEX);
                submitPart(sink, pose, leg, obj, "LeftLeg", parts.leftLeg(), light, overlay);
                submitPart(sink, pose, leg, obj, "RightLeg", parts.rightLeg(), light, overlay);
            }

            // The boots ride the leg bones — see the class comment.
            case BOOTS -> {
                RenderType leg = type(LEG_TEX);
                submitPart(sink, pose, leg, obj, "LeftBoot", parts.leftLeg(), light, overlay);
                submitPart(sink, pose, leg, obj, "RightBoot", parts.rightLeg(), light, overlay);
            }
        }
    }

    private static RenderType type(Identifier texture) {
        return CompatRenderTypes.entityCutout(texture);
    }
}
