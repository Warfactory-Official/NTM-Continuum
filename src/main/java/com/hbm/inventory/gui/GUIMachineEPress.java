package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineEPress;
import com.hbm.tileentity.machine.TileEntityMachineEPress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * Client screen for the electric press, a straight port of 1.7 {@code GUIMachineEPress}
 * ({@code processing/gui_electric_press.png}, 176x186). Reads {@code power}/{@code progress} off the synced
 * client {@link TileEntityMachineEPress} (BufPacket, not ContainerData).
 */
public class GUIMachineEPress extends CompatContainerScreen<ContainerMachineEPress> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_electric_press.png");

    public GUIMachineEPress(ContainerMachineEPress menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineEPress press = this.menu.getPress();
        if (press == null) return;

        int i = (int) press.getPowerScaled(34);
        if (i > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 52 - i, 176.0F, (float) (34 - i), 16, i, 256, 256);
        }
        int k = press.getProgressScaled(16);
        if (k > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 18, this.topPos + 33, 192.0F, 0.0F, 18, k, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 centres the title on x=89, not xSize/2.
        GuiDraw.text(graphics, this.font, this.title, 89 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineEPress press = this.menu.getPress();
        if (press == null) return;
        if (this.isHovering(152, 52 - 34, 16, 34, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font,
                    GuiUtil.energyTooltip(press.power, TileEntityMachineEPress.maxPower),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getPress();
    }
}
