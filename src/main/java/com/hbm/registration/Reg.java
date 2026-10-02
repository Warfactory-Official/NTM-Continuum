package com.hbm.registration;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Tags;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Registration and its declarations in one chain, after ntm-next's {@code Reg}:
 *
 * <pre>{@code
 * private static final Reg REG = Reg.of(BLOCKS, ITEMS);
 *
 * public static final RegistryObject<MachineSiren> MACHINE_SIREN = REG.block("machine_siren", MachineSiren::new,
 *                 p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
 *         .transform(Presets.cubeBottomTop("hbm:blocks/block_steel", "hbm:blocks/machine_siren", "hbm:blocks/block_steel"))
 *         .loot(Loot.self())
 *         .tag(BlockTag.PICKAXE);
 * }</pre>
 *
 * <h2>What is different from ntm-next, and why</h2>
 * <ul>
 *   <li><b>A {@code Reg} per registration class</b>, wrapping that class's own {@link DeferredRegister}s.
 *       Registers flush in the order they were created and entries in the order they were added, so
 *       keeping each class's registers keeps every registry's order — and with it network ids, JEI order
 *       and every "first registered" fallback — exactly as it was.
 *   <li><b>A {@link Handle} is a {@link RegistryObject}</b>, so a migrated field keeps its declared type
 *       ({@code RegistryObject<MachineSiren>}) and no call site changes.
 *   <li><b>Declarations go to Golem Core's Dejson, not to datagen.</b> Each declaring method registers its
 *       Dejson declaration on the spot: {@code dejsonGenerate} lists the registration class in
 *       {@code DECLARATION_CLASSES}, DejsonMain initialises it in a plain JVM (no game), and the chain runs
 *       as far as recording — nothing is ever flushed there. The same declarations are recorded in game,
 *       as the family {@code *Declarations.declare()} calls were, which is what {@code DejsonGameTests}
 *       holds against the shipped files. Because a declaration registers immediately, each surface is
 *       stated once: a family difference is an argument to the preset, never a second statement.
 * </ul>
 *
 * <h2>The plain-JVM rule</h2>
 * A registration class with declarations in its chains runs its static initialiser in DejsonMain, where
 * Minecraft is on the classpath but never bootstrapped. Everything that needs a running game — a
 * {@code SoundType}/{@code SoundEvents} constant, a {@code BuiltInRegistries} lookup, an
 * {@code Item.Properties} — must stay inside the factory and properties lambdas, which registration only
 * stores. A violation fails {@code dejsonGenerate} naming the class and the root cause.
 */
public final class Reg {

    private final List<DeferredRegister<?>> registers;

    private Reg(List<DeferredRegister<?>> registers) {
        this.registers = registers;
    }

    /** A registrar over the calling class's own registers (blocks, items, block entities, ...), in any order. */
    public static Reg of(DeferredRegister<?>... registers) {
        if (registers.length == 0) {
            throw new IllegalArgumentException("a Reg needs at least one register");
        }
        return new Reg(List.of(registers));
    }

    /** {@code "hbm:<block> -> #<tag>"} for every item tag declared on a BLOCK handle; see {@link Handle#tag(ItemTag)}. */
    private static final java.util.Set<String> BLOCK_ITEM_TAG_MEMBERS =
            java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());

    /** The item-tag memberships declared on block handles, each assuming a same-id block item exists. */
    public static java.util.Set<String> blockItemTagMembers() {
        synchronized (BLOCK_ITEM_TAG_MEMBERS) {
            return java.util.Set.copyOf(BLOCK_ITEM_TAG_MEMBERS);
        }
    }

    // --- blocks ---

    public <B extends Block> Handle<B> block(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
                                             UnaryOperator<BlockBehaviour.Properties> properties) {
        return new Handle<>(blocks().registerBlock(name, factory, properties), name, Kind.BLOCK);
    }

    public <B extends Block> Handle<B> block(String name, Function<BlockBehaviour.Properties, ? extends B> factory) {
        return new Handle<>(blocks().registerBlock(name, factory), name, Kind.BLOCK);
    }

    /**
     * A plain {@link Block}; {@code DeferredRegister.Blocks#registerSimpleBlock}. Named apart from
     * {@code block(name, factory)} so that an implicitly typed lambda ({@code REG.block("x", p -> new X(p))})
     * has exactly one candidate: two one-argument functional-interface overloads made every such call ambiguous.
     */
    public Handle<Block> simpleBlock(String name, UnaryOperator<BlockBehaviour.Properties> properties) {
        return new Handle<>(blocks().registerSimpleBlock(name, properties), name, Kind.BLOCK);
    }

    // --- items ---

    public <I extends Item> Handle<I> item(String name, Function<Item.Properties, ? extends I> factory,
                                           UnaryOperator<Item.Properties> properties) {
        return new Handle<>(items().registerItem(name, factory, properties), name, Kind.ITEM);
    }

    public <I extends Item> Handle<I> item(String name, Function<Item.Properties, ? extends I> factory) {
        return new Handle<>(items().registerItem(name, factory), name, Kind.ITEM);
    }

    /**
     * A plain item ({@code CompatItem}); {@code DeferredRegister.Items#registerSimpleItem}. Named apart from
     * {@code item(name, factory)} for the reason given at {@link #simpleBlock}.
     */
    public Handle<Item> simpleItem(String name, UnaryOperator<Item.Properties> properties) {
        return new Handle<>(items().registerSimpleItem(name, properties), name, Kind.ITEM);
    }

    public Handle<Item> item(String name) {
        return new Handle<>(items().registerSimpleItem(name), name, Kind.ITEM);
    }

    /**
     * The block's plain item, under the block's own name: {@code DeferredRegister.Items#registerBlockItem},
     * which already gives it the block's description id on every node.
     */
    public Handle<BlockItem> blockItem(RegistryObject<? extends Block> block) {
        String name = path(block);
        return new Handle<>(items().registerBlockItem(name, block), name, Kind.ITEM);
    }

    public Handle<BlockItem> blockItem(RegistryObject<? extends Block> block, UnaryOperator<Item.Properties> properties) {
        String name = path(block);
        return new Handle<>(items().registerBlockItem(name, block, properties), name, Kind.ITEM);
    }

    /**
     * The block's item of a custom class, under the block's own name: exactly
     * {@code ITEMS.registerItem(name, p -> factory.apply(block.get(), p), properties)}. The properties are
     * used as given — pass {@code Compat::blockDescription} where the old registration did.
     */
    public <B extends Block, I extends Item> Handle<I> blockItem(RegistryObject<B> block,
                                                                 BiFunction<? super B, Item.Properties, ? extends I> factory,
                                                                 UnaryOperator<Item.Properties> properties) {
        String name = path(block);
        return new Handle<>(items().registerItem(name, p -> factory.apply(block.get(), p), properties), name, Kind.ITEM);
    }

    // --- everything else ---

    public <E extends BlockEntity> Handle<BlockEntityType<E>> blockEntity(String name, Supplier<BlockEntityType<E>> type) {
        return registerOn(find(DeferredRegister.BlockEntityTypes.class, () -> Registries.BLOCK_ENTITY_TYPE), name, type);
    }

    /** An entity type; see {@link EntityHandle} for why it is not a {@link Handle}. */
    public <E extends Entity> EntityHandle<E> entity(String name, Supplier<EntityType<E>> type) {
        return entity(name, key -> type.get());
    }

    /**
     * An entity type whose builder needs its own key ({@code EntityType.Builder#build(ResourceKey)} on 26.1,
     * Golem's {@code Entities.build(builder, key)} everywhere): exactly
     * {@code ENTITY_TYPES.register(name, key -> ...)}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E extends Entity> EntityHandle<E> entity(String name,
                                                     Function<ResourceKey<EntityType<?>>, EntityType<E>> type) {
        DeferredRegister register = find(DeferredRegister.EntityTypes.class, () -> Registries.ENTITY_TYPE);
        RegistryObject<EntityType<E>> object = register.register(name, type);
        return new EntityHandle<>(object, name);
    }

    public <M extends AbstractContainerMenu> Handle<MenuType<M>> menu(String name, Supplier<MenuType<M>> type) {
        return registerOn(find(DeferredRegister.MenuTypes.class, () -> Registries.MENU), name, type);
    }

    /** A sound event {@code hbm:<name>} with variable range — the form every HBM sound registration uses. */
    public Handle<SoundEvent> sound(String name) {
        return registerOn(find(DeferredRegister.Sounds.class, () -> Registries.SOUND_EVENT), name,
                () -> SoundEvent.createVariableRangeEvent(dev.golem.core.Ids.of(Constants.MOD_ID, name)));
    }

    /** Any other registry this Reg holds a register for (effects, features, particle types, ...). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T, R extends T> Handle<R> register(ResourceKey<? extends Registry<T>> registry, String name,
                                               Supplier<? extends R> value) {
        return registerOn(find(registry), name, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <R> Handle<R> registerOn(DeferredRegister register, String name, Supplier<? extends R> value) {
        RegistryObject<R> object = register.register(name, value);
        return new Handle<>(object, name, Kind.OTHER);
    }

    // --- internals ---

    private DeferredRegister.Blocks blocks() {
        for (DeferredRegister<?> register : registers) {
            if (register instanceof DeferredRegister.Blocks blocks) return blocks;
        }
        throw new IllegalStateException("this Reg was given no DeferredRegister.Blocks");
    }

    private DeferredRegister.Items items() {
        for (DeferredRegister<?> register : registers) {
            if (register instanceof DeferredRegister.Items items) return items;
        }
        throw new IllegalStateException("this Reg was given no DeferredRegister.Items");
    }

    private DeferredRegister<?> find(ResourceKey<? extends Registry<?>> registry) {
        for (DeferredRegister<?> register : registers) {
            if (register.getRegistryKey().equals(registry)) return register;
        }
        throw new IllegalStateException("this Reg was given no register for " + registry);
    }

    /**
     * The register of one kind, found WITHOUT naming any registry when the class holds the typed register
     * ({@code DeferredRegister.createBlockEntityTypes/createMenuTypes/createSounds/createEntityTypes}):
     * {@code Registries.X} is only read when it is needed to tell apart registers made with
     * {@code DeferredRegister.create(...)}. On 1.20.1 naming a registry key outside a bootstrapped game fails,
     * so a registration class that DejsonMain initialises uses the typed registers.
     */
    private DeferredRegister<?> find(Class<? extends DeferredRegister> kind,
                                     Supplier<? extends ResourceKey<? extends Registry<?>>> registry) {
        for (DeferredRegister<?> register : registers) {
            if (kind.isInstance(register)) return register;
        }
        ResourceKey<? extends Registry<?>> key = registry.get();
        for (DeferredRegister<?> register : registers) {
            if (!typed(register) && register.getRegistryKey().equals(key)) return register;
        }
        throw new IllegalStateException("this Reg was given no register for " + key);
    }

    /** A register whose registry is known from its class, so it can never be the one a key search wants. */
    private static boolean typed(DeferredRegister<?> register) {
        return register instanceof DeferredRegister.Blocks || register instanceof DeferredRegister.Items
                || register instanceof DeferredRegister.BlockEntityTypes || register instanceof DeferredRegister.MenuTypes
                || register instanceof DeferredRegister.Sounds || register instanceof DeferredRegister.EntityTypes;
    }

    private static String path(RegistryObject<?> object) {
        return object instanceof Handle<?> handle ? handle.path : object.getId().getPath();
    }

    enum Kind { BLOCK, ITEM, OTHER }

    /**
     * A registration plus the declarations stated on it. It IS the {@link RegistryObject} the field always
     * held: {@code get()}, {@code getId()} and {@code getKey()} behave exactly as before.
     *
     * <p>Every declaring method states one surface for this id and registers it at once with Dejson;
     * stating a surface twice fails there ("declared twice"), which is how a forgotten line in an old
     * {@code *Declarations} class shows up.
     */
    public static final class Handle<T> extends RegistryObject<T> {

        private final String path;
        private final Kind kind;

        private Handle(RegistryObject<T> object, String path, Kind kind) {
            super(object);
            this.path = path;
            this.kind = kind;
        }

        /** The registry path, e.g. {@code machine_siren}. */
        public String path() {
            return path;
        }

        /** The namespace, always {@code hbm}. */
        public String namespace() {
            return Constants.MOD_ID;
        }

        /** {@code hbm:<path>}, as tags and loot name it. */
        public String id() {
            return Constants.MOD_ID + ":" + path;
        }

        /**
         * The block's model, blockstate and (optionally) its block item's model, as one Dejson block
         * declaration: {@code .model(d -> d.models(Models.cubeAll(...)).states(Variants.simple()).blockItemDirect())}.
         * Prefer a {@link Presets} entry where one fits; this is the escape hatch for one-offs, and a
         * transliteration target for an old {@code Dejson.block(NS, "x")...register()} line.
         */
        public Handle<T> model(UnaryOperator<Dejson.Declaration> declaration) {
            require(Kind.BLOCK, "model");
            declaration.apply(Dejson.block(Constants.MOD_ID, path)).register();
            return this;
        }

        /**
         * The item's model and definition as one Dejson item declaration:
         * {@code .icon(d -> d.models(Models.generated("hbm:items/x")))}. Valid on a block too, for a block
         * item declared apart from its block (same id).
         */
        public Handle<T> icon(UnaryOperator<Dejson.ItemDeclaration> declaration) {
            if (kind == Kind.OTHER) throw new IllegalStateException(id() + " is neither a block nor an item");
            declaration.apply(Dejson.item(Constants.MOD_ID, path)).register();
            return this;
        }

        /** The block's loot table ({@code data/hbm/loot_table/blocks/<path>.json}). */
        public Handle<T> loot(Loot loot) {
            require(Kind.BLOCK, "loot");
            loot.declare(Constants.MOD_ID, path);
            return this;
        }

        /** Makes this BLOCK a member of a block tag; memberships accumulate across the whole mod. */
        public Handle<T> tag(BlockTag tag) {
            require(Kind.BLOCK, "a block tag");
            Tags.member("block", tag.namespace(), tag.path(), id());
            return this;
        }

        /**
         * Makes this registration's ITEM (a block's item shares its id) a member of an item tag.
         *
         * <p>On a BLOCK handle this assumes the block has an item of the same id, which the declaration cannot
         * see: block items are registered elsewhere, often later. An item-less block here would write an unknown
         * required entry, and vanilla then drops the WHOLE item tag file without a word. Every such id is
         * recorded in {@link #blockItemTagMembers()}, and the {@code reg_block_item_tags_resolve} GameTest fails
         * loudly, naming the block, when one of them has no registered item.
         */
        public Handle<T> tag(ItemTag tag) {
            if (kind == Kind.OTHER) throw new IllegalStateException(id() + " is neither a block nor an item");
            if (kind == Kind.BLOCK) BLOCK_ITEM_TAG_MEMBERS.add(id() + " -> #" + tag.namespace() + ":" + tag.path());
            Tags.member("item", tag.namespace(), tag.path(), id());
            return this;
        }

        /** Shows this item in a creative tab, in call order — the same {@code ModTabs.addTo} the static blocks made. */
        @SuppressWarnings("unchecked")
        public Handle<T> tab(HbmTab tab) {
            require(Kind.ITEM, "a creative tab");
            ModTabs.addTo(tab, (RegistryObject<? extends Item>) (RegistryObject<?>) this);
            return this;
        }

        /** Applies a reusable bundle of declarations (see {@link Presets}). */
        public Handle<T> transform(Preset preset) {
            preset.applyTo(this);
            return this;
        }

        /** Runs an action against the HANDLE now (e.g. to record it); the object is not registered yet. */
        public Handle<T> andThen(Consumer<? super Handle<T>> action) {
            action.accept(this);
            return this;
        }

        public Handle<T> addTo(Collection<? super Handle<T>> roster) {
            roster.add(this);
            return this;
        }

        boolean isBlock() {
            return kind == Kind.BLOCK;
        }

        private void require(Kind expected, String what) {
            if (kind != expected) {
                throw new IllegalStateException(id() + " is a " + kind.name().toLowerCase(java.util.Locale.ROOT)
                        + " registration; " + what + " belongs on a " + expected.name().toLowerCase(java.util.Locale.ROOT));
            }
        }
    }

    /**
     * A registered entity type, with the declarations an entity type can carry (ntm-next's
     * {@code EntityHandle}). Deliberately not a {@link Handle}: one name can be a block and an entity at once
     * ({@code rbmk_debris}), so the scope rides the type and a block-only statement cannot be made on it.
     * Like {@link Handle} it IS the {@link RegistryObject} the field always held, so
     * {@code RegistryObject<EntityType<X>>} fields keep their type.
     */
    public static final class EntityHandle<E extends Entity> extends RegistryObject<EntityType<E>> {

        private final String path;

        private EntityHandle(RegistryObject<EntityType<E>> object, String path) {
            super(object);
            this.path = path;
        }

        /** The registry path, e.g. {@code entity_coin}. */
        public String path() {
            return path;
        }

        /** {@code hbm:<path>}, as tags name it. */
        public String id() {
            return Constants.MOD_ID + ":" + path;
        }

        /**
         * Makes this entity type a member of an entity-type tag; memberships accumulate across the mod. No HBM
         * entity calls it yet; it is kept as part of the ntm-next-shaped API (see {@link EntityTag}).
         */
        public EntityHandle<E> tag(EntityTag tag) {
            Tags.member("entity_type", tag.namespace(), tag.path(), id());
            return this;
        }

        public EntityHandle<E> addTo(Collection<? super EntityHandle<E>> roster) {
            roster.add(this);
            return this;
        }
    }
}
