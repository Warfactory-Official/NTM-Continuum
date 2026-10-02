package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import com.hbm.hazard.HazardData;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import com.hbm.inventory.ModPwrMenus;
import com.hbm.items.ModPwrItems;
import com.hbm.tileentity.ModPwrBlockEntities;

/** One common bootstrap hook for the pressurized water reactor's feature-local registries. */
public final class ModPwrContent {
    private ModPwrContent() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);
    private static final Reg REG = Reg.of(SOUNDS);

    /** 1.7 {@code NTMSounds.REACTOR_GEIGER_LOOP} ({@code hbm:block.reactorLoop}), the loaded-core loop. */
    public static final RegistryObject<SoundEvent> REACTOR_LOOP = REG.sound("block.reactor_loop");

    public static void init() {
        ModPwrBlocks.init();
        ModPwrItems.init();
        ModPwrBlockEntities.init();
        ModPwrMenus.init();
    }

    /**
     * 1.7 {@code HazardRegistry.registerPWRFuel}: a fresh rod radiates twice its billet, hot and depleted rods
     * ten times that, and a hot rod is also HOT 5. Runs from {@link com.hbm.hazard.Hazards#init()} after the
     * item registries flushed.
     */
    public static void registerHazards() {
        float billet = HazardRegistry.billet;
        registerPWRFuel(EnumPWRFuel.MEU, HazardRegistry.uf * billet * 2);
        registerPWRFuel(EnumPWRFuel.HEU233, HazardRegistry.u233 * billet * 2);
        registerPWRFuel(EnumPWRFuel.HEU235, HazardRegistry.u235 * billet * 2);
        registerPWRFuel(EnumPWRFuel.MEN, HazardRegistry.npf * billet * 2);
        registerPWRFuel(EnumPWRFuel.HEN237, HazardRegistry.np237 * billet * 2);
        registerPWRFuel(EnumPWRFuel.MOX, HazardRegistry.mox * billet * 2);
        registerPWRFuel(EnumPWRFuel.MEP, HazardRegistry.purg * billet * 2);
        registerPWRFuel(EnumPWRFuel.HEP239, HazardRegistry.pu239 * billet * 2);
        registerPWRFuel(EnumPWRFuel.HEP241, HazardRegistry.pu241 * billet * 2);
        registerPWRFuel(EnumPWRFuel.MEA, HazardRegistry.amrg * billet * 2);
        registerPWRFuel(EnumPWRFuel.HEA242, HazardRegistry.am242 * billet * 2);
        registerPWRFuel(EnumPWRFuel.HES326, HazardRegistry.sa326 * billet * 2);
        registerPWRFuel(EnumPWRFuel.HES327, HazardRegistry.sa327 * billet * 2);
        registerPWRFuel(EnumPWRFuel.BFB_AM_MIX, HazardRegistry.amrg * billet);
        registerPWRFuel(EnumPWRFuel.BFB_PU241, HazardRegistry.pu241 * billet);
    }

    private static void registerPWRFuel(EnumPWRFuel fuel, float baseRad) {
        HazardSystem.register(ModPwrItems.fuel(fuel), new HazardData().addEntry(HazardRegistry.RADIATION, baseRad));
        HazardSystem.register(ModPwrItems.hot(fuel), new HazardData().addEntry(HazardRegistry.RADIATION, baseRad * 10)
                .addEntry(HazardRegistry.HOT, 5));
        HazardSystem.register(ModPwrItems.depleted(fuel), new HazardData().addEntry(HazardRegistry.RADIATION, baseRad * 10));
    }
}
