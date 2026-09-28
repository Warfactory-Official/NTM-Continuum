package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerICF;
import com.hbm.tileentity.machine.TileEntityICF;
import com.hbm.util.BobMathUtil;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * The 248x222 ICF screen (1.7 {@code GUIICF}): the laser bar on the left, the coolant tank, the pellet slots, the
 * hot coolant and stellar flux tanks on the right and the heat needle between them. The background is blitted in
 * two parts, the machine half and the inventory half, exactly as 1.7 did.
 */
public class GUIICF extends CompatContainerScreen<ContainerICF> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_icf.png");

    public GUIICF(ContainerICF menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 248, 222);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, 114, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 36, topPos + 122, 36F, 122F, 176, 108, 256, 256);

        TileEntityICF icf = menu.getIcf();
        if (icf == null) return;

        if (icf.maxLaser > 0) {
            int p = (int) (icf.laser * 70 / icf.maxLaser);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 8, topPos + 88 - p, 212F, 192F - p, 16, p, 256, 256);
        }

        GuiUtil.drawSmoothGauge(graphics, leftPos + 196, topPos + 98, (double) icf.heat / (double) TileEntityICF.maxHeat,
                5, 2, 1, 0xFF00AF);

        NuclearMiscGuiUtil.drawTank(graphics, icf.tanks[0], leftPos + 44, topPos + 18, 16, 70);
        NuclearMiscGuiUtil.drawTank(graphics, icf.tanks[1], leftPos + 188, topPos + 18, 16, 70);
        NuclearMiscGuiUtil.drawTank(graphics, icf.tanks[2], leftPos + 224, topPos + 18, 16, 70);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String name = title.getString();
        GuiDraw.text(graphics, font, name, imageWidth / 2 - font.width(name) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 44, imageHeight - 93, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityICF icf = menu.getIcf();
        if (icf == null) return;

        if (hover(44, 18, 16, 70, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(icf.tanks[0]), mouseX, mouseY);
        } else if (hover(188, 18, 16, 70, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(icf.tanks[1]), mouseX, mouseY);
        } else if (hover(224, 18, 16, 70, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(icf.tanks[2]), mouseX, mouseY);
        } else if (hover(8, 18, 16, 70, mouseX, mouseY)) {
            String text = icf.maxLaser <= 0 ? "OFFLINE"
                    : (BobMathUtil.getShortNumber(icf.laser) + "TU/t - " + (icf.laser * 1000 / icf.maxLaser) / 10D + "%");
            tooltip(graphics, List.of(Component.literal(text)), mouseX, mouseY);
        } else if (hover(187, 89, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal(BobMathUtil.getShortNumber(icf.heat) + " / "
                    + BobMathUtil.getShortNumber(TileEntityICF.maxHeat) + "TU")), mouseX, mouseY);
        }
    }

    /** 1.7 {@code renderTankInfo}/{@code drawCustomInfoStat}: open lower bound, closed upper bound on Y. */
    private boolean hover(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY > topPos + y && mouseY <= topPos + y + height;
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
    }
}
