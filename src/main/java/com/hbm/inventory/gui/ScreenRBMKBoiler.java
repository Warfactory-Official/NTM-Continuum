package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuRBMKBoiler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class ScreenRBMKBoiler extends RBMKContainerScreen<MenuRBMKBoiler> {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/rbmk/gui_rbmk_boiler.png");

    public ScreenRBMKBoiler(MenuRBMKBoiler menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 186); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        texture(graphics, TEX);
        BlockEntityRBMKBoiler boiler = menu.blockEntity();
        if (boiler == null) return;
        int water = boiler.feed.getMaxFill() > 0 ? boiler.feed.getFill() * 58 / boiler.feed.getMaxFill() : 0;
        if (water > 0) GuiDraw.blit(graphics, TEX, leftPos + 126, topPos + 82 - water, 176, 58 - water, 14, water, 256, 256);
        int steam = boiler.steam.getMaxFill() > 0 ? boiler.steam.getFill() * 22 / boiler.steam.getMaxFill() : 0;
        if (steam > 0) steam++;
        if (steam > 22) steam++;
        GuiDraw.blit(graphics, TEX, leftPos + 91, topPos + 65 - steam, 190, 24 - steam, 4, steam, 256, 256);
        GuiDraw.blit(graphics, TEX, leftPos + 36, topPos + 24, steamTypeU(boiler.steam.getTankType()), 0, 14, 58, 256, 256);
    }

    @Override protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityRBMKBoiler boiler = menu.blockEntity();
        if (boiler == null) return;
        fluidTooltip(graphics, boiler.feed, 126, 24, 16, 58, mouseX, mouseY);
        fluidTooltip(graphics, boiler.steam, 89, 39, 8, 28, mouseX, mouseY);
        if (region(33, 21, 20, 64, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Compress steam (cycle tier)"),
                    Component.literal("Column heat: " + (int) boiler.heat + " °C")), mouseX, mouseY);
        }
    }

    @Override protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        if (event.button() != 0 || !region(33, 21, 20, 64, (int) event.x(), (int) event.y())) return false;
        BlockEntityRBMKBoiler boiler = menu.blockEntity();
        if (boiler == null) return false;
        CompoundTag data = new CompoundTag();
        data.putBoolean("compression", true);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(boiler.getBlockPos(), data));
        return true;
    }

    private static int steamTypeU(FluidType type) {
        if (type == Fluids.HOTSTEAM) return 208;
        if (type == Fluids.SUPERHOTSTEAM) return 222;
        if (type == Fluids.ULTRAHOTSTEAM) return 236;
        return 194;
    }
}
