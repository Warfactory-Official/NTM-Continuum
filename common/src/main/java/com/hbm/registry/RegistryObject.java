package com.hbm.registry;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A lazily-resolved reference to a registered game object. This is the cross-loader analogue of
 * NeoForge's {@code DeferredHolder} / Forge's {@code RegistryObject}, but it is built purely on
 * vanilla APIs so it behaves identically on every loader.
 *
 * <p>The underlying value is looked up from the vanilla registries the first time {@link #get()} is
 * called, which must be after registration has run. Instances are produced by
 * {@link DeferredRegister#register(String, java.util.function.Function)}.
 */
public final class RegistryObject<T> implements Supplier<T> {

    private final ResourceKey<? extends Registry<?>> registryKey;
    private final ResourceKey<?> key;
    private T value;

    RegistryObject(ResourceKey<? extends Registry<?>> registryKey, ResourceKey<?> key) {
        this.registryKey = Objects.requireNonNull(registryKey);
        this.key = Objects.requireNonNull(key);
    }

    /** The id ({@code namespace:path}) this object is registered under. */
    public Identifier getId() {
        return key.identifier();
    }

    @SuppressWarnings("unchecked")
    public ResourceKey<T> getKey() {
        return (ResourceKey<T>) key;
    }

    /** Resolve the registered value, caching it after the first lookup. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public T get() {
        if (value == null) {
            Registry<?> registry = (Registry<?>) BuiltInRegistries.REGISTRY.getValueOrThrow((ResourceKey) registryKey);
            value = (T) registry.getValueOrThrow((ResourceKey) key);
        }
        return value;
    }

    @Override
    public String toString() {
        return "RegistryObject[" + key.identifier() + "]";
    }
}
