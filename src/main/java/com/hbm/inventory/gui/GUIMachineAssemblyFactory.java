package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineAssemblyFactory;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** The 256x240 Assembly Factory screen — 1.7.10 {@code GUIMachineAssemblyFactory} ({@code processing/gui_assembly_factory.png}). */
public final class GUIMachineAssemblyFactory
        extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineAssemblyFactory>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm",
            "textures/gui/processing/gui_assembly_factory.png");
    private static final int TANK_W = 5;

    public GUIMachineAssemblyFactory(ContainerMachineAssemblyFactory menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 256, 240);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, 256, 140, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 25, topPos + 140, 25F, 140F, 231, 100, 256, 256);

        TileEntityMachineAssemblyFactory factory = menu.getFactory();
        if (factory == null) return;

        long max = factory.maxPower;
        int power = max <= 0L ? 0 : (int) (factory.power * 92L / max);
        if (power > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 234, topPos + 110 - power,
                    0F, 232F - power, 16, power, 256, 256);
        }

        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            int x = lane % 2;
            int y = lane / 2;
            GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe(factory.assemblerModule[lane].recipe);

            if (factory.assemblerModule[lane].progress > 0D) {
                int progress = (int) Math.ceil(37D * factory.assemblerModule[lane].progress);
                GuiDraw.blit(graphics, TEXTURE, leftPos + 45 + x * 109, topPos + 63 + y * 56,
                        0F, 240F, progress, 6, 256, 256);
            }

            if (factory.didProcess[lane]) {
                led(graphics, 45 + x * 109, 55 + y * 56, 4);
                led(graphics, 53 + x * 109, 55 + y * 56, 4);
            } else if (recipe != null) {
                led(graphics, 45 + x * 109, 55 + y * 56, 0);
                if (factory.power >= recipe.power && factory.canCool()) {
                    led(graphics, 53 + x * 109, 55 + y * 56, 0);
                }
            }

            GuiDraw.item(graphics, recipe != null ? recipe.getIcon()
                            : new ItemStack(ModItems.template_folder.get()),
                    leftPos + 7 + x * 109, topPos + 54 + y * 56);

            if (recipe != null && recipe.inputItem != null) {
                for (int i = 0; i < Math.min(recipe.inputItem.length, 12); i++) {
                    Slot slot = menu.slots.get(TileEntityMachineAssemblyFactory.inputStart(lane) + i);
                    if (slot.hasItem()) continue;
                    GuiDraw.item(graphics, recipe.inputItem[i].getCyclingDisplayStack(),
                            leftPos + slot.x, topPos + slot.y);
                    GuiDraw.blit(graphics, TEXTURE, leftPos + slot.x, topPos + slot.y,
                            slot.x, slot.y, 16, 16, 256, 256, 0x80FFFFFF);
                }
            }

        }

        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            int x = lane % 2;
            int y = lane / 2;
            drawTank(graphics, factory.inputTanks[lane], 105 + x * 109, 52 + y * 56, TANK_W, 32);
            drawTank(graphics, factory.outputTanks[lane], 105 + x * 109, 70 + y * 56, TANK_W, 16);
        }
        drawTank(graphics, factory.water, 232, 201, 7, 52);
        drawTank(graphics, factory.lps, 241, 201, 7, 52);
    }

    private void led(GuiGraphicsExtractor graphics, int x, int y, int u) {
        GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + y, u, 236F, 4, 4, 256, 256);
    }

    /** 1.7 {@code renderTank(guiLeft + x, guiTop + bottom, z, width, height)}: y is the bottom edge. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank,
                          int x, int bottom, int width, int height) {
        GuiUtil.tank(graphics, tank, leftPos + x, topPos + bottom, width, height);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineAssemblyFactory factory = menu.getFactory();
        if (factory == null) return;

        if (GuiUtil.hover(mouseX, mouseY, leftPos + 234, topPos + 18, 16, 92)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(factory.power, factory.maxPower), mouseX, mouseY);
            return;
        }

        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            int x = lane % 2;
            int y = lane / 2;
            if (GuiUtil.hover(mouseX, mouseY, leftPos + 6 + x * 109, topPos + 53 + y * 56, 18, 18)) {
                GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe(factory.assemblerModule[lane].recipe);
                GuiDraw.tooltip(graphics, font, recipe == null
                        ? List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW))
                        : recipe.print(), Optional.empty(), mouseX, mouseY);
                return;
            }
            if (GuiUtil.hover(mouseX, mouseY, leftPos + 105 + x * 109, topPos + 20 + y * 56, 5, 32)) {
                tankTooltip(graphics, factory.inputTanks[lane], mouseX, mouseY);
                return;
            }
            if (GuiUtil.hover(mouseX, mouseY, leftPos + 105 + x * 109, topPos + 54 + y * 56, 5, 16)) {
                tankTooltip(graphics, factory.outputTanks[lane], mouseX, mouseY);
                return;
            }
        }
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 232, topPos + 149, 7, 52)) {
            tankTooltip(graphics, factory.water, mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 241, topPos + 149, 7, 52)) {
            tankTooltip(graphics, factory.lps, mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y) {
        GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(tank), x, y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 113 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 33, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityMachineAssemblyFactory factory = menu.getFactory();
        if (factory == null) return false;
        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            int x = lane % 2;
            int y = lane / 2;
            if (GuiUtil.hover(event.x(), event.y(), leftPos + 6 + x * 109, topPos + 53 + y * 56, 18, 18)) {
                GUIScreenRecipeSelector.openSelector(AssemblyMachineRecipes.INSTANCE, factory.getBlockPos(),
                        factory.assemblerModule[lane].recipe, lane,
                        ItemBlueprints.grabPool(factory.getItem(TileEntityMachineAssemblyFactory.templateSlot(lane))), this);
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
