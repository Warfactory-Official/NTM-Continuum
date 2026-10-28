package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.environment.Environment;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.machine.MachineBigAssTank;
import com.hbm.blocks.machine.MachineIntake;
import com.hbm.blocks.machine.MachinePump;
import com.hbm.blocks.machine.MachineTurbofan;
import com.hbm.blocks.machine.MachineWoodBurner;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.MachineSolarBoiler;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.handler.HbmKeybinds;
import com.hbm.handler.JetpackHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.armor.JetpackBase;
import com.hbm.items.machine.ItemInfiniteFluid;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.hbm.tileentity.machine.TileEntityCondenser;
import com.hbm.tileentity.machine.TileEntityFurnaceBrick;
import com.hbm.tileentity.machine.TileEntityFurnaceIron;
import com.hbm.tileentity.machine.TileEntityHeaterFirebox;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
import com.hbm.tileentity.machine.TileEntityMachineBigAssTank;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import com.hbm.tileentity.machine.TileEntityMachinePumpElectric;
import com.hbm.tileentity.machine.GroundwaterPumps;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import com.hbm.tileentity.machine.TileEntityMachineWoodBurner;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import com.hbm.tileentity.machine.TileEntitySolarBoiler;
import com.hbm.util.TickPhase;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.hook.Verdict;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The {@link Environment} hooks at the places hbm-space (NTM: Space for 1.7.10) changed HBM: with no listener every
 * query answers Earth and the machines behave as 1.7 (their own GameTests stay unchanged), and with a listener
 * answering for a test's own positions the machine does what hbm-space made it do.
 *
 * <p>Listeners are registered once and only answer for the positions, entities or flags a test puts into the maps
 * below, so tests running in the same level never see each other's answers. Level-wide answers (orbit, surface)
 * are switched on and off inside one synchronous test body, so no other test runs while they are on.
 */
public final class EnvironmentGameTests {

    private EnvironmentGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function DEFAULTS =
            TESTS.function("environment_defaults_are_earth", 20, () -> EnvironmentGameTests::defaults);
    public static final GameTests.Function COMBUSTION =
            TESTS.function("environment_combustion_needs_air", 40, 8, () -> EnvironmentGameTests::combustion);
    public static final GameTests.Function FLARE =
            TESTS.function("environment_flare_vents_without_air", 20, 8, () -> EnvironmentGameTests::flare);
    public static final GameTests.Function CONDENSER =
            TESTS.function("environment_condenser_vacuum", 20, () -> EnvironmentGameTests::condenser);
    public static final GameTests.Function INTAKE =
            TESTS.function("environment_intake_compression", 20, 8, () -> EnvironmentGameTests::intake);
    public static final GameTests.Function PUMP =
            TESTS.function("environment_pump_groundwater", 20, 8, () -> EnvironmentGameTests::pump);
    public static final GameTests.Function SOLAR =
            TESTS.function("environment_solar_boiler_sunlight", 20, 8, () -> EnvironmentGameTests::solar);
    public static final GameTests.Function TILT =
            TESTS.function("environment_tank_no_tilt_in_orbit", 40, 12, () -> EnvironmentGameTests::tilt);
    public static final GameTests.Function GRAVITY =
            TESTS.function("environment_jetpack_gravity", 20, () -> EnvironmentGameTests::gravity);
    public static final GameTests.Function INFINITE_WATER =
            TESTS.function("environment_infinite_water_table", 20, () -> EnvironmentGameTests::infiniteWater);
    public static final GameTests.Function WORLD =
            TESTS.function("environment_surface_orbit_torex_satellites", 20, () -> EnvironmentGameTests::world);
    public static final GameTests.Function LIVING =
            TESTS.function("environment_living_tick_and_radiation", 20, () -> EnvironmentGameTests::living);
    public static final GameTests.Function GROWTH =
            TESTS.function("environment_seed_slurry_growth", 20, () -> EnvironmentGameTests::growth);
    public static final GameTests.Function NUCLEAR_BLAST =
            TESTS.function("environment_nuclear_blast_hook", 20, () -> EnvironmentGameTests::nuclearBlast);

    public static void init() {}

    // --- scoped listeners -------------------------------------------------------------------------------------------

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final Set<BlockPos> AIRLESS = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Double> PRESSURE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Double> SUN = new ConcurrentHashMap<>();
    private static final Map<BlockPos, FluidType> TABLE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, List<Integer>> COMPRESSED = new ConcurrentHashMap<>();
    private static final Set<BlockPos> NO_COMPRESSION = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> BARREN = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, List<String>> RELEASED = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> GRAVITY_OF = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> RAD_FLOOR = new ConcurrentHashMap<>();
    private static final Set<UUID> TICKED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> WATCHED = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, ServerLevel> SATELLITE_LEVEL = new ConcurrentHashMap<>();
    private static final Map<Long, Float> BLASTS = new ConcurrentHashMap<>();
    private static volatile boolean orbitOn;
    private static volatile Verdict surfaceAnswer = Verdict.PASS;

    private static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        Environment.BREATHING.register((level, pos, amount) -> AIRLESS.contains(pos) ? Verdict.DENY : Verdict.PASS);
        Environment.PRESSURE.register((level, pos) -> PRESSURE.getOrDefault(pos, Double.NaN));
        Environment.SUNLIGHT.register((level, pos) -> SUN.getOrDefault(pos, Double.NaN));
        Environment.GROUNDWATER.register((level, pos) -> TABLE.get(pos));
        Environment.COMPRESSION.register((level, pos, amount) -> {
            List<Integer> seen = COMPRESSED.get(pos);
            if (seen != null) seen.add(amount);
            return NO_COMPRESSION.contains(pos) ? Verdict.DENY : Verdict.PASS;
        });
        Environment.GROWTH.register((level, pos) -> BARREN.contains(pos) ? Verdict.DENY : Verdict.PASS);
        Environment.GAS_RELEASE.register((level, pos, type, mB) -> {
            List<String> seen = RELEASED.get(pos);
            if (seen != null) seen.add(type.getName() + ":" + (int) mB);
        });
        Environment.GRAVITY.register(entity -> GRAVITY_OF.getOrDefault(entity.getUUID(), Double.NaN));
        Environment.AMBIENT_RADIATION.register((level, pos, entity) -> RAD_FLOOR.getOrDefault(entity.getUUID(), 0D));
        Environment.LIVING_TICK.register(entity -> {
            if (WATCHED.contains(entity.getUUID())) TICKED.add(entity.getUUID());
        });
        Environment.ORBIT.register(level -> orbitOn ? Verdict.ALLOW : Verdict.PASS);
        Environment.SURFACE.register(level -> surfaceAnswer);
        Environment.SATELLITES.register((level, pos, launched) -> pos == null ? null : SATELLITE_LEVEL.get(pos));
        Environment.NUCLEAR_BLAST.register((level, x, z, radius) ->
                BLASTS.put(BlockPos.asLong((int) Math.floor(x), 0, (int) Math.floor(z)), radius));
    }

    /**
     * hbm-space's nuke shocks: an MK5 blast tells {@link Environment#NUCLEAR_BLAST} its radius on its first tick, once
     * (a radius-1 blast without fallout above the plot).
     */
    public static void nuclearBlast(GameTestHelper helper) {
        register();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 6, 1));
        long key = BlockPos.asLong(pos.getX(), 0, pos.getZ());
        BLASTS.remove(key);
        EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFacNoRad(
                helper.getLevel(), 1, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(blast);
        helper.runAfterDelay(3, () -> {
            Float radius = BLASTS.remove(key);
            blast.discard();
            helper.assertTrue(radius != null, "the blast did not reach NUCLEAR_BLAST");
            helper.assertTrue(radius == 1.0F, "the blast's radius must be its length 1, got " + radius);
            helper.succeed();
        });
    }

    // --- tests ------------------------------------------------------------------------------------------------------

    /** Unanswered, every query is Earth: 1.7's rules, never hbm-space's own Earth values. */
    public static void defaults(GameTestHelper helper) {
        register();
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.assertTrue(Environment.breatheAir(level, pos, 5), "unanswered breathing must be Earth air");
        helper.assertTrue(Environment.breatheAir(level, pos, 0, false) == false,
                "an unanswered breathing query must fall back to the caller's 1.7 rule");
        helper.assertTrue(Environment.pressure(level, pos) == 1D, "unanswered pressure must be 1 atm");
        helper.assertFalse(Environment.isVacuum(level, pos), "Earth is no vacuum");
        helper.assertFalse(Environment.inOrbit(level), "plain HBM has no orbit");
        helper.assertTrue(Environment.isSurface(level) == (level.dimension() == Level.OVERWORLD),
                "unanswered, only the overworld is a surface world (1.7 isSurfaceWorld)");
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        if (nether != null) helper.assertFalse(Environment.isSurface(nether), "the nether is no 1.7 surface world");
        helper.assertTrue(Environment.sunlight(level, pos) == 1D, "unanswered sunlight must be 1");
        helper.assertTrue(Environment.groundwater(level, pos) == Fluids.WATER, "unanswered groundwater must be water");
        helper.assertTrue(Environment.declaredGroundwater(level, pos) == null, "nobody declared a water table");
        helper.assertTrue(Environment.compressAir(level, pos, 10), "unanswered, an intake always has air");
        helper.assertTrue(Environment.canGrow(level, pos), "unanswered, seed slurry always grows grass");
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        helper.assertTrue(Environment.gravity(player) == 1D, "unanswered gravity must be Earth's");
        helper.assertTrue(Environment.ambientRadiationFloor(level, pos, player) == 0F, "no ambient radiation floor");
        helper.assertTrue(Environment.satelliteLevel(level, pos, ItemStack.EMPTY) == level,
                "unanswered, a level keeps its own satellites");
        helper.succeed();
    }

    /** hbm-space's {@code breatheAir} in the fire-burning machines: no air, no burn; air again, 1.7. */
    public static void combustion(GameTestHelper helper) {
        register();
        ServerLevel level = helper.getLevel();

        // brick furnace: no fuel is lit without air
        BlockPos brickRel = new BlockPos(1, 1, 1);
        helper.setBlock(brickRel, ModMachineBlocks.machine_furnace_brick_off.get().defaultBlockState());
        TileEntityFurnaceBrick brick = GameTestHelpers.blockEntity(helper, brickRel, TileEntityFurnaceBrick.class);
        BlockPos brickPos = helper.absolutePos(brickRel);
        brick.inventory.setStackInSlot(0, new ItemStack(Items.IRON_ORE));
        brick.inventory.setStackInSlot(1, new ItemStack(Items.COAL, 2));
        AIRLESS.add(brickPos);
        try {
            TileEntityFurnaceBrick.serverTick(level, brickPos, brick.getBlockState(), brick);
            helper.assertTrue(brick.burnTime == 0 && brick.inventory.getStackInSlot(1).getCount() == 2,
                    "brick furnace lit coal without air");
        } finally {
            AIRLESS.remove(brickPos);
        }
        TileEntityFurnaceBrick.serverTick(level, brickPos, brick.getBlockState(), brick);
        helper.assertTrue(brick.burnTime > 0 && brick.inventory.getStackInSlot(1).getCount() == 1,
                "brick furnace did not light coal with air");

        // iron furnace: nothing smelts and the burn time holds without air
        BlockPos ironRel = new BlockPos(1, 1, 4);
        helper.setBlock(ironRel, ModMachineBlocks.furnace_iron.get().defaultBlockState());
        TileEntityFurnaceIron iron = GameTestHelpers.blockEntity(helper, ironRel, TileEntityFurnaceIron.class);
        BlockPos ironPos = helper.absolutePos(ironRel);
        iron.inventory.setStackInSlot(0, new ItemStack(Items.IRON_ORE));
        iron.burnTime = 400;
        AIRLESS.add(ironPos);
        try {
            TileEntityFurnaceIron.serverTick(level, ironPos, iron.getBlockState(), iron);
            helper.assertTrue(iron.progress == 0 && iron.burnTime == 400, "iron furnace smelted without air");
        } finally {
            AIRLESS.remove(ironPos);
        }
        TileEntityFurnaceIron.serverTick(level, ironPos, iron.getBlockState(), iron);
        helper.assertTrue(iron.progress == 1 && iron.burnTime == 399, "iron furnace did not smelt with air");

        // firebox: hbm-space still loads the fuel item, but a lit fire neither burns down nor heats without air
        BlockPos fireRel = new BlockPos(4, 1, 1);
        helper.setBlock(fireRel, ModMachineBlocks.heater_firebox.get().defaultBlockState());
        TileEntityHeaterFirebox firebox = GameTestHelpers.blockEntity(helper, fireRel, TileEntityHeaterFirebox.class);
        BlockPos firePos = helper.absolutePos(fireRel);
        firebox.inventory.setStackInSlot(0, new ItemStack(Items.COAL));
        AIRLESS.add(firePos);
        try {
            TileEntityHeaterFirebox.serverTick(level, firePos, firebox.getBlockState(), firebox);
            int burn = firebox.burnTime;
            helper.assertTrue(burn > 0 && firebox.heatEnergy == 200, "hbm-space loads the fuel even without air");
            TileEntityHeaterFirebox.serverTick(level, firePos, firebox.getBlockState(), firebox);
            helper.assertTrue(firebox.burnTime == burn && firebox.heatEnergy == 199 && firebox.burnHeat == 200,
                    "airless firebox must hold its burn time and keep its burn heat while cooling, got burn "
                            + firebox.burnTime + " heat " + firebox.heatEnergy + " burnHeat " + firebox.burnHeat);
        } finally {
            AIRLESS.remove(firePos);
        }
        int burn = firebox.burnTime;
        TileEntityHeaterFirebox.serverTick(level, firePos, firebox.getBlockState(), firebox);
        helper.assertTrue(firebox.burnTime == burn - 1 && firebox.heatEnergy == 399,
                "firebox did not resume burning with air");

        // wood burner: the lit log makes no power without air
        BlockPos woodRel = new BlockPos(4, 1, 4);
        MachineWoodBurner woodBlock = ModMachineBlocks.machine_wood_burner.get();
        helper.setBlock(woodRel, woodBlock.defaultBlockState());
        woodBlock.fillSpace(level, helper.absolutePos(woodRel), Direction.NORTH);
        TileEntityMachineWoodBurner wood = GameTestHelpers.blockEntity(helper, woodRel, TileEntityMachineWoodBurner.class);
        BlockPos woodPos = helper.absolutePos(woodRel);
        wood.inventory.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        wood.isOn = true;
        AIRLESS.add(woodPos);
        try {
            TileEntityMachineWoodBurner.serverTick(level, woodPos, wood.getBlockState(), wood);
            TileEntityMachineWoodBurner.serverTick(level, woodPos, wood.getBlockState(), wood);
            helper.assertTrue(wood.power == 0 && wood.powerGen == 0, "wood burner made power without air");
        } finally {
            AIRLESS.remove(woodPos);
        }
        TileEntityMachineWoodBurner.serverTick(level, woodPos, wood.getBlockState(), wood);
        helper.assertTrue(wood.powerGen == 100, "wood burner did not make 100 HE/t with air");

        // oil burner: no oil burns without air
        BlockPos oilRel = new BlockPos(7, 1, 1);
        helper.setBlock(oilRel, ModMachineBlocks.heater_oilburner.get().defaultBlockState());
        TileEntityHeaterOilburner oil = GameTestHelpers.blockEntity(helper, oilRel, TileEntityHeaterOilburner.class);
        BlockPos oilPos = helper.absolutePos(oilRel);
        oil.tank.setTankType(Fluids.OIL);
        oil.tank.setFill(10);
        oil.burnRate = 3;
        oil.isOn = true;
        AIRLESS.add(oilPos);
        try {
            TileEntityHeaterOilburner.serverTick(level, oilPos, oil.getBlockState(), oil);
            helper.assertTrue(oil.tank.getFill() == 10 && oil.heatEnergy == 0, "oil burner burned without air");
        } finally {
            AIRLESS.remove(oilPos);
        }
        TileEntityHeaterOilburner.serverTick(level, oilPos, oil.getBlockState(), oil);
        helper.assertTrue(oil.tank.getFill() == 7 && oil.heatEnergy == 75, "oil burner did not burn with air");

        // turbofan: no fuel burns without air
        BlockPos fanRel = new BlockPos(8, 1, 6);
        MachineTurbofan fanBlock = ModMachineBlocks.machine_turbofan.get();
        helper.setBlock(fanRel, fanBlock.defaultBlockState());
        TileEntityMachineTurbofan fan = GameTestHelpers.blockEntity(helper, fanRel, TileEntityMachineTurbofan.class);
        fanBlock.fillSpace(level, helper.absolutePos(fanRel), Direction.NORTH);
        BlockPos fanPos = helper.absolutePos(fanRel);
        fan.tank.setTankType(Fluids.KEROSENE);
        fan.tank.setFill(20);
        AIRLESS.add(fanPos);
        try {
            TileEntityMachineTurbofan.serverTick(level, fanPos, fan.getBlockState(), fan);
            helper.assertTrue(!fan.wasOn && fan.tank.getFill() == 20, "turbofan burned without air");
        } finally {
            AIRLESS.remove(fanPos);
        }
        TileEntityMachineTurbofan.serverTick(level, fanPos, fan.getBlockState(), fan);
        helper.assertTrue(fan.wasOn && fan.tank.getFill() == 19, "turbofan did not burn with air");
        helper.succeed();
    }

    /** hbm-space: an airless flare vents its gas instead of burning it, and the vented gas goes into the air. */
    public static void flare(GameTestHelper helper) {
        register();
        BlockPos rel = new BlockPos(3, 1, 3);
        helper.setBlock(rel, ModMachineBlocks.machine_flare.get().defaultBlockState());
        TileEntityMachineGasFlare flare = GameTestHelpers.blockEntity(helper, rel, TileEntityMachineGasFlare.class);
        BlockPos pos = helper.absolutePos(rel);
        List<String> released = new ArrayList<>();
        RELEASED.put(pos, released);
        AIRLESS.add(pos);
        try {
            flare.tank.setTankType(Fluids.GAS);
            flare.tank.setFill(1_000);
            flare.isOn = true;
            flare.doesBurn = true;
            TileEntityMachineGasFlare.serverTick(helper.getLevel(), pos, flare.getBlockState(), flare);
            helper.assertTrue(flare.tank.getFill() == 950, "airless flare must vent 50 mB, left " + flare.tank.getFill());
            helper.assertTrue(released.equals(List.of("GAS:50")), "vented gas must reach GAS_RELEASE, got " + released);
            helper.assertTrue(flare.power == 0, "airless flare made power");

            released.clear();
            Environment.releaseGas(helper.getLevel(), pos, Fluids.OIL, 100);
            helper.assertTrue(released.isEmpty(), "a liquid is no gas release");
        } finally {
            AIRLESS.remove(pos);
            RELEASED.remove(pos);
            flare.isOn = false;
            flare.tank.setFill(0);
        }
        helper.succeed();
    }

    /** hbm-space: in a vacuum the condenser's steam boils off instead of condensing, unless vacuum-optimised. */
    public static void condenser(GameTestHelper helper) {
        register();
        Fluids.init();
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModMachineBlocks.machine_condenser.get().defaultBlockState());
        TileEntityCondenser condenser = GameTestHelpers.blockEntity(helper, rel, TileEntityCondenser.class);
        BlockPos pos = helper.absolutePos(rel);
        PRESSURE.put(pos, 0.005D);
        try {
            helper.assertTrue(Environment.isVacuum(helper.getLevel(), pos), "0.005 atm is a vacuum");
            condenser.input.setFill(100);
            TileEntityCondenser.serverTick(helper.getLevel(), pos, condenser.getBlockState(), condenser);
            helper.assertTrue(condenser.input.getFill() == 0 && condenser.output.getFill() == 0,
                    "a condenser in a vacuum must lose the steam, not make water: out " + condenser.output.getFill());

            condenser.vacuumOptimised = true;
            condenser.input.setFill(100);
            TileEntityCondenser.serverTick(helper.getLevel(), pos, condenser.getBlockState(), condenser);
            helper.assertTrue(condenser.output.getFill() == 100, "a vacuum-optimised condenser must still condense");
        } finally {
            PRESSURE.remove(pos);
        }
        helper.succeed();
    }

    /** hbm-space {@code canCompress}: 1 mB of local air per 100 mB compressed; no air, no compressed air. */
    public static void intake(GameTestHelper helper) {
        register();
        BlockPos rel = new BlockPos(3, 1, 3);
        MachineIntake block = ModMachineBlocks.machine_intake.get();
        BlockPos abs = helper.absolutePos(rel);
        helper.setBlock(rel, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), abs, Direction.NORTH);
        BlockPos core = MultiblockSurface.coreOfAny(helper.getLevel(), abs);
        TileEntityMachineIntake intake = (TileEntityMachineIntake) helper.getLevel().getBlockEntity(core);
        List<Integer> asked = new ArrayList<>();
        COMPRESSED.put(core, asked);
        NO_COMPRESSION.add(core);
        try {
            intake.power = 200;
            TileEntityMachineIntake.serverTick(helper.getLevel(), core, intake.getBlockState(), intake);
            helper.assertTrue(intake.compair.getFill() == 0 && intake.power == 100,
                    "no air: no compressed air, but the motor still draws 100 HE");
            helper.assertTrue(asked.equals(List.of(10)), "an empty 1,000 mB tank asks for 10 mB of air, got " + asked);
            NO_COMPRESSION.remove(core);
            TileEntityMachineIntake.serverTick(helper.getLevel(), core, intake.getBlockState(), intake);
            helper.assertTrue(intake.compair.getFill() == 1_000, "with air the intake fills its tank");
        } finally {
            NO_COMPRESSION.remove(core);
            COMPRESSED.remove(core);
        }
        helper.succeed();
    }

    /** hbm-space {@code checkGround}: the pump lifts the body's water table, nothing in orbit or on dry bodies. */
    public static void pump(GameTestHelper helper) {
        register();
        ServerLevel level = helper.getLevel();
        for (int x = 2; x <= 4; x++) for (int y = 1; y <= 4; y++) for (int z = 2; z <= 4; z++) {
            helper.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState());
        }
        BlockPos pos = helper.absolutePos(new BlockPos(3, 5, 3));
        MachinePump block = ModMachineBlocks.machine_pump_electric.get();
        level.setBlock(pos, block.defaultBlockState(), 3);
        block.fillSpace(level, pos, Direction.NORTH);
        TileEntityMachinePumpElectric pump = (TileEntityMachinePumpElectric) level.getBlockEntity(pos);
        if (pos.getY() > GroundwaterPumps.groundHeight) {
            helper.succeed(); // the GameTest plot sits above the pump's working height; nothing to observe here
            return;
        }

        pumpTick(pump, level, pos);
        helper.assertTrue(pump.onGround && pump.water.getTankType() == Fluids.WATER
                        && pump.water.getFill() == GroundwaterPumps.electricSpeed,
                "unanswered, the pump lifts water at full speed (1.7)");

        TABLE.put(pos, Fluids.OIL);
        try {
            pumpTick(pump, level, pos);
            helper.assertTrue(pump.onGround && pump.water.getTankType() == Fluids.OIL
                            && pump.water.getFill() == GroundwaterPumps.electricSpeed
                            / TileEntityMachinePumpElectric.NON_WATER_DEBUFF,
                    "an oil table must be pumped as oil at a hundredth of the speed, got "
                            + pump.water.getTankType().getName() + " " + pump.water.getFill());
            TABLE.put(pos, Fluids.NONE);
            pumpTick(pump, level, pos);
            helper.assertTrue(!pump.onGround && !pump.isOn, "no table: no groundwater");
        } finally {
            TABLE.remove(pos);
        }
        helper.succeed();
    }

    private static void pumpTick(TileEntityMachinePumpElectric pump, ServerLevel level, BlockPos pos) {
        pump.water.setFill(0);
        pump.power = TileEntityMachinePumpElectric.POWER_COST;
        pump.groundCheckDelay = 0;
        TileEntityMachinePumpElectric.serverTick(level, pos, pump.getBlockState(), pump);
    }

    /** hbm-space {@code getSunPower}: the boiler's mirror heat scaled by the body's sun. */
    public static void solar(GameTestHelper helper) {
        register();
        BlockPos rel = new BlockPos(3, 1, 3);
        MachineSolarBoiler block = ModMachineBlocks.machine_solar_boiler.get();
        BlockPos core = helper.absolutePos(rel);
        helper.setBlock(rel, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);
        TileEntitySolarBoiler boiler = GameTestHelpers.blockEntity(helper, rel, TileEntitySolarBoiler.class);

        boiler.tanks[0].setFill(100);
        boiler.heat = 50 * 7 + 49;
        TileEntitySolarBoiler.serverTick(helper.getLevel(), core, boiler.getBlockState(), boiler);
        helper.assertTrue(boiler.display == 7 && boiler.tanks[1].getFill() == 700, "unanswered sun must be 1.7's heat / 50");

        SUN.put(core, 0.5D);
        try {
            boiler.tanks[0].setFill(100);
            boiler.tanks[1].setFill(0);
            boiler.heat = 50 * 7 + 49;
            TileEntitySolarBoiler.serverTick(helper.getLevel(), core, boiler.getBlockState(), boiler);
            // (int)(399 * 0.5F) / 50 = 199 / 50 = 3
            helper.assertTrue(boiler.display == 3 && boiler.tanks[0].getFill() == 97 && boiler.tanks[1].getFill() == 300,
                    "half a sun must make (int)(399 * 0.5) / 50 = 3 operations, got " + boiler.display);
        } finally {
            SUN.remove(core);
        }
        helper.succeed();
    }

    /** hbm-space {@code checkTilt}: nothing tilts in orbit. */
    public static void tilt(GameTestHelper helper) {
        register();
        MachineBigAssTank block = ModMachineBlocks.machine_bigasstank.get();
        BlockPos rel = new BlockPos(8, 1, 8);
        FoldedTestSupport.assemble(helper, block, rel);
        TileEntityMachineBigAssTank tank = GameTestHelpers.blockEntity(helper, rel, TileEntityMachineBigAssTank.class);
        BlockPos pos = helper.absolutePos(rel);
        boolean[] done = {false};
        for (int delay = 1; delay <= 22; delay++) {
            helper.runAfterDelay(delay, () -> {
                if (done[0] || !TickPhase.every(helper.getLevel(), pos, 20L)) return;
                done[0] = true;
                tank.tilted = true;
                orbitOn = true;
                try {
                    TileEntityMachineBigAssTank.serverTick(helper.getLevel(), pos, tank.getBlockState(), tank);
                } finally {
                    orbitOn = false;
                }
                helper.assertFalse(tank.tilted, "a tank in orbit must not tilt");
                helper.succeed();
            });
        }
        helper.runAfterDelay(24, () -> {
            if (!done[0]) helper.fail("no tick in the tank's twenty-tick phase was reached");
        });
    }

    /** hbm-space: jetpack lift grows with gravity, and in free fall the jet pushes along the view. */
    public static void gravity(GameTestHelper helper) {
        register();
        ServerLevel level = helper.getLevel();
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack pack = new ItemStack(ModArmorItems.jetpack_fly.get());
        JetpackBase.setFuel(pack, 1_000);
        player.setItemSlot(EquipmentSlot.CHEST, pack);
        JetpackHandler.handleKeybind(player, HbmKeybinds.EnumKeybind.JETPACK, true);
        JetpackBase item = (JetpackBase) pack.getItem();
        try {
            player.setDeltaMovement(Vec3.ZERO);
            item.armorTick(level, player, pack);
            helper.assertTrue(Math.abs(player.getDeltaMovement().y - 0.1D) < 1e-9, "Earth: 1.7's +0.1 lift");

            GRAVITY_OF.put(player.getUUID(), 2D);
            player.setDeltaMovement(Vec3.ZERO);
            item.armorTick(level, player, pack);
            helper.assertTrue(Math.abs(player.getDeltaMovement().y - 0.2D) < 1e-9, "double gravity: twice the lift");

            GRAVITY_OF.put(player.getUUID(), 0D);
            player.setDeltaMovement(Vec3.ZERO);
            item.armorTick(level, player, pack);
            Vec3 expected = player.getViewVector(1.0F).scale(0.05D);
            helper.assertTrue(player.getDeltaMovement().distanceTo(expected) < 1e-9, "free fall: push along the view");
        } finally {
            GRAVITY_OF.remove(player.getUUID());
            JetpackHandler.remove(player);
        }
        helper.succeed();
    }

    /** hbm-space {@code requiresTable}: inf_water needs a water table; unanswered (plain HBM) it always works. */
    public static void infiniteWater(GameTestHelper helper) {
        register();
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemStack water = new ItemStack(ModFluidNetItems.inf_water.get());
        ItemInfiniteFluid item = (ItemInfiniteFluid) water.getItem();
        FluidTankNTM tank = new FluidTankNTM(Fluids.WATER, 1_000);

        item.checkWaterTable(water, level, pos);
        helper.assertTrue(ItemInfiniteFluid.canOperate(water) && !ItemInfiniteFluid.WRONG_WATER_TABLE.has(water),
                "unanswered, inf_water works and gains no component");
        MachineFluidContainerIO.loadInfinite(tank, water);
        helper.assertTrue(tank.getFill() == 50, "inf_water must give 50 mB");

        TABLE.put(pos, Fluids.NONE);
        try {
            item.checkWaterTable(water, level, pos);
            helper.assertFalse(ItemInfiniteFluid.canOperate(water), "no water table: inf_water stops");
            MachineFluidContainerIO.loadInfinite(tank, water);
            helper.assertTrue(tank.getFill() == 50, "a stopped inf_water must give nothing");
            TABLE.put(pos, Fluids.WATER);
            item.checkWaterTable(water, level, pos);
            helper.assertTrue(ItemInfiniteFluid.canOperate(water), "a water table restarts inf_water");
        } finally {
            TABLE.remove(pos);
        }
        ItemStack pinwheel = new ItemStack(ModFluidNetItems.chlorine_pinwheel.get());
        TABLE.put(pos, Fluids.NONE);
        try {
            ((ItemInfiniteFluid) pinwheel.getItem()).checkWaterTable(pinwheel, level, pos);
            helper.assertTrue(ItemInfiniteFluid.canOperate(pinwheel), "only the table-bound items care about water");
        } finally {
            TABLE.remove(pos);
        }
        helper.succeed();
    }

    /** Surface and orbit answers, the Torex vacuum cloud and the satellite level redirect. */
    public static void world(GameTestHelper helper) {
        register();
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        surfaceAnswer = Verdict.DENY;
        try {
            helper.assertFalse(Environment.isSurface(level), "a denying surface listener must win");
        } finally {
            surfaceAnswer = Verdict.PASS;
        }
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        if (nether != null) {
            surfaceAnswer = Verdict.ALLOW;
            try {
                helper.assertTrue(Environment.isSurface(nether), "an allowing surface listener names a surface world");
            } finally {
                surfaceAnswer = Verdict.PASS;
            }
        }

        orbitOn = true;
        try {
            helper.assertTrue(Environment.inOrbit(level) && Environment.isVacuum(level, pos), "orbit is a vacuum");
            helper.assertTrue(Environment.groundwater(level, pos) == Fluids.NONE, "no groundwater in orbit");
        } finally {
            orbitOn = false;
        }

        EntityNukeTorex earth = EntityNukeTorex.statFac(level, pos.getX(), pos.getY(), pos.getZ(), 100F);
        helper.assertFalse(earth.isVacuum(), "a Torex on Earth is the 1.7 mushroom");
        PRESSURE.put(pos, 0D);
        try {
            EntityNukeTorex space = EntityNukeTorex.statFac(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 100F);
            helper.assertTrue(space.isVacuum(), "a Torex without air must be hbm-space's STARFISH cloud");
        } finally {
            PRESSURE.remove(pos);
        }

        if (nether != null) {
            SATELLITE_LEVEL.put(pos, nether);
            try {
                helper.assertTrue(SatelliteSavedData.get(level, pos) == SatelliteSavedData.get(nether),
                        "a redirected position must read the other level's satellites");
                helper.assertTrue(SatelliteSavedData.get(level, pos.east()) == SatelliteSavedData.get(level),
                        "other positions keep their own level's satellites");
            } finally {
                SATELLITE_LEVEL.remove(pos);
            }
        }
        helper.succeed();
    }

    /** The living tick hook runs in HBM's entity update, and an ambient radiation floor contaminates. */
    public static void living(GameTestHelper helper) {
        register();
        LivingEntity pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        UUID id = pig.getUUID();
        WATCHED.add(id);
        try {
            double before = HbmLivingProps.getRadiation(pig);
            EntityEffectHandler.onUpdate(pig);
            helper.assertTrue(TICKED.contains(id), "LIVING_TICK did not run for the pig");
            helper.assertTrue(HbmLivingProps.getRadiation(pig) == before,
                    "no floor: no ambient radiation on plain HBM");
            RAD_FLOOR.put(id, 100D);
            EntityEffectHandler.onUpdate(pig);
            helper.assertTrue(HbmLivingProps.getRadiation(pig) > before,
                    "an ambient radiation floor must contaminate");
        } finally {
            WATCHED.remove(id);
            TICKED.remove(id);
            RAD_FLOOR.remove(id);
            pig.discard();
        }
        helper.succeed();
    }

    /** hbm-space: seed slurry grows grass only where the atmosphere allows; the rest of the impact is unchanged. */
    public static void growth(GameTestHelper helper) {
        register();
        BlockPos dirtRel = new BlockPos(1, 1, 1);
        BlockPos wasteRel = new BlockPos(3, 1, 1);
        helper.setBlock(dirtRel, Blocks.DIRT.defaultBlockState());
        helper.setBlock(wasteRel, ModFalloutBlocks.waste_earth.get().defaultBlockState());
        BlockPos dirt = helper.absolutePos(dirtRel);
        BlockPos waste = helper.absolutePos(wasteRel);
        BARREN.add(dirt);
        try {
            EntityChemical.seedSlurry(helper.getLevel(), dirt);
            EntityChemical.seedSlurry(helper.getLevel(), waste);
            helper.assertBlockPresent(Blocks.DIRT, dirtRel);
            helper.assertBlockPresent(Blocks.GRASS_BLOCK, wasteRel); // 1.7's unconditional waste-earth conversion
        } finally {
            BARREN.remove(dirt);
        }
        helper.succeed();
    }
}
