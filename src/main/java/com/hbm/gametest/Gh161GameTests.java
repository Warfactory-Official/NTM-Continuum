package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.network.FluidDuctGauge;
import com.hbm.tileentity.deco.TileEntityBobble;
import com.hbm.world.gen.NTMComponentStructures;
import com.hbm.world.gen.NTMWorldGenerator;
import com.hbm.world.gen.component.SiloComponent;
import com.hbm.world.gen.nbt.JigsawPiece;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** GitHub issue 161: structure blocks face as placed, structure bobbleheads are random, silo multiblocks stand whole. */
public final class Gh161GameTests {

    private Gh161GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function PALETTE_FACING =
            TESTS.function("gh161_structure_blocks_face_as_placed", 20, 4, () -> Gh161GameTests::paletteFacing);
    public static final GameTests.Function TURN =
            TESTS.function("gh161_structure_blocks_turn_with_the_structure", 20, 4, () -> Gh161GameTests::turn);
    public static final GameTests.Function BOBBLEHEADS =
            TESTS.function("gh161_structure_bobbleheads_are_random", 40, 20, () -> Gh161GameTests::bobbleheads);
    public static final GameTests.Function SILO_SOUTH =
            TESTS.function("gh161_silo_multiblocks_stand_whole_rot0", 80, 56, () -> helper -> silo(helper, 0));
    public static final GameTests.Function SILO_WEST =
            TESTS.function("gh161_silo_multiblocks_stand_whole_rot1", 80, 56, () -> helper -> silo(helper, 1));
    public static final GameTests.Function SILO_NORTH =
            TESTS.function("gh161_silo_multiblocks_stand_whole_rot2", 80, 56, () -> helper -> silo(helper, 2));
    public static final GameTests.Function SILO_EAST =
            TESTS.function("gh161_silo_multiblocks_stand_whole_rot3", 80, 56, () -> helper -> silo(helper, 3));

    public static void init() {}

    /** 1.7 {@code onBlockPlacedBy} metadata for the look quadrants south, west, north, east. */
    private static final int[] FACES_PLACER = {2, 5, 3, 4};
    private static final int[] FACES_LOOK = {3, 4, 2, 5};
    private static final int[] SKELETON = {5, 3, 4, 2};

    /**
     * A structure block resolves to the state a player standing where the 1.7 placer stood gets: the 1.7 metadata
     * of each look quadrant ({@code onBlockPlacedBy}) through the palette, against the port block's own placement.
     */
    private static void paletteFacing(GameTestHelper helper) {
        Map<String, int[]> table = new TreeMap<>();
        for (String id : List.of("tape_recorder", "pole_satellite_receiver", "steel_poles", "charger", "machine_diesel",
                "machine_microwave", "machine_electric_furnace_off", "machine_boiler_off", "crate_iron")) table.put(id, FACES_PLACER);
        for (String id : List.of("steel_beam", "steel_wall", "steel_corner", "steel_roof", "geiger", "radiorec")) table.put(id, FACES_LOOK);
        table.put("skeleton_holder", SKELETON);

        Player player = GameTestHelpers.mockPlayer(helper, GameType.CREATIVE);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        List<String> wrong = new ArrayList<>();
        for (var entry : table.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.getOptional(Ids.of("hbm", entry.getKey())).orElse(null);
            helper.assertTrue(block != null, "hbm:" + entry.getKey() + " is registered");
            for (int quadrant = 0; quadrant < 4; quadrant++) {
                player.setYRot(quadrant * 90F);
                ItemStack stack = new ItemStack(block.asItem());
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                BlockState placed = block.getStateForPlacement(
                        new BlockPlaceContext(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit)));
                BlockState fromStructure = LegacyStructurePalette.legacyState("hbm:tile." + entry.getKey(), entry.getValue()[quadrant]);
                Direction a = placed.getValue(BlockStateProperties.HORIZONTAL_FACING);
                Direction b = fromStructure.getValue(BlockStateProperties.HORIZONTAL_FACING);
                if (a != b) wrong.add(entry.getKey() + " quadrant " + quadrant + ": placed " + a + ", structure " + b);
            }
        }
        helper.assertTrue(wrong.isEmpty(), "structure facings differ from placement: " + wrong);
        helper.succeed();
    }

    /**
     * Every HBM block of every NBT structure palette, placed in rotation 1..3, holds its rotation-0 state turned that
     * many quarters clockwise (facings, gauges, axes, scaffold orientations, 16-step rotations, rail shapes), including
     * the blocks 1.7 left unturned.
     */
    private static void turn(GameTestHelper helper) {
        NTMWorldGenerator.registerSpawns();
        NTMComponentStructures.registerSpawns();
        Map<String, Integer> wrong = new TreeMap<>();
        List<String> examples = new ArrayList<>();
        int checked = 0;
        for (JigsawPiece piece : JigsawPiece.all()) {
            for (LegacyStructurePalette.Legacy entry : piece.structure.legacyPalette()) {
                String name = entry.name();
                if (!name.startsWith("hbm:") || name.contains("wand_")) continue;
                LegacyStructurePalette.Resolved unturned = LegacyStructurePalette.placed(name, entry.meta(), 0);
                if (unturned.kind() != LegacyStructurePalette.Kind.STATE && unturned.kind() != LegacyStructurePalette.Kind.CORE) continue;
                String id = BuiltInRegistries.BLOCK.getKey(unturned.state().getBlock()).getPath();
                for (int rot = 1; rot < 4; rot++) {
                    BlockState found = LegacyStructurePalette.placed(name, entry.meta(), rot).state();
                    String expected = turning(turned(unturned.state(), rot)), got = turning(found);
                    if (expected.isEmpty()) continue;
                    checked++;
                    if (!expected.equals(got)) {
                        wrong.merge(id, 1, Integer::sum);
                        if (examples.size() < 10) examples.add(name + ":" + entry.meta() + " rot " + rot + " expected " + expected + " found " + got);
                    }
                }
            }
        }
        helper.assertTrue(checked > 100 && wrong.isEmpty(), checked + " checks, blocks not turned with the structure: " + wrong + " " + examples);
        helper.succeed();
    }

    /** {@code state} turned {@code quarters} times clockwise, property by property. */
    private static BlockState turned(BlockState state, int quarters) {
        if (state.getBlock() instanceof BaseRailBlock) {
            BlockState s = state;
            for (int i = 0; i < quarters; i++) s = s.rotate(Rotation.CLOCKWISE_90);
            return s;
        }
        BlockState s = state;
        for (Property<?> property : state.getProperties()) {
            String key = property.getName(), value = valueName(state, property);
            switch (key) {
                case "facing", "gauge" -> {
                    Direction d = Direction.byName(value);
                    if (d == null || !d.getAxis().isHorizontal()) continue;
                    for (int i = 0; i < quarters; i++) d = d.getClockWise();
                    s = LegacyStructurePalette.with(s, key, d);
                }
                case "axis" -> {
                    if (quarters % 2 == 1) s = LegacyStructurePalette.with(s, key, value.equals("x") ? "z" : value.equals("z") ? "x" : value);
                }
                case "orient" -> {
                    if (quarters % 2 == 1) s = LegacyStructurePalette.with(s, key, value.contains("north_south")
                            ? value.replace("north_south", "east_west") : value.replace("east_west", "north_south"));
                }
                case "rotation" -> {
                    if (property.getPossibleValues().size() == 16) s = LegacyStructurePalette.with(s, key, (Integer.parseInt(value) + 4 * quarters) % 16);
                }
                default -> { }
            }
        }
        return s;
    }

    /** The properties that turn; a hanging chain's and a duct gauge's own {@code facing} draw nothing. */
    private static String turning(BlockState state) {
        StringBuilder out = new StringBuilder();
        boolean ignoreFacing = state.getBlock() instanceof FluidDuctGauge
                || state.getProperties().stream().anyMatch(p -> p.getName().equals("wall") && valueName(state, p).equals("false"));
        for (Property<?> property : state.getProperties()) {
            String key = property.getName();
            if (key.equals("facing") && ignoreFacing) continue;
            if (Set.of("facing", "gauge", "axis", "orient", "rotation").contains(key)
                    || key.equals("shape") && state.getBlock() instanceof BaseRailBlock) {
                out.append(key).append('=').append(valueName(state, property)).append(' ');
            }
        }
        return out.toString().trim();
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /** tower_base saves MELLOW; 1.7's {@code transformTE} draws any figure but NONE on every build. */
    private static void bobbleheads(GameTestHelper helper) {
        NTMWorldGenerator.registerSpawns();
        ServerLevel level = helper.getLevel();
        SpawnCondition spawn = NBTStructure.getStructure("tower_base");
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        Set<String> types = new LinkedHashSet<>();
        for (int build = 0; build < 8; build++) {
            NBTStructure.Component piece = new NBTStructure.Component(spawn, spawn.structure, origin.getX(), origin.getY(), origin.getZ(), 0);
            BoundingBox box = piece.getBoundingBox();
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
                    box, new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4), origin);
            if (level.getBlockEntity(origin.offset(5, 22, 10)) instanceof TileEntityBobble bobble) types.add(bobble.type().name());
            else types.add("missing");
        }
        helper.assertTrue(types.size() >= 3 && !types.contains("NONE") && !types.contains("missing"),
                "eight tower_base builds draw their bobblehead at random: " + types);
        helper.succeed();
    }

    /** The silo built chunk by chunk in a shuffled order: hatch, turret and launch pad each with every cell. */
    private static void silo(GameTestHelper helper, int rot) {
        NTMComponentStructures.registerSpawns();
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        List<String> report = new ArrayList<>();
        long seed = 161L;
        while (RandomSource.create(seed).nextInt(4) != rot) seed++;
        RandomSource rand = RandomSource.create(seed);
        SiloComponent silo = new SiloComponent(rand, 0, 0);
        silo.move(origin.getX() - silo.getBoundingBox().minX(), 0, origin.getZ() - silo.getBoundingBox().minZ());
        // the box floor at the test floor, as wgc_silo; y -1 is 1.7's "no height yet" marker
        silo.fixHeight(origin.getY() == -1 ? 0 : origin.getY());
        BoundingBox b = silo.getBoundingBox();
        List<BoundingBox> chunks = new ArrayList<>();
        for (int cx = (b.minX() - 2) >> 4; cx <= (b.maxX() + 2) >> 4; cx++) {
            for (int cz = (b.minZ() - 2) >> 4; cz <= (b.maxZ() + 2) >> 4; cz++) {
                chunks.add(new BoundingBox(cx << 4, level.getMinY(), cz << 4, (cx << 4) + 15, level.getMaxY() - 1, (cz << 4) + 15));
            }
        }
        Collections.shuffle(chunks, new Random(seed));
        for (BoundingBox chunk : chunks) silo.build(level, rand, chunk);

        Set<String> whole = new LinkedHashSet<>();
        for (int[] at : new int[][] {{19, 26, 14}, {29, 27, 15}, {19, 1, 14}}) {
            BlockPos core = new BlockPos(silo.getXWithOffset(at[0], at[2]), silo.getYWithOffset(at[1]), silo.getZWithOffset(at[0], at[2]));
            BlockState state = level.getBlockState(core);
            String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            if (!(state.getBlock() instanceof BlockMultiblockCore block) || block.isCellState(state)) {
                report.add("rot " + silo.getCoordMode() + ": no core at " + core + " but " + state);
                continue;
            }
            int[] counts = new int[3];
            block.visitPlacedCells(level, core, state.getValue(BlockMultiblockCore.FACING), (cell, mask) -> {
                counts[0]++;
                if (block.isCellOf(level.getBlockState(cell))) counts[1]++;
                if (MultiblockCoreIndex.lookup(level, cell) == core.asLong()) counts[2]++;
            });
            if (counts[1] != counts[0] || counts[2] != counts[0]) {
                report.add("rot " + silo.getCoordMode() + " " + name + ": " + counts[1] + "/" + counts[0] + " cells, " + counts[2] + " indexed");
            } else {
                whole.add(name);
            }
        }
        if (whole.size() != 3) report.add("rot " + silo.getCoordMode() + " whole: " + whole);
        Constants.LOG.info("[Gh161] silo rot {} box {} whole {}", silo.getCoordMode(), b, whole);
        helper.assertTrue(report.isEmpty(), "silo multiblocks: " + report);
        helper.succeed();
    }
}
