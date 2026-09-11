package com.hbm.inventory.gui;

import com.hbm.blocks.machine.pwr.ModPWRBlocks;
import com.hbm.inventory.container.ContainerPWR;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.pwr.TileEntityPWRController;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * PWR control panel. The two dials are the core and hull heat pools; the bar under the fuel slot is how
 * far the loaded pellet has burned, and the green strip below it is where the control rods sit. The field
 * beside it is a withdrawal percentage, the inverse of the block entity's {@code rodTarget}, which counts
 * insertion — so a fresh reactor reads zero and a player raises the number to open it up.
 */
public class GUIPWR extends CompatContainerScreen<ContainerPWR> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_pwr.png");

    private EditBox field;

    public GUIPWR(ContainerPWR menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 188);
    }

    @Override
    protected void init() {
        super.init();
        String previous = this.field == null ? null : this.field.getValue();
        this.field = new EditBox(this.font, leftPos + 57, topPos + 63, 30, 8, Component.empty());
        this.field.setTextColor(0xFF00FF00);
        this.field.setBordered(false);
        this.field.setMaxLength(3);
        if (previous != null) {
            this.field.setValue(previous);
        } else {
            TileEntityPWRController controller = menu.getController();
            this.field.setValue(controller == null ? "0" : String.valueOf((int) (100 - controller.rodTarget)));
        }
        addRenderableWidget(this.field);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityPWRController controller = menu.getController();
        if (controller == null) return;

        if (controller.hullHeat > TileEntityPWRController.hullHeatCapacityBase * 0.8
                || controller.coreHeat > controller.coreHeatCapacity * 0.8) {
            GuiDraw.blit(graphics, TEXTURE, x + 147, y, 176F, 14F, 26, 26, 256, 256);
        }

        if (controller.processTime > 0) {
            int progress = (int) (controller.progress * 33 / controller.processTime);
            if (progress > 0) GuiDraw.blit(graphics, TEXTURE, x + 54, y + 33, 176F, 0F, progress, 14, 256, 256);
        }

        int rods = (int) (controller.rodLevel * 52 / 100);
        if (rods > 0) GuiDraw.blit(graphics, TEXTURE, x + 53, y + 54, 176F, 40F, rods, 2, 256, 256);

        GuiUtil.drawSmoothGauge(graphics, x + 124, y + 40,
                (double) controller.coreHeat / (double) controller.coreHeatCapacity, 5, 2, 1, 0x7F0000);
        GuiUtil.drawSmoothGauge(graphics, x + 160, y + 40,
                (double) controller.hullHeat / (double) TileEntityPWRController.hullHeatCapacityBase, 5, 2, 1, 0x7F0000);

        ItemStack loaded = loadedFuel(controller);
        if (!loaded.isEmpty()) {
            GuiDraw.item(graphics, loaded, x + 89, y + 5);
            Component count = Component.literal(controller.amountLoaded + "/" + controller.rodCount)
                    .withStyle(ChatFormatting.YELLOW);
            GuiDraw.text(graphics, font, count, x + 106 - font.width(count), y + 14, 0xFFFFFFFF, true);
        }

        drawTank(graphics, controller.tanks[0], 8, 5, 16, 52);
        drawTank(graphics, controller.tanks[1], 26, 5, 16, 52);
    }

    /** Repeats the 16x16 fluid texture, matching the old GL_REPEAT tank quad. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        int max = tank.getMaxFill();
        int filled = max <= 0 ? 0 : (int) ((long) tank.getFill() * height / max);
        if (filled <= 0) return;

        int x0 = leftPos + x;
        int bottom = topPos + y + height;
        int top = bottom - filled;
        int tint = 0xFF000000 | (tank.getTankType().getTint() & 0xFFFFFF);
        Identifier fluidTexture = tank.getTankType().getTexture();

        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + width, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, fluidTexture, x0, tileY, 0F, 0F, width, 16, 16, 16, tint);
            }
            graphics.disableScissor();
        } else {
            GuiDraw.fill(graphics, x0, top, x0 + width, bottom,
                    0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);

        TileEntityPWRController controller = menu.getController();
        if (controller == null) return;

        Component flux = Component.literal(String.format(Locale.US, "%,.1f", controller.flux));
        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, 1F / 1.25F, 1F / 1.25F);
        GuiDraw.text(graphics, font, flux, (int) (165 * 1.25 - font.width(flux)), (int) (64 * 1.25), 0xFF00FF00, false);
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityPWRController controller = menu.getController();
        if (controller == null) return;

        if (isHovering(8, 5, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(controller.tanks[0]), mouseX, mouseY);
        } else if (isHovering(26, 5, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(controller.tanks[1]), mouseX, mouseY);
        } else if (isHovering(115, 31, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Core: " + comma(controller.coreHeat)
                    + " / " + comma(controller.coreHeatCapacity) + " TU")), mouseX, mouseY);
        } else if (isHovering(151, 31, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Hull: " + comma(controller.hullHeat)
                    + " / " + comma(TileEntityPWRController.hullHeatCapacityBase) + " TU")), mouseX, mouseY);
        } else if (isHovering(52, 31, 36, 18, mouseX, mouseY)) {
            int percent = controller.processTime <= 0 ? 0 : (int) (controller.progress * 100 / controller.processTime);
            tooltip(graphics, List.of(Component.literal(percent + "%")), mouseX, mouseY);
        } else if (isHovering(52, 53, 54, 4, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal(
                    "Control rod level: " + (100 - Math.round(controller.rodLevel)) + "%")), mouseX, mouseY);
        } else if (isHovering(88, 4, 18, 18, mouseX, mouseY)) {
            ItemStack loaded = loadedFuel(controller);
            if (!loaded.isEmpty()) {
                tooltip(graphics, net.minecraft.client.gui.screens.Screen.getTooltipFromItem(
                        Minecraft.getInstance(), loaded), mouseX, mouseY);
            }
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityPWRController controller = menu.getController();
        if (controller == null || field == null) return false;

        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;
        if (x < 88 || x >= 106 || y < 58 || y >= 76) return false;

        int level;
        try {
            level = Mth.clamp(Integer.parseInt(field.getValue().trim()), 0, 100);
        } catch (NumberFormatException e) {
            return false;
        }
        field.setValue(String.valueOf(level));

        CompoundTag data = new CompoundTag();
        data.putInt("control", 100 - level);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                new ControlEventPayload(controller.getBlockPos(), data));
        GuiUtil.playClickSound();
        return true;
    }

    @Override
    protected boolean keyPressed(dev.golem.core.gui.KeyInput event) {
        if (this.field != null && this.field.isFocused() && event.key() != 256 && event.key() != 258) {
            event.keyPressed(this.field);
            return true;
        }
        return false;
    }

    private static ItemStack loadedFuel(TileEntityPWRController controller) {
        if (controller.typeLoaded == -1 || controller.amountLoaded <= 0) return ItemStack.EMPTY;
        return ModPWRBlocks.pwr_fuel.get().of(
                com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel.VALUES[Math.floorMod(controller.typeLoaded,
                        com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel.VALUES.length)]);
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, text, Optional.empty(), mouseX, mouseY);
    }

    private static String comma(long value) {
        return String.format(Locale.US, "%,d", value);
    }
}
