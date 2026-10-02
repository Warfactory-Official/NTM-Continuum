package com.hbm.registration;

/**
 * A reusable bundle of declarations applied to a registration with {@code .transform(preset)}, so a family
 * states its shared surfaces once and each member says which family it is in (ntm-next's {@code Preset}).
 *
 * <p>Not generic, unlike ntm-next's: a bundle here only ever reads the handle's id, and a non-generic type
 * keeps {@code Presets.cubeAll(t).viaItemModel()} chainable without type witnesses. A member's difference
 * is an ARGUMENT to the bundle, never a second statement after it — Dejson registers a surface once.
 */
@FunctionalInterface
public interface Preset {

    void applyTo(Reg.Handle<?> handle);

    /** Both bundles, this one first. */
    default Preset and(Preset next) {
        return handle -> {
            applyTo(handle);
            next.applyTo(handle);
        };
    }
}
