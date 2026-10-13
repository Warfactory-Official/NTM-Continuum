package com.hbm.inventory.gui;

import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineFENSU;
import com.hbm.lib.Library;
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

/** FEnSU screen, retaining the legacy battery GUI's bar and redstone controls. */
public class GUIMachineFENSU extends CompatContainerScreen<ContainerMachineFENSU> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_battery.png");
    private static final String[] MODES = {"Input only", "Buffer (both)", "Output only", "Disabled"};
    private static final String[] PRIORITIES = {"Lowest", "Low", "Normal", "High", "Highest"};

    public GUIMachineFENSU(ContainerMachineFENSU menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 1.7: the FEnSU uses the energy storage block's screen (GUIMachineBattery).
        GUIMachineBattery.drawBattery(graphics, TEXTURE, leftPos, topPos, imageWidth, imageHeight,
                (int) menu.getPowerScaled(52L), menu.getRedLow(), menu.getRedHigh(), menu.getPriorityOrdinal());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component title = Component.literal(this.title.getString() + " (" + menu.getPower() + " HE)");
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(62, 17, 52, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GUIMachineBattery.barInfo(menu.getPower(), menu.getMaxPower(), menu.getDelta()),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(152, 35, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GUIMachineBattery.priorityInfo(menu.getPriorityOrdinal()), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        int button = GUIMachineBattery.button(event.x(), event.y(), leftPos, topPos);
        if (button >= 0 && Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, button);
            return true;
        }
        return false;
    }
}
