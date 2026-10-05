package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineTurbineGas;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for the combined-cycle gas turbine — 1.7.10 {@code GUIMachineTurbineGas} ({@code generators/gui_turbinegas.png},
 * 176×223): start/stop round button (88,40) r8, AUTO button (74,86) 29×13, throttle slider (36, 97−pos) 16×6,
 * horizontal power bar (26,109) 142×16 from (0,223), the RPM dial revealed as a pie sector of (176,64) 48×48 at
 * (64,16), the 2-px thermometer at (136,28), the 7-digit power meter at (65,71), and the three info panels left of
 * the GUI.
 */
public class GUIMachineTurbineGas extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineTurbineGas> {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/generators/gui_turbinegas.png");
    private boolean sliderLocked;
    private int numberToDisplay;
    private int digitNumber;
    private int exponent;

    public GUIMachineTurbineGas(ContainerMachineTurbineGas menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 223);
    }

    /** 1.7: centred title at y=6, inventory at (8, ySize-94), both 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineTurbineGas turbine = menu.getTurbine();
        if (turbine == null) return;
        if (sliderLocked) dragSlider(mouseX, mouseY);

        GuiDraw.blit(graphics, TEXTURE, leftPos + 74, topPos + 86, 194F, turbine.autoMode ? 11F : 24F, 29, 13, 256, 256);
        switch (turbine.state) {
            case 0 -> GuiDraw.blit(graphics, TEXTURE, leftPos + 80, topPos + 32, 178F, 38F, 16, 16, 256, 256);
            case -1 -> {
                GuiDraw.blit(graphics, TEXTURE, leftPos + 80, topPos + 32, 194F, 38F, 16, 16, 256, 256);
                displayStartup(graphics, turbine.counter);
            }
            case 1 -> {
                GuiDraw.blit(graphics, TEXTURE, leftPos + 80, topPos + 32, 210F, 38F, 16, 16, 256, 256);
                drawPowerMeterDisplay(graphics, 20 * turbine.instantPowerOutput);
            }
            default -> { }
        }
        GuiDraw.blit(graphics, TEXTURE, leftPos + 36, topPos + 97 - turbine.powerSliderPos, 178F, 0F, 16, 6, 256, 256);

        int power = (int) (turbine.power * 142L / TileEntityMachineTurbineGas.MAX_POWER);
        if (power > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 26, topPos + 109, 0F, 223F, power, 16, 256, 256);

        GuiUtil.smoothCircle(graphics, TEXTURE, leftPos + 64, topPos + 16, 176, 64, 48, 48, turbine.rpm / 100D);
        drawThermometer(graphics, turbine.temp);

        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 34, 3);
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 34 + 16, 2);
        if (turbine.tanks[0].getFill() < 5000 || turbine.tanks[1].getFill() < 1000)
            GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 34 + 32, 7);
        if (turbine.tanks[0].getFill() == 0 || turbine.tanks[1].getFill() == 0)
            GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 34 + 32, 6);

        GuiUtil.tank(graphics, turbine.tanks[0], leftPos + 8, topPos + 65, 16, 48);
        GuiUtil.tank(graphics, turbine.tanks[1], leftPos + 8, topPos + 103, 16, 32);
        GuiUtil.tank(graphics, turbine.tanks[2], leftPos + 147, topPos + 98, 16, 36);
        GuiUtil.tank(graphics, turbine.tanks[3], leftPos + 147, topPos + 58, 16, 36);
    }

    /** 1.7 {@code displayStartup}: the counting-up 8888888 lamp test (advances once per frame, as in 1.7). */
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
            GuiDraw.blit(graphics, TEXTURE, leftPos + 65 + i * 7, topPos + 9 + 62, 194F + digits[i] * 5F, 0F, 5, 11, 256, 256);
        }
        int leadingZeros = 0;
        while (leadingZeros < 6 && digits[leadingZeros] == 0) leadingZeros++;
        for (int i = 0; i < leadingZeros; i++) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 65 + i * 7, topPos + 9 + 62, 244F, 0F, 5, 11, 256, 256);
        }
    }

    /** 1.7 {@code drawThermometer}: a 2×(64·temp/800) column at (136, 28+64−h) sampled from (176, 64−h). */
    private void drawThermometer(GuiGraphicsExtractor graphics, int temp) {
        int h = 64 * temp / 800;
        if (h <= 0) return;
        GuiDraw.blit(graphics, TEXTURE, leftPos + 136, topPos + 28 + 64 - h, 176F, 64F - h, 2, h, 256, 256);
    }

    /** 1.7 {@code drawScreen} tooltips, in the same order and with the same strings. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineTurbineGas turbine = menu.getTurbine();
        if (turbine == null) return;
        int gl = leftPos, gt = topPos;

        if (GuiUtil.hover(mouseX, mouseY, gl + 26, gt + 108, 142, 16)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(turbine.power, turbine.getMaxPower()), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 36, gt + 36, 16, 66)) {
            if (turbine.state == 1) {
                // 1.7 fuelMaxCons: GAS 50, SYNGAS 10, OXYHYDROGEN 100, REFORMGAS 5, default 5.
                FluidType type = turbine.tanks[0].getTankType();
                double consumption = type == Fluids.GAS ? 50D : type == Fluids.SYNGAS ? 10D : type == Fluids.OXYHYDROGEN ? 100D : 5D;
                int throttle = turbine.powerSliderPos * 100 / 60; // the block entity's own throttle formula
                GuiUtil.tooltip(graphics, font, GuiUtil.lines("Fuel consumption: " + 20 * (consumption * 0.05D + consumption * throttle / 100) + " mb/s"), mouseX, mouseY);
            } else {
                GuiUtil.tooltip(graphics, font, GuiUtil.lines("Generator offline"), mouseX, mouseY);
            }
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 133, gt + 23, 8, 72)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.lines("Temperature: " + (turbine.temp >= 20 ? turbine.temp : 20) + "°C"), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 8, gt + 16, 16, 48)) { GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(turbine.tanks[0]), mouseX, mouseY); return; }
        if (GuiUtil.hover(mouseX, mouseY, gl + 8, gt + 70, 16, 32)) { GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(turbine.tanks[1]), mouseX, mouseY); return; }
        if (GuiUtil.hover(mouseX, mouseY, gl + 147, gt + 61, 16, 36)) { GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(turbine.tanks[2]), mouseX, mouseY); return; }
        if (GuiUtil.hover(mouseX, mouseY, gl + 147, gt + 21, 16, 36)) { GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(turbine.tanks[3]), mouseX, mouseY); return; }

        // 1.7 draws these three lists anchored at (guiLeft - 8, guiTop + 60), not at the mouse.
        if (GuiUtil.hover(mouseX, mouseY, gl - 16, gt + 34, 16, 16)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.keyArray("desc.gui.turbinegas.automode"), gl - 8, gt + 44 + 16);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl - 16, gt + 34 + 16, 16, 16)) {
            List<Component> fuels = new ArrayList<>();
            fuels.add(Component.translatable("desc.gui.turbinegas.fuels"));
            // 1.7 iterates Fluids.getInNiceOrder(); the port has no nice order, so this is registration order.
            for (FluidType type : Fluids.getAll()) {
                FluidCombustion.Combustible c = FluidCombustion.getCombustible(type);
                if (c != null && c.grade() == FluidCombustion.FuelGrade.GAS) fuels.add(Component.literal("  ").append(type.getDisplayName()));
            }
            GuiUtil.tooltip(graphics, font, fuels, gl - 8, gt + 44 + 16);
            return;
        }
        if ((turbine.tanks[0].getFill() < 5000 || turbine.tanks[1].getFill() < 1000) && GuiUtil.hover(mouseX, mouseY, gl - 16, gt + 34 + 32, 16, 16)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.keyArray("desc.gui.turbinegas.warning"), gl - 8, gt + 44 + 16);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        Minecraft minecraft = Minecraft.getInstance();
        TileEntityMachineTurbineGas turbine = menu.getTurbine();
        if (turbine == null || minecraft.gameMode == null) return false;
        double x = event.x(), y = event.y();

        // 1.7: circular start/stop button, radius 8 around (88, 40); only while idle (counter 0) or fully up (579).
        double dx = x - leftPos - 88, dy = y - topPos - 40;
        if (Math.sqrt(dx * dx + dy * dy) <= 8) {
            if (turbine.counter == 0 || turbine.counter == 579) {
                GuiUtil.playClickSound();
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
            return true;
        }
        // 1.7 AUTO button: x (74, 103], y [86, 99), online only.
        if (turbine.state == 1 && x > leftPos + 74 && x <= leftPos + 74 + 29 && y >= topPos + 86 && y < topPos + 86 + 13) {
            GuiUtil.playClickSound();
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            return true;
        }
        // 1.7 slider grab: y [97-pos, 103-pos), x (36, 52]; clicking it always turns AUTO off.
        if (turbine.state == 1 && topPos + 97 - turbine.powerSliderPos <= y && topPos + 103 - turbine.powerSliderPos > y
                && leftPos + 36 < x && leftPos + 52 >= x) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
            GuiUtil.playClickSound();
            sliderLocked = true;
            return true;
        }
        return false;
    }

    @Override
    protected boolean mouseReleased(dev.golem.core.gui.MouseInput event) {
        if (sliderLocked && (event.button() == 0 || event.button() == 1)) sliderLocked = false;
        return false;
    }

    /** 1.7 {@code mouseClickMove}: only with AUTO off, online, and the mouse inside x (36,52], y (37,103]. */
    private void dragSlider(double mouseX, double mouseY) {
        TileEntityMachineTurbineGas turbine = menu.getTurbine();
        Minecraft minecraft = Minecraft.getInstance();
        if (turbine == null || minecraft.gameMode == null) return;
        if (turbine.autoMode || turbine.state != 1) return;
        if (!(leftPos + 36 < mouseX && leftPos + 52 >= mouseX && topPos + 37 < mouseY && topPos + 103 >= mouseY)) return;
        int value = (int) (topPos + 100 - mouseY);
        if (value > 60) value = 60;
        else if (value < 0) value = 0;
        if (turbine.powerSliderPos != value) {
            turbine.powerSliderPos = value;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1000 + value);
        }
    }
}
