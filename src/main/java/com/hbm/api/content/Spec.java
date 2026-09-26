package com.hbm.api.content;

import com.hbm.scripting.ScriptSpecs;
import java.util.Objects;

/**
 * A script value kept raw until the registries exist, typed so editor typings can say what it takes.
 * KubeJS converts any script value to the declared spec type; {@link ScriptSpecs} resolves it.
 */
public abstract class Spec {

    public final Object raw;

    Spec(Object raw) {
        this.raw = Objects.requireNonNull(raw, "spec");
    }

    @Override
    public String toString() {
        return String.valueOf(raw);
    }

    /** {@code "hbm:plate_steel"}, {@code "3x hbm:plate_steel"}, an ItemStack. */
    public static final class Item extends Spec {
        public Item(Object raw) { super(unwrap(raw)); }
    }

    /** Item form, {@code "#c:plates/steel"}, {@code "2x #c:plates/steel"}. */
    public static final class Ingredient extends Spec {
        public Ingredient(Object raw) { super(unwrap(raw)); }
    }

    /** {@code "water"}, {@code "1000x water"}, {@code "1000x water@2"}. */
    public static final class Fluid extends Spec {
        public Fluid(Object raw) { super(unwrap(raw)); }
    }

    /** {@code "72x iron"}: quanta of a material, nugget 8, ingot 72, block 648. */
    public static final class Molten extends Spec {
        public Molten(Object raw) { super(unwrap(raw)); }
    }

    private static Object unwrap(Object raw) {
        return raw instanceof Spec spec ? spec.raw : raw;
    }
}
