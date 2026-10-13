package com.hbm.world.gen;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.main.StructureManager;
import com.hbm.world.gen.component.BunkerComponents;
import com.hbm.world.gen.component.CivilianFeatures;
import com.hbm.world.gen.component.Component.BlockSelector;
import com.hbm.world.gen.component.Component.CrabSpawners;
import com.hbm.world.gen.component.Component.GreenOoze;
import com.hbm.world.gen.component.Component.MeteorBricks;
import com.hbm.world.gen.component.Component.SupplyCrates;
import com.hbm.world.gen.component.OfficeFeatures;
import com.hbm.world.gen.component.SiloComponent;
import com.hbm.world.gen.nbt.JigsawPiece;
import com.hbm.world.gen.nbt.JigsawPool;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.HashMap;
import java.util.Map;

/**
 * The multi-piece half of 1.7's {@code NTMWorldGenerator} (WG-C): the {@code features}, {@code bunker} and
 * {@code meteor_dungeon} spawns, and the piece types of the hardcoded buildings (1.7 {@code HbmWorld.registerNTMFeatures}).
 * They go into the same region pick as the single-piece NBT structures ({@link NTMWorldGenerator}): each is an
 * {@code hbm:nbt} structure in the {@code hbm:ntm_structures} set, with its 1.7 weight and biome rule declared in
 * {@link NTMStructureDeclarations}.
 *
 * <p><b>Why the meteor dungeon is not a vanilla jigsaw.</b> Vanilla's jigsaw assembler differs from 1.7's where the
 * dungeon depends on it: 1.7 grows by placement priority with a random pick among equals, shuffles each selection
 * priority's connectors, keeps drawing from a pool until a piece fits, then falls back (ignoring bounds unless the
 * target block is already inside another piece, so grid layouts close), switches to fallbacks only once the size
 * limit (128) is reached, keeps loot pools going past the limit through a self-fallback, lets a piece follow the
 * terrain column by column (the spike), and swaps blocks per piece ({@code blockTable}). The ported 1.7 assembler
 * ({@link NBTStructure.Start}) does all of that on vanilla's structure system, so the dungeon is its 1.7
 * {@link SpawnCondition} unchanged; converting the pieces to template pools would have to re-invent each rule.
 */
public final class NTMComponentStructures {

    private NTMComponentStructures() {}

    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(PIECES);

    // 1.7 MapGenStructureIO names, lower-cased
    public static final RegistryObject<StructurePieceType> NTM_HOUSE_1 = piece("ntmhouse1", CivilianFeatures.NTMHouse1::new);
    public static final RegistryObject<StructurePieceType> NTM_HOUSE_2 = piece("ntmhouse2", CivilianFeatures.NTMHouse2::new);
    public static final RegistryObject<StructurePieceType> NTM_LAB_1 = piece("ntmlab1", CivilianFeatures.NTMLab1::new);
    public static final RegistryObject<StructurePieceType> NTM_LAB_2 = piece("ntmlab2", CivilianFeatures.NTMLab2::new);
    public static final RegistryObject<StructurePieceType> NTM_RURAL_HOUSE_1 = piece("ntmruralhouse1", CivilianFeatures.RuralHouse1::new);
    public static final RegistryObject<StructurePieceType> NTM_LARGE_OFFICE = piece("ntmlargeoffice", OfficeFeatures.LargeOffice::new);
    public static final RegistryObject<StructurePieceType> NTM_LARGE_OFFICE_CORNER = piece("ntmlargeofficecorner", OfficeFeatures.LargeOfficeCorner::new);
    public static final RegistryObject<StructurePieceType> NTM_SILO_COMPONENT = piece("ntmsilocomponent", SiloComponent::new);
    public static final RegistryObject<StructurePieceType> NTMB_STARTING_HUB = piece("ntmbstartinghub", BunkerComponents.StartingHub::new);
    public static final RegistryObject<StructurePieceType> NTMB_CORRIDOR = piece("ntmbcorridor", BunkerComponents.Corridor::new);
    public static final RegistryObject<StructurePieceType> NTMB_BEDROOM_L = piece("ntmbbedrooml", BunkerComponents.BedroomL::new);
    public static final RegistryObject<StructurePieceType> NTMB_FUN_JUNCTION = piece("ntmbfunjunction", BunkerComponents.FunJunction::new);
    public static final RegistryObject<StructurePieceType> NTMB_BATHROOM_L = piece("ntmbbathrooml", BunkerComponents.BathroomL::new);
    public static final RegistryObject<StructurePieceType> NTMB_LABORATORY = piece("ntmblaboratory", BunkerComponents.Laboratory::new);
    public static final RegistryObject<StructurePieceType> NTMB_POWER_ROOM = piece("ntmbpowerroom", BunkerComponents.PowerRoom::new);

    private static RegistryObject<StructurePieceType> piece(String name, StructurePieceType type) {
        return REG.register(Registries.STRUCTURE_PIECE, name, () -> type);
    }

    public static final String FEATURES = "features";
    public static final String BUNKER = "bunker";
    public static final String METEOR_DUNGEON = "meteor_dungeon";

    private static boolean registered;

    public static void init() {
        registerSpawns();
    }

    /** 1.7 {@code NTMWorldGenerator}'s constructor, the multi-piece registrations (weights and biomes: data). */
    public static synchronized void registerSpawns() {
        if (registered) return;
        registered = true;

        SpawnCondition features = new SpawnCondition(FEATURES);
        features.start = MapGenNTMFeatures::start;
        NBTStructure.registerStructure(features);

        SpawnCondition bunker = new SpawnCondition(BUNKER);
        bunker.start = (builder, context) -> new BunkerComponents.BunkerStart(context.random(),
                context.chunkPos().getMinBlockX() >> 4, context.chunkPos().getMinBlockZ() >> 4).addTo(builder);
        NBTStructure.registerStructure(bunker);

        NBTStructure.registerStructure(meteorDungeon());
    }

    private static JigsawPiece piece(String name, NBTStructure structure, Map<String, BlockSelector> blockTable) {
        JigsawPiece piece = new JigsawPiece(name, structure);
        piece.blockTable = blockTable;
        return piece;
    }

    /** 1.7 {@code NTMWorldGenerator}: {@code meteor_dungeon} with 1.7's pools, weights, fallbacks and block tables. */
    private static SpawnCondition meteorDungeon() {
        // 1.7 shared one selector per table entry across the whole dungeon; the selectors synchronise their pick
        MeteorBricks meteorBricks = new MeteorBricks();
        Map<String, BlockSelector> bricks = new HashMap<>();
        bricks.put("meteor_brick", meteorBricks);
        Map<String, BlockSelector> crates = new HashMap<>();
        crates.put("meteor_brick", new MeteorBricks());
        crates.put("crate", new SupplyCrates());
        crates.put("meteor_spawner", new CrabSpawners());
        Map<String, BlockSelector> ooze = new HashMap<>();
        ooze.put("meteor_brick", new MeteorBricks());
        ooze.put("concrete_colored", new GreenOoze());

        SpawnCondition spawn = new SpawnCondition(METEOR_DUNGEON);
        spawn.minHeight = 32;
        spawn.maxHeight = 32;
        spawn.sizeLimit = 128;
        spawn.startPool = "start";
        spawn.pools = new HashMap<>();

        JigsawPool start = new JigsawPool();
        start.add(piece("meteor_core", StructureManager.meteor_core, bricks), 1);
        spawn.pools.put("start", start);

        JigsawPool spike = new JigsawPool();
        JigsawPiece meteorSpike = new JigsawPiece("meteor_spike", StructureManager.meteor_spike);
        meteorSpike.heightOffset = -3;
        meteorSpike.conformToTerrain = true;
        spike.add(meteorSpike, 1);
        spawn.pools.put("spike", spike);

        JigsawPool defaultPool = new JigsawPool();
        defaultPool.add(piece("meteor_corner", StructureManager.meteor_corner, bricks), 2);
        defaultPool.add(piece("meteor_t", StructureManager.meteor_t, bricks), 3);
        defaultPool.add(piece("meteor_stairs", StructureManager.meteor_stairs, bricks), 1);
        defaultPool.add(piece("meteor_room_base_thru", StructureManager.meteor_room_base_thru, bricks), 3);
        defaultPool.add(piece("meteor_room_base_end", StructureManager.meteor_room_base_end, bricks), 4);
        defaultPool.fallback = "fallback";
        spawn.pools.put("default", defaultPool);

        JigsawPool room10 = new JigsawPool();
        room10.add(piece("meteor_room_basic", StructureManager.meteor_room_basic, bricks), 1);
        room10.add(piece("meteor_room_balcony", StructureManager.meteor_room_balcony, bricks), 1);
        room10.add(piece("meteor_room_dragon", StructureManager.meteor_room_dragon, bricks), 1);
        room10.add(piece("meteor_room_ladder", StructureManager.meteor_room_ladder, bricks), 1);
        room10.add(piece("meteor_room_ooze", StructureManager.meteor_room_ooze, ooze), 1);
        room10.add(piece("meteor_room_split", StructureManager.meteor_room_split, bricks), 1);
        room10.add(piece("meteor_room_stairs", StructureManager.meteor_room_stairs, bricks), 1);
        room10.add(piece("meteor_room_triple", StructureManager.meteor_room_triple, bricks), 1);
        room10.fallback = "roomback";
        spawn.pools.put("10room", room10);

        JigsawPool loot3 = new JigsawPool();
        loot3.add(new JigsawPiece("meteor_3_bale", StructureManager.meteor_3_bale), 1);
        loot3.add(new JigsawPiece("meteor_3_blank", StructureManager.meteor_3_blank), 1);
        loot3.add(new JigsawPiece("meteor_3_block", StructureManager.meteor_3_block), 1);
        loot3.add(new JigsawPiece("meteor_3_crab", StructureManager.meteor_3_crab), 1);
        loot3.add(new JigsawPiece("meteor_3_crab_tesla", StructureManager.meteor_3_crab_tesla), 1);
        loot3.add(new JigsawPiece("meteor_3_crate", StructureManager.meteor_3_crate), 1);
        loot3.add(new JigsawPiece("meteor_3_dirt", StructureManager.meteor_3_dirt), 1);
        loot3.add(new JigsawPiece("meteor_3_lead", StructureManager.meteor_3_lead), 1);
        loot3.add(new JigsawPiece("meteor_3_ooze", StructureManager.meteor_3_ooze), 1);
        loot3.add(new JigsawPiece("meteor_3_pillar", StructureManager.meteor_3_pillar), 1);
        loot3.add(new JigsawPiece("meteor_3_star", StructureManager.meteor_3_star), 1);
        loot3.add(new JigsawPiece("meteor_3_tesla", StructureManager.meteor_3_tesla), 1);
        loot3.add(new JigsawPiece("meteor_3_book", StructureManager.meteor_3_book), 1);
        loot3.add(new JigsawPiece("meteor_3_mku", StructureManager.meteor_3_mku), 1);
        loot3.add(new JigsawPiece("meteor_3_statue", StructureManager.meteor_3_statue), 1);
        loot3.add(new JigsawPiece("meteor_3_glow", StructureManager.meteor_3_glow), 1);
        loot3.fallback = "3x3loot"; // generate loot even if we're at the size limit
        spawn.pools.put("3x3loot", loot3);

        JigsawPool headloot = new JigsawPool();
        headloot.add(piece("meteor_dragon_chest", StructureManager.meteor_dragon_chest, crates), 1);
        headloot.add(piece("meteor_dragon_tesla", StructureManager.meteor_dragon_tesla, crates), 1);
        headloot.add(piece("meteor_dragon_trap", StructureManager.meteor_dragon_trap, crates), 1);
        headloot.add(piece("meteor_dragon_crate_crab", StructureManager.meteor_dragon_crate_crab, crates), 1);
        headloot.fallback = "headback";
        spawn.pools.put("headloot", headloot);

        JigsawPool fallback = new JigsawPool();
        fallback.add(piece("meteor_fallback", StructureManager.meteor_fallback, bricks), 1);
        spawn.pools.put("fallback", fallback);

        JigsawPool roomback = new JigsawPool();
        roomback.add(piece("meteor_room_fallback", StructureManager.meteor_room_fallback, bricks), 1);
        spawn.pools.put("roomback", roomback);

        JigsawPool headback = new JigsawPool();
        headback.add(piece("meteor_loot_fallback", StructureManager.meteor_dragon_fallback, crates), 1);
        spawn.pools.put("headback", headback);

        return spawn;
    }
}
