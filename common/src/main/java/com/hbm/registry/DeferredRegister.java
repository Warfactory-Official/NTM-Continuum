package com.hbm.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Cross-loader registration helper. Mirrors the ergonomics of NeoForge's {@code DeferredRegister}
 * but is implemented purely against vanilla APIs so the same code compiles and runs on every loader.
 *
 * <p>Common content classes ({@code ModItems}, {@code ModBlocks}, ...) declare their entries against
 * instances of this class. Each entry is stored as a deferred factory; the actual registration is
 * driven later by the loader-specific bootstrap via {@link #flushTo(RegistrationSink)} (NeoForge,
 * per {@code RegisterEvent}) or {@link #flushAll(RegistrationSink)} (Fabric, during init).
 *
 * <p>Every entry's id is set on its {@link Item.Properties}/{@link BlockBehaviour.Properties} before
 * construction, as required by 26.1.
 */
public class DeferredRegister<T> {

    private static final List<DeferredRegister<?>> ALL = new ArrayList<>();

    /**
     * Registries flushed first, in this order, before any others. Blocks must come before items so
     * that {@code BlockItem} factories can resolve their block during flush.
     */
    private static final List<ResourceKey<? extends Registry<?>>> FLUSH_ORDER = List.of(Registries.BLOCK, Registries.ITEM);

    protected final ResourceKey<? extends Registry<T>> registryKey;
    protected final String namespace;
    private final Map<ResourceKey<T>, Function<ResourceKey<T>, ? extends T>> entries = new LinkedHashMap<>();

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = Objects.requireNonNull(registryKey);
        this.namespace = Objects.requireNonNull(namespace);
        ALL.add(this);
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new DeferredRegister<>(registryKey, namespace);
    }

    public static Items createItems(String namespace) {
        return new Items(namespace);
    }

    public static Blocks createBlocks(String namespace) {
        return new Blocks(namespace);
    }

    /**
     * Register an entry. The factory receives the entry's {@link ResourceKey} (useful for setting the
     * id on properties) and must return a <em>new</em> instance.
     */
    public <I extends T> RegistryObject<I> register(String name, Function<ResourceKey<T>, ? extends I> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, name);
        ResourceKey<T> key = ResourceKey.create(registryKey, id);
        if (entries.putIfAbsent(key, factory) != null) {
            throw new IllegalArgumentException("Duplicate registration: " + id);
        }
        return new RegistryObject<>(registryKey, key);
    }

    public <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> supplier) {
        return register(name, key -> supplier.get());
    }

    public ResourceKey<? extends Registry<T>> getRegistryKey() {
        return registryKey;
    }

    /** Push every entry of this register into the given sink. */
    public void flushTo(RegistrationSink sink) {
        entries.forEach((key, factory) -> sink.accept(registryKey, key, () -> factory.apply(key)));
    }

    // --- static flush helpers used by the loader bootstraps ---

    public static List<DeferredRegister<?>> all() {
        return Collections.unmodifiableList(ALL);
    }

    /**
     * Flush every register, honouring {@link #FLUSH_ORDER} so dependencies (blocks before items) are
     * registered first. Used by Fabric, which registers everything synchronously at init time.
     */
    public static void flushAll(RegistrationSink sink) {
        List<DeferredRegister<?>> remaining = new ArrayList<>(ALL);
        for (ResourceKey<? extends Registry<?>> order : FLUSH_ORDER) {
            remaining.removeIf(dr -> {
                if (dr.registryKey.equals(order)) {
                    dr.flushTo(sink);
                    return true;
                }
                return false;
            });
        }
        remaining.forEach(dr -> dr.flushTo(sink));
    }

    // === specialized registers (NeoForge-style ergonomics, vanilla-only implementation) ===

    /** Specialized register for {@link Item}s. */
    public static class Items extends DeferredRegister<Item> {

        protected Items(String namespace) {
            super(Registries.ITEM, namespace);
        }

        public <I extends Item> RegistryObject<I> registerItem(String name, Function<Item.Properties, ? extends I> factory, UnaryOperator<Item.Properties> properties) {
            return register(name, key -> factory.apply(properties.apply(new Item.Properties()).setId(key)));
        }

        public <I extends Item> RegistryObject<I> registerItem(String name, Function<Item.Properties, ? extends I> factory) {
            return registerItem(name, factory, UnaryOperator.identity());
        }

        public RegistryObject<Item> registerSimpleItem(String name, UnaryOperator<Item.Properties> properties) {
            return registerItem(name, Item::new, properties);
        }

        public RegistryObject<Item> registerSimpleItem(String name) {
            return registerSimpleItem(name, UnaryOperator.identity());
        }

        public RegistryObject<BlockItem> registerBlockItem(String name, Supplier<? extends Block> block, UnaryOperator<Item.Properties> properties) {
            // useBlockDescriptionPrefix() makes the item's translation key "block.<ns>.<name>" (matching the
            // block) instead of "item.<ns>.<name>".
            return registerItem(name, props -> new BlockItem(block.get(), props),
                    p -> properties.apply(p).useBlockDescriptionPrefix());
        }

        public RegistryObject<BlockItem> registerBlockItem(String name, Supplier<? extends Block> block) {
            return registerBlockItem(name, block, UnaryOperator.identity());
        }
    }

    /** Specialized register for {@link Block}s. */
    public static class Blocks extends DeferredRegister<Block> {

        protected Blocks(String namespace) {
            super(Registries.BLOCK, namespace);
        }

        public <B extends Block> RegistryObject<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> factory, UnaryOperator<BlockBehaviour.Properties> properties) {
            return register(name, key -> factory.apply(properties.apply(BlockBehaviour.Properties.of()).setId(key)));
        }

        public <B extends Block> RegistryObject<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> factory) {
            return registerBlock(name, factory, UnaryOperator.identity());
        }

        public RegistryObject<Block> registerSimpleBlock(String name, UnaryOperator<BlockBehaviour.Properties> properties) {
            return registerBlock(name, Block::new, properties);
        }

        public RegistryObject<Block> registerSimpleBlock(String name) {
            return registerSimpleBlock(name, UnaryOperator.identity());
        }
    }
}
