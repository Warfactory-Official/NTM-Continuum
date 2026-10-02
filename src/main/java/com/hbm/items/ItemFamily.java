package com.hbm.items;

import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * One item per enum constant - 1.7's "one item behind the metadata", the shape ntm-next's
 * {@code ItemFamily} gives the carts, drones, trains and grenade parts. This is that surface
 * ({@link #get}, {@link #stack}, {@link #is}, {@link #typeOf}, {@link #types}) on Golem's registry.
 */
public final class ItemFamily<E extends Enum<E>, I extends Item> {

    private final Class<E> type;
    private final EnumMap<E, RegistryObject<I>> members;
    private volatile @Nullable Map<Item, E> byItem;

    private ItemFamily(Class<E> type, EnumMap<E, RegistryObject<I>> members) {
        this.type = type;
        this.members = members;
    }

    /**
     * Every constant of {@code type}, each registered by {@code member} — the Reg form, where the member's
     * chain also states its declarations: {@code ItemFamily.of(T.class, t -> REG.item(...).transform(...))}.
     */
    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> of(Class<E> type,
            Function<E, ? extends RegistryObject<I>> member) {
        EnumMap<E, RegistryObject<I>> members = new EnumMap<>(type);
        for (E constant : type.getEnumConstants()) {
            members.put(constant, member.apply(constant));
        }
        return new ItemFamily<>(type, members);
    }

    public RegistryObject<I> handle(E constant) {
        RegistryObject<I> member = members.get(constant);
        if (member == null) throw new IllegalArgumentException(constant + " has no member");
        return member;
    }

    public I get(E constant) {
        return handle(constant).get();
    }

    public ItemStack stack(E constant) {
        return new ItemStack(get(constant));
    }

    public ItemStack stack(E constant, int count) {
        return new ItemStack(get(constant), count);
    }

    public boolean is(ItemStack stack, E constant) {
        return stack.is(get(constant));
    }

    public @Nullable E typeOf(ItemStack stack) {
        return typeOf(stack.getItem());
    }

    public @Nullable E typeOf(ItemLike item) {
        Map<Item, E> table = byItem;
        if (table == null) {
            table = new IdentityHashMap<>();
            for (Map.Entry<E, RegistryObject<I>> entry : members.entrySet()) table.put(entry.getValue().get(), entry.getKey());
            byItem = table;
        }
        return table.get(item.asItem());
    }

    public List<E> types() {
        return new ArrayList<>(members.keySet());
    }

    public List<RegistryObject<I>> handles() {
        return new ArrayList<>(members.values());
    }

    public Class<E> type() {
        return type;
    }
}
