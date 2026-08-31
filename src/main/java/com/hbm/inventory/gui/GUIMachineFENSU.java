package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineFENSU;
import com.hbm.lib.Library;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** FEnSU screen, retaining the legacy battery GUI's bar and redstone controls. */
public class GUIMachineFENSU extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineFENSU> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_battery.png");
    private static final String[] MODES = {"Input only", "Buffer (both)", "Output only", "Disabled"};
    private static final String[] PRIORITIES = {"Lowest", "Low", "Normal", "High", "Highest"};

    public GUIMachineFENSU(ContainerMachineFENSU menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        int fill = (int) menu.getPowerScaled(52L);
        if (fill > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 71, topPos + 69 - fill,
                    176F, 52F - fill, 34, fill, 256, 256);
        }
        GuiDraw.blit(graphics, TEXTURE, leftPos + 7, topPos + 34,
                176F, 52F + menu.getRedLow() * 18, 18, 18, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 151, topPos + 34,
                176F, 52F + menu.getRedHigh() * 18, 18, 18, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 152, topPos + 17,
                194F, 52F + (menu.getPriorityOrdinal() - 1) * 16, 16, 16, 256, 256);
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 36, 2);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component title = Component.literal(this.title.getString() + " ("
                + Library.getShortNumber(menu.getPower()) + " HE)");
        GuiDraw.text(graphics, font, title, (imageWidth - font.width(title)) / 2, titleLabelY, -12566464, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, -12566464, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(71, 17, 34, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(menu.getPower(), menu.getMaxPower(), menu.getDelta()),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(6, 33, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, "No redstone: ", MODES, menu.getRedLow(), mouseX, mouseY);
        } else if (isHovering(150, 33, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, "Redstone on: ", MODES, menu.getRedHigh(), mouseX, mouseY);
        } else if (isHovering(151, 16, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, "Priority: ", PRIORITIES, menu.getPriorityOrdinal(), mouseX, mouseY);
        }
    }

    private static void tooltip(GuiGraphicsExtractor graphics, String prefix, String[] names, int index,
                                int mouseX, int mouseY) {
        String value = index >= 0 && index < names.length ? names[index] : "?";
        GuiDraw.tooltip(graphics, Minecraft.getInstance().font, List.of(Component.literal(prefix + value)),
                Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        int button = -1;
        if (isHovering(6, 33, 18, 18, event.x(), event.y())) button = 0;
        else if (isHovering(150, 33, 18, 18, event.x(), event.y())) button = 1;
        else if (isHovering(151, 16, 16, 16, event.x(), event.y())) button = 2;

        if (button >= 0 && Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, button);
            return true;
        }
        return false;
    }
}
