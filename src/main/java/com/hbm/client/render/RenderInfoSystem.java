package com.hbm.client.render;

import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * 1.7 {@code RenderInfoSystem}: the stack of short-lived messages (tool ability changes and the like)
 * drawn on a translucent grey panel, each fading out over its lifetime. Messages sharing an id replace
 * each other. Client-only.
 *
 * <p>1.7's {@code ClientConfig} position and offsets are all at their defaults here: mode 0, the top-left
 * corner at 15/15.
 */
public final class RenderInfoSystem {

    private RenderInfoSystem() {}

    private static int nextID = 1000;
    private static final HashMap<Integer, InfoEntry> inbox = new HashMap<>();
    private static final HashMap<Integer, InfoEntry> messages = new HashMap<>();

    /** 1.7 {@code INFO_POSITION}, {@code INFO_OFFSET_HORIZONTAL}, {@code INFO_OFFSET_VERTICAL}. */
    public static int infoPosition = 0;
    public static int infoOffsetHorizontal = 0;
    public static int infoOffsetVertical = 0;

    /** 1.7 {@code clentTick}: take in the inbox and drop expired messages. */
    public static void clientTick() {
        synchronized (inbox) {
            messages.putAll(inbox);
            inbox.clear();
        }
        long now = System.currentTimeMillis();
        messages.values().removeIf(entry -> entry.start + entry.millis < now);
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (messages.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        List<InfoEntry> entries = new ArrayList<>(messages.values());
        Collections.sort(entries);

        int longest = 0;
        for (InfoEntry entry : entries) {
            int length = mc.font.width(entry.text);
            if (length > longest) longest = length;
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int mode = infoPosition;

        int pX = mode == 0 ? 15 : mode == 1 ? (width - longest - 15) : mode == 2 ? (width / 2 + 7) : (width / 2 - longest - 6);
        int pZ = mode == 0 ? 15 : mode == 1 ? 15 : height / 2 + 7;

        pX += infoOffsetHorizontal;
        pZ += infoOffsetVertical;

        int side = pX + 5 + longest;
        int bottom = messages.size() * 10 + pZ + 2;

        // 0.25 grey at half alpha
        GuiDraw.fill(graphics, pX - 5, pZ - 5, side, bottom, 0x80404040);

        int off = 0;
        long now = System.currentTimeMillis();

        for (InfoEntry entry : entries) {
            int elapsed = (int) (now - entry.start);
            // smoothly scales down from 510 to 0, then caps at 255
            int alpha = Math.max(Math.min(510 * (entry.millis - elapsed) / entry.millis, 255), 5);
            int color = (entry.color & 0xFFFFFF) | (alpha << 24);
            GuiDraw.text(graphics, mc.font, entry.text, pX, pZ + off, color, false);
            off += 10;
        }
    }

    public static void push(InfoEntry entry) {
        push(entry, nextID++); // range is so large, collisions are unlikely and if they do occur, not a big deal
    }

    public static void push(InfoEntry entry, int id) {
        synchronized (inbox) {
            inbox.put(id, entry);
        }
    }

    /** 1.7 {@code ClientProxy.displayTooltip}: id 0 means "a new message", millis 0 means 1000. */
    public static void displayTooltip(Component msg, int millis, int id) {
        InfoEntry entry = new InfoEntry(msg, millis <= 0 ? 1000 : millis);
        if (id != 0) push(entry, id);
        else push(entry);
    }

    /** The texts currently shown, oldest-sorted as drawn; for tests and the harness. */
    public static List<String> currentTexts() {
        List<InfoEntry> entries = new ArrayList<>(messages.values());
        synchronized (inbox) {
            entries.addAll(inbox.values());
        }
        Collections.sort(entries);
        List<String> out = new ArrayList<>();
        for (InfoEntry e : entries) out.add(e.text.getString());
        return out;
    }

    public static class InfoEntry implements Comparable<InfoEntry> {

        final Component text;
        int color = 0xffffff;
        final long start;
        final int millis;

        public InfoEntry(Component text) {
            this(text, 3000);
        }

        public InfoEntry(Component text, int millis) {
            this.text = text;
            this.millis = millis;
            this.start = System.currentTimeMillis();
        }

        public InfoEntry withColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public int compareTo(InfoEntry other) {
            return Integer.compare(this.millis, other.millis);
        }
    }
}
