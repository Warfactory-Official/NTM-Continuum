package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFusionBreeder;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
import com.hbm.util.BobMathUtil;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Fusion breeder control panel, ported from 1.7 {@code GUIFusionBreeder} (176 x 200). */
public class GUIFusionBreeder extends CompatContainerScreen<ContainerFusionBreeder> {

    private static final Identifier TEXTURE =
            Ids.of("hbm", "textures/gui/reactors/gui_fusion_breeder.png");

    public GUIFusionBreeder(ContainerFusionBreeder menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 200);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityFusionBreeder breeder = menu.getBreeder();
        if (breeder == null) return;

        int progress = Math.min(42, (int) Math.ceil(breeder.progress * 42D / TileEntityFusionBreeder.CAPACITY));
        if (progress > 0) GuiDraw.blit(graphics, TEXTURE, x + 67, y + 48, 176F, 0F, progress, 10, 256, 256);

        double gauge = 1D - Math.exp(-breeder.neutronEnergy * 10D / TileEntityFusionBreeder.CAPACITY);
        // input flux
        GuiUtil.drawSmoothGauge(graphics, x + 88, y + 32, gauge, 5, 2, 1, 0xA00000);

        NuclearMiscGuiUtil.drawTank(graphics, breeder.tanks[0], x + 26, y + 18, 16, 52);
        NuclearMiscGuiUtil.drawTank(graphics, breeder.tanks[1], x + 134, y + 18, 16, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 35, imageHeight - 93, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFusionBreeder breeder = menu.getBreeder();
        if (breeder == null) return;

        if (isHovering(79, 23, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal((int) Math.ceil(breeder.neutronEnergy) + " flux/t")
                            .withStyle(ChatFormatting.RESET))), mouseX, mouseY);
        } else if (isHovering(67, 46, 42, 14, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal(BobMathUtil.format((int) Math.ceil(breeder.progress)) + " / "
                    + BobMathUtil.format((int) Math.ceil(TileEntityFusionBreeder.CAPACITY)) + " flux")), mouseX, mouseY);
        } else if (isHovering(26, 18, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(breeder.tanks[0]), mouseX, mouseY);
        } else if (isHovering(134, 18, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(breeder.tanks[1]), mouseX, mouseY);
        }
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, text, Optional.empty(), mouseX, mouseY);
    }
}
