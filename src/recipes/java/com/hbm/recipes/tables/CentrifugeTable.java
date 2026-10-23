package com.hbm.recipes.tables;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.RawOres;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes.Row;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.CrystallizerRecipes.ChanceOutput;
import com.hbm.inventory.recipes.CrystallizerRecipes.ItemOutput;
import com.hbm.inventory.recipes.CrystallizerRecipes.Output;
import com.hbm.inventory.recipes.CrystallizerRecipes.TagOutput;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.BedrockOreType.BedrockOreOutput;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import dev.golem.core.world.RecipeSource;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

/**
 * 1.7 {@code CentrifugeRecipes.registerDefaults}. Each row with an {@code lbs ? a : b} output is written twice
 * ({@link #lbs(Runnable)}): the default row with {@code "!lbsm_simple_centrifuge"} and the LBSM row, id
 * {@code <id>.lbsm_simple_centrifuge}, with {@code "lbsm_simple_centrifuge"}. 1.7's
 * ore dictionary names are the port's {@code c:} tags, its meta items the port's split items, its
 * {@code ItemBedrockOreNew} grades the port's {@code bedrock_ore_<grade>_<type>} items, and its {@code ingot_mercury}
 * the port's {@code nugget_mercury} (1.7's own {@code nugget_mercury} field is {@code nugget_mercury_tiny}).
 */
public final class CentrifugeTable extends RecipeSource<Row> {

    /** 1.7 {@code GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCentrifuge}, per branch ({@link #lbs(Runnable)}). */
    private boolean lbs;
    /** The config branch of the rows being written (a row's {@code requires}) and its id suffix. */
    private List<String> requires = List.of();
    private String suffix = "";

    public CentrifugeTable() {
        super(CentrifugeRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        // 1.7 boolean lbs = GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCentrifuge; the rows reading it
        // are written once per branch (lbs(...))

        put(new ComparableStack(ModIntermediateItems.chunk(EnumChunkType.RARE)), new ItemStack[] {
                new ItemStack(ModItems.powder_cobalt_tiny.get(), 2),
                new ItemStack(ModItems.powder_boron_tiny.get(), 2),
                new ItemStack(ModItems.powder_niobium_tiny.get(), 2),
                new ItemStack(ModItems.nugget_zirconium.get(), 3) });

        put(ore("coal"), new ItemStack[] {
                new ItemStack(ModFuelItems.powder_coal.get(), 2),
                new ItemStack(ModFuelItems.powder_coal.get(), 2),
                new ItemStack(ModFuelItems.powder_coal.get(), 2),
                new ItemStack(Items.GRAVEL, 1) });

        put(ore("lignite"), new ItemStack[] {
                new ItemStack(ModFuelItems.powder_lignite.get(), 2),
                new ItemStack(ModFuelItems.powder_lignite.get(), 2),
                new ItemStack(ModFuelItems.powder_lignite.get(), 2),
                new ItemStack(Items.GRAVEL, 1) });

        ore(IRON, new ItemStack[] {
                new ItemStack(ModItems.powder_iron.get(), 1),
                new ItemStack(ModItems.powder_iron.get(), 1),
                new ItemStack(ModItems.powder_iron.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        lbs(() ->
            ore(GOLD, new ItemStack[] {
                    lbs ? new ItemStack(ModItems.powder_gold.get(), 2) : new ItemStack(ModItems.powder_gold.get(), 1),
                    new ItemStack(ModItems.powder_gold.get(), 1),
                    lbs ? new ItemStack(ModItems.nugget_bismuth.get(), 1) : new ItemStack(ModItems.powder_gold.get(), 1),
                    new ItemStack(Items.GRAVEL, 1) }));

        put(DIAMOND.ore(), new ItemStack[] {
                new ItemStack(ModItems.powder_diamond.get(), 1),
                new ItemStack(ModItems.powder_diamond.get(), 1),
                new ItemStack(ModItems.powder_diamond.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        put(EMERALD.ore(), new ItemStack[] {
                new ItemStack(ModItems.powder_emerald.get(), 1),
                new ItemStack(ModItems.powder_emerald.get(), 1),
                new ItemStack(ModItems.powder_emerald.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        lbs(() ->
            ore(TI, new ItemStack[] {
                    lbs ? new ItemStack(ModItems.powder_titanium.get(), 2) : new ItemStack(ModItems.powder_titanium.get(), 1),
                    lbs ? ModIntermediateItems.chunk(EnumChunkType.ILMENITE, 4) : ModIntermediateItems.chunk(EnumChunkType.ILMENITE, 2),
                    new ItemStack(ModItems.powder_iron.get(), 1),
                    new ItemStack(Items.GRAVEL, 1) }));

        // 1.7 NETHERQUARTZ.ore() "oreNetherQuartz" (the nether quartz ore) is c:ores/quartz
        put(ore("quartz"), new ItemStack[] {
                new ItemStack(ModItems.powder_quartz.get(), 1),
                new ItemStack(ModItems.powder_quartz.get(), 1),
                new ItemStack(ModItems.powder_lithium_tiny.get(), 1),
                new ItemStack(Items.NETHERRACK, 1) });

        lbs(() ->
            ore(W, new ItemStack[] {
                    lbs ? new ItemStack(ModItems.powder_tungsten.get(), 2) : new ItemStack(ModItems.powder_tungsten.get(), 1),
                    new ItemStack(ModItems.powder_tungsten.get(), 1),
                    new ItemStack(ModItems.powder_iron.get(), 1),
                    new ItemStack(Items.GRAVEL, 1) }));

        lbs(() ->
            ore(CU, new ItemStack[] {
                    lbs ? new ItemStack(ModItems.powder_copper.get(), 2) : new ItemStack(ModItems.powder_copper.get(), 1),
                    new ItemStack(ModItems.powder_copper.get(), 1),
                    new ItemStack(ModItems.powder_gold.get(), 1),
                    new ItemStack(Items.GRAVEL, 1) }));

        ore(AL, new ItemStack[] {
                ModIntermediateItems.chunk(EnumChunkType.CRYOLITE, 2),
                new ItemStack(ModItems.powder_titanium.get(), 1),
                new ItemStack(ModItems.powder_iron.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        lbs(() ->
            ore(PB, new ItemStack[] {
                    lbs ? new ItemStack(ModItems.powder_lead.get(), 2) : new ItemStack(ModItems.powder_lead.get(), 1),
                    lbs ? new ItemStack(ModItems.nugget_bismuth.get(), 1) : new ItemStack(ModItems.powder_lead.get(), 1),
                    new ItemStack(ModItems.powder_gold.get(), 1),
                    new ItemStack(Items.GRAVEL, 1) }));

        ore(SA326, new ItemStack[] {
                new ItemStack(ModItems.powder_schrabidium.get(), 1),
                new ItemStack(ModItems.powder_schrabidium.get(), 1),
                new ItemStack(ModItems.nugget_solinium.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        put(RAREEARTH.ore(), new ItemStack[] {
                new ItemStack(ModItems.powder_desh_mix.get(), 1),
                new ItemStack(ModItems.nugget_zirconium.get(), 1),
                new ItemStack(ModItems.nugget_zirconium.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        ore(PU, new ItemStack[] {
                new ItemStack(ModItems.powder_plutonium.get(), 1),
                new ItemStack(ModItems.powder_plutonium.get(), 1),
                new ItemStack(ModItems.nugget_polonium.get(), 3),
                new ItemStack(Items.GRAVEL, 1) });

        lbs(() ->
            ore(U, new ItemStack[] {
                    lbs ? new ItemStack(ModItems.powder_uranium.get(), 2) : new ItemStack(ModItems.powder_uranium.get(), 1),
                    lbs ? new ItemStack(ModItems.nugget_technetium.get(), 2) : new ItemStack(ModItems.powder_uranium.get(), 1),
                    lbs ? new ItemStack(ModItems.nugget_ra226.get(), 2) : new ItemStack(ModItems.nugget_ra226.get(), 1),
                    new ItemStack(Items.GRAVEL, 1) }));

        ore(TH232, new ItemStack[] {
                new ItemStack(ModItems.powder_thorium.get(), 1),
                new ItemStack(ModItems.powder_thorium.get(), 1),
                new ItemStack(ModItems.powder_uranium.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        ore(BE, new ItemStack[] {
                new ItemStack(ModItems.powder_beryllium.get(), 1),
                new ItemStack(ModItems.powder_beryllium.get(), 1),
                new ItemStack(ModItems.powder_emerald.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        put(F.ore(), new ItemStack[] {
                new ItemStack(ModItems.fluorite.get(), 3),
                new ItemStack(ModItems.fluorite.get(), 3),
                new ItemStack(ModItems.gem_sodalite.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        lbs(() ->
            put(ore("redstone"), new ItemStack[] {
                    new ItemStack(Items.REDSTONE, 3),
                    new ItemStack(Items.REDSTONE, 3),
                    lbs ? new ItemStack(ModItems.nugget_mercury.get(), 3) : new ItemStack(ModItems.nugget_mercury.get(), 1),
                    new ItemStack(Items.GRAVEL, 1) }));

        put(new ComparableStack(ModBlocks.ore_tikite.get().asItem()), new ItemStack[] {
                new ItemStack(ModItems.powder_plutonium.get(), 1),
                new ItemStack(ModItems.powder_cobalt.get(), 2),
                new ItemStack(ModItems.powder_niobium.get(), 2),
                new ItemStack(Items.END_STONE, 1) });

        put(ore("lapis"), new ItemStack[] {
                new ItemStack(ModItems.powder_lapis.get(), 6),
                new ItemStack(ModItems.powder_cobalt_tiny.get(), 1),
                new ItemStack(ModItems.gem_sodalite.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        put(new ComparableStack(ModDecoContent.block_euphemium_cluster.get().asItem()), new ItemStack[] {
                new ItemStack(ModItems.nugget_euphemium.get(), 7),
                new ItemStack(ModItems.powder_schrabidium.get(), 4),
                new ItemStack(ModItems.ingot_starmetal.get(), 2),
                new ItemStack(ModItems.nugget_solinium.get(), 2) });

        put(new ComparableStack(ModBlocks.ore_nether_fire.get().asItem()), new ItemStack[] {
                new ItemStack(Items.BLAZE_POWDER, 2),
                new ItemStack(ModFuelItems.powder_fire.get(), 2),
                new ItemStack(ModItems.ingot_phosphorus.get()),
                new ItemStack(Items.NETHERRACK) });

        ore(CO, new ItemStack[] {
                new ItemStack(ModItems.powder_cobalt.get(), 2),
                new ItemStack(ModItems.powder_iron.get(), 1),
                new ItemStack(ModItems.powder_copper.get(), 1),
                new ItemStack(Items.GRAVEL, 1) });

        put(new ComparableStack(ModItems.powder_tektite.get()), new ItemStack[] {
                new ItemStack(ModItems.powder_meteorite_tiny.get(), 1),
                new ItemStack(ModItems.powder_paleogenite_tiny.get(), 1),
                new ItemStack(ModItems.powder_meteorite_tiny.get(), 1),
                new ItemStack(ModItems.dust.get(), 6) });

        put(new ComparableStack(ModBlocks.block_slag.get().asItem()), new ItemStack[] {
                new ItemStack(Items.GRAVEL, 1),
                new ItemStack(ModFuelItems.powder_fire.get(), 1),
                new ItemStack(ModItems.powder_calcium.get()),
                new ItemStack(ModItems.dust.get()) });

        put(new ComparableStack(ModIngredientItems.ash(EnumAshType.COAL)), new ItemStack[] {
                new ItemStack(ModFuelItems.powder_coal_tiny.get(), 2),
                new ItemStack(ModItems.powder_boron_tiny.get(), 1),
                new ItemStack(ModItems.dust_tiny.get(), 6)});

        // port: 1.7's EnumBedrockOre loop (ore_bedrock, ore_cleaned, ore_purified, ore_nitrated, ore_deepcleaned,
        // ore_seared) has no rows: the old bedrock ore items are not ported

        for (BedrockOreType type : BedrockOreType.VALUES) {

            put(new ComparableStack(ModBedrockOreItems.get(Grade.BASE, type)), new ItemStack[] {make(Grade.PRIMARY, type), new ItemStack(Items.GRAVEL)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.BASE_ROASTED, type)), new ItemStack[] {make(Grade.PRIMARY, type), new ItemStack(Items.GRAVEL)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.BASE_WASHED, type)), new ItemStack[] {make(Grade.PRIMARY, type), make(Grade.PRIMARY, type), new ItemStack(Items.GRAVEL)});

            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_SULFURIC, type)), new ItemStack[] {make(Grade.PRIMARY_NOSULFURIC, type, 2), make(Grade.SULFURIC_BYPRODUCT, type, 2)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_SOLVENT, type)), new ItemStack[] {make(Grade.PRIMARY_NOSOLVENT, type, 2), make(Grade.SULFURIC_BYPRODUCT, type, 2), make(Grade.SOLVENT_BYPRODUCT, type, 2)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_RAD, type)), new ItemStack[] {make(Grade.PRIMARY_NORAD, type, 2), make(Grade.SULFURIC_BYPRODUCT, type, 2), make(Grade.SOLVENT_BYPRODUCT, type, 2), make(Grade.RAD_BYPRODUCT, type, 2)});

            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY, type)), new ItemStack[] {extract(type.primary1, 1), extract(type.primary2, 1)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_ROASTED, type)), new ItemStack[] {extract(type.primary1, 1), extract(type.primary2, 1)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_NOSULFURIC, type)), new ItemStack[] {extract(type.primary1, 1), extract(type.primary2, 1), make(Grade.CRUMBS, type)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_NOSOLVENT, type)), new ItemStack[] {extract(type.primary1, 1), extract(type.primary2, 1), make(Grade.CRUMBS, type)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_NORAD, type)), new ItemStack[] {extract(type.primary1, 1), extract(type.primary2, 1), make(Grade.CRUMBS, type)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_FIRST, type)), new ItemStack[] {extract(type.primary1, 1), extract(type.primary1, 1), extract(type.primary2, 1), make(Grade.CRUMBS, type, 1)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_SECOND, type)), new ItemStack[] {extract(type.primary1, 1), extract(type.primary2, 1), extract(type.primary2, 1), make(Grade.CRUMBS, type, 1)});

            put(new ComparableStack(ModBedrockOreItems.get(Grade.SULFURIC_WASHED, type)), new ItemStack[] {extract(type.byproductAcid1, 1), extract(type.byproductAcid2, 1), extract(type.byproductAcid3, 1), make(Grade.CRUMBS, type)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.SOLVENT_WASHED, type)), new ItemStack[] {extract(type.byproductSolvent1, 1), extract(type.byproductSolvent2, 1), extract(type.byproductSolvent3, 1), make(Grade.CRUMBS, type)});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.RAD_WASHED, type)), new ItemStack[] {extract(type.byproductRad1, 1), extract(type.byproductRad2, 1), extract(type.byproductRad3, 1), make(Grade.CRUMBS, type)});
        }

        // 1.7 copies the first crystalCertusQuartz stack, 2 of it, when there is one
        TagKey<Item> quartz = TagKey.create(Registries.ITEM, Ids.of("c", "gems/certus_quartz"));
        requires = List.of("#" + quartz.location());
        put(ore("certus_quartz"), new Output[] {
                new TagOutput(quartz, 2),
                new TagOutput(quartz, 2),
                new TagOutput(quartz, 2),
                new TagOutput(quartz, 2) });
        // port: AE2 has no certus ore any more, so its mature clusters stand in, as in ntm-next
        put(new TagStack(TagKey.create(Registries.ITEM, Ids.of("hbm", "certus_quartz_clusters"))), new Output[] {
                new TagOutput(quartz, 2),
                new TagOutput(quartz, 2),
                new TagOutput(quartz, 2),
                new TagOutput(quartz, 2) });
        requires = List.of();

        put(new ComparableStack(Items.BLAZE_ROD), new ItemStack[] {new ItemStack(Items.BLAZE_POWDER, 1), new ItemStack(Items.BLAZE_POWDER, 1), new ItemStack(ModFuelItems.powder_fire.get(), 1), new ItemStack(ModFuelItems.powder_fire.get(), 1) });

        put(new ComparableStack(ModFuelItems.crystal_coal.get()), new ItemStack[] { new ItemStack(ModFuelItems.powder_coal.get(), 3), new ItemStack(ModFuelItems.powder_coal.get(), 3), new ItemStack(ModFuelItems.powder_coal.get(), 3), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_iron.get()), new ItemStack[] { new ItemStack(ModItems.powder_iron.get(), 2), new ItemStack(ModItems.powder_iron.get(), 2), new ItemStack(ModItems.powder_titanium.get(), 1), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_gold.get()), new ItemStack[] { new ItemStack(ModItems.powder_gold.get(), 2), new ItemStack(ModItems.powder_gold.get(), 2), new ItemStack(ModItems.nugget_mercury.get(), 1), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_redstone.get()), new ItemStack[] { new ItemStack(Items.REDSTONE, 3), new ItemStack(Items.REDSTONE, 3), new ItemStack(Items.REDSTONE, 3), new ItemStack(ModItems.nugget_mercury.get(), 3) });
        put(new ComparableStack(ModItems.crystal_lapis.get()), new ItemStack[] { new ItemStack(ModItems.powder_lapis.get(), 4), new ItemStack(ModItems.powder_lapis.get(), 4), new ItemStack(ModItems.powder_cobalt.get(), 1), new ItemStack(ModItems.gem_sodalite.get(), 2) });
        put(new ComparableStack(ModItems.crystal_diamond.get()), new ItemStack[] { new ItemStack(ModItems.powder_diamond.get(), 1), new ItemStack(ModItems.powder_diamond.get(), 1), new ItemStack(ModItems.powder_diamond.get(), 1), new ItemStack(ModItems.powder_diamond.get(), 1) });
        put(new ComparableStack(ModItems.crystal_uranium.get()), new ItemStack[] { new ItemStack(ModItems.powder_uranium.get(), 2), new ItemStack(ModItems.powder_uranium.get(), 2), new ItemStack(ModItems.nugget_ra226.get(), 2), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_thorium.get()), new ItemStack[] { new ItemStack(ModItems.powder_thorium.get(), 2), new ItemStack(ModItems.powder_thorium.get(), 2), new ItemStack(ModItems.powder_uranium.get(), 1), new ItemStack(ModItems.nugget_ra226.get(), 1) });
        put(new ComparableStack(ModItems.crystal_plutonium.get()), new ItemStack[] { new ItemStack(ModItems.powder_plutonium.get(), 2), new ItemStack(ModItems.powder_plutonium.get(), 2), new ItemStack(ModItems.powder_polonium.get(), 1), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_titanium.get()), new ItemStack[] { new ItemStack(ModItems.powder_titanium.get(), 2), new ItemStack(ModItems.powder_titanium.get(), 2), new ItemStack(ModItems.powder_iron.get(), 1), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_sulfur.get()), new ItemStack[] { new ItemStack(ModItems.sulfur.get(), 4), new ItemStack(ModItems.sulfur.get(), 4), new ItemStack(ModItems.powder_iron.get(), 1), new ItemStack(ModItems.nugget_mercury.get(), 1) });
        put(new ComparableStack(ModItems.crystal_niter.get()), new ItemStack[] { new ItemStack(ModItems.niter.get(), 3), new ItemStack(ModItems.niter.get(), 3), new ItemStack(ModItems.niter.get(), 3), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_copper.get()), new ItemStack[] { new ItemStack(ModItems.powder_copper.get(), 2), new ItemStack(ModItems.powder_copper.get(), 2), new ItemStack(ModItems.sulfur.get(), 1), new ItemStack(ModItems.powder_cobalt_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_tungsten.get()), new ItemStack[] { new ItemStack(ModItems.powder_tungsten.get(), 2), new ItemStack(ModItems.powder_tungsten.get(), 2), new ItemStack(ModItems.powder_iron.get(), 1), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_aluminium.get()), new ItemStack[] { ModIntermediateItems.chunk(EnumChunkType.CRYOLITE, 3), new ItemStack(ModItems.powder_titanium.get(), 1), new ItemStack(ModItems.powder_iron.get(), 1), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_fluorite.get()), new ItemStack[] { new ItemStack(ModItems.fluorite.get(), 4), new ItemStack(ModItems.fluorite.get(), 4), new ItemStack(ModItems.gem_sodalite.get(), 2), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_beryllium.get()), new ItemStack[] { new ItemStack(ModItems.powder_beryllium.get(), 2), new ItemStack(ModItems.powder_beryllium.get(), 2), new ItemStack(ModItems.powder_quartz.get(), 1), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_lead.get()), new ItemStack[] { new ItemStack(ModItems.powder_lead.get(), 2), new ItemStack(ModItems.powder_lead.get(), 2), new ItemStack(ModItems.powder_gold.get(), 1), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_schraranium.get()), new ItemStack[] { new ItemStack(ModItems.nugget_schrabidium.get(), 2), new ItemStack(ModItems.nugget_schrabidium.get(), 2), new ItemStack(ModItems.nugget_uranium.get(), 2), new ItemStack(ModItems.nugget_neptunium.get(), 2) });
        put(new ComparableStack(ModItems.crystal_schrabidium.get()), new ItemStack[] { new ItemStack(ModItems.powder_schrabidium.get(), 2), new ItemStack(ModItems.powder_schrabidium.get(), 2), new ItemStack(ModItems.powder_plutonium.get(), 1), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
        put(new ComparableStack(ModItems.crystal_rare.get()), new ItemStack[] { new ItemStack(ModItems.powder_desh_mix.get(), 1), new ItemStack(ModItems.powder_desh_mix.get(), 1), new ItemStack(ModItems.nugget_zirconium.get(), 2), new ItemStack(ModItems.nugget_zirconium.get(), 2) });
        put(new ComparableStack(ModItems.crystal_phosphorus.get()), new ItemStack[] { new ItemStack(ModFuelItems.powder_fire.get(), 3), new ItemStack(ModFuelItems.powder_fire.get(), 3), new ItemStack(ModItems.ingot_phosphorus.get(), 2), new ItemStack(Items.BLAZE_POWDER, 2) });
        put(new ComparableStack(ModItems.crystal_trixite.get()), new ItemStack[] { new ItemStack(ModItems.powder_plutonium.get(), 2), new ItemStack(ModItems.powder_cobalt.get(), 3), new ItemStack(ModItems.powder_niobium.get(), 2), new ItemStack(ModItems.powder_nitan_mix.get(), 1) });
        put(new ComparableStack(ModItems.crystal_lithium.get()), new ItemStack[] { new ItemStack(ModItems.powder_lithium.get(), 2), new ItemStack(ModItems.powder_lithium.get(), 2), new ItemStack(ModItems.powder_quartz.get(), 1), new ItemStack(ModItems.fluorite.get(), 1) });
        put(new ComparableStack(ModItems.crystal_starmetal.get()), new ItemStack[] { new ItemStack(ModItems.powder_dura_steel.get(), 3), new ItemStack(ModItems.powder_cobalt.get(), 3), new ItemStack(ModItems.powder_astatine.get(), 2), new ItemStack(ModItems.nugget_mercury.get(), 5) });
        put(new ComparableStack(ModItems.crystal_cobalt.get()), new ItemStack[] { new ItemStack(ModItems.powder_cobalt.get(), 2), new ItemStack(ModItems.powder_iron.get(), 3), new ItemStack(ModItems.powder_copper.get(), 3), new ItemStack(ModItems.powder_lithium_tiny.get(), 1) });
    }

    private static ItemStack make(Grade grade, BedrockOreType type) {
        return ModBedrockOreItems.make(grade, type);
    }

    private static ItemStack make(Grade grade, BedrockOreType type, int amount) {
        return ModBedrockOreItems.make(grade, type, amount);
    }

    /** 1.7 {@code ItemBedrockOreNew.extract}. */
    private static ItemStack extract(BedrockOreOutput output, double amount) {
        return BedrockOreType.fragment(output, amount);
    }

    /** 1.7 {@code new OreDictStack("ore<Name>")} for a name with no port material alias. */
    private static AStack ore(String name) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", "ores/" + name)));
    }

    /**
     * 1.7's {@code lbs ? a : b} rows: {@code rows} runs once with {@code lbs} false (the default rows,
     * {@code "!lbsm_simple_centrifuge"}) and once with it true ({@code "lbsm_simple_centrifuge"}, id suffix
     * {@code .lbsm_simple_centrifuge}).
     */
    private void lbs(Runnable rows) {
        lbs = false;
        requires = List.of("!lbsm_simple_centrifuge");
        rows.run();
        lbs = true;
        requires = List.of("lbsm_simple_centrifuge");
        suffix = ".lbsm_simple_centrifuge";
        rows.run();
        lbs = false;
        requires = List.of();
        suffix = "";
    }

    /** 1.7's ore row, then (port) ntm-next's raw row: the same outputs, each made at {@link RawOres#SHARE}. */
    private void ore(NTMMaterial material, ItemStack[] outputs) {
        put(material.ore(), outputs);
        List<Output> raw = new ArrayList<>();
        for (ItemStack stack : outputs) raw.add(new ChanceOutput(ItemOut.of(stack), RawOres.SHARE));
        put(RawOres.stack(material), raw.toArray(Output[]::new));
    }

    /** 1.7 {@code recipes.put(input, outputs)}. */
    private void put(AStack input, ItemStack[] outputs) {
        List<Output> out = new ArrayList<>();
        for (ItemStack stack : outputs) out.add(new ItemOutput(ItemOut.of(stack)));
        put(input, out.toArray(Output[]::new));
    }

    /**
     * 1.7 {@code recipes.put(input, outputs)}. The id is the input: the item (its namespace first unless {@code hbm}),
     * or {@code tag_} and the tag path with slashes as underscores; then the branch suffix.
     */
    private void put(AStack input, Output[] outputs) {
        String id;
        if (input instanceof TagStack tag) {
            id = "tag_" + tag.tag.location().getPath().replace('/', '_');
        } else {
            var key = BuiltInRegistries.ITEM.getKey(((ComparableStack) input).item);
            id = key.getNamespace().equals("hbm") ? key.getPath() : key.getNamespace() + "_" + key.getPath();
        }
        register(id + suffix, new Row(input, List.of(outputs), requires, nextOrder()));
    }
}
