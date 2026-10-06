package com.hbm.items.tool;

import com.hbm.items.ModUtilityToolItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemGuideBook}: the guide books; the {@link BookType} was the metadata and is one item per type here
 * ({@code book_guide_<type>}, as ntm-next registers them; History §19es-itemvariant). Right-click opens {@code GUIScreenGuide}. The creative tab shows the RBMK and
 * starter books only (1.7 skipped the test and hadron books in {@code getSubItems}).
 *
 * <p>Page contents are kept as texture paths ({@code hbm:textures/...}) so this class names no client type.
 */
public class ItemGuideBook extends CompatItem implements com.hbm.items.LegacyMeta {

    private final BookType type;

    public ItemGuideBook(Properties properties, BookType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public int legacyMeta() {
        return type.ordinal();
    }

    public static ItemStack of(BookType type) {
        return new ItemStack(ModUtilityToolItems.book_guide.get(type.ordinal()).get());
    }

    /** 1.7 {@code getSubItems}. */
    public static List<ItemStack> creativeStacks() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 1; i < BookType.values().length; i++)
            if (i != 2) list.add(of(BookType.values()[i]));
        return list;
    }

    public static BookType getType(ItemStack stack) {
        return stack.getItem() instanceof ItemGuideBook book ? book.type : BookType.TEST;
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide()) UtilityToolScreens.open(UtilityToolScreens.Kind.GUIDE, player.getItemInHand(hand));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        // 1.7 String.join(" ", resolveKeyArray(title)): the cover's $-separated lines on one line.
        list.add(Component.literal(String.join(" ",
                Component.translatable(getType(stack).title).getString().split("\\$"))));
    }

    public enum BookType {

        TEST("book.test.cover", 2F, statFacTest()),
        RBMK("book.rbmk.cover", 1.5F, statFacRBMK()),
        HADRON("book.error.cover", 1.5F, statFacHadron()),
        STARTER("book.starter.cover", 1.5F, statFacStarter());

        public final List<GuidePage> pages;
        public final float titleScale;
        public final String title;

        BookType(String title, float titleScale, List<GuidePage> pages) {
            this.title = title;
            this.titleScale = titleScale;
            this.pages = pages;
        }

        public static BookType getType(int i) {
            return BookType.values()[Math.abs(i) % BookType.values().length];
        }
    }

    private static final String BOOK = "hbm:textures/gui/book/";
    private static final String ITEMS = "hbm:textures/items/";

    public static List<GuidePage> statFacTest() {
        List<GuidePage> pages = new ArrayList<>();
        pages.add(new GuidePage().addTitle("Title LMAO", 0x800000, 1F)
                .addText("book.test.page1", 2F)
                .addImage(BOOK + "smileman.png", 100, 40, 40));
        pages.add(new GuidePage().addTitle("LA SEXO", 0x800000, 0.5F)
                .addText("book.test.page1", 1.75F)
                .addImage(BOOK + "smileman.png", 100, 40, 40));
        pages.add(new GuidePage().addText("test test"));
        pages.add(new GuidePage().addText("test test test"));
        pages.add(new GuidePage().addText("test test"));
        pages.add(new GuidePage().addText("test test test"));
        pages.add(new GuidePage().addText("test test"));
        return pages;
    }

    //TODO: Make sure this is all correct
    public static List<GuidePage> statFacRBMK() {
        List<GuidePage> pages = new ArrayList<>();
        pages.add(new GuidePage().addTitle("book.rbmk.title1", 0x800000, 1F).addText("book.rbmk.page1", 2F)
                .addImage(BOOK + "rbmk1.png", 90, 80, 60));
        pages.add(new GuidePage().addTitle("book.rbmk.title2", 0x800000, 1F).addText("book.rbmk.page2", 2F)
                .addImage(BOOK + "rbmk2.png", 95, 52, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title3", 0x800000, 1F).addText("book.rbmk.page3", 2F)
                .addImage(BOOK + "rbmk3.png", 95, 88, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title4", 0x800000, 1F).addText("book.rbmk.page4", 2F)
                .addImage(BOOK + "rbmk4.png", 95, 88, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title5", 0x800000, 1F).addText("book.rbmk.page5", 2F)
                .addImage(BOOK + "rbmk5.png", 95, 80, 42));
        pages.add(new GuidePage().addTitle("book.rbmk.title6", 0x800000, 1F).addText("book.rbmk.page6", 2F)
                .addImage(BOOK + "rbmk6.png", 90, 100, 60));
        pages.add(new GuidePage().addTitle("book.rbmk.title7", 0x800000, 1F).addText("book.rbmk.page7", 2F)
                .addImage(BOOK + "rbmk7.png", 95, 52, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title8", 0x800000, 1F).addText("book.rbmk.page8", 2F)
                .addImage(BOOK + "rbmk8.png", 95, 88, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title9", 0x800000, 1F).addText("book.rbmk.page9", 2F)
                .addImage(BOOK + "rbmk9.png", 95, 88, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title10", 0x800000, 1F).addText("book.rbmk.page10", 2F)
                .addImage(BOOK + "rbmk10.png", 95, 88, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title11", 0x800000, 1F).addText("book.rbmk.page11", 2F)
                .addImage(BOOK + "rbmk11.png", 75, 85, 72));
        pages.add(new GuidePage().addTitle("book.rbmk.title12", 0x800000, 1F).addText("book.rbmk.page12", 2F)
                .addImage(BOOK + "rbmk12.png", 90, 80, 60));
        pages.add(new GuidePage().addTitle("book.rbmk.title13", 0x800000, 1F).addText("book.rbmk.page13", 2F));
        pages.add(new GuidePage().addText("book.rbmk.page14", 2F)
                .addImage(BOOK + "rbmk13.png", 70, 103, 78));
        pages.add(new GuidePage().addTitle("book.rbmk.title15", 0x800000, 1F).addText("book.rbmk.page15", 2F)
                .addImage(BOOK + "rbmk15.png", 100, 48, 48));
        pages.add(new GuidePage().addTitle("book.rbmk.title16", 0x800000, 1F).addText("book.rbmk.page16", 2F)
                .addImage(BOOK + "rbmk16.png", 50, 70, 100));
        return pages;
    }

    public static List<GuidePage> statFacHadron() {
        List<GuidePage> pages = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            pages.add(new GuidePage().addTitle("book.error.title" + i, 0x800000, 1F).addText("book.error.page" + i, 2F));
        }
        return pages;
    }

    public static List<GuidePage> statFacStarter() {
        List<GuidePage> pages = new ArrayList<>();
        pages.add(new GuidePage().addTitle("book.starter.title1", 0x800000, 1F).addText("book.starter.page1", 2F)
                .addImage(BOOK + "starter1.png", 96, 101, 56));
        pages.add(new GuidePage().addTitle("book.starter.title2", 0x800000, 1F).addText("book.starter.page2", 2F)
                .addImage(ITEMS + "mask_piss.png", 85, 64, 64)); //meh
        pages.add(new GuidePage().addTitle("book.starter.title3", 0x800000, 1F).addText("book.starter.page3", 2F)
                .addImage(BOOK + "starter3.png", 89, 100, 64));
        pages.add(new GuidePage().addTitle("book.starter.title4", 0x800000, 1F)
                .addText("book.starter.page4", 1.4F, 0, 6, 72)
                .addImage(ITEMS + "template_folder.png", 72, 30, 24, 24)
                .addImage(ITEMS + "stamp_iron_flat.png", 72, 60, 24, 24)
                .addImage(ITEMS + "assembly_template.png", 72, 90, 24, 24)
                .addImage(ITEMS + "chemistry_template.png", 72, 120, 24, 24));
        pages.add(new GuidePage().addTitle("book.starter.title5", 0x800000, 1F).addText("book.starter.page5", 2F));
        pages.add(new GuidePage().addTitle("book.starter.title6", 0x800000, 1F)
                .addText("book.starter.page6a", 2F)
                .addText("book.starter.page6b", 2f, 0, 96, 100)
                .addImage(BOOK + "starter6.png", 9, 89, 84, 36));
        pages.add(new GuidePage()
                .addText("book.starter.page7a", 2F)
                .addText("book.starter.page7b", 2F, 0, 95, 100)
                .addImage(BOOK + "starter7.png", 9, 67, 84, 58));
        pages.add(new GuidePage().addTitle("book.starter.title8", 0x800000, 1F)
                .addText("book.starter.page8a", 2F, 0, -1, 50)
                .addText("book.starter.page8b", 2F, 50, 70, 50)
                .addImage(BOOK + "starter8a.png", 53, 36, 47, 61)
                .addImage(BOOK + "starter8b.png", 0, 102, 47, 61));
        pages.add(new GuidePage().addTitle("book.starter.title9", 0x800000, 1F)
                .addText("book.starter.page9", 2F)
                .addImage(ITEMS + "ingot_polymer.png", 4, 106, 24, 24)
                .addImage(ITEMS + "ingot_desh.png", 28, 130, 24, 24)
                .addImage(ITEMS + "solid_fuel_presto_triplet.png", 52, 106, 24, 24)
                .addImage(ITEMS + "canister_gasoline.png", 76, 130, 24, 24));
        pages.add(new GuidePage().addTitle("book.starter.title10", 0x800000, 1F).addText("book.starter.page10", 2F)
                .addImage(BOOK + "starter10.png", 0, 115, 100, 39));
        pages.add(new GuidePage().addTitle("book.starter.title11", 0x800000, 1F)
                .addText("book.starter.page11", 2F, 0, -1, 60)
                .addImage(BOOK + "starter11a.png", 61, 36, 45, 57)
                .addImage(BOOK + "starter11b.png", 61, 97, 45, 57));
        pages.add(new GuidePage().addTitle("book.starter.title12", 0xfece00, 1F)
                .addText("book.starter.page12a", 3F)
                .addText("book.starter.page12b", 2F, 0, 20, 100));
        pages.add(new GuidePage().addTitle("book.starter.title13", 0x800000, 1F).addText("book.starter.page13", 2F)
                .addImage(BOOK + "starter13.png", 110, 84, 42));
        pages.add(new GuidePage().addTitle("book.starter.title14", 0x800000, 1F)
                .addText("book.starter.page14", 2F, 0, 54, 100)
                .addImage(BOOK + "starter14.png", 34, 100, 46));
        pages.add(new GuidePage().addTitle("book.starter.title15", 0x800000, 1F).addText("book.starter.page15", 2F));
        pages.add(new GuidePage().addTitle("book.starter.title16", 0x800000, 1F).addText("book.starter.page16", 2F));
        pages.add(new GuidePage());
        pages.add(new GuidePage().addTitle("book.starter.title18", 0x800000, 1F).addText("book.starter.page18", 2F)
                .addImage(BOOK + "starter18.png", 10, 69, 100, 100));
        return pages;
    }

    public static class GuidePage {

        public String title;
        public int titleColor;
        public float titleScale;

        public final List<GuideText> texts = new ArrayList<>();
        public final List<GuideImage> images = new ArrayList<>();

        public GuidePage addTitle(String title, int color, float scale) {
            this.title = title;
            this.titleColor = color;
            this.titleScale = scale;
            return this;
        }

        public GuidePage addText(String text) {
            texts.add(new GuideText(text));
            return this;
        }

        public GuidePage addText(String text, float scale) {
            texts.add(new GuideText(text).setScale(scale));
            return this;
        }

        public GuidePage addText(String text, int xOffset, int yOffset, int width) {
            texts.add(new GuideText(text).setSize(xOffset, yOffset, width));
            return this;
        }

        public GuidePage addText(String text, float scale, int xOffset, int yOffset, int width) {
            texts.add(new GuideText(text).setSize(xOffset, yOffset, width).setScale(scale));
            return this;
        }

        public GuidePage addImage(String image, int xOffset, int yOffset, int sizeX, int sizeY) {
            images.add(new GuideImage(image, xOffset, yOffset, sizeX, sizeY));
            return this;
        }

        //xOffset = -1 for automatic centering
        public GuidePage addImage(String image, int yOffset, int sizeX, int sizeY) {
            images.add(new GuideImage(image, -1, yOffset, sizeX, sizeY));
            return this;
        }
    }

    public static class GuideText {
        public String text;
        public float scale = 1F;
        public int xOffset = 0;
        public int yOffset = -1;
        public int width = 100;

        public GuideText(String text) {
            this.text = text;
        }

        public GuideText setScale(float scale) {
            this.scale = scale;
            return this;
        }

        //yOffset = -1, xOffset = 0 for default
        public GuideText setSize(int xOffset, int yOffset, int width) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.width = width;
            return this;
        }
    }

    public record GuideImage(String image, int x, int y, int sizeX, int sizeY) {}
}
