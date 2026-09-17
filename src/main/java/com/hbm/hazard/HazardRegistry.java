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
 * <p>Coverage note: entries whose old item isn't ported yet are commented with {@code // TODO(port)};
 * the material families (uranium/plutonium/schrabidium/…) all land now because their base items were
 * ported in earlier waves and their {@code c:} tags exist.
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
        // Deferred: ForgeFluid / RadiationContainer / RadiationME transformers
    }

    public static void registerItems() {

        // ---- explosives (vanilla + hbm sticks) ----
        reg(net.minecraft.world.item.Items.GUNPOWDER, makeData(EXPLOSIVE, 1F));
        reg(net.minecraft.world.item.Items.TNT, makeData(EXPLOSIVE, 4F));
        regItem("ball_dynamite", makeData(EXPLOSIVE, 2F));
        regItem("stick_dynamite", makeData(EXPLOSIVE, 1F));
        regItem("cordite", makeData(EXPLOSIVE, 2F));
        regItem("ballistite", makeData(EXPLOSIVE, 1F));

        // ---- coal / lignite dust (tag-based) ----
        // TODO(port): dustCoal / dustLignite tags — coal dust items not in the material system yet

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

        // ---- crystals ----
        regItem("crystal_phosphorus", makeData(HOT, 2F * crystal));
        regItem("crystal_trixite", makeData(RADIATION, trx * crystal));

        // ---- nuke components (bomb items that exist) ----
        regItem("man_core", makeData(RADIATION, pu239 * nugget * 10));
        regItem("mike_core", makeData(RADIATION, u238 * nugget * 10));

        // ---- hazard BLOCK items (ModHazardBlocks); the block reads this back via getRawRadsFromBlock ----
        registerHazardBlocks();

        // ---- radioactive material families (tags) ----
        registerMat(U, u);
        registerMat(U233, u233);
        registerMat(U235, u235);
        registerMat(U238, u238);
        registerMat(TH232, th232);
        registerMat(PU, pu);
        registerMat(PU238, pu238, HOT, 3F);
        registerMat(PU239, pu239);
        registerMat(PU240, pu240);
        registerMat(PU241, pu241);
        registerMat(AM241, am241);
        registerMat(AM242, am242);
        registerMat(NP237, np237);
        registerMat(PO210, po210, HOT, 3F);
        registerMat(TC99, tc99);
        registerMat(RA226, ra226);
        registerMat(AC227, ac227);
        registerMat(CO60, co60, HOT, 1F);
        registerMat(AU198, au198, HOT, 5F);
        registerMatBlinding(PB209, pb209, 50F, 7F);
        registerMatBlinding(SA326, sa326, 50F, 0F);
        registerMatBlinding(SA327, sa327, 50F, 0F);
        registerMatBlinding(SBD, sb, 50F, 0F);
        registerMatBlinding(SRN, sr, 50F, 0F);
        registerMat(GH336, gh336);

        // Blacklist: raw ores of thorium/uranium never irradiate (they're common worldgen)
        HazardSystem.blacklist(TH232.tag(MaterialShapes.ORE));
        HazardSystem.blacklist(U.tag(MaterialShapes.ORE));
    }

    /**
     * Radiation on the hazard BLOCK items (values = the old {@code OreDictManager} {@code .rad(...).block(...)}
     * chains + the directly‑registered fuel/special blocks; {@code block = 10}). {@link
     * com.hbm.blocks.ModHazardBlocks#initHazards()} reads these back via {@code getRawRadsFromBlock}.
     */
    private static void registerHazardBlocks() {
        // metal storage blocks (isotope × block)
        regItem("block_uranium", makeData(RADIATION, u * block));
        regItem("block_u233", makeData(RADIATION, u233 * block));
        regItem("block_u235", makeData(RADIATION, u235 * block));
        regItem("block_u238", makeData(RADIATION, u238 * block));
        regItem("block_thorium", makeData(RADIATION, th232 * block));
        regItem("block_plutonium", makeData(RADIATION, pu * block));
        regItem("block_pu238", makeData().addEntry(RADIATION, pu238 * block).addEntry(HOT, 3F));
        regItem("block_pu239", makeData(RADIATION, pu239 * block));
        regItem("block_pu240", makeData(RADIATION, pu240 * block));
        regItem("block_pu_mix", makeData(RADIATION, purg * block));
        regItem("block_neptunium", makeData(RADIATION, np237 * block));
        regItem("block_polonium", makeData().addEntry(RADIATION, po210 * block).addEntry(HOT, 3F));
        regItem("block_ra226", makeData(RADIATION, ra226 * block));
        regItem("block_au198", makeData().addEntry(RADIATION, au198 * block).addEntry(HOT, 5F));
        regItem("block_schrabidium", makeData().addEntry(RADIATION, sa326 * block).addEntry(BLINDING, 50F));
        regItem("block_solinium", makeData().addEntry(RADIATION, sa327 * block).addEntry(BLINDING, 50F));
        regItem("block_schrabidate", makeData().addEntry(RADIATION, sb * block).addEntry(BLINDING, 50F));
        regItem("block_schraranium", makeData().addEntry(RADIATION, sr * block).addEntry(BLINDING, 50F));
        // fuel blocks
        regItem("block_uranium_fuel", makeData(RADIATION, uf * block));
        regItem("block_plutonium_fuel", makeData(RADIATION, puf * block));
        regItem("block_thorium_fuel", makeData(RADIATION, thf * block));
        regItem("block_mox_fuel", makeData(RADIATION, mox * block));
        regItem("block_schrabidium_fuel", makeData().addEntry(RADIATION, saf * block).addEntry(BLINDING, 5F * block));
        // specials
        regItem("block_trinitite", makeData(RADIATION, trn * block));
        regItem("block_white_phosphorus", makeData(HOT, 5F * block));
        regItem("block_corium", makeData(RADIATION, 150F));
        // block_magnetized_tungsten has no hazard (0 radiation) — no entry.

        // fallout (the ash item + the layer block's item; old HazardRegistry lines 481-482).
        // Note: the fallout LAYER's in-world emission is its own scheduled tick (BlockFallout), not
        // the ModHazardBlocks initHazards pass — these entries are the held-item hazards only.
        regItem("falloutitem", makeData(RADIATION, fo * powder).addEntry(CONTAMINATING, fo));
        regItem("fallout", makeData(RADIATION, fo * powder * 2));
        // sellafite radgem ore (the one sellafield block with an item hazard in the old registry)
        regItem("ore_sellafield_radgem", makeData(RADIATION, 25F));
    }

    /** Registers a radioactive material across all standard shapes (nugget/ingot/dust/billet/block/plate/crystal). */
    private static void registerMat(NTMMaterial mat, float baseRad) {
        registerMat(mat, baseRad, null, 0F);
    }

    private static void registerMat(NTMMaterial mat, float baseRad, IHazardType extra, float extraLevel) {
        registerShape(mat, MaterialShapes.NUGGET, baseRad * nugget, extra, extraLevel);
        registerShape(mat, MaterialShapes.INGOT, baseRad * ingot, extra, extraLevel);
        registerShape(mat, MaterialShapes.DUST, baseRad * powder, extra, extraLevel);
        registerShape(mat, MaterialShapes.BILLET, baseRad * billet, extra, extraLevel);
        registerShape(mat, MaterialShapes.BLOCK, baseRad * block, extra, extraLevel);
        registerShape(mat, MaterialShapes.PLATE, baseRad * plate, extra, extraLevel);
        registerShape(mat, MaterialShapes.CRYSTAL, baseRad * crystal, extra, extraLevel);
    }

    private static void registerMatBlinding(NTMMaterial mat, float baseRad, float blinding, float hot) {
        for (MaterialShapes shape : new MaterialShapes[] {MaterialShapes.NUGGET, MaterialShapes.INGOT,
                MaterialShapes.DUST, MaterialShapes.BILLET, MaterialShapes.BLOCK, MaterialShapes.PLATE, MaterialShapes.CRYSTAL}) {
            float mult = shapeMult(shape);
            HazardData data = new HazardData().setMutex(0b1).addEntry(RADIATION, baseRad * mult).addEntry(BLINDING, blinding);
            if (hot > 0) data.addEntry(HOT, hot);
            HazardSystem.register(mat.tag(shape), data);
        }
    }

    private static void registerShape(NTMMaterial mat, MaterialShapes shape, float level, IHazardType extra, float extraLevel) {
        HazardData data = new HazardData().setMutex(0b1).addEntry(RADIATION, level);
        if (extra != null && extraLevel > 0) data.addEntry(extra, extraLevel);
        HazardSystem.register(mat.tag(shape), data);
    }

    private static float shapeMult(MaterialShapes shape) {
        return switch (shape) {
            case NUGGET -> nugget;
            case DUST -> powder;
            case BILLET -> billet;
            case BLOCK -> block;
            case CRYSTAL -> crystal;
            default -> ingot;
        };
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
