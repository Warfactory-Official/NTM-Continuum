package com.hbm.client.render.item;

import com.hbm.client.anim.HbmAnimations;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.ItemCrucible;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.client.EquipProgress;
import dev.golem.core.render.SubmitSink;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.7 {@code ItemRenderCrucible}: hilt, two guards hinged at {@code y = 3} and the glowing blade. A charged
 * crucible has its guards open and its blade out, fullbright and unculled; an empty one folds the guards shut
 * and draws no blade. The inventory icon is the model, with the blade lit like the rest.
 *
 * <p>First person reads the {@code SWING_*} and {@code GUARD_ROT} buses exactly as 1.7 did; the server starts them
 * ({@code ItemCrucible.onEquip}/{@code compatOnEntitySwing} -> {@code ToolAnimations}). As in 1.7 the renderer clears
 * the vanilla arm swing every frame and pins the hand's equip progress (Core {@code EquipProgress}, 1.7 reflected
 * into {@code ItemRenderer}): a hand being lowered snaps to 0, one being raised to 1, and the blade only shows once
 * both ends read 1. The one modern addition: since 1.9 vanilla also keeps the hand low while the attack cooldown
 * recovers - after every hit and after every item switch - with the crucible already the item shown. 1.7 had no
 * cooldown, so while the crucible is the item shown the progress is held at 1; the snap to 0 applies only while the
 * renderer is still showing another item (the put-away), as 1.7's did.
 */
final class ItemRenderCrucible implements CustomItemRenderer {

    private static final Identifier HILT = tex("textures/models/weapons/crucible_hilt.png");
    private static final Identifier GUARD = tex("textures/models/weapons/crucible_guard.png");
    private static final Identifier BLADE = tex("textures/models/weapons/crucible_blade.png");

    private final WavefrontObj crucible = obj("models/weapons/crucible.obj");

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return true;
    }

    @Override
    public boolean animated(ItemStack stack, Type type) {
        return type == Type.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        // 1.7: item damage below the maximum
        boolean isOn = ItemCrucible.spent(stack) < ItemCrucible.MAX_SPENT;
        boolean blocking = owner != null && owner.isBlocking();

        pose.pushPose();
        switch (type) {
            case EQUIPPED_FIRST_PERSON -> {
                pose.translate(1.5D, -0.3D, 0D);
                if (blocking) pose.translate(-0.125D, -0.25D, 0D);

                net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null) player.swinging = false;

                float prevEq = EquipProgress.previousMainHand();
                float eq = EquipProgress.mainHand();
                if (!EquipProgress.mainHandSwitching()) {
                    // the crucible is the item shown: 1.7 had it fully up (see the class comment on the cooldown)
                    EquipProgress.pinMainHand(1.0F, 1.0F);
                } else if (eq < prevEq) {
                    EquipProgress.pinMainHand(0.0F, 0.0F);
                } else if (eq > prevEq) {
                    EquipProgress.pinMainHand(1.0F, 1.0F);
                }

                pose.scale(0.3F, 0.3F, 0.3F);
                pose.mulPose(Axis.ZP.rotationDegrees(45F));
                pose.mulPose(Axis.YP.rotationDegrees(90F));

                boolean isSwing = false;
                if (!blocking) {
                    double[] sRot = HbmAnimations.getRelevantTransformation("SWING_ROT");
                    double[] sTrans = HbmAnimations.getRelevantTransformation("SWING_TRANS");
                    pose.translate(sTrans[0], sTrans[1], sTrans[2]);
                    pose.mulPose(Axis.XP.rotationDegrees((float) sRot[0]));
                    pose.mulPose(Axis.ZP.rotationDegrees((float) sRot[2]));
                    pose.mulPose(Axis.YP.rotationDegrees((float) sRot[1]));
                    if (sRot[0] != 0) isSwing = true;
                }

                double[] rot = HbmAnimations.getRelevantTransformation("GUARD_ROT");
                part(pose, sink, crucible, culled(HILT), "Hilt", light, overlay);
                double rotGuard = rot[0];
                if (!isSwing && !isOn) rotGuard = 90;
                guards(pose, sink, (float) rotGuard, light, overlay);

                if (eq == 1.0F && prevEq == 1.0F && rot[2] == 0 && (isSwing || isOn)) blade(pose, sink, overlay);
            }
            case ENTITY, EQUIPPED -> {
                if (type == Type.ENTITY) {
                    pose.translate(-0.75D, 0.6D, 0D);
                    pose.mulPose(Axis.ZP.rotationDegrees(-45F));
                }
                pose.mulPose(Axis.ZP.rotationDegrees(45F));
                pose.translate(0.75D, -0.4D, 0D);
                pose.mulPose(Axis.YP.rotationDegrees(90F));
                pose.scale(0.15F, 0.15F, 0.15F);
                part(pose, sink, crucible, culled(HILT), "Hilt", light, overlay);
                guards(pose, sink, isOn ? 0F : 90F, light, overlay);
                if (isOn) blade(pose, sink, overlay);
            }
            case INVENTORY -> {
                pose.translate(2D, 14D, 0D);
                pose.mulPose(Axis.ZP.rotationDegrees(-135F));
                pose.mulPose(Axis.YP.rotationDegrees(90F));
                float scale = 1.5F;
                pose.scale(scale, scale, scale);
                part(pose, sink, crucible, culled(HILT), "Hilt", light, overlay);
                guards(pose, sink, isOn ? 0F : 90F, light, overlay);
                pose.translate(0.005D, 0D, 0D);
                if (isOn) part(pose, sink, crucible, culled(BLADE), "Blade", light, overlay);
            }
        }
        pose.popPose();
    }

    /** Left guard about {@code -X}, right about {@code +X}, both hinged at {@code (0, 3, ±0.5)}. */
    private void guards(PoseStack pose, SubmitSink sink, float angle, int light, int overlay) {
        pose.pushPose();
        pose.translate(0D, 3D, 0.5D);
        pose.mulPose(Axis.XP.rotationDegrees(-angle));
        pose.translate(0D, -3D, -0.5D);
        part(pose, sink, crucible, culled(GUARD), "GuardLeft", light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0D, 3D, -0.5D);
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        pose.translate(0D, -3D, 0.5D);
        part(pose, sink, crucible, culled(GUARD), "GuardRight", light, overlay);
        pose.popPose();
    }

    /** The lit blade: lighting and culling off, lightmap at 240/240, nudged 0.005 out of the guards. */
    private void blade(PoseStack pose, SubmitSink sink, int overlay) {
        pose.pushPose();
        pose.translate(0.005D, 0D, 0D);
        sink.submitCustomGeometry(pose, dev.golem.core.render.CompatRenderTypes.flatCutout(BLADE),
                (p, buffer) -> crucible.renderPart("Blade", p, buffer, FULLBRIGHT, overlay));
        pose.popPose();
    }
}
