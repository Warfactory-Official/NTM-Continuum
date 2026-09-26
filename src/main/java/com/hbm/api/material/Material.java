package com.hbm.api.material;

import net.minecraft.resources.Identifier;

/** Implemented by NTM's materials only; obtain one from a {@code MaterialsEvent} or a {@link MaterialStack}. */
public interface Material {

    /** Persisted identity. */
    Identifier id();

    /** Session index = registration order; network buffers only. */
    int index();

    /** Item-naming key: {@code iron} for {@code hbm:iron}, else {@code <namespace>_<path>}. */
    String name();

    String getDisplayName();

    int solidColorLight();

    int solidColorDark();

    int moltenColor();

    SmeltingBehavior smeltable();

    /** Melts into {@code smeltsInto()} at {@code amount * convOut() / convIn()}. */
    Material smeltsInto();

    int convIn();

    int convOut();
}
