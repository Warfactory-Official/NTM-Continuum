package com.hbm.inventory.gui;

import com.hbm.items.special.ItemHolotapeImage;
import com.hbm.items.special.ItemHolotapeImage.EnumHoloImage;
import com.hbm.sound.ModSounds;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code GUIScreenHolotape}: a bobble sound, then a translucent dark green 300x150 panel with the tape's
 * text word-wrapped at 275 pixels, centred, in green with a shadow, starting 30 pixels down.
 */
public class GUIScreenHolotape extends CompatScreen {

    EnumHoloImage holo;

    public GUIScreenHolotape() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BOBBLE.get(), 1.0F));

        ItemStack stack = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        if (stack.getItem() instanceof ItemHolotapeImage tape) {
            this.holo = (EnumHoloImage) tape.getType(stack);
        } else {
            mc.setScreen(null);
        }
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float f) {
        if (this.holo == null) return;

        double sizeX = 300;
        double sizeY = 150;
        double left = (this.width - sizeX) / 2;
        double top = (this.height - sizeY) / 2;

        // 1.7 setColorRGBA_F(0, 0.2, 0, 0.8)
        GuiDraw.fill(g, (int) left, (int) top, (int) (left + sizeX), (int) (top + sizeY), (204 << 24) | (51 << 8));

        int nextLevel = (int) top + 30;

        if (this.holo.getText() != null) {
            for (String text : autoBreak(this.holo.getText(), 275)) {
                GuiDraw.text(g, font, text, (int) (left + sizeX / 2 - font.width(text) / 2), nextLevel, 0xFF009900, true);
                nextLevel += 10;
            }
        }
    }

    /** 1.7 {@code I18nUtil.autoBreak}. */
    private List<String> autoBreak(String text, int width) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        lines.add(words[0]);
        int indent = font.width(words[0]);
        for (int w = 1; w < words.length; w++) {
            indent += font.width(" " + words[w]);
            if (indent <= width) {
                String last = lines.get(lines.size() - 1);
                lines.set(lines.size() - 1, last + " " + words[w]);
            } else {
                lines.add(words[w]);
                indent = font.width(words[w]);
            }
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
