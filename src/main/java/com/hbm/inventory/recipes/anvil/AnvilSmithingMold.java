package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.machine.ModMoldItems;
import com.hbm.items.special.ItemAutogen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The mold smithing recipe (1.12.2 {@code AnvilSmithingMold}): left = an <b>example item of a shape</b>
 * (any ingot, any plate, ...), right = a blank {@code mold_base} → the matching mold. The example item
 * is NOT consumed (only the blank is). Shape matching moves from the ore dictionary to the item's
 * {@code c:<plural>/<material>} tags (or the autogen item's own shape); alternatively an explicit
 * item list matches fixed-output molds (blades, stamps, steel pipes).
 */
public class AnvilSmithingMold extends AnvilSmithingRecipe {

    private final @Nullable MaterialShapes shape;
    private final int count;
    private final Item @Nullable [] matchesItems;

    /** Shape-matched mold (e.g. any {@code count}x ingot + blank → the ingot mold). */
    public AnvilSmithingMold(ItemStack mold, AStack demo, MaterialShapes shape, int count) {
        super(1, mold, demo, new RecipesCommon.ComparableStack(
                ModMoldItems.mold_base.get()));
        this.shape = shape;
        this.count = count;
        this.matchesItems = null;
    }

    /** Explicit-item mold (e.g. steel shredder blades + blank → the blades mold). */
    public AnvilSmithingMold(ItemStack mold, AStack demo, Item... matches) {
        super(1, mold, demo, new RecipesCommon.ComparableStack(
                ModMoldItems.mold_base.get()));
        this.shape = null;
        this.count = 1;
        this.matchesItems = matches;
    }

    @Override
    public int matchesInt(ItemStack left, ItemStack right) {
        if (!doesStackMatch(right, this.right)) return -1;

        if (shape != null && left.getCount() == count && itemHasShape(left, shape)) return 0;

        if (matchesItems != null) {
            for (Item item : matchesItems) {
                if (left.is(item) && left.getCount() == count) return 0;
            }
        }

        return -1;
    }

    /** Whether an item is that shape of ANY material (autogen field or a {@code c:<plural>/*} tag). */
    private static boolean itemHasShape(ItemStack stack, MaterialShapes shape) {
        if (stack.getItem() instanceof ItemAutogen autogen) return autogen.shape == shape;
        return stack.getItem().builtInRegistryHolder().tags().anyMatch(tag -> {
            Identifier id = tag.location();
            if (!"c".equals(id.getNamespace())) return false;
            String path = id.getPath();
            int slash = path.indexOf('/');
            return slash > 0 && path.substring(0, slash).equals(shape.tagPlural);
        });
    }

    /** The shape example (index 0) is not consumed; only the blank mold (index 1) is. */
    @Override
    public int amountConsumed(int index, boolean mirrored) {
        return index;
    }
}
