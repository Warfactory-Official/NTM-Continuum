package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.MapColors;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** GitHub issue 144: HBM blocks carry their 1.7 map colours and a shape map mods draw. */
public final class Gh144To151GameTests {

    private Gh144To151GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function MAP_COLORS =
            TESTS.function("gh144_blocks_have_1_7_map_colors", 20, () -> Gh144To151GameTests::mapColors);

    /**
     * Blocks that keep an invisible shape: world fluids (drawn by the fluid renderer), and blocks whose blockstate
     * model has geometry the renderer replaces (dark fusion parts, pipe anchors, RBMK debris, particle accelerator,
     * sat link, Soyuz launcher).
     */
    private static final Set<String> KEPT_INVISIBLE = Set.of("acid_block", "corium_block", "mud_block", "rad_lava_block",
            "schrabidic_block", "sulfuric_acid_block", "toxic_block", "volcanic_lava_block", "dfc_core", "dfc_emitter",
            "dfc_injector", "dfc_receiver", "dfc_stabilizer", "pipe_anchor", "pipe_anchor_exhaust", "pipe_anchor_pneumatic",
            "rbmk_debris", "rbmk_debris_burning", "rbmk_debris_digamma", "rbmk_debris_radiating", "pa_beamline",
            "pa_detector", "pa_dipole", "pa_quadrupole", "pa_rfc", "pa_source", "machine_satlink", "soyuz_launcher");

    public static void init() {}

    public static void mapColors(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        List<String> blank = new ArrayList<>();
        Map<String, Block> byName = new HashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            if (!key.getNamespace().equals(Constants.MOD_ID)) continue;
            byName.put(key.getPath(), block);
            MapColor color = block.defaultBlockState().getMapColor(helper.getLevel(), pos);
            if (color == MapColor.NONE && MapColors.of(key.getPath()) != MapColor.NONE) blank.add(key.getPath());
        }
        helper.assertTrue(blank.isEmpty(), blank.size() + " of " + byName.size() + " blocks draw nothing on maps: "
                + String.join(" ", blank.subList(0, Math.min(blank.size(), 40))));
        expect(helper, byName, "machine_tower_small", MapColor.METAL, pos);
        expect(helper, byName, "concrete_smooth", MapColor.STONE, pos);
        expect(helper, byName, "sandbags", MapColor.DIRT, pos);
        expect(helper, byName, "crate", MapColor.WOOD, pos);
        expect(helper, byName, "taint", MapColor.COLOR_GRAY, pos);
        expect(helper, byName, "glass_boron", MapColor.NONE, pos);
        expect(helper, byName, "gas_radon", MapColor.NONE, pos);
        // Xaero's maps skip invisible render shapes; a block drawn only by its renderer bakes its empty model
        List<String> hidden = new ArrayList<>();
        for (Map.Entry<String, Block> entry : byName.entrySet()) {
            if (KEPT_INVISIBLE.contains(entry.getKey())) continue;
            for (BlockState state : entry.getValue().getStateDefinition().getPossibleStates()) {
                if (state.getRenderShape() != RenderShape.INVISIBLE) continue;
                if (state.getMapColor(helper.getLevel(), pos) == MapColor.NONE) continue;
                hidden.add(entry.getKey());
                break;
            }
        }
        hidden.sort(null);
        helper.assertTrue(hidden.isEmpty(), hidden.size() + " blocks are hidden from map mods: " + String.join(" ", hidden));
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, Map<String, Block> byName, String name, MapColor color,
            BlockPos pos) {
        MapColor actual = byName.get(name).defaultBlockState().getMapColor(helper.getLevel(), pos);
        helper.assertTrue(actual == color, "hbm:" + name + " map colour " + actual.id + ", 1.7 " + color.id);
    }
}
