package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.generic.BlockSkeletonHolder;
import com.hbm.blocks.generic.CrateKeepContents;
import com.hbm.blocks.machine.MachineExplosionDamage;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.MobConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.config.ServerConfig;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityFBI;
import com.hbm.entity.mob.EntityFBIDrone;
import com.hbm.entity.mob.EntityGhost;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.potion.ModEffects;
import com.hbm.tileentity.bomb.TileEntityLandmine;
import com.hbm.tileentity.deco.TileEntityBobble;
import com.hbm.tileentity.deco.TileEntityLanternBehemoth;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.machine.storage.TileEntityCrate;
import com.hbm.tileentity.network.TileEntityPipeAnchor;
import com.hbm.world.gen.nbt.LegacyStructureTiles;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Effects;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contracts of gap batch B (§19ed-gaps-b), each against the 1.7 code it ports: BossSpawnHandler's raid, radiation
 * beast and ghost spawns and the FBI mark; the lantern reputation (repair, destroy, bonus) and its respawn copy; tank and
 * refinery explosion damage, leaking, repair and the napalm route to {@code inferno}; the {@code /ntmserver} keys; the
 * 1.7 tile-entity data of structure files; pipe anchor pipelines; and the saved-chunk height read the radar uses.
 */
public final class FinalGapsBGameTests {
    private FinalGapsBGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function BOSS_SPAWNS =
            TESTS.function("gapsb_boss_spawns", 60, 12, () -> logged(FinalGapsBGameTests::bossSpawns));
    public static final GameTests.Function REPUTATION =
            TESTS.function("gapsb_reputation", 40, 12, () -> FinalGapsBGameTests::reputation);
    public static final GameTests.Function TANK_EXPLOSION =
            TESTS.function("gapsb_tank_explosion", 40, 12, () -> FinalGapsBGameTests::tankExplosion);
    public static final GameTests.Function REFINERY_EXPLOSION =
            TESTS.function("gapsb_refinery_explosion", 40, 14, () -> FinalGapsBGameTests::refineryExplosion);
    public static final GameTests.Function SERVER_KEYS =
            TESTS.function("gapsb_server_keys", 600, 8, () -> FinalGapsBGameTests::serverKeys);
    public static final GameTests.Function STRUCTURE_TILES =
            TESTS.function("gapsb_structure_tiles", 40, 8, () -> FinalGapsBGameTests::structureTiles);
    public static final GameTests.Function PIPELINE =
            TESTS.function("gapsb_pipeline", 40, 16, () -> FinalGapsBGameTests::pipeline);

    public static void init() {}

    /** Logs the stack of an unexpected exception (the test reporter keeps only its message). */
    private static java.util.function.Consumer<GameTestHelper> logged(java.util.function.Consumer<GameTestHelper> test) {
        return helper -> {
            try {
                test.accept(helper);
            } catch (RuntimeException e) {
                Constants.LOG.error("[gapsb] unexpected exception", e);
                throw e;
            }
        };
    }

    /** A mock player with a mod-aware mock connection (hbm payloads may be sent to it), closed by {@link #close}. */
    private static final java.util.Map<ServerPlayer, GameTestHelpers.MockServerPlayer> MOCKS = new java.util.IdentityHashMap<>();

    private static ServerPlayer playerAt(GameTestHelper helper, BlockPos relative) {
        GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper);
        ServerPlayer player = mock.player();
        MOCKS.put(player, mock);
        BlockPos abs = helper.absolutePos(relative);
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        return player;
    }

    private static void close(ServerPlayer player) {
        GameTestHelpers.MockServerPlayer mock = MOCKS.remove(player);
        if (mock != null) mock.close();
        else player.discard();
    }

    /** 1.7 BossSpawnHandler: a raid of raidAmount agents + raidDrones drones, a pack of beasts with one leader, a ghost. */
    private static void bossSpawns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = playerAt(helper, new BlockPos(6, 2, 6));
        int distance = MobConfig.raidAttackDistance;
        MobConfig.raidAttackDistance = 0; // keep the spawns inside the test's loaded area
        try {
            BossSpawnHandler.markFBI(player);
            helper.assertTrue(HbmPlayerProps.getData(player).fbiMark == level.getGameTime() + 20 * 60 * 20,
                    "markFBI must hold the raid off for 20 minutes");

            AABB around = new AABB(player.blockPosition()).inflate(40);
            BossSpawnHandler.raid(level, player);
            int agents = level.getEntitiesOfClass(EntityFBI.class, around).size();
            int drones = level.getEntitiesOfClass(EntityFBIDrone.class, around).size();
            helper.assertTrue(agents == MobConfig.raidAmount && drones == MobConfig.raidDrones,
                    "a raid spawns " + MobConfig.raidAmount + " agents and " + MobConfig.raidDrones + " drones, got "
                            + agents + "/" + drones);

            BossSpawnHandler.elementals(level, player);
            List<EntityRADBeast> beasts = level.getEntitiesOfClass(EntityRADBeast.class, around);
            helper.assertTrue(beasts.size() == MobConfig.elementalAmount,
                    "the pack is elementalAmount beasts, got " + beasts.size());
            long leaders = beasts.stream().filter(b -> b.getMaxHealth() > 200).count();
            helper.assertTrue(leaders == 1, "exactly the first beast leads (tripled health), got " + leaders);

            // the ghost comes from 75 blocks out, usually an unloaded chunk in a test world (1.7 refused those);
            // the harness sees it land around a real player
            int ghostsBefore = ghosts(level);
            BossSpawnHandler.ghost(level, player);
            helper.assertTrue(ghosts(level) <= ghostsBefore + 1, "at most one ghost per roll");

            // the radiation beasts' mark: set by a meltdown, kept on respawn
            EntityHbmProps props = HbmPlayerProps.getData(player);
            props.radMark = true;
            EntityHbmProps copy = new EntityHbmProps();
            copy.copyPlayerPropsFrom(props);
            helper.assertTrue(copy.radMark && copy.fbiMark == props.fbiMark, "fbiMark and radMark survive the respawn copy");
        } finally {
            MobConfig.raidAttackDistance = distance;
            List<net.minecraft.world.entity.Entity> spawned = new java.util.ArrayList<>();
            for (var e : level.getAllEntities()) {
                if (e instanceof EntityFBI || e instanceof EntityFBIDrone || e instanceof EntityRADBeast || e instanceof EntityGhost) spawned.add(e);
            }
            spawned.forEach(net.minecraft.world.entity.Entity::discard);
            close(player);
        }
        helper.succeed();
    }

    private static int ghosts(ServerLevel level) {
        int n = 0;
        for (var e : level.getAllEntities()) if (e instanceof EntityGhost && e.isAlive()) n++;
        return n;
    }

    /** 1.7 lantern reputation: repair +1 (cap 25), destroyed lantern -1 for players within 50 (floor -25), bonus at 10. */
    private static void reputation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = playerAt(helper, new BlockPos(2, 2, 2));
        HbmPlayerProps.setReputation(player, 0);

        BlockPos rel = new BlockPos(5, 1, 5);
        var block = ModDungeonBlocks.lantern_behemoth.get();
        helper.setBlock(rel, block.defaultBlockState());
        block.fillSpace(level, helper.absolutePos(rel), Direction.NORTH);
        TileEntityLanternBehemoth lantern = GameTestHelpers.blockEntity(helper, rel, TileEntityLanternBehemoth.class);
        lantern.isBroken = true;
        giveRepairMaterials(player);
        boolean repaired = block.onScrew(level, player, helper.absolutePos(rel), Direction.UP,
                net.minecraft.world.phys.Vec3.ZERO, net.minecraft.world.InteractionHand.MAIN_HAND,
                com.hbm.api.block.IToolable.ToolType.TORCH);
        helper.assertTrue(repaired && !lantern.isBroken && lantern.comTimer == 400, "the blowtorch repairs the lantern");
        helper.assertTrue(HbmPlayerProps.getReputation(player) == 1, "a repair earns one reputation, got "
                + HbmPlayerProps.getReputation(player));

        HbmPlayerProps.setReputation(player, 25);
        lantern.isBroken = true;
        giveRepairMaterials(player);
        block.onScrew(level, player, helper.absolutePos(rel), Direction.UP, net.minecraft.world.phys.Vec3.ZERO,
                net.minecraft.world.InteractionHand.MAIN_HAND, com.hbm.api.block.IToolable.ToolType.TORCH);
        helper.assertTrue(HbmPlayerProps.getReputation(player) == 25, "reputation stops at 25");

        level.removeBlock(helper.absolutePos(rel), false);
        com.hbm.blocks.generic.BlockLanternBehemoth.settleRemovals(); // the end-of-tick settlement, run now
        helper.assertTrue(HbmPlayerProps.getReputation(player) == 24, "a destroyed lantern costs one reputation, got "
                + HbmPlayerProps.getReputation(player));

        // the respawn copy (1.7 onPlayerClone); the save is checked by the harness across a world reload
        EntityHbmProps respawned = new EntityHbmProps();
        respawned.copyPlayerPropsFrom(HbmPlayerProps.getData(player));
        helper.assertTrue(respawned.reputation == 24, "reputation survives respawn");
        close(player);
        helper.succeed();
    }

    /** The old lantern's repair: two steel plates and a basic circuit. */
    private static void giveRepairMaterials(Player player) {
        player.getInventory().add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                dev.golem.core.Ids.of("hbm", "plate_steel")), 2));
        player.getInventory().add(com.hbm.items.ModIntermediateItems.circuit(com.hbm.items.ItemEnums.EnumCircuitType.BASIC, 1));
    }

    private static TileEntityMachineFluidTank placeTank(GameTestHelper helper, BlockPos relative) {
        var block = ModMachineBlocks.machine_fluidtank.get();
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), helper.absolutePos(relative), Direction.NORTH);
        return GameTestHelpers.blockEntity(helper, relative, TileEntityMachineFluidTank.class);
    }

    /** 1.7 MachineFluidTank.onBlockExploded + TileEntityMachineFluidTank: explode, leak, burn, repair, the napalm route. */
    private static void tankExplosion(GameTestHelper helper) {
        Fluids.init();
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(5, 1, 5);
        TileEntityMachineFluidTank tank = placeTank(helper, rel);
        tank.tank.setTankType(Fluids.GASOLINE);
        tank.tank.setFill(100_000);
        ServerPlayer player = playerAt(helper, new BlockPos(1, 2, 1));

        EntityBombletZeta bomblet = Entities.createTriggered(ModNtmEntities.BOMBLET_ZETA.get(), level);
        Object blast = new Object();
        BlockPos cell = helper.absolutePos(rel.offset(1, 1, 0));
        helper.assertTrue(MachineExplosionDamage.hit(level, cell, blast, bomblet), "the tank owns the hit on its cell");
        helper.assertTrue(tank.hasExploded && tank.onFire, "a gasoline tank blows open and burns");
        helper.assertTrue(MachineExplosionDamage.hit(level, helper.absolutePos(rel), blast, bomblet) && tank.hasExploded
                        && level.getBlockState(helper.absolutePos(rel)).is(ModMachineBlocks.machine_fluidtank.get()),
                "the same explosion hits the tank only once");
        Boolean inferno = dev.golem.core.world.Advancements.isDone(player, "hbm:inferno");
        close(player);
        helper.assertTrue(inferno == null || inferno, "a napalm bomblet exploding a flammable tank grants inferno");

        TileEntityMachineFluidTank.serverTick(level, tank.getBlockPos(), tank.getBlockState(), tank);
        helper.assertTrue(tank.tank.getFill() == 100_000 - 256_000 / 10000, "a liquid leaks max/10000 a tick, fill "
                + tank.tank.getFill());
        helper.assertTrue(tank.getSendingTanks().length == 0 && tank.getReceivingTanks().length == 0,
                "a torn tank neither sends nor receives");
        helper.assertTrue(tank.getRepairMaterials().size() == 1 && tank.getRepairMaterials().get(0).stacksize == 6,
                "repair is six steel plates");
        tank.repair();
        helper.assertTrue(!tank.hasExploded, "repaired");

        // antimatter: blows the tank and is gone
        tank.tank.setTankType(Fluids.AMAT);
        tank.tank.setFill(1000);
        TileEntityMachineFluidTank.serverTick(level, tank.getBlockPos(), tank.getBlockState(), tank);
        helper.assertTrue(tank.hasExploded && tank.tank.getFill() == 0, "antimatter tears the tank open and is lost");

        // second, different explosion: the torn tank is removed
        helper.assertTrue(MachineExplosionDamage.hit(level, cell, new Object(), null), "second blast handled");
        helper.assertTrue(!level.getBlockState(helper.absolutePos(rel)).is(ModMachineBlocks.machine_fluidtank.get()),
                "a second blast removes a torn tank");
        helper.succeed();
    }

    /** 1.7 MachineRefinery.onBlockExploded: always burning, burns 10 mB a tick off each tank, eight plates + four tape. */
    private static void refineryExplosion(GameTestHelper helper) {
        Fluids.init();
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(5, 1, 5);
        var block = ModMachineBlocks.machine_refinery.get();
        helper.setBlock(rel, block.defaultBlockState());
        block.fillSpace(level, helper.absolutePos(rel), Direction.NORTH);
        TileEntityMachineRefinery refinery = GameTestHelpers.blockEntity(helper, rel, TileEntityMachineRefinery.class);
        refinery.tanks[0].setFill(500);
        ServerPlayer player = playerAt(helper, new BlockPos(1, 2, 1));

        EntityBombletZeta bomblet = Entities.createTriggered(ModNtmEntities.BOMBLET_ZETA.get(), level);
        helper.assertTrue(MachineExplosionDamage.hit(level, helper.absolutePos(rel.above(4)), new Object(), bomblet),
                "the refinery owns the hit on its tower");
        helper.assertTrue(refinery.hasExploded && refinery.onFire, "the refinery blows open and burns");
        Boolean inferno = dev.golem.core.world.Advancements.isDone(player, "hbm:inferno");
        close(player);
        helper.assertTrue(inferno == null || inferno, "a napalm bomblet exploding the refinery grants inferno");
        TileEntityMachineRefinery.serverTick(level, refinery.getBlockPos(), refinery.getBlockState(), refinery);
        helper.assertTrue(refinery.tanks[0].getFill() == 490, "a burning refinery loses 10 mB a tick per tank, got "
                + refinery.tanks[0].getFill());
        refinery.tryExtinguish(level, refinery.getBlockPos(), com.hbm.interfaces.IRepairable.EnumExtinguishType.FOAM);
        helper.assertTrue(!refinery.onFire, "foam puts it out");
        helper.assertTrue(refinery.getRepairMaterials().size() == 2, "eight steel plates and four duct tape");
        helper.succeed();
    }

    /** The /ntmserver keys at work where 1.7 read them. */
    private static void serverKeys(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean trails = ServerConfig.TAINT_TRAILS.get();
        boolean keep = ServerConfig.CRATE_KEEP_CONTENTS.get();
        boolean mku = ServerConfig.ENABLE_MKU.get();
        int tickrate = ServerConfig.ITEM_HAZARD_DROP_TICKRATE.get();
        try {
            // ITEM_HAZARD_DROP_TICKRATE is the dropped-item hazard cadence
            ServerConfig.ITEM_HAZARD_DROP_TICKRATE.set(7);
            helper.assertTrue(RadiationConfig.itemHazardDropTickRate == 7, "the tick rate key drives the hazard cadence");
            // restore within this tick: hbm:dropped_hazard_tick (same batch) reads the global cadence
            ServerConfig.ITEM_HAZARD_DROP_TICKRATE.set(tickrate);

            // ENABLE_MKU off: nobody is infected
            var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(2, 2, 2));
            HbmLivingProps.getData(zombie).setContagion(1000);
            ServerConfig.ENABLE_MKU.set(false);
            helper.assertTrue(HbmLivingProps.getContagion(zombie) == 0, "no MKU without ENABLE_MKU");
            ServerConfig.ENABLE_MKU.set(true);
            helper.assertTrue(HbmLivingProps.getContagion(zombie) == 1000, "MKU with ENABLE_MKU");

            // TAINT_TRAILS: the tainted leave taint in the block below
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(new BlockPos(4 + dx, 1, 4 + dz), Blocks.STONE.defaultBlockState());
            }
            var cow = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(4, 2, 4));
            ServerConfig.TAINT_TRAILS.set(true);
            Effects.addRegistered(cow, ModEffects.TAINT.get(), 40, 0);
            // the batch's explosion tests run beside this one and fling the cow off its stone: hold it in place, and
            // record how far it was moved, so the check below is about the trail and not about the neighbours.
            // The cow must actually tick for the effect to run, and on 26.1 nothing guarantees that here: the test
            // template is 26.1's 1x1x1 minecraft:empty with 8 blocks of padding, the runner forces (and waits for)
            // only the chunks of that one-block template, and the cow stands in the padding, 4 blocks out. Whether
            // that is the template's chunk depends on where the random grid start puts the chunk borders; in the
            // full suite it was a neighbour that was loaded but never entity-ticking (cow ticks 0, entity-ticking
            // false, not removed, after 190 ticks). So the test forces the cow's own chunk, polls until the cow has
            // had its ticks, and gives the chunk back only if it forced it (the forced set is one flag per chunk).
            BlockPos cowAt = helper.absolutePos(new BlockPos(4, 2, 4));
            int cowChunkX = cowAt.getX() >> 4, cowChunkZ = cowAt.getZ() >> 4;
            boolean forcedByTest = !dev.golem.core.world.Compat.isChunkForced(level, cowChunkX, cowChunkZ);
            if (forcedByTest) dev.golem.core.world.Compat.updateChunkForced(level, cowChunkX, cowChunkZ, true);
            double[] pushed = new double[1];
            boolean[] done = new boolean[1];
            // the effect acts every second tick of the cow's own, so 20 of them are ten chances; the test timeout
            // is only the bound for the chunk to become entity-ticking (GameTest ticks run unthrottled)
            final int cowTicks = 20;
            final int last = 590;
            for (int t = 1; t <= last; t++) {
                final int tick = t;
                helper.runAfterDelay(t, () -> {
                    if (done[0]) return;
                    pushed[0] = Math.max(pushed[0], cow.position().distanceTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(cowAt)));
                    cow.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    cow.snapTo(cowAt.getX() + 0.5, cowAt.getY(), cowAt.getZ() + 0.5, cow.getYRot(), cow.getXRot());
                    boolean tainted = false;
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                        tainted |= helper.getBlockState(new BlockPos(4 + dx, 1, 4 + dz)).is(ModBombBlocks.taint.get());
                    }
                    if (!tainted && cow.tickCount < cowTicks && tick < last) return;
                    done[0] = true;
                    if (forcedByTest) dev.golem.core.world.Compat.updateChunkForced(level, cowChunkX, cowChunkZ, false);
                    try {
                        helper.assertTrue(tainted,
                                "a tainted mob on stone leaves taint with TAINT_TRAILS (cow at " + cow.blockPosition() + ", effect "
                                        + dev.golem.core.world.Effects.hasRegistered(cow, ModEffects.TAINT.get()) + ", pushed up to "
                                        + pushed[0] + " blocks, key " + ServerConfig.TAINT_TRAILS.get() + ", below "
                                        + level.getBlockState(cowAt.below()) + ", cow ticks " + cow.tickCount + ", effect left "
                                        + (dev.golem.core.world.Effects.getRegistered(cow, ModEffects.TAINT.get()) == null ? -1
                                        : dev.golem.core.world.Effects.getRegistered(cow, ModEffects.TAINT.get()).getDuration())
                                        + ", removed " + cow.getRemovalReason() + ", in level " + (level.getEntity(cow.getUUID()) == cow)
                                        + ", entity-ticking " + level.isPositionEntityTicking(cowAt) + ", zombie ticks " + zombie.tickCount
                                        + " removed " + zombie.getRemovalReason() + ", after " + tick + " ticks)");

                        // CRATE_KEEP_CONTENTS off: the crate breaks like a chest
                        ServerConfig.CRATE_KEEP_CONTENTS.set(false);
                        helper.setBlock(new BlockPos(6, 1, 6), ModStorageBlocks.crate_iron.get().defaultBlockState());
                        TileEntityCrate crate = GameTestHelpers.blockEntity(helper, new BlockPos(6, 1, 6), TileEntityCrate.class);
                        crate.setItem(0, new ItemStack(Items.DIAMOND, 5));
                        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
                        BlockPos abs = helper.absolutePos(new BlockPos(6, 1, 6));
                        helper.assertTrue(CrateKeepContents.breakLoose(level, abs, helper.getBlockState(new BlockPos(6, 1, 6)), player),
                                "the key off breaks the crate here");
                        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2));
                        boolean diamonds = drops.stream().anyMatch(i -> i.getItem().is(Items.DIAMOND) && i.getItem().getCount() == 5);
                        boolean bare = drops.stream().anyMatch(i -> i.getItem().is(ModStorageBlocks.crate_iron.get().asItem())
                                && ItemStacks.carriedItems(i.getItem(), "inventory", 36).stream().allMatch(ItemStack::isEmpty));
                        helper.assertTrue(diamonds && bare && level.getBlockState(abs).isAir(),
                                "contents spill and the crate drops empty");
                    } finally {
                        // the keys are global: put them back whether this passed or failed, so no later test inherits them
                        ServerConfig.TAINT_TRAILS.set(trails);
                        ServerConfig.CRATE_KEEP_CONTENTS.set(keep);
                        ServerConfig.ENABLE_MKU.set(mku);
                        ServerConfig.ITEM_HAZARD_DROP_TICKRATE.set(tickrate);
                    }
                    helper.succeed();
                });
            }
        } catch (RuntimeException | AssertionError e) {
            ServerConfig.TAINT_TRAILS.set(trails);
            ServerConfig.CRATE_KEEP_CONTENTS.set(keep);
            ServerConfig.ENABLE_MKU.set(mku);
            ServerConfig.ITEM_HAZARD_DROP_TICKRATE.set(tickrate);
            throw e;
        }
    }

    /** 1.7 tile-entity NBT of structure files carried into port block entities. */
    private static void structureTiles(GameTestHelper helper) {
        Fluids.init();
        ServerLevel level = helper.getLevel();
        Map<Integer, String> palette = new HashMap<>();
        palette.put(5192, "hbm:item.launch_code_piece");
        palette.put(4000, "hbm:item.pipe");

        // armed landmine
        BlockPos mine = new BlockPos(1, 1, 1);
        helper.setBlock(mine, ModExplosivesBlocks.mine_ap.get().defaultBlockState());
        CompoundTag mineNbt = new CompoundTag();
        mineNbt.putString("id", "tileentity_landmine");
        mineNbt.putBoolean("primed", true);
        mineNbt.putBoolean("waiting", false);
        TileEntityLandmine landmine = GameTestHelpers.blockEntity(helper, mine, TileEntityLandmine.class);
        helper.assertTrue(LegacyStructureTiles.apply(level, helper.absolutePos(mine), landmine, mineNbt, palette, 0)
                && landmine.isPrimed(), "the 1.7 landmine arrives armed");

        // barrel fluid type (1.7 plastic barrel, tank_type 20)
        BlockPos barrelPos = new BlockPos(2, 1, 1);
        helper.setBlock(barrelPos, ModFluidNetBlocks.barrel_plastic.get().defaultBlockState());
        TileEntityBarrel barrel = GameTestHelpers.blockEntity(helper, barrelPos, TileEntityBarrel.class);
        CompoundTag barrelNbt = new CompoundTag();
        barrelNbt.putString("id", "tileentity_fluid_barrel");
        barrelNbt.putInt("tank", 500);
        barrelNbt.putInt("tank_max", 6000);
        barrelNbt.putInt("tank_type", 20);
        barrelNbt.putShort("mode", (short) 2);
        LegacyStructureTiles.apply(level, helper.absolutePos(barrelPos), barrel, barrelNbt, palette, 0);
        helper.assertTrue(barrel.tank.getTankType() == Fluids.fromID(20) && barrel.tank.getFill() == 500,
                "the barrel keeps its 1.7 fluid (" + barrel.tank.getTankType().getName() + " " + barrel.tank.getFill() + ")");

        // bobblehead type
        BlockPos bobblePos = new BlockPos(3, 1, 1);
        helper.setBlock(bobblePos, ModDecoCrateBlocks.bobblehead.get().defaultBlockState());
        TileEntityBobble bobble = GameTestHelpers.blockEntity(helper, bobblePos, TileEntityBobble.class);
        CompoundTag bobbleNbt = new CompoundTag();
        bobbleNbt.putString("id", "tileentity_ntm_bobblehead");
        bobbleNbt.putByte("type", (byte) 3);
        LegacyStructureTiles.apply(level, helper.absolutePos(bobblePos), bobble, bobbleNbt, palette, 0);
        helper.assertTrue(bobble.typeOrdinal() == 3, "the bobblehead keeps its figure");

        // iron crate items through the item palette (numeric id + material damage)
        BlockPos cratePos = new BlockPos(4, 1, 1);
        helper.setBlock(cratePos, ModStorageBlocks.crate_iron.get().defaultBlockState());
        TileEntityCrate crate = GameTestHelpers.blockEntity(helper, cratePos, TileEntityCrate.class);
        CompoundTag crateNbt = new CompoundTag();
        crateNbt.putString("id", "tileentity_crate_iron");
        ListTag items = new ListTag();
        CompoundTag code = new CompoundTag();
        code.putByte("slot", (byte) 14);
        code.putShort("id", (short) 5192);
        code.putByte("Count", (byte) 1);
        code.putShort("Damage", (short) 0);
        items.add(code);
        CompoundTag pipe = new CompoundTag();
        pipe.putByte("slot", (byte) 20);
        pipe.putShort("id", (short) 4000);
        pipe.putByte("Count", (byte) 2);
        pipe.putShort("Damage", (short) 2600);
        items.add(pipe);
        crateNbt.put("items", items);
        LegacyStructureTiles.apply(level, helper.absolutePos(cratePos), crate, crateNbt, palette, 0);
        String slot14 = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(crate.getItem(14).getItem()).toString();
        String slot20 = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(crate.getItem(20).getItem()).toString();
        helper.assertTrue(slot14.equals("hbm:launch_code_piece") && slot20.equals("hbm:pipe_iron")
                        && crate.getItem(20).getCount() == 2,
                "crate items relinked: " + slot14 + ", " + slot20);

        // skeleton holder with a book and quill
        BlockPos skelPos = new BlockPos(5, 1, 1);
        helper.setBlock(skelPos, ModDungeonBlocks.skeleton_holder.get().defaultBlockState());
        BlockSkeletonHolder.TileEntitySkeletonHolder holder =
                GameTestHelpers.blockEntity(helper, skelPos, BlockSkeletonHolder.TileEntitySkeletonHolder.class);
        CompoundTag skelNbt = new CompoundTag();
        skelNbt.putString("id", "tileentity_ntm_skeleton");
        CompoundTag book = new CompoundTag();
        book.putShort("id", (short) 386);
        book.putByte("Count", (byte) 1);
        CompoundTag tag = new CompoundTag();
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf("Still no boat."));
        tag.put("pages", pages);
        book.put("tag", tag);
        skelNbt.put("item", book);
        LegacyStructureTiles.apply(level, helper.absolutePos(skelPos), holder, skelNbt, palette, 0);
        helper.assertTrue(holder.getItem().is(Items.WRITABLE_BOOK)
                        && ItemStacks.writableBookPages(holder.getItem()).equals(List.of("Still no boat.")),
                "the skeleton holds the logbook with its pages");

        // flower pot and skull: block swaps
        BlockPos potPos = new BlockPos(6, 1, 1);
        helper.setBlock(potPos, Blocks.FLOWER_POT.defaultBlockState());
        CompoundTag potNbt = new CompoundTag();
        potNbt.putString("id", "FlowerPot");
        potNbt.putInt("Item", 39);
        potNbt.putInt("Data", 0);
        LegacyStructureTiles.apply(level, helper.absolutePos(potPos), null, potNbt, palette, 0);
        helper.assertTrue(helper.getBlockState(potPos).is(Blocks.POTTED_BROWN_MUSHROOM), "1.7 pot with a brown mushroom");

        BlockPos skullPos = new BlockPos(1, 1, 3);
        helper.setBlock(skullPos, Blocks.SKELETON_SKULL.defaultBlockState());
        CompoundTag skullNbt = new CompoundTag();
        skullNbt.putString("id", "Skull");
        skullNbt.putByte("SkullType", (byte) 2);
        skullNbt.putByte("Rot", (byte) 4);
        LegacyStructureTiles.apply(level, helper.absolutePos(skullPos), level.getBlockEntity(helper.absolutePos(skullPos)),
                skullNbt, palette, 0);
        helper.assertTrue(helper.getBlockState(skullPos).is(Blocks.ZOMBIE_HEAD)
                        && helper.getBlockState(skullPos).getValue(net.minecraft.world.level.block.SkullBlock.ROTATION) == 4,
                "1.7 zombie skull at rotation 4");

        // an unknown id is reported, not silently dropped
        CompoundTag unknown = new CompoundTag();
        unknown.putString("id", "tileentity_not_a_thing");
        helper.assertTrue(!LegacyStructureTiles.apply(level, helper.absolutePos(mine), landmine, unknown, palette, 0),
                "unknown 1.7 ids are left to the not-migrated log");
        helper.succeed();
    }

    /** 1.7 TileEntityPipelineBase: link within 10 m, errors, one network over the pipe, broken anchor disconnects. */
    private static void pipeline(GameTestHelper helper) {
        Fluids.init();
        ServerLevel level = helper.getLevel();
        BlockPos a = new BlockPos(2, 2, 2);
        BlockPos b = new BlockPos(9, 2, 2);
        BlockPos far = new BlockPos(13, 2, 13);
        var anchorBlock = ModFluidNetBlocks.pipe_anchor.get();
        helper.setBlock(a, anchorBlock.defaultBlockState());
        helper.setBlock(b, anchorBlock.defaultBlockState());
        helper.setBlock(far, anchorBlock.defaultBlockState());
        TileEntityPipeAnchor first = GameTestHelpers.blockEntity(helper, a, TileEntityPipeAnchor.class);
        TileEntityPipeAnchor second = GameTestHelpers.blockEntity(helper, b, TileEntityPipeAnchor.class);
        TileEntityPipeAnchor third = GameTestHelpers.blockEntity(helper, far, TileEntityPipeAnchor.class);
        first.setFluidType(Fluids.WATER);

        helper.assertTrue(TileEntityPipeAnchor.canConnect(first, first) == 2, "an anchor cannot link to itself");
        helper.assertTrue(TileEntityPipeAnchor.canConnect(first, third) == 3, "15.6 m is too far for 10 m pipes");
        helper.assertTrue(TileEntityPipeAnchor.canConnect(first, second) == 0 && second.getFluidType() == Fluids.WATER,
                "7 m links, and the fluid-less anchor takes the other's fluid");
        first.addConnection(second.getBlockPos());
        second.addConnection(first.getBlockPos());
        var graph = com.hbm.api.fluidmk2.FluidGraph.get(level);
        var netA = graph.networkAt(first.getBlockPos().asLong());
        helper.assertTrue(netA != null && netA == graph.networkAt(second.getBlockPos().asLong()),
                "the two anchors are one fluid network over the pipe");
        third.setFluidType(Fluids.OIL);
        helper.assertTrue(TileEntityPipeAnchor.canConnect(second, third) == 4 || TileEntityPipeAnchor.canConnect(second, third) == 3,
                "different fluids do not link");

        level.removeBlock(helper.absolutePos(b), false);
        helper.assertTrue(first.getConnected().isEmpty(), "breaking an anchor tears it out of its peer's list");
        helper.succeed();
    }
}
