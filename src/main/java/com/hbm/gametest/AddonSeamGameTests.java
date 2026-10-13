package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.computer.ComputerComponents;
import com.hbm.blocks.generic.BlockMetalFence;
import com.hbm.blocks.machine.struct.StructPattern;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.material.Mats;
import com.hbm.items.tool.ItemSurveyScanner;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.lib.Library;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The namespace and start-up seams an addon (NTM: Space) goes through (ADDON-API-INVENTORY.md sections N, S, P): ids an
 * addon writes as {@code "ns:path"} keep their namespace, a bare path stays HBM's, and the public registries reject a
 * taken name instead of silently replacing HBM's entry. None of these tests adds anything to a registry.
 */
public final class AddonSeamGameTests {

    private AddonSeamGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function IDS =
            TESTS.function("addon_seam_namespaced_ids", 20, () -> AddonSeamGameTests::namespacedIds);
    public static final GameTests.Function BULLETS =
            TESTS.function("addon_seam_bullet_config_ids", 20, () -> AddonSeamGameTests::bulletConfigIds);
    public static final GameTests.Function MATERIALS =
            TESTS.function("addon_seam_material_register", 20, () -> AddonSeamGameTests::materialRegister);
    public static final GameTests.Function PALETTE =
            TESTS.function("addon_seam_legacy_palette_namespaces", 20, () -> AddonSeamGameTests::legacyPalette);
    public static final GameTests.Function SCANNER =
            TESTS.function("addon_seam_survey_scanner_findings", 20, () -> AddonSeamGameTests::surveyScanner);

    public static final GameTests.Function COMPUTER_COMPONENTS =
            TESTS.function("addon_seam_computer_components", 20, () -> AddonSeamGameTests::computerComponents);
    public static final GameTests.Function FENCE_CONNECTS =
            TESTS.function("addon_seam_metal_fence_connects", 20, 1, () -> AddonSeamGameTests::fenceConnects);

    public static void init() {}

    /**
     * {@link com.hbm.api.computer.ComputerComponents} (inventory H-61): HBM registers nothing, so no HBM block entity or
     * block is served through it; a component calls its methods by name with the Lua arguments, in order, and refuses
     * an unknown name. Nothing is registered here (the record is built directly).
     */
    private static void computerComponents(GameTestHelper helper) {
        for (ComputerComponents.Component<?> component : ComputerComponents.all()) {
            helper.assertTrue(!Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(component.blocks().get(0).get()).getNamespace()),
                    "HBM must not serve its own blocks through ComputerComponents: " + component.name());
        }
        helper.assertTrue(!ComputerComponents.carries(Blocks.STONE), "stone is no component block");
        Map<String, ComputerComponents.Method<BlockEntity>> methods =
                new LinkedHashMap<>();
        methods.put("echo", (machine, args) -> args);
        methods.put("first", (machine, args) -> new Object[] {args.length == 0 ? null : args[0]});
        ComputerComponents.Component<BlockEntity> component =
                new ComputerComponents.Component<>(BlockEntity.class,
                        "test", methods, List.of());
        helper.assertTrue(List.copyOf(component.methods().keySet()).equals(List.of("echo", "first")), "method order");
        Object[] echoed = component.call(null, "echo", "a", 2.0);
        helper.assertTrue(Arrays.equals(echoed, new Object[] {"a", 2.0}), "echo returned " + Arrays.toString(echoed));
        boolean refused = false;
        try {
            component.call(null, "missing");
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        helper.assertTrue(refused, "an unknown method must be refused");
        helper.succeed();
    }

    /**
     * Block tag {@code hbm:metal_fence_connects} (inventory H-62): HBM puts nothing in it, so plain HBM's chain-link fence
     * still joins only fences, fence gates and opaque full blocks (1.7 {@code canConnectFenceTo}).
     */
    private static void fenceConnects(GameTestHelper helper) {
        helper.assertTrue(BlockMetalFence.Connects.TAG.location().equals(Ids.of("hbm", "metal_fence_connects")),
                "tag id");
        Block fence = BuiltInRegistries.BLOCK.getOptional(Ids.of("hbm", "fence_metal")).orElse(Blocks.AIR);
        helper.assertTrue(fence instanceof BlockMetalFence, "hbm:fence_metal is the chain-link fence");
        BlockMetalFence metal = (BlockMetalFence) fence;
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, Blocks.OAK_FENCE_GATE.defaultBlockState(), 3);
        helper.assertTrue(metal.canConnectFenceTo(helper.getLevel(), pos, Direction.EAST), "joins a fence gate");
        helper.getLevel().setBlock(pos, Blocks.TORCH.defaultBlockState(), 3);
        helper.assertTrue(!metal.canConnectFenceTo(helper.getLevel(), pos, Direction.EAST), "joins a torch");
        helper.succeed();
    }

    private static void namespacedIds(GameTestHelper helper) {
        helper.assertTrue(Library.parseId("block_steel").equals(Ids.of("hbm", "block_steel")),
                "a bare path must stay HBM's, got " + Library.parseId("block_steel"));
        helper.assertTrue(Library.parseId("ntmspace:ore_gas").equals(Ids.of("ntmspace", "ore_gas")),
                "a namespaced id must keep its namespace, got " + Library.parseId("ntmspace:ore_gas"));
        helper.assertTrue(StructPattern.blockTexture("concrete").equals(Ids.of("hbm", "textures/blocks/concrete.png")),
                "HBM's pattern textures moved: " + StructPattern.blockTexture("concrete"));
        helper.assertTrue(StructPattern.blockTexture("ntmspace:hull")
                        .equals(Ids.of("ntmspace", "textures/blocks/hull.png")),
                "an addon pattern texture must resolve in its namespace: " + StructPattern.blockTexture("ntmspace:hull"));
        helper.succeed();
    }

    private static void bulletConfigIds(GameTestHelper helper) {
        List<BulletConfig> all = BulletConfigs.all();
        helper.assertTrue(!all.isEmpty(), "no bullet configs are registered");
        for (BulletConfig config : all) {
            helper.assertTrue(config.id != null && config.id.getNamespace().equals(Constants.MOD_ID),
                    "HBM's string register must put every round in hbm, found " + config.id);
        }
        Identifier taken = all.get(0).id;
        int before = all.size();
        boolean rejected = false;
        try {
            BulletConfigs.register(taken, new BulletConfig());
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "the Identifier register must refuse a taken id (" + taken + ")");
        helper.assertTrue(BulletConfigs.all().size() == before && BulletConfigs.get(taken) == all.get(0),
                "a refused register must leave the registry untouched");
        helper.succeed();
    }

    private static void materialRegister(GameTestHelper helper) {
        int before = Mats.orderedList.size();
        boolean nameRejected = false;
        boolean idRejected = false;
        try {
            Mats.register(Integer.MAX_VALUE, "iron");
        } catch (IllegalArgumentException expected) {
            nameRejected = true;
        }
        try {
            Mats.register(Mats.MAT_IRON.id, "addon_seam_test_unused_name");
        } catch (IllegalArgumentException expected) {
            idRejected = true;
        }
        helper.assertTrue(nameRejected && idRejected, "Mats.register must refuse a taken name and a taken id");
        helper.assertTrue(Mats.orderedList.size() == before && Mats.byName("addon_seam_test_unused_name") == null
                        && Mats.matById.get(Integer.MAX_VALUE) == null,
                "a refused material must not be listed");
        helper.succeed();
    }

    private static void legacyPalette(GameTestHelper helper) {
        // HBM's names, bare and 1.7 "tile." form, are unchanged
        Block steel = BuiltInRegistries.BLOCK.getOptional(Ids.of("hbm", "block_steel")).orElse(Blocks.AIR);
        helper.assertTrue(LegacyStructurePalette.resolve("hbm:tile.block_steel", 0).state().getBlock() == steel,
                "hbm:tile.block_steel no longer resolves to HBM's steel block");
        // an addon namespace resolves through the registry; an unknown one is air (as an unknown 1.7 name always was)
        helper.assertTrue(LegacyStructurePalette.resolve("addonseamtest:tile.nothing", 0).state().is(Blocks.AIR),
                "an unregistered addon block must place air");
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            Identifier id = Ids.keyId(entry.getKey());
            if (id.getNamespace().equals("minecraft") || id.getNamespace().equals(Constants.MOD_ID)) continue;
            Block block = entry.getValue();
            if (block instanceof StairBlock) continue;
            if (block instanceof BlockMultiblockCore) continue;
            Block resolved = LegacyStructurePalette.resolve(id.getNamespace() + ":tile." + id.getPath(), 0).state().getBlock();
            helper.assertTrue(resolved == block, "the palette must resolve another namespace's block " + id
                    + " through the registry, got " + BuiltInRegistries.BLOCK.getKey(resolved));
            break;
        }
        helper.succeed();
    }

    private static void surveyScanner(GameTestHelper helper) {
        // 1.7 ItemSurveyScanner's report order and colours
        List<String> messages = List.of("Found OIL!", "Found BEDROCK OIL!", "Found COLTAN!", "Found DEPTH ROCK!",
                "Found SCHIST!", "Found AUSTRALIUM!");
        List<ChatFormatting> colours = List.of(ChatFormatting.BLACK, ChatFormatting.BLACK, ChatFormatting.GOLD,
                ChatFormatting.GRAY, ChatFormatting.DARK_AQUA, ChatFormatting.YELLOW);
        List<ItemSurveyScanner.Finding> findings = ItemSurveyScanner.findings();
        helper.assertTrue(findings.size() >= messages.size(), "the scanner lost findings: " + findings.size());
        for (int i = 0; i < messages.size(); i++) {
            helper.assertTrue(findings.get(i).message().equals(messages.get(i))
                            && findings.get(i).colour() == colours.get(i),
                    "finding " + i + " differs from 1.7: " + findings.get(i).message());
        }
        Block depthNether = BuiltInRegistries.BLOCK.getOptional(Ids.of("hbm", "stone_depth_nether")).orElse(Blocks.AIR);
        helper.assertTrue(depthNether == Blocks.AIR || findings.get(3).matches().test(depthNether),
                "nether depth rock must report as DEPTH ROCK");
        helper.assertTrue(!findings.get(0).matches().test(Blocks.STONE), "plain stone must not report");
        helper.succeed();
    }
}
