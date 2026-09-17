package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineTurbineGas;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Screen for the legacy gas turbine, including the ignition, automatic-load, and throttle controls. */
public class GUIMachineTurbineGas extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineTurbineGas> {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/generators/gui_turbinegas.png");
    private static final Identifier GAUGE = dev.golem.core.Ids.of("hbm", "textures/gui/gauges/button_big.png");
    private boolean sliderLocked;
    private int numberToDisplay;
    private int digitNumber;
    private int exponent;

    public GUIMachineTurbineGas(ContainerMachineTurbineGas menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 223);
    }

    @Override protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineTurbineGas turbine = menu.getTurbine();
        if (turbine == null) return;
        if (sliderLocked) setSlider(mouseY);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 74, topPos + 86, 194F, turbine.autoMode ? 11F : 24F, 29, 13, 256, 256);
        switch (turbine.state) {
            case -1 -> {
                GuiDraw.blit(graphics, TEXTURE, leftPos + 80, topPos + 32, 194F, 38F, 16, 16, 256, 256);
                displayStartup(graphics, turbine.counter);
            }
            case 1 -> {
                GuiDraw.blit(graphics, TEXTURE, leftPos + 80, topPos + 32, 210F, 38F, 16, 16, 256, 256);
                drawPowerMeterDisplay(graphics, 20 * turbine.instantPowerOutput);
            }
            default -> GuiDraw.blit(graphics, TEXTURE, leftPos + 80, topPos + 32, 178F, 38F, 16, 16, 256, 256);
        }
        GuiDraw.blit(graphics, TEXTURE, leftPos + 36, topPos + 97 - turbine.powerSliderPos, 178F, 0F, 16, 6, 256, 256);
        int power = (int) (turbine.power * 142L / TileEntityMachineTurbineGas.MAX_POWER);
        if (power > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 26, topPos + 109, 0F, 223F, power, 16, 256, 256);
        drawGauge(graphics, turbine.rpm, 64, 16);
        int temperature = Mth.clamp(turbine.temp, 0, 800);
        int heat = temperature * 64 / 800;
        if (heat > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 136, topPos + 28 + 64 - heat, 176F, 64F - heat, 2, heat, 256, 256);
        drawTank(graphics, turbine.tanks[0], 8, ContainerMachineTurbineGas.FUEL_TANK_TOP, 16, 48);
        drawTank(graphics, turbine.tanks[1], 8, ContainerMachineTurbineGas.LUBRICANT_TANK_TOP, 16, 32);
        drawTank(graphics, turbine.tanks[2], 147, ContainerMachineTurbineGas.WATER_TANK_TOP, 16, 36);
        drawTank(graphics, turbine.tanks[3], 147, ContainerMachineTurbineGas.STEAM_TANK_TOP, 16, 36);

        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 34, 3);
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 50, 2);
        if (turbine.tanks[0].getFill() < 5000 || turbine.tanks[1].getFill() < 1000) {
            GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 66, turbine.tanks[0].getFill() == 0 || turbine.tanks[1].getFill() == 0 ? 6 : 7);
        }
    }

    private void displayStartup(GuiGraphicsExtractor graphics, int counter) {
        if (numberToDisplay < 8_888_888 && counter < 60) {
            digitNumber++;
            if (digitNumber == 9) {
                digitNumber = 1;
                exponent++;
            }
            numberToDisplay += (int) Math.pow(10, exponent);
        }
        if (counter > 50) numberToDisplay = 0;
        drawPowerMeterDisplay(graphics, numberToDisplay);
    }

    private void drawPowerMeterDisplay(GuiGraphicsExtractor graphics, int number) {
        int[] digits = new int[7];
        for (int i = 6; i >= 0; i--) {
            digits[i] = number % 10;
            number /= 10;
            GuiDraw.blit(graphics, TEXTURE, leftPos + 65 + i * 7, topPos + 71,
                    194F + digits[i] * 5F, 0F, 5, 11, 256, 256);
        }
        int leadingZeros = 0;
        while (leadingZeros < 6 && digits[leadingZeros] == 0) leadingZeros++;
        for (int i = 0; i < leadingZeros; i++) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 65 + i * 7, topPos + 71,
                    244F, 0F, 5, 11, 256, 256);
        }
    }

    private void drawGauge(GuiGraphicsExtractor graphics, int position, int x, int y) {
        GuiDraw.blit(graphics, GAUGE, leftPos + x, topPos + y, position * 48F, 0F, 48, 48, 4848, 48);
    }

    /** Local GUI rectangle for the visible portion of a tank fill. The y argument is the top of the tank. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        TankDisplay.Fill visible = TankDisplay.visibleFill(tank, x, y, width, height);
        if (visible == null) return;
        int x0 = leftPos + visible.x(), top = topPos + visible.top(), bottom = topPos + visible.bottom();
        Identifier texture = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + visible.width(), bottom);
            for (int ty = bottom - 16; ty > top - 16; ty -= 16)
                GuiDraw.blit(graphics, texture, x0, ty, 0F, 0F, visible.width(), 16, 16, 16, visible.tint());
            graphics.disableScissor();
        } else GuiDraw.fill(graphics, x0, top, x0 + width, bottom,
                visible.tint());
    }

    @Override protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineTurbineGas turbine = menu.getTurbine();
        if (turbine == null) return;
        if (isHovering(26, 108, 142, 16, mouseX, mouseY))
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(turbine.power, turbine.getMaxPower()), Optional.empty(), mouseX, mouseY);
        else if (isHovering(8, 16, 16, 48, mouseX, mouseY))
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(turbine.tanks[0]), Optional.empty(), mouseX, mouseY);
        else if (isHovering(8, 70, 16, 32, mouseX, mouseY))
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(turbine.tanks[1]), Optional.empty(), mouseX, mouseY);
        else if (isHovering(147, 61, 16, 36, mouseX, mouseY))
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(turbine.tanks[2]), Optional.empty(), mouseX, mouseY);
        else if (isHovering(147, 21, 16, 36, mouseX, mouseY))
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(turbine.tanks[3]), Optional.empty(), mouseX, mouseY);
        else if (isHovering(36, 36, 16, 66, mouseX, mouseY)) {
            if (turbine.state == 1) {
                double consumption = turbine.tanks[0].getTankType() == Fluids.GAS ? 50D
                        : turbine.tanks[0].getTankType() == Fluids.SYNGAS ? 10D
                        : turbine.tanks[0].getTankType() == Fluids.OXYHYDROGEN ? 100D
                        : 5D;
                double rate = 20D * (consumption * 0.05D + consumption * turbine.powerSliderPos / 60D);
                GuiDraw.tooltip(graphics, font, List.of(Component.literal("Fuel consumption: " + String.format("%.1f", rate) + " mb/s")),
                        Optional.empty(), mouseX, mouseY);
            } else {
                GuiDraw.tooltip(graphics, font, List.of(Component.literal("Generator offline")), Optional.empty(), mouseX, mouseY);
            }
        } else if (isHovering(133, 23, 8, 72, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal("Temperature: " + Math.max(turbine.temp, 20) + "°C")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(-16, 34, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.literal("Automatic turbine throttling mode").withStyle(ChatFormatting.DARK_GREEN),
                    Component.literal("Click AUTO to adjust throttle based on network demand.")), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(-16, 50, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.literal("Accepted fuels:").withStyle(ChatFormatting.GOLD),
                    Component.literal("Gas, coker gas, petroleum, reforming gas, biogas, syngas, oxyhydrogen")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(-16, 66, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal("Fuel or lubricant level low!")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        Minecraft minecraft = Minecraft.getInstance();
        TileEntityMachineTurbineGas turbine = menu.getTurbine();
        if (turbine == null || minecraft.gameMode == null) return false;
        double x = event.x(), y = event.y();
        double dx = x - (leftPos + 88), dy = y - (topPos + 40);
        if (dx * dx + dy * dy <= 64D && (turbine.counter == 0 || turbine.counter == 579)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        if (turbine.state == 1 && isHovering(74, 86, 29, 13, x, y)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            return true;
        }
        if (turbine.state == 1 && !turbine.autoMode
                && isHovering(36, 97 - turbine.powerSliderPos, 16, 6, x, y)) {
            sliderLocked = true;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
            return true;
        }
        return false;
    }

    @Override protected boolean mouseReleased(dev.golem.core.gui.MouseInput event) {
        if (sliderLocked && (event.button() == 0 || event.button() == 1)) sliderLocked = false;
        return false;
    }

    private void setSlider(double mouseY) {
        TileEntityMachineTurbineGas turbine = menu.getTurbine();
        Minecraft minecraft = Minecraft.getInstance();
        if (turbine == null || minecraft.gameMode == null) return;
        int value = Mth.clamp((int) (topPos + 100 - mouseY), 0, 60);
        if (turbine.powerSliderPos != value) {
            turbine.powerSliderPos = value;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1000 + value);
        }
    }
}
