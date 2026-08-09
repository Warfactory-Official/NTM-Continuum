package com.hbm.registry;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Loader-provided callback that performs the actual registration of a single entry.
 *
 * <p>Common code never registers directly; instead each platform supplies a sink:
 * <ul>
 *   <li>NeoForge forwards to {@code RegisterEvent#register} on the mod event bus.</li>
 *   <li>Fabric calls {@code Registry.register} directly during mod initialization.</li>
 * </ul>
 *
 * The method is intentionally non-generic (using wildcards) so it can be implemented with a lambda.
 */
@FunctionalInterface
public interface RegistrationSink {

    void accept(ResourceKey<? extends Registry<?>> registryKey, ResourceKey<?> entryKey, Supplier<?> value);
}
