package com.hbm.inventory.gui;

import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import com.hbm.util.BobMathUtil;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Small client-GUI helpers shared by HBM screens (e.g. the energy-bar hover tooltip). */
public final class GuiUtil {

    private GuiUtil() {}

    /**
     * The rotating needle from 1.12.2's {@code GUIElements.drawSmoothGauge}: a dark triangle behind a
     * coloured one, both swept from the seven-o'clock position clockwise through 270 degrees as
     * {@code progress} runs 0 to 1. Upstream draws two coloured triangles directly; {@code fill} only
     * takes rectangles, so the triangle is scanline-rasterised at {@link #GAUGE_SUPERSAMPLE} times size
     * and the pose scales it back down, which keeps the rotated edges and the taper smooth.
     */
    public static void drawSmoothGauge(GuiGraphicsExtractor graphics, int cx, int cy, double progress,
                                       double tipLength, double backLength, double backSide, int color) {
        progress = Math.max(0.0, Math.min(1.0, progress));
        float angle = (float) Math.toRadians(progress * 270.0 + 45.0);
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, cx, cy);
        GuiDraw.rotate(graphics, angle);
        GuiDraw.scale(graphics, 1.0F / GAUGE_SUPERSAMPLE, 1.0F / GAUGE_SUPERSAMPLE);
        fillNeedle(graphics, tipLength * 1.5, backLength * 1.5, backSide * 1.5, 0xFF000000);
        fillNeedle(graphics, tipLength, backLength, backSide, 0xFF000000 | (color & 0xFFFFFF));
        GuiDraw.popMatrix(graphics);
    }

    /** Supersample factor the needle is rasterised at before the pose scales it back down. */
    private static final int GAUGE_SUPERSAMPLE = 8;

    private static void fillNeedle(GuiGraphicsExtractor graphics, double tipLength, double backLength,
                                   double backSide, int color) {
        double tip = tipLength * GAUGE_SUPERSAMPLE;
        double back = backLength * GAUGE_SUPERSAMPLE;
        double side = backSide * GAUGE_SUPERSAMPLE;
        double total = tip + back;
        int top = (int) Math.round(tip);
        int bottom = (int) Math.round(-back);
        for (int y = bottom; y < top; y++) {
            int halfWidth = (int) Math.round(side * (tip - y) / total);
            if (halfWidth > 0) graphics.fill(-halfWidth, y, halfWidth, y + 1, color);
        }
    }

    /**
     * The energy-bar hover tooltip, 1.7 {@code GuiInfoContainer.drawElectricityInfo}: one line
     * {@code <power>/<max>HE} using 1.7's abbreviated numbers ({@link Library#getShortNumber}).
     */
    public static List<Component> energyTooltip(long power, long maxPower) {
        return List.of(Component.literal(Library.getShortNumber(power) + "/" + Library.getShortNumber(maxPower) + "HE"));
    }

    /**
     * As {@link #energyTooltip(long, long)} plus the throughput line {@code ±<delta>HE/s}, green when
     * charging, red when draining, yellow at zero. This is exactly how the original battery GUI drew it.
     */
    public static List<Component> energyTooltip(long power, long maxPower, long delta) {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(Library.getShortNumber(power) + "/" + Library.getShortNumber(maxPower) + "HE"));
        if (delta > 0) {
            list.add(Component.literal("+" + Library.getShortNumber(delta) + "HE/s").withStyle(ChatFormatting.GREEN));
        } else if (delta < 0) {
            list.add(Component.literal("-" + Library.getShortNumber(Math.abs(delta)) + "HE/s").withStyle(ChatFormatting.RED));
        } else {
            list.add(Component.literal("+0HE/s").withStyle(ChatFormatting.YELLOW)); // 1.7 prefixes "+" at zero too
        }
        return list;
    }

    /** Plays the legacy master-channel UI click through the shared GUI helper. */
    public static void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /** The exact legacy fluid-tank tooltip: name, fill, optional pressure, then fluid metadata. */
    public static List<Component> tankTooltip(FluidTankNTM tank) {
        return TankDisplay.tooltip(tank);
    }

    /** The shared HBM {@code gui_utility.png} icon sheet (info/warning glyphs), the 1.12.2 {@code GuiInfoContainer.guiUtil}. */
    private static final Identifier GUI_UTILITY = Ids.of("hbm", "textures/gui/gui_utility.png");

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
            case 1 -> 8;
            case 4, 6, 7 -> 16;
            case 5 -> 24; // 1.7 GuiInfoContainer: small yellow ! at (0, 24)
            case 8, 10, 11 -> 32;
            case 9 -> 40;
            default -> 0;
        };
        int size = switch (type) { case 2, 3, 6, 7, 10, 11 -> 16; default -> 8; }; // 1.7: 8 and 9 are the small 8x8 stars
        GuiDraw.blit(graphics, GUI_UTILITY, x, y, sourceX, sourceY, size, size, 256, 256);
    }

    /** The 16×16 "large red !" warning glyph (1.12.2 {@code drawInfoPanel(...,16,16,6)}) at screen {@code x,y}. */
    public static void drawWarningPanel(GuiGraphicsExtractor graphics, int x, int y) {
        GuiDraw.blit(graphics, GUI_UTILITY, x, y, 8.0F, 16.0F, 16, 16, 256, 256);
    }

    /**
     * The "Acceptable Upgrades:" list shown when hovering a machine's upgrade slots, as the 1.12.2
     * {@code GuiInfoContainer.getUpgradeInfo}: one {@code upgrade.gui.<type>} line (with the machine's max stack
     * level) per upgrade type the machine reacts to ({@code canProvideInfo} probed with the level-0 sentinel).
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

    // 1:1 equivalents of the 1.7.10 GuiInfoContainer / FluidTank GUI helpers, in absolute
    // screen coordinates exactly as the 1.7 calls pass them (guiLeft + x).


    /**
     * 1.7's hover test used by {@code drawElectricityInfo}, {@code drawCustomInfoStat} and {@code renderTankInfo}:
     * {@code x <= mx < x + w} and {@code y < my <= y + h} (note the one-pixel downward shift on y, kept as in 1.7).
     */
    public static boolean hover(double mouseX, double mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY;
    }

    /**
     * 1.7 {@code FluidTank.renderTank(x, y, z, width, height)} (orientation 0): {@code y} is the BOTTOM edge; the
     * fluid texture is tiled upward from the bottom with V wrapping, U spanning {@code width/16}, tinted with
     * {@code getTint()}. Fill height is {@code fluid * height / maxFluid} (integer, as 1.7).
     */
    public static void tank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int yBottom, int width, int height) {
        int max = tank.getMaxFill();
        int i = max <= 0 ? 0 : tank.getFill() * height / max;
        if (i <= 0) return;
        int top = yBottom - i;
        int tint = 0xFF000000 | (tank.getTankType().clientMetadata().getTint() & 0xFFFFFF);
        Identifier tex = tank.getTankType().clientMetadata().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(tex).isEmpty()) {
            graphics.fill(x, top, x + width, yBottom, tint);
            return;
        }
        graphics.enableScissor(x, top, x + width, yBottom);
        for (int ty = yBottom - 16; ty > top - 16; ty -= 16) {
            for (int tx = x; tx < x + width; tx += 16) {
                GuiDraw.blit(graphics, tex, tx, ty, 0F, 0F, Math.min(16, x + width - tx), 16, 16, 16, tint);
            }
        }
        graphics.disableScissor();
    }

    /**
     * 1.7 {@code renderTank(..., orientation 1)}: fills from the left edge to {@code fluid * width / maxFluid};
     * {@code yBottom} is still the bottom edge of the {@code height}-tall gauge.
     */
    public static void tankHorizontal(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int yBottom, int width, int height) {
        int max = tank.getMaxFill();
        int i = max <= 0 ? 0 : tank.getFill() * width / max;
        if (i <= 0) return;
        int top = yBottom - height;
        int tint = 0xFF000000 | (tank.getTankType().clientMetadata().getTint() & 0xFFFFFF);
        Identifier tex = tank.getTankType().clientMetadata().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(tex).isEmpty()) {
            graphics.fill(x, top, x + i, yBottom, tint);
            return;
        }
        graphics.enableScissor(x, top, x + i, yBottom);
        for (int ty = top; ty < yBottom; ty += 16) {
            for (int tx = x; tx < x + i; tx += 16) {
                GuiDraw.blit(graphics, tex, tx, ty, 0F, 0F, 16, 16, 16, 16, tint);
            }
        }
        graphics.disableScissor();
    }

    /** Shorthand for a plain multi-line tooltip at the mouse. */
    public static void tooltip(GuiGraphicsExtractor graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
    }

    /**
     * 1.7 {@code GuiInfoContainer.drawStackText}: each line is an {@code Object[]} of {@code String}s and
     * {@code ItemStack}s; a line holding a stack is 18 tall (text offset +4), a text line 10; the first line adds 2.
     * Empty stacks (1.7 {@code stackSize == 0}) get the red-framed grey backing. Positioned at (x+12, y-12) and
     * flipped/clamped against the screen like 1.7.
     */
    public static void stackText(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight,
                                 List<Object[]> lines, int x, int y) {
        if (lines.isEmpty()) return;
        int height = 0;
        int longest = 0;
        for (Object[] line : lines) {
            int w = 0;
            boolean hasStack = false;
            for (Object o : line) {
                if (o instanceof String s) w += font.width(s);
                else { w += 18; hasStack = true; }
            }
            height += hasStack ? 18 : 10;
            if (w > longest) longest = w;
        }
        int minX = x + 12;
        int minY = y - 12;
        if (minX + longest > screenWidth) minX -= 28 + longest;
        if (minY + height + 6 > screenHeight) minY = screenHeight - height - 6;

        GuiDraw.nextStratum(graphics);
        int bg = 0xF0100010;
        GuiDraw.fill(graphics, minX - 3, minY - 4, minX + longest + 3, minY - 3, bg);
        GuiDraw.fill(graphics, minX - 3, minY + height + 3, minX + longest + 3, minY + height + 4, bg);
        GuiDraw.fill(graphics, minX - 3, minY - 3, minX + longest + 3, minY + height + 3, bg);
        GuiDraw.fill(graphics, minX - 4, minY - 3, minX - 3, minY + height + 3, bg);
        GuiDraw.fill(graphics, minX + longest + 3, minY - 3, minX + longest + 4, minY + height + 3, bg);
        int c0 = 0x505000FF;
        int c1 = (c0 & 0xFEFEFE) >> 1 | c0 & 0xFF000000;
        GuiDraw.fillGradient(graphics, minX - 3, minY - 2, minX - 2, minY + height + 2, c0, c1);
        GuiDraw.fillGradient(graphics, minX + longest + 2, minY - 2, minX + longest + 3, minY + height + 2, c0, c1);
        GuiDraw.fill(graphics, minX - 3, minY - 3, minX + longest + 3, minY - 2, c0);
        GuiDraw.fill(graphics, minX - 3, minY + height + 2, minX + longest + 3, minY + height + 3, c1);

        for (int index = 0; index < lines.size(); index++) {
            Object[] line = lines.get(index);
            int indent = 0;
            boolean hasStack = false;
            for (Object o : line) if (!(o instanceof String)) hasStack = true;
            for (Object o : line) {
                if (o instanceof String s) {
                    GuiDraw.text(graphics, font, s, minX + indent, minY + (hasStack ? 4 : 0), -1, true);
                    indent += font.width(s) + 2;
                } else if (o instanceof ItemStack stack) {
                    if (stack.isEmpty()) {
                        GuiDraw.fill(graphics, minX + indent - 1, minY - 1, minX + indent + 17, minY + 17, 0xFFFF0000);
                        GuiDraw.fill(graphics, minX + indent, minY, minX + indent + 16, minY + 16, 0xFFB0B0B0);
                    }
                    GuiDraw.item(graphics, stack, minX + indent, minY);
                    GuiDraw.itemDecorations(graphics, font, stack, minX + indent, minY);
                    indent += 18;
                }
            }
            if (index == 0) minY += 2;
            minY += hasStack ? 18 : 10;
        }
    }

    /**
     * 1.7 {@code GUIElements.drawSmoothTextureModalCircle}: reveals the {@code w×h} texture region at (u, v) as a
     * pie sector around its centre, starting at the bottom-left corner and sweeping clockwise through
     * {@code progress × 270°} (clamped to 0..1). 1.7 draws textured triangles out to the region's border; here each
     * row's covered runs (pixel-centre test against the same sector) are blitted as 1-pixel-tall strips.
     */
    public static void smoothCircle(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v, int w, int h,
                                    double progress) {
        progress = Math.max(0D, Math.min(1D, progress));
        double sweep = progress * 270D;
        double cx = w / 2D, cy = h / 2D;
        for (int row = 0; row < h; row++) {
            int runStart = -1;
            for (int col = 0; col <= w; col++) {
                boolean in = false;
                if (col < w) {
                    double dx = col + 0.5D - cx;
                    double dy = cy - (row + 0.5D); // math orientation, y up
                    double phi = Math.toDegrees(Math.atan2(dy, dx));
                    double d = ((-135D - phi) % 360D + 360D) % 360D; // clockwise distance from the bottom-left ray
                    in = d <= sweep;
                }
                if (in && runStart < 0) runStart = col;
                if (!in && runStart >= 0) {
                    GuiDraw.blit(graphics, texture, x + runStart, y + row, (float) (u + runStart), (float) (v + row), col - runStart, 1, 256, 256);
                    runStart = -1;
                }
            }
        }
    }

    /** 1.7 {@code I18nUtil.resolveKeyArray}: one translation, split on {@code $}. */
    public static List<Component> keyArray(String key, Object... args) {
        return lines(Component.translatable(key, args).getString().split("\\$"));
    }

    /** Shorthand for literal lines. */
    public static List<Component> lines(String... text) {
        List<Component> out = new ArrayList<>(text.length);
        for (String s : text) out.add(Component.literal(s));
        return out;
    }
}
