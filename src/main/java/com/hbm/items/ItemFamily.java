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
    /** The constants in registration (creative) order. */
    private final List<E> order;
    private volatile @Nullable Map<Item, E> byItem;

    private ItemFamily(Class<E> type, EnumMap<E, RegistryObject<I>> members, List<E> order) {
        this.type = type;
        this.members = members;
        this.order = order;
    }

    /**
     * Every constant of {@code type}, each registered by {@code member} — the Reg form, where the member's
     * chain also states its declarations: {@code ItemFamily.of(T.class, t -> REG.item(...).transform(...))}.
     */
    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> of(Class<E> type,
            Function<E, ? extends RegistryObject<I>> member) {
        return of(type, type.getEnumConstants(), member);
    }

    /**
     * As {@link #of(Class, Function)}, over the constants 1.7 gave items (in creative order); the rest have no
     * member. Every member is also findable by its constant alone ({@link #byConstant}) unless two families
     * share an enum.
     */
    public static <E extends Enum<E>, I extends Item> ItemFamily<E, I> of(Class<E> type, E[] constants,
            Function<E, ? extends RegistryObject<I>> member) {
        EnumMap<E, RegistryObject<I>> members = new EnumMap<>(type);
        List<E> order = new ArrayList<>();
        for (E constant : constants) {
            order.add(constant);
            RegistryObject<I> handle = member.apply(constant);
            members.put(constant, handle);
            synchronized (BY_CONSTANT) {
                BY_CONSTANT.put(constant, BY_CONSTANT.containsKey(constant) ? AMBIGUOUS : handle);
            }
        }
        return new ItemFamily<>(type, members, order);
    }

    private static final Object AMBIGUOUS = new Object();
    private static final Map<Enum<?>, Object> BY_CONSTANT = new IdentityHashMap<>();

    /**
     * The item registered for {@code constant} by whichever family owns its enum - the replacement for 1.7's
     * {@code new ItemStack(item, 1, constant.ordinal())}. Throws if no family, or more than one, registered it.
     */
    public static Item byConstant(Enum<?> constant) {
        Object handle;
        synchronized (BY_CONSTANT) {
            handle = BY_CONSTANT.get(constant);
        }
        if (handle == null || handle == AMBIGUOUS) {
            throw new IllegalArgumentException(constant.getDeclaringClass().getSimpleName() + "." + constant
                    + (handle == null ? " has no item" : " belongs to more than one item family"));
        }
        return (Item) ((RegistryObject<?>) handle).get();
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

    /** One stack of every member, in registration (creative) order. */
    public List<ItemStack> stacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (E constant : order) stacks.add(new ItemStack(members.get(constant).get()));
        return stacks;
    }

    public List<E> types() {
        return new ArrayList<>(order);
    }

    public List<RegistryObject<I>> handles() {
        List<RegistryObject<I>> handles = new ArrayList<>();
        for (E constant : order) handles.add(members.get(constant));
        return handles;
    }

    public Class<E> type() {
        return type;
    }
}
