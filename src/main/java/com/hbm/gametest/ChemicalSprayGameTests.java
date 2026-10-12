package com.hbm.gametest;

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModEntityItems;
import com.hbm.items.weapon.ItemDisperser;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.7 {@code EntityChemical}'s boil / cold / flamethrower damage, experience and ender juice, {@code EntityMist}'s
 * ender juice, and 1.7's {@code FluidType.isDispersable} on the disperser canister, glyphid gland and the fluid
 * container registry. Every expected number is worked out from the 1.7 formula in the javadoc. Registered from
 * {@link HbmGameTestFunctions}.
 */
public final class ChemicalSprayGameTests {

    private ChemicalSprayGameTests() {}

    private static EntityChemical spray(GameTestHelper helper, FluidType type, LivingEntity at) {
        EntityChemical chemical = new EntityChemical(ModEntities.CHEMICAL.get(), helper.getLevel());
        chemical.setFluid(type);
        chemical.setPos(at.getX(), at.getY(), at.getZ());
        return chemical;
    }

    private static LivingEntity pig(GameTestHelper helper, BlockPos pos) {
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, pos);
        pig.setNoGravity(true);
        pig.setDeltaMovement(Vec3.ZERO);
        return pig;
    }

    /** Applies one spray hit and returns the health it took. */
    private static float hit(EntityChemical chemical, LivingEntity target, double intensity) {
        target.setHealth(target.getMaxHealth());
        target.clearFire();
        target.invulnerableTime = 0;
        float before = target.getHealth();
        chemical.affect(target, intensity);
        return before - target.getHealth();
    }

    private static void near(GameTestHelper helper, float actual, float expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-4F, what + ": expected " + expected + ", took " + actual);
    }

    /**
     * Temperature damage, 1.7 {@code EntityChemical.affect}:
     * <ul>
     * <li>HOTSTEAM 300 °C, a gas: boil {@code min(0.25 + (300 - 100) * 0.001, 15) = 0.45}, no afterburn (< 500 °C).</li>
     * <li>ULTRAHOTSTEAM 600 °C: {@code 0.25 + 500 * 0.001 = 0.75} and 10 s afterburn = 200 fire ticks.</li>
     * <li>LAVA 1200 °C, a (viscous) liquid: {@code 0.25 + 1100 * 0.001 = 1.35}, 200 fire ticks.</li>
     * <li>CRYOGEL -170 °C, a liquid: cold {@code min(0.25 + 170 * 0.01, 2) = 1.95}.</li>
     * <li>HYDROGEN -260 °C is gaseous (ART) and combustible, so a GASFLAME, not LIQUID/GAS: no cold damage; at
     * intensity 1 its flamethrower hit is {@code (0.2 + max(5000 / 50000, min(10000 / 100000, 15))) = 0.3}.</li>
     * </ul>
     */
    public static void temperature(GameTestHelper helper) {
        LivingEntity pig = pig(helper, new BlockPos(1, 3, 1));
        helper.runAfterDelay(1, () -> {
            near(helper, hit(spray(helper, Fluids.HOTSTEAM, pig), pig, 0.5D), 0.45F, "HOTSTEAM boil");
            helper.assertTrue(pig.getRemainingFireTicks() <= 0, "300 °C must not set fire, has " + pig.getRemainingFireTicks());

            near(helper, hit(spray(helper, Fluids.ULTRAHOTSTEAM, pig), pig, 0.5D), 0.75F, "ULTRAHOTSTEAM boil");
            helper.assertTrue(pig.getRemainingFireTicks() == 200,
                    "600 °C must set 10 s = 200 fire ticks, has " + pig.getRemainingFireTicks());

            near(helper, hit(spray(helper, Fluids.LAVA, pig), pig, 1D), 1.35F, "LAVA boil");
            helper.assertTrue(pig.getRemainingFireTicks() == 200, "1200 °C must set 200 fire ticks");

            near(helper, hit(spray(helper, Fluids.CRYOGEL, pig), pig, 1D), 1.95F, "CRYOGEL cold");

            helper.assertTrue(EntityChemical.getStyleFromType(Fluids.HYDROGEN) == EntityChemical.ChemicalStyle.GASFLAME,
                    "hydrogen must spray as a GASFLAME");
            near(helper, hit(spray(helper, Fluids.HYDROGEN, pig), pig, 1D), 0.3F, "HYDROGEN gas flame (no cold)");
            helper.succeed();
        });
    }

    /**
     * Flamethrower damage, 1.7 {@code EntityChemical.affect}.
     * <ul>
     * <li>ETHANOL is a combustible liquid, a BURNING spray (intensity forced to 1): its fuel is
     * {@code registerCalculatedFuel(275_000, 2.5)}, combustion {@code round(687_500) = 687_000}, so
     * {@code 0.2 + min(687_000 / 100_000, 15) = 7.07} damage and 5 s = 100 fire ticks.</li>
     * <li>GAS is flammable and combustible gas, a GASFLAME: flammable {@code 100_000 * 1 * 0.5 = 50_000}, combustion
     * {@code 50_000 * 1.5 = 75_000}; at intensity 0.5 heat is {@code max(50_000 / 50_000, 0.75) * 0.5 = 0.5}, damage
     * {@code (0.2 + 0.5) * 0.5 = 0.35} and {@code ceil(5 * 0.5) = 3} s = 60 fire ticks.</li>
     * </ul>
     */
    public static void flamethrower(GameTestHelper helper) {
        LivingEntity pig = pig(helper, new BlockPos(1, 3, 1));
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(EntityChemical.getStyleFromType(Fluids.ETHANOL) == EntityChemical.ChemicalStyle.BURNING,
                    "ethanol must spray as BURNING");
            near(helper, hit(spray(helper, Fluids.ETHANOL, pig), pig, 0.25D), 7.07F, "ETHANOL burning spray");
            helper.assertTrue(pig.getRemainingFireTicks() == 100,
                    "a burning spray sets 5 s = 100 fire ticks, has " + pig.getRemainingFireTicks());

            helper.assertTrue(EntityChemical.getStyleFromType(Fluids.GAS) == EntityChemical.ChemicalStyle.GASFLAME,
                    "natural gas must spray as a GASFLAME");
            near(helper, hit(spray(helper, Fluids.GAS, pig), pig, 0.5D), 0.35F, "GAS flame at intensity 0.5");
            helper.assertTrue(pig.getRemainingFireTicks() == 60,
                    "a gas flame at intensity 0.5 sets ceil(2.5) = 3 s = 60 fire ticks, has " + pig.getRemainingFireTicks());
            helper.succeed();
        });
    }

    /**
     * 1.7 experience juice: a player hit gains one experience point ({@code EnchantmentUtil.addExperience(player, 1,
     * false)}: total 1, bar 1 / 7 at level 0) and the spray is used up; a pig is not affected and does not use it up.
     */
    public static void experienceJuice(GameTestHelper helper) {
        LivingEntity pig = pig(helper, new BlockPos(1, 3, 1));
        helper.runAfterDelay(1, () -> {
            EntityChemical onPig = spray(helper, Fluids.XPJUICE, pig);
            onPig.affect(pig, 1D);
            helper.assertTrue(!onPig.isRemoved(), "experience juice on a pig must not use the spray up");

            Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
            player.experienceLevel = 0;
            player.experienceProgress = 0F;
            player.totalExperience = 0;
            EntityChemical onPlayer = spray(helper, Fluids.XPJUICE, pig);
            onPlayer.affect(player, 1D);
            helper.assertTrue(player.totalExperience == 1 && player.experienceLevel == 0
                            && Math.abs(player.experienceProgress - 1F / 7F) < 1.0E-6F,
                    "experience juice must give 1 point (total 1, bar 1/7), has total " + player.totalExperience
                            + " level " + player.experienceLevel + " bar " + player.experienceProgress);
            helper.assertTrue(onPlayer.isRemoved(), "experience juice must be used up on the player");
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code teleportTo}, exactly: from the target, fall one block at a time until the block below blocks
     * movement, keep the target's x/z and fractional y, and only accept the spot if nothing collides and no liquid
     * is touched. Stone at y=1 under a target at y=6 lands the pig at y=2. Over a water source at y=2 the spot is
     * refused and the pig stays where it was.
     */
    public static void enderTeleportTo(GameTestHelper helper) {
        LivingEntity pig = pig(helper, new BlockPos(1, 3, 1));
        helper.runAfterDelay(1, () -> {
            helper.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
            helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
            helper.setBlock(new BlockPos(6, 2, 6), Blocks.WATER);

            Vec3 target = helper.absoluteVec(new Vec3(4.5D, 6D, 4.5D));
            boolean moved = EntityChemical.teleportTo(pig, target.x, target.y, target.z);
            Vec3 landed = helper.absoluteVec(new Vec3(4.5D, 2D, 4.5D));
            helper.assertTrue(moved && pig.position().distanceTo(landed) < 1.0E-6D,
                    "a pig sent to (4.5, 6, 4.5) above stone at y=1 must land at " + landed + ", is at " + pig.position());

            Vec3 before = pig.position();
            Vec3 wet = helper.absoluteVec(new Vec3(6.5D, 6D, 6.5D));
            boolean refused = !EntityChemical.teleportTo(pig, wet.x, wet.y, wet.z);
            // Take the water away in the same tick, before its first fluid tick: left alone it spreads seven blocks
            // into neighbouring plots and its current pushes their mobs (it drifted the phosgene test's pig).
            helper.setBlock(new BlockPos(6, 2, 6), Blocks.AIR);
            helper.assertTrue(refused && pig.position().distanceTo(before) < 1.0E-6D,
                    "a spot in water must be refused and the pig must stay at " + before + ", is at " + pig.position());
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code teleportRandomly} through both callers: an ender-juice spray hit and an ender-juice cloud. With the
     * caller's random seeded, the target is {@code x + (nextDouble - 0.5) * 64}, {@code y + nextInt(64) - 32},
     * {@code z + (nextDouble - 0.5) * 64}; a teleport keeps that x/z exactly and lands at or below that y. Seeds are
     * tried until each caller has moved its pig at least once (a target underground or in a wall is refused, as 1.7).
     * The plot is padded so a 32-block jump stays inside it, and each target chunk is loaded before the hit. The pig
     * sits at the padded plot's centre ({@code GameTests.paddedCentre}): a fixed 76 was the legacy template's middle
     * but lies 36 blocks outside 26.1's symmetric padding, so the full suite let a neighbour share the jump area.
     */
    static final int ENDER_JUICE_PADDING = 40;

    public static void enderJuice(GameTestHelper helper) {
        BlockPos centre = dev.golem.core.gametest.GameTests.paddedCentre(ENDER_JUICE_PADDING, 2);
        LivingEntity pig = pig(helper, centre);
        helper.runAfterDelay(1, () -> {
            ServerLevel level = helper.getLevel();
            Vec3 home = helper.absoluteVec(Vec3.atBottomCenterOf(centre));
            boolean sprayMoved = false;
            boolean mistMoved = false;
            for (long seed = 1; seed <= 64 && !(sprayMoved && mistMoved); seed++) {
                for (int caller = 0; caller < 2; caller++) {
                    pig.teleportTo(home.x, home.y, home.z);
                    Vec3 from = pig.position();
                    RandomSource expected = RandomSource.create(seed);
                    double tx = from.x + (expected.nextDouble() - 0.5D) * 64.0D;
                    double ty = from.y + (double) (expected.nextInt(64) - 32);
                    double tz = from.z + (expected.nextDouble() - 0.5D) * 64.0D;
                    // A GameTest world only loads the chunks around a plot; 1.7 refuses a target in an unloaded chunk
                    // (blockExists), so load the target chunk first to exercise the teleport itself.
                    level.getChunk(net.minecraft.util.Mth.floor(tx) >> 4, net.minecraft.util.Mth.floor(tz) >> 4);
                    if (caller == 0) {
                        EntityChemical chemical = spray(helper, Fluids.ENDERJUICE, pig);
                        chemical.seedRandom(seed);
                        chemical.affect(pig, 1D);
                    } else {
                        EntityMist mist = new EntityMist(level);
                        mist.setType(Fluids.ENDERJUICE);
                        mist.setArea(4F, 3F);
                        mist.setDuration(150);
                        mist.setPos(from.x, from.y, from.z);
                        mist.seedRandom(seed);
                        mist.tickCount = 29;
                        mist.tick();
                    }
                    Vec3 now = pig.position();
                    if (now.distanceTo(from) < 1.0E-6D) continue;
                    helper.assertTrue(Math.abs(now.x - tx) < 1.0E-6D && Math.abs(now.z - tz) < 1.0E-6D
                                    && now.y <= ty + 1.0E-6D && Math.abs((now.y - ty) - Math.rint(now.y - ty)) < 1.0E-6D,
                            (caller == 0 ? "spray" : "mist") + " seed " + seed + ": target (" + tx + ", " + ty + ", " + tz
                                    + ") but the pig is at " + now);
                    helper.assertTrue(level.getBlockState(BlockPos.containing(now.x, now.y, now.z).below()).blocksMotion(),
                            "a teleported pig must stand over a block that blocks movement, at " + now);
                    if (caller == 0) sprayMoved = true;
                    else mistMoved = true;
                }
            }
            helper.assertTrue(sprayMoved && mistMoved,
                    "ender juice must teleport for some seed in 1..64 (spray " + sprayMoved + ", mist " + mistMoved
                            + ", pig removed " + pig.isRemoved() + ")");
            pig.discard();
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code FluidType.isDispersable}: not antimatter, not container-less, not viscous. Of the 155 fluids after
     * NONE (1.7 has 156 in all), 53 are viscous, 18 have no container and 3 are antimatter, with WASTEFLUID both
     * viscous and container-less: {@code 155 - (53 + 18 + 3 - 1) = 82} are dispersable. (83 while the port still
     * carried 1.12.2's dispersable HYDRAZINE, removed 2026-10-03.) 1.7 offers exactly those as creative disperser canisters,
     * only pheromone and sulfuric acid as creative glands, and registers both items as containers for dispersable
     * fluids alone (2,000 mB canister, 4,000 mB gland), ahead of its lead-container cut-off.
     */
    public static void dispersable(GameTestHelper helper) {
        // HBM's (and vanilla's) fluids only, so the 1.7 counts hold with an addon's fluids loaded
        int dispersable = 0, ours = 0;
        FluidType[] all = Fluids.getAll();
        for (int i = 1; i < all.length; i++) {
            if (!FluidRecipeTableGameTests.ours(all[i])) continue;
            if (all[i].isDispersable()) dispersable++;
        }
        for (FluidType type : all) if (FluidRecipeTableGameTests.ours(type)) ours++;
        helper.assertTrue(ours == 156 && all[0] == Fluids.NONE, "156 (1.7) fluids with NONE first, have " + ours);
        helper.assertTrue(dispersable == 82, "82 fluids must be dispersable, counted " + dispersable);

        ItemDisperser canister = ModEntityItems.disperser_canister.get();
        ItemDisperser gland = ModEntityItems.glyphid_gland.get();
        List<ItemStack> canisters = canister.creativeVariants().stream()
                .filter(s -> FluidRecipeTableGameTests.ours(canister.getContent(s))).toList();
        List<ItemStack> glands = gland.creativeVariants();
        helper.assertTrue(canisters.size() == 82, "82 creative disperser canisters, have " + canisters.size());
        helper.assertTrue(glands.size() == 2 && glands.stream().anyMatch(s -> gland.getContent(s) == Fluids.PHEROMONE)
                        && glands.stream().anyMatch(s -> gland.getContent(s) == Fluids.SULFURIC_ACID),
                "the creative glands are pheromone and sulfuric acid, have " + glands.size());

        for (FluidType refused : new FluidType[]{Fluids.LAVA, Fluids.AMAT, Fluids.WASTEGAS, Fluids.PLASMA_DT, Fluids.OIL}) {
            helper.assertTrue(!canister.acceptsFluid(refused, new ItemStack(canister))
                            && !gland.acceptsFluid(refused, new ItemStack(gland)),
                    refused.getName() + " is not dispersable and must not fill a disperser");
        }
        for (FluidType taken : new FluidType[]{Fluids.WATER, Fluids.UF6, Fluids.PHEROMONE, Fluids.SULFURIC_ACID}) {
            helper.assertTrue(canister.acceptsFluid(taken, new ItemStack(canister))
                            && gland.acceptsFluid(taken, new ItemStack(gland)),
                    taken.getName() + " is dispersable and must fill a disperser");
        }

        FluidContainerRegistry registry = FluidContainerRegistry.INSTANCE;
        helper.assertTrue(registry.holds(canister.make(Fluids.UF6, 2000), "UF6", 2000),
                "a full UF6 canister is a 2,000 mB UF6 container");
        helper.assertTrue(registry.holds(gland.make(Fluids.WATER, 4000), "WATER", 4000),
                "a full water gland is a 4,000 mB water container");
        ItemStack forcedLava = canister.make(Fluids.LAVA, 2000);
        helper.assertTrue(!registry.holds(forcedLava, "LAVA", 2000), "a lava canister is not a registered container");
        helper.assertTrue(registry.examples("LAVA", 2000).stream().noneMatch(s -> s.is(canister)),
                "no disperser canister example for lava");
        helper.assertTrue(registry.examples("UF6", 2000).stream().anyMatch(s -> s.is(canister)),
                "a disperser canister example for UF6");
        helper.succeed();
    }
}
