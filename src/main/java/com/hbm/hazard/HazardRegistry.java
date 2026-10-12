package com.hbm.hazard;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.hazard.type.HazardTypeAsbestos;
import com.hbm.hazard.type.HazardTypeBlinding;
import com.hbm.hazard.type.HazardTypeCoal;
import com.hbm.hazard.type.HazardTypeCold;
import com.hbm.hazard.type.HazardTypeContaminating;
import com.hbm.hazard.type.HazardTypeDigamma;
import com.hbm.hazard.type.HazardTypeExplosive;
import com.hbm.hazard.type.HazardTypeHot;
import com.hbm.hazard.type.HazardTypeHydroactive;
import com.hbm.hazard.type.HazardTypeRadiation;
import com.hbm.hazard.type.HazardTypeToxic;
import com.hbm.hazard.type.IHazardType;
import com.hbm.hazard.transformer.HazardTransformerCustom;
import com.hbm.hazard.transformer.HazardTransformerRadiationNBT;
import com.hbm.hazard.transformer.HazardTransformerZirnox;
import com.hbm.hazard.transformer.IHazardTransformer;
import com.hbm.hazard.modifier.HazardModifierFuelRadiation;
import com.hbm.hazard.modifier.HazardModifierRBMKHot;
import com.hbm.hazard.modifier.HazardModifierRBMKRadiation;
import com.hbm.Constants;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers every item/material hazard, ported from 1.12.2's {@code HazardRegistry} (+ the radioactive
 * material families that the old code registered from {@code OreDictManager.rad(...)} chains — here
 * folded into {@link #registerMat}). All the isotope floats and shape multipliers are copied verbatim.
 *
 * <p>The specification is 1.7.10's {@code HazardRegistry.registerItems} plus the hazard frames of its
 * {@code OreDictManager.registerOres} ({@link #registerMaterialFrames}); {@code HazardGameTests.hazard_registry_17}
 * pins a sample of the values and the 2026-10-03 fix wave compared every registration (History §19ej-1C).
 */
@SuppressWarnings("unused")
public class HazardRegistry {

    // ---- isotope radiation constants (RAD/s), verbatim ----
    public static final float gen_S = 10_000F, gen_H = 2_000F, gen_10D = 100F, gen_100D = 80F,
            gen_1Y = 50F, gen_10Y = 30F, gen_100Y = 10F, gen_1K = 7.5F, gen_10K = 6.25F, gen_100K = 5F,
            gen_1M = 2.5F, gen_10M = 1.5F, gen_100M = 1F, gen_1B = 0.5F, gen_10B = 0.1F;

    public static final float co60 = 30.0F, sr90 = 15.0F, tc99 = 2.75F, i131 = 150.0F, xe135 = 1250.0F,
            cs137 = 20.0F, au198 = 500.0F, pb209 = 10000.0F, at209 = 7500.0F, po210 = 75.0F, ra226 = 7.5F,
            ac227 = 30.0F, th232 = 0.1F, thf = 1.75F, u = 0.35F, u233 = 5.0F, u235 = 1.0F, u238 = 0.25F,
            uf = 0.5F, uzh = 0.125F, np237 = 2.5F, npf = 1.5F, pu = 7.5F, purg = 6.25F, pu238 = 10.0F,
            pu239 = 5.0F, pu240 = 7.5F, pu241 = 25.0F, puf = 4.25F, am241 = 8.5F, am242 = 9.5F, amrg = 9.0F,
            amf = 4.75F, mox = 2.5F, sa326 = 15.0F, sa327 = 17.5F, saf = 5.85F, sas3 = 5F, gh336 = 5.0F,
            mud = 1.0F, radsource_mult = 3.0F;
    public static final float pobe = po210 * radsource_mult, rabe = ra226 * radsource_mult, pube = pu238 * radsource_mult;
    public static final float bf = 300_000.0F, bfb = 500_000.0F;
    public static final float sr = sa326 * 0.1F, sb = sa326 * 0.1F, trx = 25.0F, trn = 0.1F, wst = 15.0F,
            wstv = 7.5F, yc = u, fo = 10F;

    // ---- shape multipliers, verbatim ----
    public static final float nugget = 0.1F, ingot = 1.0F, gem = 1.0F, plate = ingot, plateCast = plate * 3,
            powder_mult = 3.0F, powder = ingot * powder_mult, powder_tiny = nugget * powder_mult, ore = ingot,
            block = 10.0F, crystal = block, billet = 0.5F, rtg = billet * 3, rod = 0.5F, rod_dual = rod * 2,
            rod_quad = rod * 4, rod_rbmk = rod * 8;

    /** Named hazard types, in registration order. Declaration order is the built-ins' order. */
    private static final Map<Identifier, IHazardType> TYPES = new LinkedHashMap<>();
    private static final Map<IHazardType, Identifier> TYPE_IDS = new IdentityHashMap<>();

    /**
     * Names a hazard type, so a hazard added from elsewhere can be found by whoever wants to extend
     * it — a recipe, a command, another mod's registration pass. Returns the type, to be used as the
     * initialiser of the constant that holds it.
     *
     * <p>Names are not part of the fold: {@link HazardSystem} keys everything off type identity, so
     * an unnamed type still works everywhere. What a name buys is being reachable without the field.
     */
    public static <T extends IHazardType> T registerType(final Identifier id, final T type) {
        final IHazardType existing = TYPES.putIfAbsent(id, type);
        if (existing != null && existing != type) {
            throw new IllegalStateException("Hazard type " + id + " is already registered to " + existing);
        }
        TYPE_IDS.putIfAbsent(type, id);
        return type;
    }

    public static @Nullable IHazardType getType(final Identifier id) {
        return TYPES.get(id);
    }

    public static @Nullable Identifier getTypeId(final IHazardType type) {
        return TYPE_IDS.get(type);
    }

    /** Every named hazard type, built-in and third-party alike. */
    public static Map<Identifier, IHazardType> types() {
        return Collections.unmodifiableMap(TYPES);
    }

    private static Identifier hazardId(final String path) {
        return dev.golem.core.Ids.of(Constants.MOD_ID, path);
    }

    // ---- hazard types ----
    public static final IHazardType RADIATION = registerType(hazardId("radiation"), new HazardTypeRadiation());
    public static final IHazardType CONTAMINATING = registerType(hazardId("contaminating"), new HazardTypeContaminating());
    public static final IHazardType DIGAMMA = registerType(hazardId("digamma"), new HazardTypeDigamma());
    public static final IHazardType HOT = registerType(hazardId("hot"), new HazardTypeHot());
    public static final IHazardType BLINDING = registerType(hazardId("blinding"), new HazardTypeBlinding());
    public static final IHazardType ASBESTOS = registerType(hazardId("asbestos"), new HazardTypeAsbestos());
    public static final IHazardType COAL = registerType(hazardId("coal"), new HazardTypeCoal());
    public static final IHazardType HYDROACTIVE = registerType(hazardId("hydroactive"), new HazardTypeHydroactive());
    public static final IHazardType EXPLOSIVE = registerType(hazardId("explosive"), new HazardTypeExplosive());
    public static final IHazardType TOXIC = registerType(hazardId("toxic"), new HazardTypeToxic());
    public static final IHazardType COLD = registerType(hazardId("cold"), new HazardTypeCold());

    public static void registerTrafos() {
        HazardSystem.trafos.add(new HazardTransformerRadiationNBT());
        HazardSystem.trafos.add(new HazardTransformerZirnox());
        HazardSystem.trafos.add(HazardTransformerCustom.INSTANCE);
        // 1.7 registered cell_tritium and cell_sas3 as items; here they are one component-backed cell, whose hazards are
        // the hbm:cell_hazards fluid data map, keyed by the fluid it holds.
        HazardSystem.trafos.add(IHazardTransformer.post((stack, entries) -> {
            if (!(stack.getItem() instanceof com.hbm.items.special.ItemCell)) return;
            com.hbm.inventory.fluid.FluidType fluid = com.hbm.items.special.ItemCell.getFluidType(stack);
            if (fluid == null || fluid == com.hbm.inventory.fluid.Fluids.NONE) return;
            com.hbm.hazard.HazardSpec spec = com.hbm.api.data.HazardDataMaps.CELL_HAZARDS.get(com.hbm.inventory.fluid.Fluids.toVanilla(fluid));
            if (spec != null) entries.addAll(spec.toData().entries);
        }));
        // 1.7 HazardRegistry:350-355 (the MK2 pile rods) are item entries of hbm:hazards: one item per rod type here.
        // RadiationContainer is added by ModUtilityToolContent; ForgeFluid / RadiationME have no port counterpart.
    }

    // The item and material hazards that 1.7 registered here ({@code registerItems}, the OreDictManager hazard frames,
    // the ZIRNOX / plate / RBMK / Watz fuels and the hazard blocks) are the hbm:hazards item data map
    // (data/hbm/value_maps/item/hazards.json, HazardDataMaps.HAZARDS), exported from this class's former Java registration
    // on 2026-10-08 and pinned by the GameTest value_map_hazards_equal_17. The constants above are the numbers it was built
    // from; they stay because transformers and modifiers still use some of them.
}
