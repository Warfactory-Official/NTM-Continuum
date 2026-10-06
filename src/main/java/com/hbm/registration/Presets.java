package com.hbm.registration;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Dispatch;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Shape;
import dev.golem.core.dejson.Tint;
import dev.golem.core.dejson.Variants;

import java.util.function.Function;

/**
 * The shared declaration vocabulary for {@code .transform(...)}: one entry per uniform SHAPE (ntm-next's
 * {@code Presets}), each emitting exactly the Dejson declaration the old {@code *Declarations} line did.
 *
 * <p>Block presets default to a single-variant blockstate and a "direct" block item (the 26.1 item
 * definition names the block model; {@code Declaration#blockItemDirect}). Say otherwise with an argument:
 * {@code .states(Variants.facing())}, {@code .viaItemModel()} (the definition names an item model whose
 * parent is the block model; {@code Declaration#blockItem}), or {@code .noItem()}. Both item spellings bake
 * identically but emit different files, so pick the one the old declaration used — the snapshot compare
 * will tell you if you picked wrong.
 *
 * <p>Hand-tuned geometry, display transforms and one-off OBJ item definitions are NOT presets: they stay
 * authored JSON (gradle/dejson-authored.txt), and the chain points at them with {@link #statesOnly} or an
 * {@code .icon(d -> d.shares(...))}. Family-specific bundles belong in the family's own class as a
 * {@code private static Preset}; add an entry here only when two families share it.
 */
public final class Presets {

    private Presets() {}

    // --- blocks ---

    /** {@code minecraft:block/cube_all}. */
    public static BlockModel cubeAll(String texture) {
        return new BlockModel(path -> Models.cubeAll(texture));
    }

    /** {@code cube_all} over {@code hbm:blocks/<path>}. */
    public static BlockModel cubeAll() {
        return new BlockModel(path -> Models.cubeAll("hbm:blocks/" + path));
    }

    /** {@link #cubeAll()} on the cutout layer, for a cube whose own icon has holes. */
    public static BlockModel cubeAllCutout() {
        return new BlockModel(path -> Models.renderType("minecraft:cutout", Models.cubeAll("hbm:blocks/" + path)));
    }

    /** {@code minecraft:block/cube_bottom_top}. */
    public static BlockModel cubeBottomTop(String bottom, String side, String top) {
        return new BlockModel(path -> Models.cubeBottomTop(bottom, side, top));
    }

    /** {@code minecraft:block/cube_column}; a pillar that does not turn (add {@code .states(Variants.axis())} for one that does). */
    public static BlockModel column(String end, String side) {
        return new BlockModel(path -> Models.column(end, side));
    }

    /** {@code minecraft:block/orientable}. */
    public static BlockModel orientable(String front, String side, String top) {
        return new BlockModel(path -> Models.orientable(front, side, top));
    }

    /** {@code minecraft:block/cross}. */
    public static BlockModel cross(String texture) {
        return new BlockModel(path -> Models.cross(texture));
    }

    /** Any other {@link Shape} built from the vocabulary. */
    public static BlockModel shape(Shape shape) {
        return new BlockModel(path -> shape);
    }

    /**
     * A block drawn by a renderer (OBJ machines): the blockstate points every state at {@code sharedModel}
     * and the block owns no model. No item by default — OBJ items are authored or typed declarations.
     */
    public static BlockModel rendered(String sharedModel, Dispatch states) {
        return new BlockModel(null, sharedModel, null, states, ItemForm.NONE);
    }

    /** The shared empty placeholder most OBJ machines render through. */
    public static BlockModel rendered(Dispatch states) {
        return rendered("hbm:block/machine_dummy_empty", states);
    }

    /** Only the blockstate; the model {@code hbm:block/<path>} is an authored file (hand-built geometry). */
    public static BlockModel statesOnly(Dispatch states) {
        return new BlockModel(null, null, "", states, ItemForm.NONE);
    }

    /** Only the blockstate, pointing at an authored model other than the block's own. */
    public static BlockModel statesOnly(String modelId, Dispatch states) {
        return new BlockModel(null, null, modelId, states, ItemForm.NONE);
    }

    // --- items ---

    /** {@code item/generated} over {@code hbm:items/<path>}. */
    public static ItemModel sprite() {
        return new ItemModel(path -> Models.generated("hbm:items/" + path), null);
    }

    /** {@code item/generated} over the given layers (layer0 first). */
    public static ItemModel sprite(String... layers) {
        return new ItemModel(path -> Models.generated(layers), null);
    }

    /**
     * A sprite held as 1.7 held a full-3D item ({@code isFull3D()}: every ItemSword/ItemTool/ItemHoe and every
     * {@code setFull3D()} item), over {@code hbm:items/<path>}: {@code hbm:item/handheld_full3d}, which is
     * {@code item/handheld} with 1.7's exact hand poses. Use {@link #sprite()} for an item 1.7 did not draw full-3D;
     * {@code port-tools/full3d_audit.py} lists any item whose held model disagrees with its 1.7 class.
     */
    public static ItemModel handheld() {
        return new ItemModel(path -> handheldFull3D("hbm:items/" + path), null);
    }

    /** {@link #handheld()} over the given sprite. */
    public static ItemModel handheld(String texture) {
        return new ItemModel(path -> handheldFull3D(texture), null);
    }

    private static Shape handheldFull3D(String layer0) {
        return Models.parented("hbm:item/handheld_full3d", dev.golem.core.dejson.Slots.of("layer0", layer0));
    }

    /** The item definition names an existing model (a block model, or an authored item model). */
    public static ItemModel shares(String modelId) {
        return new ItemModel(null, modelId);
    }

    enum ItemForm { DIRECT, VIA_ITEM_MODEL, NONE }

    /** A block's model + blockstate (+ block item model) declaration; immutable, each modifier returns a copy. */
    public static final class BlockModel implements Preset {

        private final Function<String, Shape> shape;
        private final String rendered;
        private final String statesOnly;
        private final Dispatch states;
        private final ItemForm item;

        BlockModel(Function<String, Shape> shape) {
            this(shape, null, null, Variants.simple(), ItemForm.DIRECT);
        }

        BlockModel(Function<String, Shape> shape, String rendered, String statesOnly, Dispatch states, ItemForm item) {
            this.shape = shape;
            this.rendered = rendered;
            this.statesOnly = statesOnly;
            this.states = states;
            this.item = item;
        }

        /** A different blockstate dispatch than the single variant. */
        public BlockModel states(Dispatch dispatch) {
            return new BlockModel(shape, rendered, statesOnly, dispatch, item);
        }

        /** The block item's 26.1 definition names {@code hbm:item/<path>}, whose parent is the block model. */
        public BlockModel viaItemModel() {
            return new BlockModel(shape, rendered, statesOnly, states, ItemForm.VIA_ITEM_MODEL);
        }

        /** The block item's definition names the block model directly (the default for model presets). */
        public BlockModel directItem() {
            return new BlockModel(shape, rendered, statesOnly, states, ItemForm.DIRECT);
        }

        /** No item model from this declaration (no item, or one declared apart with {@code .icon}). */
        public BlockModel noItem() {
            return new BlockModel(shape, rendered, statesOnly, states, ItemForm.NONE);
        }

        @Override
        public void applyTo(Reg.Handle<?> handle) {
            handle.model(this::fill);
        }

        private Dejson.Declaration fill(Dejson.Declaration declaration) {
            Dejson.Declaration d = declaration;
            if (shape != null) {
                d = d.models(shape.apply(d.blockId().substring(d.blockId().indexOf(':') + 1)));
            } else if (rendered != null) {
                d = d.rendered(rendered);
            } else if (statesOnly != null) {
                d = statesOnly.isEmpty() ? d.statesOnly() : d.statesOnly(statesOnly);
            }
            d = d.states(states);
            return switch (item) {
                case DIRECT -> d.blockItemDirect();
                case VIA_ITEM_MODEL -> d.blockItem();
                case NONE -> d;
            };
        }
    }

    /** An item's model + definition declaration; immutable. */
    public static final class ItemModel implements Preset {

        private final Function<String, Shape> shape;
        private final String shares;
        private final Tint[] tints;

        ItemModel(Function<String, Shape> shape, String shares) {
            this(shape, shares, null);
        }

        private ItemModel(Function<String, Shape> shape, String shares, Tint[] tints) {
            this.shape = shape;
            this.shares = shares;
            this.tints = tints;
        }

        /** Per-layer tint sources, layer0 first. */
        public ItemModel tints(Tint... tints) {
            return new ItemModel(shape, shares, tints.clone());
        }

        @Override
        public void applyTo(Reg.Handle<?> handle) {
            handle.icon(d -> {
                Dejson.ItemDeclaration item = shape != null ? d.models(shape.apply(handle.path())) : d.shares(shares);
                return tints != null ? item.tints(tints) : item;
            });
        }
    }
}
