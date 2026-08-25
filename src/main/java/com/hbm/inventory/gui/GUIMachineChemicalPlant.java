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
import net.minecraft.client.Minecraft;
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
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 62, this.topPos + 126, 176.0F, 61.0F, j, 16, 256, 256);
        }

        // Fluid tank bars: 3 input (x=8,26,44) + 3 output (x=80,98,116).
        for (int i = 0; i < 3; i++) {
            drawTank(graphics, te.inputTanks[i], 8 + i * 18);
            drawTank(graphics, te.outputTanks[i], 80 + i * 18);
        }

        // Status LEDs (recipe configured / enough power), like the assembly machine.
        GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.getRecipe(te.chemplantModule.recipe);
        boolean configured = recipe != null || ItemBlueprints.grabPool(te.getItem(1)) != null;
        boolean powered = recipe != null ? te.power >= recipe.power : te.power > 0;
        if (te.didProcess) led(graphics, 51, 195); else if (configured) led(graphics, 51, 192);
        if (te.didProcess) led(graphics, 56, 195); else if (configured && powered) led(graphics, 56, 192);

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

    /** Draws a fluid tank's fill bar at GUI x {@code barX}: tile the fluid's 16×16 sprite, or a solid colour. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX) {
        int maxF = tank.getMaxFill();
        int h = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * TANK_H / maxF);
        if (h <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + TANK_TOP + TANK_H;
        int yTop = yBottom - h;
        Identifier fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + TANK_W, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, TANK_W, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + TANK_W, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    private void led(GuiGraphicsExtractor graphics, int x, int u) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + x, this.topPos + 121, (float) u, 0.0F, 3, 6, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineChemicalPlant te = this.menu.getChemplant();
        if (te == null) return;

        if (this.isHovering(152, 18, 16, 61, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.getRecipe(te.chemplantModule.recipe);
            if (recipe != null) GuiDraw.tooltip(graphics, this.font, recipe.print(), Optional.empty(), mouseX, mouseY);
            else GuiDraw.tooltip(graphics, this.font,
                    List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)), Optional.empty(), mouseX, mouseY);
            return;
        }
        // Fluid tank hover → name + mB.
        for (int i = 0; i < 3; i++) {
            if (this.isHovering(8 + i * 18, TANK_TOP, TANK_W, TANK_H, mouseX, mouseY)) { tankTooltip(graphics, te.inputTanks[i], mouseX, mouseY); return; }
            if (this.isHovering(80 + i * 18, TANK_TOP, TANK_W, TANK_H, mouseX, mouseY)) { tankTooltip(graphics, te.outputTanks[i], mouseX, mouseY); return; }
        }
        // (No "Acceptable Upgrades" info panel here — the 1.12.2 chemical plant GUI has none; the upgrade
        // item's own tooltip [ItemMachineUpgrade + IUpgradeInfoProviderSource] shows this machine's numbers.)
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getChemplant();
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, List.of(
                tank.getTankType().getDisplayName(),
                Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")
        ), Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachineChemicalPlant te = this.menu.getChemplant();
        if (te != null && this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            GUIScreenRecipeSelector.openSelector(ChemicalPlantRecipes.INSTANCE, te.getBlockPos(),
                    te.chemplantModule.recipe, 0, ItemBlueprints.grabPool(te.getItem(1)), this);
            return true;
        }
        return false;
    }
}
