package com.hbm.gametest;

import com.hbm.blocks.StandardInfo;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModGearItems;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.armor.ItemGasMask;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.TooltipColours;
import dev.golem.core.gametest.GameTestHelpers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** 1.7's armor tooltip lines in 1.7's order and colours: power armor set lines and the gas mask filter lines. */
public final class ArmorTooltipGameTests {

    private ArmorTooltipGameTests() {}

    private static final String HINT = "dark_gray/i:Hold <|yellow/i:LSHIFT|dark_gray/i:> to display protection info";
    private static final String MASK_RAD = "yellow:Radiation resistance: 0.07";
    private static final List<String> FULL_PACKAGE = List.of("yellow:  Airborne Particles", "yellow:  Particulates",
            "yellow:  Chemical Gas", "yellow:  Bacteria / Aerosols", "yellow:  Corrosive Fumes",
            "yellow:  Carbon Monoxide", "yellow:  Bright Lights", "yellow:  Eye Irritants");

    public static void tooltips(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);

        ItemStack helmet = new ItemStack(ModArmorItems.ncrpa_helmet.get());
        List<String> ncr = new ArrayList<>(List.of(
                "gray:Charge: 2.5M / 2.5M",
                "gold:Full Set Bonus:",
                "aqua:Strength",
                "gold:  Auditory Geiger Counter",
                "red:  Enemy HUD",
                "red:  Hard Landing",
                "dark_purple:Resistances when worn in set:",
                "dark_purple:  " + name(ModArmorItems.ncrpa_helmet.get()),
                "dark_purple:  " + name(ModArmorItems.ncrpa_plate.get()),
                "dark_purple:  " + name(ModArmorItems.ncrpa_legs.get()),
                "dark_purple:  " + name(ModArmorItems.ncrpa_boots.get()),
                "gray:Fire: 10.0/90%",
                "gray:Physical: 25.0/65%",
                "gray:Energy: 10.0/50%",
                "gray:Explosion: 15.0/25%",
                "gray:Fall damage: 0.0/100%",
                "gray:Other: 15.0/25%"));
        List<String> ncrHidden = new ArrayList<>(ncr);
        ncrHidden.addAll(List.of(HINT, "yellow:Radiation resistance: 0.34"));
        expect(helper, "ncrpa_helmet", lines(helmet, player, false), ncrHidden);
        List<String> ncrShift = new ArrayList<>(ncr);
        ncrShift.add("gold:Protects against hazards:");
        ncrShift.addAll(FULL_PACKAGE);
        ncrShift.add("yellow:Radiation resistance: 0.34");
        expect(helper, "ncrpa_helmet with Shift", lines(helmet, player, true), ncrShift);

        ItemGasMask gasMask = ModGearItems.gas_mask.get();
        ItemStack mask = new ItemStack(gasMask);
        List<String> never = List.of("red:Will never protect against:", "dark_red: -Corrosive Fumes");
        List<String> empty = new ArrayList<>(List.of("red:No filter installed!"));
        empty.addAll(never);
        empty.addAll(List.of(HINT, MASK_RAD));
        expect(helper, "empty gas_mask", lines(mask, player, false), empty);

        gasMask.installFilter(mask, new ItemStack(ModGearItems.gas_mask_filter.get()));
        List<String> filtered = new ArrayList<>(List.of("gold:Installed filter:", "gray:  Gas Mask Filter (100%)",
                "yellow:  |" + HINT));
        filtered.addAll(never);
        filtered.addAll(List.of(HINT, MASK_RAD));
        expect(helper, "gas_mask with a filter", lines(mask, player, false), filtered);

        List<String> filteredShift = new ArrayList<>(List.of("gold:Installed filter:", "gray:  Gas Mask Filter (100%)",
                "yellow:  |gold:Protects against hazards:", "yellow:    Airborne Particles", "yellow:    Particulates",
                "yellow:    Chemical Gas", "yellow:    Corrosive Fumes", "yellow:    Bacteria / Aerosols"));
        filteredShift.addAll(never);
        filteredShift.addAll(List.of("gold:Protects against hazards:", "yellow:  Eye Irritants", "yellow:  Bright Lights",
                MASK_RAD));
        expect(helper, "gas_mask with a filter and Shift", lines(mask, player, true), filteredShift);
        helper.succeed();
    }

    private static String name(ArmorFSB item) {
        return new ItemStack(item).getHoverName().getString();
    }

    /** The HBM lines of the tooltip, built in the loaders' order, as merged "colour:text" runs. */
    private static List<String> lines(ItemStack stack, Player player, boolean shift) {
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName());
        StandardInfo.setShiftHeldPoll(() -> shift);
        try {
            if (stack.getItem() instanceof ArmorFSB fsb) {
                fsb.addInformation(stack, lines);
                DamageResistanceHandler.addInfo(stack, lines);
            } else {
                ArmorUtil.addGasMaskInformation(stack, lines);
            }
            TooltipColours.apply(stack, lines);
            HazardSystem.addHazardInfo(stack, player, lines);
        } finally {
            StandardInfo.setShiftHeldPoll(() -> false);
        }
        return lines.subList(1, lines.size()).stream().map(ArmorTooltipGameTests::runs).toList();
    }

    private static String runs(Component line) {
        StringBuilder out = new StringBuilder();
        String[] last = {null};
        line.visit((Style style, String text) -> {
            if (text.isEmpty()) return Optional.empty();
            String key = (style.getColor() == null ? "-" : style.getColor().toString()) + (style.isItalic() ? "/i" : "");
            if (!key.equals(last[0])) out.append(last[0] == null ? "" : "|").append(key).append(':');
            out.append(text);
            last[0] = key;
            return Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }

    private static void expect(GameTestHelper helper, String what, List<String> got, List<String> want) {
        helper.assertTrue(got.equals(want), what + " tooltip should be " + want + ", got " + got);
    }
}
