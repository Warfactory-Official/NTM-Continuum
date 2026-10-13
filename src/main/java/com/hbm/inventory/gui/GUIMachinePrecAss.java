package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachinePrecAss;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Precision assembler screen: 1.7.10 {@code GUIMachinePrecAss} ({@code processing/gui_precass.png}, 176x256). */
public final class GUIMachinePrecAss extends CompatContainerScreen<ContainerMachinePrecAss>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_precass.png");
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

        // 1.7: renderTank(guiLeft + 8 / + 80, guiTop + 115, z, 52, 16, 1). Horizontal, y is the bottom edge.
        GuiUtil.tankHorizontal(graphics, te.inputTank, leftPos + 8, topPos + 115, 52, 16);
        GuiUtil.tankHorizontal(graphics, te.outputTank, leftPos + 80, topPos + 115, 52, 16);

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

    private void led(GuiGraphicsExtractor graphics, int x, int u) {
        GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + 121, u, 0F, 3, 6, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachinePrecAss te = menu.getAssembler();
        if (te == null) return;
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 152, topPos + 18, 16, 61)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(te.power, te.maxPower), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 8, topPos + 99, 52, 16)) {
            tankTooltip(graphics, te.inputTank, mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 80, topPos + 99, 52, 16)) {
            tankTooltip(graphics, te.outputTank, mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H)) {
            GenericRecipe recipe = PrecAssRecipes.INSTANCE.getRecipe(te.assemblerModule.recipe);
            GuiDraw.tooltip(graphics, font, recipe == null
                    ? List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW))
                    : recipe.print(), Optional.empty(), mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(tank), mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 70 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityMachinePrecAss te = menu.getAssembler();
        if (te != null && GuiUtil.hover(event.x(), event.y(), leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H)) {
            GUIScreenRecipeSelector.openSelector(PrecAssRecipes.INSTANCE, te.getBlockPos(),
                    te.assemblerModule.recipe, 0, ItemBlueprints.grabPool(te.getItem(1)), this);
            return true;
        }
        return false;
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getAssembler();
    }
}
