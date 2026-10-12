package com.hbm.handler.jei;

import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.blocks.ModSatelliteBlocks;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.saveddata.satellites.SatelliteMiner;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code handler/nei/SatelliteHandler} ("Satellite", catalyst the cargo landing pad): for each mining
 * satellite whose item has a cargo pool, the satellite on the left and every pool entry on the right with its
 * red drop-chance percentage, in the anvil handler's RECYCLING layout ({@code gui_nei_anvil.png}: single slot
 * at (11,23), 6-wide output area at (47,5), operation pill at (29,14), the pad at (30,31)).
 *
 * <p>1.7 also lists {@code sat_miner} and {@code sat_lunar_miner}, but their {@code getCargoForItem} is null (the
 * legacy chips were never registered), so, as there, they produce no recipe. A pool entry whose item this port
 * has not registered keeps its weight in the total but draws no slot.
 */
public class SatelliteRecipeHandler extends HbmRecipeCategory<SatelliteRecipeHandler.Entry> {

    public static final ViewerType<Entry> TYPE = ViewerType.create("hbm", "satellite", Entry.class);

    public record Entry(ItemStack satellite, List<ItemStack> outputs, List<Float> chances) {}

    private final Drawable smallSlot;
    private final Drawable area108;
    private final Drawable opRecycle;

    public SatelliteRecipeHandler() {
        super(TYPE, Component.literal("Satellite"), new ItemStack(ModSatelliteBlocks.sat_dock.get()),
                "gui_nei_anvil.png");
        this.smallSlot = Drawable.texture(tex("gui_nei_anvil.png"), 113, 105, 18, 18);
        this.area108 = Drawable.texture(tex("gui_nei_anvil.png"), 5, 87, 108, 54);
        this.opRecycle = Drawable.texture(tex("gui_nei_anvil.png"), 131, 96, 18, 36);
    }

    /** 1.7 {@code getMiningSatellites}. */
    private static List<ItemStack> miningSatellites() {
        return List.of(ItemSatellite.make(EnumSatType.MINER_ASTRO), ItemSatellite.make(EnumSatType.MINER_LUNAR),
                new ItemStack(ModSatelliteItems.sat_miner.get()), new ItemStack(ModSatelliteItems.sat_lunar_miner.get()));
    }

    public static List<Entry> buildRecipes() {
        List<Entry> recipes = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level == null) return recipes;

        for (ItemStack satellite : miningSatellites()) {
            String poolName = SatelliteMiner.getCargoForItem(satellite);
            if (poolName == null) continue;

            List<ItemPool.Entry> pool = ItemPool.read(level.registryAccess(), poolName);
            int weight = pool.stream().mapToInt(ItemPool.Entry::weight).sum();
            List<ItemStack> outs = new ArrayList<>();
            List<Float> chances = new ArrayList<>();
            for (ItemPool.Entry entry : pool) {
                if (entry.isEmpty()) continue;
                outs.add(dev.golem.core.world.ItemStacks.copyWithCount(entry.template(), entry.max()));
                chances.add(100F * entry.weight() / weight);
            }
            recipes.add(new Entry(satellite, outs, chances));
        }
        return recipes;
    }

    @Override
    public void setRecipe(SlotLayout builder, Entry recipe) {
        builder.addSlot(Role.INPUT, 12, 24).addItemStacks(List.of(recipe.satellite()));

        // 1.7: more than eighteen outputs cycle through the eighteen positions
        int overflow = recipe.outputs().size() / 18;
        for (int i = 0; i < Math.min(recipe.outputs().size(), 18); i++) {
            List<ItemStack> stacks = new ArrayList<>();
            List<Float> chances = new ArrayList<>();
            for (int j = 0; j < overflow + 1 && j * 18 + i < recipe.outputs().size(); j++) {
                stacks.add(recipe.outputs().get(j * 18 + i));
                chances.add(recipe.chances().get(j * 18 + i));
            }
            builder.addSlot(Role.OUTPUT, 48 + 18 * (i % 6), 6 + 18 * (i / 6))
                    .addItemStacks(stacks)
                    .addTooltipCallback((shown, tooltip) -> {
                        int k = indexOf(stacks, shown);
                        if (k >= 0) tooltip.add(Component.literal(((int) (chances.get(k) * 10F) / 10F) + "%")
                                .withStyle(ChatFormatting.RED));
                    });
        }

        builder.addSlot(Role.CRAFTING_STATION, 30, 31)
                .addItemStacks(List.of(new ItemStack(ModSatelliteBlocks.sat_dock.get())));
    }

    private static int indexOf(List<ItemStack> stacks, ItemStack shown) {
        for (int k = 0; k < stacks.size(); k++) {
            if (ItemStack.isSameItem(stacks.get(k), shown)) return k;
        }
        return -1;
    }

    @Override
    public void draw(Entry recipe, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, g, mouseX, mouseY);
        smallSlot.draw(g, 11, 23);
        area108.draw(g, 47, 5);
        opRecycle.draw(g, 29, 14);
    }
}
