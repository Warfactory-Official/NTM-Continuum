package com.hbm.client.render.armor;

import com.hbm.items.gear.IArmorDisableModel;
import com.hbm.items.gear.IArmorDisableModel.EnumPlayerPart;
import dev.golem.core.render.Worn;
import dev.golem.core.world.ArmorSlot;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
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
    private static final Map<HumanoidModel<?>, EnumMap<EnumPlayerPart, Boolean>> HIDDEN =
            new WeakHashMap<>();

    private static final EnumPlayerPart[] PARTS = EnumPlayerPart.values();
    private static final ArmorSlot[] SLOTS = ArmorSlot.values();

    /** Restores anything hidden last frame, then hides what this frame's armour asks for. */
    public static void apply(HumanoidModel<?> model, ItemStack head, ItemStack chest, ItemStack legs,
            ItemStack feet) {

        EnumMap<EnumPlayerPart, Boolean> previous = HIDDEN.remove(model);
        if (previous != null) {
            for (Map.Entry<EnumPlayerPart, Boolean> entry : previous.entrySet()) {
                ModelPart part = part(model, entry.getKey());
                if (part != null) part.visible = entry.getValue();
            }
        }

        Worn worn = new Worn(head, chest, legs, feet);
        if (worn.isEmpty()) return;

        EnumMap<EnumPlayerPart, Boolean> hidden = null;

        for (ArmorSlot slot : SLOTS) {
            ItemStack stack = worn.get(slot);
            if (!(stack.getItem() instanceof IArmorDisableModel armor)) continue;

            for (EnumPlayerPart which : PARTS) {
                if (!armor.disablesPart(worn, stack, which)) continue;

                ModelPart part = part(model, which);
                if (part == null || !part.visible) continue;

                if (hidden == null) hidden = new EnumMap<>(EnumPlayerPart.class);
                hidden.put(which, part.visible);
                part.visible = false;
            }
        }

        if (hidden != null) HIDDEN.put(model, hidden);
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
