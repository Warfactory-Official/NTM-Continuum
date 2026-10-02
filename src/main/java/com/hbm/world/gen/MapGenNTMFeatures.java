package com.hbm.world.gen;

import com.hbm.world.gen.component.CivilianFeatures.NTMHouse1;
import com.hbm.world.gen.component.CivilianFeatures.NTMHouse2;
import com.hbm.world.gen.component.CivilianFeatures.NTMLab1;
import com.hbm.world.gen.component.CivilianFeatures.NTMLab2;
import com.hbm.world.gen.component.CivilianFeatures.RuralHouse1;
import com.hbm.world.gen.component.Component;
import com.hbm.world.gen.component.OfficeFeatures.LargeOffice;
import com.hbm.world.gen.component.OfficeFeatures.LargeOfficeCorner;
import com.hbm.world.gen.component.SiloComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * 1.7 {@code MapGenNTMFeatures.Start}: the {@code features} structure - one hardcoded building per start, chosen by the
 * biome at the start chunk's centre. In 1.7 the {@code NTMWorldGenerator} region pick chose {@code features} (weight 50,
 * not in water biomes) and called this start; here the {@code hbm:features} structure of the {@code hbm:ntm_structures}
 * set does ({@code NTMStructureDeclarations}), through {@link NTMComponentStructures}.
 *
 * <p>1.7's rules, in order: a kinda-flat biome ({@code heightVariation <= 0.25}) draws a 1 in 10 for the missile silo;
 * else a hot, rainless, non-mesa biome gets one of the two sandstone houses; else one of six (two labs, two offices,
 * the rural house twice). {@code heightVariation} is the 1.7 counterpart's ({@code NTMStructureDeclarations.LEGACY}; a
 * modded biome's is read off the terrain, {@link LegacyBiomes#heightVariation}). Temperature and rainfall are read from the modern biome,
 * which gives the same set for every vanilla biome (desert, the savannas; badlands are the excluded mesa) and, as the
 * 1.7 comment wished, a sensible one for modded biomes: base temperature at least 1 and no precipitation.
 *
 * <p>1.7's own {@code MapGenStructure} half of this class (a separate region grid with a 1 in 15 bunker) was never
 * registered as a world generator in 1.7 and is not ported.
 */
public final class MapGenNTMFeatures {

    private MapGenNTMFeatures() {}

    /** The {@code features} spawn's start: {@code SpawnCondition.start}. */
    public static void start(StructurePiecesBuilder builder, Structure.GenerationContext context) {
        RandomSource rand = context.random();
        int chunkX = context.chunkPos().getMinBlockX() >> 4;
        int chunkZ = context.chunkPos().getMinBlockZ() >> 4;

        int i = (chunkX << 4) + 8;
        int j = (chunkZ << 4) + 8;

        Holder<Biome> biome = biomeAt(context, i, j); //Only gets the biome in the corner of the chunk.

        Component component;

        if (heightVariation(biome, context) <= 0.25F && rand.nextInt(10) == 0) { //for now our only restriction is kinda-flat biomes. that and chance might change idk
            component = new SiloComponent(rand, i, j);
        } else if (hotAndDry(biome)) { //Desert & Savannah
            if (rand.nextBoolean()) {
                component = new NTMHouse1(rand, i, j);
            } else {
                component = new NTMHouse2(rand, i, j);
            }
        } else { //Everything else
            switch (rand.nextInt(6)) {
                case 0: component = new NTMLab2(rand, i, j); break; //and these, too
                case 1: component = new NTMLab1(rand, i, j); break;
                case 2: component = new LargeOffice(rand, i, j); break;
                case 3: component = new LargeOfficeCorner(rand, i, j); break;
                default: component = new RuralHouse1(rand, i, j); break; // cases 4 and 5
            }
        }

        component.assembleHeight(context);
        builder.addPiece(component);
    }

    /** The biome where 1.7 read it: the chunk's centre column, at the surface (an ocean at its water, not a cave below). */
    static Holder<Biome> biomeAt(Structure.GenerationContext context, int x, int z) {
        int y = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        return context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), context.randomState().sampler());
    }

    /** The 1.7 counterpart's {@code heightVariation}; a modded biome's read off the start chunk's terrain. */
    static float heightVariation(Holder<Biome> biome, Structure.GenerationContext context) {
        return LegacyBiomes.heightVariation(biome, context);
    }

    /** 1.7 {@code biome.temperature >= 1.0 && biome.rainfall == 0 && !(biome instanceof BiomeGenMesa)}. */
    static boolean hotAndDry(Holder<Biome> biome) {
        return biome.value().getBaseTemperature() >= 1.0F && !biome.value().hasPrecipitation() && !biome.is(BiomeTags.IS_BADLANDS);
    }
}
