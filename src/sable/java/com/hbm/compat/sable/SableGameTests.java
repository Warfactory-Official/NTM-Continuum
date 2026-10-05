package com.hbm.compat.sable;

import com.hbm.Constants;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.blocks.network.BlockConveyorBase;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.tileentity.turret.TileEntityTurretChekhov;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.items.ModMissileItems;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.network.TileEntityPylonBase;
import com.hbm.blocks.ModRadarUtilityBlocks;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.SubLevels;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * HBM blocks carried by Sable, through Sable's own assembly and disassembly routines (the ones Create Aeronautics
 * calls), in a server with Sable loaded ({@code -Psable.run=true}). Every check reads a value, never the absence
 * of an error: the carried inventory, the rebuilt cell index, the graph cells at the new positions, the pylon
 * lists, the missile's start point, the radar's origin, and a count of dropped items that must be zero.
 */
final class SableGameTests {

    private SableGameTests() {}

    static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    static {
        TESTS.function("sable_carry_machine_and_cables", 40, () -> SableGameTests::carryMachineAndCables);
        TESTS.function("sable_carry_pylons", 40, () -> SableGameTests::carryPylons);
        TESTS.function("sable_launch_and_radar", 40, () -> SableGameTests::launchAndRadar);
        TESTS.function("sable_conveyor_items", 120, () -> SableGameTests::conveyorItems);
        TESTS.function("sable_turret_on_turned_ship", 120, () -> SableGameTests::turretOnTurnedShip);
    }

    /**
     * Belts on a sub-level: an item already riding a belt is carried with it into the plot, an item put on a carried
     * belt stays in the plot rather than being kicked to the world, and both are still on their belts and have moved
     * along them two seconds later.
     */
    private static void conveyorItems(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos start = fixture(helper, 3);
        Set<BlockPos> blocks = new LinkedHashSet<>();
        List<BlockPos> belts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            BlockPos deck = start.east(i).below();
            level.setBlock(deck, Blocks.IRON_BLOCK.defaultBlockState(), 3);
            blocks.add(deck);
            BlockPos belt = start.east(i);
            level.setBlock(belt, ModConveyorBlocks.CONVEYOR.get().defaultBlockState()
                    .setValue(BlockConveyorBase.FACING, Direction.EAST), 3);
            blocks.add(belt);
            belts.add(belt);
        }
        EntityMovingItem riding = movingItem(level, belts.get(0));

        ServerSubLevel sub = assemble(level, start, blocks);
        SubLevelAssemblyHelper.AssemblyTransform up = new SubLevelAssemblyHelper.AssemblyTransform(
                start, sub.getPlot().getCenterBlock(), 0, Rotation.NONE, level);
        helper.assertTrue(SubLevels.isOnSubLevel(level, riding.position()) && riding.blockPosition().equals(up.apply(belts.get(0))),
                "the item riding the first belt was not carried with it (at " + riding.position() + ")");
        EntityMovingItem placed = movingItem(level, up.apply(belts.get(2)));
        helper.assertTrue(SubLevels.isOnSubLevel(level, placed.position()),
                "an item put on a carried belt was kicked out of the plot (at " + placed.position() + ")");
        Vec3 ridingFrom = riding.position();
        Vec3 placedFrom = placed.position();

        helper.runAfterDelay(40, () -> {
            for (EntityMovingItem item : List.of(riding, placed)) {
                helper.assertTrue(!item.isRemoved(), "an item fell off the carried belts (" + item.getRemovalReason()
                        + " at " + item.position() + ", carried from " + (item == riding ? ridingFrom : placedFrom) + ")");
                helper.assertTrue(SubLevels.isOnSubLevel(level, item.position()), "an item left the plot: " + item.position());
            }
            double ridden = riding.position().distanceTo(ridingFrom);
            double travelled = placed.position().distanceTo(placedFrom);
            helper.assertTrue(ridden > 0.5 && travelled > 0.5,
                    "the items did not travel along the belts (" + ridden + ", " + travelled + " blocks)");
            helper.succeed();
        });
    }

    private static EntityMovingItem movingItem(ServerLevel level, BlockPos belt) {
        EntityMovingItem item = new EntityMovingItem(level);
        item.setItemStack(new ItemStack(Items.IRON_INGOT));
        item.moveTo(belt.getX() + 0.5, belt.getY() + 0.25, belt.getZ() + 0.5, 0F, 0F);
        level.addFreshEntity(item);
        return item;
    }

    /**
     * A Chekhov turret on a ship turned a quarter about the vertical: it acquires a zombie in the world and its
     * barrel, as drawn with the ship, ends up pointing at it. Its own yaw is then a quarter off the world yaw, which
     * is the frame conversion doing its work (without it the barrel would point a quarter away).
     */
    private static void turretOnTurnedShip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos clicked = fixture(helper, 4);
        BlockMultiblockCore block = ModTurretBlocks.turret_chekhov.get();
        Player placer = helper.makeMockPlayer(GameType.SURVIVAL);
        placer.setYRot(0F);
        placer.setPos(clicked.getX() - 6D, clicked.getY(), clicked.getZ() - 6D);
        level.setBlock(clicked, block.defaultBlockState(), 3);
        block.setPlacedBy(level, clicked, block.defaultBlockState(), placer, new ItemStack(block));
        BlockPos core = clicked.above(block.getHeightOffset()).relative(Direction.NORTH, -block.getOffset());
        Set<BlockPos> blocks = new LinkedHashSet<>(MultiblockParts.partsWith(level, core));
        helper.assertTrue(blocks.size() > 1, "the turret was not placed");
        Set<BlockPos> deck = new LinkedHashSet<>();
        for (BlockPos part : blocks) if (part.getY() == core.getY()) deck.add(part.below());
        for (BlockPos pos : deck) level.setBlock(pos, Blocks.IRON_BLOCK.defaultBlockState(), 3);
        blocks.addAll(deck);

        ServerSubLevel sub = assemble(level, core, blocks);
        BlockPos plotCore = new SubLevelAssemblyHelper.AssemblyTransform(
                core, sub.getPlot().getCenterBlock(), 0, Rotation.NONE, level).apply(core);
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        Vector3d lifted = new Vector3d(sub.logicalPose().position()).add(0D, 40D, 0D);
        Quaterniond turned = new Quaterniond().rotateY(Math.PI / 2D);
        container.physicsSystem().getPipeline().resetVelocity(sub);
        container.physicsSystem().getPipeline().teleport(sub, lifted, turned);
        // the logical pose follows the physics body on its next step, which a test cannot wait on: state it too
        sub.logicalPose().position().set(lifted);
        sub.logicalPose().orientation().set(turned);

        helper.runAfterDelay(1, () -> {
            Vec3 east = SubLevels.toWorldDirection(level, plotCore, new Vec3(1, 0, 0));
            helper.assertTrue(Math.abs(east.x) < 0.1, "the ship did not turn (its east is " + east + ")");
            TileEntityTurretChekhov turret = (TileEntityTurretChekhov) level.getBlockEntity(plotCore);
            helper.assertTrue(turret != null, "the carried turret has no block entity");
            turret.isOn = true;
            turret.setPower(turret.getMaxPower());

            Vec3 pivot = turret.getTurretPos();
            Zombie zombie = EntityType.ZOMBIE.create(level);
            zombie.setNoAi(true);
            zombie.setNoGravity(true);
            zombie.setPos(pivot.x + 8D, pivot.y - 1D, pivot.z);
            level.addFreshEntity(zombie);

            BlockState state = level.getBlockState(plotCore);
            for (int i = 0; i < 60; i++) TileEntityTurretChekhov.serverTick(level, plotCore, state, turret);
            helper.assertTrue(turret.target == zombie, "the turret on the ship did not acquire the zombie (target " + turret.target + ")");

            // the barrel as drawn: the turret's own yaw and pitch, in the frame of the ship, turned into the world
            Vec3 local = new Vec3(1D, 0D, 0D).zRot((float) -turret.rotationPitch).yRot((float) -(turret.rotationYaw + Math.PI * 0.5));
            Vec3 barrel = SubLevels.toWorldDirection(level, plotCore, local).normalize();
            Vec3 toTarget = turret.getEntityPos(zombie).subtract(turret.getTurretPos()).normalize();
            double degrees = Math.toDegrees(Math.acos(Math.min(1D, barrel.dot(toTarget))));
            helper.assertTrue(degrees < 5D, "the barrel points " + degrees + " degrees off the zombie");
            double worldYaw = -Math.atan2(toTarget.x, toTarget.z);
            double offset = Math.abs(Math.IEEEremainder(turret.rotationYaw - worldYaw, Math.PI * 2D));
            helper.assertTrue(Math.abs(offset - Math.PI / 2D) < 0.1,
                    "the turret yaw is " + Math.toDegrees(offset) + " degrees from the world yaw, expected 90");
            zombie.discard();
            helper.succeed();
        });
    }

    /**
     * A centrifuge (a folded multiblock) holding an item, with two cables at its side, assembled into a sub-level and
     * then set back down turned a quarter clockwise: the structure is whole and indexed both times, the item is still
     * in it, the cables are power-graph cells at their new positions (and no longer at the old ones), the menu's reach
     * is measured where the machine is seen, and nothing dropped.
     */
    private static void carryMachineAndCables(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = fixture(helper, 0);
        BlockMultiblockCore centrifuge = ModMachineBlocks.machine_centrifuge.get();
        level.setBlock(core, centrifuge.defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH), 3);
        centrifuge.fillSpace(level, core, Direction.NORTH);
        TileEntityMachineCentrifuge machine = (TileEntityMachineCentrifuge) level.getBlockEntity(core);
        helper.assertTrue(machine != null, "no centrifuge at the core");
        machine.inventory.setStackInSlot(0, new ItemStack(Items.IRON_ORE, 7));

        Set<BlockPos> blocks = new LinkedHashSet<>(MultiblockParts.partsWith(level, core));
        helper.assertTrue(blocks.size() > 1, "the centrifuge resolved no cells");
        BlockPos cableA = lowestOutside(blocks, core).west();
        BlockPos cableB = cableA.west();
        level.setBlock(cableA, ModMachineBlocks.red_cable.get().defaultBlockState(), 3);
        level.setBlock(cableB, ModMachineBlocks.red_cable.get().defaultBlockState(), 3);
        blocks.add(cableA);
        blocks.add(cableB);
        PowerGraph.get(level); // the graph exists before the move, as it would in a played world
        helper.assertTrue(PowerGraph.get(level).containsCell(cableA.asLong()), "the placed cable is not a graph cell");

        ServerSubLevel sub = assemble(level, core, blocks);
        SubLevelAssemblyHelper.AssemblyTransform up = new SubLevelAssemblyHelper.AssemblyTransform(
                core, sub.getPlot().getCenterBlock(), 0, Rotation.NONE, level);
        BlockPos plotCore = up.apply(core);

        helper.assertTrue(SubLevels.isOnSubLevel(level, plotCore), "the carried core is not on a sub-level");
        helper.assertTrue(level.getBlockState(core).isAir(), "the old core position was not cleared");
        checkMachine(helper, level, plotCore, Direction.NORTH, blocks.size() - 2, "on the sub-level");
        helper.assertTrue(PowerGraph.get(level).containsCell(up.apply(cableA).asLong())
                        && PowerGraph.get(level).containsCell(up.apply(cableB).asLong()),
                "the carried cables did not join the power graph");
        helper.assertTrue(!PowerGraph.get(level).containsCell(cableA.asLong()), "the old cable cell is still in the graph");

        Vec3 seen = SubLevels.toWorld(level, plotCore);
        helper.assertTrue(seen.distanceToSqr(Vec3.atCenterOf(plotCore)) > 10_000, "the sub-level reports no world pose");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(seen.x + 2, seen.y, seen.z);
        helper.assertTrue(BlockMultiblockCore.isWithinInteractionRange(level, plotCore, player),
                "a player beside the carried machine is out of menu reach");
        player.setPos(seen.x + 100, seen.y, seen.z);
        helper.assertTrue(!BlockMultiblockCore.isWithinInteractionRange(level, plotCore, player),
                "a player 100 blocks from the carried machine is in menu reach");

        // Set it back down a quarter turn clockwise, as Create Aeronautics' disassembly does (angle = 4 - ordinal).
        List<BlockPos> carried = new ArrayList<>();
        for (BlockPos pos : blocks) carried.add(up.apply(pos));
        SubLevelAssemblyHelper.AssemblyTransform down = new SubLevelAssemblyHelper.AssemblyTransform(
                plotCore, core, 4 - Rotation.CLOCKWISE_90.ordinal(), Rotation.CLOCKWISE_90, level);
        SubLevelAssemblyHelper.moveBlocks(level, down, carried);
        remove(level, sub);

        checkMachine(helper, level, core, Direction.EAST, blocks.size() - 2, "after disassembly");
        helper.assertTrue(!SubLevels.isOnSubLevel(level, core), "the set-down core still reports a sub-level");
        BlockPos downA = down.apply(up.apply(cableA)), downB = down.apply(up.apply(cableB));
        helper.assertTrue(level.getBlockState(downA).is(ModMachineBlocks.red_cable.get())
                        && PowerGraph.get(level).containsCell(downA.asLong())
                        && PowerGraph.get(level).containsCell(downB.asLong()),
                "the set-down cables are not graph cells at their turned positions");
        helper.assertTrue(!PowerGraph.get(level).containsCell(up.apply(cableA).asLong()), "the plot cable cell is still in the graph");

        assertNoDrops(helper, level, core, plotCore);
        clear(level, core);
        helper.succeed();
    }

    private static void checkMachine(GameTestHelper helper, ServerLevel level, BlockPos core, Direction facing,
                                     int cells, String when) {
        BlockState state = level.getBlockState(core);
        helper.assertTrue(MultiblockSurface.foldedCore(state) != null, "no centrifuge core " + when);
        helper.assertTrue(state.getValue(BlockMultiblockCore.FACING) == facing,
                "the core faces " + state.getValue(BlockMultiblockCore.FACING) + " " + when + ", expected " + facing);
        helper.assertTrue(level.getBlockEntity(core) instanceof TileEntityMachineCentrifuge machine
                        && machine.inventory.getStackInSlot(0).is(Items.IRON_ORE)
                        && machine.inventory.getStackInSlot(0).getCount() == 7,
                "the centrifuge lost its 7 iron ore " + when);
        int indexed = 0;
        for (BlockPos part : MultiblockParts.partsWith(level, core)) {
            if (part.equals(core)) continue;
            helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(level, part)), "cell " + part + " has no core " + when);
            indexed++;
        }
        helper.assertTrue(indexed == cells - 1, indexed + " of " + (cells - 1) + " cells indexed " + when);
    }

    /**
     * Three linked pylons, two of them carried away together: the carried pair stays wired to each other at their new
     * positions and in one power network; the wire to the pylon left behind is gone from both ends.
     */
    private static void carryPylons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = fixture(helper, 1);
        BlockPos a = centre, b = centre.east(4), c = centre.west(4);
        for (BlockPos pos : List.of(a, b, c)) level.setBlock(pos, ModEnergyNetBlocks.red_pylon.get().defaultBlockState(), 3);
        // A beam under the carried pair keeps it one solid piece; a gap would have Sable split it in two.
        List<BlockPos> beam = new ArrayList<>();
        for (int i = 0; i <= 4; i++) beam.add(a.below().east(i));
        for (BlockPos pos : beam) level.setBlock(pos, Blocks.IRON_BLOCK.defaultBlockState(), 3);
        TileEntityPylonBase pa = (TileEntityPylonBase) level.getBlockEntity(a);
        TileEntityPylonBase pb = (TileEntityPylonBase) level.getBlockEntity(b);
        TileEntityPylonBase pc = (TileEntityPylonBase) level.getBlockEntity(c);
        pa.addConnection(b); pb.addConnection(a);
        pa.addConnection(c); pc.addConnection(a);

        Set<BlockPos> blocks = new LinkedHashSet<>(List.of(a, b));
        blocks.addAll(beam);
        ServerSubLevel sub = assemble(level, a, blocks);
        SubLevelAssemblyHelper.AssemblyTransform up = new SubLevelAssemblyHelper.AssemblyTransform(
                a, sub.getPlot().getCenterBlock(), 0, Rotation.NONE, level);
        BlockPos na = up.apply(a), nb = up.apply(b);

        TileEntityPylonBase ca = (TileEntityPylonBase) level.getBlockEntity(na);
        TileEntityPylonBase cb = (TileEntityPylonBase) level.getBlockEntity(nb);
        helper.assertTrue(ca != null && cb != null, "the carried pylons have no block entities");
        helper.assertTrue(ca.getConnected().equals(List.of(nb)), "carried pylon A lists " + ca.getConnected() + ", expected [" + nb + "]");
        helper.assertTrue(cb.getConnected().equals(List.of(na)), "carried pylon B lists " + cb.getConnected() + ", expected [" + na + "]");
        helper.assertTrue(pc.getConnected().isEmpty(), "the pylon left behind still lists " + pc.getConnected());
        var graph = PowerGraph.get(level);
        helper.assertTrue(graph.networkAt(na.asLong()) != null && graph.networkAt(na.asLong()) == graph.networkAt(nb.asLong()),
                "the carried pylons are not one power network");
        helper.assertTrue(!graph.containsCell(a.asLong()) && !graph.containsCell(b.asLong()), "old pylon cells are still in the graph");

        clear(level, c);
        helper.succeed();
    }

    /**
     * A launch pad and a radar on a sub-level: the missile starts where the pad is seen and lays its arc out from
     * there (its start, not the plot's far-off coordinates), and the radar scans from where it is seen.
     */
    private static void launchAndRadar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos padCore = fixture(helper, 2);
        BlockMultiblockCore pad = ModMachineBlocks.launch_pad.get();
        level.setBlock(padCore, pad.defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH), 3);
        pad.fillSpace(level, padCore, Direction.NORTH);
        Set<BlockPos> blocks = new LinkedHashSet<>(MultiblockParts.partsWith(level, padCore));
        BlockPos radar = lowestOutside(blocks, padCore).west();
        level.setBlock(radar, ModRadarUtilityBlocks.machine_radar.get().defaultBlockState(), 3);
        blocks.add(radar);

        ServerSubLevel sub = assemble(level, padCore, blocks);
        SubLevelAssemblyHelper.AssemblyTransform up = new SubLevelAssemblyHelper.AssemblyTransform(
                padCore, sub.getPlot().getCenterBlock(), 0, Rotation.NONE, level);
        BlockPos plotPad = up.apply(padCore), plotRadar = up.apply(radar);

        TileEntityLaunchPadBase launcher = (TileEntityLaunchPadBase) level.getBlockEntity(plotPad);
        helper.assertTrue(launcher != null, "the carried pad has no block entity");
        launcher.inventory.setStackInSlot(0, new ItemStack(ModMissileItems.missile_generic.get()));
        Entity missile = launcher.instantiateMissile(padCore.getX() + 500, padCore.getZ());
        helper.assertTrue(missile instanceof EntityMissileBaseNT, "the carried pad built no missile");
        Vec3 expected = SubLevels.toWorld(level, plotPad.getX() + 0.5D, plotPad.getY() + launcher.getLaunchOffset(), plotPad.getZ() + 0.5D);
        helper.assertTrue(missile.position().distanceToSqr(expected) < 0.01,
                "the missile spawned at " + missile.position() + ", expected the pad's world position " + expected);
        helper.assertTrue(!SubLevels.isOnSubLevel(level, missile.position()), "the missile spawned inside the plot");
        EntityMissileBaseNT ballistic = (EntityMissileBaseNT) missile;
        helper.assertTrue(ballistic.startX == (int) expected.x && ballistic.startZ == (int) expected.z,
                "the missile's arc starts at " + ballistic.startX + "," + ballistic.startZ + ", not at the pad");

        TileEntityMachineRadarNT scope = (TileEntityMachineRadarNT) level.getBlockEntity(plotRadar);
        helper.assertTrue(scope != null, "the carried radar has no block entity");
        TileEntityMachineRadarNT.serverTick(level, plotRadar, level.getBlockState(plotRadar), scope);
        BlockPos seen = SubLevels.toWorldBlock(level, plotRadar);
        helper.assertTrue(scope.origin.equals(seen) && !seen.equals(plotRadar),
                "the radar scans from " + scope.origin + ", expected its world position " + seen);

        helper.succeed();
    }

    // --- helpers ---

    /**
     * Where test {@code index} builds. Not in the GameTest plot: 1.21.1's test server puts its plots at random
     * coordinates up to 15 million blocks out, which can fall inside Sable's plot grid (where sub-level blocks are
     * kept), and Sable refuses to assemble there. Near the world origin is always outside it.
     */
    private static BlockPos fixture(GameTestHelper helper, int index) {
        BlockPos pos = new BlockPos(index * 64, 120, 0);
        helper.getLevel().getChunk(pos);
        return pos;
    }

    /** Clears what a fixture left in the world (the GameTest framework only clears its own plot). */
    private static void clear(ServerLevel level, BlockPos around) {
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-8, -4, -8), around.offset(8, 8, 8))) {
            if (!level.getBlockState(pos).isAir()) {
                BlockMultiblockCore.withoutTeardown(() -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16));
            }
        }
    }

    /**
     * Assembles {@code blocks} into a sub-level and force-loads it (Sable's {@code /sable forceload add}): the GameTest
     * server has no player near the plot, so otherwise the sub-level and the entities in its plot unload at once.
     */
    private static ServerSubLevel assemble(ServerLevel level, BlockPos anchor, Set<BlockPos> blocks) {
        BoundingBox3i bounds = BoundingBox3i.from(blocks);
        ServerSubLevel sub = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, bounds.expand(1, 1, 1));
        SubLevelContainer.getContainer(level).addForceLoadTicket(sub, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE);
        return sub;
    }

    /**
     * Removes a sub-level whose blocks were all set back down. A sub-level still holding blocks is left alone: the
     * GameTest world is deleted before every run, and destroying a sub-level's blocks from inside a test leaves
     * Sable a split to run on a sub-level that is gone by then.
     */
    private static void remove(ServerLevel level, ServerSubLevel sub) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null && !sub.isRemoved()) container.removeSubLevel(sub, SubLevelRemovalReason.REMOVED);
    }


    /** The lowest, west-most block of a footprint, from which a neighbour outside it is chosen. */
    private static BlockPos lowestOutside(Set<BlockPos> blocks, BlockPos core) {
        BlockPos west = core;
        for (BlockPos pos : blocks) if (pos.getY() == core.getY() && pos.getX() < west.getX()) west = pos;
        return west;
    }

    private static void assertNoDrops(GameTestHelper helper, ServerLevel level, BlockPos... around) {
        int drops = 0;
        for (BlockPos pos : around) drops += level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(16)).size();
        helper.assertTrue(drops == 0, drops + " item(s) dropped during the move");
    }
}
