package com.hbm.inventory.gui;

import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.container.ContainerAnvil;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.OverlayType;
import com.hbm.packet.AnvilCraftPayload;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
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
import java.util.Optional;

/**
 * Client screen for the {@link ContainerAnvil}, a port of 1.7 {@code GUIAnvil} ({@code processing/gui_anvil.png}, 176x222).
 * Left: the two smithing slots + output. Bottom: a paged 5×2 <b>construction-recipe browser</b> (mouse
 * wheel or the arrow buttons page by column, a search box filters), with a slide-out side panel listing
 * the selected recipe's inputs/outputs (drawn at half scale, like the original). The hammer button crafts
 * the selection straight from the player's inventory via a {@link AnvilCraftPayload} (LSHIFT = ×64).
 */
public class GUIAnvil extends CompatContainerScreen<ContainerAnvil> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_anvil.png");

    private final int tier;
    private final List<AnvilConstructionRecipe> originList = new ArrayList<>();
    private final List<AnvilConstructionRecipe> recipes = new ArrayList<>();
    private int index;
    private int size;
    private int selection = -1;
    private int lastSize = 0;
    private EditBox search;
    private OverlayType state = OverlayType.NONE;

    public GUIAnvil(ContainerAnvil menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        this.tier = menu.tier;
        this.inventoryLabelY = this.imageHeight - 94;

        for (AnvilConstructionRecipe recipe : AnvilRecipes.getConstruction()) {
            if (recipe.isTierValid(this.tier)) this.originList.add(recipe);
        }
        regenerateRecipes();
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

    private boolean matchState(AnvilConstructionRecipe recipe) {
        return this.state == OverlayType.NONE || recipe.getOverlay() == this.state;
    }

    private void regenerateRecipes() {
        this.recipes.clear();
        for (AnvilConstructionRecipe recipe : this.originList) {
            if (matchState(recipe)) this.recipes.add(recipe);
        }
        resetPaging();
    }

    /** 1.7 filters by the overlay button only while the search box is empty. */
    private void search(String term) {
        term = term.toLowerCase(Locale.ROOT);
        this.recipes.clear();

        if (term.isEmpty()) {
            for (AnvilConstructionRecipe recipe : this.originList) {
                if (matchState(recipe)) this.recipes.add(recipe);
            }
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

        // Button hover highlights (page left/right, craft). 1.7 has the apply-search button at (97,107) commented out.
        if (isOver(mouseX, mouseY, 7, 71, 9, 36)) GuiDraw.blit(graphics, TEXTURE, leftPos + 7, topPos + 71, 176.0F, 186.0F, 9, 36, 256, 256);
        if (isOver(mouseX, mouseY, 106, 71, 9, 36)) GuiDraw.blit(graphics, TEXTURE, leftPos + 106, topPos + 71, 185.0F, 186.0F, 9, 36, 256, 256);
        if (isOver(mouseX, mouseY, 52, 53, 18, 18)) GuiDraw.blit(graphics, TEXTURE, leftPos + 52, topPos + 53, 176.0F, 150.0F, 18, 18, 256, 256);

        // Overlay filter button: smithing, construction and recycling icons at u 200/218/236, hovered one row down
        int filterU = switch (this.state) {
            case SMITHING -> 200;
            case CONSTRUCTION -> 218;
            case RECYCLING -> 236;
            case NONE -> -1;
        };
        if (filterU >= 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 88, topPos + 53, filterU, 0.0F, 18, 18, 256, 256);
            if (isOver(mouseX, mouseY, 88, 53, 18, 18)) GuiDraw.blit(graphics, TEXTURE, leftPos + 88, topPos + 53, filterU, 18.0F, 18, 18, 256, 256);
        }

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
        GuiDraw.text(graphics, this.font, name, 61 - this.font.width(name) / 2, 8, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);

        // The selected recipe's ingredient/output list on the slide-out panel, at half scale (gui-local
        // coordinates: pose is already translated to leftPos/topPos here).
        if (this.selection >= 0 && this.selection < this.recipes.size()) {
            List<Component> list = recipeToList(this.recipes.get(this.selection));
            int longest = 0;
            for (Component s : list) longest = Math.max(longest, this.font.width(s));

            GuiDraw.pushMatrix(graphics);
            GuiDraw.scale(graphics, 0.5F, 0.5F);
            int offset = 0;
            for (Component s : list) {
                GuiDraw.text(graphics, this.font, s, 260, 50 + offset, 0xffffff, false);
                offset += 9;
            }
            GuiDraw.popMatrix(graphics);
            this.lastSize = (int) (longest * 0.5F);
        } else {
            this.lastSize = 0;
        }
    }

    /**
     * 1.7 {@code recipeToList}: yellow headers; an input line turns red when the player's main inventory
     * (36 slots, as 1.7's {@code mainInventory}) holds fewer than the required amount.
     */
    private List<Component> recipeToList(AnvilConstructionRecipe recipe) {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("info.template_in_p").withStyle(ChatFormatting.YELLOW));
        Inventory inventory = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getInventory() : null;
        for (AStack stack : recipe.input) {
            ItemStack display = stack.getCyclingDisplayStack();
            if (display.isEmpty()) {
                list.add(Component.literal("I AM ERROR"));
                continue;
            }
            int amount = 0;
            if (inventory != null) {
                for (int i = 0; i < 36; i++) {
                    ItemStack held = inventory.getItem(i);
                    if (!held.isEmpty() && stack.matchesRecipe(held, true)) amount += held.getCount();
                }
            }
            Component line = Component.literal(">" + stack.stacksize + "x " + display.getHoverName().getString());
            list.add(amount > 0 && amount >= stack.stacksize ? line : line.copy().withStyle(ChatFormatting.RED));
        }
        list.add(Component.empty());
        list.add(Component.translatable("info.template_out_p").withStyle(ChatFormatting.YELLOW));
        for (AnvilOutput out : recipe.output) {
            list.add(Component.literal(">" + out.stack.getCount() + "x " + out.stack.getHoverName().getString()
                    + (out.chance != 1F ? (" (" + (out.chance * 100) + "%)") : "")));
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
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
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
                GolemNetwork.sendToServer(AnvilCraftPayload.CODEC,
                        new AnvilCraftPayload(this.recipes.get(this.selection).id, event.hasShiftDown() ? 1 : 0));
            }
            return true;
        }
        if (isOver(x, y, 88, 53, 18, 18)) { // cycle the overlay filter
            click();
            OverlayType[] values = OverlayType.values();
            this.state = values[(this.state.ordinal() + 1) % values.length];
            regenerateRecipes();
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
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!isOver(mouseX, mouseY, 88, 53, 18, 18)) return;
        String label = switch (this.state) {
            case NONE -> "All recipes";
            case SMITHING -> "Smithing";
            case CONSTRUCTION -> "Construction";
            case RECYCLING -> "Recycling";
        };
        GuiDraw.tooltip(graphics, this.font, List.of(Component.literal(label)), Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected boolean keyPressed(KeyInput event) {
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

    /** 1.7's hit test: {@code left <= x < left + w}, {@code top < y <= top + h}. */
    private boolean isOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return GuiUtil.hover(mouseX, mouseY, leftPos + x, topPos + y, w, h);
    }
}
