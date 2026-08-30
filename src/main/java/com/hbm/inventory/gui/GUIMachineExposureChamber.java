package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineExposureChamber;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;
import dev.golem.core.gui.GuiDraw;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Exposure Chamber screen, preserving the legacy power, particle and progress indicators. */
public class GUIMachineExposureChamber extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineExposureChamber>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm",
            "textures/gui/processing/gui_exposure_chamber.png");

    public GUIMachineExposureChamber(ContainerMachineExposureChamber menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineExposureChamber chamber = menu.getChamber();
        if (chamber == null) return;

        int progress = chamber.getProgressScaled(42);
        if (progress > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 36, topPos + 39,
                    192F, 0F, progress, 10, 256, 256);
        }

        int particles = chamber.getParticlesScaled(16);
        if (particles > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 26, topPos + 52 - particles,
                    192F, 26F - particles, 9, particles, 256, 256);
        }

        int energy = chamber.getMaxPower() <= 0 ? 0
                : (int) (chamber.power * 34L / chamber.getMaxPower());
        if (energy > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 152, topPos + 52 - energy,
                    176F, 34F - energy, 16, energy, 256, 256);
        }

        if (chamber.consumption <= chamber.power) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 156, topPos + 4,
                    176F, 34F, 9, 12, 256, 256);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineExposureChamber chamber = menu.getChamber();
        if (chamber == null) return;

        if (isHovering(152, 18, 16, 34, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(chamber.power, chamber.getMaxPower()),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(26, 36, 9, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font,
                    List.of(Component.literal(chamber.savedParticles + " / "
                            + TileEntityMachineExposureChamber.MAX_PARTICLES)),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 70 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 0x404040, false);
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getChamber();
    }
}
