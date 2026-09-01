package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineArcFurnaceLarge;
import com.hbm.inventory.material.Mats;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The old 176x256 Large Arc Furnace screen, with solid/liquid toggle and stacked molten output. */
public class GUIMachineArcFurnaceLarge extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineArcFurnaceLarge>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm",
            "textures/gui/processing/gui_arc_furnace.png");

    public GUIMachineArcFurnaceLarge(ContainerMachineArcFurnaceLarge menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineArcFurnaceLarge furnace = menu.getFurnace();
        if (furnace == null) return;

        if (furnace.liquidMode) GuiDraw.blit(graphics, TEXTURE, leftPos + 151, topPos + 17,
                190F, 18F, 18, 18, 256, 256);
        if (furnace.isProgressing) GuiDraw.blit(graphics, TEXTURE, leftPos + 7, topPos + 17,
                190F, 0F, 18, 18, 256, 256);

        int power = (int) (furnace.power * 70L / TileEntityMachineArcFurnaceLarge.MAX_POWER);
        if (power > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 8, topPos + 106 - power,
                176F, 70F - power, 7, power, 256, 256);
        int progress = (int) (furnace.progress * 70F);
        if (progress > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 17, topPos + 106 - progress,
                183F, 70F - progress, 7, progress, 256, 256);
        drawStack(graphics, furnace.liquids, TileEntityMachineArcFurnaceLarge.MAX_LIQUID, 152, 106);
    }

    private void drawStack(GuiGraphicsExtractor graphics, List<Mats.MaterialStack> stack, int capacity,
                           int x, int y) {
        int lastHeight = 0;
        int lastAmount = 0;
        for (Mats.MaterialStack material : stack) {
            int height = (lastAmount + material.amount) * 70 / Math.max(capacity, 1);
            if (height != lastHeight) {
                int color = 0xFF000000 | (material.material.moltenColor & 0xFFFFFF);
                GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + y - height,
                        208F, 70F - height, 16, height - lastHeight, 256, 256, color);
            }
            lastAmount += material.amount;
            lastHeight = height;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineArcFurnaceLarge furnace = menu.getFurnace();
        Component label = furnace == null || furnace.hasCustomName() ? title : furnace.getDisplayName();
        GuiDraw.text(graphics, font, label, imageWidth / 2 - font.width(label) / 2, 6, 0xFFFFFF, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineArcFurnaceLarge furnace = menu.getFurnace();
        if (furnace == null) return;
        if (isHovering(8, 36, 7, 70, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(furnace.power,
                    TileEntityMachineArcFurnaceLarge.MAX_POWER), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(152, 36, 16, 70, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            if (furnace.liquids.isEmpty()) lines.add(Component.literal("Empty"));
            for (Mats.MaterialStack material : furnace.liquids) lines.add(Component.literal(
                    material.material.getDisplayName() + ": " + Mats.formatAmount(material.amount, false)));
            GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
        } else if (isHovering(151, 17, 18, 18, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(furnace.liquidMode
                    ? "Liquid mode" : "Solid mode")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachineArcFurnaceLarge furnace = menu.getFurnace();
        if (furnace != null && isHovering(151, 17, 18, 18, event.x(), event.y())) {
            GuiUtil.playClickSound();
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            data.putBoolean("liquid", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                    new ControlEventPayload(furnace.getBlockPos(), data));
            return true;
        }
        return false;
    }

    @Override public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getFurnace();
    }
}
