package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
import com.hbm.inventory.container.ContainerMachineElectrolyser;
import com.hbm.api.material.MaterialStack;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.Mats;
import com.hbm.tileentity.machine.TileEntityMachineElectrolyser;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** The two legacy electrolyser screens share this renderer and select their background by menu mode. */
public final class GUIMachineElectrolyser extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineElectrolyser>
        implements IUpgradeInfoProviderSource {

    private static final Identifier FLUID_TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_electrolyser_fluid.png");
    private static final Identifier METAL_TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_electrolyser_metal.png");

    public GUIMachineElectrolyser(ContainerMachineElectrolyser menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 210, 204);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Identifier texture = menu.isMetal() ? METAL_TEXTURE : FLUID_TEXTURE;
        GuiDraw.blit(graphics, texture, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineElectrolyser te = menu.getElectrolyser();
        if (te == null) return;

        int power = (int) (te.power * 89L / TileEntityMachineElectrolyser.MAX_POWER);
        if (power > 0) GuiDraw.blit(graphics, texture, leftPos + 186, topPos + 107 - power,
                210F, 89F - power, 16, power, 256, 256);
        if (menu.isMetal()) {
            if (te.power >= te.usageMetal) {
                GuiDraw.blit(graphics, texture, leftPos + 190, topPos + 4, 226F, 25F, 9, 12, 256, 256);
            }
            drawMaterial(graphics, te.leftStack, 58);
            drawMaterial(graphics, te.rightStack, 96);
            int progress = te.metalDuration <= 0 ? 0 : te.progressMetal * 26 / te.metalDuration;
            if (progress > 0) GuiDraw.blit(graphics, texture, leftPos + 7, topPos + 71 - progress,
                    226F, 25F - progress, 22, progress, 256, 256);
            drawTank(graphics, te.tanks[3], 36, 18);
        } else {
            if (te.power >= te.usageFluid) {
                GuiDraw.blit(graphics, texture, leftPos + 190, topPos + 4, 226F, 40F, 9, 12, 256, 256);
            }
            int progress = te.fluidDuration <= 0 ? 0 : te.progressFluid * 41 / te.fluidDuration;
            if (progress > 0) GuiDraw.blit(graphics, texture, leftPos + 62, topPos + 26,
                    226F, 0F, 12, progress, 256, 256);
            drawTank(graphics, te.tanks[0], 42, 18);
            drawTank(graphics, te.tanks[1], 96, 18);
            drawTank(graphics, te.tanks[2], 116, 18);
        }
    }

    private void drawMaterial(GuiGraphicsExtractor graphics, @org.jspecify.annotations.Nullable MaterialStack material, int x) {
        if (material == null) return;
        int height = material.amount * 42 / TileEntityMachineElectrolyser.MAX_MATERIAL;
        if (height <= 0) return;
        GuiDraw.blit(graphics, METAL_TEXTURE, leftPos + x, topPos + 60 - height,
                210F, 131F - height, 34, height, 256, 256,
                0xFF000000 | (material.material.moltenColor() & 0xFFFFFF));
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y) {
        int height = tank.getMaxFill() <= 0 ? 0 : tank.getFill() * 52 / tank.getMaxFill();
        if (height <= 0) return;
        int x0 = leftPos + x;
        int bottom = topPos + y + 52;
        int top = bottom - height;
        Identifier texture = tank.getTankType().getTexture();
        int tint = 0xFF000000 | (tank.getTankType().getTint() & 0xFFFFFF);
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + 16, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, texture, x0, tileY, 0F, 0F, 16, 16, 16, 16, tint);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, top, x0 + 16, bottom, tint);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 89 - font.width(title) / 2, 7, 0xFFFFFF, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineElectrolyser te = menu.getElectrolyser();
        if (te == null) return;
        if (isHovering(186, 18, 16, 89, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(te.power, TileEntityMachineElectrolyser.MAX_POWER),
                    Optional.empty(), mouseX, mouseY);
            return;
        }
        if (menu.isMetal()) {
            if (isHovering(36, 18, 16, 52, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(te.tanks[3]), Optional.empty(), mouseX, mouseY);
            } else if (isHovering(58, 18, 34, 42, mouseX, mouseY)) {
                materialTooltip(graphics, te.leftStack, mouseX, mouseY);
            } else if (isHovering(96, 18, 34, 42, mouseX, mouseY)) {
                materialTooltip(graphics, te.rightStack, mouseX, mouseY);
            }
        } else {
            if (isHovering(42, 18, 16, 52, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(te.tanks[0]), Optional.empty(), mouseX, mouseY);
            }
            if (isHovering(96, 18, 16, 52, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(te.tanks[1]), Optional.empty(), mouseX, mouseY);
            }
            if (isHovering(116, 18, 16, 52, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(te.tanks[2]), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (isHovering(8, 82, 54, 12, event.x(), event.y()) && minecraft.gameMode != null) {
            Gauges.click();
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                    menu.isMetal() ? ContainerMachineElectrolyser.MODE_FLUID : ContainerMachineElectrolyser.MODE_METAL);
            return true;
        }
        return false;
    }

    private void materialTooltip(GuiGraphicsExtractor graphics, @org.jspecify.annotations.Nullable MaterialStack stack, int x, int y) {
        GuiDraw.tooltip(graphics, font, stack == null ? List.of(Component.literal("Empty").withStyle(ChatFormatting.RED))
                : List.of(Component.literal(stack.material.getDisplayName() + ": "
                        + Mats.formatAmount(stack.amount, Minecraft.getInstance().player != null
                        && Minecraft.getInstance().player.isShiftKeyDown())).withStyle(ChatFormatting.YELLOW)),
                Optional.empty(), x, y);
    }

    @Override public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() { return menu.getElectrolyser(); }
}
