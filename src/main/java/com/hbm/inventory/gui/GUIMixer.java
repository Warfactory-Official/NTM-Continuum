package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMixer;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.MixerRecipes.MixerRecipe;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.TileEntityMachineMixer;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Mixer screen, preserving the legacy output-fluid recipe cycling button and three tank bars. */
public class GUIMixer extends dev.golem.core.gui.CompatContainerScreen<ContainerMixer>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_mixer.png");
    private static final int BTN_X = 62, BTN_Y = 22, BTN_W = 12, BTN_H = 12;
    private static final int TANK_TOP = 23, TANK_HEIGHT = 52;

    public GUIMixer(ContainerMixer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineMixer mixer = menu.getMixer();
        if (mixer == null) return;

        int power = (int) mixer.getPowerScaled(53);
        if (power > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 23, topPos + 75 - power,
                    176F, 52F - power, 16, power, 256, 256);
        }
        if (mixer.processTime > 0 && mixer.progress > 0) {
            int progress = mixer.getProgressScaled(53);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 36,
                    192F, 0F, progress, 44, 256, 256);
        }

        drawTank(graphics, mixer.tanks[0], 43, 7);
        drawTank(graphics, mixer.tanks[1], 52, 7);
        drawTank(graphics, mixer.tanks[2], 117, 16);

    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int width) {
        int fill = tank.getMaxFill() <= 0 ? 0 : tank.getFill() * TANK_HEIGHT / tank.getMaxFill();
        if (fill <= 0) return;
        int bottom = topPos + TANK_TOP + TANK_HEIGHT;
        int top = bottom - fill;
        int drawX = leftPos + x;
        Identifier fluidTexture = tank.getTankType().getTexture();
        int tint = 0xFF000000 | (tank.getTankType().getTint() & 0xFFFFFF);
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(drawX, top, drawX + width, bottom);
            for (int y = bottom - 16; y > top - 16; y -= 16) {
                GuiDraw.blit(graphics, fluidTexture, drawX, y, 0F, 0F, width, 16, 16, 16, tint);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(drawX, top, drawX + width, bottom, tint);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachineMixer mixer = menu.getMixer();
        if (mixer != null && isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            data.putBoolean("toggle", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                    new ControlEventPayload(mixer.getBlockPos(), data));
            return true;
        }
        return false;
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineMixer mixer = menu.getMixer();
        if (mixer == null) return;
        if (isHovering(23, 22, 16, 53, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(mixer.power, mixer.getMaxPower()),
                    Optional.empty(), mouseX, mouseY);
            return;
        }
        if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            MixerRecipe[] recipes = MixerRecipes.getOutput(mixer.tanks[2].getTankType());
            if (recipes == null || recipes.length <= 1) return;
            MixerRecipe recipe = recipes[Math.floorMod(mixer.recipeIndex, recipes.length)];
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.recipe.current", mixer.recipeIndex + 1, recipes.length)
                    .withStyle(ChatFormatting.YELLOW));
            if (recipe.input1 != null) tooltip.add(Component.literal("-").append(recipe.input1.type.getDisplayName()));
            if (recipe.input2 != null) tooltip.add(Component.literal("-").append(recipe.input2.type.getDisplayName()));
            if (recipe.solidInput != null) {
                tooltip.add(Component.literal("-").append(recipe.solidInput.getCyclingDisplayStack().getHoverName()));
            }
            tooltip.add(Component.translatable("gui.recipe.clickToChange").withStyle(ChatFormatting.RED));
            GuiDraw.tooltip(graphics, font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }
        if (isHovering(43, 23, 7, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(mixer.tanks[0]), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(52, 23, 7, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(mixer.tanks[1]), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(117, 23, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(mixer.tanks[2]), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getMixer();
    }
}
