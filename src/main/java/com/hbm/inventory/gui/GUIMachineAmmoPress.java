package com.hbm.inventory.gui;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.ContainerMachineAmmoPress;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes.AmmoPressRecipe;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.TileEntityMachineAmmoPress;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The 176x200 positional recipe browser and inventory screen from the old Ammo Press. */
public class GUIMachineAmmoPress extends CompatContainerScreen<ContainerMachineAmmoPress> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm",
            "textures/gui/processing/gui_ammo_press.png");

    private final List<AmmoPressRecipe> recipes = new ArrayList<>();
    private int page;
    private int pageCount;
    private String selection;
    private EditBox search;

    public GUIMachineAmmoPress(ContainerMachineAmmoPress menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 200);
        this.selection = menu.getPress() == null ? "" : menu.getPress().selectedRecipe;
        this.recipes.addAll(AmmoPressRecipes.getRecipes());
        resetPaging();
    }

    @Override
    protected void init() {
        super.init();
        String previous = search == null ? "" : search.getValue();
        search = new EditBox(font, leftPos + 10, topPos + 75, 66, 12, Component.empty());
        search.setTextColor(-1);
        search.setBordered(false);
        search.setMaxLength(25);
        search.setValue(previous);
        search.setResponder(this::filter);
        addRenderableWidget(search);
    }

    private void filter(String term) {
        term = term.toLowerCase(Locale.US);
        recipes.clear();
        for (AmmoPressRecipe recipe : AmmoPressRecipes.getRecipes()) {
            if (term.isEmpty() || recipe.output.getHoverName().getString().toLowerCase(Locale.US).contains(term)) {
                recipes.add(recipe);
            }
        }
        resetPaging();
    }

    private void resetPaging() {
        page = 0;
        // Deliberately preserves the old four-column page math: 12 entries per page, three rows.
        pageCount = Math.max(0, (int) Math.ceil((recipes.size() - 12) / 3D));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        if (search != null && search.isFocused()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 8, topPos + 72, 176F, 54F, 70, 16, 256, 256);
        }

        hoverBlit(graphics, mouseX, mouseY, 7, 17, 176, 0);
        hoverBlit(graphics, mouseX, mouseY, 88, 17, 185, 0);

        for (int i = page * 3; i < page * 3 + 12 && i < recipes.size(); i++) {
            int displayed = i - page * 3;
            int x = leftPos + 17 + 18 * (displayed / 3);
            int y = topPos + 18 + 18 * (displayed % 3);
            GuiDraw.item(graphics, recipes.get(i).output, x, y);
        }

        GuiDraw.nextStratum(graphics);
        for (int i = page * 3; i < page * 3 + 12 && i < recipes.size(); i++) {
            int displayed = i - page * 3;
            int x = leftPos + 16 + 18 * (displayed / 3);
            int y = topPos + 17 + 18 * (displayed % 3);
            GuiDraw.blit(graphics, TEXTURE, x, y,
                    selection.equals(recipes.get(i).id) ? 194F : 212F,
                    0F, 18, 18, 256, 256);
            drawCount(graphics, recipes.get(i).output, x + 1, y + 1);
        }

        TileEntityMachineAmmoPress press = menu.getPress();
        AmmoPressRecipe recipe = AmmoPressRecipes.byId(selection);
        if (press == null || recipe == null) return;

        for (int i = 0; i < 9; i++) {
            AStack ingredient = recipe.input[i];
            if (ingredient == null || !press.inventory.getStackInSlot(i).isEmpty()) continue;
            GuiDraw.item(graphics, ingredient.getCyclingDisplayStack(),
                    leftPos + 116 + 18 * (i % 3), topPos + 18 + 18 * (i / 3));
        }
        GuiDraw.nextStratum(graphics);
        for (int i = 0; i < 9; i++) {
            AStack ingredient = recipe.input[i];
            if (ingredient == null || !press.inventory.getStackInSlot(i).isEmpty()) continue;
            GuiDraw.blit(graphics, TEXTURE, leftPos + 116 + 18 * (i % 3), topPos + 18 + 18 * (i / 3),
                    116F + 18 * (i % 3), 18F + 18 * (i / 3), 18, 18, 256, 256, 0x80FFFFFF);
            drawCount(graphics, ingredient.getCyclingDisplayStack(),
                    leftPos + 116 + 18 * (i % 3), topPos + 18 + 18 * (i / 3));
        }
    }

    private void drawCount(GuiGraphicsExtractor graphics, net.minecraft.world.item.ItemStack stack, int x, int y) {
        if (stack.isEmpty() || stack.getCount() <= 1) return;
        String count = Integer.toString(stack.getCount());
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, x + 17F, y + 16F);
        GuiDraw.scale(graphics, 0.5F, 0.5F);
        GuiDraw.text(graphics, font, count, -font.width(count), 0, 0xFFFFFFFF, true);
        GuiDraw.popMatrix(graphics);
    }

    private void hoverBlit(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int u, int v) {
        if (isOver(mouseX, mouseY, x, y, 9, 54)) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + y, u, v, 9, 54, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int i = page * 3; i < page * 3 + 12 && i < recipes.size(); i++) {
            int displayed = i - page * 3;
            if (isOver(mouseX, mouseY, 16 + 18 * (displayed / 3), 17 + 18 * (displayed % 3), 18, 18)) {
                GuiDraw.tooltip(graphics, font, List.of(recipes.get(i).output.getHoverName()), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        int x = (int) event.x();
        int y = (int) event.y();

        if (isOver(x, y, 7, 17, 9, 54)) {
            GuiUtil.playClickSound();
            if (page > 0) page--;
            return true;
        }
        if (isOver(x, y, 88, 17, 9, 54)) {
            GuiUtil.playClickSound();
            if (page < pageCount) page++;
            return true;
        }

        for (int i = page * 3; i < page * 3 + 12 && i < recipes.size(); i++) {
            int displayed = i - page * 3;
            if (!isOver(x, y, 16 + 18 * (displayed / 3), 17 + 18 * (displayed % 3), 18, 18)) continue;
            String newSelection = recipes.get(i).id;
            selection = selection.equals(newSelection) ? "" : newSelection;
            CompoundTag data = new CompoundTag();
            data.putString("selection", newSelection);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                    new ControlEventPayload(menu.getPress().getBlockPos(), data));
            GuiUtil.playClickSound();
            return true;
        }
        return false;
    }

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos && mouseX < leftPos + imageWidth && mouseY >= topPos && mouseY < topPos + imageHeight) {
            if (scrollY > 0 && page > 0) page--;
            if (scrollY < 0 && page < pageCount) page++;
            return true;
        }
        return false;
    }

    @Override
    protected boolean keyPressed(KeyInput event) {
        // AbstractContainerScreen otherwise treats the inventory key as "close" before a focused field sees it.
        if (search != null && search.isFocused() && event.key() != 256 && event.key() != 258) {
            event.keyPressed(search);
            return true;
        }
        return false;
    }

    private boolean isOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }
}
