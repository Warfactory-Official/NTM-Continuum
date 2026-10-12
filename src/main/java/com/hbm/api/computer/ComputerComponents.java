package com.hbm.api.computer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Another mod's 1.7 OpenComputers components (a block entity's {@code getComponentName} and its
 * {@code @Callback} methods), served to computers the way HBM serves its own: as CC: Tweaked peripherals, on every
 * node that has CC (HBM's {@code compat.computercraft.Peripherals}). The registering mod needs no CC types: a method is
 * a plain function of the block entity and the Lua arguments, and its answer is an array of plain values (numbers,
 * strings, booleans, null, arrays, lists, maps), as 1.7's {@code Object[]} callbacks returned.
 *
 * <p>Methods run on the server thread (a computer's call waits for the next server tick), so they may read the block
 * entity and the world freely. A method that rejects its arguments throws {@link IllegalArgumentException}; the
 * message is the Lua error, as 1.7's {@code Arguments.checkString} threw. The peripheral's type is {@code name}
 * ({@code peripheral.find("ntm_stardar")}), reachable on every side of the given blocks. Register during
 * {@code declare()}; plain HBM registers nothing here.
 */
public final class ComputerComponents {

    /** One callback: the block entity, the Lua arguments in order; the values Lua receives. */
    @FunctionalInterface
    public interface Method<BE extends BlockEntity> {
        @Nullable Object[] call(BE machine, Object[] arguments);
    }

    /** One component: the block entity class (subclasses answer too), its name, its methods in order, its blocks. */
    public record Component<BE extends BlockEntity>(Class<BE> type, String name, Map<String, Method<BE>> methods,
                                                    List<Supplier<? extends Block>> blocks) {
        public Component {
            methods = Collections.unmodifiableMap(new LinkedHashMap<>(methods));
            blocks = List.copyOf(blocks);
        }

        /** Calls {@code method} on {@code machine}; throws {@link IllegalArgumentException} for an unknown method. */
        public @Nullable Object[] call(BlockEntity machine, String method, Object... arguments) {
            Method<BE> callback = methods.get(method);
            if (callback == null) throw new IllegalArgumentException("No such method " + method);
            return callback.call(type.cast(machine), arguments);
        }
    }

    private static final List<Component<?>> COMPONENTS = new ArrayList<>();

    private ComputerComponents() {}

    /**
     * Serves {@code type}'s block entities as the component {@code name} with {@code methods} (iteration order is the
     * order computers list them), on {@code blocks}.
     *
     * @throws IllegalStateException if {@code type} already has a component
     */
    @SafeVarargs
    public static synchronized <BE extends BlockEntity> Component<BE> register(Class<BE> type, String name,
                                                                                Map<String, Method<BE>> methods,
                                                                                Supplier<? extends Block>... blocks) {
        for (Component<?> component : COMPONENTS) {
            if (component.type() == type) throw new IllegalStateException(type.getName() + " already has a computer component");
        }
        Component<BE> component = new Component<>(type, name, methods, List.of(blocks));
        COMPONENTS.add(component);
        return component;
    }

    /** Every registered component, in registration order. */
    public static synchronized List<Component<?>> all() {
        return List.copyOf(COMPONENTS);
    }

    /** The component serving {@code machine}'s class (the nearest registered superclass), or null. */
    public static synchronized @Nullable Component<?> of(BlockEntity machine) {
        for (Class<?> c = machine.getClass(); c != null && c != BlockEntity.class; c = c.getSuperclass()) {
            for (Component<?> component : COMPONENTS) {
                if (component.type() == c) return component;
            }
        }
        return null;
    }

    /** Whether {@code block} is one a registered component is served on. */
    public static synchronized boolean carries(Block block) {
        for (Component<?> component : COMPONENTS) {
            for (Supplier<? extends Block> supplier : component.blocks()) {
                if (supplier.get() == block) return true;
            }
        }
        return false;
    }
}
