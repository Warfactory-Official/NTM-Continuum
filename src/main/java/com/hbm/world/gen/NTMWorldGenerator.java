package com.hbm.world.gen;

import com.hbm.world.gen.nbt.DuctFluidProcessor;
import com.hbm.world.gen.nbt.LegacyBlockTableProcessor;
import com.hbm.world.gen.nbt.MultiblockAssemblyProcessor;
import com.hbm.world.gen.nbt.PedestalStockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.main.StructureManager;
import com.hbm.world.gen.nbt.JigsawPiece;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Structures;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * 1.7 {@code NTMWorldGenerator}: the NBT structures that generate in the overworld.
 *
 * <p>Split along the modern data/code line. What decides <i>where</i> a structure goes (its biome predicate and spawn
 * weight, and the region grid) is data, written at build time by {@link NTMStructureDeclarations} into
 * {@code worldgen/structure}, the {@code hbm:ntm_structures} structure set and {@code hbm:has_structure/*} biome tags.
 * What decides <i>how</i> it is built (piece, height offset, height clamp, terrain conforming) is registered here, under
 * the same names 1.7 used, and read by {@link NBTStructure.GenStructure} through the structure JSON's {@code spawn}.
 *
 * <p>1.7 also registered {@code features}, {@code bunker} and {@code meteor_dungeon} here; they plug in the same way (a
 * {@link SpawnCondition} with {@code start} or {@code pools}, plus one declaration line) from
 * {@link NTMComponentStructures}.
 *
 * <p>The 1.7 names are kept, including {@code labolatory} (sic) and the capitalised {@code ruinA..J}; resource ids must
 * be lower case, so those structures are {@code hbm:ruina..hbm:ruinj}.
 */
public final class NTMWorldGenerator {

    private NTMWorldGenerator() {}

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Constants.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Constants.MOD_ID);
    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Constants.MOD_ID);
    private static final Reg REG = Reg.of(STRUCTURE_TYPES, STRUCTURE_PIECES, STRUCTURE_PROCESSORS);

    public static final RegistryObject<StructureProcessorType<MultiblockAssemblyProcessor>> MULTIBLOCK_ASSEMBLY =
            REG.register(Registries.STRUCTURE_PROCESSOR, "multiblock_assembly",
                    () -> Structures.processorType(MultiblockAssemblyProcessor.MAP_CODEC));

    public static final RegistryObject<StructureProcessorType<DuctFluidProcessor>> DUCT_FLUID =
            REG.register(Registries.STRUCTURE_PROCESSOR, "duct_fluid",
                    () -> Structures.processorType(DuctFluidProcessor.MAP_CODEC));

    public static final RegistryObject<StructureProcessorType<LegacyBlockTableProcessor>> LEGACY_BLOCK_TABLE =
            REG.register(Registries.STRUCTURE_PROCESSOR, "legacy_block_table",
                    () -> Structures.processorType(LegacyBlockTableProcessor.MAP_CODEC));

    /** Stocks placed pedestals (Ike's divine shard); the item rides the processor list entry. */
    public static final RegistryObject<StructureProcessorType<PedestalStockProcessor>> PEDESTAL_STOCK =
            REG.register(Registries.STRUCTURE_PROCESSOR, "pedestal_stock",
                    () -> Structures.processorType(PedestalStockProcessor.MAP_CODEC));

    /** {@code hbm:nbt}: one registered {@link SpawnCondition} as a vanilla structure. */
    public static final RegistryObject<StructureType<NBTStructure.GenStructure>> NBT_STRUCTURE =
            REG.register(Registries.STRUCTURE_TYPE, "nbt", () -> Structures.type(NBTStructure.GenStructure.CODEC));

    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Constants.MOD_ID);

    /** {@code hbm:config_spread}: random_spread spacing read from hbm.cfg (1.7 5.01/5.02 and the 4.xx dungeon chances). */
    public static final RegistryObject<StructurePlacementType<ConfigSpreadPlacement>> CONFIG_SPREAD =
            Reg.of(STRUCTURE_PLACEMENTS).register(Registries.STRUCTURE_PLACEMENT, "config_spread",
                    () -> Structures.placementType(ConfigSpreadPlacement.CODEC));

    static {
        ConfigSpreadPlacement.TYPE = CONFIG_SPREAD::get;
    }

    /** {@code hbm:nbt_component}: one placed jigsaw piece (1.7 {@code NBTStructure.Component}). */
    public static final RegistryObject<StructurePieceType> NBT_COMPONENT =
            REG.register(Registries.STRUCTURE_PIECE, "nbt_component", () -> (StructurePieceType) NBTStructure.Component::new);

    private static boolean registered;

    public static void init() {
        registerSpawns();
    }

    /** The 1.7 constructor's registrations, minus biome predicate and weight (see the class comment). */
    public static synchronized void registerSpawns() {
        if (registered) return;
        registered = true;

        single("spire", new JigsawPiece("spire", StructureManager.spire, -1));
        single("vertibird", new JigsawPiece("vertibird", StructureManager.vertibird, -3));
        single("crashed_vertibird", new JigsawPiece("crashed_vertibird", StructureManager.crashed_vertibird, -10));
        single("beached_patrol", new JigsawPiece("beached_patrol", StructureManager.beached_patrol, -5), 58, 67);
        single("aircraft_carrier", new JigsawPiece("aircraft_carrier", StructureManager.aircraft_carrier, -6), 1, 42);
        single("oil_rig", new JigsawPiece("oil_rig", StructureManager.oil_rig, -20), 11, 12);
        single("lighthouse", new JigsawPiece("lighthouse", StructureManager.lighthouse, -40), 28, 29);
        single("dish", new JigsawPiece("dish", StructureManager.dish, -10), 53, 65);
        single("water_pump", new JigsawPiece("water_pump", StructureManager.water_pump, -10));
        single("forestchem", new JigsawPiece("forest_chem", StructureManager.forest_chem, -9));
        single("labolatory", new JigsawPiece("laboratory", StructureManager.laboratory, -10), 53, 65);
        single("forest_post", new JigsawPiece("forest_post", StructureManager.forest_post, -10));
        single("radio", new JigsawPiece("radio_house", StructureManager.radio_house, -6));
        single("factory", new JigsawPiece("factory", StructureManager.factory, -10));
        single("crane", new JigsawPiece("crane", StructureManager.crane, -13));
        single("broadcaster_tower", new JigsawPiece("broadcaster_tower", StructureManager.broadcasting_tower, -9));
        single("plane1", new JigsawPiece("crashed_plane_1", StructureManager.plane1, -5));
        single("plane2", new JigsawPiece("crashed_plane_2", StructureManager.plane2, -8));
        single("desert_shack_1", new JigsawPiece("desert_shack_1", StructureManager.desert_shack_1, -7));
        single("desert_shack_2", new JigsawPiece("desert_shack_2", StructureManager.desert_shack_2, -7));
        single("desert_shack_3", new JigsawPiece("desert_shack_3", StructureManager.desert_shack_3, -5));
        single("dead_dish_small", new JigsawPiece("dead_dish_small", StructureManager.dead_dish_small, -5));
        ruin("ruinA", "NTMRuinsA", StructureManager.ntmruinsA);
        ruin("ruinB", "NTMRuinsB", StructureManager.ntmruinsB);
        ruin("ruinC", "NTMRuinsC", StructureManager.ntmruinsC);
        ruin("ruinD", "NTMRuinsD", StructureManager.ntmruinsD);
        ruin("ruinE", "NTMRuinsE", StructureManager.ntmruinsE);
        ruin("ruinF", "NTMRuinsF", StructureManager.ntmruinsF);
        ruin("ruinG", "NTMRuinsG", StructureManager.ntmruinsG);
        ruin("ruinH", "NTMRuinsH", StructureManager.ntmruinsH);
        ruin("ruinI", "NTMRuinsI", StructureManager.ntmruinsI);
        ruin("ruinJ", "NTMRuinsJ", StructureManager.ntmruinsJ);
        single("tower_base", new JigsawPiece("tower_base", StructureManager.tower_base, -6));
        single("falloutshelter_house", new JigsawPiece("falloutshelter_house", StructureManager.falloutshelter_house, -8));
        single("abandoned_arcticstation", new JigsawPiece("abandoned_arcticstation", StructureManager.abandoned_arcticstation, 0));
        single("military_base", new JigsawPiece("military_base", StructureManager.military_base, -5));
        single("submarine", new JigsawPiece("submarine", StructureManager.submarine, -10));

        // 1.7 registerNullWeight: a pick that spawns nothing, so plains and oceans stay emptier
        NBTStructure.registerStructure(new SpawnCondition(NTMStructureDeclarations.NULL_PLAINS));
        NBTStructure.registerStructure(new SpawnCondition(NTMStructureDeclarations.NULL_OCEAN));
    }

    private static void single(String name, JigsawPiece piece) {
        single(name, piece, 1, 128);
    }

    private static void single(String name, JigsawPiece piece, int minHeight, int maxHeight) {
        SpawnCondition spawn = new SpawnCondition(name);
        spawn.structure = piece;
        spawn.minHeight = minHeight;
        spawn.maxHeight = maxHeight;
        NBTStructure.registerStructure(spawn);
    }

    private static void ruin(String name, String pieceName, NBTStructure structure) {
        JigsawPiece piece = new JigsawPiece(pieceName, structure, -1);
        piece.conformToTerrain = true;
        single(name, piece);
    }
}
