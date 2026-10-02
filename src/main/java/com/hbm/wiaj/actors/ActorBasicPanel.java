package com.hbm.wiaj.actors;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** A plain tooltip-styled box of text and stacks, the precursor of {@link ActorFancyPanel}. */
public class ActorBasicPanel implements ISpecialActor {

    int x;
    int y;
    List<Object[]> lines;

    public ActorBasicPanel(int x, int y, Object[]... objects) {
        this.x = x;
        this.y = y;
        this.lines = new ArrayList<>();

        for(Object[] o : objects) {
            this.lines.add(o);
        }
    }

    @Override
    public void drawForegroundComponent(GuiGraphicsExtractor graphics, int w, int h, int ticks, float interp) {
        drawStackText(graphics, lines, x, y, Minecraft.getInstance().font, w, h);
    }

    @Override
    public void drawBackgroundComponent(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp) { }

    @Override
    public void updateActor(JarScene scene) { }

    @Override
    public void setActorData(CompoundTag data) { }

    @Override
    public void setDataPoint(String tag, Object o) { }

    protected void drawStackText(GuiGraphicsExtractor graphics, List<Object[]> lines, int x, int y, Font font, int w, int h) {

        x += w / 2;
        y += h / 2;

        if(!lines.isEmpty()) {

            int height = 0;
            int longestline = 0;

            for(Object[] line : lines) {
                int lineWidth = 0;

                boolean hasStack = false;

                for(Object o : line) {

                    if(o instanceof String) {
                        lineWidth += font.width((String) o);
                    } else {
                        lineWidth += 18;
                        hasStack = true;
                    }
                }

                if(hasStack) {
                    height += 18;
                } else {
                    height += 10;
                }

                if(lineWidth > longestline) {
                    longestline = lineWidth;
                }
            }

            int minX = x + 12;
            int minY = y - 12;

            if(minX + longestline > w) {
                minX -= 28 + longestline;
            }

            if(minY + height + 6 > h) {
                minY = h - height - 6;
            }

            int colorBg = 0xF0100010;
            GuiDraw.fillGradient(graphics, minX - 3, minY - 4, minX + longestline + 3, minY - 3, colorBg, colorBg);
            GuiDraw.fillGradient(graphics, minX - 3, minY + height + 3, minX + longestline + 3, minY + height + 4, colorBg, colorBg);
            GuiDraw.fillGradient(graphics, minX - 3, minY - 3, minX + longestline + 3, minY + height + 3, colorBg, colorBg);
            GuiDraw.fillGradient(graphics, minX - 4, minY - 3, minX - 3, minY + height + 3, colorBg, colorBg);
            GuiDraw.fillGradient(graphics, minX + longestline + 3, minY - 3, minX + longestline + 4, minY + height + 3, colorBg, colorBg);
            int color0 = 0x505000FF;
            int color1 = (color0 & 0xFEFEFE) >> 1 | color0 & 0xFF000000;
            GuiDraw.fillGradient(graphics, minX - 3, minY - 3 + 1, minX - 3 + 1, minY + height + 3 - 1, color0, color1);
            GuiDraw.fillGradient(graphics, minX + longestline + 2, minY - 3 + 1, minX + longestline + 3, minY + height + 3 - 1, color0, color1);
            GuiDraw.fillGradient(graphics, minX - 3, minY - 3, minX + longestline + 3, minY - 3 + 1, color0, color0);
            GuiDraw.fillGradient(graphics, minX - 3, minY + height + 2, minX + longestline + 3, minY + height + 3, color1, color1);

            for(int index = 0; index < lines.size(); ++index) {

                Object[] line = lines.get(index);
                int indent = 0;
                boolean hasStack = false;

                for(Object o : line) {
                    if(!(o instanceof String)) {
                        hasStack = true;
                    }
                }

                for(Object o : line) {

                    if(o instanceof String text) {
                        GuiDraw.text(graphics, font, text, minX + indent, minY + (hasStack ? 4 : 0), 0xFFFFFFFF, true);
                        indent += font.width(text) + 2;
                    } else {
                        ItemStack stack = (ItemStack) o;
                        GuiDraw.item(graphics, stack, minX + indent, minY);
                        GuiDraw.itemDecorations(graphics, font, stack, minX + indent, minY);
                        indent += 18;
                    }
                }

                if(index == 0) {
                    minY += 2;
                }

                minY += hasStack ? 18 : 10;
            }
        }
    }
}
