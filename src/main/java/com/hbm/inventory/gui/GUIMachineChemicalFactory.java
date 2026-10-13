package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineChemicalFactory;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** The 248x216 four-lane Chemical Factory screen, ported from 1.7.10 {@code GUIMachineChemicalFactory} ({@code processing/gui_chemical_factory.png}). */
public class GUIMachineChemicalFactory
        extends CompatContainerScreen<ContainerMachineChemicalFactory>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of(
            "hbm", "textures/gui/processing/gui_chemical_factory.png");

    public GUIMachineChemicalFactory(ContainerMachineChemicalFactory menu, Inventory playerInventory,
                                     Component title) {
        super(menu, playerInventory, title, 248, 216);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, 248, 116, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 18, topPos + 116,
                18F, 116F, 230, 100, 256, 256);

        TileEntityMachineChemicalFactory factory = menu.getFactory();
        if (factory == null) return;

        long max = factory.maxPower;
        int powerHeight = max <= 0L ? 0 : (int) (factory.power * 68L / max);
        if (powerHeight > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 224, topPos + 86 - powerHeight,
                    0F, 184F - powerHeight, 16, powerHeight, 256, 256);
        }

        for (int lane = 0; lane < TileEntityMachineChemicalFactory.LANES; lane++) {
            if (factory.chemplantModule[lane].progress > 0D) {
                int width = (int) Math.ceil(22D * factory.chemplantModule[lane].progress);
                GuiDraw.blit(graphics, TEXTURE, leftPos + 113, topPos + 29 + lane * 22,
                        0F, 216F, width, 6, 256, 256);
            }

            GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.getRecipe(
                    factory.chemplantModule[lane].recipe);
            if (factory.didProcess[lane]) {
                led(graphics, 113, 4, lane);
                led(graphics, 121, 4, lane);
            } else {
                if (recipe != null) led(graphics, 113, 0, lane);
                if (recipe != null && factory.power >= recipe.power && factory.canCool()) {
                    led(graphics, 121, 0, lane);
                }
            }

            GuiDraw.item(graphics, recipe != null ? recipe.getIcon()
                    : new ItemStack(ModItems.template_folder.get()),
                    leftPos + 75, topPos + 20 + lane * 22);

            if (recipe != null && recipe.inputItem != null) {
                for (int i = 0; i < Math.min(3, recipe.inputItem.length); i++) {
                    int machineSlot = TileEntityMachineChemicalFactory.inputStart(lane) + i;
                    var slot = menu.slots.get(machineSlot);
                    if (slot.hasItem()) continue;
                    GuiDraw.item(graphics, recipe.inputItem[i].getCyclingDisplayStack(),
                            leftPos + slot.x, topPos + slot.y);
                    GuiDraw.blit(graphics, TEXTURE, leftPos + slot.x, topPos + slot.y,
                            slot.x, slot.y, 16, 16, 256, 256, 0x80FFFFFF);
                }
            }
        }

        for (int lane = 0; lane < TileEntityMachineChemicalFactory.LANES; lane++) {
            for (int i = 0; i < 3; i++) {
                drawTank(graphics, factory.inputTanks[lane * 3 + i],
                        60 + i * 5, 20 + lane * 22, 3, 16);
                drawTank(graphics, factory.outputTanks[lane * 3 + i],
                        189 + i * 5, 20 + lane * 22, 3, 16);
            }
        }
        drawTank(graphics, factory.water, 224, 125, 7, 52);
        drawTank(graphics, factory.lps, 233, 125, 7, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 106 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 26, imageHeight - 96 + 2, 0xFF404040, false);
    }

    /** 1.7 {@code renderTank(guiLeft + x, guiTop + y + height, ...)}: the 1.7 call passes the bottom edge. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        GuiUtil.tank(graphics, tank, leftPos + x, topPos + y + height, width, height);
    }

    private void led(GuiGraphicsExtractor graphics, int x, int sourceX, int lane) {
        GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + 21 + lane * 22,
                sourceX, 222F, 4, 4, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineChemicalFactory factory = menu.getFactory();
        if (factory == null) return;

        if (GuiUtil.hover(mouseX, mouseY, leftPos + 224, topPos + 18, 16, 68)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(factory.power, factory.maxPower), mouseX, mouseY);
            return;
        }

        for (int lane = 0; lane < TileEntityMachineChemicalFactory.LANES; lane++) {
            if (GuiUtil.hover(mouseX, mouseY, leftPos + 74, topPos + 19 + lane * 22, 18, 18)) {
                GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.getRecipe(
                        factory.chemplantModule[lane].recipe);
                GuiDraw.tooltip(graphics, font,
                        recipe == null ? List.of(Component.translatable("gui.recipe.setRecipe")
                                        .withStyle(ChatFormatting.YELLOW)) : recipe.print(),
                        Optional.empty(), mouseX, mouseY);
                return;
            }
            for (int i = 0; i < 3; i++) {
                int tank = lane * 3 + i;
                if (GuiUtil.hover(mouseX, mouseY, leftPos + 60 + i * 5, topPos + 20 + lane * 22, 3, 16)) {
                    tankTooltip(graphics, factory.inputTanks[tank], mouseX, mouseY);
                    return;
                }
                if (GuiUtil.hover(mouseX, mouseY, leftPos + 189 + i * 5, topPos + 20 + lane * 22, 3, 16)) {
                    tankTooltip(graphics, factory.outputTanks[tank], mouseX, mouseY);
                    return;
                }
            }
        }
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 224, topPos + 125, 7, 52)) {
            tankTooltip(graphics, factory.water, mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 233, topPos + 125, 7, 52)) {
            tankTooltip(graphics, factory.lps, mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(tank), mouseX, mouseY);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityMachineChemicalFactory factory = menu.getFactory();
        if (factory == null) return false;
        for (int lane = 0; lane < TileEntityMachineChemicalFactory.LANES; lane++) {
            if (GuiUtil.hover(event.x(), event.y(), leftPos + 74, topPos + 19 + lane * 22, 18, 18)) {
                int blueprint = TileEntityMachineChemicalFactory.blueprintSlot(lane);
                GUIScreenRecipeSelector.openSelector(ChemicalPlantRecipes.INSTANCE,
                        factory.getBlockPos(), factory.chemplantModule[lane].recipe, lane,
                        ItemBlueprints.grabPool(factory.getItem(blueprint)), this);
                return true;
            }
        }
        return false;
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getFactory();
    }
}
