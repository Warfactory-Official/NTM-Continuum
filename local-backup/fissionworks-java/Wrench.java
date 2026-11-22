package fissionworks;

import com.simibubi.create.AllItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One place that decides what counts as a wrench, so a wrong guess is a one-line fix. */
public final class Wrench {
    // Create 6.0.7 accepts "any wrench tagged with the wrenches tag"; the exact id is unconfirmed.
    private static final TagKey<Item> WRENCHES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    private Wrench() {}

    public static boolean isWrench(ItemStack stack) {
        return AllItems.WRENCH.isIn(stack) || stack.is(WRENCHES);
    }
}
