package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.soyuz.ModSoyuzBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.missile.EntityRocketLambda;
import com.hbm.entity.missile.EntityRocketSoyuz;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.SpaceAssemblerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ISatChip;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.SoyuzSkinType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.items.machine.ItemOrbitalAssembly.EnumOrbitalAssembly;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.saveddata.satellites.SatelliteRelay;
import com.hbm.saveddata.satellites.SatelliteScience;
import com.hbm.tileentity.machine.TileEntityLaunchpadLambda;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz.SoyuzStatus;
import com.hbm.tileentity.network.RTTYSystem;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * GameTests for the X5846 space content: the space laboratory's timings against 1.7's constants, the 0G assembler,
 * the relay's cross-dimension broadcast, and what the Soyuz and Lambda launch pads refuse to launch without.
 */
public final class SpaceGameTests {

    private SpaceGameTests() {}

    private static final BlockPos PAD = new BlockPos(3, 1, 3);
    private static final int FREQ = 5151;

    /** Flight data: 15 minutes (18,000 ticks) between two disks. */
    public static void scienceFlightData(GameTestHelper helper) {
        Level level = helper.getLevel();
        long now = level.getGameTime();
        SatelliteScience lab = new SatelliteScience();
        helper.assertTrue(SatelliteScience.COOLDOWN == 18_000, "1.7 COOLDOWN = 15 * 60 * 20");

        lab.lastScience = now - 18_000;
        helper.assertTrue(!lab.hasData(level), "no data while the game time has not passed lastScience + 18,000");
        lab.lastScience = now - 18_001;
        helper.assertTrue(lab.hasData(level) && lab.lastScience == now, "data appears one tick past the cooldown");
        helper.assertTrue(lab.getOutputData(EnumDriveType.DISK_EMPTY) == EnumDriveType.DISK_FLIGHTDATA,
                "an empty disk comes back with flight data");
        helper.assertTrue(lab.getOutputData(EnumDriveType.FLASH_EMPTY) == null, "a flash drive takes nothing");
        lab.consumeData();
        helper.assertTrue(!lab.hasData(level), "the next disk waits for the cooldown");
        helper.succeed();
    }

    /** Orbital data: 7,200,000 relay-ticks, so four relays take 1,800,000 ticks (25 hours). */
    public static void scienceSensors(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SatelliteScience lab = new SatelliteScience();
        helper.assertTrue(SatelliteScience.SENSOR_DURATION == 7_200_000, "1.7 SENSOR_DURATION = 100 hours");

        lab.onUpdateTick(level);
        helper.assertTrue(lab.sensorProgress == 0 && !lab.isDirty, "without sensor relays nothing is measured");

        ItemStack sensor = ModSatelliteItems.satellite.stack(EnumSatType.SCIENCE_SENSOR);
        for (int i = 0; i < 4; i++) lab.onPartDelivered(level, sensor);
        helper.assertTrue(lab.sensorCount == 4, "every delivered sensor relay counts");

        // 1,800,000 ticks bring the progress to the limit; the tick after that produces the data
        lab.sensorProgress = 4 * (1_800_000 - 1);
        lab.onUpdateTick(level);
        helper.assertTrue(lab.sensorProgress == SatelliteScience.SENSOR_DURATION && lab.driveOutput == null,
                "the 1,800,000th tick completes the measurement");
        lab.onUpdateTick(level);
        helper.assertTrue(lab.sensorProgress == 0 && lab.driveOutput == EnumDriveType.DISK_ORBITDATA,
                "the next tick hands orbital data to the next empty disk");
        helper.succeed();
    }

    /** The crystal circuit: 3,600 assembler-ticks, so two assemblers need about 1,800 ticks. */
    public static void scienceAssembler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SatelliteScience lab = new SatelliteScience();
        ItemStack part = ModSatelliteItems.orbital_assembly.stack(EnumOrbitalAssembly.CRYSTAL_CIRCUIT);
        GenericRecipe recipe = SpaceAssemblerRecipes.INSTANCE.getRecipe(part);
        helper.assertTrue(recipe != null && recipe.duration == 3_600, "space.crystalcircuit takes 3 minutes");

        lab.onPartDelivered(level, part);
        helper.assertTrue(lab.assemblerTasks.size() == 1, "the assembly is queued");
        lab.onUpdateTick(level);
        helper.assertTrue(lab.assemblerProgress == 0D, "no assembler, no progress");

        ItemStack assembler = ModSatelliteItems.satellite.stack(EnumSatType.SCIENCE_ASSEMBLER);
        lab.onPartDelivered(level, assembler);
        lab.onPartDelivered(level, assembler);
        // 1,800 doubles of 2/3600 sum to 0.99999999999997 (as in 1.7), so the board comes on tick 1,801
        for (int i = 0; i < 1_800; i++) lab.onUpdateTick(level);
        helper.assertTrue(lab.requestableSlots.length == 0, "1,800 ticks are not enough for two assemblers");
        lab.onUpdateTick(level);
        helper.assertTrue(lab.requestableSlots.length == 1
                        && lab.requestableSlots[0].is(ModIntermediateItems.circuit(EnumCircuitType.CRYSTAL))
                        && lab.assemblerTasks.isEmpty() && lab.assemblerProgress == 0D,
                "the 1,801st tick hands over a crystal circuit board, got " + lab.requestableSlots.length);
        helper.succeed();
    }

    /** {@code relay <dimension> <channel> <signal>}: the signal arrives in the named dimension next tick. */
    public static void relay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the nether exists");
        SatelliteRelay relay = new SatelliteRelay();
        relay.onCommand(level, SatelliteRelay.CMD_RELAY, "-1", "p4relay", "hello", "nether");
        RTTYSystem.updateTestChannels(nether.getServer(), "p4relay");
        RTTYSystem.RTTYChannel channel = RTTYSystem.listen(nether, "p4relay");
        helper.assertTrue(channel != null && "hello nether".equals(channel.signal), "the nether hears the relayed signal");
        RTTYSystem.clearChannel(Level.NETHER, "p4relay");
        helper.succeed();
    }

    public static void lambdaGating(GameTestHelper helper) {
        TileEntityLaunchpadLambda pad = placeLambda(helper);
        ItemStack sat = ModSatelliteItems.satellite.stack(EnumSatType.RAY_SCAN);
        ISatChip.setFreqS(sat, FREQ);

        pad.inventory.setStackInSlot(0, new ItemStack(ModSoyuzBlocks.missile_lambda.get()));
        pad.inventory.setStackInSlot(1, sat);
        helper.assertTrue(!pad.canLaunch(), "a dry rocket must not launch");

        pad.tanks[0].setTankType(Fluids.GASOLINE_LEADED);
        pad.tanks[1].setTankType(Fluids.PEROXIDE);
        pad.tanks[0].setFill(23_999);
        pad.tanks[1].setFill(24_000);
        pad.power = TileEntityLaunchpadLambda.CONSUMPTION;
        helper.assertTrue(!pad.canLaunch(), "23,999 mB of gasoline is short of 24,000");
        pad.tanks[0].setFill(24_000);
        pad.power = TileEntityLaunchpadLambda.CONSUMPTION - 1;
        helper.assertTrue(!pad.canLaunch(), "an unpowered pad must not launch");
        pad.power = TileEntityLaunchpadLambda.CONSUMPTION;
        helper.assertTrue(pad.canLaunch(), "rocket, satellite, 24,000 mB each and 10 kHE launch");

        pad.inventory.setStackInSlot(1, ModSatelliteItems.satellite.stack(EnumSatType.MINER_LUNAR));
        helper.assertTrue(!pad.canLaunch(), "the lunar ship needs an orbiter the Lambda cannot carry");
        pad.inventory.setStackInSlot(1, new ItemStack(ModSatelliteItems.sat_gerald.get()));
        helper.assertTrue(!pad.canLaunch(), "Gerald needs an orbiter the Lambda cannot carry");

        pad.inventory.setStackInSlot(1, sat);
        pad.erected = true;
        pad.liftOff(helper.getLevel());
        helper.assertTrue(pad.tanks[0].getFill() == 0 && pad.tanks[1].getFill() == 0
                        && pad.inventory.getStackInSlot(0).isEmpty() && pad.inventory.getStackInSlot(1).isEmpty() && !pad.erected,
                "liftoff burns 24,000 mB of each and takes rocket and satellite");
        var rockets = helper.getLevel().getEntitiesOfClass(EntityRocketLambda.class, new AABB(helper.absolutePos(PAD)).inflate(4));
        helper.assertTrue(rockets.size() == 1 && ISatChip.getFreqS(rockets.get(0).getSat()) == FREQ,
                "one Lambda leaves with the satellite");
        rockets.forEach(Entity::discard);
        helper.succeed();
    }

    public static void soyuzGating(GameTestHelper helper) {
        TileEntityLaunchpadSoyuz pad = placeSoyuz(helper);
        ServerLevel level = helper.getLevel();

        pad.inventory.setStackInSlot(0, ModSoyuzBlocks.missile_soyuz.stack(SoyuzSkinType.LUNAR));
        pad.loadedType = SoyuzSkinType.LUNAR.ordinal();
        pad.inventory.setStackInSlot(2, ModSatelliteItems.satellite.stack(EnumSatType.MINER_LUNAR));
        pad.tanks[0].setFill(100_000);
        pad.tanks[1].setFill(100_000);
        pad.power = TileEntityLaunchpadSoyuz.CONSUMPTION;
        helper.assertTrue(pad.orbital() == 1 && !pad.canLaunch(), "the lunar ship without the orbital module must not launch");
        pad.inventory.setStackInSlot(3, new ItemStack(ModItems.missile_soyuz_lander.get()));
        helper.assertTrue(pad.orbital() == 2 && pad.canLaunch(), "with the module it can");
        pad.tanks[1].setFill(99_999);
        helper.assertTrue(!pad.canLaunch(), "99,999 mB of oxygen is short of 100,000");
        pad.tanks[1].setFill(100_000);

        pad.cargoMode = true;
        helper.assertTrue(pad.orbital() == 0 && !pad.canLaunch(), "cargo mode needs a designator and cargo");

        pad.cargoMode = false;
        pad.soyuzStatus = SoyuzStatus.READY;
        pad.receiveControl(launch());
        helper.assertTrue(pad.soyuzStatus == SoyuzStatus.LAUNCHING && pad.countdown == TileEntityLaunchpadSoyuz.COUNTDOWN_DURATION,
                "launch from READY starts the 30 s countdown");

        pad.countdown = 0;
        pad.updateStates(level);
        helper.assertTrue(pad.soyuzStatus == SoyuzStatus.ABSENT && pad.inventory.getStackInSlot(0).isEmpty()
                        && pad.inventory.getStackInSlot(3).isEmpty() && pad.tanks[0].getFill() == 0,
                "the countdown's end lifts off, taking rocket, module and 100,000 mB of each");
        var rockets = level.getEntitiesOfClass(EntityRocketSoyuz.class, new AABB(helper.absolutePos(PAD)).inflate(16, 16, 16));
        helper.assertTrue(rockets.size() == 1 && rockets.get(0).getSkin() == SoyuzSkinType.LUNAR.ordinal(), "one Soyuz leaves");
        rockets.forEach(Entity::discard);
        helper.succeed();
    }

    /** Status machine: a loaded rocket is carried in, stood up and strutted, then fuels for 15 s before READY. */
    public static void soyuzLoading(GameTestHelper helper) {
        TileEntityLaunchpadSoyuz pad = placeSoyuz(helper);
        ServerLevel level = helper.getLevel();
        pad.inventory.setStackInSlot(0, ModSoyuzBlocks.missile_soyuz.stack(SoyuzSkinType.NORMAL));
        pad.tanks[0].setFill(100_000);
        pad.tanks[1].setFill(100_000);

        int ticks = 0;
        while (pad.soyuzStatus != SoyuzStatus.READY && ticks < 2_000) {
            pad.power = TileEntityLaunchpadSoyuz.maxPower;
            TileEntityLaunchpadSoyuz.serverTick(level, pad.getBlockPos(), pad.getBlockState(), pad);
            ticks++;
        }
        // carriage 200 + rotor 200 + struts 60..80 + 300 fuelling, with a tick per state change
        helper.assertTrue(pad.soyuzStatus == SoyuzStatus.READY && ticks >= 760 && ticks <= 790,
                "the pad is READY after 760..790 ticks, took " + ticks);
        helper.succeed();
    }

    private static CompoundTag launch() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("launch", true);
        return tag;
    }

    private static TileEntityLaunchpadSoyuz placeSoyuz(GameTestHelper helper) {
        helper.setBlock(PAD, ModMachineBlocks.launchpad_soyuz.get().defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH));
        TileEntityLaunchpadSoyuz pad = GameTestHelpers.blockEntity(helper, PAD, TileEntityLaunchpadSoyuz.class);
        helper.assertTrue(pad != null, "the Soyuz pad has its block entity");
        return pad;
    }

    private static TileEntityLaunchpadLambda placeLambda(GameTestHelper helper) {
        helper.setBlock(PAD, ModMachineBlocks.launchpad_lambda.get().defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH));
        TileEntityLaunchpadLambda pad = GameTestHelpers.blockEntity(helper, PAD, TileEntityLaunchpadLambda.class);
        helper.assertTrue(pad != null, "the Lambda pad has its block entity");
        return pad;
    }
}
