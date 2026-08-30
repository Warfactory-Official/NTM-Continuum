package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineAssemblyMachine;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Screen for the assembly machine. Reads the live (client) BlockEntity for the power and progress bars
 * (synced via the BufPacket). The recipe is chosen automatically from the inserted template + ingredient,
 * so there's no in-GUI recipe picker yet (the 1.12.2 selector popup + ghost slots + fluid tanks are
 * deferred — see IMPLEMENTATION.md §18).
 */
public class GUIMachineAssemblyMachine extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineAssemblyMachine> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_assembler.png");

    public GUIMachineAssemblyMachine(ContainerMachineAssemblyMachine menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineAssemblyMachine te = this.menu.getAssembler();
        if (te == null) return;

        // Power bar: 16×61, anchored at the bottom (topPos+79), filling upward, sampled from u=176.
        long max = te.maxPower;
        int p = max <= 0L ? 0 : (int) (te.power * 61L / max);
        if (p > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 79 - p, 176.0F, (float) (61 - p), 16, p, 256, 256);
        }

        // Progress bar: up to 70×16 at (62,126), sampled from (u=176, v=61).
        double progress = te.assemblerModule.progress;
        if (progress > 0) {
            int j = (int) Math.ceil(70 * progress);
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 62, this.topPos + 126, 176.0F, 61.0F, j, 16, 256, 256);
        }

        // Status LEDs (3×6 each): LEFT = a recipe is configured, RIGHT = enough power for it; each turns
        // from amber/orange (u=192, "ready/idle") to green (u=195, "processing"). Ported from the 1.12.2
        // GUI — but where the original kept a manually-picked recipe selected (so the orange standby light
        // showed whenever idle), this port auto-selects and clears the recipe between cycles, so the LEDs
        // key off the inserted blueprint template (a recipe pool is configured) so the orange standby
        // light is actually visible, not just the rare 1-tick gap between auto-selected recipes.
        GenericRecipe recipe = te.assemblerModule.getRecipe();
        boolean configured = recipe != null || ItemBlueprints.grabPool(te.getItem(1)) != null;
        boolean powered = recipe != null ? te.power >= recipe.power : te.power > 0;
        boolean processing = te.didProcess;

        // LEFT LED — recipe configured
        if (processing) {
            led(graphics, 51, 195);
        } else if (configured) {
            led(graphics, 51, 192);
        }
        // RIGHT LED — enough power
        if (processing) {
            led(graphics, 56, 195);
        } else if (configured && powered) {
            led(graphics, 56, 192);
        }

        // Selector button icon: the selected recipe's output, or the template-folder icon when none is set
        // (the 1.12.2 TEMPLATE_FOLDER fallback).
        GuiDraw.item(graphics, recipe != null ? recipe.getIcon() : new ItemStack(ModItems.template_folder.get()),
                this.leftPos + 8, this.topPos + 126);

        // Ghost slots: faint previews of the selected recipe's inputs in still-empty input slots, dimmed by
        // overdrawing the slot background at 50% alpha (the 1.12.2 translucent-slot-quad trick). Inputs map
        // 1:1 onto the 3×4 input grid (machine/menu slots 4..15).
        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < Math.min(recipe.inputItem.length, 12); i++) {
                if (this.menu.slots.get(4 + i).hasItem()) continue;
                int sx = 8 + (i % 3) * 18, sy = 18 + (i / 3) * 18;
                // Cycling preview: a tag ingredient blinks between all matching items (vanilla + HBM, etc.).
                GuiDraw.item(graphics, recipe.inputItem[i].getCyclingDisplayStack(), this.leftPos + sx, this.topPos + sy);
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + sx, this.topPos + sy,
                        (float) sx, (float) sy, 16, 16, 256, 256, 0x80FFFFFF);
            }
        }

    }

    /** The selector button hot-zone (GUI-relative), matching the 1.12.2 layout. */
    private static final int BTN_X = 7, BTN_Y = 125, BTN_W = 18, BTN_H = 18;

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachineAssemblyMachine te = this.menu.getAssembler();
        if (te != null && this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            GUIScreenRecipeSelector.openSelector(AssemblyMachineRecipes.INSTANCE, te.getBlockPos(),
                    te.assemblerModule.recipe, 0, ItemBlueprints.grabPool(te.getItem(1)), this);
            return true;
        }
        return false;
    }

    /** Draws a 3×6 status LED at GUI-relative x / topPos+121 from texture column {@code u} (192 = orange, 195 = green). */
    private void led(GuiGraphicsExtractor graphics, int x, int u) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + x, this.topPos + 121, (float) u, 0.0F, 3, 6, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Power bar hover region (GUI-relative): the full 16×61 bar at (152, 18)..(152, 79).
        TileEntityMachineAssemblyMachine te = this.menu.getAssembler();
        if (te == null) return;
        if (this.isHovering(152, 18, 16, 61, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.maxPower),
                    Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            // Selector button: show the active recipe, or a hint to pick one.
            GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe(te.assemblerModule.recipe);
            if (recipe != null) {
                GuiDraw.tooltip(graphics, this.font, recipe.print(), Optional.empty(), mouseX, mouseY);
            } else {
                GuiDraw.tooltip(graphics, this.font,
                        List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)),
                        Optional.empty(), mouseX, mouseY);
            }
        }
        // (No "Acceptable Upgrades" info panel here — the 1.12.2 assembly GUI has none; the upgrade item's
        // own tooltip [ItemMachineUpgrade + IUpgradeInfoProviderSource] shows this machine's numbers.)
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getAssembler();
    }

}
