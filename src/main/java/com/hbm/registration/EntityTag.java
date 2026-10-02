package com.hbm.registration;

/**
 * An entity-type tag a registration can join with {@code .tag(...)}; see {@link BlockTag} for the spelling.
 *
 * <p>Unused in HBM as of 2026-10-02 and kept deliberately: it is part of the ntm-next-shaped {@code Reg} surface
 * ({@code EntityHandle.tag}), so entity code ported from ntm-next compiles without a second spelling. Below 1.21
 * the build's legacy rename covers the plural {@code entity_types} folder.
 */
public record EntityTag(String namespace, String path) {

    /** {@code namespace:path}. */
    public static EntityTag of(String id) {
        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) throw new IllegalArgumentException("not a tag id: " + id);
        return new EntityTag(id.substring(0, colon), id.substring(colon + 1));
    }
}
