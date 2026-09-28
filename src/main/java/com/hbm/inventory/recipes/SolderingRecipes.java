package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModUpgradeItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.hbm.inventory.OreDictManager.*;
import static com.hbm.items.ItemEnums.EnumCircuitType.*;

/**
 * Soldering station recipes — a faithful, feasibility-limited port of the 1.12.2 {@code SolderingRecipes}.
 * A recipe has three grouped ingredient sets — up to 3 <b>toppings</b> (slots 0–2), up to 2 <b>pcb</b>
 * (slots 3–4) and 1 <b>solder</b> (slot 5) — an optional fluid, and an output; {@link #getRecipe} exact-set
 * matches the six input slots against those groups.
 *
 * <p><b>What's ported:</b> the circuit chain that produces the higher-tier circuits from the ported
 * {@link ModIntermediateItems intermediates} + lead fine-wire (autogen) + fluids: ANALOG, BASIC (the
 * canonical "Integrated Circuit Board"), ADVANCED, CAPACITOR_BOARD, BISMOID, QUANTUM — plus the
 * machine-upgrade chains (speed/effect/power/fortune tiers I→II→III, the 1.12.2 tier-1 recipes +
 * {@code addFirstUpgrade}/{@code addSecondUpgrade}). The CONTROLLER* circuits are in 1.7's {@code if(no528)}. <b>Deferred:</b> the
 * afterburn upgrade chain (its machines aren't ported), and the {@code enableLBSM} "simple crafting"
 * ingredient reductions (the non-default full-cost values are used).
 */
public final class SolderingRecipes {

    private SolderingRecipes() {}

    public static final List<SolderingRecipe> recipes = new ArrayList<>();
    /** The union of every group's ingredients — the TE/container use these to validate slot placement. */
    public static final Set<AStack> toppings = new HashSet<>();
    public static final Set<AStack> pcb = new HashSet<>();
    public static final Set<AStack> solder = new HashSet<>();
    private static boolean initialized = false;

    /** One soldering recipe. Arrays, so a plain class (not a record) — matched by {@link #getRecipe}. */
    public static final class SolderingRecipe {
        public final ItemStack output;
        public final int duration;
        public final long consumption;
        public final FluidStack fluid; // may be null
        public final AStack[] toppings;
        public final AStack[] pcb;
        public final AStack[] solder;

        SolderingRecipe(ItemStack output, int duration, long consumption, FluidStack fluid,
                        AStack[] toppings, AStack[] pcb, AStack[] solder) {
            this.output = output;
            this.duration = duration;
            this.consumption = consumption;
            this.fluid = fluid;
            this.toppings = toppings;
            this.pcb = pcb;
            this.solder = solder;
        }
    }

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        // circuits (higher tiers, soldered from lower circuits + lead fine-wire (+ a fluid at higher tiers))
        add(out(ANALOG),          100,     100, null,
                a(cir(VACUUM_TUBE, 3), cir(CAPACITOR, 2)),      a(cir(PCB, 4)),                          a(PB.wire().count(4)));
        add(out(BASIC),           200,     250, null,       // the "Integrated Circuit Board"
                a(cir(CHIP, 4)),                                a(cir(PCB, 4)),                          a(PB.wire().count(4)));
        add(out(ADVANCED),        300,   1_000, new FluidStack(Fluids.SULFURIC_ACID, 1_000),
                a(cir(CHIP, 16), cir(CAPACITOR, 4)),            a(cir(PCB, 8), RUBBER.ingot().count(2)), a(PB.wire().count(8)));
        add(out(CAPACITOR_BOARD), 200,     300, new FluidStack(Fluids.PEROXIDE, 250),
                a(cir(CAPACITOR_TANTALIUM, 3)),                 a(cir(PCB, 1)),                          a(PB.wire().count(3)));
        add(out(BISMOID),         400,  10_000, new FluidStack(Fluids.SOLVENT, 1_000),
                a(cir(CHIP_BISMOID, 4), cir(CHIP, 16), cir(CAPACITOR, 24)),
                a(cir(PCB, 12), ANY_HARDPLASTIC.ingot().count(2)),                                       a(PB.wire().count(12)));
        add(out(QUANTUM),         400, 100_000, new FluidStack(Fluids.HELIUM4, 1_000),
                a(cir(CHIP_QUANTUM, 4), cir(CHIP_BISMOID, 16), cir(ATOMIC_CLOCK, 4)),
                a(cir(PCB, 16), ANY_HARDPLASTIC.ingot().count(4)),                                       a(PB.wire().count(16)));

        // 1.7 "COMPUTERS": the control units, only in 1.7's `if(no528)` (528 mode makes them in the precision
        // assembler). Non-LBSM counts, as for every other recipe here.
        if (!com.hbm.config.GeneralConfig.enable528) {
            add(out(CONTROLLER),          400,  15_000, new FluidStack(Fluids.PERFLUOROMETHYL, 1_000),
                    a(cir(CHIP, 32), cir(CAPACITOR, 32), cir(CAPACITOR_TANTALIUM, 16)),
                    a(cir(CONTROLLER_CHASSIS, 1), RUBBER.ingot().count(4)),                              a(PB.wire().count(16)));
            add(out(CONTROLLER_ADVANCED), 600,  25_000, new FluidStack(Fluids.PERFLUOROMETHYL, 4_000),
                    a(cir(CHIP_BISMOID, 16), cir(CAPACITOR_TANTALIUM, 48), cir(ATOMIC_CLOCK, 1)),
                    a(cir(CONTROLLER_CHASSIS, 1), ANY_HARDPLASTIC.ingot().count(4)),                     a(PB.wire().count(24)));
            add(out(CONTROLLER_QUANTUM),  600, 250_000, new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 6_000),
                    a(cir(CHIP_QUANTUM, 16), cir(CHIP_BISMOID, 48), cir(ATOMIC_CLOCK, 8)),
                    a(cir(CONTROLLER_ADVANCED, 2), new ComparableStack(ModUpgradeItems.upgrade_speed_3.get())),
                    a(PB.wire().count(32)));
        }

        // machine upgrades (tier 1: template + a signature dust; tiers 2/3 via the 1.12.2 upgrade chains).
        add(new ItemStack(ModUpgradeItems.upgrade_speed_1.get()), 200, 1_000, null,
                a(cir(VACUUM_TUBE, 4), cir(CAPACITOR, 1)),
                a(new ComparableStack(ModItems.upgrade_template.get()), MINGRADE.dust().count(4)),       a());
        add(new ItemStack(ModUpgradeItems.upgrade_effect_1.get()), 200, 1_000, null,
                a(cir(VACUUM_TUBE, 4), cir(CAPACITOR, 1)),
                a(new ComparableStack(ModItems.upgrade_template.get()), EMERALD.dust().count(4)), a());
        add(new ItemStack(ModUpgradeItems.upgrade_power_1.get()), 200, 1_000, null,
                a(cir(VACUUM_TUBE, 4), cir(CAPACITOR, 1)),
                a(new ComparableStack(ModItems.upgrade_template.get()), GOLD.dust().count(4)),           a());
        add(new ItemStack(ModUpgradeItems.upgrade_fortune_1.get()), 200, 1_000, null,
                a(cir(VACUUM_TUBE, 4), cir(CAPACITOR, 1)),
                a(new ComparableStack(ModItems.upgrade_template.get()), NB.dust().count(4)),             a());

        // 1.7 `if(no528)`: in 528 mode the tier II/III upgrades come from the precision assembler instead
        if (!com.hbm.config.GeneralConfig.enable528) {
        addFirstUpgrade(ModUpgradeItems.upgrade_speed_1.get(), ModUpgradeItems.upgrade_speed_2.get());
        addSecondUpgrade(ModUpgradeItems.upgrade_speed_2.get(), ModUpgradeItems.upgrade_speed_3.get());
        addFirstUpgrade(ModUpgradeItems.upgrade_effect_1.get(), ModUpgradeItems.upgrade_effect_2.get());
        addSecondUpgrade(ModUpgradeItems.upgrade_effect_2.get(), ModUpgradeItems.upgrade_effect_3.get());
        addFirstUpgrade(ModUpgradeItems.upgrade_power_1.get(), ModUpgradeItems.upgrade_power_2.get());
        addSecondUpgrade(ModUpgradeItems.upgrade_power_2.get(), ModUpgradeItems.upgrade_power_3.get());
        addFirstUpgrade(ModUpgradeItems.upgrade_fortune_1.get(), ModUpgradeItems.upgrade_fortune_2.get());
        addSecondUpgrade(ModUpgradeItems.upgrade_fortune_2.get(), ModUpgradeItems.upgrade_fortune_3.get());
        }
        // 1.7 SolderingRecipes: the satellite ID chip
        add(new ItemStack(com.hbm.items.ModSatelliteItems.sat_chip.get()), 100, 100, null,
                a(cir(CHIP, 8), cir(CAPACITOR, 2)),              a(cir(PCB, 2)),                          a(PB.wire().count(4)));
        RadarUtilityAssemblyRecipes.registerSoldering(); // forcefield radius / health upgrades
    }

    /** Tier I → II (the 1.12.2 {@code addFirstUpgrade}, non-LBSM ingredient counts). */
    private static void addFirstUpgrade(Item lower, Item higher) {
        add(new ItemStack(higher), 300, 10_000, null,
                a(cir(CHIP, 8), cir(CAPACITOR, 4)),
                a(new ComparableStack(lower), ANY_PLASTIC.ingot().count(4)),                             a());
    }

    /** Tier II → III (the 1.12.2 {@code addSecondUpgrade}, non-LBSM ingredient counts). */
    private static void addSecondUpgrade(Item lower, Item higher) {
        add(new ItemStack(higher), 400, 25_000, new FluidStack(Fluids.SOLVENT, 500),
                a(cir(CHIP, 16), cir(CAPACITOR, 16)),
                a(new ComparableStack(lower), RUBBER.ingot().count(4)),                                  a());
    }

    private static void add(ItemStack output, int duration, long consumption, FluidStack fluid,
                            AStack[] top, AStack[] board, AStack[] sold) {
        recipes.add(new SolderingRecipe(output, duration, consumption, fluid, top, board, sold));
        java.util.Collections.addAll(toppings, top);
        java.util.Collections.addAll(pcb, board);
        java.util.Collections.addAll(solder, sold);
    }

    private static AStack[] a(AStack... stacks) { return stacks; }

    /** A count of a circuit intermediate as a recipe INPUT (matcher). */
    private static ComparableStack cir(EnumCircuitType type, int count) {
        return new ComparableStack(ModIntermediateItems.circuit(type), count);
    }

    /** One circuit intermediate as a recipe OUTPUT (an actual stack). */
    private static ItemStack out(EnumCircuitType type) {
        return ModIntermediateItems.circuit(type, 1);
    }

    /** Exact-set match on the 6 input slots (0–2 toppings, 3–4 pcb, 5 solder). */
    public static SolderingRecipe getRecipe(ItemStack[] inputs) {
        if (!initialized) register();
        for (SolderingRecipe recipe : recipes) {
            if (matches(new ItemStack[]{inputs[0], inputs[1], inputs[2]}, recipe.toppings)
                    && matches(new ItemStack[]{inputs[3], inputs[4]}, recipe.pcb)
                    && matches(new ItemStack[]{inputs[5]}, recipe.solder)) return recipe;
        }
        return null;
    }

    /** Every required ingredient is satisfied by a distinct slot with enough count, and no slot is left over. */
    private static boolean matches(ItemStack[] slots, AStack[] required) {
        boolean[] used = new boolean[slots.length];
        for (AStack req : required) {
            boolean found = false;
            for (int i = 0; i < slots.length; i++) {
                if (!used[i] && req.matchesRecipe(slots[i], true) && slots[i].getCount() >= req.stacksize) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        for (int i = 0; i < slots.length; i++) if (!used[i] && !slots[i].isEmpty()) return false;
        return true;
    }
}
