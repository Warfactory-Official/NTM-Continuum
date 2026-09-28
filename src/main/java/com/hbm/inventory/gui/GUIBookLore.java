package com.hbm.inventory.gui;

import com.hbm.items.special.ItemBookLore;
import dev.golem.core.Ids;
import dev.golem.core.data.Tags;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code GUIBookLore}: a 272x182 spread from {@code book_lore.png} (512x512), the binding tinted with the
 * cover colour, two 100-pixel pages of word-wrapped text where a lone {@code $} breaks the line, and page arrows
 * at the bottom corners.
 */
public class GUIBookLore extends CompatScreen {

    private static final Identifier texture = Ids.of("hbm", "textures/gui/book/book_lore.png");

    protected int guiLeft;
    protected int guiTop;
    protected static int sizeX = 272;
    protected static int sizeY = 182;

    protected String key;
    protected CompoundTag tag;

    //judgement
    protected int color;

    protected int page;
    protected int maxPage;

    public GUIBookLore(ItemStack stack) {
        super(Component.empty());
        this.tag = ItemBookLore.data(stack);
        if (tag == null) return;
        this.key = Tags.getStringOr(tag, "k", "");
        if (key.isEmpty()) return;

        this.color = Tags.getIntOr(tag, "cov_col", 0);
        if (color <= 0) color = 0x303030;
        this.maxPage = (int) Math.ceil(Tags.getIntOr(tag, "p", 0) / 2D) - 1;
    }

    @Override
    protected void init() {
        super.init();
        if (key == null || key.isEmpty()) Minecraft.getInstance().setScreen(null);
        this.guiLeft = (this.width - sizeX) / 2;
        this.guiTop = (this.height - sizeY) / 2;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int i, int j, float partialTick) {
        GuiDraw.blit(g, texture, guiLeft, guiTop, 0F, 0F, sizeX, sizeY, 512, 512, 0xFF000000 | color);
        // the pages and the text go above the tinted binding (older versions order the GUI by depth)
        GuiDraw.nextStratum(g);
        GuiDraw.blit(g, texture, guiLeft + 7, guiTop + 7, 0F, 182F, 258, 165, 512, 512);

        final boolean overY = j >= guiTop + 155 && j < guiTop + 165;
        if (page > 0) {
            boolean over = overY && i >= guiLeft + 24 && i <= guiLeft + 42;
            GuiDraw.blit(g, texture, guiLeft + 24, guiTop + 155, over ? 295F : 272F, 13F, 18, 10, 512, 512);
        }
        if (page < maxPage) {
            boolean over = overY && i >= guiLeft + 230 && i <= guiLeft + 248;
            GuiDraw.blit(g, texture, guiLeft + 230, guiTop + 155, over ? 295F : 272F, 0F, 18, 10, 512, 512);
        }
    }

    /** 1.7 {@code I18n.format}: {@code String.format} over the translation, or "Format error: ..." if it throws. */
    private static String format(String key, Object... args) {
        String raw = Language.getInstance().getOrDefault(key);
        try {
            return String.format(raw, args);
        } catch (IllegalFormatException e) {
            return "Format error: " + raw;
        }
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor g, int x, int y, float partialTick) {
        if (tag == null) return;
        String k = "book_lore." + key + ".page.";

        for (int i = 0; i < 2; i++) {
            int defacto = this.page * 2 + i;
            if (defacto >= Tags.getIntOr(tag, "p", 0)) continue;

            String text;
            CompoundTag argTag = tag.get("p" + defacto) instanceof CompoundTag c ? c : new CompoundTag();

            if (argTag.isEmpty()) {
                text = format(k + defacto);
            } else {
                List<String> args = new ArrayList<>();
                int index = 1;
                String arg = Tags.getStringOr(argTag, "a1", "");
                while (!arg.isEmpty()) {
                    args.add(arg);
                    index++;
                    arg = Tags.getStringOr(argTag, "a" + index, "");
                }
                text = format(k + defacto, args.toArray());
            }

            float scale = 1;
            int width = 100;
            int widthScaled = (int) (width * scale);

            List<String> lines = new ArrayList<>();
            String[] words = text.split(" ");

            lines.add(words[0]);
            int indent = font.width(words[0]);

            for (int w = 1; w < words.length; w++) {
                if (words[w].equals("$")) {
                    if (w + 1 < words.length && !words[w + 1].equals("$")) {
                        lines.add(words[++w]);
                        indent = font.width(words[w]);
                    } else
                        lines.add("");
                    continue;
                }

                indent += font.width(" " + words[w]);

                if (indent <= widthScaled) {
                    String last = lines.get(lines.size() - 1);
                    lines.set(lines.size() - 1, last + " " + words[w]);
                } else {
                    lines.add(words[w]);
                    indent = font.width(words[w]);
                }
            }

            for (int l = 0; l < lines.size(); l++) {
                GuiDraw.text(g, font, lines.get(l), (int) ((guiLeft + 20 + i * 130) * scale),
                        (int) ((guiTop + 20) * scale + (9 * l)), 0xFF0F0F0F, false);
            }
        }
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        double i = event.x(), j = event.y();
        if (j < guiTop + 155 || j >= guiTop + 165) return false;

        if (page > 0 && i >= guiLeft + 24 && i <= guiLeft + 42) {
            page--;
            press();
        }
        if (page < maxPage && i >= guiLeft + 230 && i <= guiLeft + 248) {
            page++;
            press();
        }
        return true;
    }

    private static void press() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
}
