package com.hbm.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One member of a 1.7 {@code ItemEnumMulti} family. 1.7 selected the subtype with item damage; here every enum
 * constant is its own registered item (ntm-next's {@code ItemFamily}, ids {@code <base>_<constant>} or ntm-next's own
 * names), registered through {@link ItemFamily}, and the member knows its constant. Nothing about the subtype is
 * stored on the stack (the {@code hbm:item_variant} int it replaced was removed 2026-10-06, History §19es-itemvariant).
 *
 * <p>{@link #getType}/{@link #getVariant} keep their 1.7 signatures and read the member's constant; {@link #of}
 * answers the family member for another constant.
 */
public class ItemEnumMulti extends dev.golem.core.world.CompatItem implements LegacyMeta {

    public final Class<? extends Enum<?>> theEnum;
    private final Enum<?> type;

    public ItemEnumMulti(Class<? extends Enum<?>> theEnum, Enum<?> type, Properties properties) {
        super(properties);
        this.theEnum = theEnum;
        this.type = type;
    }

    /** This member's constant. */
    public Enum<?> type() {
        return type;
    }

    /** The 1.7 metadata of this member (its constant's ordinal); a runtime value, never saved. */
    public int getVariant(ItemStack stack) {
        return type.ordinal();
    }

    public Enum<?> getType(ItemStack stack) {
        return type;
    }

    @Override
    public int legacyMeta() {
        return type.ordinal();
    }

    /** A stack of the family member for {@code type}: 1.7's {@code stackFromEnum(count, type)}. */
    public ItemStack of(Enum<?> type, int count) {
        return new ItemStack(member(type), count);
    }

    public ItemStack of(Enum<?> type) {
        return of(type, 1);
    }

    /** The registered member for {@code type} (any {@code ItemEnumMulti} family). */
    public static Item member(Enum<?> type) {
        return ItemFamily.byConstant(type);
    }
}
