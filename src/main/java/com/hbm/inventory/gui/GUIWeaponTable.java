package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerWeaponTable;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The source 176x240 weapon-table screen with a GEM-backed rotatable item preview. */
public final class GUIWeaponTable extends CompatContainerScreen<ContainerWeaponTable> {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of(
            "hbm", "textures/gui/machine/gui_weapon_modifier.png");
    private static final int PREVIEW_X = 8, PREVIEW_Y = 17, PREVIEW_W = 160, PREVIEW_H = 80;
    private static final int CONFIG_X = 26, CONFIG_Y = 111, CONFIG_W = 7, CONFIG_H = 10;
    private double yaw = 20D;
    private double pitch = -10D;
    private boolean dragging;

    public GUIWeaponTable(ContainerWeaponTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 240);
        inventoryLabelY = imageHeight - 96 + 2;
        titleLabelY = 5;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        ItemStack gun = menu.previewStack();
        if (gun.getItem() instanceof ItemGunBaseNT weapon) {
            int config = weapon.isValidConfigIndex(menu.configIndex()) ? menu.configIndex() : 0;
            GuiDraw.blit(graphics, TEXTURE, leftPos + 35, topPos + 112,
                    176F + 6F * config, 0F, 6, 8, 256, 256);
        }
        if (dragging && Minecraft.getInstance().mouseHandler.isLeftPressed()) {
            updateRotation(mouseX, mouseY);
        } else if (dragging) {
            dragging = false;
        }
        if (gun.getItem() instanceof ItemGunBaseNT) submitPreview(graphics, gun);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, (imageWidth - font.width(title)) / 2, 5, 0xFFFFFFFF, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    private void submitPreview(GuiGraphicsExtractor graphics, ItemStack gun) {
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, leftPos + PREVIEW_X + PREVIEW_W / 2F,
                topPos + PREVIEW_Y + PREVIEW_H / 2F);
        GuiDraw.scale(graphics, 5F, 5F);
        GuiDraw.rotate(graphics, (float) Math.toRadians(yaw));
        GuiDraw.item(graphics, gun, -8, -8);
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();
        if (event.button() == 0 && isHovering(PREVIEW_X, PREVIEW_Y + 1, PREVIEW_W, PREVIEW_H - 1, x, y)) {
            dragging = true;
            updateRotation(x, y);
            return true;
        }
        if (event.button() == 0 && isHovering(CONFIG_X, CONFIG_Y, CONFIG_W, CONFIG_H, x, y)) {
            ItemStack gun = menu.previewStack();
            if (gun.getItem() instanceof ItemGunBaseNT weapon && weapon.getConfigCount() > 1
                    && Minecraft.getInstance().gameMode != null) {
                int target = (menu.configIndex() + 1) % weapon.getConfigCount();
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, target);
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean mouseReleased(MouseInput event) {
        if (event.button() == 0) dragging = false;
        return false;
    }

    private void updateRotation(int mouseX, int mouseY) {
        yaw = (leftPos + PREVIEW_X + PREVIEW_W / 2D - mouseX) / (PREVIEW_W / 2D) * -180D;
        pitch = (topPos + PREVIEW_Y + 1 + (PREVIEW_H - 1) / 2D - mouseY)
                / ((PREVIEW_H - 1) / 2D) * 90D;
    }
}
