package fissionworks.client;

import fissionworks.Net;
import fissionworks.ZirnoxBE;
import fissionworks.sim.Zirnox;
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
 * ZIRNOX console. Left: 6 x 4 fuel channels and the buttons. Middle: temperature and pressure gauges
 * with their meltdown limits, then three tank bars. Bottom right: a trace of how close temperature and
 * pressure are to meltdown (top of the chart = meltdown). Click a channel to load the rod in hand or take one out.
 */
public class ZirnoxScreen extends Screen {
    static final int W = 368, H = 224, CELL = 30, STEP = 32, GX = 12, GY = 34;
    static final int GY0 = 40, GH = 112, GW = 20;
    static final int[] GXS = {212, 240, 272, 302, 332};
    static final int BX = 12, B1Y = 170, B2Y = 196, BH = 20;
    static final int CX = 214, CY = 174, CW = 142, CH = 34;

    static final int RIV = 0xFF0F151C, PANEL = 0xFF1D2733, MID = 0xFF3A4A5E, LIGHT = 0xFF6F87A3, HI = 0xFFA9C1DA;
    static final int TEAL = 0xFF5FF0D0, TEAL_D = 0xFF206E62, RED = 0xFFE0525A, AMBER = 0xFFF2B441, WHITE = 0xFFFFFFFF;
    static final int TRACK = 0xFF2A3544;

    private final BlockPos pos;
    private final float[] temps = new float[71], press = new float[71];
    private int seen;

    public ZirnoxScreen(BlockPos pos) {
        super(Component.translatable("gui.fissionworks.zirnox.title"));
        this.pos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int ox() { return (width - W) / 2; }
    private int oy() { return (height - H) / 2; }

    private Zirnox zirnox() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof ZirnoxBE be ? be.model() : null;
    }

    @Override
    public void tick() {
        Zirnox z = zirnox();
        if (z != null && ++seen % 10 == 0) {
            System.arraycopy(temps, 1, temps, 0, temps.length - 1);
            System.arraycopy(press, 1, press, 0, press.length - 1);
            temps[temps.length - 1] = (float) (z.temp() / Zirnox.MELT_TEMP);
            press[press.length - 1] = (float) (z.pressure() / Zirnox.MELT_PRESSURE);
        }
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

    /** Teal when safe, amber when close, red near the limit. */
    private static int zone(double fraction) {
        return fraction < 0.62 ? TEAL : fraction < 0.88 ? AMBER : RED;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        Zirnox z = zirnox();
        if (z == null) {
            onClose();
            return;
        }
        int ox = ox(), oy = oy();
        g.fill(ox, oy, ox + W, oy + H, RIV);
        g.fill(ox + 1, oy + 1, ox + W - 1, oy + H - 1, PANEL);
        g.fill(ox + 1, oy + 1, ox + W - 1, oy + 2, MID);
        header(g, z, ox, oy);
        List<Component> tip = new ArrayList<>();
        for (int s = 0; s < Zirnox.SLOTS; s++) {
            int cx = ox + GX + (s % Zirnox.COLS) * STEP, cy = oy + GY + (s / Zirnox.COLS) * STEP;
            drawChannel(g, z, s, cx, cy);
            if (mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL) {
                outline(g, cx - 1, cy - 1, CELL + 2, CELL + 2, WHITE);
                tip = channelTip(z, s);
            }
        }
        List<Component> gtip = gauges(g, z, ox, oy, mx, my);
        if (!gtip.isEmpty()) {
            tip = gtip;
        }
        buttons(g, z, ox, oy, mx, my);
        chart(g, ox, oy);
        if (!tip.isEmpty()) {
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }

    private void header(GuiGraphics g, Zirnox z, int ox, int oy) {
        g.drawString(font, title, ox + 12, oy + 8, HI, false);
        boolean warn = z.temp() / Zirnox.MELT_TEMP > 0.88 || z.pressure() / Zirnox.MELT_PRESSURE > 0.88;
        String key = z.melted() ? "melted" : warn ? "warning" : z.running() ? "running" : "stopped";
        int col = z.melted() || (warn && blink()) ? RED : warn ? AMBER : z.running() ? TEAL : LIGHT;
        Component status = Component.translatable("gui.fissionworks.zirnox." + key);
        int w = font.width(status) + 10, x = ox + W - 12 - w;
        g.fill(x, oy + 5, x + w, oy + 18, RIV);
        g.fill(x, oy + 5, x + 3, oy + 18, col);
        g.drawString(font, status, x + 7, oy + 8, col, false);
        Component rate = Component.translatable("gui.fissionworks.zirnox.rate", Math.round(z.steamRate() * 20));
        g.drawString(font, rate, x - 10 - font.width(rate), oy + 8, LIGHT, false);
    }

    private static int fuelColor(Zirnox.Fuel f) {
        return switch (f) {
            case NATURAL_URANIUM -> 0xFF9DB56E;
            case URANIUM -> 0xFF5FF0D0;
            case THORIUM -> 0xFFA77BE8;
            default -> 0xFF5A2B2F;
        };
    }

    private void drawChannel(GuiGraphics g, Zirnox z, int s, int cx, int cy) {
        Zirnox.Fuel f = z.fuel(s);
        g.fill(cx, cy, cx + CELL, cy + CELL, RIV);
        if (f == Zirnox.Fuel.NONE) {
            g.fill(cx + 13, cy + 13, cx + 17, cy + 17, TRACK);
            return;
        }
        int col = fuelColor(f);
        boolean live = f != Zirnox.Fuel.SPENT;
        g.fill(cx + 3, cy + 3, cx + CELL - 3, cy + CELL - 3, live ? (z.running() ? col : TRACK) : col);
        g.fill(cx + 9, cy + 9, cx + CELL - 9, cy + CELL - 9, live ? HI : 0xFF2A1518);
        int bar = (int) Math.round((CELL - 6) * (1 - z.burn(s)));
        g.fill(cx + 1, cy + 3, cx + 3, cy + CELL - 3, RIV);
        g.fill(cx + 1, cy + CELL - 3 - bar, cx + 3, cy + CELL - 3, live ? col : RED);
    }

    private List<Component> channelTip(Zirnox z, int s) {
        List<Component> lines = new ArrayList<>();
        Zirnox.Fuel f = z.fuel(s);
        lines.add(Component.translatable("fuel.fissionworks." + f.name().toLowerCase()));
        if (f != Zirnox.Fuel.NONE && f != Zirnox.Fuel.SPENT) {
            lines.add(Component.translatable("gui.fissionworks.zirnox.burn", Math.round(z.burn(s) * 100)));
        }
        return lines;
    }

    private void bar(GuiGraphics g, int x, int y, double fraction, int color) {
        g.fill(x, y, x + GW, y + GH, RIV);
        g.fill(x + 2, y + 2, x + GW - 2, y + GH - 2, TRACK);
        int h = (int) Math.round((GH - 4) * Math.max(0, Math.min(1, fraction)));
        g.fill(x + 2, y + GH - 2 - h, x + GW - 2, y + GH - 2, color);
    }

    private void mark(GuiGraphics g, int x, int y, double fraction, int color) {
        int ty = y + GH - 2 - (int) Math.round((GH - 4) * fraction);
        g.fill(x - 2, ty, x + GW + 2, ty + 1, color);
    }

    private List<Component> gauges(GuiGraphics g, Zirnox z, int ox, int oy, int mx, int my) {
        double[] frac = {z.temp() / 900.0, z.pressure() / 35.0, z.co2() / (double) Zirnox.CO2_CAP,
                z.steam() / (double) Zirnox.STEAM_CAP, z.water() / (double) Zirnox.WATER_CAP};
        String[] labels = {"\u00B0C", "bar", "CO2", "STM", "H2O"};
        String[] values = {Math.round(z.temp()) + "", String.format("%.1f", z.pressure()), z.co2() + "", z.steam() + "",
                z.water() + ""};
        String[] shown = {values[0], values[1], kilo(z.co2()), kilo(z.steam()), kilo(z.water())};
        int[] colors = {zone(z.temp() / Zirnox.MELT_TEMP), zone(z.pressure() / Zirnox.MELT_PRESSURE), 0xFFB8C4D0,
                0xFFE6F2FF, 0xFF5AA0F0};
        List<Component> tip = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int x = ox + GXS[i], y = oy + GY0;
            bar(g, x, y, frac[i], colors[i]);
            g.drawCenteredString(font, labels[i], x + GW / 2, y - 12, LIGHT);
            g.drawCenteredString(font, shown[i], x + GW / 2, y + GH + 4, HI);
            if (mx >= x && mx < x + GW && my >= y && my < y + GH) {
                tip.add(Component.translatable("gui.fissionworks.zirnox.gauge." + i, values[i]));
            }
        }
        mark(g, ox + GXS[0], oy + GY0, Zirnox.MELT_TEMP / 900.0, RED);
        mark(g, ox + GXS[1], oy + GY0, Zirnox.MELT_PRESSURE / 35.0, RED);
        mark(g, ox + GXS[2], oy + GY0, Zirnox.MIN_CO2 / (double) Zirnox.CO2_CAP, AMBER);
        mark(g, ox + GXS[4], oy + GY0, Zirnox.MIN_WATER / (double) Zirnox.WATER_CAP, AMBER);
        return tip;
    }

    /** Tank amounts in thousands of mB so five columns fit side by side. */
    private static String kilo(int mb) {
        return String.format("%.1fk", mb / 1000.0);
    }

    private void button(GuiGraphics g, int x, int y, int w, Component label, int accent, boolean on, boolean hover) {
        g.fill(x, y, x + w, y + BH, RIV);
        g.fill(x + 1, y + 1, x + w - 1, y + BH - 1, hover && on ? MID : 0xFF263241);
        g.fill(x + 1, y + 1, x + 4, y + BH - 1, on ? accent : TRACK);
        g.drawString(font, label, x + 8, y + 6, on ? HI : MID, false);
    }

    private int[][] rects(int ox, int oy) {
        return new int[][] {{ox + BX, oy + B1Y, 190}, {ox + BX, oy + B2Y, 60}, {ox + BX + 65, oy + B2Y, 60},
                {ox + BX + 130, oy + B2Y, 60}};
    }

    private void buttons(GuiGraphics g, Zirnox z, int ox, int oy, int mx, int my) {
        int[][] r = rects(ox, oy);
        Component[] labels = {Component.translatable(z.running() ? "gui.fissionworks.zirnox.stop" : "gui.fissionworks.zirnox.start"),
                Component.translatable("gui.fissionworks.zirnox.vent"), Component.translatable("gui.fissionworks.zirnox.canister"),
                Component.translatable("gui.fissionworks.zirnox.bucket")};
        int[] accents = {z.running() ? RED : TEAL, AMBER, 0xFFB8C4D0, 0xFF5AA0F0};
        for (int i = 0; i < 4; i++) {
            boolean hover = mx >= r[i][0] && mx < r[i][0] + r[i][2] && my >= r[i][1] && my < r[i][1] + BH;
            button(g, r[i][0], r[i][1], r[i][2], labels[i], accents[i], !z.melted(), hover);
        }
    }

    private void chart(GuiGraphics g, int ox, int oy) {
        int x = ox + CX, y = oy + CY;
        g.fill(x, y, x + CW, y + CH, RIV);
        g.fill(x, y + 4, x + CW, y + 5, RED);
        g.drawString(font, Component.translatable("gui.fissionworks.zirnox.chart"), x + 3, y + CH + 2, LIGHT, false);
        for (int i = 0; i < temps.length; i++) {
            int px = x + 1 + i * 2;
            for (int k = 0; k < 2; k++) {
                int ty = y + CH - 2 - (int) ((CH - 6) * Math.min(1f, temps[i])), py = y + CH - 2 - (int) ((CH - 6) * Math.min(1f, press[i]));
                g.fill(px, ty, px + 1, ty + 1, TEAL);
                g.fill(px, py, px + 1, py + 1, AMBER);
            }
        }
    }

    private void send(int op, int a) {
        PacketDistributor.sendToServer(new Net.Command(pos, op, a, 0));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Zirnox z = zirnox();
        if (z == null) {
            return super.mouseClicked(mx, my, button);
        }
        for (int s = 0; s < Zirnox.SLOTS; s++) {
            int cx = ox() + GX + (s % Zirnox.COLS) * STEP, cy = oy() + GY + (s / Zirnox.COLS) * STEP;
            if (mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL) {
                send(z.fuel(s) == Zirnox.Fuel.NONE ? ZirnoxBE.OP_LOAD : ZirnoxBE.OP_UNLOAD, s);
                return true;
            }
        }
        int[][] r = rects(ox(), oy());
        int[] ops = {ZirnoxBE.OP_RODS, ZirnoxBE.OP_VENT, ZirnoxBE.OP_CANISTER, ZirnoxBE.OP_BUCKET};
        for (int i = 0; i < 4; i++) {
            if (mx >= r[i][0] && mx < r[i][0] + r[i][2] && my >= r[i][1] && my < r[i][1] + BH) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                send(ops[i], 0);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
