package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.PressRecipes.Row;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModCasingItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemStamp.StampType;
import dev.golem.core.world.RecipeSource;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7 {@code PressRecipes.registerDefaults}. */
public final class PressTable extends RecipeSource<Row> {

    private static final NTMMaterial COAL = Mats.MAT_COAL;
    private static final NTMMaterial LIGNITE = Mats.MAT_LIGNITE;

    public PressTable() {
        super(PressRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        makeRecipe(StampType.FLAT, dust("quartz"),                                                        Items.QUARTZ); // 1.7 NETHERQUARTZ.dust()
        makeRecipe(StampType.FLAT, dust("lapis"),                                                         new ItemStack(Items.LAPIS_LAZULI)); // 1.7 LAPIS.dust(), dye:4
        makeRecipe(StampType.FLAT, DIAMOND.dust(),                                                        Items.DIAMOND);
        makeRecipe(StampType.FLAT, EMERALD.dust(),                                                        Items.EMERALD);
        makeRecipe(StampType.FLAT, new ComparableStack(ModFuelItems.biomass.get()),                       ModFuelItems.biomass_compressed.get());
        makeRecipe(StampType.FLAT, new TagStack(PyroOvenRecipes.COKES),                                   ModFuelItems.ingot_graphite.get()); // 1.7 ANY_COKE.gem()
        makeRecipe(StampType.FLAT, new ComparableStack(ModAbilityToolItems.meteorite_sword_reforged.get()), ModAbilityToolItems.meteorite_sword_hardened.get());
        makeRecipe(StampType.FLAT, new ComparableStack(Items.JUNGLE_LOG),                                 ModFuelItems.ball_resin.get()); // 1.7 Blocks.log:3

        makeRecipe(StampType.FLAT, COAL.dust(),                                                           ModFinalConsumableItems.briquette_coal.get());
        makeRecipe(StampType.FLAT, LIGNITE.dust(),                                                        ModFinalConsumableItems.briquette_lignite.get());
        makeRecipe(StampType.FLAT, new ComparableStack(ModItems.powder_sawdust.get()),                    ModFinalConsumableItems.briquette_wood.get());

        makeRecipe(StampType.PLATE, IRON.ingot(),         ModItems.plate_iron.get());
        makeRecipe(StampType.PLATE, GOLD.ingot(),         ModItems.plate_gold.get());
        makeRecipe(StampType.PLATE, TI.ingot(),           ModItems.plate_titanium.get());
        makeRecipe(StampType.PLATE, AL.ingot(),           ModItems.plate_aluminium.get());
        makeRecipe(StampType.PLATE, STEEL.ingot(),        ModItems.plate_steel.get());
        makeRecipe(StampType.PLATE, PB.ingot(),           ModItems.plate_lead.get());
        makeRecipe(StampType.PLATE, CU.ingot(),           ModItems.plate_copper.get());
        makeRecipe(StampType.PLATE, SA326.ingot(),        ModItems.plate_schrabidium.get());
        makeRecipe(StampType.PLATE, CMB.ingot(),          ModItems.plate_combine_steel.get());
        makeRecipe(StampType.PLATE, GUNMETAL.ingot(),     ModItems.plate_gunmetal.get());
        makeRecipe(StampType.PLATE, WEAPONSTEEL.ingot(),  ModItems.plate_weaponsteel.get());
        makeRecipe(StampType.PLATE, BIGMT.ingot(),        ModItems.plate_saturnite.get());
        makeRecipe(StampType.PLATE, DURA.ingot(),         ModItems.plate_dura_steel.get());

        makeRecipe(StampType.C9,    GUNMETAL.plate(),     ModCasingItems.casing(EnumCasingType.SMALL, 4));
        makeRecipe(StampType.C50,   GUNMETAL.plate(),     ModCasingItems.casing(EnumCasingType.LARGE, 2));
        makeRecipe(StampType.C9,    WEAPONSTEEL.plate(),  ModCasingItems.casing(EnumCasingType.SMALL_STEEL, 4));
        makeRecipe(StampType.C50,   WEAPONSTEEL.plate(),  ModCasingItems.casing(EnumCasingType.LARGE_STEEL, 2));

        for (NTMMaterial mat : Mats.orderedList) {
            if (mat.autogen.contains(MaterialShapes.WIRE) && doesOreNameExist(MaterialShapes.INGOT, mat)) {
                makeRecipe(StampType.WIRE, mat.ingot(), new ItemStack(ModAutogenItems.get(MaterialShapes.WIRE, mat), 8));
            }
        }

        makeRecipe(StampType.CIRCUIT, SI.billet(),        ModIntermediateItems.circuit(EnumCircuitType.SILICON));

        makeRecipe(StampType.PRINTING1, new ComparableStack(Items.PAPER), ModFinalConsumableItems.flat("page_of_page1"));
        makeRecipe(StampType.PRINTING2, new ComparableStack(Items.PAPER), ModFinalConsumableItems.flat("page_of_page2"));
        makeRecipe(StampType.PRINTING3, new ComparableStack(Items.PAPER), ModFinalConsumableItems.flat("page_of_page3"));
        makeRecipe(StampType.PRINTING4, new ComparableStack(Items.PAPER), ModFinalConsumableItems.flat("page_of_page4"));
        makeRecipe(StampType.PRINTING5, new ComparableStack(Items.PAPER), ModFinalConsumableItems.flat("page_of_page5"));
        makeRecipe(StampType.PRINTING6, new ComparableStack(Items.PAPER), ModFinalConsumableItems.flat("page_of_page6"));
        makeRecipe(StampType.PRINTING7, new ComparableStack(Items.PAPER), ModFinalConsumableItems.flat("page_of_page7"));
        makeRecipe(StampType.PRINTING8, new ComparableStack(Items.PAPER), ModFinalConsumableItems.flat("page_of_page8"));
    }

    private void makeRecipe(StampType type, AStack in, Item out) {
        makeRecipe(type, in, new ItemStack(out));
    }

    /** 1.7 {@code recipes.put(new Pair(in, type), out)}; the id is the output's item name. */
    private void makeRecipe(StampType type, AStack in, ItemStack out) {
        register(BuiltInRegistries.ITEM.getKey(out.getItem()).getPath(),
                new Row(type, in, ItemSpec.of(out).getOrThrow(), nextOrder()));
    }

    /**
     * 1.7 {@code OreDictionary.doesOreNameExist(MaterialShapes.INGOT.make(mat))}: whether HBM has the material's ingot
     * (the vanilla ingots for iron, gold and copper; carbon's ingot is the graphite ingot, {@code c:ingots/carbon}).
     */
    private static boolean doesOreNameExist(MaterialShapes shape, NTMMaterial mat) {
        if (mat == Mats.MAT_CARBON) return true;
        if (shape == MaterialShapes.INGOT
                && (mat.name.equals("iron") || mat.name.equals("gold") || mat.name.equals("copper"))) return true;
        return BuiltInRegistries.ITEM.containsKey(Ids.of("hbm", shape.itemPrefix + "_" + mat.name));
    }

    /** A 1.7 {@code dust<Name>} ore name the port has no material for: its {@code c:dusts/} tag. */
    private static AStack dust(String name) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", "dusts/" + name)));
    }
}
