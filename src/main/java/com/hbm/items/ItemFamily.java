package com.hbm.items;

import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

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

    /** Registers {@code base + "_" + constant} for every constant of {@code type}. */
    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> of(DeferredRegister.Items items, String base,
            Class<E> type, BiFunction<Item.Properties, E, ? extends I> factory, UnaryOperator<Item.Properties> props) {
        return of(items, type, constant -> base + "_" + constant.name().toLowerCase(Locale.ROOT), factory, props);
    }

    /** Registers every constant of {@code type} under the id {@code name} gives it. */
    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> of(DeferredRegister.Items items,
            Class<E> type, Function<E, String> name, BiFunction<Item.Properties, E, ? extends I> factory,
            UnaryOperator<Item.Properties> props) {
        return of(items, type.getEnumConstants(), type, name, factory, props);
    }

    /** As {@link #of(DeferredRegister.Items, Class, Function, BiFunction, UnaryOperator)} over a subset of constants. */
    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> of(DeferredRegister.Items items, E[] constants,
            Class<E> type, Function<E, String> name, BiFunction<Item.Properties, E, ? extends I> factory,
            UnaryOperator<Item.Properties> props) {
        EnumMap<E, RegistryObject<I>> members = new EnumMap<>(type);
        for (E constant : constants) {
            members.put(constant, items.registerItem(name.apply(constant), p -> factory.apply(p, constant), props));
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
