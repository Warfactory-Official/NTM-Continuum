package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineArcFurnaceLarge;
import com.hbm.inventory.material.Mats;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import dev.golem.core.Ids;
import dev.golem.core.client.KeyMappings;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;

/**
 * 1.7.10 {@code GUIMachineArcFurnaceLarge} ({@code processing/gui_arc_furnace.png}, 176x256), with solid/liquid
 * toggle and stacked molten output. Not reproduced: 1.7's second additive (SRC_ALPHA, ONE) 30%-white pass over
 * each molten layer, which needs a tinted additive blit Core does not offer yet.
 */
public class GUIMachineArcFurnaceLarge extends CompatContainerScreen<ContainerMachineArcFurnaceLarge>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm",
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
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    /** 1.7 {@code drawScreen}: liquid stack info over (152,36) 16x70, then the energy info over (8,36) 7x70. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineArcFurnaceLarge furnace = menu.getFurnace();
        if (furnace == null) return;
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 152, topPos + 36, 16, 70)) {
            boolean shift = KeyMappings.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT);
            List<Component> lines = new ArrayList<>();
            if (furnace.liquids.isEmpty()) lines.add(Component.literal(ChatFormatting.RED + "Empty"));
            for (Mats.MaterialStack material : furnace.liquids) lines.add(Component.literal(ChatFormatting.YELLOW
                    + material.material.getDisplayName() + ": " + Mats.formatAmount(material.amount, shift)));
            GuiUtil.tooltip(graphics, font, lines, mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 8, topPos + 36, 7, 70)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(furnace.power, TileEntityMachineArcFurnaceLarge.MAX_POWER), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityMachineArcFurnaceLarge furnace = menu.getFurnace();
        // 1.7 checkClick(x, y, 151, 17, 18, 18): x [151,169), y (17,35].
        if (furnace != null && GuiUtil.hover(event.x(), event.y(), leftPos + 151, topPos + 17, 18, 18)) {
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("liquid", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                    new ControlEventPayload(furnace.getBlockPos(), data));
            return true;
        }
        return false;
    }

    @Override public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getFurnace();
    }
}
