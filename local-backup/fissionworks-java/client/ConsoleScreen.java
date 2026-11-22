package fissionworks.client;

import fissionworks.Net;
import fissionworks.ReactorBE;
import fissionworks.sim.Reactor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The reactor console. Left: the 5 x 5 column grid as seen from the panel (front row at the bottom).
 * Middle: one slider per colour group, down means rods in. Right: AZ-5. Top: power gauge.
 * Left-click selects a rod, wheel moves it, right-click changes its group.
 */
public class ConsoleScreen extends Screen {
    static final int W = 330, H = 204, CELL = 22, STEP = 24;
    static final int GX = 12, GY = 46;
    static final int SX = 150, SGAP = 26, SW = 14, ST = 56, SH = 104;
    static final int AX = 274, AY = 100, AW = 44, AH = 60;

    static final int RIV = 0xFF0F151C, PANEL = 0xFF1D2733, MID = 0xFF3A4A5E, LIGHT = 0xFF6F87A3, HI = 0xFFA9C1DA;
    static final int TEAL = 0xFF5FF0D0, TEAL_D = 0xFF206E62, RED = 0xFFE0525A, WHITE = 0xFFFFFFFF;
    static final int[] GROUP = {0xFFE0525A, 0xFFF2B441, 0xFF6ED17A, 0xFF5AA0F0, 0xFFA77BE8};

    private final BlockPos pos;
    private int selected = -1;
    private int dragGroup = -1;
    private int lastSent = -1;

    public ConsoleScreen(BlockPos pos) {
        super(Component.translatable("gui.fissionworks.title"));
        this.pos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int ox() { return (width - W) / 2; }
    private int oy() { return (height - H) / 2; }

    private Reactor reactor() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof ReactorBE be ? be.reactor() : null;
    }

    private static int lerp(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int out = 0xFF000000;
        for (int s = 0; s <= 16; s += 8) {
            int ca = (a >> s) & 255, cb = (b >> s) & 255;
            out |= (int) Math.round(ca + (cb - ca) * t) << s;
        }
        return out;
    }

    private static boolean blink() {
        return (System.currentTimeMillis() / 300) % 2 == 0;
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        Reactor r = reactor();
        if (r == null) {
            onClose();
            return;
        }
        int ox = ox(), oy = oy();
        g.fill(ox, oy, ox + W, oy + H, RIV);
        g.fill(ox + 1, oy + 1, ox + W - 1, oy + H - 1, PANEL);
        g.fill(ox + 1, oy + 1, ox + W - 1, oy + 2, MID);
        header(g, r, ox, oy);
        List<Component> tip = new ArrayList<>();
        for (int z = 0; z < r.h; z++) {
            for (int x = 0; x < r.w; x++) {
                int cx = ox + GX + x * STEP, cy = oy + GY + (r.h - 1 - z) * STEP;
                boolean hover = mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL;
                drawCell(g, r, x, z, cx, cy);
                if (z * r.w + x == selected) {
                    outline(g, cx - 1, cy - 1, CELL + 2, CELL + 2, WHITE);
                } else if (hover) {
                    outline(g, cx - 1, cy - 1, CELL + 2, CELL + 2, LIGHT);
                    tip = tooltip(r, x, z);
                }
            }
        }
        for (int gr = 0; gr < Reactor.GROUPS; gr++) {
            slider(g, r, gr, ox + SX + gr * SGAP, oy + ST);
        }
        azButton(g, r, mx, my, ox, oy);
        g.drawString(font, selectedInfo(r), ox + 12, oy + H - 28, HI, false);
        g.drawString(font, Component.translatable("gui.fissionworks.hint"), ox + 12, oy + H - 16, LIGHT, false);
        if (!tip.isEmpty()) {
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }

    private void header(GuiGraphics g, Reactor r, int ox, int oy) {
        g.drawString(font, title, ox + 12, oy + 8, HI, false);
        Component counts = Component.translatable("gui.fissionworks.counts", r.count(Reactor.Kind.FUEL),
                r.count(Reactor.Kind.ROD));
        g.drawString(font, counts, ox + W - 12 - font.width(counts), oy + 8, LIGHT, false);
        int x = ox + 12, y = oy + 24, w = W - 24;
        boolean alarm = r.runaway();
        g.fill(x, y, x + w, y + 14, RIV);
        int fill = (int) ((w - 2) * Math.min(100, r.power()) / 100);
        g.fill(x + 1, y + 1, x + 1 + fill, y + 13, alarm && blink() ? RED : TEAL_D);
        g.fill(x + 1, y + 1, x + 1 + fill, y + 5, alarm && blink() ? 0xFFFF8088 : TEAL);
        Component label = alarm ? Component.translatable("gui.fissionworks.runaway")
                : Component.translatable("gui.fissionworks.power", Math.round(r.power()));
        g.drawString(font, label, x + 5, y + 3, WHITE, true);
    }

    private void drawCell(GuiGraphics g, Reactor r, int x, int z, int cx, int cy) {
        switch (r.kind(x, z)) {
            case FUEL -> {
                double t = r.flux(x, z) / Reactor.CAP;
                g.fill(cx, cy, cx + CELL, cy + CELL, RIV);
                g.fill(cx + 3, cy + 3, cx + CELL - 3, cy + CELL - 3, lerp(TEAL_D, TEAL, t));
                g.fill(cx + 7, cy + 7, cx + CELL - 7, cy + CELL - 7, lerp(MID, HI, t));
            }
            case ROD -> rodCell(g, r, x, z, cx, cy);
            case MODERATOR -> {
                g.fill(cx, cy, cx + CELL, cy + CELL, 0xFF2A2F36);
                for (int k = 5; k < CELL; k += 5) {
                    g.fill(cx + k, cy, cx + k + 1, cy + CELL, 0xFF3C434D);
                    g.fill(cx, cy + k, cx + CELL, cy + k + 1, 0xFF3C434D);
                }
                g.drawCenteredString(font, "M", cx + CELL / 2, cy + CELL / 2 - 4, 0xFF9AA4B1);
            }
            case REFLECTOR -> {
                g.fill(cx, cy, cx + CELL, cy + CELL, LIGHT);
                g.fill(cx, cy, cx + CELL, cy + 2, HI);
                g.drawCenteredString(font, "R", cx + CELL / 2, cy + CELL / 2 - 4, RIV);
            }
            case ABSORBER -> {
                g.fill(cx, cy, cx + CELL, cy + CELL, 0xFF0B0F14);
                g.fill(cx, cy, cx + CELL, cy + 2, 0xFFE0B040);
                g.fill(cx, cy + CELL - 2, cx + CELL, cy + CELL, 0xFFE0B040);
                g.drawCenteredString(font, "A", cx + CELL / 2, cy + CELL / 2 - 4, 0xFFE0B040);
            }
            default -> {
                g.fill(cx, cy, cx + CELL, cy + CELL, 0xFF18212C);
                g.fill(cx + CELL / 2 - 1, cy + CELL / 2 - 1, cx + CELL / 2 + 1, cy + CELL / 2 + 1, MID);
            }
        }
    }

    private void rodCell(GuiGraphics g, Reactor r, int x, int z, int cx, int cy) {
        int col = GROUP[r.group(x, z)];
        g.fill(cx, cy, cx + CELL, cy + CELL, RIV);
        outline(g, cx, cy, CELL, CELL, col);
        outline(g, cx + 1, cy + 1, CELL - 2, CELL - 2, col);
        int bx = cx + 9, by = cy + 4, bw = 9, bh = CELL - 8;
        g.fill(bx, by, bx + bw, by + bh, 0xFF2A3544);
        g.fill(bx, by, bx + bw, by + bh * r.level(x, z) / 100, col);
        int ty = by + bh * r.target(x, z) / 100;
        g.fill(bx - 2, ty, bx + bw + 2, ty + 1, WHITE);
        g.drawString(font, String.valueOf(r.group(x, z) + 1), cx + 3, cy + CELL - 10, col, true);
    }

    private void slider(GuiGraphics g, Reactor r, int gr, int x, int top) {
        int col = GROUP[gr];
        int avg = groupAverage(r, gr, true), now = groupAverage(r, gr, false);
        g.fill(x, top - 16, x + SW, top - 4, col);
        g.drawCenteredString(font, String.valueOf(gr + 1), x + SW / 2, top - 14, RIV);
        g.fill(x, top, x + SW, top + SH, RIV);
        g.fill(x + 5, top + 2, x + 9, top + SH - 2, 0xFF2A3544);
        if (avg < 0) {
            g.drawCenteredString(font, "-", x + SW / 2, top + SH + 4, MID);
            return;
        }
        int ky = top + SH * avg / 100;
        g.fill(x + 5, top + 2, x + 9, Math.max(top + 2, ky), col);
        g.fill(x - 1, ky - 3, x + SW + 1, ky + 3, HI);
        g.fill(x, ky - 1, x + SW, ky + 1, col);
        g.fill(x + SW + 1, top + SH * now / 100 - 1, x + SW + 4, top + SH * now / 100 + 1, WHITE);
        g.drawCenteredString(font, avg + "%", x + SW / 2, top + SH + 4, HI);
    }

    private void azButton(GuiGraphics g, Reactor r, int mx, int my, int ox, int oy) {
        int x = ox + AX, y = oy + AY;
        boolean hover = mx >= x && mx < x + AW && my >= y && my < y + AH;
        boolean lit = r.scramming() || hover;
        g.fill(x, y, x + AW, y + AH, RIV);
        g.fill(x + 2, y + 2, x + AW - 2, y + AH - 2, lit ? 0xFFE0525A : 0xFF9C343B);
        g.fill(x + 2, y + 2, x + AW - 2, y + 6, lit ? 0xFFFF8088 : 0xFFC4545B);
        g.drawCenteredString(font, "AZ-5", x + AW / 2, y + AH / 2 - 4, WHITE);
    }

    private Component selectedInfo(Reactor r) {
        if (selected < 0 || r.kind(selected % r.w, selected / r.w) != Reactor.Kind.ROD) {
            return Component.translatable("gui.fissionworks.no_rod");
        }
        int x = selected % r.w, z = selected / r.w;
        return Component.translatable("gui.fissionworks.rod_info", x + 1, z + 1, r.level(x, z), r.target(x, z),
                Component.translatable("group.fissionworks." + r.group(x, z)));
    }

    private List<Component> tooltip(Reactor r, int x, int z) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("kind.fissionworks." + r.kind(x, z).name().toLowerCase()));
        if (r.kind(x, z) == Reactor.Kind.FUEL) {
            lines.add(Component.translatable("gui.fissionworks.flux", Math.round(r.flux(x, z))));
        } else if (r.kind(x, z) == Reactor.Kind.ROD) {
            lines.add(Component.translatable("gui.fissionworks.rod_line", r.level(x, z), r.target(x, z)));
        }
        return lines;
    }

    /** Average target (or current level) of the rods in a group, or -1 if the group has none. */
    private static int groupAverage(Reactor r, int gr, boolean target) {
        int sum = 0, n = 0;
        for (int z = 0; z < r.h; z++) {
            for (int x = 0; x < r.w; x++) {
                if (r.kind(x, z) == Reactor.Kind.ROD && r.group(x, z) == gr) {
                    sum += target ? r.target(x, z) : r.level(x, z);
                    n++;
                }
            }
        }
        return n == 0 ? -1 : Math.round((float) sum / n);
    }

    private void send(int op, int a, int b) {
        PacketDistributor.sendToServer(new Net.Command(pos, op, a, b));
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private int cellAt(Reactor r, double mx, double my) {
        int x = (int) Math.floor((mx - ox() - GX) / STEP), row = (int) Math.floor((my - oy() - GY) / STEP);
        boolean inside = (mx - ox() - GX) - x * STEP < CELL && (my - oy() - GY) - row * STEP < CELL;
        int z = r.h - 1 - row;
        return inside && x >= 0 && x < r.w && z >= 0 && z < r.h ? z * r.w + x : -1;
    }

    private int sliderAt(double mx, double my) {
        for (int gr = 0; gr < Reactor.GROUPS; gr++) {
            int x = ox() + SX + gr * SGAP;
            if (mx >= x - 2 && mx < x + SW + 2 && my >= oy() + ST - 4 && my < oy() + ST + SH + 4) {
                return gr;
            }
        }
        return -1;
    }

    private void dragSlider(double my) {
        int pct = (int) Math.max(0, Math.min(100, Math.round((my - oy() - ST) / SH * 100)));
        if (pct != lastSent) {
            lastSent = pct;
            send(ReactorBE.OP_GROUP, dragGroup, pct);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Reactor r = reactor();
        if (r == null) {
            return super.mouseClicked(mx, my, button);
        }
        int cell = cellAt(r, mx, my);
        if (cell >= 0) {
            boolean rod = r.kind(cell % r.w, cell / r.w) == Reactor.Kind.ROD;
            if (rod && button == 1) {
                send(ReactorBE.OP_CYCLE, cell, 0);
            }
            selected = rod ? cell : -1;
            return true;
        }
        int gr = sliderAt(mx, my);
        if (gr >= 0 && button == 0) {
            dragGroup = gr;
            lastSent = -1;
            dragSlider(my);
            return true;
        }
        if (mx >= ox() + AX && mx < ox() + AX + AW && my >= oy() + AY && my < oy() + AY + AH) {
            click();
            send(ReactorBE.OP_SCRAM, 0, 0);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragGroup >= 0) {
            dragSlider(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragGroup = -1;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        Reactor r = reactor();
        if (r != null && selected >= 0 && r.kind(selected % r.w, selected / r.w) == Reactor.Kind.ROD) {
            int now = r.target(selected % r.w, selected / r.w);
            send(ReactorBE.OP_ROD, selected, Math.max(0, Math.min(100, now + (sy > 0 ? -5 : 5))));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }
}
