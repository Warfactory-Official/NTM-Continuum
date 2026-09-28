package com.hbm.inventory.gui;

import com.hbm.Constants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.Screenshots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * 1.7 {@code GUIScreenWikiRender}, opened by {@code /ntmwikirender}: draws each stack large in the bottom-left corner
 * over a magenta screen and saves that corner as a PNG with the magenta keyed out, one stack after the other, then
 * closes itself. Files land in {@code <game dir>/<directory>/<prefix><display name>.png}.
 *
 * <p>1.7 read the pixels back with {@code glReadPixels} inside the draw call. A modern frame is composed and read
 * back by the render system, so each stack is drawn for one frame and captured on the following client tick, when
 * the main render target still holds that frame.
 */
public class GUIScreenWikiRender extends CompatScreen {

    private static final int MAGENTA = 0xFFFF00FF;

    protected final ItemStack[] preview;
    protected int index = 0;
    protected final int scale;
    protected final String saveLocation;
    protected final String prefix;
    protected Function<ItemStack, String> getStackName = stack -> stack.getHoverName().getString();
    private int drawnIndex = -1;

    public GUIScreenWikiRender(ItemStack[] stacks, String prefix, String directory, int scale) {
        super(Component.empty());
        this.preview = stacks;
        this.prefix = prefix;
        this.saveLocation = directory;
        this.scale = scale;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.fill(g, 0, 0, this.width, this.height, MAGENTA);
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (index >= preview.length) return;
        GuiDraw.pushMatrix(g);
        GuiDraw.scale(g, scale, scale);
        GuiDraw.translate(g, 9F, this.height / (float) scale - 9F);
        GuiDraw.item(g, preview[index], -8, -8);
        GuiDraw.popMatrix(g);
        drawnIndex = index;
    }

    @Override
    public void tick() {
        super.tick();
        Minecraft minecraft = Minecraft.getInstance();
        // Once we've reached the end of the array, immediately close this GUI
        if (index >= preview.length) {
            minecraft.setScreen(null);
            return;
        }
        if (drawnIndex != index) return;

        ItemStack stack = preview[index];
        String slotName = getStackName.apply(stack).replaceAll("§.", "").replaceAll("[^\\w ().-]+", "");
        int zoom = scale * (int) minecraft.getWindow().getGuiScale();
        Path file = minecraft.gameDirectory.toPath().resolve(saveLocation).resolve(prefix + slotName + ".png");
        index++;
        if (slotName.endsWith(".name")) return; // 1.7: an untranslated name is not worth a file
        Screenshots.capture(frame -> {
            try (frame) {
                int size = zoom * 16;
                try (NativeImage slot = Screenshots.crop(frame, zoom, frame.getHeight() - zoom - size, size, size)) {
                    Screenshots.keyOut(slot, MAGENTA);
                    Files.createDirectories(file.getParent());
                    slot.writeToFile(file);
                    Constants.LOG.info("Saved wiki render {}", file);
                }
            } catch (Exception ex) {
                // Just skip any failures caused by display name or rendering
                Constants.LOG.warn("Failed to save NTM screenshot {}", file, ex);
            }
        });
    }
}
