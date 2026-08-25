package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuRBMKConsole;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.RBMKColumnType;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** The 15 by 15 RBMK scanner, monitor bank, setpoint, and AZ-5 controls. */
public final class ScreenRBMKConsole extends RBMKContainerScreen<MenuRBMKConsole> {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/rbmk/gui_rbmk_console.png");
    private final boolean[] selection = new boolean[15 * 15];
    private boolean az5Lid = true;
    private long lastPress;
    private EditBox field;

    public ScreenRBMKConsole(MenuRBMKConsole menu, Inventory inventory, Component title) { super(menu, inventory, title, 244, 172); }

    @Override protected void init() {
        super.init();
        field = new EditBox(font, leftPos + 9, topPos + 84, 35, 9, Component.empty());
        field.setBordered(false);
        field.setMaxLength(3);
        field.setTextColor(0xFF00FF00);
        addRenderableWidget(field);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        texture(graphics, TEX);
        BlockEntityRBMKConsole console = menu.blockEntity();
        if (az5Lid) GuiDraw.blit(graphics, TEX, leftPos + 30, topPos + 138, 228, 172, 28, 28, 256, 256);
        else if (lastPress + 3000 >= System.currentTimeMillis()) GuiDraw.blit(graphics, TEX, leftPos + 30, topPos + 136, 228, 228, 28, 28, 256, 256);
        if (console == null) return;
        for (int j = 0; j < 3; j++) for (int k = 0; k < 2; k++) {
            int index = j * 2 + k;
            GuiDraw.blit(graphics, TEX, leftPos + 6 + 40 * k, topPos + 8 + 21 * j, console.screens[index].type.offset, 238, 18, 18, 256, 256);
        }
        drawGrid(graphics, console);
        drawFluxGraph(graphics, console);
    }

    private void drawGrid(GuiGraphicsExtractor graphics, BlockEntityRBMKConsole console) {
        int steam = RBMKColumn.fluidId(Fluids.STEAM), hot = RBMKColumn.fluidId(Fluids.HOTSTEAM);
        int superhot = RBMKColumn.fluidId(Fluids.SUPERHOTSTEAM), ultra = RBMKColumn.fluidId(Fluids.ULTRAHOTSTEAM);
        for (int i = 0; i < console.columns.length; i++) {
            RBMKColumn column = console.columns[i];
            if (column == null) continue;
            int x = leftPos + 86 + 10 * (i % 15), y = topPos + 11 + 10 * (i / 15);
            GuiDraw.blit(graphics, TEX, x, y, column.type.offset, 172, 10, 10, 256, 256);
            int heat = (int) Math.min(Math.ceil((column.heat - 20) * 10 / column.maxHeat), 10);
            if (heat > 0) GuiDraw.blit(graphics, TEX, x, y + 10 - heat, 0, 192 - heat, 10, heat, 256, 256);
            switch (column.type) {
                case CONTROL, CONTROL_AUTO -> {
                    RBMKColumn.ControlColumn control = (RBMKColumn.ControlColumn) column;
                    if (column.type == RBMKColumnType.CONTROL && control.color >= 0) GuiDraw.blit(graphics, TEX, x, y, control.color * 10, 202, 10, 10, 256, 256);
                    int withdrawn = 8 - (int) Math.ceil(control.level * 8);
                    if (withdrawn > 0) GuiDraw.blit(graphics, TEX, x + 4, y + 1, 24, 183, 2, withdrawn, 256, 256);
                }
                case COOLER -> {
                    RBMKColumn.CoolerColumn cooler = (RBMKColumn.CoolerColumn) column;
                    int fill = (int) Math.ceil((double) cooler.cryo * 8 / Math.max(cooler.maxCryo, 1));
                    if (fill > 0) GuiDraw.blit(graphics, TEX, x + 3, y + 9 - fill, 123, 191 - fill, 4, fill, 256, 256);
                }
                case FUEL, FUEL_SIM, BREEDER -> {
                    RBMKColumn.FuelColumn fuel = (RBMKColumn.FuelColumn) column;
                    if (fuel.c_maxHeat > 0) {
                        bar(graphics, x + 1, y, 11, fuel.c_heat - 20, fuel.c_maxHeat);
                        bar(graphics, x + 4, y, 14, fuel.enrichment, 1);
                        bar(graphics, x + 7, y, 17, fuel.xenon, 100);
                    }
                }
                case BOILER -> {
                    RBMKColumn.BoilerColumn boiler = (RBMKColumn.BoilerColumn) column;
                    bar(graphics, x + 1, y, 41, boiler.water, boiler.maxWater);
                    bar(graphics, x + 6, y, 46, boiler.steam, boiler.maxSteam);
                    int marker = boiler.steamType == steam ? 183 : boiler.steamType == hot ? 185 : boiler.steamType == superhot ? 187 : boiler.steamType == ultra ? 189 : -1;
                    if (marker >= 0) GuiDraw.blit(graphics, TEX, x + 4, y + (marker - 183), 44, marker, 2, 2, 256, 256);
                }
                case HEATEX -> {
                    RBMKColumn.HeaterColumn heater = (RBMKColumn.HeaterColumn) column;
                    bar(graphics, x + 1, y, 131, heater.water, heater.maxWater);
                    bar(graphics, x + 6, y, 136, heater.steam, heater.maxSteam);
                }
                default -> { }
            }
            if (selection[i]) GuiDraw.blit(graphics, TEX, x, y, 0, 192, 10, 10, 256, 256);
        }
    }

    private static void line(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, error = dx - dy;
        while (true) { graphics.fill(x0, y0, x0 + 1, y0 + 1, 0xFF00FF00); if (x0 == x1 && y0 == y1) return; int twice = error * 2; if (twice > -dy) { error -= dy; x0 += sx; } if (twice < dx) { error += dx; y0 += sy; } }
    }

    private void drawFluxGraph(GuiGraphicsExtractor graphics, BlockEntityRBMKConsole console) {
        int highest = Integer.MIN_VALUE, lowest = Integer.MAX_VALUE;
        for (int flux : console.fluxBuffer) { highest = Math.max(highest, flux); lowest = Math.min(lowest, flux); }
        int range = Math.max(highest - lowest, 1);
        for (int i = 0; i < console.fluxBuffer.length - 1; i++) line(graphics, leftPos + (int) (7 + i * 74D / console.fluxBuffer.length), topPos + (int) (127 - (console.fluxBuffer[i] - lowest) * 24D / range), leftPos + (int) (7 + (i + 1) * 74D / console.fluxBuffer.length), topPos + (int) (127 - (console.fluxBuffer[i + 1] - lowest) * 24D / range));
        dev.golem.core.gui.GuiDraw.pushMatrix(graphics);
        dev.golem.core.gui.GuiDraw.scale(graphics, .5F, .5F);
        String high = Integer.toString(highest), low = Integer.toString(lowest);
        GuiDraw.text(graphics, font, Component.literal(high), (leftPos + 8) * 2, (topPos + 98) * 2, 0xFF00FF00, false);
        GuiDraw.text(graphics, font, Component.literal(high), (int) ((leftPos + 80 - font.width(high) * .5F) * 2), (topPos + 98) * 2, 0xFF00FF00, false);
        GuiDraw.text(graphics, font, Component.literal(low), (leftPos + 8) * 2, (int) ((topPos + 133 - font.lineHeight * .5F) * 2), 0xFF00FF00, false);
        GuiDraw.text(graphics, font, Component.literal(low), (int) ((leftPos + 80 - font.width(low) * .5F) * 2), (int) ((topPos + 133 - font.lineHeight * .5F) * 2), 0xFF00FF00, false);
        dev.golem.core.gui.GuiDraw.popMatrix(graphics);
    }

    private void bar(GuiGraphicsExtractor graphics, int x, int y, int u, double value, double max) {
        if (max <= 0) return;
        int fill = Math.min((int) Math.ceil(value * 8 / max), 8);
        if (fill > 0) GuiDraw.blit(graphics, TEX, x, y + 9 - fill, u, 191 - fill, u == 41 || u == 46 || u == 131 || u == 136 ? 3 : 2, fill, 256, 256);
    }

    @Override protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityRBMKConsole console = menu.blockEntity();
        if (console != null && region(86, 11, 150, 150, mouseX, mouseY)) {
            int index = (mouseX - leftPos - 86) / 10 + (mouseY - topPos - 11) / 10 * 15;
            if (console.columns[index] != null) { List<Component> lines = new ArrayList<>(); lines.add(Component.literal(console.columns[index].type.toString())); lines.addAll(console.columns[index].getFancyStats()); tooltip(graphics, lines, mouseX, mouseY); }
        }
        if (region(61, 70, 10, 10, mouseX, mouseY)) tooltip(graphics, List.of(Component.literal("Select all control rods")), mouseX, mouseY);
        else if (region(72, 70, 10, 10, mouseX, mouseY)) tooltip(graphics, List.of(Component.literal("Deselect all")), mouseX, mouseY);
        else if (region(70, 82, 12, 12, mouseX, mouseY)) tooltip(graphics, List.of(Component.literal("Cycle steam channel compressor setting")), mouseX, mouseY);
        String[] groups = {"red", "yellow", "green", "blue", "purple"};
        for (int i = 0; i < groups.length; i++) if (region(6 + i * 11, 70, 10, 10, mouseX, mouseY)) tooltip(graphics, List.of(Component.literal("Left: select " + groups[i] + " group"), Component.literal("Right: assign " + groups[i] + " group")), mouseX, mouseY);
        if (console != null) for (int j = 0; j < 3; j++) for (int k = 0; k < 2; k++) {
            int slot = j * 2 + k, screen = slot + 1;
            if (region(6 + 40 * k, 8 + 21 * j, 18, 18, mouseX, mouseY)) {
                tooltip(graphics, List.of(Component.literal("Monitor " + screen + ": " + console.screens[slot].type.name())), mouseX, mouseY);
                return;
            }
            if (region(24 + 40 * k, 8 + 21 * j, 18, 18, mouseX, mouseY)) {
                tooltip(graphics, List.of(Component.literal("Assign selected columns to monitor " + screen)), mouseX, mouseY);
                return;
            }
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) { }

    @Override protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        BlockEntityRBMKConsole console = menu.blockEntity();
        int x = (int) event.x(), y = (int) event.y();
        if (console != null && region(86, 11, 150, 150, x, y)) { int index = (x - leftPos - 86) / 10 + (y - topPos - 11) / 10 * 15; if (console.columns[index] != null) { selection[index] = !selection[index]; return true; } }
        if (region(72, 70, 10, 10, x, y)) { Arrays.fill(selection, false); return true; }
        if (console != null && region(61, 70, 10, 10, x, y)) { select(console, column -> column.type == RBMKColumnType.CONTROL); return true; }
        if (console != null && region(70, 82, 12, 12, x, y)) { send("compressor", selected(console, RBMKColumnType.BOILER, false)); return true; }
        if (console != null) for (int i = 0; i < 5; i++) if (region(6 + i * 11, 70, 10, 10, x, y)) { if (event.button() == 0) { int color = i; select(console, column -> column.type == RBMKColumnType.CONTROL && ((RBMKColumn.ControlColumn) column).color == color); } else if (event.button() == 1) { CompoundTag data = action("color"); data.putInt("color", i); data.putIntArray("cols", selected(console, RBMKColumnType.CONTROL, false)); send(data); } return true; }
        if (region(30, 138, 28, 28, x, y)) { if (az5Lid) az5Lid = false; else if (console != null && lastPress + 3000 < System.currentTimeMillis()) { lastPress = System.currentTimeMillis(); send("level", selected(console, RBMKColumnType.CONTROL, true), 0); } return true; }
        if (region(48, 82, 12, 12, x, y)) { try { int percent = (int) Mth.clamp(Double.parseDouble(field.getValue()), 0, 100); field.setValue(String.valueOf(percent)); send("level", selectedIndices(), percent * .01D); } catch (NumberFormatException ignored) { } return true; }
        if (console != null) for (int j = 0; j < 3; j++) for (int k = 0; k < 2; k++) { int slot = j * 2 + k; if (region(6 + 40 * k, 8 + 21 * j, 18, 18, x, y)) { CompoundTag data = action("toggle"); data.putInt("slot", slot); send(data); return true; } if (region(24 + 40 * k, 8 + 21 * j, 18, 18, x, y)) { CompoundTag data = action("assign"); data.putInt("slot", slot); data.putIntArray("cols", selectedIndices()); send(data); return true; } }
        return false;
    }

    private void select(BlockEntityRBMKConsole console, java.util.function.Predicate<RBMKColumn> predicate) { for (int i = 0; i < selection.length; i++) selection[i] = console.columns[i] != null && predicate.test(console.columns[i]); }
    private int[] selectedIndices() { return java.util.stream.IntStream.range(0, selection.length).filter(i -> selection[i]).toArray(); }
    private int[] selected(BlockEntityRBMKConsole console, RBMKColumnType type, boolean all) { return java.util.stream.IntStream.range(0, selection.length).filter(i -> console.columns[i] != null && console.columns[i].type == type && (all || selection[i])).toArray(); }
    private static CompoundTag action(String action) { CompoundTag data = new CompoundTag(); data.putString("action", action); return data; }
    private void send(String action, int[] columns) { CompoundTag data = action(action); data.putIntArray("cols", columns); send(data); }
    private void send(String action, int[] columns, double level) { CompoundTag data = action(action); data.putIntArray("cols", columns); data.putDouble("level", level); send(data); }
    private void send(CompoundTag data) { BlockEntityRBMKConsole console = menu.blockEntity(); if (console != null) GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(console.getBlockPos(), data)); }
}
