package com.hbm.world.gen.nbt;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.7 {@code JigsawPiece}: one {@link NBTStructure} as a named part of a jigsaw structure, with its placement
 * modifiers. {@link #blockTable} is 1.7's block swap table (the meteor dungeon's; WG-C); 1.7's {@code platform}
 * (the fill under a piece) is set by no 1.7 structure and is left out.
 */
public class JigsawPiece {

    // Translates a given name into a jigsaw piece, for serialization
    protected static final Map<String, JigsawPiece> jigsawMap = new HashMap<>();

    public final String name;
    public final NBTStructure structure;

    /**
     * Replaces matching blocks using the result of a BlockSelector. 1.7 keyed it by {@code Block}; blocks do not exist
     * when spawns register, so the key is the port block's id path (e.g. {@code meteor_brick}).
     */
    public java.util.Map<String, com.hbm.world.gen.component.Component.BlockSelector> blockTable;

    /** Moves every single column to the terrain (for digging out trenches, natural formations, etc). */
    public boolean conformToTerrain = false;

    /** Aligns this component y-level individually, without moving individual columns (like village houses). */
    public boolean alignToTerrain = false;

    /** Height offset for this structure piece, -1 will sink the floor flush into the ground. */
    public int heightOffset = 0;

    /** If greater than 0, will limit the number of times this piece can spawn in a structure. */
    public int instanceLimit = 0;

    /** If set, will continue generation beyond the defined limits until this piece exists at least once. */
    public boolean required = false;

    public JigsawPiece(String name, NBTStructure structure) {
        this(name, structure, 0);
    }

    public JigsawPiece(String name, NBTStructure structure, int heightOffset) {
        if (name == null) throw new IllegalStateException("A severe error has occurred in NBTStructure! A jigsaw piece has been registered without a valid name!");
        if (jigsawMap.containsKey(name)) throw new IllegalStateException("A severe error has occurred in NBTStructure! A jigsaw piece has been registered with the same name as another: " + name);

        this.name = name;
        this.structure = structure;
        jigsawMap.put(name, this);

        this.heightOffset = heightOffset;
    }
}
