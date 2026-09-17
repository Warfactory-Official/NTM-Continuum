package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.machine.ItemFluidTank;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.hbm.inventory.OreDictManager.ANY_PLASTIC;
import static com.hbm.inventory.OreDictManager.CU;
import static com.hbm.inventory.OreDictManager.FERRO;
import static com.hbm.inventory.OreDictManager.PB;
import static com.hbm.inventory.OreDictManager.STEEL;
import static com.hbm.inventory.OreDictManager.WEAPONSTEEL;
import static com.hbm.inventory.OreDictManager.W;
import static com.hbm.inventory.OreDictManager.U238;
import static com.hbm.inventory.OreDictManager.PU239;
import static com.hbm.inventory.OreDictManager.P_RED;

/** The complete positional Ammo Press catalogue from original-hbm, filtered only by real dependencies. */
public final class AmmoPressRecipes {

    /** Number of positional rows in original-hbm's default Ammo Press table. */
    public static final int SOURCE_RECIPE_COUNT = 89;

    private static final TagKey<Item> ANY_SMOKELESS = TagKey.create(Registries.ITEM,
            Ids.of("c", "dusts/any_smokeless"));
    private static final TagKey<Item> ANY_HIGH_EXPLOSIVE = TagKey.create(Registries.ITEM,
            Ids.of("c", "ingots/any_highexplosive"));

    public static final List<AmmoPressRecipe> recipes = new ArrayList<>();
    private static boolean initialized;
    private static final AStack MISSING = new AStack() {
        @Override public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) { return false; }
        @Override public List<ItemStack> getDisplayStacks() { return List.of(); }
    };

    private AmmoPressRecipes() {}

    public static List<AmmoPressRecipe> getRecipes() {
        register();
        return Collections.unmodifiableList(recipes);
    }

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        AStack lead = PB.ingot();
        AStack nugget = PB.nugget();
        AStack flechette = PB.bolt();
        AStack steel = STEEL.ingot();
        AStack weaponSteel = WEAPONSTEEL.ingot();
        AStack copper = CU.ingot();
        AStack plastic = ANY_PLASTIC.ingot();
        AStack uranium = U238.ingot();
        AStack ferro = FERRO.ingot();
        AStack niobium = com.hbm.inventory.OreDictManager.NB.ingot();
        AStack smokeless = smokeless();
        AStack highExplosive = new TagStack(ANY_HIGH_EXPLOSIVE);
        AStack whitePhosphorus = exact("ingot_phosphorus");
        AStack redPhosphorus = P_RED.dust();
        AStack pipe = STEEL.pipe();
        AStack smoke = exact(Items.GUNPOWDER);
        AStack rocket = exact("rocket_fuel");
        AStack cSmall = casing(EnumCasingType.SMALL);
        AStack cBig = casing(EnumCasingType.LARGE);
        AStack sSmall = casing(EnumCasingType.SMALL_STEEL);
        AStack sBig = casing(EnumCasingType.LARGE_STEEL);
        AStack bpShell = casing(EnumCasingType.SHOTSHELL);
        AStack pShell = casing(EnumCasingType.BUCKSHOT);
        AStack sShell = casing(EnumCasingType.BUCKSHOT_ADVANCED);

        add(standard("m357_bp"), 16, null, count(lead, 2), null, null, smoke, null, null, cSmall, null);
        add(standard("m357_sp"), 8, null, lead, null, null, smokeless, null, null, cSmall, null);
        add(standard("m357_fmj"), 8, null, steel, null, null, smokeless, null, null, cSmall, null);
        add(standard("m357_jhp"), 8, plastic, copper, null, null, smokeless, null, null, cSmall, null);
        add(standard("m357_ap"), 8, null, weaponSteel, null, null, count(smokeless, 2), null, null, sSmall, null);
        add(standard("m357_express"), 8, null, steel, null, null, count(smokeless, 3), null, null, cSmall, null);

        add(standard("m44_bp"), 12, null, count(lead, 2), null, null, smoke, null, null, cSmall, null);
        add(standard("m44_sp"), 6, null, lead, null, null, smokeless, null, null, cSmall, null);
        add(standard("m44_fmj"), 6, null, steel, null, null, smokeless, null, null, cSmall, null);
        add(standard("m44_jhp"), 6, plastic, copper, null, null, smokeless, null, null, cSmall, null);
        add(standard("m44_ap"), 6, null, weaponSteel, null, null, count(smokeless, 2), null, null, sSmall, null);
        add(standard("m44_express"), 6, null, steel, null, null, count(smokeless, 3), null, null, cSmall, null);

        add(standard("p22_sp"), 24, null, lead, null, null, smokeless, null, null, cSmall, null);
        add(standard("p22_fmj"), 24, null, steel, null, null, smokeless, null, null, cSmall, null);
        add(standard("p22_jhp"), 24, plastic, copper, null, null, smokeless, null, null, cSmall, null);
        add(standard("p22_ap"), 24, null, weaponSteel, null, null, count(smokeless, 2), null, null, sSmall, null);

        add(standard("p9_sp"), 12, null, lead, null, null, smokeless, null, null, cSmall, null);
        add(standard("p9_fmj"), 12, null, steel, null, null, smokeless, null, null, cSmall, null);
        add(standard("p9_jhp"), 12, plastic, copper, null, null, smokeless, null, null, cSmall, null);
        add(standard("p9_ap"), 12, null, weaponSteel, null, null, count(smokeless, 2), null, null, sSmall, null);

        add(standard("p45_sp"), 8, null, lead, null, null, smokeless, null, null, cSmall, null);
        add(standard("p45_fmj"), 8, null, steel, null, null, smokeless, null, null, cSmall, null);
        add(standard("p45_jhp"), 8, plastic, copper, null, null, smokeless, null, null, cSmall, null);
        add(standard("p45_ap"), 8, null, weaponSteel, null, null, count(smokeless, 2), null, null, sSmall, null);
        add(standard("p45_du"), 8, null, uranium, null, null, count(smokeless, 2), null, null, sSmall, null);

        add(standard("r556_sp"), 16, null, count(lead, 2), null, null, count(smokeless, 2), null, null, count(cSmall, 2), null);
        add(standard("r556_fmj"), 16, null, count(steel, 2), null, null, count(smokeless, 2), null, null, count(cSmall, 2), null);
        add(standard("r556_jhp"), 16, plastic, count(copper, 2), null, null, count(smokeless, 2), null, null, count(cSmall, 2), null);
        add(standard("r556_ap"), 16, null, count(weaponSteel, 2), null, null, count(smokeless, 4), null, null, count(sSmall, 2), null);

        add(standard("r762_sp"), 12, null, count(lead, 2), null, null, count(smokeless, 2), null, null, count(cSmall, 2), null);
        add(standard("r762_fmj"), 12, null, count(steel, 2), null, null, count(smokeless, 2), null, null, count(cSmall, 2), null);
        add(standard("r762_jhp"), 12, plastic, count(copper, 2), null, null, count(smokeless, 2), null, null, count(cSmall, 2), null);
        add(standard("r762_ap"), 12, null, count(weaponSteel, 2), null, null, count(smokeless, 4), null, null, count(sSmall, 2), null);
        add(standard("r762_du"), 12, null, count(uranium, 2), null, null, count(smokeless, 4), null, null, count(sSmall, 2), null);
        add(standard("r762_he"), 12, highExplosive, ferro, null, null, count(smokeless, 4), null, null, count(sSmall, 2), null);

        add(standard("bmg50_sp"), 12, null, count(lead, 2), null, null, count(smokeless, 3), null, null, cBig, null);
        add(standard("bmg50_fmj"), 12, null, count(steel, 2), null, null, count(smokeless, 3), null, null, cBig, null);
        add(standard("bmg50_jhp"), 12, plastic, count(copper, 2), null, null, count(smokeless, 3), null, null, cBig, null);
        add(standard("bmg50_ap"), 12, null, count(weaponSteel, 2), null, null, count(smokeless, 6), null, null, sBig, null);
        add(standard("bmg50_du"), 12, null, count(uranium, 2), null, null, count(smokeless, 6), null, null, sBig, null);
        add(standard("bmg50_he"), 12, highExplosive, ferro, null, null, count(smokeless, 6), null, null, sBig, null);

        add(standard("g12_bp"), 6, null, count(nugget, 6), null, null, smoke, null, null, bpShell, null);
        add(standard("g12_bp_magnum"), 6, null, count(nugget, 8), null, null, smoke, null, null, bpShell, null);
        add(standard("g12_bp_slug"), 6, null, lead, null, null, smoke, null, null, bpShell, null);
        add(standard("g12"), 6, null, count(nugget, 6), null, null, smokeless, null, null, pShell, null);
        add(standard("g12_slug"), 6, null, lead, null, null, smokeless, null, null, pShell, null);
        add(standard("g12_flechette"), 6, null, count(flechette, 12), null, null, smokeless, null, null, pShell, null);
        add(standard("g12_magnum"), 6, null, count(nugget, 8), null, null, smokeless, null, null, sShell, null);
        add(standard("g12_explosive"), 6, null, highExplosive, null, null, smokeless, null, null, sShell, null);
        add(standard("g12_phosphorus"), 6, null, whitePhosphorus, null, null, smokeless, null, null, sShell, null);

        add(standard("g10"), 4, null, count(nugget, 8), null, null, count(smokeless, 2), null, null, sShell, null);
        add(standard("g10_shrapnel"), 4, plastic, count(nugget, 8), null, null, count(smokeless, 2), null, null, sShell, null);
        add(standard("g10_du"), 4, null, uranium, null, null, count(smokeless, 2), null, null, sShell, null);
        add(standard("g10_slug"), 4, null, lead, null, null, count(smokeless, 2), null, null, sShell, null);
        add(standard("g10_explosive"), 4, highExplosive, ferro, null, null, count(smokeless, 2), null, null, sShell, null);

        add(standard("g26_flare"), 4, null, redPhosphorus, null, null, smokeless, null, null, cBig, null);

        add(standard("g40_he"), 4, null, exact("ball_dynamite"), null, null, smokeless, null, null, cBig, null);
        add(standard("g40_heat"), 4, CU.plate(), highExplosive, null, null, smokeless, null, null, cBig, null);
        add(standard("g40_demo"), 4, null, count(highExplosive, 2), null, null, smokeless, null, null, cBig, null);
        add(standard("g40_inc"), 4, fluid("DIESEL", 1000), exact("ball_dynamite"), null, null, smokeless, null, null, cBig, null);
        add(standard("g40_phosphorus"), 4, whitePhosphorus, highExplosive, null, null, smokeless, null, null, cBig, null);

        add(standard("rocket_he"), 2, null, exact("ball_dynamite"), null, null, cBig, null, null, count(smokeless, 3), null);
        add(standard("rocket_he"), 2, null, exact("ball_dynamite"), null, null, cBig, null, null, rocket, null);
        add(standard("rocket_heat"), 2, CU.plate(), highExplosive, null, null, cBig, null, null, count(smokeless, 3), null);
        add(standard("rocket_heat"), 2, CU.plate(), highExplosive, null, null, cBig, null, null, rocket, null);
        add(standard("rocket_demo"), 2, null, count(highExplosive, 2), null, null, cBig, null, null, count(smokeless, 3), null);
        add(standard("rocket_demo"), 2, null, count(highExplosive, 2), null, null, cBig, null, null, rocket, null);
        add(standard("rocket_inc"), 2, fluid("DIESEL", 1000), exact("ball_dynamite"), null, null, cBig, null, null, count(smokeless, 3), null);
        add(standard("rocket_inc"), 2, fluid("DIESEL", 1000), exact("ball_dynamite"), null, null, cBig, null, null, rocket, null);
        add(standard("rocket_phosphorus"), 2, whitePhosphorus, highExplosive, null, null, cBig, null, null, count(smokeless, 3), null);
        add(standard("rocket_phosphorus"), 2, whitePhosphorus, highExplosive, null, null, cBig, null, null, rocket, null);

        AStack steelPlate = STEEL.plate();
        AStack napalm = exact("canister_napalm");
        AStack gas = fluid("GAS", 1000);
        AStack balefire = fluid("BALEFIRE", 1000);
        add(standard("flame_diesel"), 1, null, steelPlate, null, null, fluid("DIESEL", 1000), null, null, steelPlate, null);
        add(standard("flame_napalm"), 1, null, steelPlate, null, null, napalm, null, null, steelPlate, null);
        add(standard("flame_gas"), 1, null, steelPlate, null, null, gas, null, null, steelPlate, null);
        add(standard("flame_balefire"), 1, null, steelPlate, null, null, balefire, null, null, steelPlate, null);

        add(standard("capacitor"), 4, null, plastic, null, null, count(silicon(), 4), null, null, plastic, null);
        add(standard("capacitor_overcharge"), 4, null, plastic, null, null, count(silicon(), 6), null, null, plastic, null);
        add(standard("capacitor_ir"), 4, null, plastic, null, null, niobium, null, null, plastic, null);

        add(standard("tau_uranium"), 16, null, PB.plate(), null, null, uranium, null, null, PB.plate(), null);
        add(standard("coil_tungsten"), 4, null, null, null, null, tungsten(), null, null, null, null);
        add(standard("coil_ferrouranium"), 4, null, null, null, null, ferro, null, null, null, null);

        AStack shell = exact("assembly_nuke");
        AStack tatb = exact("ball_tatb");
        AStack plutonium = PU239.nugget();
        add(standard("nuke_standard"), 1, null, plutonium, null, null, shell, null, null, null, null);
        add(standard("nuke_demo"), 1, null, count(plutonium, 2), null, null, shell, null, null, null, null);
        add(standard("nuke_high"), 1, null, count(plutonium, 4), null, null, shell, null, null, null, null);
        add(standard("nuke_tots"), 1, null, count(plutonium, 2), null, null, count(tatb, 2), null, null, count(steelPlate, 4), null);
        add(standard("nuke_hive"), 1, null, count(highExplosive, 8), null, null, count(sBig, 2), null, null, count(steelPlate, 4), null);
        add(standard("nuke_balefire"), 1, null, exact("egg_balefire_shard"), null, null, shell, null, null, null, null);

        add(standard("ct_hook"), 16, null, steel, null, null, pipe, null, null, smokeless, null);
        add(standard("ct_mortar"), 4, null, count(highExplosive, 4), null, null, pipe, null, null, smokeless, null);
    }

    private static String standard(String suffix) {
        return "ammo_standard_" + suffix;
    }

    private static AStack exact(Item item) {
        return item == null ? MISSING : new ComparableStack(item);
    }

    private static AStack exact(String id) {
        return exact(BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", id)).orElse(null));
    }

    private static AStack casing(EnumCasingType type) {
        return exact("casing_" + type.name().toLowerCase(Locale.ROOT));
    }

    private static AStack smokeless() {
        return new TagStack(ANY_SMOKELESS);
    }

    private static AStack fluid(String name, int amount) {
        FluidType type = Fluids.fromName(name);
        Item container = BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", "fluid_tank")).orElse(null);
        if (type == Fluids.NONE || !(container instanceof ItemFluidTank tank)) return MISSING;
        return new ComparableStack(tank.make(type, amount));
    }

    private static AStack silicon() {
        return com.hbm.inventory.OreDictManager.SI.billet();
    }

    private static AStack tungsten() {
        return W.ingot();
    }

    private static AStack count(AStack stack, int amount) {
        if (stack instanceof TagStack tag) return new TagStack(tag.tag, amount);
        if (stack instanceof ComparableStack comparable) return new ComparableStack(comparable.item, amount);
        if (stack instanceof MaterialStack material) {
            return new MaterialStack(material.material, material.shape, amount);
        }
        throw new IllegalArgumentException("unsupported Ammo Press ingredient: " + stack.getClass());
    }

    private static void add(String outputId, int count, AStack... input) {
        if (input.length != 9) throw new IllegalArgumentException("Ammo Press recipes require exactly nine inputs");
        Item output = BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", outputId)).orElse(null);
        if (output == null) return;
        for (AStack ingredient : input) if (ingredient == MISSING) return;
        recipes.add(new AmmoPressRecipe(new ItemStack(output, count), input));
    }

    public static final class AmmoPressRecipe {
        public final ItemStack output;
        public final AStack[] input;

        private AmmoPressRecipe(ItemStack output, AStack[] input) {
            this.output = output;
            this.input = input;
        }
    }
}
