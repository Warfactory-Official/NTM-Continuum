package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachinePUREX;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
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
 * Screen for the PUREX plant — a faithful port of the 1.12.2 {@code GUIMachinePUREX} on its own
 * {@code gui_purex.png} (176×256). Power bar 16×61 at (152,18) filling upward; progress arrow up to 70×16 at
 * (62,126); two 3×6 status LEDs at (51,121)/(56,121); the recipe-selector button at (7,125) showing the
 * selected recipe's icon or the template-folder fallback; ghost previews of the recipe's inputs in empty
 * input slots; three 16×52 reagent tank bars at x=8/26/44 (top y=18) and the 16×52 product tank at x=116
 * (top y=36).
 */
public class GUIMachinePUREX extends dev.golem.core.gui.CompatContainerScreen<ContainerMachinePUREX> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_purex.png");
    private static final int BTN_X = 7, BTN_Y = 125, BTN_W = 18, BTN_H = 18;
    private static final int TANK_W = 16, TANK_H = 52;
    private static final int IN_TANK_TOP = 18, OUT_TANK_TOP = 36, OUT_TANK_X = 116;

    public GUIMachinePUREX(ContainerMachinePUREX menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 256);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2; // the old GUI centred on x=70
        this.inventoryLabelY = this.imageHeight - 94;
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
        boolean configured = recipe != null || ItemBlueprints.grabPool(te.getItem(1)) != null;
        boolean powered = recipe != null ? te.power >= recipe.power : te.power > 0;
        if (te.didProcess) led(graphics, 51, 195); else if (configured) led(graphics, 51, 192);
        if (te.didProcess) led(graphics, 56, 195); else if (configured && powered) led(graphics, 56, 192);

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

    /** Draws a tank's fill bar: tile the fluid's 16×16 sprite into the fill height, or a solid colour. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop) {
        int maxF = tank.getMaxFill();
        int h = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * TANK_H / maxF);
        if (h <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + TANK_H;
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
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachinePUREX te = this.menu.getPurex();
        if (te == null) return;

        if (this.isHovering(152, 18, 16, 61, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            GenericRecipe recipe = PUREXRecipes.INSTANCE.getRecipe(te.purexModule.recipe);
            if (recipe != null) GuiDraw.tooltip(graphics, this.font, recipe.print(), Optional.empty(), mouseX, mouseY);
            else GuiDraw.tooltip(graphics, this.font,
                    List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)), Optional.empty(), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (this.isHovering(8 + i * 18, IN_TANK_TOP, TANK_W, TANK_H, mouseX, mouseY)) {
                tankTooltip(graphics, te.inputTanks[i], mouseX, mouseY);
                return;
            }
        }
        if (this.isHovering(OUT_TANK_X, OUT_TANK_TOP, TANK_W, TANK_H, mouseX, mouseY)) {
            tankTooltip(graphics, te.outputTanks[0], mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, List.of(
                Component.literal(tank.getTankType().getName()),
                Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")
        ), Optional.empty(), mouseX, mouseY);
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getPurex();
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachinePUREX te = this.menu.getPurex();
        if (te != null && this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            GUIScreenRecipeSelector.openSelector(PUREXRecipes.INSTANCE, te.getBlockPos(),
                    te.purexModule.recipe, 0, ItemBlueprints.grabPool(te.getItem(1)), this);
            return true;
        }
        return false;
    }
}
