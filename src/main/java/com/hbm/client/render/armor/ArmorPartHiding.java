package com.hbm.client.render.armor;

import com.hbm.items.armor.IArmorDisableModel;
import com.hbm.items.armor.IArmorDisableModel.EnumPlayerPart;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.Worn;
import dev.golem.core.world.ArmorSlot;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Hides the wearer's own body parts underneath a modelled suit, from 1.12.2's
 * {@code ModEventHandlerRenderer.onRenderPlayerPre}/{@code Post} pair.
 *
 * <p>Upstream toggled {@code ModelRenderer.isHidden} from {@code RenderPlayerEvent}, restoring it after.
 * The restore is the fiddly half and is done here too, but from a recorded <em>previous value</em> rather
 * than by forcing {@code true}: a player who has turned their hat layer off in the skin options must
 * keep it off when the helmet comes back off.
 *
 * <p>Driven by {@code MixinHumanoidModel} at the tail of {@code setupAnim}, which is the one point that
 * runs every frame, before any geometry is submitted, with the model in hand; and, for players,
 * <em>after</em> {@code PlayerModel.setupAnim} has re-applied the skin-customisation flags.
 */
public final class ArmorPartHiding {

    private ArmorPartHiding() {}

    /**
     * Which parts this class turned off for each model, and what their flag was beforehand.
     *
     * <p>Weakly keyed: models belong to renderers that are rebuilt on resource reload, and a strong
     * reference here would pin every one of them for the session.
     */
    private static final Map<HumanoidModel<?>, Map<ModelPart, Boolean>> HIDDEN = new WeakHashMap<>();

    private static final EnumPlayerPart[] PARTS = EnumPlayerPart.values();
    private static final ArmorSlot[] SLOTS = ArmorSlot.values();

    /** Restores last frame's hiding, then hides what the armour asks for, or everything for an Astolfized player. */
    public static void apply(HumanoidModel<?> model, Player player, ItemStack head, ItemStack chest,
            ItemStack legs, ItemStack feet) {

        Map<ModelPart, Boolean> previous = HIDDEN.remove(model);
        if (previous != null) {
            for (Map.Entry<ModelPart, Boolean> entry : previous.entrySet()) entry.getKey().visible = entry.getValue();
        }

        Map<ModelPart, Boolean> hidden = new IdentityHashMap<>();
        if (ModelMan.isManly(player) && ModelMan.isSkinModel(model)) {
            for (EnumPlayerPart which : PARTS) hide(part(model, which), hidden);
            for (ModelPart layer : HumanoidPose.skinLayers(model)) hide(layer, hidden);
        } else {
            Worn worn = new Worn(head, chest, legs, feet);
            if (worn.isEmpty()) return;

            for (ArmorSlot slot : SLOTS) {
                ItemStack stack = worn.get(slot);
                if (!(stack.getItem() instanceof IArmorDisableModel armor)) continue;

                for (EnumPlayerPart which : PARTS) {
                    if (armor.disablesPart(worn, stack, which)) hide(part(model, which), hidden);
                }
            }
        }

        if (!hidden.isEmpty()) HIDDEN.put(model, hidden);
    }

    private static void hide(ModelPart part, Map<ModelPart, Boolean> hidden) {
        if (!part.visible) return;
        hidden.put(part, true);
        part.visible = false;
    }

    /** 1.12.2's {@code getBoxFromType}, against the modern part names. */
    private static ModelPart part(HumanoidModel<?> model, EnumPlayerPart part) {
        return switch (part) {
            case BODY -> model.body;
            case HAT -> model.hat;
            case HEAD -> model.head;
            case LEFT_ARM -> model.leftArm;
            case LEFT_LEG -> model.leftLeg;
            case RIGHT_ARM -> model.rightArm;
            case RIGHT_LEG -> model.rightLeg;
        };
    }
}
