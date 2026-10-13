package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFusionTorus;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import com.hbm.util.BobMathUtil;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * Fusion torus control panel, ported from 1.7 {@code GUIFusionTorus} (230x244). The left column is stored
 * power, then the three fuel tanks; the right side is the product tank and the two coolant tanks. The centre
 * carries the recipe button, the progress and bonus bars, the two small process LEDs, the three gauges
 * (klystron input, plasma output, fuel consumption) and the three status LEDs (power, coolant, plasma).
 */
public class GUIFusionTorus extends CompatContainerScreen<ContainerFusionTorus> {

    private static final Identifier TEXTURE =
            Ids.of("hbm", "textures/gui/reactors/gui_fusion_torus.png");

    private static final int RECIPE_X = 43;
    private static final int RECIPE_Y = 80;
    private static final int RECIPE_SIZE = 18;
    /** 1.7 hard-codes 123 K, the cooled-machine target. */
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
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, x + 8, y + 80 - p, 230F, (float) (62 - p), 16, p, 256, 256);

        if (torus.fusionModule.progress > 0) {
            int w = (int) Math.ceil(70 * torus.fusionModule.progress);
            GuiDraw.blit(graphics, TEXTURE, x + 98, y + 81, 0F, 244F, w, 6, 256, 256);
        }
        if (torus.fusionModule.bonus > 0) {
            int w = (int) Math.min(Math.ceil(70 * torus.fusionModule.bonus), 70);
            GuiDraw.blit(graphics, TEXTURE, x + 98, y + 91, 0F, 250F, w, 6, 256, 256);
        }

        FusionRecipe recipe = recipe(torus);
        // power LED, coolant LED, plasma LED
        if (recipe != null && torus.power >= recipe.power) led(graphics, x + 160, y + 115);
        if (Math.ceil(torus.temperature) <= COOL_KELVIN) led(graphics, x + 170, y + 115);
        if (torus.didProcess) led(graphics, x + 180, y + 115);

        // left LED
        if (torus.didProcess) {
            GuiDraw.blit(graphics, TEXTURE, x + 87, y + 76, 249F, 0F, 3, 6, 256, 256);
        } else if (recipe != null) {
            GuiDraw.blit(graphics, TEXTURE, x + 87, y + 76, 246F, 0F, 3, 6, 256, 256);
        }
        // right LED; 1.7 tests "torus.power >= torus.power", which is always true, so it lights with any recipe
        if (torus.didProcess) {
            GuiDraw.blit(graphics, TEXTURE, x + 92, y + 76, 249F, 0F, 3, 6, 256, 256);
        } else if (recipe != null) {
            GuiDraw.blit(graphics, TEXTURE, x + 92, y + 76, 246F, 0F, 3, 6, 256, 256);
        }

        double inputGauge = recipe == null ? 0
                : Math.min(((double) torus.klystronEnergy / (double) recipe.ignitionTemp), 1.5) / 1.5D;
        double outputGauge = recipe == null ? 0
                : Math.min(((double) torus.plasmaEnergy / (double) recipe.outputTemp), 1);
        GuiUtil.drawSmoothGauge(graphics, x + 52, y + 124, inputGauge, 5, 2, 1, 0xA00000);
        GuiUtil.drawSmoothGauge(graphics, x + 88, y + 124, outputGauge, 5, 2, 1, 0xA00000);
        GuiUtil.drawSmoothGauge(graphics, x + 124, y + 124, torus.fuelConsumption, 5, 2, 1, 0xA00000);

        // recipe selector icon, 1.7 renderItem(..., 44, 81)
        GuiDraw.item(graphics, recipe != null ? recipe.getIcon()
                        : new ItemStack(ModItems.template_folder.get()),
                x + 44, y + 81);

        NuclearMiscGuiUtil.drawTank(graphics, torus.tanks[0], x + 44, y + 18, 16, 52);
        NuclearMiscGuiUtil.drawTank(graphics, torus.tanks[1], x + 62, y + 18, 16, 52);
        NuclearMiscGuiUtil.drawTank(graphics, torus.tanks[2], x + 80, y + 18, 16, 52);
        NuclearMiscGuiUtil.drawTank(graphics, torus.tanks[3], x + 152, y + 18, 16, 52);
        NuclearMiscGuiUtil.drawTank(graphics, torus.coolantTanks[0], x + 188, y + 46, 16, 52);
        NuclearMiscGuiUtil.drawTank(graphics, torus.coolantTanks[1], x + 206, y + 46, 16, 52);
    }

    private void led(GuiGraphicsExtractor graphics, int x, int y) {
        GuiDraw.blit(graphics, TEXTURE, x, y, 246F, 14F, 8, 8, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, 106 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 35, this.imageHeight - 93, 4210752, false);

        TileEntityFusionTorus torus = this.menu.getTorus();
        if (torus == null) return;
        GuiDraw.text(graphics, this.font, Component.literal("/" + COOL_KELVIN + "K").withStyle(ChatFormatting.AQUA),
                136 + 54, 32, 4210752, false);
        int heat = (int) Math.ceil(torus.temperature);
        Component label = Component.literal(heat + "K")
                .withStyle(heat > COOL_KELVIN ? ChatFormatting.RED : ChatFormatting.AQUA);
        GuiDraw.text(graphics, this.font, label, 166 + 54 - this.font.width(label), 22, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFusionTorus torus = this.menu.getTorus();
        if (torus == null) return;
        FusionRecipe recipe = recipe(torus);

        if (this.isHovering(8, 18, 16, 62, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.energyTooltip(torus.power, torus.getMaxPower()), mouseX, mouseY);
            return;
        }
        int[][] boxes = {{44, 18}, {62, 18}, {80, 18}, {152, 18}, {188, 46}, {206, 46}};
        for (int i = 0; i < boxes.length; i++) {
            if (this.isHovering(boxes[i][0], boxes[i][1], 16, 52, mouseX, mouseY)) {
                tooltip(graphics, GuiUtil.tankTooltip(i < 4 ? torus.tanks[i] : torus.coolantTanks[i - 4]),
                        mouseX, mouseY);
                return;
            }
        }

        if (this.isHovering(43, 115, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(recipe != null
                    ? arrow("-> ", ChatFormatting.GREEN, BobMathUtil.getShortNumber(torus.klystronEnergy) + "KyU / "
                            + BobMathUtil.getShortNumber(recipe.ignitionTemp) + "KyU")
                    : Component.literal("0KyU / 0KyU")), mouseX, mouseY);
            return;
        }
        if (this.isHovering(79, 115, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(recipe != null
                    ? arrow("<- ", ChatFormatting.RED, BobMathUtil.getShortNumber(torus.plasmaEnergy) + "TU / "
                            + BobMathUtil.getShortNumber(recipe.outputTemp) + "TU")
                    : Component.literal("0TU / 0TU")), mouseX, mouseY);
            return;
        }
        if (this.isHovering(115, 115, 18, 18, mouseX, mouseY) && recipe != null && recipe.inputFluid != null) {
            List<Component> lines = new ArrayList<>();
            for (var input : recipe.inputFluid) {
                int consumption = (int) Math.ceil(input.fill * torus.fuelConsumption);
                lines.add(arrow("-> ", ChatFormatting.GREEN, consumption + "mB/t " + input.type.getDisplayNameString()));
            }
            tooltip(graphics, lines, mouseX, mouseY);
            return;
        }

        if (this.isHovering(RECIPE_X, RECIPE_Y, RECIPE_SIZE, RECIPE_SIZE, mouseX, mouseY)) {
            tooltip(graphics, recipe != null ? recipe.print()
                    : List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)),
                    mouseX, mouseY);
        }
    }

    /** 1.7's {@code COLOR + "-> " + RESET + text}: only the arrow is coloured. */
    private static Component arrow(String arrow, ChatFormatting color, String text) {
        return Component.literal(arrow).withStyle(color).append(Component.literal(text).withStyle(ChatFormatting.RESET));
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityFusionTorus torus = this.menu.getTorus();
        if (torus == null) return false;
        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;
        // 1.7 checkClick(43, 80, 18, 18): x in [43, 61), y in (80, 98].
        if (x < RECIPE_X || x >= RECIPE_X + RECIPE_SIZE || y <= RECIPE_Y || y > RECIPE_Y + RECIPE_SIZE) return false;
        GUIScreenRecipeSelector.openSelector(FusionRecipes.INSTANCE, torus.getBlockPos(), torus.fusionModule.recipe, 0,
                ItemBlueprints.grabPool(
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
