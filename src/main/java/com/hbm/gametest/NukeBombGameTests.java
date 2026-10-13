package com.hbm.gametest;

import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.bomb.NukeN2;
import com.hbm.config.BombConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.container.ContainerNukeN2;
import com.hbm.inventory.container.ContainerNukeTsar;
import com.hbm.items.ModBombItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMissileItems;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBoy;
import com.hbm.tileentity.bomb.TileEntityNukeGadget;
import com.hbm.tileentity.bomb.TileEntityNukeN2;
import com.hbm.tileentity.bomb.TileEntityNukeTsar;
import com.hbm.world.chunk.ChunkLoadingManager;
import com.hbm.world.chunk.PendingDetonations;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.net.GolemBuffers;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Focused contracts for the component-built MK5/Torex bomb blocks. */
public final class NukeBombGameTests {

    private NukeBombGameTests() {}

    public static void slotPredicates(GameTestHelper helper) {
        TileEntityNukeN2 n2 = placeN2(helper, new BlockPos(2, 2, 2));
        helper.assertTrue(n2.getContainerSize() == 12 && n2.inventory.getSlotLimit(0) == 1,
                "N2 must expose twelve one-charge slots");
        helper.assertTrue(n2.isItemValidForSlot(0, new ItemStack(ModBombItems.n2_charge.get())),
                "N2 rejected its charge");
        helper.assertFalse(n2.isItemValidForSlot(0, new ItemStack(Items.TNT)),
                "N2 accepted a non-charge item");

        TileEntityNukeBoy boy = place(helper, new BlockPos(5, 2, 2), ModBombBlocks.nuke_boy.get(), TileEntityNukeBoy.class);
        helper.assertTrue(boy.isItemValidForSlot(0, new ItemStack(ModBombItems.boy_shielding.get()))
                        && !boy.isItemValidForSlot(0, new ItemStack(ModBombItems.boy_target.get())),
                "Little Boy slot 0 lost its shielding predicate");
        TileEntityNukeGadget gadget = place(helper, new BlockPos(8, 2, 2), ModBombBlocks.nuke_gadget.get(), TileEntityNukeGadget.class);
        helper.assertTrue(gadget.isItemValidForSlot(0, new ItemStack(ModBombItems.gadget_wireing.get()))
                        && !gadget.isItemValidForSlot(0, new ItemStack(ModBombItems.gadget_core.get())),
                "Gadget wiring slot accepted the wrong component");
        TileEntityNukeTsar tsar = place(helper, new BlockPos(11, 2, 2), ModBombBlocks.nuke_tsar.get(), TileEntityNukeTsar.class);
        helper.assertTrue(tsar.isItemValidForSlot(4, new ItemStack(ModBombItems.man_core.get()))
                        && tsar.isItemValidForSlot(5, new ItemStack(ModBombItems.tsar_core.get()))
                        && !tsar.isItemValidForSlot(4, new ItemStack(ModBombItems.tsar_core.get())),
                "Tsar core slots lost their ordered predicates");
        helper.succeed();
    }

    public static void n2RadiusAndEffects(GameTestHelper helper) {
        TileEntityNukeN2 n2 = placeN2(helper, new BlockPos(2, 2, 6));
        // 1.7 TileEntityNukeN2.isReady: eleven charges are not enough
        for (int i = 0; i < 11; i++) n2.setItem(i, new ItemStack(ModBombItems.n2_charge.get()));
        BlockPos origin = n2.getBlockPos();
        helper.assertTrue(!n2.isReady() && ModBombBlocks.nuke_n2.get().explode(helper.getLevel(), origin, null)
                        == IBomb.BombReturnCode.ERROR_MISSING_COMPONENT
                        && !helper.getLevel().getBlockState(origin).isAir(),
                "N2 armed with 11 of 12 charges (1.7 needs all twelve)");
        n2.setItem(11, new ItemStack(ModBombItems.n2_charge.get()));
        try {
            IBomb.BombReturnCode result = ModBombBlocks.nuke_n2.get().explode(helper.getLevel(), origin, null);
            helper.assertTrue(result == IBomb.BombReturnCode.DETONATED, "armed N2 did not detonate");
            EntityNukeExplosionMK5 blast = onlyBlast(helper, origin);
            helper.assertTrue(blast.getRadius() == BombConfig.n2Radius && !blast.hasFallout(),
                    "N2 did not use the fixed 1.7 n2Radius and statFacNoRad");
            helper.assertTrue(blast.isChunkLoading(),
                    "N2 did not hand source-chunk ownership to MK5 at spawn time");
            var clouds = helper.getLevel().getEntitiesOfClass(EntityNukeTorex.class,
                    new AABB(origin).inflate(2D));
            helper.assertTrue(!clouds.isEmpty(),
                    "N2 did not create its configured Torex cloud");
            helper.assertTrue(clouds.get(0).isChunkLoading(),
                    "Torex did not claim its source chunk before the spawning blast handoff");
            helper.succeed();
        } finally {
            discardNuclearNear(helper, origin);
        }
    }

    public static void menuRoutesComponents(GameTestHelper helper) {
        TileEntityNukeN2 n2 = placeN2(helper, new BlockPos(2, 2, 10));
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(ModBombItems.n2_charge.get()));
        ContainerNukeN2 menu = new ContainerNukeN2(0, player.getInventory(), n2);
        helper.assertTrue(menu.slots.size() == 48, "N2 menu lost its twelve machine or 36 player slots");
        menu.quickMoveStack(player, 39);
        helper.assertTrue(n2.getItem(0).is(ModBombItems.n2_charge.get())
                        && player.getInventory().getItem(0).isEmpty(),
                "N2 shift-click did not route a charge into the first valid slot");
        player.getInventory().setItem(0, new ItemStack(Items.TNT));
        menu.quickMoveStack(player, 39);
        helper.assertTrue(n2.getItem(1).isEmpty() && player.getInventory().getItem(0).is(Items.TNT),
                "N2 menu accepted a wrong component through its validated slots");

        TileEntityNukeTsar tsar = place(helper, new BlockPos(8, 2, 10), ModBombBlocks.nuke_tsar.get(),
                TileEntityNukeTsar.class);
        ContainerNukeTsar tsarMenu = new ContainerNukeTsar(1, player.getInventory(), tsar);
        helper.assertTrue(tsarMenu.getSlot(6).x == 48 && tsarMenu.getSlot(6).y == 151
                        && tsarMenu.getSlot(33).x == 48 && tsarMenu.getSlot(33).y == 209,
                "Tsar player inventory no longer matches the legacy GUI coordinates");
        helper.succeed();
    }

    public static void persistenceAndSync(GameTestHelper helper) {
        TileEntityNukeN2 n2 = placeN2(helper, new BlockPos(2, 2, 14));
        n2.setItem(0, new ItemStack(ModBombItems.n2_charge.get()));
        n2.setItem(7, new ItemStack(ModBombItems.n2_charge.get()));
        var buffer = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        n2.serialize(buffer);
        buffer.readerIndex(0);
        TileEntityNukeN2 copy = new TileEntityNukeN2(n2.getBlockPos().east(), n2.getBlockState());
        copy.deserialize(buffer);
        helper.assertTrue(copy.getItem(0).is(ModBombItems.n2_charge.get())
                        && copy.getItem(7).is(ModBombItems.n2_charge.get())
                        && copy.countCharges() == 2,
                "N2 component inventory did not survive the live sync payload");
        buffer.release();
        helper.succeed();
    }

    public static void redstoneAndDetonator(GameTestHelper helper) {
        TileEntityNukeN2 n2 = placeN2(helper, new BlockPos(2, 2, 18));
        armN2(n2);
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        TileEntityNukeN2 redstone = placeN2(helper, new BlockPos(8, 2, 18));
        armN2(redstone);
        BlockPos directOrigin = n2.getBlockPos();
        BlockPos redstoneOrigin = redstone.getBlockPos();
        try {
            IBomb.BombReturnCode result = ModBombBlocks.nuke_n2.get().explode(helper.getLevel(), directOrigin, player);
            EntityNukeExplosionMK5 blast = onlyBlast(helper, directOrigin);
            helper.assertTrue(result == IBomb.BombReturnCode.DETONATED
                            && helper.getLevel().getBlockState(directOrigin).isAir()
                            && n2.isEmpty() && blast.detonator.equals(player.getUUID()),
                    "N2 IBomb detonation did not consume components or retain its detonator");

            BlockPos power = redstoneOrigin.east();
            helper.getLevel().setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            helper.assertTrue(helper.getLevel().hasNeighborSignal(redstoneOrigin),
                    "the redstone block did not provide a neighbor signal");
            helper.getLevel().updateNeighborsAt(power, Blocks.REDSTONE_BLOCK);
            helper.assertTrue(helper.getLevel().getBlockState(redstoneOrigin).isAir(),
                    "powered N2 did not take the redstone detonation path");
            helper.succeed();
        } finally {
            discardNuclearNear(helper, directOrigin);
            discardNuclearNear(helper, redstoneOrigin);
        }
    }

    public static void consumptionAndChunkLifecycle(GameTestHelper helper) {
        TileEntityNukeN2 n2 = placeN2(helper, new BlockPos(2, 2, 22));
        armN2(n2);
        BlockPos origin = n2.getBlockPos();
        try {
            ModBombBlocks.nuke_n2.get().explode(helper.getLevel(), origin, null);
            helper.assertTrue(helper.getLevel().getBlockState(origin).isAir() && n2.isEmpty(),
                    "N2 left its consumed component inventory behind");
            EntityNukeExplosionMK5 blast = onlyBlast(helper, origin);
            blast.tick();
            int cx = origin.getX() >> 4;
            int cz = origin.getZ() >> 4;
            helper.assertTrue(ChunkLoadingManager.isForced(helper.getLevel(), cx, cz),
                    "MK5 blast did not claim its active chunk");
            blast.discard();
            discardTorexNear(helper, origin);
            helper.assertFalse(ChunkLoadingManager.isForced(helper.getLevel(), cx, cz),
                    "MK5 blast left a forced chunk claim after removal");

            EntityFalloutRain rain = new EntityFalloutRain(ModEntities.FALLOUT_RAIN.get(), helper.getLevel());
            rain.setPos(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);
            helper.getLevel().addFreshEntity(rain);
            rain.startChunkLoading();
            helper.assertTrue(rain.isChunkLoading() && ChunkLoadingManager.isForced(helper.getLevel(), cx, cz),
                    "fallout did not claim its source chunk before the MK5 handoff");
            rain.discard();
            helper.assertFalse(ChunkLoadingManager.isForced(helper.getLevel(), cx, cz),
                    "fallout left its source chunk forced after removal");
            helper.succeed();
        } finally {
            discardNuclearNear(helper, origin);
        }
    }

    public static void detonatorLoadsTarget(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack detonator = new ItemStack(ModMissileItems.detonator.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, detonator);

        BlockPos unloaded = new BlockPos(1_600_008, 64, 1_600_008);
        int unloadedX = unloaded.getX() >> 4;
        int unloadedZ = unloaded.getZ() >> 4;
        helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(unloadedX, unloadedZ) == null,
                "deferred-detonator test target unexpectedly started loaded");
        ModDataComponents.DETONATOR_TARGET.set(detonator, unloaded);
        detonator.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(PendingDetonations.isPending(helper.getLevel(), unloaded),
                "unloaded target was not retained while its chunk loads");
        helper.assertTrue(PendingDetonations.isLoading(helper.getLevel(), unloaded),
                "detonator did not request a ticking chunk for its unloaded target");
        awaitLoadedDetonatorTarget(helper, unloaded, 400, () -> verifyNukeHandoff(helper));
    }

    private static void awaitLoadedDetonatorTarget(GameTestHelper helper, BlockPos target, int triesLeft,
                                                    Runnable then) {
        if (helper.getLevel().getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4) != null) {
            helper.assertFalse(PendingDetonations.isPending(helper.getLevel(), target),
                    "resident remote target still had an unconsumed detonation request");
            helper.runAfterDelay(3, () -> {
                helper.assertFalse(PendingDetonations.isLoading(helper.getLevel(), target),
                        "remote target ticket survived its two-tick handoff window");
                then.run();
            });
            return;
        }
        if (triesLeft <= 0) {
            helper.fail("detonator ticket never made its unloaded target chunk resident");
            return;
        }
        // The GameTest server runs its ticks back to back, so 400 ticks can pass in well under a second while a worker
        // generates the virgin chunk 1.6M blocks out (26.1 NeoForge and Fabric both ran out). Give the
        // worker wall-clock time per try: at most ~10 s; chunk completions still run on this thread between ticks.
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        helper.runAfterDelay(1, () -> awaitLoadedDetonatorTarget(helper, target, triesLeft - 1, then));
    }

    private static void verifyNukeHandoff(GameTestHelper helper) {
        TileEntityNukeN2 n2 = placeN2(helper, new BlockPos(2, 2, 2));
        armN2(n2);
        BlockPos origin = n2.getBlockPos();
        try {
            UUID offlineDetonator = UUID.randomUUID();
            PendingDetonations.queue(helper.getLevel(), origin, offlineDetonator);
            helper.assertFalse(helper.getLevel().getBlockState(origin).isAir(),
                    "loading request fired the bomb before its chunk became available");
            PendingDetonations.tick(helper.getLevel());
            EntityNukeExplosionMK5 blast = onlyBlast(helper, origin);
            helper.assertTrue(helper.getLevel().getBlockState(origin).isAir(),
                    "chunk load did not detonate the pending nuke");
            helper.assertTrue(offlineDetonator.equals(blast.detonator),
                    "pending nuke detonation lost the original detonator UUID");
            helper.assertFalse(PendingDetonations.isPending(helper.getLevel(), origin),
                    "chunk load detonated the nuke without consuming its pending request");
            blast.tick();
            helper.assertTrue(ChunkLoadingManager.isForced(helper.getLevel(), origin.getX() >> 4, origin.getZ() >> 4),
                    "spawned MK5 did not claim its chunk on the first tick");
            helper.assertTrue(PendingDetonations.isLoading(helper.getLevel(), origin),
                    "pending owner released before the spawned MK5 took ownership");
            blast.discard();
            discardTorexNear(helper, origin);
            helper.assertTrue(ChunkLoadingManager.isForced(helper.getLevel(), origin.getX() >> 4, origin.getZ() >> 4),
                    "removing the MK5 also removed the pending handoff claim too early");
            helper.runAfterDelay(3, () -> {
                helper.assertFalse(ChunkLoadingManager.isForced(helper.getLevel(), origin.getX() >> 4, origin.getZ() >> 4),
                        "pending detonation ticket survived its two-tick handoff window");
                helper.succeed();
            });
        } finally {
            discardNuclearNear(helper, origin);
        }
    }

    public static void torexTrackingAge(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        EntityNukeTorex cloud = EntityNukeTorex.statFac(helper.getLevel(),
                origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D, 100F);
        for (int i = 0; i < 12; i++) cloud.tick();
        cloud.markTerrainChanged();
        helper.assertTrue(cloud.getEntityData().get(EntityNukeTorex.ANIMATION_AGE) == cloud.tickCount,
                "Torex did not publish its current server animation age for newly tracking clients");
        helper.assertTrue(cloud.getTerrainChangeAge() == cloud.tickCount,
                "Torex did not retain the crater-completion age for faithful late-client replay");
        helper.succeed();
    }

    /** 1.7: N2 fires only with all twelve slots holding a charge. */
    private static void armN2(TileEntityNukeN2 n2) {
        for (int i = 0; i < 12; i++) n2.setItem(i, new ItemStack(ModBombItems.n2_charge.get()));
    }

    private static TileEntityNukeN2 placeN2(GameTestHelper helper, BlockPos relative) {
        return place(helper, relative, ModBombBlocks.nuke_n2.get(), TileEntityNukeN2.class);
    }

    private static <T extends TileEntityNukeBomb> T place(GameTestHelper helper, BlockPos relative,
                                                           Block block,
                                                           Class<T> type) {
        helper.setBlock(relative, block.defaultBlockState());
        return GameTestHelpers.blockEntity(helper, relative, type);
    }

    private static EntityNukeExplosionMK5 onlyBlast(GameTestHelper helper, BlockPos around) {
        var blasts = helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK5.class,
                new AABB(around).inflate(2D));
        helper.assertTrue(blasts.size() == 1, "expected one MK5 blast near " + around + ", got " + blasts.size());
        return blasts.get(0);
    }

    private static void discardTorexNear(GameTestHelper helper, BlockPos origin) {
        helper.getLevel().getEntitiesOfClass(EntityNukeTorex.class,
                new AABB(origin).inflate(2D))
                .forEach(Entity::discard);
    }

    private static void discardNuclearNear(GameTestHelper helper, BlockPos origin) {
        helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK5.class,
                new AABB(origin).inflate(3D))
                .forEach(Entity::discard);
        discardTorexNear(helper, origin);
        helper.getLevel().getEntitiesOfClass(EntityFalloutRain.class,
                new AABB(origin).inflate(3D))
                .forEach(Entity::discard);
    }
}
