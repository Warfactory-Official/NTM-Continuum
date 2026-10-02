package com.hbm.inventory.recipes;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.ModChemicalDyeItems;
import com.hbm.items.machine.ItemChemicalDye.EnumChemDye;
import com.hbm.items.machine.ItemScraps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

/**
 * Ore acidizer (crystallizer) recipes — a faithful, feasibility-limited port of the 1.12.2
 * {@code CrystallizerRecipes}. A recipe is one input (item / c:-tag, possibly requiring a stack of
 * them) + an acid (the machine tank's current fluid type selects among recipes sharing an input) →
 * one output stack. The old {@code HashMap<Pair<Object, FluidType>>} keying is replaced by a linear
 * {@link AStack#matchesRecipe} scan, which handles tag inputs uniformly (61 recipes, trivial cost).
 *
 * <p><b>What's ported:</b> every 1.7 default recipe, including mercury (1.7's {@code ingot_mercury} is
 * registered as {@code nugget_mercury}), the mustard-willow cadmium leach and the three external-mod
 * compatibility recipes, which — as in 1.7 — only register when another mod fills their tag. Not ported:
 * the legacy {@code ore_centrifuged/separated/nitrocrystalline} bedrock chain (those creative-tab-less
 * items are not in the port) and plastic scrap to circuit-star pieces (the port's {@code scrap_plastic}
 * has no {@code ScrapType} subtypes to map piece by piece).
 *
 * <p>{@code productivity} is carried for parity but unused: in the 1.12.2 tile the free-item chance
 * came only from EFFECT machine upgrades (unported), never from the recipe field.
 */
public final class CrystallizerRecipes {

    private CrystallizerRecipes() {}

    public static final List<CrystallizerRecipe> recipes = new ArrayList<>();
    private static boolean initialized = false;

    /** One acidizer recipe. {@code input.stacksize} is the required item count (old {@code setReq}). */
    public static final class CrystallizerRecipe {
        public final AStack input;
        public final FluidType fluid;
        public final int acidAmount;
        public final int duration;
        public final float productivity; // parity only — the old TE never read it (EFFECT upgrades did)
        public final ItemStack output;

        CrystallizerRecipe(AStack input, FluidType fluid, int acidAmount, int duration, float productivity, ItemStack output) {
            this.input = input;
            this.fluid = fluid;
            this.acidAmount = acidAmount;
            this.duration = duration;
            this.productivity = productivity;
            this.output = output;
        }
    }

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        final int baseTime = 600;
        final int utilityTime = 100;
        final int mixingTime = 20;

        // --- ore → crystal (600t, prod 0.05; peroxide unless noted sulfuric — matches 1.12.2) ---
        peroxide(oreTag("coal"),        stack(ModFuelItems.crystal_coal.get()),   baseTime, 0.05F);
        peroxide(IRON.ore(),           stack(ModItems.crystal_iron.get()),       baseTime, 0.05F);
        peroxide(GOLD.ore(),           stack(ModItems.crystal_gold.get()),       baseTime, 0.05F);
        peroxide(oreTag("redstone"),   stack(ModItems.crystal_redstone.get()),   baseTime, 0.05F);
        peroxide(oreTag("lapis"),      stack(ModItems.crystal_lapis.get()),      baseTime, 0.05F);
        peroxide(oreTag("diamond"),    stack(ModItems.crystal_diamond.get()),    baseTime, 0.05F);
        sulfuric(oreTag("uranium"),     stack(ModItems.crystal_uranium.get()),    baseTime, 0.05F);
        sulfuric(TH232.ore(),          stack(ModItems.crystal_thorium.get()),    baseTime, 0.05F);
        sulfuric(PU.ore(),             stack(ModItems.crystal_plutonium.get()),  baseTime, 0.05F);
        sulfuric(TI.ore(),             stack(ModItems.crystal_titanium.get()),   baseTime, 0.05F);
        peroxide(S.ore(),              stack(ModItems.crystal_sulfur.get()),     baseTime, 0.05F);
        peroxide(oreTag("niter"),      stack(ModItems.crystal_niter.get()),      baseTime, 0.05F);
        peroxide(CU.ore(),             stack(ModItems.crystal_copper.get()),     baseTime, 0.05F);
        sulfuric(W.ore(),              stack(ModItems.crystal_tungsten.get()),   baseTime, 0.05F);
        peroxide(AL.ore(),             stack(ModItems.crystal_aluminium.get()),  baseTime, 0.05F);
        peroxide(F.ore(),              stack(ModItems.crystal_fluorite.get()),   baseTime, 0.05F);
        peroxide(BE.ore(),             stack(ModItems.crystal_beryllium.get()),  baseTime, 0.05F);
        peroxide(PB.ore(),             stack(ModItems.crystal_lead.get()),       baseTime, 0.05F);
        sulfuric(SA326.ore(),          stack(ModItems.crystal_schrabidium.get()),baseTime, 0.05F);
        sulfuric(LI.ore(),             stack(ModItems.crystal_lithium.get()),    baseTime, 0.05F);
        sulfuric(CO.ore(),             stack(ModItems.crystal_cobalt.get()),     baseTime, 0.05F);
        sulfuric(oreTag("rare"),       stack(ModItems.crystal_rare.get()),       baseTime, 0.05F);
        peroxide(oreTag("cinnabar"),   stack(ModItems.crystal_cinnabar.get()),   baseTime, 0.05F);
        peroxide(item(ModBlocks.ore_nether_fire.get().asItem()), stack(ModItems.crystal_phosphorus.get()), baseTime, 0.05F);
        sulfuric(item(ModBlocks.ore_tikite.get().asItem()),      stack(ModItems.crystal_trixite.get()),    baseTime, 0.05F);
        peroxide(item(ModFallingBlocks.gravel_diamond.get().asItem()), stack(ModItems.crystal_diamond.get()), baseTime, 0.05F);
        peroxide(SRN.ingot(), stack(ModItems.crystal_schraranium.get()), baseTime, 0.05F);

        // --- utility / refining ---
        peroxide(new TagStack(ItemTags.SAND),                    stack(ModItems.ingot_fiberglass.get()),          utilityTime, 0.15F);
        add(SI.ingot(),                stack(Items.QUARTZ, 2),                   utilityTime, 0.10F, Fluids.OXYGEN, 250);
        add(CA.dust(),                 stack(ModFuelItems.powder_cement.get(), 8), utilityTime, 0.10F, Fluids.REDMUD, 75);
        // 1.7 ModItems.ingot_mercury, registered as "nugget_mercury". crystalCinnabar is the cinnabar gem
        // item in 1.7 (CINNABAR.crystal(cinnebar)); the port's c:crystals/cinnabar holds the acidizer crystal
        // instead, so the gem tag carries the 1.7 meaning here.
        peroxide(storageTag("redstone"), stack(ModItems.nugget_mercury.get()), baseTime, 0.25F);
        peroxide(CINNABAR.gem(),       stack(ModItems.nugget_mercury.get(), 3), baseTime, 0.25F);
        add(BORAX.dust(),              stack(ModItems.powder_boron_tiny.get(), 3), baseTime,  0.25F, Fluids.SULFURIC_ACID, 500);
        peroxide(storageTag("coal"),     stack(ModRBMKBlocks.block_graphite.get().asItem()), baseTime, 0F);
        peroxide(item(Blocks.COBBLESTONE.asItem()),              stack(ModBlocks.reinforced_stone.get().asItem()), utilityTime, 0F);
        peroxide(item(ModFallingBlocks.gravel_obsidian.get().asItem()), stack(ModBlocks.brick_obsidian.get().asItem()), utilityTime, 0F);
        peroxide(item(ModFuelItems.coal_infernal.get()),          stack(ModFuelItems.solid_fuel.get()), utilityTime, 0F);
        peroxide(item(Items.ROTTEN_FLESH),                       stack(Items.LEATHER),                            utilityTime, 0.25F);
        peroxide(item(ModBlocks.stone_gneiss.get().asItem()),    stack(ModItems.powder_lithium.get()),            utilityTime, 0.25F);
        add(item(Items.BONE_MEAL),     stack(Items.SLIME_BALL, 4),  mixingTime, 0F, Fluids.SULFURIC_ACID, 250);
        add(item(Items.BONE),          stack(Items.SLIME_BALL, 16), mixingTime, 0F, Fluids.SULFURIC_ACID, 1_000);
        add(item(com.hbm.blocks.ModDungeonBlocks.rebar.get().asItem()), stack(com.hbm.blocks.ModDecoContent.concrete_rebar.get().asItem()), 10, 0F, Fluids.CONCRETE, 1_000);
        add(item(com.hbm.items.ModTerrainItems.plant_item_mustardwillow.get()).count(10),
                stack(ModItems.powder_cadmium.get()), 100, 0F, Fluids.RADIOSOLVENT, 250);
        add(item(ModItems.scrap_oil.get()).count(16), stack(ModItems.nugget_arsenic.get()), utilityTime, 0.30F, Fluids.RADIOSOLVENT, 100);
        add(MALACHITE.ingot(),
                ItemScraps.create(new Mats.MaterialStack(Mats.MAT_COPPER, MaterialShapes.INGOT.quantity)),
                300, 0.10F, Fluids.SULFURIC_ACID, 250);
        add(item(ModIngredientItems.ash(EnumAshType.FULLERENE)).count(4), stack(ModItems.ingot_cft.get()),
                baseTime, 0.10F, Fluids.XYLENE, 1_000);
        peroxide(dustTag("diamond"),   stack(Items.DIAMOND),                     utilityTime, 0F);
        peroxide(dustTag("emerald"),   stack(Items.EMERALD),                     utilityTime, 0F);
        peroxide(dustTag("lapis"),     stack(Items.LAPIS_LAZULI),                utilityTime, 0F);
        peroxide(item(ModItems.powder_semtex_mix.get()), stack(ModFoodItems.ingot_semtex.get()), baseTime, 0F);
        peroxide(item(ModItems.powder_desh_ready.get()), stack(ModItems.ingot_desh.get()),         baseTime,    0F);
        peroxide(item(ModItems.powder_meteorite.get()),  stack(ModItems.fragment_meteorite.get()), utilityTime, 0F);
        AbilityToolMachineRecipes.crystallizer(recipes, baseTime); // treated -> etched meteorite sword
        BedrockOreChainRecipes.crystallizer(recipes); // bedrock ore washing / acid grades
        add(CD.dust(),                 stack(ModItems.ingot_rubber.get(), 16), utilityTime, 0F, Fluids.FISHOIL, 4_000);
        add(item(ModItems.ingot_biorubber.get()), stack(ModItems.ingot_rubber.get()), mixingTime, 0.15F,
                Fluids.SOURGAS, 25);
        add(dustTag("sawdust"),        stack(ModItems.cordite.get()),      mixingTime,  0.25F, Fluids.NITROGLYCERIN, 250);
        add(item(ModItems.powder_impure_osmiridium.get()), stack(ModItems.crystal_osmiridium.get()), baseTime, 0F, Fluids.SCHRABIDIC, 1_000);
        add(new TagStack(ItemTags.SAND), stack(Blocks.CLAY.asItem()),      mixingTime,  0F,    Fluids.COLLOID, 1_000);
        add(item(ModFallingBlocks.sand_quartz.get().asItem()), stack(ModItems.ball_dynamite.get(), 16),
                mixingTime, 0F, Fluids.NITROGLYCERIN, 1_000);
        add(dustTag("quartz"),         stack(ModItems.ball_dynamite.get(), 4), mixingTime, 0F, Fluids.NITROGLYCERIN, 250);

        // --- oil tar / wax reactions (all six metadata variants are separate registered items) ---
        add(item(ModOilItems.tar(EnumTarType.CRUDE)), stack(ModOilItems.tar(EnumTarType.WAX)),
                mixingTime, 0F, Fluids.CHLORINE, 250);
        add(item(ModOilItems.tar(EnumTarType.CRACK)), stack(ModOilItems.tar(EnumTarType.WAX)),
                mixingTime, 0F, Fluids.CHLORINE, 100);
        add(item(ModOilItems.tar(EnumTarType.PARAFFIN)), stack(ModOilItems.tar(EnumTarType.WAX)),
                mixingTime, 0F, Fluids.CHLORINE, 100);
        add(item(ModOilItems.tar(EnumTarType.PARAFFIN)), stack(ModMedicineItems.pill_red.get()),
                200, 0F, Fluids.ESTRADIOL, 250);
        add(item(ModOilItems.tar(EnumTarType.WAX)), stack(ModItems.pellet_charged.get()),
                200, 0F, Fluids.IONGEL, 500);
        add(item(ModFallingBlocks.moon_turf.get().asItem()).count(16),
                stack(ModIntermediateItems.chunk(EnumChunkType.MOONSTONE)), 1_200, 0F, Fluids.PEROXIDE, 500);

        compat(baseTime, utilityTime);

        // --- chemical dyes (the three old oil variants each unlock the same six colours) ---
        for (FluidType oil : List.of(Fluids.WOODOIL, Fluids.FISHOIL, Fluids.LIGHTOIL)) {
            add(dustTag("coal"), stack(ModChemicalDyeItems.chemicalDye(EnumChemDye.BLACK), 4),
                    mixingTime, 0.15F, oil, 100);
            add(TI.dust(), stack(ModChemicalDyeItems.chemicalDye(EnumChemDye.WHITE), 4),
                    mixingTime, 0.15F, oil, 100);
            add(IRON.dust(), stack(ModChemicalDyeItems.chemicalDye(EnumChemDye.RED), 4),
                    mixingTime, 0.15F, oil, 100);
            add(W.dust(), stack(ModChemicalDyeItems.chemicalDye(EnumChemDye.YELLOW), 4),
                    mixingTime, 0.15F, oil, 100);
            add(CU.dust(), stack(ModChemicalDyeItems.chemicalDye(EnumChemDye.GREEN), 4),
                    mixingTime, 0.15F, oil, 100);
            add(CO.dust(), stack(ModChemicalDyeItems.chemicalDye(EnumChemDye.BLUE), 4),
                    mixingTime, 0.15F, oil, 100);
        }
    }

    /**
     * 1.7's three {@code OreDictionary.getOres(...)} compat recipes, each registered only when the tag has
     * an item (HBM supplies none of them). Registration is lazy (machine / JEI first use), after tags bind.
     */
    private static void compat(int baseTime, int utilityTime) {
        Item certus = firstInTag(cTag("gems/certus_quartz"));
        if (certus != null) peroxide(new TagStack(cTag("ores/certus_quartz")), stack(certus, 12), baseTime, 0F);
        if (firstInTag(cTag("dusts/white_phosphorus")) != null) {
            add(new TagStack(cTag("dusts/white_phosphorus")), stack(ModItems.ingot_phosphorus.get()),
                    utilityTime, 0F, Fluids.AROMATICS, 50);
        }
        if (firstInTag(CINNABAR.tag(MaterialShapes.DUST)) != null) {
            add(CINNABAR.dust(), stack(ModItems.cinnabar.get()), utilityTime, 0F, Fluids.PEROXIDE, 50);
        }
    }

    private static TagKey<Item> cTag(String path) {
        return TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", path));
    }

    private static Item firstInTag(TagKey<Item> tag) {
        for (net.minecraft.core.Holder<Item> holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            return holder.value();
        }
        return null;
    }

    /** The recipe matching {@code stack} under the given tank fluid, or null. */
    public static CrystallizerRecipe getOutput(ItemStack stack, FluidType type) {
        if (stack == null || stack.isEmpty()) return null;
        if (!initialized) register();
        CrystallizerRecipe tagMatch = null;
        for (CrystallizerRecipe r : recipes) {
            if (r.fluid == type && r.input.matchesRecipe(stack, true)) {
                // The old HashMap lookup tried the exact ComparableStack key before ore-dict keys.
                if (r.input instanceof ComparableStack) return r;
                tagMatch = r;
            }
        }
        return tagMatch;
    }

    /** Whether {@code stack} is the input of ANY recipe, regardless of the acid (for slot placement). */
    public static boolean isAnyRecipeInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!initialized) register();
        for (CrystallizerRecipe r : recipes) {
            if (r.input.matchesRecipe(stack, true)) return true;
        }
        return false;
    }

    // --- registration helpers (peroxide 500 mB is the old default acid) ---

    private static void peroxide(AStack input, ItemStack output, int duration, float productivity) {
        add(input, output, duration, productivity, Fluids.PEROXIDE, 500);
    }

    private static void sulfuric(AStack input, ItemStack output, int duration, float productivity) {
        add(input, output, duration, productivity, Fluids.SULFURIC_ACID, 500);
    }

    private static void add(AStack input, ItemStack output, int duration, float productivity, FluidType fluid, int acidAmount) {
        recipes.add(new CrystallizerRecipe(input, fluid, acidAmount, duration, productivity, output));
    }

    private static ComparableStack item(Item item) {
        return new ComparableStack(item, 1);
    }

    private static ItemStack stack(Item item) {
        return new ItemStack(item);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    /** A {@code c:ores/<name>} tag input — the hand-authored ore tags backing the ore→crystal chain. */
    private static TagStack oreTag(String name) {
        return new TagStack(TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", "ores/" + name)));
    }

    /** A {@code c:dusts/<name>} tag input (these are all generated by the material-tag system). */
    private static TagStack dustTag(String name) {
        return new TagStack(TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", "dusts/" + name)));
    }

    private static TagStack storageTag(String name) {
        return new TagStack(TagKey.create(Registries.ITEM,
                dev.golem.core.Ids.of("c", "storage_blocks/" + name)));
    }
}
