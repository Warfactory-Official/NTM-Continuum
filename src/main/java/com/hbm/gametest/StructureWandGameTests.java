package com.hbm.gametest;

import com.hbm.blocks.generic.BlockWandJigsaw;
import com.hbm.blocks.generic.ModStructureWandBlocks;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import com.hbm.world.gen.nbt.NBTStructure;
import dev.golem.core.data.NbtFiles;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;

/**
 * The 1.7 NBT structure system without a client: the legacy palette map against 1.7's metadata layouts, a bundled
 * 1.7 structure built with its jigsaw placeholder replaced, and a port-written structure saved and built back.
 */
public final class StructureWandGameTests {

    private StructureWandGameTests() {}

    private static String id(BlockState state) {
        return String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    /** Hand-read from 1.7's metadata tables (BlockStateData for vanilla, the 1.7 classes for HBM). */
    public static void legacyPalette(net.minecraft.gametest.framework.GameTestHelper helper) {
        BlockState granite = LegacyStructurePalette.resolve("minecraft:stone", 1).state();
        helper.assertTrue(granite.is(Blocks.GRANITE), "stone:1 is granite, got " + granite);

        BlockState oakStairs = LegacyStructurePalette.resolve("minecraft:oak_stairs", 6).state();
        helper.assertTrue(oakStairs.getValue(StairBlock.FACING) == Direction.SOUTH && oakStairs.getValue(StairBlock.HALF) == Half.TOP,
                "oak_stairs:6 faces south, upside down: " + oakStairs);

        BlockState hbmStairs = LegacyStructurePalette.resolve("hbm:tile.reinforced_brick_stairs", 3).state();
        helper.assertTrue(id(hbmStairs).equals("hbm:reinforced_brick_stairs") && hbmStairs.getValue(StairBlock.FACING) == Direction.NORTH,
                "reinforced_brick_stairs:3 faces north: " + hbmStairs);

        BlockState slab = LegacyStructurePalette.resolve("hbm:tile.brick_slab", 9).state();
        helper.assertTrue(id(slab).equals("hbm:reinforced_brick_slab") && LegacyStructurePalette.stateString(slab).contains("type=top"),
                "brick_slab:9 is the top reinforced brick slab: " + slab);

        BlockState crt = LegacyStructurePalette.resolve("hbm:tile.deco_crt", 5).state();
        helper.assertTrue(id(crt).equals("hbm:deco_crt_broken") && LegacyStructurePalette.stateString(crt).contains("facing=west"),
                "deco_crt:5 is the broken CRT looking west: " + crt);

        LegacyStructurePalette.Resolved core = LegacyStructurePalette.resolve("hbm:tile.machine_fluidtank", 13);
        LegacyStructurePalette.Resolved cell = LegacyStructurePalette.resolve("hbm:tile.machine_fluidtank", 3);
        helper.assertTrue(core.kind() == LegacyStructurePalette.Kind.CORE && LegacyStructurePalette.stateString(core.state()).contains("facing=south"),
                "fluid tank meta 13 is a core facing south: " + core);
        helper.assertTrue(cell.kind() == LegacyStructurePalette.Kind.CELL, "fluid tank meta 3 is a cell: " + cell);

        BlockState door = LegacyStructurePalette.door(LegacyStructurePalette.resolve("minecraft:wooden_door", 1).state(), 1, 9);
        helper.assertTrue(LegacyStructurePalette.stateString(door).contains("hinge=right")
                        && LegacyStructurePalette.stateString(door).contains("facing=south")
                        && LegacyStructurePalette.stateString(door).contains("half=lower"),
                "wooden_door 1 under 9 is a lower south door hinged right: " + door);

        // rotation in metadata space: a stair facing east (0) turned 90 degrees clockwise faces south (2)
        helper.assertTrue(LegacyStructurePalette.transform("minecraft:oak_stairs", 0, 1).meta() == 2, "stair rotation");
        // spotlights are switched off and marked broken on every build
        LegacyStructurePalette.Legacy spot = LegacyStructurePalette.transform("hbm:tile.spotlight_halogen", 4, 0);
        helper.assertTrue(spot.name().equals("hbm:tile.spotlight_halogen_off") && spot.meta() == 5, "spotlight transform " + spot);

        helper.assertTrue(LegacyStructurePalette.resolve("hbm:tile.barrel_iron", 0).state().isAir(), "an unknown 1.7 block is air");
        helper.succeed();
    }

    /** meteor-3-dirt: a 3x4x3 plate of polished meteorite with dirt on top and a jigsaw in the middle of the plate. */
    public static void bundledStructure(net.minecraft.gametest.framework.GameTestHelper helper) {
        NBTStructure structure = new NBTStructure("hbm", "structures/meteor/loot3x3/meteor-3-dirt.nbt");
        helper.assertTrue(structure.isLoaded() && structure.isLegacy(), "the bundled 1.7 file loads as a legacy structure");
        helper.assertTrue(structure.getSizeX() == 3 && structure.getSizeY() == 4 && structure.getSizeZ() == 3, "size 3x4x3");

        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        structure.build(helper.getLevel(), origin.getX(), origin.getY(), origin.getZ(), 0, false, false);

        String polished = "hbm:meteor_polished";
        helper.assertTrue(id(helper.getBlockState(new BlockPos(1, 1, 1))).equals(polished), "corner");
        // the jigsaw is replaced by its configured block (1.7 "block": hbm:tile.meteor_polished)
        helper.assertTrue(id(helper.getBlockState(new BlockPos(2, 1, 2))).equals(polished),
                "jigsaw replaced: " + helper.getBlockState(new BlockPos(2, 1, 2)));
        helper.assertTrue(id(helper.getBlockState(new BlockPos(2, 2, 2))).equals("hbm:ntm_dirt"), "dirt on top");
        helper.succeed();
    }

    /** A port-saved box (stairs, a jigsaw with a replacement, wand_air) written, read back and built beside it. */
    public static void saveAndLoad(net.minecraft.gametest.framework.GameTestHelper helper) {
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        helper.setBlock(new BlockPos(1, 1, 1), stairs);
        helper.setBlock(new BlockPos(2, 1, 1), ModStructureWandBlocks.wand_jigsaw.get().defaultBlockState()
                .setValue(BlockWandJigsaw.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(1, 2, 1), ModStructureWandBlocks.wand_air.get().defaultBlockState());
        BlockWandJigsaw.TileEntityWandJigsaw jigsaw = GameTestHelpers.blockEntity(helper, new BlockPos(2, 1, 1),
                BlockWandJigsaw.TileEntityWandJigsaw.class);
        helper.assertTrue(jigsaw != null, "jigsaw block entity");
        jigsaw.replaceState = Blocks.GOLD_BLOCK.defaultBlockState();
        jigsaw.pool = "halls";

        BlockPos a = helper.absolutePos(new BlockPos(1, 1, 1)), b = helper.absolutePos(new BlockPos(2, 2, 1));
        CompoundTag saved = NBTStructure.saveArea(helper.getLevel(), a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(),
                Set.of(Blocks.AIR.defaultBlockState()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            NbtFiles.writeCompressed(saved, out);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }

        NBTStructure structure = new NBTStructure("roundtrip.nbt", new ByteArrayInputStream(out.toByteArray()));
        helper.assertTrue(structure.isLoaded() && !structure.isLegacy(), "the saved file loads as a port structure");

        // something to be cleared by the saved wand_air
        helper.setBlock(new BlockPos(1, 2, 4), Blocks.STONE.defaultBlockState());
        BlockPos target = helper.absolutePos(new BlockPos(1, 1, 4));
        structure.build(helper.getLevel(), target.getX(), target.getY(), target.getZ(), 0, false, false);

        helper.assertTrue(helper.getBlockState(new BlockPos(1, 1, 4)).equals(stairs), "stairs kept their facing: " + helper.getBlockState(new BlockPos(1, 1, 4)));
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 4)).is(Blocks.GOLD_BLOCK), "jigsaw built as its replacement");
        helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 4)).isAir(), "wand_air saved as air and cleared the stone");
        helper.succeed();
    }
}
