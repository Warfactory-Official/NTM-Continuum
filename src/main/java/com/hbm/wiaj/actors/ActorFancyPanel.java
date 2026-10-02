package com.hbm.wiaj.actors;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The framed text box of the presentations: a bevelled frame in four colours, an optional arrow pointing
 * at whatever the box is about, and lines of text and item stacks.
 *
 * <p>Geometry, colours and the arrow sprites are 1.7's to the pixel. The rects that 1.7 drew as
 * {@code drawGradientRect} at z 300 are plain fills here: drawing order is submission order in a 26.1
 * screen, and {@link GuiDraw} keeps it on the older targets.
 */
public class ActorFancyPanel implements ISpecialActor {

    static final Identifier guiUtil = dev.golem.core.Ids.of("hbm", "textures/gui/gui_utility.png");

    List<Object[]> lines = new ArrayList<>();
    Font font;

    int x;
    int y;

    boolean consistentHeight = false;
    int lineDist = 2;
    int tallestElement = 0;
    static final int STACK_HEIGHT = 18;

    Orientation o = Orientation.CENTER;
    int colorBrighter = 0xFFCCCCCC;
    int colorDarker = 0xFF7D7D7D;
    int colorFrame = 0xFFA0A0A0;
    int colorBg = 0xFF302E36;

    public ActorFancyPanel(Font font, int x, int y, Object[][] raw, int autowrap) {

        this.font = font;
        this.x = x;
        this.y = y;

        if(autowrap <= 0) { //if autowrap is off, just add the lines 1:1
            for(Object[] o : raw) {
                lines.add(o);
            }
        } else {

            for(Object[] line : raw) {

                if(line.length == 1 && line[0] instanceof String) { //auto wrap can only apply to text-only lines
                    List<String> frags = autoBreak(font, (String) line[0], autowrap);
                    for(String s : frags) {
                        lines.add(new Object[] { s });
                    }
                } else {
                    lines.add(line);
                }
            }
        }
    }

    /** 1.7 {@code I18nUtil.autoBreak}: greedy word wrap on spaces, the first word always on the first line. */
    public static List<String> autoBreak(Font font, String text, int width) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        lines.add(words[0]);
        int indent = font.width(words[0]);

        for(int w = 1; w < words.length; w++) {
            indent += font.width(" " + words[w]);

            if(indent <= width) {
                String last = lines.get(lines.size() - 1);
                lines.set(lines.size() - 1, last + " " + words[w]);
            } else {
                lines.add(words[w]);
                indent = font.width(words[w]);
            }
        }

        return lines;
    }

    public ActorFancyPanel enforceConsistentHeight() {
        this.consistentHeight = true;
        return this;
    }

    public ActorFancyPanel setLineDist(int dist) {
        this.lineDist = dist;
        return this;
    }

    public ActorFancyPanel setOrientation(Orientation o) {
        this.o = o;
        return this;
    }

    public ActorFancyPanel setColors(int brighter, int frame, int darker, int background) {
        this.colorBrighter = brighter;
        this.colorFrame = frame;
        this.colorDarker = darker;
        this.colorBg = background;
        return this;
    }

    public ActorFancyPanel setColors(int[] colors) {
        return setColors(colors[0], colors[1], colors[2], colors[3]);
    }

    public int getTallestElement() {
        if(this.tallestElement > 0) {
            return this.tallestElement;
        }

        for(Object[] line : this.lines) {
            for(Object element : line) {

                int height = getElementHeight(element);

                if(height > this.tallestElement) {
                    this.tallestElement = height;
                }
            }
        }

        return this.tallestElement;
    }

    public int getElementHeight(Object element) {

        if(element instanceof String) {
            return this.font.lineHeight;
        }

        if(element instanceof ItemStack) {
            return STACK_HEIGHT;
        }

        if(element instanceof Object[] scaledStack) { //[0] ItemStack, [1] double
            return (int) Math.ceil(STACK_HEIGHT * (double) scaledStack[1]);
        }

        return 0;
    }

    public int getElementWidth(Object element) {

        if(element instanceof String) {
            return this.font.width((String) element);
        }

        if(element instanceof ItemStack) {
            return STACK_HEIGHT;
        }

        if(element instanceof Object[] scaledStack) { //[0] ItemStack, [1] double
            return (int) Math.ceil(STACK_HEIGHT * (double) scaledStack[1]);
        }

        return 0;
    }

    int blockHeight = 0;
    private int getBlockHeight() {

        if(this.blockHeight > 0) {
            return this.blockHeight;
        }

        for(Object[] line : this.lines) {

            if(this.blockHeight > 0) {
                this.blockHeight += this.lineDist;
            }

            int lineHeight = this.font.lineHeight;

            if(this.consistentHeight) {
                lineHeight = Math.max(lineHeight, getTallestElement());
            } else {

                for(Object o : line) {
                    lineHeight = Math.max(lineHeight, getElementHeight(o));
                }
            }

            this.blockHeight += lineHeight;
        }

        return this.blockHeight;
    }

    int blockWidth = 0;
    private int getBlockWidth() {

        if(this.blockWidth > 0) {
            return this.blockWidth;
        }

        for(Object[] line : this.lines) {

            int lineWidth = 0;

            for(Object o : line) {

                if(lineWidth > 0) {
                    lineWidth += 2;
                }

                lineWidth += getElementWidth(o);
            }

            if(lineWidth > this.blockWidth) {
                this.blockWidth = lineWidth;
            }
        }

        return this.blockWidth;
    }

    @Override
    public void drawForegroundComponent(GuiGraphicsExtractor graphics, int w, int h, int ticks, float interp) {

        int height = this.getBlockHeight();
        int width = this.getBlockWidth();

        int posX = w / 2 + x;
        int posY = h / 2 + y;

        switch(o) {
        case TOP:
            posX -= width / 2;
            posY += 15;
            break;
        case BOTTOM:
            posX -= width / 2;
            posY -= height + 15;
            break;
        case LEFT:
            posX += 15;
            posY -= height / 2;
            break;
        case RIGHT:
            posX -= width + 15;
            posY -= height / 2;
            break;
        case CENTER:
            posX -= width / 2;
            posY -= height / 2;
            break;
        }

        this.drawRect(graphics, posX - 5, posY - 5, posX + width + 5, posY + height + 5, colorFrame);

        this.drawRect(graphics, posX - 5, posY - 5, posX - 4, posY + height + 4, colorBrighter);
        this.drawRect(graphics, posX - 5, posY - 5, posX + width + 4, posY - 4, colorBrighter);
        this.drawRect(graphics, posX + width + 2, posY - 2, posX + width + 3, posY + height + 3, colorBrighter);
        this.drawRect(graphics, posX - 2, posY + height + 2, posX + width + 3, posY + height + 3, colorBrighter);

        this.drawRect(graphics, posX - 3, posY - 3, posX - 2, posY + height + 2, colorDarker);
        this.drawRect(graphics, posX - 3, posY - 3, posX + width + 2, posY - 2, colorDarker);
        this.drawRect(graphics, posX + width + 4, posY - 4, posX + width + 5, posY + height + 5, colorDarker);
        this.drawRect(graphics, posX - 4, posY + height + 4, posX + width + 5, posY + height + 5, colorDarker);

        this.drawRect(graphics, posX - 2, posY - 2, posX + width + 2, posY + height + 2, colorBg);

        switch(o) {
        case TOP:
            this.drawArrow(graphics, posX + width / 2 - 7, posY - 15, 40, 14, 14, 10);
            break;
        case BOTTOM:
            this.drawArrow(graphics, posX + width / 2 - 7, posY + height + 5, 54, 14, 14, 10);
            break;
        case LEFT:
            this.drawArrow(graphics, posX - 15, posY + height / 2 - 7, 40, 0, 10, 14);
            break;
        case RIGHT:
            this.drawArrow(graphics, posX + width + 5, posY + height / 2 - 7, 50, 0, 10, 14);
            break;
        case CENTER: break;
        }

        int offsetY = 0;

        for(Object[] line : this.lines) {

            if(offsetY > 0) offsetY += this.lineDist;
            int lineHeight = 0;
            for(Object element : line) lineHeight = Math.max(lineHeight, this.getElementHeight(element));

            int indent = 0;
            for(Object element : line) {

                if(indent > 0) {
                    indent += 2;
                }

                drawElement(graphics, posX + indent, posY + offsetY + lineHeight / 2, element);
                indent += getElementWidth(element);
            }

            offsetY += lineHeight;
        }
    }

    private void drawArrow(GuiGraphicsExtractor graphics, int posX, int posY, int sourceX, int sourceY, int sizeX, int sizeY) {
        this.drawTexturedModalRect(graphics, posX, posY, sourceX + 28 * 0, sourceY, sizeX, sizeY, this.colorBrighter);
        this.drawTexturedModalRect(graphics, posX, posY, sourceX + 28 * 1, sourceY, sizeX, sizeY, this.colorFrame);
        this.drawTexturedModalRect(graphics, posX, posY, sourceX + 28 * 2, sourceY, sizeX, sizeY, this.colorDarker);
        this.drawTexturedModalRect(graphics, posX, posY, sourceX + 28 * 3, sourceY, sizeX, sizeY, this.colorBg);
    }

    private void drawElement(GuiGraphicsExtractor graphics, int x, int y, Object element) {

        if(element instanceof String text) {
            GuiDraw.text(graphics, this.font, text, x, y - this.font.lineHeight / 2, 0xFFFFFFFF, false);

        } else if(element instanceof ItemStack stack) {
            // 1.7 outlined a zero-sized stack in red here. A modern stack of size 0 is the empty stack and
            // has no item left to draw, so the presentations use size 1 where they meant "just the item".
            GuiDraw.item(graphics, stack, x, y - 8);
            GuiDraw.itemDecorations(graphics, this.font, stack, x, y - 8);
        } //TODO: scaled stacks
    }

    @Override public void drawBackgroundComponent(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp) { }
    @Override public void updateActor(JarScene scene) { }
    @Override public void setActorData(CompoundTag data) { }
    @Override public void setDataPoint(String tag, Object o) { }

    protected void drawRect(GuiGraphicsExtractor graphics, int minX, int minY, int maxX, int maxY, int color) {
        GuiDraw.fill(graphics, minX, minY, maxX, maxY, color);
    }

    public void drawTexturedModalRect(GuiGraphicsExtractor graphics, int posX, int posY, int u, int v, int sizeX, int sizeY, int color) {
        GuiDraw.blit(graphics, guiUtil, posX, posY, u, v, sizeX, sizeY, 256, 256, color);
    }

    /** where the arrow should be or if the box should be centered around the home position */
    public static enum Orientation {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT,
        CENTER
    }
}
