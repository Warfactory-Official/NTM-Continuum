package com.hbm.inventory.recipes;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.TagIds;
import dev.golem.core.world.ComponentIngredient;
import dev.golem.core.world.CompatCustomRecipe;
import dev.golem.core.world.CustomRecipes;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code PowderRecipes}:61-65, the five shapeless "mix powders into foundry scraps" recipes
 * ({@code ItemScraps.create(new MaterialStack(MAT, INGOT.q(n)))}). They are ordinary shapeless recipes in 1.7; here
 * they are decided in Java only because a scraps stack is its material, amount <em>and</em> icon tint
 * ({@link ItemScraps#create}), which a declared result cannot state.
 */
public final class ScrapsPowderRecipe extends CompatCustomRecipe {
    public static final ScrapsPowderRecipe INSTANCE = new ScrapsPowderRecipe();
    public static final RecipeSerializer<ScrapsPowderRecipe> SERIALIZER = CustomRecipes.serializer(() -> INSTANCE);

    record Entry(Supplier<NTMMaterial> material, int ingots, List<ComponentIngredient> ingredients) {
        ItemStack result() {
            return ItemScraps.create(new Mats.MaterialStack(material.get(), MaterialShapes.INGOT.quantity * ingots));
        }
    }

    /** In 1.7 order; MINGRADE is the port's red copper. */
    static final List<Entry> ENTRIES = List.of(
            new Entry(() -> Mats.MAT_RED_COPPER, 2, ShapelessMatch.of(TagIds.dusts_copper, TagIds.dusts_redstone)),
            new Entry(() -> Mats.MAT_MAGTUNG, 1, ShapelessMatch.of(TagIds.dusts_tungsten, TagIds.nuggets_schrabidium)),
            new Entry(() -> Mats.MAT_TCALLOY, 1, ShapelessMatch.of(TagIds.dusts_steel, TagIds.nuggets_technetium)),
            new Entry(() -> Mats.MAT_STEEL, 1, ShapelessMatch.of(TagIds.dusts_iron, TagIds.dusts_coal)),
            new Entry(() -> Mats.MAT_STEEL, 4, ShapelessMatch.of(TagIds.dusts_iron, TagIds.dusts_iron,
                    TagIds.dusts_iron, TagIds.dusts_iron, TagIds.dusts_coal, TagIds.dusts_coal, TagIds.dusts_coal,
                    TagIds.dusts_coal)));

    private ScrapsPowderRecipe() {
        super("hbm", "scraps_from_powders");
    }

    private static Entry find(List<ItemStack> input) {
        for (Entry entry : ENTRIES) if (ShapelessMatch.matches(input, entry.ingredients())) return entry;
        return null;
    }

    @Override
    protected boolean matchesItems(List<ItemStack> input, Level level) {
        return find(input) != null;
    }

    @Override
    protected ItemStack assembleItems(List<ItemStack> input) {
        Entry entry = find(input);
        return entry == null ? ItemStack.EMPTY : entry.result();
    }

    @Override
    protected RecipeSerializer<ScrapsPowderRecipe> serializer() {
        return SERIALIZER;
    }
}
