package com.hbm.registration;

/** An item tag a registration's item can join with {@code .tag(...)}; see {@link BlockTag} for the spelling. */
public record ItemTag(String namespace, String path) {

    /** {@code namespace:path}. */
    public static ItemTag of(String id) {
        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) throw new IllegalArgumentException("not a tag id: " + id);
        return new ItemTag(id.substring(0, colon), id.substring(colon + 1));
    }
}
