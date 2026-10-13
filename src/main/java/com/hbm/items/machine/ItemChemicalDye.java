package com.hbm.items.machine;

import dev.golem.core.world.CompatItem;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One flattened variant of the legacy 16-value chemical dye item. */
public class ItemChemicalDye extends CompatItem {

    public final EnumChemDye dye;
    private final String languagePrefix;

    public ItemChemicalDye(Item.Properties properties, EnumChemDye dye) {
        this(properties, dye, "chemical_dye");
    }

    protected ItemChemicalDye(Item.Properties properties, EnumChemDye dye, String languagePrefix) {
        super(properties);
        this.dye = dye;
        this.languagePrefix = languagePrefix;
    }

    /** The one stack represented by this flattened registry item. */
    public ItemStack stack(int count) {
        return new ItemStack(this, count);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.hbm." + languagePrefix + "." + dye.translationName());
    }

    public enum EnumChemDye {
        BLACK(1973019, "Black"),
        RED(11743532, "Red"),
        GREEN(3887386, "Green"),
        BROWN(5320730, "Brown"),
        BLUE(2437522, "Blue"),
        PURPLE(8073150, "Purple"),
        CYAN(2651799, "Cyan"),
        SILVER(11250603, "LightGray"),
        GRAY(4408131, "Gray"),
        PINK(14188952, "Pink"),
        LIME(4312372, "Lime"),
        YELLOW(14602026, "Yellow"),
        LIGHTBLUE(6719955, "LightBlue"),
        MAGENTA(12801229, "Magenta"),
        ORANGE(15435844, "Orange"),
        WHITE(15790320, "White");

        public static final EnumChemDye[] VALUES = values();

        public final int color;
        public final String dictName;

        EnumChemDye(int color, String dictName) {
            this.color = color;
            this.dictName = dictName;
        }

        public String translationName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
