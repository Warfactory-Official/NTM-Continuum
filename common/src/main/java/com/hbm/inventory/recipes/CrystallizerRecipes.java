package com.hbm.inventory.recipes;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
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
 * {@link AStack#matchesRecipe} scan, which handles tag inputs uniformly (~40 recipes, trivial cost).
 *
 * <p><b>What's ported:</b> the ore → crystal chain (via the hand-authored {@code c:ores/*} tags),
 * plus every utility/mixing recipe whose items exist. <b>Deferred with their content:</b>
 * coal/uranium crystals (crystal_coal / ore_uranium unported), cement, mercury, graphite block,
 * semtex, schraranium, the chemical dyes, all oil/tar/pill recipes, the ~90 bedrock-ore grades,
 * gravel/moon-turf/rebar inputs, and the old mod-compat recipes.
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
        peroxide(IRON.ore(),           stack(ModItems.crystal_iron.get()),       baseTime, 0.05F);
        peroxide(GOLD.ore(),           stack(ModItems.crystal_gold.get()),       baseTime, 0.05F);
        peroxide(oreTag("redstone"),   stack(ModItems.crystal_redstone.get()),   baseTime, 0.05F);
        peroxide(oreTag("lapis"),      stack(ModItems.crystal_lapis.get()),      baseTime, 0.05F);
        peroxide(oreTag("diamond"),    stack(ModItems.crystal_diamond.get()),    baseTime, 0.05F);
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

        // --- utility / refining ---
        peroxide(new TagStack(ItemTags.SAND),                    stack(ModItems.ingot_fiberglass.get()),          utilityTime, 0.15F);
        add(SI.ingot(),                stack(Items.QUARTZ, 2),                   utilityTime, 0.10F, Fluids.OXYGEN, 250);
        add(BORAX.dust(),              stack(ModItems.powder_boron_tiny.get(), 3), baseTime,  0.25F, Fluids.SULFURIC_ACID, 500);
        peroxide(item(Blocks.COBBLESTONE.asItem()),              stack(ModBlocks.reinforced_stone.get().asItem()), utilityTime, 0F);
        peroxide(item(Items.ROTTEN_FLESH),                       stack(Items.LEATHER),                            utilityTime, 0.25F);
        peroxide(item(ModBlocks.stone_gneiss.get().asItem()),    stack(ModItems.powder_lithium.get()),            utilityTime, 0.25F);
        add(item(Items.BONE_MEAL),     stack(Items.SLIME_BALL, 4),  mixingTime, 0F, Fluids.SULFURIC_ACID, 250);
        add(item(Items.BONE),          stack(Items.SLIME_BALL, 16), mixingTime, 0F, Fluids.SULFURIC_ACID, 1_000);
        add(item(ModItems.scrap_oil.get()).count(16), stack(ModItems.nugget_arsenic.get()), utilityTime, 0.30F, Fluids.RADIOSOLVENT, 100);
        peroxide(dustTag("diamond"),   stack(Items.DIAMOND),                     utilityTime, 0F);
        peroxide(dustTag("emerald"),   stack(Items.EMERALD),                     utilityTime, 0F);
        peroxide(dustTag("lapis"),     stack(Items.LAPIS_LAZULI),                utilityTime, 0F);
        peroxide(item(ModItems.powder_desh_ready.get()), stack(ModItems.ingot_desh.get()),         baseTime,    0F);
        peroxide(item(ModItems.powder_meteorite.get()),  stack(ModItems.fragment_meteorite.get()), utilityTime, 0F);
        add(CD.dust(),                 stack(ModItems.ingot_rubber.get()), utilityTime, 0F,    Fluids.FISHOIL, 250);
        add(dustTag("sawdust"),        stack(ModItems.cordite.get()),      mixingTime,  0.25F, Fluids.NITROGLYCERIN, 250);
        add(item(ModItems.powder_impure_osmiridium.get()), stack(ModItems.crystal_osmiridium.get()), baseTime, 0F, Fluids.SCHRABIDIC, 1_000);
        add(new TagStack(ItemTags.SAND), stack(Blocks.CLAY.asItem()),      mixingTime,  0F,    Fluids.COLLOID, 1_000);
        add(dustTag("quartz"),         stack(ModItems.ball_dynamite.get(), 4), mixingTime, 0F, Fluids.NITROGLYCERIN, 250);
    }

    /** The recipe matching {@code stack} under the given tank fluid, or null. */
    public static CrystallizerRecipe getOutput(ItemStack stack, FluidType type) {
        if (stack == null || stack.isEmpty()) return null;
        if (!initialized) register();
        for (CrystallizerRecipe r : recipes) {
            if (r.fluid == type && r.input.matchesRecipe(stack, true)) return r;
        }
        return null;
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
        return new TagStack(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores/" + name)));
    }

    /** A {@code c:dusts/<name>} tag input (these are all generated by the material-tag system). */
    private static TagStack dustTag(String name) {
        return new TagStack(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dusts/" + name)));
    }
}
