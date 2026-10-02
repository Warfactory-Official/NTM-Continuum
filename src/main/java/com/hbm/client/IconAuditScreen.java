package com.hbm.client;

import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Harness-only page of the item icon audit ({@code ItemIconAudit}, query {@code hbm_icon_audit}). Draws a grid of
 * vanilla-looking 18x18 slot backgrounds on the container grey and one stack in each through the same GUI item call
 * a container slot uses. Cells are {@link #CELL} GUI pixels apart, so an icon that draws outside its 16x16 slot lands
 * on plain background where the analysis can see it. No count, no hover highlight, no tooltip; decorations
 * (durability/charge bars) are left out on purpose so the measurement is the model alone.
 * An empty page (no stacks) is the background reference. The {@code alt} page draws the same layout in unrelated
 * colours, so an icon pixel that happens to match the slot grey still differs from one of the two backgrounds.
 */
public final class IconAuditScreen extends CompatScreen {

    /** Distance between slot origins, in GUI pixels. 8 px of plain background around each slot. */
    public static final int CELL = 32;
    /** Offset of the 16x16 item origin inside its cell. */
    public static final int INSET = 8;
    public static final int MARGIN = 4;
    public static final int BACKGROUND = 0xFFC6C6C6;
    // background, slot interior, dark edge, light edge; [0] = vanilla container colours, [1] = the alt measurement page
    private static final int[][] COLOURS = {
            {BACKGROUND, 0xFF8B8B8B, 0xFF373737, 0xFFFFFFFF},
            {0xFF2050A0, 0xFFA02050, 0xFF20A050, 0xFF000000}};

    private final List<ItemStack> stacks;
    private final int slots;
    private final int[] colours;

    public IconAuditScreen(List<ItemStack> stacks, int slots, boolean alt) {
        super(Component.literal("hbm icon audit"));
        this.stacks = stacks;
        this.slots = slots;
        this.colours = COLOURS[alt ? 1 : 0];
    }

    public static int columns(int guiWidth) {
        return Math.max(1, (guiWidth - 2 * MARGIN) / CELL);
    }

    public static int rows(int guiHeight) {
        return Math.max(1, (guiHeight - 2 * MARGIN) / CELL);
    }

    /** GUI x of slot {@code index}'s 16x16 item origin. */
    public static int slotX(int index, int guiWidth) {
        return MARGIN + (index % columns(guiWidth)) * CELL + INSET;
    }

    public static int slotY(int index, int guiWidth) {
        return MARGIN + (index / columns(guiWidth)) * CELL + INSET;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int[] c = this.colours;
        GuiDraw.fill(graphics, 0, 0, this.width, this.height, c[0]);
        for (int i = 0; i < this.slots; i++) {
            int x = slotX(i, this.width);
            int y = slotY(i, this.width);
            // the inventory texture's slot: dark top/left edge, white bottom/right edge, grey interior and corners
            GuiDraw.fill(graphics, x - 1, y - 1, x + 17, y + 17, c[1]);
            GuiDraw.fill(graphics, x - 1, y - 1, x + 16, y, c[2]);
            GuiDraw.fill(graphics, x - 1, y, x, y + 16, c[2]);
            GuiDraw.fill(graphics, x, y + 16, x + 17, y + 17, c[3]);
            GuiDraw.fill(graphics, x + 16, y, x + 17, y + 16, c[3]);
        }
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (int i = 0; i < this.stacks.size() && i < this.slots; i++) {
            ItemStack stack = this.stacks.get(i);
            if (stack.isEmpty()) continue;
            GuiDraw.item(graphics, stack, slotX(i, this.width), slotY(i, this.width));
        }
    }
}
