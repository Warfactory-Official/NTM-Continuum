package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.ModItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ItemEnums.EnumChunkType;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The crystal-to-molten-material electrolyser table from 1.12.2. */
public final class ElectrolyserMetalRecipes {

    public static final List<ElectrolysisMetalRecipe> RECIPES = new ArrayList<>();
    private static final Map<net.minecraft.world.item.Item, ElectrolysisMetalRecipe> BY_ITEM = new LinkedHashMap<>();
    private static boolean initialized;

    private ElectrolyserMetalRecipes() {}

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        add(ModItems.crystal_iron.get(), out(Mats.MAT_IRON, MaterialShapes.INGOT, 6), out(Mats.MAT_TITANIUM, MaterialShapes.INGOT, 2),
                stack(ModItems.powder_lithium_tiny, 3));
        add(ModItems.crystal_gold.get(), out(Mats.MAT_GOLD, MaterialShapes.INGOT, 6), out(Mats.MAT_LEAD, MaterialShapes.INGOT, 2),
                stack(ModItems.powder_lithium_tiny, 3), stack(ModItems.nugget_mercury, 18));
        add(ModItems.crystal_uranium.get(), out(Mats.MAT_URANIUM, MaterialShapes.INGOT, 6), out(Mats.MAT_RADIUM, MaterialShapes.NUGGET, 4),
                stack(ModItems.powder_lithium_tiny, 3));
        add(ModItems.crystal_thorium.get(), out(Mats.MAT_THORIUM, MaterialShapes.INGOT, 6), out(Mats.MAT_URANIUM, MaterialShapes.INGOT, 2),
                stack(ModItems.powder_lithium_tiny, 3));
        add(ModItems.crystal_plutonium.get(), out(Mats.MAT_PLUTONIUM, MaterialShapes.INGOT, 6), out(Mats.MAT_POLONIUM, MaterialShapes.INGOT, 2),
                stack(ModItems.powder_lithium_tiny, 3));
        add(ModItems.crystal_titanium.get(), out(Mats.MAT_TITANIUM, MaterialShapes.INGOT, 6), out(Mats.MAT_IRON, MaterialShapes.INGOT, 2),
                stack(ModItems.powder_lithium_tiny, 3));
        add(ModItems.crystal_copper.get(), out(Mats.MAT_COPPER, MaterialShapes.INGOT, 6), out(Mats.MAT_LEAD, MaterialShapes.NUGGET, 4),
                stack(ModItems.powder_lithium_tiny, 3), stack(ModItems.sulfur, 2));
        add(ModItems.crystal_tungsten.get(), out(Mats.MAT_TUNGSTEN, MaterialShapes.INGOT, 6), out(Mats.MAT_IRON, MaterialShapes.INGOT, 2),
                stack(ModItems.powder_lithium_tiny, 3));
        add(ModItems.crystal_aluminium.get(), out(Mats.MAT_ALUMINIUM, MaterialShapes.INGOT, 2), out(Mats.MAT_IRON, MaterialShapes.INGOT, 2),
                ModIntermediateItems.chunk(EnumChunkType.CRYOLITE, 4), stack(ModItems.powder_lithium_tiny, 3));
        add(ModItems.crystal_beryllium.get(), out(Mats.MAT_BERYLLIUM, MaterialShapes.INGOT, 6), out(Mats.MAT_LEAD, MaterialShapes.NUGGET, 4),
                stack(ModItems.powder_lithium_tiny, 3), stack(ModItems.powder_quartz, 2));
        add(ModItems.crystal_lead.get(), out(Mats.MAT_LEAD, MaterialShapes.INGOT, 6), out(Mats.MAT_GOLD, MaterialShapes.INGOT, 2),
                stack(ModItems.powder_lithium_tiny, 3));
        add(ModItems.crystal_schraranium.get(), out(Mats.MAT_SCHRABIDIUM, MaterialShapes.NUGGET, 5), out(Mats.MAT_URANIUM, MaterialShapes.NUGGET, 2),
                stack(ModItems.nugget_neptunium, 2));
        add(ModItems.crystal_schrabidium.get(), out(Mats.MAT_SCHRABIDIUM, MaterialShapes.INGOT, 6), out(Mats.MAT_PLUTONIUM, MaterialShapes.INGOT, 2),
                stack(ModItems.powder_lithium_tiny, 3));
        add(ModItems.crystal_rare.get(), out(Mats.MAT_ZIRCONIUM, MaterialShapes.NUGGET, 6), out(Mats.MAT_BORON, MaterialShapes.NUGGET, 2),
                stack(ModItems.powder_desh_mix, 3));
        add(ModItems.crystal_trixite.get(), out(Mats.MAT_PLUTONIUM, MaterialShapes.INGOT, 3), out(Mats.MAT_COBALT, MaterialShapes.INGOT, 4),
                stack(ModItems.powder_niobium, 4), stack(ModItems.powder_nitan_mix, 2));
        add(ModItems.crystal_lithium.get(), out(Mats.MAT_LITHIUM, MaterialShapes.INGOT, 6), out(Mats.MAT_BORON, MaterialShapes.INGOT, 2),
                stack(ModItems.powder_quartz, 2), stack(ModItems.fluorite, 2));
        add(ModItems.crystal_starmetal.get(), out(Mats.MAT_DURA, MaterialShapes.INGOT, 4), out(Mats.MAT_COBALT, MaterialShapes.INGOT, 4),
                stack(ModItems.powder_astatine, 3), stack(ModItems.nugget_mercury, 72));
        add(ModItems.crystal_cobalt.get(), out(Mats.MAT_COBALT, MaterialShapes.INGOT, 3), out(Mats.MAT_IRON, MaterialShapes.INGOT, 4),
                stack(ModItems.powder_copper, 4), stack(ModItems.powder_lithium_tiny, 3));
        BedrockOreChainRecipes.electrolyser(ElectrolyserMetalRecipes::add); // weighted primaries and crumbs, 20 ticks
    }

    public static ElectrolysisMetalRecipe getRecipe(ItemStack stack) {
        register();
        return stack.isEmpty() ? null : BY_ITEM.get(stack.getItem());
    }

    private static void add(net.minecraft.world.item.Item input, Mats.MaterialStack output1, Mats.MaterialStack output2,
                            ItemStack... byproduct) {
        add(input, output1, output2, 600, byproduct);
    }

    /** 1.7's second constructor, with an explicit duration (the bedrock ore grades use 20). */
    private static void add(net.minecraft.world.item.Item input, Mats.MaterialStack output1, Mats.MaterialStack output2,
                            int duration, ItemStack... byproduct) {
        ElectrolysisMetalRecipe recipe = new ElectrolysisMetalRecipe(new ComparableStack(input), output1, output2, duration, byproduct);
        RECIPES.add(recipe);
        BY_ITEM.put(input, recipe);
    }

    private static Mats.MaterialStack out(com.hbm.inventory.material.NTMMaterial material, MaterialShapes shape, int count) {
        return new Mats.MaterialStack(material, shape.quantity * count);
    }

    private static ItemStack stack(dev.golem.core.registry.RegistryObject<? extends net.minecraft.world.item.Item> item, int count) {
        return new ItemStack(item.get(), count);
    }

    public static final class ElectrolysisMetalRecipe {
        public final AStack input;
        public final Mats.MaterialStack output1;
        public final Mats.MaterialStack output2;
        public final ItemStack[] byproduct;
        public final int duration;

        private ElectrolysisMetalRecipe(AStack input, Mats.MaterialStack output1, Mats.MaterialStack output2,
                                        int duration, ItemStack[] byproduct) {
            this.duration = duration;
            this.input = input;
            this.output1 = output1;
            this.output2 = output2;
            this.byproduct = byproduct;
        }
    }
}
