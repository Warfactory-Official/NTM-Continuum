package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.special.ItemHolotapeImage;
import com.hbm.items.tool.ItemLeadBox;
import com.hbm.items.tool.ItemToolBox;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code HazardTransformerRadiationContainer}, for the containers the port has: the toolbox passes its
 * contents' radiation through in full ({@code isContainer}), the containment box only as the
 * {@code BobMathUtil.squirt} of the total. Not covered here: storage crate items and the plastic bag, whose
 * carried contents the port stores differently.
 *
 * <p>Also 1.7 {@code HazardRegistry}'s one per-variant entry: the restored holotape carries 1 digamma. The
 * port's hazard map is keyed by item, so the variant is matched here instead.
 */
public class HazardTransformerRadiationContainer implements IHazardTransformer {

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) {
        if (stack.getItem() instanceof ItemHolotapeImage tape
                && tape.getType(stack) == ItemHolotapeImage.EnumHoloImage.HOLO_RESTORED) {
            entries.add(new HazardEntry(HazardRegistry.DIGAMMA, 1F));
        }

        boolean isBox = stack.getItem() instanceof ItemLeadBox;
        boolean isContainer = stack.getItem() instanceof ItemToolBox;
        if (!isBox && !isContainer) return;

        float radiation = 0;

        if (isContainer) {
            ItemStack[] items = ItemToolBox.readStacks(stack);
            if (items != null)
                for (ItemStack held : items)
                    if (held != null)
                        radiation += HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION) * held.getCount();
        }

        if (isBox) {
            ItemStack[] fromNBT = ItemLeadBox.readStacks(stack);
            if (fromNBT == null) return;
            for (ItemStack held : fromNBT) {
                if (held != null) {
                    radiation += HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION) * held.getCount();
                }
            }
            radiation = (float) squirt(radiation);
        }

        if (radiation > 0) entries.add(new HazardEntry(HazardRegistry.RADIATION, radiation));
    }

    /** 1.7 {@code BobMathUtil.squirt}. */
    private static double squirt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }
}
