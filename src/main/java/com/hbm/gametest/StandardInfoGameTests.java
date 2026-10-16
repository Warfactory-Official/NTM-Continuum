package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.StandardInfo;
import dev.golem.core.Ids;
import dev.golem.core.world.CompatBlockItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 1.7 {@code ITooltipProvider.addStandardInfo}: the Shift-gated description on every block that called it. */
public final class StandardInfoGameTests {

    private StandardInfoGameTests() {}

    /** Every block whose 1.7 class called {@code addStandardInfo}, by port id, with its 1.7 line count. */
    static final Map<String, Integer> STANDARD_INFO_BLOCKS = new TreeMap<>(Map.ofEntries(
            Map.entry("capacitor_bus", 2),
            Map.entry("chimney_brick", 2),
            Map.entry("chimney_industrial", 2),
            Map.entry("conveyor", 3),
            Map.entry("conveyor_chute", 2),
            Map.entry("conveyor_double", 3),
            Map.entry("conveyor_express", 3),
            Map.entry("conveyor_lift", 2),
            Map.entry("conveyor_triple", 3),
            Map.entry("crane_boxer", 4),
            Map.entry("crane_extractor", 5),
            Map.entry("crane_grabber", 6),
            Map.entry("crane_inserter", 4),
            Map.entry("crane_partitioner", 3),
            Map.entry("crane_router", 3),
            Map.entry("crane_splitter", 3),
            Map.entry("crane_unboxer", 4),
            Map.entry("drone_crate", 3),
            Map.entry("drone_crate_provider", 2),
            Map.entry("drone_crate_requester", 2),
            Map.entry("drone_dock", 3),
            Map.entry("drone_waypoint", 5),
            Map.entry("fan", 4),
            Map.entry("fluid_counter_valve", 1),
            Map.entry("fluid_duct_gauge", 4),
            Map.entry("fluid_duct_paintable", 4),
            Map.entry("fluid_duct_paintable_block_exhaust", 4),
            Map.entry("furnace_iron", 2),
            Map.entry("furnace_steel", 4),
            Map.entry("fusion_boiler", 2),
            Map.entry("fusion_breeder", 3),
            Map.entry("fusion_collector", 2),
            Map.entry("fusion_coupler", 3),
            Map.entry("fusion_klystron", 2),
            Map.entry("fusion_klystron_creative", 1),
            Map.entry("fusion_mhdt", 4),
            Map.entry("fusion_torus", 4),
            Map.entry("heat_boiler", 3),
            Map.entry("heater_electric", 3),
            Map.entry("heater_firebox", 1),
            Map.entry("heater_heatex", 1),
            Map.entry("heater_oilburner", 2),
            Map.entry("heater_oven", 2),
            Map.entry("machine_ashpit", 1),
            Map.entry("machine_assembly_factory", 5),
            Map.entry("machine_autosaw", 7),
            Map.entry("machine_battery_socket", 4),
            Map.entry("machine_chemical_factory", 5),
            Map.entry("machine_chungus", 1),
            Map.entry("machine_coker", 3),
            Map.entry("machine_conveyor_press", 3),
            Map.entry("machine_crucible", 1),
            Map.entry("machine_funnel", 4),
            Map.entry("machine_furnace_combination", 3),
            Map.entry("machine_industrial_boiler", 4),
            Map.entry("machine_industrial_turbine", 1),
            Map.entry("machine_large_turbine", 1),
            Map.entry("machine_liquefactor", 3),
            Map.entry("machine_pump_electric", 3),
            Map.entry("machine_pump_steam", 3),
            Map.entry("machine_purex", 2),
            Map.entry("machine_sawmill", 3),
            Map.entry("machine_solidifier", 3),
            Map.entry("machine_steam_engine", 1),
            Map.entry("machine_stirling", 4),
            Map.entry("machine_stirling_creative", 4),
            Map.entry("machine_stirling_steel", 5),
            Map.entry("machine_thresher", 7),
            Map.entry("machine_turbine", 1),
            Map.entry("machine_wood_burner", 3),
            Map.entry("pa_beamline", 3),
            Map.entry("pa_detector", 4),
            Map.entry("pa_dipole", 6),
            Map.entry("pa_quadrupole", 3),
            Map.entry("pa_rfc", 4),
            Map.entry("pa_source", 2),
            Map.entry("piston_inserter", 4),
            Map.entry("pneumatic_tube", 5),
            Map.entry("pneumatic_tube_paintable", 4),
            Map.entry("pwr_casing", 2),
            Map.entry("pwr_channel", 2),
            Map.entry("pwr_control", 2),
            Map.entry("pwr_controller", 3),
            Map.entry("pwr_fuel", 2),
            Map.entry("pwr_heatex", 2),
            Map.entry("pwr_heatsink", 3),
            Map.entry("pwr_neutron_source", 3),
            Map.entry("pwr_port", 2),
            Map.entry("pwr_reflector", 3),
            Map.entry("radio_torch_controller", 2),
            Map.entry("radio_torch_counter", 2),
            Map.entry("radio_torch_logic", 3),
            Map.entry("radio_torch_reader", 2),
            Map.entry("radio_torch_receiver", 1),
            Map.entry("radio_torch_sender", 2),
            Map.entry("rbmk_display", 3),
            Map.entry("rbmk_display_blank", 1),
            Map.entry("rbmk_gauge", 3),
            Map.entry("rbmk_graph", 3),
            Map.entry("rbmk_indicator", 4),
            Map.entry("rbmk_key_pad", 3),
            Map.entry("rbmk_lever", 3),
            Map.entry("rbmk_loader", 3),
            Map.entry("rbmk_numitron", 3),
            Map.entry("rbmk_steam_inlet", 2),
            Map.entry("rbmk_steam_outlet", 2),
            Map.entry("rbmk_terminal", 1),
            Map.entry("red_cable_gauge", 4),
            Map.entry("red_cable_paintable", 4)));

    public static void tooltipShiftGate(GameTestHelper helper) {
        expect(helper, "machine_stirling", List.of(
                "Turns heat into energy. Requires external heat source.",
                "Heat transfer rate: T*0.1 TU/t",
                "Max intake: 300 TU/t",
                "Efficiency: 50%"));
        expect(helper, "fusion_torus", List.of(
                "Fusion reactor main component.",
                "Requires klystron input energy to ignite plasma,",
                "produces plasma energy and neutron flux.",
                "Requires cooling!"));
        expect(helper, "machine_chungus", List.of("Efficiency: 85%"));
        expect(helper, "pwr_heatsink", List.of(
                "Increases core heat capacity by 5%",
                "Makes coolant channels and heat exchangers slightly less effective",
                "Placement: Any"));
        helper.succeed();
    }

    public static void everyBlockHasItsLines(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : STANDARD_INFO_BLOCKS.entrySet()) {
            List<Component> shown = lines(entry.getKey(), true);
            List<Component> hidden = lines(entry.getKey(), false);
            if (shown == null) {
                wrong.add(entry.getKey() + " (no block item)");
            } else if (shown.size() < entry.getValue() || !yellow(shown.subList(0, entry.getValue()))
                    || shown.get(0).getString().startsWith("block.")) {
                wrong.add(entry.getKey() + " (shift: " + text(shown) + ")");
            } else if (hidden.isEmpty() || !hidden.get(0).getString().equals(HINT)) {
                wrong.add(entry.getKey() + " (no shift: " + text(hidden) + ")");
            }
        }
        helper.assertTrue(wrong.isEmpty(), "addStandardInfo lines wrong for " + wrong);
        helper.succeed();
    }

    private static final String HINT = "Hold <LSHIFT> to display more info";

    private static void expect(GameTestHelper helper, String id, List<String> expected) {
        List<Component> hidden = lines(id, false);
        helper.assertTrue(hidden != null && hidden.size() == 1 && hidden.get(0).getString().equals(HINT),
                id + " without Shift should show only the hint, got " + text(hidden));
        Component hint = hidden.get(0);
        helper.assertTrue(is(hint.getStyle(), ChatFormatting.DARK_GRAY) && hint.getSiblings().size() == 2
                        && is(hint.getSiblings().get(0).getStyle(), ChatFormatting.YELLOW)
                        && hint.getSiblings().get(0).getString().equals("LSHIFT")
                        && is(hint.getSiblings().get(1).getStyle(), ChatFormatting.DARK_GRAY),
                id + " hint should be dark grey italic with a yellow italic LSHIFT");

        List<Component> shown = lines(id, true);
        helper.assertTrue(text(shown).equals(expected) && yellow(shown),
                id + " with Shift should show 1.7's yellow lines " + expected + ", got " + text(shown));
    }

    private static List<Component> lines(String id, boolean shift) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, id));
        if (!(item instanceof CompatBlockItem blockItem)) return null;
        StandardInfo.setShiftHeldPoll(() -> shift);
        try {
            return blockItem.hoverLines(new ItemStack(item), TooltipFlag.NORMAL);
        } finally {
            StandardInfo.setShiftHeldPoll(() -> false);
        }
    }

    private static boolean is(Style style, ChatFormatting colour) {
        return style.getColor() != null && style.getColor().getValue() == colour.getColor() && style.isItalic();
    }

    private static boolean yellow(List<Component> lines) {
        for (Component line : lines) {
            if (line.getStyle().getColor() == null
                    || line.getStyle().getColor().getValue() != ChatFormatting.YELLOW.getColor()) return false;
        }
        return true;
    }

    private static List<String> text(List<Component> lines) {
        return lines == null ? List.of() : lines.stream().map(Component::getString).toList();
    }
}
