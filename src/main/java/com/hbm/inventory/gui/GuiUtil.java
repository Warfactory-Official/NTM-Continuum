package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Small client-GUI helpers shared by HBM screens (e.g. the energy-bar hover tooltip). */
public final class GuiUtil {

    private GuiUtil() {}

    /**
     * The energy-bar hover tooltip, matching the 1.12.2 GUIs: one line {@code <power>HE/<max>HE} using
     * abbreviated numbers ({@link Library#getShortNumber}).
     */
    public static List<Component> energyTooltip(long power, long maxPower) {
        return List.of(Component.literal(Library.getShortNumber(power) + "HE/" + Library.getShortNumber(maxPower) + "HE"));
    }

    /**
     * As {@link #energyTooltip(long, long)} plus the throughput line {@code ±<delta>HE/s} — green when
     * charging, red when draining, yellow at zero — exactly as the original battery GUI drew it.
     */
    public static List<Component> energyTooltip(long power, long maxPower, long delta) {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(Library.getShortNumber(power) + "HE/" + Library.getShortNumber(maxPower) + "HE"));
        if (delta > 0) {
            list.add(Component.literal("+" + Library.getShortNumber(delta) + "HE/s").withStyle(ChatFormatting.GREEN));
        } else if (delta < 0) {
            list.add(Component.literal("-" + Library.getShortNumber(Math.abs(delta)) + "HE/s").withStyle(ChatFormatting.RED));
        } else {
            list.add(Component.literal("0HE/s").withStyle(ChatFormatting.YELLOW));
        }
        return list;
    }

    /** The exact legacy fluid-tank tooltip: name, fill, optional pressure, then fluid metadata. */
    public static List<Component> tankTooltip(FluidTankNTM tank) {
        return TankDisplay.tooltip(tank);
    }

    /** The shared HBM {@code gui_utility.png} icon sheet (info/warning glyphs) — the 1.12.2 {@code GuiInfoContainer.guiUtil}. */
    private static final Identifier GUI_UTILITY = dev.golem.core.Ids.of("hbm", "textures/gui/gui_utility.png");

    /** The 8×8 info-panel dimensions + a small-blue-"*" glyph, the hover target for the upgrade panel. */
    public static final int INFO_PANEL_SIZE = 8;

    /** Draws the "small blue *" info‑panel glyph (1.12.2 {@code drawInfoPanel(...,8)}) at screen {@code x,y}. */
    public static void drawUpgradeInfoPanel(GuiGraphicsExtractor graphics, int x, int y) {
        GuiDraw.blit(graphics, GUI_UTILITY, x, y, 0.0F, 32.0F, INFO_PANEL_SIZE, INFO_PANEL_SIZE, 256, 256);
    }

    /** Draws one of the legacy info/warning glyphs from {@code gui_utility.png}. */
    public static void drawInfoPanel(GuiGraphicsExtractor graphics, int x, int y, int type) {
        int sourceX = switch (type) {
            case 2, 6, 10 -> 8;
            case 3, 7, 11 -> 24;
            default -> 0;
        };
        int sourceY = switch (type) {
            case 1, 5 -> 8;
            case 4, 6, 7 -> 16;
            case 8, 10, 11 -> 32;
            case 9 -> 40;
            default -> 0;
        };
        int size = type >= 2 && type <= 3 || type >= 6 ? 16 : 8;
        GuiDraw.blit(graphics, GUI_UTILITY, x, y, sourceX, sourceY, size, size, 256, 256);
    }

    /** The 16×16 "large red !" warning glyph (1.12.2 {@code drawInfoPanel(...,16,16,6)}) at screen {@code x,y}. */
    public static void drawWarningPanel(GuiGraphicsExtractor graphics, int x, int y) {
        GuiDraw.blit(graphics, GUI_UTILITY, x, y, 8.0F, 16.0F, 16, 16, 256, 256);
    }

    /**
     * The "Acceptable Upgrades:" list shown when hovering a machine's upgrade slots — a faithful port of
     * the 1.12.2 {@code GuiInfoContainer.getUpgradeInfo}: one {@code upgrade.gui.<type>} line (with the
     * machine's max stack level) per upgrade type the machine reacts to ({@code canProvideInfo} probed
     * with the level-0 sentinel).
     */
    public static List<Component> upgradeInfo(IUpgradeInfoProvider provider) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("upgrade.gui.title"));
        for (UpgradeType type : UpgradeType.values()) {
            if (!provider.canProvideInfo(type, 0, false)) continue;
            Integer maxLevel = provider.getValidUpgrades().get(type);
            if (maxLevel == null) continue;
            String key = switch (type) {
                case SPEED -> "upgrade.gui.speed";
                case POWER -> "upgrade.gui.power";
                case EFFECT -> "upgrade.gui.effectiveness";
                case AFTERBURN -> "upgrade.gui.afterburner";
                case OVERDRIVE -> "upgrade.gui.overdrive";
                default -> null;
            };
            if (key != null) lines.add(Component.translatable(key, maxLevel));
        }
        return lines;
    }
}
