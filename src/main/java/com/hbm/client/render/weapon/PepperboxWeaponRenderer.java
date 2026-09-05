package com.hbm.client.render.weapon;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemStack;

/** Exact Pepperbox part tree and first-person transforms. */
public final class PepperboxWeaponRenderer extends WeaponRenderer {

    private WavefrontObj model;
    private RenderType body;

    private WavefrontObj model() {
        if (model == null) {
            model = WavefrontObj.load(Ids.of("hbm", "models/weapons/pepperbox.obj"));
            body = CompatRenderTypes.entityCutout(Ids.of("hbm", "textures/models/weapons/pepperbox.png"));
        }
        return model;
    }

    @Override
    public org.joml.Vector3fc[] extents() {
        WavefrontObj obj = model();
        float[] min = obj.boundsMin(), max = obj.boundsMax();
        org.joml.Vector3fc[] out = new org.joml.Vector3fc[8];
        int i = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            out[i++] = new org.joml.Vector3f(x == 0 ? min[0] : max[0], y == 0 ? min[1] : max[1],
                    z == 0 ? min[2] : max[2]);
        }
        return out;
    }

    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
    }

    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1.5);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.25F * offset, -0.75F * offset, offset,
                0, -2.5 / 8D, 0.5);
    }

    @Override public void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink sink,
            int light, int overlay) {
        pose.scale(0.25F, 0.25F, 0.25F);
        double[] recoil = bus("RECOIL"), rotate = bus("ROTATE"), hammer = bus("HAMMER");
        double[] trigger = bus("TRIGGER"), translate = bus("TRANSLATE"), loader = bus("LOADER");
        pose.translate(translate[0], translate[1], translate[2]);
        pose.translate(0, 0, -5);
        pose.mulPose(Axis.XN.rotationDegrees((float) recoil[0]));
        pose.translate(0, 0, 5);

        if (loader[0] != 0 || loader[1] != 0 || loader[2] != 0) {
            pose.pushPose();
            pose.translate(loader[0], loader[1], loader[2]);
            submit("Speedloader", pose, sink, light, overlay);
            if (bus("SHOT")[0] != 0) submit("Shot", pose, sink, light, overlay);
            pose.popPose();
        }
        submit("Grip", pose, sink, light, overlay);
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees((float) rotate[0]));
        submit("Cylinder", pose, sink, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, 0.375, -1.875);
        pose.mulPose(Axis.XP.rotationDegrees((float) hammer[0]));
        pose.translate(0, -0.375, 1.875);
        submit("Hammer", pose, sink, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, 0, -trigger[0] * 0.5);
        submit("Trigger", pose, sink, light, overlay);
        pose.popPose();
    }

    @Override public void renderStatic(ItemStack stack, PoseStack pose, SubmitSink sink,
            int light, int overlay) {
        for (String part : new String[] {"Grip", "Cylinder", "Hammer", "Trigger"}) {
            submit(part, pose, sink, light, overlay);
        }
    }

    private void submit(String part, PoseStack pose, SubmitSink sink, int light, int overlay) {
        submitPart(sink, pose, body(), model(), part, light, overlay);
    }

    private RenderType body() { model(); return body; }
    private static double[] bus(String name) { return HbmAnimations.getRelevantTransformation(name); }
}
