package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.client.SmogFog;
import com.hbm.config.BombConfig;
import com.hbm.config.DungeonConfig;
import com.hbm.config.MobConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.config.StructureConfig;
import com.hbm.config.TerrainConfig;
import com.hbm.config.WeaponConfig;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.recipes.FuelPoolRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingCyanideRecipe;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.items.ModBatterySCItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMedicineItems;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemBatterySC.EnumBatterySC;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.special.ItemDrop;
import com.hbm.main.ModEventHandlerHooks;
import com.hbm.potion.ModEffects;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import com.hbm.world.feature.ImpactAftermathFeature;
import com.hbm.world.gen.ConfigSpreadPlacement;
import com.hbm.world.gen.NTMStructureDeclarations;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Effects;
import dev.golem.core.world.ItemUses;
import dev.golem.core.world.LightLevels;
import dev.golem.core.world.Sleeps;
import dev.golem.core.world.Spawns;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

/**
 * The 1.7 behaviours that waited for a new hook, each against its 1.7 value.
 */
public final class HooksGameTests {

    private HooksGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function CYANIDE = TESTS.function("hooks_cyanide_anvil_and_eating", 20, () -> HooksGameTests::cyanide);
    public static final GameTests.Function RAMPANT = TESTS.function("hooks_rampant_sleep_target", 20, () -> HooksGameTests::rampant);
    public static final GameTests.Function VILLAGE = TESTS.function("hooks_village_block_rewrite", 20, () -> HooksGameTests::village);
    public static final GameTests.Function BREAK = TESTS.function("hooks_block_break_lead_and_coal_dust", 20, () -> HooksGameTests::blockBreak);
    public static final GameTests.Function SMOG = TESTS.function("hooks_smog_fog_formula", 20, () -> HooksGameTests::smog);
    public static final GameTests.Function REGROW = TESTS.function("hooks_waste_earth_regrowth", 60, () -> HooksGameTests::regrow);
    public static final GameTests.Function XEN = TESTS.function("hooks_xen_crystal_drop", 40, () -> HooksGameTests::xen);
    public static final GameTests.Function FALLOUT = TESTS.function("hooks_fallout_delay_counter", 20, () -> HooksGameTests::falloutDelay);
    public static final GameTests.Function SOCKET = TESTS.function("hooks_battery_socket_sc_discharge", 20, () -> HooksGameTests::socket);
    public static final GameTests.Function DUCK = TESTS.function("hooks_duck_lays_plain_egg", 20, () -> HooksGameTests::duck);
    public static final GameTests.Function JEI = TESTS.function("hooks_jei_fuel_pool_and_arc_furnace_pass", 20, () -> HooksGameTests::jeiPages);
    public static final GameTests.Function STRUCTURES = TESTS.function("hooks_structure_placement_reads_cfg", 20, () -> HooksGameTests::structures);

    public static void init() {}

    // 1: cyanide

    private static void cyanide(GameTestHelper helper) {
        AnvilSmithingRecipe cyanide = null;
        for (AnvilSmithingRecipe recipe : AnvilRecipes.getSmithing()) if (recipe instanceof AnvilSmithingCyanideRecipe) cyanide = recipe;
        helper.assertTrue(cyanide != null, "the anvil lists 1.7's cyanide recipe");
        helper.assertTrue(cyanide.tier == 1, "tier 1");
        ItemStack apple = new ItemStack(Items.APPLE, 5);
        ItemStack planC = new ItemStack(ModMedicineItems.plan_c.get());
        ItemStack redPill = new ItemStack(ModMedicineItems.pill_red.get());
        helper.assertTrue(cyanide.matches(apple, planC), "any food + Plan C");
        helper.assertTrue(cyanide.matches(apple, redPill), "any food + red pill");
        helper.assertTrue(!cyanide.matches(new ItemStack(Items.STICK), planC), "not a non-food");
        ItemStack laced = cyanide.getOutput(apple, planC);
        helper.assertTrue(laced.is(Items.APPLE) && laced.getCount() == 1 && ModDataComponents.NTM_CYANIDE.getOr(laced, false)
                && !ModDataComponents.NTM_RED_PILL.getOr(laced, false), "one apple, ntmCyanide");
        ItemStack pilled = cyanide.getOutput(apple, redPill);
        helper.assertTrue(ModDataComponents.NTM_RED_PILL.getOr(pilled, false) && !ModDataComponents.NTM_CYANIDE.getOr(pilled, false),
                "red pill -> ntmRedPill");

        Player victim = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        victim.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
        ItemUses.finished(victim, new ItemStack(Items.APPLE)); // plain food: nothing
        helper.assertTrue(victim.getHealth() == victim.getMaxHealth(), "plain food harmless");
        ItemUses.finished(victim, pilled);
        helper.assertTrue(Effects.hasRegistered(victim, ModEffects.DEATH.get())
                && Effects.getRegistered(victim, ModEffects.DEATH.get()).getDuration() == 60 * 60 * 20, "red pill: death effect for an hour");
        ItemUses.finished(victim, laced);
        helper.assertTrue(victim.getHealth() <= 0 || !victim.isAlive(), "cyanide: ten 1000-point hits kill");
        helper.succeed();
    }

    // 2: rampant

    private static void rampant(GameTestHelper helper) {
        boolean guidance = MobConfig.rampantGlyphidGuidance;
        BlockPos before = PollutionHandler.targetCoords;
        try (GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper)) {
            BlockPos bed = helper.absolutePos(new BlockPos(1, 2, 1));
            MobConfig.rampantGlyphidGuidance = false;
            PollutionHandler.targetCoords = null;
            Sleeps.attempted(mock.player(), bed);
            helper.assertTrue(PollutionHandler.targetCoords == null, "guidance off: no target");
            MobConfig.rampantGlyphidGuidance = true;
            Sleeps.attempted(mock.player(), bed);
            helper.assertTrue(bed.equals(PollutionHandler.targetCoords), "1.7 rampantTargetSetter: the bed becomes the scouts' target");
        } finally {
            MobConfig.rampantGlyphidGuidance = guidance;
            PollutionHandler.targetCoords = before;
        }
        // the potential-spawn hook reaches the populator, which keeps 1.7's gate (off by default)
        int scouts = helper.getLevel().getEntitiesOfClass(EntityGlyphidScout.class,
                new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8)).size();
        Spawns.potentialSpawns(helper.getLevel(), MobCategory.MONSTER,
                helper.absolutePos(new BlockPos(1, 2, 1)));
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(EntityGlyphidScout.class,
                new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8)).size() == scouts, "natural scout spawn is off by default");
        helper.succeed();
    }

    // 3: village

    private static void village(GameTestHelper helper) {
        RandomSource rand = RandomSource.create(42);
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockState wood : List.of(Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(),
                Blocks.SPRUCE_STAIRS.defaultBlockState(), Blocks.OAK_FENCE.defaultBlockState(), Blocks.CRAFTING_TABLE.defaultBlockState(),
                Blocks.GLASS_PANE.defaultBlockState(), Blocks.GLASS.defaultBlockState(), Blocks.LADDER.defaultBlockState(),
                Blocks.WHEAT.defaultBlockState(), Blocks.CHEST.defaultBlockState(), Blocks.IRON_DOOR.defaultBlockState(),
                Blocks.WHITE_WOOL.defaultBlockState(), Blocks.WATER.defaultBlockState(), Blocks.RED_BED.defaultBlockState(),
                Blocks.COBBLESTONE_SLAB.defaultBlockState())) {
            helper.assertTrue(air.equals(ImpactAftermathFeature.villageBlock(wood, rand)), wood + " -> air");
        }
        for (BlockState kept : List.of(Blocks.STONE.defaultBlockState(), Blocks.ICE.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
                Blocks.WHITE_CARPET.defaultBlockState(), Blocks.TORCH.defaultBlockState(),
                Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE))) {
            helper.assertTrue(ImpactAftermathFeature.villageBlock(kept, rand) == null, kept + " kept");
        }
        helper.assertTrue(Blocks.DIRT.defaultBlockState().equals(ImpactAftermathFeature.villageBlock(Blocks.FARMLAND.defaultBlockState(), rand)),
                "farmland -> dirt");
        int gravel = 0, sand = 0;
        for (int i = 0; i < 3000; i++) {
            BlockState c = ImpactAftermathFeature.villageBlock(Blocks.COBBLESTONE.defaultBlockState(), rand);
            if (c != null) { helper.assertTrue(c.is(Blocks.GRAVEL), "cobble -> gravel"); gravel++; }
            BlockState s = ImpactAftermathFeature.villageBlock(Blocks.SANDSTONE.defaultBlockState(), rand);
            if (s != null) { helper.assertTrue(s.is(Blocks.SAND), "sandstone -> sand"); sand++; }
        }
        // 1.7 rand.nextInt(3) == 1: one in three
        helper.assertTrue(gravel > 850 && gravel < 1150 && sand > 850 && sand < 1150, "1 in 3: gravel " + gravel + ", sand " + sand);
        helper.succeed();
    }

    // 4: block break

    private static void blockBreak(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        float metalBefore = PollutionHandler.getPollution(level, pos, PollutionType.HEAVYMETAL);
        try (GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = mock.player();
            float[][] cases = {{4F, -1}, {5F, 0}, {9.9F, 0}, {10F, 1}, {24F, 1}, {25F, 2}, {100F, 2}};
            for (float[] c : cases) {
                PollutionHandler.setPollution(level, pos, PollutionType.HEAVYMETAL, c[0]);
                Effects.removeRegistered(player, ModEffects.LEAD.get());
                ModEventHandlerHooks.onBlockBreak(player, level, pos, Blocks.STONE.defaultBlockState());
                var lead = Effects.getRegistered(player, ModEffects.LEAD.get());
                if (c[1] < 0) helper.assertTrue(lead == null, "metal " + c[0] + ": no lead");
                else helper.assertTrue(lead != null && lead.getAmplifier() == (int) c[1] && lead.getDuration() == 100,
                        "metal " + c[0] + ": lead " + (int) c[1] + " for 100 ticks, got " + lead);
            }
            boolean switchOn = RadiationConfig.enableLeadFromBlocks;
            try {
                RadiationConfig.enableLeadFromBlocks = false;
                Effects.removeRegistered(player, ModEffects.LEAD.get());
                ModEventHandlerHooks.onBlockBreak(player, level, pos, Blocks.STONE.defaultBlockState());
                helper.assertTrue(Effects.getRegistered(player, ModEffects.LEAD.get()) == null, "POL_01 off: no lead");
            } finally {
                RadiationConfig.enableLeadFromBlocks = switchOn;
            }
            PollutionHandler.setPollution(level, pos, PollutionType.HEAVYMETAL, metalBefore);

            // coal dust: each air side 1 in 2
            int dust = 0;
            for (int i = 0; i < 6; i++) {
                for (Direction d : Direction.values()) level.setBlockAndUpdate(pos.relative(d), Blocks.AIR.defaultBlockState());
                ModEventHandlerHooks.onBlockBreak(player, level, pos, Blocks.COAL_ORE.defaultBlockState());
                for (Direction d : Direction.values())
                    if (level.getBlockState(pos.relative(d)).is(ModTerrainBlocks.gas_coal.get())) dust++;
            }
            helper.assertTrue(dust > 6 && dust < 30, "coal ore: about half of 36 sides get coal dust, got " + dust);
            for (Direction d : Direction.values()) level.setBlockAndUpdate(pos.relative(d), Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    // 5: smog

    private static void smog(GameTestHelper helper) {
        helper.assertTrue(Float.isNaN(SmogFog.fogEnd(35F, 192F)), "at the 35 threshold: no smog");
        // 1.7 thickenFog: 192 / (1 + 12 * 5 / 120) = 128
        helper.assertTrue(Math.abs(SmogFog.fogEnd(47F, 192F) - 128F) < 1e-4, "fog end 128");
        helper.assertTrue(Math.abs(SmogFog.tint(95F) - 0.5F) < 1e-6, "tint (95 - 35) / 120 = 0.5");
        helper.assertTrue(SmogFog.tint(500F) == 1F, "tint capped at 1");
        helper.assertTrue(SmogFog.step(0F, 1F) == 0.05F && SmogFog.step(1F, 0F) == 0.95F
                && SmogFog.step(0.98F, 1F) == 1F, "renderSoot eases by 0.05 a tick");
        helper.succeed();
    }

    // 6: waste earth

    private static void regrow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean cleanup = TerrainConfig.cleanupDeadDirt;
        try {
            TerrainConfig.cleanupDeadDirt = false;
            BlockPos covered = helper.absolutePos(new BlockPos(1, 1, 1));
            level.setBlockAndUpdate(covered, ModFalloutBlocks.waste_earth.get().defaultBlockState());
            level.setBlockAndUpdate(covered.above(), Blocks.STONE.defaultBlockState());
            BlockPos open = helper.absolutePos(new BlockPos(3, 1, 1));
            level.setBlockAndUpdate(open, ModFalloutBlocks.waste_earth.get().defaultBlockState());
            level.setBlockAndUpdate(open.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(open.above(2), Blocks.AIR.defaultBlockState());
            helper.assertTrue(level.getBlockState(covered).isRandomlyTicking(), "waste earth ticks randomly");
        } finally {
            TerrainConfig.cleanupDeadDirt = cleanup;
        }
        // let the light engine catch up with the placed blocks first
        helper.runAfterDelay(20, () -> regrowChecks(helper));
    }

    private static void regrowChecks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean cleanup = TerrainConfig.cleanupDeadDirt;
        BlockPos covered = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos open = helper.absolutePos(new BlockPos(3, 1, 1));
        try {
            TerrainConfig.cleanupDeadDirt = false;
            level.getBlockState(covered).randomTick(level, covered, level.getRandom());
            helper.assertTrue(level.getBlockState(covered).is(Blocks.DIRT), "dark and under an opaque block: back to dirt");
            int light = LightLevels.combined(level, open.above());
            level.getBlockState(open).randomTick(level, open, level.getRandom());
            helper.assertTrue(light < 4 || level.getBlockState(open).is(ModFalloutBlocks.waste_earth.get()),
                    "lit and open: stays dead (light " + light + ")");
            TerrainConfig.cleanupDeadDirt = true;
            level.setBlockAndUpdate(open, ModFalloutBlocks.waste_earth.get().defaultBlockState());
            level.getBlockState(open).randomTick(level, open, level.getRandom());
            helper.assertTrue(level.getBlockState(open).is(Blocks.DIRT), "RADWORLD_03_regrow: always back to dirt");
            // a vanilla mushroom on top becomes the glowing one
            TerrainConfig.cleanupDeadDirt = false;
            level.setBlockAndUpdate(open, ModFalloutBlocks.waste_mycelium.get().defaultBlockState());
            level.setBlock(open.above(), Blocks.BROWN_MUSHROOM.defaultBlockState(), 2);
            level.getBlockState(open).randomTick(level, open, level.getRandom());
            helper.assertTrue(level.getBlockState(open.above()).is(ModTerrainBlocks.mush.get()), "mushroom -> mush");
        } finally {
            TerrainConfig.cleanupDeadDirt = cleanup;
        }
        helper.succeed();
    }

    // 7: xen crystal

    private static void xen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemDrop crystal = ModUpgradeItems.crystal_xen.get();
        helper.assertTrue(crystal.kind == ItemDrop.Kind.XEN_CRYSTAL, "crystal_xen is a 1.7 ItemDrop");
        // high above the test area: the floater sphere (radius 25 -> squared distance below 312) holds only this glass
        BlockPos at = helper.absolutePos(new BlockPos(1, 120, 1));
        BlockPos glass = at.offset(2, 0, 0);
        level.setBlockAndUpdate(glass, Blocks.GLASS.defaultBlockState());
        ItemEntity item = new ItemEntity(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, new ItemStack(ModUpgradeItems.crystal_xen.get()));
        level.addFreshEntity(item);
        ItemDrop.tickDropped(item);
        helper.assertTrue(item.isAlive(), "airborne: nothing yet");
        item.setOnGround(true);
        boolean drop = WeaponConfig.dropCrys;
        try {
            WeaponConfig.dropCrys = true;
            ItemDrop.tickDropped(item);
        } finally {
            WeaponConfig.dropCrys = drop;
        }
        helper.assertTrue(!item.isAlive(), "grounded: the crystal is used up");
        helper.assertTrue(level.getBlockState(glass).isAir() && level.getBlockState(glass.above(75)).is(Blocks.GLASS),
                "1.7 floater: the block is lifted 75 up");
        level.setBlockAndUpdate(glass.above(75), Blocks.AIR.defaultBlockState());
        ItemEntity off = new ItemEntity(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, new ItemStack(ModUpgradeItems.crystal_xen.get()));
        level.addFreshEntity(off);
        off.setOnGround(true);
        try {
            WeaponConfig.dropCrys = false;
            ItemDrop.tickDropped(off);
        } finally {
            WeaponConfig.dropCrys = drop;
        }
        helper.assertTrue(!off.isAlive(), "10.04 off: no effect, but 1.7 still kills the grounded drop");
        helper.succeed();
    }

    // 8: fallout delay

    private static void falloutDelay(GameTestHelper helper) {
        helper.assertTrue(BombConfig.fDelay == 4, "6.04_falloutDelay default 4");
        EntityFalloutRain.TickDelay counter = new EntityFalloutRain.TickDelay(4);
        StringBuilder applied = new StringBuilder();
        for (int tick = 1; tick <= 20; tick++) if (counter.step(4)) applied.append(tick).append(' ');
        // 1.7: counted down from 4, applied when it reaches 0, reset to 4 and decremented in the same tick - the first
        // burst on tick 5, then every fourth tick
        helper.assertTrue(applied.toString().equals("5 9 13 17 "), "applies on ticks 5 9 13 17, got " + applied);
        EntityFalloutRain.TickDelay zero = new EntityFalloutRain.TickDelay(0);
        int bursts = 0;
        for (int tick = 0; tick < 50; tick++) if (zero.step(0)) bursts++;
        helper.assertTrue(bursts == 1, "1.7 quirk: fDelay 0 applies once and the counter runs negative, got " + bursts);
        helper.succeed();
    }

    // 9: battery socket

    private static void socket(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        TileEntityBatterySocket socket = new TileEntityBatterySocket(pos,
                ModBatteryBlocks.machine_battery_socket.get().defaultBlockState());
        socket.setLevel(level);
        socket.inventory.setStackInSlot(0, new ItemStack(ModBatterySCItems.get(EnumBatterySC.EMPTY)));
        helper.assertTrue(!socket.hasSCLoaded(), "the empty radiovoltaic cell does not count");
        socket.inventory.setStackInSlot(0, new ItemStack(ModBatterySCItems.get(EnumBatterySC.RA226)));
        helper.assertTrue(socket.hasSCLoaded(), "RA226 cell loaded");
        for (int i = 0; i < 500; i++) {
            long power = socket.getPower();
            helper.assertTrue(power <= 200 && power >= 20, "power = 200 * scPowerMult in 0.1..1, got " + power);
            socket.scPowerMult = Math.max(0.1, Math.min(1, socket.scPowerMult + (level.getRandom().nextDouble() * 2 - 1) / 100));
        }
        ArmorStand target = new ArmorStand(level, pos.getX() + 6.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(target);
        socket.damageTimer = 999;
        socket.discharge();
        helper.assertTrue(socket.damageTimer == 0 && socket.damageTarget >= 1200 && socket.damageTarget < 3600,
                "a discharge picks the next one 1-3 minutes away, got " + socket.damageTarget);
        helper.assertTrue(!level.getEntitiesOfClass(EntityBulletBeamBase.class, new AABB(pos).inflate(16)).isEmpty(),
                "an electric arc at the living target in range");
        helper.assertTrue(TileEntityBatterySocket.discharge != null && TileEntityBatterySocket.discharge.doesPenetrate,
                "the discharge beam config penetrates (1.7 setDoesPenetrate)");
        target.discard();
        helper.succeed();
    }

    // 11: duck

    private static void duck(GameTestHelper helper) {
        EntityDuck duck = Entities.create(ModNtmEntities.DUCK.get(), helper.getLevel(),
                EntitySpawnReason.TRIGGERED);
        duck.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
        Spawns.finalizeTriggered(duck, helper.getLevel());
        helper.assertTrue(duck.compatLaysPlainEgg(), "a duck is the temperate chicken: it lays 1.7's plain egg");
        helper.succeed();
    }

    // 12: JEI pages

    private static void jeiPages(GameTestHelper helper) {
        List<FuelPoolRecipes.Entry> pool = FuelPoolRecipes.displayRecipes();
        int rods = ModRBMKItems.PELLETS.size();
        int pwr = ItemPWRFuel.EnumPWRFuel.values().length;
        helper.assertTrue(pool.size() == 9 + 7 + pwr + rods, "1.7 FuelPool: 9 ZIRNOX, 7 plates, " + pwr + " PWR, " + rods
                + " RBMK, got " + pool.size());
        for (int i = 0; i < 16 + pwr; i++) {
            FuelPoolRecipes.Entry e = pool.get(i);
            helper.assertTrue(FuelPoolRecipes.isHot(e.input()) && !FuelPoolRecipes.isHot(e.output()), "hot in, cooled out: " + e.input());
        }
        List<ArcFurnaceRecipes.ArcFurnaceRecipe> pass = ArcFurnaceRecipes.furnacePass(List.of(
                Map.entry(new ItemStack(Items.IRON_ORE), new ItemStack(Items.IRON_INGOT)),
                Map.entry(new ItemStack(Items.IRON_ORE), new ItemStack(Items.IRON_NUGGET)),
                Map.entry(new ItemStack(Items.CLAY_BALL), new ItemStack(Items.BRICK)),
                Map.entry(new ItemStack(Items.COBBLESTONE), new ItemStack(Items.STONE)),
                Map.entry(new ItemStack(Items.SAND), new ItemStack(Items.GLASS))));
        boolean ironExplicit = ArcFurnaceRecipes.getOutput(new ItemStack(Items.IRON_ORE), false) != null;
        int expected = ironExplicit ? 1 : 2;
        helper.assertTrue(pass.size() == expected && pass.get(pass.size() - 1).solidOutput.is(Items.BRICK)
                        && (ironExplicit || pass.get(0).solidOutput.is(Items.IRON_INGOT)),
                "furnace pass: iron ore (arc smeltable, first only, unless an explicit recipe has it) and clay (its brick is), "
                        + "not cobble (neither) nor sand (explicit recipe); got " + pass.size());
        helper.succeed();
    }

    // 13: structures

    private static void structures(GameTestHelper helper) {
        long seed = 4815162342L;
        ConfigSpreadPlacement ntm = new ConfigSpreadPlacement(Vec3i.ZERO,
                StructurePlacement.FrequencyReductionMethod.DEFAULT, 1F,
                NTMStructureDeclarations.SALT, Optional.empty(), "ntm");
        RandomSpreadStructurePlacement vanilla = new RandomSpreadStructurePlacement(16, 4, RandomSpreadType.LINEAR, NTMStructureDeclarations.SALT);
        int min = StructureConfig.structureMinChunks, max = StructureConfig.structureMaxChunks;
        int antenna = DungeonConfig.antennaStructure;
        try {
            helper.assertTrue(ntm.spacing() == 16 && ntm.separation() == 4, "5.02/5.01 defaults 16/4");
            for (int x = -40; x < 40; x++) for (int z = -40; z < 40; z++) {
                ChunkPos v = vanilla.getPotentialStructureChunk(seed, x, z);
                helper.assertTrue(v.equals(ntm.getPotentialStructureChunk(seed, x, z)), "same candidates as the old fixed set");
            }
            StructureConfig.structureMaxChunks = 24;
            StructureConfig.structureMinChunks = 8;
            helper.assertTrue(ntm.spacing() == 24 && ntm.separation() == 8, "read at generation");
            ChunkPos c = ntm.getPotentialStructureChunk(seed, 30, 30);
            helper.assertTrue(Math.floorMod(c.getMinBlockX() >> 4, 24) < 16, "spread below max - min");

            ConfigSpreadPlacement dungeon = new ConfigSpreadPlacement(Vec3i.ZERO,
                    StructurePlacement.FrequencyReductionMethod.DEFAULT, 1F,
                    1_700_001, Optional.empty(), "dungeon:antenna");
            helper.assertTrue(dungeon.spacing() == 15 && Math.abs(dungeon.keepChance() - 0.9F) < 1e-6, "4.01 250: spacing 15, keep 0.9");
            int sites = 0;
            for (int x = 0; x < 300; x++) for (int z = 0; z < 300; z++) if (dungeon.isCandidate(seed, x, z)) sites++;
            // 1 in 250 of 90,000 chunks = 360
            helper.assertTrue(sites > 280 && sites < 440, "about one antenna per 250 chunks, got " + sites);
            DungeonConfig.antennaStructure = 0;
            boolean any = false;
            for (int x = 0; x < 60; x++) for (int z = 0; z < 60; z++) any |= dungeon.isCandidate(seed, x, z);
            helper.assertTrue(!any, "4.01 = 0 turns it off");
            DungeonConfig.antennaStructure = 64;
            helper.assertTrue(dungeon.spacing() == 8 && dungeon.keepChance() == 1F, "4.01 = 64: spacing 8, keep 1");

            boolean ruins = StructureConfig.enableRuins;
            try {
                helper.assertTrue(NTMStructureDeclarations.runtimeWeight("ruina") == 10 && NTMStructureDeclarations.runtimeWeight("null_ocean") == 35,
                        "weights from hbm.cfg defaults");
                StructureConfig.enableRuins = false;
                helper.assertTrue(NTMStructureDeclarations.runtimeWeight("ruina") == 0, "5.05 off: ruins weigh nothing");
            } finally {
                StructureConfig.enableRuins = ruins;
            }
        } finally {
            StructureConfig.structureMinChunks = min;
            StructureConfig.structureMaxChunks = max;
            DungeonConfig.antennaStructure = antenna;
        }
        helper.succeed();
    }
}
