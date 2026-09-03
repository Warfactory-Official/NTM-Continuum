package com.hbm.gametest;

import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.bomb.NukeN2;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.container.ContainerNukeN2;
import com.hbm.inventory.container.ContainerNukeTsar;
import com.hbm.items.ModBombItems;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBoy;
import com.hbm.tileentity.bomb.TileEntityNukeGadget;
import com.hbm.tileentity.bomb.TileEntityNukeN2;
import com.hbm.tileentity.bomb.TileEntityNukeTsar;
import com.hbm.world.chunk.ChunkLoadingManager;
import dev.golem.core.gametest.GameTestHelpers;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

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
        helper.assertTrue(NukeN2.radiusForCharges(1) == 16
                        && NukeN2.radiusForCharges(6) == 100
                        && NukeN2.radiusForCharges(12) == BombConfig.n2Radius,
                "N2 radius is not int(n2Radius * charges / 12F)");

        TileEntityNukeN2 n2 = placeN2(helper, new BlockPos(2, 2, 6));
        n2.setItem(0, new ItemStack(ModBombItems.n2_charge.get()));
        IBomb.BombReturnCode result = ModBombBlocks.nuke_n2.get().explode(helper.getLevel(), n2.getBlockPos(), null);
        helper.assertTrue(result == IBomb.BombReturnCode.DETONATED, "armed N2 did not detonate");
        EntityNukeExplosionMK5 blast = onlyBlast(helper, n2.getBlockPos());
        helper.assertTrue(blast.getRadius() == 16 && !blast.hasFallout(),
                "N2 did not use its one-charge radius and statFacNoRad");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(EntityNukeTorex.class,
                        new net.minecraft.world.phys.AABB(n2.getBlockPos()).inflate(2D)).isEmpty(),
                "N2 did not create its configured Torex cloud");
        helper.succeed();
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
        var buffer = dev.golem.core.net.GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
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
        n2.setItem(0, new ItemStack(ModBombItems.n2_charge.get()));
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        IBomb.BombReturnCode result = ModBombBlocks.nuke_n2.get().explode(helper.getLevel(), n2.getBlockPos(), player);
        EntityNukeExplosionMK5 blast = onlyBlast(helper, n2.getBlockPos());
        helper.assertTrue(result == IBomb.BombReturnCode.DETONATED
                        && helper.getLevel().getBlockState(n2.getBlockPos()).isAir()
                        && n2.isEmpty() && blast.detonator.equals(player.getUUID()),
                "N2 IBomb detonation did not consume components or retain its detonator");

        TileEntityNukeN2 redstone = placeN2(helper, new BlockPos(8, 2, 18));
        redstone.setItem(0, new ItemStack(ModBombItems.n2_charge.get()));
        BlockPos power = redstone.getBlockPos().east();
        helper.getLevel().setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().hasNeighborSignal(redstone.getBlockPos()),
                "the redstone block did not provide a neighbor signal");
        helper.getLevel().updateNeighborsAt(power, Blocks.REDSTONE_BLOCK);
        helper.assertTrue(helper.getLevel().getBlockState(redstone.getBlockPos()).isAir(),
                "powered N2 did not take the redstone detonation path");
        helper.succeed();
    }

    public static void consumptionAndChunkLifecycle(GameTestHelper helper) {
        TileEntityNukeN2 n2 = placeN2(helper, new BlockPos(2, 2, 22));
        n2.setItem(0, new ItemStack(ModBombItems.n2_charge.get()));
        BlockPos origin = n2.getBlockPos();
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
        helper.assertFalse(ChunkLoadingManager.isForced(helper.getLevel(), cx, cz),
                "MK5 blast left a forced chunk claim after removal");
        helper.succeed();
    }

    private static TileEntityNukeN2 placeN2(GameTestHelper helper, BlockPos relative) {
        return place(helper, relative, ModBombBlocks.nuke_n2.get(), TileEntityNukeN2.class);
    }

    private static <T extends TileEntityNukeBomb> T place(GameTestHelper helper, BlockPos relative,
                                                           net.minecraft.world.level.block.Block block,
                                                           Class<T> type) {
        helper.setBlock(relative, block.defaultBlockState());
        return GameTestHelpers.blockEntity(helper, relative, type);
    }

    private static EntityNukeExplosionMK5 onlyBlast(GameTestHelper helper, BlockPos around) {
        var blasts = helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK5.class,
                new net.minecraft.world.phys.AABB(around).inflate(2D));
        helper.assertTrue(blasts.size() == 1, "expected one MK5 blast near " + around + ", got " + blasts.size());
        return blasts.get(0);
    }
}
