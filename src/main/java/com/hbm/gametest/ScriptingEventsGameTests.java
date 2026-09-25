package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.radiation.RadiationBlocks;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.hazard.HazardSystem;
import com.hbm.scripting.NTMServerEvents;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.DamageResistanceHandler;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Holds what {@code src/gametest/kubejs/startup_scripts/ntm_events.js} and
 * {@code src/gametest/kubejs/server_scripts/ntm_events.js} declare. Registered only with KubeJS present.
 */
public final class ScriptingEventsGameTests {

    private ScriptingEventsGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function HAZARD =
            TESTS.function("scripting_events_hazard", 20, () -> ScriptingEventsGameTests::hazard);
    public static final GameTests.Function RADIATION =
            TESTS.function("scripting_events_radiation", 20, () -> ScriptingEventsGameTests::radiation);
    public static final GameTests.Function MELTDOWN =
            TESTS.function("scripting_events_meltdown", 20, () -> ScriptingEventsGameTests::meltdown);
    public static final GameTests.Function DETONATION =
            TESTS.function("scripting_events_detonation", 40, () -> ScriptingEventsGameTests::detonation);
    public static final GameTests.Function BLOCKS =
            TESTS.function("scripting_events_blocks", 20, () -> ScriptingEventsGameTests::blocks);
    public static final GameTests.Function EMITTER =
            TESTS.function("scripting_events_emitter", 400, () -> ScriptingEventsGameTests::emitter);
    public static final GameTests.Function ARMOR =
            TESTS.function("scripting_events_armor", 20, () -> ScriptingEventsGameTests::armor);

    public static void init() {}

    private static NTMServerEvents events(GameTestHelper helper) {
        NTMServerEvents events = NTMServerEvents.current();
        helper.assertTrue(Boolean.TRUE.equals(events.debug("server_scripts")), "server script fixture loaded");
        return events;
    }

    private static String key(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static double number(Object value) {
        return value instanceof Number n ? n.doubleValue() : Double.NaN;
    }

    public static void hazard(GameTestHelper helper) {
        NTMServerEvents events = events(helper);
        LivingEntity pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        HbmLivingProps.setRadiation(pig, 0D);
        events.debug("hazard.entity", pig.getId());
        events.debug("hazard.cancel", true);
        HazardSystem.applyHazards(new ItemStack(Items.DEAD_BUSH), pig);
        helper.assertTrue("hbm:radiation".equals(String.valueOf(events.debug("hazard.type")))
                && number(events.debug("hazard.amount")) == 7D, "hazard event carries type and level");
        helper.assertTrue(HbmLivingProps.getRadiation(pig) == 0D, "cancelled hazard applies nothing");
        events.debug("hazard.cancel", null);
        HazardSystem.applyHazards(new ItemStack(Items.DEAD_BUSH), pig);
        helper.assertTrue(HbmLivingProps.getRadiation(pig) > 0D, "uncancelled hazard applies");
        events.debug("hazard.entity", null);
        helper.succeed();
    }

    public static void radiation(GameTestHelper helper) {
        NTMServerEvents events = events(helper);
        LivingEntity pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        HbmLivingProps.setRadiation(pig, 0D);
        events.debug("radiation.entity", pig.getId());
        ContaminationUtil.contaminate(pig, ContaminationUtil.HazardType.RADIATION,
                ContaminationUtil.ContaminationType.RAD_BYPASS, 10D);
        events.debug("radiation.entity", null);
        helper.assertTrue(number(events.debug("radiation.amount")) == 10D, "radiation event carries the dose");
        helper.assertTrue(HbmLivingProps.getRadiation(pig) == 5D, "listener halved the dose");
        helper.succeed();
    }

    public static void meltdown(GameTestHelper helper) {
        NTMServerEvents events = events(helper);
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModRBMKBlocks.rbmk_fuel_rod.get().defaultBlockState());
        BlockEntityRBMKRod rod = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKRod.class);
        events.debug("meltdown.kind", null);
        events.debug("meltdown.pos", key(helper.absolutePos(pos)));
        rod.meltdown();
        events.debug("meltdown.pos", null);
        helper.assertTrue("rbmk".equals(String.valueOf(events.debug("meltdown.kind"))), "meltdown event posted");
        helper.assertBlockPresent(ModRBMKBlocks.rbmk_fuel_rod.get(), pos);
        helper.succeed();
    }

    public static void detonation(GameTestHelper helper) {
        NTMServerEvents events = events(helper);
        BlockPos floor = new BlockPos(2, 1, 2);
        helper.setBlock(floor, Blocks.STONE.defaultBlockState());
        BlockPos centre = helper.absolutePos(floor.above());
        events.debug("detonation.kind", null);
        events.debug("detonation.pos", key(centre));
        EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFac(helper.getLevel(), 3,
                centre.getX() + 0.5D, centre.getY() + 0.5D, centre.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(blast);
        helper.succeedWhen(() -> {
            helper.assertTrue(blast.isRemoved(), "cancelled blast discarded");
            events.debug("detonation.pos", null);
            helper.assertTrue("mk5".equals(String.valueOf(events.debug("detonation.kind")))
                    && number(events.debug("detonation.radius")) == 3D, "detonation event carries kind and radius");
            helper.assertBlockPresent(Blocks.STONE, floor);
        });
    }

    public static void blocks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.assertTrue(RadiationBlocks.isRadResistant(level, pos, Blocks.TINTED_GLASS.defaultBlockState()),
                "scripted block seals");
        helper.assertTrue(RadiationBlocks.isRadResistant(level, pos, Blocks.WHITE_WOOL.defaultBlockState()),
                "scripted block tag seals");
        helper.assertTrue(!RadiationBlocks.isRadResistant(level, pos, ModHazardBlocks.ducrete_smooth.get().defaultBlockState()),
                "scripted unseal beats IRadResistantBlock");
        helper.assertTrue(RadiationBlocks.isRadResistant(level, pos, ModHazardBlocks.block_lead.get().defaultBlockState())
                && !RadiationBlocks.isRadResistant(level, pos, Blocks.GLASS.defaultBlockState()), "unscripted blocks unchanged");
        helper.assertTrue(RadiationBlocks.emission(Blocks.AMETHYST_BLOCK.defaultBlockState()) == 4D,
                "scripted emitter");
        helper.succeed();
    }

    public static void emitter(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.AMETHYST_BLOCK.defaultBlockState());
        BlockPos absolute = helper.absolutePos(pos);
        helper.succeedWhen(() -> helper.assertTrue(RadiationSystemNT.getRadForCoord(helper.getLevel(), absolute) > 0D,
                "placed scripted emitter irradiates its pocket"));
    }

    public static void armor(GameTestHelper helper) {
        ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
        helper.assertTrue(HazmatRegistry.getResistance(helmet) == 0.5D, "scripted rad resistance");
        List<HazardClass> classes = ArmorRegistry.getProtectionFromItem(helmet);
        helper.assertTrue(classes.contains(HazardClass.PARTICLE_COARSE) && classes.contains(HazardClass.GAS_LUNG),
                "scripted hazard classes");
        helper.assertTrue(DamageResistanceHandler.itemStats(Items.LEATHER_HELMET) != null, "scripted damage profile");
        helper.succeed();
    }
}
