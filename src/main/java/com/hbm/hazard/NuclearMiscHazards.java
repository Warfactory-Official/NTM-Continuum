package com.hbm.hazard;

import com.hbm.api.hazard.HazardEntry;
import com.hbm.api.hazard.IHazardModifier;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemBreedingRod.RodSize;
import com.hbm.items.machine.ItemDepletedFuel;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static com.hbm.hazard.HazardRegistry.*;
import com.hbm.items.ModNuclearMiscItems;

/** The 1.7 {@code HazardRegistry} entries for this family, registered after the item registry flushes. */
public final class NuclearMiscHazards {
    private NuclearMiscHazards() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;

        HazardSystem.register(ModNuclearMiscItems.DEMON_CORE_OPEN.get(), new HazardData().addEntry(RADIATION, 5F));

        registerBreedingRodRadiation(BreedingRodType.TRITIUM, 0.001F);
        registerBreedingRodRadiation(BreedingRodType.CO60, co60);
        registerBreedingRodRadiation(BreedingRodType.RA226, ra226);
        registerBreedingRodRadiation(BreedingRodType.AC227, ac227);
        registerBreedingRodRadiation(BreedingRodType.TH232, th232);
        registerBreedingRodRadiation(BreedingRodType.THF, thf);
        registerBreedingRodRadiation(BreedingRodType.U235, u235);
        registerBreedingRodRadiation(BreedingRodType.NP237, np237);
        registerBreedingRodRadiation(BreedingRodType.U238, u238);
        registerBreedingRodRadiation(BreedingRodType.PU238, pu238); //it's in a container :)
        registerBreedingRodRadiation(BreedingRodType.PU239, pu239);
        registerBreedingRodRadiation(BreedingRodType.RGP, purg);
        registerBreedingRodRadiation(BreedingRodType.WASTE, wst);
        registerBreedingRodRadiation(BreedingRodType.URANIUM, u);

        registerOtherWaste(ModNuclearMiscItems.WASTE_PLATE_U233, wst * ingot * 13F);
        registerOtherWaste(ModNuclearMiscItems.WASTE_PLATE_U235, wst * ingot * 10F);
        registerOtherWaste(ModNuclearMiscItems.WASTE_PLATE_MOX, wst * ingot * 16F);
        registerOtherWaste(ModNuclearMiscItems.WASTE_PLATE_PU239, wst * ingot * 13.5F);
        registerOtherWaste(ModNuclearMiscItems.WASTE_PLATE_SA326, wst * ingot * 10F);
        registerRadSourceWaste(ModNuclearMiscItems.WASTE_PLATE_RA226BE, pobe * nugget * 3);
        registerRadSourceWaste(ModNuclearMiscItems.WASTE_PLATE_PU238BE, pube * nugget * 1);

        HazardSystem.register(ModNuclearMiscItems.PILE_ROD_URANIUM.get(), new HazardData().addEntry(RADIATION, u * billet * 3));
        // 1.7 without 528: purg + pu239 + u (all * billet)
        HazardSystem.register(ModNuclearMiscItems.PILE_ROD_PU239.get(), new HazardData().addEntry(RADIATION, purg * billet + pu239 * billet + u * billet));
        HazardSystem.register(ModNuclearMiscItems.PILE_ROD_PLUTONIUM.get(), new HazardData().addEntry(RADIATION, purg * billet * 2 + u * billet));
        HazardSystem.register(ModNuclearMiscItems.PILE_ROD_SOURCE.get(), new HazardData().addEntry(RADIATION, rabe * billet * 3));
    }

    public static void registerBreedingRodRadiation(BreedingRodType type, float base) {
        HazardSystem.register(ModNuclearMiscItems.rod(RodSize.SINGLE, type), new HazardData().addEntry(RADIATION, base));
        HazardSystem.register(ModNuclearMiscItems.rod(RodSize.DUAL, type), new HazardData().addEntry(RADIATION, base * rod_dual));
        HazardSystem.register(ModNuclearMiscItems.rod(RodSize.QUAD, type), new HazardData().addEntry(RADIATION, base * rod_quad));
    }

    /** 1.7 subtype 0: 7.5% of the radiation; subtype 1 (hot): the full radiation plus HOT 5. */
    private static void registerOtherWaste(RegistryObject<? extends Item> waste, float base) {
        HazardData data = new HazardData();
        data.addEntry(new HazardEntry(RADIATION, base).addMod(new HotScale(0.075F)));
        data.addEntry(new HazardEntry(HOT, 5F).addMod(new HotScale(0F)));
        HazardSystem.register(waste.get(), data);
    }

    /** 1.7: the same radiation cold or hot; HOT 5 only while hot. */
    private static void registerRadSourceWaste(RegistryObject<? extends Item> waste, float base) {
        HazardData data = new HazardData();
        data.addEntry(new HazardEntry(RADIATION, base));
        data.addEntry(new HazardEntry(HOT, 5F).addMod(new HotScale(0F)));
        HazardSystem.register(waste.get(), data);
    }

    /** Scales an entry down to {@code coldFactor} while the plate is not hot. */
    private record HotScale(float coldFactor) implements IHazardModifier {
        @Override
        public double modify(ItemStack stack, LivingEntity holder, double level) {
            return ItemDepletedFuel.isHot(stack) ? level : level * coldFactor;
        }
    }
}
