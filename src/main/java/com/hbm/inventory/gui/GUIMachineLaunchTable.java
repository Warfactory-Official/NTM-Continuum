package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerLaunchTable;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.tileentity.bomb.TileEntityCustomMissileLauncher;
import com.hbm.tileentity.bomb.TileEntityLaunchTable;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * 1.7 {@code GUIMachineLaunchTable}: {@code gui_launch_table.png}, plus the three 18x18 pad-size buttons at
 * (7,98), (25,98) and (43,98). A click sends the size's {@code PartSize} ordinal (1.7 AuxButtonPacket); the
 * selected button is drawn pressed.
 */
public class GUIMachineLaunchTable extends GUICustomMissileLauncher<ContainerLaunchTable> {

    public GUIMachineLaunchTable(ContainerLaunchTable menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Ids.of("hbm", "textures/gui/weapon/gui_launch_table.png"));
    }

    @Override
    protected void extractExtras(GuiGraphicsExtractor graphics, TileEntityCustomMissileLauncher launcher) {
        if (!(launcher instanceof TileEntityLaunchTable table)) return;
        switch (table.padSize) {
            case SIZE_10 -> GuiDraw.blit(graphics, texture(), leftPos + 7, topPos + 98, 176F, 8F, 18, 18, 256, 256);
            case SIZE_15 -> GuiDraw.blit(graphics, texture(), leftPos + 25, topPos + 98, 194F, 8F, 18, 18, 256, 256);
            case SIZE_20 -> GuiDraw.blit(graphics, texture(), leftPos + 43, topPos + 98, 212F, 8F, 18, 18, 256, 256);
            default -> { }
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            PartSize[] sizes = {PartSize.SIZE_10, PartSize.SIZE_15, PartSize.SIZE_20};
            for (int i = 0; i < 3; i++) {
                // 1.7: x in [left, left+18), y in (top, top+18]
                double x = event.x() - leftPos, y = event.y() - topPos;
                if (x >= 7 + 18 * i && x < 25 + 18 * i && y > 98 && y <= 116) {
                    GuiUtil.playClickSound();
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, sizes[i].ordinal());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected List<Component> sizeHint() {
        return List.of(Component.literal("Accepts custom missiles"), Component.literal("of all sizes, as long as the"),
                Component.literal("correct size setting is selected."));
    }

    @Override
    protected void extraTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String[] labels = {"Size 10 & 10/15", "Size 15 & 15/20", "Size 20"};
        for (int i = 0; i < 3; i++) {
            if (isHovering(7 + 18 * i, 98, 18, 18, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, font, List.of(Component.literal(labels[i])), Optional.empty(), mouseX, mouseY);
            }
        }
    }
}
