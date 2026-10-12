package com.hbm.gametest;

import com.hbm.blocks.machine.soyuz.ModSoyuzBlocks;
import com.hbm.entity.missile.EntitySoyuz;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ISatChip;
import com.hbm.items.ItemEnums.SoyuzSkinType;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.lib.ForgeDirection;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;

/**
 * GameTests for the launch path: what the complex refuses to launch without, what leaves the pad when it
 * has everything, and the satellite arriving in orbit at the top of the climb.
 */
public final class SoyuzGameTests {

    private SoyuzGameTests() {}

    private static final BlockPos LAUNCHER = new BlockPos(3, 1, 3);
    private static final int FREQ = 7171;

    public static void readiness(GameTestHelper helper) {
        TileEntitySoyuzLauncher launcher = place(helper);

        helper.assertTrue(!launcher.canLaunch(), "An empty pad must not launch");

        launcher.inventory.setStackInSlot(TileEntitySoyuzLauncher.SLOT_ROCKET,
                ModSoyuzBlocks.missile_soyuz.stack(SoyuzSkinType.NORMAL));
        helper.assertTrue(launcher.hasRocket(), "The pad should see the rocket");
        helper.assertTrue(!launcher.canLaunch(), "A rocket with no payload must not launch");

        launcher.inventory.setStackInSlot(TileEntitySoyuzLauncher.SLOT_SATELLITE,
                ModSatelliteItems.satellite.stack(EnumSatType.RAY_SCAN));
        helper.assertTrue(!launcher.canLaunch(), "A dry rocket must not launch");

        fuel(launcher);
        helper.assertTrue(!launcher.canLaunch(), "An unpowered pad must not launch");

        launcher.power = launcher.getMaxPower();
        helper.assertTrue(launcher.canLaunch(), "A fuelled, loaded, powered pad should be ready");

        launcher.startCountdown();
        helper.assertTrue(launcher.starting, "Starting the countdown should arm the pad");

        launcher.inventory.setStackInSlot(TileEntitySoyuzLauncher.SLOT_ROCKET, ItemStack.EMPTY);
        tick(helper, launcher);
        helper.assertTrue(!launcher.starting && launcher.countdown == TileEntitySoyuzLauncher.maxCount,
                "Losing the rocket mid-count should reset the clock");
        helper.succeed();
    }

    public static void liftoff(GameTestHelper helper) {
        TileEntitySoyuzLauncher launcher = place(helper);
        ItemStack satellite = ModSatelliteItems.satellite.stack(EnumSatType.RAY_SCAN);
        ISatChip.setFreqS(satellite, FREQ);

        launcher.inventory.setStackInSlot(TileEntitySoyuzLauncher.SLOT_ROCKET,
                ModSoyuzBlocks.missile_soyuz.stack(SoyuzSkinType.LUNAR));
        launcher.inventory.setStackInSlot(TileEntitySoyuzLauncher.SLOT_SATELLITE, satellite);
        fuel(launcher);
        launcher.power = launcher.getMaxPower();

        launcher.liftOff(helper.getLevel());

        helper.assertTrue(launcher.inventory.getStackInSlot(TileEntitySoyuzLauncher.SLOT_ROCKET).isEmpty(),
                "The rocket should be gone off the pad");
        helper.assertTrue(launcher.inventory.getStackInSlot(TileEntitySoyuzLauncher.SLOT_SATELLITE).isEmpty(),
                "The payload should have gone with it");
        helper.assertTrue(launcher.tanks[0].getFill() == 0 && launcher.tanks[1].getFill() == 0,
                "The launch should have burnt the propellant");

        var rockets = helper.getLevel().getEntitiesOfClass(EntitySoyuz.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(LAUNCHER)).inflate(16));
        helper.assertTrue(rockets.size() == 1, "Exactly one rocket should have left the pad");
        EntitySoyuz rocket = rockets.get(0);
        helper.assertTrue(rocket.getSkin() == SoyuzSkinType.LUNAR.ordinal(),
                "The rocket should be wearing the paint it was built in");
        helper.assertTrue(ISatChip.getFreqS(rocket.getSat()) == FREQ,
                "The rocket should be carrying the payload's frequency");

        rocket.discard();
        helper.succeed();
    }

    public static void orbit(GameTestHelper helper) {
        SatelliteSavedData data = SatelliteSavedData.get(helper.getLevel());
        data.descend(FREQ);

        ItemStack satellite = ModSatelliteItems.satellite.stack(EnumSatType.RAY_SCAN);
        ISatChip.setFreqS(satellite, FREQ);

        EntitySoyuz rocket = new EntitySoyuz(
                com.hbm.entity.ModNtmEntities.SOYUZ.get(), helper.getLevel());
        rocket.setSat(satellite);
        BlockPos pad = helper.absolutePos(LAUNCHER);
        rocket.snapTo(pad.getX() + 0.5, EntitySoyuz.ORBIT_ALTITUDE + 1, pad.getZ() + 0.5, 0F, 0F);
        helper.getLevel().addFreshEntity(rocket);

        rocket.tick();

        helper.assertTrue(rocket.isRemoved(), "The rocket should be spent once its payload separates");
        Satellite sat = data.getSatFromFreq(FREQ);
        helper.assertTrue(sat instanceof SatelliteRayScan,
                "The payload should be in orbit on the frequency it carried");

        data.descend(FREQ);
        helper.succeed();
    }

    /** 1.7 designator()/orbital()/satellite(): Gerald needs the orbital module, CARGO needs a designator. */
    public static void modes(GameTestHelper helper) {
        TileEntitySoyuzLauncher launcher = place(helper);
        launcher.inventory.setStackInSlot(TileEntitySoyuzLauncher.SLOT_ROCKET,
                ModSoyuzBlocks.missile_soyuz.stack(SoyuzSkinType.NORMAL));
        fuel(launcher);
        launcher.power = launcher.getMaxPower();

        launcher.inventory.setStackInSlot(TileEntitySoyuzLauncher.SLOT_SATELLITE,
                new ItemStack(com.hbm.items.ModSatelliteItems.sat_gerald.get()));
        helper.assertTrue(launcher.orbital() == 1 && !launcher.canLaunch(), "Gerald without the orbital module must not launch");
        launcher.inventory.setStackInSlot(TileEntitySoyuzLauncher.SLOT_ORBITAL,
                new ItemStack(com.hbm.items.ModItems.missile_soyuz_lander.get()));
        helper.assertTrue(launcher.orbital() == 2 && launcher.canLaunch(), "Gerald with the orbital module is ready");

        launcher.handleButtonPacket(1, 0);
        helper.assertTrue(launcher.mode == 1 && launcher.satellite() == 0 && launcher.designator() == 1
                && !launcher.canLaunch(), "CARGO mode needs a designator and no satellite");
        helper.assertTrue(launcher.getFuelRequired() == 5000, "CARGO fuel is 5000 + distance, 0 without a target");

        launcher.handleButtonPacket(0, 0);
        launcher.tanks[1].setFill(0);
        helper.assertTrue(launcher.canLaunch(), "1.7 canLaunch never checks oxygen");

        // the button's path: start, then thirty seconds of server ticks, then liftoff
        launcher.handleButtonPacket(0, 1);
        helper.assertTrue(launcher.starting, "the launch button starts the countdown");
        for (int i = 0; i <= TileEntitySoyuzLauncher.maxCount; i++) tick(helper, launcher);
        var rockets = helper.getLevel().getEntitiesOfClass(EntitySoyuz.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(LAUNCHER)).inflate(16));
        helper.assertTrue(rockets.size() == 1, "the countdown ends in a liftoff, rockets: " + rockets.size()
                + " countdown " + launcher.countdown + " starting " + launcher.starting);
        rockets.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] required = {
                "assets/hbm/blockstates/soyuz_launcher.json",
                "assets/hbm/blockstates/struct_soyuz_core.json",
                "assets/hbm/models/soyuz.obj",
                "assets/hbm/textures/models/soyuz/bottomstage.png",
                "assets/hbm/textures/models/soyuz_luna/bottomstage.png",
                "assets/hbm/textures/models/soyuz_authentic/bottomstage.png",
                "assets/hbm/textures/gui/machine/gui_soyuz.png",
                "assets/hbm/items/missile_soyuz_0.json",
                "assets/hbm/items/struct_soyuz_core.json",
                "assets/hbm/sounds/entity/soyuztakeoff.ogg",
                "assets/hbm/sounds/block/soyuzready.ogg",
                "data/hbm/loot_table/blocks/struct_soyuz_core.json",
                // no data/hbm/recipe/struct_soyuz_core.json: 1.7 has no crafting recipe for it (fix wave 2026-10-12)
                "data/hbm/damage_type/exhaust.json",
        };
        for (String path : required) {
            helper.assertTrue(resourceExists(path), "Missing Soyuz asset: " + path);
        }
        helper.succeed();
    }

    // --- helpers ---

    private static TileEntitySoyuzLauncher place(GameTestHelper helper) {
        FoldedTestSupport.assemble(helper, ModSoyuzBlocks.soyuz_launcher.get(), LAUNCHER, Direction.EAST);
        TileEntitySoyuzLauncher launcher = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, LAUNCHER, TileEntitySoyuzLauncher.class);
        helper.assertTrue(launcher != null, "Soyuz launcher BlockEntity was not created");
        return launcher;
    }

    private static void fuel(TileEntitySoyuzLauncher launcher) {
        launcher.tanks[0].setTankType(Fluids.KEROSENE);
        launcher.tanks[1].setTankType(Fluids.OXYGEN);
        launcher.tanks[0].setFill(TileEntitySoyuzLauncher.FUEL_REQUIRED);
        launcher.tanks[1].setFill(TileEntitySoyuzLauncher.FUEL_REQUIRED);
    }

    private static void tick(GameTestHelper helper, TileEntitySoyuzLauncher launcher) {
        BlockPos absolute = helper.absolutePos(LAUNCHER);
        BlockState state = helper.getLevel().getBlockState(absolute);
        TileEntitySoyuzLauncher.serverTick(helper.getLevel(), absolute, state, launcher);
    }

    private static boolean resourceExists(String path) {
        return dev.golem.core.gametest.GameTestHelpers.resourceExists(SoyuzGameTests.class, path);
    }
}
