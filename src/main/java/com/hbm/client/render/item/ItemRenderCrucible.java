package com.hbm.client.render.item;

import com.hbm.client.crucible.CrucibleClient;
import com.hbm.client.crucible.CrucibleEquipModel;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * old-hbm {@code ItemRenderCrucible} (a 1.12 {@code TEISRBase}), replacing the 1.7 renderer by the user's choice.
 *
 * <p>The body runs in the {@code teisr} frame: {@code BakedModelTransforms.defaultItemTransforms()} and then the
 * builtin renderer's {@code translate(-0.5, -0.5, -0.5)}, which is where 1.12 handed a TEISR its pose.
 *
 * <ul>
 *   <li><b>First person</b>: the Collada scene with the player's arms ({@link CrucibleEquipModel}), posed by the
 *       deploy animation the server starts on equip and held on its last frame; the cutting swing ({@code SWING}),
 *       the random swing ({@code SWING_ROT}/{@code SWING_TRANS}) and the cut-angle tilt on top; an empty crucible sits
 *       400 ms into the deploy, lowered. Nothing is drawn until the deploy has arrived, as in 1.12. The blade is
 *       translucent, fullbright, clipped by a plane that slides along it between 60% and 70% of the deploy, with its
 *       bloom and heat-distortion passes, and the sparks and lightning of {@code firstPersonAuxParticles} in its space
 *       (see {@link CrucibleClient#renderFirstPerson}).</li>
 *   <li><b>Third person, head, ground, frame</b>: the OBJ crucible at 0.4, guards open and blade out (fullbright,
 *       unculled) whatever the charge.</li>
 *   <li><b>Inventory</b>: the OBJ crucible turned to lie along the slot's diagonal at 0.09.</li>
 * </ul>
 */
final class ItemRenderCrucible implements CustomItemRenderer {

    static final Identifier HILT = tex("textures/models/weapons/crucible_hilt.png");
    static final Identifier GUARD = tex("textures/models/weapons/crucible_guard.png");
    static final Identifier BLADE = tex("textures/models/weapons/crucible_blade.png");

    private final WavefrontObj crucible = obj("models/weapons/crucible.obj");

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return true;
    }

    @Override
    public boolean animated(ItemStack stack, Type type) {
        return type == Type.EQUIPPED_FIRST_PERSON;
    }

    /**
     * The display slot names the hand, which is the only reliable test: the hand renderer may hold a stale copy of
     * the stack (see below), so comparing instances let a crucible in both hands draw the whole scene twice.
     */
    @Override
    public void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, SubmitSink sink, int light,
                       int overlay, @Nullable LivingEntity owner) {
        boolean firstPerson = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        if (firstPerson && owner != null) {
            boolean mainHand = (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) == (owner.getMainArm() == HumanoidArm.RIGHT);
            draw(stack, Type.EQUIPPED_FIRST_PERSON, pose, sink, light, overlay, owner, !mainHand);
        } else {
            render(stack, Type.of(context), pose, sink, light, overlay, owner);
        }
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        // Without the slot (a caller that only knows 1.7's type), fall back to telling the hands apart by instance.
        boolean offHand = owner != null && owner.getOffhandItem() == stack && owner.getMainHandItem() != stack;
        draw(stack, type, pose, sink, light, overlay, owner, offHand);
    }

    private void draw(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                      @Nullable LivingEntity owner, boolean offHand) {
        pose.pushPose();
        switch (type) {
            case EQUIPPED_FIRST_PERSON -> {
                // old-hbm RenderSpecificHandEvent drew the crucible the player holds now (not the hand renderer's possibly
                // stale copy: with the hand pinned up vanilla never swaps in the stack whose charge changed) as the main
                // hand at equip and swing 0; old-hbm drew no off-hand crucible (its lookups were the main hand's), nor does this
                if (!offHand && owner == Minecraft.getInstance().player && owner.getMainHandItem().getItem() == stack.getItem()) {
                    CrucibleClient.renderFirstPerson(owner.getMainHandItem(), pose, sink, light, overlay);
                }
            }
            case EQUIPPED, ENTITY -> {
                pose.translate(0.5D, -0.3D, 0.5D);
                pose.scale(0.4F, 0.4F, 0.4F);
                part(pose, sink, crucible, culled(HILT), "Hilt", light, overlay);
                part(pose, sink, crucible, culled(GUARD), "GuardLeft", light, overlay);
                part(pose, sink, crucible, culled(GUARD), "GuardRight", light, overlay);
                pose.pushPose();
                // lighting and culling off, lightmap 240/240
                pose.translate(0.005D, 0D, 0D);
                sink.submitCustomGeometry(pose, dev.golem.core.render.CompatRenderTypes.flatCutout(BLADE),
                        (p, buffer) -> crucible.renderPart("Blade", p, buffer, FULLBRIGHT, overlay));
                pose.popPose();
            }
            case INVENTORY -> {
                pose.translate(0.15D, 0.15D, 0D);
                pose.mulPose(Axis.ZP.rotationDegrees(-135F + 90F));
                pose.mulPose(Axis.YP.rotationDegrees(90F));
                float scale = 0.09F;
                pose.scale(scale, scale, scale);
                part(pose, sink, crucible, culled(HILT), "Hilt", light, overlay);
                part(pose, sink, crucible, culled(GUARD), "GuardLeft", light, overlay);
                part(pose, sink, crucible, culled(GUARD), "GuardRight", light, overlay);
                pose.translate(0.005D, 0D, 0D);
                part(pose, sink, crucible, culled(BLADE), "Blade", light, overlay);
            }
        }
        pose.popPose();
    }
}
