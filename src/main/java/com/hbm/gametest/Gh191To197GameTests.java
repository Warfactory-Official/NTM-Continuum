package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.network.BasePipeAnchor;
import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.config.MobConfig;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.logic.EntityWaypoint;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.machine.ItemFluidIDMulti;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.HbmPackets;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import com.hbm.tileentity.machine.TileEntityMachineElectrolyser;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import com.hbm.tileentity.network.TileEntityFluidPipeAnchor;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GitHub issues 191 (bedrock grade filter mode), 194 (duct arms after an anchor or valve retype), 195 (slider values as
 * control packets), 196 (electrolyser outputs follow the input) and 197 (rampant scouts only where 1.7's land rule holds).
 */
public final class Gh191To197GameTests {

    private Gh191To197GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function BEDROCK_FILTER =
            TESTS.function("gh191_bedrock_grade_filter_mode", 20, () -> Gh191To197GameTests::bedrockFilter);
    public static final GameTests.Function ANCHOR_ARM =
            TESTS.function("gh194_duct_arm_follows_anchor_retype", 20, () -> Gh191To197GameTests::anchorArm);
    public static final GameTests.Function SLIDERS =
            TESTS.function("gh195_sliders_use_control_packets", 20, () -> Gh191To197GameTests::sliders);
    public static final GameTests.Function ELECTROLYSER =
            TESTS.function("gh196_electrolyser_outputs_follow_input", 20, () -> Gh191To197GameTests::electrolyser);
    public static final GameTests.Function SCOUT_GUIDANCE =
            TESTS.function("gh197_guided_scout_leaves_dead_waypoint", 40, () -> Gh191To197GameTests::scoutGuidance);
    public static final GameTests.Function SCOUT_GROUND =
            TESTS.function("gh197_scout_spawn_land_rule", 20, () -> Gh191To197GameTests::scoutGround);

    public static void init() {}

    private static void bedrockFilter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack filter = ModBedrockOreItems.make(Grade.SULFURIC_BYPRODUCT, BedrockOreType.LIGHT_METAL);
        ItemStack sameGrade = ModBedrockOreItems.make(Grade.SULFURIC_BYPRODUCT, BedrockOreType.HEAVY_METAL);
        ItemStack otherGrade = ModBedrockOreItems.make(Grade.SULFURIC_ROASTED, BedrockOreType.LIGHT_METAL);
        ItemStack base = ModBedrockOreItems.make(Grade.BASE, BedrockOreType.CRYSTALLINE);
        ModulePatternMatcher matcher = new ModulePatternMatcher(2);

        matcher.initPatternStandard(level, filter, 0);
        matcher.initPatternSmart(level, filter, 1);
        helper.assertTrue(ModulePatternMatcher.MODE_BEDROCK.equals(matcher.modes[0])
                && ModulePatternMatcher.MODE_BEDROCK.equals(matcher.modes[1]), "a bedrock ore filter starts in bedrock mode");
        helper.assertTrue(matcher.isValidForFilter(filter, 0, sameGrade), "bedrock mode matches another type of the same grade");
        helper.assertFalse(matcher.isValidForFilter(filter, 0, otherGrade), "bedrock mode refuses another grade");

        matcher.modes[0] = ModulePatternMatcher.MODE_EXACT;
        helper.assertFalse(matcher.isValidForFilter(filter, 0, sameGrade), "exact mode refuses another type");
        matcher.nextMode(level, filter, 0);
        helper.assertTrue(ModulePatternMatcher.MODE_BEDROCK.equals(matcher.modes[0]), "exact cycles to bedrock, got " + matcher.modes[0]);
        matcher.nextMode(level, filter, 0);
        helper.assertTrue(ModulePatternMatcher.MODE_WILDCARD.equals(matcher.modes[0]), "bedrock cycles to wildcard, got " + matcher.modes[0]);
        helper.assertTrue(matcher.isValidForFilter(filter, 0, otherGrade) && matcher.isValidForFilter(filter, 0, base),
                "1.7's wildcard matches every bedrock ore");
        helper.assertFalse(matcher.isValidForFilter(filter, 0, new ItemStack(Items.IRON_INGOT)), "the wildcard refuses iron");
        matcher.nextMode(level, filter, 0);
        helper.assertTrue(ModulePatternMatcher.MODE_EXACT.equals(matcher.modes[0]), "wildcard cycles back to exact, got " + matcher.modes[0]);
        helper.assertTrue("Item and bedrock grade match".equals(ModulePatternMatcher.getLabel(ModulePatternMatcher.MODE_BEDROCK)),
                "1.7's bedrock label");
        helper.succeed();
    }

    /** A Diesel duct beside a NONE anchor grows its arm once the anchor is set to Diesel through the identifier. */
    private static void anchorArm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchorRel = new BlockPos(2, 2, 2);
        BlockPos ductRel = anchorRel.east();
        helper.setBlock(anchorRel.west(), Blocks.STONE);
        helper.setBlock(anchorRel, ModFluidNetBlocks.pipe_anchor.get().defaultBlockState().setValue(BasePipeAnchor.FACING, Direction.WEST));
        helper.setBlock(ductRel, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState());
        BlockPos duct = helper.absolutePos(ductRel);
        BlockPos anchorPos = helper.absolutePos(anchorRel);
        BlockFluidDuct.typePlaced(level, duct, Fluids.DIESEL);
        helper.assertFalse(level.getBlockState(duct).getValue(BlockFluidDuct.PROPERTY_BY_DIRECTION.get(Direction.WEST)),
                "a Diesel duct reaches toward a NONE anchor");

        ItemStack identifier = new ItemStack(ModUtilityToolItems.fluid_identifier_multi.get());
        ItemFluidIDMulti.setType(identifier, Fluids.DIESEL, true);
        ServerPlayer player = GameTestHelpers.survivalServerPlayerInLevel(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, identifier);
        player.gameMode.useItemOn(player, level, identifier, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(anchorPos), Direction.UP, anchorPos, false));

        TileEntityFluidPipeAnchor anchor = GameTestHelpers.blockEntity(helper, anchorRel, TileEntityFluidPipeAnchor.class);
        helper.assertTrue(anchor.getFluidType() == Fluids.DIESEL, "the identifier did not retype the anchor");
        helper.assertTrue(level.getBlockState(duct).getValue(BlockFluidDuct.PROPERTY_BY_DIRECTION.get(Direction.WEST)),
                "the duct did not grow its arm toward the retyped anchor");
        helper.succeed();
    }

    /** Each slider reaches its machine through a control packet, only from a player holding that machine's menu. */
    private static void sliders(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), ModMachineBlocks.machine_combustion_engine.get().defaultBlockState());
        helper.setBlock(new BlockPos(6, 1, 2), ModMachineBlocks.heater_oilburner.get().defaultBlockState());
        helper.setBlock(new BlockPos(2, 1, 7), ModMachineBlocks.machine_turbine_gas.get().defaultBlockState());
        TileEntityMachineCombustionEngine engine = GameTestHelpers.blockEntity(helper, new BlockPos(2, 1, 2), TileEntityMachineCombustionEngine.class);
        TileEntityHeaterOilburner burner = GameTestHelpers.blockEntity(helper, new BlockPos(6, 1, 2), TileEntityHeaterOilburner.class);
        TileEntityMachineTurbineGas turbine = GameTestHelpers.blockEntity(helper, new BlockPos(2, 1, 7), TileEntityMachineTurbineGas.class);
        ServerPlayer player = GameTestHelpers.survivalServerPlayerInLevel(helper);
        player.setPos(helper.absolutePos(new BlockPos(4, 1, 4)).getCenter());

        HbmPackets.handleControl(player, engine.getBlockPos(), tag("setting", 30));
        helper.assertTrue(engine.setting == 0, "a control packet without the engine's menu changed the throttle");
        player.openMenu(engine);
        boolean on = engine.isOn;
        HbmPackets.handleControl(player, engine.getBlockPos(), tag("setting", 30));
        helper.assertTrue(engine.setting == 30, "throttle 30 arrived as " + engine.setting);
        HbmPackets.handleControl(player, engine.getBlockPos(), tag("setting", 24));
        helper.assertTrue(engine.setting == 24 && engine.isOn == on, "throttle 24 toggled ignition or was lost");
        helper.assertTrue(player.containerMenu.clickMenuButton(player, 1030) && engine.setting == 24,
                "a 1000+ button still sets the throttle");
        player.closeContainer();

        player.openMenu(burner);
        HbmPackets.handleControl(player, burner.getBlockPos(), tag("burnRate", TileEntityHeaterOilburner.MAX_SLIDER_RATE));
        helper.assertTrue(burner.burnRate == TileEntityHeaterOilburner.MAX_SLIDER_RATE, "burn rate arrived as " + burner.burnRate);
        player.closeContainer();

        turbine.state = 1;
        player.openMenu(turbine);
        HbmPackets.handleControl(player, turbine.getBlockPos(), tag("slidPos", 60));
        helper.assertTrue(turbine.powerSliderPos == 60 && turbine.state == 1, "slider 60 arrived as " + turbine.powerSliderPos);
        player.closeContainer();
        helper.succeed();
    }

    private static CompoundTag tag(String key, int value) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(key, value);
        return tag;
    }

    /** 1.7 retypes the outputs on the tick the input changes, before it checks the input amount or power use. */
    private static void electrolyser(GameTestHelper helper) {
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel, ModMachineBlocks.machine_electrolyser.get().defaultBlockState());
        TileEntityMachineElectrolyser te = GameTestHelpers.blockEntity(helper, rel, TileEntityMachineElectrolyser.class);
        te.tanks[0].setTankType(Fluids.HEAVYWATER);
        te.tanks[1].setTankType(Fluids.HYDROGEN);
        te.tanks[1].setFill(1_000);
        te.tanks[2].setTankType(Fluids.OXYGEN);
        te.tanks[2].setFill(1_000);

        te.power = 0;
        tick(helper, te);
        helper.assertTrue(te.tanks[1].getTankType() == Fluids.HYDROGEN, "an unpowered electrolyser retyped its output");

        te.power = TileEntityMachineElectrolyser.MAX_POWER;
        tick(helper, te);
        helper.assertTrue(te.tanks[1].getTankType() == Fluids.DEUTERIUM && te.tanks[1].getFill() == 0,
                "the first output did not follow heavy water: " + te.tanks[1].getTankType().getName() + " " + te.tanks[1].getFill());
        helper.assertTrue(te.tanks[2].getTankType() == Fluids.OXYGEN && te.tanks[2].getFill() == 1_000,
                "the unchanged second output lost its oxygen");
        helper.assertTrue(te.progressFluid == 0, "the electrolyser ran without input");
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, TileEntityMachineElectrolyser te) {
        TileEntityMachineElectrolyser.serverTick(helper.getLevel(), te.getBlockPos(), te.getBlockState(), te);
    }

    /** A guided scout whose BUILD_HIVE waypoint died goes back to building; one following a retreat waypoint stays (1.7). */
    private static void scoutGuidance(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        boolean guidance = MobConfig.rampantGlyphidGuidance;
        BlockPos before = PollutionHandler.targetCoords;
        MobConfig.rampantGlyphidGuidance = true;
        PollutionHandler.targetCoords = helper.absolutePos(new BlockPos(40, 2, 4));
        EntityGlyphidScout guided = scout(helper, new BlockPos(2, 2, 2), EntityGlyphid.TASK_BUILD_HIVE);
        EntityGlyphidScout retreating = scout(helper, new BlockPos(5, 2, 5), EntityGlyphid.TASK_RETREAT_FOR_REINFORCEMENTS);
        helper.runAfterDelay(3, () -> {
            try {
                helper.assertFalse(guided.getCurrentTask() == EntityGlyphid.TASK_FOLLOW && guided.getWaypoint() != null
                        && guided.getWaypoint().isRemoved(), "the guided scout still follows its removed waypoint");
                helper.assertTrue(retreating.getCurrentTask() == EntityGlyphid.TASK_FOLLOW,
                        "a scout following a removed retreat waypoint left FOLLOW, which 1.7 never does");
            } finally {
                MobConfig.rampantGlyphidGuidance = guidance;
                PollutionHandler.targetCoords = before;
            }
            guided.discard();
            retreating.discard();
            helper.succeed();
        });
    }

    private static EntityGlyphidScout scout(GameTestHelper helper, BlockPos rel, int waypointType) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(rel);
        EntityGlyphidScout scout = new EntityGlyphidScout(ModNtmEntities.GLYPHID_SCOUT.get(), level);
        scout.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
        level.addFreshEntity(scout);
        EntityWaypoint waypoint = new EntityWaypoint(EntityWaypoint.TYPE.get(), level);
        waypoint.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
        waypoint.setWaypointType(waypointType);
        level.addFreshEntity(waypoint);
        scout.setCurrentTask(EntityGlyphid.TASK_FOLLOW, waypoint);
        waypoint.discard();
        return scout;
    }

    private static void scoutGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.BEDROCK);
        helper.setBlock(new BlockPos(5, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 2, 1), Blocks.WATER);
        BlockPos onGrass = helper.absolutePos(new BlockPos(1, 2, 1));

        helper.assertTrue(PollutionHandler.landSpawnPosition(level, MobCategory.MONSTER, onGrass), "a monster try on grass is a 1.7 spawn position");
        helper.assertFalse(PollutionHandler.landSpawnPosition(level, MobCategory.MONSTER, onGrass.above()), "a try floating above the grass");
        helper.assertFalse(PollutionHandler.landSpawnPosition(level, MobCategory.WATER_CREATURE, onGrass), "a water creature try");
        helper.assertFalse(PollutionHandler.landSpawnPosition(level, MobCategory.MONSTER, helper.absolutePos(new BlockPos(3, 2, 1))),
                "a try on bedrock");
        helper.assertFalse(PollutionHandler.landSpawnPosition(level, MobCategory.MONSTER, helper.absolutePos(new BlockPos(5, 2, 1))),
                "a try in water");
        helper.succeed();
    }
}
