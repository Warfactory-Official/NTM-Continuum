package com.hbm.inventory.gui;

import com.hbm.blocks.machine.MachineStrandCaster;
import com.hbm.inventory.container.ContainerMachineStrandCaster;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.Mats;
import com.hbm.tileentity.machine.TileEntityMachineStrandCaster;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** The old 176x214 Strand Caster screen, including the melt, water, and spent-steam columns. */
public class GUIMachineStrandCaster extends CompatContainerScreen<ContainerMachineStrandCaster> {

    private static final Identifier TEXTURE = Ids.of("hbm",
            "textures/gui/processing/gui_strand_caster.png");

    public GUIMachineStrandCaster(ContainerMachineStrandCaster menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 214);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineStrandCaster caster = menu.getCaster();
        if (caster == null) return;

        if (caster.amount > 0 && caster.type != null) {
            int height = Math.min(caster.amount * 79 / Math.max(caster.getCapacity(), 1), 92);
            int color = 0xFF000000 | (caster.type.moltenColor & 0xFFFFFF);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 17, topPos + 93 - height,
                    176F, 89F - height, 34, height, 256, 256, color);
        }
        drawTank(graphics, caster.water, 82, 38);
        drawTank(graphics, caster.steam, 82, 89);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank,
                          int x, int bottom) {
        int height = tank.getMaxFill() <= 0 ? 0 : tank.getFill() * 24 / tank.getMaxFill();
        if (height <= 0) return;
        int color = 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF);
        graphics.fill(leftPos + x, topPos + bottom - height, leftPos + x + 16, topPos + bottom, color);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineStrandCaster caster = menu.getCaster();
        if (caster == null) return;
        if (isHovering(16, 17, 36, 81, mouseX, mouseY)) {
            String content = caster.type == null ? "Empty" : caster.type.getDisplayName() + ": "
                    + Mats.formatAmount(caster.amount, false);
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(content)), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(82, 14, 16, 24, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(caster.water), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(82, 65, 16, 24, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(caster.steam), Optional.empty(), mouseX, mouseY);
        }
    }
}
