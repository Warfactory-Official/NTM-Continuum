package com.hbm.qmaw;

import com.hbm.sound.ModSounds;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 1.7 {@code GuiQMAW}: the 340x224 manual screen (gui_wiki.png, plus gui_wiki_flix.png on the last line). */
public final class GuiQMAW extends CompatScreen {
    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_wiki.png");
    /** 1.7 {@code the_man}: drawn instead of the text once the page is scrolled to its last line. */
    private static final Identifier THE_MAN = Ids.of("hbm", "textures/gui/gui_wiki_flix.png");
    private static final int WIDTH = 340;
    private static final int HEIGHT = 224;
    private static final int CONTENT_WIDTH = WIDTH - 29;
    private static final int CONTENT_BOTTOM = HEIGHT - 5;

    private final String qmawId;
    private final ItemStack icon;
    private final List<List<Element>> lines = new ArrayList<>();
    private final List<String> backHistory = new ArrayList<>();
    private final List<String> forwardHistory = new ArrayList<>();
    private String pageTitle;
    private int left;
    private int top;
    private int scrollProgress;
    private boolean dragging;

    public GuiQMAW(QuickManualAndWiki page) {
        super(Component.empty());
        this.qmawId = page.name;
        this.icon = page.icon;
        this.pageTitle = page.getTitle(Minecraft.getInstance().getLanguageManager());
        parse(page, Minecraft.getInstance().font);
    }

    /** Expands {{page}} tags before the old token-by-token line parser runs. */
    private String compose(String contents) {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        int recursionBrake = 100;
        while (contents.contains("{{") && recursionBrake > 0) {
            int begin = contents.indexOf("{{");
            int end = contents.indexOf("}}");
            if (end < begin) break;
            String tag = contents.substring(begin + 2, end);
            QuickManualAndWiki page = QMAWLoader.qmaw.get(tag);
            String substitute = tag;
            if (page != null) {
                substitute = page.contents.getOrDefault(language, page.contents.getOrDefault(QMAWLoader.EN_US, tag));
            }
            contents = contents.replace("{{" + tag + "}}", substitute);
            recursionBrake--;
        }
        return contents;
    }

    private void parse(QuickManualAndWiki page, Font manualFont) {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        String toParse = page.contents.get(language);
        if (toParse == null) toParse = page.contents.get(QMAWLoader.EN_US);
        if (toParse == null) toParse = "Missing Localization!";
        toParse = compose(toParse);

        String previous = toParse;
        int maxIterations = 1000;
        int currentLineWidth = 0;
        while (!toParse.isEmpty() && maxIterations > 0) {
            if (lines.isEmpty()) lines.add(new ArrayList<>());
            List<Element> currentLine = lines.get(lines.size() - 1);
            toParse = toParse.trim();
            maxIterations--;

            if (toParse.startsWith("<br>")) {
                toParse = toParse.substring(4);
                currentLine = new ArrayList<>();
                lines.add(currentLine);
                currentLineWidth = 0;
                continue;
            }

            if (toParse.startsWith("[[")) {
                int end = toParse.indexOf("]]", 2);
                if (end != -1) {
                    String link = toParse.substring(2, end);
                    toParse = toParse.substring(end + 2);
                    int pipe = link.indexOf('|');
                    String suffix = toParse.startsWith(" ") ? " " : "";
                    String target = link;
                    String text = link;
                    if (pipe != -1) {
                        String first = link.substring(0, pipe);
                        String second = link.substring(pipe + 1);
                        // The shipped 1.12.2 pages use display|id, while the documented form is id|text.
                        // Accept both without changing the text or target of the original pages.
                        if (QMAWLoader.qmaw.containsKey(first)) {
                            target = first;
                            text = second;
                        } else if (QMAWLoader.qmaw.containsKey(second)) {
                            target = second;
                            text = first;
                        } else {
                            target = first;
                            text = second;
                        }
                    }
                    Element element = Element.link(target, text + suffix);
                    int width = element.width(manualFont);
                    if (width + currentLineWidth > CONTENT_WIDTH) {
                        currentLine = new ArrayList<>();
                        lines.add(currentLine);
                        currentLineWidth = 0;
                    }
                    currentLine.add(element);
                    currentLineWidth += width;
                    previous = toParse;
                    continue;
                }
            }

            int delimit = toParse.length();
            int spaceIndex = toParse.indexOf(' ');
            if (spaceIndex != -1) delimit = Math.min(delimit, spaceIndex);
            int linkIndex = toParse.indexOf("[[");
            if (linkIndex != -1) delimit = Math.min(delimit, linkIndex);
            int brIndex = toParse.indexOf("<br>");
            if (brIndex != -1) delimit = Math.min(delimit, brIndex);

            if (delimit > 0) {
                String text = toParse.substring(0, delimit) + (spaceIndex == delimit ? " " : "");
                Element element = Element.text(text);
                toParse = toParse.substring(delimit);
                int width = element.width(manualFont);
                if (width + currentLineWidth > CONTENT_WIDTH) {
                    currentLine = new ArrayList<>();
                    lines.add(currentLine);
                    currentLineWidth = 0;
                }
                currentLine.add(element);
                currentLineWidth += width;
                previous = toParse;
                continue;
            }

            if (toParse.equals(previous)) break;
            previous = toParse;
        }
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateDragging(mouseY);
        GuiDraw.fill(graphics, 0, 0, width, height, 0xe0000000);
        GuiDraw.blit(graphics, TEXTURE, left, top, 0, 0, 170, HEIGHT, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, left + 170, top, 52, 0, 30, HEIGHT, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, left + 200, top, 52, 0, 140, HEIGHT, 256, 256);

        if (!backHistory.isEmpty()) GuiDraw.blit(graphics, TEXTURE, left + 3, top + 3, 204, 0, 18, 18, 256, 256);
        if (!forwardHistory.isEmpty()) GuiDraw.blit(graphics, TEXTURE, left + 21, top + 3, 222, 0, 18, 18, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, left + WIDTH - 15, top + getSliderPosition(), 192, 0, 12, 16, 256, 256);
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (showTheMan()) {
            GuiDraw.blit(graphics, THE_MAN, left + 60, top + HEIGHT - 84, 0, 0, 80, 80, 256, 256);
            GuiDraw.blit(graphics, THE_MAN, left + 140, top + HEIGHT - 60, 0, 80, 77, 39, 256, 256);
        } else {
            visitVisible((element, x, y) -> element.draw(graphics, font, x, y, over(x, y, element.width(font), element.height(font), mouseX, mouseY), qmawId));
        }

        int x = 43;
        int y = 4;
        if (icon != null && !icon.isEmpty()) {
            GuiDraw.item(graphics, icon, left + x, top + y);
            x += 18;
            y += (16 - font.lineHeight) / 2;
        }
        y += 1;
        GuiDraw.text(graphics, font, pageTitle, left + x, top + y, 0xFFFFFF, false);
    }

    public String pageId() {
        return qmawId;
    }

    /** Every parsed element as {@code text|link|ARGB} in its unhovered colour, line by line (harness). */
    public List<List<String>> describeLines() {
        List<List<String>> out = new ArrayList<>();
        for (List<Element> line : lines) {
            List<String> row = new ArrayList<>();
            for (Element element : line) {
                row.add(element.text + "|" + (element.link == null ? "" : element.link) + "|"
                        + Integer.toHexString(element.drawColor(false, qmawId)));
            }
            out.add(row);
        }
        return out;
    }

    private void updateDragging(int mouseY) {
        if (!dragging) return;
        if (!dev.golem.core.client.MouseInput.isDown(0)) {
            dragging = false;
            return;
        }
        int min = top + 25 + 8;
        int max = top + 25 + 191 - 8;
        double progress = Mth.clamp((double) (mouseY - min) / (max - min), 0D, 1D);
        scrollProgress = Mth.clamp((int) Math.round((lines.size() - 1) * progress), 0, Math.max(0, lines.size() - 1));
    }

    /** 1.7: {@code lines.size() > 1 && scrollProgress == lines.size() - 1}. */
    private boolean showTheMan() {
        return lines.size() > 1 && scrollProgress == lines.size() - 1;
    }

    private int getSliderPosition() {
        if (lines.size() <= 1) return 25;
        return 25 + (int) ((double) scrollProgress / (lines.size() - 1) * 180D);
    }

    private void scroll(double amount) {
        if (amount > 0) scrollProgress--;
        if (amount < 0) scrollProgress++;
        scrollProgress = Mth.clamp(scrollProgress, 0, Math.max(0, lines.size() - 1));
    }

    private boolean over(int x, int y, int elementWidth, int elementHeight, double mouseX, double mouseY) {
        return x <= mouseX && x + elementWidth > mouseX && y < mouseY && y + elementHeight >= mouseY;
    }

    private interface Visitor {
        void visit(Element element, int x, int y);
    }

    private void visitVisible(Visitor visitor) {
        int x = left + 7;
        int y = top + 30;
        int lineNumber = 0;
        for (List<Element> line : lines) {
            lineNumber++;
            if (lineNumber <= scrollProgress) continue;

            int maxHeight = 0;
            for (Element element : line) maxHeight = Math.max(maxHeight, element.height(font));
            if (y + maxHeight > top + CONTENT_BOTTOM) break;
            if (line.isEmpty()) y += font.lineHeight;

            int inset = 0;
            for (Element element : line) {
                int elementX = x + inset;
                int elementY = y + (maxHeight - element.height(font)) / 2;
                visitor.visit(element, elementX, elementY);
                inset += element.width(font);
            }
            y += maxHeight + 2;
        }
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        if (event.button() != 0) return false;
        if (over(left + 3, top + 3, 18, 18, event.x(), event.y())) {
            back();
            return true;
        }
        if (over(left + 21, top + 3, 18, 18, event.x(), event.y())) {
            forward();
            return true;
        }
        if (event.x() >= left + WIDTH - 15 && event.x() < left + WIDTH - 3
                && event.y() > top + 25 && event.y() <= top + 216 && lines.size() > 1) {
            dragging = true;
            updateDragging((int) event.y());
            return true;
        }
        if (showTheMan()) {
            if (over(left + 60, top + HEIGHT - 84, 80, 80, event.x(), event.y())) {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(ModSounds.ALARM_SINGER.get(), 1.0F));
                return true;
            }
            return false;
        }

        final boolean[] clicked = {false};
        visitVisible((element, x, y) -> {
            if (!clicked[0] && element.link != null && over(x, y, element.width(font), element.height(font), event.x(), event.y())) {
                clicked[0] = true;
                openLink(element.link);
            }
        });
        return clicked[0];
    }

    @Override
    protected boolean mouseReleasedCompat(MouseInput event) {
        if (event.button() == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll(scrollY);
        return scrollY != 0;
    }

    private void openLink(String target) {
        if (qmawId.equals(target)) return;
        QuickManualAndWiki page = QMAWLoader.qmaw.get(target);
        if (page == null) return;
        clickSound();
        GuiQMAW screen = new GuiQMAW(page);
        screen.backHistory.addAll(backHistory);
        screen.backHistory.add(qmawId);
        Minecraft.getInstance().setScreen(screen);
    }

    private void back() {
        if (backHistory.isEmpty()) return;
        String previous = backHistory.get(backHistory.size() - 1);
        QuickManualAndWiki page = QMAWLoader.qmaw.get(previous);
        if (page == null) return;
        clickSound();
        GuiQMAW screen = new GuiQMAW(page);
        screen.backHistory.addAll(backHistory.subList(0, backHistory.size() - 1));
        screen.forwardHistory.addAll(forwardHistory);
        screen.forwardHistory.add(qmawId);
        Minecraft.getInstance().setScreen(screen);
    }

    private void forward() {
        if (forwardHistory.isEmpty()) return;
        String next = forwardHistory.get(forwardHistory.size() - 1);
        QuickManualAndWiki page = QMAWLoader.qmaw.get(next);
        if (page == null) return;
        clickSound();
        GuiQMAW screen = new GuiQMAW(page);
        screen.backHistory.addAll(backHistory);
        screen.backHistory.add(qmawId);
        screen.forwardHistory.addAll(forwardHistory.subList(0, forwardHistory.size() - 1));
        Minecraft.getInstance().setScreen(screen);
    }

    private static void clickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        if (event.key() == 263) {
            back();
            return !backHistory.isEmpty();
        }
        if (event.key() == 262) {
            forward();
            return !forwardHistory.isEmpty();
        }
        Minecraft mc = Minecraft.getInstance();
        if (event.key() == 256 || event.matches(mc.options.keyInventory)) {
            mc.setScreen(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Element {
        private final String text;
        private final String link;
        private final ItemStack icon;
        private final int color;
        private final int hoverColor;
        /** 1.7 {@code occupiedColor}: a link to the page already open. */
        private static final int OCCUPIED_COLOR = 0xFFA0A0A0;

        private Element(String text, String link, ItemStack icon, int color, int hoverColor) {
            this.text = text;
            this.link = link;
            this.icon = icon;
            this.color = color;
            this.hoverColor = hoverColor;
        }

        private static Element text(String text) {
            return new Element(text, null, null, 0xFFFFFFFF, 0xFFFFFFFF);
        }

        private static Element link(String link, String text) {
            QuickManualAndWiki page = QMAWLoader.qmaw.get(link);
            boolean missing = page == null;
            return new Element(text, link, missing ? null : page.icon,
                    missing ? 0xFFFF7F7F : 0xFF0094FF, missing ? 0xFFFF7F7F : 0xFFFFD800);
        }

        private int drawColor(boolean mouseOver, String currentPage) {
            if (currentPage.equals(link)) return OCCUPIED_COLOR;
            return mouseOver ? hoverColor : color;
        }

        private int width(Font font) {
            return font.width(text) + (icon != null && !icon.isEmpty() ? 18 : 0);
        }

        private int height(Font font) {
            return Math.max(font.lineHeight, icon != null && !icon.isEmpty() ? 16 : 0);
        }

        private void draw(GuiGraphicsExtractor graphics, Font font,
                          int x, int y, boolean mouseOver, String currentPage) {
            if (icon != null && !icon.isEmpty()) {
                GuiDraw.item(graphics, icon, x, y - 1);
                x += 18;
                y += (16 - font.lineHeight) / 2;
            }
            GuiDraw.text(graphics, font, text, x, y, drawColor(mouseOver, currentPage), false);
        }
    }
}
