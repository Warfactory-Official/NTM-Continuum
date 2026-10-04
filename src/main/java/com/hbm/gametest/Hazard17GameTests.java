package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.EntityHbmProps;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.type.IHazardType;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.potion.ModEffects;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7.10 values for the hazard registry, the hazard types and the radiation/lung handlers (fix wave 2026-10-03,
 * History §19ej-1C). Every expected number is computed from the 1.7 formula, not read back from the port.
 */
public final class Hazard17GameTests {

    private Hazard17GameTests() {}

    public static final GameTests.Function HAZARD_REGISTRY_17 =
            HazardGameTests.TESTS.function("hazard_registry_17", 40, () -> Hazard17GameTests::hazardRegistry17);
    public static final GameTests.Function HAZARD_TYPES_17 =
            HazardGameTests.TESTS.function("hazard_types_17", 40, () -> Hazard17GameTests::hazardTypes17);
    public static final GameTests.Function RADAWAY_17 =
            HazardGameTests.TESTS.function("radaway_17", 60, () -> Hazard17GameTests::radaway17);
    public static final GameTests.Function LUNG_DISEASE_17 =
            HazardGameTests.TESTS.function("lung_disease_17", 60, () -> Hazard17GameTests::lungDisease17);
    public static final GameTests.Function HAZARD_REGISTRY_DUMP =
            HazardGameTests.TESTS.function("hazard_registry_dump", 20, () -> Hazard17GameTests::hazardRegistryDump);

    static void init() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, id));
    }

    private static double level(ItemStack stack, IHazardType type) {
        return HazardSystem.getHazardLevelFromStack(stack, type);
    }

    private static void expect(GameTestHelper helper, String id, IHazardType type, double expected) {
        Item item = item(id);
        helper.assertTrue(item != Items.AIR, "missing item hbm:" + id);
        double got = level(new ItemStack(item), type);
        helper.assertTrue(Math.abs(got - expected) < 1e-4 * Math.max(1D, expected),
                "hbm:" + id + " " + HazardRegistry.getTypeId(type) + " must be " + expected + " (1.7), got " + got);
    }

    /** OreDictManager frames (every entry cloned by the shape multiplier) and HazardRegistry.registerItems. */
    public static void hazardRegistry17(GameTestHelper helper) {
        // shape scaling of secondary entries: Pu-238 nugget RAD 10*0.1, HOT 3*0.1
        expect(helper, "nugget_pu238", HazardRegistry.RADIATION, 1.0);
        expect(helper, "nugget_pu238", HazardRegistry.HOT, 0.3);
        expect(helper, "nugget_pb209", HazardRegistry.BLINDING, 5.0);
        expect(helper, "nugget_pb209", HazardRegistry.HOT, 0.7);
        expect(helper, "nugget_co60", HazardRegistry.HOT, 0.1);
        // a block counts once (tag and item copy share the dictionary mutex) and scales every entry by 10
        expect(helper, "block_uranium", HazardRegistry.RADIATION, 3.5);
        expect(helper, "block_schrabidium", HazardRegistry.RADIATION, 150.0);
        expect(helper, "block_schrabidium", HazardRegistry.BLINDING, 500.0);
        expect(helper, "block_white_phosphorus", HazardRegistry.HOT, 50.0);
        helper.assertTrue(Math.abs(HazardSystem.getRawRadsFromBlock(
                        ((net.minecraft.world.item.BlockItem) item("block_uranium")).getBlock()) - 3.5) < 1e-4,
                "the block's in-world emission source must read the frame value 3.5");
        // families 1.7 had and the port lacked
        expect(helper, "ingot_pu_mix", HazardRegistry.RADIATION, 6.25);
        expect(helper, "nugget_am_mix", HazardRegistry.RADIATION, 0.9);
        expect(helper, "powder_cs137", HazardRegistry.RADIATION, 60.0);
        expect(helper, "powder_cs137", HazardRegistry.HOT, 9.0);
        expect(helper, "powder_cs137", HazardRegistry.HYDROACTIVE, 9.0);
        expect(helper, "powder_xe135_tiny", HazardRegistry.RADIATION, 375.0);
        expect(helper, "powder_at209", HazardRegistry.HOT, 60.0);
        expect(helper, "ingot_mud", HazardRegistry.RADIATION, 1.0);
        expect(helper, "ingot_asbestos", HazardRegistry.ASBESTOS, 1.0);
        expect(helper, "powder_asbestos", HazardRegistry.ASBESTOS, 3.0);
        expect(helper, "lithium", HazardRegistry.HYDROACTIVE, 1.0);
        expect(helper, "powder_strontium", HazardRegistry.HYDROACTIVE, 3.0);
        expect(helper, "ingot_phosphorus", HazardRegistry.HOT, 5.0);
        expect(helper, "ore_nether_plutonium", HazardRegistry.RADIATION, 7.5);
        expect(helper, "ore_uranium", HazardRegistry.RADIATION, 0.0); // blacklisted
        // a crystal is an item registration in 1.7, never a frame shape: no blinding on schrabidium crystals
        expect(helper, "crystal_schrabidium", HazardRegistry.RADIATION, 150.0);
        expect(helper, "crystal_schrabidium", HazardRegistry.BLINDING, 0.0);
        // HazardRegistry.registerItems
        expect(helper, "demon_core_closed", HazardRegistry.RADIATION, 100_000.0);
        expect(helper, "insert_polonium", HazardRegistry.RADIATION, 100.0);
        expect(helper, "stick_c4", HazardRegistry.EXPLOSIVE, 2.5);
        expect(helper, "yellow_barrel", HazardRegistry.RADIATION, 150.0);
        expect(helper, "gem_rad", HazardRegistry.RADIATION, 25.0);
        expect(helper, "solinium_core", HazardRegistry.RADIATION, 14.0);
        expect(helper, "solinium_core", HazardRegistry.BLINDING, 45.0);
        expect(helper, "holotape_damaged", HazardRegistry.DIGAMMA, 1000.0);
        expect(helper, "watz_pellet_schrabidium", HazardRegistry.RADIATION, 60.0);
        expect(helper, "powder_coal", HazardRegistry.COAL, 3.0);
        helper.assertTrue(level(new ItemStack(Items.PUMPKIN_PIE), HazardRegistry.EXPLOSIVE) == 1.0,
                "1.7 registers the pumpkin pie as EXPLOSIVE 1");
        ItemStack pile = com.hbm.blocks.machine.pile.ModPileBlocks.pile_rod.get().of(ItemPileRodMK2.EnumPileRod.RA226BE);
        helper.assertTrue(Math.abs(level(pile, HazardRegistry.RADIATION) - 33.75) < 1e-4,
                "Ra226Be pile rod must be rabe*billet*3 = 33.75, got " + level(pile, HazardRegistry.RADIATION));

        // armour radiation resistance (1.7 HazmatRegistry.initDefault) and the Faraday list
        helper.assertTrue(Math.abs(HazmatRegistry.getResistance(new ItemStack(item("gas_mask"))) - 0.07) < 1e-9,
                "gas mask resistance must be 0.07");
        helper.assertTrue(Math.abs(HazmatRegistry.getResistance(new ItemStack(item("gas_mask_m65"))) - 0.095) < 1e-9,
                "M65 resistance must be 0.095");
        helper.assertTrue(Math.abs(HazmatRegistry.getResistance(new ItemStack(Items.IRON_CHESTPLATE)) - 0.0225 * 0.4) < 1e-9,
                "iron chestplate resistance must be 0.0225 * 0.4");
        helper.assertTrue(ArmorUtil.isFaradayArmor(new ItemStack(item("t51_plate"))), "T-51 counts as Faraday armour");
        helper.succeed();
    }

    /** The hazard types' per-application amounts, with the hazard tick's rate folded in. */
    public static void hazardTypes17(GameTestHelper helper) {
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        int rate = IHazardType.hazardRate;
        try {
            // digamma: level / 20 per tick, the stack size ignored
            HbmLivingProps.setDigamma(pig, 0);
            new HazardEntry(HazardRegistry.DIGAMMA, 1D).applyHazard(new ItemStack(item("holotape_damaged"), 64), pig);
            double digamma = HbmLivingProps.getDigamma(pig);
            helper.assertTrue(Math.abs(digamma - rate / 20D) < 1e-9,
                    "digamma must not scale with the stack: expected " + rate / 20D + ", got " + digamma);
            HbmLivingProps.setDigamma(pig, 0);

            // hot: setFire(ceil(level)) seconds; a 0.3 nugget burns one second, not rate seconds
            pig.clearFire();
            new HazardEntry(HazardRegistry.HOT, 0.3D).applyHazard(new ItemStack(item("nugget_pu238")), pig);
            helper.assertTrue(pig.getRemainingFireTicks() == 20,
                    "HOT 0.3 must set 20 fire ticks (1.7 setFire(1)), got " + pig.getRemainingFireTicks());
            pig.clearFire();

            // blinding: ceil(level) ticks, padded by one hazard interval
            new HazardEntry(HazardRegistry.BLINDING, 0.5D).applyHazard(new ItemStack(item("nugget_pb209")), pig);
            var blind = Effects.get(pig, MobEffects.BLINDNESS);
            helper.assertTrue(blind != null && blind.getDuration() == 1 + rate,
                    "BLINDING 0.5 must last ceil(0.5) + " + rate + " ticks, got " + (blind == null ? "none" : blind.getDuration()));

            // radiation cap: incrementRadiation clamps at 2500 (1.7), not 25,000,000
            HbmLivingProps.setRadiation(pig, 2400);
            HbmLivingProps.incrementRadiation(pig, 500);
            helper.assertTrue(HbmLivingProps.getRadiation(pig) == 2500D,
                    "radiation must cap at 2500, got " + HbmLivingProps.getRadiation(pig));
            HbmLivingProps.setRadiation(pig, 0);
            helper.succeed();
        } finally {
            pig.discard();
        }
    }

    /** 1.7 HbmPotion.radaway: level + 1 RAD removed every tick. */
    public static void radaway17(GameTestHelper helper) {
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        // Mutation makes the pig radiation-immune to the environment (neighbouring plots irradiate their chunks),
        // while RadAway removes rads straight from the props, as 1.7's incrementRadiation did.
        Effects.addRegistered(pig, ModEffects.MUTATION.get(), 200, 0);
        HbmLivingProps.setRadiation(pig, 100);
        Effects.addRegistered(pig, ModEffects.RADAWAY.get(), 200, 1); // RadAway II: 2 RAD/t
        long start = helper.getLevel().getGameTime();
        helper.runAfterDelay(10, () -> {
            long ticks = helper.getLevel().getGameTime() - start;
            double rads = HbmLivingProps.getRadiation(pig);
            double expected = 100D - 2D * ticks;
            pig.discard();
            helper.assertTrue(Math.abs(rads - expected) <= 2.0001D,
                    "RadAway II must remove 2 RAD a tick: after " + ticks + " ticks expected ~" + expected + ", got " + rads);
            helper.succeed();
        });
    }

    /** 1.7 maxBlacklung 2*60*60*20, healing below half of it, weakness III above 75% combined damage. */
    public static void lungDisease17(GameTestHelper helper) {
        helper.assertTrue(EntityHbmProps.MAX_BLACKLUNG == 2 * 60 * 60 * 20,
                "max black lung must be 1.7's 144000, got " + EntityHbmProps.MAX_BLACKLUNG);
        LivingEntity healing = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        LivingEntity sick = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 2, 1));
        LivingEntity doomed = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 3));
        int max = EntityHbmProps.MAX_BLACKLUNG;
        int below = (int) (max * 0.4); // under half: heals one a tick
        int above = (int) (max * 0.8); // over half: stays; over 75%: weakness III
        HbmLivingProps.setBlackLung(healing, below);
        HbmLivingProps.setBlackLung(sick, above);
        // the 1.12.2 fork's kill threshold (60*60*20) must no longer kill
        HbmLivingProps.setBlackLung(doomed, 60 * 60 * 20);
        helper.assertTrue(doomed.isAlive() && HbmLivingProps.getBlackLung(doomed) == 60 * 60 * 20,
                "72000 black lung (half of 1.7's maximum) must not kill");
        HbmLivingProps.setBlackLung(doomed, max);
        helper.assertTrue(!doomed.isAlive() || doomed.getHealth() <= 0F, "1.7's maximum black lung must kill");
        helper.runAfterDelay(10, () -> {
            int healed = HbmLivingProps.getBlackLung(healing);
            int stayed = HbmLivingProps.getBlackLung(sick);
            var weakness = Effects.get(sick, MobEffects.WEAKNESS);
            boolean nausea = Effects.has(sick, MobEffects.NAUSEA);
            healing.discard();
            sick.discard();
            helper.assertTrue(healed < below && healed >= below - 12,
                    "black lung under half the maximum must heal a tick at a time: " + below + " -> " + healed);
            helper.assertTrue(stayed == above, "black lung over half the maximum must not heal: " + above + " -> " + stayed);
            helper.assertTrue(weakness != null && weakness.getAmplifier() == 2,
                    "80% lung damage must give weakness III (1.7), got " + weakness);
            helper.assertTrue(!nausea, "nausea starts above 95% combined lung damage in 1.7");
            helper.succeed();
        });
    }

    /**
     * Not an assertion: with {@code HBM_HAZARD_DUMP} set to a file path, writes every item's resolved hazards (type,
     * summed evaluated level) for the 1.7 registry comparison script. Without it, does nothing.
     */
    public static void hazardRegistryDump(GameTestHelper helper) {
        String target = System.getenv("HBM_HAZARD_DUMP");
        if (target == null || target.isBlank()) {
            helper.succeed();
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            Map<String, Double> sums = new LinkedHashMap<>();
            for (HazardEntry entry : HazardSystem.getHazardsFromStack(stack)) {
                String type = String.valueOf(HazardRegistry.getTypeId(entry.type));
                sums.merge(type, IHazardModifier.evalAllModifiers(stack, null, entry.baseLevel, entry.mods), Double::sum);
            }
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            for (Map.Entry<String, Double> e : sums.entrySet()) lines.add(id + "\t" + e.getKey() + "\t" + e.getValue());
            if (item instanceof net.minecraft.world.item.BlockItem block) {
                Block b = block.getBlock();
                double raw = HazardSystem.getRawRadsFromBlock(b);
                if (raw > 0) lines.add(id + "\tblock_emission\t" + raw);
            }
        }
        try {
            Files.write(Path.of(target), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        helper.succeed();
    }
}
