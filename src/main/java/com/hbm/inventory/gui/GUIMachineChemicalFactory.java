package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineChemicalFactory;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** The legacy 248x216 four-lane Chemical Factory screen. */
public class GUIMachineChemicalFactory
        extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineChemicalFactory>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(
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
        GuiDraw.text(graphics, font, playerInventoryTitle, 26, imageHeight - 94, 0xFF404040, false);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        int max = tank.getMaxFill();
        int filled = max <= 0 ? 0 : (int) ((long) tank.getFill() * height / max);
        if (filled <= 0) return;
        int x0 = leftPos + x;
        int bottom = topPos + y + height;
        int top = bottom - filled;
        Identifier fluidTexture = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + width, bottom);
            for (int tile = bottom - 16; tile > top - 16; tile -= 16) {
                GuiDraw.blit(graphics, fluidTexture, x0, tile, 0F, 0F, width, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, top, x0 + width, bottom,
                    0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    private void led(GuiGraphicsExtractor graphics, int x, int sourceX, int lane) {
        GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + 21 + lane * 22,
                sourceX, 222F, 4, 4, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineChemicalFactory factory = menu.getFactory();
        if (factory == null) return;

        if (isHovering(224, 18, 16, 68, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(factory.power, factory.maxPower),
                    Optional.empty(), mouseX, mouseY);
            return;
        }

        for (int lane = 0; lane < TileEntityMachineChemicalFactory.LANES; lane++) {
            if (isHovering(74, 19 + lane * 22, 18, 18, mouseX, mouseY)) {
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
                if (isHovering(60 + i * 5, 20 + lane * 22, 3, 16, mouseX, mouseY)) {
                    tankTooltip(graphics, factory.inputTanks[tank], mouseX, mouseY);
                    return;
                }
                if (isHovering(189 + i * 5, 20 + lane * 22, 3, 16, mouseX, mouseY)) {
                    tankTooltip(graphics, factory.outputTanks[tank], mouseX, mouseY);
                    return;
                }
            }
        }
        if (isHovering(224, 125, 7, 52, mouseX, mouseY)) {
            tankTooltip(graphics, factory.water, mouseX, mouseY);
        } else if (isHovering(233, 125, 7, 52, mouseX, mouseY)) {
            tankTooltip(graphics, factory.lps, mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, List.of(tank.getTankType().getDisplayName(),
                        Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")),
                Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachineChemicalFactory factory = menu.getFactory();
        if (factory == null) return false;
        for (int lane = 0; lane < TileEntityMachineChemicalFactory.LANES; lane++) {
            if (isHovering(74, 19 + lane * 22, 18, 18, (int) event.x(), (int) event.y())) {
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
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getFactory();
    }
}
