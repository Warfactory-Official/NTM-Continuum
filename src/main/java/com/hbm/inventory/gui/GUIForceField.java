package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerForceField;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityForceField;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * 1.7 {@code GUIForceField}: {@code gui_field.png} at 176x168; the power bar at (8,17) and the shield bar at
 * (62,17), both 16x52 and filling upward from y=69; the on/off switch at (142,34), lit while the field is on.
 */
public class GUIForceField extends CompatContainerScreen<ContainerForceField> {

    public static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_field.png");

    public GUIForceField(ContainerForceField menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 168);
    }

    /** 1.7's info rectangles: inclusive at the bottom edge, exclusive at the top. */
    private boolean in(double mouseX, double mouseY, int x, int y, int w, int h) {
        return leftPos + x <= mouseX && leftPos + x + w > mouseX && topPos + y < mouseY && topPos + y + h >= mouseY;
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityForceField field = menu.getField();
        if (field == null) return;
        if (in(mouseX, mouseY, 8, 69 - 52, 16, 52)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(Library.getShortNumber(field.power) + "/"
                    + Library.getShortNumber(TileEntityForceField.maxPower) + "HE")), Optional.empty(), mouseX, mouseY);
        } else if (in(mouseX, mouseY, 62, 69 - 52, 16, 52)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(field.health + " / " + field.maxHealth + "HP")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        if (in(event.x(), event.y(), 142, 34, 18, 18)) {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            return true;
        }
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        TileEntityForceField field = menu.getField();
        if (field == null) return;

        int i = (int) field.getPowerScaled(52);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 8, topPos + 69 - i, 176, 52 - i, 16, i, 256, 256);

        int j = field.getHealthScaled(52);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 69 - j, 192, 52 - j, 16, j, 256, 256);

        if (field.isOn) GuiDraw.blit(graphics, TEXTURE, leftPos + 142, topPos + 34, 176, 52, 18, 18, 256, 256);
    }
}
