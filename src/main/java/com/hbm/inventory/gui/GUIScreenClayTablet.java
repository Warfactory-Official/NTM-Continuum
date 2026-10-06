package com.hbm.inventory.gui;

import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.inventory.recipes.PedestalRecipes.Input;
import com.hbm.inventory.recipes.PedestalRecipes.PedestalExtraCondition;
import com.hbm.inventory.recipes.PedestalRecipes.PedestalRecipe;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModUtilityToolItems;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1.7 {@code GUIScreenClayTablet}: {@code guide_pedestal.png}, 142x84 (the second tablet uses the sheet's lower
 * half and darker glyphs). The tablet's seed picks one recipe of its set, then each of the nine cells shows the
 * ingredient with a 50 % (set 1: 25 %) chance and a worn glyph otherwise; the moon/sun condition sits bottom
 * right and the output with its name floats above.
 */
public class GUIScreenClayTablet extends CompatScreen {

    protected int xSize = 142;
    protected int ySize = 84;
    protected int guiLeft;
    protected int guiTop;
    protected int tabletMeta = 0;

    protected static final Identifier texture = Ids.of("hbm", "textures/gui/guide_pedestal.png");

    public GUIScreenClayTablet() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    private void blit(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) {
        GuiDraw.blit(g, texture, x, y, u, v, w, h, 256, 256);
    }

    /** 1.7 {@code AStack.extractForNEI}: every stack the ingredient accepts. */
    private static List<ItemStack> extract(Input input) {
        List<ItemStack> list = new ArrayList<>();
        String id = input.id();
        if (id.startsWith("#")) {
            for (ItemStack stack : Compat.itemsInTag(TagKey.create(Registries.ITEM, Ids.parse(id.substring(1))))) {
                stack.setCount(input.count());
                list.add(stack);
            }
            return list;
        }
        Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(id));
        if (item == null || item == Items.AIR) return list;
        list.add(new ItemStack(item, input.count()));
        return list;
    }

    private static ItemStack output(PedestalRecipe recipe) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(recipe.output));
        return item == null ? ItemStack.EMPTY : new ItemStack(item, recipe.outputCount);
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int i, int j, float f) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) tabletMeta = com.hbm.items.LegacyMeta.of(held);

        int tabletOffset = tabletMeta == 1 ? 84 : 0;
        int iconOffset = tabletMeta == 1 ? 16 : 0;
        float revealChance = tabletMeta == 1 ? 0.25F : 0.5F;
        blit(g, guiLeft, guiTop, 0, tabletOffset, xSize, ySize);

        ArrayList<PedestalRecipe> recipeSet = PedestalRecipes.recipeSets[Math.abs(tabletMeta) % PedestalRecipes.recipeSets.length];

        if (!held.isEmpty() && ModUtilityToolItems.TABLET_SEED.has(held) && !recipeSet.isEmpty()) {
            Random rand = new Random(ModUtilityToolItems.TABLET_SEED.getOr(held, 0L));
            PedestalRecipe recipe = recipeSet.get(rand.nextInt(recipeSet.size()));

            if (recipe.extra == PedestalExtraCondition.FULL_MOON) blit(g, guiLeft + 120, guiTop + 62, 142 + iconOffset, 32, 16, 16);
            if (recipe.extra == PedestalExtraCondition.NEW_MOON) blit(g, guiLeft + 120, guiTop + 62, 142 + iconOffset, 48, 16, 16);
            if (recipe.extra == PedestalExtraCondition.SUN) blit(g, guiLeft + 120, guiTop + 62, 142 + iconOffset, 64, 16, 16);

            for (int l = 0; l < 3; l++) {
                for (int r = 0; r < 3; r++) {
                    if (rand.nextFloat() > revealChance) {
                        blit(g, guiLeft + 7 + r * 27, guiTop + 7 + l * 27, 142 + iconOffset, 16, 16, 16);
                    } else {
                        Input ingredient = recipe.input[r + l * 3];
                        if (ingredient == null) {
                            blit(g, guiLeft + 7 + r * 27, guiTop + 7 + l * 27, 142 + iconOffset, 0, 16, 16);
                            continue;
                        }
                        List<ItemStack> inputs = extract(ingredient);
                        if (inputs.isEmpty()) continue; // 1.7 drew ModItems.nothing, which the port lacks
                        ItemStack input = inputs.get((int) (Math.abs(System.currentTimeMillis() / 1000) % inputs.size()));
                        GuiDraw.nextStratum(g);
                        GuiDraw.item(g, input, guiLeft + 7 + r * 27, guiTop + 7 + l * 27);
                        if (input.getCount() > 1) {
                            String count = input.getCount() + "";
                            GuiDraw.text(g, font, count, guiLeft + 7 + r * 27 + 17 - font.width(count),
                                    guiTop + 7 + l * 27 + 9, 0xFFFFFFFF, true);
                        }
                    }
                }
            }

            ItemStack out = output(recipe);
            GuiDraw.item(g, out, guiLeft + xSize / 2 - 8, guiTop - 20);
            if (out.getCount() > 1) {
                String count = out.getCount() + "";
                GuiDraw.text(g, font, count, guiLeft + xSize / 2 - 8 + 17 - font.width(count), guiTop - 20 + 9,
                        0xFFFFFFFF, true);
            }
            String label = out.getHoverName().getString();
            GuiDraw.text(g, font, label, guiLeft + (xSize - font.width(label)) / 2, guiTop - 30, 0xFFFFFFFF, false);

        } else {
            for (int l = 0; l < 3; l++) {
                for (int r = 0; r < 3; r++) {
                    blit(g, guiLeft + 7 + r * 27, guiTop + 7 + l * 27, 142 + iconOffset, 16, 16, 16);
                }
            }
        }
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.key() == 256 || event.matches(mc.options.keyInventory)) {
            mc.setScreen(null);
            return true;
        }
        return false;
    }

    @Override public boolean isPauseScreen() { return false; }
}
