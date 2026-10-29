package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.MachineDrain;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.config.MobConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionSavedData;
import com.hbm.handler.pollution.PollutionSavedData.PollutionData;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.pollution.PollutionWorldHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.tileentity.machine.TileEntityHeaterFirebox;
import com.hbm.tileentity.machine.TileEntityMachineDrain;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import com.hbm.util.TickPhase;
import dev.golem.core.gametest.GameTestHelpers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.ChunkPos;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * The pollution field: its region granularity, the store round trip, the diffusion pass and the blight.
 *
 * <p>Every world-facing assertion is scoped to the test's own position or driven against a synthetic
 * {@link PollutionSavedData}. A pollution region is 64 blocks on a side and gametest plots are much
 * closer together than that, so two plots routinely share one region - asserting an absolute level
 * read out of the live store would be asserting on a neighbouring test's soot.
 */
public final class PollutionGameTests {

    private PollutionGameTests() {
    }

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function REGIONS =
            TESTS.function("pollution_regions", 20, () -> PollutionGameTests::regions);
    public static final GameTests.Function STORE =
            TESTS.function("pollution_store", 20, () -> PollutionGameTests::store);
    public static final GameTests.Function DIFFUSION =
            TESTS.function("pollution_diffusion", 20, () -> PollutionGameTests::diffusion);
    public static final GameTests.Function BLIGHT =
            TESTS.function("pollution_blight", 20, () -> PollutionGameTests::blight);
    public static final GameTests.Function FLARE_BURN =
            TESTS.function("pollution_flare_burn", 40, () -> PollutionGameTests::flareBurn);
    public static final GameTests.Function DRAIN_SPILL =
            TESTS.function("pollution_drain_spill", 20, () -> PollutionGameTests::drainSpill);
    public static final GameTests.Function SMOKE_OVERFLOW =
            TESTS.function("pollution_smoke_overflow", 20, () -> PollutionGameTests::smokeOverflow);
    public static final GameTests.Function TRAIT_TOOLTIP =
            TESTS.function("pollution_trait_tooltip", 20, () -> PollutionGameTests::traitTooltip);

    public static void init() {
    }

    /**
     * Pollution lives per region (chunk), not per plot, so a test that zeroes it also zeroes a neighbouring plot's
     * region. Each test therefore takes every type's value first and puts them all back in a {@code finally}, so
     * the region ends exactly as it was, even when an assertion fails.
     */
    private static float[] snapshot(Level level, BlockPos pos) {
        PollutionType[] types = PollutionType.values();
        float[] values = new float[types.length];
        for (PollutionType type : types) values[type.ordinal()] = PollutionHandler.getPollution(level, pos, type);
        return values;
    }

    private static void restore(Level level, BlockPos pos, float[] values) {
        for (PollutionType type : PollutionType.values()) {
            PollutionHandler.setPollution(level, pos, type, values[type.ordinal()]);
        }
    }

    /**
     * 1.7 keys on {@code x >> 6}: four chunks a side, not one.
     */
    public static void regions(GameTestHelper helper) {
        helper.assertTrue(PollutionSavedData.region(0, 0).equals(PollutionSavedData.region(63, 63)),
                "a 64-block region must cover its whole square");
        helper.assertTrue(!PollutionSavedData.region(0, 0).equals(PollutionSavedData.region(64, 0)),
                "block 64 belongs to the next region");
        helper.assertTrue(!PollutionSavedData.region(0, 0).equals(PollutionSavedData.region(0, 64)),
                "the region key must separate Z as well as X");
        helper.assertTrue(!PollutionSavedData.region(-1, -1).equals(PollutionSavedData.region(0, 0)),
                "arithmetic shift must not fold negative coordinates into region 0");
        helper.succeed();
    }

    /**
     * {@code setPollution} writes the value as given; {@code incrementPollution} scales by the config multiplier.
     * Asserted as a delta, because a neighbouring plot shares this region.
     */
    public static void store(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        float[] before = snapshot(helper.getLevel(), pos);
        try {
            PollutionHandler.setPollution(helper.getLevel(), pos, PollutionType.SOOT, 100F);
            helper.assertTrue(PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.SOOT) == 100F,
                    "setPollution must write the value it was given, unscaled");

            PollutionHandler.incrementPollution(helper.getLevel(), pos, PollutionType.SOOT, 10F);
            float expected = (float) (100F + 10F * MobConfig.pollutionMult);
            helper.assertTrue(PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.SOOT) == expected,
                    "incrementPollution must scale by MobConfig.pollutionMult");

            PollutionHandler.setPollution(helper.getLevel(), pos, PollutionType.SOOT, 0F);
            PollutionHandler.decrementPollution(helper.getLevel(), pos, PollutionType.SOOT, 1F);
            helper.assertTrue(PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.SOOT) == 0F,
                    "pollution must clamp at zero rather than going negative");

            PollutionHandler.setPollution(helper.getLevel(), pos, PollutionType.SOOT, 999_999F);
            PollutionHandler.incrementPollution(helper.getLevel(), pos, PollutionType.SOOT, 1F);
            helper.assertTrue(PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.SOOT)
                            == PollutionHandler.MAX_POLLUTION,
                    "pollution must clamp at MAX_POLLUTION");
        } finally {
            restore(helper.getLevel(), pos, before);
        }
        helper.succeed();
    }

    /**
     * One pass decays the source and hands a share to each of the four orthogonal neighbours.
     */
    public static void diffusion(GameTestHelper helper) {
        PollutionSavedData saved = new PollutionSavedData();
        ChunkPos origin = new ChunkPos(0, 0);
        PollutionData seed = new PollutionData();
        seed.pollution[PollutionType.SOOT.ordinal()] = 1000F;
        seed.pollution[PollutionType.HEAVYMETAL.ordinal()] = 500F;
        saved.pollution.put(origin, seed);

        PollutionHandler.diffuse(saved);

        PollutionData self = saved.pollution.get(origin);
        helper.assertTrue(self != null, "the source region must survive a diffusion pass");
        // 1000 -> *0.8 (spread) -> *0.99 (decay)
        helper.assertTrue(Math.abs(self.get(PollutionType.SOOT) - 792F) < 0.01F,
                "soot above 10 decays by the spread factor and then the decay factor, got "
                        + self.get(PollutionType.SOOT));
        helper.assertTrue(Math.abs(self.get(PollutionType.HEAVYMETAL) - 499.75F) < 0.01F,
                "heavy metal decays by 0.9995 and never spreads, got " + self.get(PollutionType.HEAVYMETAL));

        for (ChunkPos neighbour : new ChunkPos[]{new ChunkPos(1, 0), new ChunkPos(-1, 0),
                new ChunkPos(0, 1), new ChunkPos(0, -1)}) {
            PollutionData data = saved.pollution.get(neighbour);
            helper.assertTrue(data != null, "each orthogonal neighbour must receive a share");
            helper.assertTrue(Math.abs(data.get(PollutionType.SOOT) - 50F) < 0.01F,
                    "a neighbour receives 5% of the pre-decay soot, got " + data.get(PollutionType.SOOT));
            helper.assertTrue(data.get(PollutionType.HEAVYMETAL) == 0F, "heavy metal must not spread");
        }
        helper.assertTrue(saved.pollution.get(new ChunkPos(1, 1)) == null,
                "diffusion is orthogonal only - no diagonal neighbour");

        // Soot at or below the spread threshold decays but does not spread.
        PollutionSavedData quiet = new PollutionSavedData();
        PollutionData small = new PollutionData();
        small.pollution[PollutionType.SOOT.ordinal()] = 5F;
        quiet.pollution.put(origin, small);
        PollutionHandler.diffuse(quiet);
        helper.assertTrue(quiet.pollution.get(new ChunkPos(1, 0)) == null,
                "soot at or below 10 must not spread");
        helper.succeed();
    }

    /**
     * Poison past the threshold turns ground cover to coarse dirt and strips the plants.
     */
    public static void blight(GameTestHelper helper) {
        helper.assertTrue(RadiationConfig.enablePollution, "pollution must be enabled for the blight to run");

        BlockPos grass = new BlockPos(1, 1, 1);
        BlockPos fern = new BlockPos(2, 1, 1);
        helper.setBlock(grass, Blocks.GRASS_BLOCK);
        helper.setBlock(fern, Blocks.FERN);

        PollutionWorldHandler.destroyBlock(helper.getLevel(), helper.absolutePos(grass));
        PollutionWorldHandler.destroyBlock(helper.getLevel(), helper.absolutePos(fern));

        helper.assertBlockPresent(Blocks.COARSE_DIRT, grass);
        helper.assertBlockPresent(Blocks.AIR, fern);

        // Stone is not ground cover and must be left alone.
        BlockPos stone = new BlockPos(3, 1, 1);
        helper.setBlock(stone, Blocks.STONE);
        PollutionWorldHandler.destroyBlock(helper.getLevel(), helper.absolutePos(stone));
        helper.assertBlockPresent(Blocks.STONE, stone);
        helper.succeed();
    }

    // FT_Polluting

    /**
     * A flare burning natural gas: every fifth tick 1.7 applies GAS's burn table ({@code P_GAS}: soot
     * {@code SOOT_PER_SECOND * 0.005 = 1/25 * 0.005 = 0.0002} per mB) to {@code eject * 5} mB, and an
     * unupgraded flare ejects 10 mB a tick: {@code 0.0002 * 50 = 0.01} soot, times the config multiplier.
     *
     * <p>The flare's own ticker also runs, so the region is zeroed and read back around one direct call.
     */
    public static void flareBurn(GameTestHelper helper) {
        Fluids.init();
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, ModMachineBlocks.machine_flare.get().defaultBlockState());
        TileEntityMachineGasFlare flare = GameTestHelpers
                .blockEntity(helper, relative, TileEntityMachineGasFlare.class);
        helper.assertTrue(flare != null, "gas flare core BlockEntity was not created");
        BlockPos pos = helper.absolutePos(relative);
        boolean[] done = {false};

        // The burn is gated on a five-tick phase; one of any five consecutive ticks is the one.
        for (int delay = 1; delay <= 6; delay++) {
            helper.runAfterDelay(delay, () -> {
                if (done[0] || !TickPhase.every(helper.getLevel(), pos, 5L)) return;
                done[0] = true;

                flare.tank.setTankType(Fluids.GAS);
                flare.tank.setFill(1_000);
                flare.isOn = true;
                flare.doesBurn = true;
                float[] before = snapshot(helper.getLevel(), pos);
                try {
                    PollutionHandler.setPollution(helper.getLevel(), pos, PollutionType.SOOT, 0F);
                    PollutionHandler.setPollution(helper.getLevel(), pos, PollutionType.POISON, 0F);

                    TileEntityMachineGasFlare.serverTick(helper.getLevel(), pos,
                            flare.getBlockState(), flare);

                    float soot = PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.SOOT);
                    double expected = 0.01D * MobConfig.pollutionMult;
                    helper.assertTrue(flare.tank.getFill() == 990,
                            "the flare must burn 10 mB, left " + flare.tank.getFill());
                    helper.assertTrue(Math.abs(soot - expected) < 1.0E-6D,
                            "burning 10 mB of gas must add 0.01 soot (x" + MobConfig.pollutionMult + "), got " + soot);
                    helper.assertTrue(PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.POISON) == 0F,
                            "burning must apply the burn table, not the spill table");
                } finally {
                    restore(helper.getLevel(), pos, before);
                    flare.isOn = false;
                    flare.tank.setFill(0);
                }
                helper.succeed();
            });
        }
        helper.runAfterDelay(8, () -> {
            if (!done[0]) helper.fail("no tick in the flare's five-tick phase was reached");
        });
    }

    /**
     * A drain spilling crude oil: half of 1,000 mB leaves in one tick, and 1.7 applies OIL's spill table
     * ({@code P_OIL}: poison {@code POISON_PER_SECOND * 0.0025 = 1/50 * 0.0025 = 0.00005} per mB) to it:
     * {@code 0.00005 * 500 = 0.025} poison, times the config multiplier, and no soot.
     */
    public static void drainSpill(GameTestHelper helper) {
        Fluids.init();
        BlockPos core = helper.absolutePos(new BlockPos(3, 1, 3));
        MachineDrain block = ModMachineBlocks.machine_drain.get();
        helper.getLevel().setBlock(core, block.defaultBlockState(), 3);
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);
        helper.assertTrue(helper.getLevel().getBlockEntity(core) instanceof TileEntityMachineDrain,
                "drain core BlockEntity was not created");
        TileEntityMachineDrain drain =
                (TileEntityMachineDrain) helper.getLevel().getBlockEntity(core);

        drain.setTankType(Fluids.OIL);
        drain.tank.setFill(1_000);
        float[] before = snapshot(helper.getLevel(), core);
        try {
            PollutionHandler.setPollution(helper.getLevel(), core, PollutionType.POISON, 0F);
            PollutionHandler.setPollution(helper.getLevel(), core, PollutionType.SOOT, 0F);

            TileEntityMachineDrain.serverTick(helper.getLevel(), core, drain.getBlockState(), drain);

            float poison = PollutionHandler.getPollution(helper.getLevel(), core, PollutionType.POISON);
            double expected = 0.025D * MobConfig.pollutionMult;
            helper.assertTrue(drain.tank.getFill() == 500, "the drain must spill 500 of 1,000 mB");
            helper.assertTrue(Math.abs(poison - expected) < 1.0E-6D,
                    "spilling 500 mB of oil must add 0.025 poison (x" + MobConfig.pollutionMult + "), got " + poison);
            helper.assertTrue(PollutionHandler.getPollution(helper.getLevel(), core, PollutionType.SOOT) == 0F,
                    "a spill must apply the spill table, not the burn table");
        } finally {
            drain.tank.setFill(0);
            restore(helper.getLevel(), core, before);
        }
        helper.succeed();
    }

    /**
     * The {@code TileEntityMachinePolluting} buffer: 100 mB of smoke per pollution unit, and what does not
     * fit vents into the region. One soot into the firebox's 50 mB tank: 50 mB kept, 50 mB = 0.5 soot out.
     * A fluid burn scales its pollution table by the amount, so five mB of leaded fuel adds
     * {@code ceil(0.0005 * 5 * 100) = 1} mB of leaded smoke.
     */
    public static void smokeOverflow(GameTestHelper helper) {
        Fluids.init();
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, ModMachineBlocks.heater_firebox.get().defaultBlockState());
        TileEntityHeaterFirebox furnace = GameTestHelpers
                .blockEntity(helper, relative, TileEntityHeaterFirebox.class);
        helper.assertTrue(furnace != null, "Firebox BlockEntity was not created");
        BlockPos pos = helper.absolutePos(relative);

        helper.assertTrue(furnace.smoke.getMaxFill() == 50 && furnace.smoke.getTankType() == Fluids.SMOKE,
                "the firebox smoke buffer must be 1.7's 50 mB of SMOKE");
        furnace.smoke.setFill(0);
        float[] before = snapshot(helper.getLevel(), pos);
        try {
            PollutionHandler.setPollution(helper.getLevel(), pos, PollutionType.SOOT, 0F);
            furnace.pollute(PollutionType.SOOT, 1F);
            float soot = PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.SOOT);
            helper.assertTrue(furnace.smoke.getFill() == 50, "the smoke tank must fill to its capacity");
            helper.assertTrue(Math.abs(soot - 0.5D * MobConfig.pollutionMult) < 1.0E-6D,
                    "the 50 mB overflow must vent as 0.5 soot, got " + soot);

            furnace.smoke_leaded.setFill(0);
            furnace.pollute(Fluids.PETROIL_LEADED,
                    FluidReleaseType.BURN, 5F);
            // Five mB at HEAVY_METAL_PER_SECOND * 0.025 = 0.0005 per mB: ceil(0.25) = 1 mB of smoke.
            helper.assertTrue(furnace.smoke_leaded.getFill() == 1,
                    "leaded fuel must put ceil(0.0005 * 100) = 1 mB into the leaded smoke tank, got " + furnace.smoke_leaded.getFill());
            furnace.pollute(Fluids.WATER,
                    FluidReleaseType.BURN, 5F);
            helper.assertTrue(furnace.smoke_leaded.getFill() == 1 && furnace.smoke.getFill() == 50,
                    "a fluid without FT_Polluting must not pollute");

        } finally {
            furnace.smoke.setFill(0);
            furnace.smoke_leaded.setFill(0);
            restore(helper.getLevel(), pos, before);
        }
        helper.succeed();
    }

    /**
     * The trait tables 1.7 declares, and the tooltip lines they produce.
     */
    public static void traitTooltip(GameTestHelper helper) {
        Fluids.init();
        FT_Polluting oil =
                Fluids.OIL.getTrait(FT_Polluting.class);
        helper.assertTrue(oil != null, "crude oil must carry FT_Polluting");
        helper.assertTrue(oil.burnMap.get(PollutionType.SOOT) == (1F / 25F) * 0.1F,
                "oil must burn to SOOT_PER_SECOND * 0.1 soot per mB");
        helper.assertTrue(oil.releaseMap.get(PollutionType.POISON) == (1F / 50F) * 0.0025F,
                "oil must spill POISON_PER_SECOND * 0.0025 poison per mB");
        helper.assertTrue(Fluids.FLUE.getTrait(FT_Polluting.class)
                        .releaseMap.get(PollutionType.SOOT) == (1F / 25F) * 0.005F * 5,
                "flue gas must spill SOOT_GAS * 5 soot per mB");
        helper.assertTrue(Fluids.WATER.getTrait(FT_Polluting.class) == null,
                "water must not pollute");

        int polluting = 0;
        for (FluidType type : Fluids.getAll()) {
            if (type.hasTrait(FT_Polluting.class)) polluting++;
        }
        // 20 P_OIL + 25 P_FUEL + 3 P_FUEL_LEADED + 7 P_GAS + 1 P_LIQUID_GAS + 9 one-off tables.
        helper.assertTrue(polluting == 65, "1.7 gives 65 fluids FT_Polluting, found " + polluting);

        List<Component> hidden = new ArrayList<>();
        Fluids.OIL.addInfo(hidden, false);
        helper.assertTrue(hidden.stream().anyMatch(c -> hasKey(c, "hbmfluid.trait.polluting")),
                "the always-visible [Polluting] tag is missing");
        helper.assertTrue(hidden.stream().noneMatch(c -> hasKey(c, "hbmfluid.trait.burned")),
                "the burn table must stay hidden until Shift is held");
        helper.assertTrue(hidden.get(hidden.size() - 1).getString().startsWith("Hold <"),
                "the Shift hint must close the tooltip while trait lines are hidden");

        List<Component> shown = new ArrayList<>();
        Fluids.OIL.addInfo(shown, true);
        helper.assertTrue(shown.stream().anyMatch(c -> hasKey(c, "hbmfluid.trait.spilled")),
                "the spill heading is missing with Shift held");
        helper.assertTrue(shown.stream().anyMatch(c -> hasKey(c, "hbmfluid.trait.burned")),
                "the burn heading is missing with Shift held");
        helper.assertTrue(shown.stream().anyMatch(c -> hasKey(c, "pollution.trait.soot")
                        && c.getString().startsWith(" - " + ((1F / 25F) * 0.1F) + " ")),
                "the soot line must print 1.7's raw float per mB");
        helper.assertTrue(shown.stream().noneMatch(c -> c.getString().startsWith("Hold <")),
                "the Shift hint must not show while Shift is held");
        helper.succeed();
    }

    private static boolean hasKey(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents t
                && t.getKey().equals(key)) return true;
        for (Component sibling : component.getSiblings()) {
            if (hasKey(sibling, key)) return true;
        }
        return false;
    }
}
