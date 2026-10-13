package com.hbm.inventory.gui;

import com.hbm.module.ModulePatternMatcher;
import com.hbm.inventory.container.ContainerPneumoTube;
import com.hbm.uninos.networkproviders.PneumaticNetwork;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Optional;

/** 1.7 {@code GUIPneumoTube}: the compressor's full GUI, or the endpoint's filter-only one. */
public class GUIPneumoTube extends PneumaticScreenBase<ContainerPneumoTube> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_pneumatic_pipe.png");
    private static final Identifier TEXTURE_ENDPOINT = Ids.of("hbm", "textures/gui/storage/gui_pneumatic_endpoint.png");

    private final boolean endpointOnly;

    public GUIPneumoTube(ContainerPneumoTube menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
        this.endpointOnly = menu.endpointOnly();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Identifier tex = endpointOnly ? TEXTURE_ENDPOINT : TEXTURE;
        GuiDraw.blit(graphics, tex, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        TileEntityPneumoTube tube = menu.tube();
        if (tube == null) return;

        if (tube.whitelist) {
            GuiDraw.blit(graphics, tex, leftPos + 139, topPos + 33, 176, 0, 3, 6, 256, 256);
        } else {
            GuiDraw.blit(graphics, tex, leftPos + 139, topPos + 47, 176, 0, 3, 6, 256, 256);
        }

        if (!endpointOnly) {
            if (tube.redstone) GuiDraw.blit(graphics, TEXTURE, leftPos + 7, topPos + 52, 179, 0, 18, 18, 256, 256);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 151, topPos + 16, 197, 18 * tube.receiveOrder, 18, 18, 256, 256);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 151, topPos + 52, 215, 18 * tube.sendOrder, 18, 18, 256, 256);

            GuiDraw.blit(graphics, TEXTURE, leftPos + 6 + 4 * (tube.compair.getPressure() - 1), topPos + 36, 179, 18, 4, 8, 256, 256);
            drawSmoothGauge(graphics, leftPos + 16, topPos + 25,
                    (double) tube.compair.getFill() / (double) tube.compair.getMaxFill(), 5, 2, 1, 0xCA6C43, 0xAB4223);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 5, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityPneumoTube tube = menu.tube();
        if (tube == null) return;

        if (!endpointOnly) {
            drawCustomInfoStat(graphics, mouseX, mouseY, 7, 16, 18, 18, GuiUtil.tankTooltip(tube.compair));
            drawCustomInfoStat(graphics, mouseX, mouseY, 7, 52, 18, 18,
                    (tube.redstone ? (ChatFormatting.GREEN + "ON ") : (ChatFormatting.RED + "OFF ")) + ChatFormatting.RESET + "with Redstone");
            drawCustomInfoStat(graphics, mouseX, mouseY, 6, 36, 20, 8, "Compressor: " + tube.compair.getPressure() + " PU",
                    "Max range: " + TileEntityPneumoTube.getRangeFromPressure(tube.compair.getPressure()) + "m");
            drawCustomInfoStat(graphics, mouseX, mouseY, 151, 16, 18, 18, ChatFormatting.YELLOW + "Receiver order:",
                    tube.receiveOrder == PneumaticNetwork.RECEIVE_ROBIN ? "Round robin" : "Random");
            drawCustomInfoStat(graphics, mouseX, mouseY, 151, 52, 18, 18, ChatFormatting.YELLOW + "Provider slot order:",
                    tube.sendOrder == PneumaticNetwork.SEND_FIRST ? "First to last"
                            : tube.sendOrder == PneumaticNetwork.SEND_LAST ? "Last to first" : "Random");
        }

        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < TileEntityPneumoTube.SLOT_COUNT; ++i) {
                Slot slot = menu.slots.get(i);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && tube.pattern.modes[i] != null) {
                    GuiDraw.tooltip(graphics, font, List.of(
                            Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                            Component.literal(ModulePatternMatcher.getLabel(tube.pattern.modes[i]))),
                            Optional.empty(), mouseX, mouseY - 30);
                    return;
                }
            }
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityPneumoTube tube = menu.tube();
        if (tube == null) return false;
        double x = event.x(), y = event.y();
        boolean handled = false;

        if (!endpointOnly) {
            handled |= clickSendFlag(tube.getBlockPos(), x, y, 7, 52, 18, 18, "redstone");
            handled |= clickSendFlag(tube.getBlockPos(), x, y, 6, 36, 20, 8, "pressure");
            handled |= clickSendFlag(tube.getBlockPos(), x, y, 151, 16, 18, 18, "receive");
            handled |= clickSendFlag(tube.getBlockPos(), x, y, 151, 52, 18, 18, "send");
        }

        handled |= clickSendFlag(tube.getBlockPos(), x, y, 128, 30, 14, 26, "whitelist");
        return handled;
    }
}
