package com.hbm.capability;

import org.jspecify.annotations.Nullable;

/** A position-bound cached find: always current, cheap per tick, reclaimed with the block it watches. */
@FunctionalInterface
public interface BlockLookupCache<T> {

    @Nullable T find();
}
