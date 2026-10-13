package com.hbm.mixin;

import com.hbm.client.render.armor.ArmorPartHiding;
import net.minecraft.client.model.HumanoidModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
//?} else {
/*import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
*///?}

/**
 * Hides the wearer's own body parts under a modelled HBM suit.
 *
 * <p><b>Why a mixin</b> (AGENTS.md §7 requires this to be written down). Rungs 1–3 of the ladder do not
 * reach:
 *
 * <ul>
 * <li><i>Vanilla API</i>: nothing exposes a hook between "the model has been posed" and "the model has
 *     been submitted", which is the only window in which a visibility flag has any effect. A render
 *     layer is too late: it runs after the body, and below 26.1 the body has already been drawn.
 * <li><i>Loader API</i>: NeoForge has {@code RenderPlayerEvent.Pre}/{@code Post}, which is what 1.12
 *     used, but fabric-api has no player-render event at all. Using the NeoForge one would put the same
 *     feature on three of six nodes.
 * <li><i>Access widener</i>: buys nothing; {@code ModelPart.visible} is already public. The obstacle is
 *     timing, not visibility.
 * </ul>
 *
 * <p>It is the smallest mixin that works: one {@code @Inject} at {@code TAIL}, no cancellation, no
 * redirect. {@code HumanoidModel.setupAnim} is chosen because it is the last thing to touch the model
 * before submission, and (for players) it runs <i>after</i> {@code PlayerModel.setupAnim} has re-applied
 * the skin-customisation flags each frame, so restoring never fights the player's own settings.
 *
 * <p>The two branches exist because the method's descriptor changed: 26.1 poses from a render state,
 * older versions from the live entity and six floats. A mixin injector's parameters must match its
 * target exactly, which is the one era-guard category AGENTS.md §1 accepts inside HBM.
 */
@Mixin(HumanoidModel.class)
public class MixinHumanoidModel {

    //? if >=26.1 {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void hbm$hideParts(HumanoidRenderState state, CallbackInfo ci) {
        ArmorPartHiding.apply((HumanoidModel<?>) (Object) this, state.headEquipment,
                state.chestEquipment, state.legsEquipment, state.feetEquipment);
    }
    //?} else {
    /*@Inject(method = "setupAnim", at = @At("TAIL"))
    private void hbm$hideParts(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ArmorPartHiding.apply((HumanoidModel<?>) (Object) this,
                entity.getItemBySlot(EquipmentSlot.HEAD), entity.getItemBySlot(EquipmentSlot.CHEST),
                entity.getItemBySlot(EquipmentSlot.LEGS), entity.getItemBySlot(EquipmentSlot.FEET));
    }
    *///?}
}
