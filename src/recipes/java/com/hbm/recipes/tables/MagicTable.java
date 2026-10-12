package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.STEEL;

import com.hbm.blocks.ModFallingBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.MagicRecipes;
import com.hbm.inventory.recipes.MagicRecipes.Row;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.ModNuclearMiscItems;
import dev.golem.core.world.RecipeSource;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** 1.7 {@code MagicRecipes.register}. */
public final class MagicTable extends RecipeSource<Row> {

    public MagicTable() {
        super(MagicRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        // 1.7 ingot_u238m2 metadata 1..3 are the items hs_elements, hs_arsenic, hs_vault
        add(ModNuclearMiscItems.INGOT_U238M2.get(), comp(ModNuclearMiscItems.HS_ELEMENTS.get()), comp(ModNuclearMiscItems.HS_ARSENIC.get()), comp(ModNuclearMiscItems.HS_VAULT.get()));
        add(ModMeleeKitItems.rod_of_discord.get(), comp(Items.ENDER_PEARL), comp(Items.BLAZE_ROD), comp(ModItems.nugget_euphemium.get()));
        add(ModFinalConsumableItems.balefire_and_steel.get(), ore(STEEL.ingot()), comp(ModItems.egg_balefire_shard.get()));
        add(ModMeleeKitItems.mysteryshovel.get(), comp(Items.IRON_SHOVEL), comp(Items.BONE), comp(ModItems.ingot_starmetal.get()), comp(ModItems.ducttape.get()));
        add(ModFinalConsumableItems.ingot_electronium.get(), comp(ModItems.pellet_charged.get()), comp(ModItems.pellet_charged.get()), comp(ModItems.ingot_dineutronium.get()), comp(ModItems.ingot_dineutronium.get()));

        add(ModMeleeKitItems.diamond_gavel.get(),
                comp(ModFallingBlocks.gravel_diamond.get().asItem()),
                comp(ModFallingBlocks.gravel_diamond.get().asItem()),
                comp(ModFallingBlocks.gravel_diamond.get().asItem()),
                comp(ModMeleeKitItems.lead_gavel.get()));

        add(ModAbilityToolItems.mese_gavel.get(),
                comp(ModItems.shimmer_handle.get()),
                comp(ModItems.powder_dineutronium.get()),
                comp(ModMachineItems.blades_desh.get()),
                comp(ModMeleeKitItems.diamond_gavel.get()));
    }

    /** 1.7 {@code recipes.add(new MagicRecipe(out, in...))}; the id is the result's item name. */
    @SafeVarargs
    private void add(Item out, Either<TagKey<Item>, Item>... in) {
        register(BuiltInRegistries.ITEM.getKey(out).getPath().toLowerCase(Locale.ROOT),
                new Row(List.of(in), out, nextOrder()));
    }

    /** 1.7 {@code new ComparableStack(item)}. */
    private static Either<TagKey<Item>, Item> comp(Item item) {
        return Either.right(item);
    }

    /** 1.7 {@code new OreDictStack(STEEL.ingot())}: the port's {@code STEEL.ingot()} is already that tag stack. */
    private static Either<TagKey<Item>, Item> ore(AStack tag) {
        return Either.left(((TagStack) tag).tag);
    }
}
