package com.hbm.inventory.gui;

import com.hbm.module.Calculator;
import com.hbm.qmaw.GuiQMAW;
import com.hbm.qmaw.QMAWLoader;
import com.hbm.qmaw.QuickManualAndWiki;
import dev.golem.core.gui.CharInput;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.gui.TextSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 1.7 {@code GUIScreenCalculator} (keybind N): a calculator and a QMAW page search, toggled with Tab. The calculator
 * evaluates on Enter and keeps the last six expressions; the search lists matching page titles, those starting with
 * the query first, and opens the selected one on Enter or left click. Arrow keys and the wheel move the selection;
 * Delete on an empty input or a middle click removes a history entry. Histories and mode are static, as in 1.7.
 */
public final class GUIScreenCalculator extends CompatScreen {

    private static final int X_SIZE = 220;
    private static final int Y_SIZE = 50;
    private static final int BORDER = 2;
    private static final int MAX_HISTORY = 6;
    private static final int QMAW_ENTRY_Y_SPACING = 7;
    private static final int BORDER_COLOR = 0xFF2d2d2d, FILL_COLOR = 0xFF3d3d3d, SELECT_COLOR = 0xFF111111;
    private static final int MOUSE_LEFT = 0, MOUSE_MIDDLE = 2;
    /** A typed character rather than a key. */
    private static final int CHAR = -1;

    private record Entry(String input, double result) {}

    private static final List<Entry> CALC_HISTORY = new ArrayList<>(MAX_HISTORY);
    private static final List<String> QMAW_HISTORY = new ArrayList<>(MAX_HISTORY);
    private static boolean qmawMode = false;

    private final List<String> qmawResults = new ArrayList<>();
    private EditBox inputField;
    private int selectedHist = -1;
    private String latestResult = "?";
    private String lastInput = "";

    public GUIScreenCalculator() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        inputField = new EditBox(font, startX() + 5, startY() + 14, 210, 13, Component.empty());
        inputField.setTextColor(-1);
        inputField.setCanLoseFocus(false);
        inputField.setFocused(true);
        inputField.setMaxLength(1000);
        addRenderableWidget(inputField);
        setInitialFocus(inputField);
        focusedEditBoxForInput = inputField;
    }

    private int startX() {
        return (width - X_SIZE) / 2;
    }

    private int startY() {
        return (height - (Y_SIZE + (font.lineHeight + 4) * MAX_HISTORY)) / 2;
    }

    /** 1.7 draws no default background behind this screen. */
    @Override
    protected boolean menuBackground() {
        return false;
    }

    private boolean isInputEmpty() {
        return inputField.getValue().isEmpty();
    }

    private static boolean isEnter(int key) {
        return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) return false;
        if (!isEnter(key) && key != GLFW.GLFW_KEY_TAB && key != GLFW.GLFW_KEY_UP && key != GLFW.GLFW_KEY_DOWN) {
            event.keyPressed(inputField);
        }
        onKey(key);
        return true;
    }

    @Override
    protected boolean charTypedCompat(CharInput event) {
        event.charTyped(inputField);
        onKey(CHAR);
        return true;
    }

    /** 1.7 {@code keyTyped}, after the text box has taken the key. */
    private void onKey(int key) {
        if (key == GLFW.GLFW_KEY_TAB) qmawMode = !qmawMode;

        if (qmawMode) trySearchForQMAW(key);
        else handleCalculator(key);
    }

    private void handleCalculator(int key) {
        String input = inputField.getValue().replaceAll("[^\\d+\\-*/%^!.()\\sA-Za-z]+", "");

        selectedHistSanityCheck();

        if (isEnter(key)) {
            if (selectedHist != -1) {
                input = CALC_HISTORY.get(selectedHist).input();
                inputField.setValue(input);
                selectedHist = -1;
            } else {
                try {
                    double result = Calculator.evaluateExpression(input);
                    CALC_HISTORY.add(0, new Entry(input, result));
                    if (CALC_HISTORY.size() > MAX_HISTORY) CALC_HISTORY.remove(CALC_HISTORY.size() - 1);
                    String plain = new BigDecimal(result, MathContext.DECIMAL64).toPlainString();
                    Minecraft.getInstance().keyboardHandler.setClipboard(plain);
                    inputField.setValue(plain);
                    inputField.setCursorPosition(plain.length());
                    inputField.setHighlightPos(0);
                } catch (RuntimeException ignored) {
                    // the evaluator throws on a malformed expression, which then stays in the input as typed
                }
                return;
            }
        }

        if (key == GLFW.GLFW_KEY_DELETE && input.isEmpty()) deleteCalcHistoryEntry();

        moveSelection(key);

        if (input.isEmpty()) {
            latestResult = "?";
            return;
        }

        try {
            latestResult = Double.toString(Calculator.evaluateExpression(input));
        } catch (RuntimeException e) {
            latestResult = e.toString();
        }
    }

    private void deleteCalcHistoryEntry() {
        selectedHistSanityCheck();
        if (selectedHist >= 0) CALC_HISTORY.remove(selectedHist);
    }

    private static void sortQMAWResults(String search, List<String> results) {
        results.sort((s1, s2) -> {
            boolean starts1 = s1.toLowerCase(Locale.ROOT).startsWith(search), starts2 = s2.toLowerCase(Locale.ROOT).startsWith(search);
            return starts1 && !starts2 ? -1 : !starts1 && starts2 ? 1 : s1.compareTo(s2);
        });
    }

    private void trySearchForQMAW(int key) {
        boolean history = isInputEmpty();
        String query = inputField.getValue().toLowerCase(Locale.ROOT);
        if (!inputField.getValue().equalsIgnoreCase(lastInput)) {
            qmawResults.clear();
            lastInput = inputField.getValue();

            LanguageManager lang = Minecraft.getInstance().getLanguageManager();
            for (QuickManualAndWiki page : QMAWLoader.sortedPages) {
                if (TextSearch.contains(page.getTitle(lang).toLowerCase(Locale.ROOT), query)) qmawResults.add(page.getTitle(lang));
            }
        }

        sortQMAWResults(query, qmawResults);

        if (isEnter(key)) executeQMAW(history);
        if (key == GLFW.GLFW_KEY_DELETE && history) deleteQMAWHistoryEntry();

        moveSelection(key);
    }

    private void executeQMAW(boolean historyMode) {
        selectedHistSanityCheck();
        if (selectedHist < 0) return;

        String key = (historyMode ? QMAW_HISTORY : qmawResults).get(selectedHist);
        QMAW_HISTORY.remove(key);
        QMAW_HISTORY.add(0, key);
        if (QMAW_HISTORY.size() > MAX_HISTORY) QMAW_HISTORY.remove(QMAW_HISTORY.size() - 1);
        inputField.setValue(key);
        selectedHist = -1;

        QuickManualAndWiki page = pageByTitle(key);
        if (page != null) {
            GuiUtil.playClickSound();
            Minecraft.getInstance().setScreen(new GuiQMAW(page));
        }
    }

    // port: 1.7 looks a title up as a page name, which misses pages whose title is not their name
    private static QuickManualAndWiki pageByTitle(String title) {
        LanguageManager lang = Minecraft.getInstance().getLanguageManager();
        for (QuickManualAndWiki page : QMAWLoader.sortedPages) {
            if (page.getTitle(lang).equals(title)) return page;
        }
        return QMAWLoader.qmaw.get(title);
    }

    /** A middle click on a search result removes the history entry in the same row, as in 1.7. */
    private void deleteQMAWHistoryEntry() {
        selectedHistSanityCheck();
        if (selectedHist >= 0 && selectedHist < QMAW_HISTORY.size()) QMAW_HISTORY.remove(selectedHist);
    }

    /** The full list the current mode shows, before the six-row cut. */
    private int listSize() {
        if (!qmawMode) return CALC_HISTORY.size();
        return isInputEmpty() ? QMAW_HISTORY.size() : qmawResults.size();
    }

    /** Up and down wrap through the shown rows; any other key clears the selection. */
    private void moveSelection(int key) {
        if (key == GLFW.GLFW_KEY_UP) selectUp();
        else if (key == GLFW.GLFW_KEY_DOWN) selectDown();
        else selectedHist = -1;
    }

    private void selectUp() {
        selectedHist = selectedHist < 0 ? Math.min(listSize(), MAX_HISTORY) - 1 : selectedHist - 1;
    }

    private void selectDown() {
        selectedHist = selectedHist >= Math.min(listSize(), MAX_HISTORY) - 1 ? -1 : selectedHist + 1;
    }

    private void selectedHistSanityCheck() {
        // port: clamped to the last row; 1.7 allows one past it, which a delete or Enter then indexes out of range
        selectedHist = Mth.clamp(selectedHist, -1, listSize() - 1);
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = startX();
        int y = startY();
        int histHeight = (font.lineHeight + 4) * MAX_HISTORY;
        int histStart = y + 30 + font.lineHeight + 8;

        GuiDraw.fill(g, x, y, x + X_SIZE, y + Y_SIZE + histHeight, BORDER_COLOR);
        GuiDraw.fill(g, x + BORDER, y + BORDER, x + X_SIZE - BORDER, y + Y_SIZE - BORDER + histHeight, FILL_COLOR);
        if (!qmawMode) GuiDraw.fill(g, x, histStart - 5, x + X_SIZE, histStart - 3, BORDER_COLOR);

        Component header = Component.translatable(qmawMode ? "gui.calc.qmawResult" : "gui.calc.calculatorResult");
        GuiDraw.text(g, font, header, x + 4, y + 4, -1, false);

        if (qmawMode) drawQMAW(g, mouseX, mouseY, x, y);
        else drawCalc(g, x, y);
    }

    private void drawCalc(GuiGraphicsExtractor g, int x, int y) {
        GuiDraw.text(g, font, "=" + latestResult, x + 5, y + 32, -1, false);

        int i = 0;
        for (Entry prev : CALC_HISTORY) {
            int hy = y + 50 + (font.lineHeight + 1) * i;
            if (i == selectedHist) GuiDraw.fill(g, x + 4, hy - 1, x + 4 + X_SIZE - 9, hy + font.lineHeight, SELECT_COLOR);
            GuiDraw.text(g, font, prev.input() + " = " + prev.result(), x + 5, hy, -1, false);
            i++;
        }
    }

    private void drawQMAW(GuiGraphicsExtractor g, int mouseX, int mouseY, int x, int y) {
        int hover = mouseEntrySelected(mouseX, mouseY);
        int i = 0;
        for (String r : isInputEmpty() ? QMAW_HISTORY : qmawResults) {
            if (i > MAX_HISTORY - 1) break;

            int hy = y + 35 + (font.lineHeight + QMAW_ENTRY_Y_SPACING) * i;
            if (i == selectedHist) {
                GuiDraw.fill(g, x + 4, hy - QMAW_ENTRY_Y_SPACING / 2, x + 4 + X_SIZE - 9,
                        hy + font.lineHeight + QMAW_ENTRY_Y_SPACING / 2, SELECT_COLOR);
            }
            GuiDraw.text(g, font, r, x + 25, hy, hover == i ? 0xffff00 : -1, false);

            QuickManualAndWiki page = pageByTitle(r);
            if (page != null && page.icon != null && !page.icon.isEmpty()) {
                GuiDraw.item(g, page.icon, x + 5, hy - QMAW_ENTRY_Y_SPACING + 2);
                GuiDraw.itemDecorations(g, font, page.icon, x + 5, hy - QMAW_ENTRY_Y_SPACING + 2);
            }
            i++;
        }
    }

    /** Whether the mouse is over the panel; clicks outside it are ignored. */
    private boolean isMouseInSegment(int mouseX, int mouseY) {
        int x = startX(), y = startY();
        return mouseX > x && mouseX < x + X_SIZE && mouseY > y && mouseY < y + Y_SIZE + (font.lineHeight + 4) * MAX_HISTORY;
    }

    /** The row under the mouse, or -1 when there is no entry there. */
    private int mouseEntrySelected(int mouseX, int mouseY) {
        int entry = -1;
        if (isMouseInSegment(mouseX, mouseY)) {
            int x = startX(), y = startY();
            for (int i = 0; i < MAX_HISTORY; i++) {
                if (qmawMode) {
                    int hy = y + 35 + (font.lineHeight + QMAW_ENTRY_Y_SPACING) * i;
                    if (mouseX > x + 4 && mouseY > hy - QMAW_ENTRY_Y_SPACING / 2 && mouseX < x + 4 + X_SIZE - 9
                            && mouseY < hy + font.lineHeight + QMAW_ENTRY_Y_SPACING / 2) {
                        entry = i;
                        break;
                    }
                } else {
                    int hy = y + 50 + (font.lineHeight + 1) * i;
                    if (mouseX > x + 4 && mouseY > hy - 1 && mouseX < x + 4 + X_SIZE - 9 && mouseY < hy + font.lineHeight) {
                        entry = i;
                        break;
                    }
                }
            }
        }
        if (entry >= listSize()) entry = -1;
        return entry;
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        int mouseX = (int) event.x(), mouseY = (int) event.y();
        if (!isMouseInSegment(mouseX, mouseY)) return false;

        selectedHist = mouseEntrySelected(mouseX, mouseY);
        if (selectedHist < 0) return false;

        if (qmawMode) {
            if (event.button() == MOUSE_LEFT) executeQMAW(isInputEmpty());
            else if (event.button() == MOUSE_MIDDLE) deleteQMAWHistoryEntry();
        } else {
            if (event.button() == MOUSE_LEFT) handleCalculator(GLFW.GLFW_KEY_ENTER);
            else if (event.button() == MOUSE_MIDDLE) deleteCalcHistoryEntry();
        }
        return true;
    }

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) selectUp();
        else if (scrollY < 0) selectDown();
        return scrollY != 0;
    }
}
