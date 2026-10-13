package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerOreSlopper;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.machine.TileEntityMachineOreSlopper;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/** 1.7 {@code GUIOreSlopper}: {@code gui_ore_slopper.png}, 176x204. */
public final class GUIOreSlopper extends CompatContainerScreen<ContainerOreSlopper> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_ore_slopper.png");

    public GUIOreSlopper(ContainerOreSlopper menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityMachineOreSlopper slopper = menu.getSlopper();
        if (slopper == null) return;

        int i = (int) (slopper.progress * 35);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 52 - i, 176F, 34F - i, 34, i, 256, 256);

        int j = (int) (slopper.power * 52 / TileEntityMachineOreSlopper.maxPower);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 8, topPos + 70 - j, 176F, 86F - j, 16, j, 256, 256);

        if (slopper.power >= slopper.consumption)
            GuiDraw.blit(graphics, TEXTURE, leftPos + 12, topPos + 4, 202F, 34F, 9, 12, 256, 256);

        Production2Gui.drawTank(graphics, slopper.tanks[0], leftPos + 26, topPos + 70, 16, 52);
        Production2Gui.drawTank(graphics, slopper.tanks[1], leftPos + 116, topPos + 70, 16, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2 - 9, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineOreSlopper slopper = menu.getSlopper();
        if (slopper == null) return;

        // 1.7 renderTankInfo(26, 18, 34, 52) covers the water column plus the gap beside it.
        if (isHovering(26, 18, 34, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(slopper.tanks[0]), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (isHovering(116, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(slopper.tanks[1]), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (isHovering(8, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(slopper.power, TileEntityMachineOreSlopper.maxPower), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getSlopper();
    }
}
