package com.hbm.items.gear;

import dev.golem.core.render.Worn;
import net.minecraft.world.item.ItemStack;

/**
 * Armour that hides parts of the wearer's own body.
 *
 * <p>A modelled suit occupies the same space the player's skin does, so without this a helmet's visor
 * fights with the hat layer and a chestplate's shoulders poke through the arms. 1.12.2 solved it by
 * toggling {@code ModelRenderer.isHidden} from {@code RenderPlayerEvent.Pre}/{@code .Post}
 * ({@code ModEventHandlerRenderer}); here the same toggling happens in Golem Core's
 * {@code PlayerParts}, and this interface is the item-side declaration it reads.
 *
 * <p>Ported 1:1 from {@code com.hbm.items.armor.IArmorDisableModel}, with one signature change: the old
 * method took the {@code EntityPlayer} so that a "hides only with the full set" suit could look the rest
 * of the armour up. It now takes the worn stacks directly, because from 26.1 the renderer is handed a
 * render state rather than an entity — and the four stacks are exactly what that state carries.
 */
public interface IArmorDisableModel {

    /** Whether wearing {@code stack} should hide {@code part}, given everything else that is worn. */
    boolean disablesPart(Worn worn, ItemStack stack, EnumPlayerPart part);

    /**
     * The parts of the vanilla humanoid model a suit may hide.
     *
     * <p>Names match 1.12.2's enum exactly. {@code HAT} is the headwear overlay — the outer skin layer —
     * and is the one a full helmet almost always wants gone; {@code HEAD} is the head itself.
     */
    enum EnumPlayerPart {
        BODY,
        HAT,
        HEAD,
        LEFT_ARM,
        LEFT_LEG,
        RIGHT_ARM,
        RIGHT_LEG
    }
}
