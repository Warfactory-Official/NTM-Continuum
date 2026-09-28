package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerRBMKAutoloader;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 {@code GUIRBMKAutoloader}: the two rod grids, the -/+ cycle buttons and the green percentage. */
public final class ScreenRBMKAutoloader extends RBMKContainerScreen<ContainerRBMKAutoloader> {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/machine/gui_autoloader.png");

    public ScreenRBMKAutoloader(ContainerRBMKAutoloader menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 182); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { texture(graphics, TEX); }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFFFFFFFF, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
        BlockEntityRBMKAutoloader loader = menu.loader();
        if (loader == null) return;
        String percent = loader.cycle + "%";
        GuiDraw.text(graphics, font, Component.literal(percent), imageWidth / 2 - font.width(percent) / 2, 23, 0xFF00FF00, false);
    }

    @Override protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        BlockEntityRBMKAutoloader loader = menu.loader();
        if (loader == null || event.button() != 0) return false;
        double x = event.x(), y = event.y();
        String key = null;
        if (leftPos + 74 <= x && leftPos + 74 + 12 > x && topPos + 36 < y && topPos + 36 + 12 >= y) key = "minus";
        if (leftPos + 90 <= x && leftPos + 90 + 12 > x && topPos + 36 < y && topPos + 36 + 12 >= y) key = "plus";
        if (key == null) return false;
        GuiUtil.playClickSound();
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, true);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(loader.getBlockPos(), data));
        return true;
    }
}
