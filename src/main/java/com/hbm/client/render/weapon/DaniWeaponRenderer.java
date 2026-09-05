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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Day And Night's two independently animated break-action revolvers. */
public final class DaniWeaponRenderer extends WeaponRenderer {

    private static final Identifier MODEL = Ids.of("hbm", "models/weapons/bio_revolver.obj");
    private WavefrontObj model;
    private RenderType celestial;
    private RenderType lunar;
    private ItemDisplayContext staticContext = ItemDisplayContext.GUI;

    /*
     * The item base has already applied the current hand's display transform before this renderer is
     * called. The old offhand copy was a second ordinary third-person item draw, so move that draw from
     * the current hand frame into the other hand frame. These are the two hand transforms represented by
     * gun_obj_dani_base.json, expressed in model space.
     */
    private static final RelativeTransform RIGHT_TO_LEFT = relativeHandTransform(
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
    private static final RelativeTransform LEFT_TO_RIGHT = relativeHandTransform(
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);

    private WavefrontObj model() {
        if (model == null) {
            model = WavefrontObj.load(MODEL);
            celestial = CompatRenderTypes.entityCutout(
                    Ids.of("hbm", "textures/models/weapons/dani_celestial.png"));
            lunar = CompatRenderTypes.entityCutout(
                    Ids.of("hbm", "textures/models/weapons/dani_lunar.png"));
        }
        return model;
    }

    @Override
    public org.joml.Vector3fc[] extents() {
        WavefrontObj obj = model();
        float[] min = obj.boundsMin();
        float[] max = obj.boundsMax();
        org.joml.Vector3fc[] corners = new org.joml.Vector3fc[8];
        int i = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            corners[i++] = new org.joml.Vector3f(x == 0 ? min[0] : max[0],
                    y == 0 ? min[1] : max[1], z == 0 ? min[2] : max[2]);
        }
        return corners;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
    }

    @Override
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    @Override
    public void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink sink, int light, int overlay) {
        model();
        float offset = 0.8F;
        for (int side = -1; side <= 1; side += 2) {
            int rail = side == -1 ? 0 : 1;
            pose.pushPose();
            standardAimingTransform(stack, pose, -1.5F * offset * side, -0.75F * offset, offset,
                    0, -3.125 / 8D, 0.25);
            pose.scale(0.125F, 0.125F, 0.125F);
            submitRevolver(pose, sink, rail == 0 ? celestial : lunar, rail, side, light, overlay);
            pose.popPose();
        }
    }

    @Override
    public void renderStatic(ItemStack stack, PoseStack pose, SubmitSink sink, int light, int overlay) {
        model();
        for (DaniStaticLayout.Draw draw : DaniStaticLayout.forContext(staticContext)) {
            pose.pushPose();
            RenderType texture = celestial;
            switch (draw) {
                case GUI_CELESTIAL -> {
                    // gun_obj_dani_base owns setupInv; these are renderInv's per-revolver calls.
                    pose.mulPose(Axis.ZP.rotationDegrees(225F));
                    pose.mulPose(Axis.YP.rotationDegrees(90F));
                    pose.mulPose(Axis.XP.rotationDegrees(25F));
                    pose.mulPose(Axis.YP.rotationDegrees(45F));
                    pose.translate(2, 0, 0);
                }
                case GUI_LUNAR -> {
                    texture = lunar;
                    pose.mulPose(Axis.ZN.rotationDegrees(225F));
                    pose.mulPose(Axis.YP.rotationDegrees(90F));
                    pose.mulPose(Axis.XP.rotationDegrees(25F));
                    pose.mulPose(Axis.YP.rotationDegrees(45F));
                    pose.mulPose(Axis.YP.rotationDegrees(180F));
                    pose.translate(2, 0, 0);
                }
                case LUNAR -> texture = lunar;
                case THIRD_PERSON_RIGHT_LUNAR -> {
                    texture = lunar;
                    if (staticContext != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                        applyHandTransform(pose, LEFT_TO_RIGHT);
                    }
                }
                case THIRD_PERSON_LEFT_LUNAR -> {
                    texture = lunar;
                    if (staticContext != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                        applyHandTransform(pose, RIGHT_TO_LEFT);
                    }
                }
                case CELESTIAL -> {}
            }
            submitAll(pose, sink, texture, light, overlay);
            pose.popPose();
        }
    }

    @Override
    public void setupStatic(PoseStack pose, ItemDisplayContext context) {
        staticContext = context;
    }

    private void submitRevolver(PoseStack pose, SubmitSink sink, RenderType body, int rail, int side,
            int light, int overlay) {
        double[] recoil = bus("RECOIL", rail);
        double[] move = bus("RELOAD_MOVE", rail);
        double[] rot = bus("RELOAD_ROT", rail);
        double[] equip = bus("EQUIP", rail);
        pose.translate(recoil[0], recoil[1], recoil[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 10)));
        pose.translate(0, -2, -2);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 2);
        pose.translate(move[0], move[1], move[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) rot[0]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (rot[2] * side)));
        pose.mulPose(Axis.YP.rotationDegrees((float) (rot[1] * side)));
        submitPart(sink, pose, body, model(), "Grip", light, overlay);

        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees((float) bus("FRONT", rail)[2]));
        submitPart(sink, pose, body, model(), "Barrel", light, overlay);
        pose.pushPose();
        pose.translate(0, 2.3125, -0.875);
        pose.mulPose(Axis.XP.rotationDegrees((float) bus("LATCH", rail)[2]));
        pose.translate(0, -2.3125, 0.875);
        submitPart(sink, pose, body, model(), "Latch", light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (bus("DRUM", rail)[2] * 60)));
        pose.translate(0, -1, bus("DRUM_PUSH", rail)[2]);
        submitPart(sink, pose, body, model(), "Drum", light, overlay);
        pose.popPose();
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, -4.5);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-45 + 45 * bus("HAMMER", rail)[2])));
        pose.translate(0, 0, 4.5);
        submitPart(sink, pose, body, model(), "Hammer", light, overlay);
        pose.popPose();
    }

    private void submitAll(PoseStack pose, SubmitSink sink, RenderType body, int light, int overlay) {
        for (String part : new String[] {"Grip", "Barrel", "Latch", "Drum", "Hammer"}) {
            submitPart(sink, pose, body, model(), part, light, overlay);
        }
    }

    private static double[] bus(String name, int rail) {
        return HbmAnimations.getRelevantTransformation(name, rail);
    }

    private record RelativeTransform(Vector3f translation, Quaternionf rotation) {}

    private static void applyHandTransform(PoseStack pose, RelativeTransform transform) {
        pose.translate(transform.translation().x(), transform.translation().y(), transform.translation().z());
        pose.mulPose(transform.rotation());
    }

    private static RelativeTransform relativeHandTransform(ItemDisplayContext from, ItemDisplayContext to) {
        Matrix4f relative = displayTransform(from).invert().mul(displayTransform(to));
        return new RelativeTransform(relative.getTranslation(new Vector3f()),
                relative.getUnnormalizedRotation(new Quaternionf()).normalize());
    }

    private static Matrix4f displayTransform(ItemDisplayContext context) {
        boolean right = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        return new Matrix4f()
                .translate((right ? -0.757072F : 8.662276F) / 16F,
                        (right ? 1.14817F : 3.115906F) / 16F,
                        (right ? -2.356844F : 0.150338F) / 16F)
                .rotateXYZ((float) Math.toRadians(right ? -179.493995F : -172.402674F),
                        (float) Math.toRadians(right ? -1.125273F : 29.330267F),
                        (float) Math.toRadians(right ? 179.74399F : 154.481445F))
                .scale(0.0703125F);
    }
}
