package com.hbm.api.fluid;

import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * 1.7 {@code FluidTrait.addInfo} / {@code addInfoHidden} for a trait another mod adds to HBM's fluids
 * ({@link com.hbm.inventory.fluid.FluidProperties#registerTrait registerTrait} or
 * {@link com.hbm.inventory.fluid.FluidProperties#registerFlag registerFlag}): a trait object that implements this
 * contributes its lines to every fluid tooltip HBM draws ({@code FluidType.addInfo}), after HBM's own traits in 1.7's
 * order, in the order the traits were registered. Hidden lines show while Shift is held; any hidden line makes the
 * tooltip end with 1.7's "Hold &lt;LSHIFT&gt;" hint when it is not.
 */
public interface TooltipTrait {

    /** Lines always shown. */
    default void addInfo(List<Component> info) {}

    /** Lines shown while Shift is held. */
    default void addInfoHidden(List<Component> info) {}
}
