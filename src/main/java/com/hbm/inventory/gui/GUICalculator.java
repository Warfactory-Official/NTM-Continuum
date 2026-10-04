package com.hbm.inventory.gui;

import com.hbm.modules.Calculator;
import dev.golem.core.gui.CharInput;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

/**
 * 1.7.10's {@code GUICalculator} (keybind N): a 220-wide dark panel with one input line, a live
 * {@code =result} preview and the last six evaluated expressions. Enter evaluates, pushes the expression to
 * the history, copies the plain-decimal result to the clipboard and replaces the input with it; up/down pick
 * a history entry and Enter recalls it. The history is static, so it survives closing the screen, as in 1.7.
 */
public final class GUICalculator extends CompatScreen {

    private static final int X_SIZE = 220;
    private static final int Y_SIZE = 50;
    private static final int BORDER = 2;
    private static final int MAX_HISTORY = 6;

    private record Entry(String input, double result) {}

    private static final Deque<Entry> HISTORY = new ArrayDeque<>();

    private EditBox inputField;
    private int selectedHist = -1;
    private String latestResult = "?";
    private String lastEvaluated = "";

    public GUICalculator() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        int x = (width - X_SIZE) / 2;
        int y = (height - Y_SIZE) / 2;
        inputField = new EditBox(font, x + 5, y + 8, 210, 13, Component.empty());
        inputField.setTextColor(-1);
        inputField.setCanLoseFocus(false);
        inputField.setFocused(true);
        inputField.setMaxLength(1000);
        addRenderableWidget(inputField);
        setInitialFocus(inputField);
        focusedEditBoxForInput = inputField;
    }

    /** 1.7 filter: digits, operators, parentheses, whitespace and letters (function names) only. */
    private String filteredInput() {
        return inputField.getValue().replaceAll("[^\\d+\\-*/%^!.()\\sA-Za-z]+", "");
    }

    @Override
    protected boolean charTypedCompat(CharInput event) {
        selectedHist = -1; // 1.7 keyTyped: any key other than up/down clears the history selection
        return false;
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        int key = event.key();
        if (key == 257 || key == 335) { // enter / keypad enter
            if (selectedHist != -1) {
                String recalled = new ArrayList<>(HISTORY).get(selectedHist).input();
                inputField.setValue(recalled);
                selectedHist = -1;
            } else {
                String input = filteredInput();
                try {
                    double result = Calculator.evaluateExpression(input);
                    HISTORY.addFirst(new Entry(input, result));
                    if (HISTORY.size() > MAX_HISTORY) HISTORY.removeLast();
                    String plain = new BigDecimal(result, MathContext.DECIMAL64).toPlainString();
                    Minecraft.getInstance().keyboardHandler.setClipboard(plain);
                    inputField.setValue(plain);
                    inputField.setCursorPosition(plain.length());
                    inputField.setHighlightPos(0);
                } catch (Exception ignored) {}
            }
            return true;
        }
        if (key == 265) { // up
            selectedHist = Math.max(selectedHist - 1, -1);
            return true;
        }
        if (key == 264) { // down
            selectedHist = Math.min(selectedHist + 1, HISTORY.size() - 1);
            return true;
        }
        if (key != 256) selectedHist = -1;
        return false;
    }

    private void updateResult() {
        String input = filteredInput();
        if (input.equals(lastEvaluated)) return;
        lastEvaluated = input;
        if (input.isEmpty()) {
            latestResult = "?";
            return;
        }
        try {
            latestResult = Double.toString(Calculator.evaluateExpression(input));
        } catch (Exception e) {
            latestResult = e.toString();
        }
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        updateResult();
        int x = (width - X_SIZE) / 2;
        int y = (height - Y_SIZE) / 2;
        int histHeight = (font.lineHeight + 2) * MAX_HISTORY;
        int histStart = y + 30 + font.lineHeight + 8;

        GuiDraw.fill(g, x, y, x + X_SIZE, y + Y_SIZE + histHeight, 0xFF2d2d2d);
        GuiDraw.fill(g, x + BORDER, y + BORDER, x + X_SIZE - BORDER, y + Y_SIZE - BORDER + histHeight, 0xFF3d3d3d);
        GuiDraw.fill(g, x, histStart - 5, x + X_SIZE, histStart - 3, 0xFF2d2d2d);

        GuiDraw.text(g, font, "=" + latestResult, x + 5, y + 30, -1, false);

        int i = 0;
        for (Entry prev : HISTORY) {
            int hy = y + 50 + (font.lineHeight + 1) * i;
            if (i == selectedHist) GuiDraw.fill(g, x + 4, hy - 1, x + 4 + X_SIZE - 9, hy + font.lineHeight, 0xFF111111);
            GuiDraw.text(g, font, prev.input() + " = " + prev.result(), x + 5, hy, -1, false);
            i++;
        }
    }
}
