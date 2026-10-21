package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.entity.missile.EntityMissileShuttle;
import com.hbm.entity.missile.EntityMissileTier0;
import com.hbm.entity.missile.EntityMissileTier1;
import com.hbm.entity.logic.EntityEMP;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.missile.EntityMissileTier2;
import com.hbm.entity.missile.EntityMissileTier3;
import com.hbm.entity.missile.EntityMissileTier4;
import com.hbm.entity.missile.ModMissileEntities;
import com.hbm.blocks.bomb.BlockTaint;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.config.BombConfig;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.explosion.ExplosionNT;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.ModMissileItems;
import com.hbm.inventory.container.ContainerLaunchPad;
import com.hbm.items.tool.ItemDesignator;
import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.items.weapon.ItemMissileStandard;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.world.chunk.ChunkLoaderTicket;
import com.hbm.world.chunk.ChunkLoadingManager;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.net.GolemBuffers;
import dev.golem.core.registry.Entities;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * GameTests for the chunk-loading layer and the missile system.
 *
 * <p>A leaked chunk claim is invisible until a save is inspected, and a missile that fails to launch looks like one
 * that was never fuelled. Each test pins one link (the refcount, the ticket lifecycle, the fuel gate, the launch
 * cost) so a break localises itself.
 */
public final class MissileGameTests {

    private MissileGameTests() {}

    // Chunk loading

    /**
     * Two claims on one chunk, released one at a time. This is the whole reason the manager exists:
     * vanilla's forced set is a boolean, so without refcounting the first release would unforce a chunk
     * the second holder still needs, and the second missile would fall out of the world.
     */
    public static void chunkRefcounting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Plain ints, not ChunkPos: its field/accessor spelling differs by era and the manager's own
        // API is int-based anyway. Far from the test plot so no real chunk is disturbed.
        int cx = (helper.absolutePos(BlockPos.ZERO).getX() >> 4) + 64;
        int cz = (helper.absolutePos(BlockPos.ZERO).getZ() >> 4) + 64;

        ChunkLoaderTicket first = ChunkLoadingManager.requestTicket(level, "test-a");
        ChunkLoaderTicket second = ChunkLoadingManager.requestTicket(level, "test-b");
        try {
            first.force(cx, cz);
            second.force(cx, cz);
            helper.assertTrue(ChunkLoadingManager.isForced(level, cx, cz),
                    "the chunk was not forced after two claims");

            first.release();
            helper.assertTrue(ChunkLoadingManager.isForced(level, cx, cz),
                    "releasing one of two claims unforced the chunk");

            second.release();
            helper.assertFalse(ChunkLoadingManager.isForced(level, cx, cz),
                    "releasing the last claim left the chunk forced — this is the leak case");
        } finally {
            first.release();
            second.release();
        }
        helper.succeed();
    }

    /**
     * A chunk forced through the level's forced set (a /forceload, a GameTest plot) keeps ticking when HBM claims
     * the same chunk first and lets go of it. The two share nothing, so the claim must not take the forced
     * chunk's ticket with it.
     */
    public static void chunkClaimLeavesForcedChunkTicking(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int cx = (origin.getX() >> 4) + 112;
        int cz = (origin.getZ() >> 4) + 112;
        // an entity-ticking chunk needs the chunks within two of it generated
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) level.getChunk(cx + dx, cz + dz);
        }

        ChunkLoaderTicket claim = ChunkLoadingManager.requestTicket(level, "test-forced-neighbour");
        claim.force(cx, cz);
        level.setChunkForced(cx, cz, true);
        claim.release();

        BlockPos probe = new BlockPos(cx << 4, origin.getY(), cz << 4);
        helper.succeedWhen(() -> helper.assertTrue(level.isPositionEntityTicking(probe),
                "a forced chunk stopped ticking once an HBM claim over it was released"));
    }

    /**
     * A ticket following a moving entity claims the new chunk and drops the old one. Upstream unforced
     * everything and re-forced each tick; the port diffs the set instead, so this also checks the diff
     * does not lose the chunk it is supposed to keep.
     */
    public static void chunkFollowsMovement(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int cx = (helper.absolutePos(BlockPos.ZERO).getX() >> 4) + 80;
        int cz = (helper.absolutePos(BlockPos.ZERO).getZ() >> 4) + 80;

        ChunkLoaderTicket ticket = ChunkLoadingManager.requestTicket(level, "test-move");
        try {
            ticket.force(cx, cz);
            LongOpenHashSet next = new LongOpenHashSet();
            next.add(ChunkPos.pack(cx + 1, cz));
            ticket.setChunks(next);

            helper.assertFalse(ChunkLoadingManager.isForced(level, cx, cz),
                    "the chunk the ticket left is still forced");
            helper.assertTrue(ChunkLoadingManager.isForced(level, cx + 1, cz),
                    "the chunk the ticket moved to was not forced");
        } finally {
            ticket.release();
        }
        helper.succeed();
    }

    /** A released ticket must be inert: a late force() from a dead entity would pin a chunk forever. */
    public static void releasedTicketIsInert(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int cx = (helper.absolutePos(BlockPos.ZERO).getX() >> 4) + 96;
        int cz = (helper.absolutePos(BlockPos.ZERO).getZ() >> 4) + 96;

        ChunkLoaderTicket ticket = ChunkLoadingManager.requestTicket(level, "test-dead");
        ticket.force(cx, cz);
        ticket.release();
        ticket.force(cx, cz);

        helper.assertFalse(ChunkLoadingManager.isForced(level, cx, cz),
                "a released ticket still forced a chunk");
        helper.succeed();
    }

    /** A live missile holds its own chunk, and gives it back when it is removed. */
    public static void missileHoldsItsChunk(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));

        EntityMissileTier1.EntityMissileGeneric missile = new EntityMissileTier1.EntityMissileGeneric(
                level, origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 200, origin.getZ() + 200);
        level.addFreshEntity(missile);
        missile.tick();

        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;
        helper.assertTrue(ChunkLoadingManager.isForced(level, cx, cz),
                "a flying missile did not force the chunk it is over");
        helper.assertTrue(ChunkLoadingManager.isForced(level, cx + 1, cz)
                        && ChunkLoadingManager.isForced(level, cx, cz + 1)
                        && ChunkLoadingManager.isForced(level, cx + 1, cz + 1),
                "a flying missile did not preload the chunks across its east/south border");

        missile.discard();
        helper.assertFalse(ChunkLoadingManager.isForced(level, cx, cz),
                "a removed missile left its chunk claim behind");
        helper.assertFalse(ChunkLoadingManager.isForced(level, cx + 1, cz + 1),
                "a removed missile left its border lookahead claim behind");
        helper.succeed();
    }

    /**
     * A missile's pitch must pass −90° on the way down.
     *
     * <p>Upstream's pitch convention runs 0° up, −90° horizontal, −180° down, and {@code Entity.setXRot} silently
     * clamps to [−90, 90], so a missile storing its pitch in the vanilla field would level out at the top of its arc
     * and fly the rest of the way flat. The test asserts both halves: the real angle noses over, and {@code xRot}
     * does not, which is why the real angle needs its own field.
     */
    public static void missilePitchNosesOver(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Well clear of the test plots and well above the ground: a missile spawned on the floor
        // impacts on its first step and never gets an arc at all.
        // Negative and far: the pre-26.1 framework's grid grows in +x/+z for thousands of blocks.
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(-20_000, 120, -20_000);

        // Hold the flight corridor as ticking chunks: a chunk that is merely loaded unloads its entities
        // on the next tick below 26.1, and an unloaded missile reads as removed after two steps.
        int minChunkX = (origin.getX() - 16) >> 4, maxChunkX = (origin.getX() + 96) >> 4;
        int minChunkZ = (origin.getZ() - 16) >> 4, maxChunkZ = (origin.getZ() + 16) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) level.setChunkForced(cx, cz, true);
        }
        EntityMissileTier1.EntityMissileGeneric missile = new EntityMissileTier1.EntityMissileGeneric(
                level, origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 60, origin.getZ());
        level.addFreshEntity(missile);

        float steepest = 0F;
        int ticks = 0;
        // Long enough for the arc to pass its apex and dive; the missile is removed on impact, and a
        // missile that hit something never got to nose over, so that is a failure too.
        for (int i = 0; i < 400 && !missile.isRemoved(); i++, ticks++) {
            // The level's entity tick does this before calling tick(), and the missile's speed ramp
            // reads it: drive the entity by hand without it and velocity stays 0 forever, so the
            // missile hangs at its launch attitude and this test measures nothing.
            missile.tickCount++;
            missile.tick();
            steepest = Math.min(steepest, missile.getEntityData().get(EntityMissileBaseNT.PITCH));
        }

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) level.setChunkForced(cx, cz, false);
        }
        helper.assertTrue(steepest < -90F,
                "missile pitch never passed horizontal on the way down (steepest was " + steepest + " after " + ticks
                        + " ticks, removed=" + missile.isRemoved() + " at " + missile.position() + " motion "
                        + missile.getDeltaMovement() + " chunkLoaded=" + level.hasChunkAt(missile.blockPosition()) + ")");
        // PITCH exists because the vanilla field cannot hold this value, but that is only true where
        // setXRot clamps, which is 26.1 and up. Below it xRot legitimately carries the same number,
        // and asserting the clamp there would be asserting something vanilla never promised.
        if (Entities.clampsXRot()) {
            helper.assertTrue(missile.getXRot() >= -90F,
                    "xRot held a value outside vanilla's clamp, so this test is no longer testing anything");
        }

        missile.discard();
        helper.succeed();
    }

    // Missiles

    /** Every launchable missile item must have an entity, and every entity a factory that runs. */
    public static void launchTableResolves(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();

        helper.assertTrue(!TileEntityLaunchPadBase.missiles.isEmpty(),
                "the missile launch table is empty: registerLaunchables() did not run");
        helper.assertTrue(TileEntityLaunchPadBase.missiles.containsKey(ModMissileItems.missile_generic.get()),
                "the HE missile is missing from the launch table");
        helper.assertTrue(TileEntityLaunchPadBase.missiles.containsKey(ModMissileItems.missile_micro.get()),
                "the micro missile is missing from the launch table");
        helper.assertTrue(TileEntityLaunchPadBase.missiles.containsKey(ModMissileItems.missile_taint.get()),
                "the taint missile is missing from the launch table");
        helper.assertTrue(TileEntityLaunchPadBase.missiles.containsKey(ModMissileItems.missile_emp_strong.get()),
                "the strong EMP missile is missing from the launch table");
        helper.assertTrue(ModMissileEntities.MISSILE_NUCLEAR.get() != null,
                "the nuclear missile entity type did not register");
        helper.assertTrue(ModMissileEntities.MISSILE_MICRO.get() != null
                        && ModMissileEntities.MISSILE_TAINT.get() != null
                        && ModMissileEntities.MISSILE_EMP_STRONG.get() != null,
                "one of the payload missile entity types did not register");
        helper.succeed();
    }

    /** The micro keeps the old high preset, including its 35-block Fat Man radius and mk5 hand-off. */
    public static void microPayloadPath(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertFalse(ExplosionNukeSmall.PARAMS_HIGH.miniNuke,
                "the micro preset must use the mk5 path, not the mini-nuke path");
        helper.assertTrue(ExplosionNukeSmall.PARAMS_HIGH.blastRadius == BombConfig.fatmanRadius
                        && BombConfig.fatmanRadius == 35,
                "the micro preset no longer uses the old 35-block Fat Man radius");

        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        EntityMissileTier0.EntityMissileMicro missile = new EntityMissileTier0.EntityMissileMicro(
                level, origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 1, origin.getZ());
        try {
            missile.onMissileImpact(null);

            List<EntityNukeExplosionMK5> blasts = level.getEntitiesOfClass(EntityNukeExplosionMK5.class,
                    new AABB(origin).inflate(4));
            helper.assertTrue(!blasts.isEmpty(), "the micro did not hand off to an mk5 crater entity");
            helper.succeed();
        } finally {
            level.getEntitiesOfClass(EntityNukeExplosionMK5.class, new AABB(origin).inflate(4))
                    .forEach(Entity::discard);
            missile.discard();
        }
    }

    /** The taint block keeps its sixteen ages and old shallow collision box. */
    public static void taintBlockContract(GameTestHelper helper) {
        BlockTaint taint = ModBombBlocks.taint.get();
        BlockState state = taint.defaultBlockState().setValue(BlockTaint.TAINTAGE, 7);
        helper.assertTrue(state.getValue(BlockTaint.TAINTAGE) == 7, "taint age property was not registered");
        helper.assertTrue(taint.getCollisionShape(state, null, BlockPos.ZERO,
                        CollisionContext.empty())
                        .max(Direction.Axis.Y) == 0.75D,
                "taint collision height changed from the old 0.75-block box");
        helper.succeed();
    }

    /** Strong EMP uses the reusable field and drains an HBM-energy receiver on its second tick. */
    public static void empFieldDrainsHbmEnergy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos batteryRelative = new BlockPos(2, 1, 2);
        BlockPos batteryPos = helper.absolutePos(batteryRelative);
        helper.setBlock(batteryRelative, ModMachineBlocks.machine_battery.get().defaultBlockState());
        TileEntityMachineBattery battery = GameTestHelpers.blockEntity(
                helper, batteryRelative, TileEntityMachineBattery.class);
        helper.assertTrue(battery != null, "the EMP test battery did not form");
        battery.power = 1234;

        EntityEMP emp = new EntityEMP(level);
        emp.setPos(batteryPos.getX() + 0.5D, batteryPos.getY(), batteryPos.getZ() + 0.5D);
        level.addFreshEntity(emp);
        emp.tick();
        emp.tick();
        helper.assertTrue(battery.power == 0, "strong EMP did not clear HBM energy");

        int cx = batteryPos.getX() >> 4;
        int cz = batteryPos.getZ() >> 4;
        helper.assertTrue(ChunkLoadingManager.isForced(level, cx - 1, cz)
                        && ChunkLoadingManager.isForced(level, cx + 1, cz),
                "strong EMP did not hold its old 3x3 chunk footprint");
        emp.discard();
        helper.assertFalse(ChunkLoadingManager.isForced(level, cx, cz),
                "removing strong EMP left its chunk claim behind");
        helper.succeed();
    }

    /**
     * The aim maths. A missile fired at a target 200 blocks away must derive {@code accelXZ = 1/d} and
     * {@code decelY = 2/d}, the two constants that make the arc land where it was pointed. Getting
     * these wrong produces a missile that flies beautifully and misses by miles.
     */
    public static void missileAimConstants(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        int targetX = origin.getX() + 200;
        int targetZ = origin.getZ();

        EntityMissileBaseNT missile = new EntityMissileTier1.EntityMissileGeneric(
                level, origin.getX(), origin.getY(), origin.getZ(), targetX, targetZ);

        helper.assertTrue(Math.abs(missile.accelXZ - 1D / 200D) < 1.0E-6D,
                "accelXZ is " + missile.accelXZ + ", expected 1/200");
        helper.assertTrue(Math.abs(missile.decelY - 2D / 200D) < 1.0E-6D,
                "decelY is " + missile.decelY + ", expected 2/200");
        helper.assertTrue(missile.getDeltaMovement().y == 2D,
                "a missile must leave the pad at motionY 2, was " + missile.getDeltaMovement().y);
        missile.discard();
        helper.succeed();
    }

    /** A pad conforms both tanks to the loaded missile's fuel pair. The player never picks them. */
    public static void padConformsTanks(GameTestHelper helper) {
        TileEntityLaunchPad pad = pad(helper);
        pad.inventory.setStackInSlot(0, new ItemStack(ModMissileItems.missile_generic.get()));
        pad.setFuel((ItemMissileStandard) ModMissileItems.missile_generic.get());

        helper.assertTrue(pad.tanks[0].getTankType() == Fluids.ETHANOL,
                "a V2 missile should conform tank 0 to ethanol, got " + pad.tanks[0].getTankType().getName());
        helper.assertTrue(pad.tanks[1].getTankType() == Fluids.PEROXIDE,
                "a V2 missile should conform tank 1 to peroxide, got " + pad.tanks[1].getTankType().getName());
        helper.succeed();
    }

    /** The launch gate: no power, no fuel, no launch, checked one condition at a time. */
    public static void padLaunchGate(GameTestHelper helper) {
        TileEntityLaunchPad pad = pad(helper);
        ItemStack missile = new ItemStack(ModMissileItems.missile_generic.get());
        pad.inventory.setStackInSlot(0, missile);
        pad.setFuel((ItemMissileStandard) missile.getItem());

        helper.assertFalse(pad.hasFuel(), "an empty pad reported itself fuelled");

        pad.power = TileEntityLaunchPadBase.launchCost;
        helper.assertFalse(pad.hasFuel(), "power alone counted as fuelled");

        int cap = ((ItemMissileStandard) missile.getItem()).fuelCap;
        pad.tanks[0].setFill(cap);
        pad.tanks[1].setFill(cap);
        helper.assertTrue(pad.hasFuel(), "a fully fuelled, powered pad still reports no fuel");

        pad.delay = 100;
        helper.assertFalse(pad.canLaunch(), "the pad launched while still reloading");
        pad.delay = 0;
        helper.assertTrue(pad.canLaunch(), "a ready pad refuses to launch");
        helper.succeed();
    }

    /**
     * The full launch, end to end: a fuelled pad with a designator fires, spends exactly one launch's
     * worth of power and fuel, and eats the missile item.
     */
    public static void padLaunches(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();
        TileEntityLaunchPad pad = pad(helper);

        ItemStack missile = new ItemStack(ModMissileItems.missile_generic.get());
        int cap = ((ItemMissileStandard) missile.getItem()).fuelCap;
        pad.inventory.setStackInSlot(0, missile);
        pad.setFuel((ItemMissileStandard) missile.getItem());
        pad.tanks[0].setFill(cap * 2);
        pad.tanks[1].setFill(cap * 2);
        pad.power = TileEntityLaunchPadBase.maxPower;

        ItemStack designator = new ItemStack(ModMissileItems.designator.get());
        ModDataComponents.DESIGNATOR_X.set(designator, helper.absolutePos(BlockPos.ZERO).getX() + 300);
        ModDataComponents.DESIGNATOR_Z.set(designator, helper.absolutePos(BlockPos.ZERO).getZ());
        pad.inventory.setStackInSlot(1, designator);

        helper.assertTrue(pad.launchFromDesignator().wasSuccessful(), "the pad refused to launch");

        helper.assertTrue(pad.power == TileEntityLaunchPadBase.maxPower - TileEntityLaunchPadBase.launchCost,
                "the launch did not cost exactly 75 kHE, power is " + pad.power);
        helper.assertTrue(pad.tanks[0].getFill() == cap, "the launch did not spend one fuelCap of fuel");
        helper.assertTrue(pad.tanks[1].getFill() == cap, "the launch did not spend one fuelCap of oxidiser");
        helper.assertTrue(pad.inventory.getStackInSlot(0).isEmpty(), "the missile item survived the launch");

        List<EntityMissileBaseNT> flying = helper.getLevel().getEntitiesOfClass(
                EntityMissileBaseNT.class, new AABB(pad.getBlockPos()).inflate(32));
        helper.assertTrue(!flying.isEmpty(), "no missile entity was spawned");
        flying.forEach(Entity::discard);
        helper.succeed();
    }

    /** A missile with no ported warhead sits in the pad and refuses to fly, rather than crashing. */
    public static void unlaunchableMissileIsRefused(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();
        TileEntityLaunchPad pad = pad(helper);

        ItemStack rusted = new ItemStack(ModMissileItems.missile_doomsday_rusted.get());
        pad.inventory.setStackInSlot(0, rusted);
        pad.power = TileEntityLaunchPadBase.maxPower;

        helper.assertFalse(pad.isMissileValid(), "the rusted doomsday missile reported itself launchable");
        helper.assertFalse(pad.launchFromDesignator().wasSuccessful(),
                "an unlaunchable missile was launched anyway");
        helper.succeed();
    }

    /** A designator records what it was pointed at, and reads it back as the target. */
    public static void designatorRecordsTarget(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModMissileItems.designator.get());
        ItemDesignator designator = (ItemDesignator) stack.getItem();
        BlockPos pad = helper.absolutePos(new BlockPos(1, 1, 1));

        helper.assertFalse(designator.isReady(helper.getLevel(), stack, pad),
                "a fresh designator claims to have a target");

        ModDataComponents.DESIGNATOR_X.set(stack, 1234);
        ModDataComponents.DESIGNATOR_Z.set(stack, -567);

        helper.assertTrue(designator.isReady(helper.getLevel(), stack, pad), "a set designator is not ready");
        helper.assertTrue(designator.getCoords(helper.getLevel(), stack, pad).x == 1234
                        && designator.getCoords(helper.getLevel(), stack, pad).z == -567,
                "the designator read back the wrong target");
        helper.succeed();
    }

    /**
     * The pad's power/fluid ports, through the real placement path.
     *
     * <p>Without {@link com.hbm.api.energymk2.IEnergyConnectorBlock} the block entity still subscribes to the
     * network, so nothing fails, but a cable asks the neighbouring <em>block</em> whether that face is a port and
     * refuses to attach when the pad has no answer. Upstream gets this from the {@code TileEntityProxyCombo} on its
     * corner cells.
     *
     * <p>What is pinned: the 3x3 fills, the four corners are flagged extra, every one of the eight
     * {@code getConPos} entries is a live port on the cell that faces it, and a cell face that is not a
     * port refuses, including facing back into the machine.
     */
    public static void padPorts(GameTestHelper helper) {
        LaunchPad block = (LaunchPad) ModMachineBlocks.launch_pad.get();
        BlockPos clicked = new BlockPos(3, 1, 2);
        // setPlacedBy resolves facing from the placer's yaw (0 -> NORTH) and puts the core `offset`
        // blocks along it: NORTH with offset 1 -> one block +Z from the clicked cell.
        BlockPos core = clicked.offset(0, 0, 1);

        Player player =
                GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new ItemStack(block));

        TileEntityLaunchPad pad = GameTestHelpers.blockEntity(helper, core, TileEntityLaunchPad.class);
        helper.assertTrue(pad != null, "the launch pad core did not form");

        int cells = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (helper.getBlockState(core.offset(dx, 0, dz)).getBlock() == block) cells++;
            }
        }
        helper.assertTrue(cells == 9, "the pad footprint must fill 3x3 cells, found " + cells);

        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                FoldedTestSupport.assertPort(helper, core, core.offset(dx, 0, dz));
            }
        }

        // Every declared port must answer on the cell that faces it. The connections are in absolute
        // coordinates, so the cell is the port position stepped back along its own direction.
        for (DirPos dp : PortDef.connections(pad.getPorts())) {
            BlockPos cell = dp.getPos().offset(-dp.getDir().offsetX, -dp.getDir().offsetY, -dp.getDir().offsetZ);
            helper.assertTrue(block.canConnect(helper.getLevel(), cell, dp.getDir()),
                    "no power port at " + dp.getPos() + " facing " + dp.getDir());
            helper.assertTrue(block.canConnect(Fluids.KEROSENE, helper.getLevel(),
                            cell.getX(), cell.getY(), cell.getZ(), dp.getDir()),
                    "no fluid port at " + dp.getPos() + " facing " + dp.getDir());
        }

        // A face that is not a port refuses: the mid-edge cell, and any cell looking inward.
        BlockPos eastEdge = helper.absolutePos(core.offset(1, 0, 0));
        helper.assertFalse(block.canConnect(helper.getLevel(), eastEdge, ForgeDirection.EAST),
                "the middle of an edge is not a port, but it accepted a connection");
        BlockPos corner = helper.absolutePos(core.offset(1, 0, 1));
        helper.assertFalse(block.canConnect(helper.getLevel(), corner, ForgeDirection.WEST),
                "a corner cell accepted a connection pointing back into the pad");
        helper.succeed();
    }

    /**
     * The redstone launch, end to end and in the world: the way a player actually fires a pad.
     *
     * <p>Unlike {@link #padLaunches}, which calls {@code launchFromDesignator} directly and cannot see how the
     * signal reaches it, this places a real pad, fuels it, arms it, drops a redstone block against one of its cells
     * and ticks.
     */
    public static void padRedstoneLaunch(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();

        LaunchPad block = (LaunchPad) ModMachineBlocks.launch_pad.get();
        BlockPos clicked = new BlockPos(3, 1, 2);
        BlockPos core = clicked.offset(0, 0, 1);

        Player player =
                GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new ItemStack(block));

        TileEntityLaunchPad pad = GameTestHelpers.blockEntity(helper, core, TileEntityLaunchPad.class);
        helper.assertTrue(pad != null, "the launch pad core did not form");

        ItemStack missile = new ItemStack(ModMissileItems.missile_generic.get());
        int cap = ((ItemMissileStandard) missile.getItem()).fuelCap;
        pad.inventory.setStackInSlot(0, missile);
        pad.setFuel((ItemMissileStandard) missile.getItem());
        pad.tanks[0].setFill(cap);
        pad.tanks[1].setFill(cap);
        pad.power = TileEntityLaunchPadBase.maxPower;
        pad.delay = 0;

        ItemStack designator = new ItemStack(ModMissileItems.designator.get());
        ModDataComponents.DESIGNATOR_X.set(designator, helper.absolutePos(BlockPos.ZERO).getX() + 300);
        ModDataComponents.DESIGNATOR_Z.set(designator, helper.absolutePos(BlockPos.ZERO).getZ());
        pad.inventory.setStackInSlot(1, designator);

        // One tick to settle: this is what sets STATE_READY, i.e. what the player sees before flipping
        // the lever. It must not consume the launch.
        TileEntityLaunchPad.serverTick(helper.getLevel(), helper.absolutePos(core), pad.getBlockState(), pad);
        helper.assertTrue(pad.state == TileEntityLaunchPadBase.STATE_READY,
                "the pad did not reach STATE_READY with a fuelled missile and full power");

        // A redstone block against the pad's east edge cell: vanilla notifies that cell, which is where
        // the block hook has to pick the signal up and hand it to the core.
        helper.setBlock(core.offset(2, 0, 0), Blocks.REDSTONE_BLOCK.defaultBlockState());

        helper.assertTrue(pad.redstonePower > 0,
                "the pad never saw the redstone signal (redstonePower is " + pad.redstonePower + ")");

        TileEntityLaunchPad.serverTick(helper.getLevel(), helper.absolutePos(core), pad.getBlockState(), pad);

        List<EntityMissileBaseNT> flying = helper.getLevel().getEntitiesOfClass(
                EntityMissileBaseNT.class, new AABB(helper.absolutePos(core)).inflate(48));
        helper.assertTrue(!flying.isEmpty(), "the redstone edge did not launch the missile");
        flying.forEach(Entity::discard);
        helper.succeed();
    }

    /**
     * The whole player-facing path, gate by gate: aim a designator by right-clicking a block, put it and
     * a missile into the pad <em>through the menu slots</em>, fuel it, and flip redstone.
     *
     * <p>{@link #padRedstoneLaunch} places the items directly into the inventory and cannot see a rejecting slot;
     * {@link #designatorRecordsTarget} writes the components directly and cannot see a broken {@code useOn}.
     */
    public static void padPlayerPath(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();

        LaunchPad block = (LaunchPad) ModMachineBlocks.launch_pad.get();
        BlockPos clicked = new BlockPos(3, 1, 2);
        BlockPos core = clicked.offset(0, 0, 1);

        Player player =
                GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new ItemStack(block));
        TileEntityLaunchPad pad = GameTestHelpers.blockEntity(helper, core, TileEntityLaunchPad.class);
        helper.assertTrue(pad != null, "the launch pad core did not form");

        // 1. Aim the designator the way a player does: right-click a block with it.
        ItemStack designator = new ItemStack(ModMissileItems.designator.get());
        BlockPos target = helper.absolutePos(new BlockPos(0, 1, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, designator);
        designator.getItem().useOn(new UseOnContext(
                helper.getLevel(), player, InteractionHand.MAIN_HAND, designator,
                new BlockHitResult(
                        Vec3.atCenterOf(target), Direction.UP,
                        target, false)));
        helper.assertTrue(ModDataComponents.DESIGNATOR_X.has(designator),
                "right-clicking a block with the designator did not record a target");

        // 2. Load the pad through the MENU, which is the only route a player has.
        ContainerLaunchPad menu = new ContainerLaunchPad(1, player.getInventory(), pad);
        ItemStack missile = new ItemStack(ModMissileItems.missile_generic.get());

        helper.assertTrue(menu.getSlot(0).mayPlace(missile), "the missile slot refused a launchable missile");
        helper.assertTrue(menu.getSlot(1).mayPlace(designator), "the designator slot refused a designator");
        menu.getSlot(0).set(missile);
        menu.getSlot(1).set(designator);
        helper.assertTrue(!pad.inventory.getStackInSlot(0).isEmpty(),
                "the missile did not reach the block entity through the menu slot");
        helper.assertTrue(!pad.inventory.getStackInSlot(1).isEmpty(),
                "the designator did not reach the block entity through the menu slot");

        // 3. Fuel and power it.
        int cap = ((ItemMissileStandard) missile.getItem()).fuelCap;
        pad.setFuel((ItemMissileStandard) missile.getItem());
        pad.tanks[0].setFill(cap);
        pad.tanks[1].setFill(cap);
        pad.power = TileEntityLaunchPadBase.maxPower;

        TileEntityLaunchPad.serverTick(helper.getLevel(), helper.absolutePos(core), pad.getBlockState(), pad);
        helper.assertTrue(pad.state == TileEntityLaunchPadBase.STATE_READY, "the pad did not reach STATE_READY");
        helper.assertTrue(pad.canLaunch(), "the pad reports READY but canLaunch() is false");

        // 4. Redstone.
        helper.setBlock(core.offset(2, 0, 0), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.assertTrue(pad.redstonePower > 0, "the pad never saw the redstone signal");
        TileEntityLaunchPad.serverTick(helper.getLevel(), helper.absolutePos(core), pad.getBlockState(), pad);

        List<EntityMissileBaseNT> flying = helper.getLevel().getEntitiesOfClass(
                EntityMissileBaseNT.class, new AABB(helper.absolutePos(core)).inflate(48));
        helper.assertTrue(!flying.isEmpty(), "the full player path did not launch a missile");
        flying.forEach(Entity::discard);
        helper.succeed();
    }

    /**
     * The detonator: bind a pad, then fire it by hand.
     *
     * <p>The manual launch path; redstone is covered separately.
     */
    public static void detonatorLaunch(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();

        LaunchPad block = (LaunchPad) ModMachineBlocks.launch_pad.get();
        BlockPos clicked = new BlockPos(3, 1, 2);
        BlockPos core = clicked.offset(0, 0, 1);

        Player player =
                GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new ItemStack(block));
        TileEntityLaunchPad pad = GameTestHelpers.blockEntity(helper, core, TileEntityLaunchPad.class);

        ItemStack missile = new ItemStack(ModMissileItems.missile_generic.get());
        int cap = ((ItemMissileStandard) missile.getItem()).fuelCap;
        pad.inventory.setStackInSlot(0, missile);
        pad.setFuel((ItemMissileStandard) missile.getItem());
        pad.tanks[0].setFill(cap);
        pad.tanks[1].setFill(cap);
        pad.power = TileEntityLaunchPadBase.maxPower;

        ItemStack designator = new ItemStack(ModMissileItems.designator.get());
        ModDataComponents.DESIGNATOR_X.set(designator, helper.absolutePos(BlockPos.ZERO).getX() + 300);
        ModDataComponents.DESIGNATOR_Z.set(designator, helper.absolutePos(BlockPos.ZERO).getZ());
        pad.inventory.setStackInSlot(1, designator);
        TileEntityLaunchPad.serverTick(helper.getLevel(), helper.absolutePos(core), pad.getBlockState(), pad);

        // Bind: shift right-click any cell of the pad.
        ItemStack det = new ItemStack(ModMissileItems.detonator.get());
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, det);
        BlockPos cell = helper.absolutePos(core.offset(1, 0, 0));
        det.getItem().useOn(new UseOnContext(
                helper.getLevel(), player, InteractionHand.MAIN_HAND, det,
                new BlockHitResult(
                        Vec3.atCenterOf(cell), Direction.UP, cell, false)));
        helper.assertTrue(ModDataComponents.DETONATOR_TARGET.get(det).isPresent(),
                "shift right-clicking the pad did not bind the detonator");

        // Fire: right-click in the air.
        player.setShiftKeyDown(false);
        det.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        List<EntityMissileBaseNT> flying = helper.getLevel().getEntitiesOfClass(
                EntityMissileBaseNT.class, new AABB(helper.absolutePos(core)).inflate(48));
        helper.assertTrue(!flying.isEmpty(), "the detonator did not launch the missile");
        flying.forEach(Entity::discard);
        helper.succeed();
    }

    /**
     * Every missile the 1.7.10 pad accepts is accepted here, including the ones refused for want of a
     * warhead (schrabidium, black hole, EMP, cluster, strong cluster, rain, drill, volcano, shuttle) - and the ABM,
     * which is built directly and never appears in the launch table. The retired endo/exo/carrier items do not
     * exist, and the rusted doomsday missile stays refused as upstream.
     */
    public static void formerlyBlockedMissilesAccepted(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();
        TileEntityLaunchPad pad = pad(helper);

        for (Item item : formerlyBlocked().keySet()) {
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(pad.isMissileValid(stack), "the pad refuses " + item);
            helper.assertTrue(pad.isItemValidForSlot(0, stack), "the missile slot refuses " + item);
        }
        helper.assertTrue(pad.isMissileValid(new ItemStack(ModMissileItems.missile_generic.get())),
                "a launchable missile was refused");
        helper.assertTrue(pad.isMissileValid(new ItemStack(ModMissileItems.missile_anti_ballistic.get())),
                "the ABM was refused - it is instantiated directly, not through the launch table");
        helper.assertFalse(pad.isMissileValid(new ItemStack(ModMissileItems.missile_doomsday_rusted.get())),
                "the rusted doomsday missile was accepted");
        helper.succeed();
    }

    /** The missiles the 1.7 pad refused, and the entity each must become (built lazily: items are registry objects). */
    private static Map<Item, Class<? extends EntityMissileBaseNT>> formerlyBlocked() {
        Map<Item, Class<? extends EntityMissileBaseNT>> map =
                new LinkedHashMap<>();
        map.put(ModMissileItems.missile_schrabidium.get(), EntityMissileTier0.EntityMissileSchrabidium.class);
        map.put(ModMissileItems.missile_bhole.get(), EntityMissileTier0.EntityMissileBHole.class);
        map.put(ModMissileItems.missile_emp.get(), EntityMissileTier0.EntityMissileEMP.class);
        map.put(ModMissileItems.missile_cluster.get(), EntityMissileTier1.EntityMissileCluster.class);
        map.put(ModMissileItems.missile_cluster_strong.get(),
                EntityMissileTier2.EntityMissileClusterStrong.class);
        map.put(ModMissileItems.missile_rain.get(), EntityMissileTier3.EntityMissileRain.class);
        map.put(ModMissileItems.missile_drill.get(), EntityMissileTier3.EntityMissileDrill.class);
        map.put(ModMissileItems.missile_volcano.get(),
                EntityMissileTier4.EntityMissileVolcano.class);
        map.put(ModMissileItems.missile_shuttle.get(), EntityMissileShuttle.class);
        return map;
    }

    /**
     * Each of those nine launches from a fuelled pad and spawns exactly its own entity class. A fresh pad per missile,
     * and the entity is discarded before the tick ends, so nothing flies or impacts.
     */
    public static void formerlyBlockedMissilesLaunch(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();
        BlockPos base = helper.absolutePos(BlockPos.ZERO);

        for (Map.Entry<Item, Class<? extends EntityMissileBaseNT>> entry
                : formerlyBlocked().entrySet()) {
            Item item = entry.getKey();
            TileEntityLaunchPad pad = pad(helper);
            ItemMissileStandard standard = (ItemMissileStandard) item;
            pad.inventory.setStackInSlot(0, new ItemStack(item));
            pad.setFuel(standard);
            pad.tanks[0].setFill(standard.fuelCap * 2);
            pad.tanks[1].setFill(standard.fuelCap * 2);
            pad.power = TileEntityLaunchPadBase.maxPower;

            ItemStack designator = new ItemStack(ModMissileItems.designator.get());
            ModDataComponents.DESIGNATOR_X.set(designator, base.getX() + 300);
            ModDataComponents.DESIGNATOR_Z.set(designator, base.getZ());
            pad.inventory.setStackInSlot(1, designator);

            helper.assertTrue(pad.launchFromDesignator().wasSuccessful(), "the pad refused to launch " + item);

            List<EntityMissileBaseNT> flying = helper.getLevel().getEntitiesOfClass(
                    EntityMissileBaseNT.class, new AABB(pad.getBlockPos()).inflate(8));
            try {
                helper.assertTrue(flying.size() == 1 && flying.get(0).getClass() == entry.getValue(),
                        item + " launched " + flying + ", expected exactly one " + entry.getValue().getSimpleName());
            } finally {
                flying.forEach(Entity::discard);
            }
        }
        helper.succeed();
    }

    /** 1.7 rare drops: the EMP missile returns the EMP bomb block (not the empty stack), as in 1.7. */
    public static void missileRareDrops(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        EntityMissileTier0.EntityMissileEMP emp = new EntityMissileTier0.EntityMissileEMP(helper.getLevel(),
                origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 1, origin.getZ());
        helper.assertTrue(emp.getDebrisRareDrop().getItem() == ModBombs2Blocks.emp_bomb.get().asItem(),
                "the EMP missile's rare drop is not the EMP bomb: " + emp.getDebrisRareDrop());
        emp.discard();
        helper.succeed();
    }

    /**
     * Free space (blocks, every side) the two strength-10 blast tests own. A vanilla strength-10 explosion breaks
     * blocks up to about 17 blocks out and hurts entities within {@code 2 * strength} = 20, as does
     * {@code ExplosionNT}'s {@code EntityProcessorStandard}. 24 keeps both inside the test's own plot, so a
     * neighbouring test is never hit.
     */
    static final int BLAST_PADDING = 24;

    /**
     * Discards what a blast test created inside its padded plot: drops, rubble, shrapnel and falling gravel. Only
     * those types are removed, so a neighbour's mobs are untouched even if the box ever overlapped one.
     */
    private static void discardBlastDebris(ServerLevel level, BlockPos centre) {
        level.getEntitiesOfClass(Entity.class, new AABB(centre).inflate(BLAST_PADDING),
                e -> e instanceof ItemEntity
                        || e instanceof FallingBlockEntity
                        || e instanceof EntityRubble
                        || e instanceof EntityShrapnel)
                .forEach(Entity::discard);
    }

    /**
     * 1.7 EntityMissileVolcano: a 3x3x3 of volcanic lava with the volcano core at the centre. The blast itself also
     * runs (strength 10, plus rubble and shrapnel), so the cube is checked right after impact; everything is cleared
     * in the same tick so the freshly placed core never gets to erupt in the test world, and the debris entities
     * are discarded before they can fly. The test sits at the centre of a {@link #BLAST_PADDING} plot.
     */
    public static void volcanoMissilePlacesCore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(GameTests.paddedCentre(BLAST_PADDING, 2));
        EntityMissileTier4.EntityMissileVolcano missile =
                new EntityMissileTier4.EntityMissileVolcano(level,
                        origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 1, origin.getZ());
        try {
            missile.onMissileImpact(null);

            Block lava = ModFluidNetBlocks.volcanic_lava_block.get();
            Block core = ModTerrainBlocks.volcanoCore("volcano_core");
            int lavaCount = 0;
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                BlockPos at = origin.offset(x, y, z);
                Block block = level.getBlockState(at).getBlock();
                if (x == 0 && y == 0 && z == 0) {
                    helper.assertTrue(block == core, "the centre is " + block + ", expected the volcano core");
                } else if (block == lava) {
                    lavaCount++;
                }
            }
            helper.assertTrue(lavaCount == 26, "the 3x3x3 holds " + lavaCount + " volcanic lava blocks, expected 26");
            helper.succeed();
        } finally {
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
            missile.discard();
            discardBlastDebris(level, origin);
        }
    }

    /**
     * 1.7 ExplosionNT ERRODE: a size-10 blast cannot break concrete (resistance 140), but an eroding one still
     * affects the concrete its rays stop in - 60% of it crumbles into gravel, the rest is cleared. A cell of air
     * inside a concrete cube is the positive signal: without ERRODE the cube would survive untouched.
     *
     * <p>What 1.7's code determines exactly: every ray starts in the air cell, steps 0.3 and loses
     * {@code (140 + 0.3) * 0.3} in the first concrete cell it samples, so it dies there - and that first cell is always
     * one of the 26 neighbours (a 0.3 step cannot jump the 1-block inner shell). ExplosionNT:117 adds that cell either
     * way, and the 16-ray fan reaches all 26 (faces, edges and corners). So the inner 26 cells all become gravel or
     * air and the 98 outer-shell cells stay concrete. Only the gravel/air split is random: Binomial(26, 0.6).
     */
    public static void erodingBlastTurnsConcreteToGravel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(GameTests.paddedCentre(BLAST_PADDING, 4));
        Block concrete = ModBlocks.concrete.get();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            level.setBlock(centre.offset(x, y, z), (x == 0 && y == 0 && z == 0) ? air : concrete.defaultBlockState(), 2);
        }
        try {
            ExplosionNT.explodeErode(level, null, centre.getX() + 0.5D, centre.getY() + 0.5D,
                    centre.getZ() + 0.5D, 10F);

            int innerGravel = 0;
            int innerAir = 0;
            int concreteLeft = 0;
            for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                BlockState state = level.getBlockState(centre.offset(x, y, z));
                boolean inner = Math.abs(x) <= 1 && Math.abs(y) <= 1 && Math.abs(z) <= 1;
                if (state.is(concrete)) concreteLeft++;
                else if (inner && state.is(Blocks.GRAVEL)) innerGravel++;
                else if (inner && state.isAir()) innerAir++;
            }
            helper.assertTrue(concreteLeft == 98,
                    "expected exactly the 98 outer-shell concrete cells to survive, found " + concreteLeft);
            helper.assertTrue(innerGravel + innerAir == 26, "every inner cell must be eroded or cleared: "
                    + innerGravel + " gravel + " + innerAir + " air of 26");
            // Binomial(26, 0.6): P(< 5) ~ 1e-6, P(26) ~ 2e-6
            helper.assertTrue(innerGravel >= 5 && innerGravel <= 25,
                    "the 60% erosion roll gave " + innerGravel + " gravel of 26, outside 5..25");
            helper.succeed();
        } finally {
            for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
                level.setBlock(centre.offset(x, y, z), air, 2);
            }
            discardBlastDebris(level, centre);
        }
    }

    /**
     * The pad tells the client which missile it is holding.
     *
     * <p>The pad's renderer draws that missile standing on the silo, and it can only know about it from
     * this field: the client never sees a block entity's inventory. If the sync breaks, every pad in the
     * world silently renders empty, and no server-side test would notice.
     */
    public static void padSyncsLoadedMissile(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();
        TileEntityLaunchPad pad = pad(helper);
        pad.inventory.setStackInSlot(0, new ItemStack(ModMissileItems.missile_nuclear.get()));

        TileEntityLaunchPad copy = pad(helper);
        roundTrip(helper, pad, copy);
        helper.assertTrue(copy.toRender != null && copy.toRender.getItem() == ModMissileItems.missile_nuclear.get(),
                "the loaded missile did not reach the client: toRender is "
                        + (copy.toRender == null ? "null" : copy.toRender.getItem().toString()));

        pad.inventory.setStackInSlot(0, ItemStack.EMPTY);
        roundTrip(helper, pad, copy);
        helper.assertTrue(copy.toRender == null, "an emptied pad still reports a missile to the client");

        // The other source: the chunk-load tag. networkPackNT only sends on CHANGE, so a player logging
        // in beside an idle loaded pad gets no pack at all and the renderer would have nothing. The
        // load path derives it from slot 0 instead. (This exercises the derivation, not the transport,
        // the tag plumbing itself is vanilla's and differs by era.)
        TileEntityLaunchPad loaded = pad(helper);
        loaded.inventory.setStackInSlot(0, new ItemStack(ModMissileItems.missile_strong.get()));
        loaded.toRender = null;
        loaded.refreshRenderMissile();
        helper.assertTrue(loaded.toRender != null && loaded.toRender.getItem() == ModMissileItems.missile_strong.get(),
                "a pad loaded from disk did not derive its render missile from slot 0");
        helper.succeed();
    }

    /** Runs one network pack from {@code from} into {@code to}, as the real sync does. */
    private static void roundTrip(GameTestHelper helper, TileEntityLaunchPad from, TileEntityLaunchPad to) {
        // GolemBuffers.wrap, not a direct constructor: the registry-aware buffer type only exists from
        // 1.20.5, and below that the plain RegistryFriendlyByteBuf takes no registry argument.
        var buf = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        from.serialize(buf);
        to.deserialize(buf);
    }

    /**
     * Exactly which missiles a pad accepts, pinned as a list because the two halves that decide it can drift apart
     * silently.
     *
     * <p>A missile is launchable when it has an entity in {@code TileEntityLaunchPadBase.missiles} (or is
     * the ABM, built directly). Registering an entity type in {@code ModMissileEntities} without adding
     * it here leaves a missile that flies in code and is refused in game; the reverse crashes on launch.
     * Update this list when a warhead's subsystem lands.
     */
    /**
     * The stealth missile's assembler recipe, 1.7 {@code ass.stealthmissile}: 1200 ticks at 100 HE/t, seven
     * ingredients, one missile. The generated recipe file has no entry for it, so without the curated recipe the
     * missile was creative-only. TNT and PVC stand in for the high-explosive and hard-plastic ore keys.
     */
    public static void stealthRecipe(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        GenericRecipe recipe =
                AssemblyMachineRecipes.INSTANCE.getRecipe("ass.stealthmissile");
        helper.assertTrue(recipe != null && recipe.duration == 1_200 && recipe.power == 100,
                "stealth missile assembly recipe is missing or has wrong processing parameters");
        helper.assertTrue(recipe.outputItem != null && recipe.outputItem.length == 1
                        && recipe.outputItem[0].is(ModMissileItems.missile_stealth.get()),
                "stealth missile assembly recipe has the wrong output");
        helper.assertTrue(recipe.inputItem.length == 7, "stealth missile recipe needs seven ingredients");
        for (Item needed : new Item[]{
                ModItems.ball_tnt.get(), ModItems.ingot_pvc.get(),
                Items.BLACK_DYE}) {
            boolean used = false;
            for (var input : recipe.inputItem) used |= input.matchesRecipe(new ItemStack(needed, 64), true);
            helper.assertTrue(used, "stealth missile recipe does not accept " + needed);
        }
        helper.succeed();
    }

    public static void launchableSet(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();
        TileEntityLaunchPad pad = pad(helper);

        List<Item> launchable = List.of(
                ModMissileItems.missile_micro.get(), ModMissileItems.missile_taint.get(),
                ModMissileItems.missile_test.get(), ModMissileItems.missile_schrabidium.get(),
                ModMissileItems.missile_bhole.get(), ModMissileItems.missile_emp.get(),
                ModMissileItems.missile_generic.get(), ModMissileItems.missile_decoy.get(),
                ModMissileItems.missile_cluster.get(), ModMissileItems.missile_cluster_strong.get(),
                ModMissileItems.missile_rain.get(), ModMissileItems.missile_drill.get(),
                ModMissileItems.missile_volcano.get(), ModMissileItems.missile_shuttle.get(),
                ModMissileItems.missile_incendiary.get(), ModMissileItems.missile_buster.get(),
                ModMissileItems.missile_anti_ballistic.get(),
                ModMissileItems.missile_strong.get(), ModMissileItems.missile_incendiary_strong.get(),
                ModMissileItems.missile_buster_strong.get(), ModMissileItems.missile_emp_strong.get(),
                ModMissileItems.missile_stealth.get(),
                ModMissileItems.missile_burst.get(), ModMissileItems.missile_inferno.get(),
                ModMissileItems.missile_nuclear.get(), ModMissileItems.missile_nuclear_cluster.get(),
                ModMissileItems.missile_doomsday.get(), ModMissileItems.missile_n2.get());

        for (Item item : launchable) {
            helper.assertTrue(pad.isMissileValid(new ItemStack(item)),
                    "the pad refuses " + item + ", which is supposed to be launchable");
        }

        // The only refusal left is upstream's own: the rusted doomsday missile is notLaunchable.
        List<Item> refused = List.of(
                ModMissileItems.missile_doomsday_rusted.get());

        for (Item item : refused) {
            helper.assertFalse(pad.isMissileValid(new ItemStack(item)),
                    item + " is accepted by the pad but upstream refuses it");
        }

        helper.assertTrue(TileEntityLaunchPadBase.missiles.size() == launchable.size() - 1,
                "the launch table holds " + TileEntityLaunchPadBase.missiles.size()
                        + " entries; the expected launchable list (minus the ABM) has " + (launchable.size() - 1));
        helper.succeed();
    }

    /**
     * A launch pad block entity bound to the test level. It is constructed directly rather than placed:
     * the pad is a folded multiblock and building the 3x3 would test the placement code,
     * not the launch code. {@code setLevel} is what makes {@code finalizeLaunch} able to spawn.
     */
    private static TileEntityLaunchPad pad(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        TileEntityLaunchPad pad = new TileEntityLaunchPad(pos,
                ModMachineBlocks.launch_pad.get().defaultBlockState());
        pad.setLevel(helper.getLevel());
        return pad;
    }
}
