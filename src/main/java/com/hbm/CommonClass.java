package com.hbm;

import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.main.MainRegistry;

public class CommonClass {

    /**
     * Upper bound for ATTACK_DAMAGE and MAX_HEALTH. 1.7's {@code SharedMonsterAttributes} declared both with a
     * maximum of {@code Double.MAX_VALUE}; modern vanilla clamps them at 2048 and 1024, below the crucible's 5000
     * damage and the UFO's 20,000 / Balls-O-Tron's 15,000 health. One million is 50x the largest 1.7 value, stays
     * exact in the float that health is stored and synced as (integers up to 2^24), and is AttributeFix's bound, so
     * a pack sees the same cap with or without it. {@code raiseMax} never lowers a bound another mod raised further.
     */
    public static final double ATTRIBUTE_CAP = 1_000_000.0D;

    public static void init() {

        // Before any entity exists (see Golem Core Attributes.raiseMax): vanilla attribute bounds below 1.7 content.
        dev.golem.core.world.Attributes.raiseMax(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, ATTRIBUTE_CAP);
        dev.golem.core.world.Attributes.raiseMax(
                net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, ATTRIBUTE_CAP);

        // Populate all DeferredRegisters. Loaders flush them afterwards (NeoForge via RegisterEvent,
        // Fabric via DeferredRegister.flushAll). This must run before registration is flushed.
        MainRegistry.bootstrap();

        // The power graph's per-tick round, and the channels that make a parked conductor face live again.
        PowerNetwork.init();
        // The fluid graph's round. Its chunk and level hooks are the ones PowerNetwork already installed.
        com.hbm.api.fluidmk2.FluidNetwork.init();

        // 1.7 MainRegistry's ChestGenHooks.addItem calls: HBM items in vanilla dungeon/mineshaft/temple chests.
        com.hbm.itempool.VanillaChestLoot.init();

        // Only a run started with -Dhbm.generateIds does anything here; see IdConstants.
        com.hbm.lib.IdConstants.install();
    }
}
