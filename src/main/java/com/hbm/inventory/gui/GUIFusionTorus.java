package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFusionTorus;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Fusion torus control panel. The left column is stored power, then the three fuel tanks; the right
 * side is the product tank and the two coolant tanks. The centre carries the recipe button, the
 * progress bar and — below it — the collector bonus bar, plus three status LEDs (power, coolant, plasma)
 * which between them tell you why the reactor is or is not burning.
 */
public class GUIFusionTorus extends dev.golem.core.gui.CompatContainerScreen<ContainerFusionTorus> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_fusion_torus.png");

    private static final int RECIPE_X = 43;
    private static final int RECIPE_Y = 80;
    private static final int RECIPE_SIZE = 18;
    /** The reactor only burns at or below this; matches the coolant LED. */
    private static final int COOL_KELVIN = 123;

    public GUIFusionTorus(ContainerFusionTorus menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 230, 244);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityFusionTorus torus = this.menu.getTorus();
        if (torus == null) return;

        int p = (int) (torus.power * 62 / torus.getMaxPower());
        GuiDraw.blit(graphics, TEXTURE, x + 8, y + 80 - p, 230F, (float) (62 - p), 16, p, 256, 256);

        if (torus.fusionModule.progress > 0) {
            int w = (int) Math.ceil(70 * torus.fusionModule.progress);
            GuiDraw.blit(graphics, TEXTURE, x + 98, y + 81, 0F, 244F, w, 6, 256, 256);
        }
        if (torus.fusionModule.bonus > 0) {
            int w = (int) Math.min(Math.ceil(70 * torus.fusionModule.bonus), 70);
            GuiDraw.blit(graphics, TEXTURE, x + 98, y + 91, 0F, 250F, w, 6, 256, 256);
        }

        FusionRecipe recipe = recipe(torus);
        // Power LED, coolant LED, plasma LED.
        if (recipe != null && torus.power >= recipe.power) led(graphics, x + 160, y + 115);
        if (Math.ceil(torus.temperature) <= COOL_KELVIN) led(graphics, x + 170, y + 115);
        if (torus.didProcess) led(graphics, x + 180, y + 115);

        // Selector button icon: the running cycle's product, or the template-folder when none is set -
        // the same affordance the assembly machine uses, and the only cue that the button is a button.
        GuiDraw.item(graphics, recipe != null ? recipe.getIcon()
                        : new net.minecraft.world.item.ItemStack(com.hbm.items.ModItems.template_folder.get()),
                x + RECIPE_X, y + RECIPE_Y);

        drawTank(graphics, torus.tanks[0], x + 44, y + 18);
        drawTank(graphics, torus.tanks[1], x + 62, y + 18);
        drawTank(graphics, torus.tanks[2], x + 80, y + 18);
        drawTank(graphics, torus.tanks[3], x + 152, y + 18);
        drawTank(graphics, torus.coolantTanks[0], x + 188, y + 46);
        drawTank(graphics, torus.coolantTanks[1], x + 206, y + 46);
    }

    private void led(GuiGraphicsExtractor graphics, int x, int y) {
        GuiDraw.blit(graphics, TEXTURE, x, y, 246F, 14F, 8, 8, 256, 256);
    }

    /** A 16-wide, 52-tall column filling upward, tinted with the fluid's colour. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y) {
        if (tank.getMaxFill() <= 0 || tank.getFill() <= 0) return;
        int height = Math.max(1, tank.getFill() * 52 / tank.getMaxFill());
        GuiDraw.fill(graphics, x, y + 52 - height, x + 16, y + 52,
                0xFF000000 | tank.getTankType().getColor());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title,
                106 - this.font.width(this.title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 35, this.imageHeight - 93, 0xFF404040, false);

        TileEntityFusionTorus torus = this.menu.getTorus();
        if (torus == null) return;
        int heat = (int) Math.ceil(torus.temperature);
        String label = heat + "K";
        int color = heat > COOL_KELVIN ? 0xFFFF5555 : 0xFF55FFFF;
        GuiDraw.text(graphics, this.font, "/" + COOL_KELVIN + "K", 190, 32, 0xFF55FFFF, false);
        GuiDraw.text(graphics, this.font, label, 220 - this.font.width(label), 22, color, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFusionTorus torus = this.menu.getTorus();
        if (torus == null) return;
        FusionRecipe recipe = recipe(torus);

        if (this.isHovering(RECIPE_X, RECIPE_Y, RECIPE_SIZE, RECIPE_SIZE, mouseX, mouseY)) {
            tooltip(graphics, recipe != null
                    ? List.of(Component.literal(recipe.getLocalizedName()).withStyle(ChatFormatting.YELLOW))
                    : List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)),
                    mouseX, mouseY);
            return;
        }
        if (this.isHovering(8, 18, 16, 62, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal(
                    String.format(Locale.US, "%,d / %,d HE", torus.power, torus.getMaxPower()))), mouseX, mouseY);
            return;
        }
        if (this.isHovering(43, 115, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("→ "
                    + String.format(Locale.US, "%,d", torus.klystronEnergy) + " KyU / "
                    + (recipe == null ? "0" : String.format(Locale.US, "%,d", recipe.ignitionTemp)) + " KyU")
                    .withStyle(ChatFormatting.GREEN)), mouseX, mouseY);
            return;
        }
        if (this.isHovering(79, 115, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("← "
                    + String.format(Locale.US, "%,d", torus.plasmaEnergy) + " TU / "
                    + (recipe == null ? "0" : String.format(Locale.US, "%,d", recipe.outputTemp)) + " TU")
                    .withStyle(ChatFormatting.RED)), mouseX, mouseY);
            return;
        }
        if (this.isHovering(115, 115, 18, 18, mouseX, mouseY) && recipe != null && recipe.inputFluid != null) {
            List<Component> lines = new ArrayList<>();
            for (var input : recipe.inputFluid) {
                int consumption = (int) Math.ceil(input.fill * torus.fuelConsumption);
                lines.add(Component.literal("→ " + consumption + " mB/t " + input.type.getDisplayNameString())
                        .withStyle(ChatFormatting.GREEN));
            }
            tooltip(graphics, lines, mouseX, mouseY);
            return;
        }

        FluidTankNTM[] tanks = {torus.tanks[0], torus.tanks[1], torus.tanks[2], torus.tanks[3],
                torus.coolantTanks[0], torus.coolantTanks[1]};
        int[][] boxes = {{44, 18}, {62, 18}, {80, 18}, {152, 18}, {188, 46}, {206, 46}};
        for (int i = 0; i < tanks.length; i++) {
            if (this.isHovering(boxes[i][0], boxes[i][1], 16, 52, mouseX, mouseY)) {
                tooltip(graphics, List.of(tanks[i].getTankType().getDisplayName(),
                        Component.literal(tanks[i].getFill() + " / " + tanks[i].getMaxFill() + " mB")), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityFusionTorus torus = this.menu.getTorus();
        if (torus == null || !this.isHovering(RECIPE_X, RECIPE_Y, RECIPE_SIZE, RECIPE_SIZE, event.x(), event.y())) {
            return false;
        }
        GUIScreenRecipeSelector.openSelector(FusionRecipes.INSTANCE, torus.getBlockPos(), torus.fusionModule.recipe, 0,
                com.hbm.items.machine.ItemBlueprints.grabPool(
                        torus.inventory.getStackInSlot(TileEntityFusionTorus.SLOT_BLUEPRINT)), this);
        return true;
    }

    private static FusionRecipe recipe(TileEntityFusionTorus torus) {
        return torus.fusionModule.getRecipe() instanceof FusionRecipe fusion ? fusion : null;
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, text, Optional.empty(), mouseX, mouseY);
    }
}
