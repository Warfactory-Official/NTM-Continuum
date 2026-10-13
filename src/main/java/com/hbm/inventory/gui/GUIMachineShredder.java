package com.hbm.inventory.gui;

import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineShredder;
import com.hbm.tileentity.machine.TileEntityMachineShredder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the shredder, a straight port of 1.7 {@code GUIMachineShredder}
 * ({@code processing/gui_shredder.png}, 176x233). Reads {@code power}/{@code progress} and the two synced
 * blade-wear values off the client {@link TileEntityMachineShredder} (BufPacket).
 */
public class GUIMachineShredder extends CompatContainerScreen<ContainerMachineShredder> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_shredder.png");

    public GUIMachineShredder(ContainerMachineShredder menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 233);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineShredder shredder = this.menu.getShredder();
        if (shredder == null) return;

        if (shredder.power > 0) {
            int i = (int) shredder.getPowerScaled(88);
            if (i > 0) {
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + 8, this.topPos + 106 - i,
                        176.0F, (float) (160 - i), 16, i, 256, 256);
            }
        }

        int progress = shredder.getProgressScaled(34);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 63, this.topPos + 89,
                176.0F, 54.0F, progress + 1, 18, 256, 256);

        drawGear(graphics, shredder.getGearLeft(), this.leftPos + 43, 176.0F);
        drawGear(graphics, shredder.getGearRight(), this.leftPos + 79, 194.0F);

        // 1.7: large red "!" hung off the left edge while a blade is missing or spent.
        if (shredder.bladesBroken()) {
            GuiUtil.drawInfoPanel(graphics, this.leftPos - 16, this.topPos + 36, 6);
        }
    }

    /** Wear icon: state 1/2/3 the fine / worn / broken sprite row. State 0 (no blade) draws nothing. */
    private void drawGear(GuiGraphicsExtractor graphics, int state, int x, float u) {
        if (state < 1 || state > 3) return;
        float v = (state - 1) * 18.0F;
        GuiDraw.blit(graphics, TEXTURE, x, this.topPos + 71, u, v, 18, 18, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineShredder shredder = this.menu.getShredder();
        if (shredder == null) return;

        if (this.isHovering(8, 106 - 88, 16, 88, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font,
                    GuiUtil.energyTooltip(shredder.power, TileEntityMachineShredder.maxPower),
                    Optional.empty(), mouseX, mouseY);
        } else if (shredder.bladesBroken() && this.isHovering(-16, 36, 16, 16, mouseX, mouseY)) {
            // 1.7 hard-codes "Error: Shredder blades are broken or missing!".
            GuiDraw.tooltip(graphics, this.font,
                    List.of(Component.literal("Error: ").append(Component.translatable("gui.shredder.bladesBroken"))),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 centres the title on x=106, not xSize/2.
        GuiDraw.text(graphics, this.font, this.title, 106 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
