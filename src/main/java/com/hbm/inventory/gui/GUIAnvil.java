package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.container.ContainerAnvil;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.packet.AnvilCraftPayload;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Client screen for the {@link ContainerAnvil} — a faithful 26.1 port of the 1.12.2 {@code GUIAnvil}.
 * Left: the two smithing slots + output. Bottom: a paged 5×2 <b>construction-recipe browser</b> (mouse
 * wheel or the arrow buttons page by column, a search box filters), with a slide-out side panel listing
 * the selected recipe's inputs/outputs (drawn at half scale, like the original). The hammer button crafts
 * the selection straight from the player's inventory via a {@link AnvilCraftPayload} (LSHIFT = ×64).
 */
public class GUIAnvil extends dev.golem.core.gui.CompatContainerScreen<ContainerAnvil> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_anvil.png");

    private final int tier;
    private final List<AnvilConstructionRecipe> originList = new ArrayList<>();
    private final List<AnvilConstructionRecipe> recipes = new ArrayList<>();
    private int index;
    private int size;
    private int selection = -1;
    private int lastSize = 0;
    private EditBox search;

    public GUIAnvil(ContainerAnvil menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        this.tier = menu.tier;
        this.inventoryLabelY = this.imageHeight - 94;

        for (AnvilConstructionRecipe recipe : AnvilRecipes.getConstruction()) {
            if (recipe.isTierValid(this.tier)) this.originList.add(recipe);
        }
        this.recipes.addAll(this.originList);
        resetPaging();
    }

    @Override
    protected void init() {
        super.init();
        String prev = this.search == null ? "" : this.search.getValue();
        this.search = new EditBox(this.font, leftPos + 10, topPos + 111, 84, 12, Component.empty());
        this.search.setTextColor(-1);
        this.search.setBordered(false);
        this.search.setMaxLength(25);
        this.search.setValue(prev);
        this.search.setResponder(this::search);
        addRenderableWidget(this.search);
    }

    private void search(String term) {
        term = term.toLowerCase(Locale.ROOT);
        this.recipes.clear();

        if (term.isEmpty()) {
            this.recipes.addAll(this.originList);
        } else {
            for (AnvilConstructionRecipe recipe : this.originList) {
                for (String s : recipeToSearchList(recipe)) {
                    if (s.contains(term)) {
                        this.recipes.add(recipe);
                        break;
                    }
                }
            }
        }
        resetPaging();
    }

    private void resetPaging() {
        this.index = 0;
        this.selection = -1;
        this.size = Math.max(0, (int) Math.ceil((this.recipes.size() - 10) / 2D));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        // Slide-out info panel, widened to fit the selected recipe's (half-scale) text.
        int slide = Mth.clamp(this.lastSize - 42, 0, 1000);
        int mul = 1;
        while (slide >= 51 * mul) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 125 + 51 * mul, topPos + 17, 125.0F, 17.0F, 54, 108, 256, 256);
            mul++;
        }
        GuiDraw.blit(graphics, TEXTURE, leftPos + 125 + slide, topPos + 17, 125.0F, 17.0F, 54, 108, 256, 256);

        if (this.search != null && this.search.isFocused()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 8, topPos + 108, 168.0F, 222.0F, 88, 16, 256, 256);
        }

        // Button hover highlights (page left/right, craft, apply-search).
        if (isOver(mouseX, mouseY, 7, 71, 9, 36)) GuiDraw.blit(graphics, TEXTURE, leftPos + 7, topPos + 71, 176.0F, 186.0F, 9, 36, 256, 256);
        if (isOver(mouseX, mouseY, 106, 71, 9, 36)) GuiDraw.blit(graphics, TEXTURE, leftPos + 106, topPos + 71, 185.0F, 186.0F, 9, 36, 256, 256);
        if (isOver(mouseX, mouseY, 52, 53, 18, 18)) GuiDraw.blit(graphics, TEXTURE, leftPos + 52, topPos + 53, 176.0F, 150.0F, 18, 18, 256, 256);
        if (isOver(mouseX, mouseY, 97, 107, 18, 18)) GuiDraw.blit(graphics, TEXTURE, leftPos + 97, topPos + 107, 176.0F, 168.0F, 18, 18, 256, 256);

        // Recipe grid icons (5 columns × 2 rows, paged by column).
        for (int i = index * 2; i < index * 2 + 10 && i < recipes.size(); i++) {
            int ind = i - index * 2;
            GuiDraw.item(graphics, recipes.get(i).getDisplay(), leftPos + 17 + 18 * (ind / 2), topPos + 72 + 18 * (ind % 2));
        }

        // Overlay badges + the selection highlight sit ON TOP of the item sprites (old zLevel 300).
        GuiDraw.nextStratum(graphics);
        for (int i = index * 2; i < index * 2 + 10 && i < recipes.size(); i++) {
            int ind = i - index * 2;
            int x = leftPos + 16 + 18 * (ind / 2);
            int y = topPos + 71 + 18 * (ind % 2);
            GuiDraw.blit(graphics, TEXTURE, x, y, 18 + 18 * recipes.get(i).getOverlay().ordinal(), 222.0F, 18, 18, 256, 256);
            if (selection == i) {
                GuiDraw.blit(graphics, TEXTURE, x, y, 0.0F, 222.0F, 18, 18, 256, 256);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String name = Component.translatable("container.anvil", this.tier).getString();
        GuiDraw.text(graphics, this.font, name, 61 - this.font.width(name) / 2, 8, 0xFF404040, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);

        // The selected recipe's ingredient/output list on the slide-out panel, at half scale (gui-local
        // coordinates: pose is already translated to leftPos/topPos here).
        if (this.selection >= 0 && this.selection < this.recipes.size()) {
            List<String> list = recipeToList(this.recipes.get(this.selection));
            int longest = 0;
            for (String s : list) longest = Math.max(longest, this.font.width(s));

            GuiDraw.pushMatrix(graphics);
            GuiDraw.scale(graphics, 0.5F, 0.5F);
            int offset = 0;
            for (String s : list) {
                boolean yellow = s.startsWith("§e");
                GuiDraw.text(graphics, this.font, yellow ? s.substring(2) : s, 260, 50 + offset, yellow ? 0xFFFFFF55 : 0xFFFFFFFF, false);
                offset += 9;
            }
            GuiDraw.popMatrix(graphics);
            this.lastSize = (int) (longest * 0.5F);
        } else {
            this.lastSize = 0;
        }
    }

    /** The structured input/output list shown on the side panel (yellow headers marked with §e). */
    private List<String> recipeToList(AnvilConstructionRecipe recipe) {
        List<String> list = new ArrayList<>();
        list.add("§e" + Component.translatable("info.template_in_p").getString());
        for (AStack stack : recipe.input) {
            ItemStack display = stack.getCyclingDisplayStack();
            list.add(display.isEmpty() ? ">I AM ERROR"
                    : ">" + stack.stacksize + "x " + display.getHoverName().getString());
        }
        list.add("");
        list.add("§e" + Component.translatable("info.template_out_p").getString());
        for (AnvilOutput out : recipe.output) {
            list.add(">" + out.stack.getCount() + "x " + out.stack.getHoverName().getString()
                    + (out.chance != 1F ? (" (" + (out.chance * 100) + "%)") : ""));
        }
        return list;
    }

    /** A flat lowercase list of all input variants + outputs, for the search filter. */
    private List<String> recipeToSearchList(AnvilConstructionRecipe recipe) {
        List<String> list = new ArrayList<>();
        for (AStack stack : recipe.input) {
            if (stack instanceof ComparableStack comp) {
                list.add(comp.getDisplayStack().getHoverName().getString().toLowerCase(Locale.ROOT));
            } else {
                for (ItemStack variant : stack.getDisplayStacks()) {
                    list.add(variant.getHoverName().getString().toLowerCase(Locale.ROOT));
                }
            }
        }
        for (AnvilOutput out : recipe.output) {
            list.add(out.stack.getHoverName().getString().toLowerCase(Locale.ROOT));
        }
        return list;
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();

        if (isOver(x, y, 7, 71, 9, 36)) { // page left
            click();
            if (this.index > 0) this.index--;
            return true;
        }
        if (isOver(x, y, 106, 71, 9, 36)) { // page right
            click();
            if (this.index < this.size) this.index++;
            return true;
        }
        if (isOver(x, y, 52, 53, 18, 18)) { // craft the selection
            if (this.selection >= 0 && this.selection < this.recipes.size()) {
                click();
                int globalIndex = AnvilRecipes.getConstruction().indexOf(this.recipes.get(this.selection));
                GolemNetwork.sendToServer(AnvilCraftPayload.CODEC,
                        new AnvilCraftPayload(globalIndex, event.hasShiftDown() ? 1 : 0));
            }
            return true;
        }
        if (isOver(x, y, 97, 107, 18, 18)) { // (re-)apply the search
            click();
            if (this.search != null) search(this.search.getValue());
            return true;
        }
        for (int i = index * 2; i < index * 2 + 10 && i < recipes.size(); i++) {
            int ind = i - index * 2;
            if (isOver(x, y, 16 + 18 * (ind / 2), 71 + 18 * (ind % 2), 18, 18)) {
                this.selection = this.selection == i ? -1 : i;
                click();
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean keyPressed(dev.golem.core.gui.KeyInput event) {
        // While the search box has focus, let it consume typing (incl. the inventory key, e.g. 'e') so the
        // default AbstractContainerScreen behaviour of closing the menu on the inventory key doesn't fire.
        if (this.search != null && this.search.isFocused() && event.key() != 256 && event.key() != 258 /* ESC/TAB pass through */) {
            event.keyPressed(this.search);
            return true;
        }
        return false;
    }

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Page the recipe browser when the cursor is inside the GUI but not over a slot (like the old GUI).
        if (isOver((int) mouseX, (int) mouseY, 0, 0, imageWidth, imageHeight) && this.hoveredSlot == null) {
            if (scrollY > 0 && this.index > 0) this.index--;
            if (scrollY < 0 && this.index < this.size) this.index++;
            return true;
        }
        return false;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private boolean isOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }
}
