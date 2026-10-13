package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachinePUREX;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Screen for the PUREX plant, from 1.7.10 {@code GUIMachinePUREX} ({@code processing/gui_purex.png}, 176×256).
 * Power bar 16×61 at (152,18) filling upward; progress arrow up to 70×16 at (62,126); two 3×6 status LEDs at (51,121)/(56,121); the recipe-selector button at (7,125) showing the
 * selected recipe's icon or the template-folder fallback; ghost previews of the recipe's inputs in empty
 * input slots; three 16×52 reagent tank bars at x=8/26/44 (top y=18) and the 16×52 product tank at x=116
 * (top y=36).
 */
public class GUIMachinePUREX extends CompatContainerScreen<ContainerMachinePUREX> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_purex.png");
    private static final int BTN_X = 7, BTN_Y = 125, BTN_W = 18, BTN_H = 18;
    private static final int TANK_W = 16, TANK_H = 52;
    private static final int IN_TANK_TOP = 18, OUT_TANK_TOP = 36, OUT_TANK_X = 116;

    public GUIMachinePUREX(ContainerMachinePUREX menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 256);
    }

    /** 1.7: title centred on x=70 at y=6, inventory at (8, ySize-96+2), both 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachinePUREX te = this.menu.getPurex();
        if (te == null) return;

        long max = te.maxPower;
        int p = max <= 0L ? 0 : (int) (te.power * 61L / max);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 79 - p,
                176.0F, (float) (61 - p), 16, p, 256, 256);

        double progress = te.purexModule.progress;
        if (progress > 0) {
            int j = (int) Math.ceil(70 * progress);
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 62, this.topPos + 126, 176.0F, 61.0F, j, 16, 256, 256);
        }

        for (int i = 0; i < 3; i++) drawTank(graphics, te.inputTanks[i], 8 + i * 18, IN_TANK_TOP);
        drawTank(graphics, te.outputTanks[0], OUT_TANK_X, OUT_TANK_TOP);

        GenericRecipe recipe = PUREXRecipes.INSTANCE.getRecipe(te.purexModule.recipe);
        // 1.7 LEDs: left = recipe set, right = recipe set and enough power; both bright while processing.
        if (te.didProcess) led(graphics, 51, 195); else if (recipe != null) led(graphics, 51, 192);
        if (te.didProcess) led(graphics, 56, 195); else if (recipe != null && te.power >= recipe.power) led(graphics, 56, 192);

        GuiDraw.item(graphics, recipe != null ? recipe.getIcon() : new ItemStack(ModItems.template_folder.get()),
                this.leftPos + 8, this.topPos + 126);

        // Ghost slots: faint previews of the recipe's item inputs in empty input slots (menu slots 4..6).
        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < Math.min(recipe.inputItem.length, 3); i++) {
                if (this.menu.slots.get(4 + i).hasItem()) continue;
                int sx = 8 + i * 18, sy = 90;
                GuiDraw.item(graphics, recipe.inputItem[i].getCyclingDisplayStack(), this.leftPos + sx, this.topPos + sy);
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + sx, this.topPos + sy,
                        (float) sx, (float) sy, 16, 16, 256, 256, 0x80FFFFFF);
            }
        }
    }

    /** 1.7 {@code renderTank(guiLeft + x, guiTop + top + 52, ...)}: the 1.7 call passes the bottom edge. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop) {
        GuiUtil.tank(graphics, tank, this.leftPos + barX, this.topPos + barTop + TANK_H, TANK_W, TANK_H);
    }

    private void led(GuiGraphicsExtractor graphics, int x, int u) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + x, this.topPos + 121, (float) u, 0.0F, 3, 6, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachinePUREX te = this.menu.getPurex();
        if (te == null) return;

        if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 152, this.topPos + 18, 16, 61)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.maxPower), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, this.leftPos + BTN_X, this.topPos + BTN_Y, BTN_W, BTN_H)) {
            GenericRecipe recipe = PUREXRecipes.INSTANCE.getRecipe(te.purexModule.recipe);
            if (recipe != null) GuiDraw.tooltip(graphics, this.font, recipe.print(), Optional.empty(), mouseX, mouseY);
            else GuiDraw.tooltip(graphics, this.font,
                    List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)), Optional.empty(), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 8 + i * 18, this.topPos + IN_TANK_TOP, TANK_W, TANK_H)) {
                tankTooltip(graphics, te.inputTanks[i], mouseX, mouseY);
                return;
            }
        }
        if (GuiUtil.hover(mouseX, mouseY, this.leftPos + OUT_TANK_X, this.topPos + OUT_TANK_TOP, TANK_W, TANK_H)) {
            tankTooltip(graphics, te.outputTanks[0], mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(tank), mouseX, mouseY);
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getPurex();
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityMachinePUREX te = this.menu.getPurex();
        if (te != null && GuiUtil.hover(event.x(), event.y(), this.leftPos + BTN_X, this.topPos + BTN_Y, BTN_W, BTN_H)) {
            GUIScreenRecipeSelector.openSelector(PUREXRecipes.INSTANCE, te.getBlockPos(),
                    te.purexModule.recipe, 0, ItemBlueprints.grabPool(te.getItem(1)), this);
            return true;
        }
        return false;
    }
}
