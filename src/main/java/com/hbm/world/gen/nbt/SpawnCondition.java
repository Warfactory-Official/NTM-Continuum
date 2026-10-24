package com.hbm.world.gen.nbt;

import com.hbm.config.ServerConfig;
import net.minecraft.world.level.Level;

import java.util.Map;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * 1.7 {@code SpawnCondition}: a named structure (a single piece, or jigsaw pools and a start pool) as registered
 * with {@link NBTStructure#registerStructure}. Tandem placeholders look their structure up by this name.
 *
 * <p>The biome predicate and spawn weight are data in this port (biome tags and the {@code hbm:ntm_structures}
 * structure set, written by {@code NTMStructureDeclarations}); {@link NBTStructure.GenStructure} reads the rest.
 * 1.7's custom coordinate rules ({@code checkCoordinates}) are not ported: nothing in 1.7 set them.
 */
public class SpawnCondition {

    public final String name;

    /** If defined, will spawn a single jigsaw piece, for single nbt structures. */
    public JigsawPiece structure;

    /** If defined, will spawn in a non-nbt structure (1.7 {@code start}: features, bunker). */
    public StartFactory start;

    /** 1.7's {@code Function<Quartet<World, Random, Integer, Integer>, StructureStart>}, as vanilla's pieces builder. */
    @FunctionalInterface
    public interface StartFactory {
        void build(StructurePiecesBuilder builder,
                   Structure.GenerationContext context);
    }

    /** The chance of this structure spawning relative to others, higher weights will spawn more often. */
    public int spawnWeight = 1;

    /** Named jigsaw pools that are referenced by jigsaw blocks within the structure. */
    public Map<String, JigsawPool> pools;

    /** The name of the "core" pool, which the structure starts generation from. */
    public String startPool;

    /** Maximum amount of components in this structure (hard limit 1024). */
    public int sizeLimit = 8;

    /** How far the structure can extend horizontally from the center, maximum of 128. */
    public int rangeLimit = 128;

    /** Height modifiers, clamping the height the start generates at. */
    public int minHeight = 1;
    public int maxHeight = 128;

    public SpawnCondition(String name) {
        this.name = name;
    }

    public JigsawPool getPool(String name) {
        JigsawPool pool = pools == null ? null : pools.get(name);
        return pool != null ? pool.clone() : null;
    }

    /** Finds a piece belonging to this spawn, including a single-piece spawn. */
    public JigsawPiece findPiece(String pieceName) {
        if (structure != null && structure.name.equals(pieceName)) return structure;
        if (pools != null) {
            for (var pool : pools.values()) {
                for (var entry : pool.entries()) {
                    if (entry.piece().name.equals(pieceName)) return entry.piece();
                }
            }
        }
        return null;
    }

    // Builds all of the pools into neat rows and columns, for editing and debugging!
    // Make sure structure debug is enabled, or it will no-op. Do not use in generation.
    public void buildAll(Level level, int x, int y, int z) {
        if (!ServerConfig.STRUCTURE_DEBUG.get() || pools == null) return;

        int padding = 5;
        int oz = 0;

        for (JigsawPool pool : pools.values()) {
            int highestWidth = 0;
            int ox = 0;

            for (JigsawPool.Weighted entry : pool.pieces) {
                NBTStructure structure = entry.piece().structure;
                structure.build(level, x + ox + (structure.getSizeX() / 2), y, z + oz + (structure.getSizeZ() / 2));

                ox += structure.getSizeX() + padding;
                highestWidth = Math.max(highestWidth, structure.getSizeZ());
            }

            oz += highestWidth + padding;
        }
    }
}
