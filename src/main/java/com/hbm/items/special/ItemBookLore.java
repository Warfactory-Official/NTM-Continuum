package com.hbm.items.special;

import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.tool.UtilityToolScreens;
import dev.golem.core.data.ModelData;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/*players can have a lil lore, as a treat.
 * nothing super complex like the guidebooks, just some NBT IDs, a bit of I18n and a centered textbox.
 * oh, and also different textures for both the book, the gui, and maybe the 'turn page' button based on what type of 'book' it is.
 * no metadata, i want it to be fairly flexible. probably like the assembly templates
 */
/**
 * 1.7 {@code ItemBookLore}: a lore book named by its key ({@code k}), with a page count ({@code p}), a cover
 * and a title colour ({@code cov_col}/{@code tit_col}) and optional per-page format arguments
 * ({@code p<page>} holding {@code a1}, {@code a2}, ...), all in the {@code hbm:book_lore} compound. The icon is
 * the page sprite under a tinted cover and a tinted title (1.7's three render passes; the two colours ride
 * custom-model-data colours, copied from the compound on the first server tick for a stack built by command).
 */
public class ItemBookLore extends CompatItem {

    /** Every lore book 1.7 creates: key, pages, cover colour, title colour (see {@link #createBook}). */
    public static final Object[][] VARIANTS = {
            // HbmChestContents.generateOfficeBook
            {"resignation_note", 3, 0x6BC8FF, 0x0A0A0A}, {"memo_stocks", 1, 0x6BC8FF, 0x0A0A0A},
            {"memo_schrab_gsa", 2, 0x6BC8FF, 0x0A0A0A}, {"memo_schrab_rd", 4, 0x6BC8FF, 0x0A0A0A},
            {"memo_schrab_nuke", 3, 0x6BC8FF, 0x0A0A0A},
            // HbmChestContents.generateLabBook
            {"bf_bomb_1", 4, 0x1E1E1E, 0x46EA44}, {"bf_bomb_2", 6, 0x1E1E1E, 0x46EA44},
            {"bf_bomb_3", 6, 0x1E1E1E, 0x46EA44}, {"bf_bomb_4", 5, 0x1E1E1E, 0x46EA44},
            {"bf_bomb_5", 9, 0x1E1E1E, 0x46EA44},
            // LootGenerator (signal beacon)
            {"beacon", 12, 0x404040, 0xD637B3},
            // MKUCraftingHandler (the last page takes the slot number as its argument)
            {"book_iodine", 3, 0x271E44, 0xFBFFF4}, {"book_phosphorous", 2, 0x271E44, 0xFBFFF4},
            {"book_dust", 3, 0x271E44, 0xFBFFF4}, {"book_mercury", 2, 0x271E44, 0xFBFFF4},
            {"book_flower", 2, 0x271E44, 0xFBFFF4}, {"book_syringe", 2, 0x271E44, 0xFBFFF4},
    };

    public ItemBookLore(Properties properties) {
        super(properties);
    }

    public static CompoundTag data(ItemStack stack) {
        return ModUtilityToolItems.BOOK_LORE.getOr(stack, null);
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide()) UtilityToolScreens.open(UtilityToolScreens.Kind.BOOK_LORE, player.getItemInHand(hand));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        CompoundTag tag = data(stack);
        if (tag == null) return;
        String key = Tags.getStringOr(tag, "k", "");
        if (key.isEmpty()) return;

        key = "book_lore." + key + ".author";
        String loc = Component.translatable(key).getString();
        if (!loc.equals(key)) list.add(Component.translatable("book_lore.author", loc));
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = data(stack);
        String key = tag == null ? "" : Tags.getStringOr(tag, "k", "");
        return Component.translatable("book_lore." + (key.isEmpty() ? "test" : key) + ".name");
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot,
                                       boolean held) {
        CompoundTag tag = data(stack);
        if (tag == null) return;
        int cover = cover(tag), title = title(tag);
        if (ModelData.color(stack, 0, ~cover) != cover || ModelData.color(stack, 1, ~title) != title) {
            ModelData.setColors(stack, cover, title);
        }
    }

    /** 1.7 {@code getColorFromItemStack} pass 1: {@code cov_col}, or 0x303030. */
    public static int cover(CompoundTag tag) {
        int color = Tags.getIntOr(tag, "cov_col", 0);
        return color > 0 ? color : 0x303030;
    }

    /** 1.7 pass 2: {@code tit_col}, or white. */
    public static int title(CompoundTag tag) {
        int color = Tags.getIntOr(tag, "tit_col", 0);
        return color > 0 ? color : 0xFFFFFF;
    }

    public static ItemStack createBook(String key, int pages, int colorCov, int colorTit) {
        ItemStack book = new ItemStack(ModUtilityToolItems.book_lore.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("k", key);
        tag.putInt("p", pages); // 1.7 setShort; an int, as the legacy tag reader reads ints only
        tag.putInt("cov_col", colorCov);
        tag.putInt("tit_col", colorTit);
        ModUtilityToolItems.BOOK_LORE.set(book, tag);
        ModelData.setColors(book, cover(tag), title(tag));
        return book;
    }

    public static void addArgs(ItemStack book, int page, String... args) {
        CompoundTag tag = data(book);
        if (tag == null) return;
        tag = tag.copy();
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < args.length; i++) {
            data.putString("a" + (i + 1), args[i]);
        }
        tag.put("p" + page, data);
        ModUtilityToolItems.BOOK_LORE.set(book, tag);
    }
}
