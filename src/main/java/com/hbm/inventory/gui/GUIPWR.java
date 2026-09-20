package com.hbm.inventory.gui;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.ControlEventPayload;
import com.hbm.inventory.container.ContainerPWR;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.ModPwrItems;
import com.hbm.tileentity.machine.TileEntityPWRController;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
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
 * The 176x188 PWR controller screen (1.7 {@code GUIPWR}): the two coolant tanks, the loaded-fuel display
 * with its {@code loaded/rods} label, the depletion bar, the control-rod bar with its percentage field and
 * apply button, the flux readout, the two heat gauges, and the overheating warning overlay.
 */
public class GUIPWR extends CompatContainerScreen<ContainerPWR> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_pwr.png");

    private EditBox field;

    public GUIPWR(ContainerPWR menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 188);
    }

    @Override
    protected void init() {
        super.init();

        String previous = field == null ? null : field.getValue();
        TileEntityPWRController controller = menu.getController();
        field = new EditBox(this.font, leftPos + 57, topPos + 63, 30, 8, Component.empty());
        field.setTextColor(0xFF00FF00);
        field.setBordered(false);
        field.setMaxLength(3);
        if (previous != null) {
            field.setValue(previous);
        } else {
            field.setValue(controller == null ? "" : ((int) (100 - controller.rodTarget)) + "");
        }
        addRenderableWidget(field);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityPWRController controller = menu.getController();
        if (controller == null) return;

        if (controller.hullHeat > TileEntityPWRController.hullHeatCapacityBase * 0.8
                || controller.coreHeat > controller.coreHeatCapacity * 0.8) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 147, topPos, 176F, 14F, 26, 26, 256, 256);
        }

        int p = controller.processTime <= 0 ? 0 : (int) (controller.progress * 33 / controller.processTime);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 54, topPos + 33, 176F, 0F, p, 14, 256, 256);

        int c = (int) (controller.rodLevel * 52 / 100);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 54, 176F, 40F, c, 2, 256, 256);

        drawSmoothGauge(graphics, leftPos + 124, topPos + 40, (double) controller.coreHeat / (double) controller.coreHeatCapacity);
        drawSmoothGauge(graphics, leftPos + 160, topPos + 40, (double) controller.hullHeat / (double) TileEntityPWRController.hullHeatCapacityBase);

        EnumPWRFuel loaded = EnumPWRFuel.byOrdinal(controller.typeLoaded);
        if (loaded != null && controller.amountLoaded > 0) {
            ItemStack display = new ItemStack(ModPwrItems.fuel(loaded));
            GuiDraw.item(graphics, display, leftPos + 89, topPos + 5);
            // 1.7 drawItemStack: the count label through renderItemOverlayIntoGUI at half scale, which lands the
            // shadowed text at (x + 8.5 - w/4, y + 19.5) for a label of unscaled width w.
            String label = ChatFormatting.YELLOW + "" + controller.amountLoaded + "/" + controller.rodCount;
            GuiDraw.pushMatrix(graphics);
            GuiDraw.translate(graphics, leftPos + 89 + 8.5F - font.width(label) / 4F, topPos + 5 + 19.5F);
            GuiDraw.scale(graphics, 0.5F, 0.5F);
            GuiDraw.text(graphics, font, label, 0, 0, 0xFFFFFFFF, true);
            GuiDraw.popMatrix(graphics);
        }

        drawTank(graphics, controller.tanks[0], 8, 5, 16, 52);
        drawTank(graphics, controller.tanks[1], 26, 5, 16, 52);
    }

    /** 1.7 {@code GUIElements.drawSmoothGauge(x, y, z, progress, 5, 2, 1, 0x7F0000)}: the same needle the press uses. */
    private static final int SS = 8;

    private static void drawSmoothGauge(GuiGraphicsExtractor graphics, int cx, int cy, double progress) {
        progress = Mth.clamp(progress, 0.0, 1.0);
        float angle = (float) Math.toRadians(progress * 270.0 + 45.0);
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, cx, cy);
        GuiDraw.rotate(graphics, angle);
        GuiDraw.scale(graphics, 1.0F / SS, 1.0F / SS);
        fillNeedle(graphics, 1.5, 0xFF000000);
        fillNeedle(graphics, 1.0, 0xFF7F0000);
        GuiDraw.popMatrix(graphics);
    }

    private static void fillNeedle(GuiGraphicsExtractor g, double mult, int color) {
        double tipLen = 5 * mult * SS, backLen = 2 * mult * SS, backSide = 1 * mult * SS;
        double total = tipLen + backLen;
        int top = (int) Math.round(tipLen), bot = (int) Math.round(-backLen);
        for (int y = bot; y < top; y++) {
            int hw = (int) Math.round(backSide * (tipLen - y) / total);
            if (hw > 0) g.fill(-hw, y, hw, y + 1, color);
        }
    }

    /** Repeats the 16x16 fluid texture up from the tank's bottom, matching the old GL_REPEAT quad. */
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
            GuiDraw.fill(graphics, x0, top, x0 + width, bottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);

        TileEntityPWRController controller = menu.getController();
        if (controller == null) return;
        double scale = 1.25;
        String flux = String.format(Locale.US, "%,.1f", controller.flux);
        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, (float) (1 / scale), (float) (1 / scale));
        GuiDraw.text(graphics, font, flux, (int) (165 * scale - font.width(flux)), (int) (64 * scale), 0xFF00FF00, false);
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityPWRController controller = menu.getController();
        if (controller == null) return;

        if (legacyHover(115, 31, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Core: " + String.format(Locale.US, "%,d", controller.coreHeat)
                    + " / " + String.format(Locale.US, "%,d", controller.coreHeatCapacity) + " TU")), mouseX, mouseY);
        } else if (legacyHover(151, 31, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Hull: " + String.format(Locale.US, "%,d", controller.hullHeat)
                    + " / " + String.format(Locale.US, "%,d", TileEntityPWRController.hullHeatCapacityBase) + " TU")), mouseX, mouseY);
        } else if (legacyHover(52, 31, 36, 18, mouseX, mouseY)) {
            int percent = controller.processTime <= 0 ? 0 : (int) (controller.progress * 100 / controller.processTime);
            tooltip(graphics, List.of(Component.literal(percent + "%")), mouseX, mouseY);
        } else if (legacyHover(52, 53, 54, 4, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Control rod level: "
                    + (100 - (Math.round(controller.rodLevel * 100) / 100)) + "%")), mouseX, mouseY);
        } else if (legacyHover(88, 4, 18, 18, mouseX, mouseY)) {
            EnumPWRFuel loaded = EnumPWRFuel.byOrdinal(controller.typeLoaded);
            if (loaded != null && controller.amountLoaded > 0) {
                ItemStack display = new ItemStack(ModPwrItems.fuel(loaded));
                tooltip(graphics, net.minecraft.client.gui.screens.Screen.getTooltipFromItem(Minecraft.getInstance(), display), mouseX, mouseY);
            }
        } else if (legacyHover(8, 5, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(controller.tanks[0]), mouseX, mouseY);
        } else if (legacyHover(26, 5, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(controller.tanks[1]), mouseX, mouseY);
        }
    }

    /** The old GUI used an open lower bound and a closed upper bound for painted hover regions. */
    private boolean legacyHover(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY > topPos + y && mouseY <= topPos + y + height;
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
    }

    /** 1.7 {@code mouseClicked}: the 18x18 button at (88, 58) applies the typed level (inverted) to the rods. */
    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityPWRController controller = menu.getController();
        if (controller == null) return false;
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        if (leftPos + 88 <= mouseX && leftPos + 88 + 18 > mouseX && topPos + 58 < mouseY && topPos + 58 + 18 >= mouseY) {
            double typed;
            try {
                typed = Double.parseDouble(field.getValue());
            } catch (NumberFormatException notANumber) {
                return false;
            }
            int level = (int) Mth.clamp(typed, 0, 100);
            field.setValue(level + "");

            CompoundTag control = new CompoundTag();
            control.putInt("control", 100 - level);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(controller.getBlockPos(), control));
            GuiUtil.playClickSound();
            return true;
        }
        return false;
    }

    @Override
    protected boolean keyPressed(KeyInput event) {
        // A focused field must consume the inventory key too, just as the old keyTyped path did.
        if (event.key() != 256 && event.key() != 258 && field != null && field.isFocused()) {
            event.keyPressed(field);
            return true;
        }
        return false;
    }
}
