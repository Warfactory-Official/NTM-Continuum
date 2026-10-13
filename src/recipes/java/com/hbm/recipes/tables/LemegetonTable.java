package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.LemegetonRecipes;
import com.hbm.inventory.recipes.LemegetonRecipes.Row;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import dev.golem.core.world.RecipeSource;
import com.mojang.datafixers.util.Either;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7 {@code LemegetonRecipes.register}. */
public final class LemegetonTable extends RecipeSource<Row> {

    /** 1.7 {@code KEY_SAND}, the "sand" ore name: {@code c:sands} (sand and red sand, not suspicious sand). */
    private static final TagKey<Item> KEY_SAND = TagKey.create(Registries.ITEM, Ids.of("c", "sands"));

    private static final NTMMaterial COAL = Mats.MAT_COAL;
    private static final NTMMaterial GRAPHITE = Mats.MAT_GRAPHITE;

    public LemegetonTable() {
        super(LemegetonRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        put(IRON.ingot(), new ItemStack(ModItems.ingot_steel.get()));
        put(STEEL.ingot(), new ItemStack(ModItems.ingot_dura_steel.get()));
        put(DURA.ingot(), new ItemStack(ModItems.ingot_tcalloy.get()));
        put(TCALLOY.ingot(), new ItemStack(ModItems.ingot_combine_steel.get()));
        put(CMB.ingot(), new ItemStack(ModItems.ingot_dineutronium.get()));

        put(TI.ingot(), new ItemStack(ModItems.ingot_saturnite.get()));
        put(BIGMT.ingot(), new ItemStack(ModItems.ingot_starmetal.get()));

        put(CU.ingot(), new ItemStack(ModItems.ingot_red_copper.get()));
        put(MINGRADE.ingot(), new ItemStack(ModItems.ingot_desh.get()));
        put(DESH.ingot(), new ItemStack(ModItems.ingot_bscco.get()));

        put(PB.ingot(), new ItemStack(Items.GOLD_INGOT));
        put(GOLD.ingot(), new ItemStack(ModItems.ingot_bismuth.get()));
        put(BI.ingot(), new ItemStack(ModItems.ingot_osmiridium.get()));

        put(ore("ingots/th232"), new ItemStack(ModItems.ingot_uranium.get())); // 1.7 TH232.ingot() "ingotThorium232": c:ingots/th232 holds exactly ingot_th232, 1.7's only member
        put(U.ingot(), new ItemStack(ModItems.ingot_u238.get()));
        put(U238.ingot(), new ItemStack(ModItems.ingot_u235.get()));
        put(U235.ingot(), new ItemStack(ModItems.ingot_plutonium.get()));
        put(PU.ingot(), new ItemStack(ModItems.ingot_pu238.get()));
        put(PU238.ingot(), new ItemStack(ModItems.ingot_pu239.get()));
        put(PU239.ingot(), new ItemStack(ModItems.ingot_pu240.get()));
        put(PU240.ingot(), new ItemStack(ModItems.ingot_pu241.get()));
        put(PU241.ingot(), new ItemStack(ModItems.ingot_am241.get()));
        put(AM241.ingot(), new ItemStack(ModItems.ingot_am242.get()));

        put(RA226.ingot(), new ItemStack(ModItems.ingot_polonium.get()));
        put(PO210.ingot(), new ItemStack(ModItems.ingot_technetium.get()));

        put(POLYMER.ingot(), new ItemStack(ModItems.ingot_pc.get()));
        put(BAKELITE.ingot(), new ItemStack(ModItems.ingot_pvc.get()));
        put(ore("ingots/latex"), new ItemStack(ModItems.ingot_rubber.get()));

        put(COAL.gem(), new ItemStack(ModFuelItems.ingot_graphite.get()));
        put(GRAPHITE.ingot(), new ItemStack(Items.DIAMOND));
        put(DIAMOND.gem(), new ItemStack(ModItems.ingot_cft.get()));

        put(F.dust(), new ItemStack(ModItems.gem_sodalite.get()));
        put(SODALITE.gem(), new ItemStack(ModItems.gem_volcanic.get()));
        put(ore("gems/volcanic"), new ItemStack(ModItems.gem_rad.get()));
        put(new ComparableStack(ModItems.gem_rad.get()), new ItemStack(ModItems.gem_alexandrite.get()));

        put(new TagStack(KEY_SAND), new ItemStack(ModItems.ingot_fiberglass.get()));
        put(ore("ingots/fiberglass"), new ItemStack(ModItems.ingot_asbestos.get()));

    }

    /** 1.7 {@code recipes.put(input, output)}; the id is the tag path with {@code _} for {@code /}, or the item name. */
    private void put(AStack input, ItemStack output) {
        if (input instanceof TagStack tag) {
            register(tag.tag.location().getPath().replace('/', '_'), new Row(Either.left(tag.tag), output.getItem(), nextOrder()));
        } else {
            Item item = ((ComparableStack) input).item;
            register(BuiltInRegistries.ITEM.getKey(item).getPath(), new Row(Either.right(item), output.getItem(), nextOrder()));
        }
    }

    /** A {@code c:} tag by path: the 1.7 {@code DictFrame}s the port has no material for ({@code LATEX}, {@code VOLCANIC}, {@code FIBER}). */
    private static AStack ore(String path) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", path)));
    }
}
