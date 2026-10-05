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
        // 1.7 registered cell_tritium and cell_sas3 as items; here they are one component-backed cell.
        HazardSystem.trafos.add(IHazardTransformer.post((stack, entries) -> {
            if (!(stack.getItem() instanceof com.hbm.items.special.ItemCell)) return;
            com.hbm.inventory.fluid.FluidType fluid = com.hbm.items.special.ItemCell.getFluidType(stack);
            if (fluid == com.hbm.inventory.fluid.Fluids.TRITIUM) {
                entries.add(new HazardEntry(RADIATION, 0.001F));
            } else if (fluid == com.hbm.inventory.fluid.Fluids.SAS3) {
                entries.add(new HazardEntry(RADIATION, sas3));
                entries.add(new HazardEntry(BLINDING, 60F));
            }
        }));
        // 1.7 HazardRegistry:350-355: the MK2 pile rods were metadata stacks, here one item with a type component.
        HazardSystem.trafos.add(IHazardTransformer.post((stack, entries) -> {
            com.hbm.items.machine.ItemPileRodMK2.EnumPileRod type = com.hbm.items.machine.ItemPileRodMK2.typeOf(stack);
            if (type == null) return;
            float rad = switch (type) {
                case RA226BE -> rabe * billet * 3;
                case PO210BE -> pobe * billet * 3;
                case NU -> u * billet * 3;
                case PU239 -> pu239 * billet * 3;
                case RGP -> purg * billet * 3;
                case WASTE -> wst * billet * 3;
                default -> 0F; // ZR, THORIUM, THORIUM_FUEL: no 1.7 entry
            };
            if (rad > 0) entries.add(new HazardEntry(RADIATION, rad));
        }));
        // RadiationContainer is added by ModUtilityToolContent; ForgeFluid / RadiationME have no port counterpart.
    }

    public static void registerItems() {

        // ---- explosives (vanilla + hbm sticks) ----
        reg(net.minecraft.world.item.Items.GUNPOWDER, makeData(EXPLOSIVE, 1F));
        reg(net.minecraft.world.item.Items.TNT, makeData(EXPLOSIVE, 4F));
        // 1.7 HazardRegistry:164, verbatim: a pumpkin pie is EXPLOSIVE 1 (it goes off when its holder burns).
        reg(net.minecraft.world.item.Items.PUMPKIN_PIE, makeData(EXPLOSIVE, 1F));
        regItem("ball_dynamite", makeData(EXPLOSIVE, 2F));
        regItem("stick_dynamite", makeData(EXPLOSIVE, 1F));
        regItem("stick_tnt", makeData(EXPLOSIVE, 1.5F));
        regItem("stick_semtex", makeData(EXPLOSIVE, 2.5F));
        regItem("stick_c4", makeData(EXPLOSIVE, 2.5F));
        regItem("cordite", makeData(EXPLOSIVE, 2F));
        regItem("ballistite", makeData(EXPLOSIVE, 1F));

        // ---- coal / lignite dust: 1.7 registered these on the bare dictionary keys (no mutex) ----
        HazardSystem.register(cTag(MaterialShapes.DUST, "coal"), makeData(COAL, powder));
        HazardSystem.register(cTag(MaterialShapes.DUSTTINY, "coal"), makeData(COAL, powder_tiny));
        HazardSystem.register(cTag(MaterialShapes.DUST, "lignite"), makeData(COAL, powder));
        HazardSystem.register(cTag(MaterialShapes.DUSTTINY, "lignite"), makeData(COAL, powder_tiny));

        // ---- 1.7 HazardRegistry:180-195 ----
        regItem("insert_polonium", makeData(RADIATION, 100F));
        regItem("demon_core_closed", makeData(RADIATION, 100_000F));
        regItem("lamp_demon", makeData(RADIATION, 100_000F));
        // roughly the amount of the balefire shard diluted in 250mB of rocket fuel
        regItem("solid_fuel_bf", makeData(RADIATION, 1000F));
        regItem("solid_fuel_presto_bf", makeData(RADIATION, 2000F));
        regItem("solid_fuel_presto_triplet_bf", makeData(RADIATION, 6000F));

        // ---- balefire / high-level sources ----
        regItem("cell_balefire", makeData(RADIATION, 50F));
        regItem("powder_balefire", makeData(RADIATION, 500F).addEntry(CONTAMINATING, bf * powder));
        regItem("egg_balefire_shard", makeData(RADIATION, bf * nugget));
        regItem("egg_balefire", makeData(RADIATION, bf * ingot));

        // ---- generic nuclear waste ----
        regItem("nuclear_waste", makeData(RADIATION, wst * ingot).addEntry(CONTAMINATING, wst * powder));
        regItem("billet_nuclear_waste", makeData(RADIATION, wst * billet).addEntry(CONTAMINATING, wst * billet));
        regItem("nuclear_waste_tiny", makeData(RADIATION, wst * nugget).addEntry(CONTAMINATING, wst * powder_tiny));
        regItem("nuclear_waste_vitrified", makeData(RADIATION, wstv * ingot).addEntry(CONTAMINATING, wstv * powder));
        regItem("nuclear_waste_vitrified_tiny", makeData(RADIATION, wstv * nugget).addEntry(CONTAMINATING, wstv * powder_tiny));
        regItem("scrap_nuclear", makeData(RADIATION, 1F));
        regItem("yellow_barrel", makeData(RADIATION, wst * ingot * 10));
        regItem("ancient_scrap", makeData(RADIATION, 150F));
        regItem("block_corium_cobble", makeData(RADIATION, 150F));
        regItem("gem_rad", makeData(RADIATION, 25F));
        regItem("trinitite", makeData(RADIATION, trn * ingot).addEntry(CONTAMINATING, 5));

        // The old metadata families share one hazard per registered item; the component only selects the isotope name.
        regItem("nuclear_waste_long", makeData(RADIATION, 5F));
        regItem("nuclear_waste_long_tiny", makeData(RADIATION, 0.5F));
        regItem("nuclear_waste_short", makeData().addEntry(RADIATION, 30F).addEntry(HOT, 5F));
        regItem("nuclear_waste_short_tiny", makeData().addEntry(RADIATION, 3F).addEntry(HOT, 5F));
        regItem("nuclear_waste_long_depleted", makeData(RADIATION, 0.5F));
        regItem("nuclear_waste_long_depleted_tiny", makeData(RADIATION, 0.05F));
        regItem("nuclear_waste_short_depleted", makeData(RADIATION, 3F));
        regItem("nuclear_waste_short_depleted_tiny", makeData(RADIATION, 0.3F));

        registerZirnoxFuel();
        registerResearchPlateFuel();
        registerRBMKFuel();

        // ---- debris ----
        regItem("debris_graphite", makeData().addEntry(RADIATION, 70F).addEntry(HOT, 5F));
        regItem("debris_metal", makeData(RADIATION, 5F));
        regItem("debris_fuel", makeData().addEntry(RADIATION, 500F).addEntry(HOT, 5F));
        regItem("debris_concrete", makeData(RADIATION, 30F));
        regItem("debris_exchanger", makeData(RADIATION, 25F));
        regItem("debris_shrapnel", makeData(RADIATION, 2.5F));
        regItem("debris_element", makeData(RADIATION, 100F));

        // ---- reactor fuels (item families that exist) ----
        regFuel("uranium_fuel", uf);
        regFuel("plutonium_fuel", puf);
        regFuel("thorium_fuel", thf);
        regFuelNoBlock("neptunium_fuel", npf);
        regFuel("mox_fuel", mox);
        regFuelNoBlock("americium_fuel", amf);
        regItem("billet_uzh", makeData(RADIATION, uzh * billet));

        regItem("nugget_schrabidium_fuel", makeData().addEntry(RADIATION, saf * nugget).addEntry(BLINDING, 5F * nugget));
        regItem("billet_schrabidium_fuel", makeData().addEntry(RADIATION, saf * billet).addEntry(BLINDING, 5F * billet));
        regItem("ingot_schrabidium_fuel", makeData().addEntry(RADIATION, saf * ingot).addEntry(BLINDING, 5F * ingot));
        regItem("block_schrabidium_fuel", makeData().addEntry(RADIATION, saf * block).addEntry(BLINDING, 5F * block));

        regItem("nugget_hes", makeData(RADIATION, saf * nugget));
        regItem("billet_hes", makeData(RADIATION, saf * billet));
        regItem("ingot_hes", makeData(RADIATION, saf * ingot));
        regItem("nugget_les", makeData(RADIATION, saf * nugget));
        regItem("billet_les", makeData(RADIATION, saf * billet));
        regItem("ingot_les", makeData(RADIATION, saf * ingot));

        regItem("billet_balefire_gold", makeData(RADIATION, au198 * billet));
        regItem("billet_flashlead", makeData().addEntry(RADIATION, pb209 * 1.25F * billet).addEntry(HOT, 7F));
        regItem("billet_po210be", makeData(RADIATION, pobe * billet));
        regItem("billet_ra226be", makeData(RADIATION, rabe * billet));
        regItem("billet_pu238be", makeData(RADIATION, pube * billet));

        // ---- yellowcake / caesium / misc powders ----
        regItem("powder_yellowcake", makeData(RADIATION, yc * powder).addEntry(CONTAMINATING, yc * powder));
        regItem("powder_caesium", makeData().addEntry(HYDROACTIVE, 1F).addEntry(HOT, 3F));

        // ---- asbestos / coltan ----
        regItem("brick_asbestos", makeData(ASBESTOS, 1F));
        regItem("tile_lab_broken", makeData(ASBESTOS, 1F));
        regItem("powder_coltan_ore", makeData(ASBESTOS, 3F));

        // ---- crystals (1.7 HazardRegistry:483-490) ----
        regItem("crystal_uranium", makeData(RADIATION, u * crystal));
        regItem("crystal_thorium", makeData(RADIATION, th232 * crystal));
        regItem("crystal_plutonium", makeData(RADIATION, pu * crystal));
        regItem("crystal_schraranium", makeData(RADIATION, sr * crystal));
        regItem("crystal_schrabidium", makeData(RADIATION, sa326 * crystal));
        regItem("crystal_phosphorus", makeData(HOT, 2F * crystal));
        regItem("crystal_lithium", makeData(HYDROACTIVE, 1F * crystal));
        regItem("crystal_trixite", makeData(RADIATION, trx * crystal));

        // ---- nuke parts (1.7 HazardRegistry:493-510) ----
        regItem("boy_propellant", makeData(EXPLOSIVE, 2F));
        regItem("gadget_core", makeData(RADIATION, pu239 * nugget * 10));
        regItem("boy_target", makeData(RADIATION, u235 * ingot * 2));
        regItem("boy_bullet", makeData(RADIATION, u235 * ingot));
        regItem("man_core", makeData(RADIATION, pu239 * nugget * 10));
        regItem("mike_core", makeData(RADIATION, u238 * nugget * 10));
        regItem("tsar_core", makeData(RADIATION, pu239 * nugget * 15));
        regItem("fleija_propellant", makeData().addEntry(RADIATION, 15F).addEntry(EXPLOSIVE, 8F).addEntry(BLINDING, 50F));
        regItem("fleija_core", makeData(RADIATION, 10F));
        regItem("solinium_propellant", makeData(EXPLOSIVE, 10F));
        regItem("solinium_core", makeData().addEntry(RADIATION, sa327 * nugget * 8).addEntry(BLINDING, 45F));
        regItem("nuke_fstbmb", makeData(DIGAMMA, 0.01F));
        // holotape_image HOLO_RESTORED (DIGAMMA 1) is a component variant: HazardTransformerRadiationContainer
        regItem("holotape_damaged", makeData(DIGAMMA, 1_000F));

        // ---- Watz pellets (1.7 HazardRegistry:444-453; fresh pellets only, metadata variants then) ----
        for (com.hbm.items.machine.EnumWatzType type : com.hbm.items.machine.EnumWatzType.VALUES) {
            float rad = switch (type) {
                case SCHRABIDIUM -> sa326 * ingot * 4;
                case HES, MES, LES -> saf * ingot * 4;
                case HEN -> np237 * ingot * 4;
                case MEU -> uf * ingot * 4;
                case MEP -> purg * ingot * 4;
                case DU -> u238 * ingot * 4;
                case NQD -> u235 * ingot * 4;
                case NQR -> pu239 * ingot * 4;
                default -> 0F; // LEAD, BORON: no entry
            };
            if (rad > 0) HazardSystem.register(com.hbm.items.ModWatzItems.fresh(type), makeData(RADIATION, rad));
        }

        // ---- hazard BLOCK items (ModHazardBlocks); the block reads this back via getRawRadsFromBlock ----
        registerHazardBlocks();

        // ---- the 1.7 OreDictManager hazard frames (registerOres), shape by shape ----
        registerMaterialFrames();

        // Blacklist: raw ores of thorium/uranium never irradiate (they're common worldgen)
        HazardSystem.blacklist(TH232.tag(MaterialShapes.ORE));
        HazardSystem.blacklist(U.tag(MaterialShapes.ORE));
    }

    /**
     * 1.7 {@code OreDictManager.registerOres} hazard frames, line for line. A frame's hazards are cloned by the
     * multiplier of each shape it registers ({@code DictFrame.registerHazards}: {@code hazard.clone(hazMult)}), so a
     * nugget of Pu-238 is HOT 0.3 and a block of schrabidium BLINDING 500 — every entry scales, not only radiation.
     * Only the shapes the 1.7 frame registered get data: a crystal or plate the port also tags is not hazardous unless
     * 1.7 said so.
     *
     * <p>Each shape is registered twice, both with the 1.7 dictionary mutex bit: on the {@code c:} tag (the ore
     * dictionary key, so another mod's item under the same key is covered as in 1.7) and on the 1.7 item itself. The
     * tag entry is read first, so an item in both yields its own copy and counts once; the item copy covers a port item
     * the tag happens to miss and is what {@link HazardSystem#getRawRadsFromBlock} reads for a block's in-world
     * emission, which 1.7 took from the same lookup ({@code BlockHazard.onBlockAdded}).
     */
    private static void registerMaterialFrames() {
        // RADIOACTIVE (OreDictManager 344-371). U and TH232 ores are blacklisted below, as in 1.7.
        frame(U).rad(u).nugget("nugget_uranium").billet("billet_uranium").ingot("ingot_uranium").dust("powder_uranium")
                .block("block_uranium");
        frame(U233).rad(u233).nugget("nugget_u233").billet("billet_u233").ingot("ingot_u233").block("block_u233");
        frame(U235).rad(u235).nugget("nugget_u235").billet("billet_u235").ingot("ingot_u235").block("block_u235");
        frame(U238).rad(u238).nugget("nugget_u238").billet("billet_u238").ingot("ingot_u238").block("block_u238");
        frame(TH232).rad(th232).nugget("nugget_th232").billet("billet_th232").ingot("ingot_th232").dust("powder_thorium")
                .block("block_thorium");
        frame(PU).rad(pu).nugget("nugget_plutonium").billet("billet_plutonium").ingot("ingot_plutonium")
                .dust("powder_plutonium").block("block_plutonium").ore("ore_nether_plutonium");
        frame(PURG).rad(purg).nugget("nugget_pu_mix").billet("billet_pu_mix").ingot("ingot_pu_mix").block("block_pu_mix");
        frame(PU238).rad(pu238).hot(3F).nugget("nugget_pu238").billet("billet_pu238").ingot("ingot_pu238")
                .block("block_pu238");
        frame(PU239).rad(pu239).nugget("nugget_pu239").billet("billet_pu239").ingot("ingot_pu239").block("block_pu239");
        frame(PU240).rad(pu240).nugget("nugget_pu240").billet("billet_pu240").ingot("ingot_pu240").block("block_pu240");
        frame(PU241).rad(pu241).nugget("nugget_pu241").billet("billet_pu241").ingot("ingot_pu241");
        frame(AM241).rad(am241).nugget("nugget_am241").billet("billet_am241").ingot("ingot_am241");
        frame(AM242).rad(am242).nugget("nugget_am242").billet("billet_am242").ingot("ingot_am242");
        frame(AMRG).rad(amrg).nugget("nugget_am_mix").billet("billet_am_mix").ingot("ingot_am_mix");
        frame(NP237).rad(np237).nugget("nugget_neptunium").billet("billet_neptunium").ingot("ingot_neptunium")
                .dust("powder_neptunium").block("block_neptunium");
        frame(PO210).rad(po210).hot(3F).nugget("nugget_polonium").billet("billet_polonium").ingot("ingot_polonium")
                .dust("powder_polonium").block("block_polonium");
        frame(TC99).rad(tc99).nugget("nugget_technetium").billet("billet_technetium").ingot("ingot_technetium");
        frame(RA226).rad(ra226).nugget("nugget_ra226").billet("billet_ra226").ingot("ingot_ra226").dust("powder_ra226")
                .block("block_ra226");
        frame(AC227).rad(ac227).nugget("nugget_actinium").billet("billet_actinium").ingot("ingot_actinium")
                .dust("powder_actinium").block("block_actinium").dustSmall("powder_actinium_tiny");
        frame(CO60).rad(co60).hot(1F).nugget("nugget_co60").billet("billet_co60").ingot("ingot_co60").dust("powder_co60");
        frame(AU198).rad(au198).hot(5F).nugget("nugget_au198").billet("billet_au198").ingot("ingot_au198")
                .dust("powder_au198");
        frame(PB209).rad(pb209).blinding(50F).hot(7F).nugget("nugget_pb209").billet("billet_pb209").ingot("ingot_pb209");
        frame(SA326).rad(sa326).blinding(50F).nugget("nugget_schrabidium").billet("billet_schrabidium")
                .ingot("ingot_schrabidium").dust("powder_schrabidium").plate("plate_schrabidium")
                .plateCast("plate_cast_schrabidium").block("block_schrabidium")
                .ore("ore_schrabidium", "ore_gneiss_schrabidium", "ore_nether_schrabidium", "ore_sellafield_schrabidium");
        frame(SA327).rad(sa327).blinding(50F).nugget("nugget_solinium").billet("billet_solinium").ingot("ingot_solinium")
                .block("block_solinium");
        frame(SBD).rad(sb).blinding(50F).ingot("ingot_schrabidate").dust("powder_schrabidate").block("block_schrabidate");
        frame(SRN).rad(sr).blinding(50F).ingot("ingot_schraranium").block("block_schraranium");
        frame(GH336).rad(gh336).nugget("nugget_gh336").billet("billet_gh336").ingot("ingot_gh336");
        frame(MUD).rad(mud).ingot("ingot_mud");

        // OreDictManager 421: asbestos (the basalt and resource-stone ore variants were metadata in 1.7)
        frame(com.hbm.inventory.OreDictManager.ASBESTOS).asbestos(1F).ingot("ingot_asbestos").dust("powder_asbestos").block("block_asbestos")
                .ore("ore_asbestos", "ore_gneiss_asbestos", "ore_basalt_asbestos", "stone_resource_asbestos");

        // HAZARDS, MISC (OreDictManager 450-451). hazIngot registers the ingot key with no item of its own: in 1.7 it
        // caught the autogen ingot_raw of the material, which the port tags as c:ingots/sodium.
        frame(LI).hydro(1F).ingot("lithium").dustSmall("powder_lithium_tiny").dust("powder_lithium").block("block_lithium")
                .ore("ore_gneiss_lithium");
        frame(NA).hydro(1F).hazIngot().dust("powder_sodium");

        // PHOSPHORUS (456). The port's "phosphorus" material is the red one, so white phosphorus is keyed by item only.
        frame((String) null).hot(5F).ingot("ingot_phosphorus").block("block_white_phosphorus");

        // FISSION FRAGMENTS (486-491). The isotopes have no port material, only the c: tags named after their items.
        frame(SR).hot(1F).hydro(1F).hazIngot().dust("powder_strontium");
        frame("sr90").rad(sr90).hot(1F).hydro(1F).dustSmall("powder_sr90_tiny").dust("powder_sr90").ingot("ingot_sr90")
                .billet("billet_sr90").nugget("nugget_sr90");
        frame("i131").rad(i131).hot(1F).dustSmall("powder_i131_tiny").dust("powder_i131");
        frame("xe135").rad(xe135).hot(10F).dustSmall("powder_xe135_tiny").dust("powder_xe135");
        frame("cs137").rad(cs137).hot(3F).hydro(3F).dustSmall("powder_cs137_tiny").dust("powder_cs137");
        frame("at209").rad(at209).hot(20F).dust("powder_at209");
    }

    private static Frame frame(NTMMaterial mat) {
        return new Frame(mat.name);
    }

    /** A frame keyed by the {@code c:} tag name alone, or by item only when {@code tagName} is null. */
    private static Frame frame(@Nullable String tagName) {
        return new Frame(tagName);
    }

    /** 1.7 {@code OreDictManager.DictFrame}'s hazard half. */
    private static final class Frame {
        private final @Nullable String tagName;
        private final java.util.List<HazardEntry> hazards = new java.util.ArrayList<>();

        private Frame(@Nullable String tagName) {
            this.tagName = tagName;
        }

        Frame rad(float level) { return haz(RADIATION, level); }
        Frame hot(float level) { return haz(HOT, level); }
        Frame blinding(float level) { return haz(BLINDING, level); }
        Frame asbestos(float level) { return haz(ASBESTOS, level); }
        Frame hydro(float level) { return haz(HYDROACTIVE, level); }

        private Frame haz(IHazardType type, float level) {
            hazards.add(new HazardEntry(type, level));
            return this;
        }

        Frame nugget(String... ids) { return shape(MaterialShapes.NUGGET, nugget, ids); }
        Frame billet(String... ids) { return shape(MaterialShapes.BILLET, billet, ids); }
        Frame ingot(String... ids) { return shape(MaterialShapes.INGOT, ingot, ids); }
        Frame dustSmall(String... ids) { return shape(MaterialShapes.DUSTTINY, powder_tiny, ids); }
        Frame dust(String... ids) { return shape(MaterialShapes.DUST, powder, ids); }
        Frame plate(String... ids) { return shape(MaterialShapes.PLATE, plate, ids); }
        Frame plateCast(String... ids) { return shape(MaterialShapes.CASTPLATE, plateCast, ids); }
        Frame block(String... ids) { return shape(MaterialShapes.BLOCK, block, ids); }
        Frame ore(String... ids) { return shape(MaterialShapes.ORE, ore, ids); }
        /** 1.7 {@code hazIngot()}: the ingot key at ingot strength, no item of HBM's own. */
        Frame hazIngot() { return shape(MaterialShapes.INGOT, ingot); }

        private Frame shape(MaterialShapes shape, float mult, String... ids) {
            if (tagName != null) HazardSystem.register(cTag(shape, tagName), data(mult));
            for (String id : ids) regItem(id, data(mult));
            return this;
        }

        private HazardData data(float mult) {
            HazardData data = new HazardData().setMutex(0b1);
            for (HazardEntry hazard : hazards) data.addEntry(hazard.clone(mult));
            return data;
        }
    }

    /**
     * Radiation on the hazard BLOCK items (values = the old {@code OreDictManager} {@code .rad(...).block(...)}
     * chains + the directly‑registered fuel/special blocks; {@code block = 10}). {@link
     * com.hbm.blocks.ModHazardBlocks#initHazards()} reads these back via {@code getRawRadsFromBlock}.
     */
    private static void registerHazardBlocks() {
        // The metal storage blocks are registered by their 1.7 frames (registerMaterialFrames, block shape).
        // block_au198 has no 1.7 counterpart (AU198 had no block shape); the port keeps it radioactive at the frame's
        // block strength, every entry scaled as a 1.7 frame would have scaled it.
        regItem("block_au198", makeData().addEntry(RADIATION, au198 * block).addEntry(HOT, 5F * block));
        // fuel blocks
        regItem("block_uranium_fuel", makeData(RADIATION, uf * block));
        regItem("block_plutonium_fuel", makeData(RADIATION, puf * block));
        regItem("block_thorium_fuel", makeData(RADIATION, thf * block));
        regItem("block_mox_fuel", makeData(RADIATION, mox * block));
        regItem("block_schrabidium_fuel", makeData().addEntry(RADIATION, saf * block).addEntry(BLINDING, 5F * block));
        // specials
        regItem("block_trinitite", makeData(RADIATION, trn * block));
        regItem("block_corium", makeData(RADIATION, 150F));
        // block_magnetized_tungsten has no hazard (0 radiation) — no entry.

        // fallout (the ash item + the layer block's item; old HazardRegistry lines 481-482).
        // Note: the fallout LAYER emits no chunk radiation in 1.7 (BlockFallout: walking on it applies the
        // radiation effect, clicking it contaminates) — these entries are the held-item hazards only.
        regItem("falloutitem", makeData(RADIATION, fo * powder).addEntry(CONTAMINATING, fo));
        regItem("fallout", makeData(RADIATION, fo * powder * 2));
        // sellafite radgem ore (the one sellafield block with an item hazard in the old registry)
        regItem("ore_sellafield_radgem", makeData(RADIATION, 25F));
    }

    /** Registers a fuel family (nugget/billet/ingot/block) by id prefix, if the items exist. */
    private static void regFuel(String name, float base) {
        regItem("nugget_" + name, makeData(RADIATION, base * nugget));
        regItem("billet_" + name, makeData(RADIATION, base * billet));
        regItem("ingot_" + name, makeData(RADIATION, base * ingot));
        regItem("block_" + name, makeData(RADIATION, base * block));
    }

    private static void regFuelNoBlock(String name, float base) {
        regItem("nugget_" + name, makeData(RADIATION, base * nugget));
        regItem("billet_" + name, makeData(RADIATION, base * billet));
        regItem("ingot_" + name, makeData(RADIATION, base * ingot));
    }

    private static void registerZirnoxFuel() {
        for (com.hbm.items.machine.ZirnoxFuel fuel : com.hbm.items.machine.ZirnoxFuel.values()) {
            if (!com.hbm.items.ModZirnoxItems.hasDepleted(fuel)) continue;
            HazardData rod = makeData(RADIATION, fuel.depletedRadiation);
            if (fuel == com.hbm.items.machine.ZirnoxFuel.LES) rod.addEntry(BLINDING, 20F);
            HazardSystem.register(com.hbm.items.ModZirnoxItems.depleted(fuel), rod);

            HazardData waste = makeData(RADIATION, fuel.depletedRadiation * 0.0375F)
                    .addEntry(CONTAMINATING, fuel == com.hbm.items.machine.ZirnoxFuel.ZFB_MOX ? 10F : 15F);
            if (fuel == com.hbm.items.machine.ZirnoxFuel.THORIUM) {
                waste = makeData(RADIATION, fuel.depletedRadiation * 0.0375F).addEntry(CONTAMINATING, 10F);
            } else if (fuel == com.hbm.items.machine.ZirnoxFuel.LES) {
                waste = makeData(RADIATION, fuel.depletedRadiation * 0.0375F).addEntry(CONTAMINATING, 40F);
            }
            HazardSystem.register(com.hbm.items.ModZirnoxItems.waste(fuel), waste);
        }
        regItem("rod_zirnox_tritium", makeData(RADIATION, 0.001F));
    }

    private static void registerResearchPlateFuel() {
        registerOtherFuel(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.U233),
                u233 * ingot, wst * ingot * 13F, false);
        registerOtherFuel(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.U235),
                u235 * ingot, wst * ingot * 10F, false);
        registerOtherFuel(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.MOX),
                mox * ingot, wst * ingot * 16F, false);
        registerOtherFuel(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.PU239),
                pu239 * ingot, wst * ingot * 13.5F, false);
        registerOtherFuel(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.SA326),
                sa326 * ingot, wst * ingot * 10F, true);
        registerOtherFuel(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.RA226BE),
                rabe * billet, pobe * nugget * 3F, false);
        registerOtherFuel(ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.PU238BE),
                pube * billet, pube * nugget, false);
    }

    /** Current ntm-next RBMK rod and pellet hazards, including depletion, heat, and DRX digamma. */
    private static void registerRBMKFuel() {
        RBMKBuilder b = new RBMKBuilder();
        b.rod(ModRBMKItems.rbmk_fuel_ueu, u * rod_rbmk, wst * rod_rbmk * 20D);
        b.rod(ModRBMKItems.rbmk_fuel_meu, uf * rod_rbmk, wst * rod_rbmk * 21.5D);
        b.rod(ModRBMKItems.rbmk_fuel_heu233, u233 * rod_rbmk, wst * rod_rbmk * 31D);
        b.rod(ModRBMKItems.rbmk_fuel_heu235, u235 * rod_rbmk, wst * rod_rbmk * 30D);
        b.rod(ModRBMKItems.rbmk_fuel_uzh, uzh * rod_rbmk, wst * rod_rbmk * 20D);
        b.rod(ModRBMKItems.rbmk_fuel_thmeu, thf * rod_rbmk, wst * rod_rbmk * 17.5D);
        b.rod(ModRBMKItems.rbmk_fuel_lep, puf * rod_rbmk, wst * rod_rbmk * 25D);
        b.rod(ModRBMKItems.rbmk_fuel_mep, purg * rod_rbmk, wst * rod_rbmk * 30D);
        b.rod(ModRBMKItems.rbmk_fuel_hep, pu239 * rod_rbmk, wst * rod_rbmk * 32.5D);
        b.rod(ModRBMKItems.rbmk_fuel_hep241, pu241 * rod_rbmk, wst * rod_rbmk * 35D);
        b.rod(ModRBMKItems.rbmk_fuel_lea, amf * rod_rbmk, wst * rod_rbmk * 26D);
        b.rod(ModRBMKItems.rbmk_fuel_mea, amrg * rod_rbmk, wst * rod_rbmk * 30.5D);
        b.rod(ModRBMKItems.rbmk_fuel_hea241, am241 * rod_rbmk, wst * rod_rbmk * 33.5D);
        b.rod(ModRBMKItems.rbmk_fuel_hea242, am242 * rod_rbmk, wst * rod_rbmk * 34D);
        b.rod(ModRBMKItems.rbmk_fuel_men, npf * rod_rbmk, wst * rod_rbmk * 22.5D);
        b.rod(ModRBMKItems.rbmk_fuel_hen, np237 * rod_rbmk, wst * rod_rbmk * 30D);
        b.rod(ModRBMKItems.rbmk_fuel_mox, mox * rod_rbmk, wst * rod_rbmk * 25.5D);
        b.rod(ModRBMKItems.rbmk_fuel_les, saf * rod_rbmk, wst * rod_rbmk * 24.5D);
        b.rod(ModRBMKItems.rbmk_fuel_mes, saf * rod_rbmk, wst * rod_rbmk * 30D);
        b.rod(ModRBMKItems.rbmk_fuel_hes, saf * rod_rbmk, wst * rod_rbmk * 50D);
        b.rod(ModRBMKItems.rbmk_fuel_leaus, 0D, wst * rod_rbmk * 37.5D);
        b.rod(ModRBMKItems.rbmk_fuel_heaus, 0D, wst * rod_rbmk * 32.5D);
        b.rod(ModRBMKItems.rbmk_fuel_po210be, pobe * rod_rbmk, pobe * rod_rbmk * 0.1D, true);
        b.rod(ModRBMKItems.rbmk_fuel_ra226be, rabe * rod_rbmk, rabe * rod_rbmk * 0.4D, true);
        b.rod(ModRBMKItems.rbmk_fuel_pu238be, pube * rod_rbmk, wst * rod_rbmk * 2.5D);
        b.rod(ModRBMKItems.rbmk_fuel_balefire_gold, au198 * rod_rbmk, bf * rod_rbmk * 0.5D, true);
        b.rod(ModRBMKItems.rbmk_fuel_flashlead, pb209 * 1.25D * rod_rbmk, pb209 * nugget * 0.05D * rod_rbmk, true);
        b.rod(ModRBMKItems.rbmk_fuel_balefire, bf * rod_rbmk, bf * rod_rbmk * 100D, true);
        b.rod(ModRBMKItems.rbmk_fuel_zfb_bismuth, pu241 * rod_rbmk * 0.1D, wst * rod_rbmk * 5D);
        b.rod(ModRBMKItems.rbmk_fuel_zfb_pu241, pu239 * rod_rbmk * 0.1D, wst * rod_rbmk * 7.5D);
        b.rod(ModRBMKItems.rbmk_fuel_zfb_am_mix, pu241 * rod_rbmk * 0.1D, wst * rod_rbmk * 10D);
        b.rbmk(ModRBMKItems.rbmk_fuel_drx, bf * rod_rbmk, bf * rod_rbmk * 100D, true, true, 0D, 1D / 3D);

        b.pellet(ModRBMKItems.rbmk_fuel_ueu, u * billet, wst * billet * 20D);
        b.pellet(ModRBMKItems.rbmk_fuel_meu, uf * billet, wst * billet * 21.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_heu233, u233 * billet, wst * billet * 31D);
        b.pellet(ModRBMKItems.rbmk_fuel_heu235, u235 * billet, wst * billet * 30D);
        b.pellet(ModRBMKItems.rbmk_fuel_uzh, uzh * billet, wst * billet * 20D);
        b.pellet(ModRBMKItems.rbmk_fuel_thmeu, thf * billet, wst * billet * 17.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_lep, puf * billet, wst * billet * 25D);
        b.pellet(ModRBMKItems.rbmk_fuel_mep, purg * billet, wst * billet * 30D);
        b.pellet(ModRBMKItems.rbmk_fuel_hep, pu239 * billet, wst * billet * 32.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_hep241, pu241 * billet, wst * billet * 35D);
        b.pellet(ModRBMKItems.rbmk_fuel_lea, amf * billet, wst * billet * 26D);
        b.pellet(ModRBMKItems.rbmk_fuel_mea, amrg * billet, wst * billet * 30.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_hea241, am241 * billet, wst * billet * 33.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_hea242, am242 * billet, wst * billet * 34D);
        b.pellet(ModRBMKItems.rbmk_fuel_men, npf * billet, wst * billet * 22.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_hen, np237 * billet, wst * billet * 30D);
        b.pellet(ModRBMKItems.rbmk_fuel_mox, mox * billet, wst * billet * 25.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_les, saf * billet, wst * billet * 24.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_mes, saf * billet, wst * billet * 30D);
        b.pellet(ModRBMKItems.rbmk_fuel_hes, saf * billet, wst * billet * 50D);
        b.pellet(ModRBMKItems.rbmk_fuel_leaus, 0D, wst * billet * 37.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_heaus, 0D, wst * billet * 32.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_po210be, pobe * billet, pobe * billet * 0.1D, true);
        b.pellet(ModRBMKItems.rbmk_fuel_ra226be, rabe * billet, rabe * billet * 0.4D, true);
        b.pellet(ModRBMKItems.rbmk_fuel_pu238be, pube * billet, wst * 1.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_balefire_gold, au198 * billet, bf * billet * 0.5D, true);
        b.pellet(ModRBMKItems.rbmk_fuel_flashlead, pb209 * 1.25D * billet, pb209 * nugget * 0.05D, true);
        b.pellet(ModRBMKItems.rbmk_fuel_balefire, bf * billet, bf * billet * 100D, true);
        b.pellet(ModRBMKItems.rbmk_fuel_zfb_bismuth, pu241 * billet * 0.1D, wst * billet * 5D);
        b.pellet(ModRBMKItems.rbmk_fuel_zfb_pu241, pu239 * billet * 0.1D, wst * billet * 7.5D);
        b.pellet(ModRBMKItems.rbmk_fuel_zfb_am_mix, pu241 * billet * 0.1D, wst * billet * 10D);
        b.pellet(ModRBMKItems.rbmk_fuel_drx, bf * billet, bf * billet * 100D, true, 0D, 1D / 24D);
    }

    private static final class RBMKBuilder {
        void rod(dev.golem.core.registry.RegistryObject<ItemRBMKRod> rod, double base, double depleted) {
            rbmk(rod, base, depleted, true, false, 0D, 0D);
        }

        void rod(dev.golem.core.registry.RegistryObject<ItemRBMKRod> rod, double base, double depleted, boolean linear) {
            rbmk(rod, base, depleted, true, linear, 0D, 0D);
        }

        void rbmk(dev.golem.core.registry.RegistryObject<ItemRBMKRod> rod, double base, double depleted, boolean hot,
                  boolean linear, double blinding, double digamma) {
            HazardData hazard = data(base, depleted, hot, linear, blinding, digamma);
            HazardSystem.register(rod.get(), hazard);
        }

        void pellet(dev.golem.core.registry.RegistryObject<ItemRBMKRod> rod, double base, double depleted) {
            pellet(rod, base, depleted, false, 0D, 0D);
        }

        void pellet(dev.golem.core.registry.RegistryObject<ItemRBMKRod> rod, double base, double depleted, boolean linear) {
            pellet(rod, base, depleted, linear, 0D, 0D);
        }

        void pellet(dev.golem.core.registry.RegistryObject<ItemRBMKRod> rod, double base, double depleted, boolean linear,
                    double blinding, double digamma) {
            dev.golem.core.registry.RegistryObject<ItemRBMKPellet> pellet = ModRBMKItems.PELLETS.get(rod);
            if (pellet == null) throw new IllegalStateException("no RBMK pellet registered for " + rod.getId());
            HazardSystem.register(pellet.get(), data(base, depleted, false, linear, blinding, digamma));
        }

        private HazardData data(double base, double depleted, boolean hot, boolean linear, double blinding, double digamma) {
            HazardData hazard = new HazardData().addEntry(new HazardEntry(RADIATION, (float) base)
                    .addMod(new HazardModifierRBMKRadiation(depleted, linear)));
            if (hot) hazard.addEntry(new HazardEntry(HOT, 0F).addMod(new HazardModifierRBMKHot()));
            if (blinding > 0D) hazard.addEntry(BLINDING, (float) blinding);
            if (digamma > 0D) hazard.addEntry(DIGAMMA, (float) digamma);
            return hazard;
        }
    }

    // ---- helpers ----

    private static TagKey<Item> cTag(MaterialShapes shape, String name) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, dev.golem.core.Ids.of("c", shape.tagPlural + "/" + name));
    }

    private static HazardData makeData() { return new HazardData(); }
    private static HazardData makeData(IHazardType hazard) { return new HazardData().addEntry(hazard); }
    private static HazardData makeData(IHazardType hazard, float level) { return new HazardData().addEntry(hazard, level); }

    private static void reg(Item item, HazardData data) {
        HazardSystem.register(item, data);
    }

    /**
     * Registers a hazard for an HBM item by id, silently skipping if the item isn't ported yet. Resolved
     * from the item registry (this runs after registration), so it doesn't need a field reference into the
     * auto-generated {@code ModItems}.
     */
    private static void regItem(String id, HazardData data) {
        Identifier loc = dev.golem.core.Ids.of(Constants.MOD_ID, id);
        Item item = BuiltInRegistries.ITEM.getValue(loc);
        if (item != null && item != net.minecraft.world.item.Items.AIR) {
            HazardSystem.register(item, data);
        }
    }

    private static void registerOtherFuel(Item fuel, float base, float target, boolean blinding) {
        HazardData data = new HazardData();
        data.addEntry(new HazardEntry(RADIATION, base).addMod(new HazardModifierFuelRadiation(target)));
        if (blinding) data.addEntry(BLINDING, 20F);
        HazardSystem.register(fuel, data);
    }
}
