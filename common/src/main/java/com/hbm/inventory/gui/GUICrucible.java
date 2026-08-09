package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCrucible;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.TileEntityCrucible;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Client screen for the crucible. Reads heat/progress and the two molten-stack columns off the synced
 * client {@link TileEntityCrucible} (BufPacket): the waste column (left), the recipe column (middle),
 * the progress/heat bars and the recipe-selector button — texture layout matches the 1.12.2
 * {@code gui_crucible.png} (176×214). The molten columns blit the molten-liquid texture region from
 * {@code gui_crucible.png} (u=176 normal / u=210 additive) tinted by the material's {@code moltenColor},
 * exactly like the 1.12.2 {@code drawStack}.
 */
public class GUICrucible extends AbstractContainerScreen<ContainerCrucible> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/processing/gui_crucible.png");

    // Selector button (GUI-relative), matching the old click region.
    private static final int BTN_X = 106, BTN_Y = 80, BTN_W = 18, BTN_H = 18;

    public GUICrucible(ContainerCrucible menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 214);
    }

    /** The 1.12.2 GUI drew no title (the texture carries it) — only the player-inventory label. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityCrucible te = this.menu.getCrucible();
        if (te == null) return;

        // Progress bar: 33×5 at (126, 82) from tex (176, 0).
        int pGauge = te.progress * 33 / TileEntityCrucible.processTime;
        if (pGauge > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 126, this.topPos + 82, 176.0F, 0.0F, pGauge, 5, 256, 256);

        // Heat bar: 33×5 at (126, 91) from tex (176, 5).
        int hGauge = te.heat * 33 / TileEntityCrucible.maxHeat;
        if (hGauge > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 126, this.topPos + 91, 176.0F, 5.0F, hGauge, 5, 256, 256);

        // Recipe selector icon.
        GenericRecipe recipe = CrucibleRecipes.INSTANCE.recipeNameMap.get(te.recipe);
        graphics.item(recipe != null ? recipe.getIcon() : new ItemStack(ModItems.template_folder.get()), this.leftPos + 107, this.topPos + 81);

        // Molten columns: waste at x=17, recipe at x=62 — 34 wide, up to 79 tall, bottom at y=97.
        drawStack(graphics, te.wasteStack, TileEntityCrucible.wasteZCapacity, 17, 97);
        drawStack(graphics, te.recipeStack, TileEntityCrucible.recipeZCapacity, 62, 97);
    }

    /**
     * The molten column: each stacked material blits a vertical slice of the molten-liquid texture from
     * {@code gui_crucible.png}, tinted by its {@code moltenColor} — bottom-anchored at v=89 so the texture
     * fills upward as material accumulates (the 1.12.2 {@code drawStack}). Additives sample the second
     * molten texture (u=176+34) to read visually distinct.
     */
    private void drawStack(GuiGraphicsExtractor graphics, List<Mats.MaterialStack> stack, int capacity, int x, int y) {
        if (stack.isEmpty()) return;

        int lastHeight = 0;
        int lastQuant = 0;

        for (Mats.MaterialStack sta : stack) {
            int targetHeight = (int) ((lastQuant + (long) sta.amount) * 79 / capacity);
            if (lastHeight != targetHeight) {
                int offset = sta.material.smeltable == SmeltingBehavior.ADDITIVE ? 34 : 0; // additives use a different texture
                int tint = 0xFF000000 | (sta.material.moltenColor & 0xFFFFFF);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                        this.leftPos + x, this.topPos + y - targetHeight,
                        176.0F + offset, 89.0F - targetHeight,
                        34, targetHeight - lastHeight, 256, 256, tint);
            }
            lastQuant += sta.amount;
            lastHeight = targetHeight;
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        TileEntityCrucible te = this.menu.getCrucible();
        if (te == null) return;

        if (this.isHovering(16, 17, 36, 81, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, stackInfo(te.wasteStack), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(61, 17, 36, 81, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, stackInfo(te.recipeStack), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(125, 81, 34, 7, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(Component.literal(
                    String.format(Locale.US, "%,d", te.progress) + " / " + String.format(Locale.US, "%,d", TileEntityCrucible.processTime) + " TU")), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(125, 90, 34, 7, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(Component.literal(
                    String.format(Locale.US, "%,d", te.heat) + " / " + String.format(Locale.US, "%,d", TileEntityCrucible.maxHeat) + " TU")), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            if (CrucibleRecipes.INSTANCE.recipeNameMap.get(te.recipe) instanceof CrucibleRecipes.CrucibleRecipe recipe) {
                graphics.setTooltipForNextFrame(this.font, recipe.print(), Optional.empty(), mouseX, mouseY);
            } else {
                graphics.setTooltipForNextFrame(this.font, List.of(
                        Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    private static List<Component> stackInfo(List<Mats.MaterialStack> stack) {
        List<Component> list = new ArrayList<>();
        if (stack.isEmpty()) list.add(Component.literal("Empty").withStyle(ChatFormatting.RED));
        for (Mats.MaterialStack sta : stack) {
            list.add(Component.literal(sta.material.getDisplayName() + ": " + Mats.formatAmount(sta.amount, false)).withStyle(ChatFormatting.YELLOW));
        }
        return list;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        TileEntityCrucible te = this.menu.getCrucible();
        if (te != null && this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            GUIScreenRecipeSelector.openSelector(CrucibleRecipes.INSTANCE, te.getBlockPos(), te.recipe, 0, null, this);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
