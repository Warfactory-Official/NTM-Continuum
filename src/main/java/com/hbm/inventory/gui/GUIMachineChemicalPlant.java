package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineChemicalPlant;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Screen for the chemical plant. Mirrors {@link GUIMachineAssemblyMachine} (power/progress bars, status
 * LEDs, the recipe-selector button with the selected/template-folder icon, and input ghost slots) and adds
 * six fluid tank bars (3 input on the left, 3 output on the right) drawn like {@link GUIMachineFluidTank}
 * (the fluid's own 16×16 sprite tiled into the fill height, or a solid colour fallback). Reads the live
 * client BlockEntity (synced via the BufPacket). Faithful to the 1.12.2 {@code GUIMachineChemicalPlant}.
 */
public class GUIMachineChemicalPlant extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineChemicalPlant> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_chemplant.png");
    private static final int BTN_X = 7, BTN_Y = 125, BTN_W = 18, BTN_H = 18;
    // Tank bars: 16 wide × 34 tall, top at y=18 (so bottom at y=52).
    private static final int TANK_TOP = 18, TANK_W = 16, TANK_H = 34;

    public GUIMachineChemicalPlant(ContainerMachineChemicalPlant menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineChemicalPlant te = this.menu.getChemplant();
        if (te == null) return;

        // Power bar: 16×61 at (152,18), filling upward, sampled from u=176.
        long max = te.maxPower;
        int p = max <= 0L ? 0 : (int) (te.power * 61L / max);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 79 - p, 176.0F, (float) (61 - p), 16, p, 256, 256);

        // Progress bar: up to 70×16 at (62,126), sampled from (u=176, v=61).
        double progress = te.chemplantModule.progress;
        if (progress > 0) {
            int j = (int) Math.ceil(70 * progress);
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 62, this.topPos + 126, 176.0F, te.chemplantModule.restrictedMode ? 77.0F : 61.0F, j, 16, 256, 256); // 1.7: restricted-mode bar below
        }

        // Fluid tank bars: 3 input (x=8,26,44) + 3 output (x=80,98,116).
        for (int i = 0; i < 3; i++) {
            drawTank(graphics, te.inputTanks[i], 8 + i * 18);
            drawTank(graphics, te.outputTanks[i], 80 + i * 18);
        }

        // Status LEDs (recipe configured / enough power), like the assembly machine.
        GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.getRecipe(te.chemplantModule.recipe);
        // 1.7: left LED lit by a set recipe, right LED by a set recipe with enough power for it.
        if (te.didProcess) led(graphics, 51, 195); else if (recipe != null) led(graphics, 51, 192);
        if (te.didProcess) led(graphics, 56, 195); else if (recipe != null && te.power >= recipe.power) led(graphics, 56, 192);

        // Selector button icon: the selected recipe's icon, or the template-folder fallback.
        GuiDraw.item(graphics, recipe != null ? recipe.getIcon() : new ItemStack(ModItems.template_folder.get()),
                this.leftPos + 8, this.topPos + 126);

        // Ghost slots: faint previews of the recipe's item inputs in empty input slots (menu slots 4..6).
        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < Math.min(recipe.inputItem.length, 3); i++) {
                if (this.menu.slots.get(4 + i).hasItem()) continue;
                int sx = 8 + i * 18, sy = 99;
                GuiDraw.item(graphics, recipe.inputItem[i].getCyclingDisplayStack(), this.leftPos + sx, this.topPos + sy);
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + sx, this.topPos + sy,
                        (float) sx, (float) sy, 16, 16, 256, 256, 0x80FFFFFF);
            }
        }
    }

    /** 1.7 {@code renderTank(guiLeft + barX, guiTop + 52, z, 16, 34)}: the tinted fluid column, bottom-anchored. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX) {
        Production2Gui.drawTank(graphics, tank, this.leftPos + barX, this.topPos + TANK_TOP + TANK_H, TANK_W, TANK_H);
    }

    private void led(GuiGraphicsExtractor graphics, int x, int u) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + x, this.topPos + 121, (float) u, 0.0F, 3, 6, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineChemicalPlant te = this.menu.getChemplant();
        if (te == null) return;

        if (in17(152, 18, 16, 61, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (in17(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.getRecipe(te.chemplantModule.recipe);
            if (recipe != null) GuiDraw.tooltip(graphics, this.font, recipe.print(), Optional.empty(), mouseX, mouseY);
            else GuiDraw.tooltip(graphics, this.font,
                    List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)), Optional.empty(), mouseX, mouseY);
            return;
        }
        // Fluid tank hover → name + mB.
        for (int i = 0; i < 3; i++) {
            if (in17(8 + i * 18, TANK_TOP, TANK_W, TANK_H, mouseX, mouseY)) { tankTooltip(graphics, te.inputTanks[i], mouseX, mouseY); return; }
            if (in17(80 + i * 18, TANK_TOP, TANK_W, TANK_H, mouseX, mouseY)) { tankTooltip(graphics, te.outputTanks[i], mouseX, mouseY); return; }
        }
        // (No "Acceptable Upgrades" info panel here — the 1.12.2 chemical plant GUI has none; the upgrade
        // item's own tooltip [ItemMachineUpgrade + IUpgradeInfoProviderSource] shows this machine's numbers.)
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getChemplant();
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, GuiUtil.tankTooltip(tank), Optional.empty(), mouseX, mouseY);
    }

    /** 1.7 {@code GUIMachineChemicalPlant.drawGuiContainerForegroundLayer}: title centred on x 70 at y 6, "Inventory" at (8, ySize - 94). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachineChemicalPlant te = this.menu.getChemplant();
        if (te != null && in17(BTN_X, BTN_Y, BTN_W, BTN_H, (int) event.x(), (int) event.y())) {
            GUIScreenRecipeSelector.openSelector(ChemicalPlantRecipes.INSTANCE, te.getBlockPos(),
                    te.chemplantModule.recipe, 0, ItemBlueprints.grabPool(te.getItem(1)), this);
            return true;
        }
        return false;
    }

    /** The 1.7 GUI hit test ({@code checkClick}/{@code renderTankInfo}): x <= mouse < x + w, y < mouse <= y + h. */
    private boolean in17(int x, int y, int w, int h, int mouseX, int mouseY) {
        int mx = mouseX - this.leftPos, my = mouseY - this.topPos;
        return x <= mx && x + w > mx && y < my && y + h >= my;
    }
}
