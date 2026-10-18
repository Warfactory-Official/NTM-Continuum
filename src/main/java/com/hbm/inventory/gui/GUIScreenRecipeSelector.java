package com.hbm.inventory.gui;

import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.packet.ControlEventPayload;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The in-GUI recipe selector popup, a 26.1 port of HBM's 1.12.2 {@code GUIScreenRecipeSelector}.
 * Opened from a machine screen via {@link #openSelector}, it shows the machine's recipes (filtered to the
 * non-pooled ones plus any matching the inserted blueprint's pool), with a search box, scroll paging, a
 * recipe tooltip on hover, and a "current selection" slot. On close it sends the chosen recipe back to the
 * server via a {@link ControlEventPayload} (the {@code index}/{@code selection} tag the machine's
 * {@link com.hbm.interfaces.IControlReceiver#receiveControl} expects) and returns to the previous screen.
 */
public class GUIScreenRecipeSelector extends CompatScreen {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_recipe_selector.png");
    public static final String NULL_SELECTION = "null";

    private final int xSize = 176;
    private final int ySize = 132;
    private int guiLeft;
    private int guiTop;

    private final GenericRecipes recipeSet;
    private final List<GenericRecipe> recipes = new ArrayList<>();
    private EditBox search;
    private int pageIndex;
    private int size;
    private String selection;

    private final int index;
    private final BlockPos tilePos;
    private final Screen previousScreen;
    private final String installedPool;

    public static void openSelector(GenericRecipes recipeSet, BlockPos tilePos, String selection, int index,
                                    String installedPool, Screen previousScreen) {
        Minecraft.getInstance().setScreen(new GUIScreenRecipeSelector(recipeSet, tilePos, selection, index, installedPool, previousScreen));
    }

    public GUIScreenRecipeSelector(GenericRecipes recipeSet, BlockPos tilePos, String selection, int index,
                                   String installedPool, Screen previousScreen) {
        super(Component.translatable("gui.recipe.selector"));
        this.recipeSet = recipeSet;
        this.tilePos = tilePos;
        this.selection = selection == null ? NULL_SELECTION : selection;
        this.index = index;
        this.installedPool = installedPool;
        this.previousScreen = previousScreen;
        regenerateRecipes();
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        this.search = new EditBox(this.font, guiLeft + 28, guiTop + 111, 102, 12, Component.empty());
        this.search.setTextColor(-1);
        this.search.setBordered(false);
        this.search.setMaxLength(32);
        this.search.setResponder(this::search);
        addRenderableWidget(this.search);
    }

    private void regenerateRecipes() {
        this.recipes.clear();
        for (GenericRecipe recipe : recipeSet.recipeOrderedList) {
            if (!recipe.isPooled() || (installedPool != null && recipe.isPartOfPool(installedPool))) this.recipes.add(recipe);
        }
        resetPaging();
    }

    private void search(String term) {
        this.recipes.clear();
        if (term.isEmpty()) {
            regenerateRecipes();
            return;
        }
        for (GenericRecipe recipe : recipeSet.recipeOrderedList) {
            if (recipe.matchesSearch(term) && (!recipe.isPooled() || (installedPool != null && recipe.isPartOfPool(installedPool)))) {
                this.recipes.add(recipe);
            }
        }
        resetPaging();
    }

    private void resetPaging() {
        this.pageIndex = 0;
        this.size = Math.max(0, (int) Math.ceil((this.recipes.size() - 40) / 8D));
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0.0F, 0.0F, xSize, ySize, 256, 256);

        if (this.search.isFocused()) {
            GuiDraw.blit(graphics, TEXTURE, guiLeft + 26, guiTop + 108, 0.0F, 132.0F, 106, 16, 256, 256);
        }

        // Button hover highlights (page up/down, close, clear search, focus hint).
        hoverBlit(graphics, mouseX, mouseY, 152, 18, 176, 0);
        hoverBlit(graphics, mouseX, mouseY, 152, 36, 176, 16);
        hoverBlit(graphics, mouseX, mouseY, 152, 90, 176, 32);
        hoverBlit(graphics, mouseX, mouseY, 134, 108, 176, 48);
        hoverBlit(graphics, mouseX, mouseY, 8, 108, 176, 64);

        // Selection highlight behind the matching grid cell.
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40 && i < recipes.size(); i++) {
            int ind = i - pageIndex * 8;
            if (recipes.get(i).getInternalName().equals(this.selection)) {
                GuiDraw.blit(graphics, TEXTURE, guiLeft + 7 + 18 * (ind % 8), guiTop + 17 + 18 * (ind / 8), 192.0F, 0.0F, 18, 18, 256, 256);
            }
        }

        // Recipe icons grid (8 wide × 5 tall).
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40 && i < recipes.size(); i++) {
            int ind = i - pageIndex * 8;
            GuiDraw.item(graphics, recipes.get(i).getIcon(), guiLeft + 8 + 18 * (ind % 8), guiTop + 18 + 18 * (ind / 8));
        }

        // Current selection icon.
        GenericRecipe selected = recipeSet.getRecipe(selection);
        if (selected != null) GuiDraw.item(graphics, selected.getIcon(), guiLeft + 152, guiTop + 72);
    }

    private void hoverBlit(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int u, int v) {
        if (isOver(mouseX, mouseY, x, y, 16, 16)) {
            GuiDraw.blit(graphics, TEXTURE, guiLeft + x, guiTop + y, (float) u, (float) v, 16, 16, 256, 256);
        }
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

        // Recipe tooltips over the grid.
        if (isOver(mouseX, mouseY, 7, 17, 144, 90)) {
            for (int i = pageIndex * 8; i < pageIndex * 8 + 40 && i < recipes.size(); i++) {
                int ind = i - pageIndex * 8;
                int ix = 7 + 18 * (ind % 8), iy = 17 + 18 * (ind / 8);
                if (isOver(mouseX, mouseY, ix, iy, 18, 18)) {
                    GuiDraw.tooltip(graphics, this.font, recipes.get(i).print(), Optional.empty(), mouseX, mouseY);
                }
            }
        }

        // Tooltip over the current selection icon.
        GenericRecipe selected = recipeSet.getRecipe(selection);
        if (selected != null && isOver(mouseX, mouseY, 151, 71, 18, 18)) {
            GuiDraw.tooltip(graphics, this.font, selected.print(), Optional.empty(), mouseX, mouseY);
        }
        if (isOver(mouseX, mouseY, 152, 90, 16, 16)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.literal("Close").withStyle(ChatFormatting.YELLOW)), Optional.empty(), mouseX, mouseY);
        }
        if (isOver(mouseX, mouseY, 134, 108, 16, 16)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.literal("Clear search").withStyle(ChatFormatting.YELLOW)), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClickedAfterWidgets(MouseInput event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();

        if (isOver(x, y, 152, 18, 16, 16)) { GuiUtil.playClickSound(); if (pageIndex > 0) pageIndex--; return true; }   // page up
        if (isOver(x, y, 152, 36, 16, 16)) { GuiUtil.playClickSound(); if (pageIndex < size) pageIndex++; return true; } // page down

        if (isOver(x, y, 134, 108, 16, 16)) { // clear search
            this.search.setValue("");
            this.setFocused(this.search);
            return true;
        }

        // Grid selection (click again to deselect).
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40 && i < recipes.size(); i++) {
            int ind = i - pageIndex * 8;
            int ix = 7 + 18 * (ind % 8), iy = 17 + 18 * (ind / 8);
            if (isOver(x, y, ix, iy, 18, 18)) {
                String clicked = recipes.get(i).getInternalName();
                this.selection = clicked.equals(this.selection) ? NULL_SELECTION : clicked;
                GuiUtil.playClickSound();
                return true;
            }
        }

        if (isOver(x, y, 151, 71, 18, 18) && !NULL_SELECTION.equals(this.selection)) { // deselect current
            this.selection = NULL_SELECTION;
            GuiUtil.playClickSound();
            return true;
        }

        if (isOver(x, y, 152, 90, 16, 16)) { onClose(); return true; } // close
        return false;
    }

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && pageIndex > 0) pageIndex--;
        if (scrollY < 0 && pageIndex < size) pageIndex++;
        return true;
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        if (event.key() == 257 || event.key() == 335) { // ENTER / numpad enter toggles search focus
            this.setFocused(this.search.isFocused() ? null : this.search);
            return true;
        }
        if (!this.search.isFocused() && this.minecraft != null
                && event.matches(this.minecraft.options.keyInventory)) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        CompoundTag data = new CompoundTag();
        data.putInt("index", this.index);
        data.putString("selection", this.selection);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(tilePos, data));
        GuiUtil.playClickSound();
        if (this.minecraft != null) this.minecraft.setScreen(previousScreen);
    }

    private boolean isOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= guiLeft + x && mouseX < guiLeft + x + w && mouseY >= guiTop + y && mouseY < guiTop + y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
