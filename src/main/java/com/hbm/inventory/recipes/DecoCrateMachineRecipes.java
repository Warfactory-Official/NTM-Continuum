package com.hbm.inventory.recipes;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockCap;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.block.ItemBlockDecoVariant;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The machine recipes of the crate and deco blocks, each handed to its registry from one line in that registry's own
 * registration: 1.7 {@code AssemblyMachineRecipes} "ass.cap*" (two stacks of 64 caps into a cap block),
 * {@code AnvilRecipes} recycling of the FM radio and the filing cabinet, and the bobblehead rule of
 * {@code ShredderRecipes} (each figure shreds to its own {@code scrap_plastic_<type>}, see {@link #bobbleScrap}; the
 * tungsten crate and plain bobblehead rows are files of the shredder table, M2 R-C1).
 */
public final class DecoCrateMachineRecipes {

    private DecoCrateMachineRecipes() {}

    private static ItemStack stack(String id, int count) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.of("hbm", id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    /** 1.7 AssemblyMachineRecipes:237-248. */
    public static void assembly(GenericRecipes recipes) {
        for (BlockCap.EnumCapBlock type : BlockCap.EnumCapBlock.values()) {
            String name = type.name().toLowerCase(Locale.US);
            ItemStack cap = stack("cap_" + name, 1);
            if (cap.isEmpty() || recipes.getRecipe("ass.cap" + name) != null) continue;
            recipes.registerGenerated(new GenericRecipe("ass.cap" + name).setup(10, 100)
                    .outputItems(new ItemStack(ModDecoCrateBlocks.CAP_BLOCKS.get(type).get(), 1))
                    .inputItems(new ComparableStack(cap.getItem(), 64), new ComparableStack(cap.getItem(), 64)));
        }
    }

    /** 1.7 ShredderRecipes:338-341: a bobblehead gives the plastic scrap of its {@code BobbleType.scrap}; else null. */
    public static ItemStack bobbleScrap(ItemStack stack) {
        BobbleScrap result = bobbleScrapAndOrder(stack);
        return result == null ? null : result.scrap();
    }

    /** An addon bobblehead item's shredder scrap suffix and PIN order. */
    public record BobbleExtra(String scrap, int order) {}

    /** A resolved bobblehead shredder row: its scrap and its PIN order. */
    public record BobbleScrap(ItemStack scrap, int order) {}

    /** Base heads whose shredder order follows Space's PIN order rather than NTM's enum order. */
    private static final Map<BlockBobble.BobbleType, Integer> BOBBLE_PIN_ORDERS = new LinkedHashMap<>();

    /** Addon bobblehead items (item supplier: registration runs before the registries flush). */
    private static final Map<Supplier<Item>, BobbleExtra> EXTRA_BOBBLES = new LinkedHashMap<>();

    /** Gives a base head its Space PIN order for the shredder rule (NTM's own order stays the enum's). */
    public static void orderBobble(BlockBobble.BobbleType type, int order) {
        BOBBLE_PIN_ORDERS.put(type, order);
    }

    /** Gives an addon's bobblehead item its shredder scrap and PIN order. */
    public static void addBobble(Supplier<Item> item, String scrap, int order) {
        EXTRA_BOBBLES.put(item, new BobbleExtra(scrap, order));
    }

    /** 1.7 ShredderRecipes:338-341 with the row's PIN order: base heads, then addon-registered items. */
    public static BobbleScrap bobbleScrapAndOrder(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof ItemBlockDecoVariant item
                && item.getBlock() == ModDecoCrateBlocks.bobblehead.get()) {
            BlockBobble.BobbleType type =
                    BlockBobble.BobbleType.of(item.variant(stack));
            ItemStack scrap = stack("scrap_plastic_" + type.scrap.toLowerCase(Locale.US), 1);
            if (scrap.isEmpty()) return null;
            return new BobbleScrap(scrap, BOBBLE_PIN_ORDERS.getOrDefault(type, Integer.MAX_VALUE));
        }
        for (var entry : EXTRA_BOBBLES.entrySet()) {
            if (stack.is(entry.getKey().get())) {
                ItemStack scrap = stack("scrap_plastic_" + entry.getValue().scrap().toLowerCase(Locale.US), 1);
                if (scrap.isEmpty()) return null;
                return new BobbleScrap(scrap, entry.getValue().order());
            }
        }
        return null;
    }
}
