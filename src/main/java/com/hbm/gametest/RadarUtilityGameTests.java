package com.hbm.gametest;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.api.entity.IRadarDetectableNT.RadarScanParams;
import com.hbm.api.entity.RadarEntry;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.blocks.machine.BlockSeal;
import com.hbm.entity.missile.EntityMissileStealth;
import com.hbm.entity.missile.EntityMissileTier1;
import com.hbm.entity.missile.ModMissileEntities;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityMachineTeleporter;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The radar and utility machines without a client: the radar's converters and both redstone modes against the
 * 1.7 formulas, the silo hatch closing and opening inside its frame, and the teleporter spending a million HE to
 * move what stands on it.
 */
public final class RadarUtilityGameTests {

    private RadarUtilityGameTests() {}

    /** 1.7 {@code getRedPower}: proximity is {@code 15 - floor(dist / (range * sqrt 2) * 15)}, tier is blip + 1. */
    public static void radarDetectionAndRedstone(GameTestHelper helper) {
        TileEntityMachineRadarNT.registerEntityClasses();
        TileEntityMachineRadarNT.registerConverters();

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModRadarUtilityBlocks.machine_radar.get().defaultBlockState());
        TileEntityMachineRadarNT radar = GameTestHelpers.blockEntity(helper, pos, TileEntityMachineRadarNT.class);
        helper.assertTrue(radar != null, "no radar block entity");
        BlockPos abs = helper.absolutePos(pos);

        // a tier-1 missile falling, seen with smart mode on: supplies redstone; climbing: does not
        EntityMissileTier1.EntityMissileGeneric missile = new EntityMissileTier1.EntityMissileGeneric(
                ModMissileEntities.MISSILE_GENERIC.get(), helper.getLevel());
        missile.setPos(abs.getX() + 500.5, abs.getY() + 100, abs.getZ() + 0.5);
        missile.setDeltaMovement(new Vec3(0, -1, 0));
        RadarScanParams smart = new RadarScanParams(true, true, true, true);
        RadarEntry entry = convert(missile, radar, smart);
        helper.assertTrue(entry != null && entry.blipLevel == IRadarDetectableNT.TIER1
                        && "radar.target.tier1".equals(entry.unlocalizedName) && entry.redstone,
                "a falling tier-1 missile is not a redstone tier-1 blip");
        missile.setDeltaMovement(new Vec3(0, 1, 0));
        helper.assertTrue(!convert(missile, radar, smart).redstone, "smart mode counted an ascending missile");
        helper.assertTrue(convert(missile, radar, new RadarScanParams(false, true, true, true)) == null,
                "a missile was seen with missile scanning off");

        EntityMissileStealth stealth = new EntityMissileStealth(ModMissileEntities.MISSILE_STEALTH.get(), helper.getLevel());
        helper.assertTrue(convert(stealth, radar, smart) == null, "the stealth missile showed up");

        // proximity: 500 blocks off a 1000-block radar is floor(500 / 1414.2 * 15) = 5 below 15
        radar.redMode = true;
        radar.entries.clear();
        radar.entries.add(new RadarEntry("x", IRadarDetectableNT.TIER1, abs.getX() + 500, abs.getY() + 100,
                abs.getZ(), "minecraft:overworld", 0, true));
        helper.assertTrue(radar.getRedPower() == 10, "proximity redstone was " + radar.getRedPower() + ", expected 10");
        radar.entries.add(new RadarEntry("y", IRadarDetectableNT.TIER4, abs.getX(), abs.getY() + 100, abs.getZ(),
                "minecraft:overworld", 1, false));
        helper.assertTrue(radar.getRedPower() == 10, "a non-redstone entry drove the proximity output");
        radar.redMode = false;
        helper.assertTrue(radar.getRedPower() == IRadarDetectableNT.TIER1 + 1, "tier redstone was " + radar.getRedPower());
        radar.entries.clear();
        helper.assertTrue(radar.getRedPower() == 0, "an empty scope put out redstone");

        helper.succeed();
    }

    private static RadarEntry convert(Entity e, TileEntityMachineRadarNT radar, RadarScanParams params) {
        for (var converter : TileEntityMachineRadarNT.converters) {
            RadarEntry entry = converter.apply(new TileEntityMachineRadarNT.Scan(e, radar, params));
            if (entry != null) return entry;
        }
        return null;
    }

    /** A 5x5 ring with the controller in its near edge: toggling fills the 3x3 inside with hatch, then clears it. */
    public static void sealTogglesHatch(GameTestHelper helper) {
        BlockPos controller = new BlockPos(2, 1, 0);
        // FACING north: the face looks north, the ring runs south (+Z) from the controller
        helper.setBlock(controller, ModRadarUtilityBlocks.seal_controller.get().defaultBlockState()
                .setValue(BlockSeal.FACING, Direction.NORTH));
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                boolean edge = x == 0 || x == 4 || z == 0 || z == 4;
                if (edge && !(x == 2 && z == 0)) helper.setBlock(new BlockPos(x, 1, z), ModBlocks.seal_frame.get().defaultBlockState());
                // closeSeal only fills air (as in 1.7), and the 26.1 runner's enclosure of the 1x1x1 test
                // structure left a barrier at (1, 1, 1) inside this frame; start from a clear interior.
                else if (!edge) helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR.defaultBlockState());
            }
        }
        BlockPos abs = helper.absolutePos(controller);
        int size = BlockSeal.getFrameSize(helper.getLevel(), abs);
        helper.assertTrue(size == 2, "the 5x5 frame was sized " + size);

        BlockSeal.closeSeal(helper.getLevel(), abs, size);
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.assertBlockPresent(ModRadarUtilityBlocks.seal_hatch.get(), new BlockPos(x, 1, z));
            }
        }
        helper.assertTrue(BlockSeal.isSealClosed(helper.getLevel(), abs, size), "the closed seal reads open");
        BlockSeal.openSeal(helper.getLevel(), abs, size);
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(2, 1, 2))).isAir(),
                "the opened seal kept its hatch");
        helper.succeed();
    }

    /** One entity on the pad costs 1,000,000 HE and lands with its feet 1.5 above the exit block. */
    public static void teleporterMovesEntity(GameTestHelper helper) {
        BlockPos pad = new BlockPos(1, 1, 1);
        helper.setBlock(pad, ModRadarUtilityBlocks.machine_teleporter.get().defaultBlockState());
        TileEntityMachineTeleporter tele = GameTestHelpers.blockEntity(helper, pad, TileEntityMachineTeleporter.class);
        helper.assertTrue(tele != null, "no teleporter block entity");
        BlockPos exit = helper.absolutePos(new BlockPos(4, 1, 4));
        tele.targetX = exit.getX();
        tele.targetY = exit.getY();
        tele.targetZ = exit.getZ();
        tele.targetDim = Ids.keyId(helper.getLevel().dimension()).toString();
        tele.power = TileEntityMachineTeleporter.maxPower;

        BlockPos abs = helper.absolutePos(pad);
        ItemEntity item = new ItemEntity(helper.getLevel(), abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 0.5,
                new ItemStack(Items.STONE));
        item.setDeltaMovement(Vec3.ZERO);
        item.setNoGravity(true);
        helper.getLevel().addFreshEntity(item);

        TileEntityMachineTeleporter.serverTick(helper.getLevel(), abs, tele.getBlockState(), tele);
        helper.assertTrue(Math.abs(item.getX() - (exit.getX() + 0.5)) < 1e-6 && Math.abs(item.getY() - (exit.getY() + 1.5)) < 1e-6
                && Math.abs(item.getZ() - (exit.getZ() + 0.5)) < 1e-6, "the item did not land on the exit: " + item.position());
        helper.assertTrue(tele.power == TileEntityMachineTeleporter.maxPower - TileEntityMachineTeleporter.consumption,
                "the trip cost " + (TileEntityMachineTeleporter.maxPower - tele.power) + " HE");
        item.discard();
        helper.succeed();
    }

    /**
     * The radar map on chunks that were never saved: 1.7 {@code provideChunk} finds nothing and {@code getHeightValue} answers
     * 0 for an absent chunk, which is stored clamped to 50, a black cell, on the first visit. The region-file read is
     * asynchronous here, so a slot visited cold leaves its cells blank until the read lands, while the prefetch that
     * {@code updateServer} runs a few ticks ahead makes the very first visit complete once its reads have landed. The
     * test waits for the IO worker to answer rather than a fixed tick count: under a full GameTest run the region
     * reads of 800+ parallel plots took longer than 20 ticks, which says nothing about the radar.
     */
    public static void radarMapNeverSavedChunks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModRadarUtilityBlocks.machine_radar.get().defaultBlockState());
        TileEntityMachineRadarNT radar = GameTestHelpers.blockEntity(helper, pos, TileEntityMachineRadarNT.class);
        helper.assertTrue(radar != null, "no radar block entity");
        ServerLevel level = helper.getLevel();
        int warmSlot = 7, coldSlot = 300;
        radar.map = new byte[40_000];

        // Cells whose chunk another plot keeps loaded are read live, not off the region file, and which those are depends
        // on the batch layout (1.20.1 Forge: 7 of the cold slot's cells). Only the others are the subject.
        boolean[] warmUnloaded = unloadedCells(radar, level, warmSlot);
        boolean[] coldUnloaded = unloadedCells(radar, level, coldSlot);
        int coldExpected = 0;
        for (boolean unloaded : coldUnloaded) if (unloaded) coldExpected++;
        radar.prefetchSlot(level, warmSlot); // what updateServer does PREFETCH_TICKS before a slot is visited
        radar.sampleSlot(level, coldSlot);   // a slot visited with no prefetch only starts its reads
        helper.assertTrue(blank(radar, coldSlot) == coldExpected, "a cold visit filled "
                + (coldExpected - blank(radar, coldSlot)) + " never-loaded cells before any read could have completed");

        helper.succeedWhen(() -> {
            helper.assertTrue(radar.readsInFlight() == 0, radar.readsInFlight() + " region reads still in flight");
            radar.sampleSlot(level, warmSlot); // the first visit of the prefetched slot
            helper.assertTrue(blank(radar, warmSlot) == 0, "the prefetched slot still had " + blank(radar, warmSlot)
                    + " blank cells on its first visit");
            for (int i = 0; i < 100; i++) {
                if (!warmUnloaded[i]) continue;
                helper.assertTrue(radar.map[warmSlot * 100 + i] == 50, "a never-saved cell was " + radar.map[warmSlot * 100 + i]
                        + ", 1.7 stores 0 clamped to 50");
            }
            radar.sampleSlot(level, coldSlot); // the cold slot's second visit, one sweep later in a real world
            helper.assertTrue(blank(radar, coldSlot) == 0, "the cold slot still had " + blank(radar, coldSlot)
                    + " blank cells once its reads had landed");
        });
    }

    /** Which of a slot's cells lie in a chunk that is not loaded now (the radar's own cell-to-block mapping). */
    private static boolean[] unloadedCells(TileEntityMachineRadarNT radar, ServerLevel level,
            int slot) {
        boolean[] unloaded = new boolean[100];
        int range = radar.getRange();
        for (int i = 0; i < 100; i++) {
            int index = slot * 100 + i;
            int x = radar.getBlockPos().getX() - range + (index % 200) * range * 2 / 200;
            int z = radar.getBlockPos().getZ() - range + index / 200 * range * 2 / 200;
            unloaded[i] = !level.hasChunk(x >> 4, z >> 4);
        }
        return unloaded;
    }

    private static int blank(TileEntityMachineRadarNT radar, int slot) {
        int blank = 0;
        for (int i = 0; i < 100; i++) if (radar.map[slot * 100 + i] == 0) blank++;
        return blank;
    }
}
