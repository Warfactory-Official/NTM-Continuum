package com.hbm.recipes.tables;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes.ItemOutput;
import com.hbm.inventory.recipes.CrystallizerRecipes.Output;
import com.hbm.inventory.recipes.CrystallizerRecipes.Row;
import com.hbm.inventory.recipes.CrystallizerRecipes.ScrapsOutput;
import com.hbm.inventory.recipes.CrystallizerRecipes.TagOutput;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.ModChemicalDyeItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.ModTerrainItems;
import com.hbm.items.machine.ItemChemicalDye.EnumChemDye;
import com.hbm.items.machine.ItemScraps;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import dev.golem.core.world.RecipeSource;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

import static com.hbm.inventory.OreDictManager.*;

/**
 * 1.7 {@code CrystallizerRecipes.registerDefaults}. 1.7's ore dictionary names are the port's {@code c:} tags, its meta
 * items the port's split items, its {@code ItemBedrockOreNew} grades the port's {@code bedrock_ore_<grade>_<type>}
 * items, and its {@code ingot_mercury} the port's {@code nugget_mercury} (1.7's own {@code nugget_mercury} field is
 * {@code nugget_mercury_tiny}). 1.7's three {@code OreDictionary.getOres(...)} compat rows are rows with
 * {@code requires}: the machine counts them only while that tag has a member, as 1.7 registered them only then.
 */
public final class CrystallizerTable extends RecipeSource<Row> {

    /** 1.7 {@code KEY_SAND}, the "sand" ore name: {@code c:sands} (sand and red sand, not suspicious sand). */
    private static final TagKey<Item> KEY_SAND = TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", "sands"));

    public CrystallizerTable() {
        super(CrystallizerRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        final int baseTime = 600;
        final int utilityTime = 100;
        final int mixingTime = 20;
        FluidStack sulfur = new FluidStack(Fluids.SULFURIC_ACID, 500);

        registerRecipe(ore("coal"),         new CrystallizerRecipe(ModFuelItems.crystal_coal.get(), baseTime).prod(0.05F));
        registerRecipe(IRON.ore(),          new CrystallizerRecipe(ModItems.crystal_iron.get(), baseTime).prod(0.05F));
        registerRecipe(GOLD.ore(),          new CrystallizerRecipe(ModItems.crystal_gold.get(), baseTime).prod(0.05F));
        registerRecipe(ore("redstone"),     new CrystallizerRecipe(ModItems.crystal_redstone.get(), baseTime).prod(0.05F));
        registerRecipe(ore("lapis"),        new CrystallizerRecipe(ModItems.crystal_lapis.get(), baseTime).prod(0.05F));
        registerRecipe(DIAMOND.ore(),       new CrystallizerRecipe(ModItems.crystal_diamond.get(), baseTime).prod(0.05F));
        registerRecipe(U.ore(),             new CrystallizerRecipe(ModItems.crystal_uranium.get(), baseTime).prod(0.05F), sulfur);
        // 1.7 loops every thorium ore name (TH232.all(ONLY_ORE)); the port has the one tag
        registerRecipe(TH232.ore(),         new CrystallizerRecipe(ModItems.crystal_thorium.get(), baseTime).prod(0.05F), sulfur);
        registerRecipe(PU.ore(),            new CrystallizerRecipe(ModItems.crystal_plutonium.get(), baseTime).prod(0.05F), sulfur);
        registerRecipe(TI.ore(),            new CrystallizerRecipe(ModItems.crystal_titanium.get(), baseTime).prod(0.05F), sulfur);
        registerRecipe(S.ore(),             new CrystallizerRecipe(ModItems.crystal_sulfur.get(), baseTime).prod(0.05F));
        registerRecipe(KNO.ore(),           new CrystallizerRecipe(ModItems.crystal_niter.get(), baseTime).prod(0.05F));
        registerRecipe(CU.ore(),            new CrystallizerRecipe(ModItems.crystal_copper.get(), baseTime).prod(0.05F));
        registerRecipe(W.ore(),             new CrystallizerRecipe(ModItems.crystal_tungsten.get(), baseTime).prod(0.05F), sulfur);
        registerRecipe(AL.ore(),            new CrystallizerRecipe(ModItems.crystal_aluminium.get(), baseTime).prod(0.05F));
        registerRecipe(F.ore(),             new CrystallizerRecipe(ModItems.crystal_fluorite.get(), baseTime).prod(0.05F));
        registerRecipe(BE.ore(),            new CrystallizerRecipe(ModItems.crystal_beryllium.get(), baseTime).prod(0.05F));
        registerRecipe(PB.ore(),            new CrystallizerRecipe(ModItems.crystal_lead.get(), baseTime).prod(0.05F));
        registerRecipe(SA326.ore(),         new CrystallizerRecipe(ModItems.crystal_schrabidium.get(), baseTime).prod(0.05F), sulfur);
        registerRecipe(LI.ore(),            new CrystallizerRecipe(ModItems.crystal_lithium.get(), baseTime).prod(0.05F), sulfur);
        registerRecipe(CO.ore(),            new CrystallizerRecipe(ModItems.crystal_cobalt.get(), baseTime).prod(0.05F), sulfur);

        registerRecipe(new ComparableStack(ModItems.powder_calcium.get()),           new CrystallizerRecipe(new ItemStack(ModFuelItems.powder_cement.get(), 8), utilityTime).prod(0.1F), new FluidStack(Fluids.REDMUD, 75));
        registerRecipe(MALACHITE.ingot(),   new CrystallizerRecipe(ItemScraps.create(new MaterialStack(Mats.MAT_COPPER, MaterialShapes.INGOT.quantity * 1)), 300).prod(0.1F), new FluidStack(Fluids.SULFURIC_ACID, 250));

        registerRecipe(ore("rare"),         new CrystallizerRecipe(ModItems.crystal_rare.get(), baseTime).prod(0.05F), sulfur);
        registerRecipe(ore("cinnabar"),     new CrystallizerRecipe(ModItems.crystal_cinnabar.get(), baseTime).prod(0.05F));

        registerRecipe(new ComparableStack(ModBlocks.ore_nether_fire.get().asItem()),     new CrystallizerRecipe(ModItems.crystal_phosphorus.get(), baseTime).prod(0.05F));
        registerRecipe(new ComparableStack(ModBlocks.ore_tikite.get().asItem()),          new CrystallizerRecipe(ModItems.crystal_trixite.get(), baseTime).prod(0.05F), sulfur);
        registerRecipe(new ComparableStack(ModFallingBlocks.gravel_diamond.get().asItem()), new CrystallizerRecipe(ModItems.crystal_diamond.get(), baseTime).prod(0.05F));
        registerRecipe(SRN.ingot(),                                                       new CrystallizerRecipe(ModItems.crystal_schraranium.get(), baseTime).prod(0.05F));

        registerRecipe(new TagStack(KEY_SAND), new CrystallizerRecipe(ModItems.ingot_fiberglass.get(), utilityTime).prod(0.15F));
        registerRecipe(SI.ingot(),          new CrystallizerRecipe(new ItemStack(Items.QUARTZ, 2), utilityTime).prod(0.1F), new FluidStack(Fluids.OXYGEN, 250));
        registerRecipe(storageBlock("redstone"), new CrystallizerRecipe(ModItems.nugget_mercury.get(), baseTime).prod(0.25F));
        // 1.7 CINNABAR.crystal() is the cinnabar gem item (crystalCinnabar); the port's c:crystals/cinnabar holds the
        // acidizer crystal instead, so the gem tag carries 1.7's meaning
        registerRecipe(CINNABAR.gem(),      new CrystallizerRecipe(new ItemStack(ModItems.nugget_mercury.get(), 3), baseTime).prod(0.25F));
        registerRecipe(BORAX.dust(),        new CrystallizerRecipe(new ItemStack(ModItems.powder_boron_tiny.get(), 3), baseTime).prod(0.25F), sulfur);
        registerRecipe(storageBlock("coal"), new CrystallizerRecipe(ModRBMKBlocks.block_graphite.get(), baseTime));

        registerRecipe(new ComparableStack(Items.COBBLESTONE),                              new CrystallizerRecipe(ModBlocks.reinforced_stone.get(), utilityTime));
        registerRecipe(new ComparableStack(ModFallingBlocks.gravel_obsidian.get().asItem()), new CrystallizerRecipe(ModBlocks.brick_obsidian.get(), utilityTime));
        registerRecipe(new ComparableStack(Items.ROTTEN_FLESH),                             new CrystallizerRecipe(Items.LEATHER, utilityTime).prod(0.25F));
        registerRecipe(new ComparableStack(ModFuelItems.coal_infernal.get()),               new CrystallizerRecipe(ModFuelItems.solid_fuel.get(), utilityTime));
        registerRecipe(new ComparableStack(ModBlocks.stone_gneiss.get().asItem()),          new CrystallizerRecipe(ModItems.powder_lithium.get(), utilityTime).prod(0.25F));
        // 1.7 Items.dye meta 15 is bone meal
        registerRecipe(new ComparableStack(Items.BONE_MEAL),                                new CrystallizerRecipe(new ItemStack(Items.SLIME_BALL, 4), mixingTime), new FluidStack(Fluids.SULFURIC_ACID, 250));
        registerRecipe(new ComparableStack(Items.BONE),                                     new CrystallizerRecipe(new ItemStack(Items.SLIME_BALL, 16), mixingTime), new FluidStack(Fluids.SULFURIC_ACID, 1_000));
        registerRecipe(new ComparableStack(ModTerrainItems.plant_item_mustardwillow.get()), new CrystallizerRecipe(new ItemStack(ModItems.powder_cadmium.get()), 100).setReq(10), new FluidStack(Fluids.RADIOSOLVENT, 250));
        registerRecipe(new ComparableStack(ModItems.scrap_oil.get()),                       new CrystallizerRecipe(new ItemStack(ModItems.nugget_arsenic.get()), 100).prod(0.3F).setReq(16), new FluidStack(Fluids.RADIOSOLVENT, 100));
        registerRecipe(new ComparableStack(ModIngredientItems.ash(EnumAshType.FULLERENE)),  new CrystallizerRecipe(new ItemStack(ModItems.ingot_cft.get()), baseTime).prod(0.1F).setReq(4), new FluidStack(Fluids.XYLENE, 1_000));

        registerRecipe(dust("diamond"),     new CrystallizerRecipe(Items.DIAMOND, utilityTime));
        registerRecipe(dust("emerald"),     new CrystallizerRecipe(Items.EMERALD, utilityTime));
        // 1.7 Items.dye meta 4 is lapis lazuli
        registerRecipe(dust("lapis"),       new CrystallizerRecipe(new ItemStack(Items.LAPIS_LAZULI, 1), utilityTime));
        registerRecipe(new ComparableStack(ModItems.powder_semtex_mix.get()), new CrystallizerRecipe(ModFoodItems.ingot_semtex.get(), baseTime));
        registerRecipe(new ComparableStack(ModItems.powder_desh_ready.get()), new CrystallizerRecipe(ModItems.ingot_desh.get(), baseTime));
        registerRecipe(new ComparableStack(ModItems.powder_meteorite.get()),  new CrystallizerRecipe(ModItems.fragment_meteorite.get(), utilityTime));
        registerRecipe(CD.dust(),           new CrystallizerRecipe(new ItemStack(ModItems.ingot_rubber.get(), 16), utilityTime), new FluidStack(Fluids.FISHOIL, 4_000));
        // 1.7 LATEX.ingot() is the port's ingot_biorubber
        registerRecipe(new ComparableStack(ModItems.ingot_biorubber.get()), new CrystallizerRecipe(ModItems.ingot_rubber.get(), mixingTime).prod(0.15F), new FluidStack(Fluids.SOURGAS, 25));
        registerRecipe(new ComparableStack(ModItems.powder_sawdust.get()), new CrystallizerRecipe(ModItems.cordite.get(), mixingTime).prod(0.25F), new FluidStack(Fluids.NITROGLYCERIN, 250));
        registerRecipe(new ComparableStack(ModDungeonBlocks.rebar.get().asItem()), new CrystallizerRecipe(ModDecoContent.concrete_rebar.get(), 10), new FluidStack(Fluids.CONCRETE, 1_000));

        registerRecipe(new ComparableStack(ModAbilityToolItems.meteorite_sword_treated.get()), new CrystallizerRecipe(ModAbilityToolItems.meteorite_sword_etched.get(), baseTime));
        registerRecipe(new ComparableStack(ModItems.powder_impure_osmiridium.get()), new CrystallizerRecipe(ModItems.crystal_osmiridium.get(), baseTime), new FluidStack(Fluids.SCHRABIDIC, 1_000));

        // 1.7 ScrapType: one scrap_plastic / circuit_star_piece item per type
        for (String scrap : ModFinalConsumableItems.SCRAPS) {
            registerRecipe(new ComparableStack(ModFinalConsumableItems.flat("scrap_plastic_" + scrap)), new CrystallizerRecipe(ModFinalConsumableItems.flat("circuit_star_piece_" + scrap), baseTime));
        }

        FluidStack nitric = new FluidStack(Fluids.NITRIC_ACID, 500);
        FluidStack organic = new FluidStack(Fluids.SOLVENT, 500);
        FluidStack hiperf = new FluidStack(Fluids.RADIOSOLVENT, 500);

        int oreTime = 200;

        // port: 1.7's EnumBedrockOre loop (ore_centrifuged, ore_separated, ore_nitrocrystalline with water, sulfuric
        // acid, nitric acid, solvent and radiosolvent) has no rows: the old bedrock ore items are not ported

        int bedrock = 200;
        int washing = 100;
        for (BedrockOreType type : BedrockOreType.VALUES) {
            registerRecipe(in(Grade.BASE, type), new CrystallizerRecipe(make(Grade.BASE_WASHED, type), washing), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.BASE_ROASTED, type), new CrystallizerRecipe(make(Grade.BASE_WASHED, type), washing), new FluidStack(Fluids.WATER, 250));

            registerRecipe(in(Grade.PRIMARY, type), new CrystallizerRecipe(make(Grade.PRIMARY_SULFURIC, type), bedrock), new FluidStack(Fluids.SULFURIC_ACID, 250));
            registerRecipe(in(Grade.PRIMARY_ROASTED, type), new CrystallizerRecipe(make(Grade.PRIMARY_SULFURIC, type), bedrock), new FluidStack(Fluids.SULFURIC_ACID, 250));

            registerRecipe(in(Grade.PRIMARY, type), new CrystallizerRecipe(make(Grade.PRIMARY_SOLVENT, type), bedrock), new FluidStack(Fluids.SOLVENT, 250));
            registerRecipe(in(Grade.PRIMARY_ROASTED, type), new CrystallizerRecipe(make(Grade.PRIMARY_SOLVENT, type), bedrock), new FluidStack(Fluids.SOLVENT, 250));
            registerRecipe(in(Grade.PRIMARY_NOSULFURIC, type), new CrystallizerRecipe(make(Grade.PRIMARY_SOLVENT, type), bedrock), new FluidStack(Fluids.SOLVENT, 250));

            registerRecipe(in(Grade.PRIMARY, type), new CrystallizerRecipe(make(Grade.PRIMARY_RAD, type), bedrock), new FluidStack(Fluids.RADIOSOLVENT, 250));
            registerRecipe(in(Grade.PRIMARY_ROASTED, type), new CrystallizerRecipe(make(Grade.PRIMARY_RAD, type), bedrock), new FluidStack(Fluids.RADIOSOLVENT, 250));
            registerRecipe(in(Grade.PRIMARY_NOSULFURIC, type), new CrystallizerRecipe(make(Grade.PRIMARY_RAD, type), bedrock), new FluidStack(Fluids.RADIOSOLVENT, 250));
            registerRecipe(in(Grade.PRIMARY_NOSOLVENT, type), new CrystallizerRecipe(make(Grade.PRIMARY_RAD, type), bedrock), new FluidStack(Fluids.RADIOSOLVENT, 250));

            int sulf = 4;
            registerRecipe(in(Grade.SULFURIC_BYPRODUCT, type), new CrystallizerRecipe(make(Grade.SULFURIC_WASHED, type), washing).setReq(sulf), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.SULFURIC_ROASTED, type), new CrystallizerRecipe(make(Grade.SULFURIC_WASHED, type), washing).setReq(sulf), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.SULFURIC_ARC, type), new CrystallizerRecipe(make(Grade.SULFURIC_WASHED, type), washing).setReq(sulf), new FluidStack(Fluids.WATER, 250));

            int solv = 4;
            registerRecipe(in(Grade.SOLVENT_BYPRODUCT, type), new CrystallizerRecipe(make(Grade.SOLVENT_WASHED, type), washing).setReq(solv), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.SOLVENT_ROASTED, type), new CrystallizerRecipe(make(Grade.SOLVENT_WASHED, type), washing).setReq(solv), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.SOLVENT_ARC, type), new CrystallizerRecipe(make(Grade.SOLVENT_WASHED, type), washing).setReq(solv), new FluidStack(Fluids.WATER, 250));

            int rad = 4;
            registerRecipe(in(Grade.RAD_BYPRODUCT, type), new CrystallizerRecipe(make(Grade.RAD_WASHED, type), washing).setReq(rad), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.RAD_ROASTED, type), new CrystallizerRecipe(make(Grade.RAD_WASHED, type), washing).setReq(rad), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.RAD_ARC, type), new CrystallizerRecipe(make(Grade.RAD_WASHED, type), washing).setReq(rad), new FluidStack(Fluids.WATER, 250));

            FluidStack primary = new FluidStack(Fluids.HYDROGEN, 250);
            registerRecipe(in(Grade.PRIMARY, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_ROASTED, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_SULFURIC, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_NOSULFURIC, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_SOLVENT, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_NOSOLVENT, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_RAD, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_NORAD, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);

            FluidStack secondary = new FluidStack(Fluids.CHLORINE, 250);
            registerRecipe(in(Grade.PRIMARY, type), new CrystallizerRecipe(make(Grade.PRIMARY_SECOND, type), bedrock), secondary);
            registerRecipe(in(Grade.PRIMARY_ROASTED, type), new CrystallizerRecipe(make(Grade.PRIMARY_SECOND, type), bedrock), secondary);
            registerRecipe(in(Grade.PRIMARY_SULFURIC, type), new CrystallizerRecipe(make(Grade.PRIMARY_SECOND, type), bedrock), secondary);
            registerRecipe(in(Grade.PRIMARY_NOSULFURIC, type), new CrystallizerRecipe(make(Grade.PRIMARY_SECOND, type), bedrock), secondary);
            registerRecipe(in(Grade.PRIMARY_SOLVENT, type), new CrystallizerRecipe(make(Grade.PRIMARY_SECOND, type), bedrock), secondary);
            registerRecipe(in(Grade.PRIMARY_NOSOLVENT, type), new CrystallizerRecipe(make(Grade.PRIMARY_SECOND, type), bedrock), secondary);
            registerRecipe(in(Grade.PRIMARY_RAD, type), new CrystallizerRecipe(make(Grade.PRIMARY_SECOND, type), bedrock), secondary);
            registerRecipe(in(Grade.PRIMARY_NORAD, type), new CrystallizerRecipe(make(Grade.PRIMARY_SECOND, type), bedrock), secondary);

            registerRecipe(in(Grade.CRUMBS, type), new CrystallizerRecipe(make(Grade.BASE, type), bedrock).setReq(64), new FluidStack(Fluids.SLOP, 1000));
        }

        FluidStack[] dyes = new FluidStack[] {new FluidStack(Fluids.WOODOIL, 100), new FluidStack(Fluids.FISHOIL, 100), new FluidStack(Fluids.LIGHTOIL, 100)};
        for (FluidStack dye : dyes) {
            registerRecipe(dust("coal"),    new CrystallizerRecipe(new ItemStack(ModChemicalDyeItems.chemicalDye(EnumChemDye.BLACK), 4), mixingTime).prod(0.15F), dye);
            registerRecipe(TI.dust(),       new CrystallizerRecipe(new ItemStack(ModChemicalDyeItems.chemicalDye(EnumChemDye.WHITE), 4), mixingTime).prod(0.15F), dye);
            registerRecipe(IRON.dust(),     new CrystallizerRecipe(new ItemStack(ModChemicalDyeItems.chemicalDye(EnumChemDye.RED), 4), mixingTime).prod(0.15F), dye);
            registerRecipe(W.dust(),        new CrystallizerRecipe(new ItemStack(ModChemicalDyeItems.chemicalDye(EnumChemDye.YELLOW), 4), mixingTime).prod(0.15F), dye);
            registerRecipe(CU.dust(),       new CrystallizerRecipe(new ItemStack(ModChemicalDyeItems.chemicalDye(EnumChemDye.GREEN), 4), mixingTime).prod(0.15F), dye);
            registerRecipe(CO.dust(),       new CrystallizerRecipe(new ItemStack(ModChemicalDyeItems.chemicalDye(EnumChemDye.BLUE), 4), mixingTime).prod(0.15F), dye);
        }

        registerRecipe(new ComparableStack(ModOilItems.tar(EnumTarType.CRUDE)),     new CrystallizerRecipe(ModOilItems.tar(EnumTarType.WAX), 20),    new FluidStack(Fluids.CHLORINE, 250));
        registerRecipe(new ComparableStack(ModOilItems.tar(EnumTarType.CRACK)),     new CrystallizerRecipe(ModOilItems.tar(EnumTarType.WAX), 20),    new FluidStack(Fluids.CHLORINE, 100));
        registerRecipe(new ComparableStack(ModOilItems.tar(EnumTarType.PARAFFIN)),  new CrystallizerRecipe(ModOilItems.tar(EnumTarType.WAX), 20),    new FluidStack(Fluids.CHLORINE, 100));
        registerRecipe(new ComparableStack(ModOilItems.tar(EnumTarType.WAX)),       new CrystallizerRecipe(new ItemStack(ModItems.pellet_charged.get()), 200), new FluidStack(Fluids.IONGEL, 500));
        registerRecipe(new ComparableStack(ModOilItems.tar(EnumTarType.PARAFFIN)),  new CrystallizerRecipe(new ItemStack(ModMedicineItems.pill_red.get()), 200),  new FluidStack(Fluids.ESTRADIOL, 250));

        registerRecipe(new TagStack(KEY_SAND), new CrystallizerRecipe(net.minecraft.world.level.block.Blocks.CLAY, 20), new FluidStack(Fluids.COLLOID, 1_000));
        registerRecipe(new ComparableStack(ModFallingBlocks.sand_quartz.get().asItem()), new CrystallizerRecipe(new ItemStack(ModItems.ball_dynamite.get(), 16), 20), new FluidStack(Fluids.NITROGLYCERIN, 1_000));
        // 1.7 NETHERQUARTZ.dust() (dustNetherQuartz) is c:dusts/quartz
        registerRecipe(dust("quartz"), new CrystallizerRecipe(new ItemStack(ModItems.ball_dynamite.get(), 4), 20), new FluidStack(Fluids.NITROGLYCERIN, 250));

        /// COMPAT CERTUS QUARTZ ///
        // 1.7 copies the first crystalCertusQuartz stack, 12 of it, when there is one
        TagKey<Item> quartz = tag("gems/certus_quartz");
        registerRecipe(ore("certus_quartz"), new CrystallizerRecipe(new TagOutput(quartz, 12), baseTime).requires(quartz));

        /// COMPAT WHITE PHOSPHORUS DUST ///
        TagKey<Item> dustWhitePhosphorus = tag("dusts/white_phosphorus");
        registerRecipe(dust("white_phosphorus"), new CrystallizerRecipe(new ItemStack(ModItems.ingot_phosphorus.get()), utilityTime).requires(dustWhitePhosphorus), new FluidStack(Fluids.AROMATICS, 50));

        /// COMPAT CINNABAR DUST ///
        TagKey<Item> dustCinnabar = CINNABAR.tag(MaterialShapes.DUST);
        registerRecipe(CINNABAR.dust(), new CrystallizerRecipe(new ItemStack(ModItems.cinnabar.get()), utilityTime).requires(dustCinnabar), new FluidStack(Fluids.PEROXIDE, 50));

        registerRecipe(new ComparableStack(ModFallingBlocks.moon_turf.get().asItem()), new CrystallizerRecipe(ModIntermediateItems.chunk(EnumChunkType.MOONSTONE, 1), 1200).setReq(16));
    }

    private static ComparableStack in(Grade grade, BedrockOreType type) {
        return new ComparableStack(ModBedrockOreItems.get(grade, type));
    }

    private static ItemStack make(Grade grade, BedrockOreType type) {
        return ModBedrockOreItems.make(grade, type);
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Ids.of("c", path));
    }

    /** 1.7 {@code new OreDictStack("ore<Name>")} for a name with no port material alias. */
    private static AStack ore(String name) {
        return new TagStack(tag("ores/" + name));
    }

    /** 1.7 {@code new OreDictStack("dust<Name>")} for a name with no port material alias. */
    private static AStack dust(String name) {
        return new TagStack(tag("dusts/" + name));
    }

    /** 1.7 {@code new OreDictStack("block<Name>")}. */
    private static AStack storageBlock(String name) {
        return new TagStack(tag("storage_blocks/" + name));
    }

    /** 1.7 {@code registerRecipe(input, recipe)}: 500 mB of hydrogen peroxide. */
    private void registerRecipe(AStack input, CrystallizerRecipe recipe) {
        registerRecipe(input, recipe, new FluidStack(Fluids.PEROXIDE, 500));
    }

    /**
     * 1.7 {@code registerRecipe(input, recipe, acid)}; {@code setReq} is the input's count. The id is the input (the item,
     * its namespace first unless {@code hbm}, or {@code tag_} and the tag, its namespace first unless {@code c}, slashes
     * as underscores) and the acid.
     */
    private void registerRecipe(AStack input, CrystallizerRecipe recipe, FluidStack stack) {
        AStack counted = recipe.itemAmount == 1 ? input : input.count(recipe.itemAmount);
        register(id(input) + "_" + stack.type.getKey(), new Row(counted, new FluidStack(stack.type, stack.fill), recipe.output,
                recipe.duration, recipe.productivity, Optional.ofNullable(recipe.requires), nextOrder()));
    }

    private static String id(AStack input) {
        if (input instanceof TagStack tag) {
            var key = tag.tag.location();
            return "tag_" + (key.getNamespace().equals("c") ? "" : key.getNamespace() + "_") + key.getPath().replace('/', '_');
        }
        var key = BuiltInRegistries.ITEM.getKey(((ComparableStack) input).item);
        return (key.getNamespace().equals("hbm") ? "" : key.getNamespace() + "_") + key.getPath();
    }

    /** 1.7 {@code CrystallizerRecipe}: an output, a duration, {@code prod} and {@code setReq}. */
    private static final class CrystallizerRecipe {
        final Output output;
        final int duration;
        int itemAmount = 1;
        float productivity = 0F;
        @Nullable TagKey<Item> requires;

        CrystallizerRecipe(Block output, int duration) { this(new ItemStack(output), duration); }
        CrystallizerRecipe(Item output, int duration) { this(new ItemStack(output), duration); }

        /** A scrap stack (1.7 {@code ItemScraps.create}) is written as its material. */
        CrystallizerRecipe(ItemStack output, int duration) {
            this(output.getItem() == ModMachineItems.scraps.get() ? new ScrapsOutput(ItemScraps.getMats(output))
                    : new ItemOutput(ItemOut.of(output)), duration);
        }

        CrystallizerRecipe(Output output, int duration) {
            this.output = output;
            this.duration = duration;
        }

        CrystallizerRecipe setReq(int amount) {
            this.itemAmount = amount;
            return this;
        }

        CrystallizerRecipe prod(float productivity) {
            this.productivity = productivity;
            return this;
        }

        /** The 1.7 {@code OreDictionary.getOres(name)} check that guarded the row. */
        CrystallizerRecipe requires(TagKey<Item> tag) {
            this.requires = tag;
            return this;
        }
    }
}
