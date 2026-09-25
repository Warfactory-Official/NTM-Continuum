package com.hbm.gametest;

import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.PwrParts;
import com.hbm.blocks.machine.MachinePWRController;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.blocks.machine.rbmk.RBMKAbsorber;
import com.hbm.blocks.machine.rbmk.RBMKControl;
import com.hbm.blocks.machine.rbmk.RBMKModerator;
import com.hbm.blocks.machine.rbmk.RBMKReflector;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.handler.neutron.NeutronHandler;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemBreedingRod.RodSize;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.scripting.fuels.FuelRecipes;
import com.hbm.scripting.fuels.PileFuels;
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.machine.TileEntityPWRController;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAbsorber;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKModerator;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKReflector;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds the content {@code src/gametest/kubejs/startup_scripts/ntm_fuels.js} declares. Registered only with KubeJS present. */
public final class ScriptingFuelsGameTests {

    private ScriptingFuelsGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function PILE =
            TESTS.function("scripting_fuels_pile", 20, () -> ScriptingFuelsGameTests::pile);
    public static final GameTests.Function BREEDING =
            TESTS.function("scripting_fuels_breeding", 20, () -> ScriptingFuelsGameTests::breeding);
    public static final GameTests.Function RECIPES =
            TESTS.function("scripting_fuels_recipes", 20, () -> ScriptingFuelsGameTests::recipes);
    public static final GameTests.Function RBMK_COLUMNS =
            TESTS.function("scripting_fuels_rbmk_columns", 20, 8, () -> ScriptingFuelsGameTests::rbmkColumns);
    public static final GameTests.Function PWR_PARTS =
            TESTS.function("scripting_fuels_pwr_parts", 20, () -> ScriptingFuelsGameTests::pwrParts);

    public static void init() {}

    public static void pile(GameTestHelper helper) {
        EnumPileRod fuel = EnumPileRod.byName("gametest2_fuel");
        EnumPileRod spent = EnumPileRod.byName("gametest2_spent");
        EnumPileRod source = EnumPileRod.byName("gametest2_source");
        helper.assertTrue(fuel != null && spent != null && source != null, "scripted pile rods declared");
        helper.assertTrue(fuel.ordinal() > EnumPileRod.THORIUM_FUEL.ordinal() && spent.ordinal() == fuel.ordinal() + 1
                && EnumPileRod.byOrdinal(fuel.ordinal()) == fuel, "scripted pile rods index after built-ins");
        helper.assertTrue(fuel.reactionMult == 2D && fuel.life == 1000D && fuel.heatMult == 0.75D && fuel.described
                && !source.described, "scripted pile rod stats");

        ItemStack rod = ModPileBlocks.pile_rod.get().of(fuel);
        helper.assertTrue(ItemPileRodMK2.typeOf(rod) == fuel, "scripted pile rod variant round-trips");
        helper.assertTrue(rod.getItem().getName(rod).getContents() instanceof TranslatableContents name
                && name.getKey().equals("item.hbm.pile_rod.gametest2_fuel"), "scripted pile rod name key");
        helper.assertTrue(ItemPileRodMK2.getReactivity(rod, 0D) == 0D
                && ItemPileRodMK2.getReactivity(ModPileBlocks.pile_rod.get().of(source), 0D) == 3D,
                "scripted pile source and fuel reactivity");
        helper.assertTrue(ItemPileRodMK2.typeOf(ItemPileRodMK2.react(rod, 1000D)) == spent,
                "scripted pile rod turns into its scripted successor");
        helper.assertTrue(ItemPileRodMK2.typeOf(ItemPileRodMK2.react(ModPileBlocks.pile_rod.get().of(spent), 50D))
                == EnumPileRod.WASTE, "scripted pile rod turns into a built-in");

        List<String> written = new ArrayList<>();
        PileFuels.itemModels((path, json) -> written.add(path + json));
        String models = String.join("\n", written);
        helper.assertTrue(models.contains("hbm:item/pile_rod_gametest2_source") && models.contains("hbm:item/pile_rod_mk2_pu239")
                && models.contains("hbm:item_variant"), "pile_rod selection covers built-in and scripted variants");
        helper.succeed();
    }

    public static void breeding(GameTestHelper helper) {
        BreedingRodType type = BreedingRodType.byName("gametest2");
        helper.assertTrue(type != null, "scripted breeding rod type declared");
        for (RodSize size : RodSize.values()) {
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(ModNuclearMiscItems.rod(size, type)).getPath()
                    .equals(size.base + "_gametest2"), "scripted breeding rod item " + size);
            BreederRecipes.BreederRecipe bred = BreederRecipes.getOutput(new ItemStack(ModNuclearMiscItems.rod(size, type)));
            helper.assertTrue(bred != null && bred.flux() == 150 * size.fluxMultiplier
                    && bred.output().getItem() == ModNuclearMiscItems.rod(size, BreedingRodType.TRITIUM),
                    "scripted breeder recipe " + size);
        }
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(new ItemStack(ModNuclearMiscItems.rod(RodSize.SINGLE, type)),
                HazardRegistry.RADIATION) == 4D, "scripted breeding rod radiation");
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(new ItemStack(ModNuclearMiscItems.rod(RodSize.QUAD, type)),
                HazardRegistry.RADIATION) > 4D, "scripted quad rod radiation scales");
        helper.succeed();
    }

    public static void recipes(GameTestHelper helper) {
        Map<String, JsonObject> all = FuelRecipes.all();
        assertRecipe(helper, all, "scripted/rbmk_fuel_gametest2", "hbm:rbmk_fuel_gametest2", 9);
        assertRecipe(helper, all, "scripted/rbmk_fuel_gametest2_1", "hbm:rbmk_fuel_gametest2", 2);
        assertRecipe(helper, all, "scripted/rod_zirnox_gametest2", "hbm:rod_zirnox_gametest2", 4);
        assertRecipe(helper, all, "scripted/pellet_rtg_gametest2", "hbm:pellet_rtg_gametest2", 4);
        assertRecipe(helper, all, "scripted/rod_gametest2", "hbm:rod_gametest2", 2);
        assertRecipe(helper, all, "scripted/rod_dual_gametest2", "hbm:rod_dual_gametest2", 3);
        assertRecipe(helper, all, "scripted/rod_quad_gametest2", "hbm:rod_quad_gametest2", 5);
        assertShaped(helper, all, "scripted/pwr_fuel_gametest2", "hbm:pwr_fuel_gametest2", 3);
        assertShaped(helper, all, "scripted/watz_pellet_gametest2", "hbm:watz_pellet_gametest2", 3);
        assertShaped(helper, all, "scripted/plate_fuel_gametest2", "hbm:plate_fuel_gametest2", 3);
        helper.assertTrue(all.containsKey("scripted/billet_u235") && all.containsKey("scripted/billet_u235_2")
                && all.get("scripted/billet_u235_2").toString().contains("hbm:rod_quad_gametest2"),
                "scripted breeding rod unload recipes");
        helper.succeed();
    }

    private static void assertRecipe(GameTestHelper helper, Map<String, JsonObject> all, String id, String result, int ingredients) {
        JsonObject recipe = all.get(id);
        helper.assertTrue(recipe != null && recipe.get("type").getAsString().equals("minecraft:crafting_shapeless")
                && recipe.getAsJsonArray("ingredients").size() == ingredients && recipe.get("result").toString().contains(result),
                "scripted shapeless recipe " + id);
    }

    private static void assertShaped(GameTestHelper helper, Map<String, JsonObject> all, String id, String result, int rows) {
        JsonObject recipe = all.get(id);
        helper.assertTrue(recipe != null && recipe.get("type").getAsString().equals("minecraft:crafting_shaped")
                && recipe.getAsJsonArray("pattern").size() == rows && recipe.get("result").toString().contains(result),
                "scripted shaped recipe " + id);
    }

    public static void rbmkColumns(GameTestHelper helper) {
        BlockPos controlPos = new BlockPos(1, 1, 1);
        helper.setBlock(controlPos, block("rbmk_control_gametest2").defaultBlockState());
        BlockEntityRBMKControl control = GameTestHelpers.blockEntity(helper, controlPos, BlockEntityRBMKControl.class);
        helper.assertTrue(control.getType() == ModRBMKBlockEntities.RBMK_CONTROL.get() && control.isModerated()
                && control.getBlockState().getBlock() instanceof RBMKControl rod && rod.speed == 2D,
                "scripted control rod reuses the manual control type");
        control.setTarget(1D);
        control.tickServer();
        helper.assertTrue(control.level == BlockEntityRBMKControl.speed * 2D * RBMKDials.getControlSpeed(helper.getLevel()),
                "scripted control rod moves at its speed");

        assertColumn(helper, new BlockPos(1, 1, 3), "rbmk_moderator_gametest2", BlockEntityRBMKModerator.class,
                ModRBMKBlockEntities.RBMK_MODERATOR.get());
        assertColumn(helper, new BlockPos(3, 1, 3), "rbmk_reflector_gametest2", BlockEntityRBMKReflector.class,
                ModRBMKBlockEntities.RBMK_REFLECTOR.get());
        helper.assertTrue(block("rbmk_moderator_gametest2") instanceof RBMKModerator moderator && moderator.moderation == 0.5D
                && block("rbmk_reflector_gametest2") instanceof RBMKReflector reflector && reflector.reflection == 0.25D,
                "scripted moderator and reflector factors");

        BlockPos sourcePos = new BlockPos(2, 1, 5), absorberPos = sourcePos.east(), referencePos = sourcePos.west();
        helper.setBlock(sourcePos, ModRBMKBlocks.rbmk_fuel_rod.get().defaultBlockState());
        BlockEntityRBMKAbsorber absorber = assertColumn(helper, absorberPos, "rbmk_absorber_gametest2",
                BlockEntityRBMKAbsorber.class, ModRBMKBlockEntities.RBMK_ABSORBER.get());
        helper.assertTrue(absorber.getBlockState().getBlock() instanceof RBMKAbsorber scripted && scripted.heatMult == 3D,
                "scripted absorber factor");
        helper.setBlock(referencePos, ModRBMKBlocks.rbmk_absorber.get().defaultBlockState());
        BlockEntityRBMKAbsorber reference = GameTestHelpers.blockEntity(helper, referencePos, BlockEntityRBMKAbsorber.class);
        BlockEntityRBMKRod source = GameTestHelpers.blockEntity(helper, sourcePos, BlockEntityRBMKRod.class);
        source.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_po210be.get()));
        double before = absorber.heat, referenceBefore = reference.heat;
        source.tickServer();
        NeutronHandler.onServerTick(helper.getLevel().getServer());
        double gained = absorber.heat - before, referenceGained = reference.heat - referenceBefore;
        helper.assertTrue(referenceGained > 0D && gained / referenceGained > 2.9D && gained / referenceGained < 3.1D,
                "scripted absorber heats at 3x the built-in, got " + gained + " vs " + referenceGained);

        for (String id : new String[] {"rbmk_control_gametest2", "rbmk_moderator_gametest2", "rbmk_reflector_gametest2",
                "rbmk_absorber_gametest2"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, id)), "scripted column item " + id);
        }
        helper.succeed();
    }

    private static <T extends BlockEntityRBMKBase> T assertColumn(GameTestHelper helper, BlockPos pos, String id, Class<T> type,
                                                                  net.minecraft.world.level.block.entity.BlockEntityType<?> expected) {
        helper.setBlock(pos, block(id).defaultBlockState());
        T column = GameTestHelpers.blockEntity(helper, pos, type);
        helper.assertTrue(column.getType() == expected && expected.isValid(block(id).defaultBlockState()),
                "scripted column " + id + " accepted by its family's type");
        return column;
    }

    public static void pwrParts(GameTestHelper helper) {
        Block heatsink = block("pwr_heatsink_gametest2");
        Block heatex = block("pwr_heatex_gametest2");
        Block source = block("pwr_source_gametest2");
        Block channel = block("pwr_channel_gametest2");
        PwrParts.Part part = PwrParts.of(heatsink);
        helper.assertTrue(part != null && part.kind() == PwrParts.Kind.HEATSINK && part.weight() == 2
                && MachinePWRController.isValidCore(heatsink) && MachinePWRController.isValidCore(channel)
                && !MachinePWRController.isValidCasing(source), "scripted PWR part kinds");

        Map<BlockPos, Block> parts = new HashMap<>();
        parts.put(new BlockPos(0, 0, 0), heatsink);
        parts.put(new BlockPos(1, 0, 0), heatsink);
        parts.put(new BlockPos(2, 0, 0), ModPwrBlocks.PWR_HEATSINK.get());
        parts.put(new BlockPos(3, 0, 0), heatex);
        parts.put(new BlockPos(4, 0, 0), source);
        parts.put(new BlockPos(5, 0, 0), channel);
        TileEntityPWRController controller = new TileEntityPWRController(helper.absolutePos(new BlockPos(1, 1, 1)),
                ModPwrBlocks.PWR_CONTROLLER.get().defaultBlockState());
        controller.setup(parts, Map.of());
        helper.assertTrue(controller.heatsinkCount == 5 && controller.heatexCount == 3 && controller.sourceCount == 2
                && controller.channelCount == 4, "scripted PWR parts count their weight");
        for (String id : new String[] {"pwr_heatsink_gametest2", "pwr_heatex_gametest2", "pwr_source_gametest2",
                "pwr_channel_gametest2"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, id)), "scripted PWR part item " + id);
        }
        helper.succeed();
    }

    private static Block block(String id) {
        var key = Ids.of(Constants.MOD_ID, id);
        if (!BuiltInRegistries.ITEM.containsKey(key)
                || !(BuiltInRegistries.ITEM.getValue(key) instanceof BlockItem item)) {
            throw new IllegalStateException("Missing scripted block item " + id);
        }
        return item.getBlock();
    }
}
