package com.hbm.gametest;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Pheromone;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;

/**
 * The 1.7 fluid traits: {@code FT_VentRadiation},
 * {@code FT_Poison}, {@code FT_Pheromone} and the {@code FluidTraitSimple} tags, with hand-computed 1.7 values,
 * and 1.7's {@code traitList} tooltip order. Registered from {@link HbmGameTestFunctions}.
 */
public final class FluidTraitGameTests {

    private FluidTraitGameTests() {}

    /** A cloud aged so that, after the hand tick's own increment, it is 30 of 150: intensity 1 - 30/150 = 0.8. */
    private static EntityMist mist(GameTestHelper helper, FluidType type, LivingEntity at) {
        EntityMist mist = new EntityMist(helper.getLevel());
        mist.setType(type);
        mist.setArea(4F, 3F);
        mist.setDuration(150);
        mist.setPos(at.getX(), at.getY(), at.getZ());
        return mist;
    }

    private static void tickAt30(EntityMist mist) {
        mist.tickCount = 29;
        mist.tick();
    }

    private static LivingEntity floating(GameTestHelper helper, EntityType<? extends Mob> type,
            BlockPos pos) {
        LivingEntity mob = helper.spawnWithNoFreeWill(type, pos);
        mob.setNoGravity(true);
        mob.setDeltaMovement(Vec3.ZERO);
        return mob;
    }

    /**
     * UF6 ({@code FT_VentRadiation(0.2F)}). 1.7 {@code EntityMist}: every server tick the cloud adds
     * {@code radPerMB * 2 = 0.4} to the chunk radiation at its block, and contaminates a living thing inside it by
     * {@code radPerMB * 5 = 1.0} (CREATIVE contamination, so an unarmoured pig takes the full 1.0). Both are
     * measured as deltas across one hand tick so diffusion between ticks cannot move them.
     */
    public static void ventRadiationMist(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FT_VentRadiation vent = Fluids.UF6.getTrait(FT_VentRadiation.class);
        helper.assertTrue(vent != null && vent.getRadPerMB() == 0.2F, "UF6 must carry FT_VentRadiation(0.2F)");
        LivingEntity pig = floating(helper, EntityType.PIG, new BlockPos(1, 3, 1));
        EntityMist mist = mist(helper, Fluids.UF6, pig);

        helper.runAfterDelay(1, () -> {
            BlockPos at = BlockPos.containing(mist.getX(), mist.getY(), mist.getZ());
            double chunkBefore = ChunkRadiationManager.proxy.getRadiation(level, at);
            double pigBefore = HbmLivingProps.getRadiation(pig);
            double chunkDelta;
            try {
                tickAt30(mist);
                chunkDelta = ChunkRadiationManager.proxy.getRadiation(level, at) - chunkBefore;
            } finally {
                // chunk radiation is world state outside the plot: put it back so a neighbour reading absolute
                // values in the same chunk never sees this test's 0.4
                ChunkRadiationManager.proxy.setRadiation(level, at, chunkBefore);
                mist.discard();
            }
            double pigDelta = HbmLivingProps.getRadiation(pig) - pigBefore;
            helper.assertTrue(Math.abs(chunkDelta - 0.4D) < 1.0E-6D,
                    "a UF6 cloud must add 0.2 * 2 = 0.4 rad to its block per tick, added " + chunkDelta);
            helper.assertTrue(Math.abs(pigDelta - 1.0D) < 1.0E-6D,
                    "a UF6 cloud must contaminate a pig by 0.2 * 5 = 1.0 per tick, added " + pigDelta);
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code FT_Poison}: {@code (withering ? wither : poison)} for {@code (int) (5 * 20 * intensity)} ticks at
     * amplifier 0. At intensity 0.8 that is 80 ticks. Mercury is {@code FT_Poison(false, 2)} (poison), schrabidic
     * acid {@code FT_Poison(true, 2)} (wither); the level is never read, as in 1.7.
     */
    public static void poisonMist(GameTestHelper helper) {
        FT_Poison mercury = Fluids.MERCURY.getTrait(FT_Poison.class);
        FT_Poison schrabidic = Fluids.SCHRABIDIC.getTrait(FT_Poison.class);
        helper.assertTrue(mercury != null && !mercury.isWithering() && mercury.getLevel() == 2,
                "mercury must carry FT_Poison(false, 2)");
        helper.assertTrue(schrabidic != null && schrabidic.isWithering() && schrabidic.getLevel() == 2,
                "schrabidic acid must carry FT_Poison(true, 2)");

        LivingEntity poisoned = floating(helper, EntityType.PIG, new BlockPos(1, 3, 1));
        LivingEntity withered = floating(helper, EntityType.PIG, new BlockPos(5, 3, 5));
        EntityMist mercuryMist = mist(helper, Fluids.MERCURY, poisoned);
        EntityMist schrabidicMist = mist(helper, Fluids.SCHRABIDIC, withered);

        helper.runAfterDelay(1, () -> {
            tickAt30(mercuryMist);
            tickAt30(schrabidicMist);
            MobEffectInstance poison = Effects.get(poisoned, MobEffects.POISON);
            MobEffectInstance wither = Effects.get(withered, MobEffects.WITHER);
            helper.assertTrue(poison != null && poison.getDuration() == 80 && poison.getAmplifier() == 0,
                    "mercury mist at intensity 0.8 must poison for 5 * 20 * 0.8 = 80 ticks at amplifier 0, has " + poison);
            helper.assertTrue(Effects.get(poisoned, MobEffects.WITHER) == null, "mercury must not wither");
            helper.assertTrue(wither != null && wither.getDuration() == 80 && wither.getAmplifier() == 0,
                    "schrabidic mist at intensity 0.8 must wither for 80 ticks at amplifier 0, has " + wither);
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code FT_Pheromone}. {@code EntityMist} buffs a glyphid in glyphid pheromone (type 1, multiplier 1):
     * speed II, haste II, resistance I, strength II and fire resistance for 1 * 60 * 20 = 1200 ticks, regeneration
     * I for 1 * 2 * 20 = 40 ticks; a pig in the same cloud gets nothing. The modified pheromone (type 2) buffs a
     * player with the same table at multiplier 2 (2400 / 80). {@code EntityChemical}'s own table: resistance III
     * 2400, speed II 6000, haste V 2400, and for a glyphid in type 1 also strength V 6000, fire resistance 1200 and
     * absorption XX 1200.
     */
    public static void pheromone(GameTestHelper helper) {
        helper.assertTrue(Fluids.PHEROMONE.getTrait(FT_Pheromone.class).getType() == 1
                        && Fluids.PHEROMONE_M.getTrait(FT_Pheromone.class).getType() == 2,
                "PHEROMONE must be FT_Pheromone(1) and PHEROMONE_M FT_Pheromone(2)");

        LivingEntity glyphid = floating(helper, ModNtmEntities.GLYPHID.get(), new BlockPos(1, 3, 1));
        LivingEntity pig = floating(helper, EntityType.PIG, new BlockPos(1, 3, 2));
        EntityMist cloud = mist(helper, Fluids.PHEROMONE, glyphid);

        helper.runAfterDelay(1, () -> {
            Effects.clear(glyphid);
            Effects.clear(pig);
            tickAt30(cloud);
            expect(helper, glyphid, Effects.speed(), 1200, 1, "mist glyphid speed");
            expect(helper, glyphid, Effects.haste(), 1200, 1, "mist glyphid haste");
            expect(helper, glyphid, MobEffects.REGENERATION, 40, 0, "mist glyphid regeneration");
            expect(helper, glyphid, Effects.resistance(), 1200, 0, "mist glyphid resistance");
            expect(helper, glyphid, Effects.strength(), 1200, 1, "mist glyphid strength");
            expect(helper, glyphid, MobEffects.FIRE_RESISTANCE, 1200, 0, "mist glyphid fire resistance");
            helper.assertTrue(pig.getActiveEffects().isEmpty(), "glyphid pheromone must not buff a pig");

            Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
            EntityMist.applyPheromone(player, Fluids.PHEROMONE_M.getTrait(FT_Pheromone.class));
            expect(helper, player, Effects.speed(), 2400, 1, "mist player speed");
            expect(helper, player, MobEffects.REGENERATION, 80, 0, "mist player regeneration");
            expect(helper, player, Effects.strength(), 2400, 1, "mist player strength");
            Player unbuffed = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
            EntityMist.applyPheromone(unbuffed, Fluids.PHEROMONE.getTrait(FT_Pheromone.class));
            helper.assertTrue(unbuffed.getActiveEffects().isEmpty(), "glyphid pheromone mist must not buff a player");

            Effects.clear(glyphid);
            EntityChemical.applyPheromone(glyphid, Fluids.PHEROMONE.getTrait(FT_Pheromone.class));
            expect(helper, glyphid, Effects.resistance(), 2400, 2, "spray glyphid resistance");
            expect(helper, glyphid, Effects.speed(), 6000, 1, "spray glyphid speed");
            expect(helper, glyphid, Effects.haste(), 2400, 4, "spray glyphid haste");
            expect(helper, glyphid, Effects.strength(), 6000, 4, "spray glyphid strength");
            expect(helper, glyphid, MobEffects.FIRE_RESISTANCE, 1200, 0, "spray glyphid fire resistance");
            expect(helper, glyphid, MobEffects.ABSORPTION, 1200, 19, "spray glyphid absorption");
            helper.succeed();
        });
    }

    private static void expect(GameTestHelper helper, LivingEntity entity, Object effect, int duration, int amplifier,
            String what) {
        MobEffectInstance instance = Effects.get(entity, effect);
        helper.assertTrue(instance != null && instance.getDuration() == duration && instance.getAmplifier() == amplifier,
                what + " must be " + duration + " ticks at amplifier " + amplifier + ", has " + instance);
    }

    /**
     * Every simple tag on exactly 1.7's fluid count (Fluids.java declarations), and the predicates that
     * keep private copies of these lists read the traits.
     */
    public static void simpleTraitCounts(GameTestHelper helper) {
        count(helper, FluidTraitSimple.FT_Liquid.class, 113);
        count(helper, FluidTraitSimple.FT_Viscous.class, 53);
        count(helper, FluidTraitSimple.FT_Gaseous.class, 36);
        count(helper, FluidTraitSimple.FT_Gaseous_ART.class, 2);
        count(helper, FluidTraitSimple.FT_Plasma.class, 6);
        count(helper, FluidTraitSimple.FT_Amat.class, 3);
        count(helper, FluidTraitSimple.FT_LeadContainer.class, 2);
        count(helper, FluidTraitSimple.FT_NoContainer.class, 18);
        count(helper, FluidTraitSimple.FT_NoID.class, 9);
        count(helper, FluidTraitSimple.FT_Delicious.class, 4);
        count(helper, FluidTraitSimple.FT_Unsiphonable.class, 5);
        count(helper, FT_VentRadiation.class, 8);
        count(helper, FT_Poison.class, 6);
        count(helper, FT_Pheromone.class, 2);
        helper.assertTrue(Fluids.STELLAR_FLUX.isAntimatter() && Fluids.HYDROGEN.hasTrait(FluidTraitSimple.FT_Gaseous_ART.class)
                        && FluidTraitSimple.isGaseousAny(Fluids.HYDROGEN)
                        && !FluidCombustion.isGaseous(Fluids.HYDROGEN),
                "stellar flux is antimatter; hydrogen is gaseous-at-room-temperature, not FT_Gaseous");
        helper.assertTrue(!FluidCombustion.isLiquidAndViscous(Fluids.NITAN)
                        && FluidCombustion.isLiquidAndViscous(Fluids.OIL),
                "1.7 NITAN is liquid but not viscous; crude oil is both");
        helper.succeed();
    }

    private static void count(GameTestHelper helper, Class<?> trait, int expected) {
        int n = 0;
        // HBM's (and vanilla's) fluids only, so the 1.7 counts hold with an addon's fluids loaded
        for (FluidType type : Fluids.getAll()) if (FluidRecipeTableGameTests.ours(type) && type.hasTrait(trait)) n++;
        helper.assertTrue(n == expected, "1.7 gives " + expected + " fluids " + trait.getSimpleName() + ", found " + n);
    }

    /**
     * 1.7 {@code FluidType.addInfo} walks {@code FluidTrait.traitList} in registration order. Tritium:
     * flammable 5,000 TU and combustible HIGH 10,000 HE ({@code BobMathUtil.getShortNumber}: 5.0k / 10.0k), then
     * vent radiation, then the Shift-only gaseous tag. MUG: the heatable block (400 TU per 1 mB; heat exchanger
     * 100%, PWR 100%, ICF 125%) comes before the PWR moderator, whose core flux is {@code (int) (1.15 * 100 - 100)},
     * 14 in double arithmetic, not 15. Then liquid, then delicious.
     */
    public static void tooltipOrder(GameTestHelper helper) {
        exact(helper, lines(Fluids.TRITIUM, true), List.of(
                "[{hbmfluid.trait.flammable}]",
                "{hbmfluid.trait.provides} 5.0kTU {hbmfluid.trait.perBucket}",
                "[{hbmfluid.trait.combustible}]",
                "{hbmfluid.trait.provides} 10.0kHE {hbmfluid.trait.perBucket}",
                "{hbmfluid.trait.fuelGrade}: {hbmfluid.trait.fuel.high}",
                "[{hbmfluid.trait.radioactive}]",
                "[{hbmfluid.trait.gaseous}]"));
        exact(helper, lines(Fluids.MUG, true), List.of(
                "{hbmfluid.trait.thermalCapacity}: 400 {hbmfluid.trait.perTU} 1mB",
                "[{hbmfluid.trait.heatable}] {hbmfluid.trait.efficiency}: 100%",
                "[{hbmfluid.trait.coolantPWR}] {hbmfluid.trait.efficiency}: 100%",
                "[{hbmfluid.trait.coolantICF}] {hbmfluid.trait.efficiency}: 125%",
                "[{hbmfluid.trait.pwrFluxMultiplier}]",
                "{hbmfluid.trait.pwrFluxCore} +14%",
                "[{hbmfluid.trait.liquid}]",
                "[{hbmfluid.trait.delicious}]"));
        exact(helper, lines(Fluids.SCHRABIDIC, false), List.of(
                "[{hbmfluid.trait.corrosiveStrong}]",
                "[{hbmfluid.trait.radioactive}]",
                "Hold <LSHIFT> to display more info"));
        exact(helper, lines(Fluids.SCHRABIDIC, true), List.of(
                "[{hbmfluid.trait.corrosiveStrong}]",
                "[Toxic Fumes]",
                "[{hbmfluid.trait.radioactive}]",
                "[{hbmfluid.trait.liquid}]"));
        helper.succeed();
    }

    private static void exact(GameTestHelper helper, List<String> actual, List<String> expected) {
        helper.assertTrue(actual.equals(expected), "tooltip must be " + expected + ", is " + actual);
    }

    private static List<String> lines(FluidType type, boolean shift) {
        List<Component> info = new ArrayList<>();
        type.addInfo(info, shift);
        List<String> out = new ArrayList<>();
        for (Component c : info) {
            StringBuilder sb = new StringBuilder();
            flatten(c, sb);
            out.add(sb.toString());
        }
        return out;
    }

    private static void flatten(Component c, StringBuilder sb) {
        if (c.getContents() instanceof TranslatableContents t) {
            sb.append('{').append(t.getKey()).append('}');
        } else {
            c.getContents().visit(s -> {
                sb.append(s);
                return Optional.empty();
            });
        }
        for (Component sibling : c.getSiblings()) flatten(sibling, sb);
    }
}
