package com.hbm.gametest;

import com.hbm.hazard.HazardSystem;
import com.hbm.items.ModBombs2Items;
import com.hbm.items.ModRtgContent;
import com.hbm.util.TooltipColours;
import dev.golem.core.gametest.GameTestHelpers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** 1.7's tooltip colours: uncoloured lines after an HBM item's name are gray, hazard lines keep 1.7's text. */
public final class TooltipColourGameTests {

    private TooltipColourGameTests() {}

    public static void colours(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);

        ItemStack pellet = new ItemStack(ModRtgContent.PELLET_RTG_RADIUM.get());
        List<Component> lines = description(pellet, "item.hbm.pellet_rtg_radium.desc");
        lines.add(Component.empty().append(Component.literal("aqua").withStyle(ChatFormatting.AQUA)));
        Component name = lines.get(0);
        TooltipColours.apply(pellet, lines);
        TooltipColours.apply(pellet, lines);
        HazardSystem.addHazardInfo(pellet, player, lines);

        helper.assertTrue(lines.get(0) == name, "the name line is left alone");
        expect(helper, lines.get(1), ChatFormatting.GRAY, "the description is gray");
        expect(helper, lines.get(2), ChatFormatting.GRAY, "a literal line is gray");
        helper.assertTrue(lines.get(3).getSiblings().get(0).getStyle().getColor() == TextColor.fromLegacyFormat(ChatFormatting.AQUA),
                "a coloured part keeps its colour: " + lines.get(3));
        expectLine(helper, lines, "11.25RAD/s", ChatFormatting.YELLOW);

        ItemStack core = new ItemStack(ModBombs2Items.solinium_core.get());
        List<Component> coreLines = description(core, "item.bomb_part.used_in");
        TooltipColours.apply(core, coreLines);
        HazardSystem.addHazardInfo(core, player, coreLines);
        expect(helper, coreLines.get(1), ChatFormatting.GRAY, "\"Used in:\" is gray");
        expectLine(helper, coreLines, "14.0RAD/s", ChatFormatting.YELLOW);

        ItemStack stick = new ItemStack(Items.STICK);
        List<Component> vanilla = description(stick, "item.minecraft.stick");
        TooltipColours.apply(stick, vanilla);
        expect(helper, vanilla.get(1), null, "another namespace's lines are left alone");
        helper.succeed();
    }

    private static List<Component> description(ItemStack stack, String key) {
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName());
        lines.add(Component.translatable(key));
        lines.add(Component.literal("Power Level: 3"));
        return lines;
    }

    private static void expect(GameTestHelper helper, Component line, ChatFormatting colour, String what) {
        TextColor want = colour == null ? null : TextColor.fromLegacyFormat(colour);
        helper.assertTrue(want == null ? line.getStyle().getColor() == null : want.equals(line.getStyle().getColor()),
                what + ": " + line.getString() + " has " + line.getStyle().getColor());
    }

    private static void expectLine(GameTestHelper helper, List<Component> lines, String text, ChatFormatting colour) {
        Component line = lines.stream().filter(l -> l.getString().equals(text)).findFirst().orElse(null);
        helper.assertTrue(line != null, "tooltip must contain \"" + text + "\": " + lines.stream().map(Component::getString).toList());
        expect(helper, line, colour, text);
    }
}
