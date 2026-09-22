package com.hbm.inventory.gui;

import com.hbm.items.ModDataComponents;
import com.hbm.packet.ManualDesignatorPayload;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** The source-shaped 176x178 manual designator coordinate editor. */
public final class GUIScreenDesignator extends CompatScreen {
    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_designator.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 178;
    private final ItemStack stack;
    private int left;
    private int top;
    private int shownX;
    private int shownZ;

    public GUIScreenDesignator(ItemStack stack) {
        super(Component.empty());
        this.stack = stack;
        this.shownX = ModDataComponents.DESIGNATOR_X.getOr(stack, 0);
        this.shownZ = ModDataComponents.DESIGNATOR_Z.getOr(stack, 0);
    }

    @Override
    protected void init() {
        super.init();
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                           float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, left, top, 0F, 0F, WIDTH, HEIGHT, 256, 256);
        for (Button button : buttons()) {
            boolean hover = mouseX >= button.x && mouseX < button.x + 18
                    && mouseY >= button.y && mouseY < button.y + 18;
            GuiDraw.blit(graphics, TEXTURE, button.x, button.y,
                    hover ? 194F : 176F, button.type * 18F, 18, 18, 256, 256);
        }
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                           float partialTick) {
        String x = "X: " + shownX;
        String z = "Z: " + shownZ;
        GuiDraw.text(graphics, font, x, left + WIDTH / 2 - font.width(x) / 2, top + 50, 0xFF404040, false);
        GuiDraw.text(graphics, font, z, left + WIDTH / 2 - font.width(z) / 2, top + 50 + 18 * 4,
                0xFF404040, false);
    }

    @Override
    public void tick() {
        super.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getMainHandItem().getItem()
                instanceof com.hbm.items.tool.ItemDesignatorManual)) {
            mc.setScreen(null);
        }
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        if (event.button() != 0) return false;
        for (Button button : buttons()) {
            if (event.x() >= button.x && event.x() < button.x + 18
                    && event.y() >= button.y && event.y() < button.y + 18) {
                apply(button.operator, button.value, button.reference);
                return true;
            }
        }
        return false;
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

    private void apply(int operator, int value, int reference) {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        GolemNetwork.sendToServer(ManualDesignatorPayload.CODEC,
                new ManualDesignatorPayload(operator, value, reference));
        if (operator == 2) {
            if (reference == 0) shownX = (int) Math.round(Minecraft.getInstance().player.getX());
            else shownZ = (int) Math.round(Minecraft.getInstance().player.getZ());
        } else {
            int delta = operator == 0 ? value : -value;
            if (reference == 0) shownX += delta;
            else shownZ += delta;
        }
    }

    private Button[] buttons() {
        Button[] result = new Button[22];
        int index = 0;
        int[] steps = {1, 5, 10, 50, 100};
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < steps.length; i++)
                result[index++] = new Button(left + 25 + i * 27, top + 26 + row * 36,
                        row * 5 + i, row == 0 ? 0 : 1, 0, steps[i]);
        }
        result[index++] = new Button(left + 133, top + 44, 10, 2, 0, 0);
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < steps.length; i++)
                result[index++] = new Button(left + 25 + i * 27, top + 98 + row * 36,
                        row * 5 + i, row == 0 ? 0 : 1, 1, steps[i]);
        }
        result[index] = new Button(left + 133, top + 116, 10, 2, 1, 0);
        return result;
    }

    private record Button(int x, int y, int type, int operator, int reference, int value) {}
}
