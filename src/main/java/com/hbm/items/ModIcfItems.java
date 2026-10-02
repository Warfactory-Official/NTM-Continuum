package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.items.machine.ItemICFPellet.EnumICFFuel;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.mojang.serialization.Codec;
import dev.golem.core.dejson.Tints;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The ICF's own item: the filled fuel pellet. 1.7 kept its fuel pair, muon flag and depletion in the stack's
 * NBT ({@code type1}, {@code type2}, {@code muon}, {@code depletion}); those are four data components here.
 * The empty and depleted pellets are ordinary generated items already.
 */
public final class ModIcfItems {

    private ModIcfItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);
    /** 1.7 {@code ItemICFPellet.getColorFromItemStack} for a pellet with no NBT: the deuterium/tritium average. */
    private static final int DEFAULT_PELLET_TINT = 0x1414AE;

    /** 1.7 {@code type1}: the first fuel's {@link EnumICFFuel} ordinal. */
    public static final GolemComponent<Integer> PELLET_TYPE1 = GolemComponent.integer(Constants.MOD_ID, "icf_type1");
    /** 1.7 {@code type2}: the second fuel's ordinal. */
    public static final GolemComponent<Integer> PELLET_TYPE2 = GolemComponent.integer(Constants.MOD_ID, "icf_type2");
    /** 1.7 {@code muon}: muon-catalysed, a quarter of the fusing difficulty. */
    public static final GolemComponent<Boolean> PELLET_MUON = GolemComponent.of(Constants.MOD_ID, "icf_muon", Codec.BOOL);
    /** 1.7 {@code depletion}: the TU of laser heat the pellet has absorbed. */
    public static final GolemComponent<Long> PELLET_DEPLETION = GolemComponent.longValue(Constants.MOD_ID, "icf_depletion");

    // 1.7: setMaxStackSize(1), controlTab
    public static final RegistryObject<ItemICFPellet> icf_pellet =
            REG.item("icf_pellet", ItemICFPellet::new, p -> p.stacksTo(1))
                    // 1.7 two passes: pass 0 the tinted icf_pellet_bg, pass 1 the untinted icf_pellet shell over it
                    .transform(Presets.sprite("hbm:items/icf_pellet_bg", "hbm:items/icf_pellet")
                            .tints(Tints.customModelData(0, DEFAULT_PELLET_TINT), Tints.constant(-1)));

    static {
        // 1.7 getSubItems: five preset pairs, the last two muon-catalysed
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> List.<ItemStack>of(
                ItemICFPellet.setup(EnumICFFuel.DEUTERIUM, EnumICFFuel.TRITIUM, false),
                ItemICFPellet.setup(EnumICFFuel.HELIUM3, EnumICFFuel.HELIUM4, false),
                ItemICFPellet.setup(EnumICFFuel.LITHIUM, EnumICFFuel.OXYGEN, false),
                ItemICFPellet.setup(EnumICFFuel.SODIUM, EnumICFFuel.CHLORINE, true),
                ItemICFPellet.setup(EnumICFFuel.BERYLLIUM, EnumICFFuel.CALCIUM, true)));
    }

    public static void init() {}
}
