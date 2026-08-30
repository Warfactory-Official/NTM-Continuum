package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachinePlasmaForge;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Plasma forge control panel, including recipe selection, energy, plasma ignition and fluid status. */
public class GUIMachinePlasmaForge extends dev.golem.core.gui.CompatContainerScreen<ContainerMachinePlasmaForge> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_fusion_plasmaforge.png");
    private static final int RECIPE_X = PlasmaForgeGuiLayout.RECIPE_X;
    private static final int RECIPE_Y = PlasmaForgeGuiLayout.RECIPE_Y;
    private static final int RECIPE_SIZE = PlasmaForgeGuiLayout.RECIPE_SIZE;

    public GUIMachinePlasmaForge(ContainerMachinePlasmaForge menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 244);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos, y = topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityFusionPlasmaForge forge = menu.getForge();
        if (forge == null) return;

        int p = forge.maxPower <= 0 ? 0 : (int) Math.min(62L, forge.power * 62L / forge.maxPower);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, x + 152, y + 80 - p, 176F, 62F - p, 16, p, 256, 256);
        if (forge.plasmaModule.progress > 0) {
            int w = (int) Math.ceil(70 * forge.plasmaModule.progress);
            GuiDraw.blit(graphics, TEXTURE, x + 62, y + 81, 176F, 62F, w, 16, 256, 256);
        }

        GenericRecipe recipe = forge.plasmaModule.getRecipe();
        PlasmaForgeRecipe plasmaRecipe = recipe instanceof PlasmaForgeRecipe plasma ? plasma : null;
        if (forge.didProcess) {
            led(graphics, x + 51, y + 76, 195);
            led(graphics, x + 56, y + 76, 195);
        } else if (recipe != null) {
            led(graphics, x + 51, y + 76, 192);
            if (forge.power >= recipe.power) led(graphics, x + 56, y + 76, 192);
        }

        GuiDraw.item(graphics, recipe == null ? new net.minecraft.world.item.ItemStack(ModItems.template_folder.get()) : recipe.getIcon(),
                x + PlasmaForgeGuiLayout.RECIPE_ICON_X, y + PlasmaForgeGuiLayout.RECIPE_ICON_Y);
        drawGauge(graphics, x + 34, y + 124, plasmaRecipe);
        drawGauge(graphics, x + 70, y + 124, forge.booster, forge.maxBooster);
        drawTank(graphics, forge.inputTank, x + 80, y + 18);
        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < Math.min(recipe.inputItem.length, 12); i++) {
                if (menu.slots.get(3 + i).hasItem()) continue;
                int sx = 8 + (i % 3) * 18;
                int sy = 18 + (i / 3) * 18;
                GuiDraw.item(graphics, recipe.inputItem[i].getCyclingDisplayStack(), x + sx, y + sy);
                GuiDraw.blit(graphics, TEXTURE, x + sx, y + sy, sx, sy, 16, 16, 256, 256, 0x80FFFFFF);
            }
        }
    }

    private void led(GuiGraphicsExtractor graphics, int x, int y, int u) {
        GuiDraw.blit(graphics, TEXTURE, x, y, u, 0F, 3, 6, 256, 256);
    }

    private void drawGauge(GuiGraphicsExtractor graphics, int x, int y, PlasmaForgeRecipe recipe) {
        double progress = recipe == null || recipe.ignitionTemp <= 0 ? 0D
                : Math.min((double) forgeEnergy() / recipe.ignitionTemp, 1.5D) / 1.5D;
        drawNeedle(graphics, x, y, progress, 0xFFA00000);
    }

    private void drawGauge(GuiGraphicsExtractor graphics, int x, int y, int value, int max) {
        drawNeedle(graphics, x, y, max <= 0 ? 0D : (double) value / max, 0xFFA00000);
    }

    private long forgeEnergy() {
        TileEntityFusionPlasmaForge forge = menu.getForge();
        return forge == null ? 0L : forge.plasmaEnergySync;
    }

    private static void drawNeedle(GuiGraphicsExtractor graphics, int x, int y, double progress, int color) {
        progress = Math.max(0D, Math.min(1D, progress));
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, x, y);
        GuiDraw.rotate(graphics, (float) Math.toRadians(progress * 270D + 45D));
        GuiDraw.scale(graphics, 1F / 8F, 1F / 8F);
        fillNeedle(graphics, 1.5D, 0xFF000000);
        fillNeedle(graphics, 1D, color);
        GuiDraw.popMatrix(graphics);
    }

    private static void fillNeedle(GuiGraphicsExtractor graphics, double multiplier, int color) {
        double tip = 5D * multiplier * 8D;
        double back = 2D * multiplier * 8D;
        double side = multiplier * 8D;
        double total = tip + back;
        for (int row = (int) Math.round(-back); row < (int) Math.round(tip); row++) {
            int halfWidth = (int) Math.round(side * (tip - row) / total);
            if (halfWidth > 0) graphics.fill(-halfWidth, row, halfWidth, row + 1, color);
        }
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y) {
        int max = tank.getMaxFill();
        int height = max <= 0 ? 0 : tank.getFill() * 52 / max;
        if (height <= 0) return;

        int bottom = y + 52;
        int top = bottom - height;
        int tint = 0xFF000000 | (tank.getTankType().getTint() & 0xFFFFFF);
        Identifier fluidTexture = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x, top, x + 16, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, fluidTexture, x, tileY, 0F, 0F, 16, 16, 16, 16, tint);
            }
            graphics.disableScissor();
        } else {
            GuiDraw.fill(graphics, x, top, x + 16, bottom, tint);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 70 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFusionPlasmaForge forge = menu.getForge();
        if (forge == null) return;
        GenericRecipe recipe = forge.plasmaModule.getRecipe();
        if (isHovering(152, 18, 16, 62, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(forge.power, forge.maxPower), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(RECIPE_X, RECIPE_Y, RECIPE_SIZE, RECIPE_SIZE, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, recipe == null
                    ? List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW))
                    : recipe.print(), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(80, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(forge.inputTank),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(25, 115, 18, 18, mouseX, mouseY)) {
            PlasmaForgeRecipe plasma = recipe instanceof PlasmaForgeRecipe p ? p : null;
            Component ignition = plasma == null
                    ? Component.literal("0TU / 0TU")
                    : Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(Library.getShortNumber(forge.plasmaEnergySync) + "TU / "
                            + Library.getShortNumber(plasma.ignitionTemp) + "TU"));
            GuiDraw.tooltip(graphics, font, List.of(ignition),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(98, 116, 16, 16, mouseX, mouseY)
                && !menu.getSlot(TileEntityFusionPlasmaForge.SLOT_BOOSTER).hasItem()
                && menu.getCarried().isEmpty()) {
            drawBoosterTooltip(graphics, forge, mouseX, mouseY);
        }
    }

    private void drawBoosterTooltip(GuiGraphicsExtractor graphics, TileEntityFusionPlasmaForge forge,
                                    int mouseX, int mouseY) {
        List<ItemStack> boosters = forge.getBoosterDisplayStacks();
        if (boosters.isEmpty()) return;

        int selectedIndex = (int) (System.currentTimeMillis() % (1000L * boosters.size()) / 1000L);
        ItemStack selected = boosters.get(selectedIndex);
        List<List<ItemStack>> rows = new ArrayList<>();
        if (boosters.size() < 10) {
            rows.add(boosters);
        } else if (boosters.size() < 24) {
            int split = boosters.size() / 2;
            rows.add(boosters.subList(0, split));
            rows.add(boosters.subList(split, boosters.size()));
        } else {
            int split = (int) Math.ceil(boosters.size() / 3D);
            int split2 = (int) Math.ceil(boosters.size() / 3D * 2D);
            rows.add(boosters.subList(0, split));
            rows.add(boosters.subList(split, split2));
            rows.add(boosters.subList(split2, boosters.size()));
        }

        String tooltipTitle = "Booster Isotope:";
        String selectedName = selected.getHoverName().getString();
        int tooltipWidth = Math.max(font.width(tooltipTitle), font.width(selectedName));
        for (List<ItemStack> row : rows) tooltipWidth = Math.max(tooltipWidth, row.size() * 18);
        int tooltipHeight = 20 + rows.size() * 18;
        int minX = mouseX + 12;
        int minY = mouseY - 12;
        if (minX + tooltipWidth > width) minX -= 28 + tooltipWidth;
        if (minY + tooltipHeight + 6 > height) minY = height - tooltipHeight - 6;

        GuiDraw.nextStratum(graphics);

        // This is the old drawStackText frame: dark backing with the one-pixel blue edge.
        int background = 0xF0100010;
        graphics.fill(minX - 3, minY - 4, minX + tooltipWidth + 3, minY - 3, background);
        graphics.fill(minX - 3, minY + tooltipHeight + 3, minX + tooltipWidth + 3, minY + tooltipHeight + 4, background);
        graphics.fill(minX - 3, minY - 3, minX + tooltipWidth + 3, minY + tooltipHeight + 3, background);
        graphics.fill(minX - 4, minY - 3, minX - 3, minY + tooltipHeight + 3, background);
        graphics.fill(minX + tooltipWidth + 3, minY - 3, minX + tooltipWidth + 4, minY + tooltipHeight + 3, background);

        int border = 0x505000FF;
        int borderShadow = (border & 0xFEFEFE) >> 1 | border & 0xFF000000;
        graphics.fill(minX - 3, minY - 2, minX - 2, minY + tooltipHeight + 2, border);
        graphics.fill(minX + tooltipWidth + 2, minY - 2, minX + tooltipWidth + 3, minY + tooltipHeight + 2, border);
        graphics.fill(minX - 3, minY - 3, minX + tooltipWidth + 3, minY - 2, border);
        graphics.fill(minX - 3, minY + tooltipHeight + 2, minX + tooltipWidth + 3, minY + tooltipHeight + 3, borderShadow);

        GuiDraw.text(graphics, font, tooltipTitle, minX, minY, 0xFFFFFFFF, true);
        int lineY = minY + 12;
        int itemIndex = 0;
        for (List<ItemStack> row : rows) {
            for (int column = 0; column < row.size(); column++) {
                if (itemIndex == selectedIndex) {
                    graphics.fill(minX + column * 18 - 1, lineY - 1,
                            minX + column * 18 + 17, lineY + 17, 0xFFFF0000);
                    graphics.fill(minX + column * 18, lineY,
                            minX + column * 18 + 16, lineY + 16, 0xFF808080);
                }
                GuiDraw.item(graphics, row.get(column), minX + column * 18, lineY);
                itemIndex++;
            }
            lineY += 18;
        }
        GuiDraw.text(graphics, font, selectedName, minX, lineY, 0xFFFFFFFF, true);
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityFusionPlasmaForge forge = menu.getForge();
        if (forge != null && isHovering(RECIPE_X, RECIPE_Y, RECIPE_SIZE, RECIPE_SIZE, event.x(), event.y())) {
            GUIScreenRecipeSelector.openSelector(PlasmaForgeRecipes.INSTANCE, forge.getBlockPos(),
                    forge.plasmaModule.recipe, 0, ItemBlueprints.grabPool(forge.getItem(TileEntityFusionPlasmaForge.SLOT_BLUEPRINT)), this);
            return true;
        }
        return false;
    }
}
