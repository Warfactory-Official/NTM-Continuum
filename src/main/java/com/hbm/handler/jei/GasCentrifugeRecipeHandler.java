package com.hbm.handler.jei;

import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the legacy gas centrifuge's real-fluid recipes. */
public final class GasCentrifugeRecipeHandler extends HbmRecipeCategory<GasCentrifugeRecipes.GasCentrifugeRecipe> {

    private final ItemStack[] batteries;
    private final Drawable powerAnimated;
    private final Drawable progressAnimated;
    private final Drawable.Texture highSpeedOverlay;

    public GasCentrifugeRecipeHandler(ViewerType<GasCentrifugeRecipes.GasCentrifugeRecipe> type,
                                       Component title, ItemStack machine) {
        super(type, title, machine, "gui_jei_gas_centrifuge.png", 162, 54, 3, 6);
        Drawable.Texture powerStatic = Drawable.texture(tex("gui_jei_gas_centrifuge.png"), 168, 37, 16, 34);
        powerAnimated = Drawable.animated(powerStatic, 480, Drawable.Direction.TOP, true);
        Drawable.Texture progressStatic = Drawable.texture(tex("gui_jei_gas_centrifuge.png"), 168, 0, 44, 37);
        progressAnimated = Drawable.animated(progressStatic, 150, Drawable.Direction.LEFT, false);
        highSpeedOverlay = Drawable.texture(tex("gui_jei_gas_centrifuge.png"), 184, 37, 16, 16);
        List<ItemStack> list = new ArrayList<>();
        for (var battery : ModBatteryItems.PACKS.values()) list.add(new ItemStack(battery.get()));
        batteries = list.toArray(new ItemStack[0]);
    }

    @Override
    public void setRecipe(SlotLayout builder,
                           GasCentrifugeRecipes.GasCentrifugeRecipe recipe) {
        builder.addSlot(Role.INPUT, 45, 19)
                .addItemStacks(List.of(ItemFluidIcon.make(recipe.input)));
        for (int i = 0; i < recipe.output.length; i++) {
            int x = i % 2 == 0 ? 127 : 145;
            int y = i < 2 ? 10 : 28;
            builder.addSlot(Role.OUTPUT, x, y).addItemStacks(List.of(recipe.output[i]));
        }
        builder.addSlot(Role.INPUT, 1, 37).addItemStacks(List.of(batteries));
        if (recipe.highSpeed) {
            builder.addSlot(Role.INPUT, 24, 20)
                    .addItemStacks(List.of(new ItemStack(ModUpgradeItems.upgrade_gc_speed.get())));
        }
    }

    @Override
    public void draw(GasCentrifugeRecipes.GasCentrifugeRecipe recipe, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, graphics, mouseX, mouseY);
        powerAnimated.draw(graphics, 1, 1);
        progressAnimated.draw(graphics, 72, 12);
        if (recipe.highSpeed) {
            GuiDraw.nextStratum(graphics);
            highSpeedOverlay.draw(graphics, 23, 19);
        }

        String centrifuges = recipe.centrifuges + " G. Cents";
        GuiDraw.text(graphics, Minecraft.getInstance().font, centrifuges,
                50 - textWidth(centrifuges) / 2 - 3, 4, 0xFF00FF00, false);
    }
}
