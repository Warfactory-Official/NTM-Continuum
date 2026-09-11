package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.ISatChip;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.lib.ForgeDirection;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.saveddata.satellites.SatelliteRegistry;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;

/**
 * GameTests for the satellites and the ground station: the id table's save-format contract, the orbit
 * registry, the emission logs machines report into, and the sat link's radio surface over all of it.
 */
public final class SatelliteGameTests {

    private SatelliteGameTests() {}

    private static final BlockPos LINK = new BlockPos(3, 1, 3);
    private static final int FREQ = 4242;

    public static void registry(GameTestHelper helper) {
        helper.assertTrue(SatelliteRegistry.create(1) instanceof com.hbm.saveddata.satellites.SatelliteScanner,
                "Slot 1 must stay the depth scanner: the slot is the persisted id");
        helper.assertTrue(SatelliteRegistry.create(11) instanceof SatelliteRayScan,
                "Slot 11 must stay the ray scanner");
        helper.assertTrue(SatelliteRegistry.create(0) == null, "Slot 0 is not ported yet");
        helper.assertTrue(SatelliteRegistry.create(99) == null, "A slot past the table is not a satellite");

        Satellite scanner = SatelliteRegistry.create(1);
        helper.assertTrue(scanner != null && scanner.getID() == 1,
                "A satellite must report the slot it was made from");

        ItemStack stack = ModSatelliteItems.satellite.get().of(EnumSatType.RAY_SCAN);
        helper.assertTrue(SatelliteRegistry.idFromStack(stack) == 11,
                "The ray-scan item must map to slot 11");
        helper.assertTrue(SatelliteRegistry.idFromStack(ItemStack.EMPTY) == -1, "Nothing is not a satellite");
        helper.succeed();
    }

    public static void orbit(GameTestHelper helper) {
        SatelliteSavedData data = SatelliteSavedData.get(helper.getLevel());
        data.remove(FREQ);

        helper.assertTrue(!data.isFreqTaken(FREQ), "The test frequency should start empty");

        Satellite.orbit(helper.getLevel(), 11, FREQ, 100, 700, -250);
        Satellite sat = data.getSatFromFreq(FREQ);
        helper.assertTrue(sat instanceof SatelliteRayScan, "Orbit should have put a ray scanner up");
        helper.assertTrue(sat.targetX == 100 && sat.targetZ == -250,
                "A satellite takes its target from where it was launched");

        Satellite.orbit(helper.getLevel(), 1, FREQ, 0, 700, 0);
        helper.assertTrue(data.getSatFromFreq(FREQ) == sat,
                "A second launch on a taken frequency must not replace what is there");

        Satellite.orbit(helper.getLevel(), 0, FREQ + 1, 0, 700, 0);
        helper.assertTrue(!data.isFreqTaken(FREQ + 1),
                "An unported slot must not put anything in orbit");

        data.remove(FREQ);
        helper.succeed();
    }

    public static void link(GameTestHelper helper) {
        SatelliteSavedData data = SatelliteSavedData.get(helper.getLevel());
        data.remove(FREQ);

        TileEntityMachineSatLink link = place(helper);
        link.freq = FREQ;

        tick(helper, link);
        helper.assertTrue(!link.connected, "An empty frequency is not a connection");
        helper.assertTrue("FALSE".equals(link.provideRORValue("VAL:connected")),
                "VAL:connected should report the disconnection");

        Satellite.orbit(helper.getLevel(), 11, FREQ, 40, 700, 60);
        tick(helper, link);
        helper.assertTrue(link.connected, "A taken frequency under open sky is a connection");
        helper.assertTrue("TRUE".equals(link.provideRORValue("VAL:connected")),
                "VAL:connected should report the connection");
        helper.assertTrue("NB_RAY_SCANNER".equals(link.provideRORValue("VAL:type")),
                "VAL:type should name what answered");
        helper.assertTrue(("" + FREQ).equals(link.provideRORValue("VAL:freq")),
                "VAL:freq should report the dialled frequency");
        helper.assertTrue(link.info.length > 0, "A connected link should carry the satellite's readout");

        link.runRORFunction("FUN:tx", new String[]{"gettarget"});
        helper.assertTrue("40;60".equals(link.provideRORValue("VAL:rx")),
                "A target query should come back on VAL:rx, got " + link.provideRORValue("VAL:rx"));

        link.runRORFunction("FUN:tx", new String[]{"settarget 7 9"});
        link.runRORFunction("FUN:tx", new String[]{"gettargetx"});
        helper.assertTrue("7".equals(link.provideRORValue("VAL:rx")), "settarget should have moved the target");

        link.runRORFunction("FUN:setfreq", new String[]{"17"});
        helper.assertTrue(link.freq == 17, "setfreq should retune the dish");

        data.remove(FREQ);
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
        helper.assertTrue("1".equals(scanner.tx),
                "Only the emission inside scan range should be picked up, got " + scanner.tx);

        scanner.onCommand(level, SatelliteRayScan.CMD_GETINFO, "1");
        helper.assertTrue(SatelliteRayScan.RayEvent.INFO_NUCLEAR.equals(scanner.tx),
                "The survey should report what kind of emission it found");

        SatelliteDetector detector = new SatelliteDetector();
        SatelliteDetector.reportEvent(level, SatelliteDetector.DURATION_HIGH,
                SatelliteDetector.BurstIntensity.HIGH, 0, 0);
        detector.onCommand(level, SatelliteDetector.CMD_SURVEY);
        detector.onCommand(level, SatelliteDetector.CMD_COUNT);
        helper.assertTrue("1".equals(detector.tx), "The detector should see the burst, got " + detector.tx);
        detector.onCommand(level, SatelliteDetector.CMD_GETTYPE, "1");
        helper.assertTrue("HIGH".equals(detector.tx), "The detector should report the burst's class");

        SatelliteRayScan.rayEvents.clear();
        SatelliteDetector.bursts.clear();
        helper.succeed();
    }

    public static void chip(GameTestHelper helper) {
        ItemStack stack = ModSatelliteItems.satellite.get().of(EnumSatType.RAY_SCAN);
        helper.assertTrue(ISatChip.getFreqS(stack) == 0, "A fresh satellite is on frequency zero");
        ISatChip.setFreqS(stack, 1234);
        helper.assertTrue(ISatChip.getFreqS(stack) == 1234, "A satellite should keep the frequency set on it");
        helper.assertTrue(ISatChip.getFreqS(ItemStack.EMPTY) == 0, "Nothing carries no frequency");

        TileEntityMachineSatLink link = place(helper);
        link.freq = 0;
        boolean handled = ModMachineBlocks.machine_satlink.get()
                .tuneTo(helper.getLevel(), helper.absolutePos(LINK), stack, null);
        helper.assertTrue(handled, "The dish should take a chip held against it");
        helper.assertTrue(link.freq == 1234, "The chip should have retuned the dish, got " + link.freq);
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] required = {
                "assets/hbm/blockstates/machine_satlink.json",
                "assets/hbm/models/block/machine_satlink.json",
                "assets/hbm/models/machines/satlink.obj",
                "assets/hbm/textures/models/machines/satlink.png",
                "assets/hbm/items/machine_satlink.json",
                "assets/hbm/items/satellite.json",
                "assets/hbm/textures/items/satellite_ray_scan.png",
                "assets/hbm/textures/items/satellite_relay.png",
                "assets/hbm/models/item/satellite_detector.json",
                "data/hbm/loot_table/blocks/machine_satlink.json",
        };
        for (String path : required) {
            helper.assertTrue(resourceExists(path), "Missing satellite asset: " + path);
        }
        helper.succeed();
    }

    // --- helpers ---

    private static TileEntityMachineSatLink place(GameTestHelper helper) {
        helper.setBlock(LINK, ModMachineBlocks.machine_satlink.get().defaultBlockState()
                .setValue(BlockDummyable.META, BlockDummyable.offset + ForgeDirection.NORTH.ordinal()));
        TileEntityMachineSatLink link = dev.golem.core.gametest.GameTestHelpers.blockEntity(
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
        try (InputStream stream = SatelliteGameTests.class.getClassLoader().getResourceAsStream(path)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }
}
