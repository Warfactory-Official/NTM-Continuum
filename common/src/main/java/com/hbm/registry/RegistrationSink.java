package com.hbm.registry;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

@FunctionalInterface
public interface RegistrationSink {

    void accept(ResourceKey<? extends Registry<?>> registryKey, ResourceKey<?> entryKey, Supplier<?> value);
}
