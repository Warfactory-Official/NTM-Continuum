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

/** The unmodified G3 renderer; optional weapon-mod geometry is intentionally not claimed. */
public final class G3WeaponRenderer extends WeaponRenderer {

    private WavefrontObj model;
    private RenderType body;

    private WavefrontObj model() {
        if (model == null) {
            model = WavefrontObj.load(Ids.of("hbm", "models/weapons/g3.obj"));
            body = CompatRenderTypes.entityCutout(Ids.of("hbm", "textures/models/weapons/g3.png"));
        }
        return model;
    }

    private RenderType body() { model(); return body; }

    @Override public org.joml.Vector3fc[] extents() {
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
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.25F * offset, -offset, 2.75F * offset,
                0, -3.5625 / 8D, 1.75);
    }

    @Override public void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink sink,
            int light, int overlay) {
        pose.scale(0.375F, 0.375F, 0.375F);
        double[] equip = bus("EQUIP"), lift = bus("LIFT"), recoil = bus("RECOIL");
        double[] mag = bus("MAG"), speen = bus("SPEEN"), bolt = bus("BOLT");
        double[] plug = bus("PLUG"), handle = bus("HANDLE"), bullet = bus("BULLET");
        pose.translate(0, -2, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 6);
        pose.translate(0, 0, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 0, 4 + recoil[2]);

        submit("Rifle", pose, sink, light, overlay);
        submit("Stock", pose, sink, light, overlay);
        submit("Flash_Hider", pose, sink, light, overlay);
        submit("Trigger", pose, sink, light, overlay);
        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        pose.translate(0, -1.75, -0.5);
        pose.mulPose(Axis.ZP.rotationDegrees((float) speen[2]));
        pose.mulPose(Axis.YP.rotationDegrees((float) speen[1]));
        pose.translate(0, 1.75, 0.5);
        submit("Magazine", pose, sink, light, overlay);
        if (bullet[0] == 0) submit("Bullet", pose, sink, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, 0, bolt[2]);
        submit("Guide_And_Bolt", pose, sink, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, 0.625, plug[2]);
        pose.mulPose(Axis.ZP.rotationDegrees((float) handle[2]));
        pose.translate(0, -0.625, 0);
        submit("Plug", pose, sink, light, overlay);
        pose.translate(0, 0.625, 5.25);
        pose.mulPose(Axis.ZP.rotationDegrees(22.5F));
        pose.mulPose(Axis.YP.rotationDegrees((float) handle[1]));
        pose.mulPose(Axis.ZN.rotationDegrees(22.5F));
        pose.translate(0, -0.625, -5.25);
        submit("Handle", pose, sink, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, -0.875, -3.5);
        pose.mulPose(Axis.XP.rotationDegrees(-30F * (1 - ItemGunBaseNT.getMode(stack, 0))));
        pose.translate(0, 0.875, 3.5);
        submit("Selector", pose, sink, light, overlay);
        pose.popPose();
    }

    @Override public void renderStatic(ItemStack stack, PoseStack pose, SubmitSink sink,
            int light, int overlay) {
        for (String part : G3StaticLayout.PARTS) {
            submit(part, pose, sink, light, overlay);
        }
        pose.pushPose();
        pose.translate(0, -0.875, -3.5);
        pose.mulPose(Axis.XN.rotationDegrees(30F));
        pose.translate(0, 0.875, 3.5);
        submit("Selector", pose, sink, light, overlay);
        pose.popPose();
    }

    private void submit(String part, PoseStack pose, SubmitSink sink, int light, int overlay) {
        submitPart(sink, pose, body(), model(), part, light, overlay);
    }

    private static double[] bus(String name) { return HbmAnimations.getRelevantTransformation(name); }
}
