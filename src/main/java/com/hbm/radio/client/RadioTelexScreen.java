package com.hbm.radio.client;

import com.hbm.packet.ControlEventPayload;
import com.hbm.radio.RadioTelexMenu;
import com.hbm.tileentity.network.TileEntityRadioTelex;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/** The original 256x244 TELEX panel, including its character waveform and five-line editor. */
public final class RadioTelexScreen extends CompatContainerScreen<RadioTelexMenu> {
    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_telex.png");
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 244;

    private final EditBox[] txLines = new EditBox[TileEntityRadioTelex.LINE_COUNT];
    private EditBox txFrequency;
    private EditBox rxFrequency;
    private int cursorPos;
    private boolean textFocus;

    public RadioTelexScreen(RadioTelexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        TileEntityRadioTelex telex = menu.telex();
        if (telex != null) {
            for (int i = TileEntityRadioTelex.LINE_COUNT - 1; i > 0; i--) {
                if (telex.txBuffer[i] != null && !telex.txBuffer[i].isEmpty()) {
                    cursorPos = i;
                    break;
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        TileEntityRadioTelex telex = menu.telex();

        txFrequency = field(29, 110, 90, telex == null ? "" : telex.txChannel, TileEntityRadioTelex.CHANNEL_LENGTH,
                0xFF00FF00);
        rxFrequency = field(29, 224, 90, telex == null ? "" : telex.rxChannel, TileEntityRadioTelex.CHANNEL_LENGTH,
                0xFF00FF00);

        // These widgets receive charTyped/backspace events. Their text is transparent because the old
        // renderer lays out each character on its fixed seven-pixel terminal grid below.
        for (int i = 0; i < txLines.length; i++) {
            txLines[i] = field(7, 7 + 14 * i, 242,
                    telex == null ? "" : telex.txBuffer[i], TileEntityRadioTelex.LINE_WIDTH, 0x00000000);
        }
        setLineFocus();
    }

    private EditBox field(int x, int y, int width, String value, int max, int color) {
        EditBox box = new EditBox(font, leftPos + x, topPos + y, width, 14, Component.empty());
        box.setBordered(false);
        box.setTextColor(color);
        box.setTextColorUneditable(color);
        box.setMaxLength(max);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        for (int line = 0; line < txLines.length; line++) {
            drawTxLine(graphics, txLines[line].getValue(), line);
        }

        TileEntityRadioTelex telex = menu.telex();
        if (telex == null) return;
        for (int line = 0; line < TileEntityRadioTelex.LINE_COUNT; line++) {
            drawRxLine(graphics, telex.rxBuffer[line], line);
        }
        drawWaveform(graphics, telex);
    }

    private void drawTxLine(GuiGraphicsExtractor graphics, String text, int line) {
        String format = "\u00a7r";
        int y = 11 + 14 * line;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            int x = 11 + 7 * index + (7 - font.width(String.valueOf(character))) / 2;
            if (character == '\u00a7' && text.length() > index + 1) {
                format = "\u00a7" + text.charAt(index + 1);
                x -= 3;
            }
            String glyph = format + character;
            if (character == TileEntityRadioTelex.BELL) glyph = "\u00a7cB";
            if (character == TileEntityRadioTelex.PRINT) glyph = "\u00a7cP";
            if (character == TileEntityRadioTelex.CLEAR) glyph = "\u00a7c<";
            if (character == TileEntityRadioTelex.PAUSE) glyph = "\u00a7cW";
            GuiDraw.text(graphics, font, glyph, leftPos + x, topPos + y, 0xFF00FF00, false);
        }

        if (System.currentTimeMillis() % 1000 < 500 && textFocus && cursorPos == line) {
            int x = Math.max(11 + 7 * (text.length() - 1) + 7, 11);
            GuiDraw.text(graphics, font, "|", leftPos + x, topPos + y, 0xFF00FF00, false);
        }
    }

    private void drawRxLine(GuiGraphicsExtractor graphics, String text, int line) {
        String format = "\u00a7r";
        int x = 11;
        int y = 145 + 14 * line;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            x += (7 - font.width(String.valueOf(character))) / 2;
            if (character == '\u00a7' && text.length() > index + 1) {
                format = "\u00a7" + text.charAt(index + 1);
                character = ' ';
            } else if (character == '\u00a7') {
                character = ' ';
            } else if (index > 0 && text.charAt(index - 1) == '\u00a7') {
                character = ' ';
                x -= 14;
            }
            GuiDraw.text(graphics, font, format + character, leftPos + x, topPos + y, 0xFF00FF00, false);
            x += 7;
        }
    }

    /** Re-expresses the old GL_LINES waveform with 3px GUI quads while retaining all 48 samples and RNG. */
    private void drawWaveform(GuiGraphicsExtractor graphics, TileEntityRadioTelex telex) {
        Random random = new Random(telex.sendingChar);
        double offset = 0D;
        for (int i = 0; i < 48; i++) {
            int x0 = leftPos + 199 + i;
            int y0 = topPos + (int) Math.round(93.5D + offset);
            if (telex.sendingChar != ' ' && i > 4 && i < 43) offset = random.nextGaussian() * 7D;
            else offset = 0D;
            offset = Math.max(-7D, Math.min(7D, offset));
            int x1 = x0 + 1;
            int y1 = topPos + (int) Math.round(93.5D + offset);
            drawWaveSegment(graphics, x0, y0, x1, y1);
        }
    }

    private void drawWaveSegment(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            GuiDraw.fill(graphics, x0 - 1, y0 - 1, x0 + 2, y0 + 2, 0xFF00FF00);
            if (x0 == x1 && y0 == y1) return;
            int twice = error * 2;
            if (twice >= dy) {
                error += dy;
                x0 += sx;
            }
            if (twice <= dx) {
                error += dx;
                y0 += sy;
            }
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (over(mouseX, mouseY, 7, 85, 18, 18)) tooltip(graphics, "BELL",
                "Plays a bell when this character is received", ChatFormatting.GOLD, mouseX, mouseY);
        if (over(mouseX, mouseY, 27, 85, 18, 18)) tooltip(graphics, "PRINT",
                "Forces recipient to print message after transmission ends", ChatFormatting.GOLD, mouseX, mouseY);
        if (over(mouseX, mouseY, 47, 85, 18, 18)) tooltip(graphics, "CLEAR SCREEN",
                "Wipes message buffer when this character is received", ChatFormatting.GOLD, mouseX, mouseY);
        if (over(mouseX, mouseY, 67, 85, 18, 18)) tooltip(graphics, "FORMAT",
                "Inserts format character for message formatting", ChatFormatting.GOLD, mouseX, mouseY);
        if (over(mouseX, mouseY, 87, 85, 18, 18)) tooltip(graphics, "PAUSE",
                "Pauses message transmission for one second", ChatFormatting.GOLD, mouseX, mouseY);

        if (over(mouseX, mouseY, 127, 105, 18, 18) || over(mouseX, mouseY, 127, 219, 18, 18))
            tooltip(graphics, "SAVE ID", null, ChatFormatting.GREEN, mouseX, mouseY);
        if (over(mouseX, mouseY, 147, 105, 18, 18))
            tooltip(graphics, "SEND MESSAGE", null, ChatFormatting.YELLOW, mouseX, mouseY);
        if (over(mouseX, mouseY, 167, 105, 18, 18))
            tooltip(graphics, "DELETE MESSAGE BUFFER", null, ChatFormatting.RED, mouseX, mouseY);
        if (over(mouseX, mouseY, 147, 219, 18, 18))
            tooltip(graphics, "PRINT MESSAGE", null, ChatFormatting.AQUA, mouseX, mouseY);
        if (over(mouseX, mouseY, 167, 219, 18, 18))
            tooltip(graphics, "CLEAR SCREEN", null, ChatFormatting.RED, mouseX, mouseY);
    }

    private void tooltip(GuiGraphicsExtractor graphics, String title, @Nullable String description,
                         ChatFormatting color, int mouseX, int mouseY) {
        List<Component> lines = description == null
                ? List.of(Component.literal(title).withStyle(color))
                : List.of(Component.literal(title).withStyle(color), Component.literal(description));
        GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        int x = (int) event.x();
        int y = (int) event.y();

        // The old terminal clicked anywhere in this rectangle and kept the current line selection.
        if (over(x, y, 7, 7, 242, 74)) {
            textFocus = true;
            txFrequency.setFocused(false);
            rxFrequency.setFocused(false);
            setLineFocus();
            return true;
        }

        char character = 0;
        if (over(x, y, 7, 85, 18, 18)) character = TileEntityRadioTelex.BELL;
        if (over(x, y, 27, 85, 18, 18)) character = TileEntityRadioTelex.PRINT;
        if (over(x, y, 47, 85, 18, 18)) character = TileEntityRadioTelex.CLEAR;
        if (over(x, y, 67, 85, 18, 18)) character = '\u00a7';
        if (over(x, y, 87, 85, 18, 18)) character = TileEntityRadioTelex.PAUSE;

        String command = null;
        if (over(x, y, 127, 105, 18, 18) || over(x, y, 127, 219, 18, 18)) command = "sve";
        if (over(x, y, 147, 105, 18, 18)) command = "snd";
        if (over(x, y, 167, 105, 18, 18)) command = "rxdel";
        if (over(x, y, 147, 219, 18, 18)) command = "rxprt";
        if (over(x, y, 167, 219, 18, 18)) command = "rxcls";

        if (command != null) {
            click();
            if ("rxdel".equals(command)) {
                // The old GUI cleared its local transmit buffer before sending the delete packet.
                for (EditBox line : txLines) line.setValue("");
            }
            CompoundTag data = new CompoundTag();
            data.putString("cmd", command);
            if ("snd".equals(command) || "rxdel".equals(command)) {
                for (int i = 0; i < txLines.length; i++) data.putString("tx" + i, txLines[i].getValue());
            }
            if ("sve".equals(command)) {
                data.putString("txChan", txFrequency.getValue());
                data.putString("rxChan", rxFrequency.getValue());
            }
            send(data);
            return true;
        }

        if (character != 0) {
            click();
            textFocus = true;
            txFrequency.setFocused(false);
            rxFrequency.setFocused(false);
            setLineFocus();
            appendCharacter(character);
            return true;
        }
        return false;
    }

    @Override
    protected boolean keyPressed(KeyInput event) {
        if (txFrequency != null && txFrequency.isFocused() && event.key() != 256 && event.key() != 258) {
            event.keyPressed(txFrequency);
            return true;
        }
        if (rxFrequency != null && rxFrequency.isFocused() && event.key() != 256 && event.key() != 258) {
            event.keyPressed(rxFrequency);
            return true;
        }

        if (textFocus) {
            if (event.key() == 256) {
                // Legacy behavior: Escape leaves terminal-line editing, but does not close the GUI.
                textFocus = false;
                setLineFocus();
                return true;
            }
            if (event.key() == 258) return false;
            if (event.key() == 265) {
                cursorPos = Math.max(0, cursorPos - 1);
                setLineFocus();
                return true;
            }
            if (event.key() == 264) {
                cursorPos = Math.min(TileEntityRadioTelex.LINE_COUNT - 1, cursorPos + 1);
                setLineFocus();
                return true;
            }

            if (event.key() == 259) {
                EditBox active = txLines[cursorPos];
                String value = active.getValue();
                if (!value.isEmpty()) active.setValue(value.substring(0, value.length() - 1));
                return true;
            }
            // The old terminal only accepted append/backspace/up/down; do not let EditBox add
            // left/right/delete/home cursor editing that the legacy keyTyped path never had.
            return true;
        }
        return false;
    }

    private void appendCharacter(char character) {
        EditBox line = txLines[cursorPos];
        String value = line.getValue();
        if (value.length() < TileEntityRadioTelex.LINE_WIDTH) line.setValue(value + character);
    }

    private void setLineFocus() {
        if (txLines[0] == null) return;
        for (int i = 0; i < txLines.length; i++) txLines[i].setFocused(textFocus && i == cursorPos);
        setFocused(textFocus ? txLines[cursorPos] : null);
    }

    private void send(CompoundTag data) {
        TileEntityRadioTelex telex = menu.telex();
        if (telex != null) {
            GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                    new ControlEventPayload(telex.getBlockPos(), data));
        }
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    private boolean over(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY > topPos + y && mouseY <= topPos + y + height;
    }

    @Override
    public void onClose() {
        CompoundTag data = new CompoundTag();
        if (txLines != null && txLines[0] != null) {
            for (int i = 0; i < txLines.length; i++) data.putString("tx" + i, txLines[i].getValue());
            send(data);
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
