package com.hbm.inventory.gui;

import com.hbm.blocks.generic.BlockSnowglobe.SnowglobeType;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.deco.TileEntitySnowglobe;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code GUIScreenSnowglobe}: the bobblehead panel for a snowglobe - its label, and the inscription word-wrapped
 * to 280 pixels ({@code I18nUtil.autoBreakWithParagraphs}). No container.
 */
public class GUIScreenSnowglobe extends CompatScreen {

    private final TileEntitySnowglobe snowglobe;

    public GUIScreenSnowglobe(TileEntitySnowglobe snowglobe) {
        super(Component.literal("Snowglobe"));
        this.snowglobe = snowglobe;
    }

    @Override
    protected void init() {
        super.init();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BOBBLE.get(), 1.0F));
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int sizeX = 300;
        int sizeY = 150;
        int left = (this.width - sizeX) / 2;
        int top = (this.height - sizeY) / 2;
        GuiDraw.fill(graphics, left, top, left + sizeX, top + sizeY, 0xCC003300);

        int nextLevel = top + 10;
        centered(graphics, "Nuclear Tech Commemorative Snowglobe", left, sizeX, nextLevel, 0x00ff00);
        nextLevel += 10;

        SnowglobeType type = snowglobe.type();
        centered(graphics, type.label, left, sizeX, nextLevel, 0x009900);
        nextLevel += 20;

        if (type.inscription != null) {
            centered(graphics, "On the bottom is the following inscription:", left, sizeX, nextLevel, 0x00ff00);
            nextLevel += 10;
            for (String text : autoBreakWithParagraphs(font, type.inscription, 280)) {
                centered(graphics, text, left, sizeX, nextLevel, 0x009900);
                nextLevel += 10;
            }
        }
    }

    private void centered(GuiGraphicsExtractor graphics, String text, int left, int sizeX, int y, int color) {
        GuiDraw.text(graphics, font, text, left + sizeX / 2 - font.width(text) / 2, y, 0xFF000000 | color, true);
    }

    /** 1.7 {@code I18nClient.autoBreakWithParagraphs}: '$' splits paragraphs, words wrap at {@code width}. */
    static List<String> autoBreakWithParagraphs(Font font, String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\$")) {
            String[] words = paragraph.split(" ");
            List<String> para = new ArrayList<>();
            para.add(words[0]);
            int indent = font.width(words[0]);
            for (int w = 1; w < words.length; w++) {
                indent += font.width(" " + words[w]);
                if (indent <= width) {
                    para.set(para.size() - 1, para.get(para.size() - 1) + " " + words[w]);
                } else {
                    para.add(words[w]);
                    indent = font.width(words[w]);
                }
            }
            lines.addAll(para);
        }
        return lines;
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
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
}
