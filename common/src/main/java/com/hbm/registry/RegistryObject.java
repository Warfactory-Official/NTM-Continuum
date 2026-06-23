package com.hbm.registry;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class RegistryObject<T> implements Supplier<T> {

    private final ResourceKey<? extends Registry<?>> registryKey;
    private final ResourceKey<?> key;
    private T value;

    RegistryObject(ResourceKey<? extends Registry<?>> registryKey, ResourceKey<?> key) {
        this.registryKey = Objects.requireNonNull(registryKey);
        this.key = Objects.requireNonNull(key);
    }

    public Identifier getId() {
        return key.identifier();
    }

    @SuppressWarnings("unchecked")
    public ResourceKey<T> getKey() {
        return (ResourceKey<T>) key;
    }

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
