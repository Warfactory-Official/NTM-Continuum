package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.handler.jei.ArcWelderRecipeHandler;
import com.hbm.handler.jei.AshpitRecipeHandler;
import com.hbm.handler.jei.BoilingRecipeHandler;
import com.hbm.handler.jei.CokerRecipeHandler;
import com.hbm.handler.jei.CombinationRecipeHandler;
import com.hbm.handler.jei.CompressorRecipeHandler;
import com.hbm.handler.jei.CoolingRecipeHandler;
import com.hbm.handler.jei.CrackingRecipeHandler;
import com.hbm.handler.jei.ElectrolyserFluidRecipeHandler;
import com.hbm.handler.jei.ElectrolyserMetalRecipeHandler;
import com.hbm.handler.jei.FluidContainerRecipeHandler;
import com.hbm.handler.jei.FractioningRecipeHandler;
import com.hbm.handler.jei.HbmJeiRecipeTypes;
import com.hbm.handler.jei.HbmRecipeViewer;
import com.hbm.handler.jei.HydrotreatingRecipeHandler;
import com.hbm.handler.jei.LiquefactionRecipeHandler;
import com.hbm.handler.jei.PyrolysisRecipeHandler;
import com.hbm.handler.jei.RadiolysisRecipeHandler;
import com.hbm.handler.jei.RefineryRecipeHandler;
import com.hbm.handler.jei.ReformingRecipeHandler;
import com.hbm.handler.jei.RotaryFurnaceRecipeHandler;
import com.hbm.handler.jei.SolidificationRecipeHandler;
import com.hbm.handler.jei.UniversalRecipeHandler;
import com.hbm.handler.jei.VacuumRecipeHandler;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemModLens;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import dev.golem.core.viewer.ViewerCategory;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * GitHub issue 185: every fluid a recipe viewer row shows carries 1.7's NEI amount, so its tooltip reads "N mB".
 * Issue 188: the neutrino lens sweeps from the world floor, where the bedrock deposits are, and looks for blocks that exist.
 */
public final class Gh185To188GameTests {

    private Gh185To188GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function AMOUNTS =
            TESTS.function("gh185_viewer_fluids_show_amounts", 20, () -> Gh185To188GameTests::amounts);

    public static final GameTests.Function LENS =
            TESTS.function("gh188_lens_sweeps_from_the_floor", 20, () -> Gh185To188GameTests::lens);

    public static void init() {}

    // 1.7 builds these icons without an amount: annihilator milestones and SILEX fluid inputs.
    private static final Set<ViewerType<?>> NO_AMOUNT = Set.of(HbmJeiRecipeTypes.ANNIHILATOR, HbmJeiRecipeTypes.SILEX);

    private static void amounts(GameTestHelper helper) {
        List<ViewerCategory<?>> categories = new ArrayList<>();
        HbmRecipeViewer.INSTANCE.categories(added -> categories.addAll(List.of(added)));
        Map<ViewerType<?>, List<?>> recipes = recipes();

        Set<String> missing = new TreeSet<>();
        int checked = 0;
        for (ViewerCategory<?> category : categories) {
            if (NO_AMOUNT.contains(category.type())) continue;
            List<ItemStack> icons = new ArrayList<>();
            for (Object recipe : recipes.getOrDefault(category.type(), List.of())) record(category, recipe, icons);
            for (ItemStack icon : icons) {
                checked++;
                if (ModDataComponents.fluidAmount(icon) <= 0) missing.add(category.type().path());
            }
        }
        helper.assertTrue(missing.isEmpty(), "fluid icons without an amount in " + missing);
        helper.assertTrue(checked > 500, "only " + checked + " fluid icons checked");

        // 1.7 RefineryRecipes, HydrotreatingRecipes, ReformingRecipes, VacuumRefineryRecipes: rows per 1,000 mB of input
        for (ViewerType<?> type : List.of(HbmJeiRecipeTypes.REFINERY, HbmJeiRecipeTypes.HYDROTREATING,
                HbmJeiRecipeTypes.REFORMING, HbmJeiRecipeTypes.VACUUM)) {
            for (Object recipe : recipes.get(type)) {
                List<ItemStack> icons = new ArrayList<>();
                record(category(categories, type), recipe, icons);
                helper.assertTrue(ModDataComponents.fluidAmount(icons.get(0)) == 1000,
                        type.path() + " input is not 1,000 mB: " + describe(icons));
                if (recipe instanceof HydrotreatingRecipeHandler.Entry row) {
                    helper.assertTrue(ModDataComponents.fluidPressure(icons.get(1)) == row.recipe().hydrogen().pressure,
                            "hydrotreating hydrogen lost its row's pressure: " + describe(icons));
                }
            }
        }

        // 1.7 CrystallizerRecipes: iron ore and 500 mB of hydrogen peroxide make an iron crystal (raw iron is a port row)
        List<ItemStack> iron = new ArrayList<>();
        for (Object recipe : recipes.get(HbmJeiRecipeTypes.CRYSTALLIZER)) {
            List<ItemStack> icons = new ArrayList<>();
            boolean[] crystal = {false};
            SlotLayout layout = layout(icons, crystal);
            category(categories, HbmJeiRecipeTypes.CRYSTALLIZER).setRecipe(layout, cast(recipe));
            if (crystal[0]) iron.addAll(icons);
        }
        helper.assertTrue(iron.stream().anyMatch(icon -> ItemFluidIcon.getFluid(icon) == Fluids.PEROXIDE
                && ModDataComponents.fluidAmount(icon) == 500), "iron crystal acid: " + describe(iron));
        helper.succeed();
    }

    private static void lens(GameTestHelper helper) {
        for (String id : ItemModLens.targetIds()) {
            helper.assertTrue(BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).isPresent(), "lens looks for unknown " + id);
        }
        ServerLevel level = helper.getLevel();
        int y = 70, floor = level.getMinY();
        int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
        for (long time = 0; time < 1000; time++) {
            int layer = ItemModLens.layer(level, y, time);
            lowest = Math.min(lowest, layer);
            highest = Math.max(highest, layer);
        }
        // 1.7: layers 0 to y + 9 above its floor, here min(y - floor + 10, height - 1) layers from the floor up
        int top = floor + Math.max(Math.min(y - floor + 10, level.getHeight() - 1), 64) - 1;
        helper.assertTrue(lowest == floor && highest == top, "lens sweeps " + lowest + ".." + highest + ", not " + floor + ".." + top);
        helper.succeed();
    }

    private static <T> void record(ViewerCategory<T> category, Object recipe, List<ItemStack> icons) {
        category.setRecipe(layout(icons, new boolean[1]), cast(recipe));
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object recipe) {
        return (T) recipe;
    }

    @SuppressWarnings("unchecked")
    private static <T> ViewerCategory<T> category(List<ViewerCategory<?>> categories, ViewerType<T> type) {
        for (ViewerCategory<?> category : categories) if (category.type() == type) return (ViewerCategory<T>) category;
        throw new IllegalStateException("no category " + type.path());
    }

    private static String describe(List<ItemStack> icons) {
        List<String> out = new ArrayList<>();
        for (ItemStack icon : icons) out.add(ItemFluidIcon.getFluid(icon).getName() + " " + ModDataComponents.fluidAmount(icon));
        return out.toString();
    }

    /** Collects the fluid icons a row puts in drawn slots; {@code crystal} marks a row making an iron crystal. */
    private static SlotLayout layout(List<ItemStack> icons, boolean[] crystal) {
        return new SlotLayout() {
            @Override
            public Slot addSlot(Role role, int x, int y) {
                return new Slot() {
                    @Override
                    public Slot addItemStacks(List<ItemStack> stacks) {
                        for (ItemStack stack : stacks) {
                            if (stack.getItem() instanceof ItemFluidIcon) icons.add(stack);
                            if (role == Role.OUTPUT && stack.is(ModItems.crystal_iron.get())) crystal[0] = true;
                        }
                        return this;
                    }

                    @Override
                    public Slot addFluidStack(Fluid fluid, long milliBuckets) {
                        return this;
                    }

                    @Override
                    public Slot setBackground(Drawable background, int dx, int dy) {
                        return this;
                    }

                    @Override
                    public Slot addTooltipCallback(TooltipCallback callback) {
                        return this;
                    }
                };
            }

            @Override
            public void addInvisible(Role role, List<ItemStack> stacks) {}
        };
    }

    /** The rows of every category with a fluid slot, as {@link HbmRecipeViewer#recipes} adds them (that needs a client level). */
    private static Map<ViewerType<?>, List<?>> recipes() {
        CrystallizerRecipes.register();
        GasCentrifugeRecipes.register();
        SolderingRecipes.register();
        Map<ViewerType<?>, List<?>> map = new HashMap<>();
        map.put(HbmJeiRecipeTypes.CRYSTALLIZER, CrystallizerRecipes.recipes);
        map.put(HbmJeiRecipeTypes.GAS_CENTRIFUGE, GasCentrifugeRecipes.RECIPES);
        map.put(HbmJeiRecipeTypes.SOLDERING_STATION, SolderingRecipes.recipes);
        map.put(HbmJeiRecipeTypes.REFINERY, RefineryRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.FRACTIONING, FractioningRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.LIQUEFACTION, LiquefactionRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.SOLIDIFICATION, SolidificationRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.BOILING, BoilingRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.COOLING, CoolingRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.VACUUM, VacuumRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.REFORMING, ReformingRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.CRACKING, CrackingRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.HYDROTREATING, HydrotreatingRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.COKER, CokerRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.PYROLYSIS, PyrolysisRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.COMBINATION, CombinationRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.COMPRESSOR, CompressorRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.ELECTROLYSIS_FLUID, ElectrolyserFluidRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.ELECTROLYSIS_METAL, ElectrolyserMetalRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.FLUID_CONTAINERS, FluidContainerRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.ASHPIT, AshpitRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.ARC_WELDER, ArcWelderRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.ROTARY_FURNACE, RotaryFurnaceRecipeHandler.buildRecipes());
        map.put(HbmJeiRecipeTypes.ORE_SLOPPER, UniversalRecipeHandler.oreSlopper());
        map.put(HbmJeiRecipeTypes.OUTGASSER, UniversalRecipeHandler.outgasser());
        map.put(HbmJeiRecipeTypes.DEUTERIUM, UniversalRecipeHandler.deuterium());
        map.put(RadiolysisRecipeHandler.TYPE, RadiolysisRecipeHandler.buildRecipes());
        return map;
    }
}
