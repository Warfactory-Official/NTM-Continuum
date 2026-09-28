package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.handler.BobmazonOfferFactory;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModSingularityItems;
import com.hbm.items.special.CellVariants;
import com.hbm.items.special.ItemCell;
import com.hbm.items.special.ItemStarterKit;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Recipes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Regression contracts for the 2026-09-25 cell / Bobmazon correction.
 *
 * <p>Cells: every full cell (the eight 1.7 cell items folded into one component-backed {@code hbm:cell})
 * selects its own model variant and name, independent of the port's fluid ids, carries its 1.7 hazards,
 * and the recipes that make cells produce stacks identical to {@link ItemCell#getFullCell}.
 *
 * <p>Ids: every Bobmazon offer / requirement icon and every starter-kit entry resolves in the registry.
 */
public final class FinalCellHudFixGameTests {
    private FinalCellHudFixGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CELL_VARIANTS =
            TESTS.function("final_cell_variants", 20, () -> FinalCellHudFixGameTests::cellVariants);
    public static final GameTests.Function OFFER_KIT_IDS =
            TESTS.function("final_bobmazon_kit_ids", 20, () -> FinalCellHudFixGameTests::offerAndKitIds);

    /**
     * The two hidden-catalogue satellite offers, {@code hbm:sat_laser} (1.7 {@code ModItems.sat_laser}, 8 caps) and
     * {@code hbm:sat_gerald} (1.7 {@code ModItems.sat_gerald}, 32 caps). They were withdrawn with the satellites
     * until §19ec-satellites ported them; they must now resolve.
     */
    private static final Set<String> SATELLITE_OFFERS = Set.of("hbm:sat_laser", "hbm:sat_gerald");

    /**
     * The HIDDEN requirement's icon, 1.7 {@code achievement_icon:QUESTIONMARK} (the bobHidden achievement's icon),
     * is one of the icon/dummy registrations §19ea excluded on purpose; that row draws no icon. It is an icon,
     * not an offer, and is the only exception.
     */
    private static final String EXCLUDED_ICON = "hbm:achievement_icon";

    public static void init() {}

    public static void cellVariants(GameTestHelper helper) {
        Object[][] table = {
                {null, CellVariants.EMPTY, "empty"},
                {Fluids.UF6, CellVariants.UF6, "uf6"},
                {Fluids.PUF6, CellVariants.PUF6, "puf6"},
                {Fluids.AMAT, CellVariants.ANTIMATTER, "antimatter"},
                {Fluids.DEUTERIUM, CellVariants.DEUTERIUM, "deuterium"},
                {Fluids.TRITIUM, CellVariants.TRITIUM, "tritium"},
                {Fluids.SAS3, CellVariants.SAS3, "sas3"},
                {Fluids.ASCHRAB, CellVariants.ANTI_SCHRABIDIUM, "anti_schrabidium"},
        };
        helper.assertTrue(CellVariants.NAMES.length == table.length, "cell variant table length drifted");
        for (Object[] row : table) {
            FluidType fluid = (FluidType) row[0];
            int variant = (Integer) row[1];
            String name = (String) row[2];
            ItemStack stack = fluid == null ? new ItemStack(ModSingularityItems.cell.get()) : ItemCell.getFullCell(fluid);
            helper.assertTrue(CellVariants.NAMES[variant].equals(name), "variant " + variant + " is not " + name);
            helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(stack, 0) == variant,
                    "cell_" + name + " selects model variant " + ModDataComponents.ITEM_VARIANT.getOr(stack, 0)
                            + ", expected " + variant);
            if (fluid != null) {
                helper.assertTrue(ItemCell.isFullCell(stack, fluid) && ModDataComponents.fluidId(stack) == fluid.getID(),
                        "cell_" + name + " does not hold its fluid");
            }
            Component display = stack.getHoverName();
            String key = display.getContents() instanceof TranslatableContents t ? t.getKey() : display.getString();
            helper.assertTrue(("item.hbm.cell_" + name).equals(key), "cell_" + name + " is named " + key);
        }

        ItemStack sas3 = ItemCell.getFullCell(Fluids.SAS3);
        helper.assertTrue(TextColor.fromLegacyFormat(ChatFormatting.AQUA).equals(sas3.getHoverName().getStyle().getColor()),
                "SAS3 cell name lacks the 1.7 rare (aqua) colour");
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(sas3, HazardRegistry.RADIATION) == HazardRegistry.sas3
                        && HazardSystem.getHazardLevelFromStack(sas3, HazardRegistry.BLINDING) == 60D,
                "SAS3 cell lacks the 1.7 radiation 5 / blinding 60 hazards");
        helper.assertTrue(Math.abs(HazardSystem.getHazardLevelFromStack(ItemCell.getFullCell(Fluids.TRITIUM),
                        HazardRegistry.RADIATION) - 0.001D) < 1E-6, "tritium cell lacks 1.7 radiation 0.001");
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(ItemCell.getFullCell(Fluids.UF6), HazardRegistry.RADIATION) == 0D,
                "UF6 cell gained a hazard 1.7 never registered");

        // Recipe outputs must be the same stack getFullCell makes, or crafted and machine-filled cells never stack.
        ItemStack empty = new ItemStack(ModSingularityItems.cell.get());
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(empty.copy());
        grid.set(4, new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "mike_deut"))));
        ItemStack deuterium = Recipes.craftingResult(helper.getLevel(), 3, 3, grid).orElse(ItemStack.EMPTY);
        helper.assertTrue(deuterium.getCount() == 8 && ItemStacks.sameItemAndData(deuterium, ItemCell.getFullCell(Fluids.DEUTERIUM)),
                "deuterium cell recipe output differs from getFullCell: " + deuterium);
        List<ItemStack> rod = new ArrayList<>();
        for (int i = 0; i < 9; i++) rod.add(ItemStack.EMPTY);
        rod.set(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "rod_tritium"))));
        rod.set(1, empty.copy());
        ItemStack tritium = Recipes.craftingResult(helper.getLevel(), 3, 3, rod).orElse(ItemStack.EMPTY);
        helper.assertTrue(ItemStacks.sameItemAndData(tritium, ItemCell.getFullCell(Fluids.TRITIUM)),
                "tritium unload recipe output differs from getFullCell: " + tritium);

        List<ItemStack> kit = ItemStarterKit.contents(ItemStarterKit.Kind.PROTOTYPE);
        helper.assertTrue(kit.stream().anyMatch(s -> s.getCount() == 4 && ItemCell.isFullCell(s, Fluids.SAS3)
                        && ModDataComponents.ITEM_VARIANT.getOr(s, 0) == CellVariants.SAS3),
                "prototype kit lost its four SAS3 cells");
        helper.succeed();
    }

    public static void offerAndKitIds(GameTestHelper helper) {
        Set<String> offers = new TreeSet<>(BobmazonOfferFactory.unresolvedIds());
        for (String id : SATELLITE_OFFERS) {
            helper.assertTrue(!offers.contains(id), "satellite offer " + id + " no longer resolves");
            helper.assertTrue(BobmazonOfferFactory.special().stream()
                            .anyMatch(o -> BuiltInRegistries.ITEM.getKey(o.offer.getItem()).toString().equals(id)),
                    "hidden catalogue lost the 1.7 " + id + " offer");
        }
        offers.remove(EXCLUDED_ICON);
        helper.assertTrue(offers.isEmpty(), "unresolved Bobmazon offer ids: " + offers);
        Set<String> kits = ItemStarterKit.unresolvedIds();
        helper.assertTrue(kits.isEmpty(), "unresolved starter kit ids: " + kits);
        helper.assertTrue(BobmazonOfferFactory.special().stream()
                        .anyMatch(o -> BuiltInRegistries.ITEM.getKey(o.offer.getItem()).getPath().equals("chopper")),
                "hidden catalogue lost the 1.7 spawn_chopper offer");
        helper.succeed();
    }
}
