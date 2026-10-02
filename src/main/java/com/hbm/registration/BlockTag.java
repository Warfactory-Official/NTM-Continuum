package com.hbm.registration;

/**
 * A block tag a registration can join with {@code .tag(...)}. Spelled as a string pair rather than a
 * vanilla {@code TagKey} because the chain also runs in DejsonMain's plain JVM, and because the id is all
 * the tag file needs.
 */
public record BlockTag(String namespace, String path) {

    public static final BlockTag PICKAXE = new BlockTag("minecraft", "mineable/pickaxe");
    public static final BlockTag AXE = new BlockTag("minecraft", "mineable/axe");
    public static final BlockTag SHOVEL = new BlockTag("minecraft", "mineable/shovel");
    public static final BlockTag HOE = new BlockTag("minecraft", "mineable/hoe");
    public static final BlockTag NEEDS_IRON_TOOL = new BlockTag("minecraft", "needs_iron_tool");
    public static final BlockTag CLIMBABLE = new BlockTag("minecraft", "climbable");

    /** {@code namespace:path}. */
    public static BlockTag of(String id) {
        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) throw new IllegalArgumentException("not a tag id: " + id);
        return new BlockTag(id.substring(0, colon), id.substring(colon + 1));
    }
}
