package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachinePrecAss;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Precision assembler screen, preserving the old 176x256 layout and horizontal fluid bars. */
public final class GUIMachinePrecAss extends dev.golem.core.gui.CompatContainerScreen<ContainerMachinePrecAss>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_precass.png");
    private static final int BTN_X = 7, BTN_Y = 125, BTN_W = 18, BTN_H = 18;

    public GUIMachinePrecAss(ContainerMachinePrecAss menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachinePrecAss te = menu.getAssembler();
        if (te == null) return;

        int power = te.maxPower <= 0 ? 0 : (int) (te.power * 61L / te.maxPower);
        if (power > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 152, topPos + 79 - power,
                176F, 61F - power, 16, power, 256, 256);

        if (te.assemblerModule.progress > 0) {
            int progress = (int) Math.ceil(70 * te.assemblerModule.progress);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 126,
                    176F, 61F, progress, 16, 256, 256);
        }

        drawTank(graphics, te.inputTank, leftPos + 8, topPos + 115, 52, 16);
        drawTank(graphics, te.outputTank, leftPos + 80, topPos + 115, 52, 16);

        GenericRecipe recipe = PrecAssRecipes.INSTANCE.getRecipe(te.assemblerModule.recipe);
        if (te.didProcess) {
            led(graphics, 51, 195);
            led(graphics, 56, 195);
        } else if (recipe != null) {
            led(graphics, 51, 192);
            if (te.power >= recipe.power) led(graphics, 56, 192);
        }

        GuiDraw.item(graphics, recipe != null ? recipe.getIcon() : new ItemStack(ModItems.template_folder.get()),
                leftPos + 8, topPos + 126);
        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < Math.min(recipe.inputItem.length, 9); i++) {
                int slot = 4 + i;
                if (menu.slots.get(slot).hasItem()) continue;
                int x = leftPos + menu.slots.get(slot).x;
                int y = topPos + menu.slots.get(slot).y;
                GuiDraw.item(graphics, recipe.inputItem[i].getCyclingDisplayStack(), x, y);
                GuiDraw.blit(graphics, TEXTURE, x, y, menu.slots.get(slot).x, menu.slots.get(slot).y,
                        16, 16, 256, 256, 0x80FFFFFF);
            }
        }
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        if (tank.getFill() <= 0 || tank.getMaxFill() <= 0 || tank.getTankType() == com.hbm.inventory.fluid.Fluids.NONE) return;
        int fill = tank.getFill() * width / tank.getMaxFill();
        if (fill <= 0) return;
        Identifier texture = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            graphics.enableScissor(x, y, x + fill, y + height);
            for (int tile = 0; tile < width; tile += 16)
                GuiDraw.blit(graphics, texture, x + tile, y, 0F, 0F, 16, height, 16, 16);
            graphics.disableScissor();
        } else {
            GuiDraw.fill(graphics, x, y, x + fill, y + height,
                    0xFF000000 | (tank.getTankType().getTint() & 0xFFFFFF));
        }
    }

    private void led(GuiGraphicsExtractor graphics, int x, int u) {
        GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + 121, u, 0F, 3, 6, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachinePrecAss te = menu.getAssembler();
        if (te == null) return;
        if (isHovering(152, 18, 16, 61, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(te.power, te.maxPower), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(8, 99, 52, 16, mouseX, mouseY)) {
            tankTooltip(graphics, te.inputTank, mouseX, mouseY);
        } else if (isHovering(80, 99, 52, 16, mouseX, mouseY)) {
            tankTooltip(graphics, te.outputTank, mouseX, mouseY);
        } else if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            GenericRecipe recipe = PrecAssRecipes.INSTANCE.getRecipe(te.assemblerModule.recipe);
            GuiDraw.tooltip(graphics, font, recipe == null
                    ? List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW))
                    : recipe.print(), Optional.empty(), mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(tank), Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 70 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 0x404040, false);
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachinePrecAss te = menu.getAssembler();
        if (te != null && isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            GUIScreenRecipeSelector.openSelector(PrecAssRecipes.INSTANCE, te.getBlockPos(),
                    te.assemblerModule.recipe, 0, ItemBlueprints.grabPool(te.getItem(1)), this);
            return true;
        }
        return false;
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getAssembler();
    }
}
