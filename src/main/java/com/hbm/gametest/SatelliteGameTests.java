package com.hbm.gametest;

import com.hbm.api.data.SatelliteDataMaps;
import com.hbm.blocks.ModSatelliteBlocks;
import com.hbm.blocks.machine.MachineSatDock;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.entity.logic.EntityDeathBlast;
import com.hbm.entity.logic.EntityOrbitalLaser;
import com.hbm.entity.missile.EntitySatellitePod;
import com.hbm.entity.projectile.EntityTom;
import com.hbm.items.ISatChip;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteDeathRay;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteHorizons;
import com.hbm.saveddata.satellites.SatelliteMapper;
import com.hbm.saveddata.satellites.SatelliteMiner;
import com.hbm.saveddata.satellites.SatellitePrecisionLaser;
import com.hbm.saveddata.satellites.SatelliteRadar;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.saveddata.satellites.SatelliteRelay;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.saveddata.satellites.SatelliteScience;
import com.hbm.saveddata.satellites.XSatelliteRegistry;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityMachineSatDock;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import com.hbm.tileentity.machine.TileEntityMachineSatLinker;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * GameTests for the satellite system (1.7 {@code saveddata.satellites}): the id/item registry, the orbit entry
 * point, persistence, every satellite's radio commands and coordinate actions, the ground station, the ID manager
 * and the cargo landing pad.
 */
public final class SatelliteGameTests {

    private SatelliteGameTests() {}

    private static final BlockPos LINK = new BlockPos(3, 1, 3);
    private static final int FREQ = 4242;

    public static void registry(GameTestHelper helper) {
        String[] names = {"SatelliteMapper", "SatelliteScanner", "SatelliteRadar", "SatelliteDeathRay",
                "SatelliteResonator", "SatelliteRelay", "SatelliteMiner", "SatelliteLunarMiner", "SatelliteHorizons",
                "SatellitePrecisionLaser", "SatelliteDetector", "SatelliteRayScan", "SatelliteScience"};
        // 1.7 idToClass order and the names saves carry; the registry keeps both (numeric ids are gone, X-22)
        String[] saved = {"mapper", "scanner", "radar", "laser", "resonator", "relay", "miner", "lunar_miner",
                "horizons", "precision_laser", "detector", "ray_scan", "science"};
        helper.assertTrue(SatelliteType.values().size() >= names.length, "1.7 registers thirteen satellite types");
        for (int id = 0; id < names.length; id++) {
            SatelliteType type = SatelliteType.values().get(id);
            Satellite sat = type.create();
            helper.assertTrue(saved[id].equals(type.name()) && SatelliteType.byName(saved[id]) == type
                            && names[id].equals(sat.getClass().getSimpleName()) && sat.type() == type,
                    "1.7 id " + id + " must be " + names[id] + " saved as " + saved[id] + ", got " + type.name());
        }
        // the hbm:satellite item data map must equal 1.7's registerSatellite table (EnumSatType -> class)
        SatelliteType[] chips = {SatelliteType.MAPPER, SatelliteType.SCANNER, SatelliteType.RADAR, SatelliteType.MINER,
                SatelliteType.LUNAR_MINER, SatelliteType.PRECISION_LASER, SatelliteType.DEATH_RAY,
                SatelliteType.RESONATOR, SatelliteType.RELAY, SatelliteType.DETECTOR, SatelliteType.RAY_SCAN,
                SatelliteType.SCIENCE, null, null};
        for (EnumSatType chip : EnumSatType.VALUES) {
            helper.assertTrue(XSatelliteRegistry.typeFor(ItemSatellite.make(chip)) == chips[chip.ordinal()],
                    chip + " must launch " + chips[chip.ordinal()]);
        }
        helper.assertTrue(SatelliteDataMaps.SATELLITE.byId().size() == 13,
                "the satellite data map has 1.7's thirteen registerSatellite entries");
        helper.assertTrue(XSatelliteRegistry.stackFor(SatelliteType.HORIZONS).is(ModSatelliteItems.sat_gerald.get()),
                "Horizons' chip is sat_gerald");

        helper.assertTrue(XSatelliteRegistry.typeFor(ItemSatellite.make(EnumSatType.SPY)) == SatelliteType.MAPPER, "SPY launches the mapper");
        helper.assertTrue(XSatelliteRegistry.typeFor(ItemSatellite.make(EnumSatType.DEATH_RAY)) == SatelliteType.DEATH_RAY, "DEATH_RAY launches the death ray");
        helper.assertTrue(XSatelliteRegistry.typeFor(ItemSatellite.make(EnumSatType.PRECISION_LASER)) == SatelliteType.PRECISION_LASER, "PRECISION_LASER launches the precision laser");
        helper.assertTrue(XSatelliteRegistry.typeFor(ItemSatellite.make(EnumSatType.MINER_LUNAR)) == SatelliteType.LUNAR_MINER, "MINER_LUNAR launches the lunar miner");
        helper.assertTrue(XSatelliteRegistry.typeFor(new ItemStack(ModSatelliteItems.sat_gerald.get())) == SatelliteType.HORIZONS, "Gerald launches Horizons");
        // 1.7 registerSatellite refuses a class already mapped, so every legacy chip launches nothing
        for (ItemStack legacy : new ItemStack[]{new ItemStack(ModSatelliteItems.sat_mapper.get()),
                new ItemStack(ModSatelliteItems.sat_laser.get()), new ItemStack(ModSatelliteItems.sat_miner.get()),
                new ItemStack(ModSatelliteItems.sat_foeq.get()), new ItemStack(ModSatelliteItems.sat_relay.get()),
                new ItemStack(ModSatelliteItems.sat_chip.get())}) {
            helper.assertTrue(XSatelliteRegistry.typeFor(legacy) == null, legacy + " must launch nothing (1.7)");
        }
        helper.assertTrue(EnumSatType.values().length == 14, "1.7 EnumSatType has fourteen variants");
        helper.succeed();
    }

    public static void orbit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SatelliteSavedData data = SatelliteSavedData.get(level);
        data.descend(FREQ);
        helper.assertTrue(!data.isFreqTaken(FREQ), "The test frequency should start empty");

        Satellite.orbit(level, ItemSatellite.make(EnumSatType.RAY_SCAN), FREQ, 100.7, 700, -250.2);
        Satellite sat = data.getSatFromFreq(FREQ);
        helper.assertTrue(sat instanceof SatelliteRayScan, "Orbit should have put a ray scanner up");
        helper.assertTrue(sat.targetX == 100 && sat.targetZ == -251, "The target is the floored launch site");

        Satellite.orbit(level, ItemSatellite.make(EnumSatType.SCANNER), FREQ, 0, 700, 0);
        helper.assertTrue(data.getSatFromFreq(FREQ) == sat, "A second launch on a taken frequency must not replace it");
        data.descend(FREQ);

        Satellite.orbit(level, new ItemStack(ModSatelliteItems.sat_mapper.get()), FREQ, 0, 700, 0);
        helper.assertTrue(!data.isFreqTaken(FREQ), "A legacy chip reaches orbit with nothing on board");

        Satellite.orbit(level, ItemSatellite.make(EnumSatType.RELAY), FREQ, 55, 700, 66);
        Satellite relay = data.getSatFromFreq(FREQ);
        helper.assertTrue(relay instanceof SatelliteRelay && relay.targetX == 55 && relay.targetZ == 66,
                "The relay takes the launch site as its target like any satellite");
        data.descend(FREQ);
        helper.succeed();
    }

    public static void persistence(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SatelliteSavedData data = new SatelliteSavedData();
        SatelliteDeathRay ray = new SatelliteDeathRay();
        ray.setTarget(12, -34);
        ray.lastShot = 5555L;
        ray.tx = "HELLO";
        SatelliteMiner miner = new SatelliteMiner();
        miner.setTarget(7, 8);
        miner.progress = 0.25D;
        miner.requestableSlots = new ItemStack[] {new ItemStack(Items.IRON_INGOT, 3), ItemStack.EMPTY};
        SatelliteScience lab = new SatelliteScience();
        lab.lastScience = 1234L;
        lab.sensorCount = 2;
        lab.sensorProgress = 77;
        lab.assemblerCount = 1;
        lab.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_ORBITDATA);
        data.sats.put(4, lab);
        SatelliteHorizons horizons = new SatelliteHorizons();
        horizons.used = true;
        horizons.setTarget(1, 2);
        data.sats.put(1, ray);
        data.sats.put(2, miner);
        data.sats.put(3, horizons);

        Tag tag = SatelliteSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().orElseThrow();
        SatelliteSavedData back = SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow();

        Satellite ray2 = back.getSatFromFreq(1);
        helper.assertTrue(ray2 instanceof SatelliteDeathRay d && d.lastShot == 5555L && d.targetX == 12
                && d.targetZ == -34 && "HELLO".equals(d.tx), "The death ray round-trips its shot and base data");
        Satellite miner2 = back.getSatFromFreq(2);
        helper.assertTrue(miner2 instanceof SatelliteMiner m && m.progress == 0.25D && m.targetX == 7 && m.targetZ == 8
                        && m.requestableSlots.length == 2 && m.requestableSlots[0].getCount() == 3 && m.requestableSlots[1].isEmpty(),
                "the miner keeps its progress, target and hold");
        Satellite lab2 = back.getSatFromFreq(4);
        helper.assertTrue(lab2 instanceof SatelliteScience l && l.lastScience == 1234L && l.sensorCount == 2
                        && l.sensorProgress == 77 && l.assemblerCount == 1 && l.driveOutput == EnumDriveType.DISK_ORBITDATA
                        && l.getOutputData(EnumDriveType.DISK_EMPTY) == EnumDriveType.DISK_ORBITDATA,
                "the space laboratory keeps its counters and the data waiting for a drive");
        Satellite hz = back.getSatFromFreq(3);
        helper.assertTrue(hz instanceof SatelliteHorizons h && h.used && h.targetX == 0,
                "1.7 SatelliteHorizons.writeToNBT keeps only 'used'");
        helper.succeed();
    }

    public static void link(GameTestHelper helper) {
        SatelliteSavedData data = SatelliteSavedData.get(helper.getLevel());
        data.descend(FREQ);

        TileEntityMachineSatLink link = place(helper);
        link.freq = FREQ;

        tick(helper, link);
        helper.assertTrue(!link.connected, "An empty frequency is not a connection");
        helper.assertTrue("FALSE".equals(link.provideRORValue("VAL:connected")), "VAL:connected answers FALSE");

        Satellite.orbit(helper.getLevel(), ItemSatellite.make(EnumSatType.RAY_SCAN), FREQ, 40, 700, 60);
        tick(helper, link);
        helper.assertTrue(link.connected, "A taken frequency under open sky is a connection");
        helper.assertTrue("TRUE".equals(link.provideRORValue("VAL:connected")), "VAL:connected answers TRUE");
        helper.assertTrue(link.info.length == 1, "the ground station shows the ray scanner's name");
        helper.assertTrue(("" + FREQ).equals(link.provideRORValue("VAL:freq")), "VAL:freq reports the frequency");
        helper.assertTrue("NB_RAY_SCANNER".equals(link.provideRORValue("VAL:type")), "VAL:type names the satellite");

        link.runRORFunction("FUN:tx", new String[]{"gettarget"});
        helper.assertTrue("40;60".equals(link.provideRORValue("VAL:rx")),
                "A target query comes back on VAL:rx, got " + link.provideRORValue("VAL:rx"));

        link.runRORFunction("FUN:tx", new String[]{"settarget 7 9"});
        link.runRORFunction("FUN:tx", new String[]{"gettargetx"});
        helper.assertTrue("7".equals(link.provideRORValue("VAL:rx")), "settarget moves the target");

        link.runRORFunction("FUN:txrx", new String[]{"p4reply", "gettargetz"});
        helper.assertTrue("9".equals(link.provideRORValue("VAL:rx")), "txrx runs the command");

        link.runRORFunction("FUN:setfreq", new String[]{"17"});
        helper.assertTrue(link.freq == 17, "setfreq retunes the dish");

        data.descend(FREQ);
        helper.succeed();
    }

    public static void emissions(GameTestHelper helper) {
        Level level = helper.getLevel();
        SatelliteRayScan.rayEvents.clear();
        SatelliteDetector.bursts.clear();

        SatelliteRayScan scanner = new SatelliteRayScan();
        scanner.setTarget(0, 0);

        SatelliteRayScan.reportEvent(level, 10, 64, 10, SatelliteRayScan.RayEvent.INFO_NUCLEAR, 200);
        SatelliteRayScan.reportEvent(level, 5_000, 64, 0, SatelliteRayScan.RayEvent.INFO_RADIO, 200);

        scanner.onCommand(level, SatelliteRayScan.CMD_SURVEY);
        scanner.onCommand(level, SatelliteRayScan.CMD_COUNT);
        helper.assertTrue("1".equals(scanner.tx), "Only the emission inside scan range is picked up, got " + scanner.tx);

        scanner.onCommand(level, SatelliteRayScan.CMD_GETINFO, "1");
        helper.assertTrue(SatelliteRayScan.RayEvent.INFO_NUCLEAR.equals(scanner.tx), "The survey reports the kind");

        SatelliteDetector detector = new SatelliteDetector();
        SatelliteDetector.reportEvent(level, SatelliteDetector.DURATION_HIGH, SatelliteDetector.BurstIntensity.HIGH, 0, 0);
        detector.onCommand(level, SatelliteDetector.CMD_SURVEY);
        detector.onCommand(level, SatelliteDetector.CMD_COUNT);
        helper.assertTrue("1".equals(detector.tx), "The detector sees the burst, got " + detector.tx);
        detector.onCommand(level, SatelliteDetector.CMD_GETTYPE, "1");
        helper.assertTrue("HIGH".equals(detector.tx), "The detector reports the burst's class");

        SatelliteRayScan.rayEvents.clear();
        SatelliteDetector.bursts.clear();
        helper.succeed();
    }

    public static void weapons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos target = helper.absolutePos(new BlockPos(2, 1, 2));
        long now = level.getGameTime();

        SatelliteDeathRay ray = new SatelliteDeathRay();
        ray.setTarget(target.getX(), target.getZ());
        ray.lastShot = now;
        ray.onCommand(level, SatelliteDeathRay.CMD_CANFIRE);
        helper.assertTrue("FALSE".equals(ray.tx), "A death ray that just fired is charging (5 min)");
        ray.lastShot = now - SatelliteDeathRay.CHARGE_TIME - 1;
        ray.onCommand(level, SatelliteDeathRay.CMD_CANFIRE);
        helper.assertTrue("TRUE".equals(ray.tx), "A charged death ray reports TRUE");
        ray.onCommand(level, SatelliteDeathRay.CMD_FIRE);
        AABB box = new AABB(target).inflate(2, 400, 2);
        var blasts = level.getEntitiesOfClass(EntityDeathBlast.class, box);
        helper.assertTrue(blasts.size() == 1 && ray.lastShot == now, "fire spawns one death blast and stamps the shot");
        blasts.forEach(Entity::discard);

        SatellitePrecisionLaser laser = new SatellitePrecisionLaser();
        laser.lastShot = now - SatellitePrecisionLaser.CHARGE_TIME - 1;
        laser.onCoordAction(level, null, target.getX(), -1, target.getZ());
        var beams = level.getEntitiesOfClass(EntityOrbitalLaser.class, box);
        helper.assertTrue(beams.size() == 1 && laser.targetX == target.getX(), "the coordinate action fires one orbital laser");
        laser.onCommand(level, SatellitePrecisionLaser.CMD_CANFIRE);
        helper.assertTrue("FALSE".equals(laser.tx), "the precision laser recharges for 5 s");
        beams.forEach(Entity::discard);

        SatelliteHorizons horizons = new SatelliteHorizons();
        horizons.onCommand(level, "settarget", "" + target.getX(), "" + target.getZ());
        helper.assertTrue("TRUE".equals(horizons.tx) && horizons.targetX == target.getX(),
                "1.7 CMD_CANFIRE is \"settarget\": one command both aims and answers");
        horizons.onCoordAction(level, null, target.getX(), -1, target.getZ());
        helper.assertTrue(horizons.used, "Horizons is single use");
        var toms = level.getEntitiesOfClass(EntityTom.class, new AABB(target).inflate(2, 1000, 2));
        helper.assertTrue(toms.size() == 1 && toms.get(0).getY() >= 590, "Tom is dropped from y=600 over the target");
        toms.forEach(Entity::discard);
        horizons.onCommand(level, "settarget", "0", "0");
        helper.assertTrue("FALSE".equals(horizons.tx), "a used Horizons reports FALSE");
        helper.succeed();
    }

    public static void survey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos here = helper.absolutePos(new BlockPos(1, 1, 1));

        SatelliteMapper mapper = new SatelliteMapper();
        mapper.setTarget(here.getX(), here.getZ());
        mapper.onCommand(level, SatelliteMapper.CMD_TARGET_LOADED);
        helper.assertTrue("TRUE".equals(mapper.tx), "the test chunk is loaded");
        mapper.setTarget(here.getX() + 3_000_000, here.getZ());
        mapper.onCommand(level, SatelliteMapper.CMD_TARGET_LOADED);
        helper.assertTrue("FALSE".equals(mapper.tx), "a chunk three million blocks out is not loaded");
        mapper.setTarget(here.getX() + 100_000, here.getZ());
        mapper.onCommand(level, SatelliteMapper.CMD_SPOT_PLAYER);
        helper.assertTrue("NONE".equals(mapper.tx), "nobody stands 100 km out, got " + mapper.tx);

        EntitySatellitePod pig = new EntitySatellitePod(level);
        pig.setPos(here.getX() + 0.5, here.getY() + 5, here.getZ() + 0.5);
        TileEntityMachineRadarNT.matchingEntities.add(pig);
        try {
            SatelliteRadar radar = new SatelliteRadar();
            radar.setTarget(here.getX(), here.getZ());
            radar.onCommand(level, SatelliteRadar.CMD_SURVEY);
            radar.onCommand(level, SatelliteRadar.CMD_FILTER, "satellitepod");
            radar.onCommand(level, SatelliteRadar.CMD_COUNT);
            helper.assertTrue(Integer.parseInt(radar.tx) >= 1, "the radar indexes the pig, got " + radar.tx);
            radar.onCommand(level, SatelliteRadar.CMD_GETNAME, "1");
            helper.assertTrue("entitysatellitepod".equals(radar.tx), "getname is the lower-case class name, got " + radar.tx);
            radar.onCommand(level, SatelliteRadar.CMD_GETTARGETID, "1");
            helper.assertTrue(("" + pig.getId()).equals(radar.tx), "gettargetid answers the entity id, got " + radar.tx);
        } finally {
            TileEntityMachineRadarNT.matchingEntities.remove(pig);
            pig.discard();
        }
        helper.succeed();
    }

    public static void chip(GameTestHelper helper) {
        ItemStack stack = ItemSatellite.make(EnumSatType.RAY_SCAN);
        helper.assertTrue(ISatChip.getFreqS(stack) == 0, "A fresh satellite is on frequency zero");
        ISatChip.setFreqS(stack, 1234);
        helper.assertTrue(ISatChip.getFreqS(stack) == 1234, "A satellite keeps the frequency set on it");
        helper.assertTrue(ISatChip.getFreqS(ItemStack.EMPTY) == 0, "Nothing carries no frequency");

        TileEntityMachineSatLink link = place(helper);
        link.freq = 0;
        boolean handled = ModMachineBlocks.machine_satlink.get()
                .tuneTo(helper.getLevel(), helper.absolutePos(LINK), stack, null);
        helper.assertTrue(handled && link.freq == 1234, "The chip retunes the dish, got " + link.freq);
        helper.succeed();
    }

    public static void linker(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModSatelliteBlocks.machine_satlinker.get());
        TileEntityMachineSatLinker linker = GameTestHelpers.blockEntity(helper, rel,
                TileEntityMachineSatLinker.class);
        helper.assertTrue(linker != null, "the ID manager has its block entity");

        ItemStack source = new ItemStack(ModSatelliteItems.sat_chip.get());
        ISatChip.setFreqS(source, 777);
        ItemStack copy = ItemSatellite.make(EnumSatType.SPY);
        ItemStack random = new ItemStack(ModSatelliteItems.sat_designator.get());
        linker.inventory.setStackInSlot(0, source);
        linker.inventory.setStackInSlot(1, copy);
        linker.inventory.setStackInSlot(2, random);

        BlockPos abs = helper.absolutePos(rel);
        TileEntityMachineSatLinker.serverTick(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), linker);
        helper.assertTrue(ISatChip.getFreqS(linker.inventory.getStackInSlot(1)) == 777, "slot 1 copies slot 0's frequency");
        int r = ISatChip.getFreqS(linker.inventory.getStackInSlot(2));
        helper.assertTrue(r >= 0 && r < TileEntityMachineSatLinker.SAT_ID_RANGE, "slot 2 is randomised below 10,000,000, got " + r);
        helper.succeed();
    }

    public static void dock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(3, 1, 3);
        FoldedTestSupport.assemble(helper, ModSatelliteBlocks.sat_dock.get(), rel);
        TileEntityMachineSatDock dock = GameTestHelpers.blockEntity(helper, rel,
                TileEntityMachineSatDock.class);
        helper.assertTrue(dock != null, "the cargo pad has its block entity");
        BlockPos abs = helper.absolutePos(rel);
        BlockState state = level.getBlockState(abs);
        helper.assertTrue(MachineSatDock.isCore(state), "the rocket lands on the core cell");
        helper.assertTrue(!MachineSatDock.isCore(level.getBlockState(abs.east())),
                "the pad's outer cells are not the core");

        SatelliteSavedData data = SatelliteSavedData.get(level);
        data.descend(FREQ);
        Satellite.orbit(level, ItemSatellite.make(EnumSatType.MINER_ASTRO), FREQ, 0, 700, 0);
        SatelliteMiner miner = (SatelliteMiner) data.getSatFromFreq(FREQ);
        helper.assertTrue(!miner.tryRequestItems(level, abs.getX(), abs.getY(), abs.getZ()),
                "a miner with an empty hold sends nothing");

        // 1.7 SPEED = 1 / 18,000: the hold fills on the 18,000th tick
        miner.progress = 1D - SatelliteMiner.SPEED / 2;
        miner.onUpdateTick(level);
        helper.assertTrue(miner.requestableSlots.length >= 10 && miner.requestableSlots.length <= 15 && miner.progress == 0D,
                "a full hold is 10..15 draws, got " + miner.requestableSlots.length);
        double before = miner.progress;
        miner.onUpdateTick(level);
        helper.assertTrue(miner.progress == before, "mining waits while the hold is full");

        int loaded = miner.requestableSlots.length;
        helper.assertTrue(miner.tryRequestItems(level, abs.getX(), abs.getY(), abs.getZ()) && miner.requestableSlots.length == 0,
                "the pad calls the cargo down");
        var pods = level.getEntitiesOfClass(EntitySatellitePod.class, new AABB(abs).inflate(1, 400, 1));
        helper.assertTrue(pods.size() == 1 && Math.abs(pods.get(0).getY() - 300) < 1 && pods.get(0).slots.length == loaded,
                "a drop pod carrying the hold falls from y=300");

        EntitySatellitePod pod = pods.get(0);
        pod.setPos(abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 0.5);
        pod.unloadItems();
        int items = 0;
        for (int i = 0; i < 15; i++) items += dock.inventory.getStackInSlot(i).isEmpty() ? 0 : 1;
        helper.assertTrue(items > 0 && !pod.isLanding(), "the landed pod unloads its cargo into the pad");
        pod.discard();
        data.descend(FREQ);
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] required = {
                "assets/hbm/blockstates/machine_satlink.json",
                "assets/hbm/models/machines/satlink.obj",
                "assets/hbm/items/satellite_spy.json",
                "assets/hbm/items/sat_gerald.json",
                "assets/hbm/items/sat_coord.json",
                "assets/hbm/items/sat_dock.json",
                "assets/hbm/blockstates/sat_dock.json",
                "assets/hbm/blockstates/machine_satlinker.json",
                "assets/hbm/models/sat_dock.obj",
                "assets/hbm/textures/models/sat_dock.png",
                "assets/hbm/textures/gui/satellites/gui_sat_coord.png",
                "assets/hbm/textures/gui/machine/gui_sat_linker.png",
                "assets/hbm/textures/gui/storage/gui_sat_dock.png",
                "assets/hbm/textures/items/satellite_relay.png",
                "assets/hbm/textures/items/sat_gerald.png.mcmeta",
                "data/hbm/loot_table/blocks/machine_satlinker.json",
                "data/hbm/loot_table/blocks/sat_dock.json",
                "data/hbm/recipe/machine_satlinker.json",
                "data/hbm/recipe/sat_coord.json",
                "data/hbm/recipe/sat_designator.json",
                "data/hbm/recipe/sat_relay.json",
        };
        for (String path : required) {
            helper.assertTrue(resourceExists(path), "Missing satellite asset: " + path);
        }
        helper.succeed();
    }

    // --- helpers ---

    private static TileEntityMachineSatLink place(GameTestHelper helper) {
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_satlink.get(), LINK);
        TileEntityMachineSatLink link = GameTestHelpers.blockEntity(
                helper, LINK, TileEntityMachineSatLink.class);
        helper.assertTrue(link != null, "Sat link BlockEntity was not created");
        return link;
    }

    private static void tick(GameTestHelper helper, TileEntityMachineSatLink link) {
        BlockPos absolute = helper.absolutePos(LINK);
        BlockState state = helper.getLevel().getBlockState(absolute);
        TileEntityMachineSatLink.serverTick(helper.getLevel(), absolute, state, link);
    }

    private static boolean resourceExists(String path) {
        return GameTestHelpers.resourceExists(SatelliteGameTests.class, path);
    }
}
