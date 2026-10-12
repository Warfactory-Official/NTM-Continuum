package com.hbm.registration;

import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;

/**
 * HBM's registration chain: Golem Core's {@link Registrar} (registration plus its Dejson declarations, see there),
 * kept under its old name so every {@code Reg.of(...)}, {@code Reg.Handle} and {@code Reg.EntityHandle} in HBM reads
 * as before. HBM-only bundles live in {@link Presets}; an addon uses {@code Registrar} directly with its own
 * registers, and HBM's {@code Presets} and {@code HbmTab} where they fit.
 */
public final class Reg extends Registrar {

    private Reg(DeferredRegister<?>... registers) {
        super(registers);
    }

    /** A registrar over the calling class's own registers (blocks, items, block entities, ...), in any order. */
    public static Reg of(DeferredRegister<?>... registers) {
        return new Reg(registers);
    }
}
