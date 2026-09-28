package com.hbm.inventory.gui;

import com.hbm.items.tool.ItemGuideBook;
import com.hbm.items.tool.ItemGuideBook.BookType;
import com.hbm.items.tool.ItemGuideBook.GuideImage;
import com.hbm.items.tool.ItemGuideBook.GuidePage;
import com.hbm.items.tool.ItemGuideBook.GuideText;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code GUIScreenGuide}: a 272x182 book from a 512x512 sheet. It opens on the cover (the book's
 * {@code $}-separated title in gold, scaled up), a click turns to the first spread, and the arrows at the
 * bottom corners page through two pages at a time. Page text is drawn at {@code 1/scale} size and wrapped at
 * {@code width * scale} unscaled pixels, both exactly as 1.7 computed them.
 */
public class GUIScreenGuide extends CompatScreen {

    private static final Identifier texture = Ids.of("hbm", "textures/gui/book/book.png");
    private static final Identifier texture_cover = Ids.of("hbm", "textures/gui/book/book_cover.png");

    protected int xSize;
    protected int ySize;
    protected int guiLeft;
    protected int guiTop;

    private final BookType type;

    int page;
    int maxPage;

    public GUIScreenGuide(ItemStack held) {
        super(Component.empty());
        type = ItemGuideBook.getType(held);
        page = -1;
        maxPage = (int) Math.ceil(type.pages.size() / 2D) - 1;
        this.xSize = 272;
        this.ySize = 182;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int i, int j, float partialTick) {
        if (page < 0) {
            GuiDraw.blit(g, texture_cover, guiLeft, guiTop, 0F, 0F, xSize, ySize, 512, 512);
            return;
        }

        GuiDraw.blit(g, texture, guiLeft, guiTop, 0F, 0F, xSize, ySize, 512, 512);

        boolean overLeft = i >= guiLeft + 24 && i < guiLeft + 42 && j >= guiTop + 155 && j < guiTop + 165;
        boolean overRight = i >= guiLeft + 230 && i < guiLeft + 248 && j >= guiTop + 155 && j < guiTop + 165;

        if (this.page > 0) {
            GuiDraw.blit(g, texture, guiLeft + 24, guiTop + 155, overLeft ? 26F : 3F, 207F, 18, 10, 512, 512);
        }
        if (this.page < this.maxPage) {
            GuiDraw.blit(g, texture, guiLeft + 230, guiTop + 155, overRight ? 26F : 3F, 194F, 18, 10, 512, 512);
        }
    }

    private static String resolve(String key) {
        return Component.translatable(key).getString();
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor g, int x, int y, float partialTick) {
        if (this.page < 0) {
            float scale = this.type.titleScale;
            String[] coverLines = resolve(this.type.title).split("\\$");

            for (int i = 0; i < coverLines.length; i++) {
                String cover = coverLines[i];
                GuiDraw.pushMatrix(g);
                GuiDraw.scale(g, scale, scale);
                GuiDraw.text(g, font, cover,
                        (int) ((guiLeft + ((this.xSize / 2) - (font.width(cover) / 2 * scale))) / scale),
                        (int) ((guiTop + 50 + i * 10 * scale) / scale), 0xFFFECE00, false);
                GuiDraw.popMatrix(g);
            }
            return;
        }

        int sideOffset = 130;

        for (int i = 0; i < 2; i++) {
            int defacto = this.page * 2 + i;
            if (defacto >= this.type.pages.size()) continue;

            GuidePage page = this.type.pages.get(defacto);

            for (GuideText textBox : page.texts) {
                float scale = textBox.scale;
                String text = resolve(textBox.text);
                int width = textBox.width;

                int widthScaled = (int) (width * scale);
                List<String> lines = new ArrayList<>();
                String[] words = text.split(" ");

                lines.add(words[0]);
                int indent = font.width(words[0]);

                for (int w = 1; w < words.length; w++) {
                    indent += font.width(" " + words[w]);
                    if (indent <= widthScaled) {
                        String last = lines.get(lines.size() - 1);
                        lines.set(lines.size() - 1, last + " " + words[w]);
                    } else {
                        lines.add(words[w]);
                        indent = font.width(words[w]);
                    }
                }

                float titleScale = getOverrideScale(page.titleScale, page.title + ".scale");

                GuiDraw.pushMatrix(g);
                GuiDraw.scale(g, 1F / scale, 1F / scale);

                float topOffset;
                if (textBox.yOffset == -1) {
                    topOffset = page.title == null ? -10 : 6 / titleScale;
                } else {
                    topOffset = textBox.yOffset;
                }

                for (int l = 0; l < lines.size(); l++) {
                    GuiDraw.text(g, font, lines.get(l), (int) ((guiLeft + 20 + i * sideOffset + textBox.xOffset) * scale),
                            (int) ((guiTop + 30 + topOffset) * scale + (12 * l)), 0xFF404040, false);
                }
                GuiDraw.popMatrix(g);
            }

            if (page.title != null) {
                float tScale = page.titleScale;
                String titleLoc = resolve(page.title);

                GuiDraw.pushMatrix(g);
                GuiDraw.scale(g, 1F / tScale, 1F / tScale);
                GuiDraw.text(g, font, titleLoc,
                        (int) ((guiLeft + 20 + i * sideOffset + ((100 / 2) - (font.width(titleLoc) / 2 / tScale))) * tScale),
                        (int) ((guiTop + 20) * tScale), 0xFF000000 | page.titleColor, false);
                GuiDraw.popMatrix(g);
            }

            for (GuideImage image : page.images) {
                int ix = image.x();
                if (ix == -1) ix = 100 / 2 - image.sizeX() / 2;
                // 1.7 drawImage: the whole texture stretched over the box, whatever its pixel size.
                GuiDraw.blitUv(g, Ids.parse(image.image()), guiLeft + 20 + ix + sideOffset * i, guiTop + image.y(),
                        image.sizeX(), image.sizeY(), 0F, 0F, 1F, 1F, image.sizeX(), image.sizeY());
            }

            String pageLabel = (defacto + 1) + "/" + (this.type.pages.size());
            GuiDraw.text(g, font, pageLabel, guiLeft + 44 + i * 185 - i * font.width(pageLabel), guiTop + 156,
                    0xFF404040, false);
        }
    }

    private static float getOverrideScale(float def, String tag) {
        String scale = resolve(tag);
        try {
            return 1F / Float.parseFloat(scale);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        double i = event.x(), j = event.y();
        if (page < 0) {
            page = 0;
            press();
            return true;
        }

        boolean overLeft = i >= guiLeft + 24 && i < guiLeft + 42 && j >= guiTop + 155 && j < guiTop + 165;
        boolean overRight = i >= guiLeft + 230 && i < guiLeft + 248 && j >= guiTop + 155 && j < guiTop + 165;

        if (overLeft && page > 0) {
            page--;
            press();
        }
        if (overRight && page < maxPage) {
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
