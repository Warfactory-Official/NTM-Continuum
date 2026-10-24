package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.special.ItemHolotapeImage;
import com.hbm.items.tool.ItemLeadBox;
import com.hbm.items.tool.ItemToolBox;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import dev.golem.core.world.ItemStacks;

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
    public boolean readsLiveStorage() { return true; }

    @Override
    public boolean readsLiveStorage(ItemStack stack) {
        return stack.getItem() instanceof ItemLeadBox || stack.getItem() instanceof ItemToolBox
                || dev.golem.core.world.Compat.containerItemCount(stack, 256) > 0;
    }

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) {
        if (stack.getItem() instanceof ItemHolotapeImage tape
                && tape.getType(stack) == ItemHolotapeImage.EnumHoloImage.HOLO_RESTORED) {
            entries.add(new HazardEntry(HazardRegistry.DIGAMMA, 1F));
        }

        boolean isBox = stack.getItem() instanceof ItemLeadBox;
        boolean isContainer = stack.getItem() instanceof ItemToolBox;
        float radiation = 0;

        //? if >=1.20.5 {
        var contents = stack.get(net.minecraft.core.component.DataComponents.CONTAINER);
        if (contents != null) {
            //? if >=26.1 {
            for (var held : contents.nonEmptyItemCopyStream().toList()) {
            //?} else {
            /*for (var held : contents.nonEmptyStream().toList()) {
            *///?}
                radiation += HazardSystem.getRawRadsFromStack(held) * held.getCount();
            }
        }
        //?} else {
        /*for (var held : ItemStacks.carriedItems(stack, 256)) {
            radiation += HazardSystem.getRawRadsFromStack(held) * held.getCount();
        }
        *///?}

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
