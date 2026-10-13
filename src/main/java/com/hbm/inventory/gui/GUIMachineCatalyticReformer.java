package com.hbm.inventory.gui;

import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineCatalyticReformer;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Screen for the catalytic reformer, 1.7.10 {@code GUIMachineCatalyticReformer}
 * ({@code processing/gui_catalytic_reformer.png}, 176×238). Power bar at x=17; 4 tank bars at x=35/107/125/143
 * (bottom y=70); white title at y=5; hovering the empty catalyst slot shows the converter as a stack hint. */
public class GUIMachineCatalyticReformer extends CompatContainerScreen<ContainerMachineCatalyticReformer> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_catalytic_reformer.png");
    private static final int[] TANK_X = {35, 107, 125, 143};

    public GUIMachineCatalyticReformer(ContainerMachineCatalyticReformer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 238);
    }

    /** 1.7: centred title at y=5 in 0xffffff, inventory at (8, ySize-96+2) in 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 5, 0xffffff, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineCatalyticReformer te = this.menu.getTile();
        if (te == null) return;

        // 1.7 quirk: scaled to 54 against a 52-tall bar (a full bar overdraws two rows upward), kept.
        int j = (int) (te.power * 54 / TileEntityMachineCatalyticReformer.maxPower);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 17, this.topPos + 70 - j, 176.0F, (float) (52 - j), 16, j, 256, 256);

        for (int i = 0; i < 4; i++) GuiUtil.tank(graphics, te.tanks[i], this.leftPos + TANK_X[i], this.topPos + 70, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCatalyticReformer te = this.menu.getTile();
        if (te == null) return;

        for (int i = 0; i < 4; i++) {
            if (GuiUtil.hover(mouseX, mouseY, this.leftPos + TANK_X[i], this.topPos + 18, 16, 52)) {
                GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tanks[i]), mouseX, mouseY);
                return;
            }
        }
        if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 17, this.topPos + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineCatalyticReformer.maxPower), mouseX, mouseY);
            return;
        }

        Slot slot = this.menu.getSlot(10);
        if (this.menu.getCarried().isEmpty() && !slot.hasItem() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
            ItemStack converter = new ItemStack(ModItems.catalytic_converter.get());
            List<Object[]> lines = new ArrayList<>();
            lines.add(new Object[] {converter});
            lines.add(new Object[] {converter.getHoverName().getString()});
            GuiUtil.stackText(graphics, this.font, this.width, this.height, lines, mouseX, mouseY);
        }
    }
}
